/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Filter;
import com.csipsimple.voxmobile.exception.AuthException;
import com.csipsimple.voxmobile.exception.HandlerException;
import com.csipsimple.voxmobile.types.DidCity;
import com.csipsimple.voxmobile.types.DidState;
import com.csipsimple.voxmobile.types.OrderResult;
import com.csipsimple.voxmobile.types.ServicePlan;
import com.csipsimple.voxmobile.types.SipUserInfo;
import com.csipsimple.voxmobile.utils.OrderHelper;
import com.csipsimple.wizards.impl.VoXMobile;
 
public class MobileService extends TrackedService {

    /** Array of registered clients **/
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	
	/** Stores the current state of the service **/
	private int mState;
	
	/** Log identifier **/
	protected String THIS_SERVICE = "Mobile Svc";
	
	/** Messenger invoked by MobileService clients when they need to communicate with this service **/
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
	/** Handler that receives messages from the MobileService clients **/
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ServiceHelper.MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                	sendServiceState();
                    break;
                case ServiceHelper.MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }	
    
    // Timer and handler for provision wait functionality
	final TimerHandler mTimerHandler = new TimerHandler();
	final Messenger mTimerMessenger = new Messenger(mTimerHandler);
	final int mDelay = 60000;
	

	// CheckTask is a helper utility to poll for the order status and 
	// notify the UI of the progress (ie, display/hide the 'checking'
	// message) without blocking UI updates.
	class CheckTask extends AsyncTask<Context, Integer, Integer> {

		@Override
		protected Integer doInBackground(Context... params) {

        	RemoteOrderingService orderingService = new RemoteOrderingService(null);
    		try{ 
				if (orderingService.queryIsProvisioned(MobileService.this)) {
    				provisionClient();
    			} else {
    				mTimerHandler.sendMessageAtTime(new Message(), SystemClock.uptimeMillis() + mDelay);
    			}

    		} catch (Exception e) {
    			e.printStackTrace();
	    		mTimerHandler.sendMessageAtTime(new Message(), SystemClock.uptimeMillis() + mDelay);
    		}

			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			Log.d(THIS_SERVICE, "End Provision Check");
			notifyClients(ServiceHelper.METHOD_IS_PROVISIONED, ServiceHelper.END_PROVISIONED_CHECK, null);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.d(THIS_SERVICE, "Start Provision Check");
			notifyClients(ServiceHelper.METHOD_IS_PROVISIONED, ServiceHelper.START_PROVISIONED_CHECK, null);
		}

	}

	/** Timer Handler to poll for order status **/
    class TimerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            CheckTask checkTask = new CheckTask();
            checkTask.execute(MobileService.this);
        }
    }	
    
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    
	@Override
	public void onCreate() {
		super.onCreate();
		mState = ServiceHelper.STATE_NOT_RUNNING;
		
		Log.d(THIS_SERVICE, "Creating service");
	}

	@Override
	public void onDestroy() {
		Log.d(THIS_SERVICE, "Destroying service");
		super.onDestroy();
	}

	public MobileService() {
		super("MobileService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		Log.d(THIS_SERVICE, "Executing service request");

		mState = ServiceHelper.STATE_RUNNING;
		sendServiceState();
		
		TelephonyManager telephonyManager;
		int method = intent.getIntExtra(ServiceHelper.METHOD, 0);
		String uuid;
		String packageId;
		String stateId;
		
		RemoteProvisioningService provisioningService;
		RemoteOrderingService orderingService;
		
		try {
			switch (method) {
				case ServiceHelper.METHOD_IS_VERSION_SUPPORTED:
					trackPageView("query_check_client_version");
					
					orderingService = new RemoteOrderingService(null);
					Boolean isSupported = orderingService.queryCheckClientVersion(this);
					notifyClients(method, ServiceHelper.SUCCESS_IS_VERSION_SUPPORTED, isSupported);
					
					break;
					
				case ServiceHelper.METHOD_GET_AUTH_UUID:
					trackPageView("get_auth_uuid");
					
					telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
					
					provisioningService = new RemoteProvisioningService(null);
					uuid = provisioningService.getAuthUuid(
											intent.getStringExtra(ServiceHelper.LOGIN_UID), 
											intent.getStringExtra(ServiceHelper.LOGIN_PWD), 
											telephonyManager.getDeviceId());
					
					VoXMobile.setUuid(this, uuid);
					notifyClients(method, ServiceHelper.SUCCESS_GET_AUTH_UUID, null);
					
					break;
									
				case ServiceHelper.METHOD_GET_SIP_USERS:
					trackPageView("get_sip_users");
					
					provisioningService = new RemoteProvisioningService(null);
					ArrayList<String> sipUsernameList = provisioningService.getSipUsers(
															intent.getStringExtra(ServiceHelper.UUID));
						
					notifyClients(method, ServiceHelper.SUCCESS_GET_SIP_USERS, sipUsernameList);
					
					break;
									
				case ServiceHelper.METHOD_GET_SIP_USER_INFO:
					trackPageView("get_user_info");
					
					provisioningService = new RemoteProvisioningService(null);
					SipUserInfo sipUserInfoList = provisioningService.getSipUserInfo(
															intent.getStringExtra(ServiceHelper.UUID), 
															intent.getStringExtra(ServiceHelper.SIP_USERNAME));
						
					notifyClients(method, ServiceHelper.SUCCESS_GET_SIP_USER_INFO, sipUserInfoList);
					
					break;
					
				case ServiceHelper.METHOD_GET_SERVICE_PLANS:
					trackPageView("get_service_plans");

					orderingService = new RemoteOrderingService(null);
					ArrayList<ServicePlan> planList = orderingService.getServicePlans();
					notifyClients(method, ServiceHelper.SUCCESS_GET_SERVICE_PLANS, planList);
					break;
									
				case ServiceHelper.METHOD_GET_DID_STATES:
					trackPageView("get_did_states");

					uuid = intent.getStringExtra(ServiceHelper.UUID);
					packageId = OrderHelper.getStringValue(this, OrderHelper.PLAN_ID);
					
					orderingService = new RemoteOrderingService(null);
					ArrayList<DidState> stateList = orderingService.getDidStates(packageId);

					notifyClients(method, ServiceHelper.SUCCESS_GET_DID_STATES, stateList);
					break;

				case ServiceHelper.METHOD_GET_DID_CITIES:
					trackPageView("get_did_cities");

					uuid = intent.getStringExtra(ServiceHelper.UUID);
					packageId = OrderHelper.getStringValue(this, OrderHelper.PLAN_ID);
					stateId = OrderHelper.getStringValue(this, OrderHelper.DID_STATE);
					
					orderingService = new RemoteOrderingService(null);
					ArrayList<DidCity> cityList = orderingService.getDidCities(packageId, stateId);

					notifyClients(method, ServiceHelper.SUCCESS_GET_DID_CITIES, cityList);
					break;
														
				case ServiceHelper.METHOD_SUBMIT_ORDER:
					trackPageView("submit_order");
				
					telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

					orderingService = new RemoteOrderingService(null);
					OrderResult result = orderingService.createSimpleMobileServiceOrder(this, telephonyManager.getDeviceId());

					notifyClients(method, ServiceHelper.SUCCESS_SUBMIT_ORDER, result);
					break;
							
				case ServiceHelper.METHOD_IS_PROVISIONED:
					trackPageView("is_provisioned");

		    		mTimerHandler.sendMessageAtTime(new Message(), SystemClock.uptimeMillis());
					break;
					
				default:
					throw new HandlerException("Invalid service request");
			}
			
		} catch (AuthException e) {
			notifyClients(method, ServiceHelper.ERROR_UNAUTHORIZED, null);

		} catch (HandlerException e) {
			e.printStackTrace();
			notifyClients(method, ServiceHelper.ERROR_GENERAL, e.getCause().getMessage());
		} finally {
			if (method != ServiceHelper.METHOD_IS_PROVISIONED)
				mState = ServiceHelper.STATE_NOT_RUNNING;
		}
		
	}

	/** Notify all registered clients that some processing result is available **/
	private void notifyClients(int method, int result, Object obj) {
		
        for (int i = mClients.size()-1; i >= 0; i--) {
        	
            try {
                mClients.get(i).send(Message.obtain(null, ServiceHelper.MSG_SERVICE_RESPONSE, method, result, obj));
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
	}

	/** Notify all registered clients of the current state of this service **/
	private void sendServiceState() {
        for (int i = mClients.size()-1; i >= 0; i--) {
        	
            try {
                mClients.get(i).send(Message.obtain(null, ServiceHelper.MSG_GET_STATE, mState, 0));
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
	}

	private boolean accountExists(String sipUsername)  {
		List<SipProfile> accountsList;
		
		DBAdapter database = new DBAdapter(this);
    	database.open();
		accountsList = database.getListAccounts();
		database.close();
		
		boolean found = false;
		
		Iterator<SipProfile> iList = accountsList.iterator();
		while (iList.hasNext()) {
			SipProfile sp = iList.next();

	        if (!VoXMobile.isVoXMobile(sp.proxies))
	        	continue;
			
	        found = sp.username.equals(sipUsername);
		}

		return found;
	}

	private void provisionClient() {
		int method = ServiceHelper.METHOD_IS_PROVISIONED;
		String uuid = VoXMobile.getUuid(this);
		RemoteProvisioningService provisioningService = new RemoteProvisioningService(null);
		
		try {
			ArrayList<String> sipUsernameList = provisioningService.getSipUsers(uuid);
			SipUserInfo sipUserInfo = provisioningService.getSipUserInfo(uuid, sipUsernameList.get(0));
			
			// sanity check to make sure account doesn't already exist
			if (accountExists(sipUserInfo.mUsername)) {
				mState = ServiceHelper.STATE_NOT_RUNNING;	
				return;
			}
			
			VoXMobile wizard = new VoXMobile();
			SipProfile account = new SipProfile();
			
			wizard.buildAccount(account, sipUserInfo);
			DBAdapter database = new DBAdapter(this);
			database.open();

			account.id = (int) database.insertAccount(account);
			account.active = true;
			List<Filter> filters = wizard.getDefaultFilters(account);
			if(filters != null && filters.size() > 0 ) {
				for(Filter filter : filters) {
					// Ensure the correct id if not done by the wizard
					filter.account = account.id;
					database.insertFilter(filter);
				}
			}
			database.setAccountActive(account.id, account.active);
			database.close();

			OrderHelper.clear(this);

			Intent publishIntent = new Intent(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED);
			publishIntent.putExtra(SipManager.EXTRA_ACCOUNT_ID, account.id);
			publishIntent.putExtra(SipManager.EXTRA_ACTIVATE, account.active);
			this.sendBroadcast(publishIntent);
			
			notifyClients(method, ServiceHelper.SUCCESS_IS_PROVISIONED, null);
			mState = ServiceHelper.STATE_NOT_RUNNING;
			
		} catch (AuthException e) {
			notifyClients(method, ServiceHelper.ERROR_UNAUTHORIZED, null);

		} catch (HandlerException e) {
			e.printStackTrace();
			notifyClients(method, ServiceHelper.ERROR_GENERAL, e.getMessage());
		} finally {
			mState = ServiceHelper.STATE_NOT_RUNNING;
		}
		
	}
}
