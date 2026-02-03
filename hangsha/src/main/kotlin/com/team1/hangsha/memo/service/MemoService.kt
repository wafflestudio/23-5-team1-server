package com.team1.hangsha.memo.service

import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.event.repository.EventRepository
import com.team1.hangsha.memo.dto.CreateMemoRequest
import com.team1.hangsha.memo.dto.CreateMemoResponse
import com.team1.hangsha.memo.dto.core.TagResponse
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
    fun createMemo(userId: Long, req: CreateMemoRequest): CreateMemoResponse {
        val event = eventRepository.findById(req.eventId)
            .orElseThrow { DomainException(ErrorCode.EVENT_NOT_FOUND) }

        val tagRefs = req.tagNames.distinct().map { name ->
            val tag = tagRepository.findByName(name)
                .orElseGet { tagRepository.save(Tag(name = name)) }
            MemoTagRef(tagId = tag.id!!)
        }.toSet()

        val memo = memoRepository.save(
            Memo(
                userId = userId,
                eventId = req.eventId,
                content = req.content,
                tags = tagRefs
            )
        )

        return CreateMemoResponse(
            id = memo.id!!,
            eventId = event.id!!,
            eventTitle = event.title,
            content = memo.content,
            tags = req.tagNames,
            createdAt = memo.createdAt
        )
    }

    @Transactional(readOnly = true)
    fun getMyMemos(userId: Long): List<CreateMemoResponse> {
        val memos = memoRepository.findAllByUserIdOrderByCreatedAtDesc(userId)

        return memos.map { memo ->
            val event = eventRepository.findById(memo.eventId).orElse(null)

            val tagNames = memo.tags.mapNotNull { ref ->
                tagRepository.findById(ref.tagId)
                    .map { it.name }
                    .orElse(null)
            }

            CreateMemoResponse(
                id = memo.id!!,
                eventId = memo.eventId,
                eventTitle = event?.title ?: "Unknown Event",
                content = memo.content,
                tags = tagNames,
                createdAt = memo.createdAt
            )
        }
    }

    @Transactional(readOnly = true)
    fun getAllTags(): List<TagResponse> {
        return tagRepository.findAll().map { TagResponse(it.id!!, it.name) }
    }
}