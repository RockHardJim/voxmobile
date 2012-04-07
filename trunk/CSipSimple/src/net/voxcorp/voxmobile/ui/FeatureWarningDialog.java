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
