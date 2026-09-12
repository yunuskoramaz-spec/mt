package com.q3rvo.erciyesflight

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class PipeSpawnerTest {
    private fun newSpawner(): PipeSpawner = PipeSpawner(
        designWidth = 1080f,
        designHeight = 1920f,
        spacing = 620f,
        pipeWidth = 190f,
        firstX = 1550f,
        spawnTriggerDistance = 480f,
        minGapCenter = 690f,
        maxGapCenter = 1000f,
        initialGapCenter = 835f,
        random = Random(42)
    )

    @Test
    fun spawningContinuesPastTwentyObstacles() {
        val spawner = newSpawner()
        spawner.reset()
        repeat(900) { spawner.update(1f / 60f, 430f) }
        assertTrue("Expected more than 20 spawned pipes", spawner.spawnCount > 20)
        assertTrue(spawner.pipes.isNotEmpty())
    }

    @Test
    fun activeListStaysBoundedWhileWorldContinues() {
        val spawner = newSpawner()
        spawner.reset()
        repeat(3600) { spawner.update(1f / 60f, 430f) }
        assertTrue(spawner.spawnCount > 35)
        assertTrue("Active obstacle list grew without bound", spawner.pipes.size <= 4)
    }

    @Test
    fun spacingRemainsConstantAndFuturePipeExists() {
        val spawner = newSpawner()
        spawner.reset()
        repeat(1800) {
            spawner.update(1f / 60f, 430f)
            assertTrue(spawner.pipes.isNotEmpty())
            assertTrue(spawner.pipes.none { it.x.isNaN() || it.x.isInfinite() })
            for (i in 1 until spawner.pipes.size) {
                assertTrue(abs(spawner.pipes[i].x - spawner.pipes[i - 1].x - 620f) < 0.01f)
            }
            assertTrue(spawner.pipes.maxOf { it.x } > 1080f - 620f - 1f)
        }
    }

    @Test
    fun resetStartsCleanAndIdsRestart() {
        val spawner = newSpawner()
        spawner.reset()
        repeat(300) { spawner.update(1f / 60f, 430f) }
        assertTrue(spawner.spawnCount > 1)
        spawner.reset()
        assertTrue(spawner.pipes.size == 1)
        assertTrue(spawner.pipes.first().id == 1L)
        assertTrue(spawner.pipes.first().x == 1550f)
    }

    @Test
    fun oneUpdateCannotSpawnMoreThanOnePipe() {
        val spawner = newSpawner()
        spawner.reset()
        repeat(100) {
            val before = spawner.spawnCount
            spawner.update(1f / 60f, 430f)
            assertTrue(spawner.spawnCount - before <= 1)
        }
    }
}
