package com.secpro.platform.monitoring.agent.services;

import javax.management.DynamicMBean;
import javax.xml.bind.annotation.XmlElement;

import com.secpro.platform.core.metrics.AbstractMetricMBean;
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
public class MonitoringNodeService extends AbstractMetricMBean implements IService, DynamicMBean {
	final private static PlatformLogger theLogger = PlatformLogger.getLogger(MonitoringNodeService.class);
	//
	@XmlElement(name = "jmxObjectName", defaultValue = "secpro.mca:type=MonitoringNodeService")
	public String _jmxObjectName = "secpro.mca:type=MonitoringNodeService";
	
	@XmlElement(name = "nodeLocation", defaultValue = "HB")
	public String _nodeLocation = "";

	@Override
	public void start() throws Exception {
		this.registerMBean(_jmxObjectName, this);
		theLogger.info("startUp", this._nodeLocation);
	}

	@Override
	public void stop() throws Exception {
	}

	public void registerNode() {
		// TODO Auto-generated method stub
		theLogger.info("registerNode the metric collect agent service, the agent location is " + _nodeLocation);
	}

	public void nodeStarted() {
		// TODO Auto-generated method stub

	}

	public void nodeStopped() {
		// TODO Auto-generated method stub

	}

	public void unregisterNode() {
		// TODO Auto-generated method stub
		theLogger.info("unregisterNode the metric collect agent service, the agent location is " + _nodeLocation);
	}

	public void setServer(String fileName) {

	}
}
