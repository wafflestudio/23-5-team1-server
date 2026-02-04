package com.team1.hangsha.memo.controller

import com.team1.hangsha.memo.dto.*
import com.team1.hangsha.memo.dto.core.MemoResponse
import com.team1.hangsha.memo.service.MemoService
import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/memos")
class MemoController(
    private val memoService: MemoService
) {

    @PostMapping
    fun createMemo(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestBody req: CreateMemoRequest
    ): ResponseEntity<MemoResponse> {
        val response = memoService.createMemo(user.id!!, req)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getMyMemos(@Parameter(hidden = true) @LoggedInUser user: User,): ResponseEntity<ListMemoResponse> {
        val memos = memoService.getMyMemos(user.id!!)
        return ResponseEntity.ok(ListMemoResponse(memos))
    }

    @GetMapping("/by-tag/{tagId}")
    fun findMemosByTagId(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable tagId: Long,
    ): ResponseEntity<ListMemoResponse> {
        val response = memoService.findMemosByTagId(user.id!!,tagId)
        return ResponseEntity.ok(ListMemoResponse(response))
    }

    // 메모 수정 (내용 + 태그)
    @PatchMapping("/{memoId}")
    fun updateMemo(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable memoId: Long,
        @RequestBody body: JsonNode,
    ): ResponseEntity<MemoResponse> {
        val response = memoService.updateMemo(user.id!!, memoId, body)
        return ResponseEntity.ok(response)
    }

    // 메모 삭제
    @DeleteMapping("/{memoId}")
    fun deleteMemo(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable memoId: Long
    ): ResponseEntity<Unit> {
        memoService.deleteMemo(user.id!!, memoId)
        return ResponseEntity.noContent().build()
    }
}