package net.voxcorp.voxmobile.types;

import android.os.Parcelable;

public abstract class JsonBase implements Parcelable {

	public boolean success;
	public int httpStatus;
	public String error;

	public JsonBase() {
		super();
		success = false;
		httpStatus = 0;
		error = "";
	}

}
