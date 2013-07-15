package com.secpro.platform.monitoring.agent.operations;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;


public interface IMonitorOperation {
	public void start() throws PlatformException;
	public void destroy() throws PlatformException;
	public void doIt(MonitoringTask task) throws PlatformException;
	public void stopIt(MonitoringTask task) throws PlatformException;	
}
