package net.voxcorp.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

public class Plan implements Parcelable {
	
	public String plan_id;
	public String title;
	public String description;
	public String total_price;
	public PlanCharge[] charges;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(plan_id);
		dest.writeString(title);
		dest.writeString(description);
		dest.writeString(total_price);
		dest.writeParcelableArray(charges, 0);
	}

	public Plan() {
		super();
		plan_id = "";
		title = "";
		description = "";
		total_price = "";
	}

	public Plan(Parcel source) {
		super();
		plan_id = source.readString();
		title = source.readString();
		description = source.readString();
		total_price = source.readString();
        charges = (PlanCharge[]) source.readParcelableArray(PlanCharge.class.getClassLoader());
	}
	
    public static final Parcelable.Creator<Plan> CREATOR = new Parcelable.Creator<Plan>() {
        @Override
        public Plan createFromParcel(Parcel in) {
            return new Plan(in);
        }

        @Override
        public Plan[] newArray(int size) {
            return new Plan[size];
        }
    };
}
