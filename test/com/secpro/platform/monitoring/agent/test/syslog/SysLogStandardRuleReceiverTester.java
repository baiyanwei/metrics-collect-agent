package com.secpro.platform.monitoring.agent.test.syslog;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;
import com.secpro.platform.monitoring.agent.test.syslog.http.HttpClient;

public class SysLogStandardRuleReceiverTester {

	public static void main(String[] args) {
		String ruleContent = getOneJobTest(1);
		URI uri;
		try {
			uri = new URI("http://localhost:8889/");
			new HttpClient(uri).start(ruleContent);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static String getOneJobTest(int count) {
		try {
			JSONArray messageObj = new JSONArray();
			//
			/*
			{
				ip string： 采集对象IP地址
				regexs Map： syslog正则表达式规则库
				checkNum int：syslog标准化后上发条件
				checkAction String：syslog标准化后上传动作
			}
			*/
			for (int i = 1; i <= count; i++) {
				JSONObject ruleObj = new JSONObject();
				ruleObj.put("ip", "10.0.0." + i);
				ruleObj.put("checkAction", "ruleAValue");
				ruleObj.put("checkNum", "100");
				JSONObject regexs = new JSONObject();
				ruleObj.put("regexs", "ruleBValue");
				//
				regexs.put("regexs1", "regexs1");
				messageObj.put(ruleObj);
			}
			//
			return messageObj.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
}
