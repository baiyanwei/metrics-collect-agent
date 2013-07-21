package com.secpro.platform.monitoring.agent.operations;

import com.secpro.platform.core.configuration.GenericConfiguration;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.monitoring.agent.services.MonitoringService;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;
import com.secpro.platform.monitoring.agent.workflow.MonitoringWorkflow;


/**
 * @author baiyanwei
 * Jul 21, 2013
 *
 * Define the operation's ability in Metrics-Collect-Agent.
 *
 */
public interface IMonitorOperation {
	/**
	 * @param operationID
	 * 
	 * set operation ID
	 */
	public void setOperationID(String operationID);
	/**
	 * @return
	 * 
	 * get operation ID
	 */
	public String getOperationsID();
	/**
	 * @throws PlatformException
	 * 
	 * start this operation, build ENV. and configuration.
	 * register into WorkFlow
	 */
	public void start() throws PlatformException;
	/**
	 * @throws PlatformException
	 * 
	 * destroy operation ,clear all ENV.
	 * unregister from WorkFlow
	 */
	public void destroy() throws PlatformException;
	/**
	 * @param task
	 * @throws PlatformException
	 * 
	 * do your business in doIt
	 */
	public void doIt(MonitoringTask task) throws PlatformException;
	/**
	 * @param task
	 * @throws PlatformException
	 * 
	 * stop your business in stopIt.
	 */
	public void stopIt(MonitoringTask task) throws PlatformException;
	/**
	 * @param operationConfiguration
	 * @param monitoringService
	 * @param monitoringWorkflow
	 * 
	 * set operation Env.
	 */
	public void configure(GenericConfiguration operationConfiguration, MonitoringService monitoringService, MonitoringWorkflow monitoringWorkflow);
	
	/**
	 * @return
	 * 
	 * get result Message object
	 */
	public Object getMessageObject();
}
