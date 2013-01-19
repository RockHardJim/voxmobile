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
 *  
 *  This file and this file only is released under dual Apache license
 */
package net.voxcorp.api;

import android.os.RemoteException;

public final class SipManager {
	// -------
	// Static constants
	// PERMISSION
	public static final String PERMISSION_USE_SIP = "android.permission.USE_SIP";
	public static final String PERMISSION_CONFIGURE_SIP = "android.permission.CONFIGURE_SIP";
	
	// SERVICE intents

	public static final String INTENT_SIP_CONFIGURATION = "net.voxcorp.service.SipConfiguration";
	public static final String INTENT_SIP_SERVICE = "net.voxcorp.service.SipService";
	public static final String INTENT_SIP_ACCOUNT_ACTIVATE = "net.voxcorp.accounts.activate";
	public static final Object INTENT_GET_ACCOUNTS_LIST = "net.voxcorp.accounts.list";
	
	// -------
	// ACTIONS
	public static final String ACTION_SIP_CALL_UI = "net.voxcorp.phone.action.INCALL";
	public static final String ACTION_SIP_DIALER = "net.voxcorp.phone.action.DIALER";
	public static final String ACTION_SIP_CALLLOG = "net.voxcorp.phone.action.CALLLOG";
	public static final String ACTION_SIP_MESSAGES = "net.voxcorp.phone.action.MESSAGES";
	
	// SERVICE BROADCASTS
	public static final String ACTION_SIP_NEGATIVE_SIP_RESPONSE = "net.voxcorp.service.NEGATIVE_SIP_RESPONSE";
	public static final String ACTION_SIP_CALL_CHANGED = "net.voxcorp.service.CALL_CHANGED";
	public static final String ACTION_SIP_REGISTRATION_CHANGED = "net.voxcorp.service.REGISTRATION_CHANGED";
	public static final String ACTION_SIP_MEDIA_CHANGED = "net.voxcorp.service.MEDIA_CHANGED";
	public static final String ACTION_SIP_ACCOUNT_ACTIVE_CHANGED = "net.voxcorp.service.ACCOUNT_ACTIVE_CHANGED";
	public static final String ACTION_SIP_CAN_BE_STOPPED = "net.voxcorp.service.ACTION_SIP_CAN_BE_STOPPED";
	public static final String ACTION_ZRTP_SHOW_SAS = "net.voxcorp.service.SHOW_SAS";
	public static final String ACTION_VOXMOBILE_BUY_NOW = "net.voxcorp.voxmobile.ui.PROMPT_BUY_NOW";
	public static final String ACTION_VOXMOBILE_PREPAID_ALERT = "net.voxcorp.voxmobile.ui.PREPAID_ALERT";
	public static final String ACTION_ACCOUNT_CHANGED = "net.voxcorp.service.ACCOUNT_CHANGED";
	
	public static final String ACTION_SIP_MESSAGE_RECEIVED = "net.voxcorp.service.MESSAGE_RECEIVED";
	//TODO : message sent?
	public static final String ACTION_SIP_MESSAGE_STATUS = "net.voxcorp.service.MESSAGE_STATUS";
	public static final String ACTION_GET_DRAWABLES = "net.voxcorp.themes.GET_DRAWABLES";
	public static final String ACTION_GET_PHONE_HANDLERS = "net.voxcorp.phone.action.HANDLE_CALL";
	public static final String ACTION_GET_EXTRA_CODECS = "net.voxcorp.codecs.action.REGISTER_CODEC";
	
	public static final String META_LIB_NAME = "lib_name";
	public static final String META_LIB_INIT_FACTORY = "init_factory";
	
	// EXTRAS
	public static final String EXTRA_CALL_INFO = "call_info";
	public static final String EXTRA_ACCOUNT_ID = "acc_id";
	public static final String EXTRA_ACTIVATE = "activate";
	public static final String EXTRA_PROFILES = "profiles";
	
	
	// Constants
	public static final int SUCCESS = 0;
	public static final int ERROR_CURRENT_NETWORK = 10;
	
	public static final int CURRENT_API = 1003;
	
	public static boolean isApiCompatible(ISipService service) {
		if(service != null) {
			try {
				int version = service.getVersion();
				return (Math.floor(version / 1000) == Math.floor(CURRENT_API % 1000));
			} catch (RemoteException e) {
				// We consider this is a bad api version that does not have versionning at all
				return false;
			}
		}
		
		return false;
	}
}
