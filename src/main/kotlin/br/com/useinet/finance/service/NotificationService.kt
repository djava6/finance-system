package br.com.useinet.finance.service

import br.com.useinet.finance.model.Usuario
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotificationService {

    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    fun send(usuario: Usuario, title: String, body: String) {
        val token = usuario.fcmToken ?: return
        val message = Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .build()
        try {
            FirebaseMessaging.getInstance().send(message)
        } catch (e: Exception) {
            log.warn("FCM send failed for user ${usuario.email}: ${e.message}")
        }
    }
}
