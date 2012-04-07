package net.voxcorp.voxmobile.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class DBContract implements BaseColumns {

	public static final String AUTHORITY = "net.voxcorp.voxmobile.provider";
	public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    
    // This class cannot be instantiated
    private DBContract() {}

    public interface SyncStatus {
    	int CURRENT = -2;
    	int UPDATING = -1;
    	int STALE = 0;
    }
    
    public interface DBBoolean {
		int TRUE = 0;
		int FALSE = 1;
	}
	
	public interface Tables {
		String ACCOUNT = "account";
		String ACCOUNT_SUMMARY = "account_summary";
		String DID_CITY = "did_city";
		String DID_STATE = "did_state";
		String ORDER_RESULT = "order_result";
		String PLAN = "plan";
		String PLAN_CHARGE = "plan_charge";
		String RATE_CITY = "rate_city";
		String RATE_COUNTRY = "rate_country";
		String REQUEST = "request";
		String SIP_USER = "sip_user";
		String TRIAL_DIAL_CODE = "trial_dial_code";
		String VERSION = "version";
	}
	
	public static class AccountContract implements BaseColumns {
		
		/** FIELDS **/
		public static final String UUID = "uuid";
		public static final String ACCOUNT_NO = "account_no";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int UUID_INDEX = 1;
		public static final int ACCOUNT_NO_INDEX = 2;

		public static final Uri CONTENT_URI_UUID_LOGIN =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.ACCOUNT).appendPath("uuid_login").build();
		public static final Uri CONTENT_URI_LOGIN =
			BASE_CONTENT_URI.buildUpon().appendPath(Tables.ACCOUNT).appendPath("login").build();
		public static final Uri CONTENT_URI_LOGOUT =
			BASE_CONTENT_URI.buildUpon().appendPath(Tables.ACCOUNT).appendPath("logout").build();
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.ACCOUNT).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.account";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.account";
		public static final String[] PROJECTION = {
			_ID, UUID, ACCOUNT_NO };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL);",
					Tables.ACCOUNT,
					BaseColumns._ID,
					UUID,
					ACCOUNT_NO);
	}

	public static class AccountSummaryContract implements BaseColumns {
		
		/** FIELDS **/
		public static final String TIMESTAMP = "time_stamp";
		public static final String UUID = "uuid";
		public static final String SUMMARY = "summary";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int TIMESTAMP_INDEX = 1;
		public static final int UUID_INDEX = 2;
		public static final int SUMMARY_INDEX = 3;

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.ACCOUNT_SUMMARY).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.account.summary";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.account.summary";
		public static final String[] PROJECTION = {
			_ID, TIMESTAMP, UUID, SUMMARY };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s INTEGER NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL);",
					Tables.ACCOUNT_SUMMARY,
					BaseColumns._ID,
					TIMESTAMP,
					UUID,
					SUMMARY);
	}

	public static class DIDCityContract implements BaseColumns {

		/** FIELDS **/
		public static final String STATE_ID = "state_id";
		public static final String CITY_ID = "city_id";
		public static final String DESCRIPTION = "description";
		public static final String DID_COUNT = "did_count";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int STATE_ID_INDEX = 1;
		public static final int CITY_ID_INDEX = 2;
		public static final int DESCRIPTION_INDEX = 3;
		public static final int DID_COUNT_INDEX = 4;

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.DID_CITY).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.did.city";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.did.city";
		public static final String[] PROJECTION = {
			_ID, STATE_ID, CITY_ID, DESCRIPTION, DID_COUNT };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s INTEGER NOT NULL);",
					Tables.DID_CITY,
					BaseColumns._ID,
					STATE_ID,
					CITY_ID,
					DESCRIPTION,
					DID_COUNT);
		public static String INDEX_CREATE_SQL =
				String.format(
					"CREATE INDEX IF NOT EXISTS ix_" + Tables.DID_CITY + " " +
					"ON " + Tables.DID_CITY + "(" + STATE_ID, CITY_ID + ")");
	}

	public static class DIDStateContract implements BaseColumns {

		/** FIELDS **/
		public static final String TIMESTAMP = "time_stamp";
		public static final String STATE_ID = "state_id";
		public static final String DESCRIPTION = "description";
		public static final String DID_COUNT = "did_count";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int TIMESTAMP_INDEX = 1;
		public static final int STATE_ID_INDEX = 2;
		public static final int DESCRIPTION_INDEX = 3;
		public static final int DID_COUNT_INDEX = 4;

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.DID_STATE).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.did.state";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.did.state";
		public static final String[] PROJECTION = {
			_ID, TIMESTAMP, STATE_ID, DESCRIPTION, DID_COUNT };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s INTEGER NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s INTEGER NOT NULL);",
					Tables.DID_STATE,
					BaseColumns._ID,
					TIMESTAMP,
					STATE_ID,
					DESCRIPTION,
					DID_COUNT);
		public static String INDEX_CREATE_SQL =
				String.format(
					"CREATE INDEX IF NOT EXISTS ix_" + Tables.DID_STATE + " " +
					"ON " + Tables.DID_STATE + "(" + STATE_ID + ")");
	}

	public static class OrderResultContract implements BaseColumns {

		/** FIELDS **/
		public static final String RESULT_STRING = "result_string";
		public static final String LOGIN_NAME = "login_name";
		public static final String LOGIN_PASSWORD = "login_password";
		public static final String AUTH_UUID = "auth_uuid";
		public static final String CC_CHARGE_AMOUNT = "cc_charge_amount";
		public static final String CC_AUTH_CODE = "cc_auth_code";
		public static final String ERROR_TYPE = "error_type";
		public static final String ERROR_MSG = "error_msg";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int RESULT_STRING_INDEX = 1;
		public static final int LOGIN_NAME_INDEX = 2;
		public static final int LOGIN_PASSWORD_INDEX = 3;
		public static final int AUTH_UUID_INDEX = 4;
		public static final int CC_CHARGE_AMOUNT_INDEX = 5;
		public static final int CC_AUTH_CODE_INDEX = 6;
		public static final int ERROR_TYPE_INDEX = 7;
		public static final int ERROR_MSG_INDEX = 8;

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.ORDER_RESULT).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.order";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.order";
		public static final String[] PROJECTION = {
			_ID, RESULT_STRING, LOGIN_NAME, LOGIN_PASSWORD, AUTH_UUID, CC_CHARGE_AMOUNT, CC_AUTH_CODE, ERROR_TYPE, ERROR_MSG };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s TEXT NULL," +
					"%s TEXT NULL," +
					"%s TEXT NULL," +
					"%s TEXT NULL," +
					"%s TEXT NULL," +
					"%s TEXT NULL," +
					"%s INTEGER NOT NULL DEFAULT 0," +					
					"%s TEXT NULL);",
					Tables.ORDER_RESULT,
					BaseColumns._ID,
					RESULT_STRING,
					LOGIN_NAME,
					LOGIN_PASSWORD,
					AUTH_UUID,
					CC_CHARGE_AMOUNT,
					CC_AUTH_CODE,
					ERROR_TYPE,
					ERROR_MSG);
	}

	public static class PlanContract implements BaseColumns {
		
		/** FIELDS **/
		public static final String TIMESTAMP = "time_stamp";
		public static final String PLAN_ID = "plan_id";
		public static final String TITLE = "title";
		public static final String DESCRIPTION = "description";
		public static final String TOTAL_PRICE = "total_price";
		public static final String TOTAL_PRICE_AS_REAL = "total_price_as_real";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int TIMESTAMP_INDEX = 1;
		public static final int PLAN_ID_INDEX = 2;
		public static final int TITLE_INDEX = 3;
		public static final int DESCRIPTION_INDEX = 4;
		public static final int TOTAL_PRICE_INDEX = 5;
		public static final int TOTAL_PRICE_AS_REAL_INDEX = 6;

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.PLAN).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.plan";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.plan";
		public static final String[] PROJECTION = {
			_ID, TIMESTAMP, PLAN_ID, TITLE, DESCRIPTION, TOTAL_PRICE, TOTAL_PRICE_AS_REAL };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s INTEGER NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL, " +
					"%s TEXT NOT NULL, " +
					"%s REAL NOT NULL);",
					Tables.PLAN,
					BaseColumns._ID,
					TIMESTAMP,
					PLAN_ID,
					TITLE,
					DESCRIPTION,
					TOTAL_PRICE,
					TOTAL_PRICE_AS_REAL);
		public static String INDEX_CREATE_SQL =
				String.format(
					"CREATE INDEX IF NOT EXISTS ix_" + Tables.PLAN + " " +
					"ON " + Tables.PLAN + "(" + PLAN_ID + ")");
	}

	public static class PlanChargeContract implements BaseColumns {
		
		/** FIELDS **/
		public static final String PLAN_ID = "plan_id";
		public static final String DESCRIPTION = "description";
		public static final String PRICE = "price";
		public static final String RECURRING = "recurring";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int PLAN_ID_INDEX = 1;
		public static final int DESCRIPTION_INDEX = 2;
		public static final int PRICE_INDEX = 3;
		public static final int RECURRING_INDEX = 4;

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.PLAN_CHARGE).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.plan.charge";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.plan.charge";
		public static final String[] PROJECTION = {
			_ID, PLAN_ID, DESCRIPTION, PRICE, RECURRING };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL, " +
					"%s TEXT NOT NULL);",
					Tables.PLAN_CHARGE,
					BaseColumns._ID,
					PLAN_ID,
					DESCRIPTION,
					PRICE,
					RECURRING);
		public static String INDEX_CREATE_SQL =
				String.format(
					"CREATE INDEX IF NOT EXISTS ix_" + Tables.PLAN_CHARGE + " " +
					"ON " + Tables.PLAN_CHARGE + "(" + PLAN_ID + ")");
	}

	public static class RateCityContract implements BaseColumns {
		/** FIELDS **/
		public static final String COUNTRY_ID = "country_id";
		public static final String CITY = "city";
		public static final String RATE = "rate";
		public static final String DIAL_CODE = "dial_code";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int COUNTRY_ID_INDEX = 1;
		public static final int CITY_INDEX = 2;
		public static final int RATE_INDEX = 3;
		public static final int DIAL_CODE_INDEX = 4;

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.RATE_CITY).build();
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.rate.city";
		public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.rate.city";
		public static final String[] PROJECTION = {
				_ID, COUNTRY_ID, CITY, RATE, DIAL_CODE };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s INTEGER NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL);",
					Tables.RATE_CITY,
					BaseColumns._ID,
					COUNTRY_ID,
					CITY,
					RATE,
					DIAL_CODE);

		public static String TABLE_CREATE_INDEX_1 =
				String.format(
					"CREATE INDEX IF NOT EXISTS IX_RATE_CITY ON %s (" + COUNTRY_ID + ");",
					Tables.RATE_CITY);
	}

	public static class RateCountryContract implements BaseColumns {
		/** FIELDS **/
		public static final String COUNTRY_ID = "country_id";
		public static final String COUNTRY_GROUP = "country_group";
		public static final String COUNTRY = "country";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int COUNTRY_ID_INDEX = 1;
		public static final int COUNTRY_GROUP_INDEX = 2;
		public static final int COUNTRY_INDEX = 3;

		public static final Uri CONTENT_URI_GROUPS_TRIAL =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.RATE_COUNTRY).appendPath("groups").appendPath("trial").build();
		public static final Uri CONTENT_URI_GROUPS =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.RATE_COUNTRY).appendPath("groups").build();
		public static final Uri CONTENT_TRIAL_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.RATE_COUNTRY).appendPath("trial").build();
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.RATE_COUNTRY).build();
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.rate.country";
		public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.rate.country";
		public static final String[] PROJECTION = {
				_ID, COUNTRY_ID, COUNTRY_GROUP, COUNTRY };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s INTEGER NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL);",
					Tables.RATE_COUNTRY,
					BaseColumns._ID,
					COUNTRY_ID,
					COUNTRY_GROUP,
					COUNTRY);

		public static String TABLE_CREATE_INDEX_1 =
				String.format(
					"CREATE INDEX IF NOT EXISTS IX_RATE_COUNTRY_GROUP ON %s (" + COUNTRY_GROUP + ");",
					Tables.RATE_COUNTRY);

		public static String TABLE_CREATE_INDEX_2 =
				String.format(
					"CREATE INDEX IF NOT EXISTS IX_RATE_COUNTRY_ID ON %s (" + COUNTRY_ID + ");",
					Tables.RATE_COUNTRY);
	}

	public static class RequestContract implements BaseColumns {
		/** FIELDS **/
		public static final String UPDATED = "updated";
		public static final String TIMESTAMP = "time_stamp";
		public static final String SUCCESS = "success";
		public static final String HTTP_STATUS = "http_status";
		public static final String ERROR = "error";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int UPDATED_INDEX = 1;
		public static final int TIMESTAMP_INDEX = 2;
		public static final int SUCCESS_INDEX = 3;
		public static final int HTTP_STATUS_INDEX = 4;
		public static final int ERROR_INDEX = 5;
		
		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(Tables.REQUEST).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.request";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.request";
		public static final String[] PROJECTION = {
			_ID, UPDATED, TIMESTAMP, SUCCESS, HTTP_STATUS, ERROR };

		public static String TABLE_CREATE_SQL =
			String.format(
				"CREATE TABLE IF NOT EXISTS %s (" +
				"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
				"%s INTEGER NOT NULL," +
				"%s INTEGER NOT NULL," +
				"%s INTEGER NOT NULL," +
				"%s INTEGER NOT NULL," +
				"%s TEXT NULL);",
				Tables.REQUEST,
				BaseColumns._ID,
				UPDATED,
				TIMESTAMP,
				SUCCESS,
				HTTP_STATUS,
				ERROR);
	}
	
	public static class SipUserContract implements BaseColumns {
		
		/** FIELDS **/
		public static final String TIMESTAMP = "time_stamp";
		public static final String ACCOUNT_NO = "account_no";
		public static final String USERNAME = "username";
		public static final String PASSWORD = "password";
		public static final String DISPLAY_NAME = "display_name";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int TIMESTAMP_INDEX = 1;
		public static final int ACCOUNT_NO_INDEX = 2;
		public static final int USERNAME_INDEX = 3;
		public static final int PASSWORD_INDEX = 4;
		public static final int DISPLAY_NAME_INDEX = 5;

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.SIP_USER).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.sipuser";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.sipuser";
		public static final String[] PROJECTION = {
			_ID, TIMESTAMP, ACCOUNT_NO, USERNAME, PASSWORD, DISPLAY_NAME };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s INTEGER NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s TEXT NOT NULL, " +
					"%s TEXT NOT NULL);",
					Tables.SIP_USER,
					BaseColumns._ID,
					TIMESTAMP,
					ACCOUNT_NO,
					USERNAME,
					PASSWORD,
					DISPLAY_NAME);
		public static String INDEX_CREATE_SQL =
				String.format(
					"CREATE INDEX IF NOT EXISTS ix_" + Tables.SIP_USER + " " +
					"ON " + Tables.SIP_USER + "(" + ACCOUNT_NO + ")");
	}

	public static class TrialDialCodeContract implements BaseColumns {
		/** FIELDS **/
		public static final String COUNTRY_ID = "country_id";
		public static final String DIAL_CODE = "dial_code";
		public static final String BLOCKED = "blocked";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int COUNTRY_ID_INDEX = 1;
		public static final int DIAL_CODE_INDEX = 2;
		public static final int BLOCKED_INDEX = 3;

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.TRIAL_DIAL_CODE).build();
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.trial.dialcode";
		public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.trial.dialcode";
		public static final String[] PROJECTION = {
				_ID, COUNTRY_ID, DIAL_CODE, BLOCKED };

		public static String TABLE_CREATE_SQL =
				String.format(
					"CREATE TABLE IF NOT EXISTS %s (" +
					"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
					"%s INTEGER NOT NULL," +
					"%s TEXT NOT NULL," +
					"%s INTEGER NOT NULL);",
					Tables.TRIAL_DIAL_CODE,
					BaseColumns._ID,
					COUNTRY_ID,
					DIAL_CODE,
					BLOCKED);

		public static String TABLE_CREATE_INDEX =
				String.format(
					"CREATE INDEX IF NOT EXISTS IX_TRIAL_COUNTRY_ID ON %s (" + COUNTRY_ID + ");",
					Tables.TRIAL_DIAL_CODE);
	}

	public static class VersionCheckContract implements BaseColumns {
		/** FIELDS **/
		public static final String SUPPORTED = "supported";
		public static final String TIMESTAMP = "time_stamp";

		/** FIELD INDEXES **/
		public static final int ID_INDEX = 0;
		public static final int SUPPORTED_INDEX = 1;
		public static final int TIMESTAMP_INDEX = 2;

		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(Tables.VERSION).build();
		public static final String CONTENT_TYPE =
			ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.check.version";
		public static final String CONTENT_ITEM_TYPE =
			ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.check.version";
		public static final String[] PROJECTION = {
			_ID, SUPPORTED, TIMESTAMP };

		public static String TABLE_CREATE_SQL =
			String.format(
				"CREATE TABLE IF NOT EXISTS %s (" +
				"%s INTEGER PRIMARY KEY AUTOINCREMENT," +
				"%s INTEGER NOT NULL," +
				"%s INTEGER NOT NULL);",
				Tables.VERSION,
				BaseColumns._ID,
				SUPPORTED,
				TIMESTAMP);
	}

	public static class ProvisionCheckContract {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath("provision_check").build();
			public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.check.provision";
			public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.voxcorp.check.provision";
	}

}
