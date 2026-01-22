package com.team1.hangsha.tag.controller

import com.team1.hangsha.tag.dto.*
import com.team1.hangsha.tag.service.TagService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tags")
class TagController(
    private val tagService: TagService
) {

    // GET /api/v1/tags
    @GetMapping
    fun getAllTags(): ResponseEntity<ListTagResponse> {
        val tags = tagService.getAllTags()
        return ResponseEntity.ok(ListTagResponse(tags))
    }

    // POST /api/v1/tags
    @PostMapping
    fun createTag(@RequestBody req: CreateTagRequest): ResponseEntity<CreateTagResponse> {
        val response = tagService.createTag(req)
        return ResponseEntity.ok(response)
    }

    // PATCH /api/v1/tags/{tagId}
    @PatchMapping("/{tagId}")
    fun updateTag(
        @PathVariable tagId: Long,
        @RequestBody req: UpdateTagRequest
    ): ResponseEntity<UpdateTagResponse> {
        val response = tagService.updateTag(tagId, req)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{tagId}")
    fun deleteTag(@PathVariable tagId: Long): ResponseEntity<Unit> {
        tagService.deleteTag(tagId)
        return ResponseEntity.noContent().build() // 204 No Content 반환
    }
}