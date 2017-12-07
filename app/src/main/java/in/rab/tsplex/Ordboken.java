package in.rab.tsplex;

import android.app.Activity;
import android.app.SearchManager;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

public class Ordboken {
    private static Ordboken sInstance = null;
    private final ConnectivityManager mConnMgr;

    private Ordboken(Context context) {
        mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean isOnline() {
        NetworkInfo networkInfo = mConnMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static Ordboken getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Ordboken(context);
        }

        return sInstance;
    }

    public SearchView initSearchView(Activity activity, Menu menu, String query, Boolean focus) {
        activity.getMenuInflater().inflate(R.menu.main, menu);

        SearchManager searchManager = (SearchManager) activity
                .getSystemService(Context.SEARCH_SERVICE);
        MenuItem item = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) item.getActionView();

        searchView.setQueryRefinementEnabled(true);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(activity,
                SearchActivity.class)));

        return searchView;
    }

    public boolean onOptionsItemSelected(Activity activity, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = NavUtils.getParentActivityIntent(activity);
            if (NavUtils.shouldUpRecreateTask(activity, upIntent)) {
                TaskStackBuilder.create(activity)
                        .addNextIntentWithParentStack(upIntent)
                        .startActivities();
            } else {
                NavUtils.navigateUpFromSameTask(activity);
            }
            return true;
        }

        return false;
    }

    static void startWordActivity(Activity activity, String word, String url) {
        Intent intent = new Intent(activity, SignActivity.class);

        intent.putExtra("title", word);
        intent.putExtra("url", url);

        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
    }
}
