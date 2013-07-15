package com.secpro.platform.monitoring.agent.operations;

/**
 * This interface is used to allow an operation to communicate 
 * information back to the MonitorWorkflow object
 * 
 * @author bbuffone
 *
 */
public interface IOperationListener {

	public void operationCompletedSuccessfully(IMonitorOperation operation);
	public void operationError(IMonitorOperation operation, OperationError operationError);
	
}
