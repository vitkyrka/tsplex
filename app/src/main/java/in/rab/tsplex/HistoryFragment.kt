package `in`.rab.tsplex

class HistoryFragment : DatabaseListFragment("history", "history.date DESC") {
    companion object {
        fun newInstance() = HistoryFragment()
    }
}