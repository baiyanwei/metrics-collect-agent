package com.secpro.platform.monitoring.agent.services;

import javax.xml.bind.annotation.XmlElement;

import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.log.utils.PlatformLogger;

/**
 * @author baiyanwei Jul 16, 2013
 * 
 *         采集器节点管理服务，提供采集器的LOCATION信息，提供采集器的性能指标与心跳影响
 * 
 */
@ServiceInfo(description = "Monitoring Node management, work on node information, node ability", configurationPath = "mca/services/MonitoringNodeService/")
public class MonitoringNodeService implements IService {
	private static PlatformLogger theLogger = PlatformLogger.getLogger(MonitoringNodeService.class);

	// l(location) - (string) location used internal to identify a dataCenter,
	// usually the provider-dataCenter (HB).
	@XmlElement(name = "nodeLocation", defaultValue = "HB")
	public String _nodeLocation = "";

	public String getNodeLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub
		//
		theLogger.info("locationName", this._nodeLocation);
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub

	}

	public void registerNode() {
		// TODO Auto-generated method stub

	}

	public void nodeStarted() {
		// TODO Auto-generated method stub

	}

	public void nodeStopped() {
		// TODO Auto-generated method stub

	}

	public void unregisterNode() {
		// TODO Auto-generated method stub

	}

}
