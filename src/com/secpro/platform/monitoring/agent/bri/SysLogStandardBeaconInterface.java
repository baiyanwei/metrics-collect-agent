package com.secpro.platform.monitoring.agent.bri;

import com.secpro.platform.api.common.http.server.HttpServer;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.monitoring.agent.services.MetricStandardService;

/**
 * @author baiyanwei Jul 21, 2013
 * 
 *         SysLog listenter
 * 
 */
public class SysLogStandardBeaconInterface extends HttpServer {

	@Override
	public void start() throws Exception {
		super.start();
		MetricStandardService metricStandardService = ServiceHelper.findService(MetricStandardService.class);
		if (metricStandardService != null) {
			metricStandardService._callbackPort = this.port;
		}
	}
}
