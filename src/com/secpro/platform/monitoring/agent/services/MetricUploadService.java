package com.secpro.platform.monitoring.agent.services;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.services.ServiceInfo;

/**
 * @author baiyanwei Jul 22, 2013
 * 
 *         for SysLog , upload the SYSLOG metric into remote.
 * 
 */
@ServiceInfo(description = "metric upload service, upload the metric to data process server.", configurationPath = "mca/services/MetricUploadService/")
public class MetricUploadService implements IService {
	final public static String UPLOAD_MODE_POOL = "pool";
	final public static String UPLOAD_MODE_DIRECTLY = "directly";

	@XmlElement(name = "uploadMode", defaultValue = "directly")
	public String _uploadMode = "";

	@XmlElement(name = "isUploadMetricInPackage", type = Boolean.class, defaultValue = "120000")
	public boolean _isUploadMetricInPackage = false;
	/**
	 * upload the metric package limit for minimum,unit is KB
	 */
	@XmlElement(name = "minLimitPackageSiz", type = Long.class, defaultValue = "1024")
	public long _minLimitPackageSize = 1024;

	/**
	 * upload the metric package limit for maximum ,unit is KB
	 */
	@XmlElement(name = "maxLimitPackageSize", type = Long.class, defaultValue = "5120")
	public long _maxLimitPackageSize = 5120;
	
	@XmlElement(name = "uploadThreadMax", type = Long.class, defaultValue = "6")
	public long _uploadThreadMax = 6;

	/**
	 * waiting upload queue.
	 */
	private ArrayList<byte[]> _uploadMetricQueue = new ArrayList<byte[]>();

	Thread[] _uploadThreads = null;

	private StorageAdapterService _storageAdapter = null;

	@Override
	public void start() throws Exception {
		if (UPLOAD_MODE_POOL.equalsIgnoreCase(_uploadMode)) {
			_uploadThreads = new Thread[6];
			initUploadThread(_uploadThreads);
		}
		_storageAdapter = ServiceHelper.findService(StorageAdapterService.class);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void stop() throws Exception {
		if (this._uploadThreads != null) {
			for (int i = 0; i < _uploadThreads.length; i++) {
				if (_uploadThreads[i] != null) {
					try {
						_uploadThreads[i].stop();
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}
		}
	}

	private void initUploadThread(Thread[] uploadThreads) {
		for (int i = 0; i < uploadThreads.length; i++) {
			uploadThreads[i] = new Thread("MetricUploadService.uploadThread#" + i) {
				public void run() {
					while (true) {
						try {
							sleep(2000L);
							// upload the raw data to server
							uploadMetricInPackage();
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
	 * add a metric message object into waiting queue.
	 * 
	 * @param metricObj
	 */
	public void addUploadMetric(JSONObject metricObj) {
		if (metricObj == null) {
			return;
		}
		// upload the current if we defined the upload mode is directly
		if (UPLOAD_MODE_DIRECTLY.equalsIgnoreCase(_uploadMode)) {
			try {
				if (_storageAdapter == null) {
					_storageAdapter = ServiceHelper.findService(StorageAdapterService.class);
				}
				// upload to DTSS
				_storageAdapter.uploadRawData(formatMessage(metricObj));
			} catch (PlatformException e) {
				e.printStackTrace();
			}
			return;
		}
		synchronized (this._uploadMetricQueue) {
			this._uploadMetricQueue.add(getJSONObjectContent(metricObj).getBytes());
		}
	}

	/**
	 * get metric message object from upload queue.
	 * 
	 * @return
	 */
	public JSONArray getUploadMetric() {

		JSONArray packageObject = new JSONArray();
		byte[] metricContentArray = null;
		synchronized (this._uploadMetricQueue) {
			if (this._uploadMetricQueue.isEmpty() == false) {
				metricContentArray = this._uploadMetricQueue.remove(0);
			}
		}
		if (metricContentArray != null) {
			packageObject.put(getJSONObjectObj(new String(metricContentArray)));
		}
		if (packageObject.length() == 0) {
			return null;
		}
		return packageObject;
	}

	/**
	 * get multi-metric in one package
	 * 
	 * @return
	 */
	public JSONArray getPackageUploadMetric() {
		JSONArray packageObject = new JSONArray();
		long currentPackageSize = 0;
		synchronized (this._uploadMetricQueue) {
			long poolSiz = this._uploadMetricQueue.size();
			for (int i = 0; i < poolSiz && this._uploadMetricQueue.isEmpty() == false; i++) {
				currentPackageSize = currentPackageSize + (this._uploadMetricQueue.get(0).length * 8);
				packageObject.put(new String(this._uploadMetricQueue.remove(0)));
				if (currentPackageSize > this._maxLimitPackageSize) {
					break;
				}
			}
		}
		if (packageObject.length() == 0) {
			return null;
		}
		return packageObject;
	}

	/**
	 * upload metric in package to server
	 */
	private void uploadMetricInPackage() {
		if (_storageAdapter == null) {
			_storageAdapter = ServiceHelper.findService(StorageAdapterService.class);
		}
		Object messageObject = null;
		if (this._isUploadMetricInPackage) {
			messageObject = this.getPackageUploadMetric();
		} else {
			messageObject = this.getUploadMetric();
		}
		if (messageObject == null) {
			return;
		}
		try {
			// upload to DTSS
			_storageAdapter.uploadRawData(formatMessage(messageObject));
		} catch (PlatformException e) {
			e.printStackTrace();
		}
	}

	/**
	 * get JSONObject content in String , to store it into pool.
	 * 
	 * @param json
	 * @return
	 */
	private String getJSONObjectContent(JSONObject json) {
		return json.toString();
	}

	/**
	 * get JSONObject Object by string content.
	 * 
	 * @param content
	 * @return
	 */
	private JSONObject getJSONObjectObj(String content) {
		try {
			return new JSONObject(content);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	private JSONObject formatMessage(Object message) {
		JSONObject packageMessage = new JSONObject();
		try {
			packageMessage.put("timestamp", System.currentTimeMillis());
			packageMessage.put("body", message);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return packageMessage;
	}
}
