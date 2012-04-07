package net.voxcorp.voxmobile.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * Proxy ResultReceiver that offers a listener interface that can be
 * detached. Useful for when sending callbacks to a Service where a
 * listening Activity can be swapped out during configuration changes.
 */
public class DetachableResultReceiver extends ResultReceiver {
	private static final String THIS_FILE = "DetachableResultReceiver";
	private Receiver mReceiver;
	public DetachableResultReceiver(Handler handler) {
		super(handler);
	}
	public void clearReceiver() {
		mReceiver = null;
	}
	public void setReceiver(Receiver receiver) {
		mReceiver = receiver;
	}
	public interface Receiver {
		public void onReceiveResult(int resultCode, Bundle resultData);
	}
	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData) {
		if (mReceiver != null) {
			mReceiver.onReceiveResult(resultCode, resultData);
		} else {
			Log.w(THIS_FILE,
					String.format("Dropping result on floor for code %d: %s",
							resultCode, resultData.toString()));
        }
    }
}
