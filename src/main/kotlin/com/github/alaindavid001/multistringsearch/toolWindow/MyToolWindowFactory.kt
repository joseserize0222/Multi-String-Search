package com.github.alaindavid001.multistringsearch.toolWindow

import com.github.alaindavid001.multistringsearch.utils.AhoCorasick
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.observable.util.addComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBFont
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.*


class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) : Disposable {
        private val myDisposable : Disposable = Disposer.newDisposable()

        init {
            toolWindow.project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    updateResultsDebounced()
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
                    updateResultsDebounced()
                }
            }, myDisposable)
        }

        private val executor: ExecutorService = Executors.newSingleThreadExecutor()
        private var currentTask: Future<*>? = null
        private var debounceDelay: Long = 300

        private val project = toolWindow.project

        private var myOpenedFileText : String? = null
        private var myPatterns : List<String> = emptyList()

        private val textFieldListPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.CENTER_ALIGNMENT

            addRemovableTextField(0)
        }.also { panel ->
            panel.add(JButton(AllIcons.General.Add).apply {
                preferredSize = Dimension(30, 30)
                minimumSize = Dimension(30, 30)
                maximumSize = Dimension(30, 30)
                alignmentX = Component.CENTER_ALIGNMENT

                addActionListener {
                    val newTextField = panel.addRemovableTextField(panel.componentCount - 1)
                    refreshComponent(panel)
                    newTextField.requestFocus()
                }
            })
        }

        private val resultsList = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.CENTER_ALIGNMENT
            alignmentY = Component.CENTER_ALIGNMENT
            border = BorderFactory.createEmptyBorder(0,0,10,0)
            add(JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                alignmentX = Component.CENTER_ALIGNMENT
                border = BorderFactory.createEmptyBorder(90,0,0,0)
                add(JBLabel("Please select a file"))
            })
        }

        private fun getOpenedFileText(): String? = FileEditorManager.getInstance(project).selectedTextEditor?.document?.text

        private fun getTextFieldValues(): List<String> = (0 until textFieldListPanel.componentCount - 1)
                .mapNotNull { textFieldListPanel.getComponent(it) as? JBPanel<*> }
                .mapNotNull { it.getComponent(0) as? JBTextField }
                .map { it.text }


        private fun moveToCharacter(offset: Int) {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
            val caretModel: CaretModel = editor.caretModel
            caretModel.moveToOffset(offset)
            IdeFocusManager.getInstance(project).requestFocus(editor.contentComponent, true)
        }

        private fun getLineAndColumn(offset: Int): Pair<Int, Int>? {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
            val document: Document = editor.document

            if (offset < 0 || offset >= document.textLength) return null

            val lineNumber = document.getLineNumber(offset)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val columnOffset = offset - lineStartOffset

            return lineNumber to columnOffset
        }

        private fun getLineText(lineNumber: Int): String? {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
            val document = editor.document

            // Ensure the line number is within the valid range
            if (lineNumber < 0 || lineNumber >= document.lineCount) return null

            // Get the start and end offsets for the desired line
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val lineEndOffset = document.getLineEndOffset(lineNumber)
            return document.getText(TextRange(lineStartOffset, lineEndOffset))
        }

        private fun createHighlightedLabelText(line: Int, col: Int, strLen: Int): String {
            val fullText = getLineText(line) ?: return "Error while getting line"

            // Ensure offset and length are within bounds
            val safeOffset = col.coerceAtLeast(0).coerceAtMost(fullText.length)
            val end = (safeOffset + strLen).coerceAtMost(fullText.length)

            // Split the text into three parts: before, highlighted, and after
            val before = fullText.substring(0, safeOffset)
            val highlighted = fullText.substring(safeOffset, end)
            val after = fullText.substring(end)

            // Wrap the highlighted part with HTML tags for styling
            return "<html><body>Line ${line+1} Col ${col+1}: $before<span style='background-color: #FFD54F;'><font color='black'>$highlighted</font></span>$after</body></html>"
        }

        private fun refreshComponent(component: JBPanel<JBPanel<*>>) {
            component.revalidate()
            component.repaint()
        }

        fun updateResultsDebounced() {
            currentTask?.cancel(true)

            // Schedule a new task with debounce delay
            currentTask = executor.submit {
                try {
                    TimeUnit.MILLISECONDS.sleep(debounceDelay) // Debounce delay
                    updateResults()
                } catch (e: InterruptedException) {
                    // Handle the interruption, if needed
                }
            }
        }

        private fun updateResults() {
            ApplicationManager.getApplication().executeOnPooledThread {
                myOpenedFileText = getOpenedFileText()
                myPatterns = getTextFieldValues()

                resultsList.removeAll()
                val ac = AhoCorasick(myOpenedFileText ?: "", myPatterns)

                // Update the UI on the Event Dispatch Thread (EDT) after computation finishes
                ApplicationManager.getApplication().invokeLater {
                    if (myOpenedFileText != null) {
                        updateResultsList(ac.getMatches())
                    } else {
                        showNoFileSelectedMessage()
                    }
                    refreshComponent(resultsList)
                }
            }
        }

        private fun updateResultsList(matches: Array<MutableList<Int>>) {
            for ((ind, matchPositionsForPatternInd) in matches.withIndex()) {
                for (matchPosition in matchPositionsForPatternInd) {
                    val (line, col) = getLineAndColumn(matchPosition) ?: Pair(0, 0)
                    val strLen = myPatterns[ind].length
                    resultsList.addComponent(createResultPanel(line, col, strLen, matchPosition))
                }
            }
        }

        private fun createResultPanel(line: Int, col: Int, strLen: Int, matchPosition: Int): JBPanel<JBPanel<*>> {
            return JBPanel<JBPanel<*>>().apply {
                border = BorderFactory.createEmptyBorder(0, 13, 0, 5)
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT

                add(JBLabel(createHighlightedLabelText(line, col, strLen)).apply {
                    alignmentX = Component.LEFT_ALIGNMENT
                })
                add(Box.createHorizontalGlue()) // This will push the button to the right
                add(createNavigateButton(matchPosition))
            }
        }

        private fun createNavigateButton(matchPosition: Int): JButton {
            return JButton(AllIcons.General.ArrowRight).apply {
                preferredSize = Dimension(20, 20)
                minimumSize = Dimension(20, 20)
                maximumSize = Dimension(20, 20)
                alignmentX = Component.CENTER_ALIGNMENT
                isBorderPainted = false // Remove the border
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                addActionListener {
                    moveToCharacter(matchPosition)
                }
            }
        }

        private fun showNoFileSelectedMessage() {
            resultsList.addComponent(JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                alignmentX = Component.CENTER_ALIGNMENT
                border = BorderFactory.createEmptyBorder(90, 0, 0, 0)
                add(JBLabel("Please select a file"))
            })
        }

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            val upperSectionPanel = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(JBScrollPane(textFieldListPanel).apply {
                    preferredSize = Dimension(Int.MAX_VALUE, 200)
                    isOverlappingScrollBar = true
                    border = BorderFactory.createEmptyBorder()
                })
            }

            val lowerSectionPanel = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                alignmentX = Component.CENTER_ALIGNMENT

                add(JBPanel<JBPanel<*>>().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    preferredSize = Dimension(Int.MAX_VALUE, 18)
                    maximumSize = Dimension(Int.MAX_VALUE, 100)

                    add(JBLabel("Results:").apply {
                        alignmentX = Component.CENTER_ALIGNMENT
                        font = JBFont.h4()
                    })
                })

                add(JBPanel<JBPanel<*>>().apply {
                    add(JBScrollPane(resultsList).apply {
                        preferredSize = Dimension(300, 200)
                        isOverlappingScrollBar = true
                        border = LineBorder(JBColor.LIGHT_GRAY, 1, true)
                    })
                })
            }

            add(upperSectionPanel)
            add(Box.createRigidArea(Dimension(0, 8)))
            add(lowerSectionPanel)
        }

        private fun JBPanel<JBPanel<*>>.addRemovableTextField(position: Int): JBTextField {
            // Create the panel that will contain the text field and the remove button
            val removableTextField = JBPanel<JBPanel<*>>()
            removableTextField.layout = BoxLayout(removableTextField, BoxLayout.X_AXIS)
            removableTextField.alignmentX = Component.CENTER_ALIGNMENT

            val textField = JBTextField().apply {
                maximumSize = Dimension(200, 30)
            }
            textField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = updateResultsDebounced()
                override fun removeUpdate(e: DocumentEvent) = updateResultsDebounced()
                override fun changedUpdate(e: DocumentEvent) = updateResultsDebounced()
            })

            val removeButton = JButton(AllIcons.General.Remove).apply {
                preferredSize = Dimension(30, 30)
                minimumSize = Dimension(30, 30)
                maximumSize = Dimension(30, 30)
                addActionListener {
                    textFieldListPanel.remove(removableTextField) // Remove the panel containing the text field and the remove button
                    refreshComponent(textFieldListPanel)
                    updateResultsDebounced()
                }
            }

            removableTextField.add(textField)
            removableTextField.add(removeButton)

            add(removableTextField, position)

            return textField
        }

        override fun dispose() {
            executor.shutdownNow()
            Disposer.dispose(myDisposable)
        }
    }
}
