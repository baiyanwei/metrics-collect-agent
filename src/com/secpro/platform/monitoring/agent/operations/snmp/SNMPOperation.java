package com.secpro.platform.monitoring.agent.operations.snmp;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.monitoring.agent.operations.MonitorOperation;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;

public class SNMPOperation extends MonitorOperation {

	@Override
	public void doIt(MonitoringTask task) throws PlatformException {
		// set your collecting result into message.
		this._resultMessageObject = "SNMPOperation is executed";
		this.fireCompletedSuccessfully();
	}

	@Override
	public void stopIt(MonitoringTask task) throws PlatformException {
		this._operationError._message = "What is happen here?";
		this.fireError(this._operationError);
	}

	@Override
	public void start() throws PlatformException {
		// TODO Auto-generated method stub
		System.out.println(">>>>>>>>test SNMPOperation");
	}

	@Override
	public void destroy() throws PlatformException {
		this._operationError = null;
	}
}
