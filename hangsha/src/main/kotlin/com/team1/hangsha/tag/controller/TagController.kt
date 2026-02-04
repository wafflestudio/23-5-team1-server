package com.team1.hangsha.tag.controller

import com.team1.hangsha.tag.dto.*
import com.team1.hangsha.tag.service.TagService
import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tags")
class TagController(
    private val tagService: TagService
) {

    @GetMapping
    fun getAllTags(@LoggedInUser user: User): ResponseEntity<ListTagResponse> {
        val tags = tagService.getAllTags(user.id!!)
        return ResponseEntity.ok(ListTagResponse(items = tags))
    }

    @PostMapping
    fun createTag(
        @LoggedInUser user: User,
        @RequestBody req: CreateTagRequest
    ): ResponseEntity<CreateTagResponse> {
        val response = tagService.createTag(user.id!!, req)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{tagId}")
    fun updateTag(
        @LoggedInUser user: User,
        @PathVariable tagId: Long,
        @RequestBody req: UpdateTagRequest
    ): ResponseEntity<UpdateTagResponse> {
        val response = tagService.updateTag(user.id!!, tagId, req)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{tagId}")
    fun deleteTag(
        @LoggedInUser user: User,
        @PathVariable tagId: Long
    ): ResponseEntity<Unit> {
        tagService.deleteTag(user.id!!, tagId)
        return ResponseEntity.noContent().build()
    }
}