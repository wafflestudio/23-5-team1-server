package com.team1.hangsha.notification.repository

import com.team1.hangsha.notification.model.PushDevice
import com.team1.hangsha.notification.model.PushPlatform
import com.team1.hangsha.notification.model.PushTargetKind
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class PushDeviceRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    /**
     * 동일 FCM 대상은 마지막으로 로그인한 사용자에게 연결합니다.
     * 앱 재설치, 계정 전환, FCM 대상 갱신을 모두 하나의 upsert로 처리합니다.
     */
    fun upsert(
        userId: Long,
        targetKind: PushTargetKind,
        pushTarget: String,
        platform: PushPlatform,
        appVersion: String?,
    ): PushDevice {
        val sql = """
            INSERT INTO push_devices (
                user_id, target_kind, push_target, platform, app_version, is_active, last_seen_at
            ) VALUES (
                :userId, :targetKind, :pushTarget, :platform, :appVersion, TRUE, CURRENT_TIMESTAMP(6)
            )
            ON DUPLICATE KEY UPDATE
                user_id = VALUES(user_id),
                platform = VALUES(platform),
                app_version = VALUES(app_version),
                is_active = TRUE,
                last_seen_at = CURRENT_TIMESTAMP(6)
        """.trimIndent()

        val params = mapOf(
            "userId" to userId,
            "targetKind" to targetKind.name,
            "pushTarget" to pushTarget,
            "platform" to platform.name,
            "appVersion" to appVersion,
        )
        jdbc.update(sql, params)

        return findByTarget(targetKind, pushTarget)
            ?: error("push device upsert succeeded but no row was found")
    }

    fun deleteByIdAndUserId(id: Long, userId: Long): Int = jdbc.update(
        """
            DELETE FROM push_devices
            WHERE id = :id AND user_id = :userId
        """.trimIndent(),
        mapOf("id" to id, "userId" to userId),
    )

    fun findActiveByUserId(userId: Long): List<PushDevice> = jdbc.query(
        """
            SELECT id, user_id, target_kind, push_target, platform, is_active
            FROM push_devices
            WHERE user_id = :userId AND is_active = TRUE
            ORDER BY id ASC
        """.trimIndent(),
        mapOf("userId" to userId),
    ) { rs, _ -> rs.toPushDevice() }

    private fun findByTarget(targetKind: PushTargetKind, pushTarget: String): PushDevice? = jdbc.query(
        """
            SELECT id, user_id, target_kind, push_target, platform, is_active
            FROM push_devices
            WHERE target_kind = :targetKind AND push_target = :pushTarget
            LIMIT 1
        """.trimIndent(),
        mapOf("targetKind" to targetKind.name, "pushTarget" to pushTarget),
    ) { rs, _ -> rs.toPushDevice() }.firstOrNull()
}

private fun java.sql.ResultSet.toPushDevice(): PushDevice = PushDevice(
    id = getLong("id"),
    userId = getLong("user_id"),
    targetKind = PushTargetKind.valueOf(getString("target_kind")),
    pushTarget = getString("push_target"),
    platform = PushPlatform.valueOf(getString("platform")),
    isActive = getBoolean("is_active"),
)
