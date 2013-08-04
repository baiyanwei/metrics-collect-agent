package com.secpro.platform.monitoring.agent.bri.syslog;

import java.util.List;
/**
 * 
 * @author sxf
 * ���ڷ�װsyslog����
 *
 */
public class SyslogBean {
	private String IPAdd;
	private List<String> regexs;
	
	public String getIPAdd() {
		return IPAdd;
	}
	public void setIPAdd(String add) {
		IPAdd = add;
	}
	public List<String> getRegexs() {
		return regexs;
	}
	public void setRegexs(List<String> regexs) {
		this.regexs = regexs;
	}
	

}
