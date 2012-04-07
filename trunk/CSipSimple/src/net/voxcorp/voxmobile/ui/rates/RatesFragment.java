package net.voxcorp.voxmobile.ui.rates;

import java.io.IOException;
import java.io.InputStream;

import net.voxcorp.R;
import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;
import net.voxcorp.voxmobile.provider.DBContract.RateCityContract;
import net.voxcorp.voxmobile.provider.DBContract.RateCountryContract;
import net.voxcorp.voxmobile.provider.DBContract.Tables;
import net.voxcorp.voxmobile.provider.DBContract.TrialDialCodeContract;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class RatesFragment extends ListFragment {

	private static final int DIAL_CODES = 0;
	public static final int RESTART = -1;
	
	private RatesActivity mActivity;
	public int mSelectedCountry = -1;
	private String mGroup;
	private SimpleCursorAdapter mCountryAdapter;
	private SimpleCursorAdapter mCityAdapter;
	private GridView mGrid;
	private LinearLayout mList;
	private LinearLayout mContainer;
	private boolean isLargeScreen;
	private boolean isXLargeScreen;
	private LinearLayout mView;
	private int mDefaultTextColor = 0;

	private String mSelectedCountryName;
	private String mSelectedCountrySample;
	
	public void reset() {
		mSelectedCountryName = null;
		mSelectedCountrySample = null;
		mCountryAdapter = null;
		mCityAdapter = null;
		mSelectedCountry = -1;
		update();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mView = (LinearLayout) inflater.inflate(R.layout.voxmobile_rates, null);

		Bundle args = getArguments();
		mGroup = args.getString("group");

		int screenLayout = getResources().getConfiguration().screenLayout;
		int screenMask = Configuration.SCREENLAYOUT_SIZE_MASK;
		isXLargeScreen = (screenLayout & screenMask) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
		isLargeScreen = isXLargeScreen || ((screenLayout & screenMask) == Configuration.SCREENLAYOUT_SIZE_LARGE);

		mList = (LinearLayout) mView.findViewById(R.id.LinearLayout02);
		mContainer = (LinearLayout) mView.findViewById(R.id.LinearLayout01);
		mGrid = (GridView) mView.findViewById(R.id.GridView01);
		
	    mGrid.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	    		Cursor c = (Cursor)mCountryAdapter.getItem(position);

	    		int countryId = c.getInt(RateCountryContract.COUNTRY_ID_INDEX);
	    		String country = c.getString(RateCountryContract.COUNTRY_INDEX);
	    		
	    		mActivity.trackEvent(countryId);

    			mSelectedCountry = countryId;

	    		if (isLargeScreen) {
	    			mSelectedCountrySample = String.format("%s: 011 + %d + XXXXX", getString(R.string.voxmobile_sample_call), mSelectedCountry);
	    			mSelectedCountryName = country;

	    			updateSelectedCountry();
	    			RatesFragment.this.setListAdapter(null);

    				updateDialCodeList();
	    		} else {
    	    		Intent intent = new Intent(mActivity, DialCodeListActivity.class);
    	    		intent.putExtra(DialCodeListActivity.COUNTRY_ID_KEY, mSelectedCountry);
    	    		intent.putExtra(DialCodeListActivity.COUNTRY_KEY, country);
    	    		intent.putExtra(DialCodeListActivity.COUNTRY_GROUP_KEY, mGroup);
    	    		intent.putExtra(RatesActivity.TRIAL_MODE, mActivity.mTrialMode);
    	        	startActivityForResult(intent, DIAL_CODES);
	    		}
	        }
	    });

	    setupLayout(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);

	    setRetainInstance(true);
		return mView;
	}

	@Override
	public void onResume() {
		update();
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("position", mSelectedCountry);
		outState.putString("group", mGroup);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setupLayout(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mSelectedCountry = savedInstanceState.getInt("position");
			mGroup = savedInstanceState.getString("group");
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
		case DIAL_CODES:
			if (resultCode == RESTART) {
				mActivity.restart();
			}
			break;
		}
	}

	private void setupLayout(boolean isPortrait) {
		int top;
		int bottom;
		int layout;

		if (isPortrait) {
			top = 45;
			bottom = 55;
			layout = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		} else {
			top = isXLargeScreen ? 40 : 60;
			bottom = isXLargeScreen ? 60 : 40;
			layout = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		}
		
		if (mContainer != null) {
			mContainer.setOrientation(layout);
		}
		
		if (isLargeScreen && mGrid != null) {
			mGrid.setLayoutParams(new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, 
					LayoutParams.FILL_PARENT, top));
		}

		if (isLargeScreen && mList != null) {
			mList.setLayoutParams(new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, 
					LayoutParams.FILL_PARENT, bottom));
		}
	}

	private void update() {
		mActivity = (RatesActivity)getActivity();
		updateCountryList();
		if (isLargeScreen) {
			updateDialCodeList();
			updateSelectedCountry();
		}
	}
	
	private void updateSelectedCountry() {
		if (mView == null) {
			return;
		}
	
		TextView text = (TextView) mView.findViewById(R.id.Country);
		text.setText(mSelectedCountryName);

		text = (TextView) mView.findViewById(R.id.SampleCall);
		text.setText(mSelectedCountrySample);
	}
	
	private void updateCountryList() {
		if (mGroup == null) {
			return;
		}

		Cursor c;
		if (mActivity.mTrialMode) {
			String subQuery = String.format("SELECT %s FROM %s",
					TrialDialCodeContract.COUNTRY_ID,
					Tables.TRIAL_DIAL_CODE);

			String where = String.format("%s=? AND %s IN (%s)",
					RateCountryContract.COUNTRY_GROUP,
					RateCountryContract.COUNTRY_ID,
					subQuery);

			c = mActivity.getContentResolver().query(
					RateCountryContract.CONTENT_URI, 
					RateCountryContract.PROJECTION,
					where,
					new String[] { mGroup },
					RateCountryContract.COUNTRY);
		} else {
			c = mActivity.getContentResolver().query(
					RateCountryContract.CONTENT_URI, 
					RateCountryContract.PROJECTION,
					RateCountryContract.COUNTRY_GROUP + "=?",
					new String[] { mGroup },
					RateCountryContract.COUNTRY);
		}
		mActivity.startManagingCursor(c);

		if (c.getCount() == 0) {
			mSelectedCountry = -1;
			mActivity.restart();
			return;
		}

		mCountryAdapter = new SimpleCursorAdapter(
				mActivity,
				R.layout.voxmobile_grid_flag_item,
				c,
				new String[] { RateCountryContract.COUNTRY_ID, RateCountryContract.COUNTRY },
				new int[] { R.id.ImageView01, R.id.TextView01 });

		mCountryAdapter.setViewBinder(new ViewBinder() {
			public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {

				if (aView.getId() == R.id.ImageView01) {
					Bitmap flag = getFlag(aCursor.getInt(RateCountryContract.COUNTRY_ID_INDEX));
			        ImageView imageView = (ImageView)aView.findViewById(R.id.ImageView01);
			        imageView.setImageBitmap(flag);
					return true;
				} else if (aView.getId() == R.id.TextView01) {
					TextView text = (TextView) aView;
					text.setText(aCursor.getString(RateCountryContract.COUNTRY_INDEX));
					return true;
				}
				return false;
			}
		});

		mGrid.setAdapter(mCountryAdapter);
	}
	
	public void updateDialCodeList() {

		if (mActivity == null)
			return;
		
		if (!isLargeScreen || mSelectedCountry == -1) {
			setListAdapter(null);
			return;
		}

		Cursor c = mActivity.getContentResolver().query(
				RateCountryContract.CONTENT_URI, 
				RateCountryContract.PROJECTION,
				RateCountryContract.COUNTRY_GROUP + "=?",
				new String[] { mGroup },
				RateCountryContract.COUNTRY);
		int i = c.getCount();
		c.close();
		if (i == 0) {
			mActivity.restart();
			return;
		}

		if (mActivity.mTrialMode) {
			c = mActivity.getContentResolver().query(
					TrialDialCodeContract.CONTENT_URI, 
					TrialDialCodeContract.PROJECTION,
					TrialDialCodeContract.COUNTRY_ID + "=?",
					new String[] { "" + mSelectedCountry },
					TrialDialCodeContract.DIAL_CODE);
		} else {
			c = mActivity.getContentResolver().query(
					RateCityContract.CONTENT_URI, 
					RateCityContract.PROJECTION,
					RateCityContract.COUNTRY_ID + "=?",
					new String[] { "" + mSelectedCountry },
					RateCityContract.CITY);
		}
		mActivity.startManagingCursor(c);

		if (c.getCount() == 0 && mSelectedCountry != -1) {
			mActivity.updateDialCodes(mSelectedCountry);
	        return;
		}

		if (mActivity.mTrialMode) {
			mCityAdapter = new SimpleCursorAdapter(
					mActivity,
					R.layout.voxmobile_trial_dial_code_item,
					c,
					new String[] { TrialDialCodeContract.DIAL_CODE, TrialDialCodeContract.BLOCKED },
					new int[] { R.id.TextView01, R.id.TextView02 });

			mCityAdapter.setViewBinder(new ViewBinder() {
				public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {

					TextView text;
					if (aView.getId() == R.id.TextView01) {
						text = (TextView) aView;
						text.setText(aCursor.getString(TrialDialCodeContract.DIAL_CODE_INDEX));
						
						if (mDefaultTextColor == 0) {
							mDefaultTextColor = text.getCurrentTextColor();
						}
						return true;
					} else if (aView.getId() == R.id.TextView02) {
						text = (TextView) aView;
						String status;
						if (aCursor.getInt(TrialDialCodeContract.BLOCKED_INDEX) == DBBoolean.TRUE) {
							status = getString(R.string.voxmobile_disabled);
							text.setTextColor(mDefaultTextColor);
						} else {
							status = getString(R.string.voxmobile_enabled);
							text.setTextColor(Color.GREEN);
						}
						text.setText(status);
						return true;
					}
					return false;
				}
			});
		} else {
			mCityAdapter = new SimpleCursorAdapter(
					mActivity,
					R.layout.voxmobile_rate_detail_item,
					c,
					new String[] { RateCityContract.CITY, RateCityContract.RATE, RateCityContract.DIAL_CODE },
					new int[] { R.id.TextView01, R.id.TextView02, R.id.TextView03 });

			mCityAdapter.setViewBinder(new ViewBinder() {
				public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {

					if (aView.getId() == R.id.TextView01) {
						TextView text = (TextView) aView;
						text.setText(aCursor.getString(RateCityContract.CITY_INDEX));
						return true;
					} else if (aView.getId() == R.id.TextView02) {
						TextView text = (TextView) aView;
						text.setText("$" + aCursor.getString(RateCityContract.RATE_INDEX));
						return true;
					} else if (aView.getId() == R.id.TextView03) {
						TextView text = (TextView) aView;
						text.setText(aCursor.getString(RateCityContract.DIAL_CODE_INDEX));
						return true;
					}
					return false;
				}
			});
		}

		setListAdapter(mCityAdapter);
	}

    private Bitmap getFlag(int countryId) {
    	InputStream is = null;
    	try {
    		String fileName = String.format("voxmobile_flags/%d.png", countryId); 
    	    is = mActivity.getResources().getAssets().open(fileName);
    	} catch (IOException e) {
    	}

    	if (is == null) {
    		try {
	    		String fileName = "voxmobile_flags/unknown.png"; 
	    	    is = mActivity.getResources().getAssets().open(fileName);
    		} catch (IOException e) {
    		}
    	}
    	return BitmapFactory.decodeStream(is);	    	
    }
}
