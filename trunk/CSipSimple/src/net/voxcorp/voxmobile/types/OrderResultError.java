package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.utils.OrderHelper;

import android.os.Parcel;
import android.os.Parcelable;

public class OrderResultError implements Parcelable {
	
	public int typeInt;
	public String type;
	public String msg;
	
	public int getTypeAsInt() {
		if ("badvalue.billing_address.address1".equals(type)) return OrderHelper.Error.BILLING_ADDRESS1;
		else if ("badvalue.billing_address.city".equals(type)) return OrderHelper.Error.CITY;
		else if ("badvalue.billing_address.country".equals(type)) return OrderHelper.Error.COUNTRY;
		else if ("badvalue.billing_address.zip".equals(type)) return OrderHelper.Error.POSTAL_CODE;
		else if ("badvalue.cc_info.cc_cvv".equals(type)) return OrderHelper.Error.CC_CVV;
		else if ("badvalue.cc_info.cc_exp_month".equals(type)) return OrderHelper.Error.CC_MONTH;
		else if ("badvalue.cc_info.cc_exp_year".equals(type)) return OrderHelper.Error.CC_YEAR;
		else if ("badvalue.cc_info.cc_number".equals(type)) return OrderHelper.Error.CC_NUMBER;
		else if ("badvalue.contact.email".equals(type)) return OrderHelper.Error.EMAIL;
		else if ("badvalue.contact.first_name".equals(type)) return OrderHelper.Error.FIRST_NAME;
		else if ("badvalue.contact.last_name".equals(type)) return OrderHelper.Error.LAST_NAME;
		else if ("badvalue.contact.phone".equals(type)) return OrderHelper.Error.CONTACT_PHONE;
		else if ("badvalue.plan".equals(type)) return OrderHelper.Error.PLAN;
		else if ("cc.authfail".equals(type)) return OrderHelper.Error.CC_AUTH_FAIL;
		else if ("did.notfound".equals(type)) return OrderHelper.Error.DID_NOT_FOUND;
		else if ("sys.billing".equals(type)) return OrderHelper.Error.MISSING_BILLING_ADDRESS;
		else if ("sys.cc".equals(type)) return OrderHelper.Error.MISSING_CC;
		else if ("sys.contact".equals(type)) return OrderHelper.Error.CONTACT;
		else if ("sys.ccfailure".equals(type)) return OrderHelper.Error.CC_FAILURE;
		else if ("sys.did".equals(type)) return OrderHelper.Error.MISSING_DID;
		else if ("sys.plan".equals(type)) return OrderHelper.Error.MISSING_PLAN;
		else if ("sys.ccfailure".equals(type)) return OrderHelper.Error.CC_FAILURE;
		else if ("sys.oversubscribed".equals(type)) return OrderHelper.Error.OVERSUBSCRIBED;
		else return -99;
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(typeInt);
		dest.writeString(type);
		dest.writeString(msg);
	}

	public OrderResultError() {
		super();
		typeInt = 0;
		type = "";
		msg = "";
	}

	public OrderResultError(Parcel source) {
		super();
		typeInt = source.readInt();
		type = source.readString();
		msg = source.readString();
	}
	
    public static final Parcelable.Creator<OrderResultError> CREATOR = new Parcelable.Creator<OrderResultError>() {
        @Override
        public OrderResultError createFromParcel(Parcel in) {
            return new OrderResultError(in);
        }

        @Override
        public OrderResultError[] newArray(int size) {
            return new OrderResultError[size];
        }
    };
}
