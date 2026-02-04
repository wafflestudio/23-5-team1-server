package com.team1.hangsha.common.upload

import com.team1.hangsha.common.error.DomainException
import com.team1.hangsha.common.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

@Service
class LocalUploadService(
    private val props: UploadProperties,
) {
    /**
     * 유저 프로필 이미지를 로컬 디스크에 저장하고, 공개 URL을 반환합니다.
     */
    fun uploadProfileImage(userId: Long, file: MultipartFile): String {
        if (file.isEmpty || file.size <= 0) {
            throw DomainException(ErrorCode.UPLOAD_FILE_EMPTY)
        }
        if (file.size > props.maxSizeBytes) {
            throw DomainException(ErrorCode.UPLOAD_FAILED, "파일이 너무 큽니다 (max=${props.maxSizeBytes} bytes)")
        }

        val contentType = file.contentType ?: ""
        if (!contentType.startsWith("image/")) {
            throw DomainException(ErrorCode.UPLOAD_UNSUPPORTED_MEDIA_TYPE, "이미지 파일만 업로드할 수 있습니다")
        }

        val ext = guessExtension(file.originalFilename, contentType)
        val relativeKey = "uploads/users/$userId/${UUID.randomUUID()}.$ext"

        val root: Path = Paths.get(props.dir).toAbsolutePath().normalize()
        val target: Path = root.resolve(relativeKey).normalize()

        // directory traversal 방지
        if (!target.startsWith(root)) {
            throw DomainException(ErrorCode.UPLOAD_FAILED, "Invalid path")
        }

        try {
            Files.createDirectories(target.parent)
            file.inputStream.use { input ->
                Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            throw DomainException(ErrorCode.UPLOAD_FAILED, cause = e)
        }

        val base = props.publicBaseUrl.trimEnd('/')
        return "$base/$relativeKey"
    }

    private fun guessExtension(originalFilename: String?, contentType: String): String {
        val fromName = originalFilename
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }

        if (!fromName.isNullOrBlank()) return fromName

        return when (contentType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "bin"
        }
    }
}
