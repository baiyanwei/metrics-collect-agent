package com.secpro.platform.monitoring.agent.workflow;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author baiyanwei Jul 13, 2013
 * 
 *         Monitoring Task define
 */
public class MonitoringTask {
	// '{'"tid":"{0}","sid": "{1}","reg": "{2}","ope":
	// "{3}","cat":"{4}","sat":"{5}","tip":"{6}","tpt":"{7}","con":'{8}',"mda":'{9}''}'

	public static final String TASK_ID_PROPERTY_NAME = "tid";
	public static final String TASK_SCHEDULE_ID_PROPERTY_NAME = "sid";
	public static final String TASK_REGION_PROPERTY_NAME = "reg";
	public static final String TASK_OPERATION_PROPERTY_NAME = "ope";
	public static final String TASK_CREATED_AD_PROPERTY_NAME = "cat";
	public static final String TASK_SCHEDULE_POINT_PROPERTY_NAME = "sat";
	public static final String TASK_TARGET_IP_PROPERTY_NAME = "tip";
	public static final String TASK_TARGET_PORT_PROPERTY_NAME = "tpt";
	public static final String TASK_CONTENT_PROPERTY_NAME = "con";
	public static final String TASK_META_DATA_NAME = "mda";
	//public static final String TASK_META_DATA_SECRET_NAME = "secret";

	//

	//
	private JSONObject _taskObj = null;
	private HashMap<String, String> _metaDataMap = null;
	private long _excuteIndex = 0;

	public MonitoringTask(JSONObject taskObj) {
		this._taskObj = taskObj;
		this._excuteIndex = System.nanoTime();
		initTaskMetaData();
	}

	public JSONObject getTaskObj() {
		return _taskObj;
	}

	/**
	 * read meta data
	 */
	private void initTaskMetaData() {
		_metaDataMap = new HashMap<String, String>();
		try {
			JSONObject metaObj = this._taskObj.getJSONObject(TASK_META_DATA_NAME);
			if (metaObj == null || metaObj.length() == 0) {
				return;
			}
			String[] names = JSONObject.getNames(metaObj);
			if (names == null || names.length == 0) {
				return;
			}
			for (int i = 0; i < names.length; i++) {
				_metaDataMap.put(names[i], metaObj.getString(names[i]));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the original url of the task.
	 * 
	 * @return
	 */
	public String getOperations() {
		return getPropertyString(TASK_OPERATION_PROPERTY_NAME);
	}
	public String getRegion() {
		return getPropertyString(TASK_REGION_PROPERTY_NAME);
	}

	public String getTaskCreatedTime() {
		return getPropertyString(TASK_CREATED_AD_PROPERTY_NAME);
	}

	public String getTaskID() {
		return getPropertyString(TASK_ID_PROPERTY_NAME);
	}

	public String getScheduleID() {
		return getPropertyString(TASK_SCHEDULE_ID_PROPERTY_NAME);
	}

	public String getScheduleTimestamp() {
		return getPropertyString(TASK_SCHEDULE_POINT_PROPERTY_NAME);
	}
	public String getTargetIP() {
		return getPropertyString(TASK_TARGET_IP_PROPERTY_NAME);
	}
	public String getTargetPort() {
		return getPropertyString(TASK_TARGET_PORT_PROPERTY_NAME);
	}
	public String getContent() {
		return getPropertyString(TASK_CONTENT_PROPERTY_NAME);
	}
	public boolean isBundle() {
		return true;
	}

	public long getExcuteIndex() {
		return this._excuteIndex;
	}

	public String getTaskDescription() {
		return getTaskID() + "-" + getTaskCreatedTime() + "-" + getPropertyString(TASK_TARGET_IP_PROPERTY_NAME) + getPropertyString(TASK_OPERATION_PROPERTY_NAME);
	}

	/**
	 * This internal method will deal with the JSONExpeption in one place
	 * 
	 * @param propertyID
	 * @return
	 */
	public String getPropertyString(String propertyID) {
		try {
			if (_taskObj != null) {
				return _taskObj.getString(propertyID);
			}
		} catch (JSONException e) {

		}
		return "";
	}

	/**
	 * get Task Meta data.
	 * 
	 * @return
	 */
	public HashMap<String, String> getTaskMetaData() {
		return this._metaDataMap;
	}
}
