/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.ui;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import net.voxcorp.R;

public class DIDChooserActivity extends ListActivity {
	
	private static ArrayList<String> mList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		Bundle b = getIntent().getBundleExtra("data");
    	mList = b.getStringArrayList("values");
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mList));
		
		setContentView(R.layout.voxmobile_did_chooser);

    	setTitle(R.string.voxmobile_did_chooser_title);

		Button cancelBt = (Button) findViewById(R.id.voxmobile_did_list_cancel);
		cancelBt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Object o = this.getListAdapter().getItem(position);
		String DID = o.toString();

		Intent result = getIntent();
		result.putExtra("did", DID);
		
		setResult(RESULT_OK, result);
		finish();		
	}
	
}
