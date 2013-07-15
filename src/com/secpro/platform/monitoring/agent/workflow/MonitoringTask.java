package com.secpro.platform.monitoring.agent.workflow;

import java.net.URL;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;




/**
 * @author baiyanwei
 * Jul 13, 2013
 * Monitoring Task define
 */
public class MonitoringTask {
	// TODO Monitoring System task header parameter stander.
	public static final String TASK_OPERATION_PROPERTY_NAME = "operation";
	public static final String TASK_MONITOR_ID_PROPERTY_NAME = "monitor_id";
	public static final String TASK_URL_PROPERTY_NAME = "url";
	public static final String TASK_TIMESTAMP_PROPERTY_NAME = "timestamp";
	public static final String TASK_CREATED_AD_PROPERTY_NAME = "create_at";
	//
	public static final String ORIGINAL_URL_PROPERTY_NAME = "og_url";
	public static final String SCREEN_SHOTS_PROPERTY_NAME = "screenshots";
	public static final String SCREEN_CAPTURE_PROPERTY_NAME = "screen_capture";
	public static final String HEADERS_HOST_PROERTY_NAME = "host";
	public static final String HEADERS_PROPERTY_NAME = "headers";
	public static final String TASK_HTTP_BODY = "http_body";
	public static final String TASK_HTTP_METHOD = "http_method";
	public static final String TASK_BROWSER_PROPERTY_NAME="browser";
	public static final String TASK_BANK_WIDTH_PROPERTY_NAME="bankwidth";
	public static final String TASK_CONNECTIVITY_PROPERTY_NAME="connectivity";
	public static final String TASK_SLA_PROPERTY_NAME="sla";
	/*
	 * public static final String TASK_HEADERS_PRE_HEAT_PROERTY_NAME =
	 * "pre_heat"; public static final String ORIGINAL_URL_PROPERTY_NAME =
	 * "og_url"; public static final String TASK_VERSION_PROPERTY_NAME =
	 * "version"; public static final String TASK_SUPPORT_VERSION = "2"; public
	 * static final String TASK_BUNDLE_PROPERTY_NAME="bundle";
	 */
	//
	private JSONObject taskObj = null;
	private long _excuteIndex = 0;
	private int[] _connectivity = null;

	public MonitoringTask(JSONObject taskObj) {
		this.taskObj = taskObj;
		this._excuteIndex = System.nanoTime();
		setPropertyValue(ORIGINAL_URL_PROPERTY_NAME, getSiteUrl());
	}

	public JSONObject getTaskObj() {
		return taskObj;
	}

	/**
	 * @return is need to screen shots action;
	 */
	public boolean isScreenShots() {
		try {
			if (this.taskObj.has(SCREEN_CAPTURE_PROPERTY_NAME) == false) {
				return false;
			}
			return this.taskObj.getBoolean(SCREEN_CAPTURE_PROPERTY_NAME);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * @return get header parameters collection from task content, and
	 *         collections will be used in http request or webpage operations.
	 */
	public HashMap<String, String> getHeaderParameters() {
		HashMap<String, String> paraMap = new HashMap<String, String>();
		//
		if (taskObj == null || taskObj.has(HEADERS_PROPERTY_NAME) == false) {
			return paraMap;
		}
		try {
			JSONObject headerObj = taskObj.getJSONObject(HEADERS_PROPERTY_NAME);
			if (headerObj == null) {
				return paraMap;
			}
			JSONArray names = headerObj.names();
			if (names == null) {
				return paraMap;
			}

			for (int i = 0; i < names.length(); i++) {
				String name = names.getString(i);
				String value = headerObj.getString(name);
				paraMap.put(name, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		//
		return paraMap;
	}

	/**
	 * Returns the original url of the task.
	 * 
	 * @return
	 */
	public String getSiteUrl() {
		return getPropertyString(TASK_URL_PROPERTY_NAME);
	}

	/**
	 * Returns the original url of the task.
	 * 
	 * @return
	 */
	public String getOriginalSiteUrl() {
		return getPropertyString(ORIGINAL_URL_PROPERTY_NAME);
	}

	/**
	 * Returns the original url of the task.
	 * 
	 * @return
	 */
	public String getOperations() {
		return getPropertyString(TASK_OPERATION_PROPERTY_NAME);
	}

	public String getTaskCreatedTime() {
		return getPropertyString(TASK_CREATED_AD_PROPERTY_NAME);
	}

	public String getMonitorID() {
		return getPropertyString(TASK_MONITOR_ID_PROPERTY_NAME);
	}

	public boolean isBundle() {
		return true;
	}
	
	public JSONObject getSimulateConnectivity(){
		try{
			return this.taskObj.getJSONObject(TASK_CONNECTIVITY_PROPERTY_NAME);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	public int[] getConnectivity() {
		return _connectivity;
	}

	public void setConnectivity(int[] connectivity) {
		_connectivity = connectivity;
	}
	/**
	 * @return get method of http operations careful ,if method is null , it
	 *         will throw a NullOpointExceptionl.
	 */
//	public HttpMethod getHttpMethod() throws Exception {
//		if (taskObj == null || taskObj.has(TASK_HTTP_METHOD) == false) {
//			return null;
//		}
//
//		return HttpMethod.valueOf(getPropertyString(TASK_HTTP_METHOD));
//	}

	/**
	 * @return String get HostName for task Object , if does't have ,return
	 *         null;
	 */
	public String getHostNameInHeader() {
		if (taskObj == null || taskObj.has(HEADERS_PROPERTY_NAME) == false) {
			return null;
		}
		try {
			JSONObject headerObj = taskObj.getJSONObject(HEADERS_PROPERTY_NAME);
			if (headerObj == null || headerObj.has(HEADERS_HOST_PROERTY_NAME) == false) {
				return null;
			}
			String hostName = headerObj.getString(HEADERS_HOST_PROERTY_NAME);
			if (hostName == null || hostName.trim().equals("")) {
				return null;
			}
			if (hostName.indexOf("://") != -1) {
				hostName = hostName.substring(hostName.indexOf("://") + 3);
			}
			if (hostName.indexOf("/") != -1) {
				hostName = hostName.substring(0, hostName.indexOf("/"));
			}
			return hostName;
		} catch (Exception e) {
		}
		return null;
	}
	/**
	 * @return String
	 * get hostName by target URL.
	 */
	public String getHostNameByTargetURL() {
		if (taskObj == null || taskObj.has(TASK_URL_PROPERTY_NAME) == false) {
			return null;
		}
		try {
			String targetUlrStr = taskObj.getString(TASK_URL_PROPERTY_NAME);
			if (targetUlrStr == null || targetUlrStr.trim().equals("")) {
				return null;
			}
//			if (Utils.testHttpPrefix(targetUlrStr) == false) {
//				targetUlrStr = "http://" + targetUlrStr;
//			}
			return new URL(targetUlrStr.trim()).getHost();
		} catch (Exception e) {
		}
		return null;
	}
	
	public void setDnsIP(String ip) {
		try {
			taskObj.put("DNS_IP", ip);
		} catch (JSONException e) {
		}
	}

	public String getDnsIP() {
		try {
			if (taskObj != null && taskObj.has("DNS_IP")) {
				return taskObj.getString("DNS_IP");
			}
		} catch (JSONException e) {
		}
		return null;
	}

	public boolean isNeedModifyHostsFile() {
		// if (task.getTaskObj().has(MonitoringTask.HEADERS_PROPERTY_NAME)) {
		// url = ModifyHostsFileAction.getUrlByHostName(task.getTaskObj());
		// }
		// ???
		return false;
	}

	public String getHttpBody() {
		return "";
	}

	public long getExcuteIndex() {
		return this._excuteIndex;
	}
	
	public String getSLA(){
		try{
			return this.taskObj.getJSONObject(TASK_SLA_PROPERTY_NAME).toString();
		}catch(Exception e){
			e.printStackTrace();
		}
		return new JSONObject().toString();
	}
	/**
	 * This internal method will deal with the JSONExpeption in one place
	 * 
	 * @param propertyID
	 * @return
	 */
	private String getPropertyString(String propertyID) {
		try {
			if (taskObj != null) {
				return taskObj.getString(propertyID);
			}
		} catch (JSONException e) {

		}
		return "";
	}

	/**
	 * Store the value of the property and handle the exception.
	 * 
	 * @param propertyID
	 * @param value
	 */
	private void setPropertyValue(String propertyID, String value) {
		try {
			if (taskObj != null) {
				taskObj.put(propertyID, getSiteUrl());
			}
		} catch (JSONException exception) {
			exception.printStackTrace();
		}
	}

}
