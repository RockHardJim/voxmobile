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

import net.voxcorp.voxmobile.provider.DBContract.AccountContract;
import net.voxcorp.voxmobile.provider.DBContract.ProvisionCheckContract;
import net.voxcorp.voxmobile.provider.DBContract.RequestContract;
import net.voxcorp.voxmobile.provider.DBContract.Tables;
import net.voxcorp.voxmobile.provider.DBContract.VersionCheckContract;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class VoXMobileProvider extends ContentProvider {

	static String THIS_FILE = "VoXMobile Provider";

	public static final int REQUEST = 10;
	public static final int VERSION_CHECK = 12;
	public static final int PROVISION_CHECK = 13;
	public static final int ACCOUNT = 14;

	private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		matcher.addURI(DBContract.AUTHORITY, Tables.ACCOUNT, ACCOUNT);
		matcher.addURI(DBContract.AUTHORITY, Tables.REQUEST, REQUEST);
		matcher.addURI(DBContract.AUTHORITY, Tables.VERSION, VERSION_CHECK);
		matcher.addURI(DBContract.AUTHORITY, "provision_check", PROVISION_CHECK);
	}

	@Override
	public String getType(Uri uri) {
		int matchType = matcher.match(uri);
		switch (matchType) {
		case ACCOUNT:
			return AccountContract.CONTENT_TYPE;
		case REQUEST:
			return RequestContract.CONTENT_TYPE;
		case VERSION_CHECK:
			return VersionCheckContract.CONTENT_TYPE;
		case PROVISION_CHECK:
			return ProvisionCheckContract.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return uri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public boolean onCreate() {
		return false;
	}
}
