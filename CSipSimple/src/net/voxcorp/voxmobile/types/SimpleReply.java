package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class SimpleReply extends JsonBase {
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
	}

	public SimpleReply() {
		super();
	}

	public SimpleReply(Parcel source) {
		super();
        success = source.readInt() == DBBoolean.TRUE ? true : false;
        httpStatus = source.readInt();
        error = source.readString();
	}
	
    public static final Parcelable.Creator<SimpleReply> CREATOR = new Parcelable.Creator<SimpleReply>() {
        @Override
        public SimpleReply createFromParcel(Parcel in) {
            return new SimpleReply(in);
        }

        @Override
        public SimpleReply[] newArray(int size) {
            return new SimpleReply[size];
        }
    };
}
