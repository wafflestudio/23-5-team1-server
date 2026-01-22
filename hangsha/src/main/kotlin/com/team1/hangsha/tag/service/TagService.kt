package com.team1.hangsha.tag.service

import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import com.team1.hangsha.tag.dto.*
import com.team1.hangsha.tag.dto.core.TagDto
import com.team1.hangsha.tag.model.Tag
import com.team1.hangsha.tag.repository.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagService(
    private val tagRepository: TagRepository
) {
    @Transactional
    fun createTag(req: CreateTagRequest): CreateTagResponse {
        if (tagRepository.findByName(req.name).isPresent) {
            throw DomainException(ErrorCode.TAG_ALREADY_EXISTS)
        }

        val tag = tagRepository.save(Tag(name = req.name))
        return CreateTagResponse(tag.id!!, tag.name)
    }

    @Transactional(readOnly = true)
    fun getAllTags(): List<TagDto> {
        return tagRepository.findAll()
            .map { TagDto(it.id!!, it.name) }
    }

    @Transactional
    fun updateTag(tagId: Long, req: UpdateTagRequest): UpdateTagResponse {
        val tag = tagRepository.findById(tagId)
            .orElseThrow { DomainException(ErrorCode.TAG_NOT_FOUND) }

        // 이름 중복 체크
        if (tagRepository.findByName(req.name).isPresent) {
            throw DomainException(ErrorCode.TAG_ALREADY_EXISTS)
        }

        val updatedTag = tagRepository.save(tag.copy(name = req.name))

        return UpdateTagResponse(updatedTag.id!!, updatedTag.name)
    }

    @Transactional
    fun deleteTag(tagId: Long) {
        val tag = tagRepository.findById(tagId)
            .orElseThrow { DomainException(ErrorCode.TAG_NOT_FOUND) }

        tagRepository.delete(tag)
    }
}