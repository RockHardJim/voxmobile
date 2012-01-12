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

public class Consts {
	public final static int MODE_DEVELOPMENT = 1; 
	public final static int MODE_STAGE = 2; 
	public final static int MODE_PRODUCTION = 3;
	public final static int MODE_ACTIVE = MODE_DEVELOPMENT;

	public static final boolean GOOGLE_ANALYTICS_DEBUG = false;
	public static final boolean GOOGLE_ANALYTICS_DRY_RUN = false;
	public static final int GOOGLE_ANALYTICS_DISPATCH_INTERVAL = 10;

	public static final int REST_UNAUTHORIZED = 1;
	public static final int REST_UNSUPPORTED = 2;
	public static final int REST_HTTP_ERROR = 3;
	public static final int REST_ERROR = 4;

	public static final String VOX_MOBILE_INVITE_EVENT = "send_invitation";	
}
