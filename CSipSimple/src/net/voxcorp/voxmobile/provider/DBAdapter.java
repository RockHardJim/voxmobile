package net.voxcorp.voxmobile.provider;

import java.util.ArrayList;

import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.provider.DBContract.AccountContract;
import net.voxcorp.voxmobile.provider.DBContract.AccountSummaryContract;
import net.voxcorp.voxmobile.provider.DBContract.DIDCityContract;
import net.voxcorp.voxmobile.provider.DBContract.DIDStateContract;
import net.voxcorp.voxmobile.provider.DBContract.OrderResultContract;
import net.voxcorp.voxmobile.provider.DBContract.PlanChargeContract;
import net.voxcorp.voxmobile.provider.DBContract.PlanContract;
import net.voxcorp.voxmobile.provider.DBContract.RateCityContract;
import net.voxcorp.voxmobile.provider.DBContract.RateCountryContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SipUserContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.provider.DBContract.Tables;
import net.voxcorp.voxmobile.provider.DBContract.TrialDialCodeContract;
import net.voxcorp.voxmobile.provider.DBContract.VersionCheckContract;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DBAdapter {
	
	static String THIS_FILE = "VoXMobile DBAdapter";

	private static final String DATABASE_NAME = "voxmobile.db";
	private static final int DATABASE_VERSION = 5;

	private boolean opened = false;
	private final Context context;
	private DatabaseHelper databaseHelper;
	private SQLiteDatabase db;

	public DBAdapter(Context aContext) {
		context = aContext;
		databaseHelper = new DatabaseHelper(context);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(AccountContract.TABLE_CREATE_SQL);
			db.execSQL(AccountSummaryContract.TABLE_CREATE_SQL);
			db.execSQL(DIDCityContract.TABLE_CREATE_SQL);
			db.execSQL(DIDStateContract.TABLE_CREATE_SQL);
			db.execSQL(OrderResultContract.TABLE_CREATE_SQL);
			db.execSQL(PlanContract.TABLE_CREATE_SQL);
			db.execSQL(PlanChargeContract.TABLE_CREATE_SQL);
			db.execSQL(RequestContract.TABLE_CREATE_SQL);
			db.execSQL(SipUserContract.TABLE_CREATE_SQL);
			db.execSQL(SipUserContract.INDEX_CREATE_SQL);
			db.execSQL(VersionCheckContract.TABLE_CREATE_SQL);
			
			// International rates
			db.execSQL(RateCountryContract.TABLE_CREATE_SQL);
			db.execSQL(RateCountryContract.TABLE_CREATE_INDEX_1);
			db.execSQL(RateCountryContract.TABLE_CREATE_INDEX_2);

			db.execSQL(RateCityContract.TABLE_CREATE_SQL);
			db.execSQL(RateCityContract.TABLE_CREATE_INDEX_1);

			// Trial dial codes
			db.execSQL(TrialDialCodeContract.TABLE_CREATE_SQL);
			db.execSQL(TrialDialCodeContract.TABLE_CREATE_INDEX);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(THIS_FILE, "Upgrading database from version " + oldVersion + " to " + newVersion);

			try {

				if (oldVersion < 2) {
					db.execSQL("ALTER TABLE " + Tables.VERSION + " ADD "+ VersionCheckContract.TIMESTAMP + " INTEGER");
				}

				// Always purge version table to force new check
				db.execSQL("DELETE FROM " + Tables.VERSION);
			} catch(SQLiteException e) {
				Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
			}

			onCreate(db);
		}
	}

	public DBAdapter open() throws SQLException {
		if (opened) return this;
		db = databaseHelper.getWritableDatabase();
		opened = true;
		return this;
	}

	public void close() {
		if (!opened) return;
		databaseHelper.close();
		opened = false;
	}
	
	public boolean isOpen() {
		return opened;
	}

	/***********************************************************************
	 * Account Stuff
	 **********************************************************************/
	private boolean accountExists(String uuid) {
		Log.d(THIS_FILE, "accountExists()");
		Cursor c = getAccount(AccountContract.PROJECTION,
				AccountContract.UUID + "=?",
				new String[] { uuid });
		boolean found = c.getCount() > 0;
		c.close();
		return found;
	}
	
	public int deleteAccount(String selection, String[] selectionArgs) {
		Log.d(THIS_FILE, "deleteAccount(" + selectionArgs[0] + ")");

		/* Delete all SIP accounts associated with the accounts being logged out */
		ArrayList<String> tmp = new ArrayList<String>();
		Cursor c = getAccount(AccountContract.PROJECTION, selection, selectionArgs);
		while (c.moveToNext()) {
			tmp.add(c.getString(AccountContract.ACCOUNT_NO_INDEX));
		}
		c.close();
		
		String[] accounts = tmp.toArray(new String[tmp.size()]);
		db.delete(DBContract.Tables.SIP_USER, SipUserContract.ACCOUNT_NO + "=?", accounts);

		/* Logout of selected accounts */
		return db.delete(DBContract.Tables.ACCOUNT, selection, selectionArgs);
	}

	public Cursor getAccount(String[] projection, String selection, String[] selectionArgs) {
		Log.d(THIS_FILE, "getAccount()");
		return db.query(Tables.ACCOUNT, projection, selection, selectionArgs, 
				null, null, DBContract.AccountContract.ACCOUNT_NO + "," + BaseColumns._ID);
	}
	
	public long insertAccount(ContentValues values) {
		Log.d(THIS_FILE, "insertAccount()");
		String uuid = values.getAsString(AccountContract.UUID);
		if (!accountExists(uuid)) {
			return db.insert(Tables.ACCOUNT, null, values);
		} else {
			return 0;
		}
	}
	
	public long updateAccount(ContentValues values, String whereClause, String[] whereArgs) {
		Log.d(THIS_FILE, "updateAccount()");
		return db.update(Tables.ACCOUNT, values, whereClause, whereArgs);
	}

	/***********************************************************************
	 * Account Summary Stuff
	 **********************************************************************/

	private int removeStaleAccountSummary() {
		long five_minutes = 1000 * 60 * 30;
		long t = System.currentTimeMillis() - five_minutes;
		return db.delete(Tables.ACCOUNT_SUMMARY, AccountSummaryContract.TIMESTAMP + "<=?", new String[] { "" + t });
	}

	public Cursor getAccountSummary(String[] projection, String selection, String[] selectionArgs) {
		Log.d(THIS_FILE, "getAccountSummary()");
		removeStaleAccountSummary();
		return db.query(Tables.ACCOUNT_SUMMARY, projection, selection, selectionArgs, null, null, null);
	}

	public long insertAccountSummary(ContentValues values) {
		Log.d(THIS_FILE, "insertAccountSummary()");
		String uuid = values.getAsString(AccountSummaryContract.UUID);

		// Safety check to keep duplicates from getting stored
		db.delete(Tables.ACCOUNT_SUMMARY, AccountSummaryContract.UUID + "=?", new String[] { uuid });

		if (!values.containsKey(AccountSummaryContract.TIMESTAMP)) {
			values.put(AccountSummaryContract.TIMESTAMP, System.currentTimeMillis());
		}
		return db.insert(Tables.ACCOUNT_SUMMARY, null, values);
	}

	/***********************************************************************
	 * DID City Stuff
	 **********************************************************************/

	public Cursor getDIDCities(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Log.d(THIS_FILE, "getDIDCities()");
		return db.query(Tables.DID_CITY, projection, selection, selectionArgs, null, null, sortOrder);
	}

	public long insertDIDCity(ContentValues values) {
		Log.d(THIS_FILE, "insertDIDCity()");

		// Safety check to keep duplicates from getting stored
		String stateId = values.getAsString(DIDCityContract.STATE_ID);
		String cityId = values.getAsString(DIDCityContract.CITY_ID);
		db.delete(Tables.DID_CITY,
				DIDCityContract.STATE_ID + "=? AND " + DIDCityContract.CITY_ID + "=?",
				new String[] { stateId, cityId });

		return db.insert(Tables.DID_CITY, null, values);
	}

	/***********************************************************************
	 * DID State Stuff
	 **********************************************************************/

	private int removeStaleDIDStates() {
		long five_minutes = 1000 * 60 * 5;
		long t = System.currentTimeMillis() - five_minutes;
		int deleted = 0;
		
		String where = DIDStateContract.TIMESTAMP + "<=?";
		String[] whereArgs = new String[] { "" + t };
		
		Cursor c = db.query(Tables.DID_STATE, DIDStateContract.PROJECTION, where, whereArgs, null, null, null);
		while (c.moveToNext()) {
			String w = DIDCityContract.STATE_ID + "=?";
			String[] wA = new String[] { c.getString(DIDStateContract.STATE_ID_INDEX) };
			
			/* Delete all DID cities associated with the state */
			db.delete(DBContract.Tables.DID_CITY, w, wA);
			
			/* Delete stale state */
			deleted += db.delete(DBContract.Tables.DID_STATE, w, wA);
		}
		c.close();
		
		return deleted;
	}

	public Cursor getDIDStates(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Log.d(THIS_FILE, "getDIDStates()");
		removeStaleDIDStates();
		return db.query(Tables.DID_STATE, projection, selection, selectionArgs, null, null, sortOrder);
	}

	public long insertDIDState(ContentValues values) {
		Log.d(THIS_FILE, "insertDIDState()");

		// Safety check to keep duplicates from getting stored
		String stateId = values.getAsString(DIDStateContract.STATE_ID);
		db.delete(Tables.DID_STATE, DIDStateContract.STATE_ID + "=?", new String[] { stateId });
		db.delete(Tables.DID_CITY, DIDCityContract.STATE_ID + "=?", new String[] { stateId });

		if (!values.containsKey(RequestContract.TIMESTAMP)) {
			values.put(DIDStateContract.TIMESTAMP, System.currentTimeMillis());
		}
		return db.insert(Tables.DID_STATE, null, values);
	}

	/***********************************************************************
	 * Order Result Stuff
	 **********************************************************************/

	public int deleteOrderResult() {
		Log.d(THIS_FILE, "deleteOrderResult()");
		return db.delete(Tables.ORDER_RESULT, null, null);
	}

	public Cursor getOrderResult(String[] projection) {
		Log.d(THIS_FILE, "getOrderResult()");
		return db.query(Tables.ORDER_RESULT, projection, null, null, null, null, null);
	}

	public long insertOrderResult(ContentValues values) {
		Log.d(THIS_FILE, "insertOrderResult()");
		return db.insert(Tables.ORDER_RESULT, null, values);
	}

	/***********************************************************************
	 * Plan Stuff
	 **********************************************************************/
	private int removeStalePlans() {
		long five_minutes = 1000 * 60 * 5;
		long t = System.currentTimeMillis() - five_minutes;

		// Delete stale charges associated with to-be-deleted plans
		String where =
				PlanChargeContract.PLAN_ID + " IN (" + 
				"SELECT " + PlanContract.PLAN_ID + " FROM " + Tables.PLAN + " WHERE " + PlanContract.TIMESTAMP + " <= ?" +
				")";
		db.delete(Tables.PLAN_CHARGE, where, new String[] { "" + t });
		
		// Delete stale plans
		return db.delete(Tables.PLAN, PlanContract.TIMESTAMP + "<=?", new String[] { "" + t });
	}

	public Cursor getPlans(String[] projection, String selection, String[] selectionArgs) {
		Log.d(THIS_FILE, "getPlans()");
		removeStalePlans();
		return db.query(Tables.PLAN, projection, selection, selectionArgs, null, null, PlanContract.TOTAL_PRICE_AS_REAL);
	}
	
	public long insertPlan(ContentValues values) {
		Log.d(THIS_FILE, "insertPlan()");

		// Safety check to keep duplicates from getting stored
		String planId = values.getAsString(PlanContract.PLAN_ID);
		db.delete(Tables.PLAN, PlanContract.PLAN_ID + "=?", new String[] { planId });
		db.delete(Tables.PLAN_CHARGE, PlanChargeContract.PLAN_ID + "=?", new String[] { planId });

		if (!values.containsKey(PlanContract.TIMESTAMP)) {
			values.put(PlanContract.TIMESTAMP, System.currentTimeMillis());
		}
		return db.insert(Tables.PLAN, null, values);
	}

	/***********************************************************************
	 * Plan Charge Stuff
	 **********************************************************************/
	public Cursor getPlanCharge(String[] projection, String selection, String[] selectionArgs) {
		Log.d(THIS_FILE, "getCharge()");
		removeStalePlans();
		return db.query(Tables.PLAN_CHARGE, projection, selection, selectionArgs, null, null, PlanChargeContract.DESCRIPTION);
	}
	
	public long insertPlanCharge(ContentValues values) {
		Log.d(THIS_FILE, "insertPlanCharge()");
		return db.insert(Tables.PLAN_CHARGE, null, values);
	}

	/***********************************************************************
	 * International City Rate Stuff
	 **********************************************************************/
	public int deleteRateCity() {
		Log.d(THIS_FILE, "Delete rate_city record");
		return db.delete(Tables.RATE_CITY, null, null);
	}

	public Cursor getRateCity(String[] projection, String selection, String[] selectionArgs) {
		Log.d(THIS_FILE, "Query rate_city record");
		return db.query(
			Tables.RATE_CITY,
			projection,
			selection,
			selectionArgs,
			null, null,
			RateCityContract.CITY + "," + RateCityContract.DIAL_CODE);
	}
	
	public long insertRateCity(ContentValues values) {
		Log.d(THIS_FILE, "Insert rate_city record");
		return db.insert(Tables.RATE_CITY, null, values);
	}

	/***********************************************************************
	 * International Country Rate Stuff
	 **********************************************************************/
	public int deleteRateCountry() {
		Log.d(THIS_FILE, "Delete rate_country record");
		deleteRateCity();
		deleteTrialDialCode();
		return db.delete(Tables.RATE_COUNTRY, null, null);
	}

	public Cursor getRateCountryGroups() {
		Log.d(THIS_FILE, "Query rate_country groups");
		return db.query(
			true,
			Tables.RATE_COUNTRY,
			new String [] { RateCountryContract.COUNTRY_GROUP },
			null, null, null, null,
			RateCountryContract.COUNTRY_GROUP,
			null);
	}

	public Cursor getTrailRateCountryGroups() {
		Log.d(THIS_FILE, "Query trial rate_country groups");
		
		return db.rawQuery(
			"SELECT DISTINCT " + RateCountryContract.COUNTRY_GROUP + " " +
			"FROM " + Tables.RATE_COUNTRY + " T1, " + Tables.TRIAL_DIAL_CODE + " T2 " +
			"WHERE T1." + RateCountryContract.COUNTRY_ID + "=T2." + TrialDialCodeContract.COUNTRY_ID + " " +
			"ORDER BY " + RateCountryContract.COUNTRY_GROUP + ";", null);
	}

	public Cursor getRateCountry(String[] projection, String selection, String[] selectionArgs) {
		Log.d(THIS_FILE, "Query rate_country record");
		return db.query(Tables.RATE_COUNTRY,
			projection,
			selection,
			selectionArgs,
			null, null,
			RateCountryContract.COUNTRY);
	}

	public long insertRateCountry(ContentValues values) {
		Log.d(THIS_FILE, "Insert rate_country record");

		return db.insert(Tables.RATE_COUNTRY, null, values);
	}

	/***********************************************************************
	 * Request Stuff
	 **********************************************************************/
	public int deleteRequest() {
		Log.d(THIS_FILE, "Delete request record");
		return db.delete(Tables.REQUEST, null, null);
	}
	
	private boolean isRequestStale() {
		long ts = System.currentTimeMillis();	
		Cursor c = db.query(Tables.REQUEST, null, null, null, null, null, null);
		if (c.getCount() != 0 && c.moveToFirst()) {
			ts = c.getLong(RequestContract.TIMESTAMP_INDEX);
		}
		c.close();
		
		long ten_seconds = 1000 * 10;
		return ts + ten_seconds < System.currentTimeMillis();
	}

	public Cursor getRequest(String[] projection) {
		Log.d(THIS_FILE, "Query request record");
		if (isRequestStale()) {
			ContentValues values = new ContentValues();
			values.put(RequestContract.UPDATED, SyncStatus.STALE);
			db.update(Tables.REQUEST, values, null, null);
		}
		return db.query(Tables.REQUEST, projection, null, null, null, null, null);
	}
	
	public long insertRequest(ContentValues values) {
		Log.d(THIS_FILE, "Insert request record");
		if (!values.containsKey(RequestContract.TIMESTAMP)) {
			values.put(RequestContract.TIMESTAMP, System.currentTimeMillis());
		}
		return db.insert(Tables.REQUEST, null, values);
	}

	public long updateRequest(ContentValues values) {
		Log.d(THIS_FILE, "Update request record");
		if (!values.containsKey(RequestContract.TIMESTAMP)) {
			values.put(RequestContract.TIMESTAMP, System.currentTimeMillis());
		}
		return db.update(Tables.REQUEST, values, null, null);
	}

	/***********************************************************************
	 * SIP User Stuff
	 **********************************************************************/

	private int removeStaleSipUsers() {
		long five_minutes = 1000 * 60 * 5;
		long t = System.currentTimeMillis() - five_minutes;
		return db.delete(Tables.SIP_USER, SipUserContract.TIMESTAMP + "<=?", new String[] { "" + t });
	}

	public Cursor getSipUsers(String[] projection, String selection, String[] selectionArgs) {
		Log.d(THIS_FILE, "getSipUsers()");
		removeStaleSipUsers();
		return db.query(Tables.SIP_USER, projection, selection, selectionArgs, null, null, SipUserContract.USERNAME);
	}
	
	public long insertSipUser(ContentValues values) {
		Log.d(THIS_FILE, "insertSipUser()");

		// Safety check to keep duplicates from getting stored
		String username = values.getAsString(SipUserContract.USERNAME);
		db.delete(Tables.SIP_USER, SipUserContract.USERNAME + "=?", new String[] { username });

		if (!values.containsKey(RequestContract.TIMESTAMP)) {
			values.put(SipUserContract.TIMESTAMP, System.currentTimeMillis());
		}
		return db.insert(Tables.SIP_USER, null, values);
	}

	/***********************************************************************
	 * Trail Dial Code Stuff
	 **********************************************************************/
	public int deleteTrialDialCode() {
		Log.d(THIS_FILE, "Delete trial_dial_code records");
		return db.delete(Tables.TRIAL_DIAL_CODE, null, null);
	}

	public Cursor getTrialDialCodes(String[] projection, String selection, String[] selectionArgs) {
		Log.d(THIS_FILE, "Query trial_dial_code record");
		return db.query(Tables.TRIAL_DIAL_CODE,
			projection,
			selection,
			selectionArgs,
			null, null,
			TrialDialCodeContract.COUNTRY_ID + ", " + TrialDialCodeContract.DIAL_CODE);
	}
	
	public long insertTrialDialCode(ContentValues values) {
		Log.d(THIS_FILE, "Insert trial_dial_code record");

		return db.insert(Tables.TRIAL_DIAL_CODE, null, values);
	}

	/***********************************************************************
	 * Version Check Stuff
	 **********************************************************************/
	private int removeStaleVersion() {
		long sixty_minutes = 1000 * 60 * 60;
		long t = System.currentTimeMillis() - sixty_minutes;
		return db.delete(Tables.VERSION, VersionCheckContract.TIMESTAMP + "<=?", new String[] { "" + t });
	}

	public int deleteVersionCheck() {
		Log.d(THIS_FILE, "Delete version check record");
		return db.delete(Tables.VERSION, null, null);
	}

	public Cursor getVersionCheck(String[] projection) {
		Log.d(THIS_FILE, "Query version check record");
		removeStaleVersion();
		return db.query(Tables.VERSION, projection, null, null, null, null, null);
	}

	public long insertVersionCheck(ContentValues values) {
		Log.d(THIS_FILE, "Insert version check record");
		if (!values.containsKey(VersionCheckContract.TIMESTAMP)) {
			values.put(VersionCheckContract.TIMESTAMP, System.currentTimeMillis());
		}
		return db.insert(Tables.VERSION, null, values);
	}
	
	public long updateVersionCheck(ContentValues values) {
		Log.d(THIS_FILE, "Update version check record");
		return db.update(Tables.VERSION, values, null, null);
	}
	
}
