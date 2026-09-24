package com.example

import com.example.audio.AudioFrame
import com.example.audio.FftProcessor
import com.example.model.ExpressionEvaluator
import com.example.model.PresetTemplates
import com.example.storage.HistoryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerUnitTest {

    @Test
    fun testFftProcessorAudioFrame() {
        val fft = FftProcessor(binCount = 64)
        val pcm = FloatArray(256) { (it % 100) / 100f }
        val frame = fft.processSamples(pcm, timeMs = 1500L, bpm = 128)

        assertNotNull(frame)
        assertEquals(1500L, frame.timeMs)
        assertEquals(128, frame.bpm)
        assertEquals(64, frame.spectrum.size)
        assertTrue(frame.bass in 0.0f..1.0f)
        assertTrue(frame.mid in 0.0f..1.0f)
        assertTrue(frame.treble in 0.0f..1.0f)
        assertTrue(frame.amplitude in 0.0f..1.0f)
    }

    @Test
    fun testExpressionEvaluator() {
        val frame = AudioFrame(
            bass = 0.8f,
            mid = 0.5f,
            treble = 0.2f,
            amplitude = 0.6f,
            isBeat = true
        )

        val bassResult = ExpressionEvaluator.evaluate("bass * 2.0", frame, 0f)
        assertEquals(1.6f, bassResult, 0.01f)

        val beatResult = ExpressionEvaluator.evaluate("beat", frame, 0f)
        assertEquals(1.0f, beatResult, 0.01f)
    }

    @Test
    fun testPresetTemplatesIntegrity() {
        val templates = PresetTemplates.getAllTemplates()
        assertEquals(12, templates.size)

        templates.forEach { tmpl ->
            assertFalse(tmpl.name.isBlank())
            assertTrue(tmpl.scenes.isNotEmpty())
            assertTrue(tmpl.activeScene.layers.isNotEmpty())
        }
    }

    @Test
    fun testHistoryManagerUndoRedo() {
        val history = HistoryManager(maxSteps = 5)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        val initial = PresetTemplates.createNeonSpectrumTemplate()
        val modified1 = initial.copy(name = "Step 1")
        val modified2 = initial.copy(name = "Step 2")

        history.pushState(initial)
        assertTrue(history.canUndo)

        val reverted = history.undo(modified1)
        assertEquals(initial.name, reverted?.name)
        assertTrue(history.canRedo)

        val redone = history.redo(initial)
        assertEquals(modified1.name, redone?.name)
    }

    @Test
    fun testResolutionPresets() {
        val fhd = com.example.model.ResolutionPreset.FULL_HD_1080P
        assertEquals(1920, fhd.width)
        assertEquals(1080, fhd.height)

        val portrait = com.example.model.ResolutionPreset.PORTRAIT_1080P
        assertEquals(1080, portrait.width)
        assertEquals(1920, portrait.height)

        val square = com.example.model.ResolutionPreset.SQUARE_1080P
        assertEquals(1080, square.width)
        assertEquals(1080, square.height)
    }

    @Test
    fun testNewProjectDefaults() {
        val proj = com.example.model.VisualizerProject(
            name = "My Test Project",
            resolution = com.example.model.ResolutionPreset.PORTRAIT_1080P,
            targetFps = 60
        )
        assertEquals("My Test Project", proj.name)
        assertEquals(60, proj.targetFps)
        assertEquals(com.example.model.ResolutionPreset.PORTRAIT_1080P, proj.resolution)
        assertTrue(proj.scenes.isNotEmpty())
    }
}
