package net.voxcorp.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

public class SipUser implements Parcelable {
	
	public String username;
	public String password;
	public String displayname;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(username);
		dest.writeString(password);
		dest.writeString(displayname);

	}

	public SipUser() {
		super();
		username = "";
		password = "";
		displayname = "";
	}

	public SipUser(Parcel source) {
		super();
        username = source.readString();
        password = source.readString();
        displayname = source.readString();
	}
	
    public static final Parcelable.Creator<SipUser> CREATOR = new Parcelable.Creator<SipUser>() {
        @Override
        public SipUser createFromParcel(Parcel in) {
            return new SipUser(in);
        }

        @Override
        public SipUser[] newArray(int size) {
            return new SipUser[size];
        }
    };
}
