package net.voxcorp.voxmobile.ui.order;

import net.voxcorp.R;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.ui.TrackedActivity;
import net.voxcorp.voxmobile.utils.OrderHelper;

public class SelectCountryActivity extends TrackedActivity implements OnClickListener {
	
	private static final String THIS_FILE = "SelectCountryActivity";
	
	private static String[] mCountries = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "SelectCountryActivity.onCreate()");
		
		// Build window
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voxmobile_country_chooser);
		setTitle(R.string.voxmobile_country_msg);
		
		if (mCountries == null)
			mCountries = getResources().getStringArray(R.array.voxmobile_country_array);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.voxmobile_simple_list_item, mCountries);
		Spinner countrySpinner = (Spinner)findViewById(R.id.Spinner01);
		countrySpinner.setAdapter(adapter);
		countrySpinner.setSelection(OrderHelper.getIntValue(this,
				OrderHelper.BILLING_COUNTRY_INDEX, 0));
		
		countrySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				Button button = (Button)findViewById(R.id.Button02);
				button.setEnabled(position > 0);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parentView) {}
		});
		
		// set up continue button
		Button button = (Button)findViewById(R.id.Button02);
		button.setText(R.string.voxmobile_continue);
		button.setEnabled(false);
		button.setOnClickListener(this);
		
		// set up cancel button
		button = (Button)findViewById(R.id.Button01);
		button.setText(R.string.voxmobile_back);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		trackPageView("order/choose_country");
	}

	@Override
	public void onClick(View v) {
		Spinner list = (Spinner)findViewById(R.id.Spinner01);
		int selectedCountry = list.getSelectedItemPosition();

		String[] mCountryCodes = getResources().getStringArray(R.array.voxmobile_country_code_array);
		OrderHelper.setIntValue(SelectCountryActivity.this, OrderHelper.BILLING_COUNTRY_INDEX, selectedCountry);
		OrderHelper.setStringValue(SelectCountryActivity.this, OrderHelper.BILLING_COUNTRY, mCountryCodes[(int) selectedCountry]);

		setResult(RESULT_OK);
		finish();
	}
}
