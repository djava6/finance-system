package br.com.useinet.finance

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sqladmin.SQLAdmin
import com.google.api.services.sqladmin.model.DatabaseInstance
import com.google.api.services.sqladmin.model.Settings
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.functions.BackgroundFunction
import com.google.cloud.functions.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.sql.DriverManager
import java.util.Base64

class BillingAlertFunction : BackgroundFunction<BillingAlertFunction.PubSubMessage> {

    data class PubSubMessage(val data: String? = null)

    private val projectId = System.getenv("GCP_PROJECT_ID") ?: "finance-app-489504"
    private val instanceName = System.getenv("CLOUD_SQL_INSTANCE") ?: "finance-db"
    private val cloudSqlInstance = "$projectId:us-central1:$instanceName"
    private val jdbcUrl = "jdbc:postgresql:///finance" +
        "?cloudSqlInstance=$cloudSqlInstance" +
        "&socketFactory=com.google.cloud.sql.postgres.SocketFactory" +
        "&user=postgres"

    override fun accept(message: PubSubMessage, context: Context) {
        val raw = message.data
            ?.let { String(Base64.getDecoder().decode(it)) }
            ?: run { println("Mensagem Pub/Sub sem dados — ignorando."); return }

        val json = Gson().fromJson(raw, JsonObject::class.java)
        println("Billing alert recebido: $json")

        val budgetPct = json.getAsJsonObject("budgetNotification")
            ?.get("alertThresholdExceeded")?.asDouble ?: 0.0
        val costUsd = json.getAsJsonObject("costAmount")
            ?.get("units")?.asDouble ?: 0.0

        suspendCloudSQL()

        logBillingEvent(
            eventType  = "CLOUD_SQL_SUSPENDED",
            service    = instanceName,
            reason     = "Budget alert: ${budgetPct * 100}% do orçamento atingido",
            budgetPct  = budgetPct * 100,
            costUsd    = costUsd,
            triggeredBy = "CloudFunction",
            extraInfo  = json.toString()
        )
    }

    private fun suspendCloudSQL() {
        val credentials = GoogleCredentials.getApplicationDefault()
            .createScoped("https://www.googleapis.com/auth/cloud-platform")

        SQLAdmin.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName("finance-billing-alert")
            .build()
            .instances()
            .patch(
                projectId,
                instanceName,
                DatabaseInstance().setSettings(Settings().setActivationPolicy("NEVER"))
            )
            .execute()

        println("Cloud SQL suspenso automaticamente.")
    }

    private fun logBillingEvent(
        eventType: String,
        service: String,
        reason: String,
        budgetPct: Double,
        costUsd: Double,
        triggeredBy: String,
        extraInfo: String? = null
    ) {
        DriverManager.getConnection(jdbcUrl).use { conn ->
            conn.prepareStatement(
                """INSERT INTO billing_events
                   (event_type, service, reason, budget_pct, cost_usd, triggered_by, extra_info)
                   VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)"""
            ).use { stmt ->
                stmt.setString(1, eventType)
                stmt.setString(2, service)
                stmt.setString(3, reason)
                stmt.setDouble(4, budgetPct)
                stmt.setDouble(5, costUsd)
                stmt.setString(6, triggeredBy)
                stmt.setString(7, extraInfo)
                stmt.executeUpdate()
            }
        }
        println("Evento registrado em billing_events.")
    }
}
