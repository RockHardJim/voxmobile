/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;

import net.voxcorp.R;
import com.csipsimple.voxmobile.service.MobileService;
import com.csipsimple.voxmobile.service.ServiceHelper;

public class LoginActivity extends ServiceClientBaseActivity implements OnClickListener {

	/** Dialog Types used in to respond to various MobileService exceptions **/
	private static final int DIALOG_GENERAL_ERROR = 1;
	private static final int DIALOG_UNAUTHORIZED = 2;
	private static final int DIALOG_MISSING_UID = 3;
	private static final int DIALOG_MISSING_PWD = 4;

	private EditText mUid;
	private EditText mPwd;
	
	private static String mDialogMsg = "";
	
	/** Handler that receives messages from the MobileService process **/
	class IncomingHandler extends ServiceClientBaseActivity.IncomingHandler {
	    @Override
	    public void handleMessage(Message msg) {
	    	
	        switch (msg.what) {
	            case ServiceHelper.MSG_SERVICE_RESPONSE:
	                
	                dismissProgressDialog();
	                
	                switch (msg.arg2) {
	                	case ServiceHelper.SUCCESS_GET_AUTH_UUID:
                			startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
                			finish();
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
	                
	                if (msg.arg1 == ServiceHelper.STATE_RUNNING)
	                	mProgress.show();
	                
	                break;
	                
	            default:
	                break;
	        }
	    }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		THIS_FILE = "LoginActivity";
		
		setContentView(R.layout.voxmobile_login);

        mProgress.setMessage(getString(R.string.voxmobile_authenticating));

    	mMessenger = new Messenger(new IncomingHandler());
		
		// Bind User name field
		mUid = (EditText)findViewById(R.id.voxmobile_login_uid);

		// Bind password field
		mPwd = (EditText)findViewById(R.id.voxmobile_login_pwd);

		// Bind login button
		LinearLayout loginButton = (LinearLayout)findViewById(R.id.do_voxmobile_login);
        loginButton.setOnClickListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		startActivity(new Intent(this, StartActivity.class));
		super.onBackPressed();
	}

	@Override
	public void onClick(View v) {		
		switch (v.getId()) {
			case R.id.do_voxmobile_login:
				trackEvent("login", "clicked", 0);
				getAuthUuid();
				break;
		}
	}

	private void getAuthUuid() {
		
		if (mUid.getText().toString().length() < 3) {
			showDialog(DIALOG_MISSING_UID);
			return;
		}

		if (mPwd.getText().toString().length() < 3) {
			showDialog(DIALOG_MISSING_PWD);
			return;
		}

		mProgress.setMessage(getString(R.string.voxmobile_authenticating));
		mProgress.show();
			    
		Intent intent = new Intent(this, MobileService.class);
		intent.putExtra(ServiceHelper.METHOD, ServiceHelper.METHOD_GET_AUTH_UUID);
		intent.putExtra(ServiceHelper.LOGIN_UID, mUid.getText().toString());
		intent.putExtra(ServiceHelper.LOGIN_PWD, mPwd.getText().toString());
		startService(intent);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		AlertDialog dlg = new AlertDialog.Builder(this).create();
		dlg.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});

		switch (id) {
			case DIALOG_MISSING_UID:
				trackEvent("login_failure", "missing_uid", 0);
				dlg.setTitle(R.string.voxmobile_attention);
				dlg.setMessage(getString(R.string.voxmobile_login_missing_uid));
				break;

			case DIALOG_MISSING_PWD:
				trackEvent("login_failure", "missing_pwd", 0);
				dlg.setTitle(R.string.voxmobile_attention);
				dlg.setMessage(getString(R.string.voxmobile_login_missing_pwd));
				break;
			
			case DIALOG_UNAUTHORIZED:
				trackEvent("login_failure", "unauthorized", 0);
				dlg.setTitle(R.string.voxmobile_unauthorized);
				dlg.setMessage(getString(R.string.voxmobile_unauthorized_msg));
				break;

			case DIALOG_GENERAL_ERROR:
				trackEvent("login_failure", "general_error", 0);
				dlg.setTitle(R.string.voxmobile_server_error);
				dlg.setMessage(mDialogMsg);
				break;

		}
		return dlg;
	}
}
