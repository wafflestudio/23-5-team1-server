package com.team1.hangsha.notification.model

/**
 * FCM 발송 대상입니다. 하나의 사용자는 Android/iOS 여러 기기를 등록할 수 있습니다.
 */
data class PushDevice(
    val id: Long,
    val userId: Long,
    val targetKind: PushTargetKind,
    val pushTarget: String,
    val platform: PushPlatform,
    val isActive: Boolean,
)

enum class PushTargetKind {
    FID,
    FCM_REGISTRATION_TOKEN,
}

enum class PushPlatform {
    ANDROID,
    IOS,
}
