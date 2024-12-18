package com.github.alaindavid001.multistringsearch.utils

import java.util.*
import kotlin.math.max

/**
 * Implementation of the
 * [Aho-Corasick Algorithm](https://en.wikipedia.org/wiki/Aho–Corasick_algorithm).
 * Finds all occurrences of a set of patterns by their starting index in given text.
 *
 * @param text The text forming the text searched through for matches
 * @param patterns A list of patterns
 */
class AhoCorasick(private val text: String, private val patterns: List<String>, var offset: Int, private val pageSize: Int) {
    private val alphaSet: MutableSet<Char> = text.toHashSet()
    private val hashMap: MutableMap<Char, Int> = HashMap()
    private var alphaSize = 128
    private val trie = mutableListOf<Vertex>()
    private val matches = Array(patterns.size) { mutableListOf<Int>() }
    private val largestPattern: Int = try { patterns.maxOf { it.length } } catch (e: Exception) { 0 }


    init {
        processAlphabet()
        trie.add(Vertex(0))
        for (i in patterns.indices) {
            addPattern(patterns[i], i)
        }
        processSuffixLinks()
        offset = max(0, offset - largestPattern)
        findMatches()
    }

    private fun processAlphabet() {
        for (pattern in patterns) {
            for (char in pattern) {
                alphaSet.add(char)
            }
        }
        alphaSet.add(Char.MIN_VALUE)
        for ((i, char) in alphaSet.withIndex()) {
            hashMap[char] = i
        }
        alphaSize = hashMap.size
    }

    private fun getCode(char: Char) : Int {
        return hashMap[char]!!
    }

    /**
     * Return an array of lists with starting indices of occurrence of a
     * matching pattern from the text searched through.
     *
     * @return array of list storing indices in the text matched against
     */
    fun getMatches() = AhoSearchResult(matches, offset)

    /**
     * Add a given pattern to the Trie.
     *
     * @param word Word representing a pattern
     * @param patIndex Index of the pattern in the patterns list
     */
    private fun addPattern(word: String, patIndex: Int) {
        var curr = 0
        for (char in word) {
            val index = getCode(char)
            if (trie[curr].edge[index] == -1) {
                trie[curr].edge[index] = trie.size
                trie.add(Vertex(trie.size, curr, char))
            }
            curr = trie[curr].edge[index]
        }
        trie[curr].leaf = true
        trie[curr].patIndices.add(patIndex)
    }

    /**
     * Performs a BFS of the vertices in the Trie.
     * For each vertex is suffix link and match link is calculated.
     */
    private fun processSuffixLinks() {
        val queue = LinkedList<Int>()
        queue.addFirst(0)

        while (queue.isNotEmpty()) {
            val curr = queue.pollFirst()
            addSuffixLink(curr)
            for (i in trie[curr].edge.indices) {
                if (trie[curr].edge[i] != -1) {
                    queue.addLast(trie[curr].edge[i])
                }
            }
        }
    }

    /**
     * Calculates the suffix link and the match link for
     * a given vertex.
     *
     * @param ind List index of current vertex
     */
    private fun addSuffixLink(ind: Int) {
        val curr = trie[ind]
        if (curr.p == 0) {
            curr.link = 0
            if (curr.leaf) {
                curr.matchLink = ind
            }
        }
        if (ind != 0 && curr.p != 0) {
            var potentialBest = trie[curr.p].link
            val currChar = getCode(curr.pChar)
            while (true) {
                if (trie[potentialBest].edge[currChar] != -1) {
                    curr.link = trie[potentialBest].edge[currChar]
                    break
                }
                if (potentialBest == 0) {
                    curr.link = 0
                    break
                }
                potentialBest = trie[potentialBest].link
            }
            if (curr.leaf) {
                curr.matchLink = curr.index
            } else {
                curr.matchLink = trie[curr.link].matchLink
            }
        }
    }

    /**
     * Iterates through the given text, with corresponding transitions
     * in the Trie. Whenever matches are found in a vertex and zero or more
     * of its predecessors, the indices are added to the matches list.
     */
    private fun findMatches() {
        var curr = trie[0]
        var numberOfMatches = 0
        var patternsNeeded = true
        while (offset < text.length && patternsNeeded) {
            val ind = getCode(text[offset])
            while (true) {
                if (curr.edge[ind] != -1) {
                    curr = trie[curr.edge[ind]]
                    break
                }
                if (curr.index == 0) {
                    break
                }
                curr = trie[curr.link]
            }

            var matchState = curr
            while (true) {
                matchState = trie[matchState.matchLink]
                if (matchState.index == 0) {
                    break
                }
                for (matchIndex in matchState.patIndices) {
                    matches[matchIndex].add(offset - patterns[matchIndex].length + 1)
                    numberOfMatches++
                    if (numberOfMatches == pageSize) {
                        patternsNeeded = false
                    }
                }
                matchState = trie[matchState.link]
            }
            offset++
        }
//        println("Number of matches: $numberOfMatches")
    }

    /**
     * A class representing a vertex in the Trie.
     *
     * @param index The vertex index in the Trie list
     * @param p Index of parent vertex
     * @param pChar The character representing the transition edge between current edge and its parent
     */
    inner class Vertex(val index: Int, val p: Int = -1, val pChar: Char = Char.MIN_VALUE) {
        val edge = IntArray(alphaSize) { -1 }
        var leaf = false
        var patIndices = mutableListOf<Int>()
        var link = 0
        var matchLink = 0
    }
}

class AhoSearchResult(val matches: Array<MutableList<Int>>, val newOffset: Int)