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
	// TODO Monitoring System task header parameter stander.
	public static final String TASK_MONITOR_ID_PROPERTY_NAME = "monitor_id";
	public static final String TASK_TIMESTAMP_PROPERTY_NAME = "timestamp";
	public static final String TASK_CREATED_AD_PROPERTY_NAME = "create_at";
	public static final String TASK_OPERATION_PROPERTY_NAME = "operation";
	public static final String TASK_TARGET_IP_PROPERTY_NAME = "target_ip";
	//
	public static final String TASK_META_DATA_NAME = "meta_data";
	//
	private JSONObject _taskObj = null;
	private HashMap<String, Object> _metaDataMap = null;
	private long _excuteIndex = 0;

	public MonitoringTask(JSONObject taskObj) {
		this._taskObj = taskObj;
		this._excuteIndex = System.nanoTime();
		initTaskMetaData();
	}

	public JSONObject getTaskObj() {
		return _taskObj;
	}
	
	private void initTaskMetaData(){
		_metaDataMap=new HashMap<String,Object>();
		try {
			JSONObject metaObj=this._taskObj.getJSONObject(TASK_META_DATA_NAME);
			if(metaObj==null||metaObj.length()==0){
				return;
			}
			//TODO ready the meta data.
		} catch (JSONException e) {
			// TODO Auto-generated catch block
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

	public String getTaskCreatedTime() {
		return getPropertyString(TASK_CREATED_AD_PROPERTY_NAME);
	}

	public String getMonitorID() {
		return getPropertyString(TASK_MONITOR_ID_PROPERTY_NAME);
	}

	public String getTimestamp() {
		return getPropertyString(TASK_TIMESTAMP_PROPERTY_NAME);
	}

	public boolean isBundle() {
		return true;
	}

	public long getExcuteIndex() {
		return this._excuteIndex;
	}

	public String getTaskDescription() {
		return getMonitorID() + "-" + getTaskCreatedTime() + "-" + getPropertyString(TASK_TARGET_IP_PROPERTY_NAME) + getPropertyString(TASK_OPERATION_PROPERTY_NAME);
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
	public HashMap<String,Object> getTaskMetaData(){
		return this._metaDataMap; 
	}
}
