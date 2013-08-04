package com.secpro.platform.monitoring.agent.operations.snmp;

import java.util.List;

/**
 * 
 * @author sxf this is an entity snmp Contains all the properties of snmp
 */
public class SNMPReferentBean {
	private int version;
	private String targetIP;
	private int port;
	private String community;
	private List<String> mibList;
	private String userName;
	private String auth;
	private String authPass;
	private String priv;
	private String privPass;

	public SNMPReferentBean() {
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getTargetIP() {
		return targetIP;
	}

	public void setTargetIP(String targetIP) {
		this.targetIP = targetIP;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getCommunity() {
		return community;
	}

	public void setCommunity(String community) {
		this.community = community;
	}

	public List<String> getMibList() {
		return mibList;
	}

	public void setMibList(List<String> mibList) {
		this.mibList = mibList;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getAuthPass() {
		return authPass;
	}

	public void setAuthPass(String authPass) {
		this.authPass = authPass;
	}

	public String getPriv() {
		return priv;
	}

	public void setPriv(String priv) {
		this.priv = priv;
	}

	public String getPrivPass() {
		return privPass;
	}

	public void setPrivPass(String privPass) {
		this.privPass = privPass;
	}

}
