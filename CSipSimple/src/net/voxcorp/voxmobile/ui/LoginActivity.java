package net.voxcorp.voxmobile.ui;

import net.voxcorp.R;
import net.voxcorp.voxmobile.provider.DBContract.AccountContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.service.RestService;
import net.voxcorp.voxmobile.utils.Consts;

import org.springframework.http.HttpStatus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

public class LoginActivity extends TrackedActivity implements OnClickListener {
	
    private EditText mUid;
    private EditText mPwd;
    private LinearLayout mBtn;
    private int mAccountCount;
    
	private static VoXObserver mVoXObserver;
	private ProgressDialog mProgressDialog = null;   
	
	private static final int DUPLICATE_ACCOUNT = -1;
	private static final int MISSING_UID = -2;
	private static final int MISSING_PWD = -3;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
        setContentView(R.layout.voxmobile_login);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_accounts);
        
        // Bind User name field
        mUid = (EditText)findViewById(R.id.EditText01);

        // Bind password field
        mPwd = (EditText)findViewById(R.id.EditText02);

        // Bind login button
        mBtn = (LinearLayout)findViewById(R.id.LinearLayout01);
        mBtn.setOnClickListener(this);
        
        mUid.requestFocus();
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

	private void showProgressDialog() {
		if (isFinishing() || mProgressDialog != null) return;
		mProgressDialog = ProgressDialog.show(this, "", getString(R.string.voxmobile_authenticating), true, true);
	}
	
	private void dismissProgressDialog() {
		if (mProgressDialog == null) return;
		mProgressDialog.dismiss();
		mProgressDialog = null;
		
		mBtn.setEnabled(true);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		mBtn.setEnabled(true);

		dismissProgressDialog();
		
		switch (id) {
        case MISSING_UID:
            trackEvent("login_failure", "missing_uid", 0);
            
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.voxmobile_attention)
			.setMessage(getString(R.string.voxmobile_login_missing_uid))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {					
                    mUid.requestFocus();
                }
            }).create();
        case MISSING_PWD:
            trackEvent("login_failure", "missing_pwd", 0);
            
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.voxmobile_attention)
			.setMessage(getString(R.string.voxmobile_login_missing_pwd))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {					
                    mPwd.requestFocus();
                }
            }).create();
		case DUPLICATE_ACCOUNT:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.voxmobile_duplicate_account)
			.setMessage(getString(R.string.voxmobile_duplicate_login))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {					
                    
                }
            }).create();
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
            trackEvent("rest", "unsupported", 0);
            
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_attention))
			.setMessage(getString(R.string.voxmobile_upgrade))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {					
                    
                }
            }).create();
		case Consts.REST_HTTP_ERROR:
            trackEvent("rest", "http_error", 0);

            return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_server_error))
			.setMessage(VoXObserverState.mError)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {					
                    
                }
            }).create();
		case Consts.REST_ERROR:
            trackEvent("rest", "rest_error", 0);

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
			String accountSave = null;
			
			super.onChange(selfChange);
			
			Cursor c = managedQuery(RequestContract.CONTENT_URI, RequestContract.PROJECTION, null, null, null);
			int count = c.getCount();
			if (count == 0) {
				return;
			}

			if (c.moveToFirst()) {
				// used if successful login to hold account number
				accountSave = c.getString(RequestContract.ERROR_INDEX);
				
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
					c = managedQuery(AccountContract.CONTENT_URI, AccountContract.PROJECTION, null, null, null);
					count = c.getCount();
					if (count == mAccountCount) {
						showDialog(DUPLICATE_ACCOUNT);
					} else {
						// Account just logged in to is passed in the request table error column
						c = managedQuery(AccountContract.CONTENT_URI,
								AccountContract.PROJECTION,
								AccountContract.ACCOUNT_NO + "=?",
								new String[] { accountSave },
								null);

						if (c.moveToFirst()) {
							Intent intent = getIntent();
							intent.putExtra(AccountContract.UUID, c.getString(AccountContract.UUID_INDEX));
							intent.putExtra(AccountContract.ACCOUNT_NO, c.getString(AccountContract.ACCOUNT_NO_INDEX));
							setResult(RESULT_OK, intent);
						} else {
							setResult(RESULT_OK);
						}
						finish();
					}
				}
				break;
			}			
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerContentObservers();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		unregisterContentObservers();
		super.onStop();
	}
	
	@Override
	public void onClick(View v) {
        if (mUid.getText().toString().trim().length() < 3) {
            showDialog(MISSING_UID);
            return;
        }

        if (mPwd.getText().toString().trim().length() < 3) {
            showDialog(MISSING_PWD);
            return;
        }

		mBtn.setEnabled(false);
		
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(mUid.getWindowToken(), 0);
		mgr.hideSoftInputFromWindow(mPwd.getWindowToken(), 0);

   		ContentValues values = new ContentValues();
   		values.put(RestService.REST_DATA1, mUid.getText().toString().trim());
   		values.put(RestService.REST_DATA2, mPwd.getText().toString().trim());
   		getContentResolver().update(AccountContract.CONTENT_URI_LOGIN, values, null, null);
	}
	
}
