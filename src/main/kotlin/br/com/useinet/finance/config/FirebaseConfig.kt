package br.com.useinet.finance.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseConfig {

    @Value("\${firebase.project-id}")
    private lateinit var projectId: String

    @Bean
    @ConditionalOnMissingBean(FirebaseAuth::class)
    fun firebaseAuth(): FirebaseAuth {
        if (FirebaseApp.getApps().isEmpty()) {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(projectId)
                .build()
            FirebaseApp.initializeApp(options)
        }
        return FirebaseAuth.getInstance()
    }
}
