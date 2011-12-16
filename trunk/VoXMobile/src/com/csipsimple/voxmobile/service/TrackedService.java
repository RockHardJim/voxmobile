/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.service;

import android.app.IntentService;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public abstract class TrackedService extends IntentService {

	protected GoogleAnalyticsTracker mTracker;
		
	public TrackedService(String name) {
		super(name);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		mTracker = GoogleAnalyticsTracker.getInstance();
		mTracker.start(ServiceHelper.getGoogleAnalyticsAccount(), 
						ServiceHelper.GOOGLE_ANALYTICS_DISPATCH_INTERVAL,
						getApplicationContext());
		mTracker.setDebug(ServiceHelper.GOOGLE_ANALYTICS_DEBUG);
		mTracker.setDryRun(ServiceHelper.GOOGLE_ANALYTICS_DRY_RUN);
	}

	protected void trackPageView(String page) {
		mTracker.trackPageView("/android/csipsimple/webservice/" + page);
	}
	
	protected void trackEvent(String action, String label, int value) {
		mTracker.trackEvent("csipsimple_android_service", action, label, value);
	};

}
