/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.voxcorp.voxmobile.wizards.impl;

import android.text.InputType;

import net.voxcorp.voxmobile.api.SipConfigManager;
import net.voxcorp.voxmobile.api.SipProfile;
import net.voxcorp.voxmobile.api.SipUri;
import net.voxcorp.voxmobile.utils.PreferencesWrapper;

public class ReachPhones extends SimpleImplementation {
	

	@Override
	protected String getDomain() {
		return "telopar.us";
	}
	
	@Override
	protected String getDefaultName() {
		return "ReachPhones.com";
	}
	
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		
	}

	
	@Override
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		String finalUsername = accountUsername.getText().trim();
		account.acc_id = "\"1-877-617-1017\" <sip:" + SipUri.encodeUser(finalUsername) + "@"+getDomain()+">";
		
		return account;
	}
	
	@Override
	protected boolean canTcp() {
		return false;
	}
	
	@Override
	public boolean needRestart() {
	    return true;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
	    super.setDefaultParams(prefs);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_DNS_SRV, true);
        prefs.addStunServer("stun.telopar.net");
	}
}
