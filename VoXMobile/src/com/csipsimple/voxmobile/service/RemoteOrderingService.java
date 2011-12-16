/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.service;

import java.io.IOException;
import java.util.ArrayList;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.csipsimple.voxmobile.exception.AuthException;
import com.csipsimple.voxmobile.exception.HandlerException;
import com.csipsimple.voxmobile.io.transport.HttpsTransportSE;
import com.csipsimple.voxmobile.types.DidCity;
import com.csipsimple.voxmobile.types.DidState;
import com.csipsimple.voxmobile.types.OrderResult;
import com.csipsimple.voxmobile.types.ServicePlan;
import com.csipsimple.voxmobile.utils.OrderHelper;
import com.csipsimple.wizards.impl.VoXMobile;

public class RemoteOrderingService {
	
	/** Log identifier **/
	private static final String THIS_SERVICE = "Remote Order Svc";
	
	private final String NAMESPACE = "namespace";
	private final String SERVICE = "service";
	private final String SOAP_ACTION = "action";

	private HttpsTransportSE mSecureTransport = null;
	private HttpTransportSE mTransport = null;

	public RemoteOrderingService(ContentResolver resolver) {
		
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
	    	Log.d(THIS_SERVICE + " fault", sf.faultcode + ": " + sf.faultstring);
		
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
	
	public ArrayList<ServicePlan> getServicePlans() throws AuthException, HandlerException {
		
		SoapObject request = new SoapObject(NAMESPACE, "action");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
			
		request.addProperty(ServiceHelper.ORDER_SERVICE_KEY, ServiceHelper.ORDER_SERVICE_KEY_ID);

		ArrayList<ServicePlan> list = new ArrayList<ServicePlan>();
		
		try {
			getTransport().call(SOAP_ACTION, envelope);
			checkSoapFault(envelope); 
			
			SoapObject soap = (SoapObject) envelope.bodyIn;

			SoapObject result = (SoapObject) soap.getProperty(0);
			
			// iterate all plans 
	        for (int i = 0; i < result.getPropertyCount(); i++) {
	            SoapObject plan = (SoapObject) result.getProperty(i);

	            SoapObject charges = (SoapObject) plan.getProperty("value");
	            Double totalPrice = Double.valueOf(plan.getPropertyAsString("value"));
	            String planId = plan.getPropertyAsString("value");
	            String description = String.format(fixNewLines(plan.getPropertyAsString("value")), totalPrice.toString());
	            String title = plan.getPropertyAsString("value");
	            
	            ServicePlan svcPlan = new ServicePlan(totalPrice, 
	            									  planId, 
	            									  description, 
	            									  title);
	            for (int j = 0; j < charges.getPropertyCount(); j++) {
	            	SoapObject charge = (SoapObject) charges.getProperty(j);
	            	
	            	Double price = Double.valueOf(charge.getPropertyAsString("value"));
	            	String recurring = charge.getPropertyAsString("value");
	            	String chargeDescr = charge.getPropertyAsString("value");
	            	
	            	svcPlan.addCharge(Double.valueOf(price), 
	            				 	  Boolean.parseBoolean(recurring), 
	            				 	  chargeDescr);
	            }
	            list.add(svcPlan);
	        }
	        
			Log.d(THIS_SERVICE, "getServicePlans: " + list.toString());
		} catch (Exception e) {
			handleException(e);			
		}			
		
		return list;
	}

	public ArrayList<DidState> getDidStates(String packageId) throws AuthException, HandlerException {
		
		SoapObject request = new SoapObject(NAMESPACE, "action");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
			
		request.addProperty(ServiceHelper.ORDER_SERVICE_KEY, ServiceHelper.ORDER_SERVICE_KEY_ID);
		request.addProperty(ServiceHelper.PACKAGE, packageId);

		ArrayList<DidState> list = new ArrayList<DidState>();
		
		try {
			getTransport().call(SOAP_ACTION, envelope);
			checkSoapFault(envelope); 
			
			SoapObject soap = (SoapObject) envelope.bodyIn;

			SoapObject result = (SoapObject) soap.getProperty(0);
			
			// iterate all states 
	        for (int i = 0; i < result.getPropertyCount(); i++) {
	            SoapObject state = (SoapObject) result.getProperty(i);

	            String stateId = state.getPropertyAsString("value");
	            int count = Integer.valueOf(state.getPropertyAsString("value"));
	            String description = state.getPropertyAsString("value");
	            
	            list.add(new DidState(stateId, count, description));
	        }
	        
			Log.d(THIS_SERVICE, "getDidStates: " + list.toString());
		} catch (Exception e) {
			handleException(e);			
		}			
		
		return list;
	}

	public ArrayList<DidCity> getDidCities(String packageId, String stateId) throws AuthException, HandlerException {
		
		SoapObject request = new SoapObject(NAMESPACE, "action");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
			
		request.addProperty(ServiceHelper.ORDER_SERVICE_KEY, ServiceHelper.ORDER_SERVICE_KEY_ID);
		request.addProperty(ServiceHelper.PACKAGE, packageId);
		request.addProperty(ServiceHelper.DID_STATE, stateId);

		ArrayList<DidCity> list = new ArrayList<DidCity>();
		
		try {
			getTransport().call(SOAP_ACTION, envelope);
			checkSoapFault(envelope); 
			
			SoapObject soap = (SoapObject) envelope.bodyIn;

			SoapObject result = (SoapObject) soap.getProperty(0);
			
			// iterate all states 
	        for (int i = 0; i < result.getPropertyCount(); i++) {
	            SoapObject city = (SoapObject) result.getProperty(i);

	            String cityId = city.getPropertyAsString("value");
	            int count = Integer.valueOf(city.getPropertyAsString("value"));
	            String description = city.getPropertyAsString("value");
	            
	            list.add(new DidCity(cityId, count, description));
	        }
	        
			Log.d(THIS_SERVICE, "getDidCities: " + list.toString());
		} catch (Exception e) {
			handleException(e);			
		}			
		
		return list;
	}
		
	public OrderResult createSimpleMobileServiceOrder(Context context, String imei) throws AuthException, HandlerException {
		
		SoapObject request = new SoapObject(NAMESPACE, "action");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);
		
		SoapObject order = new SoapObject(NAMESPACE, "action");
		order.addProperty(OrderHelper.PLAN_ID, OrderHelper.getStringValue(context, OrderHelper.PLAN_ID));
		order.addProperty(OrderHelper.IMEI, imei);
		order.addProperty(OrderHelper.FIRST_NAME, OrderHelper.getStringValue(context, OrderHelper.FIRST_NAME));
		order.addProperty(OrderHelper.LAST_NAME, OrderHelper.getStringValue(context, OrderHelper.LAST_NAME));
		order.addProperty(OrderHelper.EMAIL, OrderHelper.getStringValue(context, OrderHelper.EMAIL));
		
		order.addProperty(OrderHelper.DID_CITY, OrderHelper.getStringValue(context, OrderHelper.DID_CITY));
		order.addProperty(OrderHelper.DID_STATE, OrderHelper.getStringValue(context, OrderHelper.DID_STATE));

		if (OrderHelper.getStringValue(context, OrderHelper.DID_STATE) == ServiceHelper.VOXLAND_STATE) {
			order.addProperty(OrderHelper.CC_NUMBER, "");
			order.addProperty(OrderHelper.CC_CVV, "");
			order.addProperty(OrderHelper.CC_EXP_MONTH, "");
			order.addProperty(OrderHelper.CC_EXP_YEAR, "");
			order.addProperty(OrderHelper.BILLING_COUNTRY, "");
			order.addProperty(OrderHelper.BILLING_CITY, "");
			order.addProperty(OrderHelper.BILLING_STREET, "");
			order.addProperty(OrderHelper.BILLING_POSTAL_CODE, "");
		} else {
			order.addProperty(OrderHelper.CC_NUMBER, OrderHelper.getStringValue(context, OrderHelper.CC_NUMBER));
			order.addProperty(OrderHelper.CC_CVV, OrderHelper.getStringValue(context, OrderHelper.CC_CVV));
			order.addProperty(OrderHelper.CC_EXP_MONTH, OrderHelper.getIntValue(context, OrderHelper.CC_EXP_MONTH, 0));
			order.addProperty(OrderHelper.CC_EXP_YEAR, OrderHelper.getSelectedYear(context));
			order.addProperty(OrderHelper.BILLING_COUNTRY, OrderHelper.getStringValue(context, OrderHelper.BILLING_COUNTRY));
			order.addProperty(OrderHelper.BILLING_CITY, OrderHelper.getStringValue(context, OrderHelper.BILLING_CITY));
			order.addProperty(OrderHelper.BILLING_STREET, OrderHelper.getStringValue(context, OrderHelper.BILLING_STREET));
			order.addProperty(OrderHelper.BILLING_POSTAL_CODE, OrderHelper.getStringValue(context, OrderHelper.BILLING_POSTAL_CODE));
		}
		
		request.addProperty(ServiceHelper.ORDER_SERVICE_KEY, ServiceHelper.ORDER_SERVICE_KEY_ID);
		request.addSoapObject(order);

		OrderResult result = new OrderResult();
		
		try {
			getTransport().call(SOAP_ACTION, envelope);
			checkSoapFault(envelope); 
			
			SoapObject soap = (SoapObject) envelope.bodyIn;

			SoapObject response = (SoapObject) soap.getProperty(0);
			
            result.mSuccess = Boolean.parseBoolean(response.getPropertyAsString("value"));
            result.mFailureType = response.getPropertyAsString("value");
            result.mAuthUuid = response.getPropertyAsString("value");
            result.mResultString = response.getPropertyAsString("value");
            result.mLoginName = response.getPropertyAsString("value");
            result.mLoginPassword = response.getPropertyAsString("value");
            try {
            	result.mChargeAmount = Double.parseDouble(response.getPropertyAsString("value"));
            } catch (Exception e) {
            	result.mChargeAmount = 0.00;
            }
            result.mChargeAuthCode = response.getPropertyAsString("value");
	        
			Log.d(THIS_SERVICE, "createSimpleMobileServiceOrder: " + response.toString());
		} catch (Exception e) {
			handleException(e);			
		}			
		
		return result;
	}
	
	public boolean queryIsProvisioned(Context context) throws AuthException, HandlerException {
		
		SoapObject request = new SoapObject(NAMESPACE, "action");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);

		String uuid = VoXMobile.getUuid(context);
		request.addProperty(ServiceHelper.AUTH_UUID, uuid);
		
		boolean isProvisioned = false;
		
		try {
			getTransport().call(SOAP_ACTION, envelope);
			checkSoapFault(envelope); 
			
			SoapObject result = (SoapObject) envelope.bodyIn;
            isProvisioned = Boolean.parseBoolean(result.getPropertyAsString(0));
	        
			Log.d(THIS_SERVICE, "queryIsProvisioned: " + Boolean.toString(isProvisioned));
		} catch (Exception e) {
            isProvisioned = false;
		}			
		
		return isProvisioned;
	}

	public boolean queryCheckClientVersion(Context context) throws HandlerException {
		
		SoapObject request = new SoapObject(NAMESPACE, "action");
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);

		int versionCode = 0;
		try {
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			versionCode = pinfo.versionCode;
		} catch (NameNotFoundException e1) {
		}
		request.addProperty(ServiceHelper.ORDER_SERVICE_KEY, ServiceHelper.ORDER_SERVICE_KEY_ID);
		request.addProperty(ServiceHelper.REQUESTED_VERSION, versionCode);
		
		boolean isSupported = false;
		
		try {
			getTransport().call(SOAP_ACTION, envelope);
			checkSoapFault(envelope); 
			
			SoapObject result = (SoapObject) envelope.bodyIn;
			isSupported = Boolean.parseBoolean(result.getPropertyAsString(0));
	        
			Log.d(THIS_SERVICE, "queryCheckClientVersion: " + Boolean.toString(isSupported));
		} catch (Exception e) {
			isSupported = false;
		}			
		
		return isSupported;
	}
	
	/**
	 * This utility function is needed because the SOAP server returns
	 * strings with "\n" in them, which are not interpreted by the compiler
	 * as a new line character because this happens at runtime. Therefore,
	 * we need to convert them to new line characters manually.
	 * 
	 * @param str
	 * @return String with all literal "\n" converted to (char)(10).
	 */
	private String fixNewLines(String str) {
		
		final byte SLASH = 92;
		byte[] b = str.getBytes();
		
		// Replace all "\" with (char)10 and replace the following "n"
		// with a <SPACE>, which will then be trimmed in the next section.
		for (int i = 0; i < b.length; i++) {
			if ((b[i] == SLASH) && (b[i + 1] == "n".getBytes()[0])) {
				b[i] = (char)(10);
				b[i+1] = ' ';
				i++;
			}
		}
		
		// Split the string on the newly inserted "\n" character
		String[] s = new String(b).split("\n");
		
		// Reassemble the parts, trimming whitespace
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length; i++) {
			sb.append(s[i].trim());
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
}
