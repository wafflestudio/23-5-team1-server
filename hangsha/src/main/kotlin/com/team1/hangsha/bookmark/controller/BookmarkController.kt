package com.team1.hangsha.bookmark.controller

import com.team1.hangsha.bookmark.service.BookmarkService
import com.team1.hangsha.event.dto.response.BookmarkedEventResponse
import com.team1.hangsha.user.LoggedInUser
import com.team1.hangsha.user.model.User
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class BookmarkController(
    private val bookmarkService: BookmarkService,
) {

    @PostMapping("/events/{eventId}/bookmark")
    fun addBookmark(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable eventId: Long,
    ): ResponseEntity<Void> {
        bookmarkService.addBookmark(requireNotNull(user.id), eventId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/events/{eventId}/bookmark")
    fun removeBookmark(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable eventId: Long,
    ): ResponseEntity<Void> {
        bookmarkService.removeBookmark(requireNotNull(user.id), eventId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/users/me/bookmarks")
    fun listMyBookmarks(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestParam("page", defaultValue = "1") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int,
    ): ResponseEntity<BookmarkedEventResponse> {
        val res = bookmarkService.listMyBookmarks(requireNotNull(user.id), page, size)
        return ResponseEntity.ok(res)
    }
}
