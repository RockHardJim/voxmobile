package net.voxcorp.voxmobile.ui.order;

import net.voxcorp.R;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.provider.DBContract.OrderResultContract;
import net.voxcorp.voxmobile.ui.TrackedActivity;
import net.voxcorp.voxmobile.utils.OrderHelper;

public class OrderSuccessActivity extends TrackedActivity {
	private static final String THIS_FILE = "OrderSuccessActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "OrderSuccessActivity.onCreate()");

		// Build window
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voxmobile_order_success);
		setTitle(R.string.voxmobile_order_success_header);

		String chargeAmount = getIntent().getStringExtra(OrderResultContract.CC_CHARGE_AMOUNT);
		String loginName = getIntent().getStringExtra(OrderResultContract.LOGIN_NAME);
		String loginPassword = getIntent().getStringExtra(OrderResultContract.LOGIN_PASSWORD);

		// set up text
		TextView text = (TextView) findViewById(R.id.TextView01);
		if (!OrderHelper.getBooleanValue(this, OrderHelper.IS_FREE, false)) {
			text.setText(getString(R.string.voxmobile_order_success_charge) + " $" + chargeAmount);
		} else {
			text.setVisibility(View.GONE);
		}

		// set up text
		text = (TextView) findViewById(R.id.TextView02);
		text.setTextColor(Color.YELLOW);
		text.setText(String.format("%s: %s\n%s: %s",
				getString(R.string.voxmobile_login_username),
				loginName,
				getString(R.string.voxmobile_login_password),
				loginPassword));

		// set up continue button
		Button button = (Button) findViewById(R.id.Button01);
		button.setText(R.string.voxmobile_continue);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		trackPageView("order/success");
	}
}
