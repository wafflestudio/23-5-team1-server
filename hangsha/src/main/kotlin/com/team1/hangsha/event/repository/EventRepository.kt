package com.team1.hangsha.event.repository

import com.team1.hangsha.event.model.Event
import org.springframework.data.repository.CrudRepository

interface EventRepository : CrudRepository<Event, Long> {
    fun findByApplyLink(applyLink: String): Event?
}