package net.voxcorp.voxmobile.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.voxcorp.R;

import org.springframework.http.HttpStatus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

import net.voxcorp.api.SipManager;
import net.voxcorp.api.SipProfile;
import net.voxcorp.db.DBAdapter;
import net.voxcorp.models.Filter;
import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.provider.DBContract.AccountContract;
import net.voxcorp.voxmobile.provider.DBContract.AccountSummaryContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SipUserContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.types.SipUser;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.wizards.impl.VoXMobile;

public class SipAccountsListActivity extends ListActivity implements OnItemClickListener {
	
	private static final String THIS_FILE = "SipAccountsListActivity";

	private static final int ALREADY_CONFIGURED = -1;
	private static final int ACCOUNT_SUMMARY = -2;
	
	private ProgressDialog mProgressDialog = null;

	private String mSummary;
	private long mSummaryTimestamp;
	private String mAccountNo;
	private String mUuid;
	private Cursor mAccountCursor;
	private SimpleCursorAdapter mAccountAdapter;
	private List<SipProfile> sipProfiles;
	
	final String KEY_ICON = "icon";
	final String KEY_ITEM = "item";
	final String KEY_SUB_ITEM = "subitem";

	private static final int SETUP_ACTION = 0;

	private ListView actionsListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		mUuid = getIntent().getStringExtra(AccountContract.UUID);
		mAccountNo = getIntent().getStringExtra(AccountContract.ACCOUNT_NO);
		
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voxmobile_accounts_list);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_accounts);
		setTitle(getString(R.string.voxmobile_sip_account) + ": " + mAccountNo);
		
		// hook up context menu for account list view
		registerForContextMenu(getListView());
		
		List<HashMap<String, Object>> fillMaps = new ArrayList<HashMap<String, Object>>();
		
		HashMap<String, Object> item1 = new HashMap<String, Object>();
		item1.put(KEY_ICON, R.drawable.voxmobile_money);
		item1.put(KEY_ITEM, getString(R.string.voxmobile_check_usage));
		item1.put(KEY_SUB_ITEM, getString(R.string.voxmobile_check_usage_sub));
		fillMaps.add(item1);
		
		SimpleAdapter adapter = new SimpleAdapter(
				this, 
				fillMaps, 
				R.layout.voxmobile_list_item,
				new String[] { KEY_ICON, KEY_ITEM, KEY_SUB_ITEM }, 
				new int[] { R.id.ImageView01, R.id.TextView01, R.id.TextView02 });		
		
		actionsListView = (ListView) findViewById(R.id.ListView01);
		
		actionsListView.setAdapter(adapter);
		actionsListView.setOnItemClickListener(this);
		
		getListView().getEmptyView().setVisibility(View.GONE);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
        Cursor c = (Cursor)mAccountAdapter.getItem(position);           
        String username = c.getString(SipUserContract.USERNAME_INDEX);
        String password = c.getString(SipUserContract.PASSWORD_INDEX);
        String displayName = c.getString(SipUserContract.DISPLAY_NAME_INDEX);
    
        if (accountConfigured(username)) {
        	showDialog(ALREADY_CONFIGURED);
        	return;
        } else {
        	SipUser sipUser = new SipUser();
        	sipUser.username = username;
        	sipUser.password = password;
        	sipUser.displayname = displayName;
        	configureSipProfile(sipUser);
        }
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (parent.getId() == R.id.ListView01) {
			if (position == 0) {
				showAccountSummary();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SETUP_ACTION:
			if(resultCode == RESULT_OK) {
				Toast.makeText(this, "Do Setup", Toast.LENGTH_SHORT).show();
			}
			break;
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
		cr.registerContentObserver(SipUserContract.CONTENT_URI, true, mVoXObserver);
		cr.registerContentObserver(AccountSummaryContract.CONTENT_URI, true, mVoXObserver);
	}

	private void unregisterContentObservers() {
		ContentResolver cr = getContentResolver();
		if (mVoXObserver != null) {
			cr.unregisterContentObserver(mVoXObserver);
			mVoXObserver = null;
		}
	}
	
    private boolean accountConfigured(String sipUsername)  {

        boolean found = false;

        Iterator<SipProfile> iList = sipProfiles.iterator();
        while (iList.hasNext()) {
            SipProfile sp = iList.next();

            if (!VoXMobile.isVoXMobile(sp.proxies))
                continue;

            found = sp.username.equals(sipUsername);
            if (found) {
                break;
            }
        }

        return found;
    }

    private void showAccountSummary() {
		Cursor c = managedQuery(AccountSummaryContract.CONTENT_URI, 
				AccountSummaryContract.PROJECTION,
				AccountSummaryContract.UUID + "=?",
				new String[] { mUuid },
				null);

		if (c.getCount() == 0) {
			mSummary = "";
			mSummaryTimestamp = 0;

			VoXObserverState.mSyncType = VoXObserverState.SyncType.SUMMARY;

			ContentValues values = new ContentValues();
			values.put(AccountSummaryContract.UUID, mUuid);
			getContentResolver().update(AccountSummaryContract.CONTENT_URI, values, null, null);
			return;
		}

		if (c.moveToFirst()) {
			mSummary = c.getString(AccountSummaryContract.SUMMARY_INDEX);
			mSummaryTimestamp = c.getLong(AccountSummaryContract.TIMESTAMP_INDEX);
			showDialog(ACCOUNT_SUMMARY);
		}
    }

	private void updateList() {
        DBAdapter database = new DBAdapter(this);
        database.open();
        sipProfiles = database.getListAccounts();
        database.close();

		mAccountCursor = getContentResolver().query(
				SipUserContract.CONTENT_URI, 
				SipUserContract.PROJECTION,
				SipUserContract.ACCOUNT_NO + "=?",
				new String[] { mAccountNo },
				SipUserContract.ACCOUNT_NO);
		
		startManagingCursor(mAccountCursor);
		
		if (mAccountCursor.getCount() == 0) {
			VoXObserverState.mSyncType = VoXObserverState.SyncType.USERS;

			ContentValues values = new ContentValues();
			values.put(AccountContract.UUID, mUuid);
			values.put(AccountContract.ACCOUNT_NO, mAccountNo);
            getContentResolver().update(SipUserContract.CONTENT_URI, values, null, null);
			return;
		}
		
		mAccountAdapter = new SimpleCursorAdapter(
				this,
				R.layout.voxmobile_account_list_item,
				mAccountCursor,
				new String[] { SipUserContract.USERNAME, SipUserContract.ACCOUNT_NO, SipUserContract.ACCOUNT_NO },
				new int[] { R.id.TextView01, R.id.TextView02, R.id.ImageView01 });
		
		mAccountAdapter.setViewBinder(new ViewBinder() {
			
			public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {
				
				String sipUser;
				TextView textView;
				
				if (aView.getId() == R.id.TextView01) {
					textView = (TextView) aView;
					textView.setText(aCursor.getString(aColumnIndex));
					return true;
				} else if (aView.getId() == R.id.TextView02) {
					textView = (TextView) aView;

					sipUser = aCursor.getString(SipUserContract.USERNAME_INDEX);
					if (accountConfigured(sipUser)) {
						textView.setText(getString(R.string.voxmobile_sip_account_configured));
						textView.setTextColor(Color.GREEN);
					} else {
						textView.setText(getString(R.string.voxmobile_sip_account_unconfigured));
					}
					return true;
				} else if (aView.getId() == R.id.ImageView01) {
					ImageView image = (ImageView) aView;
					sipUser = aCursor.getString(SipUserContract.USERNAME_INDEX);
					if (accountConfigured(sipUser)) {
						image.setImageResource(R.drawable.ic_voxmobile_check);
					} else {
						image.setImageResource(R.drawable.voxmobile_sip_account);
					}
					return true;
				}
				return false;
			}
		});

		setListAdapter(mAccountAdapter);
	}

	private static VoXObserver mVoXObserver;

	private static class VoXObserverState {
		private enum SyncType { USERS, SUMMARY };

		private static int mSyncStatus = SyncStatus.STALE;
		private static int mHttpCode = 0;
		private static String mError = "";
		private static SyncType mSyncType = SyncType.USERS;
		
		private static void reset() {
			mSyncStatus = SyncStatus.STALE;
			mHttpCode = 0;
			mError = "";
			mSyncType = SyncType.USERS;
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
					if (VoXObserverState.mSyncType == VoXObserverState.SyncType.USERS) {
						updateList();
					} else {
						showAccountSummary();
					}
				}
				break;
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		dismissProgressDialog();

		switch (id) {
		case ACCOUNT_SUMMARY:
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(mSummaryTimestamp);
			String date = formatter.format(calendar.getTime());

			return new AlertDialog.Builder(SipAccountsListActivity.this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.voxmobile_summary)
			.setMessage(date + "\n\n" + mSummary)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					removeDialog(ACCOUNT_SUMMARY);
				}
            }).create();
        case ALREADY_CONFIGURED:
			return new AlertDialog.Builder(SipAccountsListActivity.this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.voxmobile_attention)
			.setMessage(getString(R.string.voxmobile_already_configured))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
            }).create();
		case Consts.REST_UNAUTHORIZED:
			return new AlertDialog.Builder(SipAccountsListActivity.this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.voxmobile_attention)
			.setMessage(getString(R.string.voxmobile_unauthorized_msg))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
            }).create();
		case Consts.REST_UNSUPPORTED:
			return new AlertDialog.Builder(SipAccountsListActivity.this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_attention))
			.setMessage(getString(R.string.voxmobile_upgrade))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

                }
            }).create();
		case Consts.REST_HTTP_ERROR:
            return new AlertDialog.Builder(SipAccountsListActivity.this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_server_error))
			.setMessage(VoXObserverState.mError)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

                }
            }).create();
		case Consts.REST_ERROR:
            return new AlertDialog.Builder(SipAccountsListActivity.this)
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
	
	private void configureSipProfile(SipUser sipUser) {
		Log.d(THIS_FILE, "Creating SIP profile for " + sipUser.username);
		
        VoXMobile wizard = new VoXMobile();
        SipProfile account = new SipProfile();

        wizard.buildAccount(account, sipUser);
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

		Intent result = getIntent();
		setResult(RESULT_OK, result);
		finish();		
	}

}
