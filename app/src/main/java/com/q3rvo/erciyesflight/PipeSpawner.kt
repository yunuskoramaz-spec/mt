package com.q3rvo.erciyesflight

import kotlin.random.Random

/** Pure pipe lifecycle. Rendering never creates or removes pipes. */
class PipeSpawner(
    private val spacing: Float,
    private val pipeWidth: Float,
    private val firstX: Float,
    private val spawnTriggerDistance: Float,
    private val initialGapCenter: Float,
    private val minGapCenter: Float,
    private val maxGapCenter: Float,
    private val random: Random = Random.Default
) {
    data class Pipe(var x: Float, var gapCenter: Float, var passed: Boolean = false, val id: Long)

    val pipes = mutableListOf<Pipe>()
    var spawnCount: Int = 0
        private set

    private var nextId = 1L

    fun reset() {
        pipes.clear()
        spawnCount = 0
        nextId = 1L
        addPipe(firstX, initialGapCenter)
    }

    fun clear() {
        pipes.clear()
        spawnCount = 0
    }

    fun update(dt: Float, speed: Float) {
        for (pipe in pipes) pipe.x -= speed * dt

        var index = pipes.size - 1
        while (index >= 0) {
            if (pipes[index].x + pipeWidth < 0f) pipes.removeAt(index)
            index--
        }

        // At most one spawn per physics update. The next obstacle is always
        // placed at a fixed distance from the rightmost active obstacle.
        val lastPipe = pipes.maxByOrNull { it.x }
        if (lastPipe == null || lastPipe.x < 1080f - spawnTriggerDistance) {
            val nextX = if (lastPipe == null) 1080f + spacing else lastPipe.x + spacing
            val gapCenter = if (spawnCount < 3) {
                initialGapCenter
            } else {
                random.nextFloat() * (maxGapCenter - minGapCenter) + minGapCenter
            }
            addPipe(nextX, gapCenter)
        }
        enforceInvariants()
    }

    fun ensureFuturePipe() {
        if (pipes.isEmpty()) addPipe(1080f + spacing, initialGapCenter)
        enforceInvariants()
    }

    private fun addPipe(x: Float, gapCenter: Float) {
        pipes.add(Pipe(x, gapCenter, false, nextId++))
        spawnCount++
    }

    private fun enforceInvariants() {
        require(pipes.isNotEmpty())
        require(pipes.none { it.x.isNaN() || it.x.isInfinite() })
        val sorted = pipes.sortedBy { it.x }
        for (i in 1 until sorted.size) {
            val distance = sorted[i].x - sorted[i - 1].x
            require(kotlin.math.abs(distance - spacing) <= 0.01f) { "Pipe spacing violated: $distance" }
        }
        require(sorted.last().x > 1080f - spacing - 1f)
    }
}
