package com.example.model

object PresetTemplates {

    fun getAllTemplates(): List<VisualizerProject> = listOf(
        createNeonSpectrumTemplate(),
        createCircularVisualizerTemplate(),
        createGalaxyParticlesTemplate(),
        createCyberpunkGlowTemplate(),
        createRetroVHSTemplate(),
        createDigitalGlitchTemplate(),
        createFireStormTemplate(),
        create3DTunnelTemplate(),
        createMinimalistWaveformTemplate(),
        create3DTorusTemplate(),
        createParticleExplosionTemplate(),
        createAudioReactiveTextTemplate()
    )

    fun createNeonSpectrumTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Neon Spectrum",
            audioTitle = "Cyber Neon Pulse",
            scenes = listOf(
                VisualizerScene(
                    name = "Neon Intro",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Dark Space BG",
                            type = LayerType.GRADIENT_BACKGROUND,
                            primaryColorHex = 0xFF050510,
                            secondaryColorHex = 0xFF140826
                        ),
                        VisualizerLayer(
                            name = "Cosmic Dust",
                            type = LayerType.PARTICLES,
                            particlePreset = ParticlePreset.DUST,
                            particleCount = 50,
                            primaryColorHex = 0xFF6C5CE7,
                            transform = LayerTransform(opacity = 0.6f)
                        ),
                        VisualizerLayer(
                            name = "Neon Glow Ring",
                            type = LayerType.SHAPE,
                            shapeType = ShapeType.RING,
                            primaryColorHex = 0xFFFF007F,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BASS,
                                target = AudioTarget.SCALE,
                                minVal = 0.9f,
                                maxVal = 1.3f
                            )
                        ),
                        VisualizerLayer(
                            name = "Circular Spectrum",
                            type = LayerType.CIRCULAR_SPECTRUM,
                            primaryColorHex = 0xFF00F5D4,
                            secondaryColorHex = 0xFF7B2CBF,
                            barCount = 64,
                            spectrumRadius = 150f,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.MID,
                                target = AudioTarget.ROTATION,
                                minVal = 0f,
                                maxVal = 360f,
                                sensitivity = 0.5f
                            ),
                            effects = listOf(
                                LayerEffect(type = EffectType.GLOW, intensity = 0.8f)
                            )
                        ),
                        VisualizerLayer(
                            name = "Audio Reactive Title",
                            type = LayerType.TEXT_REACTIVE,
                            textContent = "NEON BEAT",
                            textSize = 34f,
                            primaryColorHex = 0xFFFFFFFF,
                            secondaryColorHex = 0xFF00F5D4,
                            transform = LayerTransform(y = 0.5f),
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BEAT,
                                target = AudioTarget.SCALE,
                                minVal = 0.95f,
                                maxVal = 1.2f
                            )
                        )
                    )
                )
            )
        )
    }

    fun createCircularVisualizerTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Circular Music Visualizer",
            audioTitle = "Bassline Drive",
            scenes = listOf(
                VisualizerScene(
                    name = "Circle Center",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Vignette BG",
                            type = LayerType.SOLID_BACKGROUND,
                            primaryColorHex = 0xFF0A0E17
                        ),
                        VisualizerLayer(
                            name = "Pulsing Center Disk",
                            type = LayerType.SHAPE,
                            shapeType = ShapeType.CIRCLE,
                            primaryColorHex = 0xFF1E293B,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BASS,
                                target = AudioTarget.SCALE,
                                minVal = 0.85f,
                                maxVal = 1.15f
                            )
                        ),
                        VisualizerLayer(
                            name = "Radial Bars",
                            type = LayerType.CIRCULAR_SPECTRUM,
                            barCount = 72,
                            primaryColorHex = 0xFFFF3366,
                            secondaryColorHex = 0xFFFFD166,
                            spectrumRadius = 130f,
                            effects = listOf(LayerEffect(type = EffectType.GLOW, intensity = 0.7f))
                        ),
                        VisualizerLayer(
                            name = "Track Title",
                            type = LayerType.TEXT_REACTIVE,
                            textContent = "NOW PLAYING",
                            textSize = 22f,
                            primaryColorHex = 0xFFF8FAFC,
                            transform = LayerTransform(y = 0.5f)
                        )
                    )
                )
            )
        )
    }

    fun createGalaxyParticlesTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Galaxy",
            audioTitle = "Cosmic Voyage",
            scenes = listOf(
                VisualizerScene(
                    name = "Deep Galaxy",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Space Background",
                            type = LayerType.GRADIENT_BACKGROUND,
                            primaryColorHex = 0xFF02000A,
                            secondaryColorHex = 0xFF0D0C22
                        ),
                        VisualizerLayer(
                            name = "Swirling Stars",
                            type = LayerType.PARTICLES,
                            particlePreset = ParticlePreset.STARS,
                            particleCount = 120,
                            primaryColorHex = 0xFF8BE9FD,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.TREBLE,
                                target = AudioTarget.PARTICLE_BURST,
                                sensitivity = 2.0f
                            )
                        ),
                        VisualizerLayer(
                            name = "Energy Nebula",
                            type = LayerType.PARTICLES,
                            particlePreset = ParticlePreset.ENERGY_BURST,
                            particleCount = 80,
                            primaryColorHex = 0xFFFF79C6,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BASS,
                                target = AudioTarget.SCALE,
                                minVal = 1.0f,
                                maxVal = 2.0f
                            )
                        ),
                        VisualizerLayer(
                            name = "Center Galaxy Wave",
                            type = LayerType.WAVEFORM,
                            primaryColorHex = 0xFF50FA7B,
                            effects = listOf(LayerEffect(type = EffectType.CHROMATIC_ABERRATION, intensity = 0.6f))
                        )
                    )
                )
            )
        )
    }

    fun createCyberpunkGlowTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Cyberpunk",
            audioTitle = "Night City 2088",
            scenes = listOf(
                VisualizerScene(
                    name = "Cyber Scene",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Dark City BG",
                            type = LayerType.SOLID_BACKGROUND,
                            primaryColorHex = 0xFF08070C
                        ),
                        VisualizerLayer(
                            name = "Equalizer Bars",
                            type = LayerType.SPECTRUM,
                            barCount = 48,
                            primaryColorHex = 0xFF00F0FF,
                            secondaryColorHex = 0xFFFF003C,
                            transform = LayerTransform(y = 0.65f),
                            effects = listOf(
                                LayerEffect(type = EffectType.GLOW, intensity = 0.9f),
                                LayerEffect(type = EffectType.SCANLINES, intensity = 0.4f)
                            )
                        ),
                        VisualizerLayer(
                            name = "Cyber Hexagon",
                            type = LayerType.SHAPE,
                            shapeType = ShapeType.HEXAGON,
                            primaryColorHex = 0xFFFFE600,
                            transform = LayerTransform(y = 0.35f, scaleX = 0.8f, scaleY = 0.8f),
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BASS,
                                target = AudioTarget.SCALE,
                                minVal = 0.8f,
                                maxVal = 1.4f
                            )
                        ),
                        VisualizerLayer(
                            name = "Cyberpunk Text",
                            type = LayerType.TEXT_REACTIVE,
                            textContent = "CYBERPUNK",
                            textSize = 38f,
                            primaryColorHex = 0xFF00F0FF,
                            transform = LayerTransform(y = 0.35f),
                            effects = listOf(LayerEffect(type = EffectType.GLITCH, intensity = 0.5f))
                        )
                    )
                )
            )
        )
    }

    fun createRetroVHSTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Retro VHS",
            audioTitle = "Outrun 1984",
            scenes = listOf(
                VisualizerScene(
                    name = "VHS Synth",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Synth Sunset BG",
                            type = LayerType.GRADIENT_BACKGROUND,
                            primaryColorHex = 0xFF2A0845,
                            secondaryColorHex = 0xFFFF416C
                        ),
                        VisualizerLayer(
                            name = "Oscilloscope Wave",
                            type = LayerType.WAVEFORM,
                            primaryColorHex = 0xFF00FFFF,
                            transform = LayerTransform(y = 0.5f),
                            effects = listOf(
                                LayerEffect(type = EffectType.VHS, intensity = 0.7f),
                                LayerEffect(type = EffectType.SCANLINES, intensity = 0.5f)
                            )
                        ),
                        VisualizerLayer(
                            name = "Synth Text",
                            type = LayerType.TEXT_REACTIVE,
                            textContent = "SYNTHWAVE",
                            textSize = 32f,
                            primaryColorHex = 0xFFFFE600,
                            transform = LayerTransform(y = 0.3f),
                            effects = listOf(LayerEffect(type = EffectType.CHROMATIC_ABERRATION, intensity = 0.8f))
                        )
                    )
                )
            )
        )
    }

    fun createDigitalGlitchTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Glitch",
            audioTitle = "Distortion Overdrive",
            scenes = listOf(
                VisualizerScene(
                    name = "Glitch Scene",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Glitch BG",
                            type = LayerType.SOLID_BACKGROUND,
                            primaryColorHex = 0xFF111111
                        ),
                        VisualizerLayer(
                            name = "Glitch Bars",
                            type = LayerType.SPECTRUM,
                            barCount = 36,
                            primaryColorHex = 0xFFFF0055,
                            secondaryColorHex = 0xFF00FFCC,
                            effects = listOf(
                                LayerEffect(type = EffectType.GLITCH, intensity = 0.9f),
                                LayerEffect(type = EffectType.CHROMATIC_ABERRATION, intensity = 0.8f)
                            )
                        ),
                        VisualizerLayer(
                            name = "SYSTEM OVERLOAD",
                            type = LayerType.TEXT_REACTIVE,
                            textContent = "GLITCH DETECTED",
                            textSize = 28f,
                            primaryColorHex = 0xFFFFFFFF,
                            transform = LayerTransform(y = 0.4f),
                            audioMapping = AudioMappingConfig(source = AudioSource.BEAT, target = AudioTarget.GLOW)
                        )
                    )
                )
            )
        )
    }

    fun createFireStormTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Fire",
            audioTitle = "Inferno Beats",
            scenes = listOf(
                VisualizerScene(
                    name = "Inferno",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Ember BG",
                            type = LayerType.GRADIENT_BACKGROUND,
                            primaryColorHex = 0xFF120300,
                            secondaryColorHex = 0xFF350900
                        ),
                        VisualizerLayer(
                            name = "Fire Particles",
                            type = LayerType.PARTICLES,
                            particlePreset = ParticlePreset.FIRE,
                            particleCount = 100,
                            primaryColorHex = 0xFFFF5722,
                            secondaryColorHex = 0xFFFFC107,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BASS,
                                target = AudioTarget.PARTICLE_BURST,
                                sensitivity = 2.0f
                            )
                        ),
                        VisualizerLayer(
                            name = "Flames Spectrum",
                            type = LayerType.SPECTRUM,
                            barCount = 52,
                            primaryColorHex = 0xFFFF3D00,
                            secondaryColorHex = 0xFFFFEA00,
                            transform = LayerTransform(y = 0.75f),
                            effects = listOf(LayerEffect(type = EffectType.GLOW, intensity = 0.8f))
                        )
                    )
                )
            )
        )
    }

    fun create3DTunnelTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "3D Tunnel",
            audioTitle = "Dimension Warp",
            scenes = listOf(
                VisualizerScene(
                    name = "Warp Tunnel",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Deep Void BG",
                            type = LayerType.SOLID_BACKGROUND,
                            primaryColorHex = 0xFF030308
                        ),
                        VisualizerLayer(
                            name = "Cyber Tunnel",
                            type = LayerType.MESH_3D,
                            meshType = Mesh3DType.TUNNEL,
                            primaryColorHex = 0xFF00E5FF,
                            secondaryColorHex = 0xFFD500F9,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BASS,
                                target = AudioTarget.SCALE,
                                minVal = 0.8f,
                                maxVal = 1.4f
                            )
                        ),
                        VisualizerLayer(
                            name = "Cosmic Stars",
                            type = LayerType.PARTICLES,
                            particlePreset = ParticlePreset.STARS,
                            particleCount = 60,
                            primaryColorHex = 0xFFFFFFFF
                        )
                    )
                )
            )
        )
    }

    fun createMinimalistWaveformTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Minimal",
            audioTitle = "Acoustic Horizon",
            scenes = listOf(
                VisualizerScene(
                    name = "Clean Horizon",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Pure Minimal BG",
                            type = LayerType.SOLID_BACKGROUND,
                            primaryColorHex = 0xFF0F172A
                        ),
                        VisualizerLayer(
                            name = "Center Oscilloscope",
                            type = LayerType.WAVEFORM,
                            primaryColorHex = 0xFF38BDF8,
                            transform = LayerTransform(y = 0.5f, scaleY = 1.2f)
                        ),
                        VisualizerLayer(
                            name = "Track Meta",
                            type = LayerType.TEXT_REACTIVE,
                            textContent = "MINIMAL AUDIO",
                            textSize = 20f,
                            primaryColorHex = 0xFF94A3B8,
                            transform = LayerTransform(y = 0.65f)
                        )
                    )
                )
            )
        )
    }

    fun create3DTorusTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Space",
            audioTitle = "Orbital Rings",
            scenes = listOf(
                VisualizerScene(
                    name = "Orbit 3D",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Dark Space BG",
                            type = LayerType.SOLID_BACKGROUND,
                            primaryColorHex = 0xFF05050A
                        ),
                        VisualizerLayer(
                            name = "Wireframe Torus",
                            type = LayerType.MESH_3D,
                            meshType = Mesh3DType.TORUS,
                            primaryColorHex = 0xFF00FF88,
                            secondaryColorHex = 0xFF0099FF,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BASS,
                                target = AudioTarget.SCALE,
                                minVal = 0.85f,
                                maxVal = 1.35f
                            ),
                            effects = listOf(LayerEffect(type = EffectType.GLOW, intensity = 0.6f))
                        ),
                        VisualizerLayer(
                            name = "Floating Dust",
                            type = LayerType.PARTICLES,
                            particlePreset = ParticlePreset.DUST,
                            particleCount = 50,
                            primaryColorHex = 0xFF00FF88
                        )
                    )
                )
            )
        )
    }

    fun createParticleExplosionTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Particle Explosion",
            audioTitle = "Drop Explosion",
            scenes = listOf(
                VisualizerScene(
                    name = "Explosion Arena",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Black Pit BG",
                            type = LayerType.SOLID_BACKGROUND,
                            primaryColorHex = 0xFF000000
                        ),
                        VisualizerLayer(
                            name = "Exploding Sparks",
                            type = LayerType.PARTICLES,
                            particlePreset = ParticlePreset.ENERGY_BURST,
                            particleCount = 140,
                            primaryColorHex = 0xFFFFEE00,
                            secondaryColorHex = 0xFFFF0055,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BEAT,
                                target = AudioTarget.PARTICLE_BURST,
                                sensitivity = 3.0f
                            )
                        ),
                        VisualizerLayer(
                            name = "Shockwave Ring",
                            type = LayerType.SHAPE,
                            shapeType = ShapeType.RING,
                            primaryColorHex = 0xFFFFFFFF,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BEAT,
                                target = AudioTarget.SCALE,
                                minVal = 0.5f,
                                maxVal = 2.0f
                            )
                        )
                    )
                )
            )
        )
    }

    fun createAudioReactiveTextTemplate(): VisualizerProject {
        return VisualizerProject(
            name = "Audio Reactive Text",
            audioTitle = "Vocal Kinetic",
            scenes = listOf(
                VisualizerScene(
                    name = "Kinetic Words",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Gradient Backdrop",
                            type = LayerType.GRADIENT_BACKGROUND,
                            primaryColorHex = 0xFF180A2A,
                            secondaryColorHex = 0xFF0A1828
                        ),
                        VisualizerLayer(
                            name = "Kinetic Typography",
                            type = LayerType.TEXT_REACTIVE,
                            textContent = "FEEL THE BEAT",
                            textSize = 44f,
                            primaryColorHex = 0xFFFF007F,
                            secondaryColorHex = 0xFF00E5FF,
                            audioMapping = AudioMappingConfig(
                                source = AudioSource.BASS,
                                target = AudioTarget.SCALE,
                                minVal = 0.9f,
                                maxVal = 1.4f
                            ),
                            effects = listOf(
                                LayerEffect(type = EffectType.GLOW, intensity = 0.9f),
                                LayerEffect(type = EffectType.CHROMATIC_ABERRATION, intensity = 0.5f)
                            )
                        ),
                        VisualizerLayer(
                            name = "Sub Waveform",
                            type = LayerType.WAVEFORM,
                            primaryColorHex = 0xFF00E5FF,
                            transform = LayerTransform(y = 0.7f, scaleY = 0.6f)
                        )
                    )
                )
            )
        )
    }
}
