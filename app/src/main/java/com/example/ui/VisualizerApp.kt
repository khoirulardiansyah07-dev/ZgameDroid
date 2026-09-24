package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreviewQuality
import com.example.ui.components.AudioTab
import com.example.ui.components.CanvasPreview
import com.example.ui.components.ExportDialog
import com.example.ui.components.HelpDialog
import com.example.ui.components.LayerInspectorSheet
import com.example.ui.components.LayerListSheet
import com.example.ui.components.MainMenuScreen
import com.example.ui.components.NewProjectDialog
import com.example.ui.components.NodeGraphView
import com.example.ui.components.TemplatesSheet
import com.example.ui.components.TimelineView
import com.example.ui.theme.MobileVisualizerTheme
import com.example.ui.theme.ThemeMode

@Composable
fun VisualizerApp(viewModel: VisualizerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val project by viewModel.project.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val savedProjects by viewModel.savedProjects.collectAsState()
    val showNewProjectDialog by viewModel.showNewProjectDialog.collectAsState()

    val audioFrame by viewModel.audioEngine.audioFrame.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val playbackPos by viewModel.audioEngine.playbackPositionMs.collectAsState()
    val totalDuration by viewModel.audioEngine.totalDurationMs.collectAsState()
    val trackName by viewModel.audioEngine.currentTrackName.collectAsState()

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val showHelpDialog by viewModel.showHelpDialog.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()

    var showQualityMenu by remember { mutableStateOf(false) }

    MobileVisualizerTheme(themeMode = themeMode) {
        BackHandler(enabled = currentScreen == AppScreen.STUDIO_EDITOR) {
            viewModel.setScreen(AppScreen.MAIN_MENU)
        }

        if (currentScreen == AppScreen.MAIN_MENU) {
            MainMenuScreen(
                currentProject = project,
                savedProjects = savedProjects,
                audioFrame = audioFrame,
                isPlayingAudio = isPlaying,
                currentAudioTrack = trackName,
                themeMode = themeMode,
                onOpenStudio = { viewModel.setScreen(AppScreen.STUDIO_EDITOR) },
                onNewProjectClick = { viewModel.showNewProject(true) },
                onSelectProject = { viewModel.openProject(it) },
                onDeleteProject = { viewModel.deleteSavedProject(it) },
                onDuplicateProject = { viewModel.duplicateSavedProject(it) },
                onRenameProject = { proj, name -> viewModel.renameSavedProject(proj, name) },
                onSelectTemplate = {
                    viewModel.selectTemplate(it)
                    viewModel.setScreen(AppScreen.STUDIO_EDITOR)
                },
                onQuickImportAudio = { uri, title ->
                    viewModel.audioEngine.loadCustomAudio(uri, title)
                },
                onPlayDemoTrack = { track ->
                    viewModel.audioEngine.selectDemoTrack(track)
                    viewModel.audioEngine.play()
                },
                onTogglePlayAudio = { viewModel.audioEngine.togglePlayPause() },
                onToggleTheme = { viewModel.toggleTheme() },
                onShowHelp = { viewModel.showHelp(true) }
            )
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    // Professional Studio Top Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Brand & Project Name with Home Action
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                IconButton(
                                    onClick = { viewModel.setScreen(AppScreen.MAIN_MENU) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("home_menu_button")
                                ) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = "Main Menu",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Column {
                                    Text(
                                        text = project.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Mobile Visualizer Studio",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Top Action Icons (New Project +, Undo, Redo, Quality, Theme, Help)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.showNewProject(true) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("top_new_project_button")
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "New Project",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.undo() },
                                    enabled = canUndo,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("undo_button")
                                ) {
                                    Icon(
                                        Icons.Default.Undo,
                                        contentDescription = "Undo",
                                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.redo() },
                                    enabled = canRedo,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("redo_button")
                                ) {
                                    Icon(
                                        Icons.Default.Redo,
                                        contentDescription = "Redo",
                                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }

                                // Quality Dropdown
                                Box {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .testTag("quality_selector")
                                    ) {
                                        Text(
                                            text = quality.displayName.substringBefore(" "),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showQualityMenu,
                                        onDismissRequest = { showQualityMenu = false }
                                    ) {
                                        PreviewQuality.values().forEach { q ->
                                            DropdownMenuItem(
                                                text = { Text(q.displayName) },
                                                onClick = {
                                                    viewModel.setQuality(q)
                                                    showQualityMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Theme Toggle
                                IconButton(
                                    onClick = { viewModel.toggleTheme() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (themeMode == ThemeMode.LIGHT) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = "Toggle Theme",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Help
                                IconButton(
                                    onClick = { viewModel.showHelp(true) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("help_button")
                                ) {
                                    Icon(
                                        Icons.Default.HelpOutline,
                                        contentDescription = "Help Guide",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == StudioTab.PROJECTS,
                            onClick = { viewModel.setTab(StudioTab.PROJECTS) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") },
                            label = { Text("Projects", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == StudioTab.LAYERS,
                            onClick = { viewModel.setTab(StudioTab.LAYERS) },
                            icon = { Icon(Icons.Default.Layers, contentDescription = "Layers") },
                            label = { Text("Layers", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == StudioTab.INSPECTOR,
                            onClick = { viewModel.setTab(StudioTab.INSPECTOR) },
                            icon = { Icon(Icons.Default.Tune, contentDescription = "Inspector") },
                            label = { Text("Inspector", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == StudioTab.AUDIO,
                            onClick = { viewModel.setTab(StudioTab.AUDIO) },
                            icon = { Icon(Icons.Default.MusicNote, contentDescription = "Audio") },
                            label = { Text("Audio", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == StudioTab.NODE_GRAPH,
                            onClick = { viewModel.setTab(StudioTab.NODE_GRAPH) },
                            icon = { Icon(Icons.Default.AccountTree, contentDescription = "Nodes") },
                            label = { Text("Nodes", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = showExportDialog,
                            onClick = { viewModel.showExport(true) },
                            icon = { Icon(Icons.Default.Download, contentDescription = "Export") },
                            label = { Text("Export", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Top Half: Live Preview Canvas (or Node Graph if selected)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f)
                    ) {
                        if (currentTab == StudioTab.NODE_GRAPH) {
                            NodeGraphView(
                                audioFrame = audioFrame,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CanvasPreview(
                                scene = project.activeScene,
                                audioFrame = audioFrame,
                                resolution = project.resolution,
                                quality = quality,
                                selectedLayer = viewModel.selectedLayer,
                                onUpdateLayerTransform = { dx, dy, zoom, rot ->
                                    viewModel.updateLayerTransformDelta(dx, dy, zoom, rot)
                                },
                                onDoubleTap = { viewModel.audioEngine.togglePlayPause() }
                            )
                        }
                    }

                    // Middle: Timeline scrubber
                    TimelineView(
                        currentTimeMs = playbackPos,
                        totalDurationMs = totalDuration,
                        isPlaying = isPlaying,
                        bpm = audioFrame.bpm,
                        selectedLayer = viewModel.selectedLayer,
                        onPlayPauseToggle = { viewModel.audioEngine.togglePlayPause() },
                        onSeek = { viewModel.audioEngine.seekTo(it) },
                        onAddKeyframe = {
                            viewModel.addKeyframe(
                                com.example.model.Keyframe(property = "scale", value = 1.25f)
                            )
                        }
                    )

                    // Bottom Half: Tab Context Editor
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.9f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        when (currentTab) {
                            StudioTab.PROJECTS -> {
                                TemplatesSheet(
                                    currentProject = project,
                                    onSelectTemplate = { viewModel.selectTemplate(it) },
                                    onNewBlankProject = { viewModel.newBlankProject() },
                                    onOpenNewProjectDialog = { viewModel.showNewProject(true) },
                                    onGoToMainMenu = { viewModel.setScreen(AppScreen.MAIN_MENU) },
                                    onSaveProject = { viewModel.saveProject() },
                                    onProjectNameChange = { viewModel.updateProjectName(it) }
                                )
                            }
                            StudioTab.LAYERS -> {
                                LayerListSheet(
                                    layers = project.activeScene.layers,
                                    selectedLayerId = selectedLayerId,
                                    onSelectLayer = { viewModel.selectLayer(it) },
                                    onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                                    onToggleLock = { viewModel.toggleLayerLock(it) },
                                    onMoveLayerUp = { viewModel.moveLayerUp(it) },
                                    onMoveLayerDown = { viewModel.moveLayerDown(it) },
                                    onDuplicateLayer = { viewModel.duplicateLayer(it) },
                                    onDeleteLayer = { viewModel.deleteLayer(it) },
                                    onAddLayer = { viewModel.addLayer(it) }
                                )
                            }
                            StudioTab.INSPECTOR -> {
                                val layer = viewModel.selectedLayer
                                if (layer != null) {
                                    LayerInspectorSheet(
                                        layer = layer,
                                        onUpdateLayer = { viewModel.updateLayer(it) },
                                        onAddKeyframe = { viewModel.addKeyframe(it) },
                                        onDeleteKeyframe = { viewModel.deleteKeyframe(it) }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No layer selected. Select a layer in 'Layers' tab to edit.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            StudioTab.AUDIO -> {
                                AudioTab(
                                    currentTrackName = trackName,
                                    audioFrame = audioFrame,
                                    sensitivity = viewModel.audioEngine.sensitivity,
                                    bassGain = viewModel.audioEngine.bassGain,
                                    onSelectDemoTrack = { viewModel.audioEngine.selectDemoTrack(it) },
                                    onImportAudio = { uri, title -> viewModel.audioEngine.loadCustomAudio(uri, title) },
                                    onSensitivityChange = { viewModel.audioEngine.sensitivity = it },
                                    onBassGainChange = { viewModel.audioEngine.bassGain = it }
                                )
                            }
                            StudioTab.NODE_GRAPH -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Node Graph Flow is active on canvas above. Drag nodes to modify audio flow.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }

        // New Project Dialog
        if (showNewProjectDialog) {
            NewProjectDialog(
                onDismiss = { viewModel.showNewProject(false) },
                onCreateProject = { name, res, fps, tmpl, demo, uri, title, bg ->
                    viewModel.createNewProject(
                        name = name,
                        resolution = res,
                        targetFps = fps,
                        template = tmpl,
                        demoTrack = demo,
                        audioUri = uri,
                        audioTitle = title,
                        backgroundColorHex = bg
                    )
                },
                onPreviewTrack = { track ->
                    viewModel.audioEngine.selectDemoTrack(track)
                    viewModel.audioEngine.play()
                },
                isPlayingPreview = isPlaying,
                currentPreviewTrack = trackName,
                onTogglePreviewPlay = { viewModel.audioEngine.togglePlayPause() }
            )
        }

        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                project = project,
                isExporting = isExporting,
                exportProgress = exportProgress,
                exportStatus = exportStatus,
                exportedFile = exportedFile,
                onStartExport = { viewModel.startExport(it) },
                onCancelExport = { viewModel.cancelExport() },
                onDismiss = { viewModel.showExport(false) }
            )
        }

        // Help Dialog
        if (showHelpDialog) {
            HelpDialog(onDismiss = { viewModel.showHelp(false) })
        }
    }
}

