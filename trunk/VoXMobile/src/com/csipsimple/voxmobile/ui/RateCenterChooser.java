/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import net.voxcorp.R;
import com.csipsimple.voxmobile.service.MobileService;
import com.csipsimple.voxmobile.service.ServiceHelper;
import com.csipsimple.voxmobile.types.DidCity;
import com.csipsimple.voxmobile.types.DidState;
import com.csipsimple.voxmobile.utils.OrderHelper;


public class RateCenterChooser extends ServiceClientBaseActivity {

	/** Dialog Types used in to respond to various MobileService exceptions **/
	private static final int DIALOG_GENERAL_ERROR = 1;
	private static final int DIALOG_UNAUTHORIZED = 2;

	private static String mDialogMsg = "";	
	private static ArrayList<DidState> mStates;
	private static ArrayList<DidCity> mCities;

	private Button mContinue;
	private Spinner mDidState;
	private Spinner mDidCity;
	private int mSelectedState = 0;
	private int mSelectedCity = 0;

	/** Handler that receives messages from the MobileService process **/
	class IncomingHandler extends ServiceClientBaseActivity.IncomingHandler {
	    @Override
	    public void handleMessage(Message msg) {
	    	
	        switch (msg.what) {
	            case ServiceHelper.MSG_SERVICE_RESPONSE:
	                
	                dismissProgressDialog();
	                
	                switch (msg.arg2) {
	                	case ServiceHelper.SUCCESS_GET_DID_STATES:
	                		mStates = (ArrayList<DidState>) msg.obj;
	                		populateDidStates();
                			break;
	                	case ServiceHelper.SUCCESS_GET_DID_CITIES:
	                		mCities = (ArrayList<DidCity>) msg.obj;
	                		populateDidCities();
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
	
	private void populateDidStates() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.voxmobile_list_item, getStates());
		mDidState.setAdapter(adapter);
		
		if (mSelectedState == 0) {
	    	String state = getIntent().getStringExtra(ServiceHelper.DID_STATE_NAME);
	    	
	    	for (int i = 0; i < adapter.getCount(); i++) {
	    		if (adapter.getItem(i).contentEquals(state)) {
	    			mSelectedState = i;
	    			break;
	    		}
	    	}
		}

		mDidState.setSelection(mSelectedState);
	}
	
	private String[] getStates() {
		ArrayList<String> list = new ArrayList<String>();
		
		Iterator<DidState> it = mStates.iterator();
		while (it.hasNext()) {
			list.add(it.next().mDescription);
		}
		
		Collections.sort(list);
		list.add(0, getString(R.string.voxmobile_please_select));
		
		return (String[])list.toArray(new String[list.size()]);
	}

	private void populateDidCities() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.voxmobile_list_item, getCities());
		mDidCity.setAdapter(adapter);
		mDidCity.setEnabled(true);
		
		if (mSelectedCity == 0) {
	    	String city = getIntent().getStringExtra(ServiceHelper.DID_CITY);
	    	
	    	for (int i = 0; i < adapter.getCount(); i++) {
	    		if (adapter.getItem(i).contentEquals(city)) {
	    			mSelectedCity = i;
	    			break;
	    		}
	    	}
		}
		
		mDidCity.setSelection(mSelectedCity);
	}
	
	private String[] getCities() {
		ArrayList<String> list = new ArrayList<String>();
		
		Iterator<DidCity> it = mCities.iterator();
		while (it.hasNext()) {
			list.add(it.next().mDescription);
		}
		
		Collections.sort(list);
		list.add(0, getString(R.string.voxmobile_please_select));
		
		return (String[])list.toArray(new String[list.size()]);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		THIS_FILE = "RateCenterChooser";
		
		setContentView(R.layout.voxmobile_rate_center_chooser);

        mProgress.setMessage(getString(R.string.voxmobile_please_wait));

    	mMessenger = new Messenger(new IncomingHandler());
    	
		// Bind state field
		mDidState = (Spinner)findViewById(R.id.voxmobile_did_state);
		mDidState.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				mSelectedState = findStateId(position);
				if (mSelectedState >= 0) {
					OrderHelper.setStringValue(RateCenterChooser.this, OrderHelper.DID_STATE, mStates.get(mSelectedState).mStateId);
					trackEvent("state_selected", mStates.get(mSelectedState).mStateId, 0);
				}
					
				mContinue.setEnabled(false);		
				mSelectedCity = 0;
				getDidCities(); 
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {}
		});
		
		// Bind city field
		mDidCity = (Spinner)findViewById(R.id.voxmobile_did_city);
		mDidCity.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				
				mSelectedCity = findCityId(position);
				if (mSelectedCity >= 0) {
					OrderHelper.setStringValue(RateCenterChooser.this, OrderHelper.DID_CITY, mCities.get(mSelectedCity).mCityId);
					trackEvent("city_selected", mCities.get(mSelectedCity).mCityId, 0);
				}
					
				mContinue.setEnabled(mSelectedCity >= 0);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {}
		});

		// Bind continue button
		mContinue = (Button)findViewById(R.id.do_voxmobile_rate_center_continue);
		mContinue.setEnabled(false);
		mContinue.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent result = getIntent();
				result.putExtra(ServiceHelper.DID_STATE, mStates.get(mSelectedState).mStateId);
				result.putExtra(ServiceHelper.DID_STATE_NAME, mStates.get(mSelectedState).mDescription);
				result.putExtra(ServiceHelper.DID_CITY, mCities.get(mSelectedCity).mCityId);
				setResult(RESULT_OK, result);
				finish();
			}
        	
        });

		// Bind cancel button
		Button button = (Button)findViewById(R.id.do_voxmobile_rate_center_cancel);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent result = getIntent();
				setResult(RESULT_CANCELED, result);
				finish();
			}
        	
        });
        
        getDidStates();
	}

	@Override
	public void onBackPressed() {
		startActivity(new Intent(this, StartActivity.class));
		super.onBackPressed();
	}
	
	private int findStateId(int position) {
		String selectedState = mDidState.getItemAtPosition(position).toString();
		Iterator<DidState> it = mStates.iterator();
		boolean found = false;
		int i = 0;
		while (it.hasNext()) {
			if (it.next().mDescription.equals(selectedState)) {
				found = true;
				break;
			}
			i++;
		}
		return (found) ? i : -1;
	}

	private int findCityId(int position) {
		String selectedCity = mDidCity.getItemAtPosition(position).toString();
		Iterator<DidCity> it = mCities.iterator();
		boolean found = false;
		int i = 0;
		while (it.hasNext()) {
			if (it.next().mDescription.equals(selectedCity)) {
				found = true;
				break;
			}
			i++;
		}
		return (found) ? i : -1;
	}
	
	private void getDidStates() {
		
		mProgress.show();
			    
		Intent intent = new Intent(this, MobileService.class);
		intent.putExtra(ServiceHelper.METHOD, ServiceHelper.METHOD_GET_DID_STATES);
		startService(intent);
	}
	
	private void getDidCities() {
		
		if (mSelectedState == -1) {
			mDidState.setSelection(0);
			mDidCity.setEnabled(false);
		} else {
			mProgress.show();
		    
			Intent intent = new Intent(this, MobileService.class);
			intent.putExtra(ServiceHelper.METHOD, ServiceHelper.METHOD_GET_DID_CITIES);
			intent.putExtra(ServiceHelper.DID_STATE, mStates.get(mSelectedState).mStateId);
			startService(intent);			
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		AlertDialog dlg = new AlertDialog.Builder(this).create();
		dlg.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
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
}
