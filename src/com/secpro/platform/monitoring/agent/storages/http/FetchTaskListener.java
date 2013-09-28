package com.secpro.platform.monitoring.agent.storages.http;

import org.json.JSONArray;
import org.json.JSONException;

import com.secpro.platform.api.client.IClientResponseListener;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.TaskProcessingAction;
import com.secpro.platform.monitoring.agent.services.MonitoringTaskCacheService;

/**
 * @author baiyanwei Jul 13, 2013
 * 
 *         Storage listener instance. We fetch the task from TSS,if network
 *         doesn't work,then try for 4 times, if network is disconnection after
 *         4 times, get the task from local cache service.
 * 
 */
public class FetchTaskListener implements IClientResponseListener {
	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(FetchTaskListener.class);

	public TaskProcessingAction _taskProcessingAction = null;
	// 0 false 1 true
	// private byte _isHasResponse = 0;

	private String _listenerID = "FetchTaskListener";

	private String _listenerName = "FetchTaskListener";

	private String _listenerDescription = null;

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
		this._listenerID = id;
	}

	@Override
	public String getID() {
		return this._listenerID + this.hashCode();
	}

	@Override
	public void setName(String name) {
		this._listenerName = name;
	}

	@Override
	public String getName() {
		return this._listenerName;
	}

	@Override
	public void setDescription(String description) {
		this._listenerDescription = description;
	}

	@Override
	public String getDescription() {
		return this._listenerDescription;
	}

	@Override
	public void fireSucceed(Object messageObj) throws PlatformException {
		// if (_isHasResponse == 0) {
		// _isHasResponse = 1;
		// StorageAdapterService.updateResponseCount();
		// }
		// clear the error counter about disconnection.
		try {
			if (messageObj != null) {
				final String contents = messageObj.toString();
				// final String contents = (String) (storageResponse.getData());
				if (contents != null && contents.trim().length() > 0) {
					new Thread("DPUStorageListener.taskProcessingAction.processTasks") {
						public void run() {
							// execute job
							processTasks(contents);
							// put job into local task cache.
							putJobIntoLocalCache(contents);
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
		// if (_isHasResponse == 0) {
		// _isHasResponse = 1;
		// StorageAdapterService.updateResponseCount();
		// }
		if (messageObj != null) {
			theLogger.exception(new Exception(messageObj.toString()));
		}
		new Thread("DPUStorageListener.taskProcessingAction.recycle") {
			public void run() {
				recycleWorkflows();
			}
		}.start();

	}

	/**
	 * put job into local cache.
	 * 
	 * @param jobString
	 */
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