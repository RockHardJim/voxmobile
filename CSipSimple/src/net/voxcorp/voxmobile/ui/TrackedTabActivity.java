package net.voxcorp.voxmobile.ui;

import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.VoXSettings;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.TabActivity;
import android.os.Bundle;

public class TrackedTabActivity extends TabActivity {

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

        mTracker.trackPageView("");
    }

    protected void trackPageView(String page) {
        mTracker.trackPageView("");
    }

    protected void trackEvent(String action, String label, int value) {
        mTracker.trackEvent("", action, label, value);
    };
}
