package `in`.rab.tsplex

import android.content.SearchRecentSuggestionsProvider

class SignRecentSuggestionsProvider : SearchRecentSuggestionsProvider() {
    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    companion object {
        const val AUTHORITY = "in.rab.tsplex.SignRecentSuggestionsProvider"
        const val MODE: Int = SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES
    }
}