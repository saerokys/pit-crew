package ai.pleos.playground.sample.vehicle.record

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** SC-270 [기록 저장]의 진행 상태. 누르기 전에는 IDLE이고, 화면을 떠나면 다시 IDLE로 돌아간다. */
enum class SaveState { IDLE, SAVING, SAVED, FAILED }

/**
 * 저장한 사고 기록 1건을 앱 내부 저장소에 JSON 파일 하나로 둔다.
 * 위치는 `noBackupFilesDir`이라 백업·기기 이전 대상이 아니다. 새로 저장하면 이전 기록을 덮어쓴다(최근 1건).
 * 모든 호출은 예외를 잡고 결과(true/false)만 돌려준다. 기록 내용은 로그에 남기지 않는다.
 */
class RecordStore(context: Context) {
    private val dir: File = context.applicationContext.noBackupFilesDir
    private val file = File(dir, FILE_NAME)
    private val tempFile = File(dir, "$FILE_NAME.tmp")

    /** 저장된 기록이 있는지. */
    fun exists(): Boolean = try {
        file.isFile
    } catch (e: Exception) {
        false
    }

    /**
     * [record]를 저장한다. 임시 파일에 다 쓰고 디스크에 반영한 뒤 이름을 바꾸므로,
     * 쓰다가 실패해도 이전 기록은 그대로 남는다. 실패하면 false.
     */
    fun save(record: Map<String, Any?>): Boolean {
        return try {
            val bytes = toJson(record).toString().toByteArray(StandardCharsets.UTF_8)
            FileOutputStream(tempFile).use { out ->
                out.write(bytes)
                out.fd.sync()
            }
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: Exception) {
            try { tempFile.delete() } catch (_: Exception) { }
            false
        }
    }

    /** 저장된 기록을 지운다(디버그 [개발자 도구]의 "저장 기록 삭제"). 기록이 없어도 true. */
    fun delete(): Boolean = try {
        (!file.exists() || file.delete()).also { try { tempFile.delete() } catch (_: Exception) { } }
    } catch (e: Exception) {
        false
    }

    private fun toJson(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().also { json -> value.forEach { (k, v) -> json.put(k.toString(), toJson(v)) } }
        is List<*> -> JSONArray().also { array -> value.forEach { array.put(toJson(it)) } }
        else -> value // 문자열, 숫자, 불리언
    }

    private companion object {
        const val FILE_NAME = "accident_record.json"
    }
}
