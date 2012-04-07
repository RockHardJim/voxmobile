package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class Account extends JsonBase {
	
	public String uuid;
	public String account_no;
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
		dest.writeString(uuid);
		dest.writeString(account_no);
		dest.writeParcelableArray(users, 0);
	}

	public Account() {
		super();
		uuid = "";
		account_no = "";
	}

	public Account(Parcel source) {
		super();
        success = source.readInt() == DBBoolean.TRUE ? true : false;
        httpStatus = source.readInt();
        error = source.readString();
        uuid = source.readString();
        account_no = source.readString();
        users = (SipUser[]) source.readParcelableArray(SipUser.class.getClassLoader());
	}
	
    public static final Parcelable.Creator<Account> CREATOR = new Parcelable.Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };
}
