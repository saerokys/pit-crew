package ai.pleos.playground.sample.vehicle.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** 위치 읽기 결과 종류. OK가 아니면 화면에는 "확인 불가"로 보인다. */
enum class LocationStatus {
    OK,
    /** 서비스가 위치를 주지 않음(콜백이 null) */
    NO_LOCATION,
    /** 이 차량에 위치 서비스가 없음(isSupported가 false) */
    UNSUPPORTED,
    /** 앱 권한(FUSED_LOCATION)이 허용되지 않음 */
    NO_PERMISSION,
    /** 오류 콜백 또는 예외 */
    ERROR,
    /** 3초 안에 응답이 없음 */
    TIMEOUT
}

/**
 * 읽은 위치. 좌표는 사고 모드 세션 메모리에만 두고 저장·전송·로그·화면 표시를 하지 않는다.
 * 고도, 방향 등 쓰지 않는 값은 가져오지 않는다.
 */
data class SessionLocation(val latitude: Double, val longitude: Double, val timestampMillis: Long) {
    // 좌표가 로그나 문자열로 새지 않게 toString에서 숨긴다.
    override fun toString(): String = "SessionLocation(hidden)"
}

data class LocationResult(val status: LocationStatus, val location: SessionLocation? = null, val errorText: String? = null)

const val FUSED_LOCATION_PERMISSION = "pleos.car.permission.FUSED_LOCATION"

fun hasLocationPermission(context: Context): Boolean = try {
    ContextCompat.checkSelfPermission(context, FUSED_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED
} catch (e: Exception) {
    false
}

/** 이 차량에 위치 서비스가 있는지(서비스를 시작하거나 연결하지 않는 조회). 실패하면 false. */
fun isLocationSupported(context: Context): Boolean = try {
    FusedLocation(context.applicationContext).isSupported()
} catch (e: Exception) {
    false
}

/**
 * FusedLocation으로 마지막 위치를 한 번만 읽는다: isSupported -> initialize -> getLastKnownLocation -> release.
 * 연속 업데이트(register)는 쓰지 않는다. 모든 호출은 예외를 잡고 3초 제한을 둔다.
 * 호출하는 쪽이 사용자의 위치 허용(SC-205)을 먼저 확인해야 한다. 이 클래스는 권한이 없으면 SDK를 부르지 않는다.
 */
class LocationReader(context: Context, private val timeoutMs: Long = 3_000L) {
    private val appContext = context.applicationContext

    suspend fun readOnce(): LocationResult {
        if (!hasLocationPermission(appContext)) return LocationResult(LocationStatus.NO_PERMISSION)
        val fused = try {
            FusedLocation(appContext)
        } catch (e: Exception) {
            return LocationResult(LocationStatus.ERROR, errorText = e.javaClass.simpleName)
        }
        var initialized = false
        try {
            if (!fused.isSupported()) return LocationResult(LocationStatus.UNSUPPORTED)
            fused.initialize()
            initialized = true
            val result = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<LocationResult> { continuation ->
                    val finished = AtomicBoolean(false)
                    fun finish(value: LocationResult) {
                        if (finished.compareAndSet(false, true) && continuation.isActive) continuation.resume(value)
                    }
                    try {
                        fused.getLastKnownLocation(object : FusedLocationListener {
                            override fun onChanged(location: LocationData?) {
                                finish(
                                    if (location == null) LocationResult(LocationStatus.NO_LOCATION)
                                    else LocationResult(
                                        LocationStatus.OK,
                                        SessionLocation(location.latitude, location.longitude, location.timestampMillis)
                                    )
                                )
                            }

                            override fun onError(exception: Exception) {
                                finish(LocationResult(LocationStatus.ERROR, errorText = exception.message))
                            }
                        })
                    } catch (e: SecurityException) {
                        finish(LocationResult(LocationStatus.NO_PERMISSION))
                    } catch (e: Exception) {
                        finish(LocationResult(LocationStatus.ERROR, errorText = e.javaClass.simpleName))
                    }
                }
            }
            return result ?: LocationResult(LocationStatus.TIMEOUT)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return LocationResult(LocationStatus.ERROR, errorText = e.javaClass.simpleName)
        } finally {
            if (initialized) {
                try {
                    fused.release()
                } catch (e: Exception) {
                    // release 실패는 무시한다(다음 읽기는 새 인스턴스를 쓴다)
                }
            }
        }
    }
}
