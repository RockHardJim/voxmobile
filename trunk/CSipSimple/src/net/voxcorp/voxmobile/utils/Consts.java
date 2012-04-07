package net.voxcorp.voxmobile.utils;

public class Consts {
		
	public final static int MODE_DEVELOPMENT = 1; 
	public final static int MODE_STAGE = 2; 
	public final static int MODE_PRODUCTION = 3;
	public final static int MODE_ACTIVE = MODE_DEVELOPMENT;

	/** SOAP server hosts **/
	public static final String URL_PROD = "";
	public static final String URL_STAGE = "";
	public static final String URL_DEV = "";
	
	/** STUN server hosts **/
	public static final String STUN_HOST_PROD = "";
	public static final String STUN_HOST_STAGE = "";
	public static final String STUN_HOST_DEV = "";
	
	/** FAQ links **/
	public static final String FAQ_LINK_DEV = "";
	public static final String FAQ_LINK_STAGE = "";
	public static final String FAQ_LINK_PROD = "";
	
	/** Google Analytics account **/
	public static final String GOOGLE_ANALYTICS_PROD = "";
	public static final String GOOGLE_ANALYTICS_STAGE = "";
	public static final String GOOGLE_ANALYTICS_DEV = "";
	
	/** Google Analytics Tracker settings */
	public static final boolean GOOGLE_ANALYTICS_DEBUG = false;
	public static final boolean GOOGLE_ANALYTICS_DRY_RUN = false;
	public static final int GOOGLE_ANALYTICS_DISPATCH_INTERVAL = 10;

	/** Ordering Service **/
	public static final String ORDER_SERVICE_KEY = "service_key";
	public static final String ORDER_SERVICE_KEY_ID = "";
	public static final String BAD_ORDER_SERVICE_KEY_ID = "";
	
	/** Stuff for faking VoX-to-VoX rate center **/
	public static final String VOXLAND_STATE = "";
	public static final String VOXLAND_STATE_NAME = "";
	public static final String VOXLAND_CITY = "";
	
	/** REST Dialog States **/
	public static final int REST_UNAUTHORIZED = 1;
	public static final int REST_UNSUPPORTED = 2;
	public static final int REST_HTTP_ERROR = 3;
	public static final int REST_ERROR = 4;

	/* VoX Mobile Invite tag */
	public static final String VOX_MOBILE_INVITE_EVENT = "send_invitation";

	/* VoX Mobile Trial Dial Codes */
	public static final String VOX_MOBILE_TRIAL_DIALCODE = "trial_footprint";

	/* VoX Mobile Trial End tag */
	public static final String VOX_MOBILE_TRIAL_END = "trial_end";
}
