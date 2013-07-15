package com.secpro.platform.monitoring.agent.storages;

import java.util.HashMap;
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
	 * @param isReattime
	 * @param messageInputAndRequestHeaders
	 * @throws RouterException
	 * update capture data to server
	 */
	public void uploadRawData(HashMap<String, String> messageInputAndRequestHeaders) throws PlatformException;

	
	/**
	 * @param workflows
	 * fetch task from target server ,if no workflow free, dont't fetch
	 */
	public void executeFetchMessage(List<MonitoringWorkflow> workflows);
}
