package com.secpro.platform.monitoring.agent.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;

import javax.xml.bind.annotation.XmlElement;

import org.json.JSONException;
import org.json.JSONObject;

import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.core.utils.Constants;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.CacheTaskManageAction;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;

/**
 * 
 * @author liyan
 * 
 *         用以将任务进行缓存和本地文件存储，以及采集端启动时从本地文件读取近两天执行过的任务
 */
@ServiceInfo(description = "metric upload service, upload the metric to data process server.", configurationPath = "mca/services/MonitoringTaskCacheService/")
public class MonitoringTaskCacheService implements IService {
	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(MonitoringTaskCacheService.class);
	//
	// The cache for task in local
	private ArrayList<JSONObject> _taskCacheQueue = new ArrayList<JSONObject>();
	/**
	 * 是否删除队列中的任务，true允许删除，false不允许删除
	 */
	public boolean _isflag = true;
	/**
	 * local task storage in directory
	 */
	@XmlElement(name = "taskPath", defaultValue = "data/mca/task/")
	public String _taskPath = "data/mca/task/";
	/**
	 * the prefix name of task file
	 */
	@XmlElement(name = "prefixTaskName", defaultValue = "task")
	public String _prefixTaskName;
	//TODO remove later
	@XmlElement(name = "taskExecuteHour", type = Long.class, defaultValue = "21")
	public String _taskExecuteHour;
	@XmlElement(name = "taskExecMin", type = Long.class, defaultValue = "45")
	public String _taskExecMin;
	@XmlElement(name = "taskExecSec", type = Long.class, defaultValue = "0")
	public String _taskExecSec;
	//END remove later
	@XmlElement(name = "taskNameTimeFormat1", defaultValue = "yyyyMMdd")
	public String _taskNameTimeFormat1 = "yyyyMMdd";

	@XmlElement(name = "taskNameTimeFormat2", defaultValue = "yyyyMMddHHmmss")
	public String _taskNameTimeFormat2 = "yyyyMMddHHmmss";

	@XmlElement(name = "taskTimerExecuteInterval", type = Long.class, defaultValue = "120000")
	public long _taskTimerExecuteInterval = 120000;
	//
	private SimpleDateFormat _taskNameDataFormat1 = null;
	private SimpleDateFormat _taskNameDataFormat2 = null;
	private Timer _fetchTSSTaskTimer = null;
	private FilenameFilter _taskJSONFileFilter = null;

	@Override
	public void start() throws Exception {
		_taskNameDataFormat1 = new SimpleDateFormat(_taskNameTimeFormat1);
		_taskNameDataFormat2 = new SimpleDateFormat(_taskNameTimeFormat2);
		// load the Fetch task timer
		_taskJSONFileFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".json");
			}
		};
		loadLocalTaskByFile();
		//
		if (_isflag) {
			_fetchTSSTaskTimer = new Timer("MonitoringService._fetchTSSTaskTimer");
			_fetchTSSTaskTimer.schedule(new CacheTaskManageAction(this), 20000L, _taskTimerExecuteInterval);
		}
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * 采集端启动时读取本地文件中的任务，并放到缓存中
	 */
	private void loadLocalTaskByFile() {
		MonitoringEncryptService monitoringEncryptService = ServiceHelper.findService(MonitoringEncryptService.class);
		if (monitoringEncryptService == null) {
			theLogger.error("No found MonitoringEncryptService ERROR");
			return;
		}
		Date todayDate = new Date();
		// today's task directory name
		String todayDirName = _taskNameDataFormat1.format(todayDate);
		// yesterday's task directory name.
		String yesterdayDirName = _taskNameDataFormat1.format(new Date(todayDate.getTime() - (60 * 60 * 24 * 1000)));
		//
		File yesterdayTaskDir = new File(_taskPath + yesterdayDirName);
		File todayTaskDir = new File(_taskPath + todayDirName);
		//
		List<String> localTaskContentList = new ArrayList<String>();
		ArrayList<String> yesterdayTaskContentList = loadFileInString(yesterdayTaskDir);
		ArrayList<String> todayTaskContentList = loadFileInString(todayTaskDir);
		if (yesterdayTaskContentList != null && yesterdayTaskContentList.isEmpty() == false) {
			localTaskContentList.addAll(yesterdayTaskContentList);
		}
		if (todayTaskContentList != null && todayTaskContentList.isEmpty() == false) {
			localTaskContentList.addAll(todayTaskContentList);
		}
		// analyzed local file task and add into the cache queue.
		for (int i = 0; i < localTaskContentList.size(); i++) {
			synchronized (_taskCacheQueue) {
				try {
					_taskCacheQueue.add(new JSONObject(localTaskContentList.get(i)));
				} catch (JSONException e) {
					theLogger.exception(e);
				}
			}
		}
	}

	/**
	 * 取得对应目录中所有JSON文件内容
	 * 
	 * @return
	 */
	private ArrayList<String> loadFileInString(File dir) {
		if (dir == null || dir.exists() == false || dir.isDirectory() == false) {
			return null;
		}
		String[] sonFileNames = dir.list(_taskJSONFileFilter);
		ArrayList<String> fileNameList = new ArrayList<String>();
		// Create a file reader
		FileReader fileReader = null;
		StringBuffer contentBuff = new StringBuffer();
		for (int i = 0; i < sonFileNames.length; i++) {
			contentBuff.setLength(0);
			try {
				fileReader = new FileReader(sonFileNames[i]);
				int fileIndex;
				// Read characters
				while ((fileIndex = fileReader.read()) != -1) {
					contentBuff.append((char) fileIndex);
				}
			} catch (FileNotFoundException e) {
				theLogger.exception(e);
			} catch (IOException e) {
				theLogger.exception(e);
			} finally {
				if (fileReader != null) {
					// Close file reader
					try {
						fileReader.close();
						fileReader = null;
					} catch (IOException e) {
					}
				}
			}
			if (contentBuff.length() == 0) {
				continue;
			}
			fileNameList.add(contentBuff.toString());

		}
		return fileNameList;
	}

	/**
	 * 将任务并存储到本地文件
	 * 
	 * @param t
	 *            JSON格式的任务
	 */
	private void storeTaskInFile(JSONObject taskObj) {
		if (taskObj == null) {
			return;
		}
		MonitoringEncryptService monitoringEncryptService = ServiceHelper.findService(MonitoringEncryptService.class);
		if (monitoringEncryptService == null) {
			theLogger.error("No found MonitoringEncryptService ERROR");
			return;
		}
		PrintWriter filePrinter = null;
		Date currentDay = new Date();
		String taskDirName = _taskNameDataFormat1.format(currentDay);
		String fileNameTimePrefix = _taskNameDataFormat2.format(currentDay);
		try {
			File taskFile = new File(_taskPath + taskDirName + File.separator + fileNameTimePrefix + "_" + fileNameTimePrefix
					+ taskObj.getString(MonitoringTask.TASK_MONITOR_ID_PROPERTY_NAME) + ".json");
			if (taskFile.getParentFile().exists() == false) {
				taskFile.getParentFile().mkdirs();
			}
			if (taskFile.exists() == false) {
				taskFile.createNewFile();
			}
			filePrinter = new PrintWriter(taskFile, Constants.DEFAULT_ENCODING);
			filePrinter.print(monitoringEncryptService.encode(taskObj.toString()));
			filePrinter.flush();
		} catch (Exception e) {
			theLogger.exception("Create task file error", e);
		} finally {
			if (filePrinter != null) {
				filePrinter.close();
				filePrinter = null;
			}
		}
	}

	/**
	 * 核心出现问题时调用此方法获取前一天距离此当前时间最近，切5分钟内的任务
	 * 
	 * @return 任务对象
	 */
	public JSONObject getCacheTaskInReferent() {
		Date d = new Date();
		// 计算昨天此时的毫秒数
		long yestoday = d.getTime() - 60 * 60 * 24 * 1000;
		JSONObject temp = null;
		// 间隔5分钟
		long timeTemp = 5 * 60 * 1000;
		// 遍历全部任务
		Iterator<JSONObject> it = _taskCacheQueue.iterator();
		while (it.hasNext()) {
			JSONObject te = it.next();
			String execT = "";
			try {

				execT = te.getString("execTime");

				Date execD = _taskNameDataFormat2.parse(execT);
				long yExecT = execD.getTime();
				long spacing = 5 * 60 * 1000;// 将5分钟转换为毫秒
				d.setTime(yestoday);
				// 昨天的此时间点减去任务执行时间，差值大于0，小于5分钟，且差值最小，则为需要执行的时间。
				if ((yestoday - yExecT) > 0 && (yestoday - yExecT) <= spacing && (yestoday - yExecT) < timeTemp) {
					timeTemp = yestoday - yExecT;
					temp = te;
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				System.out.println("获取前一天此时刻任务，执行日期解析错误：" + execT);
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				System.out.println("解析JSONOBJECT ，获取时间失败");
				e.printStackTrace();

			}
		}
		return temp;
	}

	/**
	 * @return
	 */
	public JSONObject getOneTaskFromCache() {
		synchronized (_taskCacheQueue) {
			if (_taskCacheQueue.isEmpty() == false) {
				return _taskCacheQueue.get(0);
			} else {
				return null;
			}

		}
	}

	/**
	 * get the size of task quque in cache.
	 * 
	 * @return
	 */
	public int getTaskCacheQueueSize() {
		synchronized (_taskCacheQueue) {
			return _taskCacheQueue.size();
		}
	}

	/**
	 * get hole task queue from local queue
	 * 
	 * @return
	 */
	public ArrayList<JSONObject> getTaskCacheQueue() {
		return _taskCacheQueue;
	}

	/**
	 * add one task object into local cache queue.
	 * 
	 * @param taskObj
	 */
	public void addTaskIntoCache(JSONObject taskObj) {
		if (taskObj == null) {
			return;
		}
		synchronized (_taskCacheQueue) {
			_taskCacheQueue.add(taskObj);
		}
		storeTaskInFile(taskObj);
	}

}
