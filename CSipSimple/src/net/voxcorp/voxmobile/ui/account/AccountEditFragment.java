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

package net.voxcorp.voxmobile.ui.account;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragment;
import net.voxcorp.voxmobile.api.SipProfile;

public class AccountEditFragment extends SherlockFragment {

	public static AccountEditFragment newInstance(long profileId) {
		AccountEditFragment f = new AccountEditFragment();

		// Supply index input as an argument.
		Bundle args = new Bundle();
		args.putLong(SipProfile.FIELD_ID, profileId);
		f.setArguments(args);

		return f;
	}
	
	/*
	private OnQuitListener onQuitListener;
	public void setOnQuitListener(OnQuitListener l) {
		onQuitListener = l;
	}
	public interface OnQuitListener {
		public void onQuit();
		public void onShowProfile(long profileId);
	}
	*/
}