package com.team1.hangsha.memo.controller

import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import com.team1.hangsha.memo.dto.*
import com.team1.hangsha.memo.dto.core.TagResponse
import com.team1.hangsha.memo.service.MemoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class MemoController(
    private val memoService: MemoService
) {

    @PostMapping("/memos")
    fun createMemo(
        @LoggedInUser user: User,
        @RequestBody req: CreateMemoRequest
    ): ResponseEntity<CreateMemoResponse> {
        val response = memoService.createMemo(user.id!!, req)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/memos")
    fun getMyMemos(@LoggedInUser user: User): ResponseEntity<ListMemoResponse> {
        val memos = memoService.getMyMemos(user.id!!)
        return ResponseEntity.ok(ListMemoResponse(memos))
    }

}