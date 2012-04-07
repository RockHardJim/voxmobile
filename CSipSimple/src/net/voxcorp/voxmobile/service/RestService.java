/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package net.voxcorp.voxmobile.service;

import java.util.Iterator;
import java.util.List;

import net.voxcorp.R;
import net.voxcorp.api.SipManager;
import net.voxcorp.api.SipProfile;
import net.voxcorp.models.Filter;
import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.provider.DBAdapter;
import net.voxcorp.voxmobile.provider.DBContract.AccountContract;
import net.voxcorp.voxmobile.provider.DBContract.ProvisionCheckContract;
import net.voxcorp.voxmobile.provider.DBContract.SipUserContract;
import net.voxcorp.voxmobile.types.Account;
import net.voxcorp.voxmobile.types.AccountSummary;
import net.voxcorp.voxmobile.types.DIDCities;
import net.voxcorp.voxmobile.types.DIDStates;
import net.voxcorp.voxmobile.types.OrderResult;
import net.voxcorp.voxmobile.types.Plans;
import net.voxcorp.voxmobile.types.ProvisionCheck;
import net.voxcorp.voxmobile.types.RateCountries;
import net.voxcorp.voxmobile.types.RateDialCodes;
import net.voxcorp.voxmobile.types.SimpleReply;
import net.voxcorp.voxmobile.types.SipUser;
import net.voxcorp.voxmobile.types.SipUsers;
import net.voxcorp.voxmobile.types.TrialDialCodes;
import net.voxcorp.voxmobile.types.VersionCheck;
import net.voxcorp.voxmobile.utils.SimpleCrypto;
import net.voxcorp.voxmobile.utils.Utils;
import net.voxcorp.wizards.impl.VoXMobile;

import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.ResultReceiver;
import android.provider.BaseColumns;

public class RestService extends IntentService {

	/** Log identifier **/
	protected String THIS_FILE = "VoXMobile REST Svc";

	/** REST service stuff **/
    public static final String REST_RESPONSE = "net.voxcorp.voxmobile.REST_RESPONSE";
    public static final String REST_METHOD = "net.voxcorp.voxmobile.REST_METHOD";
	public static final String REST_STATUS_RECEIVER = "net.voxcorp.voxmobile.STATUS_RECEIVER";
	public static final String REST_DATA1 = "net.voxcorp.voxmobile.REST_DATA1";
	public static final String REST_DATA2 = "net.voxcorp.voxmobile.REST_DATA2";

	/** REST Service Results **/
	public static final int STATUS_RUNNING = 0x1;
    public static final int STATUS_ERROR = 0x2;
    public static final int STATUS_HTTP_ERROR = 0x3;
    public static final int STATUS_FINISHED = 0x4;

    /** REST Methods **/
    public static final int GET_SIP_USERS = 1001;
    public static final int UUID_LOGIN = 1002;
    public static final int LOGIN = 1003;
    public static final int LOGOUT = 1004;
    public static final int GET_DID_CITIES = 1005;
    public static final int GET_DID_STATES = 1006;
    public static final int GET_PLANS = 1007;
    public static final int SUBMIT_ORDER = 1008;
    public static final int VERSION_CHECK = 1009;
    public static final int PROVISION_CHECK = 1010;
    public static final int ACCOUNT_SUMMARY = 1011;
    public static final int RATE_COUNTRIES = 1012;
    public static final int RATE_DIAL_CODES = 1013;
    
    /** Provision Check Messages **/
    private static final int START_PROVISION_CHECK_TASK = -1;
    private static final int START_PROVISION_ACCOUNT_TASK = -2;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(THIS_FILE, "Creating REST service");
	}

	@Override
	public void onDestroy() {
		Log.d(THIS_FILE, "Destroying REST service");
		super.onDestroy();
	}

	public RestService() {
		super("RestService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		Log.d(THIS_FILE, "Processing REST request");

		final int method = intent.getIntExtra(REST_METHOD, 0);
		final ResultReceiver receiver = intent.getParcelableExtra(REST_STATUS_RECEIVER);
		Bundle bundle = new Bundle();

		String encryptedUuid;
		String uuid;
		Account account;

		try {
			ServiceHelper helper = new ServiceHelper(this);

			switch (method) {
			case UUID_LOGIN:
				uuid = intent.getStringExtra(REST_DATA1);

				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				account = helper.uuidLogin(uuid);
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putParcelable(REST_RESPONSE, account);
				if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case LOGIN:
				String uid = intent.getStringExtra(REST_DATA1);
				String pwd = intent.getStringExtra(REST_DATA2);

				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				account = helper.login(uid, pwd);
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putParcelable(REST_RESPONSE, account);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case LOGOUT:
				encryptedUuid = intent.getStringExtra(REST_DATA1);
				uuid = SimpleCrypto.decrypt(encryptedUuid);

				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				SimpleReply reply = helper.logout(uuid);
				bundle.clear();
				bundle.putString(REST_DATA1, encryptedUuid);
				bundle.putInt(REST_METHOD, method);
				bundle.putParcelable(REST_RESPONSE, reply);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case GET_DID_CITIES:
				String stateId = intent.getStringExtra(REST_DATA1);
				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				DIDCities cities = helper.getDIDCities(stateId);
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putParcelable(REST_RESPONSE, cities);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case GET_DID_STATES:
				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				DIDStates states = helper.getDIDStates();
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putParcelable(REST_RESPONSE, states);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case GET_PLANS:
				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				Plans plans = helper.getPlans();
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putParcelable(REST_RESPONSE, plans);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case GET_SIP_USERS:
				encryptedUuid = intent.getStringExtra(REST_DATA1);
				uuid = SimpleCrypto.decrypt(encryptedUuid);
				String accountNo = intent.getStringExtra(REST_DATA2);

				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				SipUsers sipUsers = helper.getSipUsers(uuid);
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putString(AccountContract.ACCOUNT_NO, accountNo);
				bundle.putParcelable(REST_RESPONSE, sipUsers);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case SUBMIT_ORDER:
				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				OrderResult orderResult = helper.submitOrder();
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putParcelable(REST_RESPONSE, orderResult);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);

		        if (orderResult.success && !"".equals(orderResult.auth_uuid)) {
		    		// Kick off provision check task
		            getContentResolver().update(ProvisionCheckContract.CONTENT_URI, null, null, null);
		        }

				break;
			case VERSION_CHECK:
				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				VersionCheck check = helper.checkVersion();
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putParcelable(REST_RESPONSE, check);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case RATE_COUNTRIES:
				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				TrialDialCodes trailDialCodes = helper.trialDialCodes();
				
				RateCountries rateCountries = helper.rateCountries();
				for (int i = 0; i < rateCountries.countries.length; i++) {
					rateCountries.countries[i].country = Utils.properCase(rateCountries.countries[i].country);
				}
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putParcelable(REST_RESPONSE, rateCountries);
				bundle.putParcelable(REST_DATA1, trailDialCodes);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case RATE_DIAL_CODES:
				int countryId = intent.getIntExtra(REST_DATA1, 0);

				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				RateDialCodes dialCodes = helper.rateDialCodes(countryId);
				for (int i = 0; i < dialCodes.dial_codes.length; i++) {
					dialCodes.dial_codes[i].country_id = countryId;
					dialCodes.dial_codes[i].city = Utils.properCase(dialCodes.dial_codes[i].city);
				}
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putInt(REST_DATA1, countryId);
				bundle.putParcelable(REST_RESPONSE, dialCodes);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case ACCOUNT_SUMMARY:
				encryptedUuid = intent.getStringExtra(REST_DATA1);
				uuid = SimpleCrypto.decrypt(encryptedUuid);

				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				AccountSummary summary = helper.accountSummary(uuid);
				bundle.clear();
				bundle.putInt(REST_METHOD, method);
				bundle.putString(REST_DATA1, encryptedUuid);
				bundle.putParcelable(REST_RESPONSE, summary);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			case PROVISION_CHECK:
				bundle.putInt(REST_METHOD, method);
				if (receiver != null) receiver.send(STATUS_RUNNING, bundle);

				// kick off provision check task
				if (mTimerStarted) {
					Log.d(THIS_FILE, "Provision check task already started!");
				} else {
					mTimerStarted = true;
					
					Message message = new Message();
					message.what = START_PROVISION_CHECK_TASK;
					mTimerHandler.sendMessageDelayed(message, mDelay);
				}

				bundle.clear();
				bundle.putInt(REST_METHOD, method);
		        if (receiver != null) receiver.send(STATUS_FINISHED, bundle);
				break;
			}

        } catch (ResourceAccessException eHost) {
            Log.e(THIS_FILE, "ResourceAccessException while RESTing", eHost);

            if (receiver != null) {
                bundle.clear();
				bundle.putInt(REST_METHOD, STATUS_ERROR);
                bundle.putString(Intent.EXTRA_TEXT, getString(R.string.voxmobile_network_error_msg));
				receiver.send(STATUS_ERROR, bundle);
            }
		} catch (HttpStatusCodeException eHttp) {
            Log.e(THIS_FILE, "HttpStatusCodeException while RESTing", eHttp);

            if (receiver != null) {
                bundle.clear();
				bundle.putInt(REST_METHOD, STATUS_HTTP_ERROR);
				bundle.putInt(Intent.EXTRA_SUBJECT, eHttp.getStatusCode().value());
                bundle.putString(Intent.EXTRA_TEXT, eHttp.getStatusText());
				receiver.send(STATUS_ERROR, bundle);
            }
        } catch (Exception e) {
            Log.e(THIS_FILE, "Problem while RESTing", e);

            if (receiver != null) {
                bundle.clear();
				bundle.putInt(REST_METHOD, STATUS_ERROR);
                bundle.putString(Intent.EXTRA_TEXT, e.getCause().getMessage());
				receiver.send(STATUS_ERROR, bundle);
            }
        }
	}

    // Timer and handler for provision wait functionality
	static boolean mTimerStarted = false;
	final TimerHandler mTimerHandler = new TimerHandler();
	final Messenger mTimerMessenger = new Messenger(mTimerHandler);
	final int mDelay = 60000;

	// ProvisionCheckTask is a helper utility to poll for the order status 
	class ProvisionCheckTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			DBAdapter database = new DBAdapter(RestService.this);
			DBAdapter db = database.open();
			Cursor c = db.getAccount(AccountContract.PROJECTION, AccountContract.ACCOUNT_NO + "=?", new String[] { "" } );
			if (c.getCount() == 0) {
				Log.d(THIS_FILE, "Provisioning Check, nothing to do.");
				mTimerStarted = false;
				c.close();
				db.close();
				return null;
			}

			try {
				ServiceHelper helper = new ServiceHelper(RestService.this);
				
				while (c.moveToNext()) {
					String encryptedUuid = c.getString(AccountContract.UUID_INDEX);
					String uuid = SimpleCrypto.decrypt(encryptedUuid);
					ProvisionCheck check;
					try {
						check = helper.provisionCheck(uuid);
			        } catch (ResourceAccessException eHost) {
			            Log.e(THIS_FILE, "ResourceAccessException while RESTing", eHost);
						check = new ProvisionCheck();
						check.httpStatus = 0;
						check.error = getString(R.string.voxmobile_network_error_msg); 
					} catch (HttpServerErrorException eHttp) {
			            Log.e(THIS_FILE, "HttpServerErrorException while RESTing", eHttp);
						check = new ProvisionCheck();
						check.httpStatus = eHttp.getStatusCode().value();
						check.error = getString(R.string.voxmobile_network_error_msg); 
			        } catch (Exception e) {
			            Log.e(THIS_FILE, "Problem while RESTing", e);
						check = new ProvisionCheck();
						check.httpStatus = 0;
						check.error = e.getCause().getMessage(); 
			        }
					if (check.success && check.provisioned) {
						ContentValues values = new ContentValues();
						values.put(AccountContract.ACCOUNT_NO, check.account_no);
						db.updateAccount(values, AccountContract.UUID + "=?", new String[] { encryptedUuid });
						getContentResolver().notifyChange(AccountContract.CONTENT_URI, null);
						
						Message message = new Message();
						message.what = START_PROVISION_ACCOUNT_TASK;
						message.arg1 = c.getInt(AccountContract.ID_INDEX);
			    		mTimerHandler.sendMessageDelayed(message, 0);						
					}
				}

			} finally {
				c.close();
			}

			// Now re-query database to see if all pending accounts are provisioned
			c = db.getAccount(AccountContract.PROJECTION, AccountContract.ACCOUNT_NO + "=?", new String[] { "" } );
			if (c.getCount() > 0) {
				Log.d(THIS_FILE, String.format("Still have %d orders waiting to complete.", c.getCount()));
				Message message = new Message();
				message.what = START_PROVISION_CHECK_TASK;
	    		mTimerHandler.sendMessageDelayed(message, mDelay);
			} else {
				Log.d(THIS_FILE, String.format("No more orders waiting to complete. Stopping provision check task.", c.getCount()));
				mTimerStarted = false;
			}
			c.close();
			db.close();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			Log.d(THIS_FILE, "End Provision Check Task");
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.d(THIS_FILE, "Start Provision Check Task");
		}
	}

	// ProvisionAccountTask is a helper utility create accounts once provisioned 
	class ProvisionAccountTask extends AsyncTask<String, Void, Void> {

		private List<SipProfile> sipProfiles;

		@Override
		protected Void doInBackground(String... params) {

			net.voxcorp.db.DBAdapter sipDatabase = new net.voxcorp.db.DBAdapter(RestService.this);
	        sipDatabase.open();
	        sipProfiles = sipDatabase.getListAccounts();
	        sipDatabase.close();

			String id = params[0];

			DBAdapter database = new DBAdapter(RestService.this);
			DBAdapter db = database.open();
			Cursor c = db.getAccount(AccountContract.PROJECTION, BaseColumns._ID + "=?", new String[] { id } );
			if (c.getCount() == 0 || !c.moveToFirst()) {
				Log.d(THIS_FILE, "Provisioning Account Task, unable to find record ID " + id + "!!!");
				c.close();
				db.close();
				return null;
			}
			
			long ts = System.currentTimeMillis();
			String account = c.getString(AccountContract.ACCOUNT_NO_INDEX);

			try {
				ServiceHelper helper = new ServiceHelper(RestService.this);
				
				SipUsers users = helper.getSipUsers(SimpleCrypto.decrypt(c.getString(AccountContract.UUID_INDEX)));
				if (users.success) {
					ContentValues values = new ContentValues();
					for (SipUser user : users.users) {
						String encryptedPwd = SimpleCrypto.encrypt(user.password);

						values.clear();
						values.put(SipUserContract.TIMESTAMP, ts);
						values.put(SipUserContract.ACCOUNT_NO, account);
						values.put(SipUserContract.USERNAME, user.username);
						values.put(SipUserContract.PASSWORD, encryptedPwd);
						values.put(SipUserContract.DISPLAY_NAME, user.displayname);
						db.insertSipUser(values);
						getContentResolver().notifyChange(SipUserContract.CONTENT_URI, null);
						
						user.password = encryptedPwd;
						if (!accountConfigured(user.username)) {
							configureSipProfile(user);
						}
					}
				}

			} finally {
				c.close();
				db.close();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			Log.d(THIS_FILE, "End Provision Account Task");
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.d(THIS_FILE, "Start Provision Account Task");
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

		private void configureSipProfile(SipUser sipUser) {
			Log.d(THIS_FILE, "Creating SIP profile for " + sipUser.username);

			VoXMobile wizard = new VoXMobile();
			SipProfile account = new SipProfile();

			wizard.buildAccount(account, sipUser);
			net.voxcorp.db.DBAdapter database = new net.voxcorp.db.DBAdapter(RestService.this);
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
			RestService.this.sendBroadcast(publishIntent);
		}
	}

	/** Timer Handler to poll for order status **/
    class TimerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
    		Log.d(THIS_FILE, "RestService.TimerHandler.handleMessage(" + msg.what + ")");

    		if (msg.what == START_PROVISION_CHECK_TASK) {
    			ProvisionCheckTask checkTask = new ProvisionCheckTask();
                checkTask.execute();
    		} else if (msg.what == START_PROVISION_ACCOUNT_TASK) {
    			ProvisionAccountTask provisionTask = new ProvisionAccountTask();
    			provisionTask.execute("" + msg.arg1);
    		}
        }
    }	
}
