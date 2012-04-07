package net.voxcorp.voxmobile.ui.rates;

import java.util.HashMap;

import net.voxcorp.R;
import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.provider.DBContract.RateCityContract;
import net.voxcorp.voxmobile.provider.DBContract.RateCountryContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.service.RestService;
import net.voxcorp.voxmobile.service.ServiceHelper;
import net.voxcorp.voxmobile.ui.TrackedFragmentActivity;
import net.voxcorp.voxmobile.utils.Consts;

import org.springframework.http.HttpStatus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.view.Menu;

public class RatesActivity extends TrackedFragmentActivity {

	private static final String THIS_FILE = "RatesActivity";
	
	public static String TRIAL_MODE = "trial";

	public static final int MENU_ITEM_RATES = Menu.FIRST;

	private TabsAdapter mTabsAdapter;
	private ViewPager mViewPager;
	private ProgressDialog mProgressDialog = null;
	public boolean mTrialMode;
	private int mLatestRatesVersion = 0;
	private int mLocalRatesVersion = 0;

	private static int mPage = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.voxmobile_rates_activity);
		
		mViewPager = (ViewPager) findViewById(R.id.ViewPager01);
		
		mTrialMode = getIntent().getBooleanExtra(TRIAL_MODE, false);
		if (mTrialMode) {
			setTitle(R.string.voxmobile_trial_mode);
		} else {
			setTitle(R.string.voxmobile_international_rates);
		}
	
		LocalBroadcastManager.getInstance(this).registerReceiver(mProgressReceiver,
			      new IntentFilter("progress"));
	}

	@Override
	protected void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mProgressReceiver);
		super.onDestroy();
	}
	
	private int getLocalRatesVersion() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return prefs.getInt("VOX_RATES", 0);
	}
	
	private int getLatestRatesVersion() {
		ServiceHelper helper = new ServiceHelper(this);
		return helper.ratesCheckVersion().version;
	}	

	@Override
	public boolean onPrepareOptionsMenu(android.support.v4.view.Menu menu) {
		int label = mTrialMode ? R.string.voxmobile_international_rates : R.string.voxmobile_trial_dial_codes;
		menu.clear();
        menu.add(Menu.NONE, MENU_ITEM_RATES, Menu.NONE, label).setIcon(R.drawable.ic_voxmobile_rates_menu);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		mTrialMode = !mTrialMode;
		
		// hack to make sure we still have data, if not then
		// bail and force the user to re-enter the activity
		Tab tab = mTabsAdapter.mActionBar.getTabAt(0);
		if (tab == null) {
			finish();
			return super.onOptionsItemSelected(item);
		}
		Cursor c = getContentResolver().query(
				RateCountryContract.CONTENT_URI, 
				RateCountryContract.PROJECTION,
				RateCountryContract.COUNTRY_GROUP + "=?",
				new String[] { tab.getText().toString() },
				RateCountryContract.COUNTRY);
		int i = c.getCount();
		c.close();
		if (i == 0) {
			finish();
			return super.onOptionsItemSelected(item);
		}		
		
		for (i = 0; i < mTabsAdapter.mTabs.size(); i++) {
			mTabsAdapter.getItem(i).reset();
		}

		mTabsAdapter = new TabsAdapter(this, makeActionBar(), mViewPager);
		updateCountries();
		return super.onOptionsItemSelected(item);
	}

	private BroadcastReceiver mProgressReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int max = intent.getIntExtra("max", 1);
			int current = intent.getIntExtra("current", 0);
			String country = intent.getStringExtra("country");
			boolean showFinishing = intent.getBooleanExtra("show_finish", true);
			
			if (mProgressDialog != null) {
				if (country == null) {
					if (showFinishing) {
						mProgressDialog.setMessage(getString(R.string.voxmobile_finishing));
					} else {
						mProgressDialog.setMessage(getString(R.string.voxmobile_please_wait));
					}
				} else {
					mProgressDialog.setMessage(getString(R.string.voxmobile_loading) + " "  +country);
				}
				mProgressDialog.setMax(max);
				mProgressDialog.setProgress(current);
			}
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		registerContentObservers();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		unregisterContentObservers();
		
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		dismissProgressDialog();

		mTabsAdapter = new TabsAdapter(this, makeActionBar(), mViewPager);

		updateCountries();
	}
	
	private ActionBar makeActionBar() {
		final ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ab.setDisplayShowHomeEnabled(true);
        ab.setDisplayShowTitleEnabled(true);
		return ab;
	}

	@Override
	protected void onPause() {
		mPage = mTabsAdapter.mViewPager.getCurrentItem();
		mTabsAdapter = null;
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}
	
	public void trackEvent(int dialCode) {
		trackPageView("/android/csipsimple/rates/" + dialCode);
	}
	
	private void showProgressTaskDialog() {
		if (isFinishing()) {
			return;
		}

		if (mProgressDialog != null) {
			dismissProgressDialog();
		}
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setMessage(getString(R.string.voxmobile_please_wait));
		mProgressDialog.setCancelable(true);
		mProgressDialog.setMax(100);
		mProgressDialog.setProgress(0);
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
		mProgressDialog.show();
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
		private enum SyncType { NONE, COUNTRIES, DIAL_CODES };
		private static int mSyncStatus = SyncStatus.STALE;
		private static int mHttpCode = 0;
		private static String mError = "";
		private static SyncType mSyncType = SyncType.NONE;

		private static void reset() {
			mSyncStatus = SyncStatus.STALE;
			mHttpCode = 0;
			mError = "";
			mSyncType = SyncType.NONE;
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
					if (VoXObserverState.mSyncType == VoXObserverState.SyncType.COUNTRIES) {
						VoXObserverState.mSyncType = VoXObserverState.SyncType.NONE;
						
						updateCountries();
						updateDialCodes();
						
					} else if (VoXObserverState.mSyncType == VoXObserverState.SyncType.DIAL_CODES) {
						VoXObserverState.mSyncType = VoXObserverState.SyncType.NONE;
						updateDialCodes();
					} else {
						VoXObserverState.mSyncType = VoXObserverState.SyncType.NONE;
					}
				}
				break;
			}
		}
	}
	
	private void updateDialCodes() {
		int position = mViewPager.getCurrentItem();
		RatesFragment frag = (RatesFragment)mTabsAdapter.getItem(position);
		if (frag != null) {
			frag.updateDialCodeList();
		}
	}

	public void updateCountries() {
		mLatestRatesVersion = getLatestRatesVersion();
		mLocalRatesVersion = getLocalRatesVersion();
		Log.d(THIS_FILE, "Rates Versions: [latest] " + mLatestRatesVersion + " [local] " + mLocalRatesVersion);
		
		if (mLocalRatesVersion == 0 || mLocalRatesVersion < mLatestRatesVersion) {
			Log.d(THIS_FILE, "Rates update needed");
			
			// Kick off update task
			showProgressTaskDialog();
			VoXObserverState.mSyncType = VoXObserverState.SyncType.COUNTRIES;
	        getContentResolver().update(RateCountryContract.CONTENT_URI_GROUPS, null, null, null);
	        return;
		}
		
		Cursor c = getContentResolver().query(
				RateCountryContract.CONTENT_URI_GROUPS, null, null, null, null);

		startManagingCursor(c);

		if (mTabsAdapter.mActionBar != null) {
			try {
				mTabsAdapter.mActionBar.removeAllTabs();
			} catch (NullPointerException e) {
				e.printStackTrace();
				finish();
				return;
			}
		}

		if (c.getCount() == 0 && VoXObserverState.mSyncType == VoXObserverState.SyncType.NONE) {
			Log.d(THIS_FILE, "No rates found. Fetching new rates.");
			// Kick off update task
			showProgressTaskDialog();
			VoXObserverState.mSyncType = VoXObserverState.SyncType.COUNTRIES;
	        getContentResolver().update(RateCountryContract.CONTENT_URI_GROUPS, null, null, null);
	        return;
		}
		
		while (c.moveToNext()) {
			ActionBar.Tab tab = getSupportActionBar().newTab();
			tab.setText(c.getString(0));
			mTabsAdapter.addTab(tab);
		}
		
		if (mPage != -1) {
			mTabsAdapter.mViewPager.setCurrentItem(mPage);
			mPage = -1;
		}
	}

	public void updateDialCodes(int countryId) {
		// Kick off update task
		if (VoXObserverState.mSyncType == VoXObserverState.SyncType.NONE) {
			VoXObserverState.mSyncType = VoXObserverState.SyncType.DIAL_CODES;
			ContentValues values = new ContentValues();
			values.put(RestService.REST_DATA1, countryId);
			getContentResolver().update(RateCityContract.CONTENT_URI, values, null, null);		
		}
	}
	
	public void restart() {
		// This is called when the rates data becomes stale
		// and the UI is active. We need to completely restart
		// the activity - to make things simple.
		//
		// This is a total hack, but I just didn't have time to
		// get the UI working 100% correctly in the case where 
		// the data becomes stale during usage.
		finish();
	}

    private class TabsAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener, ActionBar.TabListener {
    	private final Context mContext;
    	private final ActionBar mActionBar;
    	private final ViewPager mViewPager;
    	private final HashMap<String, Fragment> mTabs = new HashMap<String, Fragment>();

		public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
    	    super(activity.getSupportFragmentManager());
    	    mContext = activity;
    	    mActionBar = actionBar;
    	    mViewPager = pager;
    	    mViewPager.setAdapter(this);
    	    mViewPager.setOnPageChangeListener(this);
    	}

    	public void addTab(ActionBar.Tab tab) {
    		mTabs.put(tab.getText().toString(), null);
    	    mActionBar.addTab(tab.setTabListener(this));
    	    notifyDataSetChanged();
    	}

    	@Override
    	public int getCount() {
    	    return mTabs.size();
    	}

    	@Override
    	public RatesFragment getItem(int position) {
    		Tab tab = mTabsAdapter.mActionBar.getTabAt(position);
    		if (tab == null) return null;
    		
    		String group = tab.getText().toString();
    		
    		Fragment frag = mTabs.get(group);
    		if (frag == null) {
        		Bundle args = new Bundle();
        		args.putString("group", group);
        	    frag = Fragment.instantiate(mContext, RatesFragment.class.getName(), args);
        	    mTabs.remove(group);
        	    mTabs.put(group, frag);
    		}

			return (RatesFragment)frag;
    	}

    	@Override
    	public void onTabSelected(Tab tab, FragmentTransaction ft) {
    	    if (mViewPager.getCurrentItem() != tab.getPosition()) {
    	        mViewPager.setCurrentItem(tab.getPosition(), true);
    	    }
    	}

    	@Override
    	public void onPageSelected(int position) {
    	    mActionBar.setSelectedNavigationItem(position);
    	}

    	@Override
    	public void onTabReselected(Tab tab, FragmentTransaction ft) {
    	}

    	@Override
    	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    	}

    	@Override
    	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    	}

    	@Override
    	public void onPageScrollStateChanged(int state) {    		
    	    switch (state) {
    	        case ViewPager.SCROLL_STATE_IDLE: {
    	            invalidateOptionsMenu();
    	            break;
    	        }
    	        default:
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
