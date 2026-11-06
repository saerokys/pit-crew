package ai.pleos.playground.sample.vehicle.navi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SC-280 시연용 후보 목록 확인. 이름은 가상이어야 하고 좌표는 서울 강남 일대 안에 있어야 한다. */
class DemoPlacesTest {

    private val all = DestinationCategory.entries.flatMap { DemoPlaces.forCategory(it) }

    @Test
    fun threeCategoriesWithThreeCandidatesEach() {
        assertEquals(3, DestinationCategory.entries.size)
        DestinationCategory.entries.forEach { assertEquals(3, DemoPlaces.forCategory(it).size) }
    }

    @Test
    fun categoryTitlesAreFromTheDesign() {
        assertEquals(listOf("안전한 곳으로 이동", "정비소", "병원"), DestinationCategory.entries.map { it.title })
        assertEquals(listOf("차를 세울 수 있는 곳", "차를 점검하고 고칠 곳", "다친 곳을 확인할 곳"), DestinationCategory.entries.map { it.subtitle })
    }

    @Test
    fun namesAreFictionalExampleNames() {
        val pattern = Regex("예시 (병원|정비소|지점) [1-3]")
        all.forEach { assertTrue(it.name, pattern.matches(it.name)) }
        DemoPlaces.forCategory(DestinationCategory.HOSPITAL).forEachIndexed { i, p -> assertEquals("예시 병원 ${i + 1}", p.name) }
        DemoPlaces.forCategory(DestinationCategory.GARAGE).forEachIndexed { i, p -> assertEquals("예시 정비소 ${i + 1}", p.name) }
        DemoPlaces.forCategory(DestinationCategory.SAFE).forEachIndexed { i, p -> assertEquals("예시 지점 ${i + 1}", p.name) }
    }

    @Test
    fun idsAreUniqueAndCoordinatesAreInGangnamArea() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all.size, all.map { it.latitude to it.longitude }.toSet().size)
        all.forEach {
            assertTrue(it.id, it.latitude in 37.40..37.60)
            assertTrue(it.id, it.longitude in 126.95..127.15)
        }
    }

    @Test
    fun findReturnsOnlyThatCategorysCandidate() {
        val hospital = DemoPlaces.forCategory(DestinationCategory.HOSPITAL).first()
        assertNotNull(DemoPlaces.find(DestinationCategory.HOSPITAL, hospital.id))
        assertNull(DemoPlaces.find(DestinationCategory.GARAGE, hospital.id))
        assertNull(DemoPlaces.find(DestinationCategory.HOSPITAL, null))
        assertNull(DemoPlaces.find(DestinationCategory.HOSPITAL, "없는 id"))
    }
}
