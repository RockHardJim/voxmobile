package net.voxcorp.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

public class OrderResult extends JsonBase {
	
	public String result_string;
	public String login_name;
	public String login_password;
	public String auth_uuid;
	public String cc_charge_amount;
	public String cc_auth_code;
	public OrderResultError order_error;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		
		dest.writeString(result_string);
		dest.writeString(login_name);
		dest.writeString(login_password);
		dest.writeString(auth_uuid);
		dest.writeString(cc_charge_amount);
		dest.writeString(cc_auth_code);
		dest.writeInt(order_error.typeInt);
		dest.writeString(order_error.type);
		dest.writeString(order_error.msg);
	}

	public OrderResult() {
		super();
		result_string = "";
		login_name = "";
		login_password = "";
		auth_uuid = "";
		cc_charge_amount = "";
		cc_auth_code = "";
		error = null;
	}

	public OrderResult(Parcel source) {
		super();
        success = source.readInt() == DBBoolean.TRUE ? true : false;
        httpStatus = source.readInt();
        error = source.readString();

        result_string = source.readString();
		login_name = source.readString();
		login_password = source.readString();
		auth_uuid = source.readString();
		cc_charge_amount = source.readString();
		cc_auth_code = source.readString();
		order_error.typeInt = source.readInt();
		order_error.type = source.readString();
		order_error.msg = source.readString();
	}
	
    public static final Parcelable.Creator<OrderResult> CREATOR = new Parcelable.Creator<OrderResult>() {
        @Override
        public OrderResult createFromParcel(Parcel in) {
            return new OrderResult(in);
        }

        @Override
        public OrderResult[] newArray(int size) {
            return new OrderResult[size];
        }
    };
}
