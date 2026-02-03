package com.team1.hangsha.memo.controller

import com.team1.hangsha.memo.dto.*
import com.team1.hangsha.memo.dto.core.MemoResponse
import com.team1.hangsha.memo.service.MemoService
import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/memos")
class MemoController(
    private val memoService: MemoService
) {

    @PostMapping
    fun createMemo(
        @LoggedInUser user: User,
        @RequestBody req: CreateMemoRequest
    ): ResponseEntity<MemoResponse> {
        val response = memoService.createMemo(user.id!!, req)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getMyMemos(@LoggedInUser user: User): ResponseEntity<ListMemoResponse> {
        val memos = memoService.getMyMemos(user.id!!)
        return ResponseEntity.ok(ListMemoResponse(memos))
    }

    // 메모 수정 (내용 + 태그)
    @PatchMapping("/{memoId}")
    fun updateMemo(
        @LoggedInUser user: User,
        @PathVariable memoId: Long,
        @RequestBody req: UpdateMemoRequest
    ): ResponseEntity<MemoResponse> {
        val response = memoService.updateMemo(user.id!!, memoId, req)
        return ResponseEntity.ok(response)
    }

    // 메모 삭제
    @DeleteMapping("/{memoId}")
    fun deleteMemo(
        @LoggedInUser user: User,
        @PathVariable memoId: Long
    ): ResponseEntity<Unit> {
        memoService.deleteMemo(user.id!!, memoId)
        return ResponseEntity.noContent().build()
    }
}