package com.secpro.platform.monitoring.agent.workflow;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.management.DynamicMBean;
import javax.management.ObjectName;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.secpro.platform.core.configuration.GenericConfiguration;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.message.MessagePreparer;
import com.secpro.platform.core.metrics.AbstractMetricMBean;
import com.secpro.platform.core.metrics.Metric;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.utils.Utils;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.Activator;
import com.secpro.platform.monitoring.agent.operations.IMonitorOperation;
import com.secpro.platform.monitoring.agent.operations.IOperationListener;
import com.secpro.platform.monitoring.agent.operations.MonitorOperation;
import com.secpro.platform.monitoring.agent.operations.OperationError;
import com.secpro.platform.monitoring.agent.services.MonitoringService;
import com.secpro.platform.monitoring.agent.services.StorageAdapterService;

/**
 * @author baiyanwei Jul 18, 2013
 * 
 * 
 *         The main Logic unit in MCA, work to create operation resource ,handle
 *         operation , execute job on sort, collect operation return message,
 *         limit running time.
 * 
 */
public class MonitoringWorkflow extends AbstractMetricMBean implements IService, IOperationListener, DynamicMBean {
	// WORKFLOW's status
	public static final byte WORKFLOW_START = 0;
	public static final byte WORKFLOW_READY = 1;
	public static final byte WORKFLOW_WAIT = 2;
	public static final byte WORKFLOW_INPROCESSING = 3;
	public static final byte WORKFLOW_FINISH = 4;
	public static final byte WORKFLOW_STOP = 5;
	//
	//
	public static final long _waitForFetchTime = 10000L;
	//
	// Logging Object
	private static PlatformLogger logger = PlatformLogger.getLogger(Activator.class);

	//
	// PUBLIC STATIC FINAL INSTANCE VARIABLES
	//
	public static final String OPERATION_EXTENSION_POINT_ID = "com.secpro.platform.monitoring.agent.metrics_collect_agent_operations";

	private static HashMap<String, MessageFormat> _messageFormatters = new HashMap<String, MessageFormat>();
	private static String[] _messageFiles = new String[] { "error.js", "error-scheduled.js", "ssh.js", "ssh-scheduled.js", "finished.js", "finished-scheduled.js", "snmp.js",
			"snmp-scheduled.js", "result.js", "result-scheduled.js" };

	//
	// We need to create the message formatters used in the workflow manager
	//
	static {
		createMessageFormatters();
	};

	//
	// PRIVATE INSTANCE VARIABLES
	//
	private List<MonitorOperation> _operationList = new ArrayList<MonitorOperation>();
	private HashMap<String, MonitorOperation> _id2Operation = new HashMap<String, MonitorOperation>();
	private HashMap<String, String> _scheduledResultsData = new HashMap<String, String>();

	/*
	 * Contains the list of operations that still need to be completed.
	 */
	private Queue<MonitorOperation> _operationToCompleteList = new LinkedList<MonitorOperation>();

	private MonitoringService _monitoringService = null;
	private StorageAdapterService _storageAdapter = null;
	private MonitorOperation _currentOperation = null;

	private long _startTime = 0;
	private MonitoringTask _monitoringTask = null;

	private String _dataFolder = "";

	// Identifies the number of tasks that were executed when the browser
	// was last closed.
	private boolean _isBundle = true;

	//
	// TOTAL INFORMATION FIELDS
	//
	@Metric(description = "total errors")
	public static long _totalErrors = 0;
	@Metric(description = "total tasks")
	public static long _totalTasks = 0;
	@Metric(description = "total time of processing")
	public static long _totalTimeOfProcessing = 0;

	// objectName associates with this instance
	public ObjectName _objectName = null;

	public long totalErrors = 0;

	public long totalTasks = 0;

	protected OperationError _errorOperation = new OperationError();

	protected Date _executedAt = null;
	//
	private Thread timer = null;
	// 0==start, 1==ready ,2==in processing, 3==finish ,4==stop
	private byte _status = 0;

	//
	// CONSTRUCTOR
	//
	public MonitoringWorkflow(MonitoringService monitoringService) {
		_monitoringService = monitoringService;
	}

	//
	// PUBLIC GETTER & SETTERS
	//
	/**
	 * This method returns true if and only if all the operations have been
	 * successfully processed.
	 * 
	 * @return
	 */

	public synchronized boolean isReady() {
		return (this._status <= WORKFLOW_READY ? true : false);
	}

	public synchronized boolean isWaiting() {
		return (this._status == WORKFLOW_WAIT ? true : false);
	}

	public synchronized boolean isInProcessing() {
		return (this._status == WORKFLOW_INPROCESSING ? true : false);
	}

	public synchronized boolean isComplete() {
		return ((this._status == WORKFLOW_FINISH || this._status == WORKFLOW_READY) ? true : false);
	}

	public synchronized byte getStatus() {
		return this._status;
	}

	public String toString() {
		return this.hashCode() + "#" + this._status;
	}

	/**
	 * recycle worklfow to ready status. ANY=>WORKFLOW_READY
	 */
	@SuppressWarnings("deprecation")
	public synchronized void recycleForReady() {
		switch (this._status) {
		case WORKFLOW_WAIT: {
			this._status = WORKFLOW_READY;
			if (this.timer != null) {
				this.timer.stop();
				this.timer = null;
			}
		}
			break;
		case WORKFLOW_INPROCESSING: {
			fixTimedOut();
		}
			break;
		case WORKFLOW_FINISH:
			this._status = WORKFLOW_READY;
			if (this.timer != null) {
				this.timer.stop();
				this.timer = null;
			}
			break;
		}
	}

	/**
	 * apply this workflow to using ,and set it into waiting status.
	 * WORKFLOW_READY=>WORKFLOW_WAIT
	 */
	public synchronized void setWorkflowToWait() {
		this._status = WORKFLOW_WAIT;
		activateTime(_waitForFetchTime);
	}

	/**
	 * set workflow into running status, but this just be called in doIt().
	 * WORKFLOW_WAIT=>WORKFLOW_INPROCESSING
	 */
	private void setWorkflowToRunning() {
		this._status = WORKFLOW_INPROCESSING;
		activateTime(_monitoringService._messageTimeout.longValue());
	}

	/**
	 * @param seconds
	 *            make a timer for recycle workflows
	 */
	@SuppressWarnings("deprecation")
	private void activateTime(final long seconds) {
		if (this.timer != null) {
			this.timer.stop();
			this.timer = null;
		}
		this.timer = new Thread() {
			public void run() {
				try {
					sleep(seconds);
					recycleForReady();
				} catch (Exception e) {
					// e.printStackTrace();
				} catch (java.lang.ThreadDeath err) {
					// Catch the java.lang.ThreadDeath
					// err.printStackTrace();
				}
			}
		};
		this.timer.start();
	}

	//
	public boolean isBundle() {
		return this._isBundle;
	}

	public long getStartTime() {
		return _startTime;
	}

	public long getTotalTime() {
		return System.currentTimeMillis() - getStartTime();
	}

	public String getDataFolder() {
		return _dataFolder;
	}

	public double getAverageTaskTime() {
		return (double) _totalTimeOfProcessing / (double) _totalTasks;
	}

	public MonitoringService getMonitoringService() {
		return _monitoringService;
	}

	/**
	 * This method returns the operation specificy by the input task
	 * 
	 * @param task
	 * @return
	 */
	public MonitorOperation getOperation(String operation) {
		return _id2Operation.get(operation);
	}

	/**
	 * This method is called when the Queue checking task has found a new
	 * monitoring task that needs to be performed. This method should only be
	 * called when the isComplete method returns true.
	 * 
	 * @param message
	 * @param bDeleteFormQueue
	 * @throws PlatformException
	 * @throws WORKFLOW_WAIT
	 *             =>WORKFLOW_INPROCESSING.
	 */
	public synchronized void doIt(JSONObject taskObject) throws PlatformException {
		// mark workflow status.
		setWorkflowToRunning();
		if (taskObject == null) {
			// when has this case in processing ,MCA should reset the status and
			// stop a timer .
			logger.warn("dateStringError", "taskObject is null");
			this.recycleForReady();
			return;
		}
		// set task running ENV.
		_monitoringTask = new MonitoringTask(taskObject);
		_startTime = System.currentTimeMillis();
		_scheduledResultsData.clear();
		_operationToCompleteList.clear();
		_executedAt = new Date();
		// determine real-time or scheduled
		_isBundle = _monitoringTask.isBundle();
		// We use this time to create the total time it takes to accomplish
		// tasks
		MonitoringWorkflow._totalTasks++;
		this.totalTasks++;

		// Add the operations that are specified in this task to the
		// operationsToComplete list.
		String tasks = _monitoringTask.getOperations();
		String[] operations = tasks.split(",");
		for (int index = 0; index < operations.length; index++) {
			MonitorOperation operation = _id2Operation.get(operations[index]);
			if (operation != null) {
				_operationToCompleteList.add(operation);
			} else {
				logger.warn("operationNotFound", operations[index]);
			}
		}
		// start working on operations in task.
		if (_operationToCompleteList.isEmpty() == false) {
			// Need to add a status message that we received the task.
			logStatusMessage("received", "A monitoring node(hashCode:" + this.hashCode() + ") has received the task to start the monitoring process.");

			_currentOperation = _operationToCompleteList.remove();
			{
				// using thread to start up operation.
				new Thread() {
					public void run() {
						try {
							_currentOperation.doIt(_monitoringTask);
						} catch (Exception doItException) {
							try {
								workflowCreationError("start " + _currentOperation.getOperationsID() + " operation error", doItException);
							} catch (Exception errException) {
								logger.exception(errException);
							}
						}
					}
				}.start();
			}
		} else {
			workflowCreationError("The monitoring task supplied contains " + "no operations to preform (" + tasks + ")", null);
		}
	}

	/**
	 * This method is called by the Queue Checking task to verify that the
	 * workflow has not timed out.
	 */
	private void fixTimedOut() {
		// set ready status.
		if (_monitoringTask != null && _currentOperation != null) {
			try {
				_currentOperation.stopIt(_monitoringTask);
			} catch (PlatformException e) {
				logger.exception("fixTimedOut", e);
			}
			String PlatformExceptionMessage = String.format("The workflow(" + this.hashCode() + ") for the site %s %s", _monitoringTask.getTaskDescription(), "has timed out");
			// make errorOperation for operation
			OperationError errOperation = _currentOperation._operationError;
			errOperation._message = _currentOperation.getErrorMessageAboutTimeout();
			errOperation._code = _currentOperation.getErrorTypeAboutTimeout();
			errOperation._exception = new PlatformException(PlatformExceptionMessage, null);
			// for error entry.
			errOperation._entry = "{}";
			errOperation._screenshots = new ArrayList<String>();
			//
			logErrorMessage(_currentOperation, errOperation, false);
		} else {
			logger.warn("monitorErrorMessage", "workflow(hashCode:" + this.hashCode() + ") is time out", _monitoringTask.getTaskDescription(), getAverageTaskTime());
			workflowCompleted(true);
		}

	}

	//
	// PUBLIC IService METHODS
	//
	@Override
	public void start() throws PlatformException {
		_storageAdapter = ServiceHelper.findService(StorageAdapterService.class);

		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint point = registry.getExtensionPoint(OPERATION_EXTENSION_POINT_ID);
		if (point == null) {
			throw new PlatformException("The extension POINT for " + OPERATION_EXTENSION_POINT_ID + " could not be found!!");
		}

		IExtension[] extensions = point.getExtensions();
		if (extensions == null) {
			throw new PlatformException("No extensions for " + OPERATION_EXTENSION_POINT_ID + " could not be found!!");
		}

		for (int index = 0; index < extensions.length; index++) {
			IConfigurationElement[] elements = extensions[index].getConfigurationElements();
			for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
				try {
					IConfigurationElement configurationElement = elements[elementIndex];
					// This is where we create the server object that will be

					MonitorOperation operation = (MonitorOperation) configurationElement.createExecutableExtension("class");
					GenericConfiguration operationConfiguration = new GenericConfiguration();

					String id = configurationElement.getAttribute("id");
					String reSetIDStr = configurationElement.getAttribute("reSetID");

					// Populate the configuration properties.
					operationConfiguration.populateProperties(configurationElement);
					operation.configure(operationConfiguration, _monitoringService, this);
					operation.addOperationListener(this);
					//
					if (reSetIDStr == null || reSetIDStr.length() == 0 || "true".equals(reSetIDStr.toLowerCase())) {
						operation.setOperationID(id);
					}
					//
					_id2Operation.put(operation.getOperationsID(), operation);
					//
					operation.start();
					// We will keep track of these, and will
					_operationList.add(operation);
				} catch (Exception e) {
					logger.exception("start", e);
				}
			}
		}
		// mark ready.
		_status = WORKFLOW_READY;
		// start the timer watch.
	}

	@Override
	public void stop() throws PlatformException {
		if (_operationList != null && _operationList.isEmpty() == false) {
			for (int i = 0; i < _operationList.size(); i++) {
				_operationList.get(i).destroy();
			}
			_operationList.clear();
		}
		_status = WORKFLOW_STOP;
	}

	//
	// IOperationListener METHODS
	//
	@Override
	public synchronized void operationCompletedSuccessfully(IMonitorOperation operation) {
		if (this.isComplete()) {
			return;
		}
		operationFinished(operation);
	}

	@Override
	public synchronized void operationError(IMonitorOperation operation, OperationError operationError) {
		if (this.isComplete()) {
			return;
		}
		logErrorMessage(operation, operationError);
	}

	/**
	 * When an operation requires a message to be sent it will call this method.
	 * received, ping, firebug, slow.
	 * 
	 * @param state
	 * @param messageString
	 */
	public void logStatusMessage(String state, String messageString) {
		logger.info("logStatusMessage", messageString, _monitoringTask.getTaskDescription(), getAverageTaskTime());
	}

	/**
	 * This method creates a scheduled task's raw data message and send to dpu
	 * directly. This method is called when either a error or finished method is
	 * called.
	 * 
	 * @param state
	 * @param messageString
	 */
	public void logScheduledFinishedMessage(String messageType, String messageString) {
		logger.info("logStatusMessage", messageString, _monitoringTask.getTaskDescription(), getAverageTaskTime());
		//
		String scheduledData = createScheduledData();
		//
		HashMap<String, String> messageInputAndRequestHeaders = getMessageInputAndRequestHeaders(messageType, scheduledData);
		storeResultsMessage(messageType, messageInputAndRequestHeaders);
	}

	/**
	 * send the data to dpu if it's a realtime task, otherwise put it into
	 * scheduledResult
	 * 
	 * @param type
	 * @param messageInputAndRequestHeaders
	 * 
	 * @author wlan
	 */
	public void createResultsMessage(String type, HashMap<String, String> messageInputAndRequestHeaders) {
		if (_isBundle == false) {
			storeResultsMessage(type, messageInputAndRequestHeaders);
		} else {
			_scheduledResultsData.put(type, messageInputAndRequestHeaders.get("body"));
		}
	}

	/**
	 * This method generates the path to the data with in the s3 bucket.
	 * 
	 * <md5-of-url>/2009/12/28/12/34/05/us-east/received.json
	 * 
	 * http://app1.app2.yottaa.com/mydir1/mydir2/file.html the domain is the the
	 * "yottaa.com" part.
	 * 
	 * @return
	 */
	protected String getResultDataPath(String messageType) {
		return getDataFolder() + "/" + messageType + ".json";
	}

	/**
	 * get the message body and header to send to dpu
	 * 
	 * @param messageFile
	 * @param objects
	 * @return return a hashmap that contains the message body and headers
	 */
	public HashMap<String, String> getMessageInputAndRequestHeaders(String messageFile, Object... objects) {
		HashMap<String, String> results = new HashMap<String, String>();

		MessageFormat messageFormatter = null;
		if (_isBundle == false) {
			messageFormatter = _messageFormatters.get(messageFile);
		} else {
			messageFormatter = _messageFormatters.get(messageFile + "-scheduled");
		}

		// TODO Monitoring System V1 HEADER
		results.put(MonitoringTask.TASK_TIMESTAMP_PROPERTY_NAME, String.valueOf(_executedAt.getTime()));
		results.put(MonitoringTask.TASK_MONITOR_ID_PROPERTY_NAME, _monitoringTask.getMonitorID());
		// Add the message specific information.
		results.put("body", messageFormatter.format(objects));

		return results;
	}

	//
	// PRIVATE METHODS
	//
	/**
	 * This method is called from the doIt method and handles any exception the
	 * cause the workflow to stop
	 * 
	 */
	private void workflowCreationError(String message, Exception exception) throws PlatformException {
		PlatformException PlatformException = new PlatformException(message, exception);

		OperationError operationError = new OperationError();
		operationError._code = OperationError.McaError.GENERAL_ERROR;
		operationError._message = OperationError.McaError.GENERAL_ERROR_MESSAGE;
		operationError._exception = PlatformException;

		logErrorMessage(_currentOperation, operationError);

		throw PlatformException;
	}

	private void logErrorMessage(IMonitorOperation operation, OperationError operationError) {
		logErrorMessage(operation, operationError, true);
	}

	/**
	 * This method logs an error that occurred in the monitoring workflow.
	 * 
	 * @param operationError
	 */
	private void logErrorMessage(IMonitorOperation operation, OperationError operationError, boolean isCheckOperation) {
		// check operation ,if get operation is current operation or firefox
		// operations then do it.
		if (isCheckOperation && isCurrentOperation(operation) == false) {
			return;
		}
		try {
			logger.info("monitorErrorMessage", operationError._message, _monitoringTask.getTaskDescription(), getAverageTaskTime());
			// Increment the number of errors
			MonitoringWorkflow._totalErrors++;
			this.totalErrors++;
			_monitoringService._lastError = "Task error :" + operationError._type + " - " + operationError._message;

			// Need to create the error message.
			String rawErrorData = MessagePreparer.format(getClass(), "rawErrorString", operationError._type, operationError._code, operationError._message, operationError._entry,
					new JSONArray(operationError._screenshots).toString());

			if (_isBundle == false) {
				HashMap<String, String> messageInputAndRequestHeaders = getMessageInputAndRequestHeaders("error", rawErrorData);
				storeResultsMessage("error", messageInputAndRequestHeaders);
			} else {
				_scheduledResultsData.put("error", rawErrorData);
				// need to update error data to DPU when on bundle is true
				logScheduledFinishedMessage("finished", "Tasks was completed in " + getTotalTime() + " millis with error, workflow(hasCode:" + this.hashCode() + ").");
			}

		} finally {
			// We need to stop processing and return the workflow to the queuing
			// system.
			completeWorkflowOnError();
		}

	}

	private static void createMessageFormatters() {
		for (int index = 0; index < _messageFiles.length; index++) {
			String fileName = _messageFiles[index];
			StringBuffer stringBuffer = Utils.getInputStream2StringBuffer(MonitoringWorkflow.class.getResourceAsStream("messages/" + fileName));

			MessageFormat messageFormat = new MessageFormat(stringBuffer.toString());

			// Remove the file extension.
			int intPos = fileName.indexOf(".");
			if (intPos != -1) {
				fileName = fileName.substring(0, intPos);
			}
			_messageFormatters.put(fileName, messageFormat);
		}
	}

	/**
	 * This method will remove the finished operation and
	 */
	private void operationFinished(IMonitorOperation operation) {
		// check operation ,if get operation is current operation or firefox
		// operations then do it.
		if (isCurrentOperation(operation) == false) {
			return;
		}
		if (_operationToCompleteList.isEmpty() == true) {
			try {
				_totalTimeOfProcessing += getTotalTime();
				// Log a finished status message.
				if (_isBundle == false) {
					logStatusMessage("finished", "Tasks was completed in " + getTotalTime() + " millis ,on workflow(hashCode:" + this.hashCode() + ")");
				} else {
					logScheduledFinishedMessage("finished", "Tasks was completed in " + getTotalTime() + " millis ,on workflow(hashCode:" + this.hashCode() + ")");
				}
			} finally {
				// This variable here { true | false } causes concurrency issues
				// when set to true.
				// If this test comes from a firefox operation skip and we want
				// to
				// return as quickly as possble.
				workflowCompleted(true);
			}

		} else {
			// If we are not finished pull the next operation from the
			// workflow and then execute it.
			// try {
			_currentOperation = _operationToCompleteList.remove();
			if (_currentOperation != null) {
				new Thread() {
					public void run() {
						try {
							_currentOperation.doIt(_monitoringTask);
						} catch (Exception doItException) {
							try {
								workflowCreationError("start " + _currentOperation.getOperationsID() + " operation error", doItException);
							} catch (Exception errException) {
								// TODO Auto-generated catch block
								logger.exception(errException);
							}
						}
					}
				}.start();

			}
		}
	}

	/**
	 * This method is called when the workflow has completed.
	 * 
	 * @param resultQueue
	 * @param rawData
	 * @param messageType
	 */
	protected void workflowCompleted(boolean bError) {
		try {
			_monitoringTask = null;
			this._status = WORKFLOW_FINISH;
			this.recycleForReady();
			logger.debug("workflowCompleted", MonitoringWorkflow._totalTasks, _monitoringService.getProcessingAverage());
		} catch (Exception e) {
			logger.exception("workflowCompleted", e);
		} finally {
			// Setting this to null, will allow the work to be reused.

		}
	}

	/**
	 * This method will create the scheduled data information.
	 * 
	 * @return
	 */
	private String createScheduledData() {
		// String messageTypes = "";
		String rawDataTypes = "";

		// Create the messageTypes string.
		Iterator<String> iterator = _scheduledResultsData.keySet().iterator();
		while (iterator.hasNext() == true) {
			String type = iterator.next();
			// messageTypes += "\"" + type + "\"";
			rawDataTypes += _scheduledResultsData.get(type);
			if (iterator.hasNext() == true) {
				// messageTypes += ",";
				rawDataTypes += ",";
			}
		}
		// new String[] {messageTypes,rawDataTypes };
		return rawDataTypes;
	}

	/**
	 * This method upload the .
	 * 
	 * @param type
	 * @param messageInputAndRequestHeaders
	 */
	protected void storeResultsMessage(String type, HashMap<String, String> messageInputAndRequestHeaders) {
		final JSONObject messageObj = new JSONObject(messageInputAndRequestHeaders);
		new Thread() {
			public void run() {
				try {
					_storageAdapter.uploadRawData(messageObj);
				} catch (PlatformException e) {
					logger.exception("storeResultsMessage", e);
				}
			}
		}.start();
	}

	/**
	 * This is called when there is an error in the workflow. At this point we
	 * should clear the messages and finish
	 */
	private void completeWorkflowOnError() {
		try {
			while (_operationToCompleteList.isEmpty() == false) {
				try {
					MonitorOperation operation = _operationToCompleteList.remove();
					if (operation != null) {
						operation.stopIt(_monitoringTask);
					}
				} catch (PlatformException e) {
					logger.exception("completeWorkflowOnError", e);
				}
			}
		} finally {
			workflowCompleted(true);
		}
	}

	/**
	 * @param operation
	 * @return
	 */
	private boolean isCurrentOperation(IMonitorOperation operation) {
		if (operation != null && _currentOperation != null && operation.equals(_currentOperation)) {
			return true;
		}
		return false;
	}

	public MonitoringTask getCurrentMonitoringTask() {
		return this._monitoringTask;
	}
}
