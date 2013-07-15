package com.secpro.platform.monitoring.agent.services;

import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.monitoring.agent.storages.IDataStorage;
import com.secpro.platform.monitoring.agent.workflow.MonitoringWorkflow;


/**
 * This class handles the interaction of the router with the Amazon.
 * 
 * @author bbuffone
 * 
 */
@ServiceInfo(description = "This service deals with Storing data into s3.", configurationPath = "router/services/storageAdapter/")
public class StorageAdapterService implements IService, IDataStorage {

	@XmlElement(name = "storageType")
	public String _storageType = "";
	// count for how many request MCA sends to DPU.
	public static long _sendRquest2Dpu = 0;
	// count for how many response MCA get from DPU.
	public static long _getResponse4Dpu = 0;
	//
	// PRIVATE INSTANCE VARIABLES
	//
	private IDataStorage _dataStorage = null;

	// private int count = 1;

	//
	// IService METHODS
	//
	@Override
	public void start() throws PlatformException {
		// Get the configured storate interface.
		_dataStorage = (IDataStorage) ServiceHelper.findService(_storageType);
		if (_dataStorage == null) {
			throw new PlatformException("Storage implementation was not found.", null);
		}
	}

	@Override
	public void stop() throws PlatformException {
	}

	//
	// IDataStorage METHODS
	//

	@Override
	public void uploadRawData(HashMap<String, String> messageInputAndRequestHeaders) throws PlatformException {
		if (_dataStorage != null) {
			_dataStorage.uploadRawData(messageInputAndRequestHeaders);
		}
	}

	@Override
	public void executeFetchMessage(List<MonitoringWorkflow> workflows) {
		// TODO Auto-generated method stub
		if (_dataStorage != null) {
			_dataStorage.executeFetchMessage(workflows);
		}
	}

	public static void updateRquest2DpuCount() {
		if (Long.MAX_VALUE == (_sendRquest2Dpu + 1)) {
			_sendRquest2Dpu = 0;
			_getResponse4Dpu = 0;
		}
		_sendRquest2Dpu++;
	}

	public static void updateResponse4DpuCount() {
		if (Long.MAX_VALUE == (_getResponse4Dpu + 1)) {
			_sendRquest2Dpu = 0;
			_getResponse4Dpu = 0;
		}
		_getResponse4Dpu++;
	}
}
