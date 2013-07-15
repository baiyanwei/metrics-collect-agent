package com.secpro.platform.monitoring.agent.storages.http;

import com.secpro.platform.api.client.IClientResponseListener;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.TaskProcessingAction;
import com.secpro.platform.monitoring.agent.services.StorageAdapterService;

/**
 * @author baiyanwei
 * Jul 13, 2013
 *
 * HTTP Storage listener instance. for pushing data sample to server
 *
 */
public class PushDataSampleListener implements IClientResponseListener {
	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(PushDataSampleListener.class);

	public TaskProcessingAction _taskProcessingAction = null;
	// 0 false 1 true
	private byte _isHasResponse = 0;

	public PushDataSampleListener() {
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
			StorageAdapterService.updateResponse4DpuCount();
		}
		try {
			if (messageObj != null) {
				final String contents = messageObj.toString();
				// final String contents = (String) (storageResponse.getData());
				if (contents != null && contents.trim().length() > 0) {
					new Thread("DPUStorageListener.taskProcessingAction.processTasks") {
						public void run() {
							//do work.
						}
					}.start();
					return;
				}
			}
		} catch (Exception e) {
			theLogger.exception(e);
		}

	}

	@Override
	public void fireError(Object messageObj) throws PlatformException {
		// TODO Auto-generated method stub

		if (_isHasResponse == 0) {
			_isHasResponse = 1;
			StorageAdapterService.updateResponse4DpuCount();
		}
		new Thread("DPUStorageListener.taskProcessingAction.recycle") {
			public void run() {
				//do work.
			}
		}.start();
		if (messageObj != null) {
			theLogger.exception(new Exception(messageObj.toString()));
		}

	}
}