package com.github.alaindavid001.multistringsearch.ui

import com.github.alaindavid001.multistringsearch.search.SearchManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PatternInputPanel(val project: Project, val resultsPanel: ResultsPanel, val searchManager: SearchManager) {
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
                refreshComponent()
                newTextField.requestFocus()
            }
        })
    }

    fun getWrappedPanelInputPanel() = JBPanel<JBPanel<*>>().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(JBScrollPane(textFieldListPanel).apply {
            preferredSize = Dimension(Int.MAX_VALUE, 200)
            isOverlappingScrollBar = true
            border = BorderFactory.createEmptyBorder()
        })
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
            override fun insertUpdate(e: DocumentEvent) = searchManager.updateResultsDebounced(project, this@PatternInputPanel, resultsPanel)
            override fun removeUpdate(e: DocumentEvent) = searchManager.updateResultsDebounced(project, this@PatternInputPanel, resultsPanel)
            override fun changedUpdate(e: DocumentEvent) = searchManager.updateResultsDebounced(project, this@PatternInputPanel, resultsPanel)
        })

        val removeButton = JButton(AllIcons.General.Remove).apply {
            preferredSize = Dimension(30, 30)
            minimumSize = Dimension(30, 30)
            maximumSize = Dimension(30, 30)
            addActionListener {
                textFieldListPanel.remove(removableTextField) // Remove the panel containing the text field and the remove button
                refreshComponent()
                searchManager.updateResultsDebounced(project, this@PatternInputPanel, resultsPanel)
            }
        }

        removableTextField.add(textField)
        removableTextField.add(removeButton)

        add(removableTextField, position)

        return textField
    }

    fun getTextFieldValues(): List<String> = (0 until textFieldListPanel.componentCount - 1)
        .mapNotNull { textFieldListPanel.getComponent(it) as? JBPanel<*> }
        .mapNotNull { it.getComponent(0) as? JBTextField }
        .map { it.text }

    private fun refreshComponent() {
        textFieldListPanel.revalidate()
        textFieldListPanel.repaint()
    }
}