/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import net.voxcorp.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.voxmobile.service.MobileService;
import com.csipsimple.voxmobile.service.ServiceHelper;


public class ProvisionWaitActivity extends ServiceClientBaseActivity implements OnClickListener {

	/** Dialog Types used in to respond to various MobileService exceptions **/
	private static final int DIALOG_GENERAL_ERROR = 1;
	private static final int DIALOG_UNAUTHORIZED = 2;

	private static String mDialogMsg = "";
	private TextView mCheckingMsg;
	
	/** Handler that receives messages from the MobileService process **/
	class IncomingHandler extends ServiceClientBaseActivity.IncomingHandler {
	    @Override
	    public void handleMessage(Message msg) {
	    	
	        switch (msg.what) {
	            case ServiceHelper.MSG_SERVICE_RESPONSE:
	                
	                switch (msg.arg2) {
	                
	                	case ServiceHelper.SUCCESS_IS_PROVISIONED:
	                		finish();
                			break;
                			
	                	case ServiceHelper.START_PROVISIONED_CHECK:
	                		Log.d("ProvisioningWait", "Start is_provisioned_check");
	                		showChecking(true);
	                		break;                			

	                	case ServiceHelper.END_PROVISIONED_CHECK:
	                		Log.d("ProvisioningWait", "End is_provisioned_check");
	                		showChecking(false);
	                		break;                			
                			
	            		case ServiceHelper.ERROR_UNAUTHORIZED:
	            			showDialog(DIALOG_UNAUTHORIZED);
	                		break;
	                		
	            		case ServiceHelper.ERROR_GENERAL:
	            			mDialogMsg = (String)msg.obj;
	            			showDialog(DIALOG_GENERAL_ERROR);
	            			break;
	        		}	
	                
	                break;
	                
	            case ServiceHelper.MSG_GET_STATE:
	                Log.d(THIS_FILE, "Service State: " + msg.arg1);
	                break;
	                
	            default:
	                break;
	        }
	    }
	}
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		THIS_FILE = "ProvisionWaitActivity";
		
		setContentView(R.layout.voxmobile_provision_wait);

    	mMessenger = new Messenger(new IncomingHandler());
		
		// Bind checking message
    	mCheckingMsg = (TextView)findViewById(R.id.TextView01);
    	mCheckingMsg.setVisibility(View.INVISIBLE);

    	// Bind close button
		Button button = (Button)findViewById(R.id.do_provision_wait_continue);
        button.setOnClickListener(this);

        startWaiting();
	}

	@Override
	protected void onStart() {
		registerReceiver(registrationStateReceiver, new IntentFilter(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED));
		super.onStart();
	}
	
	@Override
	protected void onPause() {
		unregisterReceiver(registrationStateReceiver);
		super.onPause();
	}

	private void showChecking(boolean visible) {
		mCheckingMsg.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		mCheckingMsg.postInvalidate();
	}

   	private BroadcastReceiver registrationStateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			finish();
		}
	};
		
	@Override
	protected Dialog onCreateDialog(int id) {
		
		AlertDialog dlg = new AlertDialog.Builder(this).create();
		dlg.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				startWaiting();
			}
		});

		switch (id) {
			
			case DIALOG_UNAUTHORIZED:
				dlg.setTitle(R.string.voxmobile_unauthorized);
				dlg.setMessage(getString(R.string.voxmobile_unauthorized_msg));
				break;

			case DIALOG_GENERAL_ERROR:
				dlg.setTitle(R.string.voxmobile_server_error);
				dlg.setMessage(mDialogMsg);
				break;

		}
		return dlg;
	}

	@Override
	public void onClick(View v) {
		finish();		
	}

	private void startWaiting() {
		Intent intent = new Intent(this, MobileService.class);
		intent.putExtra(ServiceHelper.METHOD, ServiceHelper.METHOD_IS_PROVISIONED);
		startService(intent);		
	}
}
