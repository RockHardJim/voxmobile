package net.voxcorp.voxmobile.provider;

import java.util.ArrayList;

import net.voxcorp.api.SipConfigManager;
import net.voxcorp.utils.Log;
import net.voxcorp.utils.PreferencesWrapper;
import net.voxcorp.voxmobile.provider.DBContract.AccountContract;
import net.voxcorp.voxmobile.provider.DBContract.AccountSummaryContract;
import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;
import net.voxcorp.voxmobile.provider.DBContract.DIDCityContract;
import net.voxcorp.voxmobile.provider.DBContract.DIDStateContract;
import net.voxcorp.voxmobile.provider.DBContract.OrderResultContract;
import net.voxcorp.voxmobile.provider.DBContract.PlanChargeContract;
import net.voxcorp.voxmobile.provider.DBContract.PlanContract;
import net.voxcorp.voxmobile.provider.DBContract.ProvisionCheckContract;
import net.voxcorp.voxmobile.provider.DBContract.RateCityContract;
import net.voxcorp.voxmobile.provider.DBContract.RateCountryContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SipUserContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.provider.DBContract.Tables;
import net.voxcorp.voxmobile.provider.DBContract.TrialDialCodeContract;
import net.voxcorp.voxmobile.provider.DBContract.VersionCheckContract;
import net.voxcorp.voxmobile.service.DetachableResultReceiver;
import net.voxcorp.voxmobile.service.RestService;
import net.voxcorp.voxmobile.types.Account;
import net.voxcorp.voxmobile.types.AccountSummary;
import net.voxcorp.voxmobile.types.DIDCities;
import net.voxcorp.voxmobile.types.DIDCity;
import net.voxcorp.voxmobile.types.DIDState;
import net.voxcorp.voxmobile.types.DIDStates;
import net.voxcorp.voxmobile.types.OrderResult;
import net.voxcorp.voxmobile.types.Plan;
import net.voxcorp.voxmobile.types.PlanCharge;
import net.voxcorp.voxmobile.types.Plans;
import net.voxcorp.voxmobile.types.RateCountries;
import net.voxcorp.voxmobile.types.RateCountry;
import net.voxcorp.voxmobile.types.RateDialCode;
import net.voxcorp.voxmobile.types.RateDialCodes;
import net.voxcorp.voxmobile.types.SipUser;
import net.voxcorp.voxmobile.types.SipUsers;
import net.voxcorp.voxmobile.types.TrialDialCode;
import net.voxcorp.voxmobile.types.TrialDialCodes;
import net.voxcorp.voxmobile.types.VersionCheck;
import net.voxcorp.voxmobile.utils.SimpleCrypto;

import org.springframework.http.HttpStatus;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

public class VoXMobileProvider extends ContentProvider {

	static String THIS_FILE = "VoXMobile Provider";

	DBAdapter mDatabase;
	public static final int UUID_LOGIN = 1;
	public static final int DID_CITY = 2;
	public static final int DID_STATE = 3;
	public static final int LOGIN = 4;
	public static final int LOGOUT = 5;
	public static final int ORDER_RESULT = 6;
	public static final int ORDER_RESULT_ERROR = 7;
	public static final int PLAN = 8;
	public static final int PLAN_CHARGE = 9;
	public static final int REQUEST = 10;
	public static final int SIP_USER = 11;
	public static final int VERSION_CHECK = 12;
	public static final int PROVISION_CHECK = 13;
	public static final int ACCOUNT = 14;
	public static final int ACCOUNT_SUMMARY = 15;
	public static final int RATE_COUNTRY = 16;
	public static final int RATE_COUNTRY_GROUPS = 17;
	public static final int RATE_COUNTRY_GROUPS_TRIAL = 18;
	public static final int RATE_CITY = 19;
	public static final int RATE_PROGRESS = 20;
	public static final int TRIAL_DIAL_CODE = 21;

	private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		matcher.addURI(DBContract.AUTHORITY, Tables.ACCOUNT + "/uuid_login", UUID_LOGIN);
		matcher.addURI(DBContract.AUTHORITY, Tables.ACCOUNT + "/login", LOGIN);
		matcher.addURI(DBContract.AUTHORITY, Tables.ACCOUNT + "/logout", LOGOUT);
		matcher.addURI(DBContract.AUTHORITY, Tables.ACCOUNT, ACCOUNT);
		matcher.addURI(DBContract.AUTHORITY, Tables.ACCOUNT_SUMMARY, ACCOUNT_SUMMARY);
		matcher.addURI(DBContract.AUTHORITY, Tables.DID_CITY, DID_CITY);
		matcher.addURI(DBContract.AUTHORITY, Tables.DID_STATE, DID_STATE);
		matcher.addURI(DBContract.AUTHORITY, Tables.ORDER_RESULT, ORDER_RESULT);
		matcher.addURI(DBContract.AUTHORITY, Tables.PLAN, PLAN);
		matcher.addURI(DBContract.AUTHORITY, Tables.PLAN_CHARGE, PLAN_CHARGE);
		matcher.addURI(DBContract.AUTHORITY, Tables.REQUEST, REQUEST);
		matcher.addURI(DBContract.AUTHORITY, Tables.SIP_USER, SIP_USER);
		matcher.addURI(DBContract.AUTHORITY, Tables.VERSION, VERSION_CHECK);
		matcher.addURI(DBContract.AUTHORITY, "provision_check", PROVISION_CHECK);

		matcher.addURI(DBContract.AUTHORITY, Tables.RATE_COUNTRY, RATE_COUNTRY);		
		matcher.addURI(DBContract.AUTHORITY, Tables.RATE_COUNTRY + "/groups", RATE_COUNTRY_GROUPS);
		matcher.addURI(DBContract.AUTHORITY, Tables.RATE_COUNTRY + "/groups/trial", RATE_COUNTRY_GROUPS_TRIAL);
		matcher.addURI(DBContract.AUTHORITY, Tables.RATE_CITY, RATE_CITY);
		
		matcher.addURI(DBContract.AUTHORITY, Tables.TRIAL_DIAL_CODE, TRIAL_DIAL_CODE);
	}

	@Override
	public boolean onCreate() {
		mDatabase = new DBAdapter(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		int matchType = matcher.match(uri);
		switch (matchType) {
		case DID_CITY:
			return DIDCityContract.CONTENT_TYPE;
		case DID_STATE:
			return DIDStateContract.CONTENT_TYPE;
		case UUID_LOGIN:
		case LOGIN:
		case LOGOUT:
		case ACCOUNT:
			return AccountContract.CONTENT_TYPE;
		case ACCOUNT_SUMMARY:
			return AccountSummaryContract.CONTENT_TYPE;
		case ORDER_RESULT:
			return OrderResultContract.CONTENT_TYPE;
		case PLAN:
			return PlanContract.CONTENT_TYPE;
		case PLAN_CHARGE:
			return PlanChargeContract.CONTENT_TYPE;
		case RATE_CITY:
			return RateCityContract.CONTENT_TYPE;
		case RATE_COUNTRY:
		case RATE_COUNTRY_GROUPS:
		case RATE_COUNTRY_GROUPS_TRIAL:
			return RateCountryContract.CONTENT_TYPE;
		case REQUEST:
			return RequestContract.CONTENT_TYPE;
		case SIP_USER:
			return SipUserContract.CONTENT_TYPE;
		case TRIAL_DIAL_CODE:
			return TrialDialCodeContract.CONTENT_TYPE;
		case VERSION_CHECK:
			return VersionCheckContract.CONTENT_TYPE;
		case PROVISION_CHECK:
			return ProvisionCheckContract.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		DBAdapter db = mDatabase.open();
		int matchType = matcher.match(uri);
		int rows = 0;
		switch (matchType) {
		case LOGOUT:
			if (!mRestReceiver.mSyncing) {
				Cursor c = db.getAccount(AccountContract.PROJECTION, selection, selectionArgs);
				if (c.getCount() == 0) {
					c.close();
					break;
				}
				if (c.moveToFirst()) {
		            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
		            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
		            intent.putExtra(RestService.REST_METHOD, RestService.LOGOUT);
		            intent.putExtra(RestService.REST_DATA1, c.getString(AccountContract.UUID_INDEX));
		            
		            getContext().startService(intent);
				}
				c.close();
			}
			break;
		case ORDER_RESULT:
			rows = db.deleteOrderResult();
			break;
		case REQUEST:
			rows = db.deleteRequest();
			break;
		case VERSION_CHECK:
			rows = db.deleteVersionCheck();
			break;
		default:
			throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rows;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		DBAdapter db = mDatabase.open();
		int matchType = matcher.match(uri);
		switch (matchType) {
		case ACCOUNT:
			db.insertAccount(values);
			break;
		case ACCOUNT_SUMMARY:
			db.insertAccountSummary(values);
			break;
		case DID_CITY:
			db.insertDIDCity(values);
			break;
		case DID_STATE:
			db.insertDIDState(values);
			break;
		case LOGIN:
			db.insertAccount(values);
			break;
		case ORDER_RESULT:
			db.insertOrderResult(values);
			break;
		case PLAN:
			db.insertPlan(values);
			break;
		case PLAN_CHARGE:
			db.insertPlanCharge(values);
			break;
		case RATE_CITY:
			db.insertRateCity(values);
			break;
		case RATE_COUNTRY:
			db.insertRateCountry(values);
			break;
		case REQUEST:
			db.insertRequest(values);
			break;
		case SIP_USER:
			db.insertSipUser(values);
			break;
		case TRIAL_DIAL_CODE:
			db.insertTrialDialCode(values);
			break;
		case VERSION_CHECK:
			// Initialize new record with default values
			db.insertVersionCheck(values);
			break;
		default:
			throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return uri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		DBAdapter db = mDatabase.open();
		Cursor c = null;
		switch (matcher.match(uri)) {
		case ACCOUNT:
			c = db.getAccount(projection, selection, selectionArgs);
			break;
		case ACCOUNT_SUMMARY:
			c = db.getAccountSummary(projection, selection, selectionArgs);
			break;
		case DID_CITY:
			c = db.getDIDCities(projection, selection, selectionArgs, sortOrder);
			break;
		case DID_STATE:
			c = db.getDIDStates(projection, selection, selectionArgs, sortOrder);
			break;
		case ORDER_RESULT:
			c = db.getOrderResult(projection);
			break;
		case PLAN:
			c = db.getPlans(projection, selection, selectionArgs);
			break;
		case PLAN_CHARGE:
			c = db.getPlanCharge(projection, selection, selectionArgs);
			break;
		case RATE_CITY:
			c = db.getRateCity(projection, selection, selectionArgs);
			break;
		case RATE_COUNTRY:
			c = db.getRateCountry(projection, selection, selectionArgs);
			break;
		case RATE_COUNTRY_GROUPS:
			c = db.getRateCountryGroups();
			break;
		case RATE_COUNTRY_GROUPS_TRIAL:
			c = db.getTrailRateCountryGroups();
			break;
		case REQUEST:
			c = db.getRequest(projection);
			break;
		case SIP_USER:
			c = db.getSipUsers(projection, selection, selectionArgs);
			break;
		case TRIAL_DIAL_CODE:
			c = db.getTrialDialCodes(projection, selection, selectionArgs);
			break;
		case VERSION_CHECK:
			c =  db.getVersionCheck(projection);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Make sure observers are notified in the event of
		// the data behind the cursor is changed
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		DBAdapter db = mDatabase.open();
		int count = 0;
		Intent i;

		switch (matcher.match(uri)) {
		case ACCOUNT:
			db.updateAccount(values, selection, selectionArgs);
			count = 1;
			break;
		case ACCOUNT_SUMMARY:
			if (!mRestReceiver.mSyncing) {
	            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            intent.putExtra(RestService.REST_METHOD, RestService.ACCOUNT_SUMMARY);
	            intent.putExtra(RestService.REST_DATA1, values.getAsString(AccountSummaryContract.UUID));
	            getContext().startService(intent);
			}
            break;
		case DID_CITY:
			if (!mRestReceiver.mSyncing) {
	            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            intent.putExtra(RestService.REST_METHOD, RestService.GET_DID_CITIES);
	            intent.putExtra(RestService.REST_DATA1, values.getAsString(DIDCityContract.STATE_ID));
	            getContext().startService(intent);
			}
            break;
		case DID_STATE:
			if (!mRestReceiver.mSyncing) {
	            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            intent.putExtra(RestService.REST_METHOD, RestService.GET_DID_STATES);
	            getContext().startService(intent);
			}
            break;
		case UUID_LOGIN:
			if (!mRestReceiver.mSyncing) {
				Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
				intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
				intent.putExtra(RestService.REST_METHOD, RestService.UUID_LOGIN);
				intent.putExtra(RestService.REST_DATA1, values.getAsString(RestService.REST_DATA1));

				getContext().startService(intent);
			}
			break;
		case LOGIN:
			if (!mRestReceiver.mSyncing) {
	            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            intent.putExtra(RestService.REST_METHOD, RestService.LOGIN);
	            intent.putExtra(RestService.REST_DATA1, values.getAsString(RestService.REST_DATA1));
				intent.putExtra(RestService.REST_DATA2, values.getAsString(RestService.REST_DATA2));

	            getContext().startService(intent);
			} else {
				db.updateAccount(values, selection, selectionArgs);
				count = 1;
			}
			break;
		case ORDER_RESULT:
			if (!mRestReceiver.mSyncing) {
				db.deleteOrderResult();
				
	            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            intent.putExtra(RestService.REST_METHOD, RestService.SUBMIT_ORDER);

	            getContext().startService(intent);
			}
            break;
		case PLAN:
			if (!mRestReceiver.mSyncing) {
	            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            intent.putExtra(RestService.REST_METHOD, RestService.GET_PLANS);
	            getContext().startService(intent);
			}
            break;
		case RATE_CITY:
			if (!mRestReceiver.mSyncing) {
	            i = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            i.putExtra(RestService.REST_DATA1, values.getAsInteger(RestService.REST_DATA1));
	            i.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            i.putExtra(RestService.REST_METHOD, RestService.RATE_DIAL_CODES);
	            getContext().startService(i);
			}
			break;
		case RATE_COUNTRY:
			if (!mRestReceiver.mSyncing) {
	            i = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            i.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            i.putExtra(RestService.REST_METHOD, RestService.RATE_COUNTRIES);
	            getContext().startService(i);
			}
			break;
		case RATE_COUNTRY_GROUPS:
			if (!mRestReceiver.mSyncing) {
	            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            intent.putExtra(RestService.REST_METHOD, RestService.RATE_COUNTRIES);

	            getContext().startService(intent);
			}
			break;
		case REQUEST:
			db.updateRequest(values);
			count = 1;
			break;
		case SIP_USER:
			if (!mRestReceiver.mSyncing) {
	            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            intent.putExtra(RestService.REST_METHOD, RestService.GET_SIP_USERS);
	            intent.putExtra(RestService.REST_DATA1, values.getAsString(AccountContract.UUID));
	            intent.putExtra(RestService.REST_DATA2, values.getAsString(AccountContract.ACCOUNT_NO));
	            getContext().startService(intent);
			}
            break;
		case VERSION_CHECK:
			if (!mRestReceiver.mSyncing && values == null) {
	            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
	            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
	            intent.putExtra(RestService.REST_METHOD, RestService.VERSION_CHECK);
	            getContext().startService(intent);
			} else {
				db.updateVersionCheck(values);
				count = 1;
			}
			break;
		case PROVISION_CHECK:
            Intent intent = new Intent(Intent.ACTION_SYNC, null, getContext(), RestService.class);
            intent.putExtra(RestService.REST_STATUS_RECEIVER, mRestReceiver.mReceiver);
            intent.putExtra(RestService.REST_METHOD, RestService.PROVISION_CHECK);
            getContext().startService(intent);
            break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (count != 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

	private RestReceiver mRestReceiver = new RestReceiver();
	
	/**
	 * A non-UI fragment, retained across configuration changes, that updates its activity's UI
	 * when sync status changes. */
	class RestReceiver implements DetachableResultReceiver.Receiver {
		private boolean mSyncing = false;
		private DetachableResultReceiver mReceiver;

		public RestReceiver() {
			super();
			mReceiver = new DetachableResultReceiver(new Handler());
			mReceiver.setReceiver(this);
		}

		private boolean requestActive() {
            DBAdapter db = mDatabase.open();
			Cursor c = db.getRequest(null);
			try {
				if (c.getCount() == 0 && c.moveToFirst()) {
					return c.getInt(RequestContract.UPDATED_INDEX) == SyncStatus.UPDATING;
				} else {
					return false;
				}
			} finally {
				c.close();
			}
		}

		private void clearLegacyData() {
			// remove sharedPreferences that are now stored in SQLite
			SharedPreferences prefs = getContext().getSharedPreferences("voxmobile", Context.MODE_PRIVATE);
			Editor edit = prefs.edit();
			edit.remove("uuid");
			edit.remove("plan_id");
			edit.remove("first_name");
			edit.remove("last_name");
			edit.remove("email");
			edit.remove("email_confirm");
			edit.remove("billing_country_index");
			edit.remove("billing_country");
			edit.remove("billing_street");
			edit.remove("billing_city");
			edit.remove("billing_postal_code");
			edit.remove("cc_number");
			edit.remove("cc_cvv");
			edit.remove("cc_exp_month");
			edit.remove("cc_exp_year");
			edit.remove("did_state");
			edit.remove("did_state_name");
			edit.remove("did_city");
			edit.remove("imei");
			edit.remove("provision_status");
			edit.commit();
		}

		public void onReceiveResult(int resultCode, Bundle resultData) {
			Log.d(THIS_FILE, "onReceiveResult: " + resultCode);

			final DBAdapter db = mDatabase.open();

			ContentValues values = null;
			int method = 0;
			int success = DBBoolean.FALSE;
			PreferencesWrapper prefWrapper;
			boolean setCallerId;
			String uuid;
			long ts;

			switch (resultCode) {
			case RestService.STATUS_RUNNING:
				mSyncing = true;

				values = new ContentValues();
				values.put(RequestContract.UPDATED, SyncStatus.UPDATING);
				values.put(RequestContract.HTTP_STATUS, 0);
				values.put(RequestContract.ERROR, "");
				values.put(RequestContract.SUCCESS, Integer.valueOf(DBBoolean.FALSE));

				delete(RequestContract.CONTENT_URI, null, null);
				insert(RequestContract.CONTENT_URI, values);

				method = resultData.getInt(RestService.REST_METHOD);
				switch (method) {
				case RestService.ACCOUNT_SUMMARY:
					Log.d(THIS_FILE, "Account summary starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Account summary already started");
						return;
					}
					break;
				case RestService.UUID_LOGIN:
					Log.d(THIS_FILE, "Account uuid login starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Account uuid login already started");
						return;
					}
					break;
				case RestService.LOGIN:
					Log.d(THIS_FILE, "Account login starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Account login already started");
						return;
					}
					break;
				case RestService.GET_DID_CITIES:
					Log.d(THIS_FILE, "Get DID cities starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Get DID cities already started");
						return;
					}
					break;
				case RestService.GET_DID_STATES:
					Log.d(THIS_FILE, "Get DID states starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Get DID states already started");
						return;
					}
					break;
				case RestService.GET_PLANS:
					Log.d(THIS_FILE, "Get service plans starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Get service plans already started");
						return;
					}
					break;
				case RestService.GET_SIP_USERS:
					Log.d(THIS_FILE, "Get SIP users starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Get SIP users already started");
						return;
					}
					break;
				case RestService.SUBMIT_ORDER:
					Log.d(THIS_FILE, "Submit order starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Submit order already started");
						return;
					}
					break;
				case RestService.PROVISION_CHECK:
					Log.d(THIS_FILE, "Provision check background task started");
					return;
				case RestService.RATE_COUNTRIES:
					Log.d(THIS_FILE, "Get rate countries starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Get rate countries already started");
						return;
					}
					break;
				case RestService.RATE_DIAL_CODES:
					Log.d(THIS_FILE, "Get rate dial codes starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Get rate dial codes already started");
						return;
					}
					break;
				case RestService.VERSION_CHECK:
					Log.d(THIS_FILE, "Version check starting");

					if (requestActive()) {
						Log.d(THIS_FILE, "Version check already started");
						return;
					}

					delete(VersionCheckContract.CONTENT_URI, null, null);
					break;
				}

				break;
			case RestService.STATUS_FINISHED:

				method = resultData.getInt(RestService.REST_METHOD);
				switch (method) {
				case RestService.GET_PLANS:
					Log.d(THIS_FILE, "Get service plans complete");

					Plans plans = resultData.getParcelable(RestService.REST_RESPONSE);
					success = plans.success ? DBBoolean.TRUE : DBBoolean.FALSE;

					values = new ContentValues();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, plans.httpStatus);
					values.put(RequestContract.ERROR, plans.error);
					values.put(RequestContract.SUCCESS, success);
					update(RequestContract.CONTENT_URI, values, null, null);

					// save plans if HTTP request was successful
					if (plans.httpStatus == HttpStatus.OK.value() && plans.success) {
						ts = System.currentTimeMillis();

						for (Plan plan : plans.plans) {
							double price;
							try {
								price = Double.parseDouble(plan.total_price);
							} catch (NumberFormatException e) {
								price = 0.00;
							}

							values.clear();
							values.put(PlanContract.TIMESTAMP, ts);
							values.put(PlanContract.PLAN_ID, plan.plan_id);
							values.put(PlanContract.TITLE, plan.title);
							values.put(PlanContract.DESCRIPTION, plan.description);
							values.put(PlanContract.TOTAL_PRICE, plan.total_price);
							values.put(PlanContract.TOTAL_PRICE_AS_REAL, price);							
							insert(PlanContract.CONTENT_URI, values);

							// save plan charge detail
							for (PlanCharge charge : plan.charges) {
								values.clear();
								values.put(PlanChargeContract.PLAN_ID, plan.plan_id);
								values.put(PlanChargeContract.DESCRIPTION, charge.description);
								values.put(PlanChargeContract.PRICE, charge.price);
								values.put(PlanChargeContract.RECURRING, charge.recurring);
								insert(PlanChargeContract.CONTENT_URI, values);								
							}
						}
					}
					break;

				case RestService.GET_DID_CITIES:
					Log.d(THIS_FILE, "Get DID cities complete");

					DIDCities cities = resultData.getParcelable(RestService.REST_RESPONSE);
					success = cities.success ? DBBoolean.TRUE : DBBoolean.FALSE;

					values = new ContentValues();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, cities.httpStatus);
					values.put(RequestContract.ERROR, cities.error);
					values.put(RequestContract.SUCCESS, success);
					update(RequestContract.CONTENT_URI, values, null, null);

					// save user detail if HTTP request was successful
					if (cities.httpStatus == HttpStatus.OK.value() && cities.success) {
						for (DIDCity city : cities.cities) {
							values.clear();
							values.put(DIDCityContract.STATE_ID, city.state_id);
							values.put(DIDCityContract.CITY_ID, city.city_id);
							values.put(DIDCityContract.DESCRIPTION, city.description);
							values.put(DIDCityContract.DID_COUNT, city.did_count);
							insert(DIDCityContract.CONTENT_URI, values);
						}
					}

					break;

				case RestService.GET_DID_STATES:
					Log.d(THIS_FILE, "Get DID states complete");

					DIDStates states = resultData.getParcelable(RestService.REST_RESPONSE);
					success = states.success ? DBBoolean.TRUE : DBBoolean.FALSE;

					values = new ContentValues();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, states.httpStatus);
					values.put(RequestContract.ERROR, states.error);
					values.put(RequestContract.SUCCESS, success);
					update(RequestContract.CONTENT_URI, values, null, null);

					// save user detail if HTTP request was successful
					if (states.httpStatus == HttpStatus.OK.value() && states.success) {
						ts = System.currentTimeMillis();

						for (DIDState state : states.states) {
							values.clear();
							values.put(DIDStateContract.TIMESTAMP, ts);
							values.put(DIDStateContract.STATE_ID, state.state_id);
							values.put(DIDStateContract.DESCRIPTION, state.description);
							values.put(DIDStateContract.DID_COUNT, state.did_count);
							insert(DIDStateContract.CONTENT_URI, values);
						}
					}

					break;

				case RestService.GET_SIP_USERS:
					Log.d(THIS_FILE, "Get SIP users complete");

					SipUsers sipUsers = resultData.getParcelable(RestService.REST_RESPONSE);
					String accountNo = resultData.getString(AccountContract.ACCOUNT_NO);
					success = sipUsers.success ? DBBoolean.TRUE : DBBoolean.FALSE;

					values = new ContentValues();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, sipUsers.httpStatus);
					values.put(RequestContract.ERROR, sipUsers.error);
					values.put(RequestContract.SUCCESS, success);
					update(RequestContract.CONTENT_URI, values, null, null);

					// save user detail if HTTP request was successful
					if (sipUsers.httpStatus == HttpStatus.OK.value() && sipUsers.success) {
						ts = System.currentTimeMillis();

						prefWrapper = new PreferencesWrapper(getContext());
						setCallerId = "".equals(prefWrapper.getPreferenceStringValue(SipConfigManager.DEFAULT_CALLER_ID).trim());

						for (SipUser user : sipUsers.users) {
							String encryptedPwd = SimpleCrypto.encrypt(user.password);
							
							values.clear();
							values.put(SipUserContract.TIMESTAMP, ts);
							values.put(SipUserContract.ACCOUNT_NO, accountNo);
							values.put(SipUserContract.USERNAME, user.username);
							values.put(SipUserContract.PASSWORD, encryptedPwd);
							values.put(SipUserContract.DISPLAY_NAME, user.displayname);
							insert(SipUserContract.CONTENT_URI, values);

							if (setCallerId) {
								setCallerId = false;
								prefWrapper.setPreferenceStringValue(SipConfigManager.DEFAULT_CALLER_ID, user.displayname);
							}
						}
					}

					break;

				case RestService.UUID_LOGIN:
				case RestService.LOGIN:
					if (resultCode == RestService.LOGIN) {
						Log.d(THIS_FILE, "Account login complete");
					} else {
						Log.d(THIS_FILE, "Account uuid login complete");
					}

					Account account = resultData.getParcelable(RestService.REST_RESPONSE);
					success = account.success ? DBBoolean.TRUE : DBBoolean.FALSE;

					values = new ContentValues();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, account.httpStatus);

					if (account.httpStatus == HttpStatus.OK.value() && account.success) {
						values.put(RequestContract.ERROR, account.account_no);
					} else {
						values.put(RequestContract.ERROR, account.error);
					}
					values.put(RequestContract.SUCCESS, success);
					update(RequestContract.CONTENT_URI, values, null, null);

					// save login info if HTTP request was successful
					if (account.httpStatus == HttpStatus.OK.value() && account.success) {
						values.clear();
						values.put(AccountContract.UUID, SimpleCrypto.encrypt(account.uuid));
						values.put(AccountContract.ACCOUNT_NO, account.account_no);
						insert(AccountContract.CONTENT_URI_LOGIN, values);

						ts = System.currentTimeMillis();

						prefWrapper = new PreferencesWrapper(getContext());
						setCallerId = "".equals(prefWrapper.getPreferenceStringValue(SipConfigManager.DEFAULT_CALLER_ID).trim());

						for (SipUser user : account.users) {
							String encryptedPwd = SimpleCrypto.encrypt(user.password);

							values.clear();
							values.put(SipUserContract.TIMESTAMP, ts);
							values.put(SipUserContract.ACCOUNT_NO, account.account_no);
							values.put(SipUserContract.USERNAME, user.username);
							values.put(SipUserContract.PASSWORD, encryptedPwd);
							values.put(SipUserContract.DISPLAY_NAME, user.displayname);
							insert(SipUserContract.CONTENT_URI, values);

							if (setCallerId) {
								setCallerId = false;
								prefWrapper.setPreferenceStringValue(SipConfigManager.DEFAULT_CALLER_ID, user.displayname);
							}
						}
					}

					if (resultCode == RestService.UUID_LOGIN) {
						// UUID Login will only ever happen if we have legacy
						// credentials in sharedPreferences. These will be
						// stored in SQLite upon detection and then removed
						// from sharedPreferences
						clearLegacyData();
					}
					break;

				case RestService.LOGOUT:
					Log.d(THIS_FILE, "Account logout complete");

					// never worry about whether or not the "logout" request
					// was successful, just update the Request table as if it
					// were successful and delete the account because if our
					// auth uuid table gets cleared out and a user tries to
					// log out then the log out request will be answered with
					// an HTTP 404, and the user will see "invalid user name/pwd"
					// rather than actually logging out.
					uuid = resultData.getString(RestService.REST_DATA1);
					db.deleteAccount(AccountContract.UUID + "=?", new String[] { uuid });

					values = new ContentValues();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, HttpStatus.OK.value());
					values.put(RequestContract.ERROR, "");
					values.put(RequestContract.SUCCESS, DBBoolean.TRUE);
					update(RequestContract.CONTENT_URI, values, null, null);
					break;

				case RestService.SUBMIT_ORDER:
					Log.d(THIS_FILE, "Submit order complete");

					OrderResult orderResult = resultData.getParcelable(RestService.REST_RESPONSE);
					success = orderResult.success ? DBBoolean.TRUE : DBBoolean.FALSE;

					values = new ContentValues();

					if (orderResult.httpStatus == HttpStatus.OK.value()) {
						if (orderResult.success) {
							values.put(OrderResultContract.RESULT_STRING, orderResult.result_string);
							values.put(OrderResultContract.LOGIN_NAME, orderResult.login_name);
							values.put(OrderResultContract.LOGIN_PASSWORD, orderResult.login_password);
							values.put(OrderResultContract.AUTH_UUID, SimpleCrypto.encrypt(orderResult.auth_uuid));
							values.put(OrderResultContract.CC_CHARGE_AMOUNT, orderResult.cc_charge_amount);
							values.put(OrderResultContract.CC_AUTH_CODE, orderResult.cc_auth_code);
						} else {
							values.put(OrderResultContract.ERROR_TYPE, orderResult.order_error.getTypeAsInt());
							values.put(OrderResultContract.ERROR_MSG, orderResult.order_error.msg);							
						}
						insert(OrderResultContract.CONTENT_URI, values);

						if (orderResult.success){
							values.clear();
							values.put(AccountContract.UUID, SimpleCrypto.encrypt(orderResult.auth_uuid));
							values.put(AccountContract.ACCOUNT_NO, "");
							insert(AccountContract.CONTENT_URI, values);
							
							// kick off provision check
							update(ProvisionCheckContract.CONTENT_URI, null, null, null);
						}
					}

					values.clear();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, orderResult.httpStatus);
					values.put(RequestContract.ERROR, orderResult.error);
					values.put(RequestContract.SUCCESS, success);
					update(RequestContract.CONTENT_URI, values, null, null);

					break;
				case RestService.ACCOUNT_SUMMARY:
					Log.d(THIS_FILE, "Account summary complete");

					String encryptedUuid = resultData.getString(RestService.REST_DATA1);

					AccountSummary summary = resultData.getParcelable(RestService.REST_RESPONSE);
					success = summary.success ? DBBoolean.TRUE : DBBoolean.FALSE;

					values = new ContentValues();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, summary.httpStatus);
					values.put(RequestContract.ERROR, summary.error);
					values.put(RequestContract.SUCCESS, success);
					update(RequestContract.CONTENT_URI, values, null, null);

					// save summary info if HTTP request was successful
					if (summary.httpStatus == HttpStatus.OK.value() && summary.success) {
						values.clear();
						values.put(AccountSummaryContract.UUID, encryptedUuid);
						values.put(AccountSummaryContract.SUMMARY, summary.summary);
						insert(AccountSummaryContract.CONTENT_URI, values);
					}
					break;
				case RestService.RATE_DIAL_CODES:
					Log.d(THIS_FILE, "Dial codes update complete");

					final RateDialCodes dialCodes = resultData.getParcelable(RestService.REST_RESPONSE);
					if (dialCodes == null)
						break;

					Thread insertTask = new Thread (new Runnable() {
						public void run() {

							int success = dialCodes.success ? DBBoolean.TRUE : DBBoolean.FALSE;
							ContentValues values = new ContentValues();
							
							ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

							for (int i = 0; i < dialCodes.dial_codes.length; i++) {
								RateDialCode dialCode = dialCodes.dial_codes[i];

								Log.d(THIS_FILE, String.format("Country load (%s): %d of %d",
										dialCode.city, i + 1, dialCodes.dial_codes.length));

								ContentProviderOperation.Builder op = ContentProviderOperation.newInsert(RateCityContract.CONTENT_URI);
								op.withValue(RateCityContract.COUNTRY_ID, dialCode.country_id);
								op.withValue(RateCityContract.CITY, dialCode.city);
								op.withValue(RateCityContract.RATE, dialCode.rate);
								op.withValue(RateCityContract.DIAL_CODE, dialCode.dial_code);
								operations.add(op.build());

								if (operations.size() >= 20) {
									try {
										Intent intent = new Intent("progress");
										intent.putExtra("country", dialCode.city);
										intent.putExtra("show_finish", false);
										LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

										applyBatch(operations);
										operations.clear();
									} catch (OperationApplicationException e) {
										e.printStackTrace();
									}
								}
							}

							if (operations.size() > 0) {
								try {
									applyBatch(operations);

									Intent intent = new Intent("progress");
									intent.putExtra("show_finish", false);
									LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
								} catch (OperationApplicationException e) {
									e.printStackTrace();
								}
							}

							values.clear();
							values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
							values.put(RequestContract.HTTP_STATUS, dialCodes.httpStatus);
							values.put(RequestContract.ERROR, dialCodes.error);
							values.put(RequestContract.SUCCESS, success);
							update(RequestContract.CONTENT_URI, values, null, null);
						}
					});
					// start the insert operations in background thread
					insertTask.start();

					break;
				case RestService.RATE_COUNTRIES:
					Log.d(THIS_FILE, "Rates update complete");

					final SharedPreferences sysPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
					final RateCountries countries = resultData.getParcelable(RestService.REST_RESPONSE);
					if (countries == null)
						break;

					final TrialDialCodes trialDialCodes = resultData.getParcelable(RestService.REST_DATA1);

					Thread insertCountryTask = new Thread (new Runnable() {
						
						private void doInsertTrialDialCodes() {
							if (trialDialCodes != null) {
								ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
								int recCount = 0;
								int totalCount = trialDialCodes.allowed.length + trialDialCodes.blocked.length;
								
								for (int i = 0; i < trialDialCodes.allowed.length; i++) {
									TrialDialCode trialDialCode = trialDialCodes.allowed[i];

									Log.d(THIS_FILE, String.format("Trial dial code load (allowed, %s): %d of %d",
											trialDialCode.dial_code, i + 1, trialDialCodes.allowed.length));
									
									ContentProviderOperation.Builder op = ContentProviderOperation.newInsert(TrialDialCodeContract.CONTENT_URI);
									op.withValue(TrialDialCodeContract.COUNTRY_ID, trialDialCode.country_id);
									op.withValue(TrialDialCodeContract.DIAL_CODE, trialDialCode.dial_code);
									op.withValue(TrialDialCodeContract.BLOCKED, DBBoolean.FALSE);
									operations.add(op.build());
									recCount++;

									if (operations.size() >= 20) {
										try {
											Intent intent = new Intent("progress");
											intent.putExtra("max", totalCount);
											intent.putExtra("current", recCount + 1);
											intent.putExtra("show_finish", false);
											LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

											applyBatch(operations);
											operations.clear();
										} catch (OperationApplicationException e) {
											e.printStackTrace();
										}
									}
								}

								if (operations.size() > 0) {
									try {
										Intent intent = new Intent("progress");
										intent.putExtra("max", totalCount);
										intent.putExtra("current", recCount + 1);
										intent.putExtra("show_finish", false);
										LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

										applyBatch(operations);
										operations.clear();
									} catch (OperationApplicationException e) {
										e.printStackTrace();
									}
								}

								for (int i = 0; i < trialDialCodes.blocked.length; i++) {
									TrialDialCode trialDialCode = trialDialCodes.blocked[i];

									Log.d(THIS_FILE, String.format("Trial dial code load (blocked, %s): %d of %d",
											trialDialCode.dial_code, i + 1, trialDialCodes.blocked.length));
									
									ContentProviderOperation.Builder op = ContentProviderOperation.newInsert(TrialDialCodeContract.CONTENT_URI);
									op.withValue(TrialDialCodeContract.COUNTRY_ID, trialDialCode.country_id);
									op.withValue(TrialDialCodeContract.DIAL_CODE, trialDialCode.dial_code);
									op.withValue(TrialDialCodeContract.BLOCKED, DBBoolean.TRUE);
									operations.add(op.build());
									recCount++;

									if (operations.size() >= 20) {
										try {
											Intent intent = new Intent("progress");
											intent.putExtra("max", totalCount);
											intent.putExtra("current", recCount + 1);
											intent.putExtra("show_finish", false);
											LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

											applyBatch(operations);
											operations.clear();
										} catch (OperationApplicationException e) {
											e.printStackTrace();
										}
									}
								}

								if (operations.size() > 0) {
									try {
										Intent intent = new Intent("progress");
										intent.putExtra("max", totalCount);
										intent.putExtra("current", totalCount);
										intent.putExtra("show_finish", false);
										LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

										applyBatch(operations);
										operations.clear();
									} catch (OperationApplicationException e) {
										e.printStackTrace();
									}
								}
							}
						}
						
						private void doInsertCountries() {
							int  success = countries.success ? DBBoolean.TRUE : DBBoolean.FALSE;
							ContentValues values = new ContentValues();
							ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

							for (int i = 0; i < countries.countries.length; i++) {
								RateCountry country = countries.countries[i];
								Log.d(THIS_FILE, String.format("Country load (%s): %d of %d",
										country.country, i + 1, countries.countries.length));
								
								ContentProviderOperation.Builder op = ContentProviderOperation.newInsert(RateCountryContract.CONTENT_URI);
								op.withValue(RateCountryContract.COUNTRY_ID, country.country_id);
								op.withValue(RateCountryContract.COUNTRY_GROUP, country.group);
								op.withValue(RateCountryContract.COUNTRY, country.country);
								operations.add(op.build());
								
								if (operations.size() >= 20) {
									try {
										Intent intent = new Intent("progress");
										intent.putExtra("max", countries.countries.length);
										intent.putExtra("current", i + 1);
										intent.putExtra("country", country.country);
										LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
										
										applyBatch(operations);
										operations.clear();
									} catch (OperationApplicationException e) {
										e.printStackTrace();
									}
								}
							}
							
							if (operations.size() > 0) {
								try {
									applyBatch(operations);
									
									Intent intent = new Intent("progress");
									intent.putExtra("max", countries.countries.length);
									intent.putExtra("current", countries.countries.length);
									LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
								} catch (OperationApplicationException e) {
									e.printStackTrace();
								}
							}
							
							saveLocalRatesVersion(countries.version);

							values.clear();
							values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
							values.put(RequestContract.HTTP_STATUS, countries.httpStatus);
							values.put(RequestContract.ERROR, countries.error);
							values.put(RequestContract.SUCCESS, success);
							update(RequestContract.CONTENT_URI, values, null, null);
						}
						
						private void saveLocalRatesVersion(int version) {
							Editor editor = sysPrefs.edit();
							editor.putInt("VOX_RATES", version);
							editor.commit();
							Log.d(THIS_FILE, "Rates version updated to " + version);
						}
						
						public void run() {
							doInsertTrialDialCodes();
							doInsertCountries();
						}
					});

					// Delete all countries, cities and trial dial codes
					Log.d(THIS_FILE, "Purging rates database");
					mDatabase.deleteRateCountry();

					// start the insert operations in background thread
					insertCountryTask.start();
					
					break;
				case RestService.VERSION_CHECK:
					Log.d(THIS_FILE, "Version check complete");

					VersionCheck check = resultData.getParcelable(RestService.REST_RESPONSE);
					success = check.success ? DBBoolean.TRUE : DBBoolean.FALSE;
					int supported = (check.success && check.supported && check.httpStatus == HttpStatus.OK.value()) ? DBBoolean.TRUE : DBBoolean.FALSE;

					values = new ContentValues();

					// save version support info if HTTP request was successful
					if (check.httpStatus == HttpStatus.OK.value() && check.success) {
						values.clear();
						values.put(VersionCheckContract.SUPPORTED, supported); 
						values.put(VersionCheckContract.TIMESTAMP, System.currentTimeMillis());
						insert(VersionCheckContract.CONTENT_URI, values);
					}

					SharedPreferences prefs = getContext().getSharedPreferences("voxmobile", Context.MODE_PRIVATE);
					if (!"".equals(prefs.getString("uuid", ""))) {
						uuid = SimpleCrypto.decrypt(prefs.getString("uuid", ""));

						values.clear();
						values.put(RestService.REST_DATA1, uuid);
						update(AccountContract.CONTENT_URI_UUID_LOGIN, values, null, null);
					}

					values.clear();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, check.httpStatus);
					values.put(RequestContract.ERROR, check.error);
					values.put(RequestContract.SUCCESS, success);
					update(RequestContract.CONTENT_URI, values, null, null);
					break;
				case RestService.PROVISION_CHECK:
					Log.d(THIS_FILE, "Provision check complete");

					values = new ContentValues();
					values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
					values.put(RequestContract.HTTP_STATUS, HttpStatus.OK.value());
					values.put(RequestContract.ERROR, "");
					values.put(RequestContract.SUCCESS, DBBoolean.TRUE);
					update(RequestContract.CONTENT_URI, values, null, null);
					break;
				}

				mSyncing = false;
				break;
			case RestService.STATUS_HTTP_ERROR:
				// Real exception caught in RestService.java

				int httpCode = resultData.getInt(Intent.EXTRA_SUBJECT);
				String httpText = resultData.getString(Intent.EXTRA_TEXT);;

				method = resultData.getInt(RestService.REST_METHOD);
				values = new ContentValues();
				values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
				values.put(RequestContract.HTTP_STATUS, httpCode);
				values.put(RequestContract.ERROR, httpText);
				values.put(RequestContract.SUCCESS, DBBoolean.FALSE);
				update(RequestContract.CONTENT_URI, values, null, null);

				mSyncing = false;
				break;
			case RestService.STATUS_ERROR:
				// Real exception caught in RestService.java
				String err = resultData.getString(Intent.EXTRA_TEXT);;

				method = resultData.getInt(RestService.REST_METHOD);
				values = new ContentValues();
				values.put(RequestContract.UPDATED, SyncStatus.CURRENT);
				values.put(RequestContract.HTTP_STATUS, 0);
				values.put(RequestContract.ERROR, err);
				values.put(RequestContract.SUCCESS, DBBoolean.FALSE);
				update(RequestContract.CONTENT_URI, values, null, null);

				mSyncing = false;
			}
		}
	}
}
