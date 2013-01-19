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

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import net.voxcorp.R;
import net.voxcorp.api.SipConfigManager;
import net.voxcorp.api.SipManager;
import net.voxcorp.api.SipProfile;
import net.voxcorp.db.DBAdapter;
import net.voxcorp.service.SipService;
import net.voxcorp.ui.help.Help;
import net.voxcorp.ui.messages.ConversationList;
import net.voxcorp.ui.prefs.MainPrefs;
import net.voxcorp.ui.prefs.PrefsFast;
import net.voxcorp.utils.Compatibility;
import net.voxcorp.utils.CustomDistribution;
import net.voxcorp.utils.Log;
import net.voxcorp.utils.NightlyUpdater;
import net.voxcorp.utils.NightlyUpdater.UpdaterPopupLauncher;
import net.voxcorp.utils.PreferencesProviderWrapper;
import net.voxcorp.utils.PreferencesWrapper;
import net.voxcorp.voxmobile.provider.DBContract.AccountContract;
import net.voxcorp.voxmobile.provider.DBContract.ProvisionCheckContract;
import net.voxcorp.voxmobile.service.ServiceHelper;
import net.voxcorp.voxmobile.types.AccountSearch;
import net.voxcorp.voxmobile.ui.SipAccountsListActivity;
import net.voxcorp.voxmobile.ui.TopUpPrepaidMain;
import net.voxcorp.voxmobile.ui.TrackedTabActivity;
import net.voxcorp.voxmobile.ui.rates.RatesActivity;
import net.voxcorp.voxmobile.types.VersionCheck;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.widgets.IndicatorTab;
import net.voxcorp.wizards.BasePrefsWizard;
import net.voxcorp.wizards.WizardUtils.WizardInfo;
import net.voxcorp.wizards.impl.VoXMobile;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

public class SipHome extends TrackedTabActivity {
	public static final int ACCOUNTS_MENU = Menu.FIRST + 1;
	public static final int PARAMS_MENU = Menu.FIRST + 2;
	public static final int CLOSE_MENU = Menu.FIRST + 3;
	public static final int HELP_MENU = Menu.FIRST + 4;
	public static final int DISTRIB_ACCOUNT_MENU = Menu.FIRST + 5;
	public static final int INVITE_MENU = Menu.FIRST + 6;
	public static final int RATES_MENU = Menu.FIRST + 7;
	public static final int MANAGE_ACCOUNT_MENU = Menu.FIRST + 8;
	public static final int TOP_UP_MENU = Menu.FIRST + 9;

	public static final String LAST_KNOWN_VERSION_PREF = "last_known_version";
	public static final String LAST_KNOWN_ANDROID_VERSION_PREF = "last_known_aos_version";
	public static final String HAS_ALREADY_SETUP = "has_already_setup";

	private static final String THIS_FILE = "SIP_HOME";
	
	private static final String TAB_DIALER = "dialer";
	private static final String TAB_CALLLOG = "calllog";
	private static final String TAB_MESSAGES = "messages";
	
//	protected static final int PICKUP_PHONE = 0;
	private static final int REQUEST_EDIT_DISTRIBUTION_ACCOUNT = 0; //PICKUP_PHONE + 1;

	private Intent serviceIntent;

	private Intent dialerIntent,calllogsIntent, messagesIntent;
	private PreferencesWrapper prefWrapper;
	private PreferencesProviderWrapper prefProviderWrapper;
	private String mRestError;
	
	private boolean has_tried_once_to_activate_account = false;
//	private ImageButton pickupContact;

	private IntentFilter accountChangedFilter = new IntentFilter(SipManager.ACTION_ACCOUNT_CHANGED);
	private int activeAccountId = SipProfile.INVALID_ID;
	private BroadcastReceiver accountChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(SipManager.ACTION_ACCOUNT_CHANGED)) {
				activeAccountId = intent.getIntExtra("ACCOUNT_ID", SipProfile.INVALID_ID);
			}
		}
	};

	// Set up account search handler for RESTing
	private static int ACCOUNT_SEARCH_SUCCESS = -1;
	static class AccountSearchHandler extends Handler {
		private WeakReference<SipHome> mRef;

		AccountSearchHandler(SipHome obj) {
			super();
			mRef = new WeakReference<SipHome>(obj);
		}
		
		@Override
		public void handleMessage(Message msg) {
			final SipHome a = mRef.get();

    		if (a == null) {
    			return;
    		}

    		a.mRestError = msg.getData().getString("error");
			if (msg.arg1 != ACCOUNT_SEARCH_SUCCESS) {
				a.showDialog(msg.arg1);
			}
		}
	};
	private AccountSearchHandler mAccountSearchHandler = new AccountSearchHandler(this);
	
	// Set up version check handler for RESTing
	static class VersionCheckHandler extends Handler {
		private WeakReference<SipHome> mRef;

		VersionCheckHandler(SipHome obj) {
			super();
			mRef = new WeakReference<SipHome>(obj);
		}

		@Override
		public void handleMessage(Message msg) {
			final SipHome a = mRef.get();

    		if (a == null) {
    			return;
    		}

    		if (msg.arg1 != VOX_MOBILE_VERSION_CHECK_SUCCESS) {
    			Log.d(THIS_FILE, "PANIC!!! Cannget Get Version Check");
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
    			Editor editor = prefs.edit();
    			editor.putBoolean("VOXMOBILE_SUPPORTED", false);
    			a.showDialog(msg.arg1);
    			editor.commit();
    			return;
    		}

   			a.mVersionCheck = msg.getData().getParcelable("version_check");

			String apiSupported = a.mVersionCheck.api_supported ? "YES" : "NO";
			String buildSupported = a.mVersionCheck.build_supported ? "YES" : "NO";
			Log.d(THIS_FILE, String.format("Version Check Result [API Supported: %s] [Build Supported: %s]", apiSupported, buildSupported));

   			boolean supported = a.mVersionCheck.api_supported && a.mVersionCheck.build_supported; 

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
			Editor editor = prefs.edit();
   			editor.putBoolean("VOXMOBILE_SUPPORTED", supported);
			editor.commit();

			if (supported) {
				a.checkNoAccounts();
			}
		}
	};
	private VersionCheckHandler mVersionCheckHandler = new VersionCheckHandler(this);
	private VersionCheck mVersionCheck;
	private static final int VOX_MOBILE_VERSION_CHECK_SUCCESS = -100;

	private void doVoXMobileVersionCheck() {

		Runnable runnable = new Runnable() {

			@Override
			public void run() {

				Message msg = new Message();
				try {
					ServiceHelper helper = new ServiceHelper(SipHome.this);
					VersionCheck reply = helper.checkVersion();

					Bundle data = new Bundle();
					data.putParcelable("version_check", reply);
					msg.setData(data);

					if (reply.httpStatus == HttpStatus.UNAUTHORIZED.value()) {
						msg.arg1 = Consts.REST_UNAUTHORIZED;
					} else if (!reply.success && reply.httpStatus == HttpStatus.OK.value()) {
						msg.arg1 = Consts.REST_ERROR;
					} else if (reply.httpStatus == 0) {
						msg.arg1 = Consts.REST_ERROR;
					} else if (reply.httpStatus != HttpStatus.OK.value()) {
						msg.arg1 = Consts.REST_HTTP_ERROR;
					} else if (!reply.api_supported || !reply.build_supported) {
						msg.arg1 = Consts.REST_UNSUPPORTED;
					} else {
						msg.arg1 = VOX_MOBILE_VERSION_CHECK_SUCCESS;
					}

				} catch (Exception e) {
					msg.arg1 = Consts.REST_ERROR;
				} finally {
					mVersionCheckHandler.sendMessage(msg);
				}
			}
		};
		new Thread(runnable).start();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)  {
		
		registerReceiver(accountChangedReceiver, accountChangedFilter);
		
		prefWrapper = new PreferencesWrapper(this);
		prefProviderWrapper = new PreferencesProviderWrapper(this);
		super.onCreate(savedInstanceState);
		
		// BUNDLE MODE -- upgrade settings
		Integer runningVersion = needUpgrade();
		if(runningVersion != null) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = prefs.edit();
			editor.putInt(SipHome.LAST_KNOWN_VERSION_PREF, runningVersion);
			editor.commit();
		}
		

		setContentView(R.layout.home);
		

		dialerIntent = new Intent(this, Dialer.class);
		calllogsIntent = new Intent(this, CallLogsList.class);
		messagesIntent = new Intent(this, ConversationList.class);

		addTab(TAB_DIALER, getString(R.string.dial_tab_name_text), R.drawable.ic_tab_unselected_dialer, R.drawable.ic_tab_selected_dialer, dialerIntent);
		addTab(TAB_CALLLOG, getString(R.string.calllog_tab_name_text), R.drawable.ic_tab_unselected_recent, R.drawable.ic_tab_selected_recent, calllogsIntent);
		if(CustomDistribution.supportMessaging()) {
			addTab(TAB_MESSAGES, getString(R.string.messages_tab_name_text), R.drawable.ic_tab_unselected_messages, R.drawable.ic_tab_selected_messages, messagesIntent);
		}
		/*
		pickupContact = (ImageButton) findViewById(R.id.pickup_contacts);
		pickupContact.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(Compatibility.getContactPhoneIntent(), PICKUP_PHONE);
			}
		});
		*/
		
		has_tried_once_to_activate_account = false;

		if(!prefWrapper.getPreferenceBooleanValue(SipConfigManager.PREVENT_SCREEN_ROTATION)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
		
		selectTabWithAction(getIntent());
		Log.setLogLevel(prefWrapper.getLogLevel());
		
		// Async check
		
		Thread t = new Thread() {
			public void run() {
				asyncSanityCheck();
			};
		};
		t.start();

		// Kick off provision check task
        getContentResolver().update(ProvisionCheckContract.CONTENT_URI, null, null, null);
	}
	
	/**
	 * Check wether an upgrade is needed
	 * @return null if not needed, else the new version to upgrade to
	 */
	private Integer needUpgrade() {
		Integer runningVersion = null;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// Application upgrade
		PackageInfo pinfo = PreferencesProviderWrapper.getCurrentPackageInfos(this);
		if(pinfo != null) {
			runningVersion = pinfo.versionCode;
			int lastSeenVersion = prefs.getInt(LAST_KNOWN_VERSION_PREF, 0);
	
			Log.d(THIS_FILE, "Last known version is " + lastSeenVersion + " and currently we are running " + runningVersion);
			if (lastSeenVersion != runningVersion) {
				Compatibility.updateVersion(prefWrapper, lastSeenVersion, runningVersion);
			}else {
				runningVersion = null;
			}
		}
		
		// Android upgrade
		{
			int lastSeenVersion = prefs.getInt(LAST_KNOWN_ANDROID_VERSION_PREF, 0);
			Log.d(THIS_FILE, "Last known android version "+lastSeenVersion);
			if(lastSeenVersion != Compatibility.getApiLevel()) {
				Compatibility.updateApiVersion(prefWrapper, lastSeenVersion, Compatibility.getApiLevel());
				Editor editor = prefs.edit();
				editor.putInt(SipHome.LAST_KNOWN_ANDROID_VERSION_PREF, Compatibility.getApiLevel());
				editor.commit();
			}
		}
		return runningVersion;
	}
	
	private void asyncSanityCheck() {
//		if(Compatibility.isCompatible(9)) {
//			// We check now if something is wrong with the gingerbread dialer integration
//			Compatibility.getDialerIntegrationState(SipHome.this);
//		}

		PackageInfo pinfo = PreferencesProviderWrapper.getCurrentPackageInfos(this);
		if(pinfo != null) {
			if(pinfo.applicationInfo.icon == R.drawable.ic_launcher_nightly) {
				Log.d(THIS_FILE, "Sanity check : we have a nightly build here");
				ConnectivityManager connectivityService = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				NetworkInfo ni = connectivityService.getActiveNetworkInfo();
				// Only do the process if we are on wifi
				if(ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI) {
					// Only do the process if we didn't dismissed previously
					NightlyUpdater nu = new NightlyUpdater(this);
					
					if(!nu.ignoreCheckByUser()) {
						long lastCheck = nu.lastCheck();
						long current = System.currentTimeMillis();
						long oneDay = 43200000; // 12 hours
						if(current - oneDay > lastCheck) {
							if(onForeground) {
								// We have to check for an update
								UpdaterPopupLauncher ru = nu.getUpdaterPopup(false);
								if(ru != null) {	
									runOnUiThread(ru);
								}
							}
						}
					}
				}
			}
		}
	}

	private void startSipService() {
		if (serviceIntent == null) {
			serviceIntent = new Intent(this, SipService.class);
		}
		Thread t = new Thread("StartSip") {
			public void run() {
				startService(serviceIntent);
				postStartSipService();
			};
		};
		t.start();

	}
	
	private void postStartSipService() {
		// If we have never set fast settings
		if(CustomDistribution.showFirstSettingScreen()) {
			if (!prefWrapper.hasAlreadySetup()) {
				Intent prefsIntent = new Intent(this, PrefsFast.class);
				prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(prefsIntent);
				return;
			}
		}else {
			boolean doFirstParams = !prefWrapper.hasAlreadySetup();
			prefWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, true);
			if(doFirstParams) {
				Compatibility.setFirstRunParameters(prefWrapper);
			}
		}
        doVoXMobileVersionCheck();
	}

	private void checkNoAccounts() {
		// If we have no account yet, open account panel,
		if (!has_tried_once_to_activate_account) {
			SipProfile account = null;
			DBAdapter db = new DBAdapter(this);
			db.open();
			int nbrOfAccount = db.getNbrOfAccount();
			
			if (nbrOfAccount == 0) {
				WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
				if(distribWizard != null) {
					account = db.getAccountForWizard(distribWizard.id);
				}
			}
			
			db.close();
			
			if(nbrOfAccount == 0) {
				Intent accountIntent = null;
				if(account != null) {
					if(account.id == SipProfile.INVALID_ID) {
						accountIntent = new Intent(this, BasePrefsWizard.class);
						accountIntent.putExtra(SipProfile.FIELD_WIZARD, account.wizard);
						startActivity(new Intent(this, AccountsList.class));
					}
				}else {
					accountIntent = new Intent(this, AccountsList.class);
				}
				
				if(accountIntent != null) {
					accountIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(accountIntent);
					has_tried_once_to_activate_account = true;
					return;
				}
			}
			has_tried_once_to_activate_account = true;
		}
	}
	
	private Method setIndicatorMethod = null;
	
	private void addTab(String tag, String label, int icon, int ficon, Intent content) {
		TabHost tabHost = getTabHost();
		TabSpec tabspecDialer = tabHost.newTabSpec(tag).setContent(content);

		boolean fails = true;
		if (Compatibility.isCompatible(4)) {
			IndicatorTab icTab = new IndicatorTab(this, null);
			icTab.setResources(label, icon, ficon);
			try {
				if(setIndicatorMethod == null) {
					setIndicatorMethod = tabspecDialer.getClass().getDeclaredMethod("setIndicator", View.class);
				}
				setIndicatorMethod.invoke(tabspecDialer, icTab);
				fails = false;
			} catch (Exception e) {
				Log.d(THIS_FILE, "We are probably on 1.5 : use standard simple tabs");
			}

		}
		if (fails) {
			tabspecDialer.setIndicator(label, getResources().getDrawable(icon));
		}

		tabHost.addTab(tabspecDialer);
	}
	
	boolean onForeground = false;

	@Override
	protected void onPause() {
		Log.d(THIS_FILE, "On Pause SIPHOME");
		onForeground = false;
		super.onPause();
		
	}

	@Override
	protected void onResume() {
		Log.d(THIS_FILE, "On Resume SIPHOME");
		super.onResume();
		onForeground = true;
		
		prefWrapper.setQuit(false);

		boolean isDebuggable = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
		if (isDebuggable && Consts.MODE_ACTIVE == Consts.MODE_PRODUCTION) {
			new AlertDialog.Builder(this)
			.setTitle(R.string.warning)
			.setMessage(getString(R.string.voxmobile_corrupted))
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					disconnectAndQuit();
				}
			})
			.show();
			return;
		}

		Log.d(THIS_FILE, "WE CAN NOW start SIP service");
		startSipService();

		
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		selectTabWithAction(intent);
	}

	private void selectTabWithAction(Intent intent) {
		if(intent != null) {
			String callAction = intent.getAction();
			if(SipManager.ACTION_SIP_CALLLOG.equalsIgnoreCase(callAction)) {
				getTabHost().setCurrentTab(1);
			}else if(SipManager.ACTION_SIP_DIALER.equalsIgnoreCase(callAction)) {
				getTabHost().setCurrentTab(0);
			}else if(SipManager.ACTION_SIP_MESSAGES.equalsIgnoreCase(callAction)) {
				if(CustomDistribution.supportMessaging()) {
					getTabHost().setCurrentTab(2);
				}
			}
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(accountChangedReceiver);
		super.onDestroy();
		Log.d(THIS_FILE, "---DESTROY SIP HOME END---");
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
	    if (keyCode == KeyEvent.KEYCODE_BACK 
	    		&& event.getRepeatCount() == 0
	    		&& !Compatibility.isCompatible(5)) {
	    	onBackPressed();
	    	
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	public void onBackPressed() {
		if(prefProviderWrapper != null) {
			Log.d(THIS_FILE, "On back pressed ! ");
    		//ArrayList<String> networks = prefWrapper.getAllIncomingNetworks();
			//if (networks.size() == 0) {
			if( ! prefProviderWrapper.isValidConnectionForIncoming()) {
				disconnectAndQuit();
				return;
			}
    	}
		finish();
	}

	private void populateMenu(Menu menu) {
		WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
		if(distribWizard != null) {
			menu.add(Menu.NONE, DISTRIB_ACCOUNT_MENU, Menu.NONE, "My " + distribWizard.label).setIcon(distribWizard.icon);
		}
		menu.add(Menu.NONE, TOP_UP_MENU, Menu.NONE, R.string.voxmobile_top_up).setIcon(R.drawable.ic_voxmobile_menu_topup);
		menu.add(Menu.NONE, MANAGE_ACCOUNT_MENU, Menu.NONE, R.string.voxmobile_manage_account).setIcon(R.drawable.ic_voxmobile_menu_manage);
		if(CustomDistribution.distributionWantsOtherAccounts()) {
			menu.add(Menu.NONE, ACCOUNTS_MENU, Menu.NONE, (distribWizard == null)?R.string.accounts:R.string.other_accounts).setIcon(R.drawable.ic_menu_accounts);
		}	
		menu.add(Menu.NONE, RATES_MENU, Menu.NONE, R.string.voxmobile_rates).setIcon(R.drawable.ic_voxmobile_rates_menu);
		menu.add(Menu.NONE, INVITE_MENU, Menu.NONE, R.string.voxmobile_invite).setIcon(android.R.drawable.ic_menu_send);
		menu.add(Menu.NONE, PARAMS_MENU, Menu.NONE, R.string.prefs).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, HELP_MENU, Menu.NONE, R.string.help).setIcon(android.R.drawable.ic_menu_help);
		menu.add(Menu.NONE, CLOSE_MENU, Menu.NONE, R.string.menu_disconnect).setIcon(R.drawable.ic_lock_power_off);

	}

	private SipProfile getActiveSipProfile() {
		SipProfile sp = null;
		if (activeAccountId != SipProfile.INVALID_ID) {
			DBAdapter database = new DBAdapter(this);
			database.open();
			List<SipProfile> accountsList = database.getListAccounts(true);
			database.close();
			for (final SipProfile a : accountsList) {
				if (a.id == activeAccountId) {
					sp = a;
					break;
				}			
			}
		}
		return sp;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// VoX menu items may not always be present
		MenuItem miVoXAccount = menu.findItem(MANAGE_ACCOUNT_MENU);
		MenuItem miVoXTopUp = menu.findItem(TOP_UP_MENU);
		if (miVoXAccount != null) {
			boolean isVoXMobile = false;
			boolean isPrepaid = false;

			SipProfile sp = getActiveSipProfile();
			if ((sp != null) && (sp.id != SipProfile.INVALID_ID)) {
				isVoXMobile = VoXMobile.isVoXMobile(sp.proxies);
				isPrepaid = 
						VoXMobile.getAccountType(sp.wizard) == VoXMobile.VoXAccountType.PAYGO ||
						VoXMobile.getAccountType(sp.wizard) == VoXMobile.VoXAccountType.PREPAID;
			}

			miVoXAccount.setVisible(isVoXMobile);
			miVoXTopUp.setVisible(isPrepaid);
		}

	//	PreferencesWrapper prefsWrapper = new PreferencesWrapper(this);
	//	menu.findItem(CLOSE_MENU).setVisible(!prefsWrapper.isValidConnectionForIncoming());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		populateMenu(menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	private void voxmobileAccountAction(boolean requestTopUp) {
		final ProgressDialog mProgress = ProgressDialog.show(this, "", getString(R.string.voxmobile_please_wait), true, false);
		final boolean isTopUp = requestTopUp; 
		final SipProfile account = getActiveSipProfile();
		final String username = account.username;

		if ((account != null) && (account.id != SipProfile.INVALID_ID) && VoXMobile.isVoXMobile(account.proxies)) {
			Runnable runnable = new Runnable() {
				private Context mContext;
				
				private String[] getUuids() {
					// Build array of all existing account UUIDs
					ArrayList<String> uuid_list = new ArrayList<String>();
					Cursor c = managedQuery(
							AccountContract.CONTENT_URI,
							AccountContract.PROJECTION, null, null, null);
					c.moveToFirst();
					while (!c.isAfterLast()) {
						uuid_list.add(c.getString(AccountContract.UUID_INDEX));
						c.moveToNext();
					}

					if (uuid_list.size() == 0) {
						return null;
					}

					return uuid_list.toArray(new String[uuid_list.size()]);
				}

				@Override
				public void run() {
					mContext = SipHome.this;
					String[] list = getUuids();
					if (list == null) return;

					Message msg = new Message();
					try {
						ServiceHelper helper = new ServiceHelper(mContext);
						AccountSearch reply = helper.accountSearch(username, list);

						Bundle data = new Bundle();
						data.putString("error", reply.error);
						msg.setData(data);

						if (reply.httpStatus == HttpStatus.UNAUTHORIZED.value()) {
							msg.arg1 = Consts.REST_UNAUTHORIZED;
						} else if (reply.httpStatus == 0) {
							msg.arg1 = Consts.REST_ERROR;
						} else if (reply.httpStatus != HttpStatus.OK.value()) {
							msg.arg1 = Consts.REST_HTTP_ERROR;
						} else {
							Intent intent;
							if (isTopUp) {
								intent = new Intent(SipHome.this, TopUpPrepaidMain.class);
								intent.putExtra(AccountContract.UUID, reply.uuid);
								intent.putExtra(AccountContract.ACCOUNT_NO, reply.account_no);
							} else {
								intent = new Intent(SipHome.this, SipAccountsListActivity.class);
								intent.putExtra(SipAccountsListActivity.ACTION_MANAGE, true);
								intent.putExtra(SipAccountsListActivity.UUID, reply.uuid);
								intent.putExtra(SipAccountsListActivity.ACCOUNT, reply.account_no);
							}
							startActivity(intent);
							msg.arg1 = ACCOUNT_SEARCH_SUCCESS;
						}

			        } catch (ResourceAccessException eHost) {
			            Log.e(THIS_FILE, "ResourceAccessException while RESTing", eHost);
		                mRestError = getString(R.string.voxmobile_network_error_msg);
		                msg.arg1 = Consts.REST_ERROR;
					} catch (HttpStatusCodeException eHttp) {
			            Log.e(THIS_FILE, "HttpStatusCodeException while RESTing", eHttp);
			            mRestError = eHttp.getStatusText();
			            msg.arg1 = Consts.REST_HTTP_ERROR;
					} catch (Exception e) {
			            Log.e(THIS_FILE, "Problem while RESTing", e);
			            mRestError = e.getCause().getMessage();
						msg.arg1 = Consts.REST_ERROR;
					} finally {
						mAccountSearchHandler.sendMessage(msg);
						mProgress.dismiss();
					}
				}
				
			};
			new Thread(runnable).start();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SipProfile account = null;
		switch (item.getItemId()) {
		case TOP_UP_MENU:
			voxmobileAccountAction(true);
			return true;
		case MANAGE_ACCOUNT_MENU:
			voxmobileAccountAction(false);
			return true;
		case RATES_MENU:
			final String items[] = {
					getString(R.string.voxmobile_rates_free_60),
					getString(R.string.voxmobile_rates_unlimited_60),
					getString(R.string.voxmobile_rates_all) };

			AlertDialog.Builder ab = new AlertDialog.Builder(this)
			.setTitle(getString(R.string.voxmobile_please_select))
			.setNegativeButton(getString(R.string.voxmobile_back), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface d, int choice) {
					d.dismiss();
					}
				})
			.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface d, int choice) {
					d.dismiss();
					Intent intent = new Intent(SipHome.this, RatesActivity.class);
					Bundle bundle = new Bundle();
					switch (choice) {
					case 0:
						bundle.putParcelable(RatesActivity.EXTRA_MODE, RatesActivity.RateMode.FREE_60);
						break;
					case 1:
						bundle.putParcelable(RatesActivity.EXTRA_MODE, RatesActivity.RateMode.UNLIMITED_60);
						break;
					default:
						bundle.putParcelable(RatesActivity.EXTRA_MODE, RatesActivity.RateMode.NORMAL);
					}
					intent.putExtra(RatesActivity.EXTRA_DATA, bundle);
					startActivity(intent);
				}
			});
			ab.show();

			return true;
		case INVITE_MENU:
			account = getActiveSipProfile();
			if ((account == null) || (account.id == SipProfile.INVALID_ID) || !VoXMobile.isVoXMobile(account.proxies)) {
				// No VoX Mobile SIP accounts found
				new AlertDialog.Builder(this)
				.setTitle(R.string.voxmobile_attention)
				.setMessage(getString(R.string.voxmobile_invite_error))
				.setPositiveButton(R.string.voxmobile_yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(SipHome.this, AccountsList.class));
					}
				})
				.setNegativeButton(R.string.voxmobile_no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.show();

				return true;
			}

			String body = getString(R.string.voxmobile_invite_message);
			body += String.format(" %s.", account.username);

			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.voxmobile_invite_subject));
			sendIntent.putExtra(Intent.EXTRA_TEXT, body);
			sendIntent.setType("text/plain");
			startActivity(Intent.createChooser(sendIntent, getString(R.string.voxmobile_invite_title)));
			trackEvent(Consts.VOX_MOBILE_INVITE_EVENT, "yes", 1);
			return true;
		case ACCOUNTS_MENU:
			startActivity(new Intent(this, AccountsList.class));
			return true;
		case PARAMS_MENU:
			startActivity(new Intent(this, MainPrefs.class));
			return true;
		case CLOSE_MENU:
			Log.d(THIS_FILE, "CLOSE");
			if(prefProviderWrapper.isValidConnectionForIncoming()) {
				//Alert user that we will disable for all incoming calls as he want to quit
				new AlertDialog.Builder(this)
					.setTitle(R.string.warning)
					.setMessage(getString(R.string.disconnect_and_incoming_explaination))
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							//prefWrapper.disableAllForIncoming();
							prefWrapper.setQuit(true);
							disconnectAndQuit();
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.show();
			}else {
				ArrayList<String> networks = prefWrapper.getAllIncomingNetworks();
				if (networks.size() > 0) {
					String msg = getString(R.string.disconnect_and_will_restart, TextUtils.join(", ", networks));
					Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
				}
				disconnectAndQuit();
			}
			return true;
		case HELP_MENU:
			startActivity(new Intent(this, Help.class));
			return true;
		case DISTRIB_ACCOUNT_MENU:
			WizardInfo distribWizard = CustomDistribution.getCustomDistributionWizard();
			DBAdapter db = new DBAdapter(this);
			db.open();
			account = db.getAccountForWizard(distribWizard.id);
			db.close();
			
			Intent it = new Intent(this, BasePrefsWizard.class);
			if(account.id != SipProfile.INVALID_ID) {
				it.putExtra(Intent.EXTRA_UID,  (int) account.id);
			}
			it.putExtra(SipProfile.FIELD_WIZARD, account.wizard);
			startActivityForResult(it, REQUEST_EDIT_DISTRIBUTION_ACCOUNT);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void disconnectAndQuit() {
		Log.d(THIS_FILE, "True disconnection...");
		if (serviceIntent != null) {
			//stopService(serviceIntent);
			// we don't not need anymore the currently started sip
			Intent it = new Intent(SipManager.ACTION_SIP_CAN_BE_STOPPED);
			sendBroadcast(it);
		}
		serviceIntent = null;
		finish();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case Consts.REST_UNAUTHORIZED:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_unauthorized_msg))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									checkNoAccounts();
								}
							}).create();
		case Consts.REST_UNSUPPORTED:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(getString(R.string.voxmobile_attention))
					.setMessage(getString(R.string.voxmobile_upgrade))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									checkNoAccounts();
								}
							}).create();
		case Consts.REST_HTTP_ERROR:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(getString(R.string.voxmobile_server_error))
					.setMessage(mRestError)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									removeDialog(Consts.REST_HTTP_ERROR);
									checkNoAccounts();
								}
							}).create();
		case Consts.REST_ERROR:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(getString(R.string.voxmobile_network_error))
					.setMessage(mRestError)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									removeDialog(Consts.REST_ERROR);
									checkNoAccounts();
								}
							}).create();
		}
		return null;
	}
}
