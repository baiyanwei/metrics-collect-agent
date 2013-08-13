package com.secpro.platform.monitoring.agent.actions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.services.MonitoringTaskCacheService;

/**
 * @author baiyanwei Aug 3, 2013
 * 
 * 
 * 
 */
public class CacheTaskManageAction extends TimerTask {
	private static PlatformLogger theLogger = PlatformLogger.getLogger(CacheTaskManageAction.class);
	private MonitoringTaskCacheService _monitoringTaskCacheService = null;
	//
	//private final int hourOfDay, minute, second;
	private static SimpleDateFormat timedf1 = new SimpleDateFormat("yyyyMMddHHmmss");
	private static SimpleDateFormat timedf2 = new SimpleDateFormat("yyyyMMdd");

	public CacheTaskManageAction(MonitoringTaskCacheService monitoringTaskCacheService) {
		this._monitoringTaskCacheService = monitoringTaskCacheService;
	}

	private int diffDate(Date now, Date execTime) throws ParseException {

		System.out.println((int) ((getMillis(timedf2.parse(timedf2.format(now))) - getMillis(timedf2.parse(timedf2.format(execTime)))) / (24 * 3600 * 1000)));
		return (int) ((getMillis(timedf2.parse(timedf2.format(now))) - getMillis(timedf2.parse(timedf2.format(execTime)))) / (24 * 3600 * 1000));
	}

	private long getMillis(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.getTimeInMillis();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		removeTask();
	}

	private void removeTask() {
		// TODO Auto-generated method stub
		System.out.println("========================开始删除任务池中两天前的任务==========================");
		int count = 0;
		int sum = _monitoringTaskCacheService.getTaskCacheQueueSize();
		System.out.println("size=" + sum);
		for (int i = 0; i < sum; i++) {
			JSONObject oneTaskObj = _monitoringTaskCacheService.getOneTaskFromCache();
			if (oneTaskObj == null) {
				continue;
			}
			try {
				Date execTime = timedf1.parse(oneTaskObj.getString("execTime"));

				// 如果任务的执行时间与今天相差2天和2天以上则删除任务
				if (diffDate(new Date(), execTime) >= 2) {
					// TODO delete the old task.
					// TaskPool.getAndRemoveTask();
					_monitoringTaskCacheService.getTaskCacheQueue().remove(oneTaskObj);
					count++;
				} else {
					break;
				}
			} catch (ParseException e) {
				theLogger.exception(e);
				e.printStackTrace();
			} catch (JSONException e) {
				theLogger.exception("Task Content Format Error", e);
			}
		}
		System.out.println("============结束删除任务池中两天前的任务，共计删除:" + count + "个任务==================");
	}
}
