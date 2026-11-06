package ai.pleos.playground.sample.vehicle.stt

/** 음성 메모 글자 규칙(Android·SDK 의존 없는 순수 함수라 JVM 단위 테스트로 확인한다). */
object VoiceMemoText {
    /** 메모 한 건의 최대 글자 수(사용자 결정). */
    const val MAX_CHARS = 100

    /**
     * 줄바꿈과 연속 공백을 한 칸으로 줄이고 앞뒤를 지운 뒤 최대 [MAX_CHARS]자로 자른다.
     * 자른 끝의 공백은 지운다. 비어 있으면 빈 문자열이다.
     */
    fun clean(raw: String): String {
        val text = normalize(raw)
        return if (text.length > MAX_CHARS) text.take(MAX_CHARS).trimEnd() else text
    }

    /** 공백을 정리한 글자가 [MAX_CHARS]자를 넘어 [clean]이 잘랐는지(잘렸을 때만 확인 화면에 안내를 보여 준다). */
    fun wasCut(raw: String): Boolean = normalize(raw).length > MAX_CHARS

    private fun normalize(raw: String): String = raw.replace(Regex("\\s+"), " ").trim()
}
