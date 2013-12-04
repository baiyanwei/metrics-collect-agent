package com.secpro.platform.monitoring.agent.storages.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

import javax.xml.bind.annotation.XmlElement;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.secpro.platform.api.client.ClientConfiguration;
import com.secpro.platform.api.common.http.client.HttpClient;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.core.utils.Constants;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.TaskProcessingAction;
import com.secpro.platform.monitoring.agent.node.InterfaceParameter;
import com.secpro.platform.monitoring.agent.services.MonitoringEncryptService;
import com.secpro.platform.monitoring.agent.services.MonitoringService;
import com.secpro.platform.monitoring.agent.services.MonitoringTaskCacheService;
import com.secpro.platform.monitoring.agent.storages.IDataStorage;
import com.secpro.platform.monitoring.agent.utils.file.FileSystemStorageUtil;
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

	@XmlElement(name = "fetchTaskClient", type = ClientConfiguration.class)
	public ClientConfiguration _fetchTaskClient = new ClientConfiguration();
	//
	@XmlElement(name = "pushSampleClient", type = ClientConfiguration.class)
	public ClientConfiguration _pushSampleClient = new ClientConfiguration();

	final private static String FETCH_MESSAGE_BODY = "ok";

	private MonitoringService _monitoringService = null;

	private int _netwokDisconnectionErrorCounter = 0;

	@Override
	public void start() throws PlatformException {
		_monitoringService = ServiceHelper.findService(MonitoringService.class);
		theLogger.info("accessPathCheck", this._fetchTaskClient.toString(), this._pushSampleClient.toString());
	}

	@Override
	public void stop() throws PlatformException {
		_netwokDisconnectionErrorCounter = 0;
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
		return null;
		// create string to sign
		// String contentMD5 =
		// requestHeaders.get(HttpHeaders.Names.CONTENT_MD5);
		// String contentType =
		// requestHeaders.get(HttpHeaders.Names.CONTENT_TYPE);
		// String stringToSign = httpMethod.toString() + "#" + (contentMD5 !=
		// null ? contentMD5 : "") + "#" + (contentType != null ? contentType :
		// "") + "#"
		// + requestHeaders.get(HttpHeaders.Names.DATE) + "#" + this._username +
		// "#";
		//
		// // return the signature
		// return signWithHmacSha(stringToSign, hmacSha);
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
		return null;
		// if (this._password == null) {
		// return null;
		// }
		//
		// // The following HMAC/SHA1 code for the signature is taken from the
		// // AWS Platform's implementation of RFC2104
		// // (amazon.webservices.common.Signature)
		// //
		// // Acquire an HMAC/SHA1 from the raw key bytes.
		// SecretKeySpec signingKey = null;
		// try {
		// signingKey = new
		// SecretKeySpec(this._password.getBytes(Constants.DEFAULT_ENCODING),
		// Constants.HMAC_SHA256_ALGORITHM);
		// } catch (UnsupportedEncodingException e) {
		// throw e;
		// }
		//
		// // Acquire the MAC instance and initialize with the signing key.
		// Mac mac = null;
		// try {
		// mac = Mac.getInstance(hmacSha);
		// } catch (NoSuchAlgorithmException e) {
		// // should not happen
		// throw new NoSuchAlgorithmException("Could not find sha1 algorithm",
		// e);
		// }
		// try {
		// mac.init(signingKey);
		// } catch (InvalidKeyException e) {
		// // also should not happen
		// throw new
		// InvalidKeyException("Could not initialize the MAC algorithm", e);
		// }
		//
		// // Compute the HMAC on the digest, and set it.
		// try {
		// byte[] encrypted =
		// mac.doFinal(canonicalString.getBytes(Constants.DEFAULT_ENCODING));
		// ChannelBuffer channelBuffer =
		// ChannelBuffers.buffer(encrypted.length);
		// channelBuffer.writeBytes(encrypted);
		// ChannelBuffer b64 = Base64.encode(channelBuffer);
		// return b64.toString(Constants.DEFAULT_CHARSET);
		// } catch (UnsupportedEncodingException e) {
		// throw e;
		// }
	}

	@Override
	public void uploadRawData(final Object rawDataObj) throws PlatformException {
		if (rawDataObj == null) {
			throw new PlatformException("invalid upload sample data");
		}
		try {
			//System.out.println("rawDataObj:"+rawDataObj);
			theLogger.debug("readyForUpload", rawDataObj.toString());
			if (_monitoringService == null) {
				_monitoringService = ServiceHelper.findService(MonitoringService.class);
			}
			//
			DefaultHttpRequest httpRequestV2 = createHttpMessage(this._pushSampleClient, HttpMethod.PUT, rawDataObj.toString());
			//
			HttpClient client = new HttpClient();
			ClientConfiguration config = new ClientConfiguration();
			config._endPointHost = this._pushSampleClient._endPointHost;
			config._endPointPort = this._pushSampleClient._endPointPort;
			config._synchronousConnection = false;
			config._httpRequest = httpRequestV2;
			config._content = rawDataObj.toString();
			//
			PushDataSampleListener pushDataSampleListener = new PushDataSampleListener();
			pushDataSampleListener.setSampleData(config._content);
			//
			config._responseListener = pushDataSampleListener;

			//
			HashMap<String, String> requestHeadParaMap = new HashMap<String, String>();
			appendRequestHeaderParameters(requestHeadParaMap, 0);
			config._parameterMap = requestHeadParaMap;
			//
			client.configure(config);
			//
			client.start();
			//
			// StorageAdapterService.updateRquestCount();
		} catch (Exception e) {
			theLogger.exception("uploadRawData", e);
			if (e.getMessage().contains(HttpClient.NETWORK_ERROR_CONNECTION_REFUSED)) {
				// write the sample body into file when connect to server in
				// exception.
				new Thread("HTTPStorageAdapter.uploadRawData.storeSampleDateToFile") {
					// when upload sample data is in disconnection. We should
					// handle
					// this case on later.
					// We should put sample data into local system. and upload
					// it
					// when connection is ready.
					public void run() {
						try {
							MonitoringTaskCacheService monitoringTaskCacheService = ServiceHelper.findService(MonitoringTaskCacheService.class);
							// store the file content into local system.
							String filePath = FileSystemStorageUtil.storeSampleDateToFile(monitoringTaskCacheService.getFileStorageNameForTask(), rawDataObj.toString());
							if (filePath == null) {
								return;
							}
							// add file and waiting for uploading.
							monitoringTaskCacheService.addUploadSampleForDisconnection(filePath);
						} catch (Exception e) {
							theLogger.exception(e);
						}

					}
				}.start();
				return;
			}
		}
	}

	@Override
	public void executeFetchMessage(List<MonitoringWorkflow> workflows) {

		if (workflows == null || workflows.isEmpty()) {
			return;
		}
		final TaskProcessingAction taskAction = new TaskProcessingAction(workflows);
		try {
			if (_monitoringService == null) {
				_monitoringService = ServiceHelper.findService(MonitoringService.class);
			}
			// 获得加密服务
			MonitoringEncryptService encryptService = ServiceHelper.findService(MonitoringEncryptService.class);
			if (encryptService == null) {
				return;
			}
			// {public,private}
			String[] keyPair = encryptService.getKeyPair();
			if (keyPair == null) {
				return;
			}
			if (Assert.isEmptyString(keyPair[0]) == true || Assert.isEmptyString(keyPair[1]) == true) {
				return;
			}
			// This is the parameters of the Fetch message
			HashMap<String, String> requestHeadParaMap = new HashMap<String, String>();
			// 将公钥加入到HTTP协议头中
			requestHeadParaMap.put(InterfaceParameter.HttpHeaderParameter.PUBLIC_KEY, keyPair[0]);

			appendRequestHeaderParameters(requestHeadParaMap, workflows.size());
			//
			DefaultHttpRequest httpRequestV2 = createHttpMessage(this._fetchTaskClient, HttpMethod.GET, FETCH_MESSAGE_BODY);
			//
			HttpClient client = new HttpClient();
			ClientConfiguration config = new ClientConfiguration();
			config._endPointHost = this._fetchTaskClient._endPointHost;
			config._endPointPort = this._fetchTaskClient._endPointPort;
			config._synchronousConnection = false;
			config._httpRequest = httpRequestV2;
			// 将私钥传给取任务监听
			config._responseListener = new FetchTaskListener(taskAction, keyPair[1]);
			config._parameterMap = requestHeadParaMap;
			config._content = null;
			//
			client.configure(config);
			//
			client.start();
			//
			_netwokDisconnectionErrorCounter = 0;

			// StorageAdapterService.updateRquestCount();
		} catch (Exception e) {
			theLogger.exception("executeFetchMessage", e);
			if (_monitoringService._isFetchCacheTaskOnError == true && e.getMessage().contains(HttpClient.NETWORK_ERROR_CONNECTION_REFUSED)) {
				_netwokDisconnectionErrorCounter++;
				// adjust fetch action is OK or not, if not , then fetch a job
				// from local cache.
				if (_netwokDisconnectionErrorCounter >= 3) {
					final String cacheTaskObj = getTaskFromLocalCache();

					if (cacheTaskObj != null) {
						new Thread("HTTPStorageAdapter.executeFetchMessage.ProcessCacheTasks") {
							public void run() {
								taskAction.processTasks(cacheTaskObj);
								putJobIntoLocalCache(cacheTaskObj);
							}
						}.start();
						return;
					}
				}
			}
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

	private DefaultHttpRequest createHttpMessage(ClientConfiguration targetClient, HttpMethod httpMethod, String content) throws NoSuchAlgorithmException, IOException, Exception {
		if (content == null) {
			content = "";
		}

		// fill the request parameters in to query string.
		// StringBuilder parametersBuilder = new StringBuilder("?");
		// parametersBuilder.append("c=&l=&o=");
		// create HTTP request with query parameter.

		DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, httpMethod, targetClient._endPointPath);
		// identify HTTP port we use
		if (80 == targetClient._endPointPort) {
			request.addHeader(HttpHeaders.Names.HOST, targetClient._endPointHost);
		} else {
			request.addHeader(HttpHeaders.Names.HOST, targetClient._endPointHost + ":" + targetClient._endPointPort);
		}
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
		// String signature = this.createSignature(content, requestHeaders,
		// httpMethod, Constants.HMAC_SHA1_ALGORITHM);

		// Add the security parameters
		// request.addHeader("SignatureMethod", Constants.HMAC_SHA1_ALGORITHM);
		// request.addHeader("SignatureVersion", "2");
		// request.addHeader("Authorization", "AWS " + this._username + ":" +
		// signature);
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
		requestHeadParaMap.put(InterfaceParameter.HttpHeaderParameter.REGION, _monitoringService.getNodeLocation());
		requestHeadParaMap.put(InterfaceParameter.HttpHeaderParameter.COUNTER, String.valueOf(workflowCount));
		requestHeadParaMap.put(InterfaceParameter.HttpHeaderParameter.OPERATIONS, _monitoringService._operationCapabilities);
		requestHeadParaMap.put(InterfaceParameter.HttpHeaderParameter.MCA_NAME, _monitoringService._mcaName);
	}

	/**
	 * get local task from cache
	 * 
	 * @return
	 */
	private String getTaskFromLocalCache() {
		MonitoringTaskCacheService monitoringTaskCacheService = ServiceHelper.findService(MonitoringTaskCacheService.class);
		JSONObject taskObj = monitoringTaskCacheService.getCacheTaskInReferent();
		if (taskObj == null) {
			return null;
		}
		JSONArray taskArray = new JSONArray();
		taskArray.put(taskObj);
		theLogger.debug("runJobFromLocalCache", taskObj.toString());
		return taskArray.toString();
	}

	private void putJobIntoLocalCache(String jobsString) {
		if (Assert.isEmptyString(jobsString) == true) {
			return;
		}
		MonitoringTaskCacheService taskCache = ServiceHelper.findService(MonitoringTaskCacheService.class);
		try {
			// JSONTokener parser = new JSONTokener(content);

			JSONArray taskJsons = new JSONArray(jobsString);
			if (taskJsons == null || taskJsons.length() == 0) {
				return;
			}
			for (int i = 0; i < taskJsons.length(); i++) {
				taskCache.addTaskIntoCache(taskJsons.getJSONObject(i));
			}
			theLogger.debug("putJobIntoLocalCache", jobsString);
		} catch (JSONException e) {
			theLogger.exception(e);
		}
	}
}