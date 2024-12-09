package com.github.alaindavid001.multistringsearch.search

import com.github.alaindavid001.multistringsearch.ui.PatternInputPanel
import com.github.alaindavid001.multistringsearch.ui.ResultsPanel
import com.github.alaindavid001.multistringsearch.utils.AhoCorasick
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.IdeFocusManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class SearchManager {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var currentTask: Future<*>? = null
    private var debounceDelay: Long = 300

    var myOpenedFileText : String? = null
    var myPatterns : List<String> = emptyList()

    fun updateResultsDebounced(project: Project, patternInputPanel: PatternInputPanel, resultsPanel: ResultsPanel) {
        currentTask?.cancel(true)

        // Schedule a new task with debounce delay
        currentTask = executor.submit {
            try {
                TimeUnit.MILLISECONDS.sleep(debounceDelay) // Debounce delay
                updateResults(project, patternInputPanel, resultsPanel)
            } catch (e: InterruptedException) {
                // Handle the interruption, if needed
            }
        }
    }

    private fun updateResults(project: Project, patternInputPanel: PatternInputPanel, resultsPanel: ResultsPanel) {
        ApplicationManager.getApplication().executeOnPooledThread {
            myOpenedFileText = getOpenedFileText(project)
            myPatterns = patternInputPanel.getTextFieldValues().toSet().toList()

            resultsPanel.clearResults()
            val ac = AhoCorasick(myOpenedFileText ?: "", myPatterns, 0)

            // Update the UI on the Event Dispatch Thread (EDT) after computation finishes
            ApplicationManager.getApplication().invokeLater {
                if (myOpenedFileText != null) {
                    resultsPanel.updateResultsList(ac.getMatches())
                } else {
                    resultsPanel.showNoFileSelectedMessage()
                }
                resultsPanel.refreshComponent()
            }
        }
    }

    private fun getOpenedFileText(project: Project): String? = FileEditorManager.getInstance(project).selectedTextEditor?.document?.text

    fun moveToCharacter(project: Project, offset: Int) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val caretModel: CaretModel = editor.caretModel
        caretModel.moveToOffset(offset)
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
    }

    fun getLineAndColumn(project: Project, offset: Int): Pair<Int, Int>? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val document: Document = editor.document

        if (offset < 0 || offset >= document.textLength) return null

        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val columnOffset = offset - lineStartOffset

        return lineNumber to columnOffset
    }

    fun getLineText(project: Project, lineNumber: Int): String? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val document = editor.document

        // Ensure the line number is within the valid range
        if (lineNumber < 0 || lineNumber >= document.lineCount) return null

        // Get the start and end offsets for the desired line
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        return document.getText(TextRange(lineStartOffset, lineEndOffset))
    }
}