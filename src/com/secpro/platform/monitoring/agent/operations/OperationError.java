package com.secpro.platform.monitoring.agent.operations;

import java.util.ArrayList;

/**
 * This class communicates the error with the rest of the monitoring application
 * and the DPU see the page http://wiki.yottaa.com/Data-Submission-API.html for
 * more information.
 * 
 * @author bob
 * 
 */
public class OperationError {
	public static final class DnsError {
		// 1 - Unrecoverable Error
		final public static int UNRECOVERABLE = 1;
		// 2 - timeout
		final public static int TIMEOUT = 2;
		// 3 - Host not found
		final public static int HOST_NOT_FOUND = 3;
		// 4 - Type not found
		final public static int TYPE_NOT_FOUND = 4;
		// 5 - Malformed URL Exception
		final public static int MALFORMED_URL_EXCEPTION = 5;
		// 6 - General Error
		final public static int GENERAL_ERROR = 6;

		final public static String UNRECOVERABLE_MESSAGE = "DNS problem detected(Unrecoverable Error)";
		final public static String TIMEOUT_MESSAGE = "DNS problem detected(timeout)";
		final public static String HOST_NOT_FOUND_MESSAGE = "DNS problem detected(Host not found)";
		final public static String TYPE_NOT_FOUD_MESSAGE = "DNS problem detected(Type not found)";
		final public static String MALFORMED_RUL_MESSAGE = "DNS problem detected(Malformed URL Exception)";
		final public static String GENERAL_ERROR_MESSAGE = "DNS problem detected(General Error)";
	}

	public static final class HttpError {
		// Connection Refused - The server appears to not be responsing to the
		final public static int DNS_RESOLVE_ERROR = 11;
		// port or the TCP connection was refused.
		final public static int CONNECTION_REFUSED = 12;
		// Website unavailable - The hostname could not be accessed.
		final public static int UNAVAILABLE = 13;
		// Timeout - The HTTP operation failed to complete in the specified time
		final public static int CONNECTION_TIMEOUT = 14;
		// Too Many Redirections - The website redirected more than the allowed
		// number. (typically 5)
		final public static int TOO_MANY_REDIRECTIONS = 15;
		// also has a lot of HTTP standard errors
		final public static String DNS_RESOLVE_ERROR_MESSAGE = "DNS problem detected(Host not found, Can't find A record)";
		final public static String CONNECTION_RESFUSED_MESSAGE = "HTTP server problem detected(Connection Refused)";
		final public static String UNAVAILABLE_MESSAGE = "HTTP server problem detected(Website unavailable)";
		final public static String CONNECTION_TIMEOUT_MESSAGE = "HTTP server problem detected(Http request Timeout)";
		final public static String TOO_MANY_REDIRECTIONS_MESSAGE = "HTTP server problem detected(Too Many Redirections)";

	}

	public static final class WebpageError {
		final public static int DNS_RESOLVE_ERROR = 21;
		final public static int START_BROWSER_ERROR = 22;
		final public static int CALCULATE_YOTTAA_SCORE_ERROR = 23;
		final public static int CONNECTION_TIMEOUT = 24;
		final public static int CAPTURE_TIMEOUT = 25;
		final public static int SSL_CERT_ERROR = 26;
		final public static int UNAVAILABLE = 27;

		// also has a lot of HTTP standard errors
		final public static String DNS_RESOLVE_ERROR_MESSAGE = "DNS problem detected(Host not found, Can't find A record)";
		final public static String START_BROWSER_ERROR_MESSAGE = "webpage problem detected(Can't start browser)";
		final public static String CALCULATE_YOTTAA_SCORE_ERROR_MESSAGE = "webpage problem detected(Can't calculate yottaa score)";
		final public static String CONNECTION_TIMEOUT_MESSAGE = "webpage problem detected(Connection timeout)";
		final public static String CAPTURE_TIMEOUT_MESSAGE = "webpage problem detected(Capture Timeout)";
		final public static String SSL_CERT_ERROR_MESSAGE = "webpage problem detected(Invalid SSL Cert)";
		final public static String UNAVAILABLE_MESSAGE = "webpage problem detected(Website unavailable)";

	}

	public static final class McaError {
		final public static int WORKFLOW_TOMEOUT = 31;
		final public static int GENERAL_ERROR = 32;
		//
		final public static String WORKFLOW_TOMEOUT_MESSAGE = "MCA problem detected(Workflow timeout)";
		final public static String GENERAL_ERROR_MESSAGE = "MCA problem detected(General Error)";
	}

	public enum ErrorType {
		/**
		 * Error occurred trying to obtain a DNS time
		 */
		dns,
		/**
		 * Error occurred trying to obtain a HTTP time.
		 */
		http,
		/**
		 * Error occurred trying to obtain a web page test operations.
		 */
		webpage,
		/**
		 * Error occurred outside of trying to obtaining a metric, these are
		 * general workflow errors.
		 */
		mca
	}

	/**
	 * This is the general type of errors
	 */
	public ErrorType _type = ErrorType.mca;

	/**
	 * integer based error code.
	 */
	public int _code = 0;

	/**
	 * Human readable error message
	 */
	public String _message = "";

	/**
	 * Metadata specific to the time of the error
	 */
	public String _entry = "{}";

	/**
	 * This was the exception of the caused the error.
	 */
	public Exception _exception = null;

	public ArrayList<String> _screenshots = new ArrayList<String>();
}
