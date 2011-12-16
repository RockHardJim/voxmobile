/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.ui;

import com.csipsimple.voxmobile.service.ServiceHelper;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.Activity;
import android.os.Bundle;

public class TrackedActivity extends Activity {
	
	protected GoogleAnalyticsTracker mTracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mTracker = GoogleAnalyticsTracker.getInstance();
		mTracker.start(ServiceHelper.getGoogleAnalyticsAccount(), 
						ServiceHelper.GOOGLE_ANALYTICS_DISPATCH_INTERVAL, 
						getApplicationContext());
		mTracker.setDebug(ServiceHelper.GOOGLE_ANALYTICS_DEBUG);
		mTracker.setDryRun(ServiceHelper.GOOGLE_ANALYTICS_DRY_RUN);
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
