package net.voxcorp.voxmobile.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.voxcorp.R;

import org.springframework.http.HttpStatus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.provider.DBContract.AccountContract;
import net.voxcorp.voxmobile.provider.DBContract.ProvisionCheckContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.ui.order.ServicePlansListActivity;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.OrderHelper;

public class AccountsListActivity extends TrackedListActivity implements OnItemClickListener {
	
	private static final String THIS_FILE = "AccountsListActivity";

	public static final int MENU_ITEM_LOGOUT = Menu.FIRST;
	
	private ProgressDialog mProgressDialog = null;

	private static final int ORDER_INFO = -1;
	private static final int PROVISION_WAIT = -2;

	private Cursor mAccountCursor;
	private SimpleCursorAdapter mAccountAdapter;
	
	final String KEY_ICON = "icon";
	final String KEY_ITEM = "item";
	final String KEY_SUB_ITEM = "subitem";

	private ListView actionsListView;

	private static final int ORDER_ACTION = 0;
	private static final int LOGIN_ACTION = 1;
	private static final int SIPACCOUNT_ACTION = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "AccountsListActivity.onCreate()");
		
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voxmobile_accounts_list);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_accounts);
		
		// hook up context menu for account list view
		registerForContextMenu(getListView());		
		
		List<HashMap<String, Object>> fillMaps = new ArrayList<HashMap<String, Object>>();
		
		HashMap<String, Object> item1 = new HashMap<String, Object>();
		item1.put(KEY_ICON, R.drawable.voxmobile_signup);
		item1.put(KEY_ITEM, getString(R.string.voxmobile_signup));
		item1.put(KEY_SUB_ITEM, getString(R.string.voxmobile_signup_sub));
		
		HashMap<String, Object> item2 = new HashMap<String, Object>();
		item2.put(KEY_ICON, R.drawable.voxmobile_login);
		item2.put(KEY_ITEM, getString(R.string.voxmobile_login));
		item2.put(KEY_SUB_ITEM, getString(R.string.voxmobile_login_sub));
		
		fillMaps.add(item1);
		fillMaps.add(item2);
		
		SimpleAdapter adapter = new SimpleAdapter(
				this, 
				fillMaps, 
				R.layout.voxmobile_list_item,
				new String[] { KEY_ICON, KEY_ITEM, KEY_SUB_ITEM }, 
				new int[] { R.id.ImageView01, R.id.TextView01, R.id.TextView02 });		
		
		actionsListView = (ListView) findViewById(R.id.ListView01);
		
		actionsListView.setAdapter(adapter);
		actionsListView.setOnItemClickListener(this);
	}

	private boolean isProvisioned(String accountNo) {
		return !"".equals(accountNo);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo mi = (AdapterContextMenuInfo)menuInfo;

        Cursor c = (Cursor)mAccountAdapter.getItem(mi.position);

        if (isProvisioned(c.getString(AccountContract.ACCOUNT_NO_INDEX))) {
            menu.add(0, MENU_ITEM_LOGOUT, 0, R.string.voxmobile_logout);
        }
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return false;
        }
        
        switch (item.getItemId()) {
        case MENU_ITEM_LOGOUT: {
            Cursor c = (Cursor)mAccountAdapter.getItem(info.position);           
            String id = c.getString(AccountContract.ID_INDEX);
            
            getContentResolver().delete(
            		AccountContract.CONTENT_URI_LOGOUT,
        			BaseColumns._ID + "=?",
        			new String[] { id });
        	
            return true;            
            }
        }
        return false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
        Cursor c = (Cursor)mAccountAdapter.getItem(position);           
        String uuid = c.getString(AccountContract.UUID_INDEX);
        String accountNo = c.getString(AccountContract.ACCOUNT_NO_INDEX);
        
        if (isProvisioned(c.getString(AccountContract.ACCOUNT_NO_INDEX))) {
    		Intent intent = new Intent(this, SipAccountsListActivity.class);
    		intent.putExtra(AccountContract.UUID, uuid);
    		intent.putExtra(AccountContract.ACCOUNT_NO, accountNo);
        	startActivityForResult(intent, SIPACCOUNT_ACTION);
        } else {
        	showDialog(PROVISION_WAIT);
        }
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (parent.getId() == R.id.ListView01) {
			if (position == 0) {
				if (OrderHelper.getBooleanValue(AccountsListActivity.this, OrderHelper.SHOW_ORDER_OVERVIEW, true)) {
					OrderHelper.setBooleanValue(AccountsListActivity.this, OrderHelper.SHOW_ORDER_OVERVIEW, false);
					showDialog(ORDER_INFO);
				} else {
					unhookButtonListeners();
					startActivityForResult(new Intent(AccountsListActivity.this, ServicePlansListActivity.class), ORDER_ACTION);
				}
			} else {
				unhookButtonListeners();
		    	startActivityForResult(new Intent(AccountsListActivity.this, LoginActivity.class), LOGIN_ACTION);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Make sure no progress is shown
		dismissProgressDialog();

		// hook click events backup
		actionsListView.setOnItemClickListener(this);
		
		switch (requestCode) {
		case ORDER_ACTION:
			break;
		case LOGIN_ACTION:
			if (data == null) break;

			String uuid = data.getStringExtra(AccountContract.UUID);
			String accountNo = data.getStringExtra(AccountContract.ACCOUNT_NO);

			if (uuid == null || accountNo == null) {
				break;
			}

			Intent intent = new Intent(this, SipAccountsListActivity.class);
			intent.putExtra(AccountContract.UUID, uuid);
			intent.putExtra(AccountContract.ACCOUNT_NO, accountNo);
			startActivityForResult(intent, SIPACCOUNT_ACTION);
			break;
		case SIPACCOUNT_ACTION:
			if(resultCode == RESULT_OK) {
				Intent result = getIntent();
				setResult(RESULT_OK, result);
				finish();		
			}
			break;
		
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		registerContentObservers();

		updateList();

		// Kick off provision check task
        getContentResolver().update(ProvisionCheckContract.CONTENT_URI, null, null, null);
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
		cr.registerContentObserver(AccountContract.CONTENT_URI, true, mVoXObserver);
	}

	private void unregisterContentObservers() {
		ContentResolver cr = getContentResolver();
		if (mVoXObserver != null) {
			cr.unregisterContentObserver(mVoXObserver);
			mVoXObserver = null;
		}
	}

	private void updateList() {
		mAccountCursor = getContentResolver().query(
				AccountContract.CONTENT_URI, 
				AccountContract.PROJECTION,
				null,
				null,
				AccountContract.ACCOUNT_NO + "," + BaseColumns._ID);

		startManagingCursor(mAccountCursor);

		mAccountAdapter = new SimpleCursorAdapter(
				this,
				R.layout.voxmobile_account_list_item,
				mAccountCursor,
				new String[] { AccountContract.ACCOUNT_NO, AccountContract.ACCOUNT_NO, AccountContract.ACCOUNT_NO },
				new int[] { R.id.TextView01, R.id.TextView02, R.id.ImageView01 });

		mAccountAdapter.setViewBinder(new ViewBinder() {
			public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {

				TextView textView;
				boolean isReady = isProvisioned(aCursor.getString(AccountContract.ACCOUNT_NO_INDEX));

				if (aView.getId() == R.id.TextView01) {
					textView = (TextView) aView;

					if (isReady) {
						textView.setText(aCursor.getString(aColumnIndex));
					} else {
						textView.setText(getString(R.string.voxmobile_new_account));
					}
					return true;
				} else if (aView.getId() == R.id.TextView02) {
					textView = (TextView) aView;

					if (isReady) {
						textView.setText(getString(R.string.voxmobile_account_ready));
						textView.setTextColor(0xff888888);
					} else {
						textView.setText(getString(R.string.voxmobile_account_notready));
						textView.setTextColor(Color.YELLOW);
					}
					return true;
				} else if (aView.getId() == R.id.ImageView01) {
					ImageView image = (ImageView) aView;

					if (isReady) {
						image.setImageResource(R.drawable.voxmobile_account);
					} else {
						image.setImageResource(R.drawable.voxmobile_account_wait);
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

	@Override
	protected Dialog onCreateDialog(int id) {
		dismissProgressDialog();

		switch (id) {
		case PROVISION_WAIT:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.voxmobile_attention)
			.setMessage(getString(R.string.voxmobile_provision_wait))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
            }).create();
		case ORDER_INFO:
			trackPageView("order/start");
			
			// set up sign up overview dialog
			Dialog dlgOrder = new Dialog(this);
			dlgOrder.setContentView(R.layout.voxmobile_signup_overview);
			dlgOrder.setTitle(R.string.voxmobile_signup_header);
			dlgOrder.setCancelable(true);
			
			// set up text
			TextView text = (TextView) dlgOrder.findViewById(R.id.TextView01);
			text.setText(R.string.voxmobile_signup_text_1);
			
			text = (TextView) dlgOrder.findViewById(R.id.TextView02);
			text.setText(R.string.voxmobile_signup_text_2);
			
			text = (TextView) dlgOrder.findViewById(R.id.TextView03);
			text.setText(R.string.voxmobile_signup_text_3);
			
			// set up continue button
			Button button = (Button) dlgOrder.findViewById(R.id.Button02);
			button.setText(R.string.voxmobile_continue);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					unhookButtonListeners();

					dismissDialog(ORDER_INFO);
					startActivityForResult(new Intent(AccountsListActivity.this, ServicePlansListActivity.class), ORDER_ACTION);
				}
			});
			
			// set up cancel button
			button = (Button) dlgOrder.findViewById(R.id.Button01);
			button.setText(R.string.cancel);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// hook click events backup
					actionsListView.setOnItemClickListener(AccountsListActivity.this);

					dismissDialog(ORDER_INFO);
				}
			});
			
			return dlgOrder;
		
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
	
	private void unhookButtonListeners() {
		// Disable click events so that the user can't get
		// click-happy and confuse the application. We
		// hook click events back up when the get activity
		// result (see onActivityResult)
		actionsListView.setOnItemClickListener(null);
	}
}
