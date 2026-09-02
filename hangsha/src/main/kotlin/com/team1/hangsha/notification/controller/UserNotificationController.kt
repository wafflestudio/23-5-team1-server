package com.team1.hangsha.notification.controller

import com.team1.hangsha.notification.dto.NotificationPreferenceResponse
import com.team1.hangsha.notification.dto.PushDeviceResponse
import com.team1.hangsha.notification.dto.ReplaceNotificationPreferenceRequest
import com.team1.hangsha.notification.dto.UpsertPushDeviceRequest
import com.team1.hangsha.notification.model.NotificationPreference
import com.team1.hangsha.notification.service.UserNotificationService
import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
class UserNotificationController(
    private val userNotificationService: UserNotificationService,
) {
    @PutMapping("/push-devices")
    fun upsertPushDevice(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @Valid @RequestBody request: UpsertPushDeviceRequest,
    ): ResponseEntity<PushDeviceResponse> {
        val device = userNotificationService.registerPushDevice(
            userId = requireNotNull(user.id),
            targetKind = request.targetKind,
            pushTarget = request.pushTarget,
            platform = request.platform,
            appVersion = request.appVersion,
        )
        return ResponseEntity.ok(PushDeviceResponse(device))
    }

    @DeleteMapping("/push-devices/{deviceId}")
    fun deletePushDevice(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable deviceId: Long,
    ): ResponseEntity<Void> {
        userNotificationService.deletePushDevice(requireNotNull(user.id), deviceId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/notification-preferences")
    fun getNotificationPreferences(
        @Parameter(hidden = true) @LoggedInUser user: User,
    ): ResponseEntity<NotificationPreferenceResponse> =
        ResponseEntity.ok(
            NotificationPreferenceResponse(userNotificationService.getPreference(requireNotNull(user.id))),
        )

    @PutMapping("/notification-preferences")
    fun replaceNotificationPreferences(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestBody request: ReplaceNotificationPreferenceRequest,
    ): ResponseEntity<NotificationPreferenceResponse> {
        val preference = userNotificationService.replacePreference(
            userId = requireNotNull(user.id),
            preference = NotificationPreference(
                pushEnabled = request.pushEnabled,
                bookmarkEventEnabled = request.bookmarkEventEnabled,
                interestRecommendationEnabled = request.interestRecommendationEnabled,
                weeklyAiRecommendationEnabled = request.weeklyAiRecommendationEnabled,
            ),
        )
        return ResponseEntity.ok(NotificationPreferenceResponse(preference))
    }
}
