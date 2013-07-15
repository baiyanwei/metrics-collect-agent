package com.secpro.platform.monitoring.agent.actions;

import java.util.List;
import java.util.TimerTask;

import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.services.MonitoringService;
import com.secpro.platform.monitoring.agent.services.StorageAdapterService;
import com.secpro.platform.monitoring.agent.workflow.MonitoringWorkflow;

/**
 * @author baiyanwei Jul 6, 2013
 * 
 *         Fetch Server Task Action
 */
public class FetchTSSTaskAction extends TimerTask {
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(FetchTSSTaskAction.class);
	private static boolean isServiceReady = false;
	//
	private MonitoringService _monitoringService = null;

	//

	public FetchTSSTaskAction(MonitoringService monitoringService) {
		_monitoringService = monitoringService;
	}

	@Override
	public void run() {
		// check in that are all request services start up
		if (isServiceReady == false && isServiceComplete() == false) {
			return;
		}
		// get idle workflow and mark them are working.
		List<MonitoringWorkflow> workflows = _monitoringService.getWorkflowsForTask();
		if (workflows.size() != 0) {
			StorageAdapterService storageAdapter = ServiceHelper.findService(StorageAdapterService.class);
			storageAdapter.executeFetchMessage(workflows);
		}
		theLogger.debug("fetchTask", workflows.size());
	}

	/**
	 * check service is running or not.
	 * 
	 * @return
	 */
	private boolean isServiceComplete() {
		isServiceReady = true;
		return false;
	}
}
