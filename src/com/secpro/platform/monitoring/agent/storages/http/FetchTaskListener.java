package com.secpro.platform.monitoring.agent.storages.http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.secpro.platform.api.client.IClientResponseListener;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.TaskProcessingAction;
import com.secpro.platform.monitoring.agent.services.MonitoringEncryptService;
import com.secpro.platform.monitoring.agent.services.MonitoringTaskCacheService;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;

/**
 * @author baiyanwei Jul 13, 2013
 * 
 *         Storage listener instance. We fetch the task from TSS,if network
 *         doesn't work,then try for 4 times, if network is disconnection after
 *         4 times, get the task from local cache service.
 * 
 */
public class FetchTaskListener implements IClientResponseListener {
	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(FetchTaskListener.class);

	public TaskProcessingAction _taskProcessingAction = null;
	// 0 false 1 true
	// private byte _isHasResponse = 0;
	
	private String _listenerID = "FetchTaskListener";

	private String _listenerName = "FetchTaskListener";

	private String _listenerDescription = null;
	private String _privateKey="";

	public FetchTaskListener(TaskProcessingAction action,String privateKey) {
		this._taskProcessingAction = action;
		this._privateKey=privateKey;
	}

	private void recycleWorkflows() {
		if (_taskProcessingAction != null) {
			_taskProcessingAction.recycle();
		}
	}

	private void processTasks(String contents) {
		if (_taskProcessingAction != null) {
			_taskProcessingAction.processTasks(contents);
		}
	}

	@Override
	public void setID(String id) {
		this._listenerID = id;
	}

	@Override
	public String getID() {
		return this._listenerID + this.hashCode();
	}

	@Override
	public void setName(String name) {
		this._listenerName = name;
	}

	@Override
	public String getName() {
		return this._listenerName;
	}

	@Override
	public void setDescription(String description) {
		this._listenerDescription = description;
	}

	@Override
	public String getDescription() {
		return this._listenerDescription;
	}

	@Override
	public void fireSucceed(Object messageObj) throws PlatformException {
		// if (_isHasResponse == 0) {
		// _isHasResponse = 1;
		// StorageAdapterService.updateResponseCount();
		// }
		// clear the error counter about disconnection.
		try {
			if (messageObj != null) {
				final String contents = messageObj.toString();
				// final String contents = (String) (storageResponse.getData());
				if (contents != null && contents.trim().length() > 0) {
					new Thread("DPUStorageListener.taskProcessingAction.processTasks") {
						public void run() {
							//String decodeContents=decodeContents(contents);
							//test 
							String decodeContents=contents;
							// execute job
							processTasks(decodeContents);
							// put job into local task cache.
							putJobIntoLocalCache(decodeContents);
						}
					}.start();
					return;
				}
			}
			// function go here ,that mean processTask can not run in case, so
			// we need to recycle!
			new Thread("DPUStorageListener.taskProcessingAction.recycle") {
				public void run() {
					recycleWorkflows();
				}
			}.start();
		} catch (Exception e) {
			theLogger.exception(e);
		}

	}

	@Override
	public void fireError(Object messageObj) throws PlatformException {
		// if (_isHasResponse == 0) {
		// _isHasResponse = 1;
		// StorageAdapterService.updateResponseCount();
		// }
		if (messageObj != null) {
			theLogger.exception(new Exception(messageObj.toString()));
		}
//		new Thread("DPUStorageListener.taskProcessingAction.recycle") {
//			public void run() {
//				recycleWorkflows();
//			}
//		}.start();

	}

	/**
	 * put job into local cache.
	 * 
	 * @param jobString
	 */
	private void putJobIntoLocalCache(String jobsString) {
		if (Assert.isEmptyString(jobsString) == true) {
			return;
		}
		MonitoringTaskCacheService taskCache = ServiceHelper.findService(MonitoringTaskCacheService.class);
		try {
			// JSONTokener parser = new JSONTokener(content);

			JSONArray taskJsons = new JSONArray(jobsString);
			if (taskJsons == null || taskJsons.length() == 0) {
				return;
			}
			for (int i = 0; i < taskJsons.length(); i++) {
				taskCache.addTaskIntoCache(taskJsons.getJSONObject(i));
			}
			theLogger.debug("putJobIntoLocalCache", jobsString);
		} catch (JSONException e) {
			theLogger.exception(e);
		}
	}
	/**
	 * 当接收到任务队列后，对任务队列中加密块（secret）进行解密
	 * @param contents
	 * @return
	 */
	private String decodeContents(String contents)
	{
		
		theLogger.debug("decodeContents", contents);

		if (contents == null || contents.trim().length() <= 0) {
			return contents;
		}
		JSONArray decodeTaskJsons=null;
		JSONTokener parser = new JSONTokener(contents);
		try {
			JSONArray taskJsons = new JSONArray(parser);
			decodeTaskJsons=new JSONArray();
			for (int i = 0; i < taskJsons.length(); i++) {
				JSONObject taskObject = taskJsons.getJSONObject(i);
				if(taskObject!=null&&taskObject.has(MonitoringTask.TASK_META_DATA_NAME))
				{
					JSONObject metaJsonObj=taskObject.getJSONObject(MonitoringTask.TASK_META_DATA_NAME);
					if(metaJsonObj!=null&&metaJsonObj.has("secret")&&Assert.isEmptyString(_privateKey)==false)
					{
						String secret=metaJsonObj.getString("secret");
						metaJsonObj.remove("secret");
						MonitoringEncryptService encryptService=ServiceHelper.findService(MonitoringEncryptService.class);
						if(Assert.isEmptyString(secret)==false&&encryptService!=null)
						{
							byte[] decodeData=encryptService.decryptByPrivateKey(encryptService.decryptBASE64(secret), _privateKey);
							if(decodeData!=null&&decodeData.length>0){
							JSONTokener secretParser=new JSONTokener(new String(decodeData));
							JSONObject secretJsonObj=new JSONObject(secretParser);
							String[] names=JSONObject.getNames(secretJsonObj);
							if(names!=null&&names.length>0)
							{
								for(int j=0;j<names.length;j++)
								{
									metaJsonObj.put(names[j], secretJsonObj.getString(names[j]));
									
								}
							}
							}
						}
					}
				}
				decodeTaskJsons.put(taskObject);
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return decodeTaskJsons.toString();
	}
}