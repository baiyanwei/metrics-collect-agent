package com.secpro.platform.monitoring.agent.storages;

import java.util.List;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.monitoring.agent.workflow.MonitoringWorkflow;


/**
 * This interface defines the storage mechanism.
 * 
 * @author baiyanwei
 * Jul 13, 2013
 *
 */
public interface IDataStorage {

	/**
	 * update capture data to server
	 * 
	 * @param rawDataObj
	 * @throws PlatformException
	 */
	public void uploadRawData(Object rawDataObj) throws PlatformException;

	
	/**
	 * @param workflows
	 * fetch task from target server ,if no workflow free, dont't fetch
	 */
	public void executeFetchMessage(List<MonitoringWorkflow> workflows);
}
