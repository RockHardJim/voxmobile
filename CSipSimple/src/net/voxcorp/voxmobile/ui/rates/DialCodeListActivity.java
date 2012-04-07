package net.voxcorp.voxmobile.ui.rates;

import net.voxcorp.R;
import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;
import net.voxcorp.voxmobile.provider.DBContract.RateCityContract;
import net.voxcorp.voxmobile.provider.DBContract.RateCountryContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.provider.DBContract.TrialDialCodeContract;
import net.voxcorp.voxmobile.service.RestService;
import net.voxcorp.voxmobile.utils.Consts;

import org.springframework.http.HttpStatus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class DialCodeListActivity extends ListActivity {

	public static final String COUNTRY_KEY = "country";
	public static final String COUNTRY_ID_KEY = "country_id";
	public static final String COUNTRY_GROUP_KEY = "country_group";
	
	private ProgressDialog mProgressDialog = null;
	private SimpleCursorAdapter mAdapter;
	private int mCountryId;
	private String mCountry;
	private String mGroup;
	private boolean mTrialMode;
	private int mDefaultFontColor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.voxmobile_dial_code_list);
		
		mCountryId = getIntent().getIntExtra(COUNTRY_ID_KEY, 0);
		mCountry = getIntent().getStringExtra(COUNTRY_KEY);
		mGroup = getIntent().getStringExtra(COUNTRY_GROUP_KEY);
		mTrialMode = getIntent().getBooleanExtra(RatesActivity.TRIAL_MODE, false);
		
		TextView text = (TextView)findViewById(R.id.TextView01);
		text.setText(mCountry);
		
		// save default text color for use in trial dial code display binder
		mDefaultFontColor = text.getCurrentTextColor();

		text = (TextView)findViewById(R.id.TextView02);
		String str = String.format("%s: 011 + %d + XXXXX", getString(R.string.voxmobile_sample_call), mCountryId);
		text.setText(str);
		
		if (mTrialMode) {
			setTitle(R.string.voxmobile_trial_mode);
		} else {
			setTitle(R.string.voxmobile_international_rates);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerContentObservers();

		updateList();
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
		private static int mSyncStatus = SyncStatus.STALE;
		private static int mHttpCode = 0;
		private static String mError = "";

		private static void reset() {
			mSyncStatus = SyncStatus.STALE;
			mHttpCode = 0;
			mError = "";
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
				} else if (VoXObserverState.mHttpCode != HttpStatus.OK.value()) {
					showDialog(Consts.REST_HTTP_ERROR);
				} else {
					updateList();
				}
				break;
			}
		}
	}

	public void updateDialCodes() {
		// Kick off update task
		if (VoXObserverState.mSyncStatus != SyncStatus.UPDATING) {
			ContentValues values = new ContentValues();
			values.put(RestService.REST_DATA1, mCountryId);
			getContentResolver().update(RateCityContract.CONTENT_URI, values, null, null);		
		}
	}

	private void updateList() {
		if (mCountryId == -1) {
			setListAdapter(null);
			return;
		}

		Cursor c = getContentResolver().query(
				RateCountryContract.CONTENT_URI, 
				RateCountryContract.PROJECTION,
				RateCountryContract.COUNTRY_GROUP + "=?",
				new String[] { mGroup },
				RateCountryContract.COUNTRY);
		int i = c.getCount();
		c.close();
		if (i == 0) {
			setResult(RatesFragment.RESTART);
			finish();
			return;
		}

		if (mTrialMode) {
			c = getContentResolver().query(
					TrialDialCodeContract.CONTENT_URI, 
					TrialDialCodeContract.PROJECTION,
					TrialDialCodeContract.COUNTRY_ID + "=?",
					new String[] { "" + mCountryId },
					TrialDialCodeContract.DIAL_CODE);
		} else {
			c = getContentResolver().query(
					RateCityContract.CONTENT_URI, 
					RateCityContract.PROJECTION,
					RateCityContract.COUNTRY_ID + "=?",
					new String[] { "" + mCountryId },
					RateCityContract.CITY);
		}
		startManagingCursor(c);

		if (c.getCount() == 0 && mCountryId != -1) {
			updateDialCodes();
	        return;
		}

		if (mTrialMode) {
			mAdapter = new SimpleCursorAdapter(
					this,
					R.layout.voxmobile_trial_dial_code_item,
					c,
					new String[] { TrialDialCodeContract.DIAL_CODE, TrialDialCodeContract.BLOCKED },
					new int[] { R.id.TextView01, R.id.TextView02 });

			mAdapter.setViewBinder(new ViewBinder() {
				public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {

					TextView text;
					if (aView.getId() == R.id.TextView01) {
						text = (TextView) aView;
						text.setText(aCursor.getString(TrialDialCodeContract.DIAL_CODE_INDEX));
						return true;
					} else if (aView.getId() == R.id.TextView02) {
						text = (TextView) aView;
						String status;
						if (aCursor.getInt(TrialDialCodeContract.BLOCKED_INDEX) == DBBoolean.TRUE) {
							status = getString(R.string.voxmobile_disabled);
							text.setTextColor(mDefaultFontColor);
						} else {
							status = getString(R.string.voxmobile_enabled);
							text.setTextColor(Color.GREEN);
						}
						text.setText(status);
						return true;
					}
					return false;
				}
			});
		} else {
			mAdapter = new SimpleCursorAdapter(
					this,
					R.layout.voxmobile_rate_detail_item,
					c,
					new String[] { RateCityContract.CITY, RateCityContract.RATE, RateCityContract.DIAL_CODE },
					new int[] { R.id.TextView01, R.id.TextView02, R.id.TextView03 });

			mAdapter.setViewBinder(new ViewBinder() {
				public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {

					if (aView.getId() == R.id.TextView01) {
						TextView text = (TextView) aView;
						text.setText(aCursor.getString(RateCityContract.CITY_INDEX));
						return true;
					} else if (aView.getId() == R.id.TextView02) {
						TextView text = (TextView) aView;
						text.setText("$" + aCursor.getString(RateCityContract.RATE_INDEX));
						return true;
					} else if (aView.getId() == R.id.TextView03) {
						TextView text = (TextView) aView;
						text.setText(aCursor.getString(RateCityContract.DIAL_CODE_INDEX));
						return true;
					}
					return false;
				}
			});
		}

		setListAdapter(mAdapter);		
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
					finish();
				}
            }).create();
		case Consts.REST_UNSUPPORTED:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_attention))
			.setMessage(getString(R.string.voxmobile_upgrade))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
                }
            }).create();
		case Consts.REST_HTTP_ERROR:
            return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_server_error))
			.setMessage(VoXObserverState.mError)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
                }
            }).create();
		case Consts.REST_ERROR:
            return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_network_error))
			.setMessage(VoXObserverState.mError)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
                }
            }).create();
		}
		return null;
	}

}
