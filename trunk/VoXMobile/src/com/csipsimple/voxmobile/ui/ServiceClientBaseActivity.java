/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.ui;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.csipsimple.voxmobile.service.MobileService;
import com.csipsimple.voxmobile.service.ServiceHelper;
import com.csipsimple.wizards.impl.VoXMobile;

public abstract class ServiceClientBaseActivity extends TrackedActivity {
	
	protected String THIS_FILE;

	/** Progress indicator to show during network operations **/
	protected ProgressDialog mProgress;

	/** Messenger for communicating with the MobileService process. */
	protected Messenger mService = null;
	
	/** Flag indicating whether we have called bind on the service. */
	protected boolean mIsBound;

	/** Service connection used to allow the remote service to communicate with this Activity **/
	protected ServiceConnection mConnection;	
	
	/** Messenger invoked by MobileService when it needs to communicate with this mConnection **/
	Messenger mMessenger;

	/** Handler that receives messages from the MobileService process **/
	abstract class IncomingHandler extends Handler {
	    @Override
	    abstract public void handleMessage(Message msg);
	}
	
	protected ServiceConnection getServiceConnectionInstance() {
		
		if (mConnection != null) {
			return mConnection;
		} else {
			
			return new ServiceConnection() {

			    @Override
				public void onServiceConnected(ComponentName className, IBinder service) {
			        // This is called when the connection with the service has been
			        // established, giving us the service object we can use to
			        // interact with the service.  We are communicating with our
			        // service through an IDL interface, so get a client-side
			        // representation of that from the raw service object.
			        mService = new Messenger(service);

			        // We want to monitor the service for as long as we are connected to it.
			        try {
			        	
			            Message msg = Message.obtain(null, ServiceHelper.MSG_REGISTER_CLIENT);
			            msg.replyTo = mMessenger;
			            mService.send(msg);

			        } catch (RemoteException e) {
			            // In this case the service has crashed before we could even
			            // do anything with it; we can count on soon being
			            // disconnected (and then reconnected if it can be restarted)
			            // so there is no need to do anything here.
			        }
			    }

			    @Override
			    public void onServiceDisconnected(ComponentName className) {
			        // This is called when the connection with the service has been
			        // unexpectedly disconnected -- that is, its process crashed.
			        mService = null;
			        Log.d(THIS_FILE, "Disconnected from remote service");
			    }
			};
		}
			
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(true);
        mProgress.setCanceledOnTouchOutside(false);
	}

	/** 
	 * 	Establish a connection with the MobileService service.  We use an
	 *  explicit class name because there is no reason to be able to let
	 *  other applications replace our component.
	 **/
	void doBindService() {
		if (mIsBound) {
		    Log.d("Base Svc", "Already bound to remote service");
		} else {
		    Log.d("Base Svc", "Binding to remote service");
		    bindService(new Intent(this, MobileService.class), mConnection, Context.BIND_AUTO_CREATE);
		    mIsBound = true;
		}
	}
	
	void doUnbindService() {
	    if (mIsBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.
	        if (mService != null) {
	            try {
	                Message msg = Message.obtain(null, ServiceHelper.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service
	                // has crashed.
	            }
	        }

	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	        Log.d(THIS_FILE, "Unbinding from remote service");
	    }
	}

	protected void dismissProgressDialog() {
		if (mProgress != null) {
			mProgress.dismiss();
		}
	}

	@Override
	protected void onPause() {
		dismissProgressDialog();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		doUnbindService();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		if (mConnection == null) {
			mConnection = getServiceConnectionInstance();
		}
		
        doBindService();
		super.onStart();
	}
	
	/** Helper function to access the user's UUID **/
	protected String getLocalUuid() {
		return VoXMobile.getUuid(this);
	}

	/** Helper function to log out **/
	protected void logOut() {
		// Remove UUID and send the user to the start activity
		VoXMobile.clearUuid(this);
		startActivity(new Intent(this, StartActivity.class));
		finish();
	}
		
}
