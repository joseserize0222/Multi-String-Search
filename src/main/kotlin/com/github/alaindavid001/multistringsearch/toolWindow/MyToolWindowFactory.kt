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
import org.jetbrains.plugins.notebooks.visualization.r.inlays.table.NULL_VALUE
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit



class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) : Disposable {

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
        private val myDisposable : Disposable = Disposer.newDisposable()

        private val executor: ExecutorService = Executors.newSingleThreadExecutor()
        private var currentTask: Future<*>? = null
        private var debounceDelay: Long = 300 // Adjust debounce delay as needed

        private val project = toolWindow.project
//        private val service = toolWindow.project.service<MyProjectService>()

        private var myOpenedFileText : String? = NULL_VALUE
        private var myPatterns : List<String> = emptyList()

        private val addTextFieldButton = JButton(AllIcons.General.Add).apply {
            preferredSize = Dimension(30,30)
            minimumSize = Dimension(30,30)
            maximumSize = Dimension(30,30)
            alignmentX = Component.CENTER_ALIGNMENT
        }

        private val textFieldListPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.CENTER_ALIGNMENT

            addTextFieldWithButton(0)
            add(addTextFieldButton)
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

        private fun getOpenedFileText(): String? {
            // Get the currently selected editor in the project
            return FileEditorManager.getInstance(project).selectedTextEditor?.document?.text
        }

        private fun getTextFieldValues(): List<String> {
            return (0 until textFieldListPanel.componentCount - 1)
                .mapNotNull { textFieldListPanel.getComponent(it) as? JBPanel<*> }
                .mapNotNull { it.getComponent(0) as? JBTextField }
                .map { it.text }
        }

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
            // Get the currently open editor
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
            val document = editor.document

            // Ensure the line number is within the valid range
            if (lineNumber < 0 || lineNumber >= document.lineCount) return null

            // Get the start and end offsets for the desired line
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val lineEndOffset = document.getLineEndOffset(lineNumber)

            // Return the line text
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
//            return "<html><body>$before<b><font color='red'>$highlighted</font></b>$after</body></html>"
//            return "<html><body>Line ${line+1} Col ${col+1}: $before<b><font color='red'>$highlighted</font></b>$after </body></html>"
            return "<html><body>Line ${line+1} Col ${col+1}: $before<span style='background-color: #FFD54F;'><font color='black'>$highlighted</font></span>$after </body></html>"
//            return "<html><body>${line+1}:${col+1} $before<span style='background-color: #FFD54F;'><font color='black'>$highlighted</font></span>$after </body></html>"
        }

        fun updateResultsDebounced() {
            // Cancel the current task if it's still running
            currentTask?.cancel(true)

            // Schedule a new task with debounce delay
            currentTask = executor.submit {
                try {
                    TimeUnit.MILLISECONDS.sleep(debounceDelay) // Debounce delay
                    updateResults() // Call your updateResults method
                } catch (e: InterruptedException) {
                    // Handle the interruption, if needed
                }
            }
        }

        private fun updateResults(){
            ApplicationManager.getApplication().executeOnPooledThread {
                myOpenedFileText = getOpenedFileText()
                myPatterns = getTextFieldValues()

                println(myOpenedFileText ?: "Please open a file")
                println(myPatterns)

                resultsList.removeAll()
                val ac = AhoCorasick(myOpenedFileText ?: "", myPatterns)

                // Update the UI on the Event Dispatch Thread (EDT) after computation finishes
                ApplicationManager.getApplication().invokeLater {
                    if(myOpenedFileText != null) {

                        val answer = ac.getMatches()
                        for ((ind, i) in answer.withIndex()){
                            println(i)
                            for(j in i){
                                val (line, col) = getLineAndColumn(j) ?: Pair(0,0)
                                val strLen = myPatterns[ind].length

                                resultsList.addComponent(JBPanel<JBPanel<*>>().apply {
                                    border = BorderFactory.createEmptyBorder(0,13,0,5)
                                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                                    alignmentX = Component.LEFT_ALIGNMENT
                                    //                            add(JBLabel("Line ${line+1} Col ${col+1}: ${myPatterns[ind]} ---> ${createHighlightedLabelText(getLineText(line)!!,col,myPatterns[ind].length)}").apply {
                                    //                                alignmentX = Component.LEFT_ALIGNMENT
                                    //                            })
                                    add(JBLabel(createHighlightedLabelText(line, col, strLen)).apply {
                                        alignmentX = Component.LEFT_ALIGNMENT
                                    })
                                    //                            add(JBLabel(createHighlightedLabelText(getLineText(line)!!,col,myPatterns[ind].length)))
                                    add(Box.createHorizontalGlue()) // This will push the button to the right
                                    add(JButton(AllIcons.General.ArrowRight).apply {
                                        preferredSize = Dimension(20, 20)
                                        minimumSize = Dimension(20, 20)
                                        maximumSize = Dimension(20, 20)
                                        alignmentX = Component.CENTER_ALIGNMENT
                                        isBorderPainted = false // Remove the border
                                        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                        addActionListener {
                                            println("Button clicked!" + "   ${myPatterns[ind]}") // Debug action
                                            moveToCharacter(j)
                                        }
                                    })
                                })
                            }
                        }
                    } else {
                        resultsList.addComponent(JBPanel<JBPanel<*>>().apply {
                            layout = BoxLayout(this, BoxLayout.Y_AXIS)
                            alignmentX = Component.CENTER_ALIGNMENT
                            border = BorderFactory.createEmptyBorder(90,0,0,0)
                            add(JBLabel("Please select a file"))
                        })
                    }
                    resultsList.revalidate() // Refresh the layout
                    resultsList.repaint() // Repaint the panel
                }
            }
        }

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            addTextFieldButton.addActionListener {
                val newTextField = textFieldListPanel.addTextFieldWithButton(textFieldListPanel.componentCount-1)
                textFieldListPanel.revalidate() // Refresh the layout
                textFieldListPanel.repaint() // Repaint the panel

                newTextField.requestFocus() // Focus on the newly added text field
            }

            val scrollPane = JBScrollPane(textFieldListPanel).apply {
                preferredSize = Dimension(Int.MAX_VALUE, 200)
                isOverlappingScrollBar = true
                border = BorderFactory.createEmptyBorder() // Set border to empty
            }
            add(JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(scrollPane)
            })

            add(Box.createRigidArea(Dimension(0, 8)))

            val resultsPanel = JBPanel<JBPanel<*>>().apply {
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
                    val myTable = JBScrollPane(resultsList).apply {
                        preferredSize = Dimension(300, 200)
                        isOverlappingScrollBar = true
                        val textFieldBorder = LineBorder(JBColor.LIGHT_GRAY, 1, true) // Create a border similar to JBTextField
                        border = textFieldBorder
                    }
                    add(myTable)
                })
            }
            add(resultsPanel)
//            add(JBPanel<JBPanel<*>>().apply {
//                val label = JBLabel(MyBundle.message("randomLabel", "?"))
//                add(label)
//                add(JButton(MyBundle.message("shuffle")).apply {
//                    addActionListener {
//                        label.text = MyBundle.message("randomLabel", service.getRandomNumber())
//                    }
//                })
//            })
        }

        private fun JBPanel<JBPanel<*>>.addTextFieldWithButton(position: Int): JBTextField {
            // Create the panel that will contain the text field and the icon button
            val panel = JBPanel<JBPanel<*>>()
            panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
            panel.alignmentX = Component.CENTER_ALIGNMENT

            // Create and add the text field
            val textField = JBTextField().apply {
                maximumSize = Dimension(200, 30)
            }
            textField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = updateResultsDebounced()
                override fun removeUpdate(e: DocumentEvent) = updateResultsDebounced()
                override fun changedUpdate(e: DocumentEvent) = updateResultsDebounced()
            })
            panel.add(textField)

            // Create and add the icon button
            val iconButton = JButton(AllIcons.General.Remove).apply {
                preferredSize = Dimension(30, 30)
                minimumSize = Dimension(30, 30)
                maximumSize = Dimension(30, 30)
                addActionListener {
                    this@addTextFieldWithButton.remove(panel) // Remove the entire panel containing the text field and button
                    this@addTextFieldWithButton.revalidate() // Refresh the layout
                    this@addTextFieldWithButton.repaint() // Repaint the panel
                    updateResultsDebounced()
                }
            }
            panel.add(iconButton)

            add(panel, position)

            return textField
        }

        override fun dispose() {
            executor.shutdownNow()
            Disposer.dispose(myDisposable)
        }
    }
}
