/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.utils;

import android.content.pm.PackageManager.NameNotFoundException;

public class VersionCodeHelper {

	private static final int CSIPSIMPLE_CODE_BASE_VERSION = 993;

	@SuppressWarnings("unused")
	public static int getVersion() throws NameNotFoundException {
		// fake exception to avoid removing try/catch 
		// blocks that would have referenced pinfo.versionCode
		if (CSIPSIMPLE_CODE_BASE_VERSION == -1)
			throw new NameNotFoundException();
		
		return CSIPSIMPLE_CODE_BASE_VERSION;
	}
}
