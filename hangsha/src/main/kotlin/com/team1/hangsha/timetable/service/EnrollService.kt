package com.team1.hangsha.timetable.service

import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.common.enums.CourseSource
import com.team1.hangsha.course.model.Course
import com.team1.hangsha.course.model.CourseTimeSlot
import com.team1.hangsha.course.dto.core.CourseTimeSlotDto
import com.team1.hangsha.course.repository.*
import com.team1.hangsha.course.model.toCourseDto
import com.team1.hangsha.timetable.dto.*
import com.team1.hangsha.timetable.model.Enroll
import com.team1.hangsha.timetable.repository.*
import com.team1.hangsha.timetable.model.toCourseDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

@Service
class EnrollService(
    private val timetableRepository: TimetableRepository,
    private val enrollRepository: EnrollRepository,
    private val courseRepository: CourseRepository,
    private val courseTimeSlotRepository: CourseTimeSlotRepository,
    private val objectMapper: ObjectMapper,
) {

    // -------------------------
    // GET /timetables/{timetableId}/enrolls
    // -------------------------
    @Transactional(readOnly = true)
    fun listEnrolls(userId: Long, timetableId: Long): ListEnrollsResponse {
        val timetable = getOwnedTimetable(userId, timetableId)

        val rows = enrollRepository.findAllWithCourseByTimetableId(timetable.id!!)
        if (rows.isEmpty()) return ListEnrollsResponse(items = emptyList())

        val courseIds = rows.map { it.courseId }.distinct()

        // timeSlots는 별도 조회 필요 (여기서는 IN 조회 메서드가 없으면 루프라도 OK)
        val slots = courseTimeSlotRepository
            .findAllByCourseIdsOrderByCourseIdAscDayOfWeekAscStartAtAsc(courseIds)

        val slotsByCourseId = slots.groupBy { it.courseId }

        val items = rows.map { row ->
            EnrollResponse(
                enrollId = row.enrollId,
                course = row.toCourseDto(slotsByCourseId[row.courseId].orEmpty())
            )
        }
        return ListEnrollsResponse(items)
    }

    // -------------------------
    // GET /timetables/{timetableId}/enrolls/{enrollId}
    // -------------------------
    @Transactional(readOnly = true)
    fun getEnroll(userId: Long, timetableId: Long, enrollId: Long): EnrollResponse {
        getOwnedTimetable(userId, timetableId)

        val row = enrollRepository.findOneWithCourseByTimetableIdAndEnrollId(timetableId, enrollId)
            ?: throw DomainException(ErrorCode.ENROLL_NOT_FOUND)

        val slots = courseTimeSlotRepository.findAllByCourseId(row.courseId)
        return EnrollResponse(
            enrollId = row.enrollId,
            course = row.toCourseDto(slots)
        )
    }

    // -------------------------
    // POST /timetables/{timetableId}/enrolls/custom
    // 커스텀 강의 생성 + enroll 생성
    // -------------------------
    @Transactional
    fun createCustomCourseAndEnroll(userId: Long, timetableId: Long, req: CreateCustomCourseRequest): EnrollResponse {
        val timetable = getOwnedTimetable(userId, timetableId)

        // 권장: 시간표 학기/년도 일치
        if (req.year != timetable.year || req.semester != timetable.semester) {
            throw DomainException(ErrorCode.TIMETABLE_TERM_MISMATCH)
        }

        assertNoTimeConflict(
            timetableId = timetable.id!!,
            newSlots = req.timeSlots,
            excludeCourseId = null,
        )


        val course = courseRepository.save(
            Course(
                year = req.year,
                semester = req.semester,
                courseTitle = req.courseTitle.trim(),
                source = CourseSource.CUSTOM,
                ownerUserId = userId, // 네 스키마에 owner_user_id가 있어서 넣는 걸 권장
                courseNumber = req.courseNumber,
                lectureNumber = req.lectureNumber,
                credit = req.credit,
                instructor = req.instructor
            )
        )

        val slots = req.timeSlots.map { s ->
            CourseTimeSlot(
                courseId = course.id!!,
                dayOfWeek = s.dayOfWeek,
                startAt = s.startAt,
                endAt = s.endAt
            )
        }
        courseTimeSlotRepository.saveAll(slots)

        val enroll = enrollRepository.save(
            Enroll(
                timetableId = timetable.id!!,
                courseId = course.id!!
            )
        )

        return EnrollResponse(
            enrollId = enroll.id!!,
            course = course.toCourseDto(slots)
        )
    }

    // -------------------------
    // (추후) POST /timetables/{timetableId}/enrolls  (크롤링 강의 추가)
    // -------------------------
//    @Transactional
//    fun addCrawledCourse(userId: Long, timetableId: Long, courseId: Long): AddCourseResponse {
//        val timetable = getOwnedTimetable(userId, timetableId)
//
//        // course 존재 검증 + CRAWLED인지 확인
//        val course = courseRepository.findById(courseId).orElseThrow {
//            DomainException(ErrorCode.COURSE_NOT_FOUND)
//        }
//
//        // 중복 enroll 방지
//        if (enrollRepository.existsByTimetableIdAndCourseId(timetable.id!!, courseId)) {
//            throw DomainException(ErrorCode.ENROLL_ALREADY_EXISTS)
//        }
//
//        val enroll = enrollRepository.save(
//            Enroll(timetableId = timetable.id!!, courseId = courseId)
//        )
//        return AddCourseResponse(enrollId = enroll.id!!)
//    }

    // -------------------------
    // PATCH /timetables/{timetableId}/enrolls/{enrollId}
    // 커스텀 강의만 수정 가능
    // 정책:
    // - courseTitle/timeSlots null 금지
    // - timeSlots 빈 배열 금지
    // -------------------------
    @Transactional
    fun updateCustomEnroll(
        userId: Long,
        timetableId: Long,
        enrollId: Long,
        body: JsonNode,
    ): EnrollResponse {
        getOwnedTimetable(userId, timetableId)

        val row = enrollRepository.findOneWithCourseByTimetableIdAndEnrollId(timetableId, enrollId)
            ?: throw DomainException(ErrorCode.ENROLL_NOT_FOUND)

        if (row.source != CourseSource.CUSTOM) {
            throw DomainException(ErrorCode.COURSE_NOT_EDITABLE)
        }

        val hasCourseTitle = body.has("courseTitle")
        val hasTimeSlots = body.has("timeSlots")
        val hasCourseNumber = body.has("courseNumber")
        val hasLectureNumber = body.has("lectureNumber")
        val hasCredit = body.has("credit")
        val hasInstructor = body.has("instructor")

        if (!hasCourseTitle && !hasTimeSlots && !hasCourseNumber && !hasLectureNumber && !hasCredit && !hasInstructor) {
            throw DomainException(ErrorCode.ENROLL_PATCH_EMPTY)
        }

        val req = try {
            objectMapper.treeToValue(body, UpdateCustomCourseRequest::class.java)
        } catch (e: Exception) {
            throw DomainException(ErrorCode.INVALID_REQUEST, "Invalid request body")
        }

        val course = courseRepository.findById(row.courseId).orElseThrow {
            DomainException(ErrorCode.INTERNAL_ERROR, "Course not found")
        }

        // -------- courseTitle 정책: null 금지 + blank 금지 --------
        if (hasCourseTitle) {
            if (body.get("courseTitle").isNull) throw DomainException(ErrorCode.COURSE_TITLE_CANNOT_BE_NULL)
            val title = (req.courseTitle ?: "").trim()
            if (title.isBlank()) throw DomainException(ErrorCode.COURSE_TITLE_CANNOT_BE_BLANK)
            course.courseTitle = title
        }

        // -------- timeSlots 정책: null 금지 + empty 금지 + replace-all --------
        if (hasTimeSlots) {
            if (body.get("timeSlots").isNull) throw DomainException(ErrorCode.TIME_SLOTS_CANNOT_BE_NULL)
            val slots = req.timeSlots ?: throw DomainException(ErrorCode.TIME_SLOTS_REQUIRED)
            if (slots.isEmpty()) throw DomainException(ErrorCode.TIME_SLOTS_CANNOT_BE_EMPTY)
            assertNoTimeConflict(
                timetableId = timetableId,
                newSlots = slots,
                excludeCourseId = course.id!!,
            )

            courseTimeSlotRepository.deleteAllByCourseId(course.id!!)
            courseTimeSlotRepository.saveAll(
                slots.map { s ->
                    CourseTimeSlot(
                        courseId = course.id!!,
                        dayOfWeek = s.dayOfWeek,
                        startAt = s.startAt,
                        endAt = s.endAt
                    )
                }
            )
        }

        // -------- optional 필드 정책: 미포함=변경없음 / null=삭제 / 값=업데이트 --------
        if (hasCourseNumber) course.courseNumber = req.courseNumber
        if (hasLectureNumber) course.lectureNumber = req.lectureNumber
        if (hasCredit) course.credit = req.credit
        if (hasInstructor) course.instructor = req.instructor

        courseRepository.save(course)

        val savedSlots = courseTimeSlotRepository.findAllByCourseId(course.id!!)
        return EnrollResponse(
            enrollId = row.enrollId,
            course = course.toCourseDto(savedSlots)
        )
    }

    // -------------------------
    // DELETE /timetables/{timetableId}/enrolls/{enrollId}
    // -------------------------
    @Transactional
    fun deleteEnroll(userId: Long, timetableId: Long, enrollId: Long) {
        getOwnedTimetable(userId, timetableId)

        val affected = enrollRepository.deleteByIdAndTimetableId(enrollId, timetableId)
        if (affected == 0) {
            throw DomainException(ErrorCode.ENROLL_NOT_FOUND)
        }
    }

    // ----------------- helpers -----------------

    private fun getOwnedTimetable(userId: Long, timetableId: Long) =
        timetableRepository.findById(timetableId).orElseThrow {
            DomainException(ErrorCode.TIMETABLE_NOT_FOUND)
        }.also { tt ->
            if (tt.userId != userId) throw DomainException(ErrorCode.TIMETABLE_NOT_FOUND)
        }

    private fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean {
        // [start, end) 반열린구간: 끝이 맞닿는 건 겹침 아님
        return aStart < bEnd && bStart < aEnd
    }

    private fun assertNoTimeConflict(
        timetableId: Long,
        newSlots: List<CourseTimeSlotDto>,
        excludeCourseId: Long? = null, // update 시 자기 자신 course는 제외
    ) {
        if (newSlots.isEmpty()) return

        val rows = enrollRepository.findAllWithCourseByTimetableId(timetableId)
        val existingCourseIds = rows.map { it.courseId }
            .distinct()
            .filter { it != excludeCourseId }

        if (existingCourseIds.isEmpty()) return

        val existingSlots = courseTimeSlotRepository
            .findAllByCourseIdsOrderByCourseIdAscDayOfWeekAscStartAtAsc(existingCourseIds)

        // day별로 묶어서 비교 비용 줄이기
        val existingByDay = existingSlots.groupBy { it.dayOfWeek }

        for (ns in newSlots) {
            val daySlots = existingByDay[ns.dayOfWeek].orEmpty()
            for (es in daySlots) {
                if (overlaps(ns.startAt, ns.endAt, es.startAt, es.endAt)) {
                    throw DomainException(ErrorCode.ENROLL_TIME_CONFLICT)
                }
            }
        }
    }
}