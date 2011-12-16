/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.util.Log;

import com.csipsimple.voxmobile.exception.AuthException;
import com.csipsimple.voxmobile.exception.HandlerException;
import com.csipsimple.voxmobile.io.transport.HttpsTransportSE;
import com.csipsimple.voxmobile.types.SipUserInfo;

public class RemoteProvisioningService {
	
	/** Log identifier **/
	private static final String THIS_SERVICE = "Remote Mobile Svc";
	
	private final static int mMode = ServiceHelper.MODE_ACTIVE;
		
	private final String NAMESPACE = "namespace";
	private final String SERVICE = "service";
	private final String SOAP_ACTION = "action";

	private HttpsTransportSE mSecureTransport = null;
	private HttpTransportSE mTransport = null;

	public RemoteProvisioningService(ContentResolver resolver) {
		
		super();
		
		// This is required to prevent problems that Android has with
		// HTTPS keep-alive noted at:
		// http://code.google.com/p/ksoap2-android/issues/detail?id=35
		System.setProperty("http.keepAlive", "false");
	}

	private HttpTransportSE getTransport() {
		HttpTransportSE transport = null;
		
		String host = ServiceHelper.getWebserviceHost();
		
		switch (ServiceHelper.MODE_ACTIVE) {
			case ServiceHelper.MODE_PRODUCTION:
			case ServiceHelper.MODE_STAGE:
				if (mSecureTransport == null)
					mSecureTransport = new HttpsTransportSE(host, 443, SERVICE, 5000);
				
				transport = mSecureTransport;
				break;
				
			default:
				if (mTransport == null)
					mTransport = new HttpTransportSE(host + "url");
				transport = mTransport;				
		}
		return transport;
	}

	private void checkSoapFault(SoapEnvelope envelope) throws SoapFault {
		if (envelope.bodyIn.getClass().equals(SoapFault.class)) {
			throw (SoapFault) envelope.bodyIn;
		}
	}
		
	protected void handleException(Exception e) throws AuthException, HandlerException {
		
	    if (e.getClass().equals(SoapFault.class)) {
	    	SoapFault sf = (SoapFault)e;
	    	Log.d(THIS_SERVICE + " Fault", sf.faultcode + ": " + sf.faultstring);
		
			if ("Unauthorized".equalsIgnoreCase(sf.faultstring)) {
				throw new AuthException("Authentication Error", e);
			} else {
				throw new HandlerException("Service Error", e);
			}

	    } else if (e.getClass().equals(XmlPullParserException.class)) {
	    	e.printStackTrace();
            throw new HandlerException("Malformed server response", e);

	    } else if (e.getClass().equals(IOException.class)) {
	    	e.printStackTrace();
	    	throw new HandlerException("Error reading server response", e);
	    }
		
	}
	
	public String getAuthUuid(String username, String password, String imei) throws AuthException, HandlerException {
		
		SoapObject request = new SoapObject(NAMESPACE, "action");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
			
		request.addProperty(ServiceHelper.SIP_USERNAME, username);
		request.addProperty(ServiceHelper.SIP_PASSWORD, password);
		request.addProperty(ServiceHelper.IMEI, imei);
		
		String uuid = null;
		
		try {
			getTransport().call(SOAP_ACTION, envelope);
			checkSoapFault(envelope); 
			
			SoapObject result = (SoapObject) envelope.bodyIn;
			uuid = result.getPropertyAsString(0);
			Log.d(THIS_SERVICE, "getAuthUuid: " + uuid);
		} catch (Exception e) {
			handleException(e);			
		}			
		
		return uuid;
	}

	public ArrayList<String> getSipUsers(String uuid) throws AuthException, HandlerException {
		
		SoapObject request = new SoapObject(NAMESPACE, "action");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
			
		request.addProperty(ServiceHelper.UUID, uuid);

		ArrayList<String> list = new ArrayList<String>();
		
		try {
			getTransport().call(SOAP_ACTION, envelope);
			checkSoapFault(envelope); 
			
			SoapObject soap = (SoapObject) envelope.bodyIn;

			SoapObject result = (SoapObject) soap.getProperty(0);			
			
	        for (int i = 0; i < result.getPropertyCount(); i++)
	            list.add(result.getPropertyAsString(i));
	        
	        Collections.sort(list);
            
			Log.d(THIS_SERVICE, "getSipUsers: " + list.toString());
		} catch (Exception e) {
			handleException(e);			
		}			
		
		return list;
	}
	
	public SipUserInfo getSipUserInfo(String uuid, String sipUsername) throws AuthException, HandlerException {
		
		SoapObject request = new SoapObject(NAMESPACE, "action");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
			
		request.addProperty(ServiceHelper.UUID, uuid);
		request.addProperty(ServiceHelper.SIP_USERNAME, sipUsername);

		SipUserInfo info = new SipUserInfo();
		
		try {
			getTransport().call(SOAP_ACTION, envelope);
			checkSoapFault(envelope); 
			
			SoapObject soap = (SoapObject) envelope.bodyIn;

			SoapObject result = (SoapObject) soap.getProperty(0);			
			
	        info.mUsername = result.getPropertyAsString("value");
	        info.mPassword = result.getPropertyAsString("value");
	        info.mDisplayName = result.getPropertyAsString("value");
	        
			Log.d(THIS_SERVICE, "getSipUserInfo: " + result.toString());
		} catch (Exception e) {
			handleException(e);			
		}			
		
		return info;
	}
	
}
