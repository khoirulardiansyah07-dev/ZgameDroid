package com.example.model

import java.util.UUID

enum class LayerType(val displayName: String) {
    SPECTRUM("Spectrum Analyzer"),
    CIRCULAR_SPECTRUM("Circular Spectrum"),
    WAVEFORM("Waveform Oscilloscope"),
    PARTICLES("Particle System"),
    MESH_3D("3D Object"),
    TEXT_REACTIVE("Audio-Reactive Text"),
    SHAPE("Pulsing Shape"),
    IMAGE("Image / Logo"),
    SOLID_BACKGROUND("Solid Background"),
    GRADIENT_BACKGROUND("Gradient Background")
}

enum class ShapeType(val displayName: String) {
    CIRCLE("Circle"),
    RING("Neon Ring"),
    HEXAGON("Hexagon"),
    STAR("Star"),
    RECTANGLE("Rounded Box")
}

enum class Mesh3DType(val displayName: String) {
    CUBE("Wireframe Cube"),
    SPHERE("Wireframe Sphere"),
    TORUS("Donut Torus"),
    TUNNEL("Cyber Tunnel"),
    TERRAIN("Audio Grid Terrain")
}

enum class ParticlePreset(val displayName: String) {
    ENERGY_BURST("Energy Sparks"),
    STARS("Cosmic Stars"),
    FIRE("Rising Fire"),
    SMOKE("Mystic Smoke"),
    SNOW("Falling Snow"),
    DUST("Floating Dust")
}

enum class AudioSource(val displayName: String) {
    BASS("Bass (20-250Hz)"),
    MID("Mid (250-4000Hz)"),
    TREBLE("Treble (4-20kHz)"),
    AMPLITUDE("Overall Amplitude"),
    BEAT("Beat Pulse")
}

enum class AudioTarget(val displayName: String) {
    SCALE("Scale / Size"),
    GLOW("Glow Intensity"),
    ROTATION("Rotation Angle"),
    OPACITY("Opacity"),
    POSITION_Y("Vertical Bounce"),
    PARTICLE_BURST("Particle Spawn Rate"),
    MESH_DEFORM("3D Vertex Deformation")
}

enum class BlendModeType(val displayName: String) {
    NORMAL("Normal"),
    ADD("Add / Screen Glow"),
    MULTIPLY("Multiply"),
    OVERLAY("Overlay")
}

enum class InterpolationType(val displayName: String) {
    LINEAR("Linear"),
    EASE_IN_OUT("Smooth Ease In/Out"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out"),
    STEP("Step")
}

enum class EffectType(val displayName: String) {
    GLOW("Neon Glow"),
    CHROMATIC_ABERRATION("RGB Glitch / Aberration"),
    SCANLINES("CRT Scanlines"),
    VHS("Retro VHS Noise"),
    BLUR("Zoom Blur"),
    HUE_SHIFT("Hue Rotation"),
    VIGNETTE("Cinematic Vignette"),
    INVERT("Invert Colors"),
    GLITCH("Digital Glitch")
}

data class LayerEffect(
    val id: String = UUID.randomUUID().toString(),
    val type: EffectType = EffectType.GLOW,
    val enabled: Boolean = true,
    val intensity: Float = 0.5f,
    val speed: Float = 1.0f
)

data class AudioMappingConfig(
    val enabled: Boolean = true,
    val source: AudioSource = AudioSource.BASS,
    val target: AudioTarget = AudioTarget.SCALE,
    val sensitivity: Float = 1.5f,
    val smoothing: Float = 0.7f,
    val attack: Float = 0.9f,
    val decay: Float = 0.8f,
    val minVal: Float = 1.0f,
    val maxVal: Float = 2.0f,
    val invert: Boolean = false
)

data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long = 0L,
    val property: String = "scale",
    val value: Float = 1.0f,
    val interpolation: InterpolationType = InterpolationType.EASE_IN_OUT
)

data class LayerTransform(
    val x: Float = 0.5f,        // 0.0 to 1.0 relative to canvas width
    val y: Float = 0.5f,        // 0.0 to 1.0 relative to canvas height
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val rotation: Float = 0.0f, // degrees
    val opacity: Float = 1.0f
)

data class VisualizerLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New Layer",
    val type: LayerType = LayerType.SPECTRUM,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val transform: LayerTransform = LayerTransform(),
    val blendMode: BlendModeType = BlendModeType.NORMAL,
    val primaryColorHex: Long = 0xFF00E5FF,   // Cyan default
    val secondaryColorHex: Long = 0xFFFF007F, // Magenta default
    val audioMapping: AudioMappingConfig = AudioMappingConfig(),
    val effects: List<LayerEffect> = listOf(),
    val keyframes: List<Keyframe> = listOf(),
    
    // Type-specific properties
    val barCount: Int = 48,
    val barWidth: Float = 6f,
    val spectrumRadius: Float = 140f,
    val shapeType: ShapeType = ShapeType.CIRCLE,
    val meshType: Mesh3DType = Mesh3DType.CUBE,
    val particlePreset: ParticlePreset = ParticlePreset.ENERGY_BURST,
    val particleCount: Int = 80,
    val textContent: String = "DROP THE BASS",
    val textSize: Float = 36f,
    val imageUri: String? = null,
    val expressionFormula: String = "bass * 1.5"
)

data class VisualizerScene(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Main Scene",
    val layers: List<VisualizerLayer> = listOf(),
    val durationMs: Long = 30000L
)

enum class ResolutionPreset(val displayName: String, val width: Int, val height: Int) {
    HD_720P("720p HD (1280x720)", 1280, 720),
    FULL_HD_1080P("1080p FHD (1920x1080)", 1920, 1080),
    SQUARE_1080P("Square 1:1 (1080x1080)", 1080, 1080),
    PORTRAIT_1080P("Reels / TikTok (1080x1920)", 1080, 1920)
}

enum class PreviewQuality(val displayName: String, val scaleFactor: Float) {
    LOW("Low (Fastest)", 0.5f),
    MEDIUM("Medium", 0.75f),
    HIGH("High (Native)", 1.0f)
}

data class VisualizerProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Cyber Visualizer",
    val resolution: ResolutionPreset = ResolutionPreset.HD_720P,
    val targetFps: Int = 60,
    val durationMs: Long = 30000L,
    val audioTitle: String = "Synthwave Odyssey",
    val audioUri: String? = null,
    val scenes: List<VisualizerScene> = listOf(VisualizerScene()),
    val currentSceneIndex: Int = 0
) {
    val activeScene: VisualizerScene
        get() = scenes.getOrElse(currentSceneIndex) { scenes.firstOrNull() ?: VisualizerScene() }
}
