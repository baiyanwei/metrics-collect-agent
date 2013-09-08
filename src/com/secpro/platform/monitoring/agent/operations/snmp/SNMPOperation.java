package com.secpro.platform.monitoring.agent.operations.snmp;

import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONObject;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.operations.MonitorOperation;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;

public class SNMPOperation extends MonitorOperation {
	private final static PlatformLogger theLogger = PlatformLogger.getLogger(SNMPOperation.class);
	private SNMPCollectAdapter _SNMPCollectAdapter = null;

	@Override
	public void doIt(MonitoringTask task) throws PlatformException {
		try {
			if (this._SNMPCollectAdapter != null) {
				this._SNMPCollectAdapter.stopListener();
				this._SNMPCollectAdapter = null;
			}
			// set your collecting result into message.
			System.out.println("SNMPOperation>>do task:" + task.getTaskDescription());
			// get task meta data
			HashMap<String, String> metaMap = task.getTaskMetaData();
			//
			String version = metaMap.get("snmp_version");
			if (Assert.isEmptyString(version) == true) {
				throw new PlatformException("invaild snmp_version in SNMP operation.");
			}
			String hostIp = metaMap.get("host_ip");
			if (Assert.isEmptyString(hostIp) == true) {
				throw new PlatformException("invaild host_ip in SNMP operation.");
			}
			String protStr = metaMap.get("port");
			if (Assert.isEmptyString(protStr) == true) {
				throw new PlatformException("invaild port in SNMP operation.");
			}
			int port = Integer.parseInt(protStr);
			//
			String community = metaMap.get("community");
			if (Assert.isEmptyString(protStr) == true) {
				throw new PlatformException("invaild community in SNMP operation.");
			}
			String mibs = metaMap.get("mibs");
			if (Assert.isEmptyString(protStr) == true) {
				throw new PlatformException("invaild mibs in SNMP operation.");
			}
			String[] mibArray = mibs.split(",");
			//
			String username = metaMap.get("username");
			String auth = metaMap.get("auth");
			String authPass = metaMap.get("authPass");
			String priv = metaMap.get("priv");
			String privPass = metaMap.get("privPass");
			SNMPReferentBean referentBean = new SNMPReferentBean();
			referentBean.setVersion(Integer.parseInt(version));
			referentBean.setTargetIP(hostIp);
			referentBean.setPort(port);
			referentBean.setCommunity(community);
			if (version.trim().equals("3")) {
				referentBean.setUserName(username);
				referentBean.setAuth(auth);
				referentBean.setAuthPass(authPass);
				referentBean.setPriv(priv);
				referentBean.setPrivPass(privPass);
			}
			//
			referentBean.setMibList(Arrays.asList(mibArray));
			HashMap<String, String> metricMap = collectSNMP(referentBean);
			/*
			 * "ssh": '{' "mid": "{0}", "t": "{1}", "ip": "{2}", "s":
			 * "{3}","c":"{4}" '}'
			 */
			// build collection for result.
			HashMap<String, String> messageInputAndRequestHeaders = this._monitoringWorkflow.getMessageInputAndRequestHeaders(this._operationID, task.getMonitorID(),
					task.getTimestamp(), task.getPropertyString(MonitoringTask.TASK_TARGET_IP_PROPERTY_NAME), "mib", new JSONObject(metricMap).toString());
			this._monitoringWorkflow.createResultsMessage(this._operationID, messageInputAndRequestHeaders);
			this.fireCompletedSuccessfully();
		} catch (Exception e) {
			theLogger.exception(e);
			this._operationError._exception = new PlatformException(e.getMessage(), e);
			this.fireError(this._operationError);
			return;
		}
	}

	@Override
	public void stopIt(MonitoringTask task) throws PlatformException {
		if (this._SNMPCollectAdapter != null) {
			this._SNMPCollectAdapter.stopListener();
			this._SNMPCollectAdapter = null;
		}
		this._operationError._message = "stop the operation on stopIt";
		this.fireError(this._operationError);
	}

	@Override
	public void start() throws PlatformException {
		theLogger.info("startOperation");
	}

	@Override
	public void destroy() throws PlatformException {
		this._operationError = null;
		if (this._SNMPCollectAdapter != null) {
			this._SNMPCollectAdapter.stopListener();
			this._SNMPCollectAdapter = null;
		}
		theLogger.info("destroyOperation");
	}

	/**
	 * SNMP方式采集
	 * 
	 * @param referentBean
	 * @return
	 */
	private HashMap<String, String> collectSNMP(SNMPReferentBean referentBean) {
		this._SNMPCollectAdapter = new SNMPCollectAdapter();
		return this._SNMPCollectAdapter.snmpAllVer(referentBean);
	}
}
