/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.service;


public class ServiceHelper {
		
	public final static int MODE_DEVELOPMENT = 1; 
	public final static int MODE_STAGE = 2; 
	public final static int MODE_PRODUCTION = 3;
	public final static int MODE_ACTIVE = MODE_DEVELOPMENT;

	/** SOAP server hosts **/
	private static final String URL_PROD = "host";
	private static final String URL_STAGE = "host";
	private static final String URL_DEV = "host";
	
	/** STUN server hosts **/
	private static final String STUN_HOST_PROD = "host";
	private static final String STUN_HOST_STAGE = "host";
	private static final String STUN_HOST_DEV = "host";
	
	/** FAQ links **/
	private static final String FAQ_LINK_DEV = "url";
	private static final String FAQ_LINK_STAGE = "url";
	private static final String FAQ_LINK_PROD = "url";
	
	/** Google Analytics account **/
	private static final String GOOGLE_ANALYTICS_PROD = "account";
	private static final String GOOGLE_ANALYTICS_STAGE = "account";
	private static final String GOOGLE_ANALYTICS_DEV = "account";
	
	/** Google Analytics Tracker settings */
	public static final boolean GOOGLE_ANALYTICS_DEBUG = false;
	public static final boolean GOOGLE_ANALYTICS_DRY_RUN = false;
	public static final int GOOGLE_ANALYTICS_DISPATCH_INTERVAL = 10;

	/** Ordering Service **/
	public static final String ORDER_SERVICE_KEY = "service_key";
	public static final String ORDER_SERVICE_KEY_ID = "value";
	
	/** Actions that can be invoked by registered clients **/
	public static final int MSG_BASE = 1001;
    public static final int MSG_REGISTER_CLIENT = MSG_BASE + 1;
    public static final int MSG_UNREGISTER_CLIENT = MSG_BASE + 2;
    public static final int MSG_GET_STATE = MSG_BASE + 3;
    public static final int MSG_SERVICE_RESPONSE = MSG_BASE + 4;

    /** Mobile Service methods available to registered clients **/
	public final static int METHOD_BASE = 2001;
	public final static int METHOD_GET_AUTH_UUID = METHOD_BASE + 1;
	public final static int METHOD_GET_SIP_USERS = METHOD_BASE + 2;
	public final static int METHOD_GET_SIP_USER_INFO = METHOD_BASE + 3;
	public final static int METHOD_GET_SERVICE_PLANS = METHOD_BASE + 4;
	public final static int METHOD_GET_DID_STATES = METHOD_BASE + 5;
	public final static int METHOD_GET_DID_CITIES = METHOD_BASE + 6;
	public final static int METHOD_SUBMIT_ORDER = METHOD_BASE + 7;
	public final static int METHOD_IS_PROVISIONED = METHOD_BASE + 8;
	public final static int METHOD_IS_VERSION_SUPPORTED = METHOD_BASE + 9;

	/** Mobile Service response types **/
	private static final int SUCCESS_BASE = 200;
	public static final int SUCCESS_GET_AUTH_UUID = SUCCESS_BASE + 1;
	public static final int SUCCESS_GET_SIP_USERS = SUCCESS_BASE + 2;
	public static final int SUCCESS_GET_SIP_USER_INFO = SUCCESS_BASE + 3;
	public static final int SUCCESS_GET_SERVICE_PLANS = SUCCESS_BASE + 4;
	public static final int SUCCESS_GET_DID_STATES = SUCCESS_BASE + 5;
	public static final int SUCCESS_GET_DID_CITIES = SUCCESS_BASE + 6;
	public static final int SUCCESS_SUBMIT_ORDER = SUCCESS_BASE + 7;
	public static final int SUCCESS_IS_PROVISIONED = SUCCESS_BASE + 8;
	public static final int SUCCESS_IS_VERSION_SUPPORTED = SUCCESS_BASE + 9;
	public static final int START_PROVISIONED_CHECK = SUCCESS_BASE + 10;
	public static final int END_PROVISIONED_CHECK = SUCCESS_BASE + 11;

	/** Activity codes **/
	public static final int CHOOSE_PLAN = 0;
	public static final int SELECT_RATE_CENTER = 1;

	/** Service error types **/
	private static final int ERROR_BASE = 300; 
	public static final int ERROR_UNAUTHORIZED = ERROR_BASE + 1;
	public static final int ERROR_GENERAL = ERROR_BASE + 2;
	
	/** Web Service attributes **/
	public static final String METHOD = "method";
	public static final String SIP_USERNAME = "username";
	public static final String SIP_PASSWORD = "password";
	public static final String IMEI = "imei";
	public static final String AUTH_UUID = "auth_uuid";
	public static final String UUID = "uuid";
	public static final String LOGIN_UID = "uid";
	public static final String LOGIN_PWD = "pwd";
	public static final String PACKAGE = "plan_id";
	public static final String DID_STATE = "state";
	public static final String DID_STATE_NAME = "state_name";
	public static final String DID_CITY = "city";
	public static final String REQUESTED_VERSION = "requested_version";
	
	/** Service states **/
	public static final int STATE_RUNNING = 1;
	public static final int STATE_NOT_RUNNING = 2;
	
	/** Stuff for faking VoX-to-VoX rate center **/
	public static final String VOXLAND_STATE = "state";
	public static final String VOXLAND_STATE_NAME = "state_name";
	public static final String VOXLAND_CITY = "city";

	public static String getWebserviceHost() {
		switch (MODE_ACTIVE) {
          case MODE_DEVELOPMENT:
			  return URL_DEV;
		  case MODE_STAGE:
			  return URL_STAGE;
		  default:
			  return URL_PROD;
		}
	}
	
	public static String getStunServer() {
		switch (MODE_ACTIVE) {
          case MODE_DEVELOPMENT:
			  return STUN_HOST_DEV;
		  case MODE_STAGE:
			  return STUN_HOST_STAGE;
		  default:
			  return STUN_HOST_PROD;
		}
	}
	
	public static String getFaqLink() {
		switch (MODE_ACTIVE) {
          case MODE_DEVELOPMENT:
			  return FAQ_LINK_DEV;
		  case MODE_STAGE:
			  return FAQ_LINK_STAGE;
		  default:
			  return FAQ_LINK_PROD;
		}
	}
	
	public static String getGoogleAnalyticsAccount() {
		switch (MODE_ACTIVE) {
		  case MODE_DEVELOPMENT:
			  return GOOGLE_ANALYTICS_DEV;
		  case MODE_STAGE:
			  return GOOGLE_ANALYTICS_STAGE;
		  default:
			  return GOOGLE_ANALYTICS_PROD;
		}
	}
}
