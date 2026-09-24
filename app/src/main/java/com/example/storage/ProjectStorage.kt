package com.example.storage

import android.content.Context
import com.example.model.VisualizerProject
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class ProjectStorage(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(VisualizerProject::class.java).indent("  ")

    private val projectsDir: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    private val autosaveFile: File
        get() = File(context.filesDir, "autosave.mvproj")

    fun saveProject(project: VisualizerProject, customFile: File? = null): File {
        val targetFile = customFile ?: File(projectsDir, "${project.id}.mvproj")
        val json = adapter.toJson(project)
        targetFile.writeText(json)
        return targetFile
    }

    fun loadProject(file: File): VisualizerProject? {
        return try {
            val json = file.readText()
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveAutosave(project: VisualizerProject) {
        try {
            autosaveFile.writeText(adapter.toJson(project))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAutosave(): VisualizerProject? {
        return if (autosaveFile.exists()) {
            loadProject(autosaveFile)
        } else null
    }

    fun listSavedProjects(): List<File> {
        return projectsDir.listFiles { file -> file.extension == "mvproj" }?.toList() ?: emptyList()
    }

    fun getAllSavedProjects(): List<VisualizerProject> {
        val files = listSavedProjects()
        return files.mapNotNull { loadProject(it) }
            .sortedByDescending { it.durationMs } // or default sorting
    }

    fun deleteProject(projectId: String): Boolean {
        val targetFile = File(projectsDir, "$projectId.mvproj")
        return if (targetFile.exists()) {
            targetFile.delete()
        } else false
    }

    fun renameProject(projectId: String, newName: String): VisualizerProject? {
        val targetFile = File(projectsDir, "$projectId.mvproj")
        val proj = if (targetFile.exists()) loadProject(targetFile) else null
        return if (proj != null) {
            val updated = proj.copy(name = newName)
            saveProject(updated)
            updated
        } else null
    }
}

class HistoryManager(val maxSteps: Int = 50) {

    private val undoStack = ArrayDeque<VisualizerProject>()
    private val redoStack = ArrayDeque<VisualizerProject>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun pushState(currentState: VisualizerProject) {
        undoStack.addLast(currentState)
        if (undoStack.size > maxSteps) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(currentState: VisualizerProject): VisualizerProject? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(currentState)
        return undoStack.removeLast()
    }

    fun redo(currentState: VisualizerProject): VisualizerProject? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(currentState)
        return redoStack.removeLast()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
