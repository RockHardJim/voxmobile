/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.types;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class ServicePlan implements Parcelable {
	
    public ArrayList<ServicePlanCharge> mCharges = new ArrayList<ServicePlanCharge>();
	public Double mTotalPrice;
	public String mPlanId;
	public String mPlanDescription;
	public String mPlanName;
	
	public ServicePlan(Double mTotalPrice, String mPlanId, String mPlanDescription, String mPlanName) {
		super();
		
		this.mTotalPrice = mTotalPrice;
		this.mPlanId = mPlanId;
		this.mPlanDescription = mPlanDescription;
		this.mPlanName = mPlanName;
	}
	
	public void addCharge(Double price, boolean recurring, String description) {
		ServicePlanCharge c = new ServicePlanCharge(price, recurring, description);
		mCharges.add(c);
	}

	public ServicePlan(Parcel source) {
		Bundle b = source.readBundle(ServicePlanCharge.class.getClassLoader());        
		mCharges = b.getParcelableArrayList("charges");		
		mTotalPrice = source.readDouble();
		mPlanId = source.readString();
		mPlanDescription = source.readString();
		mPlanName = source.readString();
	}

	public static final Parcelable.Creator<ServicePlan> CREATOR = new Parcelable.Creator<ServicePlan>() {
		@Override
		public ServicePlan createFromParcel(Parcel in) {
			return new ServicePlan(in);
		}
		
		@Override
		public ServicePlan[] newArray(int size) {
			return new ServicePlan[size];
		}
	};
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Bundle charges = new Bundle();
		charges.putParcelableArrayList("charges", mCharges);
		dest.writeBundle(charges);
		dest.writeDouble(mTotalPrice);
		dest.writeString(mPlanId);
		dest.writeString(mPlanDescription);
		dest.writeString(mPlanName);
	}
	
}
