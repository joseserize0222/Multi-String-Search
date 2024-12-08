package com.github.alaindavid001.multistringsearch.ui

import com.github.alaindavid001.multistringsearch.MyBundle
import com.github.alaindavid001.multistringsearch.search.SearchManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.util.addComponent
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBFont
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.border.LineBorder
import com.intellij.ui.components.JBScrollPane
import javax.swing.JScrollBar

class ResultsPanel(private val project: Project, private val searchManager: SearchManager) {
    private val resultsList = JBPanel<JBPanel<*>>().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.CENTER_ALIGNMENT
        alignmentY = Component.CENTER_ALIGNMENT
        border = BorderFactory.createEmptyBorder(0,0,10,0)
        showNoFileSelectedMessage(this)
    }
    private var resultsScrollPane = JBScrollPane()
    private var allMatches: List<MatchData> = emptyList()
    private var currentPage: Int = 0
    private val pageSize: Int = 10

    fun getWrappedResultsPanel() = JBPanel<JBPanel<*>>().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.CENTER_ALIGNMENT

        add(JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            preferredSize = Dimension(Int.MAX_VALUE, 18)
            maximumSize = Dimension(Int.MAX_VALUE, 100)

            add(JBLabel(MyBundle.message("results")).apply {
                alignmentX = Component.CENTER_ALIGNMENT
                font = JBFont.h4()
            })
        })

        add(JBPanel<JBPanel<*>>().apply {
            resultsScrollPane = JBScrollPane(resultsList).apply {
                preferredSize = Dimension(300, 200)
                isOverlappingScrollBar = true
                border = LineBorder(JBColor.LIGHT_GRAY, 1, true)
            }
            add(resultsScrollPane)
        })
        addScrollListener()
    }

    fun updateResultsList(matches: Array<MutableList<Int>>) {
        allMatches = matches.flatMapIndexed { patternIndex, positions ->
            positions.map { position ->
                val (line, col) = searchManager.getLineAndColumn(project, position) ?: Pair(0, 0)
                MatchData(line, col, searchManager.myPatterns[patternIndex].length, position)
            }
        }
        println(allMatches.size)

        currentPage = 0
        clearResults()
        loadNextPage()
    }

    private fun loadNextPage() {
        val startIndex = currentPage * pageSize
        val endIndex = (startIndex + pageSize).coerceAtMost(allMatches.size)
        if(startIndex>endIndex)return
        val nextPage = allMatches.subList(startIndex, endIndex)
        println(currentPage)
        for (match in nextPage) {
            resultsList.addComponent(createResultPanel(match.line, match.col, match.strLen, match.matchPosition))
//            println("Pepe ${match.line} ${match.col} ${match.matchPosition}")
        }

        currentPage++
        refreshComponent()
    }

    private fun createResultPanel(line: Int, col: Int, strLen: Int, matchPosition: Int): JBPanel<JBPanel<*>> {
        return JBPanel<JBPanel<*>>().apply {
            border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT

            add(createNavigateButton(matchPosition))
            add(JBLabel(createHighlightedLabelText(line, col, strLen)).apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
        }
    }

    private fun addScrollListener() {
//        val scrollPane = JBScrollPane(resultsList)
        println("AYYYYYYYYYYYYYYYY NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO")
        resultsScrollPane.verticalScrollBar.addAdjustmentListener { e ->
            if (!e.valueIsAdjusting) {
                val scrollBar = e.source as JScrollBar
                if (scrollBar.value + scrollBar.visibleAmount >= scrollBar.maximum) {
                    loadNextPage()
                }
            }
        }
    }

    private fun createHighlightedLabelText(line: Int, col: Int, strLen: Int): String {
        val fullText = searchManager.getLineText(project, line) ?: return "Error while getting line"

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

    private fun createNavigateButton(matchPosition: Int): JButton {
        return JButton(AllIcons.General.ArrowRight).apply {
            preferredSize = Dimension(20, 20)
            minimumSize = Dimension(20, 20)
            maximumSize = Dimension(20, 20)
            alignmentX = Component.CENTER_ALIGNMENT
            alignmentY = Component.CENTER_ALIGNMENT
            isBorderPainted = false // Remove the border
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            addActionListener {
                searchManager.moveToCharacter(project, matchPosition)
            }
        }
    }

    fun showNoFileSelectedMessage(targetPanel: JBPanel<JBPanel<*>> = resultsList) {
        targetPanel.addComponent(JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.CENTER_ALIGNMENT
            border = BorderFactory.createEmptyBorder(90, 0, 0, 0)
            add(JBLabel(MyBundle.message("selectAFile")))
        })
    }

    fun clearResults() {
        resultsList.removeAll()
    }

    fun refreshComponent() {
        resultsList.revalidate()
        resultsList.repaint()
    }
}

data class MatchData(val line: Int, val col: Int, val strLen: Int, val matchPosition: Int)