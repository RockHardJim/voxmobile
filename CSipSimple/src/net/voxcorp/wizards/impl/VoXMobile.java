package net.voxcorp.wizards.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;
import android.preference.EditTextPreference;

import net.voxcorp.R;
import net.voxcorp.api.SipConfigManager;
import net.voxcorp.api.SipProfile;
import net.voxcorp.models.Filter;
import net.voxcorp.utils.CustomDistribution;
import net.voxcorp.utils.PreferencesWrapper;
import net.voxcorp.voxmobile.types.SipUser;
import net.voxcorp.voxmobile.utils.Consts;
import net.voxcorp.voxmobile.utils.SimpleCrypto;
import net.voxcorp.voxmobile.utils.VoXMobileSettings;
import net.voxcorp.voxmobile.utils.VoXMobileUtils;

public class VoXMobile extends BaseImplementation {

	public static final int ACCOUNT_CREATED = 2000;
	
	protected static String DISPLAY_NAME = "display_name";
	protected static String USER_NAME = "username";

	private EditTextPreference accountDisplayName;
	private EditTextPreference accountUserName;
	
	private void bindFields() {
		accountDisplayName = (EditTextPreference) findPreference(SipProfile.FIELD_DISPLAY_NAME);
		accountUserName = (EditTextPreference) findPreference(SipProfile.FIELD_USERNAME);
	}
	
	public enum VoXAccountType {
		PAYGO,
		PREPAID,
		STANDARD,
		TRIAL,
		UNKNOWN
	}

	public static VoXAccountType getAccountType(String wizardName) {
		if ("VOXMOBILE-PAYGO".equalsIgnoreCase(wizardName)) {
			return VoXAccountType.PAYGO;
		} else if ("VOXMOBILE-PREPAID".equalsIgnoreCase(wizardName)) {
			return VoXAccountType.PREPAID;
		} else if ("VOXMOBILE-TRIAL".equalsIgnoreCase(wizardName)) {
			return VoXAccountType.TRIAL;
		} else if ("VOXMOBILE".equalsIgnoreCase(wizardName)) {
			return VoXAccountType.STANDARD;
		} else {
			return VoXAccountType.UNKNOWN;
		}
	}

	public static boolean isVoXMobile(String[] proxies) {
		if (proxies == null) {
			return false;
		}

		boolean result = false;
		for (int i = 0; i < proxies.length; i++) {
			if (proxies[i].toLowerCase().contains("voxcorp.net")) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	@Override
	public void fillLayout(final SipProfile account) {
		bindFields();

		accountDisplayName.setText(account.display_name);
		accountUserName.setText(account.username);
	}

	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>() {

		private static final long serialVersionUID = 1L;

	{
		put(DISPLAY_NAME, R.string.w_common_display_name_desc);
		put(USER_NAME, R.string.w_common_phone_number);
	}};
	
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		Integer res = SUMMARIES.get(fieldName);
		if(res != null) {
			return parent.getString( res );
		}
		return "";
	}

	@Override
	public void updateDescriptions() {
		setStringFieldSummary(DISPLAY_NAME);
		setStringFieldSummary(USER_NAME);
	}

	public boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountUserName, isEmpty(accountUserName));

		return isValid;
	}
	
	public String getDomain() {
		if (VoXMobileSettings.getMode() == Consts.MODE_PRODUCTION)
			return "sip.voxcorp.net";
		else
			return "sipstage.voxcorp.net";
	}
	
	protected String getDefaultName() {
		return "VoX Mobile";
	}
	
	private SipProfile updateAccount(SipProfile account, String displayName) {
		account.display_name = displayName;
		return account;
	}
	
	private SipProfile createAccount(SipProfile account, String accountType,
			String sipUid, String sipPwd, String displayName) {
		
		account.id = SipProfile.INVALID_ID;
		account.display_name = VoXMobileUtils.formatPhoneNumber(sipUid);

		account.acc_id = "<sip:" + Uri.encode(sipUid) + "@"+getDomain()+">";
		
		String regUri = "sip:"+getDomain();
		account.reg_uri = regUri;
		account.proxies = new String[] { "sip:" + getDomain() } ;
		
		account.realm = "*";
		account.username = sipUid;
		account.data = sipPwd;
		account.scheme = SipProfile.CRED_SCHEME_DIGEST;
		account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;

		account.reg_timeout = 900;	
		account.transport = SipProfile.TRANSPORT_UDP;
		account.vm_nbr = sipUid;

		if (accountType.equalsIgnoreCase("PAYGO")) {
			account.wizard = "VOXMOBILE-PAYGO";
		} else if (accountType.equalsIgnoreCase("PREPAID")) {
			account.wizard = "VOXMOBILE-PREPAID";
		} else if (accountType.equalsIgnoreCase("TRIAL")) {
			account.wizard = "VOXMOBILE-TRIAL";
		} else {
			account.wizard = "VOXMOBILE";
		}
		
		return account;
	}

	@Override
	public SipProfile buildAccount(SipProfile account) {
		return updateAccount(account, accountDisplayName.getText());
	}
	
	public SipProfile buildAccount(SipProfile account, SipUser sipUser, boolean encryptPassword) {
		return createAccount(account,
				sipUser.account_type,
				sipUser.username, 
				encryptPassword ? SimpleCrypto.encrypt(sipUser.password) : sipUser.password, 
				sipUser.displayname);
	}

	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_voxmobile_preferences;
	}

	@Override
	public boolean needRestart() {
		return false;
	}

	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceStringValue(SipConfigManager.USER_AGENT, CustomDistribution.getUserAgent());
        prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
	}

	@Override
	public List<Filter> getDefaultFilters(SipProfile acc) {
		ArrayList<Filter> filters = new ArrayList<Filter>();

		Filter f = new Filter();
		f = new Filter();
		f.account = (int) acc.id;
		f.action = Filter.ACTION_REPLACE;
		f.matchPattern = "^"+Pattern.quote("00")+"(.*)$";
		f.replacePattern = "011$1";
		f.matchType = Filter.MATCHER_STARTS;
		filters.add(f);

		return filters;
	}
}
