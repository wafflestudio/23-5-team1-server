package org.example

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.crawler.ExtraSnuCrawler
import java.io.File

fun main(args: Array<String>) {
    val opt = Args.parse(args)

    val applyChkCodes = when (opt.crawlMode.lowercase()) {
        "init" -> listOf("0001", "0002", "0003", "0004") // init: 전체
        else -> listOf("0001", "0002", "0004")           // sync: 마감(0003) 제외
    }

    ExtraSnuCrawler(
        delayMsBetweenPages = opt.delayMs,
        delayMsBetweenDetails = opt.detailDelayMs,
        applyChkCodes = applyChkCodes
    ).use { crawler ->

        val baseEvents = when {
            opt.fromFile != null -> {
                val html = File(opt.fromFile).readText(Charsets.UTF_8)
                crawler.parseListHtml(html)
            }
            else -> crawler.crawlAll(startPage = opt.startPage, maxPages = opt.maxPages)
        }

        val events = if (!opt.withDetails) baseEvents else crawler.enrichDetails(baseEvents) { e ->
            if (opt.crawlMode.lowercase() == "init") e.status != "마감" else true
        }

        val json = Json {
            prettyPrint = true
            encodeDefaults = true
            explicitNulls = false
        }.encodeToString(events)

        if (opt.out == null) {
            println(json)
        } else {
            File(opt.out).writeText(json, Charsets.UTF_8)
            println("Saved ${events.size} events to ${opt.out}")
        }
    }
}

private fun env(key: String): String =
    System.getenv(key) ?: error("Missing env: $key")

private data class Args(
    val startPage: Int = 1,
    val maxPages: Int = 500,
    val delayMs: Long = 200,

    val withDetails: Boolean = true,
    val detailDelayMs: Long = 100,

    val crawlMode: String = "sync",   // sync | init
    val out: String? = "events.json", // null이면 stdout
    val fromFile: String? = null      // list html 파일로 오프라인 테스트
) {
    companion object {
        fun parse(raw: Array<String>): Args {
            var startPage = 1
            var maxPages = 500
            var delayMs = 200L

            var withDetails = true
            var detailDelayMs = 100L

            var crawlMode = "sync"
            var out: String? = "events.json"
            var fromFile: String? = null

            for (a in raw) {
                when {
                    a.startsWith("--startPage=") -> startPage = a.substringAfter("=").toInt()
                    a.startsWith("--maxPages=") -> maxPages = a.substringAfter("=").toInt()
                    a.startsWith("--delayMs=") -> delayMs = a.substringAfter("=").toLong()

                    a == "--withDetails" -> withDetails = true
                    a == "--noDetails" -> withDetails = false
                    a.startsWith("--detailDelayMs=") -> detailDelayMs = a.substringAfter("=").toLong()

                    a == "--init" -> crawlMode = "init"
                    a == "--sync" -> crawlMode = "sync"
                    a.startsWith("--crawlMode=") -> crawlMode = a.substringAfter("=")

                    a.startsWith("--out=") -> out = a.substringAfter("=").ifBlank { null }
                    a == "--stdout" -> out = null

                    a.startsWith("--fromFile=") -> fromFile = a.substringAfter("=").ifBlank { null }
                }
            }
            return Args(startPage, maxPages, delayMs, withDetails, detailDelayMs, crawlMode, out, fromFile)
        }
    }
}
