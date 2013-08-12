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
		//String ruleKey = ip + "_" + t;
		//JSONObject ruleObj = new JSONObject();
		//try {
			//ruleObj.put("ip", ip);
			//ruleObj.put("type", type);
		// ruleObj.put("rule", regexs);
		// } catch (JSONException e) {
		// theLogger.exception(e);
		// }
		synchronized (this._metricStandardRuleMap) {
			//this._metricStandardRuleMap.put(ruleKey, ruleObj);
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

	// private HashMap<String, String> matcherSyslog(String syslog, String
	// regex) {
	// HashMap<String, String> resultMap = null;
	// if (syslog.trim().length() > 0) {
	// List<String> keys = new ArrayList<String>();
	// Pattern pattern = Pattern.compile("(<\\w+>)");
	// Matcher mat = pattern.matcher(regex);
	// int i = 0;
	// if (mat.find()) {
	// while (mat.find(i)) {
	// String key = mat.group(1);
	// keys.add(key.substring(key.indexOf("<") + 1, key.lastIndexOf(">")));
	// i = mat.end();
	// }
	// String newRegex = mat.replaceAll("");
	// resultMap = matcherSyslogC(syslog, newRegex, keys);
	// }
	//
	// }
	// return resultMap;
	// }

	// private HashMap<String, String> matcherSyslogC(String syslog, String
	// regex, List<String> keys) {
	// HashMap<String, String> resultMap = null;
	// Pattern pattern = Pattern.compile(regex);
	// Matcher mat = pattern.matcher(syslog);
	// if (mat.find()) {
	// int count = mat.groupCount();
	// if (keys.size() >= count) {
	// resultMap = new HashMap<String, String>();
	// for (int i = 0; i < count; i++) {
	// resultMap.put(keys.get(i), mat.group(i + 1));
	// }
	// }
	// }
	// return resultMap;
	//
	// }

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
			HashMap<String, String> result = new HashMap<String, String>();

			int flag = 0;

			String[] properties = JSONObject.getNames(standardRuleObj);
			for (int i = 0; i < properties.length; i++) {
				String property = properties[i];
				String regex = regexs.getString(property);
				Pattern pattern = Pattern.compile(regex);
				Matcher mat = pattern.matcher(syslog);

				if (mat.find()) {
					if (property != null && !("".equals(property))) {
						if ("proto".equals(property)) {
							String value = protoFormat(standardRuleObj, mat.group(1));

							result.put(property, value);
							flag++;
						} else if ("date".equals(property)) {
							String value = dateFormat(standardRuleObj, mat.group(1));
							result.put(property, value);
							flag++;
						} else {
							result.put(property, mat.group(1));
							flag++;
						}

					}

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

	/**
	 * format the protocol of syslog
	 * 
	 * @param sys
	 * @param before
	 * @return
	 */
	private static String protoFormat(JSONObject standardRuleObj, String before) {
		JSONObject ProtoFormatObj;
		try {
			ProtoFormatObj = standardRuleObj.getJSONObject("ProtoFormat");
			if (ProtoFormatObj != null) {
				String value = ProtoFormatObj.getString(before);

				if (value != null && !("".equals(value)))

				{
					return value;
				} else {
					return before;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return before;

	}

	private static String dateFormat(JSONObject standardRuleObj, String dateS) {
		try {
			String dateFormat = standardRuleObj.getString("dataFormat");
			String before = dateS.trim();
			String after = null;
			if (dateFormat != null && !("".equals(dateFormat))) {
				if (dateFormat.indexOf("y") == -1) {

					Calendar calendar = Calendar.getInstance();
					calendar.setTime(new Date());
					int year = calendar.get(Calendar.YEAR);
					SimpleDateFormat format = new SimpleDateFormat(dateFormat, Locale.US);
					try {
						Date dateD = format.parse(dateS);
						SimpleDateFormat sdf1 = new SimpleDateFormat("MMddHHmmss");
						String dateS1 = sdf1.format(dateD);
						after = year + dateS1;
						return after;
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					SimpleDateFormat format = new SimpleDateFormat(dateFormat, Locale.US);
					try {
						Date dateD = format.parse(dateS);
						SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmmss");
						String dateS1 = sdf1.format(dateD);
						after = dateS1;
						return after;
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();

					}
				}

			}
			return before;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
