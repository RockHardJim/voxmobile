/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.utils;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class OrderHelper {
	
	private static final String ORDER_DATA = "voxmobile_order";
	
	/* Order Fields */
	public static final String PLAN_ID = "plan_id";
	public static final String FIRST_NAME = "first_name";
	public static final String LAST_NAME = "last_name";
	public static final String EMAIL = "email";
	public static final String EMAIL_CONFIRM = "email_confirm";
	public static final String BILLING_COUNTRY_INDEX = "billing_country_index";
	public static final String BILLING_COUNTRY = "billing_country";
	public static final String BILLING_STREET = "billing_street";
	public static final String BILLING_CITY = "billing_city";
	public static final String BILLING_POSTAL_CODE = "billing_postal_code";
	public static final String CC_NUMBER = "cc_number";
	public static final String CC_CVV = "cc_cvv";
	public static final String CC_EXP_MONTH = "cc_exp_month";
	public static final String CC_EXP_YEAR = "cc_exp_year";
	public static final String DID_STATE = "did_state";
	public static final String DID_STATE_NAME = "did_state_name";
	public static final String DID_CITY = "did_city";
	public static final String IMEI = "imei";
	public static final String PROVISION_STATUS = "provision_status";
	
	/** Provision status values **/
	public static final String PROVISION_STATUS_WAITING = "waiting";
	public static final String PROVISION_STATUS_NONE = "none";
	
	public static void setProvisionStatus(Context context, String status) {
		setStringValue(context, PROVISION_STATUS, status);
	}
	
	public static String getProvisionStatus(Context context) {
		return getStringValue(context, PROVISION_STATUS);
	}

	public static String[] getCardYears() {
	   	String[] years = new String[10];
	   	
	    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
       	for (int i = 0; i < 10; i++)
       		years[i] = Integer.toString(currentYear + i);
       	
       	return years;
	}
	
	public static int getSelectedYear(Context context) {
		int yearOffset = getIntValue(context, CC_EXP_YEAR, 0);
	    int currentYear = Calendar.getInstance().get(Calendar.YEAR);	
		
		return currentYear + yearOffset;
	}
	
	public static boolean isCanadianOrder(Context context) {
		String country = getStringValue(context, BILLING_COUNTRY);
		return country.equalsIgnoreCase("CA");
	}
	
	public static boolean isDomesticOrder(Context context) {
		String country = getStringValue(context, BILLING_COUNTRY);
		return (country.equalsIgnoreCase("CA") ||
				country.equalsIgnoreCase("PR") ||
				country.equalsIgnoreCase("US"));
	}
	
	private static SharedPreferences getPrefs(Context context) {
		return context.getSharedPreferences(ORDER_DATA, Context.MODE_PRIVATE);
	}
	
	public static void clear(Context context) {

		SharedPreferences prefs = getPrefs(context);
		Editor edit = prefs.edit();
		edit.remove(PLAN_ID);
		edit.remove(CC_NUMBER);
		edit.remove(CC_CVV);
		edit.remove(CC_EXP_MONTH);
		edit.remove(CC_EXP_YEAR);
		edit.remove(DID_STATE);
		edit.remove(DID_STATE_NAME);
		edit.remove(DID_CITY);
		edit.remove(PROVISION_STATUS);
		edit.commit();
	}
	
	public static String getStringValue(Context context, String item) {
		return getPrefs(context).getString(item, "");
	}
	
	public static void setStringValue(Context context, String item, String value) {
		getPrefs(context).edit().putString(item, value).commit();
	}
	
	public static int getIntValue(Context context, String item, int defaultValue) {
		return getPrefs(context).getInt(item, defaultValue);
	}
	
	public static void setIntValue(Context context, String item, int value) {
		getPrefs(context).edit().putInt(item, value).commit();
	}
	
}
