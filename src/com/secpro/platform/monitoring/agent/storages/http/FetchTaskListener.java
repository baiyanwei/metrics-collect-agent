package com.secpro.platform.monitoring.agent.storages.http;

import org.json.JSONArray;
import org.json.JSONObject;

import com.secpro.platform.api.client.IClientResponseListener;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.TaskProcessingAction;
import com.secpro.platform.monitoring.agent.services.MonitoringService;
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
		if (_taskProcessingAction._isFetchCacheTaskOnError == true) {
			new Thread("DPUStorageListener.taskProcessingAction.processTasks") {
				public void run() {
					processTasks(getTaskFromLocalCache());
				}
			}.start();
			return;
		} else {
			new Thread("DPUStorageListener.taskProcessingAction.recycle") {
				public void run() {
					recycleWorkflows();
				}
			}.start();
			if (messageObj != null) {
				theLogger.exception(new Exception(messageObj.toString()));
			}
		}
	}

	private void addLocalTaskCache(JSONObject taskObj) {

	}

	public String getTaskFromLocalCache() {
		return null;
	}
}