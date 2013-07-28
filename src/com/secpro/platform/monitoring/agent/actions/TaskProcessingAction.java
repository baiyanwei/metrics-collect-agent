package com.secpro.platform.monitoring.agent.actions;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;
import com.secpro.platform.monitoring.agent.workflow.MonitoringWorkflow;


/**
 * This class will process the messages returned be the DPU
 * 
 * @author Martin
 * 
 */
public class TaskProcessingAction {
	private static PlatformLogger theLogger = PlatformLogger.getLogger(TaskProcessingAction.class);
	private List<MonitoringWorkflow> _workflows = null;

	public TaskProcessingAction(List<MonitoringWorkflow> workflows) {
		_workflows = workflows;
	}

	/**
	 * @param content
	 * @return ArrayList<Message> msgList
	 */
	public void processTasks(String content) {

		try {
			theLogger.debug("processTask",content);

			if (_workflows == null || content == null || content.trim().length() <= 0) {
				return;
			}
			JSONTokener parser = new JSONTokener(content);
			
			JSONArray taskJsons = new JSONArray(parser);

			// No tasks were return from the
			if (taskJsons == null || taskJsons.length() == 0){
				theLogger.debug("invalidTaskcontent");
				return;
			}

			synchronized (_workflows) {
				if (_workflows.size() < taskJsons.length()) {
					theLogger.warn("tooManyTasks", _workflows.size(), taskJsons.length());
				}

				for (int i = 0; i < taskJsons.length(); i++) {
					if (_workflows.isEmpty()) {
						break;
					}
					JSONObject taskObject = taskJsons.getJSONObject(i);
					if (taskObject == null || checkForTaskFormat(taskObject) == false) {
						continue;
					}
					try {
						processFoundQueueMessage(taskObject, _workflows.remove(0));
					} catch (Exception e) {
						theLogger.exception(e);
					}
				}
			}

		} catch (Exception e1) {
			theLogger.exception("getMessageForReturn", e1);
		} finally {
			// always recycle workflows.
			recycle();
		}
	}

	/**
	 * Set all workflows that were not process back to being waiting to be
	 * processed.
	 * 
	 */
	public void recycle() {
		if (_workflows == null) {
			return;
		}
		// Need to set the waiting flag back.
		synchronized (_workflows) {
			while (_workflows.isEmpty() == false) {
				try {
					MonitoringWorkflow workflow = _workflows.remove(0);
					if (workflow == null) {
						continue;
					}
					workflow.recycleForReady();
				} catch (Exception e) {
					continue;
				}
			}
		}
	}

	//
	// PRIVATE
	//
	/**
	 * This method will attempt to call kick of the
	 * 
	 * @param msgList
	 *            do message list
	 * @throws JSONException
	 */
	private void processFoundQueueMessage(JSONObject taskObject, MonitoringWorkflow workflow) throws Exception {
		if (workflow == null) {
			return;
		}
		// check running conditions.
		synchronized (workflow) {
			try {
				workflow.doIt(taskObject);
			} catch (Exception e) {
				theLogger.exception("processFoundQueueMessages", e);
			}
		}

	}

	/**
	 * we can't run task like no url or operation.
	 * 
	 * @param taskObject
	 * @return
	 */
	private boolean checkForTaskFormat(JSONObject taskObject) {
		if (taskObject == null) {
			return false;
		}
		try {
			if (taskObject.getString(MonitoringTask.TASK_MONITOR_ID_PROPERTY_NAME) == null || taskObject.getString(MonitoringTask.TASK_MONITOR_ID_PROPERTY_NAME).trim().equals("")) {
				theLogger.warn("errorTaskFormat", MonitoringTask.TASK_MONITOR_ID_PROPERTY_NAME, taskObject.getString(MonitoringTask.TASK_MONITOR_ID_PROPERTY_NAME));
				return false;
			}
			if (taskObject.getString(MonitoringTask.TASK_TARGET_IP_PROPERTY_NAME) == null || taskObject.getString(MonitoringTask.TASK_TARGET_IP_PROPERTY_NAME).trim().equals("")) {
				theLogger.warn("errorTaskFormat", MonitoringTask.TASK_TARGET_IP_PROPERTY_NAME, taskObject.getString(MonitoringTask.TASK_TARGET_IP_PROPERTY_NAME));
				return false;
			}
			if (taskObject.getString(MonitoringTask.TASK_OPERATION_PROPERTY_NAME) == null || taskObject.getString(MonitoringTask.TASK_OPERATION_PROPERTY_NAME).trim().equals("")) {
				theLogger.warn("errorTaskFormat", MonitoringTask.TASK_OPERATION_PROPERTY_NAME, taskObject.getString(MonitoringTask.TASK_OPERATION_PROPERTY_NAME));
				return false;
			}
		} catch (JSONException e) {
			theLogger.exception("checkForTaskFormat", e);
			return false;
		}
		return true;
	}
}