/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.ui;

import java.util.ArrayList;
import java.util.Iterator;

import net.voxcorp.R;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.csipsimple.voxmobile.types.ServicePlan;

public class ServicePlanChooser extends ListActivity {

	/** Dialog Types used in to respond to various MobileService exceptions **/
	private static final int DIALOG_SHOW_DETAILS = 1;

	private static ArrayList<ServicePlan> mPlans;
	private static int mPosition;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.voxmobile_service_plan_chooser);
		
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		
		mPlans = bundle.getParcelableArrayList("plans");

        // Build array lists for expandable list
        ArrayList<String> names = new ArrayList<String>();
        Iterator<ServicePlan> it = mPlans.iterator();
        while (it.hasNext()) {
            ServicePlan sp = it.next();
            names.add(sp.mPlanName);
        }
        
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names));

		Button cancelBt = (Button) findViewById(R.id.voxmobile_plan_cancel);
		cancelBt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
    			Intent result = getIntent();
    			setResult(RESULT_CANCELED, result);
				finish();
			}
		});
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		mPosition = position;
		showDialog(DIALOG_SHOW_DETAILS);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		switch (id) {
			case DIALOG_SHOW_DETAILS:
				// set up dialog
                Dialog dialog = new Dialog(ServicePlanChooser.this);
                dialog.setContentView(R.layout.voxmobile_service_plan_detail);
                dialog.setTitle(R.string.voxmobile_service_plan_details);
                dialog.setCancelable(true);
                 
                // set up text
                TextView text = (TextView) dialog.findViewById(R.id.TextView01);
                text.setText(mPlans.get(mPosition).mPlanName);

                text = (TextView) dialog.findViewById(R.id.TextView02);
                text.setSingleLine(false);
                text.setText(mPlans.get(mPosition).mPlanDescription);

                // set up continue button 
                Button button = (Button) dialog.findViewById(R.id.do_service_plan_continue);
                button.setText(R.string.voxmobile_continue);
                button.setOnClickListener(new OnClickListener() {
                @Override
                    public void onClick(View v) {

            			removeDialog(DIALOG_SHOW_DETAILS);
            			
       					Intent result = getIntent();
       					result.putExtra("plan_index", mPosition);
       					setResult(RESULT_OK, result);
       					finish();
                    }
                });

                // set up cancel button 
                button = (Button) dialog.findViewById(R.id.do_service_plan_cancel);
                button.setText(R.string.cancel);
                button.setOnClickListener(new OnClickListener() {
                @Override
                    public void onClick(View v) {
                		removeDialog(DIALOG_SHOW_DETAILS);
                    }
                });
                
                return dialog;
		}
		
		return null;
	}

}
