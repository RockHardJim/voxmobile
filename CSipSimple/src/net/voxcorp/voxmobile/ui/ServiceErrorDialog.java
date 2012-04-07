package net.voxcorp.voxmobile.ui;

import java.util.Iterator;
import java.util.List;

import net.voxcorp.R;
import net.voxcorp.api.SipCallSession;
import net.voxcorp.api.SipManager;
import net.voxcorp.api.SipProfile;
import net.voxcorp.db.DBAdapter;
import net.voxcorp.voxmobile.ui.order.ServicePlansListActivity;
import net.voxcorp.voxmobile.ui.rates.RatesActivity;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.wizards.impl.VoXMobile;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class ServiceErrorDialog extends TrackedActivity {

	private static int mStatusCode;
	private AlertDialog mAlertDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.voxmobile_service_error);
		
		mStatusCode = getIntent().getExtras().getInt(SipManager.ACTION_SIP_NEGATIVE_SIP_RESPONSE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		String msg;
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setTitle(R.string.voxmobile_attention);
		
		if (mStatusCode == SipCallSession.StatusCode.FORBIDDEN) {
			msg = getString(R.string.voxmobile_sip_error_403);

			mAlertDialog.setButton(getString(R.string.callLog_delDialog_yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					SipProfile account = null;
					DBAdapter db = new DBAdapter(ServiceErrorDialog.this);
					db.open();

					List<SipProfile> accounts = db.getListAccounts();
					Iterator<SipProfile> iterator = accounts.iterator();
					while (iterator.hasNext()) {
						SipProfile sp = iterator.next();
						if (VoXMobile.isVoXMobile(sp.proxies)) {
							account = sp;
							if (sp.active) break;
						}
					}
					db.close();

					if (account == null) {
						finish();
						return;
					}

					String body = getString(R.string.voxmobile_invite_message);
					body += String.format(" %s.", account.username);

					Intent sendIntent = new Intent(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.voxmobile_invite_subject));
					sendIntent.putExtra(Intent.EXTRA_TEXT, body);
					sendIntent.setType("text/plain");
					startActivity(Intent.createChooser(sendIntent, getString(R.string.voxmobile_invite_title)));
					trackEvent(Consts.VOX_MOBILE_INVITE_EVENT, "yes", 0);
					finish();
				}
    	    }); 
			mAlertDialog.setButton2(getString(R.string.callLog_delDialog_no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					trackEvent(Consts.VOX_MOBILE_INVITE_EVENT, "no", 0);
					finish();
				}
    	    }); 

		} else if (mStatusCode == SipCallSession.StatusCode.PRECONDITION_FAILURE) {

			msg = getString(R.string.voxmobile_sip_error_580);

			mAlertDialog.setButton(getString(R.string.callLog_delDialog_yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					Intent it = new Intent(ServiceErrorDialog.this, RatesActivity.class);
					it.putExtra(RatesActivity.TRIAL_MODE, true);
					startActivity(it);
					trackEvent(Consts.VOX_MOBILE_TRIAL_DIALCODE, "yes", 0);
					finish();
				}
    	    }); 
			mAlertDialog.setButton2(getString(R.string.callLog_delDialog_no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					trackEvent(Consts.VOX_MOBILE_TRIAL_DIALCODE, "no", 0);
					finish();
				}
    	    }); 

		} else if (mStatusCode == SipCallSession.StatusCode.DECLINE) {

			msg = getString(R.string.voxmobile_sip_error_603);

			mAlertDialog.setButton(getString(R.string.callLog_delDialog_yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(ServiceErrorDialog.this, ServicePlansListActivity.class));
					trackEvent(Consts.VOX_MOBILE_TRIAL_END, "yes", 0);
					finish();
				}
    	    }); 
			mAlertDialog.setButton2(getString(R.string.callLog_delDialog_no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					trackEvent(Consts.VOX_MOBILE_TRIAL_END, "no", 0);
					finish();
				}
    	    }); 

		} else {
			msg = getString(
					R.string.voxmobile_sip_error_general) + "\n\n" +
					mStatusCode + ": " + getSipResponseText();

			mAlertDialog.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
    	    	public void onClick(DialogInterface dialog, int which) {
    	    		finish();
    	    	}
    	    }); 
		}

		mAlertDialog.setMessage(msg);
		mAlertDialog.show();
	}

	@Override
	protected void onPause() {
		mAlertDialog.dismiss();
		super.onPause();
	}

	private String getSipResponseText() {

		switch (mStatusCode) {
		case 404: return "Not Found";
		case 476: return "Unresolvable destination";
		case 484: return "Address Incomplete";
		case 488: return "Not Acceptable Here";

		case 500: return "Internal Server Error";
		case 501: return "Not Implemented";
		case 502: return "Bad Gateway";
		case 503: return "Service Unavailable";
		case 504: return "Server Timeout";
		case 505: return "Version Not Supported";
		case 513: return "Message Too Large";
		case 580: return "Precondition Failure";

		case 600: return "Busy Everywhere";
		case 603: return "Decline";
		case 604: return "Does Not Exist Anywhere";
		case 606: return "Not Acceptable Anywhere";
		}
		return "";
	}
}
