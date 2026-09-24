package com.example.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.audio.AudioFrame
import com.example.model.ParticlePreset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var size: Float = 4f,
    var alpha: Float = 1f,
    var life: Float = 1f,
    var maxLife: Float = 1f,
    var color: Color = Color.White
)

class ParticleSimulation(val maxParticles: Int = 100) {

    private val particles = ArrayList<Particle>(maxParticles)
    private val random = Random(42)

    init {
        for (i in 0 until maxParticles) {
            particles.add(Particle(life = 0f))
        }
    }

    fun update(
        width: Float,
        height: Float,
        preset: ParticlePreset,
        baseColor: Color,
        audioFrame: AudioFrame
    ) {
        if (width <= 0 || height <= 0) return

        val speedMultiplier = 1.0f + audioFrame.treble * 2.0f
        val sizeMultiplier = 1.0f + audioFrame.bass * 1.5f

        // Beat burst spawn
        val spawnCount = if (audioFrame.isBeat) 8 else 2

        var spawned = 0
        for (p in particles) {
            if (p.life <= 0f && spawned < spawnCount) {
                spawnParticle(p, width, height, preset, baseColor, sizeMultiplier)
                spawned++
            }

            if (p.life > 0f) {
                p.x += p.vx * speedMultiplier
                p.y += p.vy * speedMultiplier

                // Preset dynamics
                when (preset) {
                    ParticlePreset.FIRE -> {
                        p.vy -= 0.15f // rise up
                        p.vx += (random.nextFloat() - 0.5f) * 0.3f
                    }
                    ParticlePreset.SNOW -> {
                        p.vy += 0.05f // drift down
                        p.vx = sin(p.y * 0.02f) * 0.8f
                    }
                    ParticlePreset.ENERGY_BURST -> {
                        p.vx *= 0.95f
                        p.vy *= 0.95f
                    }
                    ParticlePreset.STARS -> {
                        // slow radial drift
                    }
                    ParticlePreset.DUST -> {
                        p.vx = (random.nextFloat() - 0.5f) * 0.4f
                        p.vy = (random.nextFloat() - 0.5f) * 0.4f
                    }
                    ParticlePreset.SMOKE -> {
                        p.vy -= 0.08f
                        p.size += 0.2f
                    }
                }

                p.life -= 0.016f
                p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            }
        }
    }

    private fun spawnParticle(
        p: Particle,
        width: Float,
        height: Float,
        preset: ParticlePreset,
        baseColor: Color,
        sizeMultiplier: Float
    ) {
        p.maxLife = random.nextFloat() * 1.5f + 0.8f
        p.life = p.maxLife
        p.size = (random.nextFloat() * 8f + 3f) * sizeMultiplier

        when (preset) {
            ParticlePreset.FIRE -> {
                p.x = width * 0.5f + (random.nextFloat() - 0.5f) * width * 0.6f
                p.y = height * 0.85f + random.nextFloat() * 30f
                p.vx = (random.nextFloat() - 0.5f) * 2f
                p.vy = -(random.nextFloat() * 4f + 3f)
                p.color = Color(0xFFFF5722)
            }
            ParticlePreset.SNOW -> {
                p.x = random.nextFloat() * width
                p.y = -10f
                p.vx = (random.nextFloat() - 0.5f) * 1.5f
                p.vy = random.nextFloat() * 2f + 1f
                p.color = Color.White
            }
            ParticlePreset.ENERGY_BURST -> {
                p.x = width * 0.5f
                p.y = height * 0.5f
                val angle = random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = random.nextFloat() * 7f + 3f
                p.vx = cos(angle) * speed
                p.vy = sin(angle) * speed
                p.color = baseColor
            }
            ParticlePreset.STARS -> {
                p.x = random.nextFloat() * width
                p.y = random.nextFloat() * height
                p.vx = (random.nextFloat() - 0.5f) * 0.5f
                p.vy = (random.nextFloat() - 0.5f) * 0.5f
                p.color = Color(0xFF8BE9FD)
            }
            ParticlePreset.DUST -> {
                p.x = random.nextFloat() * width
                p.y = random.nextFloat() * height
                p.vx = (random.nextFloat() - 0.5f) * 0.8f
                p.vy = (random.nextFloat() - 0.5f) * 0.8f
                p.color = baseColor.copy(alpha = 0.6f)
            }
            ParticlePreset.SMOKE -> {
                p.x = width * 0.5f + (random.nextFloat() - 0.5f) * width * 0.2f
                p.y = height * 0.7f
                p.vx = (random.nextFloat() - 0.5f) * 1f
                p.vy = -(random.nextFloat() * 1.5f + 1f)
                p.color = Color(0xFF9E9E9E)
            }
        }
    }

    fun render(drawScope: DrawScope) {
        for (p in particles) {
            if (p.life > 0f) {
                drawScope.drawCircle(
                    color = p.color.copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}
