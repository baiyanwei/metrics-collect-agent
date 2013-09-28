package com.secpro.platform.monitoring.agent.actions;

import java.util.TimerTask;

import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.services.MonitoringTaskCacheService;

/**
 * @author baiyanwei Aug 3, 2013
 * 
 * 
 *         according to current point , remove the cache task before two days
 *         age.
 */
public class CacheTaskManageAction extends TimerTask {
	final private static PlatformLogger theLogger = PlatformLogger.getLogger(CacheTaskManageAction.class);
	private MonitoringTaskCacheService _monitoringTaskCacheService = null;

	//
	public CacheTaskManageAction(MonitoringTaskCacheService monitoringTaskCacheService) {
		this._monitoringTaskCacheService = monitoringTaskCacheService;
	}

	@Override
	public void run() {
		theLogger.info("startAction");
		long twoDaysAgoPoint = System.currentTimeMillis() - MonitoringTaskCacheService.DAY_MSECONDS * 2;
		int clearCacheTaskResult = _monitoringTaskCacheService.clearCacheTaskByTimePoint(twoDaysAgoPoint);
		theLogger.info("removeResult", String.valueOf(clearCacheTaskResult));
	}
}
