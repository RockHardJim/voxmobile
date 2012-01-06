/**
 * Copyright (C) 2012 VoX Communications
 * This file is part of VoX Mobile.
 *
 *  VoX Mobile is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  VoX Mobile is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with VoX Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.voxcorp.voxmobile.utils;

import java.util.HashMap;

import android.content.Context;

import net.voxcorp.api.SipConfigManager;
import net.voxcorp.utils.PreferencesWrapper;

public class CodecHelper {

	private static final String G729 = "G729";
	private static final String CODEC_NAME = "codec_name";
	private static final String CODEC_ID = "codec_id";
	private static final String CODEC_PRIORITY = "codec_priority";
	public static short CODEC_ENABLE = 250;
	private static short CODEC_DISABLE = 0;
	
	public static boolean isPreferredCodec(String codec) {
		return codec.startsWith("AMR") || codec.startsWith("G729") || codec.startsWith("SILK");
	}
	
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
