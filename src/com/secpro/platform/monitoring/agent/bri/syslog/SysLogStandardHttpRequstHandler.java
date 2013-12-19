package com.secpro.platform.monitoring.agent.bri.syslog;

import javax.xml.bind.annotation.XmlElement;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.secpro.platform.api.client.InterfaceParameter;
import com.secpro.platform.api.server.IHttpRequestHandler;
import com.secpro.platform.core.services.ServiceHelper;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.FetchSysLogStandardRuleAction;
import com.secpro.platform.monitoring.agent.bri.SysLogStandardBeaconInterface;
import com.secpro.platform.monitoring.agent.services.MetricStandardService;

/**
 * @author baiyanwei Sep 24, 2013
 * 
 *         for SYSLOG standard rule receiver.
 * 
 */
public class SysLogStandardHttpRequstHandler implements IHttpRequestHandler {
	final private static PlatformLogger theLogger = PlatformLogger.getLogger(SysLogStandardHttpRequstHandler.class);
	private String id = null;
	private String name = null;
	private String description = null;
	@XmlElement(name = "path", type = String.class)
	public String path = "";

	@Override
	public Object DELETE(HttpRequest request, Object messageObj) throws Exception {
		return "DELETE";
	}

	@Override
	public Object HEAD(HttpRequest request, Object messageObj) throws Exception {
		return "HEAD";
	}

	@Override
	public Object OPTIONS(HttpRequest request, Object messageObj) throws Exception {
		return "OPTIONS";
	}

	@Override
	public Object PUT(HttpRequest request, Object messageObj) throws Exception {
		return POST(request, messageObj);
	}

	@Override
	public Object TRACE(HttpRequest request, Object messageObj) throws Exception {
		return "TRACE";
	}

	@Override
	public Object GET(HttpRequest request, Object messageObj) throws Exception {
		return POST(request, messageObj);
	}

	@Override
	public Object POST(HttpRequest request, Object messageObj) throws Exception {
		String operationCode = request.getHeader(InterfaceParameter.ManagementParameter.OPERATION_TYPE);
		if (Assert.isEmptyString(operationCode) == true) {
			throw new Exception("invalid " + InterfaceParameter.ManagementParameter.OPERATION_TYPE);
		}
		if (messageObj == null) {
			throw new Exception("invalid messag object");
		}
		MetricStandardService metricService = ServiceHelper.findService(MetricStandardService.class);
		if (SysLogStandardBeaconInterface.MSU_COMMAND_SYSLOG_RULE_ADD.equalsIgnoreCase(operationCode) == true
				|| SysLogStandardBeaconInterface.MSU_COMMAND_SYSLOG_RULE_UPDATE.equalsIgnoreCase(operationCode) == true) {
			FetchSysLogStandardRuleAction ruleAction = new FetchSysLogStandardRuleAction(metricService);
			try {
				ruleAction.analyzeStandardRuleOK((String) messageObj);
			} catch (Exception e) {
				theLogger.exception(e);
				throw e;
			}
			return "OK";
		} else if (SysLogStandardBeaconInterface.MSU_COMMAND_SYSLOG_RULE_REMOVE.equalsIgnoreCase(operationCode) == true) {
			metricService.removeRules((String) messageObj);
			return "OK";
		} else {
			return "invalid operationType:" + operationCode;
		}

	}

	@Override
	public void setID(String id) {
		this.id = id;
	}

	@Override
	public String getID() {
		return this.id;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public String getRequestMappingPath() {
		// TODO Auto-generated method stub
		return this.path;
	}

	public String toString() {
		return theLogger.MessageFormat("toString", name, path);
	}
}
