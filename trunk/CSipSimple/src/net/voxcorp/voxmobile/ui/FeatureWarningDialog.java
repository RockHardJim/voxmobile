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

import net.voxcorp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class FeatureWarningDialog extends Activity {

	AlertDialog mDialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voxmobile_feature_warning);
		
		mDialog = new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(getString(R.string.voxmobile_attention))
		.setMessage(getString(R.string.voxmobile_multiple_call_support))
		.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {					
				finish();
            }
        }).create();
		
		mDialog.show();
	}

}
