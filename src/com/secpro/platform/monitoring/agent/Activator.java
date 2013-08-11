package com.secpro.platform.monitoring.agent;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.Version;

import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.bri.SysLogBeaconInterface;
import com.secpro.platform.monitoring.agent.services.MetricStandardService;
import com.secpro.platform.monitoring.agent.services.MetricUploadService;
import com.secpro.platform.monitoring.agent.services.MonitoringEncryptService;
import com.secpro.platform.monitoring.agent.services.MonitoringNodeService;
import com.secpro.platform.monitoring.agent.services.MonitoringService;
import com.secpro.platform.monitoring.agent.services.MonitoringTaskCacheService;
import com.secpro.platform.monitoring.agent.services.StorageAdapterService;
import com.secpro.platform.monitoring.agent.storages.http.HTTPStorageAdapter;

/**
 * @author baiyanwei
 * Aug 10, 2013
 *
 * The MCA Activator
 *
 */
public class Activator implements BundleActivator, ServiceListener {
	// Logging Object
	private static PlatformLogger theLogger = PlatformLogger.getLogger(Activator.class);

	public static Version _version = null;
	private static BundleContext _context = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		theLogger.info("start the metric collect agent service");
		_context = context;
		_version = context.getBundle().getVersion();
		registerServices();
		// We need to register we are going to be running a TPU
		registerNode();
		_context.addServiceListener(this);
		theLogger.info("The metric collect agent service stared complete");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		unregisterNode();
	}

	public static BundleContext getContext() {
		return _context;
	}

	@Override
	public void serviceChanged(ServiceEvent event) {

		if (event.getType() == ServiceEvent.REGISTERED) {
			Object service = _context.getService(event.getServiceReference());
			if (service.getClass() == MetricStandardService.class) {
				try {
					_context.removeServiceListener(this);
					// SYSLOG interface service
					ServiceHelper.registerService(new SysLogBeaconInterface());
				} catch (Exception e) {
					theLogger.exception(e);
				}
			}
		}

	}

	/**
	 * @throws Exception
	 *             register mca all services.
	 */
	private void registerServices() throws Exception {
		//
		ServiceHelper.registerService(new MonitoringNodeService());
		//
		ServiceHelper.registerService(new MonitoringEncryptService());
		//
		ServiceHelper.registerService(new MonitoringTaskCacheService());
		//
		ServiceHelper.registerService(new HTTPStorageAdapter());
		//
		ServiceHelper.registerService(new StorageAdapterService());
		// Monitoring Service Mca main logic.
		ServiceHelper.registerService(new MonitoringService());
		// start metric upload service.
		ServiceHelper.registerService(new MetricUploadService());
		//
		ServiceHelper.registerService(new MetricStandardService());
		
		
	}

	/**
	 * register one node to server,provide its name ,ability and basic
	 * information.
	 */
	private void registerNode() {
		// We need to register we are going to be running a TPU
		try {
			//
			MonitoringNodeService nodeService = ServiceHelper.findService(MonitoringNodeService.class);
			nodeService.registerNode();
			// We are broadcasting to everyone that we are ready for business.
			nodeService.nodeStarted();
			
		} catch (Exception e) {
			theLogger.exception(e);
		}
	}

	private void unregisterNode() {
		try {
			// We are broadcasting to everyone that we are going offline.
			MonitoringNodeService nodeService = ServiceHelper.findService(MonitoringNodeService.class);
			nodeService.nodeStopped();
			nodeService.unregisterNode();
		} catch (Exception e) {
			theLogger.exception(e);
		}
	}
}