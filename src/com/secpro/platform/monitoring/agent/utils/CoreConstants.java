package com.secpro.platform.monitoring.agent.utils;

import java.nio.charset.Charset;

public class CoreConstants {

	public static final long MILLI_IN_SECOND = 1000l;
	
	public static final String UTF_8 = "UTF-8";
	
	public static final Charset CHARSET_UTF_8 = Charset.forName(UTF_8);
	
	// IPV4 Address size
	public static final int IPV4_ADDR_SZ = 4;
	
	public static final String DOT = ".";
	public static final String COLON = ":";

	public static final String IPSTART_COLUMN_NAME = "ip_start";
	public static final String LATITUDE_COLUMN_NAME = "latitude";
	public static final String LONGITUDE_COLUMN_NAME = "longitude";
	public static final String COUNTRY_COLUMN_NAME = "country_code";
	public static final String REGION_COLUMN_NAME = "region_name";
	
	public static final String ORIGIN = "origin";
}
