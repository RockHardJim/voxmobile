package net.voxcorp.voxmobile.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.voxcorp.api.SipManager;
import net.voxcorp.voxmobile.ui.ServiceErrorDialog;

public class ServerErrorBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// pass along the SIP status code to the alert activity
		int status_code = intent.getExtras().getInt(SipManager.ACTION_SIP_NEGATIVE_SIP_RESPONSE);

		Intent i = new Intent(context, ServiceErrorDialog.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(SipManager.ACTION_SIP_NEGATIVE_SIP_RESPONSE, status_code);
		context.startActivity(i);
	}

}
