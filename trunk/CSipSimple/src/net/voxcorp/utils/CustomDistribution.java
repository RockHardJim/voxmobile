/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.voxcorp.utils;

import net.voxcorp.voxmobile.utils.VoXSettings;
import net.voxcorp.wizards.WizardUtils.WizardInfo;

public class CustomDistribution {

	// CSipSimple trunk distribution
	
	public static boolean distributionWantsOtherAccounts() {
		return true;
	}
	
	public static boolean distributionWantsOtherProviders() {
		return true;
	}
	
	public static String getSupportEmail() {
		return "support@voxcorp.net";
	}
	
	public static String getUserAgent() {
		return "VoXMobile (Android)";
	}
	
	public static WizardInfo getCustomDistributionWizard() {
		return null; 
	}
	
	public static String getRootPackage() {
		return "net.voxcorp";
	}
	
	public static boolean showIssueList() {
		return false;
	}
	
	public static String getFaqLink() {
		return VoXSettings.getFaqLink();
	}
	
	public static boolean showFirstSettingScreen() {
		return true;
	}
	
	public static boolean supportMessaging() {
		return true;
	}

	public static boolean forceNoMultipleCalls() {
		return false;
	}
	
}
