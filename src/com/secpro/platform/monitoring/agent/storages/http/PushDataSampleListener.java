package com.secpro.platform.monitoring.agent.storages.http;

import com.secpro.platform.api.client.IClientResponseListener;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.services.MonitoringTaskCacheService;
import com.secpro.platform.monitoring.agent.utils.file.FileSystemStorageUtil;

/**
 * @author baiyanwei Jul 13, 2013
 * 
 *         HTTP Storage listener instance. for pushing data sample to server
 * 
 */
public class PushDataSampleListener implements IClientResponseListener {
	//
	// Logging Object
	//
	final private static PlatformLogger theLogger = PlatformLogger.getLogger(PushDataSampleListener.class);

	// 0 false 1 true
	// private byte _isHasResponse = 0;

	private String _listenerID = "PushDataSampleListener";

	private String _listenerName = "PushDataSampleListener";

	private String _listenerDescription = "PushDataSampleListener";

	/**
	 * upload sample data
	 */
	private String _sampleData = null;

	public PushDataSampleListener() {
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

	/**
	 * @return the _sampleData
	 */
	public String getSampleData() {
		return _sampleData;
	}

	/**
	 * @param _sampleData
	 *            the _sampleData to set
	 */
	public void setSampleData(String sampleData) {
		this._sampleData = sampleData;
	}

	@Override
	public void fireSucceed(Object messageObj) throws PlatformException {

		// if (_isHasResponse == 0) {
		// _isHasResponse = 1;
		// StorageAdapterService.updateResponseCount();
		// }
		// try {
		// if (messageObj != null) {
		// final String contents = messageObj.toString();
		// if (contents != null && contents.trim().length() > 0) {
		// new Thread("PushDataSampleListener.fireSucceed.Thread") {
		// public void run() {
		// }
		// }.start();
		// return;
		// }
		// }
		// } catch (Exception e) {
		// theLogger.exception(e);
		// }

	}

	@Override
	public void fireError(Object messageObj) throws PlatformException {
		//java.net.ConnectException: Connection refused
		// if (_isHasResponse == 0) {
		// _isHasResponse = 1;
		// StorageAdapterService.updateResponseCount();
		// }
		if (messageObj != null) {
			theLogger.warn("fireError",messageObj.toString());
			theLogger.exception(new Exception(messageObj.toString()));
		}
		/*if (Assert.isEmptyString(this._sampleData) == false) {
			new Thread("PushDataSampleListener.fireError.storeSampleDateToFile") {

				// when upload sample data is in disconnection. We should handle
				// this case on later.
				// We should put sample data into local system. and upload it
				// when connection is ready.
				public void run() {
					try {
						MonitoringTaskCacheService monitoringTaskCacheService = ServiceHelper.findService(MonitoringTaskCacheService.class);
						// store the file content into local system.
						String filePath = FileSystemStorageUtil.storeSampleDateToFile(monitoringTaskCacheService.getFileStorageNameForTask(), _sampleData);
						if (filePath == null) {
							return;
						}
						// add file and waiting for uploading.
						monitoringTaskCacheService.addUploadSampleForDisconnection(filePath);
					} catch (Exception e) {
						theLogger.exception(e);
					}

				}
			}.start();
		}*/
	}
}