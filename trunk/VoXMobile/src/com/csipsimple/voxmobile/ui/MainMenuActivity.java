/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.voxcorp.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Filter;
import com.csipsimple.service.SipService;
import com.csipsimple.voxmobile.service.MobileService;
import com.csipsimple.voxmobile.service.ServiceHelper;
import com.csipsimple.voxmobile.types.SipUserInfo;
import com.csipsimple.voxmobile.utils.CodecHelper;
import com.csipsimple.wizards.impl.VoXMobile;

public class MainMenuActivity extends ServiceClientBaseActivity implements OnClickListener {

	private static final int ACTION_BASE = 0;
	private static final int CHOOSE_DID = ACTION_BASE + 1;
	
	/** Dialog Types used in to respond to various MobileService exceptions **/
	private static final int DIALOG_GENERAL_ERROR = 1;
	private static final int DIALOG_DUPLICATE_ACCOUNT = 2;

	private static ArrayList<String> mList = new ArrayList<String>();
	
	private static String mUuid = "";
	private static String mSipUsername = null;
	private static String mDialogMsg = "";
	private TextView mG729;
	private boolean mG729Changed = false;
	private ImageView mG729Indicator;

	private ISipService sipService = null;
	
	private ServiceConnection restartServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder aService) {
			sipService = ISipService.Stub.asInterface(aService);
		}
	};
	
	/** Handler that receives messages from the MobileService process **/
	class IncomingHandler extends ServiceClientBaseActivity.IncomingHandler {
		@Override
	    public void handleMessage(Message msg) {
	    	
	        switch (msg.what) {
	            case ServiceHelper.MSG_SERVICE_RESPONSE:
	                
	                dismissProgressDialog();
	                
	                switch (msg.arg2) {
	                	case ServiceHelper.SUCCESS_GET_SIP_USERS:
	                		mList.clear();
	                		mList.addAll((ArrayList<String>)(msg.obj));
	                		showAvailableSipUsers();
                			break;
	                	case ServiceHelper.SUCCESS_GET_SIP_USER_INFO:
	                		SipUserInfo sipUserInfo = (SipUserInfo)(msg.obj);
	                		createAccount(sipUserInfo);
	                		break;
	            		case ServiceHelper.ERROR_UNAUTHORIZED:
                			startActivity(new Intent(MainMenuActivity.this, StartActivity.class));
            				setResult(RESULT_OK);
                			finish();
	                		break;
	            		case ServiceHelper.ERROR_GENERAL:
	            			mDialogMsg = (String)msg.obj;
	            			showDialog(DIALOG_GENERAL_ERROR);
	        				setResult(RESULT_OK);
	            			finish();
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
		
		THIS_FILE = "MainMenuActivity";
		
		setContentView(R.layout.voxmobile_main_menu);
		
		mUuid = VoXMobile.getUuid(this);
		
    	mMessenger = new Messenger(new IncomingHandler());	

		// Bind setup button
		LinearLayout add_row = (LinearLayout)findViewById(R.id.do_voxmobile_setup_phone);
        add_row.setOnClickListener(this);

		// Bind G.729 button
		add_row = (LinearLayout)findViewById(R.id.do_voxmobile_g729);
        add_row.setOnClickListener(this);

        // Bind logout button
		add_row = (LinearLayout)findViewById(R.id.do_voxmobile_logout);
        add_row.setOnClickListener(this);
        
        // Get G.729 text view
        mG729Indicator = (ImageView)findViewById(R.id.bar_onoff);
        mG729 = (TextView)findViewById(R.id.voxmobile_g729);
        if (CodecHelper.isG729Disabled(this)) {
        	mG729.setText(R.string.voxmobile_enable_g729);
        	mG729.setTextColor(Color.GREEN);
			mG729Indicator.setImageResource(R.drawable.ic_indicator_off);
        } else {
        	mG729.setText(R.string.voxmobile_disable_g729);
        	mG729.setTextColor(Color.WHITE);
			mG729Indicator.setImageResource(R.drawable.ic_indicator_on);
        }
        
		//Attach to the service
		Intent serviceIntent =  new Intent(this, SipService.class);
		try {
			bindService(serviceIntent, restartServiceConnection, 0);
			startService(serviceIntent);
		} catch(Exception e) {}
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		
		if (mG729Changed && (sipService !=null) ) {
			try {
				sipService.askThreadedRestart();
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Impossible to restart sip", e);
			}
		}
		
		sipService = null;
		if(restartServiceConnection != null) {
			try {
				unbindService(restartServiceConnection);
			}catch(Exception e) {
				//Nothing to do service was just not binded
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.do_voxmobile_setup_phone:
				trackEvent("setup_phone", "clicked", 0);
				
				mProgress.setMessage(getString(R.string.voxmobile_please_wait));
				mProgress.show();
					    
				mSipUsername = null;
				
				Intent intent = new Intent(this, MobileService.class);
				intent.putExtra(ServiceHelper.METHOD, ServiceHelper.METHOD_GET_SIP_USERS);
				intent.putExtra(ServiceHelper.UUID, mUuid);
				startService(intent);
				break;
			case R.id.do_voxmobile_g729:
				if (CodecHelper.isG729Disabled(this)) {
					final TextView message = new TextView(this);
					final SpannableString s = new SpannableString(getString(R.string.this_codec_is_not_free) + "http://www.synapseglobal.com/g729_codec_license.html");
					Linkify.addLinks(s, Linkify.WEB_URLS);
					message.setText(s);
					message.setMovementMethod(LinkMovementMethod.getInstance());
					message.setPadding(10, 10, 10, 10);
					
					new AlertDialog.Builder(this)
						.setTitle(R.string.warning)
						.setView(message)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								if (CodecHelper.enableG729(MainMenuActivity.this)) {
									mG729Changed = true;
									mG729.setText(R.string.voxmobile_disable_g729);
									mG729.setTextColor(Color.WHITE);
									mG729Indicator.setImageResource(R.drawable.ic_indicator_on);
									trackEvent("codec", "g729", 1);
								}
							}
						})
						.setNegativeButton(R.string.cancel, null)
						.show();
				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					        switch (which){
					        case DialogInterface.BUTTON_POSITIVE:
								if (CodecHelper.disbleG729(MainMenuActivity.this)) {
									mG729Changed = true;
									mG729.setText(R.string.voxmobile_enable_g729);
									mG729.setTextColor(Color.GREEN);
									mG729Indicator.setImageResource(R.drawable.ic_indicator_off);
									trackEvent("codec", "g729", 0);
								}
					            break;

					        case DialogInterface.BUTTON_NEGATIVE:
					            break;
					        }
					    }
					};
					
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.voxmobile_confirm_disable_g729_msg)
						.setPositiveButton(R.string.voxmobile_yes, dialogClickListener)
					    .setNegativeButton(R.string.voxmobile_no, dialogClickListener);
					
					AlertDialog dlg = builder.create();
					dlg.setTitle(R.string.voxmobile_confirm_disable_g729);
					dlg.show();
				}
				break;
			case R.id.do_voxmobile_logout:
				trackEvent("logout", "clicked", 0);
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        switch (which){
				        case DialogInterface.BUTTON_POSITIVE:
							// Remove UUID and send the user to the start activity
							VoXMobile.clearUuid(MainMenuActivity.this);
							startActivity(new Intent(MainMenuActivity.this, StartActivity.class));
							setResult(RESULT_OK);
							finish();
				            break;

				        case DialogInterface.BUTTON_NEGATIVE:
				            break;
				        }
				    }
				};
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.voxmobile_confirm_logout)
					.setPositiveButton(R.string.voxmobile_yes, dialogClickListener)
				    .setNegativeButton(R.string.voxmobile_no, dialogClickListener);
				
				AlertDialog dlg = builder.create();
				dlg.setTitle(R.string.voxmobile_logout);
				dlg.show();

				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode){
		case CHOOSE_DID:
			if(resultCode == RESULT_OK) {
				if(data != null) {
					mSipUsername = data.getStringExtra("did");
					if (!accountExists(mSipUsername)) {
						Intent intent = new Intent(this, MobileService.class);
						intent.putExtra(ServiceHelper.METHOD, ServiceHelper.METHOD_GET_SIP_USER_INFO);
						intent.putExtra(ServiceHelper.UUID, mUuid);
						intent.putExtra(ServiceHelper.SIP_USERNAME, mSipUsername);
						startService(intent);
					}
				}
			}
			break;
		}
	}
	
	private boolean accountExists(String sipUsername)  {
		List<SipProfile> accountsList;
		
		DBAdapter database = new DBAdapter(this);
    	database.open();
		accountsList = database.getListAccounts();
		database.close();
		
		boolean found = false;
		
		Iterator<SipProfile> iList = accountsList.iterator();
		while (iList.hasNext()) {
			SipProfile sp = iList.next();

	        if (!VoXMobile.isVoXMobile(sp.proxies))
	        	continue;
			
	        found = sp.username.equals(sipUsername);
	        if (found) {
	        	mDialogMsg = sp.display_name;
	    		showDialog(DIALOG_DUPLICATE_ACCOUNT);
	        	break;
	        }
		}

		return found;
	}
	
	private void createAccount(SipUserInfo sipUserInfo) {
			
		VoXMobile wizard = new VoXMobile();
		SipProfile account = new SipProfile();
		
		wizard.buildAccount(account, sipUserInfo);
		DBAdapter database = new DBAdapter(this);
		database.open();

		account.id = (int) database.insertAccount(account);
		account.active = true;
		
		List<Filter> filters = wizard.getDefaultFilters(account);
		if(filters != null && filters.size() > 0 ) {
			for(Filter filter : filters) {
				// Ensure the correct id if not done by the wizard
				filter.account = account.id;
				database.insertFilter(filter);
			}
		}
		database.setAccountActive(account.id, true);
		database.close();

		Intent publishIntent = new Intent(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED);
		publishIntent.putExtra(SipManager.EXTRA_ACCOUNT_ID, account.id);
		publishIntent.putExtra(SipManager.EXTRA_ACTIVATE, true);
		this.sendBroadcast(publishIntent);
		
		finish();
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		AlertDialog dlg = new AlertDialog.Builder(this).create();
		dlg.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			   public void onClick(DialogInterface dialog, int which) {}
			});
		
		switch (id) {

			case DIALOG_DUPLICATE_ACCOUNT:
				trackEvent("setup_phone_failure", "duplicate_account", 0);
				dlg.setTitle(R.string.voxmobile_duplicate_account);
				dlg.setMessage(getString(R.string.voxmobile_duplicate_account_message) + " " + mDialogMsg);
				break;
				
			case DIALOG_GENERAL_ERROR:
				trackEvent("setup_phone_failure", "general_error", 0);
				dlg.setTitle(R.string.voxmobile_server_error);
				dlg.setMessage(mDialogMsg);
				break;
		}
		return dlg;
	}

	protected void showAvailableSipUsers() {
		Bundle b = new Bundle();
		b.putStringArrayList("values", mList);

		Intent i = new Intent(MainMenuActivity.this, DIDChooserActivity.class);
		i.putExtra("data", b);
		
		startActivityForResult(i, CHOOSE_DID);
	}
	
}
