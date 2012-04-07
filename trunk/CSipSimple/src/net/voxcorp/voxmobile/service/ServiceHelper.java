package net.voxcorp.voxmobile.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.voxcorp.utils.Log;
import net.voxcorp.voxmobile.http.EasyHttpClient;
import net.voxcorp.voxmobile.types.Account;
import net.voxcorp.voxmobile.types.AccountSummary;
import net.voxcorp.voxmobile.types.DIDCities;
import net.voxcorp.voxmobile.types.DIDCity;
import net.voxcorp.voxmobile.types.DIDStates;
import net.voxcorp.voxmobile.types.OrderResult;
import net.voxcorp.voxmobile.types.Plan;
import net.voxcorp.voxmobile.types.Plans;
import net.voxcorp.voxmobile.types.ProvisionCheck;
import net.voxcorp.voxmobile.types.RateCountries;
import net.voxcorp.voxmobile.types.RateDialCodes;
import net.voxcorp.voxmobile.types.RatesVersionCheck;
import net.voxcorp.voxmobile.types.SimpleReply;
import net.voxcorp.voxmobile.types.SipUsers;
import net.voxcorp.voxmobile.types.TrialDialCodes;
import net.voxcorp.voxmobile.types.VersionCheck;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.OrderHelper;
import net.voxcorp.voxmobile.utils.VoXSettings;

import org.apache.http.client.HttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.springframework.http.ContentCodingType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ServiceHelper {

	static String THIS_FILE = "VoXMobile REST";

	private GoogleAnalyticsTracker mTracker;
	private Context mContext;

	public ServiceHelper(Context context) {
		super();
		mContext = context;

		mTracker = GoogleAnalyticsTracker.getInstance();
		mTracker.start(VoXSettings.getGoogleAnalyticsAccount(),
				Consts.GOOGLE_ANALYTICS_DISPATCH_INTERVAL,
				mContext);
		mTracker.setDebug(Consts.GOOGLE_ANALYTICS_DEBUG);
		mTracker.setDryRun(Consts.GOOGLE_ANALYTICS_DRY_RUN);
	}

	private String getUniqueDeviceId() {
		// Not all devices will return an IMEI so for those running
		// Gingerbread or newer (API level >= 9) we can grab the
		// android BUILD - otherwise we're going to return NULL and
		// the user of the device will have no access to VoX Mobile.
		TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		String uniqueId = telephonyManager.getDeviceId();
		if (uniqueId == null && Build.VERSION.SDK_INT >= 9) {
			uniqueId = Build.SERIAL;
			if (uniqueId.equalsIgnoreCase(Build.UNKNOWN)) {
				uniqueId = null;
			}
		}
		return uniqueId;
	}

	private void trackPageView(String page) {
		mTracker.trackPageView("/android/csipsimple/webservice/" + page);
	}

	public class MyCommonsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

		@Override
		public HttpClient getHttpClient() {
			// Disable SSL cert stuff because our stage cert
			// is not valid and our production cert root CA
			// is not a trusted CA and thus also invalid.
	        HttpParams httpParameters = new BasicHttpParams();

	        int timeoutConnection = 10000;
	        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);

	        int timeoutSocket = 10000;
	        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

	        return new EasyHttpClient(httpParameters);
		}
	}

	public DIDCities getDIDCities(String stateId) {	
		String requestURL = "rest/order/did";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				stateId,
				Consts.ORDER_SERVICE_KEY_ID);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		DIDCities cities;
		ResponseEntity<DIDCities> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, DIDCities.class);
		cities = response.getBody();
		for (DIDCity city : cities.cities) {
			city.state_id = stateId;
		}

		cities.httpStatus = HttpStatus.OK.value();
		return cities;
	}

	public DIDStates getDIDStates() {	
		String requestURL = "rest/order/did/states";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				Consts.ORDER_SERVICE_KEY_ID);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		DIDStates states;
		ResponseEntity<DIDStates> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, DIDStates.class);
		states = response.getBody();
		states.httpStatus = HttpStatus.OK.value();
		return states;
	}

	public Plans getPlans() {	
		String requestURL = "rest/order/plans";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				Consts.ORDER_SERVICE_KEY_ID);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		Plans plans;
		ResponseEntity<Plans> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Plans.class);
		plans = response.getBody();

		formatPlans(plans);

		plans.httpStatus = HttpStatus.OK.value();
		return plans;
	}

	private void formatPlans(Plans plans) {
		for (Plan plan : plans.plans) {
			plan.description = String.format(fixNewLines(plan.description), plan.total_price);
		}
	}

    /**
     * This utility function is needed because the SOAP server returns
     * strings with "\n" in them, which are not interpreted by the compiler
     * as a new line character because this happens at runtime. Therefore,
     * we need to convert them to new line characters manually.
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

    public SipUsers getSipUsers(String uuid) {

		String requestURL = "rest/account/users";
		trackPageView(requestURL);

		Log.d(THIS_FILE, "REST method: " + requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				uuid);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		SipUsers sipUsers;
		ResponseEntity<SipUsers> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, SipUsers.class);
		sipUsers = response.getBody();
		sipUsers.httpStatus = HttpStatus.OK.value();
		return sipUsers;
	}

	public Account uuidLogin(String uuid) {
		String requestURL = "rest/account/auth/uuid_login";
		trackPageView(requestURL);

		Log.d(THIS_FILE, "REST method: " + requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				uuid);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		Account account;
		ResponseEntity<Account> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Account.class);
		account = response.getBody();
		account.httpStatus = HttpStatus.OK.value();
		return account;
	}

	public Account login(String username, String password) {
		String imei = getUniqueDeviceId();

		String requestURL = "rest/account/auth/login";
		trackPageView(requestURL);

		Log.d(THIS_FILE, "REST method: " + requestURL);

		String url = String.format("%s/%s/%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				imei,
				username,
				password);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		Account account;
		ResponseEntity<Account> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Account.class);
		account = response.getBody();
		account.httpStatus = HttpStatus.OK.value();
		return account;
	}

	public SimpleReply logout(String uuid) {
		String imei = getUniqueDeviceId();

		String requestURL = "rest/account/auth/logout";
		trackPageView(requestURL);

		Log.d(THIS_FILE, "REST method: " + requestURL);

		String url = String.format("%s/%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				uuid,
				imei);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		SimpleReply reply;
		ResponseEntity<SimpleReply> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, SimpleReply.class);
		reply = response.getBody();
		reply.httpStatus = HttpStatus.OK.value();
		return reply;
	}

	public ProvisionCheck provisionCheck(String uuid) {
		String requestURL = "rest/order/check/status";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				uuid);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		ProvisionCheck check;
		ResponseEntity<ProvisionCheck> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, ProvisionCheck.class);
		check = response.getBody();
		check.httpStatus = HttpStatus.OK.value();
		return check;
	}

	public AccountSummary accountSummary(String uuid) {
		String requestURL = "rest/account/summary";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				uuid);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		AccountSummary summary;
		ResponseEntity<AccountSummary> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, AccountSummary.class);
		summary = response.getBody();
		summary.httpStatus = HttpStatus.OK.value();
		return summary;
	}
	
	public VersionCheck checkVersion() {	
		String requestURL = "rest/version/check";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		int versionCode = 0;
		try {
			PackageInfo pinfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
			versionCode = pinfo.versionCode;
		} catch (NameNotFoundException e1) {
			versionCode = 0;
		}

		String url = String.format("%s/%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				Consts.ORDER_SERVICE_KEY_ID,
				Integer.toString(versionCode));
		
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		VersionCheck check;
		ResponseEntity<VersionCheck> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, VersionCheck.class);
		check = response.getBody();
		check.httpStatus = HttpStatus.OK.value();
		return check;
	}

	public RatesVersionCheck ratesCheckVersion() {	
		String requestURL = "rest/rates/version";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				Consts.ORDER_SERVICE_KEY_ID);
		
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		RatesVersionCheck check;
		ResponseEntity<RatesVersionCheck> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, RatesVersionCheck.class);
		check = response.getBody();
		check.httpStatus = HttpStatus.OK.value();
		return check;
	}
	
	public RateCountries rateCountries() {	
		String requestURL = "rest/rates";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				Consts.ORDER_SERVICE_KEY_ID);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		RateCountries countries;
		try {
			ResponseEntity<RateCountries> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, RateCountries.class);
			countries = response.getBody();
			countries.httpStatus = HttpStatus.OK.value();
		} catch (HttpStatusCodeException e) {
			Log.d(THIS_FILE, e.getMessage());

			countries = new RateCountries();
			countries.success = false;
			countries.httpStatus = e.getStatusCode().value();
		}
		return countries;
	}

	public RateDialCodes rateDialCodes(int countryId) {	
		String requestURL = "rest/rates";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%d/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				countryId,
				Consts.ORDER_SERVICE_KEY_ID);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		RateDialCodes dialCodes;
		try {
			ResponseEntity<RateDialCodes> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, RateDialCodes.class);
			dialCodes = response.getBody();
			dialCodes.httpStatus = HttpStatus.OK.value();
		} catch (HttpStatusCodeException e) {
			Log.d(THIS_FILE, e.getMessage());
			
			dialCodes = new RateDialCodes();
			dialCodes.success = false;
			dialCodes.httpStatus = e.getStatusCode().value();
		}
		return dialCodes;
	}

	public TrialDialCodes trialDialCodes() {	
		String requestURL = "rest/trial/dialcodes";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				Consts.ORDER_SERVICE_KEY_ID);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));

		HttpEntity<?> requestEntity = new HttpEntity<Object>(requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		TrialDialCodes dialCodes;
		try {
			ResponseEntity<TrialDialCodes> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, TrialDialCodes.class);
			dialCodes = response.getBody();
			dialCodes.httpStatus = HttpStatus.OK.value();
		} catch (HttpStatusCodeException e) {
			Log.d(THIS_FILE, e.getMessage());

			dialCodes = new TrialDialCodes();
			dialCodes.success = false;
			dialCodes.httpStatus = e.getStatusCode().value();
		}
		return dialCodes;
	}

	public OrderResult submitOrder() {
		String requestURL = "rest/order/submit";
		Log.d(THIS_FILE, "REST method: " + requestURL);
		trackPageView(requestURL);

		String url = String.format("%s/%s/%s",
				VoXSettings.getWebserviceHost(),
				requestURL,
				Consts.ORDER_SERVICE_KEY_ID);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAcceptEncoding(Collections.singletonList(ContentCodingType.GZIP));
		requestHeaders.setContentType(MediaType.APPLICATION_JSON);
		
		Map<String, Object> uriArguments = createOrderRequest();

		HttpEntity<?> requestEntity = new HttpEntity<Object>(uriArguments, requestHeaders);
		RestTemplate restTemplate = new RestTemplate(new MyCommonsClientHttpRequestFactory());

		OrderResult result;
		ResponseEntity<OrderResult> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, OrderResult.class);
		result = response.getBody();
		result.httpStatus = HttpStatus.OK.value();
		return result;
	}

	private Map<String, Object> createOrderRequest() {
		String imei = getUniqueDeviceId();

		HashMap<String, Object> map = new HashMap<String, Object>();

		map.put(OrderHelper.AGENT_CODE, OrderHelper.getStringValue(mContext, OrderHelper.AGENT_CODE_VALUE));
		map.put(OrderHelper.PLAN_ID, OrderHelper.getStringValue(mContext, OrderHelper.PLAN_ID));
		map.put(OrderHelper.IMEI, imei);
		map.put(OrderHelper.FIRST_NAME, OrderHelper.getStringValue(mContext, OrderHelper.FIRST_NAME));
		map.put(OrderHelper.LAST_NAME, OrderHelper.getStringValue(mContext, OrderHelper.LAST_NAME));
		map.put(OrderHelper.EMAIL, OrderHelper.getStringValue(mContext, OrderHelper.EMAIL));
		map.put(OrderHelper.DID_CITY, OrderHelper.getStringValue(mContext, OrderHelper.DID_CITY));
		map.put(OrderHelper.DID_STATE, OrderHelper.getStringValue(mContext, OrderHelper.DID_STATE));

		if (OrderHelper.getStringValue(mContext, OrderHelper.DID_STATE) == Consts.VOXLAND_STATE) {
			map.put(OrderHelper.CC_NUMBER, "");
			map.put(OrderHelper.CC_CVV, "");
			map.put(OrderHelper.CC_EXP_MONTH, "");
			map.put(OrderHelper.CC_EXP_YEAR, "");
			map.put(OrderHelper.BILLING_COUNTRY, "");
			map.put(OrderHelper.BILLING_CITY, "");
			map.put(OrderHelper.BILLING_STREET, "");
			map.put(OrderHelper.BILLING_POSTAL_CODE, "");
		} else {
			map.put(OrderHelper.CC_NUMBER, OrderHelper.getStringValue(mContext, OrderHelper.CC_NUMBER));
			map.put(OrderHelper.CC_CVV, OrderHelper.getStringValue(mContext, OrderHelper.CC_CVV));
			map.put(OrderHelper.CC_EXP_MONTH, OrderHelper.getIntValue(mContext, OrderHelper.CC_EXP_MONTH, 0));
			map.put(OrderHelper.CC_EXP_YEAR, OrderHelper.getSelectedYear(mContext));
			map.put(OrderHelper.BILLING_COUNTRY, OrderHelper.getStringValue(mContext, OrderHelper.BILLING_COUNTRY));
			map.put(OrderHelper.BILLING_CITY, OrderHelper.getStringValue(mContext, OrderHelper.BILLING_CITY));
			map.put(OrderHelper.BILLING_STREET, OrderHelper.getStringValue(mContext, OrderHelper.BILLING_STREET));
			map.put(OrderHelper.BILLING_POSTAL_CODE, OrderHelper.getStringValue(mContext, OrderHelper.BILLING_POSTAL_CODE));
		}
		map.put(Consts.ORDER_SERVICE_KEY, Consts.ORDER_SERVICE_KEY_ID);
		return map;
	}

}
