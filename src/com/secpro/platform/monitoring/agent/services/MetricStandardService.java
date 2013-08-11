package com.secpro.platform.monitoring.agent.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.DynamicMBean;
import javax.xml.bind.annotation.XmlElement;

import org.json.JSONException;
import org.json.JSONObject;

import com.secpro.platform.core.metrics.AbstractMetricMBean;
import com.secpro.platform.core.metrics.Metric;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.FetchSysLogStandardRuleAction;

@ServiceInfo(description = "SYSLOG record stander service", configurationPath = "mca/services/MetricStandardService/")
public class MetricStandardService extends AbstractMetricMBean implements IService, DynamicMBean {

	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(MetricStandardService.class);
	final public static String FETCH_MESSAGE_BODY = "ok";

	private HashMap<String, JSONObject> _metricStandardRuleMap = new HashMap<String, JSONObject>();

	@XmlElement(name = "jmxObjectName", defaultValue = "secpro:type=MetricStandardService")
	public String _jmxObjectName = "secpro:type=MetricStandardService";
	@XmlElement(name = "hostName")
	public String _hostName = "";

	@XmlElement(name = "hostPort", type = Long.class, defaultValue = "80")
	public Long _hostPort = new Long(80);

	// this is the path for the task to be fetched to.
	@XmlElement(name = "fetchStandardRulesPath", defaultValue = "/tss/standard/fetch")
	public String _fetchStandardRulesPath = "";

	@XmlElement(name = "userName", defaultValue = "mca")
	public String _username = "";

	@XmlElement(name = "passWord", defaultValue = "123456")
	public String _password = "";

	@Override
	public void start() throws Exception {
		// register itself as dynamic bean
		this.registerMBean(_jmxObjectName, this);
		try {
			fetchServerStandardRule();
		} catch (Exception e) {
			theLogger.exception(e);
		}
	}

	@Override
	public void stop() throws Exception {
		_metricStandardRuleMap.clear();
		// unregister itself
		this.unRegisterMBean(_jmxObjectName);
	}

	/**
	 * create the rule of syslog
	 * 
	 * @param ip
	 * @param regexs
	 * @return
	 */
	public JSONObject buildRule(String ip, String type, List<String> regexs) {
		if (Assert.isEmptyString(ip) == true || Assert.isEmptyString(type) == true) {
			return null;
		}
		if (regexs == null || regexs.isEmpty() == true) {
			return null;
		}
		String ruleKey = ip + "_" + type;
		JSONObject ruleObj = new JSONObject();
		try {
			ruleObj.put("ip", ip);
			ruleObj.put("type", type);
			ruleObj.put("rule", regexs);
		} catch (JSONException e) {
			theLogger.exception(e);
		}
		synchronized (this._metricStandardRuleMap) {
			this._metricStandardRuleMap.put(ruleKey, ruleObj);
		}
		return ruleObj;
	}

	public JSONObject buildRule(JSONObject ruleObj) {
		if (ruleObj == null) {
			return null;
		}

		try {
			if (ruleObj.has("rule") == false || ruleObj.get("rule") == null) {
				return null;
			}
			String ruleKey = ruleObj.getString("ip") + "_" + ruleObj.getString("type");
			synchronized (this._metricStandardRuleMap) {
				this._metricStandardRuleMap.put(ruleKey, ruleObj);
			}
		} catch (JSONException e) {
			theLogger.exception(e);
		}

		return ruleObj;
	}

	/**
	 * change the rule of syslog
	 * 
	 * @param ip
	 * @param regexs
	 * @return
	 */
	public boolean changeRule(String ip, String type, List<String> regexs) {
		return buildRule(ip, type, regexs) == null ? false : true;

	}

	/**
	 * delete the rule of syslog,by the given ipAddress
	 * 
	 * @param ip
	 * @return
	 */
	public JSONObject removeRule(String ip, String type) {
		if (Assert.isEmptyString(ip) == true || Assert.isEmptyString(type) == true) {
			return null;
		}
		synchronized (this._metricStandardRuleMap) {
			return this._metricStandardRuleMap.remove(ip + "_" + type);
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> findRegexs(String ip, String type) {
		if (Assert.isEmptyString(ip) == true || Assert.isEmptyString(type) == true) {
			return null;
		}
		String ruleKey = ip + "_" + type;
		if (this._metricStandardRuleMap.containsKey(ruleKey)) {
			return null;
		}
		JSONObject ruleObj = this._metricStandardRuleMap.get(ruleKey);
		if (ruleObj == null) {
			return null;
		}

		try {
			return (List<String>) ruleObj.get("rule");
		} catch (JSONException e) {
			theLogger.exception(e);
		}
		return null;
	}

	/**
	 * Based on regular expressions to format the given syslog
	 * 
	 * @param ip
	 * @param syslog
	 * @return
	 */
	public HashMap<String, String> matcher(String ip, String type, String syslog) {
		List<String> regexs = findRegexs(ip, type);
		if (regexs == null || regexs.isEmpty() == true) {
			return null;
		}
		HashMap<String, String> result = null;
		int flag = 0;

		for (int i = 0; i < regexs.size(); i++) {
			result = matcherSyslog(syslog, regexs.get(i));
			if (result != null && result.size() > 0) {
				flag = 1;
				i = regexs.size();
			}
		}

		if (flag == 0) {
			System.out.println("all of regular expressions is not matcher the syslog,format failed");
		}
		return result;

	}

	private HashMap<String, String> matcherSyslog(String syslog, String regex) {
		HashMap<String, String> resultMap = null;
		if (syslog.trim().length() > 0) {
			List<String> keys = new ArrayList<String>();
			Pattern pattern = Pattern.compile("(<\\w+>)");
			Matcher mat = pattern.matcher(regex);
			int i = 0;
			if (mat.find()) {
				while (mat.find(i)) {
					String key = mat.group(1);
					keys.add(key.substring(key.indexOf("<") + 1, key.lastIndexOf(">")));
					i = mat.end();
				}
				String newRegex = mat.replaceAll("");
				resultMap = matcherSyslogC(syslog, newRegex, keys);
			}

		}
		return resultMap;
	}

	private HashMap<String, String> matcherSyslogC(String syslog, String regex, List<String> keys) {
		HashMap<String, String> resultMap = null;
		Pattern pattern = Pattern.compile(regex);
		Matcher mat = pattern.matcher(syslog);
		if (mat.find()) {
			int count = mat.groupCount();
			if (keys.size() >= count) {
				resultMap = new HashMap<String, String>();
				for (int i = 0; i < count; i++) {
					resultMap.put(keys.get(i), mat.group(i + 1));
				}
			}
		}
		return resultMap;

	}

	/**
	 * fetch all syslog standard rules from server when starting.
	 */
	@Metric(description = "fetch all syslog standard rules from server")
	public void fetchServerStandardRule() {
		new FetchSysLogStandardRuleAction(this).start();
	}
}
