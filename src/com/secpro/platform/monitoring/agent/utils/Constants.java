package com.secpro.platform.monitoring.agent.utils;

import java.nio.charset.Charset;

public class Constants {
	
    /**
     * The default encoding used for text data: UTF-8 
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    
    public static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_ENCODING);

    /**
     * HMAC/SHA1 Algorithm per RFC 2104, used when generating S3 signatures.
     */
    public static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * HMAC/SHA1 Algorithm per RFC 2104, used when generating S3 signatures.
     */
    public static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

}
