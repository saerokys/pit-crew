package ai.pleos.playground.sample.vehicle.vehicle

import ai.pleos.playground.vehicle.Vehicle
import android.content.Context
import android.util.Log

/**
 * Vehicle SDK 연결 한 개(초기화와 해제). 값 읽기는 [CapabilityProbe]와 [VehicleReader]가 이 객체의 [vehicle]로 한다.
 * 초기화가 실패하면 예외를 잡고 false를 돌려준다(앱은 멈추지 않고 차량 연동만 끈다).
 */
class VehicleClient(context: Context) : VehicleSession {

    override val vehicle = Vehicle(context.applicationContext)
    private var isInitialized = false

    fun initialize(): Boolean {
        if (isInitialized) return true

        return try {
            vehicle.initialize()
            isInitialized = true
            true
        } catch (exception: Exception) {
            Log.w(TAG, "vehicle initialize failed: ${exception.javaClass.simpleName}")
            false
        }
    }

    fun release() {
        if (!isInitialized) return

        try {
            vehicle.release()
            isInitialized = false
        } catch (exception: Exception) {
            Log.w(TAG, "vehicle release failed: ${exception.javaClass.simpleName}")
        }
    }

    override fun requireInitialized() {
        check(isInitialized) { "VehicleClient must be initialized before use." }
    }

    private companion object {
        const val TAG = "PitCrewVehicle"
    }
}
