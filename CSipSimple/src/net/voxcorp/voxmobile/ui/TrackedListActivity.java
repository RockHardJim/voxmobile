package net.voxcorp.voxmobile.ui;

import android.app.ListActivity;
import android.os.Bundle;

import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.VoXSettings;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class TrackedListActivity extends ListActivity {

    protected GoogleAnalyticsTracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTracker = GoogleAnalyticsTracker.getInstance();
        mTracker.start(VoXSettings.getGoogleAnalyticsAccount(),
                        Consts.GOOGLE_ANALYTICS_DISPATCH_INTERVAL,
                        getApplicationContext());
        mTracker.setDebug(Consts.GOOGLE_ANALYTICS_DEBUG);
        mTracker.setDryRun(Consts.GOOGLE_ANALYTICS_DRY_RUN);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTracker.trackPageView("/android/csipsimple/" + this.getLocalClassName());
    }

    protected void trackPageView(String page) {
        mTracker.trackPageView("/android/csipsimple/" + page);
    }

    protected void trackEvent(String action, String label, int value) {
        mTracker.trackEvent("/android/csipsimple/" + this.getLocalClassName(), action, label, value);
    };
	
}
