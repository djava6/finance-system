package br.com.useinet.finance.service

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.postgresql.PGNotification
import org.postgresql.jdbc.PgConnection
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import javax.sql.DataSource

@Service
class TransacaoNotificationService(
    private val dataSource: DataSource,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile private var running = false
    private var listenerThread: Thread? = null

    @PostConstruct
    fun start() {
        running = true
        listenerThread = Thread(::listenLoop, "pg-notify-listener").also {
            it.isDaemon = true
            it.start()
        }
    }

    @PreDestroy
    fun stop() {
        running = false
        listenerThread?.interrupt()
    }

    private fun listenLoop() {
        while (running) {
            try {
                dataSource.connection.use { conn ->
                    val pgConn = conn.unwrap(PgConnection::class.java)
                    pgConn.createStatement().use { it.execute("LISTEN transacoes_channel") }
                    log.info("PostgreSQL LISTEN transacoes_channel iniciado")

                    while (running) {
                        val notifications: Array<PGNotification>? = pgConn.getNotifications(5_000)
                        notifications?.forEach { notification ->
                            val usuarioId = notification.parameter
                            log.debug("NOTIFY recebido para usuário {}", usuarioId)
                            messagingTemplate.convertAndSend("/topic/transacoes/$usuarioId", "updated")
                        }
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                if (running) {
                    log.warn("Listener NOTIFY desconectado, reconectando em 5s: {}", e.message)
                    Thread.sleep(5_000)
                }
            }
        }
    }
}
