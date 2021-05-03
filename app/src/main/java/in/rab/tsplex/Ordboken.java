package in.rab.tsplex;

import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.app.NavUtils;

public class Ordboken {
    private static Ordboken sInstance = null;
    private final ConnectivityManager mConnMgr;

    private Ordboken(Context context) {
        mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static Ordboken getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Ordboken(context);
        }

        return sInstance;
    }

    static void startWordActivity(Activity activity, String word, String url) {
        Intent intent = new Intent(activity, SignActivity.class);

        intent.putExtra("title", word);
        intent.putExtra("url", url);

        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
    }

    public boolean isOnline() {
        NetworkInfo networkInfo = mConnMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public void initSearchView(Activity activity, Menu menu, String query, Boolean focus) {
        activity.getMenuInflater().inflate(R.menu.main, menu);
    }

    public boolean onOptionsItemSelected(Activity activity, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = NavUtils.getParentActivityIntent(activity);
            if (NavUtils.shouldUpRecreateTask(activity, upIntent)) {
                TaskStackBuilder.create(activity)
                        .addNextIntentWithParentStack(upIntent)
                        .startActivities();
            } else {
                NavUtils.navigateUpTo(activity, upIntent);
            }
            return true;
        } else if (item.getItemId() == R.id.search) {
            Intent intent = new Intent(activity, SearchActivity.class);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else if (item.getItemId() == R.id.reverseSearch) {
            Intent intent = new Intent(activity, ReverseSearchActivity.class);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }

        return false;
    }
}
