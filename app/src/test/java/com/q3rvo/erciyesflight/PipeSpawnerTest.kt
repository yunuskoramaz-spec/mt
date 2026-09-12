package com.q3rvo.erciyesflight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PipeSpawnerTest {
    private fun spawner() = PipeSpawner(
        spacing = 620f,
        pipeWidth = 190f,
        firstX = 1550f,
        spawnTriggerDistance = 480f,
        initialGapCenter = 835f,
        minGapCenter = 690f,
        maxGapCenter = 1000f,
        random = kotlin.random.Random(42)
    )

    @Test
    fun spawnsBeyondEightObstacles() {
        val s = spawner()
        s.reset()
        repeat(5000) { s.update(1f / 60f, 430f) }
        assertTrue("More than 15 obstacles must be generated", s.spawnCount > 15)
        assertTrue("Simulation should reach 30+ generated obstacles", s.spawnCount > 30)
        assertTrue(s.pipes.isNotEmpty())
        assertTrue("Active list must stay bounded", s.pipes.size <= 3)
        assertTrue(s.pipes.zipWithNext().all { (a, b) -> kotlin.math.abs((b.x - a.x) - 620f) < 0.01f })
        assertTrue(s.pipes.none { it.x.isNaN() || it.x.isInfinite() })
    }

    @Test
    fun resetStartsClean() {
        val s = spawner()
        s.reset()
        repeat(1000) { s.update(1f / 60f, 430f) }
        s.reset()
        assertEquals(1, s.pipes.size)
        assertEquals(1550f, s.pipes[0].x, 0.01f)
        assertEquals(1L, s.pipes[0].id)
    }

    @Test
    fun onePhysicsUpdateSpawnsAtMostOnePipe() {
        val s = spawner()
        s.reset()
        repeat(200) { s.update(1f / 60f, 430f) }
        val before = s.spawnCount
        s.update(0.033f, 430f)
        assertTrue(s.spawnCount - before <= 1)
    }
}
