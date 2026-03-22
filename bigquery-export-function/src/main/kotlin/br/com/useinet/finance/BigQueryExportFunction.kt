package br.com.useinet.finance

import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.http.HttpCredentialsAdapter
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sqladmin.SQLAdmin
import com.google.api.services.sqladmin.model.ExportContext
import com.google.api.services.sqladmin.model.InstancesExportRequest
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.CsvOptions
import com.google.cloud.bigquery.LoadJobConfiguration
import com.google.cloud.bigquery.TableId
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse

class BigQueryExportFunction : HttpFunction {

    private val projectId  = "finance-app-489504"
    private val instance   = "finance-db"
    private val database   = "finance_app"
    private val bucket     = "gs://finance-exports-489504"
    private val dataset    = "finance_analytics"

    private val tables = listOf("billing_events", "transacoes")

    override fun service(request: HttpRequest, response: HttpResponse) {
        try {
            val credentials = GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/cloud-platform")

            exportFromCloudSql(credentials)
            loadIntoBigQuery()

            response.writer.write("Exportação concluída: ${tables.joinToString(", ")}")
        } catch (e: Exception) {
            response.setStatusCode(500)
            response.writer.write("Erro: ${e.message}")
            throw e
        }
    }

    private fun exportFromCloudSql(credentials: GoogleCredentials) {
        val sqladmin = SQLAdmin.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        ).setApplicationName("bigquery-export").build()

        tables.forEach { table ->
            val exportContext = ExportContext()
                .setKind("sql#exportContext")
                .setFileType("CSV")
                .setUri("$bucket/$table.csv")
                .setDatabases(listOf(database))
                .setCsvExportOptions(
                    ExportContext.CsvExportOptions().setSelectQuery("SELECT * FROM $table")
                )

            val operation = sqladmin.instances()
                .export(projectId, instance, InstancesExportRequest().setExportContext(exportContext))
                .execute()

            // Aguarda a operação de exportação concluir
            waitForOperation(sqladmin, operation.name)
            println("Exportado: $table → $bucket/$table.csv")
        }
    }

    private fun waitForOperation(sqladmin: SQLAdmin, operationName: String) {
        repeat(60) {
            Thread.sleep(5_000)
            val op = sqladmin.operations().get(projectId, operationName).execute()
            if (op.status == "DONE") {
                if (op.error != null) throw RuntimeException("Exportação falhou: ${op.error}")
                return
            }
        }
        throw RuntimeException("Timeout aguardando exportação do Cloud SQL")
    }

    private fun loadIntoBigQuery() {
        val bq = BigQueryOptions.getDefaultInstance().service

        tables.forEach { table ->
            val tableId = TableId.of(projectId, dataset, table)
            val csvOptions = CsvOptions.newBuilder().setSkipLeadingRows(0).build()
            val config = LoadJobConfiguration.newBuilder(tableId, "$bucket/$table.csv")
                .setFormatOptions(csvOptions)
                .setWriteDisposition(com.google.cloud.bigquery.JobInfo.WriteDisposition.WRITE_TRUNCATE)
                .setAutodetect(false)
                .build()

            val job = bq.create(com.google.cloud.bigquery.JobInfo.of(config))
            val completed = job.waitFor()

            if (completed.status.error != null) {
                throw RuntimeException("Load job falhou para $table: ${completed.status.error}")
            }
            println("Carregado no BigQuery: $table")
        }
    }
}
