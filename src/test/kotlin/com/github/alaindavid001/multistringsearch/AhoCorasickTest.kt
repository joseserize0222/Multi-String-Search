package com.github.alaindavid001.multistringsearch

import com.github.alaindavid001.multistringsearch.utils.AhoCorasick
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class AhoCorasickTest : BasePlatformTestCase() {

    fun testFindMatchesBasic() {
        val text = "ababcababc"
        val patterns = listOf("ab", "bc")
        val ahoCorasick = AhoCorasick(text, patterns)

        val matches = ahoCorasick.getMatches()

        assertEquals("Pattern 'ab' should match at indices 0, 2, 6", listOf(0, 2, 5, 7), matches[0])
        assertEquals("Pattern 'bc' should match at indices 1, 7", listOf(3, 8), matches[1])
    }

    fun testFindMatchesNoMatch() {
        val text = "abcdef"
        val patterns = listOf("xyz", "abc")
        val ahoCorasick = AhoCorasick(text, patterns)

        val matches = ahoCorasick.getMatches()

        assertTrue("Pattern 'xyz' should not match", matches[0].isEmpty())
        assertEquals("Pattern 'abc' should match at index 0", listOf(0), matches[1])
    }

    fun testEmptyText() {
        val text = ""
        val patterns = listOf("abc", "def")
        val ahoCorasick = AhoCorasick(text, patterns)

        val matches = ahoCorasick.getMatches()

        assertTrue("No matches should be found in empty text", matches[0].isEmpty())
        assertTrue("No matches should be found in empty text", matches[1].isEmpty())
    }

    fun testEmptyPatterns() {
        val text = "abcdef"
        val patterns = listOf<String>()
        val ahoCorasick = AhoCorasick(text, patterns)

        val matches = ahoCorasick.getMatches()

        assertTrue("No patterns should yield no matches", matches.isEmpty())
    }

    fun testMultipleMatches() {
        val text = "ababababab"
        val patterns = listOf("ab", "ba")
        val ahoCorasick = AhoCorasick(text, patterns)

        val matches = ahoCorasick.getMatches()

        assertEquals("Pattern 'ab' should match at indices 0, 2, 4, 6, 8", listOf(0, 2, 4, 6, 8), matches[0])
        assertEquals("Pattern 'ba' should match at indices 1, 3, 5, 7, 9", listOf(1, 3, 5, 7), matches[1])
    }

    fun testFindMatchesWithSpecialCharacters() {
        val text = "abc@123₣☃💡abc💬abc🌍"
        val patterns = listOf("abc", "💡", "123", "☃", "🌍", "💬")
        val ahoCorasick = AhoCorasick(text, patterns)

        val matches = ahoCorasick.getMatches()

        assertEquals("Pattern 'abc' should match at indices 0, 11, 16", listOf(0, 11, 16), matches[0])
        assertEquals("Pattern '💡' should match at index 9", listOf(9), matches[1])
        assertEquals("Pattern '123' should match at index 4", listOf(4), matches[2])
        assertEquals("Pattern '☃' should match at index 8", listOf(8), matches[3])
        assertEquals("Pattern '🌍' should match at index 19", listOf(19), matches[4])
        assertEquals("Pattern '💬' should match at index 14", listOf(14), matches[5])
    }

    // Function to simulate getting text from an opened file
    private fun getOpenedFileText(): String? {
        // Access the text of the opened file via the editor
        val editor = myFixture.editor
        return editor?.document?.text
    }

    fun testFindPatternsFromFile() {
        myFixture.configureByFile("Test.kt")

        val fileText = getOpenedFileText()
        val patterns = listOf("main", "run", "import")
        val ahoCorasick = AhoCorasick(fileText ?: "", patterns)

        val matches = ahoCorasick.getMatches()

        assertTrue("Pattern 'main' should be found", matches[0].isNotEmpty())
        assertTrue("Pattern 'run' should be found", matches[1].isNotEmpty())
        assertTrue("Pattern 'import' should be found", matches[2].isNotEmpty())

        assertEquals("Pattern 'main' should match at index 87", listOf(87), matches[0])
        assertEquals("Pattern 'run' should match at index 159", listOf(159), matches[1])
        assertEquals("Pattern 'import' should match at indices 0, 44", listOf(0, 44), matches[2])
    }

    override fun getTestDataPath() = "src/test/testData/ahoCorasickTests"
}
