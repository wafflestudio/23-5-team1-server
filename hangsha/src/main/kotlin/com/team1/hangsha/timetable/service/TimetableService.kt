package com.team1.hangsha.timetable.service

import com.team1.hangsha.common.enums.Semester
import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.timetable.dto.CreateTimetableRequest
import com.team1.hangsha.timetable.dto.ListTimetablesResponse
import com.team1.hangsha.timetable.dto.TimetableResponse
import com.team1.hangsha.timetable.dto.UpdateTimetableRequest
import com.team1.hangsha.timetable.model.Timetable
import com.team1.hangsha.timetable.repository.EnrollRepository
import com.team1.hangsha.timetable.repository.TimetableRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TimetableService(
    private val timetableRepository: TimetableRepository,
    private val enrollRepository: EnrollRepository,
) {

    // -------------------------
    // GET /timetables
    // -------------------------
    @Transactional(readOnly = true)
    fun listTimetables(
        userId: Long,
        year: Int?,
        semester: Semester?,
    ): ListTimetablesResponse {
        val items: List<Timetable> = when {
            year != null && semester != null ->
                timetableRepository.findAllByUserIdAndYearAndSemesterOrderByIdDesc(userId, year, semester)

            year != null ->
                timetableRepository.findAllByUserIdAndYearOrderByIdDesc(userId, year)

            semester != null ->
                timetableRepository.findAllByUserIdAndSemesterOrderByIdDesc(userId, semester)

            else ->
                timetableRepository.findAllByUserIdOrderByIdDesc(userId)
        }

        return ListTimetablesResponse(
            items = items.map { it.toTimetableResponse() }
        )
    }

    // -------------------------
    // POST /timetables
    // -------------------------
    @Transactional
    fun createTimetable(userId: Long, req: CreateTimetableRequest): TimetableResponse {
        if (req.name.isBlank()) throw DomainException(ErrorCode.TIMETABLE_NAME_CANNOT_BE_BLANK)

        val saved = timetableRepository.save(
            Timetable(
                userId = userId,
                name = req.name.trim(),
                year = req.year,
                semester = req.semester
            )
        )

        return saved.toTimetableResponse()
    }

    // -------------------------
    // PATCH /timetables/{timetableId}
    // -------------------------
    @Transactional
    fun updateTimetable(userId: Long, timetableId: Long, req: UpdateTimetableRequest): TimetableResponse {
        if (req.name.isBlank()) throw DomainException(ErrorCode.TIMETABLE_NAME_CANNOT_BE_BLANK)

        val tt = getOwnedTimetable(userId, timetableId)
        tt.name = req.name.trim()

        val saved = timetableRepository.save(tt)
        return saved.toTimetableResponse()
    }

    // -------------------------
    // DELETE /timetables/{timetableId}
    // -------------------------
    @Transactional
    fun deleteTimetable(userId: Long, timetableId: Long) {
        getOwnedTimetable(userId, timetableId) // 여기서 NOT_FOUND 처리 끝
        timetableRepository.deleteById(timetableId) // 그냥 PK로 삭제
    }

    // ----------------- helpers -----------------

    private fun getOwnedTimetable(userId: Long, timetableId: Long): Timetable =
        timetableRepository.findById(timetableId).orElseThrow {
            DomainException(ErrorCode.TIMETABLE_NOT_FOUND)
        }.also { tt ->
            if (tt.userId != userId) throw DomainException(ErrorCode.TIMETABLE_NOT_FOUND)
        }

    private fun Timetable.toTimetableResponse(): TimetableResponse =
        TimetableResponse(
            id = this.id!!,
            name = this.name,
            year = this.year,
            semester = this.semester
        )
}