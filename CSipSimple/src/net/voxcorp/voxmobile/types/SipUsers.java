package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class SipUsers extends JsonBase {
	
	public SipUser[] users;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeParcelableArray(users, 0);
	}

	public SipUsers() {
		super();
	}

	public SipUsers(Parcel source) {
		super();
        success = source.readInt() == DBBoolean.TRUE ? true : false;
        httpStatus = source.readInt();
        error = source.readString();
        users = (SipUser[]) source.readParcelableArray(SipUser.class.getClassLoader());
	}
	
    public static final Parcelable.Creator<SipUsers> CREATOR = new Parcelable.Creator<SipUsers>() {
        @Override
        public SipUsers createFromParcel(Parcel in) {
            return new SipUsers(in);
        }

        @Override
        public SipUsers[] newArray(int size) {
            return new SipUsers[size];
        }
    };
}
