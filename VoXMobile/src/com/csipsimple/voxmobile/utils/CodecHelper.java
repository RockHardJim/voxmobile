/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.utils;

import java.util.HashMap;

import android.content.Context;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.PreferencesWrapper;

public class CodecHelper {

	private static final String G729 = "G729";
	private static final String CODEC_NAME = "codec_name";
	private static final String CODEC_ID = "codec_id";
	private static final String CODEC_PRIORITY = "codec_priority";
	public static short CODEC_ENABLE = 251;
	private static short CODEC_DISABLE = 0;
	
	private static HashMap<String, Object> getG729Codec(Context context, String type) {
		
		PreferencesWrapper prefsWrapper = new PreferencesWrapper(context);
		HashMap<String, Object> codec = new HashMap<String, Object>();
		
		String[] codecNames = prefsWrapper.getCodecList();
		
		for(String codecName : codecNames) {
			String[] codecParts = codecName.split("/");
			if (G729.equals(codecParts[0])) {
				if(codecParts.length >=2 ) {
					codec.put(CODEC_ID, codecName);
					codec.put(CODEC_NAME, codecParts[0]+" "+codecParts[1].substring(0, codecParts[1].length()-3)+" kHz");
					codec.put(CODEC_PRIORITY, prefsWrapper.getCodecPriority(codecName, type, Integer.toString((short)255)));
				}
				break;
			}
		}

		return codec;
	}
	
	public static boolean isG729Disabled(Context context) {
		HashMap<String, Object> codecWB = getG729Codec(context, SipConfigManager.CODEC_WB);
		HashMap<String, Object> codecNB = getG729Codec(context, SipConfigManager.CODEC_NB);

		boolean hasWB = (codecWB.size() > 0);
		boolean wbEnabled = hasWB && ((Short)codecWB.get(CODEC_PRIORITY) > 0); 
		boolean hasNB = (codecNB.size() > 0);
		boolean nbEnabled = hasNB && ((Short)codecNB.get(CODEC_PRIORITY) > 0); 

		return !wbEnabled && !nbEnabled;
	}
	
	public static boolean enableG729(Context context) {
		try {
			PreferencesWrapper prefsWrapper = new PreferencesWrapper(context);

			HashMap<String, Object> codecWB = getG729Codec(context, SipConfigManager.CODEC_WB);
			HashMap<String, Object> codecNB = getG729Codec(context, SipConfigManager.CODEC_NB);

			codecWB.put(CODEC_PRIORITY, CODEC_ENABLE);
			codecNB.put(CODEC_PRIORITY, CODEC_ENABLE);

			prefsWrapper.setCodecPriority((String) codecWB.get(CODEC_ID), SipConfigManager.CODEC_WB, Short.toString(CODEC_ENABLE));
			prefsWrapper.setCodecPriority((String) codecNB.get(CODEC_ID), SipConfigManager.CODEC_NB, Short.toString(CODEC_ENABLE));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean disbleG729(Context context) {
		try {
			PreferencesWrapper prefsWrapper = new PreferencesWrapper(context);

			HashMap<String, Object> codecWB = getG729Codec(context, SipConfigManager.CODEC_WB);
			HashMap<String, Object> codecNB = getG729Codec(context, SipConfigManager.CODEC_NB);

			codecWB.put(CODEC_PRIORITY, CODEC_DISABLE);
			codecNB.put(CODEC_PRIORITY, CODEC_DISABLE);

			prefsWrapper.setCodecPriority((String) codecWB.get(CODEC_ID), SipConfigManager.CODEC_WB, Short.toString(CODEC_DISABLE));
			prefsWrapper.setCodecPriority((String) codecNB.get(CODEC_ID), SipConfigManager.CODEC_NB, Short.toString(CODEC_DISABLE));
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
}
