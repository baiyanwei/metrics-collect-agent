package com.secpro.platform.monitoring.agent.storages.http;

import org.json.JSONArray;
import org.json.JSONObject;

import com.secpro.platform.api.client.IClientResponseListener;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.TaskProcessingAction;
import com.secpro.platform.monitoring.agent.services.StorageAdapterService;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;

/**
 * @author baiyanwei Jul 13, 2013
 * 
 *         Storage listener instance.
 * 
 */
public class FetchTaskListener implements IClientResponseListener {
	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(FetchTaskListener.class);

	public TaskProcessingAction _taskProcessingAction = null;
	// 0 false 1 true
	private byte _isHasResponse = 0;

	public FetchTaskListener(TaskProcessingAction action) {
		this._taskProcessingAction = action;
	}

	private void recycleWorkflows() {
		if (_taskProcessingAction != null) {
			_taskProcessingAction.recycle();
		}
	}

	private void processTasks(String contents) {
		if (_taskProcessingAction != null) {
			_taskProcessingAction.processTasks(contents);
		}
	}

	@Override
	public void setID(String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDescription(String description) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void fireSucceed(Object messageObj) throws PlatformException {
		// TODO Auto-generated method stub

		if (_isHasResponse == 0) {
			_isHasResponse = 1;
			StorageAdapterService.updateResponseCount();
		}
		try {
			if (messageObj != null) {
				final String contents = messageObj.toString();
				// final String contents = (String) (storageResponse.getData());
				if (contents != null && contents.trim().length() > 0) {
					new Thread("DPUStorageListener.taskProcessingAction.processTasks") {
						public void run() {
							processTasks(contents);
						}
					}.start();
					return;
				}
			}
			// function go here ,that mean processTask can not run in case, so
			// we need to recycle!
			new Thread("DPUStorageListener.taskProcessingAction.recycle") {
				public void run() {
					recycleWorkflows();
				}
			}.start();
		} catch (Exception e) {
			theLogger.exception(e);
		}

	}

	@Override
	public void fireError(Object messageObj) throws PlatformException {
		// TODO Auto-generated method stub

		if (_isHasResponse == 0) {
			_isHasResponse = 1;
			StorageAdapterService.updateResponseCount();
		}
		// TODO for test.
		if (true) {
			justForTest();
			return;
		}
		new Thread("DPUStorageListener.taskProcessingAction.recycle") {
			public void run() {
				recycleWorkflows();
			}
		}.start();
		if (messageObj != null) {
			theLogger.exception(new Exception(messageObj.toString()));
		}

	}

	private void justForTest() {
		try {
			JSONArray messageObj = new JSONArray();
			//
			JSONObject taskObjA = new JSONObject();
			taskObjA.put(MonitoringTask.TASK_MONITOR_ID_PROPERTY_NAME, "TASK-HB-TEST-0001");
			taskObjA.put(MonitoringTask.TASK_TIMESTAMP_PROPERTY_NAME, "2013/07/28 16:08:00");
			taskObjA.put(MonitoringTask.TASK_CREATED_AD_PROPERTY_NAME, "2013/07/28 16:08:00");
			taskObjA.put(MonitoringTask.TASK_OPERATION_PROPERTY_NAME, "snmp,ssh");
			taskObjA.put(MonitoringTask.TASK_TARGET_IP_PROPERTY_NAME, "10.0.0.1");
			//
			JSONObject metaObjA = new JSONObject();
			metaObjA.put("user", "root");
			metaObjA.put("password", "123");
			metaObjA.put("shell", "ls");
			taskObjA.put(MonitoringTask.TASK_META_DATA_NAME, metaObjA);
			messageObj.put(taskObjA);
			//
			JSONObject taskObjB = new JSONObject();
			taskObjB.put(MonitoringTask.TASK_MONITOR_ID_PROPERTY_NAME, "TASK-HB-TEST-0002");
			taskObjB.put(MonitoringTask.TASK_TIMESTAMP_PROPERTY_NAME, "2013/07/28 16:08:00");
			taskObjB.put(MonitoringTask.TASK_CREATED_AD_PROPERTY_NAME, "2013/07/28 16:08:00");
			taskObjB.put(MonitoringTask.TASK_OPERATION_PROPERTY_NAME, "snmp,ssh");
			taskObjB.put(MonitoringTask.TASK_TARGET_IP_PROPERTY_NAME, "10.0.0.2");
			//
			JSONObject metaObjB = new JSONObject();
			metaObjB.put("user", "root");
			metaObjB.put("password", "123");
			metaObjB.put("shell", "ls");
			taskObjB.put(MonitoringTask.TASK_META_DATA_NAME, metaObjB);
			//
			messageObj.put(taskObjB);
			if (messageObj != null) {
				final String contents = messageObj.toString();
				// final String contents = (String) (storageResponse.getData());
				if (contents != null && contents.trim().length() > 0) {
					new Thread("DPUStorageListener.taskProcessingAction.processTasks") {
						public void run() {
							processTasks(contents);
						}
					}.start();
					return;
				}
			}
			// function go here ,that mean processTask can not run in case, so
			// we need to recycle!
			new Thread("DPUStorageListener.taskProcessingAction.recycle") {
				public void run() {
					recycleWorkflows();
				}
			}.start();
		} catch (Exception e) {
			theLogger.exception(e);
		}
	}

	private void addLocalTaskCache(JSONObject taskObj) {

	}

	public String getTaskFromLocalCache() {
		return null;
	}
}