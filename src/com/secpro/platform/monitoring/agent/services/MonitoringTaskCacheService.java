package com.secpro.platform.monitoring.agent.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;

import javax.management.DynamicMBean;
import javax.xml.bind.annotation.XmlElement;

import org.json.JSONException;
import org.json.JSONObject;

import com.secpro.platform.core.metrics.AbstractMetricMBean;
import com.secpro.platform.core.metrics.Metric;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.core.utils.Constants;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.CacheTaskManageAction;
import com.secpro.platform.monitoring.agent.utils.file.FileSystemStorageUtil;

/**
 * 
 * @author liyan
 * 
 *         用以将任务进行缓存和本地文件存储，以及采集端启动时从本地文件读取近两天执行过的任务, 上传错误的SAMPLE.
 */
@ServiceInfo(description = "task and sample local cache.", configurationPath = "mca/services/MonitoringTaskCacheService/")
public class MonitoringTaskCacheService extends AbstractMetricMBean implements IService, DynamicMBean {
	//
	final private static PlatformLogger theLogger = PlatformLogger.getLogger(MonitoringTaskCacheService.class);
	//
	final public static long DAY_MSECONDS = 86400000L;
	@XmlElement(name = "jmxObjectName", defaultValue = "secpro.mca:type=MonitoringTaskCacheService")
	public String _jmxObjectName = "secpro.mca:type=MonitoringTaskCacheService";
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
	@XmlElement(name = "taskCacheFile", defaultValue = "data/mca/task/taskCache.list")
	public String _taskCacheFile = "data/mca/task/taskCache.list";

	@XmlElement(name = "sampleFileNameTimeFormat", defaultValue = "yyyyMMddHHmmssSSS")
	public String _sampleFileNameTimeFormat = "yyyyMMddHHmmssSSS";

	@XmlElement(name = "taskTimerExecuteInterval", type = Long.class, defaultValue = "86400000")
	public long _taskTimerExecuteInterval = 86400000;
	//
	private SimpleDateFormat _sampleNameDataFormat = null;
	private Timer _taskManageTimer = null;
	private FilenameFilter _sampleFileFilter = null;
	// sample
	@XmlElement(name = "waitUploadSampleLocalPath", defaultValue = "/data/mca/sample/waitupload/")
	public String _waitUploadSampleLocalPath = "";
	/**
	 * upload file name list.
	 */
	private ArrayList<String> _uploadSampleFileQueue = new ArrayList<String>();

	/**
	 * upload sample worker.
	 */
	private Thread[] _uploadSampleThreads = null;
	//
	private StorageAdapterService _storageAdapter = null;

	private Thread _storeCacheTaskThread = null;

	@Override
	public void start() throws Exception {
		// register itself as dynamic bean
		this.registerMBean(_jmxObjectName, this);
		//
		if (_isflag) {
			Date today = new Date();
			@SuppressWarnings("deprecation")
			Date tomorrow = new Date(today.getYear(), today.getMonth(), today.getDate() + 1, 0, 0, 0);
			_taskManageTimer = new Timer("MonitoringTaskCacheService._taskManageTimer");
			// start on tomorrow 0:00
			_taskManageTimer.schedule(new CacheTaskManageAction(this), tomorrow.getTime() - today.getTime(), _taskTimerExecuteInterval);
		}
		theLogger.info("startUp");
		//
		_sampleFileFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".sample");
			}
		};
		// upload sample staff
		_sampleNameDataFormat = new SimpleDateFormat(_sampleFileNameTimeFormat);
		initLocalUploadFile();
		_uploadSampleThreads = new Thread[1];
		initUploadSampleThread(_uploadSampleThreads);
		Thread loadTaskCacheFileThread = new Thread("MonitoringTaskCacheService.loadTaskCacheFileThread") {
			public void run() {
				try {
					loadLocalTaskByFile();
				} catch (IOException e) {
					theLogger.exception(e);
				}
			}
		};
		loadTaskCacheFileThread.start();
		_storeCacheTaskThread = new Thread("MonitoringTaskCacheService.storeCacheTaskThread") {
			public void run() {
				while (true) {
					try {
						// hourly
						sleep(3600000L);
						// synchronized cache task into file.
						storeCacheTaskInFile();
					} catch (Exception e) {
						theLogger.exception(e);
					}
				}
			}
		};
		_storeCacheTaskThread.start();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void stop() throws Exception {
		this.unRegisterMBean(_jmxObjectName);
		if (this._uploadSampleThreads != null) {
			for (int i = 0; i < _uploadSampleThreads.length; i++) {
				if (_uploadSampleThreads[i] != null) {
					try {
						_uploadSampleThreads[i].stop();
					} catch (Exception e) {
						theLogger.exception(e);
					}

				}
			}
		}
		if (_storeCacheTaskThread != null) {
			try {
				_storeCacheTaskThread.stop();
			} catch (Exception e) {
				theLogger.exception(e);
			}
		}
		_taskManageTimer.cancel();
	}

	/**
	 * 采集端启动时读取本地文件中的任务，并放到缓存中
	 * 
	 * @throws IOException
	 */
	private void loadLocalTaskByFile() throws IOException {
		MonitoringEncryptService monitoringEncryptService = ServiceHelper.findService(MonitoringEncryptService.class);
		if (monitoringEncryptService == null) {
			theLogger.error("No found MonitoringEncryptService ERROR");
			return;
		}
		File taskCacheFile = new File(_taskCacheFile);
		if (taskCacheFile.exists() == false) {
			File parentFile = taskCacheFile.getParentFile();
			if (parentFile.exists() == false) {
				parentFile.mkdirs();
			}
			taskCacheFile.createNewFile();
			return;
		}
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(taskCacheFile), Constants.DEFAULT_ENCODING));
			String lineStr = null;
			while ((lineStr = bufferedReader.readLine()) != null) {
				if (lineStr.length() == 0) {
					continue;
				}
				// analyzed local file task and add into the cache queue.
				synchronized (_taskCacheQueue) {
					try {
						_taskCacheQueue.add(new JSONObject(monitoringEncryptService.decode(lineStr)));
					} catch (JSONException e) {
						theLogger.exception(e);
					}
				}
			}
		} catch (FileNotFoundException e) {
			theLogger.exception(e);
		} catch (IOException e) {
			theLogger.exception(e);
		} finally {
			if (bufferedReader != null) {
				// Close file reader
				try {
					bufferedReader.close();
					bufferedReader = null;
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * 将任务并存储到本地文件
	 * 
	 * @param t
	 *            JSON格式的任务
	 * @throws IOException
	 */
	@Metric(description = "Just for test")
	public void storeCacheTaskInFile() throws IOException {
		if (this._taskCacheQueue.isEmpty()) {
			return;
		}
		MonitoringEncryptService monitoringEncryptService = ServiceHelper.findService(MonitoringEncryptService.class);
		if (monitoringEncryptService == null) {
			theLogger.error("No found MonitoringEncryptService ERROR");
			return;
		}
		File taskCacheFile = new File(_taskCacheFile);
		if (taskCacheFile.exists() == false) {
			File parentFile = taskCacheFile.getParentFile();
			if (parentFile.exists() == false) {
				parentFile.mkdirs();
			}
		} else {
			// clear the file content.
			taskCacheFile.delete();
		}
		//
		taskCacheFile.createNewFile();
		//
		PrintWriter filePrinter = null;
		try {
			filePrinter = new PrintWriter(taskCacheFile, Constants.DEFAULT_ENCODING);
			for (int i = 0; i < _taskCacheQueue.size(); i++) {
				String taskContent = _taskCacheQueue.get(i).toString();
				if (Assert.isEmptyString(taskContent) == true) {
					continue;
				}
				filePrinter.println(monitoringEncryptService.encode(taskContent));
			}
			filePrinter.flush();
		} catch (Exception e) {
			theLogger.exception(e);
		} finally {
			if (filePrinter != null) {
				filePrinter.close();
				filePrinter = null;
			}
		}
	}

	/**
	 * 核心出现问题时调用此方法获取前一天距离此当前时间最近，切5分钟内的任务 目前只考虑返回一个最近的任务,后期如果有需要再返回多个任务.
	 * 
	 * @return 任务对象
	 */
	public JSONObject getCacheTaskInReferent() {
		// Date d = new Date();
		// 计算昨天此时的毫秒数
		long yesterday = System.currentTimeMillis() - DAY_MSECONDS;
		JSONObject temp = null;
		// 间隔5分钟
		long timeTemp = 300000;
		// 遍历全部任务
		Iterator<JSONObject> it = _taskCacheQueue.iterator();
		while (it.hasNext()) {
			JSONObject te = it.next();
			try {
				String execT = te.getString("create_at");
				if (Assert.isEmptyString(execT) == true) {
					continue;
				}
				//
				// Date execD = _taskNameDataFormat2.parse(execT);
				// long yExecT = execD.getTime();
				//
				long yExecT = Long.parseLong(execT);
				if (yExecT > yesterday) {
					continue;
				}
				long timeInterval = yesterday - yExecT;
				// [n-timeTemp,n]
				if (timeInterval > timeTemp) {
					continue;
				}
				// long spacing = 300000;// 将5分钟转换为毫秒
				// d.setTime(yesterday);
				// 昨天的此时间点减去任务执行时间，差值大于0，小于5分钟，且差值最小，则为需要执行的时间。
				// if ((yesterday - yExecT) > 0 && (yesterday - yExecT) <=
				// spacing && (yesterday - yExecT) < timeTemp) {
				// timeTemp = yesterday - yExecT;
				// temp = te;
				// }
				timeTemp = timeInterval;
				temp = te;
			} catch (JSONException e) {
				theLogger.exception(e);

			}
		}
		return temp;
	}

	/**
	 * remove all cache task before the clear point.
	 * 
	 * @param clearTimePoint
	 * @return
	 */
	public int clearCacheTaskByTimePoint(long clearTimePoint) {
		if (clearTimePoint <= 0) {
			return 0;
		}
		int clearTaskCouter = 0;
		synchronized (_taskCacheQueue) {
			Iterator<JSONObject> taskIter = _taskCacheQueue.iterator();
			JSONObject targetTask = null;
			while (taskIter.hasNext()) {
				targetTask = taskIter.next();
				try {
					String createAt = targetTask.getString("create_at");
					if (Assert.isEmptyString(createAt) == true) {
						continue;
					}
					long createAtVal = Long.parseLong(createAt);
					if (createAtVal > clearTimePoint) {
						continue;
					}
					taskIter.remove();
					clearTaskCouter++;
				} catch (JSONException e) {
					theLogger.exception(e);
				}
			}
		}
		return clearTaskCouter;
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
	}

	// sample
	/**
	 * get local file storage name and path
	 * 
	 * @param fileContent
	 * @return
	 */
	public String getFileStorageNameForTask() {
		synchronized (_sampleNameDataFormat) {
			String sampleFileName = _sampleNameDataFormat.format(new Date());
			return _waitUploadSampleLocalPath + sampleFileName + "_" + System.nanoTime() + ".sample";
		}
	}

	/**
	 * add a file path ,and ready to upload it.
	 * 
	 * @param fileName
	 */
	public void addUploadSampleForDisconnection(String fileName) {
		if (fileName == null || fileName.length() == 0) {
			return;
		}
		synchronized (this._uploadSampleFileQueue) {
			this._uploadSampleFileQueue.add(fileName);
		}
	}

	/**
	 * load all sample file in local file system.
	 */
	private void initLocalUploadFile() {
		File sampleDir = new File(this._waitUploadSampleLocalPath);
		if (sampleDir.exists() == false) {
			sampleDir.mkdirs();
			return;
		}
		File[] sampleFiles = sampleDir.listFiles(_sampleFileFilter);
		if (sampleFiles == null) {
			return;
		}
		synchronized (_uploadSampleFileQueue) {
			for (int i = 0; i < sampleFiles.length; i++) {
				if (sampleFiles[i] == null || sampleFiles[i].isDirectory()) {
					continue;
				}
				this._uploadSampleFileQueue.add(sampleFiles[i].getAbsolutePath());
			}
		}
	}

	/**
	 * start the all upload for sample
	 * 
	 * @param uploadThreads
	 */
	private void initUploadSampleThread(Thread[] uploadThreads) {
		for (int i = 0; i < uploadThreads.length; i++) {
			uploadThreads[i] = new Thread("MonitoringTaskCacheService.uploadSampleThread#" + i) {
				public void run() {
					while (true) {
						try {
							sleep(2000L);
							// upload the raw data to server
							uploadSampleDataForFile();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

			};
			uploadThreads[i].start();
		}
	}

	/**
	 * upload metric in package to server
	 */
	private void uploadSampleDataForFile() {
		if (_storageAdapter == null) {
			_storageAdapter = ServiceHelper.findService(StorageAdapterService.class);
		}
		String pathName = null;
		synchronized (this._uploadSampleFileQueue) {
			if (this._uploadSampleFileQueue.isEmpty()) {
				return;
			}
			pathName = this._uploadSampleFileQueue.remove(0);
		}
		try {
			String sampleDataContent = FileSystemStorageUtil.readSampleDateToFile(pathName, true);
			if (Assert.isEmptyString(sampleDataContent)) {
				return;
			}
			_storageAdapter.uploadRawData(sampleDataContent);
		} catch (Exception e) {
			theLogger.exception(e);
		}
	}
}
