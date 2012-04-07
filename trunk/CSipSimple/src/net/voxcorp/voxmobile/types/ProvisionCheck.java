package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class ProvisionCheck extends JsonBase {
	
	public boolean provisioned;
	public String account_no;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeInt(provisioned ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeString(account_no);
	}

	public ProvisionCheck() {
		super();
		provisioned = false;
		account_no = "";
	}

	public ProvisionCheck(Parcel source) {
		super();
        success = source.readInt() == DBBoolean.TRUE ? true : false;
        httpStatus = source.readInt();
        error = source.readString();
        provisioned = source.readInt() == DBBoolean.TRUE ? true : false;
        account_no = source.readString();
	}
	
    public static final Parcelable.Creator<ProvisionCheck> CREATOR = new Parcelable.Creator<ProvisionCheck>() {
        @Override
        public ProvisionCheck createFromParcel(Parcel in) {
            return new ProvisionCheck(in);
        }

        @Override
        public ProvisionCheck[] newArray(int size) {
            return new ProvisionCheck[size];
        }
    };
}
