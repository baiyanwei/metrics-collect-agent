package com.secpro.platform.monitoring.agent.actions;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.secpro.platform.api.client.ClientConfiguration;
import com.secpro.platform.api.common.http.client.HttpClient;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.core.utils.Constants;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.node.InterfaceParameter;
import com.secpro.platform.monitoring.agent.services.MetricStandardService;
import com.secpro.platform.monitoring.agent.services.MonitoringNodeService;
import com.secpro.platform.monitoring.agent.services.StorageAdapterService;
import com.secpro.platform.monitoring.agent.storages.http.FetchStandardRuleListener;

/**
 * @author baiyanwei Aug 10, 2013
 * 
 *         fetch rule of the SysLog metric standard
 * 
 */
public class FetchSysLogStandardRuleAction extends Thread {
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(FetchSysLogStandardRuleAction.class);
	//
	private MetricStandardService _metricStandardService = null;

	//

	public FetchSysLogStandardRuleAction(MetricStandardService metricStandardService) {
		_metricStandardService = metricStandardService;
	}

	@Override
	public void run() {
		theLogger.info("fetchRule");
		executeFetchStandardRule();
	}

	public void executeFetchStandardRule() {
		try {
			MonitoringNodeService monitoringNodeService = ServiceHelper.findService(MonitoringNodeService.class);

			// This is the parameters of the Fetch message
			HashMap<String, String> requestHeadParaMap = new HashMap<String, String>();
			requestHeadParaMap.put(InterfaceParameter.LOCATION, monitoringNodeService._nodeLocation);
			//
			DefaultHttpRequest httpRequestV2 = createHttpMessage(this._metricStandardService._fetchStandardRulesPath, HttpMethod.GET, MetricStandardService.FETCH_MESSAGE_BODY);
			//
			HttpClient client = new HttpClient();
			ClientConfiguration config = new ClientConfiguration();
			config._endPointHost = this._metricStandardService._hostName;
			config._endPointPort = this._metricStandardService._hostPort.intValue();
			config._synchronousConnection = false;
			config._httpRequest = httpRequestV2;
			config._responseListener = new FetchStandardRuleListener(this);
			config._parameterMap = requestHeadParaMap;
			config._content = MetricStandardService.FETCH_MESSAGE_BODY;
			//
			client.configure(config);
			//
			client.start();
			//
			StorageAdapterService.updateRquestCount();
		} catch (Exception e) {
			theLogger.exception("executeFetchStandardRule", e);
		}
	}

	private DefaultHttpRequest createHttpMessage(String accessPath, HttpMethod httpMethod, String content) throws NoSuchAlgorithmException, IOException, Exception {
		if (content == null) {
			content = "";
		}

		DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, httpMethod, accessPath);
		// identify HTTP port we use
		if (80 == this._metricStandardService._hostPort.intValue()) {
			request.addHeader(HttpHeaders.Names.HOST, this._metricStandardService._hostName);
		} else {
			request.addHeader(HttpHeaders.Names.HOST, this._metricStandardService._hostName + ":" + this._metricStandardService._hostPort);
		}
		theLogger.info(this._metricStandardService._hostName + ":" + this._metricStandardService._hostPort + accessPath);
		TreeMap<String, String> requestHeaders = new TreeMap<String, String>(new Comparator<String>() {
			public int compare(String string0, String string1) {
				return string0.compareToIgnoreCase(string1);
			}
		});
		//
		requestHeaders.put(HttpHeaders.Names.DATE, new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(new Date()));
		// md5 coding hash string.
		requestHeaders.put(HttpHeaders.Names.CONTENT_TYPE, "text/json");

		// We need to set the content encoding to be UTF-8 in order to have the
		// message properly decoded.
		requestHeaders.put(HttpHeaders.Names.CONTENT_ENCODING, Constants.DEFAULT_ENCODING);
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

	public void analyzeStandardRuleOK(String content) {
		if (Assert.isEmptyString(content) == true) {
			return;
		}
		try {
			theLogger.debug("analyzeStandardRule", content);

			JSONTokener parser = new JSONTokener(content);
			JSONArray ruleJsons = new JSONArray(parser);

			// No tasks were return from the
			if (ruleJsons == null || ruleJsons.length() == 0) {
				theLogger.error("invalid Standard Rule content");
				return;
			}
			int successfulCounter = 0;
			for (int i = 0; i < ruleJsons.length(); i++) {
				JSONObject ruleObject = ruleJsons.getJSONObject(i);
				if (ruleObject == null || checkForRuleFormat(ruleObject) == false) {
					continue;
				}
				try {
					_metricStandardService.buildRule(ruleObject);
					successfulCounter++;
				} catch (Exception e) {
					theLogger.exception(e);
				}
			}
			theLogger.debug("reportFetchRuleResult", ruleJsons.length(), successfulCounter);
		} catch (Exception e1) {
			theLogger.exception("getMessageForReturn", e1);
		}
	}

	public void analyzeStandardRuleError(String contents) {
		theLogger.error(contents);
	}

	/**
	 * we can't run task like no url or operation.
	 * 
	 * @param taskObject
	 * @return
	 */
	private boolean checkForRuleFormat(JSONObject ruleObject) {

		if (ruleObject.has("ip") == false) {
			theLogger.warn("errorRuleFormat", "ip");
			return false;
		}
		if (ruleObject.has("type") == false) {
			theLogger.warn("errorRuleFormat", "type");
			return false;
		}
		if (ruleObject.has("rule") == false) {
			theLogger.warn("errorRuleFormat", "rule");
			return false;
		}
		return true;
	}
}
