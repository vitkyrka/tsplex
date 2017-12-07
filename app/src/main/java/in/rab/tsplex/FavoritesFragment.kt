package `in`.rab.tsplex

class FavoritesFragment : LocalListFragment("favorites", "signs.sv ASC") {
    companion object {
        fun newInstance() = FavoritesFragment()
    }
}