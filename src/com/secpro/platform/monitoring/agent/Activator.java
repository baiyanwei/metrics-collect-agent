package com.secpro.platform.monitoring.agent;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.Version;

import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.monitoring.agent.services.MetricUploadService;
import com.secpro.platform.monitoring.agent.services.MonitoringNodeService;
import com.secpro.platform.monitoring.agent.services.MonitoringService;
import com.secpro.platform.monitoring.agent.services.StorageAdapterService;
import com.secpro.platform.monitoring.agent.storages.http.HTTPStorageAdapter;

public class Activator implements BundleActivator, ServiceListener {
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
		_context = context;
		_version = context.getBundle().getVersion();
		registerServices();
		// synchronized (check_necessary_services) {
		// if (ServiceHelper.findService(NodeService.class) == null) {
		// check_necessary_services.add(NodeService.class);
		// }

		// _context.addServiceListener(this);
		// }

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
			// Object service =
			// _context.getService(event.getServiceReference());
			// synchronized (check_necessary_services) {
			// if (check_necessary_services.contains(service.getClass())) {
			// check_necessary_services.remove(service.getClass());
			// }
			// if (check_necessary_services.isEmpty()) {
			try {
				_context.removeServiceListener(this);
				registerServices();
			} catch (Exception e) {
				e.printStackTrace();
			}
			// }
			// }
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
		ServiceHelper.registerService(new HTTPStorageAdapter());
		
		//
		ServiceHelper.registerService(new StorageAdapterService());
		//
		// Monitoring Service Mca main logic.
		ServiceHelper.registerService(new MonitoringService());
		//
		//start metric upload service.
		ServiceHelper.registerService(new MetricUploadService());
		//

		// _context.addServiceListener(new ServiceListener() {
		//
		// @Override
		// public void serviceChanged(ServiceEvent event) {
		// if (event.getType() == ServiceEvent.REGISTERED) {
		// Object[] obj = (Object[])
		// event.getServiceReference().getProperty("objectClass");
		// if (obj[0].equals(LoggingService.class.getName())) {
		//
		// MonitoringNodeService mNodeService =
		// OSGiServiceHelper.findService(MonitoringNodeService.class);
		// mNodeService.updateLoggingPrefix();
		// _context.removeServiceListener(this);
		// }
		// }
		// }
		//
		// });
		// We need to register we are going to be running a TPU
		registerNode();
	}

	/**
	 * register one node to server,provide its name ,ability and basic
	 * information.
	 */
	private void registerNode() {
		// We need to register we are going to be running a TPU
		try {
			//
			((MonitoringNodeService) ServiceHelper.findService(MonitoringNodeService.class)).registerNode();
			// We are broadcasting to everyone that we are ready for business.
			((MonitoringNodeService) ServiceHelper.findService(MonitoringNodeService.class)).nodeStarted();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void unregisterNode() {
		try {
			// We are broadcasting to everyone that we are going offline.
			((MonitoringNodeService) ServiceHelper.findService(MonitoringNodeService.class)).nodeStopped();
			((MonitoringNodeService) ServiceHelper.findService(MonitoringNodeService.class)).unregisterNode();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}