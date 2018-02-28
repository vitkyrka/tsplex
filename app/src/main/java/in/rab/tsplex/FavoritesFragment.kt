package `in`.rab.tsplex

class FavoritesFragment : DatabaseListFragment("favorites", "signs.sv ASC") {
    companion object {
        fun newInstance() = FavoritesFragment()
    }
}