/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import net.voxcorp.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.csipsimple.voxmobile.service.MobileService;
import com.csipsimple.voxmobile.service.ServiceHelper;
import com.csipsimple.voxmobile.types.OrderResult;
import com.csipsimple.voxmobile.types.ServicePlan;
import com.csipsimple.voxmobile.types.ServicePlanCharge;
import com.csipsimple.voxmobile.utils.OrderHelper;
import com.csipsimple.wizards.impl.VoXMobile;

public class StartActivity extends ServiceClientBaseActivity implements OnClickListener, TextWatcher {

	/** Dialog Types used in to respond to various MobileService exceptions **/
	private static final int DIALOG_GENERAL_ERROR = 1;
	private static final int DIALOG_UNAUTHORIZED = 2;
	private static final int DIALOG_VALIDATION_ERROR = 3;
	private static final int DIALOG_SIGNUP_OVERVIEW = 4;
	private static final int DIALOG_CHOOSE_COUNTRY = 5;
	private static final int DIALOG_BILLING_INFO = 6;
	private static final int DIALOG_DID_OVERVIEW = 7;
	private static final int DIALOG_ORDER_SUMMARY = 8;
	private static final int DIALOG_ORDER_ERROR = 9;
	private static final int DIALOG_ORDER_SUCCESS = 10;
	
	private static String mDialogMsg = "";
	private static ArrayList<ServicePlan> mServicePlans = null;
	private static ServicePlan mServicePlan = null;

	private static String[] mCountries = null;
	private static String[] mMonths = null;
	private static OrderResult mOrderResult = null;

	private Dialog dlgBilling;
	private boolean mShowSignupOverview = true;
	private boolean mShowDIDOverview = true;
	
	private static final int SUPPORTED_UNKNOWN = 0;
	private static final int SUPPORTED_NO = 1;
	private static final int SUPPORTED_YES = 2;
	private static int mIsSupported = SUPPORTED_UNKNOWN;

	/** Handler that receives messages from the OrderService process **/
	class IncomingHandler extends ServiceClientBaseActivity.IncomingHandler {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case ServiceHelper.MSG_SERVICE_RESPONSE:

				dismissProgressDialog();

				switch (msg.arg2) {
				case ServiceHelper.SUCCESS_IS_VERSION_SUPPORTED:
					mIsSupported = ((Boolean)msg.obj == true) ? SUPPORTED_YES : SUPPORTED_NO;
					break;
				case ServiceHelper.SUCCESS_GET_SERVICE_PLANS:
					mServicePlans = (ArrayList<ServicePlan>) msg.obj;

					// Build array lists for expandable list
					ArrayList<String> names = new ArrayList<String>();
					ArrayList<String> descriptions = new ArrayList<String>();
					Iterator<ServicePlan> it = mServicePlans.iterator();
					while (it.hasNext()) {
						ServicePlan sp = it.next();
						names.add(sp.mPlanName);
						descriptions.add(sp.mPlanDescription);
					}

					Bundle bundle = new Bundle();
					bundle.putParcelableArrayList("plans", mServicePlans);

					Intent intent = new Intent(StartActivity.this, ServicePlanChooser.class);
					intent.putExtras(bundle);
					startActivityForResult(intent, ServiceHelper.CHOOSE_PLAN);
					break;
				case ServiceHelper.SUCCESS_SUBMIT_ORDER:
					mOrderResult = (OrderResult) msg.obj;
					
					mProgress.dismiss();
					
					handleOrderResult();
					
					break;
				case ServiceHelper.ERROR_UNAUTHORIZED:
					showDialog(DIALOG_UNAUTHORIZED);
					break;
				case ServiceHelper.ERROR_GENERAL:
					mDialogMsg = (String) msg.obj;
					showDialog(DIALOG_GENERAL_ERROR);
					break;
				}

				break;

			case ServiceHelper.MSG_GET_STATE:
				Log.d(THIS_FILE, "Service State: " + msg.arg1);

				if (msg.arg1 == ServiceHelper.STATE_RUNNING)
					mProgress.show();

				break;

			default:
				break;
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		THIS_FILE = "StartActivity";
		
		setContentView(R.layout.voxmobile_start);

		mMessenger = new Messenger(new IncomingHandler());
		
		// Bind signup button
		LinearLayout add_row = (LinearLayout) findViewById(R.id.voxmobile_signup);
		add_row.setOnClickListener(this);

		// Bind login button
		add_row = (LinearLayout) findViewById(R.id.voxmobile_login);
		add_row.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		if (mIsSupported != SUPPORTED_YES) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle(R.string.voxmobile_attention);
			alertDialog.setMessage(getString(R.string.voxmobile_upgrade));
			alertDialog.setButton(getString(R.string.voxmobile_close), new DialogInterface.OnClickListener() {
			   public void onClick(DialogInterface dialog, int which) {
			      // here you can add functions
			   }
			});
			alertDialog.show();
			return;
		}			
		
		switch (v.getId()) {
		case R.id.voxmobile_signup:
			if (mShowSignupOverview) {
				trackEvent("signup", "clicked", 0);
				mShowSignupOverview = false;
				showDialog(DIALOG_SIGNUP_OVERVIEW);
			} else {
				getServicePlans();
			}
			
			break;
		case R.id.voxmobile_login:
			trackEvent("login", "clicked", 0);
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			break;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (mIsSupported == SUPPORTED_UNKNOWN) {
			mProgress.setMessage(getString(R.string.voxmobile_please_wait));
			mProgress.show();
			
			Intent intent = new Intent(StartActivity.this, MobileService.class);
			intent.putExtra(ServiceHelper.METHOD, ServiceHelper.METHOD_IS_VERSION_SUPPORTED);
			startService(intent);
		}
	}

	private void getServicePlans() {
		mProgress.setMessage(getString(R.string.voxmobile_please_wait));
		mProgress.show();

		Intent intent = new Intent(StartActivity.this, MobileService.class);
		intent.putExtra(ServiceHelper.METHOD, ServiceHelper.METHOD_GET_SERVICE_PLANS);
		startService(intent);
	}
	
	private void showDIDSelection() {
		Intent intent = new Intent(StartActivity.this, RateCenterChooser.class);
		intent.putExtra(ServiceHelper.DID_STATE_NAME, OrderHelper.getStringValue(this, OrderHelper.DID_STATE_NAME));
		intent.putExtra(ServiceHelper.DID_STATE, OrderHelper.getStringValue(this, OrderHelper.DID_STATE));
		intent.putExtra(ServiceHelper.DID_CITY, OrderHelper.getStringValue(this, OrderHelper.DID_CITY));
		startActivityForResult(intent, ServiceHelper.SELECT_RATE_CENTER);
	}
	
	private String getCharges() {
		String charges = "";
		double total = 0.00;
			
		if (mServicePlan != null) {
			Iterator<ServicePlanCharge> it = mServicePlan.mCharges.iterator();
			while (it.hasNext()) {
				ServicePlanCharge charge = it.next();
				charges += String.format("%s: $%4.2f\n", charge.mDescription, charge.mPrice);
				total += charge.mPrice;
			}
			
			charges += String.format("%s: $%4.2f",
						getString(R.string.voxmobile_total_charges),
						total);
		}
		
		return charges;
	}
	
	private void submitOrder() {
		mProgress.setMessage(getString(R.string.voxmobile_submitting_order));
		mProgress.show();

		Intent intent = new Intent(StartActivity.this, MobileService.class);
		intent.putExtra(ServiceHelper.METHOD, ServiceHelper.METHOD_SUBMIT_ORDER);
		startService(intent);
	}
	
	private void handleOrderResult() {
		if (mOrderResult.mSuccess) {
			trackPageView("order/provision_wait");
			VoXMobile.setUuid(this, mOrderResult.mAuthUuid);
			OrderHelper.setProvisionStatus(this, OrderHelper.PROVISION_STATUS_WAITING);
			showDialog(DIALOG_ORDER_SUCCESS);
		} else {
			trackPageView("order/error");
			OrderHelper.setProvisionStatus(this, OrderHelper.PROVISION_STATUS_NONE);
			showDialog(DIALOG_ORDER_ERROR);
		}
	}

	private void doFinish() {
		startActivity(new Intent(StartActivity.this, ProvisionWaitActivity.class));
		finish();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {

		TextView text;
		Button button;
		ArrayAdapter<String> adapter;

		switch (id) {
		case DIALOG_SIGNUP_OVERVIEW:
			trackPageView("order/start");

			// set up sign up overview dialog
			Dialog dlgSignup = new Dialog(StartActivity.this);
			dlgSignup.setContentView(R.layout.voxmobile_signup_overview);
			dlgSignup.setTitle(R.string.voxmobile_signup_header);
			dlgSignup.setCancelable(true);

			// set up text
			text = (TextView) dlgSignup.findViewById(R.id.TextView01);
			text.setText(R.string.voxmobile_signup_text_1);

			text = (TextView) dlgSignup.findViewById(R.id.TextView02);
			text.setText(R.string.voxmobile_signup_text_2);

			text = (TextView) dlgSignup.findViewById(R.id.TextView03);
			text.setText(R.string.voxmobile_signup_text_3);

			// set up continue button
			button = (Button) dlgSignup.findViewById(R.id.do_signup_continue);
			button.setText(R.string.voxmobile_continue);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_SIGNUP_OVERVIEW);
					getServicePlans();
				}
			});

			// set up cancel button
			button = (Button) dlgSignup.findViewById(R.id.do_signup_cancel);
			button.setText(R.string.cancel);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_SIGNUP_OVERVIEW);
				}
			});

			return dlgSignup;

		case DIALOG_CHOOSE_COUNTRY:
			trackPageView("order/choose_country");

			// set up country chooser dialog
			final Dialog dlgCountry = new Dialog(StartActivity.this);
			dlgCountry.setContentView(R.layout.voxmobile_country_chooser);
			dlgCountry.setTitle(R.string.voxmobile_country_msg);
			dlgCountry.setCancelable(true);

			if (mCountries == null)
				mCountries = getResources().getStringArray(
						R.array.voxmobile_country_array);

			adapter = new ArrayAdapter<String>(this, R.layout.voxmobile_list_item, mCountries);
			Spinner countrySpinner = (Spinner) dlgCountry.findViewById(R.id.voxmobile_country);
			countrySpinner.setAdapter(adapter);
			countrySpinner.setSelection(OrderHelper.getIntValue(this,
					OrderHelper.BILLING_COUNTRY_INDEX, 0));

			countrySpinner
					.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
							Button button = (Button) dlgCountry.findViewById(R.id.do_voxmobile_country_list_continue);
							button.setEnabled(position > 0);
						}

						@Override
						public void onNothingSelected(AdapterView<?> parentView) {}
					});

			// set up continue button
			button = (Button) dlgCountry.findViewById(R.id.do_voxmobile_country_list_continue);
			button.setText(R.string.voxmobile_continue);
			button.setEnabled(false);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Spinner list = (Spinner) dlgCountry.findViewById(R.id.voxmobile_country);
					int selectedCountry = list.getSelectedItemPosition();

					String[] mCountryCodes = getResources().getStringArray(R.array.voxmobile_country_code_array);
					OrderHelper.setIntValue(StartActivity.this, OrderHelper.BILLING_COUNTRY_INDEX, selectedCountry);
					OrderHelper.setStringValue(StartActivity.this, OrderHelper.BILLING_COUNTRY, mCountryCodes[(int) selectedCountry]);
					dismissDialog(DIALOG_CHOOSE_COUNTRY);
					showDialog(DIALOG_BILLING_INFO);
				}
			});

			// set up cancel button
			button = (Button) dlgCountry.findViewById(R.id.do_voxmobile_country_list_cancel);
			button.setText(R.string.cancel);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_CHOOSE_COUNTRY);
				}
			});

			return dlgCountry;

		case DIALOG_BILLING_INFO:
			trackPageView("order/billing_info");

			// set up country chooser dialog
			dlgBilling = new Dialog(StartActivity.this);
			dlgBilling.setContentView(R.layout.voxmobile_billing_info);
			dlgBilling.setTitle(R.string.voxmobile_billing_title);
			dlgBilling.setCancelable(true);

			// First Name
			EditText firstName = (EditText) dlgBilling.findViewById(R.id.voxmobile_first_name);
			firstName.setText(OrderHelper.getStringValue(this, OrderHelper.FIRST_NAME), TextView.BufferType.EDITABLE);
			firstName.addTextChangedListener(this);

			// Last Name
			EditText lastName = (EditText) dlgBilling.findViewById(R.id.voxmobile_last_name);
			lastName.setText(OrderHelper.getStringValue(this, OrderHelper.LAST_NAME), TextView.BufferType.EDITABLE);
			lastName.addTextChangedListener(this);

			// Email
			EditText email = (EditText) dlgBilling.findViewById(R.id.voxmobile_email_address);
			email.setText(OrderHelper.getStringValue(this, OrderHelper.EMAIL), TextView.BufferType.EDITABLE);
			email.addTextChangedListener(this);

			// Email Confirm
			EditText confirmEmail = (EditText) dlgBilling.findViewById(R.id.voxmobile_email_address_confirm);
			confirmEmail.setText(OrderHelper.getStringValue(this, OrderHelper.EMAIL_CONFIRM), TextView.BufferType.EDITABLE);
			confirmEmail.addTextChangedListener(this);

			if (mServicePlan.mTotalPrice > 0.00) {

				// Card Number
				EditText ccNumber = (EditText) dlgBilling.findViewById(R.id.voxmobile_card_number);
				ccNumber.setText(OrderHelper.getStringValue(this, OrderHelper.CC_NUMBER), TextView.BufferType.EDITABLE);
				ccNumber.addTextChangedListener(this);

				// Card CVV
				EditText ccCvv = (EditText) dlgBilling.findViewById(R.id.voxmobile_card_cvv);
				ccCvv.setText(OrderHelper.getStringValue(this, OrderHelper.CC_CVV), TextView.BufferType.EDITABLE);
				ccCvv.addTextChangedListener(this);

				// Card Month
				if (mMonths == null)
					mMonths = getResources().getStringArray(R.array.voxmobile_months_array);

				adapter = new ArrayAdapter<String>(this, R.layout.voxmobile_list_item, mMonths);
				Spinner ccMonth = (Spinner) dlgBilling.findViewById(R.id.voxmobile_card_month);
				ccMonth.setAdapter(adapter);
				ccMonth.setSelection(OrderHelper.getIntValue(this, OrderHelper.CC_EXP_MONTH, 0));
				ccMonth.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
						preValidateForm();
					}

					@Override
					public void onNothingSelected(AdapterView<?> parentView) {}
				});

				// Card Year
				adapter = new ArrayAdapter<String>(this, R.layout.voxmobile_list_item, OrderHelper.getCardYears());
				Spinner ccYear = (Spinner) dlgBilling.findViewById(R.id.voxmobile_card_year);
				ccYear.setAdapter(adapter);
				ccYear.setSelection(OrderHelper.getIntValue(this, OrderHelper.CC_EXP_YEAR, 0));
				ccYear.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
						preValidateForm();
					}

					@Override
					public void onNothingSelected(AdapterView<?> parentView) {}
				});

				LinearLayout cardAddress = (LinearLayout) dlgBilling.findViewById(R.id.voxmobile_card_billing_fields);
				if (!OrderHelper.isDomesticOrder(this)) {
					cardAddress.setVisibility(View.GONE);
				}

				// Billing Street
				EditText street = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_street);
				street.setText(OrderHelper.getStringValue(this, OrderHelper.BILLING_STREET), TextView.BufferType.EDITABLE);
				street.addTextChangedListener(this);

				// Billing City
				EditText city = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_city);
				city.setText(OrderHelper.getStringValue(this, OrderHelper.BILLING_CITY), TextView.BufferType.EDITABLE);
				city.addTextChangedListener(this);

				// Billing Zip
				EditText zip = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_zip);
				zip.setText(OrderHelper.getStringValue(this, OrderHelper.BILLING_POSTAL_CODE), TextView.BufferType.EDITABLE);
				zip.addTextChangedListener(this);
				
				if ((mOrderResult != null) && (!mOrderResult.mSuccess)) {
					switch (mOrderResult.getErrorCode()) {
					case OrderResult.BAD_FIRST_NAME:
						firstName.requestFocus();
						break;
					case OrderResult.BAD_LAST_NAME:
						lastName.requestFocus();
						break;
					case OrderResult.BAD_EMAIL:
						email.requestFocus();
						break;
					case OrderResult.BAD_BILLING_ADDRESS:
						street.requestFocus();
						break;
					case OrderResult.BAD_BILLING_CITY:
						city.requestFocus();
						break;
					case OrderResult.BAD_BILLING_ZIP:
						zip.requestFocus();
						break;
					case OrderResult.BAD_CC_AUTH_FAIL:
					case OrderResult.BAD_CC_NUMBER:
						ccNumber.requestFocus();
						break;
					case OrderResult.BAD_CC_INFO_MONTH:
						ccMonth.requestFocus();
						break;
					case OrderResult.BAD_CC_INFO_YEAR:
						ccYear.requestFocus();
						break;
					case OrderResult.BAD_CC_INFO_CVV:
						ccCvv.requestFocus();
						break;
					}
					
					mOrderResult = null;
				}
			} else {
							
				// Credit Card Fields
				LinearLayout card = (LinearLayout) dlgBilling.findViewById(R.id.voxmobile_card_fields);
				card.setVisibility(View.GONE);

				// Billing Card Address Fields
				LinearLayout cardAddress = (LinearLayout) dlgBilling.findViewById(R.id.voxmobile_card_billing_fields);
				cardAddress.setVisibility(View.GONE);
				
				if ((mOrderResult != null) && (!mOrderResult.mSuccess)) {
					switch (mOrderResult.getErrorCode()) {
					case OrderResult.BAD_FIRST_NAME:
						firstName.requestFocus();
						break;
					case OrderResult.BAD_LAST_NAME:
						lastName.requestFocus();
						break;
					case OrderResult.BAD_EMAIL:
						email.requestFocus();
						break;
					}
					
					mOrderResult = null;
				}
			}
			
			// set up continue button
			button = (Button) dlgBilling.findViewById(R.id.do_voxmobile_billing_info_continue);
			button.setText(R.string.voxmobile_continue);
			button.setEnabled(false);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!validateForm())
						return;

					// save first name
					EditText editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_first_name);
					OrderHelper.setStringValue(StartActivity.this, OrderHelper.FIRST_NAME, editText.getText().toString());

					// save last name
					editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_last_name);
					OrderHelper.setStringValue(StartActivity.this, OrderHelper.LAST_NAME, editText.getText().toString());

					// save email
					editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_email_address);
					OrderHelper.setStringValue(StartActivity.this, OrderHelper.EMAIL, editText.getText().toString());

					// save email confirm
					editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_email_address_confirm);
					OrderHelper.setStringValue(StartActivity.this, OrderHelper.EMAIL_CONFIRM, editText.getText().toString());

					if (mServicePlan.mTotalPrice > 0.00) {

						// save card number
						editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_card_number);
						OrderHelper.setStringValue(StartActivity.this, OrderHelper.CC_NUMBER, editText.getText().toString());

						// save card cvv
						editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_card_cvv);
						OrderHelper.setStringValue(StartActivity.this, OrderHelper.CC_CVV, editText.getText().toString());

						// save card month
						Spinner list = (Spinner) dlgBilling.findViewById(R.id.voxmobile_card_month);
						int selectedMonth = list.getSelectedItemPosition();
						OrderHelper.setIntValue(StartActivity.this, OrderHelper.CC_EXP_MONTH, selectedMonth);

						// save card year
						list = (Spinner) dlgBilling.findViewById(R.id.voxmobile_card_year);
						int selectedYear = list.getSelectedItemPosition();
						OrderHelper.setIntValue(StartActivity.this, OrderHelper.CC_EXP_YEAR, selectedYear);

						// save billing street
						editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_street);
						OrderHelper.setStringValue(StartActivity.this, OrderHelper.BILLING_STREET, editText.getText().toString());

						// save billing city
						editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_city);
						OrderHelper.setStringValue(StartActivity.this, OrderHelper.BILLING_CITY, editText.getText().toString());

						// save billing zip
						editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_zip);
						OrderHelper.setStringValue(StartActivity.this, OrderHelper.BILLING_POSTAL_CODE, editText.getText().toString());
					}

					dismissDialog(DIALOG_BILLING_INFO);
					removeDialog(DIALOG_BILLING_INFO);
					
					if (mServicePlan.mTotalPrice > 0.00) {
						if (mShowDIDOverview) {
							mShowDIDOverview = false;
							showDialog(DIALOG_DID_OVERVIEW);
						} else {
							showDIDSelection();
						}
					} else {
						showDialog(DIALOG_ORDER_SUMMARY);						
					}
				}
			});

			// set up cancel button
			button = (Button) dlgBilling.findViewById(R.id.do_voxmobile_billing_info_cancel);
			button.setText(R.string.cancel);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_BILLING_INFO);
					removeDialog(DIALOG_BILLING_INFO);
				}
			});
			
			preValidateForm();

			return dlgBilling;
			
		case DIALOG_DID_OVERVIEW:
			trackPageView("order/did_overview");

			// set up sign up overview dialog
			Dialog dlgDID = new Dialog(StartActivity.this);
			dlgDID.setContentView(R.layout.voxmobile_did_overview);
			dlgDID.setTitle(R.string.voxmobile_did_header);
			dlgDID.setCancelable(true);

			// set up text
			text = (TextView) dlgDID.findViewById(R.id.TextView01);
			text.setText(R.string.voxmobile_did_text_1);

			// set up continue button
			button = (Button) dlgDID.findViewById(R.id.do_did_continue);
			button.setText(R.string.voxmobile_continue);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_DID_OVERVIEW);
					showDIDSelection();
				}
			});

			// set up cancel button
			button = (Button) dlgDID.findViewById(R.id.do_did_cancel);
			button.setText(R.string.cancel);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_DID_OVERVIEW);
				}
			});

			return dlgDID;
			
		case DIALOG_ORDER_SUMMARY:
			trackPageView("order/summary");

			// set up sign up overview dialog
			Dialog dlgSummary = new Dialog(StartActivity.this);
			dlgSummary.setContentView(R.layout.voxmobile_order_summary);
			dlgSummary.setTitle(R.string.voxmobile_summary_header);
			dlgSummary.setCancelable(true);

			// set up text
			text = (TextView) dlgSummary.findViewById(R.id.TextView02_data);
			text.setText(mServicePlan.mPlanName);

			text = (TextView) dlgSummary.findViewById(R.id.TextView03_data);
			text.setText(String.format("%s %s", 
					OrderHelper.getStringValue(this, OrderHelper.FIRST_NAME),
					OrderHelper.getStringValue(this, OrderHelper.LAST_NAME)));

			text = (TextView) dlgSummary.findViewById(R.id.TextView04_data);
			text.setText(OrderHelper.getStringValue(this, OrderHelper.EMAIL));

			if (mServicePlan.mTotalPrice > 0.00) {
			
				text = (TextView) dlgSummary.findViewById(R.id.TextView05_data);
				text.setText(OrderHelper.getStringValue(this, OrderHelper.CC_NUMBER));

				text = (TextView) dlgSummary.findViewById(R.id.TextView06_data);
				text.setText(OrderHelper.getStringValue(this, OrderHelper.CC_CVV));

				text = (TextView) dlgSummary.findViewById(R.id.TextView07_data);
				text.setText(String.format("%02d/%04d",
						OrderHelper.getIntValue(this, OrderHelper.CC_EXP_MONTH, 0),
						OrderHelper.getSelectedYear(this)));
				
				text = (TextView) dlgSummary.findViewById(R.id.TextView08_data);
				text.setText(OrderHelper.getStringValue(this, OrderHelper.BILLING_COUNTRY));

				text = (TextView) dlgSummary.findViewById(R.id.TextView09_data);
				text.setText(String.format("%s, %s",
						OrderHelper.getStringValue(this, OrderHelper.DID_CITY),
						OrderHelper.getStringValue(this, OrderHelper.DID_STATE)));
				
				text = (TextView) dlgSummary.findViewById(R.id.TextView10_data);
				text.setText(getCharges());

			} else {
				
				// Billing Summary Fields
				LinearLayout billing = (LinearLayout) dlgSummary.findViewById(R.id.voxmobile_billing_summary);
				billing.setVisibility(View.GONE);
				
			}
			
			// set up continue button
			button = (Button) dlgSummary.findViewById(R.id.do_summary_continue);
			button.setText(R.string.voxmobile_continue);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_ORDER_SUMMARY);
					removeDialog(DIALOG_ORDER_SUMMARY);
					submitOrder();
				}
			});

			// set up cancel button
			button = (Button) dlgSummary.findViewById(R.id.do_summary_cancel);
			button.setText(R.string.cancel);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_ORDER_SUMMARY);
					removeDialog(DIALOG_ORDER_SUMMARY);
				}
			});

			return dlgSummary;

		case DIALOG_ORDER_ERROR:
			trackPageView("order/error");

			Dialog dlgOrder = new Dialog(StartActivity.this);
			dlgOrder.setContentView(R.layout.voxmobile_order_error);
			dlgOrder.setTitle(R.string.voxmobile_order_error);
			dlgOrder.setCancelable(false);

			String msg;
			
			// set up text
			text = (TextView) dlgOrder.findViewById(R.id.TextView01);
			TextView tvErr;

			switch (mOrderResult.getErrorCode()) {
				case OrderResult.BAD_FIRST_NAME:
					text.setText(R.string.voxmobile_bad_first_name);
					break;
				case OrderResult.BAD_LAST_NAME:
					text.setText(R.string.voxmobile_bad_last_name);
					break;
				case OrderResult.BAD_EMAIL:
					text.setText(R.string.voxmobile_bad_email);
					break;
				case OrderResult.BAD_BILLING_ADDRESS:
					text.setText(R.string.voxmobile_bad_billing_address);
					break;
				case OrderResult.BAD_BILLING_CITY:
					text.setText(R.string.voxmobile_bad_billing_city);
					break;
				case OrderResult.BAD_BILLING_ZIP:
					text.setText(R.string.voxmobile_bad_billing_zip);
					break;
				case OrderResult.BAD_BILLING_COUNTRY:
					text.setText(R.string.voxmobile_bad_billing_country);
					break;
				case OrderResult.BAD_CC_NUMBER:
					text.setText(R.string.voxmobile_bad_cc_number);
					break;
				case OrderResult.BAD_CC_INFO_MONTH:
					text.setText(R.string.voxmobile_bad_cc_month);
					break;
				case OrderResult.BAD_CC_INFO_YEAR:
					text.setText(R.string.voxmobile_bad_cc_year);
					break;
				case OrderResult.BAD_CC_INFO_CVV:
					text.setText(R.string.voxmobile_bad_cc_cvv);
					break;
				case OrderResult.BAD_CC_AUTH_FAIL:
					msg = getString(R.string.voxmobile_cc_auth_fail) + "\n\n" +
								  			mOrderResult.getErrorString();
					text.setText(msg);
					break;
				case OrderResult.PLAN_OVERSUBSCRIBED:
					tvErr = (TextView) dlgOrder.findViewById(R.id.RetryMessage);
					tvErr.setVisibility(View.GONE);
					msg = getString(R.string.voxmobile_oversubscribed);
					text.setText(msg);
					break;
				case OrderResult.DEPRECATED_BUILD:
					tvErr = (TextView) dlgOrder.findViewById(R.id.RetryMessage);
					tvErr.setVisibility(View.GONE);
					msg = getString(R.string.voxmobile_upgrade);
					text.setText(msg);
					break;
				case OrderResult.SYSTEM_ERROR:
					text.setText(mOrderResult.getErrorString());
					break;
			}
			
			// set up continue button
			button = (Button) dlgOrder.findViewById(R.id.do_order_error_continue);
			button.setText(R.string.voxmobile_continue);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_ORDER_ERROR);
					removeDialog(DIALOG_ORDER_ERROR);
					
					if (mOrderResult.getErrorCode() != OrderResult.PLAN_OVERSUBSCRIBED) {
						showDialog(DIALOG_BILLING_INFO);
					}
				}
			});

			return dlgOrder;
			
		case DIALOG_VALIDATION_ERROR:
			trackPageView("order/validation_error");

			AlertDialog dlgValidation = new AlertDialog.Builder(this).create();
			dlgValidation.setButton(getString(R.string.ok),	new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					removeDialog(DIALOG_VALIDATION_ERROR);
				}
			});

			dlgValidation.setTitle(R.string.voxmobile_validate_error);
			dlgValidation.setMessage(mDialogMsg);
			return dlgValidation;
			
		case DIALOG_ORDER_SUCCESS:
			trackPageView("order/success");

			Dialog dlgSuccess = new Dialog(StartActivity.this);
			dlgSuccess.setContentView(R.layout.voxmobile_order_success);
			dlgSuccess.setTitle(R.string.voxmobile_order_success_header);
			dlgSuccess.setCancelable(false);

			// set up text
			text = (TextView) dlgSuccess.findViewById(R.id.TextView02);
			if (mServicePlan.mTotalPrice > 0.00) {
				text.setText(getString(R.string.voxmobile_order_success_charge) + " $" + Double.valueOf(mOrderResult.mChargeAmount).toString());
			} else {
				text.setVisibility(View.GONE);
			}
			
			// set up text
			text = (TextView) dlgSuccess.findViewById(R.id.TextView03);
			text.setText(String.format("%s: %s\n%s: %s",
					getString(R.string.voxmobile_login_username),
					mOrderResult.mLoginName,
					getString(R.string.voxmobile_login_password),
					mOrderResult.mLoginPassword));

			// set up continue button
			button = (Button) dlgSuccess.findViewById(R.id.do_order_success_continue);
			button.setText(R.string.voxmobile_continue);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DIALOG_ORDER_SUCCESS);
					removeDialog(DIALOG_ORDER_SUCCESS);
					doFinish();
				}
			});
			
			return dlgSuccess;

		case DIALOG_UNAUTHORIZED:
			trackPageView("order/unauthorized");

			AlertDialog dlg = new AlertDialog.Builder(this).create();
			dlg.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});

			dlg.setTitle(R.string.voxmobile_unauthorized);
			dlg.setMessage(getString(R.string.voxmobile_unauthorized_msg));
			return dlg;

		case DIALOG_GENERAL_ERROR:
			trackPageView("order/general_error");

			AlertDialog errDlg = new AlertDialog.Builder(this).create();
			errDlg.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});

			errDlg.setTitle(R.string.voxmobile_server_error);
			errDlg.setMessage(mDialogMsg);
			return errDlg;
		}

		return null;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ServiceHelper.CHOOSE_PLAN:
			if (resultCode == RESULT_OK) {
				mServicePlan = null;
				
				if (data != null) {
					int planIndex = data.getIntExtra("plan_index", -1);

					if (planIndex >= 0) {
						mServicePlan = mServicePlans.get(planIndex);
						OrderHelper.setStringValue(this, OrderHelper.PLAN_ID, mServicePlan.mPlanId);
					}
					
					if (mServicePlan.mTotalPrice > 0.00) {
						if (OrderHelper.getStringValue(this, OrderHelper.DID_STATE) == ServiceHelper.VOXLAND_STATE) {
							OrderHelper.setStringValue(this, OrderHelper.DID_STATE, "");
							OrderHelper.setStringValue(this, OrderHelper.DID_STATE_NAME, "");
							OrderHelper.setStringValue(this, OrderHelper.DID_CITY, "");
						}
						
						showDialog(DIALOG_CHOOSE_COUNTRY);
					} else {
						showDialog(DIALOG_BILLING_INFO);

						OrderHelper.setStringValue(this, OrderHelper.DID_STATE, ServiceHelper.VOXLAND_STATE);
						OrderHelper.setStringValue(this, OrderHelper.DID_STATE_NAME, ServiceHelper.VOXLAND_STATE_NAME);
						OrderHelper.setStringValue(this, OrderHelper.DID_CITY, ServiceHelper.VOXLAND_CITY);
					}
				}
			}
			break;
		case ServiceHelper.SELECT_RATE_CENTER:
			if (resultCode == RESULT_OK) {
				if (data != null) {
					String didState = data.getStringExtra(ServiceHelper.DID_STATE);
					OrderHelper.setStringValue(this, OrderHelper.DID_STATE, didState);

					String didStateName = data.getStringExtra(ServiceHelper.DID_STATE_NAME);
					OrderHelper.setStringValue(this, OrderHelper.DID_STATE_NAME, didStateName);
					
					String didCity = data.getStringExtra(ServiceHelper.DID_CITY);
					OrderHelper.setStringValue(this, OrderHelper.DID_CITY, didCity);
					
					showDialog(DIALOG_ORDER_SUMMARY);
				}
			}
			break;
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		preValidateForm();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	private void preValidateForm() {
		boolean enableButton = false;

		EditText editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_first_name);
		String firstName = editText.getText().toString();

		editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_last_name);
		String lastName = editText.getText().toString();

		editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_email_address);
		String email = editText.getText().toString();

		editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_email_address_confirm);
		String emailConfirm = editText.getText().toString();

		editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_card_number);
		String cardNumber = editText.getText().toString();

		editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_card_cvv);
		String cardCVV = editText.getText().toString();

		Spinner list = (Spinner) dlgBilling.findViewById(R.id.voxmobile_card_month);
		int cardMonth = list.getSelectedItemPosition();

		list = (Spinner) dlgBilling.findViewById(R.id.voxmobile_card_year);
		int cardYear = list.getSelectedItemPosition();

		editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_street);
		String street = editText.getText().toString();

		editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_city);
		String city = editText.getText().toString();

		editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_zip);
		String zip = editText.getText().toString();

		if (mServicePlan.mTotalPrice > 0.00) {
			// baseline checks for all domestic and international orders
			enableButton = (firstName.trim().length() > 0)
					&& (lastName.trim().length() > 0)
					&& (email.trim().length() > 0)
					&& (emailConfirm.trim().length() > 0)
					&& (cardNumber.trim().length() > 0)
					&& (cardCVV.trim().length() > 0) && (cardMonth > 0)
					&& (cardYear >= 0);

			// for domestic orders we also need to validate billing
			// address for credit card AVS checks
			if (OrderHelper.isDomesticOrder(this)) {
				enableButton = enableButton && 
								(street.trim().length() > 0) && 
								(city.trim().length() > 0) && 
								(zip.trim().length() > 0);
			}

		} else {
			
			// baseline checks for all domestic and international orders
			enableButton = (firstName.trim().length() > 0)
					&& (lastName.trim().length() > 0)
					&& (email.trim().length() > 0)
					&& (emailConfirm.trim().length() > 0);
		}
		
		Button button = (Button) dlgBilling.findViewById(R.id.do_voxmobile_billing_info_continue);
		button.setEnabled(enableButton);
	}

	private boolean validateForm() {

		Pattern pattern;

		EditText editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_email_address);
		String email = editText.getText().toString().trim();

		editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_email_address_confirm);
		String emailConfirm = editText.getText().toString().trim();

		if (!email.equalsIgnoreCase(emailConfirm)) {
			editText.requestFocus();
			mDialogMsg = getString(R.string.voxmobile_email_mismatch);
			showDialog(DIALOG_VALIDATION_ERROR);
			return false;
		}

		if (mServicePlan.mTotalPrice > 0.00) {
			editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_card_number);
			String cardNumber = editText.getText().toString().trim();
			pattern = Pattern.compile("([0-9]{10})([0-9]+)");
			if (!pattern.matcher(cardNumber).matches()) {
				editText.requestFocus();
				mDialogMsg = getString(R.string.voxmobile_invalid_card_number);
				showDialog(DIALOG_VALIDATION_ERROR);
				return false;
			}

			editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_card_cvv);
			String cardCVV = editText.getText().toString().trim();
			pattern = Pattern.compile("[0-9]{3}|[0-9]{4}");
			if (!pattern.matcher(cardCVV).matches()) {
				editText.requestFocus();
				mDialogMsg = getString(R.string.voxmobile_invalid_card_cvv);
				showDialog(DIALOG_VALIDATION_ERROR);
				return false;
			}

			Spinner list = (Spinner) dlgBilling.findViewById(R.id.voxmobile_card_month);
			if (list.getSelectedItemPosition() == 0) {
				editText.requestFocus();
				mDialogMsg = getString(R.string.voxmobile_invalid_card_month);
				showDialog(DIALOG_VALIDATION_ERROR);
				return false;
			}

			// for domestic orders we also need to validate billing
			// address for credit card AVS checks
			if (OrderHelper.isDomesticOrder(this)) {
				editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_street);
				String street = editText.getText().toString().trim();
				if (street.length() < 5) {
					editText.requestFocus();
					mDialogMsg = getString(R.string.voxmobile_invalid_steet);
					showDialog(DIALOG_VALIDATION_ERROR);
					return false;
				}

				editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_city);
				String city = editText.getText().toString().trim();
				if (city.length() < 3) {
					editText.requestFocus();
					mDialogMsg = getString(R.string.voxmobile_invalid_city);
					showDialog(DIALOG_VALIDATION_ERROR);
					return false;
				}

				editText = (EditText) dlgBilling.findViewById(R.id.voxmobile_billing_zip);
				String zip = editText.getText().toString().trim();
				if (OrderHelper.isDomesticOrder(this)) {

					if (OrderHelper.isCanadianOrder(this)) {
						pattern = Pattern.compile("[A-Z][0-9][A-Z][ ][0-9][A-Z][0-9]");
					} else {
						pattern = Pattern.compile("[0-9]{5}");
					}

					if (!pattern.matcher(zip).matches()) {
						editText.requestFocus();
						mDialogMsg = getString(R.string.voxmobile_invalid_zip);
						showDialog(DIALOG_VALIDATION_ERROR);
						return false;
					}
				}
			}
		}
		
		return true;
	}

}
