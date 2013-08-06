package com.secpro.platform.monitoring.agent.services;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.bind.annotation.XmlElement;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.metrics.AbstractMetricMBean;
import com.secpro.platform.core.metrics.Metric;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.Activator;
import com.secpro.platform.monitoring.agent.actions.FetchTSSTaskAction;
import com.secpro.platform.monitoring.agent.workflow.MonitoringWorkflow;

/**
 * @author baiyanwei Jul 6, 2013
 * 
 */
@ServiceInfo(description = "The main service of Metrics-Collect-Agent", configurationPath = "mca/services/MonitoringService/")
public class MonitoringService extends AbstractMetricMBean implements IService, DynamicMBean {

	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(MonitoringService.class);

	@XmlElement(name = "messageTimeout", type = Long.class, defaultValue = "120000")
	public Long _messageTimeout = new Long(120000);

	@XmlElement(name = "operationCapabilities", defaultValue = "snmp,ssh")
	public String _operationCapabilities = "snmp,ssh";

	// Time interval when fetch task from the TSS. 
	@XmlElement(name = "fetchTSSTaskInterval", type = Long.class, defaultValue = "2000")
	public long _fetchTSSTaskInterval = 2000L;

	/**
	 * Maximum number of concurrent workflows to be run concurrently.
	 */
	@XmlElement(name = "concurrentWorkflows", type = Long.class, defaultValue = "1")
	public Long _concurrentWorkflows = new Long(1);

	/**
	 * This is the number of workflows that need to be completed before we read
	 * and process the tasks queues.
	 */
	public Long _workflowThreshold = new Long(1);

	@XmlElement(name = "jmxObjectName", defaultValue = "secpro:type=MonitoringService")
	public String _jmxObjectName = "secpro:type=MonitoringService";

	// cache the version number
	@Metric(description = "The version number of MCA")
	public String _version = Activator._version.toString();
	
	public boolean _isFetchCacheTaskOnError=false;
	

	// This is the list of workflows that the task queue will
	// use to process tasks. the more of these, the more tasks
	// can be processed at once.
	private List<MonitoringWorkflow> _monitoringWorkflows = new ArrayList<MonitoringWorkflow>();
	//
	// ERROR FIELDS
	//
	public String _lastError = "";
	public long _timeLastTaskFinished = 0;

	// polling DPU task timer caller.
	private Timer _fetchTSSTaskTimer = null;

	@Override
	public void start() throws PlatformException {
		//
		// This will create the current workflows.
		createMonitoringWorkflows(_workflowThreshold.intValue());
		// register itself as dynamic bean
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName objectName = new ObjectName(_jmxObjectName);
			mBeanServer.registerMBean(this, objectName);
		} catch (Exception e) {
			theLogger.exception("JMX error when registering the MonitoringService to JMX", e);
		}

		// load the Fetch task timer
		_fetchTSSTaskTimer = new Timer("MonitoringService._fetchTSSTaskTimer");
		_fetchTSSTaskTimer.schedule(new FetchTSSTaskAction(this), 20000L, _fetchTSSTaskInterval);

		theLogger.info("startUp", _workflowThreshold.intValue(), _fetchTSSTaskInterval, _operationCapabilities);
	}

	@Override
	public void stop() throws PlatformException {
		// unregister itself
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName objectName = new ObjectName(_jmxObjectName);
			mBeanServer.unregisterMBean(objectName);

			// unregister all the MonitoringWorkflow
			synchronized (_monitoringWorkflows) {
				Iterator<MonitoringWorkflow> iterator = _monitoringWorkflows.iterator();
				while (iterator.hasNext() == true) {
					MonitoringWorkflow workflow = iterator.next();
					if (workflow._objectName != null)
						try {
							mBeanServer.unregisterMBean(workflow._objectName);
						} catch (Exception e) {
							theLogger.exception("JMX error when unregistering the MonitoringWorkflow", e);
						}
				}

				_monitoringWorkflows = null;
			}
		} catch (Exception e) {
			theLogger.exception("JMX error when unregistering the MonitoringService", e);
		}
		_fetchTSSTaskTimer.cancel();
	}

	@Metric(description = "restart the MonitoringService", ignoreInSummary = true)
	public void restart() throws PlatformException {
		stop();

		_monitoringWorkflows = new ArrayList<MonitoringWorkflow>();

		start();
	}

	//
	// PUBLIC GETTERS AND SETTERS
	//
	/**
	 * @return get all workflow in waiting.
	 */
	public List<MonitoringWorkflow> getWorkflowsWithStatus(byte status) {
		List<MonitoringWorkflow> reList = new ArrayList<MonitoringWorkflow>();
		Iterator<MonitoringWorkflow> iterator = _monitoringWorkflows.iterator();
		while (iterator.hasNext() == true) {
			MonitoringWorkflow workflow = iterator.next();
			if (workflow.getStatus() == status) {
				reList.add(workflow);
			}

		}
		return reList;
	}

	/**
	 * This method returns a list of only the completed monitoring workflows. If
	 * there isn't enough items than mode will be created and added to the
	 * available work flows. This method of growing the list give the monitor a
	 * faster startup time and hopefully will be able to maximize available
	 * resources.
	 * 
	 * @return
	 */
	public List<MonitoringWorkflow> getWorkflowsForTask() {
		List<MonitoringWorkflow> completed = new ArrayList<MonitoringWorkflow>();
		synchronized (_monitoringWorkflows) {
			Iterator<MonitoringWorkflow> iterator = _monitoringWorkflows.iterator();
			while (iterator.hasNext() == true) {
				MonitoringWorkflow workflow = iterator.next();
				synchronized (workflow) {
					if (workflow.isReady()) {
						workflow.setWorkflowToWait();
						completed.add(workflow);
					}
				}
			}
			// increase workflow for many task.
			if (completed.size() < _workflowThreshold.intValue() && _monitoringWorkflows.size() < _concurrentWorkflows.intValue()) {
				createMonitoringWorkflows(_workflowThreshold.intValue() - completed.size());
			}
		}
		return completed;
	}

	//
	// PRIVATE METHODS
	//
	/**
	 * Create "N" number of workflows based on the _concurrentWorkflows
	 * property.
	 */
	private void createMonitoringWorkflows(int size) {
		SimpleDateFormat bartDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		for (int index = 0; index < size; index++) {
			MonitoringWorkflow workflow = new MonitoringWorkflow(this);
			try {
				workflow.start();
				_monitoringWorkflows.add(workflow);
				// register workflows as dynamic bean
				MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
				try {
					Calendar cal = Calendar.getInstance();
					long currentTime = System.currentTimeMillis();
					cal.setTimeInMillis(currentTime);

					// Need to create a unique name for the object.
					ObjectName objectName = new ObjectName("secpro:type=MonitoringWorkflow" + String.valueOf(bartDateFormat.format(cal.getTime())) + "-"
							+ String.valueOf(_monitoringWorkflows.size()));
					mBeanServer.registerMBean(workflow, objectName);
					workflow._objectName = objectName;
				} catch (Exception e) {
					theLogger.exception("createMonitoringWorkflows", e);
				}
			} catch (PlatformException e) {
				theLogger.exception("createMonitoringWorkflows", e);
			}
		}
	}

	//
	// STATISTICAL METHODS
	//
	@Metric(description = "get the error rate of the MonitoringService")
	public double getErrorRate() {
		if (MonitoringWorkflow._totalTasks == 0) {
			return 0.0;
		} else {
			return (double) MonitoringWorkflow._totalErrors / (double) MonitoringWorkflow._totalTasks;
		}
	}

	@Metric(description = "get the processing average of MonitoringService")
	public double getProcessingAverage() {
		if (MonitoringWorkflow._totalTasks == 0) {
			return 0;
		} else {
			return (double) MonitoringWorkflow._totalTimeOfProcessing / (double) MonitoringWorkflow._totalTasks;
		}
	}

	@Metric(description = "get the last error of the MonitoringService")
	public String getLastError() {
		return _lastError;
	}

	@Metric(description = "get the system total memory status")
	public long getSystemTotalMemory() {
		return Runtime.getRuntime().totalMemory();
	}

	@Metric(description = "get the system free memory status")
	public long getSystemFreeMemory() {
		return Runtime.getRuntime().freeMemory();
	}

	@Metric(description = "get the system load average status")
	public double getSystemLoadAverage() {
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		double cpuTime = 0;
		if (os instanceof OperatingSystemMXBean) {
			cpuTime = ((OperatingSystemMXBean) os).getSystemLoadAverage();
		}
		return cpuTime;
	}

	@Metric(description = "get size of the task queue times ")
	public int getTaskQueueTimesSize() {
		return 0;
	}

	@Metric(description = "get size of the in progress workflows")
	public int getInProgressWorkflowsSize() {
		return getWorkflowsWithStatus(MonitoringWorkflow.WORKFLOW_INPROCESSING).size();
	}

	@Metric(description = "get the time since last task")
	public long getTimeSinceLastTask() {
		if (this._timeLastTaskFinished == 0) {
			return 0;
		} else {
			return System.nanoTime() - this._timeLastTaskFinished;
		}
	}
}