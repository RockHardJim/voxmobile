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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

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
import net.voxcorp.voxmobile.provider.DBContract.ProvisionCheckContract;
import net.voxcorp.voxmobile.ui.TrackedTabActivity;
import net.voxcorp.voxmobile.ui.rates.RatesActivity;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.OrderHelper;
import net.voxcorp.widgets.IndicatorTab;
import net.voxcorp.wizards.BasePrefsWizard;
import net.voxcorp.wizards.WizardUtils.WizardInfo;
import net.voxcorp.wizards.impl.VoXMobile;

public class SipHome extends TrackedTabActivity {
	public static final int ACCOUNTS_MENU = Menu.FIRST + 1;
	public static final int PARAMS_MENU = Menu.FIRST + 2;
	public static final int CLOSE_MENU = Menu.FIRST + 3;
	public static final int HELP_MENU = Menu.FIRST + 4;
	public static final int DISTRIB_ACCOUNT_MENU = Menu.FIRST + 5;
	public static final int INVITE_MENU = Menu.FIRST + 6;
	public static final int RATES_MENU = Menu.FIRST + 7;

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
	
	private boolean has_tried_once_to_activate_account = false;
//	private ImageButton pickupContact;


	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
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
		
		OrderHelper.reset(this);

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
		if(CustomDistribution.distributionWantsOtherAccounts()) {
			menu.add(Menu.NONE, ACCOUNTS_MENU, Menu.NONE, (distribWizard == null)?R.string.accounts:R.string.other_accounts).setIcon(R.drawable.ic_menu_accounts);
		}
		menu.add(Menu.NONE, PARAMS_MENU, Menu.NONE, R.string.prefs).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, HELP_MENU, Menu.NONE, R.string.help).setIcon(android.R.drawable.ic_menu_help);
		menu.add(Menu.NONE, INVITE_MENU, Menu.NONE, R.string.voxmobile_invite).setIcon(android.R.drawable.ic_menu_send);
		menu.add(Menu.NONE, RATES_MENU, Menu.NONE, R.string.voxmobile_rates).setIcon(R.drawable.ic_voxmobile_rates_menu);
		menu.add(Menu.NONE, CLOSE_MENU, Menu.NONE, R.string.menu_disconnect).setIcon(R.drawable.ic_lock_power_off);

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

	//	PreferencesWrapper prefsWrapper = new PreferencesWrapper(this);
	//	menu.findItem(CLOSE_MENU).setVisible(!prefsWrapper.isValidConnectionForIncoming());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		populateMenu(menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SipProfile account = null;
		DBAdapter db = null;
		switch (item.getItemId()) {
		case RATES_MENU:
			startActivity(new Intent(this, RatesActivity.class));
			return true;
		case INVITE_MENU:
			db = new DBAdapter(this);
			db.open();

			List<SipProfile> accounts = db.getListAccounts();
			Iterator<SipProfile> iterator = accounts.iterator();
			while (iterator.hasNext()) {
				SipProfile sp = iterator.next();
				if (VoXMobile.isVoXMobile(sp.proxies)) {
					account = sp;
					if (sp.active) break;
				}
			}
			db.close();
			if (account == null) {
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
			db = new DBAdapter(this);
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
}
