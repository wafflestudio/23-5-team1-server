package com.team1.hangsha.batch.job

import com.team1.hangsha.notification.repository.BookmarkNotificationQueryRepository
import com.team1.hangsha.notification.repository.NotificationOutboxRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
@ConditionalOnProperty(
    prefix = "batch",
    name = ["job"],
    havingValue = "bookmark-notification",
)
class BookmarkNotificationJob(
    private val bookmarkNotificationQueryRepository: BookmarkNotificationQueryRepository,
    private val notificationOutboxRepository: NotificationOutboxRepository,
) : BatchJob {

    override val names: Set<String> = setOf("bookmark-notification")

    override fun run(args: ApplicationArguments) {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

        // 1. 오늘 지원 마감
        // 2. 내일 시작하는 일반 행사
        // 3. 내일 마감하는 공모전/경진대회
        // 4. 북마크 + 수신 설정이 켜진 사용자 조회
        // 5. notification_outbox에 PENDING으로 INSERT
        //    → dedupe_key UNIQUE 제약으로 중복 방지
    }
}