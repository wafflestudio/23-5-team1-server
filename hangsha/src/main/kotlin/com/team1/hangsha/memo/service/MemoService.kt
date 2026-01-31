package com.team1.hangsha.memo.service

import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.event.repository.EventRepository
import com.team1.hangsha.memo.dto.*
import com.team1.hangsha.memo.dto.core.MemoResponse
import com.team1.hangsha.memo.dto.core.MemoTagResponse
import com.team1.hangsha.memo.model.Memo
import com.team1.hangsha.memo.model.MemoTagRef
import com.team1.hangsha.memo.repository.MemoRepository
import com.team1.hangsha.tag.model.Tag
import com.team1.hangsha.tag.repository.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemoService(
    private val memoRepository: MemoRepository,
    private val tagRepository: TagRepository,
    private val eventRepository: EventRepository
) {

    @Transactional
    fun createMemo(userId: Long, req: CreateMemoRequest): MemoResponse {
        val event = eventRepository.findById(req.eventId)
            .orElseThrow { DomainException(ErrorCode.EVENT_NOT_FOUND) }

        val tagRefs = resolveTags(userId, req.tagNames)

        val memo = memoRepository.save(
            Memo(
                userId = userId,
                eventId = req.eventId,
                content = req.content,
                tags = tagRefs
            )
        )

        return mapToMemoResponse(memo, event.title)
    }

    @Transactional
    fun updateMemo(userId: Long, memoId: Long, req: UpdateMemoRequest): MemoResponse {
        val memo = memoRepository.findById(memoId)
            .orElseThrow { DomainException(ErrorCode.MEMO_NOT_FOUND) }

        if (memo.userId != userId) {
            throw DomainException(ErrorCode.MEMO_NOT_FOUND)
        }

        val newTagRefs = resolveTags(userId, req.tagNames)

        val updatedMemo = memoRepository.save(
            memo.copy(
                content = req.content,
                tags = newTagRefs
            )
        )

        val eventTitle = eventRepository.findById(updatedMemo.eventId)
            .map { it.title }
            .orElse("Unknown Event")

        return mapToMemoResponse(updatedMemo, eventTitle)
    }

    @Transactional
    fun deleteMemo(userId: Long, memoId: Long) {
        val memo = memoRepository.findById(memoId)
            .orElseThrow { DomainException(ErrorCode.MEMO_NOT_FOUND) }

        if (memo.userId != userId) {
            throw DomainException(ErrorCode.MEMO_NOT_FOUND)
        }

        memoRepository.delete(memo)
    }

    @Transactional(readOnly = true)
    fun getMyMemos(userId: Long): List<MemoResponse> {
        val memos = memoRepository.findAllByUserIdOrderByCreatedAtDesc(userId)

        return memos.map { memo ->
            val eventTitle = eventRepository.findById(memo.eventId)
                .map { it.title }
                .orElse("Unknown Event")
            mapToMemoResponse(memo, eventTitle)
        }
    }

    // 요청하신 특정 메모 태그 조회 기능
    @Transactional(readOnly = true)
    fun getTagsByMemoId(userId: Long, memoId: Long): List<MemoTagResponse> {
        val memo = memoRepository.findById(memoId)
            .orElseThrow { DomainException(ErrorCode.MEMO_NOT_FOUND) }

        if(memo.userId != userId) throw DomainException(ErrorCode.MEMO_NOT_FOUND)

        // import com.team1.hangsha.memo.dto.core.MemoTagResponse 가 없으면 여기서 에러가 납니다.
        return memo.tags.mapNotNull { ref ->
            tagRepository.findById(ref.tagId)
                .map { MemoTagResponse(it.id!!, it.name) }
                .orElse(null)
        }
    }

    // --- Helper Methods ---

    private fun resolveTags(userId: Long, tagNames: List<String>): Set<MemoTagRef> {
        return tagNames.distinct().map { name ->
            val tag = tagRepository.findByUserIdAndName(userId, name)
                .orElseGet {
                    tagRepository.save(Tag(userId = userId, name = name))
                }
            MemoTagRef(tagId = tag.id!!)
        }.toSet()
    }

    private fun mapToMemoResponse(memo: Memo, eventTitle: String): MemoResponse {
        val tagNames = memo.tags.mapNotNull { ref ->
            tagRepository.findById(ref.tagId)
                .map { it.name }
                .orElse(null)
        }

        return MemoResponse(
            id = memo.id!!,
            eventId = memo.eventId,
            eventTitle = eventTitle,
            content = memo.content,
            tags = tagNames,
            createdAt = memo.createdAt,
            updatedAt = memo.updatedAt
        )
    }
}