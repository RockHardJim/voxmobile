/*
 * Copyright (C) 2011 VoX Communications
 *
 */

package com.csipsimple.voxmobile.io.transport;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import android.util.Log;

public class AllowAllSSL {

	private static TrustManager[] trustManagers;

	public static class _FakeX509TrustManager implements javax.net.ssl.X509TrustManager {
		private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[]{};

		public void checkClientTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] arg0, String arg1)
			throws CertificateException {
		}

		public boolean isClientTrusted(X509Certificate[] chain) {
			return (true);
		}

		public boolean isServerTrusted(X509Certificate[] chain) {
			return (true);
		}

		public X509Certificate[] getAcceptedIssuers() {
			return (_AcceptedIssuers);
		}
	}

	public static void allowAllSSL(HttpsURLConnection con) {

		con.setHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
				return true;
			}
		});

		SSLContext context = null;

		if (trustManagers == null) {
			trustManagers = new TrustManager[]{new _FakeX509TrustManager()};
		}

		try {
			context = SSLContext.getInstance("TLS");
			context.init(null, trustManagers, new SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			Log.e("allowAllSSL", e.toString(), e);
		} catch (KeyManagementException e) {
			Log.e("allowAllSSL", e.toString(), e);
		}

		con.setSSLSocketFactory(context.getSocketFactory());
	}
}