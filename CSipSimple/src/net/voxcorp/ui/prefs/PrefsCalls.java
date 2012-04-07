/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.voxcorp.ui.prefs;

import net.voxcorp.R;
import net.voxcorp.api.SipConfigManager;
import net.voxcorp.utils.CustomDistribution;
import net.voxcorp.utils.PreferencesWrapper;
import net.voxcorp.voxmobile.ui.FeatureWarningDialog;
import android.content.Intent;
import android.content.SharedPreferences;


public class PrefsCalls extends GenericPrefs {
	private boolean mWarningDisplayed = false;

	@Override
	protected int getXmlPreferences() {
		return R.xml.prefs_calls;
	}

	@Override
	protected void afterBuildPrefs() {
		super.afterBuildPrefs();
		PreferencesWrapper pfw = new PreferencesWrapper(this);
		if(!pfw.isAdvancedUser()) {
			
		}
		
		if(CustomDistribution.forceNoMultipleCalls()) {
			hidePreference(null, SipConfigManager.SUPPORT_MULTIPLE_CALLS);
		}
	}
	
	
	@Override
	protected void updateDescriptions() {
		
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		super.onSharedPreferenceChanged(sharedPreferences, key);
		
		if (!key.equals("support_multiple_calls")) {
			return;
		}
		
		if (!sharedPreferences.getBoolean(key, false)) {
			return;
		}

		if (!mWarningDisplayed) {
			mWarningDisplayed = true;
			
    		Intent intent = new Intent(this, FeatureWarningDialog.class);
        	startActivity(intent);
		}
	}
	
}
