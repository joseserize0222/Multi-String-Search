package com.github.alaindavid001.multistringsearch.toolWindow

import com.github.alaindavid001.multistringsearch.search.SearchManager
import com.github.alaindavid001.multistringsearch.ui.PatternInputPanel
import com.github.alaindavid001.multistringsearch.ui.ResultsPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBPanel
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout

class MyToolWindow(toolWindow: ToolWindow) : Disposable {
    private val myDisposable : Disposable = Disposer.newDisposable()
    private var searchManager : SearchManager = SearchManager()
    private var resultsPanel : ResultsPanel = ResultsPanel(toolWindow.project, searchManager)
    private var patternInputPanel : PatternInputPanel = PatternInputPanel(toolWindow.project, resultsPanel, searchManager)

    init {
        toolWindow.project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object :
            FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                searchManager.updateResultsDebounced(toolWindow.project, patternInputPanel, resultsPanel)
                addDocumentListenerToSelectedFile(toolWindow.project)
            }
        })

        addDocumentListenerToSelectedFile(toolWindow.project)
    }

    private fun addDocumentListenerToSelectedFile(project: Project) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val document = editor?.document

        document?.addDocumentListener(object : com.intellij.openapi.editor.event.DocumentListener {
            override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                searchManager.updateResultsDebounced(project, patternInputPanel, resultsPanel)
            }
        }, myDisposable)
    }

    fun getContent() = JBPanel<JBPanel<*>>().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        val upperSectionPanel = patternInputPanel.getWrappedPanelInputPanel()
        val lowerSectionPanel = resultsPanel.getWrappedResultsPanel()

        add(upperSectionPanel)
        add(Box.createRigidArea(Dimension(0, 8)))
        add(lowerSectionPanel)
    }

    override fun dispose() {
        searchManager.executor.shutdownNow()
        Disposer.dispose(myDisposable)
    }
}
