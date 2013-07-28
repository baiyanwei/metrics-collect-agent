package com.secpro.platform.monitoring.agent.operations.snmp;

import java.util.HashMap;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.monitoring.agent.operations.MonitorOperation;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;

public class SNMPOperation extends MonitorOperation {

	@Override
	public void doIt(MonitoringTask task) throws PlatformException {
		// set your collecting result into message.
		System.out.println("SNMPOperation>>do task:" + task.getTaskDescription());
		//get task meta data
		HashMap<String,Object> metaMap= task.getTaskMetaData();
		String user=(String) metaMap.get("userName");
		String passwd=(String) metaMap.get("password");
		String sheelCommand=(String) metaMap.get("shell");
 		/*
		 * "ssh": '{' "mid": "{0}", "t": "{1}", "ip": "{2}", "s": "{3}","c":"{4}" '}'
		 */
		//build collection for result.
		HashMap<String, String> messageInputAndRequestHeaders = this._monitoringWorkflow.getMessageInputAndRequestHeaders(this._operationID, task.getMonitorID(),
				task.getTimestamp(), task.getPropertyString(MonitoringTask.TASK_TARGET_IP_PROPERTY_NAME), "sheel command",
				"result message");
		this._monitoringWorkflow.createResultsMessage(this._operationID, messageInputAndRequestHeaders);
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
