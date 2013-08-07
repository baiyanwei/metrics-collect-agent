package com.secpro.platform.monitoring.agent.storages.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.annotation.XmlElement;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.secpro.platform.api.client.ClientConfiguration;
import com.secpro.platform.api.common.http.client.HttpClient;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.core.utils.Constants;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.TaskProcessingAction;
import com.secpro.platform.monitoring.agent.node.InterfaceParameter;
import com.secpro.platform.monitoring.agent.services.MonitoringNodeService;
import com.secpro.platform.monitoring.agent.services.MonitoringService;
import com.secpro.platform.monitoring.agent.services.StorageAdapterService;
import com.secpro.platform.monitoring.agent.storages.IDataStorage;
import com.secpro.platform.monitoring.agent.workflow.MonitoringWorkflow;

/**
 * @author baiyanwei Jul 21, 2013
 * 
 *         HTTP storage adapter
 * 
 */
@ServiceInfo(description = "Store data directly into the TSS with HTTP", configurationPath = "mca/services/HTTPStorageAdapter/")
public class HTTPStorageAdapter implements IService, IDataStorage {

	private static PlatformLogger theLogger = PlatformLogger.getLogger(HTTPStorageAdapter.class);
	@XmlElement(name = "hostName")
	public String _hostName = "";

	@XmlElement(name = "hostPort", type = Long.class, defaultValue = "80")
	public Long _hostPort = new Long(80);

	// this is the path for the task to be fetched to.
	@XmlElement(name = "fetchTaskPath", defaultValue = "/samples/fetch")
	public String _fetchTaskPath = "";

	@XmlElement(name = "pushSamplePath", defaultValue = "/samples/push")
	public String _pushSamplePath = "";

	@XmlElement(name = "userName", defaultValue = "mca")
	public String _username = "";

	@XmlElement(name = "passWord", defaultValue = "123456")
	public String _password = "";

	final private static String FETCH_MESSAGE_BODY = "ok";

	private MonitoringNodeService _monitoringNodeService = null;
	private MonitoringService _monitoringService = null;

	@Override
	public void start() throws PlatformException {
		_monitoringNodeService = ServiceHelper.findService(MonitoringNodeService.class);
		_monitoringService = ServiceHelper.findService(MonitoringService.class);
	}

	@Override
	public void stop() throws PlatformException {
	}

	/**
	 * 
	 * @param contents
	 * @param requestHeaders
	 * @param httpMethod
	 * @param hmacSha
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws AwsException
	 */
	public String createSignature(String contents, Map<String, String> requestHeaders, HttpMethod httpMethod, String hmacSha) throws NoSuchAlgorithmException, IOException,
			Exception {

		// create string to sign
		String contentMD5 = requestHeaders.get(HttpHeaders.Names.CONTENT_MD5);
		String contentType = requestHeaders.get(HttpHeaders.Names.CONTENT_TYPE);
		String stringToSign = httpMethod.toString() + "#" + (contentMD5 != null ? contentMD5 : "") + "#" + (contentType != null ? contentType : "") + "#"
				+ requestHeaders.get(HttpHeaders.Names.DATE) + "#" + this._username + "#";

		// return the signature
		return signWithHmacSha(stringToSign, hmacSha);
	}

	/**
	 * Calculate the HMAC/SHA1 on a string.
	 * 
	 * TODO this code is retrieved from AwsRequest, we will have to refactor it
	 * sooner of later
	 * 
	 * @param awsSecretKey
	 *            AWS secret key.
	 * @param canonicalString
	 *            canonical string representing the request to sign.
	 * @return Signature
	 * @throws S3ServiceException
	 */
	protected String signWithHmacSha(String canonicalString, String hmacSha) throws Exception {
		if (this._password == null) {
			return null;
		}

		// The following HMAC/SHA1 code for the signature is taken from the
		// AWS Platform's implementation of RFC2104
		// (amazon.webservices.common.Signature)
		//
		// Acquire an HMAC/SHA1 from the raw key bytes.
		SecretKeySpec signingKey = null;
		try {
			signingKey = new SecretKeySpec(this._password.getBytes(Constants.DEFAULT_ENCODING), Constants.HMAC_SHA256_ALGORITHM);
		} catch (UnsupportedEncodingException e) {
			throw e;
		}

		// Acquire the MAC instance and initialize with the signing key.
		Mac mac = null;
		try {
			mac = Mac.getInstance(hmacSha);
		} catch (NoSuchAlgorithmException e) {
			// should not happen
			throw new NoSuchAlgorithmException("Could not find sha1 algorithm", e);
		}
		try {
			mac.init(signingKey);
		} catch (InvalidKeyException e) {
			// also should not happen
			throw new InvalidKeyException("Could not initialize the MAC algorithm", e);
		}

		// Compute the HMAC on the digest, and set it.
		try {
			byte[] encrypted = mac.doFinal(canonicalString.getBytes(Constants.DEFAULT_ENCODING));
			ChannelBuffer channelBuffer = ChannelBuffers.buffer(encrypted.length);
			channelBuffer.writeBytes(encrypted);
			ChannelBuffer b64 = Base64.encode(channelBuffer);
			return b64.toString(Constants.DEFAULT_CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw e;
		}
	}

	@Override
	public void uploadRawData(Object rawDataObj) throws PlatformException {
		try {
			if(true){
				System.out.println("uploadRawData>>>"+rawDataObj);
				return;
			}
			//
			DefaultHttpRequest httpRequestV2 = createHttpMessage(this._pushSamplePath, HttpMethod.PUT, rawDataObj.toString());
			//
			HttpClient client = new HttpClient();
			ClientConfiguration config = new ClientConfiguration();
			config._endPointHost = this._hostName;
			config._endPointPort = this._hostPort.intValue();
			config._synchronousConnection = false;
			config._httpRequest = httpRequestV2;
			config._responseListener = new PushDataSampleListener();
			config._content = null;
			//
			HashMap<String, String> requestHeadParaMap = new HashMap<String, String>();
			appendRequestHeaderParameters(requestHeadParaMap, 0);
			config._parameterMap = requestHeadParaMap;
			//
			client.configure(config);
			//
			client.start();
			//
			StorageAdapterService.updateRquestCount();
		} catch (Exception e) {
			theLogger.exception(e);
		}
	}

	@Override
	public void executeFetchMessage(List<MonitoringWorkflow> workflows) {
		try {
			if (_monitoringService == null) {
				_monitoringService = ServiceHelper.findService(MonitoringService.class);
			}
			TaskProcessingAction taskAction = new TaskProcessingAction(workflows);
			// This is the parameters of the Fetch message
			HashMap<String, String> requestHeadParaMap = new HashMap<String, String>();
			appendRequestHeaderParameters(requestHeadParaMap, workflows.size());
			//
			DefaultHttpRequest httpRequestV2 = createHttpMessage(this._fetchTaskPath, HttpMethod.GET, FETCH_MESSAGE_BODY);
			//
			HttpClient client = new HttpClient();
			ClientConfiguration config = new ClientConfiguration();
			config._endPointHost = this._hostName;
			config._endPointPort = this._hostPort.intValue();
			config._synchronousConnection = false;
			config._httpRequest = httpRequestV2;
			config._responseListener = new FetchTaskListener(taskAction);
			config._parameterMap = requestHeadParaMap;
			config._content = null;
			//
			client.configure(config);
			//
			client.start();
			//
			StorageAdapterService.updateRquestCount();
		} catch (Exception e) {
			theLogger.exception("executeFetchMessage", e);
			for (Iterator<MonitoringWorkflow> iter = workflows.iterator(); iter.hasNext();) {
				try {
					iter.next().recycleForReady();
				} catch (Exception loopException) {
					continue;
				}
			}
		}
	}

	//
	// PRIVATE METHODS
	//

	private DefaultHttpRequest createHttpMessage(String accessPath, HttpMethod httpMethod, String content) throws NoSuchAlgorithmException, IOException, Exception {
		if (content == null) {
			content = "";
		}
		
		// fill the request parameters in to query string.
		// StringBuilder parametersBuilder = new StringBuilder("?");
		// parametersBuilder.append("c=&l=&o=");
		// create HTTP request with query parameter.
		
		DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, httpMethod, accessPath);
		// identify HTTP port we use
		if (80 == this._hostPort.intValue()) {
			request.addHeader(HttpHeaders.Names.HOST, this._hostName);
		} else {
			request.addHeader(HttpHeaders.Names.HOST, this._hostName + ":" + this._hostPort);
		}
		theLogger.info(this._hostName+":"+this._hostPort+accessPath);
		TreeMap<String, String> requestHeaders = new TreeMap<String, String>(new Comparator<String>() {
			public int compare(String string0, String string1) {
				return string0.compareToIgnoreCase(string1);
			}
		});
		//
		requestHeaders.put(HttpHeaders.Names.DATE, new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(new Date()));
		// md5 coding hash string.
		requestHeaders.put(HttpHeaders.Names.CONTENT_MD5, computeMD5Hash(content));
		requestHeaders.put(HttpHeaders.Names.CONTENT_TYPE, "text/plain");

		// We need to set the content encoding to be UTF-8 in order to have the
		// message properly decoded.
		requestHeaders.put(HttpHeaders.Names.CONTENT_ENCODING, Constants.DEFAULT_ENCODING);
		// Create the security signature.
		String signature = this.createSignature(content, requestHeaders, httpMethod, Constants.HMAC_SHA1_ALGORITHM);

		// Add the security parameters
		request.addHeader("SignatureMethod", Constants.HMAC_SHA1_ALGORITHM);
		request.addHeader("SignatureVersion", "2");
		request.addHeader("Authorization", "AWS " + this._username + ":" + signature);
		// Add the customer headers to the request.
		Iterator<String> iterator = requestHeaders.keySet().iterator();
		while (iterator.hasNext() == true) {
			String name = iterator.next();
			String value = requestHeaders.get(name);
			request.addHeader(name, value);
		}

		// Needs to use the size of the bytes in the string.
		byte[] bytes = content.getBytes(Constants.DEFAULT_CHARSET);

		request.addHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(bytes.length));
		request.addHeader(HttpHeaders.Names.USER_AGENT, "Mectrics-Collect-Agent");

		ChannelBuffer channelBuffer = ChannelBuffers.buffer(bytes.length);
		channelBuffer.writeBytes(bytes);
		request.setContent(channelBuffer);
		return request;
	}

	/**
	 * Computes the MD5 hash of the data and returns it as a hex string.
	 * 
	 * @param is
	 * @return MD5 hash
	 */
	private String computeMD5Hash(String contents) {
		InputStream inputStream = new ByteArrayInputStream(contents.getBytes());
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("MD5");

			int length = inputStream.available();
			ChannelBuffer channelBuffer = ChannelBuffers.buffer(length);
			channelBuffer.writeBytes(inputStream, length);
			byte[] buffer = new byte[16384];
			int bytesRead = -1;
			while ((bytesRead = channelBuffer.readableBytes()) > 0) {
				// if the readable bytes are bigger than the buffer length.
				// need to only read the buffer length.
				bytesRead = bytesRead >= buffer.length ? buffer.length : bytesRead;
				channelBuffer.readBytes(buffer, 0, bytesRead);
				messageDigest.update(buffer, 0, bytesRead);
			}

			byte[] digest = messageDigest.digest();
			ChannelBuffer channelBufferMD5 = ChannelBuffers.buffer(digest.length);
			channelBufferMD5.writeBytes(digest);
			ChannelBuffer b64 = Base64.encode(channelBufferMD5);
			return b64.toString(Constants.DEFAULT_CHARSET);
		} catch (NoSuchAlgorithmException e) {
			theLogger.error("computeMD5Hash", e);
		} catch (IOException e) {
			theLogger.error("computeMD5Hash", e);
		}

		return "";
	}

	/**
	 * append Head parameter into request.
	 * 
	 * @param requestHeadParaMap
	 * @param workflows
	 */
	private void appendRequestHeaderParameters(HashMap<String, String> requestHeadParaMap, int workflowCount) {
		requestHeadParaMap.put(InterfaceParameter.LOCATION, _monitoringNodeService._nodeLocation);
		requestHeadParaMap.put(InterfaceParameter.COUNT, String.valueOf(workflowCount));
		requestHeadParaMap.put(InterfaceParameter.OPERATIONS, _monitoringService._operationCapabilities);
	}
}