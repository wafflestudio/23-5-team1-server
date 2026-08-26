package com.team1.hangsha.batch.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// notification CronJob에만 firebase.enabled=true와 GOOGLE_APPLICATION_CREDENTIALS를 설정
@Configuration
@ConditionalOnProperty(prefix = "firebase", name = ["enabled"], havingValue = "true")
class FirebaseMessagingConfig {
    @Bean
    fun firebaseMessaging(): FirebaseMessaging {
        val app = FirebaseApp.getApps().firstOrNull()
            ?: FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build(),
            )
        return FirebaseMessaging.getInstance(app)
    }
}
