/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

public class ServicePlanCharge implements Parcelable {
	public Double mPrice;
	public boolean mRecurring;
	public String mDescription;
	
	public ServicePlanCharge(Double mPrice, boolean mRecurring, String mDescription) {
		super();
		this.mPrice = mPrice;
		this.mRecurring = mRecurring;
		this.mDescription = mDescription;
	}
	
	public ServicePlanCharge(Parcel in) {
		mPrice = in.readDouble();
		mRecurring = Boolean.valueOf(in.readString());
		mDescription = in.readString();
	}

	public static final Parcelable.Creator<ServicePlanCharge> CREATOR = new Parcelable.Creator<ServicePlanCharge>() {
		@Override
		public ServicePlanCharge createFromParcel(Parcel in) {
			return new ServicePlanCharge(in);
		}
		
		@Override
		public ServicePlanCharge[] newArray(int size) {
			return new ServicePlanCharge[size];
		}
	};
		
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(mPrice);
		dest.writeString(Boolean.toString(mRecurring));
		dest.writeString(mDescription);
	}
}
