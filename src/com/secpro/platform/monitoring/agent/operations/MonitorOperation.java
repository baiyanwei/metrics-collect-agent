package com.secpro.platform.monitoring.agent.operations;

import com.secpro.platform.core.configuration.GenericConfiguration;
import com.secpro.platform.monitoring.agent.services.MonitoringService;
import com.secpro.platform.monitoring.agent.workflow.MonitoringWorkflow;

/**
 * @author baiyanwei Jul 21, 2013
 * 
 *         monitor operation basic abstract class .all operation should extends
 *         this class .and implement its method. And do your business in it.
 * 
 */
public abstract class MonitorOperation implements IMonitorOperation {

	public static long MILLI_TO_NANO = 1000000;
	public static long _totalOperationComplete = 0;
	public static long _totalOperationError = 0;
	protected String _operationID = "MonitorOperation";
	protected MonitoringService _monitoringService = null;
	protected GenericConfiguration _operationConfiguration = null;
	protected IOperationListener _operationListener = null;
	protected MonitoringWorkflow _monitoringWorkflow = null;
	//The operation execution message.
	protected Object _resultMessageObject = null;

	// this is used to mark the monitor operation in progress. Need
	// to set to true
	protected boolean _bInOperation = false;

	// Keeps track of the number of times this operation object has been used.
	protected int _objectUsed = 0;

	// This object is used to report errors
	public OperationError _operationError = new OperationError();

	//
	// CONSTUCTOR
	//
	public MonitorOperation() {
	}

	public OperationError getOperationError() {
		return _operationError;
	}

	//
	// EVENT METHODS
	//
	public void addOperationListener(IOperationListener operationListener) {
		_operationListener = operationListener;
	}

	public void removeOperationListener(IOperationListener operationListener) {
		_operationListener = null;
	}

	/**
	 * Coverts a nano second measurement into a millisecond measurement.
	 * 
	 * @param nanoTime
	 * @return
	 */
	public long nanoToMillis(long nanoTime) {
		return nanoTime / MILLI_TO_NANO;
	}

	//
	// PROTECTED METHODS
	//
	protected void fireCompletedSuccessfully() {
		if (_operationListener != null) {
			countOperationComplete();
			_operationListener.operationCompletedSuccessfully(this);
		}
	}

	protected void fireError(OperationError operationError) {
		if (_operationListener != null) {
			countOperationError();
			_operationListener.operationError(this, operationError);
		}
	}

	protected void countOperationComplete() {
		if (_totalOperationComplete == Long.MAX_VALUE || _totalOperationComplete < 0) {
			_totalOperationComplete = 0;
		}
		_totalOperationComplete++;
	}

	protected void countOperationError() {
		if (_totalOperationError == Long.MAX_VALUE || _totalOperationError < 0) {
			_totalOperationError = 0;
		}
		_totalOperationError++;
	}

	/**
	 * @return Get timeout code of operation
	 */
	public int getErrorTypeAboutTimeout() {
		return OperationError.McaError.WORKFLOW_TOMEOUT;
	}

	/**
	 * @return get timeout message of operation.
	 */
	public String getErrorMessageAboutTimeout() {
		return OperationError.McaError.WORKFLOW_TOMEOUT_MESSAGE;
	}

	@Override
	public void configure(GenericConfiguration operationConfiguration, MonitoringService monitoringService, MonitoringWorkflow monitoringWorkflow) {
		_operationConfiguration = operationConfiguration;
		_monitoringService = monitoringService;
		_monitoringWorkflow = monitoringWorkflow;
	}

	@Override
	public void setOperationID(String operationID) {
		this._operationID = operationID;
	}

	@Override
	public String getOperationsID() {
		return this._operationID;
	}
	
	@Override
	public Object getMessageObject() {
		return this._resultMessageObject;
	}
}
