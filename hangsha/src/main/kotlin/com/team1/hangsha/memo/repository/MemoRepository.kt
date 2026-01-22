package com.team1.hangsha.memo.repository

import com.team1.hangsha.memo.model.Memo
import org.springframework.data.repository.CrudRepository

interface MemoRepository : CrudRepository<Memo, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Memo>
}