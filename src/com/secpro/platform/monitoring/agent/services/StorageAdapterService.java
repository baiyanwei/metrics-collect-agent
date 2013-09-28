package com.secpro.platform.monitoring.agent.services;

import java.util.List;

import javax.management.DynamicMBean;
import javax.xml.bind.annotation.XmlElement;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.metrics.AbstractMetricMBean;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.monitoring.agent.storages.IDataStorage;
import com.secpro.platform.monitoring.agent.workflow.MonitoringWorkflow;

/**
 * @author baiyanwei Jul 16, 2013
 * 
 *         存储接口适配器 当其它服务在使用存储服务时，直接本服务， 具体的存储现实方式由StorageAdapterService根据 配置生产。
 * 
 */
@ServiceInfo(description = "storage adpater ,define implements", configurationPath = "mca/services/StorageAdapterService/")
public class StorageAdapterService extends AbstractMetricMBean implements IService, IDataStorage, DynamicMBean {

	@XmlElement(name = "storageType")
	public String _storageType = "";
	@XmlElement(name = "jmxObjectName", defaultValue = "secpro:type=StorageAdapterService")
	public String _jmxObjectName = "secpro:type=StorageAdapterService";
	//
	// count for how many request MCA sends to DPU.
	public long _sendRquestCount = 0;
	// count for how many response MCA get from DPU.
	public long _getResponseCount = 0;
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
		// Get the configured storage interface.
		_dataStorage = (IDataStorage) ServiceHelper.findService(_storageType);
		if (_dataStorage == null) {
			throw new PlatformException("Storage implementation was not found.", null);
		}
		// register itself as dynamic bean
		this.registerMBean(_jmxObjectName, this);
	}

	@Override
	public void stop() throws PlatformException {
		_dataStorage = null;
		// unregister itself
		this.unRegisterMBean(_jmxObjectName);
	}

	//
	// IDataStorage METHODS
	//

	@Override
	public void uploadRawData(Object rawDataObj) throws PlatformException {
		if (_dataStorage != null) {
			updateRquestCount();
			_dataStorage.uploadRawData(rawDataObj);
		}
	}

	@Override
	public void executeFetchMessage(List<MonitoringWorkflow> workflows) {
		if (_dataStorage != null) {
			updateRquestCount();
			_dataStorage.executeFetchMessage(workflows);
		}
	}

	public void updateRquestCount() {
		if (Long.MAX_VALUE == (_sendRquestCount + 1)) {
			_sendRquestCount = 0;
			_sendRquestCount = 0;
		}
		_sendRquestCount++;
	}

	public void updateResponseCount() {
		if (Long.MAX_VALUE == (_getResponseCount + 1)) {
			_getResponseCount = 0;
			_getResponseCount = 0;
		}
		_getResponseCount++;
	}
}
