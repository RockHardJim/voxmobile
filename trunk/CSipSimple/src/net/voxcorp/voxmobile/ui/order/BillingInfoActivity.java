package net.voxcorp.voxmobile.ui.order;

import java.util.regex.Pattern;

import net.voxcorp.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.ui.TrackedActivity;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.OrderHelper;

public class BillingInfoActivity extends TrackedActivity implements TextWatcher {

	private static final String THIS_FILE = "BillingInfoActivity";

	private static final int VALIDATION_ERROR = -1;
		
	private static String[] mMonths = null;

	/* dialog items, PITA because API level 7 is needed but doesn't 
	 * support showDialog(int, Bundle) which could have been used
	 * to pass dialog strings.
	 */
	private String mDialogMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "BillingInfoActivity.onCreate()");
		
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voxmobile_billing_info);
		setTitle(R.string.voxmobile_billing_title);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_voxmobile_info);
		
		// set up cancel button
		Button button = (Button) findViewById(R.id.Button01);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		// set up continue button
		button = (Button) findViewById(R.id.Button02);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (saveForm()) {
					if (!OrderHelper.getBooleanValue(BillingInfoActivity.this, OrderHelper.IS_FREE, false)) {
						showDIDSelection();
					} else {
                        OrderHelper.setStringValue(BillingInfoActivity.this, OrderHelper.DID_STATE, Consts.VOXLAND_STATE);
                        OrderHelper.setStringValue(BillingInfoActivity.this, OrderHelper.DID_STATE_NAME, Consts.VOXLAND_STATE_NAME);
                        OrderHelper.setStringValue(BillingInfoActivity.this, OrderHelper.DID_CITY, Consts.VOXLAND_CITY);

                        startActivity(new Intent(BillingInfoActivity.this, OrderSummaryActivity.class));
						finish();
					}
				}
			}
		});

		initForm();
		preValidateForm();
		
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey(OrderSummaryActivity.ORDER_EDIT)) {
			editItem(extras.getInt(OrderSummaryActivity.ORDER_EDIT));
		}

		trackPageView("order/billing_info");
	}
	
	private void editItem(int item) {
		View view = null;
		
		switch (item) {
		case OrderSummaryActivity.EDIT_FIRST_NAME:
			view = (View)findViewById(R.id.voxmobile_first_name);
			break;
		case OrderSummaryActivity.EDIT_LAST_NAME:
			view = (View)findViewById(R.id.voxmobile_last_name);
			break;
		case OrderSummaryActivity.EDIT_EMAIL:
			view = (View)findViewById(R.id.voxmobile_email_address);
			break;
		case OrderSummaryActivity.EDIT_CC_NUMBER:
			view = (View)findViewById(R.id.voxmobile_card_number);
			break;
		case OrderSummaryActivity.EDIT_CC_CVV:
			view = (View)findViewById(R.id.voxmobile_card_cvv);
			break;
		case OrderSummaryActivity.EDIT_CC_EXP_MONTH:
			// focus card number because android Spinners act
			// really odd when it comes to being focusable
			view = (View)findViewById(R.id.voxmobile_card_number);
			break;
		case OrderSummaryActivity.EDIT_STREET:
			view = (View)findViewById(R.id.voxmobile_billing_street);
			break;
		case OrderSummaryActivity.EDIT_CITY:
			view = (View)findViewById(R.id.voxmobile_billing_city);
			break;
		case OrderSummaryActivity.EDIT_ZIP:
			view = (View)findViewById(R.id.voxmobile_billing_zip);
			break;
		}
		view.requestFocus();
	}

	private boolean saveForm() {
		if (!validateForm()) return false;
		
		// save first name
		EditText editText = (EditText)findViewById(R.id.voxmobile_first_name);
		OrderHelper.setStringValue(this, OrderHelper.FIRST_NAME, editText.getText().toString());
		
		// save last name
		editText = (EditText)findViewById(R.id.voxmobile_last_name);
		OrderHelper.setStringValue(this, OrderHelper.LAST_NAME, editText.getText().toString());
		
		// save email
		editText = (EditText)findViewById(R.id.voxmobile_email_address);
		OrderHelper.setStringValue(this, OrderHelper.EMAIL, editText.getText().toString());
		
		// save email confirm
		editText = (EditText)findViewById(R.id.voxmobile_email_address_confirm);
		OrderHelper.setStringValue(this, OrderHelper.EMAIL_CONFIRM, editText.getText().toString());
		if (!OrderHelper.getBooleanValue(this, OrderHelper.IS_FREE, false)) {
			// save card number
			editText = (EditText)findViewById(R.id.voxmobile_card_number);
			OrderHelper.setStringValue(this, OrderHelper.CC_NUMBER, editText.getText().toString());
			
			// save card cvv
			editText = (EditText)findViewById(R.id.voxmobile_card_cvv);
			OrderHelper.setStringValue(this, OrderHelper.CC_CVV, editText.getText().toString());
			
			// save card month
			Spinner list = (Spinner)findViewById(R.id.voxmobile_card_month);
			int selectedMonth = list.getSelectedItemPosition();
			OrderHelper.setIntValue(this, OrderHelper.CC_EXP_MONTH, selectedMonth);
			
			// save card year
			list = (Spinner)findViewById(R.id.voxmobile_card_year);
			int selectedYear = list.getSelectedItemPosition();
			OrderHelper.setIntValue(this, OrderHelper.CC_EXP_YEAR, selectedYear);
			
			// save billing street
			editText = (EditText)findViewById(R.id.voxmobile_billing_street);
			OrderHelper.setStringValue(this, OrderHelper.BILLING_STREET, editText.getText().toString());
			
			// save billing city
			editText = (EditText)findViewById(R.id.voxmobile_billing_city);
			OrderHelper.setStringValue(this, OrderHelper.BILLING_CITY, editText.getText().toString());
			
			// save billing zip
			editText = (EditText)findViewById(R.id.voxmobile_billing_zip);
			OrderHelper.setStringValue(this, OrderHelper.BILLING_POSTAL_CODE, editText.getText().toString());
		}
		
		return true;
	}

	private final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
			"[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
			"\\@" +
			"[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
			"(" +
			"\\." +
			"[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
			")+",
			Pattern.CASE_INSENSITIVE);

	private boolean validateForm() {
		Pattern pattern;
		
		EditText editText = (EditText)findViewById(R.id.voxmobile_email_address);
		String email = editText.getText().toString().trim();
		
		if (email.length() == 0 || !EMAIL_ADDRESS_PATTERN.matcher(email).matches()) {
			mDialogMessage = getString(R.string.voxmobile_invalid_email);
			showDialog(VALIDATION_ERROR);
			return false;
		}

		editText = (EditText)findViewById(R.id.voxmobile_email_address_confirm);
		String emailConfirm = editText.getText().toString().trim();
		
		if (!email.equalsIgnoreCase(emailConfirm)) {
			editText.requestFocus();
			mDialogMessage = getString(R.string.voxmobile_email_mismatch);
			showDialog(VALIDATION_ERROR);
			return false;
		}
		
		if (!OrderHelper.getBooleanValue(this, OrderHelper.IS_FREE, false)) {
			editText = (EditText)findViewById(R.id.voxmobile_card_number);
			String cardNumber = editText.getText().toString().trim();
			pattern = Pattern.compile("([0-9]{10})([0-9]+)");
			if (!pattern.matcher(cardNumber).matches()) {
				editText.requestFocus();
				mDialogMessage = getString(R.string.voxmobile_invalid_card_number);
				showDialog(VALIDATION_ERROR);
				return false;
			}
			
			editText = (EditText)findViewById(R.id.voxmobile_card_cvv);
			String cardCVV = editText.getText().toString().trim();
			pattern = Pattern.compile("[0-9]{3}|[0-9]{4}");
			if (!pattern.matcher(cardCVV).matches()) {
				editText.requestFocus();
				mDialogMessage = getString(R.string.voxmobile_invalid_card_cvv);
				showDialog(VALIDATION_ERROR);
				return false;
			}
			
			Spinner list = (Spinner)findViewById(R.id.voxmobile_card_month);
			if (list.getSelectedItemPosition() == 0) {
				editText.requestFocus();
				mDialogMessage = getString(R.string.voxmobile_invalid_card_month);
				showDialog(VALIDATION_ERROR);
				return false;
			}
			
			// for domestic orders we also need to validate billing
			// address for credit card AVS checks
			if (OrderHelper.isDomesticOrder(this)) {
				editText = (EditText)findViewById(R.id.voxmobile_billing_street);
				String street = editText.getText().toString().trim();
				if (street.length() < 5) {
					editText.requestFocus();
					mDialogMessage= getString(R.string.voxmobile_invalid_steet);
					showDialog(VALIDATION_ERROR);
					return false;
				}
				
				editText = (EditText)findViewById(R.id.voxmobile_billing_city);
				String city = editText.getText().toString().trim();
				if (city.length() < 3) {
					editText.requestFocus();
					mDialogMessage = getString(R.string.voxmobile_invalid_city);
					showDialog(VALIDATION_ERROR);
					return false;
				}
				
				editText = (EditText)findViewById(R.id.voxmobile_billing_zip);
				String zip = editText.getText().toString().trim();
				if (OrderHelper.isDomesticOrder(this)) {
					
					if (OrderHelper.isCanadianOrder(this)) {
						pattern = Pattern.compile("[A-Z][0-9][A-Z][ ][0-9][A-Z][0-9]");
					} else {
						pattern = Pattern.compile("[0-9]{5}");
					}
					
					if (!pattern.matcher(zip).matches()) {
						editText.requestFocus();
						mDialogMessage = getString(R.string.voxmobile_invalid_zip);
						showDialog(VALIDATION_ERROR);
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private void initForm() {
		// First Name
		EditText firstName = (EditText)findViewById(R.id.voxmobile_first_name);
		firstName.setText(OrderHelper.getStringValue(this, OrderHelper.FIRST_NAME), TextView.BufferType.EDITABLE);
		firstName.addTextChangedListener(this);

		// Last Name
		EditText lastName = (EditText)findViewById(R.id.voxmobile_last_name);
		lastName.setText(OrderHelper.getStringValue(this, OrderHelper.LAST_NAME), TextView.BufferType.EDITABLE);
		lastName.addTextChangedListener(this);

		// Email
		EditText email = (EditText)findViewById(R.id.voxmobile_email_address);
		email.setText(OrderHelper.getStringValue(this, OrderHelper.EMAIL), TextView.BufferType.EDITABLE);
		email.addTextChangedListener(this);

		// Email Confirm
		EditText confirmEmail = (EditText)findViewById(R.id.voxmobile_email_address_confirm);
		confirmEmail.setText(OrderHelper.getStringValue(this, OrderHelper.EMAIL_CONFIRM), TextView.BufferType.EDITABLE);
		confirmEmail.addTextChangedListener(this);

		if (!OrderHelper.getBooleanValue(this, OrderHelper.IS_FREE, false)) {

			// Card Number
			EditText ccNumber = (EditText)findViewById(R.id.voxmobile_card_number);
			ccNumber.setText(OrderHelper.getStringValue(this, OrderHelper.CC_NUMBER), TextView.BufferType.EDITABLE);
			ccNumber.addTextChangedListener(this);

			// Card CVV
			EditText ccCvv = (EditText)findViewById(R.id.voxmobile_card_cvv);
			ccCvv.setText(OrderHelper.getStringValue(this, OrderHelper.CC_CVV), TextView.BufferType.EDITABLE);
			ccCvv.addTextChangedListener(this);

			// Card Month
			if (mMonths == null)
				mMonths = getResources().getStringArray(R.array.voxmobile_months_array);

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.voxmobile_simple_list_item, mMonths);
			Spinner ccMonth = (Spinner)findViewById(R.id.voxmobile_card_month);
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
			adapter = new ArrayAdapter<String>(this, R.layout.voxmobile_simple_list_item, OrderHelper.getCardYears());
			Spinner ccYear = (Spinner)findViewById(R.id.voxmobile_card_year);
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

			LinearLayout cardAddress = (LinearLayout)findViewById(R.id.voxmobile_card_billing_fields);
			if (!OrderHelper.isDomesticOrder(this)) {
				cardAddress.setVisibility(View.GONE);
			}

			// Billing Street
			EditText street = (EditText)findViewById(R.id.voxmobile_billing_street);
			street.setText(OrderHelper.getStringValue(this, OrderHelper.BILLING_STREET), TextView.BufferType.EDITABLE);
			street.addTextChangedListener(this);

			// Billing City
			EditText city = (EditText)findViewById(R.id.voxmobile_billing_city);
			city.setText(OrderHelper.getStringValue(this, OrderHelper.BILLING_CITY), TextView.BufferType.EDITABLE);
			city.addTextChangedListener(this);

			// Billing Zip
			EditText zip = (EditText)findViewById(R.id.voxmobile_billing_zip);
			zip.setText(OrderHelper.getStringValue(this, OrderHelper.BILLING_POSTAL_CODE), TextView.BufferType.EDITABLE);
			zip.addTextChangedListener(this);
		} else {
			// Credit Card Fields
			LinearLayout card = (LinearLayout)findViewById(R.id.voxmobile_card_fields);
			card.setVisibility(View.GONE);
			
			// Billing Card Address Fields
			LinearLayout cardAddress = (LinearLayout)findViewById(R.id.voxmobile_card_billing_fields);
			cardAddress.setVisibility(View.GONE);
		}
	}

	private void preValidateForm() {
		boolean enableButton = false;
		
		EditText editText = (EditText)findViewById(R.id.voxmobile_first_name);
		String firstName = editText.getText().toString();
		
		editText = (EditText)findViewById(R.id.voxmobile_last_name);
		String lastName = editText.getText().toString();
		
		editText = (EditText)findViewById(R.id.voxmobile_email_address);
		String email = editText.getText().toString();
		
		editText = (EditText)findViewById(R.id.voxmobile_email_address_confirm);
		String emailConfirm = editText.getText().toString();
		
		editText = (EditText)findViewById(R.id.voxmobile_card_number);
		String cardNumber = editText.getText().toString();
		
		editText = (EditText)findViewById(R.id.voxmobile_card_cvv);
		String cardCVV = editText.getText().toString();
		
		Spinner list = (Spinner)findViewById(R.id.voxmobile_card_month);
		int cardMonth = list.getSelectedItemPosition();
		
		list = (Spinner)findViewById(R.id.voxmobile_card_year);
		int cardYear = list.getSelectedItemPosition();
		
		editText = (EditText)findViewById(R.id.voxmobile_billing_street);
		String street = editText.getText().toString();
		
		editText = (EditText)findViewById(R.id.voxmobile_billing_city);
		String city = editText.getText().toString();
		
		editText = (EditText)findViewById(R.id.voxmobile_billing_zip);
		String zip = editText.getText().toString();
		
		if (!OrderHelper.getBooleanValue(this, OrderHelper.IS_FREE, false)) {
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
		
		Button button = (Button)findViewById(R.id.Button02);
		button.setEnabled(enableButton);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		
		switch (id) {
		case VALIDATION_ERROR:
			AlertDialog alertDlg = new AlertDialog.Builder(this).create();
			alertDlg.setTitle(R.string.voxmobile_validate_error);
			alertDlg.setMessage(mDialogMessage);
			alertDlg.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					removeDialog(VALIDATION_ERROR);
				}
			});
			return alertDlg;
		}
		return null;
	}

	@Override
	public void afterTextChanged(Editable s) {
		preValidateForm();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
	}
	
	private void showDIDSelection() {
		Intent intent = new Intent(this, DIDSelectionActivity.class);
		startActivity(intent);
		finish();
	}
}
