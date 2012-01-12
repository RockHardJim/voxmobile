/**
 * Copyright (C) 2012 VoX Communications
 * This file is part of VoX Mobile.
 *
 *  VoX Mobile is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  VoX Mobile is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with VoX Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */
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
