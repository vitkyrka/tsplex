package `in`.rab.tsplex

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class ScraperTest {
    @Test
    fun testScraper() {
        val scraper = Scraper(File("foo"))

        val page = scraper.getSignPage(1, forceNetwork = true)
        assertThat(page).isNotNull()

        val url = scraper.parseVideoUrl(page!!)
        assertThat(url).isNotNull()

        assertThat(scraper.isDeadLink(url!!)).isFalse()

        val examples = scraper.parseExamples(page)
        assertThat(examples.isNotEmpty()).isTrue()

        for (example in examples) {
            assertThat(scraper.isDeadLink((example.video))).isFalse()
        }
    }
}
