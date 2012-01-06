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
package net.voxcorp.voxmobile.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class DBContract implements BaseColumns {

	public static final String AUTHORITY = "net.voxcorp.voxmobile.provider";
	public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    
    // This class cannot be instantiated
    private DBContract() {}

    public interface SyncStatus {
    	int CURRENT = -2;
    	int UPDATING = -1;
    	int STALE = 0;
    }

    public interface DBBoolean {
    	int TRUE = 0;
    	int FALSE = 1;
    }

    interface Tables {
    	String ACCOUNT = "account";
    	String REQUEST = "request";
    	String VERSION = "version";
    }

	public static class AccountContract implements BaseColumns {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.ACCOUNT).build();
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.account";
	}

	public static class RequestContract implements BaseColumns {
        public static final String UPDATED = "updated";
        public static final String TIMESTAMP = "time_stamp";
        public static final String SUCCESS = "success";
        public static final String HTTP_STATUS = "http_status";
        public static final String ERROR = "error";

        public static final int ID_INDEX = 0;
        public static final int UPDATED_INDEX = 1;
        public static final int TIMESTAMP_INDEX = 2;
        public static final int SUCCESS_INDEX = 3;
        public static final int HTTP_STATUS_INDEX = 4;
        public static final int ERROR_INDEX = 5;

		public static final Uri CONTENT_URI =
			BASE_CONTENT_URI.buildUpon().appendPath(Tables.REQUEST).build();
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.request";
        public static final String[] PROJECTION = {
        	_ID, UPDATED, TIMESTAMP, SUCCESS, HTTP_STATUS, ERROR };
	}
	
	public static class VersionCheckContract implements BaseColumns {
		public static final String SUPPORTED = "supported";
        public static final int ID_INDEX = 0;
        public static final int SUPPORTED_INDEX = 1;
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(Tables.VERSION).build();
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.check.version";
		public static final String[] PROJECTION = { _ID, SUPPORTED };
	}

	public static class ProvisionCheckContract {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath("provision_check").build();
		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.voxcorp.check.provision";
	}
}
