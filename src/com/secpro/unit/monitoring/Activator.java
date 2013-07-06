package com.secpro.unit.monitoring;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.unit.monitoring.services.MonitoringService;

public class Activator implements BundleActivator {

	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		// register services
		ServiceHelper.registerService(new MonitoringService());
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
