package net.voxcorp.voxmobile.ui.order;

import net.voxcorp.R;

import org.springframework.http.HttpStatus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.provider.DBContract.PlanContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.SyncStatus;
import net.voxcorp.voxmobile.ui.TrackedListActivity;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.OrderHelper;

public class ServicePlansListActivity extends TrackedListActivity implements OnClickListener {

	private static final String THIS_FILE = "ServicePlansListActivity";
	private static final int SHOW_PLAN = -1;
	
	private static final int CHOOSE_COUNTRY = 1;
	
	private static VoXObserver mVoXObserver;
	private ProgressDialog mProgressDialog = null;   
	private Cursor mPlanCursor;
	private SimpleCursorAdapter mPlanAdapter;
	private int mSelectedPlan = -1;
	
	/* dialog items, PITA because API level 7 is needed but doesn't 
	 * support showDialog(int, Bundle) which could have been used
	 * to pass dialog strings.
	 */
	private String mDialogTitle;
	private String mDialogMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		Log.d(THIS_FILE, "ServicePlansListActivity.onCreate()");
		
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voxmobile_service_plans_list);
		setTitle(R.string.voxmobile_calling_plans);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_voxmobile_info);
		
		trackPageView("order/start");
	}

	private void registerContentObservers() {
		ContentResolver cr = getContentResolver();
		mVoXObserver = new VoXObserver(new Handler());
		cr.registerContentObserver(RequestContract.CONTENT_URI, true, mVoXObserver);
	}

	private void unregisterContentObservers() {
		ContentResolver cr = getContentResolver();
		if (mVoXObserver != null) {
			cr.unregisterContentObserver(mVoXObserver);
			mVoXObserver = null;
		}
	}

	private void showProgressDialog() {
		if (isFinishing() || mProgressDialog != null) return;
		mProgressDialog = ProgressDialog.show(this, "", getString(R.string.voxmobile_please_wait), true, true);
	}
	
	private void dismissProgressDialog() {
		if (mProgressDialog == null) return;
		mProgressDialog.dismiss();
		mProgressDialog = null;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {

		dismissProgressDialog();
		
		switch (id) {
		case SHOW_PLAN:
			
			// set up sign up overview dialog
			Dialog dlg = new Dialog(this);
			dlg.setContentView(R.layout.voxmobile_service_plan_detail);
			dlg.setTitle(R.string.voxmobile_signup_header);
			dlg.setCancelable(true);
			dlg.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					removeDialog(SHOW_PLAN);
				}
			});
			
			// set up text
			TextView text = (TextView) dlg.findViewById(R.id.TextView01);
			text.setText(mDialogTitle);
			
			text = (TextView) dlg.findViewById(R.id.TextView02);
			text.setText(mDialogMessage);
			
			// set up continue button
			Button button = (Button) dlg.findViewById(R.id.Button02);
			button.setText(R.string.voxmobile_continue);
			button.setOnClickListener(this);
			
			// set up cancel button
			button = (Button) dlg.findViewById(R.id.Button01);
			button.setText(R.string.voxmobile_back);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(SHOW_PLAN);
				}
			});
			
			return dlg;
		case Consts.REST_UNAUTHORIZED:
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.voxmobile_attention)
			.setMessage(getString(R.string.voxmobile_unauthorized_msg))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {					

				}
            }).create();
		case Consts.REST_UNSUPPORTED:
            trackEvent("rest", "unsupported", 0);
            
			return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_attention))
			.setMessage(getString(R.string.voxmobile_upgrade))
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {					
                    
                }
            }).create();
		case Consts.REST_HTTP_ERROR:
            trackEvent("rest", "http_error", 0);

            return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_server_error))
			.setMessage(VoXObserverState.mError)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {					
                    
                }
            }).create();
		case Consts.REST_ERROR:
            trackEvent("rest", "rest_error", 0);

            return new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(getString(R.string.voxmobile_network_error))
			.setMessage(VoXObserverState.mError)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {					
                    
                }
            }).create();
		}
		return null;
	}

	private static class VoXObserverState {
		private static int mSyncStatus = SyncStatus.STALE;
		private static int mHttpCode = 0;
		private static String mError = "";
		
		private static void reset() {
			mSyncStatus = SyncStatus.STALE;
			mHttpCode = 0;
			mError = "";
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

				if (VoXObserverState.mHttpCode == HttpStatus.UNAUTHORIZED.value()) {
					showDialog(Consts.REST_UNAUTHORIZED);
				} else if (VoXObserverState.mHttpCode == 0) {
					showDialog(Consts.REST_ERROR);
				} else if (VoXObserverState.mHttpCode != HttpStatus.OK.value()) {
					showDialog(Consts.REST_HTTP_ERROR);
				} else {
					updateList();
				}
				break;
			}			
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerContentObservers();
		updateList();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		unregisterContentObservers();
		super.onStop();
	}

	@Override
	public void onClick(View v) {
		dismissDialog(SHOW_PLAN);
		
		Cursor c = (Cursor)mPlanAdapter.getItem(mSelectedPlan);
		String planId = c.getString(PlanContract.PLAN_ID_INDEX);
		Boolean isFree = c.getDouble(PlanContract.TOTAL_PRICE_AS_REAL_INDEX) == 0.00;
		
		OrderHelper.setStringValue(this, OrderHelper.PLAN_ID, planId);
		OrderHelper.setBooleanValue(this, OrderHelper.IS_FREE, isFree);

		Intent intent = new Intent(this, SelectCountryActivity.class);
		startActivityForResult(intent, CHOOSE_COUNTRY);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		mSelectedPlan = position;
		
        Cursor c = (Cursor)mPlanAdapter.getItem(position);           
        mDialogTitle = c.getString(PlanContract.TITLE_INDEX);
        mDialogMessage = c.getString(PlanContract.DESCRIPTION_INDEX);
    
        showDialog(SHOW_PLAN);
	}

	private void updateList() {
		mPlanCursor = getContentResolver().query(
				PlanContract.CONTENT_URI, 
				PlanContract.PROJECTION,
				null,
				null,
				PlanContract.TITLE);
		
		startManagingCursor(mPlanCursor);
		
		if (mPlanCursor.getCount() == 0) {
            getContentResolver().update(PlanContract.CONTENT_URI, null, null, null);
			return;
		}
		
		mPlanAdapter = new SimpleCursorAdapter(
				this,
				R.layout.voxmobile_account_list_item,
				mPlanCursor,
				new String[] { PlanContract.TITLE, PlanContract.TOTAL_PRICE, PlanContract.TOTAL_PRICE },
				new int[] { R.id.TextView01, R.id.TextView02, R.id.ImageView01 });
		
		mPlanAdapter.setViewBinder(new ViewBinder() {
			public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {
				
				TextView textView;
				
				if (aColumnIndex == PlanContract.TITLE_INDEX) {
					textView = (TextView) aView;
					textView.setText(aCursor.getString(aColumnIndex));
					return true;
				} else if (aColumnIndex == PlanContract.TOTAL_PRICE_INDEX && aView.getId() == R.id.TextView02) {
					textView = (TextView) aView;

					String price = aCursor.getString(PlanContract.TOTAL_PRICE_INDEX);
					if (aCursor.getDouble(PlanContract.TOTAL_PRICE_AS_REAL_INDEX) == 0) {
						textView.setText(getString(R.string.voxmobile_no_cost));
					} else {
						String fmt = getString(R.string.voxmobile_cost) + ": $%s " + getString(R.string.voxmobile_per_month);
						textView.setText(String.format(fmt, price));
					}					
					
					return true;
				} else if (aColumnIndex == PlanContract.TOTAL_PRICE_INDEX && aView.getId() == R.id.ImageView01) {

					ImageView image = (ImageView) aView;
					
					Double price = aCursor.getDouble(PlanContract.TOTAL_PRICE_AS_REAL_INDEX);
					if (price == 0.00) {
						image.setImageResource(R.drawable.ic_voxmobile_check);
					} else if (price > 0.00 && price <= 6.00) {
						image.setImageResource(R.drawable.ic_voxmobile_money);
					} else if (price > 6.00 && price <= 15.00) {
						image.setImageResource(R.drawable.ic_voxmobile_money_2);
					} else {
						image.setImageResource(R.drawable.ic_voxmobile_money_3);
					}
					
					return true;
				}
				return false;
			}
		});

		setListAdapter(mPlanAdapter);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case CHOOSE_COUNTRY:
			if (resultCode == RESULT_OK) {
				startActivity(new Intent(this, BillingInfoActivity.class));
				finish();
				break;
			}
		}
	}
}
