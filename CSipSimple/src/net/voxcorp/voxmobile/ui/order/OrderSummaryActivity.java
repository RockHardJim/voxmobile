package net.voxcorp.voxmobile.ui.order;

import java.util.ArrayList;
import java.util.List;

import net.voxcorp.R;

import org.springframework.http.HttpStatus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;
import net.voxcorp.voxmobile.provider.DBContract.OrderResultContract;
import net.voxcorp.voxmobile.provider.DBContract.PlanChargeContract;
import net.voxcorp.voxmobile.provider.DBContract.PlanContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.types.OrderResult;
import net.voxcorp.voxmobile.types.OrderResultError;
import net.voxcorp.voxmobile.ui.TrackedListActivity;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.OrderHelper;

public class OrderSummaryActivity extends TrackedListActivity {

	public static final String ORDER_EDIT = "order_edit";
	public static final int EDIT_NULL = 0;
	public static final int EDIT_FIRST_NAME = 1;
	public static final int EDIT_LAST_NAME = 2;
	public static final int EDIT_EMAIL = 3;
	public static final int EDIT_CC_NUMBER = 4;
	public static final int EDIT_CC_CVV = 5;
	public static final int EDIT_CC_EXP_MONTH = 6;
	public static final int EDIT_STREET = 7;
	public static final int EDIT_CITY = 8;
	public static final int EDIT_ZIP = 9;

	private static final int OVERSUBSCRIBED = -1;
	private static final int CC_AUTH_FAIL = -2;
	private static final int BILLING_ADDRESS1 = -3;
	private static final int CITY = -4;
	private static final int COUNTRY = -5;
	private static final int POSTAL_CODE = -6;
	private static final int CC_CVV = -7;
	private static final int CC_MONTH = -8;
	private static final int CC_YEAR = -9;
	private static final int CC_NUMBER = -10;
	private static final int EMAIL = -11;
	private static final int FIRST_NAME = -12;
	private static final int LAST_NAME = -13;
	private static final int OTHER = -14;

	private static boolean mOrderCanceled = false;

	enum DataType {
		TEXT, ITEM, CHARGE
	};

	private OrderResult mOrderResult;

	class SummaryItem {
		DataType type;
		String name;
		String value;
		@SuppressWarnings("rawtypes")
		Class editAction;
		int editField;
		boolean finishOnEdit;
		int textColor;

		@SuppressWarnings("rawtypes")
		public SummaryItem(DataType type, String name, String value,
				Class editAction, int editField, boolean finishOnEdit,
				int textColor) {
			super();
			this.type = type;
			this.name = name;
			this.value = value;
			this.editAction = editAction;
			this.editField = editField;
			this.finishOnEdit = finishOnEdit;
			this.textColor = textColor;
		}
	}

	class ChargeItem {
		String name;
		String value;

		public ChargeItem(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}
	}

	private static final int VIEW_TYPE_TEXT = 0;
	private static final int VIEW_TYPE_CHARGE = 1;
	private static final int VIEW_TYPE_ITEM = 2;

	private ArrayList<SummaryItem> mSummaryItems;

	private static final String THIS_FILE = "OrderSummaryActivity";
	private String mPlanName = null;
	private String mTotalPrice = null;
	SummaryAdapter mAdapter;
	private ArrayList<ChargeItem> mCharges;

	private static VoXObserver mVoXObserver;
	private ProgressDialog mProgressDialog = null;

	class SummaryAdapter extends ArrayAdapter<SummaryItem> {

		public SummaryAdapter(Context context, int resource, List<SummaryItem> objects) {
			super(context, resource, resource, objects);
		}

		@Override
		public int getViewTypeCount() {
			return 3;
		}

		@Override
		public int getItemViewType(int position) {
			SummaryItem item = mSummaryItems.get(position);

			if (item.type == DataType.TEXT)
				return VIEW_TYPE_TEXT;
			else if (item.type == DataType.ITEM)
				return VIEW_TYPE_ITEM;
			else
				return VIEW_TYPE_CHARGE;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			int viewType = getItemViewType(position);
			LinearLayout view = (convertView != null) ? (LinearLayout) convertView : createView(parent, viewType);
			SummaryItem item = mSummaryItems.get(position);

			TextView nameView;
			TextView valueView;

			switch (viewType) {
			case VIEW_TYPE_TEXT:
				nameView = (TextView) view.findViewById(R.id.TextView01);
				nameView.setText(item.name);

				if (item.textColor != -1) {
					nameView.setTextColor(item.textColor);
				}
				break;
			case VIEW_TYPE_CHARGE:
				nameView = (TextView) view.findViewById(R.id.TextView01);
				nameView.setText(item.name);

				valueView = (TextView) view.findViewById(R.id.TextView02);
				valueView.setText(item.value);

				if (item.textColor != -1) {
					nameView.setTextColor(item.textColor);
					valueView.setTextColor(item.textColor);
				}
				break;
			default:
				nameView = (TextView) view.findViewById(R.id.TextView01);
				nameView.setText(item.name);

				valueView = (TextView) view.findViewById(R.id.TextView02);
				valueView.setText(item.value);

				if (item.textColor != -1) {
					nameView.setTextColor(item.textColor);
					valueView.setTextColor(item.textColor);
				}
			}

			return view;
		}

		private LinearLayout createView(ViewGroup parent, int viewType) {
			switch (viewType) {
			case VIEW_TYPE_TEXT:
				return (LinearLayout) getLayoutInflater().inflate(
						R.layout.voxmobile_order_summary_text_item, parent,
						false);
			case VIEW_TYPE_CHARGE:
				return (LinearLayout) getLayoutInflater().inflate(
						R.layout.voxmobile_order_summary_charge_item, parent,
						false);
			default:
				return (LinearLayout) getLayoutInflater().inflate(
						R.layout.voxmobile_order_summary_list_item, parent,
						false);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "OrderSummaryActivity.onCreate()");

		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voxmobile_order_summary);
		setTitle(R.string.voxmobile_summary_header);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_voxmobile_info);

		trackPageView("order/summary");

		mSummaryItems = new ArrayList<SummaryItem>();
		mCharges = new ArrayList<ChargeItem>();

		mAdapter = new SummaryAdapter(this, R.layout.voxmobile_order_summary_list_item, mSummaryItems);
		initSummary();

		getListView().setDivider(null);
		getListView().setDividerHeight(0);
		setListAdapter(mAdapter);

		// set up continue button
		Button button = (Button) findViewById(R.id.Button02);
		button.setText(R.string.voxmobile_continue);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mOrderCanceled = false;
				
				// prevent double-clicking
				Button button = (Button) findViewById(R.id.Button02);
				button.setEnabled(false);

				VoXObserverState.mSyncType = VoXObserverState.SyncType.ORDER;
				getContentResolver().update(OrderResultContract.CONTENT_URI, null, null, null);
			}
		});

		// set up cancel button
		button = (Button) findViewById(R.id.Button01);
		button.setText(R.string.cancel);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mOrderCanceled = true;
				finish();
			}
		});
	}

	private void registerContentObservers() {
		ContentResolver cr = getContentResolver();
		mVoXObserver = new VoXObserver(new Handler());
		cr.registerContentObserver(RequestContract.CONTENT_URI, true, mVoXObserver);
		cr.registerContentObserver(OrderResultContract.CONTENT_URI, true, mVoXObserver);
	}

	private void unregisterContentObservers() {
		ContentResolver cr = getContentResolver();
		if (mVoXObserver != null) {
			cr.unregisterContentObserver(mVoXObserver);
			mVoXObserver = null;
		}
	}

	private void showProgressDialog() {
		if (isFinishing() || mProgressDialog != null)
			return;
		mProgressDialog = ProgressDialog.show(this, "", getString(R.string.voxmobile_please_wait), true, true);
	}

	private void dismissProgressDialog() {
		if (mProgressDialog == null)
			return;
		mProgressDialog.dismiss();
		mProgressDialog = null;
	}

	private static class VoXObserverState {
		private enum SyncType {
			PLAN, ORDER
		};

		private static boolean mSuccess = false;
		private static int mSyncStatus = SyncStatus.STALE;
		private static int mHttpCode = 0;
		private static String mError = "";
		private static SyncType mSyncType = SyncType.PLAN;

		private static void reset() {
			mSuccess = false;
			mSyncStatus = SyncStatus.STALE;
			mHttpCode = 0;
			mError = "";
			mSyncType = SyncType.PLAN;
		}
	}

	private class VoXObserver extends ContentObserver {

		public VoXObserver(Handler h) {
			super(h);
			VoXObserverState.reset();
		}

    	private void setError(int httpCode, String errorMsg) {
			VoXObserverState.mHttpCode = httpCode;

			if (httpCode == HttpStatus.OK.value()) {
				VoXObserverState.mError = "";
			} else if (httpCode != 0) {
				VoXObserverState.mError = "" + httpCode + ": " + HttpStatus.valueOf(httpCode).getReasonPhrase();
			} else {
				VoXObserverState.mError = errorMsg;
			}
    	}

    	@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			Cursor c = managedQuery(RequestContract.CONTENT_URI, RequestContract.PROJECTION, null, null, null);
			int count = c.getCount();
			if (count == 0) {
				return;
			}

			if (c.moveToFirst()) {
				VoXObserverState.mSyncStatus = c.getInt(RequestContract.UPDATED_INDEX);
				VoXObserverState.mSuccess = c.getInt(RequestContract.SUCCESS_INDEX) == DBBoolean.TRUE;
				setError(c.getInt(RequestContract.HTTP_STATUS_INDEX), c.getString(RequestContract.ERROR_INDEX));
			} else {
				VoXObserverState.mSyncStatus = SyncStatus.STALE;
			}

			switch (VoXObserverState.mSyncStatus) {
			case SyncStatus.UPDATING:
				showProgressDialog();
				break;
			case SyncStatus.CURRENT:
				dismissProgressDialog();
				
				if (mOrderCanceled) return;

				// re-enable button, disabled to prevent double-clicking
				Button button = (Button) findViewById(R.id.Button02);

				if (VoXObserverState.mHttpCode == HttpStatus.UNAUTHORIZED.value()) {
					showDialog(Consts.REST_UNAUTHORIZED);
					button.setEnabled(true);
				} else if (VoXObserverState.mHttpCode == 0) {
					showDialog(Consts.REST_ERROR);
					button.setEnabled(true);
				} else if (VoXObserverState.mHttpCode != HttpStatus.OK.value() && VoXObserverState.mHttpCode != -1) {
					showDialog(Consts.REST_HTTP_ERROR);
					button.setEnabled(true);
				} else {

					// If we made it this far then the HTTP request was
					// successful
					if (VoXObserverState.mSyncType == VoXObserverState.SyncType.PLAN) {
						if (VoXObserverState.mSuccess) {
							initSummary();
						} else {
							showDialog(Consts.REST_ERROR);
						}
					} else {
						c = managedQuery(OrderResultContract.CONTENT_URI, OrderResultContract.PROJECTION, null, null, null);
						count = c.getCount();
						if (count == 0) {
							return;
						}

						if (!c.moveToFirst())
							return;

						mOrderResult = new OrderResult();
						mOrderResult.result_string = c.getString(OrderResultContract.RESULT_STRING_INDEX);
						mOrderResult.login_name = c.getString(OrderResultContract.LOGIN_NAME_INDEX);
						mOrderResult.login_password = c.getString(OrderResultContract.LOGIN_PASSWORD_INDEX);
						mOrderResult.auth_uuid = c.getString(OrderResultContract.AUTH_UUID_INDEX);
						mOrderResult.cc_charge_amount = c.getString(OrderResultContract.CC_CHARGE_AMOUNT_INDEX);
						mOrderResult.cc_auth_code = c.getString(OrderResultContract.CC_AUTH_CODE_INDEX);

						if (VoXObserverState.mSuccess) {
							Intent intent = new Intent(OrderSummaryActivity.this, OrderSuccessActivity.class);
							intent.putExtra(OrderResultContract.CC_CHARGE_AMOUNT, mOrderResult.cc_charge_amount);
							intent.putExtra(OrderResultContract.LOGIN_NAME, mOrderResult.login_name);
							intent.putExtra(OrderResultContract.LOGIN_PASSWORD, mOrderResult.login_password);
							startActivity(intent);
							finish();
						} else {
							OrderResultError error = new OrderResultError();
							error = new OrderResultError();
							error.typeInt = c.getInt(OrderResultContract.ERROR_TYPE_INDEX);
							error.msg = c.getString(OrderResultContract.ERROR_MSG_INDEX);
							mOrderResult.order_error = error;
							handleOrderError();
						}
					}
				}
				break;
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void fixError(Class c, int data) {
		Intent intent = null;
		intent = new Intent(this, c);
		intent.putExtra(ORDER_EDIT, data);

		startActivity(intent);
		finish();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		dismissProgressDialog();

		switch (id) {
		case OVERSUBSCRIBED:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_oversubscribed))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,	int whichButton) {

								}
							}).create();
		case CC_AUTH_FAIL:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_cc_auth_fail) + "\n\n" + mOrderResult.order_error.msg)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

								}
							}).create();
		case BILLING_ADDRESS1:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(
							getString(R.string.voxmobile_bad_billing_address))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									fixError(BillingInfoActivity.class, EDIT_STREET);
								}
							}).create();
		case CITY:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_bad_billing_city))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									fixError(BillingInfoActivity.class,
											EDIT_CITY);
								}
							}).create();
		case COUNTRY:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(
							getString(R.string.voxmobile_bad_billing_country))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Intent intent = null;
									intent = new Intent(
											OrderSummaryActivity.this,
											SelectCountryActivity.class);
									startActivity(intent);
								}
							}).create();
		case POSTAL_CODE:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_bad_billing_zip))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									fixError(BillingInfoActivity.class,
											EDIT_ZIP);
								}
							}).create();
		case CC_CVV:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_bad_cc_cvv))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									fixError(BillingInfoActivity.class,
											EDIT_CC_CVV);
								}
							}).create();
		case CC_MONTH:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_bad_cc_month))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									fixError(BillingInfoActivity.class,
											EDIT_CC_NUMBER);
								}
							}).create();
		case CC_YEAR:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_bad_cc_year))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									fixError(BillingInfoActivity.class,
											EDIT_CC_NUMBER);
								}
							}).create();
		case CC_NUMBER:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_bad_cc_number))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									fixError(BillingInfoActivity.class,
											EDIT_CC_NUMBER);
								}
							}).create();
		case EMAIL:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_bad_email))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									fixError(BillingInfoActivity.class,
											EDIT_EMAIL);
								}
							}).create();
		case FIRST_NAME:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_bad_first_name))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									fixError(BillingInfoActivity.class,
											EDIT_FIRST_NAME);
								}
							}).create();
		case LAST_NAME:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_bad_last_name))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									fixError(BillingInfoActivity.class,
											EDIT_LAST_NAME);
								}
							}).create();
		case OTHER:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(
							getString(R.string.voxmobile_general_fault)
									+ "\n\n" + mOrderResult.order_error.msg)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create();
		case Consts.REST_UNAUTHORIZED:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(R.string.voxmobile_attention)
					.setMessage(getString(R.string.voxmobile_unauthorized_msg))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create();
		case Consts.REST_UNSUPPORTED:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(getString(R.string.voxmobile_attention))
					.setMessage(getString(R.string.voxmobile_upgrade))
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create();
		case Consts.REST_HTTP_ERROR:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(getString(R.string.voxmobile_server_error))
					.setMessage(VoXObserverState.mError)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create();
		case Consts.REST_ERROR:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(getString(R.string.voxmobile_network_error))
					.setMessage(VoXObserverState.mError)
					.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create();
		}
		return null;
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerContentObservers();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		unregisterContentObservers();
		super.onStop();
	}

	private void initSummary() {
		mSummaryItems.clear();
		mCharges.clear();

		if (!getPlan())
			return;

		mSummaryItems.add(new SummaryItem(DataType.TEXT,
				getString(R.string.voxmobile_summary_text_1), null, null,
				EDIT_NULL, true, Color.YELLOW));

		mSummaryItems.add(new SummaryItem(DataType.ITEM,
				getString(R.string.voxmobile_summary_text_2), mPlanName,
				ServicePlansListActivity.class, EDIT_NULL, true, -1));

		mSummaryItems.add(new SummaryItem(DataType.ITEM,
				getString(R.string.voxmobile_summary_text_3), String.format(
						"%s %s", OrderHelper.getStringValue(this,
								OrderHelper.FIRST_NAME), OrderHelper
								.getStringValue(this, OrderHelper.LAST_NAME)),
				BillingInfoActivity.class, EDIT_FIRST_NAME, true, -1));

		mSummaryItems.add(new SummaryItem(DataType.ITEM,
				getString(R.string.voxmobile_summary_text_4), OrderHelper
						.getStringValue(this, OrderHelper.EMAIL),
				BillingInfoActivity.class, EDIT_EMAIL, true, -1));

		mSummaryItems.add(new SummaryItem(DataType.ITEM,
				getString(R.string.voxmobile_summary_text_8), OrderHelper
						.getStringValue(this, OrderHelper.BILLING_COUNTRY),
				SelectCountryActivity.class, EDIT_NULL, false, -1));

		if (!OrderHelper.getBooleanValue(this, OrderHelper.IS_FREE, false)) {
			mSummaryItems.add(new SummaryItem(DataType.ITEM,
					getString(R.string.voxmobile_summary_text_5), OrderHelper
							.getStringValue(this, OrderHelper.CC_NUMBER),
					BillingInfoActivity.class, EDIT_CC_NUMBER, true, -1));

			mSummaryItems.add(new SummaryItem(DataType.ITEM,
					getString(R.string.voxmobile_summary_text_6), OrderHelper
							.getStringValue(this, OrderHelper.CC_CVV),
					BillingInfoActivity.class, EDIT_CC_CVV, true, -1));

			mSummaryItems.add(new SummaryItem(DataType.ITEM,
					getString(R.string.voxmobile_summary_text_7), String
							.format("%02d/%04d", OrderHelper.getIntValue(this,
									OrderHelper.CC_EXP_MONTH, 0), OrderHelper
									.getSelectedYear(this)),
					BillingInfoActivity.class, EDIT_CC_EXP_MONTH, true, -1));

			mSummaryItems.add(new SummaryItem(DataType.ITEM,
					getString(R.string.voxmobile_summary_text_9), String
							.format("%s, %s", OrderHelper.getStringValue(this,
									OrderHelper.DID_CITY),
									OrderHelper.getStringValue(this,
											OrderHelper.DID_STATE)),
					DIDSelectionActivity.class, EDIT_NULL, true, -1));

			mSummaryItems.add(new SummaryItem(DataType.ITEM,
					getString(R.string.voxmobile_billing_street), String
							.format("%s\n%s, %s", OrderHelper.getStringValue(
									this, OrderHelper.BILLING_STREET),
									OrderHelper.getStringValue(this,
											OrderHelper.BILLING_CITY),
									OrderHelper.getStringValue(this,
											OrderHelper.BILLING_POSTAL_CODE)),
					BillingInfoActivity.class, EDIT_STREET, true, -1));

			mSummaryItems.add(new SummaryItem(DataType.TEXT,
					getString(R.string.voxmobile_summary_text_10), null, null,
					EDIT_NULL, true, -1));

			for (ChargeItem charge : mCharges) {
				mSummaryItems.add(new SummaryItem(DataType.CHARGE, charge.name,
						charge.value, null, EDIT_NULL, true, -1));
			}

			mSummaryItems
					.add(new SummaryItem(DataType.CHARGE,
							getString(R.string.voxmobile_total_charges), "$"
									+ mTotalPrice, null, EDIT_NULL, true,
							Color.YELLOW));
		}
		mAdapter.notifyDataSetChanged();
	}

	private boolean getPlan() {
		String planId = OrderHelper.getStringValue(this, OrderHelper.PLAN_ID);

		Cursor c = getContentResolver().query(PlanContract.CONTENT_URI,
				PlanContract.PROJECTION, PlanContract.PLAN_ID + "=?",
				new String[] { planId }, null);

		if (c.getCount() == 0) {
			c.close();
			VoXObserverState.mSyncType = VoXObserverState.SyncType.PLAN;
			getContentResolver().update(PlanContract.CONTENT_URI, null, null,
					null);
			return false;
		}

		if (c.moveToFirst()) {
			mPlanName = c.getString(PlanContract.TITLE_INDEX);
			mTotalPrice = c.getString(PlanContract.TOTAL_PRICE_INDEX);
		}
		c.close();

		c = getContentResolver().query(PlanChargeContract.CONTENT_URI,
				PlanChargeContract.PROJECTION,
				PlanChargeContract.PLAN_ID + "=?", new String[] { planId },
				PlanChargeContract.DESCRIPTION);

		while (c.moveToNext()) {
			ChargeItem item = new ChargeItem(
					c.getString(PlanChargeContract.DESCRIPTION_INDEX), "$"
							+ c.getString(PlanChargeContract.PRICE_INDEX));

			mCharges.add(item);
		}
		c.close();

		return true;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		SummaryItem item = mSummaryItems.get(position);

		if (item.editAction != null) {
			Intent intent = null;
			intent = new Intent(this, item.editAction);
			intent.putExtra(ORDER_EDIT, item.editField);

			if (item.finishOnEdit) {
				startActivity(intent);
				finish();
			} else {
				startActivityForResult(intent, 0);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		initSummary();
		mAdapter.notifyDataSetChanged();
	}

	private void handleOrderError() {
		switch (mOrderResult.order_error.typeInt) {
		case OrderHelper.Error.OVERSUBSCRIBED:
			showDialog(OVERSUBSCRIBED);
			break;
		case OrderHelper.Error.CC_AUTH_FAIL:
			showDialog(CC_AUTH_FAIL);
			break;
		case OrderHelper.Error.MISSING_BILLING_ADDRESS:
		case OrderHelper.Error.BILLING_ADDRESS1:
			showDialog(BILLING_ADDRESS1);
			break;
		case OrderHelper.Error.CITY:
			showDialog(CITY);
			break;
		case OrderHelper.Error.COUNTRY:
			showDialog(COUNTRY);
			break;
		case OrderHelper.Error.POSTAL_CODE:
			showDialog(POSTAL_CODE);
			break;
		case OrderHelper.Error.CC_CVV:
			showDialog(CC_CVV);
			break;
		case OrderHelper.Error.CC_MONTH:
			showDialog(CC_MONTH);
			break;
		case OrderHelper.Error.CC_YEAR:
			showDialog(CC_YEAR);
			break;
		case OrderHelper.Error.MISSING_CC:
		case OrderHelper.Error.CC_NUMBER:
			showDialog(CC_NUMBER);
			break;
		case OrderHelper.Error.EMAIL:
			showDialog(EMAIL);
			break;
		case OrderHelper.Error.FIRST_NAME:
			showDialog(FIRST_NAME);
			break;
		case OrderHelper.Error.LAST_NAME:
			showDialog(LAST_NAME);
			break;
		default:
			showDialog(OTHER);
		}
	}
}
