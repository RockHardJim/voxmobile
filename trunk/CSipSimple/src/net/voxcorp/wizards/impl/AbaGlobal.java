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

package net.voxcorp.wizards.impl;

import android.text.InputType;

import net.voxcorp.R;
import net.voxcorp.api.SipConfigManager;
import net.voxcorp.api.SipProfile;
import net.voxcorp.utils.PreferencesWrapper;

public class AbaGlobal extends SimpleImplementation {
	
    /* (non-Javadoc)
     * @see net.voxcorp.wizards.impl.SimpleImplementation#getDomain()
     */
    @Override
    protected String getDomain() {
        return "213.91.64.83:5786";
    }
	
    /* (non-Javadoc)
     * @see net.voxcorp.wizards.impl.SimpleImplementation#getDefaultName()
     */
    @Override
    protected String getDefaultName() {
        return "ABAglobal";
    }

	
	//Customization
	/* (non-Javadoc)
	 * @see net.voxcorp.wizards.impl.SimpleImplementation#fillLayout(net.voxcorp.api.SipProfile)
	 */
	@Override
	public void fillLayout(SipProfile account) {
	    super.fillLayout(account);
        
        accountUsername.setTitle(R.string.w_common_phone_number);
        accountUsername.setDialogTitle(R.string.w_common_phone_number);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
	}
	
	/* (non-Javadoc)
	 * @see net.voxcorp.wizards.impl.SimpleImplementation#getDefaultFieldSummary(java.lang.String)
	 */
	@Override
	public String getDefaultFieldSummary(String fieldName) {
        if(fieldName.equals(USER_NAME)) {
            return parent.getString(R.string.w_common_phone_number_desc);
        }
	    return super.getDefaultFieldSummary(fieldName);
	}
	
	/* (non-Javadoc)
	 * @see net.voxcorp.wizards.impl.SimpleImplementation#buildAccount(net.voxcorp.api.SipProfile)
	 */
	@Override
	public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
        //Ensure registration timeout value
        account.transport = SipProfile.TRANSPORT_TLS;
        return account;
	}

	/* (non-Javadoc)
	 * @see net.voxcorp.wizards.impl.BaseImplementation#setDefaultParams(net.voxcorp.utils.PreferencesWrapper)
	 */
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
	    super.setDefaultParams(prefs);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
	}
    
    /* (non-Javadoc)
     * @see net.voxcorp.wizards.impl.SimpleImplementation#needRestart()
     */
    @Override
    public boolean needRestart() {
        return true;
    }
}
