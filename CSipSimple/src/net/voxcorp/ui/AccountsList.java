/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.voxcorp.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.http.HttpStatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.VersionCheckContract;
import net.voxcorp.voxmobile.ui.AccountsListActivity;
import net.voxcorp.voxmobile.utils.Consts;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.voxcorp.R;
import net.voxcorp.api.SipManager;
import net.voxcorp.api.SipProfile;
import net.voxcorp.db.DBAdapter;
import net.voxcorp.api.ISipService;
import net.voxcorp.service.SipService;
import net.voxcorp.utils.AccountListUtils;
import net.voxcorp.utils.AccountListUtils.AccountStatusDisplay;
import net.voxcorp.utils.Log;
import net.voxcorp.utils.PreferencesWrapper;
import net.voxcorp.utils.SipProfileJson;
import net.voxcorp.wizards.BasePrefsWizard;
import net.voxcorp.wizards.WizardChooser;
import net.voxcorp.wizards.WizardUtils;
import net.voxcorp.wizards.WizardUtils.WizardInfo;
import net.voxcorp.wizards.impl.VoXMobile;

public class AccountsList extends Activity implements OnItemClickListener {
	
	private DBAdapter database;
	private AccountAdapter adapter;
	
	private List<SipProfile> accountsList;
	private ListView accountsListView;
	private GestureDetector gestureDetector;
	
	private static final String THIS_FILE = "SIP AccountList";
	
	public static final int MENU_ITEM_ACTIVATE = Menu.FIRST;
	public static final int MENU_ITEM_MODIFY = Menu.FIRST+1;
	public static final int MENU_ITEM_DELETE = Menu.FIRST+2;
	public static final int MENU_ITEM_WIZARD = Menu.FIRST+3;
	

	
	private static final int CHOOSE_WIZARD = 0;
	private static final int REQUEST_MODIFY = CHOOSE_WIZARD + 1;
	private static final int CHANGE_WIZARD =  REQUEST_MODIFY + 1;
	private static final int VOX_ACCOUNTS =  CHANGE_WIZARD + 1;
	
	private static final int NEED_LIST_UPDATE = 1;
	private static final int UPDATE_LINE = 2;
	
    // mVoXActivityLock prevents net.voxcorp.voxmobile.ui.AccountListsActivity
    // from being started more than once, which can happen in the case of upgrading
    // legacy builds (ie, <= 998). This happens because of all the REST stuff that
    // triggers observer callbacks, some of which we want to ignore and prevent
    // the duplicate starting of the activity.
    private static boolean mVoXActivityLock = false;
    private static VoXObserver mVoXObserver;
    private ProgressDialog mProgressDialog = null;
    
    private void registerContentObservers() {
    	ContentResolver cr = getContentResolver();
    	mVoXObserver = new VoXObserver(new Handler());
    	cr.registerContentObserver(RequestContract.CONTENT_URI, true, mVoXObserver);
    	cr.registerContentObserver(VersionCheckContract.CONTENT_URI, true, mVoXObserver);
    }
    
    private void unregisterContentObservers() {
    	ContentResolver cr = getContentResolver();
    	if (mVoXObserver != null) {
    		cr.unregisterContentObserver(mVoXObserver);
    		mVoXObserver = null;
    	}
    }
    
    private void showProgressDialog() {
    	if (mProgressDialog != null) return;
    	mProgressDialog = ProgressDialog.show(this, "", "Please wait...");
    }
    
    private void dismissProgressDialog() {
    	if (mProgressDialog == null) return;
    	mProgressDialog.dismiss();
    	mProgressDialog = null;
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	LinearLayout btn = (LinearLayout) findViewById(R.id.add_account);
    	btn.setEnabled(true);
    	
    	dismissProgressDialog();
    	
    	switch (id) {
    	case Consts.REST_UNAUTHORIZED:
    		return new AlertDialog.Builder(AccountsList.this)
    		.setIcon(android.R.drawable.ic_dialog_alert)
    		.setTitle(R.string.voxmobile_attention)
    		.setMessage(getString(R.string.voxmobile_corrupted))
    		.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) {
    				
    			}
    		}).create();
    	case Consts.REST_UNSUPPORTED:
    		return new AlertDialog.Builder(AccountsList.this)
    		.setIcon(android.R.drawable.ic_dialog_alert)
    		.setTitle(getString(R.string.voxmobile_attention))
    		.setMessage(getString(R.string.voxmobile_upgrade))
    		.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) {
    				
    			}
    		}).create();
    	case Consts.REST_HTTP_ERROR:
    		return new AlertDialog.Builder(AccountsList.this)
    		.setIcon(android.R.drawable.ic_dialog_alert)
    		.setTitle(getString(R.string.voxmobile_network_error))
    		.setMessage(VoXObserverState.mError)
    		.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) {
    				
    			}
    		}).create();
    	case Consts.REST_ERROR:
    		return new AlertDialog.Builder(AccountsList.this)
    		.setIcon(android.R.drawable.ic_dialog_alert)
    		.setTitle(getString(R.string.voxmobile_server_error))
    		.setMessage(VoXObserverState.mError)
    		.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) {
    				
    			}
    		}).create();
    	}
    	return null;
    }
    
    private static class VoXObserverState {
    	private static boolean mSuccess = false;
    	private static int mSyncStatus = SyncStatus.STALE;
    	private static int mHttpCode = 0;
    	private static String mError = "";
    	
    	private static void reset() {
    		mSuccess = false;
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
    	
    	@Override
    	public void onChange(boolean selfChange) {
    		super.onChange(selfChange);
    		
    		Cursor c = managedQuery(RequestContract.CONTENT_URI, RequestContract.PROJECTION, null, null, null);
    		if (c.getCount() == 0) {
    			c.close();
    			return;
    		}
    		
    		if (c.moveToFirst()) {
    			VoXObserverState.mSyncStatus = c.getInt(RequestContract.UPDATED_INDEX);
    			VoXObserverState.mSuccess = c.getInt(RequestContract.SUCCESS_INDEX) == DBBoolean.TRUE;
    			VoXObserverState.mHttpCode = c.getInt(RequestContract.HTTP_STATUS_INDEX);
    			    			
    			if (VoXObserverState.mHttpCode == HttpStatus.OK.value()) {
    				VoXObserverState.mError = c.getString(RequestContract.ERROR_INDEX);
    			} else if (VoXObserverState.mHttpCode != 0) {
    				VoXObserverState.mError = "" + VoXObserverState.mHttpCode + ": " + HttpStatus.valueOf(VoXObserverState.mHttpCode).getReasonPhrase();
    			}
    		} else {
    			VoXObserverState.mSyncStatus = SyncStatus.STALE;
    		}
    		c.close();
    		
    		switch (VoXObserverState.mSyncStatus) {
    		case SyncStatus.UPDATING:
    			showProgressDialog();
    			break;
    		case SyncStatus.CURRENT:
    			dismissProgressDialog();
    			
    			if (VoXObserverState.mHttpCode == HttpStatus.UNAUTHORIZED.value()) {
    				showDialog(Consts.REST_UNAUTHORIZED);
    			} else if (VoXObserverState.mHttpCode != HttpStatus.OK.value()) {
    				showDialog(Consts.REST_HTTP_ERROR);
    			} else if (!VoXObserverState.mSuccess) {
    				showDialog(Consts.REST_ERROR);
    			} else if (checkSupported(false)) {
    				if (!mVoXActivityLock) {
    					mVoXActivityLock = true;
    					startActivityForResult(new Intent(AccountsList.this, AccountsListActivity.class), VOX_ACCOUNTS);
    				}
    			}
    			break;
    		}
    	}
    	
    	public boolean checkSupported(boolean allowUpdate) {
    		if (VoXObserverState.mSyncStatus == SyncStatus.UPDATING) {
    			return false;
    		}
    		
    		Cursor c = managedQuery(VersionCheckContract.CONTENT_URI, VersionCheckContract.PROJECTION, null, null, null);
    		if (c == null) return false;
    		try {
    			if (c.getCount() == 0) {
    				if (allowUpdate) {
    					getContentResolver().update(VersionCheckContract.CONTENT_URI, null, null, null);
    				}
    				return false;
    			}
    			
    			if (c.moveToFirst()) {
    				boolean supported = c.getInt(VersionCheckContract.SUPPORTED_INDEX) == DBBoolean.TRUE;
    				if (!supported) {
    					showDialog(Consts.REST_UNSUPPORTED);
    				}
    				return supported;
    			}
    			return false;
    		} finally {
    			c.close();
    		}
    	}
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accounts_list);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_accounts);

		
		// Fill accounts with currently avalaible accounts
		updateList();
		
		accountsListView = (ListView) findViewById(R.id.account_list);
		
		accountsListView.setAdapter(adapter);
		accountsListView.setOnItemClickListener(this);
		accountsListView.setOnCreateContextMenuListener(this);
		

		//Add add row
		LinearLayout add_row = (LinearLayout) findViewById(R.id.add_account);
		add_row.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clickAddAccount();
			}
		});
		
		
		//Add gesture detector
		gestureDetector = new GestureDetector(this, new BackGestureDetector());
		accountsListView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		});
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(THIS_FILE, "Bind to service");
		//Bind to sip service
		bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
		//And register to ua state events
		registerReceiver(registrationStateReceiver, new IntentFilter(SipManager.ACTION_SIP_REGISTRATION_CHANGED));
		registerReceiver(registrationStateReceiver, new IntentFilter(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED));
		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d(THIS_FILE, "Unbind from service");
		try {
			unbindService(connection);
		}catch(Exception e) {
			//Just ignore that
		}
		service = null;
		try {
			unregisterReceiver(registrationStateReceiver);
		}catch(Exception e) {
			//Just ignore that
		}
		dismissProgressDialog();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		registerContentObservers();
		mVoXActivityLock = false;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterContentObservers();
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return;
        }

        SipProfile account = (SipProfile) adapter.getItem(info.position);
        if (account == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }
        
        WizardInfo wizardInfos = WizardUtils.getWizardClass(account.wizard);

        // Setup the menu header
        menu.setHeaderTitle(account.display_name);
        menu.setHeaderIcon(wizardInfos.icon);

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_ACTIVATE, 0, account.active?R.string.deactivate_account:R.string.activate_account);
        menu.add(0, MENU_ITEM_MODIFY, 0, R.string.modify_account);
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.delete_account);
        
        if (!VoXMobile.isVoXMobile(account.proxies))
        	menu.add(0, MENU_ITEM_WIZARD, 0, R.string.choose_wizard);
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return false;
        }
        SipProfile account = (SipProfile) adapter.getItem(info.position);
        
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
            	database.open();
        		database.deleteAccount(account);
        		database.close();
				reloadAsyncAccounts(account.id, 0);
                return true;
            }
            case MENU_ITEM_MODIFY : {
        			Intent it = new Intent(this, BasePrefsWizard.class);
        			it.putExtra(Intent.EXTRA_UID,  (int) account.id);
        			it.putExtra(SipProfile.FIELD_WIZARD, account.wizard);
        			startActivityForResult(it, REQUEST_MODIFY);
        		return true;
            }
            case MENU_ITEM_ACTIVATE: {
            	account.active = ! account.active;
            	database.open();
    			database.setAccountActive(account.id, account.active);
            	database.close();
			//	reloadAsyncAccounts(account.id, account.active?1:0);
				return true;
            }
            case MENU_ITEM_WIZARD:{
            	Intent it = new Intent(this, WizardChooser.class);
            	it.putExtra(Intent.EXTRA_UID, (int) account.id);
            	startActivityForResult(it, CHANGE_WIZARD);
            	return true;
            }
        }
        return false;
    }
    
    
    private synchronized void updateList() {
    	
    //	Log.d(THIS_FILE, "We are updating the list");
    	if(database == null) {
    		database = new DBAdapter(this);
    	}
    	
    	database.open();
		accountsList = database.getListAccounts();
		database.close();
    	
    	if(adapter == null) {
    		adapter = new AccountAdapter(this, accountsList);
    		adapter.setNotifyOnChange(false);
    	}else {
    		adapter.clear();
    		for(SipProfile acc : accountsList){
    			adapter.add(acc);
    		}
    		adapter.notifyDataSetChanged();
    	}
    }
    
    
    private void clickAddAccount() {
    	LinearLayout btn = (LinearLayout) findViewById(R.id.add_account);
    	btn.setEnabled(false);
    	
    	if (!mVoXActivityLock && mVoXObserver.checkSupported(true)) {
    		mVoXActivityLock = true;
    		startActivityForResult(new Intent(AccountsList.this, AccountsListActivity.class), VOX_ACCOUNTS);
    	}
    }	

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		SipProfile account = adapter.getItem(position);
		Intent intent = new Intent(this, BasePrefsWizard.class);
		if(account.id != SipProfile.INVALID_ID) {
			intent.putExtra(Intent.EXTRA_UID,  (int) account.id);
		}
		intent.putExtra(SipProfile.FIELD_WIZARD, account.wizard);
		
		startActivityForResult(intent, REQUEST_MODIFY);
		
	}

	
	/**
	 * FOr now appears when we come back from a add/modify 
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data); 
		switch(requestCode){
		case VOX_ACCOUNTS:
			mVoXActivityLock = false;
			LinearLayout btn = (LinearLayout) findViewById(R.id.add_account);
			btn.setEnabled(true);
			break;
		case CHOOSE_WIZARD:
			if(resultCode == RESULT_OK) {
				if(data != null) {
					String wizardId = data.getStringExtra(WizardUtils.ID);
					if(wizardId != null) {
						Intent intent = new Intent(this, BasePrefsWizard.class);
						intent.putExtra(SipProfile.FIELD_WIZARD, wizardId);
						startActivityForResult(intent, REQUEST_MODIFY);
					}
				}
			}
			break;
		case REQUEST_MODIFY:
			if(resultCode == RESULT_OK){
				handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
				/*
				int accId = data.getIntExtra(Intent.EXTRA_UID, -1);
				if(accId != -1) {
					reloadAsyncAccounts(accId, 1);
				}else {
					reloadAsyncAccounts(null, 1);
				}
				*/
			}
			break;
		case CHANGE_WIZARD:
			if(resultCode == RESULT_OK) {
				if(data != null && data.getExtras() != null) {
					String wizardId = data.getStringExtra(WizardUtils.ID);
					int accountId = data.getIntExtra(Intent.EXTRA_UID, SipProfile.INVALID_ID);
					if(wizardId != null && accountId != SipProfile.INVALID_ID) {
						database.open();
						database.setAccountWizard(accountId, wizardId);
						database.close();
						handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
					}
				}
			}
			break;
		}
	}
	

	private abstract class OnServiceConnect{
		protected abstract void serviceConnected(); 
	}
	private OnServiceConnect onServiceConnect = null;
	private void reloadAsyncAccounts(final Integer accountId, final Integer renew) {
		//Force reflush accounts
		Log.d(THIS_FILE, "Reload async accounts "+accountId+" renew : "+renew);
		onServiceConnect = new OnServiceConnect() {
			@Override
			protected void serviceConnected() {
				if (service != null) {
					Log.d(THIS_FILE, "Will reload all accounts !");
					try {
						//Ensure sip service is started
						service.sipStart();
						
						if(accountId == null) {
							service.reAddAllAccounts();
						}else {
							service.setAccountRegistration(accountId, renew);
						}
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Impossible to reload accounts", e);
					}finally {
						Log.d(THIS_FILE, "> Need to update list !");
						handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
					}
				}
			}
		};
		
		Thread t = new Thread("ReloadAccounts") {
			@Override
			public void run() {
				Log.d(THIS_FILE, "Would like to reload all accounts");
				if(service != null) {
					onServiceConnect.serviceConnected();
					onServiceConnect = null;
				}
			};
		};
		t.start();
	}
	
	private static final class AccountListItemViews {
		TextView labelView;
		TextView statusView;
		View indicator;
		CheckBox activeCheckbox;
		ImageView barOnOff;
		int accountPosition;
	}
	
	
	class AccountAdapter extends ArrayAdapter<SipProfile> implements OnClickListener {
		Activity context;
		private HashMap<Integer, AccountStatusDisplay> cacheStatusDisplay;
		
		AccountAdapter(Activity context, List<SipProfile> list) {
			super(context, R.layout.accounts_list_item, list);
			this.context = context;
			cacheStatusDisplay = new HashMap<Integer, AccountStatusDisplay>();
		}
		
		@Override
		public void notifyDataSetChanged() {
			cacheStatusDisplay.clear();
			super.notifyDataSetChanged();
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
			//Create view if not existant
			View view = convertView;
            if (view == null) {
                LayoutInflater viewInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = viewInflater.inflate(R.layout.accounts_list_item, parent, false);
                
                AccountListItemViews tagView = new AccountListItemViews();
                tagView.labelView = (TextView)view.findViewById(R.id.AccTextView);
                tagView.indicator = view.findViewById(R.id.indicator);
                tagView.activeCheckbox = (CheckBox)view.findViewById(R.id.AccCheckBoxActive);
                tagView.statusView =  (TextView) view.findViewById(R.id.AccTextStatusView);
                tagView.barOnOff = (ImageView) tagView.indicator.findViewById(R.id.bar_onoff);
                
                view.setTag(tagView);
                
                tagView.indicator.setOnClickListener(this);
                tagView.indicator.setTag(view);
            }
            
            
            bindView(view, position);
           
	        return view;
	        
	    }
		
		
		public void bindView(View view, int position) {
			AccountListItemViews tagView = (AccountListItemViews) view.getTag();
			tagView.accountPosition = position;
			view.setTag(tagView);
			
			
			// Get the view object and account object for the row
	        final SipProfile account = getItem(position);
	        if (account == null){
	        	return;
	        }
	        AccountStatusDisplay accountStatusDisplay = null;
			accountStatusDisplay = (AccountStatusDisplay) cacheStatusDisplay.get(position);
			if(accountStatusDisplay == null) {
				//In an ideal world, should be threaded
				accountStatusDisplay = AccountListUtils.getAccountDisplay(context, service, account.id);
				cacheStatusDisplay.put(position, accountStatusDisplay);
			}
			
			tagView.labelView.setText(account.display_name);
            
            //Update status label and color
			tagView.statusView.setText(accountStatusDisplay.statusLabel);
			tagView.labelView.setTextColor(accountStatusDisplay.statusColor);
            
            //Update checkbox selection
			tagView.activeCheckbox.setChecked( account.active );
			tagView.barOnOff.setImageResource( account.active ? accountStatusDisplay.checkBoxIndicator : R.drawable.ic_indicator_off );
            
            //Update account image
            final WizardInfo wizardInfos = WizardUtils.getWizardClass(account.wizard);
            if(wizardInfos != null) {
            	tagView.activeCheckbox.setBackgroundResource(wizardInfos.icon);
            }
		}


		@Override
		public void onClick(View view) {
			AccountListItemViews tagView = (AccountListItemViews) ((View)view.getTag()).getTag();
			
			final SipProfile account = getItem(tagView.accountPosition);
			if(account == null) {
				return;
			}
			tagView.activeCheckbox.toggle();
			
			
			boolean isActive = tagView.activeCheckbox.isChecked();
			account.active = ! account.active;
			

			//Update visual
			/* -- Not need cause setAccount active will broadcast finally ACTION_SIP_ACCOUNT_ACTIVE_CHANGED
			tagView.barOnOff.setImageResource(account.active ? R.drawable.ic_indicator_yellow : R.drawable.ic_indicator_off);
			tagView.labelView.setTextColor(account.active ? getResources().getColor(R.color.account_unregistered) : getResources().getColor(R.color.account_inactive) );
			tagView.statusView.setText(getResources().getText(R.string.acct_unregistered));
			*/
			//Update database and reload accounts
			database.open();
			database.setAccountActive(account.id, isActive);
			database.close();
		//	reloadAsyncAccounts(account.id, account.active?1:0);
		}

	}
	
	

	// Service connection
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			if(onServiceConnect != null) {
				Thread t = new Thread("Service-connected") {
					public void run() {
						onServiceConnect.serviceConnected();
						onServiceConnect = null;
					};
				};
				t.start();
			}
			handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
    };
    
   	private BroadcastReceiver registrationStateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			//Log.d(THIS_FILE, "Received a registration update");
			updateList();
		}
	};
	
	// Ui handler
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NEED_LIST_UPDATE:
				updateList();
				break;
			case UPDATE_LINE:
				
			default:
				super.handleMessage(msg);
			}
		}
	};
	
	// Gesture detector
	private class BackGestureDetector extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if(e1 == null || e2 == null) {
				return false;
			}
			float deltaX = e2.getX() - e1.getX();
			float deltaY = e2.getY() - e1.getY();
			
			if(deltaX > 0 && deltaX > Math.abs(deltaY * 3) ) {
				finish();
				return true;
			}
			return false;
		}
	}
	public static final int ADD_VOX_MENU = Menu.FIRST + 1;
	public static final int ADD_MENU = Menu.FIRST + 2;
	public static final int REORDER_MENU = Menu.FIRST + 3;
	public static final int BACKUP_MENU = Menu.FIRST + 4;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, ADD_VOX_MENU, Menu.NONE, R.string.voxmobile_add_vox_account).setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, ADD_MENU, Menu.NONE, R.string.voxmobile_add_other_account).setIcon(android.R.drawable.ic_menu_add);
		menu.add(Menu.NONE, REORDER_MENU, Menu.NONE, R.string.reorder).setIcon(android.R.drawable.ic_menu_sort_by_size);
		menu.add(Menu.NONE, BACKUP_MENU, Menu.NONE, R.string.backup_restore).setIcon(android.R.drawable.ic_menu_save);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ADD_VOX_MENU:
			clickAddAccount();
			return true;
		case ADD_MENU:
			startActivityForResult(new Intent(AccountsList.this, WizardChooser.class), CHOOSE_WIZARD);
			return true;
		case REORDER_MENU:
			startActivityForResult(new Intent(this, ReorderAccountsList.class), REQUEST_MODIFY);
			return true;
		case BACKUP_MENU:
			
			//Populate choice list
			List<String> items = new ArrayList<String>();
			items.add(getResources().getString(R.string.backup));
			final File backupDir = PreferencesWrapper.getConfigFolder(this);
			if(backupDir != null) {
				String[] filesNames = backupDir.list();
				for(String fileName : filesNames) {
					items.add(fileName);
				}
			}
			
			final String[] fItems = (String[]) items.toArray(new String[0]);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.backup_restore);
			builder.setItems(fItems, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	if(item == 0) {
			    		SipProfileJson.saveSipConfiguration(AccountsList.this);
			    	}else {
						File fileToRestore = new File(backupDir + File.separator + fItems[item]);
			    		SipProfileJson.restoreSipConfiguration(AccountsList.this, fileToRestore);
			    		reloadAsyncAccounts(null, null);
			    		handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
			    	}
			    }
			});
			builder.setCancelable(true);
			AlertDialog backupDialog = builder.create();
			backupDialog.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
