package net.voxcorp.voxmobile.ui.order;

import java.util.ArrayList;

import net.voxcorp.R;

import org.springframework.http.HttpStatus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.provider.DBContract.DIDCityContract;
import net.voxcorp.voxmobile.provider.DBContract.DIDStateContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.types.DIDCity;
import net.voxcorp.voxmobile.types.DIDState;
import net.voxcorp.voxmobile.ui.TrackedActivity;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.OrderHelper;

public class DIDSelectionActivity extends TrackedActivity {

	private static final String THIS_FILE = "DIDSelectionActivity";

	private ProgressDialog mProgressDialog = null;
	private Button mContinue;
	private Spinner mState;
	private Spinner mCity;
	private DIDState[] mStates;
	private DIDCity[] mCities;
	
	private int mSelectedState = 0;
	private int mSelectedCity = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "DIDSelectionActivity.onCreate()");
		
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voxmobile_rate_center);
		setTitle(R.string.voxmobile_rate_center);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_voxmobile_info);
		
		// set up cancel button
		Button button = (Button) findViewById(R.id.Button01);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

        // Bind continue button
        mContinue = (Button)findViewById(R.id.Button02);
        mContinue.setEnabled(false);
		mContinue.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(DIDSelectionActivity.this, OrderSummaryActivity.class);
				startActivity(intent);
				finish();
			}
		});

		mState = (Spinner)findViewById(R.id.Spinner01);
		mState.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				mSelectedState = position;
				
				if (position > 0) {
					String state = mStates[mSelectedState].state_id;
					if (state != "") {
						trackEvent("state_selected", state, 0);
						if (!state.equals(OrderHelper.getStringValue(DIDSelectionActivity.this, OrderHelper.DID_STATE))) {
							OrderHelper.setStringValue(DIDSelectionActivity.this, OrderHelper.DID_STATE, state);
							OrderHelper.setStringValue(DIDSelectionActivity.this, OrderHelper.DID_CITY, "");
							mSelectedCity = 0;
							mCities = new DIDCity[0];
						}
						populateDidCities();
					}
				} else {
					OrderHelper.setStringValue(DIDSelectionActivity.this, OrderHelper.DID_CITY, "");
					mSelectedCity = 0;

					DIDCity city = new DIDCity();
					city.state_id = "";
					city.city_id = "";
					city.description = getString(R.string.voxmobile_please_select);
					city.did_count = 0;

					mCities = new DIDCity[1];
					mCities[0] = city;
					
			    	ArrayAdapter<DIDCity> adapter = new ArrayAdapter<DIDCity>(DIDSelectionActivity.this, 
			    			R.layout.voxmobile_simple_list_item, 
			    			mCities);
			    	mCity.setAdapter(adapter);

			    	mCity.setEnabled(false);
				}
				
				mContinue.setEnabled(mSelectedState > 0 && mSelectedCity > 0);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parentView) {}
		});

		mCity = (Spinner)findViewById(R.id.Spinner02);
		mCity.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				mSelectedCity = position;
				
				if (position > 0) {
					String city = mCities[mSelectedCity].city_id;
					if (city != "") {
						trackEvent("city_selected", city, 0);
						OrderHelper.setStringValue(DIDSelectionActivity.this, OrderHelper.DID_CITY, city);
					}
				}
				
				mContinue.setEnabled(mSelectedState > 0 && mSelectedCity > 0);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parentView) {}
		});

		trackPageView("order/rate_center");
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerContentObservers();
		populateDidStates();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		unregisterContentObservers();
		super.onStop();
	}

	private void showProgressDialog() {
		if (isFinishing() || mProgressDialog != null) return;
		mProgressDialog = ProgressDialog.show(this, "", getString(R.string.voxmobile_please_wait), true, true);
	}

	private void dismissProgressDialog() {
		if (mProgressDialog == null) return;
		mProgressDialog.dismiss();
		mProgressDialog = null;
	}

	private void registerContentObservers() {
		ContentResolver cr = getContentResolver();
		mVoXObserver = new VoXObserver(new Handler());
		cr.registerContentObserver(RequestContract.CONTENT_URI, true, mVoXObserver);
	}

	private void unregisterContentObservers() {
		ContentResolver cr = getContentResolver();
		if (mVoXObserver != null) {
			cr.unregisterContentObserver(mVoXObserver);
			mVoXObserver = null;
		}
	}

	private static VoXObserver mVoXObserver;

	private static class VoXObserverState {
		private enum SyncType { STATE, CITY };
		
		private static int mSyncStatus = SyncStatus.STALE;
		private static int mHttpCode = 0;
		private static String mError = "";
		private static SyncType mSyncType = SyncType.STATE; 
		
		private static void reset() {
			mSyncStatus = SyncStatus.STALE;
			mHttpCode = 0;
			mError = "";
			mSyncType = SyncType.STATE;
		}
	}

	private class VoXObserver extends ContentObserver {

		public VoXObserver(Handler h) {
			super(h);
			VoXObserverState.reset();
		}

    	private void setError(int httpCode, String errorMsg) {
			VoXObserverState.mHttpCode = httpCode;

			if (httpCode == HttpStatus.OK.value()) {
				VoXObserverState.mError = "";
			} else if (httpCode != 0) {
				VoXObserverState.mError = "" + httpCode + ": " + HttpStatus.valueOf(httpCode).getReasonPhrase();
			} else {
				VoXObserverState.mError = errorMsg;
			}
    	}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			Cursor c = managedQuery(RequestContract.CONTENT_URI, RequestContract.PROJECTION, null, null, null);
			if (c.getCount() == 0) {
				return;
			}

			if (c.moveToFirst()) {
				VoXObserverState.mSyncStatus = c.getInt(RequestContract.UPDATED_INDEX);
				setError(c.getInt(RequestContract.HTTP_STATUS_INDEX), c.getString(RequestContract.ERROR_INDEX));
			} else {
				VoXObserverState.mSyncStatus = SyncStatus.STALE;
			}

			switch (VoXObserverState.mSyncStatus) {
			case SyncStatus.UPDATING:
				showProgressDialog();
				break;
			case SyncStatus.CURRENT:
				dismissProgressDialog();
				
				if (VoXObserverState.mHttpCode == HttpStatus.UNAUTHORIZED.value()) {
					showDialog(Consts.REST_UNAUTHORIZED);
				} else if (VoXObserverState.mHttpCode == 0) {
					showDialog(Consts.REST_ERROR);
				} else if (VoXObserverState.mHttpCode != HttpStatus.OK.value() && VoXObserverState.mHttpCode != -1) {
					showDialog(Consts.REST_HTTP_ERROR);
				} else {
					if (VoXObserverState.mSyncType == VoXObserverState.SyncType.STATE)
						populateDidStates();
					else
						populateDidCities();
				}
				break;
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		dismissProgressDialog();
		
		switch (id) {
		case Consts.REST_UNAUTHORIZED:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.voxmobile_attention)
			.setMessage(getString(R.string.voxmobile_unauthorized_msg))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
			}).create();
		case Consts.REST_UNSUPPORTED:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_attention))
			.setMessage(getString(R.string.voxmobile_upgrade))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
			}).create();
		case Consts.REST_HTTP_ERROR:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_server_error))
			.setMessage(VoXObserverState.mError)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
			}).create();
		case Consts.REST_ERROR:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_network_error))
			.setMessage(VoXObserverState.mError)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
			}).create();
		}
		return null;
	}

    private void populateDidStates() {
    	Spinner list = (Spinner)findViewById(R.id.Spinner01);
    	
    	DIDState[] states = getStates();
    	if (states.length == 0) {
    		return;
    	}

    	ArrayAdapter<DIDState> adapter = new ArrayAdapter<DIDState>(this, R.layout.voxmobile_simple_list_item, states);
    	list.setAdapter(adapter);

        mSelectedState = 0;

        String selectedState = OrderHelper.getStringValue(this, OrderHelper.DID_STATE);
       	for (int i = 0; i < adapter.getCount(); i++) {
       		DIDState state = adapter.getItem(i);
       		if (state.state_id.equals(selectedState)) {
       			mSelectedState = i;
       			break;
       		}
        }
        list.setSelection(mSelectedState);
    }

	private DIDState[] getStates() {
		Cursor c = getContentResolver().query(
				DIDStateContract.CONTENT_URI, 
				DIDStateContract.PROJECTION,
				null,
				null,
				DIDStateContract.DESCRIPTION);
		
		ArrayList<DIDState> list = new ArrayList<DIDState>();

		if (c.getCount() == 0) {
			VoXObserverState.mSyncType = VoXObserverState.SyncType.STATE;
            getContentResolver().update(DIDStateContract.CONTENT_URI, null, null, null);
		} else {
			DIDState state = new DIDState();
			state.state_id = "";
			state.description = getString(R.string.voxmobile_please_select);
			state.did_count = 0;
			list.add(state);
			
			while (c.moveToNext()) {
				state = new DIDState();
				state.state_id = c.getString(DIDStateContract.STATE_ID_INDEX);
				state.description = c.getString(DIDStateContract.DESCRIPTION_INDEX);
				state.did_count = c.getInt(DIDStateContract.DID_COUNT_INDEX);
				list.add(state);
			}
		}
		c.close();

		mStates = (DIDState[])list.toArray(new DIDState[list.size()]);
		return mStates;
	}

	private void populateDidCities() {
		Spinner list = (Spinner)findViewById(R.id.Spinner02);
    	
		initializeCities();
    	if (mCities.length == 0) {
    		return;
    	}

    	ArrayAdapter<DIDCity> adapter = new ArrayAdapter<DIDCity>(this, R.layout.voxmobile_simple_list_item, mCities);
    	list.setAdapter(adapter);

		mSelectedCity = 0;

        String selectedCity = OrderHelper.getStringValue(this, OrderHelper.DID_CITY);
       	for (int i = 0; i < adapter.getCount(); i++) {
       		DIDCity city = adapter.getItem(i);
       		if (city.city_id.equals(selectedCity)) {
       			mSelectedCity = i;
       			break;
       		}
        }
        list.setSelection(mSelectedCity);
		mCity.setEnabled(true);
	}

	private void initializeCities() {
		Cursor c = getContentResolver().query(
				DIDCityContract.CONTENT_URI, 
				DIDCityContract.PROJECTION,
				DIDCityContract.STATE_ID + "=?",
				new String[] { mStates[mSelectedState].state_id },
				DIDCityContract.DESCRIPTION);
		
		ArrayList<DIDCity> list = new ArrayList<DIDCity>();

		if (c.getCount() == 0) {
			VoXObserverState.mSyncType = VoXObserverState.SyncType.CITY;
			ContentValues values = new ContentValues();
			values.put(DIDCityContract.STATE_ID, mStates[mSelectedState].state_id);
			getContentResolver().update(DIDCityContract.CONTENT_URI, values, null, null);
		} else {	
			DIDCity city = new DIDCity();
			city.state_id = "";
			city.city_id = "";
			city.description = getString(R.string.voxmobile_please_select);
			city.did_count = 0;
			list.add(city);
			
			while (c.moveToNext()) {
				city = new DIDCity();
				city.state_id = c.getString(DIDCityContract.STATE_ID_INDEX);
				city.city_id = c.getString(DIDCityContract.CITY_ID_INDEX);
				city.description = c.getString(DIDCityContract.DESCRIPTION_INDEX);
				city.did_count = c.getInt(DIDCityContract.DID_COUNT_INDEX);
				list.add(city);
			}
		}
		c.close();

		mCities = (DIDCity[])list.toArray(new DIDCity[list.size()]);
	}
}
