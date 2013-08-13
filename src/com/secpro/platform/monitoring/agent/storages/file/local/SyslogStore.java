package com.secpro.platform.monitoring.agent.storages.file.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.json.JSONObject;

import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.monitoring.agent.bri.SysLogBeaconInterface;
import com.secpro.platform.monitoring.agent.services.MetricStandardService;
import com.secpro.platform.monitoring.agent.services.MetricUploadService;

/**
 * 用以syslog日志本地存储
 * 
 * @author liyan
 * 
 */
public class SyslogStore {
	private SysLogBeaconInterface _sysLogBeaconInterface = null;
	private MetricUploadService _metricUploadService = null;
	private MetricStandardService _metricStandardService = null;
	private SimpleDateFormat timedf1 = new SimpleDateFormat("yyyyMMddHH");
	private PrintStream out;
	private Queue<JSONObject> syslogs = new LinkedList<JSONObject>();
	private int index = 1;
	private String lastDate = "";
	// private boolean flag;
	private File f;

	public SyslogStore(SysLogBeaconInterface sysLogBeaconInterface) {
		this._sysLogBeaconInterface = sysLogBeaconInterface;
	}

	/**
	 * 获取本地存储的文件名称
	 * 
	 * @param filename
	 * @return
	 */
	private String getLogName(String filename) {
		// 为日志文件增加日期后缀
		String logFileName = filename + timedf1.format(new Date());
		// 判断日期文件名称是否重新计数
		if (!logFileName.equals(lastDate)) {
			index = 1;
		}
		// 保存当前文件名称，用于下次判断是否重新计数
		lastDate = logFileName;
		// 日志文件增加序列号
		if (index < 10) {
			logFileName = logFileName + "00" + index;
		} else if (index >= 10 && index < 100) {
			logFileName = logFileName + "0" + index;
		} else {
			logFileName = logFileName + index;
		}
		return logFileName;
	}

	/**
	 * syslog日志存储方法
	 */
	public void storeSyslog() {
		if (_metricStandardService == null) {
			_metricStandardService = ServiceHelper.findService(MetricStandardService.class);
		}
		try {

			// 判断文件流日否为空，如为空时创建文件流

			if (out == null) {
				String sylogFileName = getLogName("syslog");
				f = new File(_sysLogBeaconInterface._sysLogPath + sylogFileName);
				System.out.println(f.getAbsolutePath());
				if (!f.exists()) {
					f.createNewFile();
				}
				out = new PrintStream(new FileOutputStream(f, true));
			}
			if (compareDate()) {

				long syslogMaxMB = 0;
				long syslogMaxB = 0;
				// 计算日志文件最大字节数限制

				syslogMaxMB = _sysLogBeaconInterface._sysLogMax;
				syslogMaxB = syslogMaxMB * 1024 * 1024;
				// 当文件对象不为空，文件大小小于限制大小时

				if (f != null && f.length() <= syslogMaxB) {

					for (int i = 0; i < syslogs.size(); i++) {
						synchronized (syslogs) {
							// 存储日志
							System.out.println(syslogs.peek().toString());
							JSONObject syslog = syslogs.peek();
							// *******格式化********
							String ip = syslog.getString("hostIP");
							String msg = syslog.getString("msg");
							String cdate = syslog.getString("cdate");
							Map<String, String> syslogMap = _metricStandardService.matcher(ip, msg);
							//如果解析出来的数据小于2个将不上传服务器，直接丢弃（一般都会有时间和类型）
							//调试阶段暂时设置为0，后续调整为配置方式
							if(syslogMap!=null&&syslogMap.size()>0){
								JSONObject syslogFormt = new JSONObject();
								JSONObject sys=new JSONObject();
								sys.put("ip", ip);
								sys.put("ct", cdate);
								sys.put("s", syslogMap);
								//测试阶段暂时上传原始日志
								sys.put("o", msg);
								syslogFormt.put("syslog", sys); 
								// put it into upload pool
								if (_metricUploadService == null) {
									_metricUploadService = ServiceHelper.findService(MetricUploadService.class);
								}
								_metricUploadService.addUploadMetric(syslogFormt);
								// ---------------
							}
							// 存储文件
							out.println(syslogs.poll().toString());
						}
					}
					out.flush();
				} else if (f != null && f.length() > syslogMaxB) {
					System.out.println(f);
					System.out.println(f.length());
					index++;
					out.close();
					String sylogFileName = getLogName("syslog");
					f = new File(_sysLogBeaconInterface._sysLogPath + sylogFileName);
					if (!f.exists()) {
						f.createNewFile();
					}
					out = new PrintStream(new FileOutputStream(f, true));
					storeSyslog();
				} else {
					System.out.println("kong");
				}
			} else {
				if (out != null) {
					out.close();
				}
				String sylogFileName = getLogName("syslog");
				f = new File(_sysLogBeaconInterface._sysLogPath + sylogFileName);
				if (!f.exists()) {
					f.createNewFile();
				}
				out = new PrintStream(new FileOutputStream(f, true));

				for (int i = 0; i < syslogs.size(); i++) {
					synchronized (syslogs) {
						JSONObject syslog = syslogs.peek();
						// *******格式化********
						String ip = syslog.getString("hostIP");
						String msg = syslog.getString("msg");
						String cdate = syslog.getString("cdate");
						Map<String, String> syslogMap = _metricStandardService.matcher(ip, msg);
						//如果解析出来的数据小于2个将不上传服务器，直接丢弃（一般都会有时间和类型）
						//调试阶段暂时设置为0，后续调整为配置方式
						if(syslogMap!=null&&syslogMap.size()>0){
							JSONObject syslogFormt = new JSONObject();
							JSONObject sys=new JSONObject();
							sys.put("ip", ip);
							sys.put("ct", cdate);
							sys.put("s", syslogMap);
							//测试阶段暂时上传原始日志
							sys.put("o", msg);
							syslogFormt.put("syslog", sys);
							// put it into upload pool
							if (_metricUploadService == null) {
								_metricUploadService = ServiceHelper.findService(MetricUploadService.class);
							}
							_metricUploadService.addUploadMetric(syslogFormt);
							// ---------------
						}
						out.println(syslogs.poll());
					}
				}

				out.flush();
			}
			out.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 判断是否到下一个小时，需要产生新文件
	 * 
	 * @return
	 */
	private boolean compareDate() {
		String logFileName = _sysLogBeaconInterface._sysLogPath + timedf1.format(new Date());
		if (logFileName.equals(lastDate)) {
			return true;
		}

		return false;
	}

	/**
	 * 将syslog日志存储到队列中
	 * 
	 * @param syslog
	 */
	public void addSyslog(JSONObject syslog) {
		synchronized (syslogs) {
			syslogs.add(syslog);
		}
	}
	/*
	 * private String getLastFileName(String fullfileName, int index) { flag =
	 * false; String SyslogfileName = fullfileName; String lastFileName = ""; if
	 * (index < 10) { SyslogfileName = fullfileName + "00" + index; } else if
	 * (index >= 10 && index < 100) { SyslogfileName = fullfileName + "0" +
	 * index; } else { SyslogfileName = fullfileName + index; } index++;
	 * lastFileName = SyslogfileName; File f = new File(SyslogfileName); if
	 * (f.exists()) { lastFileName = getLastFileName(fullfileName, index); }
	 * return lastFileName; }
	 */
}