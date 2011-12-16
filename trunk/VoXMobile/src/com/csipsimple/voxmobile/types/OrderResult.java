/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.types;

public class OrderResult {

	private static final String _BAD_FIRST_NAME = "badvalue.contact.first_name";
	private static final String _BAD_LAST_NAME = "badvalue.contact.last_name";
	private static final String _BAD_EMAIL = "badvalue.contact.email";
	private static final String _BAD_BILLING_ADDRESS = "badvalue.billing_address.address1";
	private static final String _BAD_BILLING_CITY = "badvalue.billing_address.city";
	private static final String _BAD_BILLING_ZIP = "badvalue.billing_address.zip";
	private static final String _BAD_BILLING_COUNTRY = "badvalue.billing_address.country";
	private static final String _BAD_CC_NUMBER = "badvalue.cc_info.cc_number";
	private static final String _BAD_CC_INFO_MONTH = "badvalue.cc_info.cc_exp_month";
	private static final String _BAD_CC_INFO_YEAR = "badvalue.cc_info.cc_exp_year";
	private static final String _BAD_CC_INFO_CVV = "badvalue.cc_info.cc_cvv";
	private static final String _BAD_CC_AUTH_FAIL = "cc.authfail";
	private static final String _PLAN_OVERSUBSCRIBED = "sys.oversubscribed";
	private static final String _DEPRECATED_BUILD = "sys.deprecated_build";

	public static final int NO_ERROR = 0;
	public static final int BAD_FIRST_NAME = 1;
	public static final int BAD_LAST_NAME = 2;
	public static final int BAD_EMAIL = 3;
	public static final int BAD_BILLING_ADDRESS = 4;
	public static final int BAD_BILLING_CITY = 5;
	public static final int BAD_BILLING_ZIP = 6;
	public static final int BAD_BILLING_COUNTRY = 7;
	public static final int BAD_CC_NUMBER = 8;
	public static final int BAD_CC_INFO_MONTH = 9;
	public static final int BAD_CC_INFO_YEAR = 10;
	public static final int BAD_CC_INFO_CVV = 11;
	public static final int BAD_CC_AUTH_FAIL = 12;
	public static final int PLAN_OVERSUBSCRIBED = 13;
	public static final int DEPRECATED_BUILD = 14;
	public static final int SYSTEM_ERROR = 15;
	
	public boolean mSuccess = false;
	public String mFailureType;
	public String mAuthUuid;
	public String mResultString;
	public String mChargeAuthCode;
	public double mChargeAmount;
	public String mLoginName;
	public String mLoginPassword;
	
	public int getErrorCode() {
		if (mSuccess) return NO_ERROR;
		else if (mResultString.startsWith(_BAD_FIRST_NAME)) return BAD_FIRST_NAME;
		else if (mResultString.startsWith(_BAD_LAST_NAME)) return BAD_LAST_NAME;
		else if (mResultString.startsWith(_BAD_EMAIL)) return BAD_EMAIL;
		else if (mResultString.startsWith(_BAD_BILLING_ADDRESS)) return BAD_BILLING_ADDRESS;
		else if (mResultString.startsWith(_BAD_BILLING_CITY)) return BAD_BILLING_CITY;
		else if (mResultString.startsWith(_BAD_BILLING_ZIP)) return BAD_BILLING_ZIP;
		else if (mResultString.startsWith(_BAD_BILLING_COUNTRY)) return BAD_BILLING_COUNTRY;
		else if (mResultString.startsWith(_BAD_CC_NUMBER)) return BAD_CC_NUMBER;
		else if (mResultString.startsWith(_BAD_CC_INFO_MONTH)) return BAD_CC_INFO_MONTH;
		else if (mResultString.startsWith(_BAD_CC_INFO_YEAR)) return BAD_CC_INFO_YEAR;
		else if (mResultString.startsWith(_BAD_CC_INFO_CVV)) return BAD_CC_INFO_CVV;
		else if (mResultString.startsWith(_BAD_CC_AUTH_FAIL)) return BAD_CC_AUTH_FAIL;
		else if (mResultString.startsWith(_PLAN_OVERSUBSCRIBED)) return PLAN_OVERSUBSCRIBED;
		else if (mResultString.startsWith(_DEPRECATED_BUILD)) return DEPRECATED_BUILD;
		else return SYSTEM_ERROR;
	}
	
	public String getErrorString() {
		if (mSuccess) {
			return "";
		} else {
			int idx = mResultString.indexOf(":");
			if (idx < 0)
				return mResultString;
			else
				return mResultString.substring(idx + 1).trim();
		}
	}
}
