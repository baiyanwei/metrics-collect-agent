package com.secpro.unit.monitoring.services;

import com.secpro.platform.core.metrics.AbstractMetricMBean;
import com.secpro.platform.core.metrics.MetricUtils;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.log.utils.PlatformLogger;

@ServiceInfo(description = "Monitoring Service", configurationPath = "application/services/monitoringService/")
public class MonitoringService extends AbstractMetricMBean implements IService {
	private static PlatformLogger _logger = PlatformLogger.getLogger(MonitoringService.class);

	@Override
	public void start() throws Exception {
		_logger.info("startService");
		MetricUtils.registerMBean(this);
		//
		
	}

	@Override
	public void stop() throws Exception {
		_logger.info("stopService");
	}


}
