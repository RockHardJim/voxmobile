package net.voxcorp.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

public class PlanCharge implements Parcelable {
	
	public String description;
	public String price;
	public String recurring;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(description);
		dest.writeString(price);
		dest.writeString(recurring);

	}

	public PlanCharge() {
		super();
		description = "";
		price = "";
		recurring = "";
	}

	public PlanCharge(Parcel source) {
		super();
		description = source.readString();
		price = source.readString();
		recurring = source.readString();
	}
	
    public static final Parcelable.Creator<PlanCharge> CREATOR = new Parcelable.Creator<PlanCharge>() {
        @Override
        public PlanCharge createFromParcel(Parcel in) {
            return new PlanCharge(in);
        }

        @Override
        public PlanCharge[] newArray(int size) {
            return new PlanCharge[size];
        }
    };
}
