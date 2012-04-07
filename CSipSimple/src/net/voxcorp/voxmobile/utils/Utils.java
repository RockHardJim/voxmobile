package net.voxcorp.voxmobile.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

	public static String properCase(String s) {
		Pattern p = Pattern.compile("(^|\\W)([a-z])");
		Matcher m = p.matcher(s.toLowerCase());
		StringBuffer sb = new StringBuffer(s.length());
		while(m.find()) {
			m.appendReplacement(sb, m.group(1) + m.group(2).toUpperCase() );
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
