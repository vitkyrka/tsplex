package `in`.rab.tsplex

class HistoryFragment : LocalListFragment("history", "history.date DESC") {
    companion object {
        fun newInstance() = HistoryFragment()
    }
}