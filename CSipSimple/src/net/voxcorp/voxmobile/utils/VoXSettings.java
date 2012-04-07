package net.voxcorp.voxmobile.utils;

public class VoXSettings {
	
	public static int getMode() {
		return Consts.MODE_ACTIVE;
	}

	public static String getWebserviceHost() {
		switch (Consts.MODE_ACTIVE) {
          case Consts.MODE_DEVELOPMENT:
			  return Consts.URL_DEV;
		  case Consts.MODE_STAGE:
			  return Consts.URL_STAGE;
		  default:
			  return Consts.URL_PROD;
		}
	}
	
	public static String getStunServer() {
		switch (Consts.MODE_ACTIVE) {
          case Consts.MODE_DEVELOPMENT:
			  return Consts.STUN_HOST_DEV;
		  case Consts.MODE_STAGE:
			  return Consts.STUN_HOST_STAGE;
		  default:
			  return Consts.STUN_HOST_PROD;
		}
	}
	
	public static String getFaqLink() {
		switch (Consts.MODE_ACTIVE) {
          case Consts.MODE_DEVELOPMENT:
			  return Consts.FAQ_LINK_DEV;
		  case Consts.MODE_STAGE:
			  return Consts.FAQ_LINK_STAGE;
		  default:
			  return Consts.FAQ_LINK_PROD;
		}
	}
	
	public static String getGoogleAnalyticsAccount() {
		switch (Consts.MODE_ACTIVE) {
		  case Consts.MODE_DEVELOPMENT:
			  return Consts.GOOGLE_ANALYTICS_DEV;
		  case Consts.MODE_STAGE:
			  return Consts.GOOGLE_ANALYTICS_STAGE;
		  default:
			  return Consts.GOOGLE_ANALYTICS_PROD;
		}
	}

}
