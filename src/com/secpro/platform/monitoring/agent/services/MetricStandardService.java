package com.secpro.platform.monitoring.agent.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
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
/**
 * 
 * @author sxf
 * MOD Sep 21, 2013
 * 将部分功能转移到核心，进行syslog日志二次标准化
 */
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
	/*
	{
		regexs Map
		protoFormat Map
		num int
		dateFormat string
	}
	*/
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
	public JSONObject buildRule(String ip,JSONObject ruleObj) {
		if (Assert.isEmptyString(ip) == true||ruleObj == null) {
			return null;
		}
		synchronized (this._metricStandardRuleMap) {
			this._metricStandardRuleMap.put(ip, ruleObj);
		}
		return ruleObj;
	}

	public JSONObject buildRule(JSONObject ruleObj) {
		if (ruleObj == null) {
			return null;
		}

		try {
			if (ruleObj.has("ip") == false || ruleObj.get("ip") == null) {
				return null;
			}
			if (ruleObj.has("regexs") == false || ruleObj.get("regexs") == null) {
				return null;
			}
			//String ruleKey = ruleObj.getString("ip") + "_" + ruleObj.getString("type");
			synchronized (this._metricStandardRuleMap) {
				this._metricStandardRuleMap.put(ruleObj.getString("ip"), ruleObj);
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
	public boolean changeRule(String ip,JSONObject ruleObj) {
		return buildRule(ip, ruleObj) == null ? false : true;

	}

	/**
	 * delete the rule of syslog,by the given ipAddress
	 * 
	 * @param ip
	 * @return
	 */
	public JSONObject removeRule(String ip) {
		if (Assert.isEmptyString(ip) == true) {
			return null;
		}
		synchronized (this._metricStandardRuleMap) {
			return this._metricStandardRuleMap.remove(ip);
		}
	}

	public JSONObject findRegexs(String ip) {
		if (Assert.isEmptyString(ip) == true) {
			return null;
		}
		return this._metricStandardRuleMap.get(ip);
	}
	/**
	 * 提供syslog标准化后上传条件，checkNum：成功标准化的元素个数
	 * @param ip
	 * @return
	 */
	public String findCheckNum(String ip){
		JSONObject standardRuleObj = findRegexs(ip);
		if (standardRuleObj == null) {
			return null;
		}
		if (standardRuleObj.has("checkNum") == false) {
			return null;
		}
		
		try {
			return standardRuleObj.getString("checkNum");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			theLogger.exception(e);
		}
		return null;
	}
	/**
	 * 提供syslog标准化后上传条件，checkAction(可能取值drop,upload)：根据参数值判断是否上传原始日志
	 * @param ip
	 * @return
	 */
	public String findCheckAction(String ip){
		JSONObject standardRuleObj = findRegexs(ip);
		if (standardRuleObj == null) {
			return null;
		}
		if (standardRuleObj.has("checkAction") == false) {
			return null;
		}
		
		try {
			return standardRuleObj.getString("checkAction");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			theLogger.exception(e);
		}
		return null;
	}

	/**
	 * fetch all syslog standard rules from server when starting.
	 */
	@Metric(description = "fetch all syslog standard rules from server")
	public void fetchServerStandardRule() {
		new FetchSysLogStandardRuleAction(this).start();
	}

	/**
	 * Based on regular expressions to format the given syslog
	 * 
	 * @param ip
	 * @param syslog
	 * @return
	 */
	public HashMap<String, String> matcher(String ip, String syslog) {
		if (Assert.isEmptyString(syslog) == true) {
			return null;
		}
		
		JSONObject standardRuleObj = findRegexs(ip);
		if (standardRuleObj == null) {
			return null;
		}
		try {
			JSONObject regexs = standardRuleObj.getJSONObject("regexs");
			if (regexs == null) {
				return null;
			}
			String[] properties = JSONObject.getNames(regexs);
			if(properties==null){
				return null;
			}
			HashMap<String, String> result = new HashMap<String, String>();

			int flag = 0;
			for (int i = 0; i < properties.length; i++) {
				String property = properties[i];
				if(property==null||"".equals(property))
				{
					return null;
				}
				String regex = regexs.getString(property);
				if(regex==null||"".equals(regex))
				{
					return null;
				}
				Pattern pattern = Pattern.compile(regex);
				Matcher mat = pattern.matcher(syslog);

				if (mat.find()) {
				
						result.put(property, mat.group(1));
						flag++;
				}

			}

			if (flag == 0) {
				theLogger.info("all of regular expressions is not matcher the syslog,format failed");
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

}
