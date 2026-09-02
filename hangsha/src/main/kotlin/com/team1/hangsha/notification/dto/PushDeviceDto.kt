package com.team1.hangsha.notification.dto

import com.team1.hangsha.notification.model.PushDevice
import com.team1.hangsha.notification.model.PushPlatform
import com.team1.hangsha.notification.model.PushTargetKind
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpsertPushDeviceRequest(
    val targetKind: PushTargetKind,
    @field:NotBlank
    @field:Size(max = 512)
    val pushTarget: String,
    val platform: PushPlatform,
    @field:Size(max = 64)
    val appVersion: String? = null,
)

data class PushDeviceResponse(
    val id: Long,
    val targetKind: PushTargetKind,
    val platform: PushPlatform,
    val isActive: Boolean,
) {
    constructor(device: PushDevice) : this(
        id = device.id,
        targetKind = device.targetKind,
        platform = device.platform,
        isActive = device.isActive,
    )
}
