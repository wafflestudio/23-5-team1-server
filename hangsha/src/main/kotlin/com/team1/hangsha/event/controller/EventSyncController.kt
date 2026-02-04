package com.team1.hangsha.event.controller

import com.team1.hangsha.event.repository.EventRepository
import com.team1.hangsha.event.service.EventSyncService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/events")
class EventSyncController(
    private val eventSyncService: EventSyncService,
    private val eventRepository: EventRepository,
) {
    @PostMapping("/sync")
    fun sync() = eventSyncService.syncFromFile()

    @DeleteMapping("/delete")
    fun deleteAll(): Map<String, Any> {
        val deleted = eventRepository.deleteAllEventsRaw()
        return mapOf("ok" to true, "deleted" to deleted)
    }
}