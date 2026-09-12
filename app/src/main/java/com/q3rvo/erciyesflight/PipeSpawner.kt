package com.q3rvo.erciyesflight

import kotlin.math.abs
import kotlin.random.Random

/** Owns obstacle lifetime. Rendering and input never mutate this list. */
class PipeSpawner(
    private val designWidth: Float,
    private val designHeight: Float,
    private val spacing: Float,
    private val pipeWidth: Float,
    private val firstX: Float,
    private val spawnTriggerDistance: Float,
    private val minGapCenter: Float,
    private val maxGapCenter: Float,
    private val initialGapCenter: Float,
    private val random: Random = Random.Default
) {
    data class Pipe(
        var x: Float,
        var gapCenter: Float,
        var passed: Boolean = false,
        val id: Long
    )

    val pipes = mutableListOf<Pipe>()
    var spawnCount: Int = 0
        private set
    var lastSpawnTimeSeconds: Float = 0f
        private set
    var lastSpawnX: Float = Float.NaN
        private set

    private var nextId = 1L
    private var elapsedSeconds = 0f

    fun reset() {
        pipes.clear()
        spawnCount = 0
        nextId = 1L
        elapsedSeconds = 0f
        lastSpawnTimeSeconds = 0f
        lastSpawnX = Float.NaN
        addPipe(firstX, initialGapCenter)
        enforceInvariants()
    }

    fun clear() {
        pipes.clear()
        spawnCount = 0
        nextId = 1L
        elapsedSeconds = 0f
        lastSpawnTimeSeconds = 0f
        lastSpawnX = Float.NaN
    }

    fun update(dt: Float, speed: Float) {
        if (pipes.isEmpty()) addPipe(designWidth + spacing, initialGapCenter)
        elapsedSeconds += dt

        for (pipe in pipes) pipe.x -= speed * dt
        pipes.removeAll { it.x + pipeWidth < 0f }

        // At most one spawn per physics update. Spawning is independent of score,
        // touch count and rendering, so the obstacle stream cannot silently stop.
        val lastPipe = pipes.maxByOrNull { it.x }
        if (lastPipe == null || lastPipe.x < designWidth - spawnTriggerDistance) {
            val nextX = if (lastPipe == null) designWidth + spacing else lastPipe.x + spacing
            addPipe(nextX, nextGapCenter())
        }
        enforceInvariants()
    }

    fun markPassed(pipe: Pipe): Boolean {
        if (pipe.passed) return false
        pipe.passed = true
        return true
    }

    fun ensureFuturePipe() {
        if (pipes.isEmpty()) addPipe(designWidth + spacing, initialGapCenter)
        enforceInvariants()
    }

    private fun nextGapCenter(): Float {
        val difficulty = (spawnCount / 12f).coerceIn(0f, 1f)
        val safeMin = minGapCenter + 20f * difficulty
        val safeMax = maxGapCenter - 20f * difficulty
        return random.nextFloat() * (safeMax - safeMin) + safeMin
    }

    private fun addPipe(x: Float, gapCenter: Float) {
        pipes += Pipe(x, gapCenter, false, nextId++)
        spawnCount++
        lastSpawnTimeSeconds = elapsedSeconds
        lastSpawnX = x
    }

    private fun enforceInvariants() {
        require(pipes.isNotEmpty())
        require(pipes.none { it.x.isNaN() || it.x.isInfinite() })
        val ordered = pipes.sortedBy { it.x }
        for (i in 1 until ordered.size) {
            val distance = ordered[i].x - ordered[i - 1].x
            require(abs(distance - spacing) <= 0.01f) { "Pipe spacing violated: $distance" }
        }
        require(ordered.last().x > designWidth - spacing - 1f)
    }
}
