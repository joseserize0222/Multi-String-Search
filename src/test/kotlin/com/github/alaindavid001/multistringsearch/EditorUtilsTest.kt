package com.github.alaindavid001.multistringsearch

import com.github.alaindavid001.multistringsearch.search.SearchManager
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import java.io.File

@TestDataPath("\$CONTENT_ROOT/src/test/testData/")
class EditorUtilsTest : BasePlatformTestCase() {
    private val searchManager = SearchManager()

    fun `test getLineText`() {
        val file = myFixture.configureByFile("MySuffixArray.cpp")
        val reader = File("src/test/testData/EditorUtilsTest/MySuffixArray.cpp")
        for ((i, line) in reader.readLines().withIndex()) {
            TestCase.assertEquals(line, (searchManager.getLineText(file.project, i)))
        }
    }

    fun `test getLineAndColumn`() {
        val file = myFixture.configureByFile("MySuffixArray.cpp")
        val reader = File("src/test/testData/EditorUtilsTest/MySuffixArray.cpp")
        var offset = 0
        for ((i, line) in reader.readLines().withIndex()) {
            for (j in line.indices) {
                TestCase.assertEquals((i to j), searchManager.getLineAndColumn(file.project, offset))
                offset += 1
            }
            offset += 1
        }
    }

    override fun getTestDataPath() = "src/test/testData/EditorUtilsTest"
}
