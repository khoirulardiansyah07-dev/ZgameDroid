package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.AudioFrame
import com.example.audio.DemoTrack
import com.example.export.ExportConfig
import com.example.export.VideoExportEngine
import com.example.model.Keyframe
import com.example.model.LayerTransform
import com.example.model.LayerType
import com.example.model.PresetTemplates
import com.example.model.PreviewQuality
import com.example.model.VisualizerLayer
import com.example.model.VisualizerProject
import com.example.storage.HistoryManager
import com.example.storage.ProjectStorage
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class StudioTab(val title: String) {
    CANVAS("Preview"),
    PROJECTS("Projects"),
    LAYERS("Layers"),
    INSPECTOR("Inspector"),
    AUDIO("Audio"),
    NODE_GRAPH("Nodes")
}

enum class AppScreen {
    MAIN_MENU,
    STUDIO_EDITOR
}

class VisualizerViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = AudioEngine(application)
    private val projectStorage = ProjectStorage(application)
    private val historyManager = HistoryManager(50)
    private val videoExportEngine = VideoExportEngine(application)

    // App Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // New Project Dialog State
    private val _showNewProjectDialog = MutableStateFlow(false)
    val showNewProjectDialog: StateFlow<Boolean> = _showNewProjectDialog.asStateFlow()

    // Saved Projects List
    private val _savedProjects = MutableStateFlow<List<VisualizerProject>>(emptyList())
    val savedProjects: StateFlow<List<VisualizerProject>> = _savedProjects.asStateFlow()

    // Current Project State
    private val _project = MutableStateFlow(PresetTemplates.createNeonSpectrumTemplate())
    val project: StateFlow<VisualizerProject> = _project.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private val _currentTab = MutableStateFlow(StudioTab.LAYERS)
    val currentTab: StateFlow<StudioTab> = _currentTab.asStateFlow()

    private val _quality = MutableStateFlow(PreviewQuality.HIGH)
    val quality: StateFlow<PreviewQuality> = _quality.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _showHelpDialog = MutableStateFlow(false)
    val showHelpDialog: StateFlow<Boolean> = _showHelpDialog.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    // Export Progress
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportStatus = MutableStateFlow("")
    val exportStatus: StateFlow<String> = _exportStatus.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    // Undo / Redo Status
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    init {
        // Select first layer by default
        _selectedLayerId.value = _project.value.activeScene.layers.firstOrNull()?.id

        // Check for autosave
        val restored = projectStorage.loadAutosave()
        if (restored != null) {
            _project.value = restored
            _selectedLayerId.value = restored.activeScene.layers.firstOrNull()?.id
        }

        refreshSavedProjects()
    }

    val selectedLayer: VisualizerLayer?
        get() = _project.value.activeScene.layers.firstOrNull { it.id == _selectedLayerId.value }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun showNewProject(show: Boolean) {
        _showNewProjectDialog.value = show
    }

    fun refreshSavedProjects() {
        _savedProjects.value = projectStorage.getAllSavedProjects()
    }

    fun openProject(proj: VisualizerProject) {
        saveUndoState()
        _project.value = proj
        _selectedLayerId.value = proj.activeScene.layers.firstOrNull()?.id
        if (proj.audioUri != null) {
            try {
                audioEngine.loadCustomAudio(android.net.Uri.parse(proj.audioUri), proj.audioTitle)
            } catch (e: Exception) {
                audioEngine.selectDemoTrack(DemoTrack.SYNTHWAVE_PULSE)
            }
        } else {
            val demo = DemoTrack.values().firstOrNull { it.title == proj.audioTitle } ?: DemoTrack.SYNTHWAVE_PULSE
            audioEngine.selectDemoTrack(demo)
        }
        saveAutosave()
        _currentScreen.value = AppScreen.STUDIO_EDITOR
    }

    fun createNewProject(
        name: String,
        resolution: com.example.model.ResolutionPreset = com.example.model.ResolutionPreset.HD_720P,
        targetFps: Int = 60,
        template: VisualizerProject? = null,
        demoTrack: DemoTrack? = null,
        audioUri: Uri? = null,
        audioTitle: String? = null,
        backgroundColorHex: Long? = null
    ) {
        saveUndoState()
        val baseScenes = if (template != null) {
            template.scenes
        } else {
            val bgLayer = VisualizerLayer(
                name = "Background",
                type = LayerType.GRADIENT_BACKGROUND,
                primaryColorHex = backgroundColorHex ?: 0xFF050510L,
                secondaryColorHex = 0xFF140826L
            )
            val coreLayer = VisualizerLayer(
                name = "Audio Spectrum",
                type = LayerType.SPECTRUM,
                primaryColorHex = 0xFF00E5FFL,
                secondaryColorHex = 0xFFFF007FL
            )
            listOf(
                com.example.model.VisualizerScene(
                    name = "Scene 1",
                    layers = listOf(bgLayer, coreLayer)
                )
            )
        }

        val trackTitle = audioTitle ?: demoTrack?.title ?: template?.audioTitle ?: "Cyberpunk Synthwave"

        val newProj = VisualizerProject(
            name = name.ifBlank { "New Visualizer" },
            resolution = resolution,
            targetFps = targetFps,
            durationMs = demoTrack?.durationMs ?: 45000L,
            audioTitle = trackTitle,
            audioUri = audioUri?.toString(),
            scenes = baseScenes
        )

        _project.value = newProj
        _selectedLayerId.value = newProj.activeScene.layers.firstOrNull()?.id

        projectStorage.saveProject(newProj)
        saveAutosave()
        refreshSavedProjects()

        if (audioUri != null) {
            audioEngine.loadCustomAudio(audioUri, trackTitle)
        } else if (demoTrack != null) {
            audioEngine.selectDemoTrack(demoTrack)
        } else {
            audioEngine.selectDemoTrack(DemoTrack.SYNTHWAVE_PULSE)
        }

        _showNewProjectDialog.value = false
        _currentScreen.value = AppScreen.STUDIO_EDITOR
    }

    fun deleteSavedProject(proj: VisualizerProject) {
        projectStorage.deleteProject(proj.id)
        refreshSavedProjects()
    }

    fun duplicateSavedProject(proj: VisualizerProject) {
        val clone = proj.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${proj.name} (Copy)"
        )
        projectStorage.saveProject(clone)
        refreshSavedProjects()
    }

    fun renameSavedProject(proj: VisualizerProject, newName: String) {
        val updated = projectStorage.renameProject(proj.id, newName)
        if (_project.value.id == proj.id && updated != null) {
            _project.value = updated
        }
        refreshSavedProjects()
    }

    fun setTab(tab: StudioTab) {
        _currentTab.value = tab
    }

    fun selectLayer(layerId: String) {
        _selectedLayerId.value = layerId
        _currentTab.value = StudioTab.INSPECTOR
    }

    fun updateProjectName(name: String) {
        saveUndoState()
        _project.value = _project.value.copy(name = name)
        saveAutosave()
    }

    fun selectTemplate(template: VisualizerProject) {
        saveUndoState()
        _project.value = template
        _selectedLayerId.value = template.activeScene.layers.firstOrNull()?.id
        audioEngine.selectDemoTrack(DemoTrack.SYNTHWAVE_PULSE)
        saveAutosave()
    }

    fun newBlankProject() {
        saveUndoState()
        val blank = VisualizerProject(
            name = "Untitled Project",
            scenes = listOf(
                com.example.model.VisualizerScene(
                    name = "Scene 1",
                    layers = listOf(
                        VisualizerLayer(
                            name = "Spectrum Core",
                            type = LayerType.SPECTRUM
                        )
                    )
                )
            )
        )
        _project.value = blank
        _selectedLayerId.value = blank.activeScene.layers.firstOrNull()?.id
        saveAutosave()
    }

    fun updateLayer(updated: VisualizerLayer) {
        saveUndoState()
        val currentScene = _project.value.activeScene
        val newLayers = currentScene.layers.map { if (it.id == updated.id) updated else it }
        val newScene = currentScene.copy(layers = newLayers)
        val newScenes = _project.value.scenes.mapIndexed { idx, scene ->
            if (idx == _project.value.currentSceneIndex) newScene else scene
        }
        _project.value = _project.value.copy(scenes = newScenes)
        saveAutosave()
    }

    fun updateLayerTransformDelta(dx: Float, dy: Float, zoom: Float, rotDelta: Float) {
        val current = selectedLayer ?: return
        val t = current.transform
        val newTransform = t.copy(
            x = (t.x + dx).coerceIn(0f, 1f),
            y = (t.y + dy).coerceIn(0f, 1f),
            scaleX = (t.scaleX * zoom).coerceIn(0.2f, 4f),
            scaleY = (t.scaleY * zoom).coerceIn(0.2f, 4f),
            rotation = (t.rotation + rotDelta) % 360f
        )
        updateLayer(current.copy(transform = newTransform))
    }

    fun addLayer(type: LayerType) {
        saveUndoState()
        val currentScene = _project.value.activeScene
        val newLayer = VisualizerLayer(
            name = "${type.displayName} ${currentScene.layers.size + 1}",
            type = type
        )
        val newLayers = currentScene.layers + newLayer
        val newScene = currentScene.copy(layers = newLayers)
        val newScenes = _project.value.scenes.mapIndexed { idx, scene ->
            if (idx == _project.value.currentSceneIndex) newScene else scene
        }
        _project.value = _project.value.copy(scenes = newScenes)
        _selectedLayerId.value = newLayer.id
        _currentTab.value = StudioTab.INSPECTOR
        saveAutosave()
    }

    fun deleteLayer(layerId: String) {
        saveUndoState()
        val currentScene = _project.value.activeScene
        val newLayers = currentScene.layers.filter { it.id != layerId }
        val newScene = currentScene.copy(layers = newLayers)
        val newScenes = _project.value.scenes.mapIndexed { idx, scene ->
            if (idx == _project.value.currentSceneIndex) newScene else scene
        }
        _project.value = _project.value.copy(scenes = newScenes)
        if (_selectedLayerId.value == layerId) {
            _selectedLayerId.value = newLayers.firstOrNull()?.id
        }
        saveAutosave()
    }

    fun duplicateLayer(layerId: String) {
        val layer = _project.value.activeScene.layers.firstOrNull { it.id == layerId } ?: return
        saveUndoState()
        val currentScene = _project.value.activeScene
        val copy = layer.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${layer.name} Copy",
            transform = layer.transform.copy(x = (layer.transform.x + 0.05f).coerceIn(0f, 1f))
        )
        val newLayers = currentScene.layers + copy
        val newScene = currentScene.copy(layers = newLayers)
        val newScenes = _project.value.scenes.mapIndexed { idx, scene ->
            if (idx == _project.value.currentSceneIndex) newScene else scene
        }
        _project.value = _project.value.copy(scenes = newScenes)
        _selectedLayerId.value = copy.id
        saveAutosave()
    }

    fun toggleLayerVisibility(layerId: String) {
        val layer = _project.value.activeScene.layers.firstOrNull { it.id == layerId } ?: return
        updateLayer(layer.copy(isVisible = !layer.isVisible))
    }

    fun toggleLayerLock(layerId: String) {
        val layer = _project.value.activeScene.layers.firstOrNull { it.id == layerId } ?: return
        updateLayer(layer.copy(isLocked = !layer.isLocked))
    }

    fun moveLayerUp(index: Int) {
        if (index <= 0) return
        saveUndoState()
        val currentScene = _project.value.activeScene
        val list = currentScene.layers.toMutableList()
        val item = list.removeAt(index)
        list.add(index - 1, item)
        val newScene = currentScene.copy(layers = list)
        val newScenes = _project.value.scenes.mapIndexed { idx, scene ->
            if (idx == _project.value.currentSceneIndex) newScene else scene
        }
        _project.value = _project.value.copy(scenes = newScenes)
        saveAutosave()
    }

    fun moveLayerDown(index: Int) {
        val currentScene = _project.value.activeScene
        if (index >= currentScene.layers.size - 1) return
        saveUndoState()
        val list = currentScene.layers.toMutableList()
        val item = list.removeAt(index)
        list.add(index + 1, item)
        val newScene = currentScene.copy(layers = list)
        val newScenes = _project.value.scenes.mapIndexed { idx, scene ->
            if (idx == _project.value.currentSceneIndex) newScene else scene
        }
        _project.value = _project.value.copy(scenes = newScenes)
        saveAutosave()
    }

    fun addKeyframe(keyframe: Keyframe) {
        val current = selectedLayer ?: return
        val currentMs = audioEngine.playbackPositionMs.value
        val kf = keyframe.copy(timeMs = currentMs)
        val updated = current.copy(keyframes = current.keyframes + kf)
        updateLayer(updated)
    }

    fun deleteKeyframe(keyframeId: String) {
        val current = selectedLayer ?: return
        val updated = current.copy(keyframes = current.keyframes.filter { it.id != keyframeId })
        updateLayer(updated)
    }

    // Undo / Redo
    private fun saveUndoState() {
        historyManager.pushState(_project.value)
        _canUndo.value = historyManager.canUndo
        _canRedo.value = historyManager.canRedo
    }

    fun undo() {
        val prev = historyManager.undo(_project.value) ?: return
        _project.value = prev
        _canUndo.value = historyManager.canUndo
        _canRedo.value = historyManager.canRedo
        saveAutosave()
    }

    fun redo() {
        val next = historyManager.redo(_project.value) ?: return
        _project.value = next
        _canUndo.value = historyManager.canUndo
        _canRedo.value = historyManager.canRedo
        saveAutosave()
    }

    fun saveProject() {
        projectStorage.saveProject(_project.value)
        refreshSavedProjects()
    }

    private fun saveAutosave() {
        projectStorage.saveAutosave(_project.value)
    }

    fun setQuality(q: PreviewQuality) {
        _quality.value = q
    }

    fun toggleTheme() {
        _themeMode.value = when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.AMOLED
            ThemeMode.AMOLED -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
        }
    }

    fun showHelp(show: Boolean) {
        _showHelpDialog.value = show
    }

    fun showExport(show: Boolean) {
        _showExportDialog.value = show
        if (!show) {
            _exportedFile.value = null
        }
    }

    // Video Export Execution
    fun startExport(config: ExportConfig) {
        _isExporting.value = true
        _exportProgress.value = 0f
        _exportStatus.value = "Initializing MediaCodec..."
        _exportedFile.value = null

        viewModelScope.launch {
            val file = videoExportEngine.exportVideo(
                project = _project.value,
                config = config,
                onProgress = { progress, status ->
                    _exportProgress.value = progress
                    _exportStatus.value = status
                }
            )
            _isExporting.value = false
            _exportedFile.value = file
        }
    }

    fun cancelExport() {
        videoExportEngine.cancel()
        _isExporting.value = false
        _exportStatus.value = "Cancelled"
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
