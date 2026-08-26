package com.team1.hangsha.notification.service

import com.team1.hangsha.notification.model.NotificationPreference
import com.team1.hangsha.notification.model.PushDevice
import com.team1.hangsha.notification.model.PushPlatform
import com.team1.hangsha.notification.model.PushTargetKind
import com.team1.hangsha.notification.repository.NotificationPreferenceRepository
import com.team1.hangsha.notification.repository.PushDeviceRepository
import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserNotificationService(
    private val pushDeviceRepository: PushDeviceRepository,
    private val notificationPreferenceRepository: NotificationPreferenceRepository,
) {
    @Transactional
    fun registerPushDevice(
        userId: Long,
        targetKind: PushTargetKind,
        pushTarget: String,
        platform: PushPlatform,
        appVersion: String?,
    ): PushDevice {
        val normalizedTarget = pushTarget.trim()
        if (normalizedTarget.isBlank()) {
            throw DomainException(ErrorCode.INVALID_REQUEST, "pushTarget은 비어 있을 수 없습니다")
        }

        return pushDeviceRepository.upsert(
            userId = userId,
            targetKind = targetKind,
            pushTarget = normalizedTarget,
            platform = platform,
            appVersion = appVersion?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    @Transactional
    fun deletePushDevice(userId: Long, deviceId: Long) {
        // 삭제 대상이 없거나 다른 사용자의 기기여도 204를 반환하는 idempotent API입니다.
        pushDeviceRepository.deleteByIdAndUserId(deviceId, userId)
    }

    @Transactional(readOnly = true)
    fun getPreference(userId: Long): NotificationPreference =
        notificationPreferenceRepository.findByUserId(userId) ?: NotificationPreference()

    @Transactional
    fun replacePreference(userId: Long, preference: NotificationPreference): NotificationPreference {
        notificationPreferenceRepository.upsert(userId, preference)
        return preference
    }
}
