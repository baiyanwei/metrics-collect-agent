package com.secpro.platform.monitoring.agent.storages.http;

import com.secpro.platform.api.client.IClientResponseListener;
import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.actions.FetchSysLogStandardRuleAction;

/**
 * @author baiyanwei Jul 13, 2013
 * 
 *         Storage listener instance.
 * 
 */
public class FetchStandardRuleListener implements IClientResponseListener {
	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(FetchStandardRuleListener.class);
	public FetchSysLogStandardRuleAction _fetchSysLogStandardRuleAction = null;
	private String _name = "FetchStandardRuleListener";
	private String _id = "FetchStandardRuleListener";
	private String _description = "FetchStandardRuleListener";

	public FetchStandardRuleListener(FetchSysLogStandardRuleAction action) {
		this._fetchSysLogStandardRuleAction = action;
	}

	@Override
	public void setID(String id) {
		this._id = id;

	}

	@Override
	public String getID() {
		return _id;
	}

	@Override
	public void setName(String name) {
		this._name = name;
	}

	@Override
	public String getName() {
		return this._name;
	}

	@Override
	public void setDescription(String description) {
		this._description = description;
	}

	@Override
	public String getDescription() {
		return this._description;
	}

	@Override
	public void fireSucceed(Object messageObj) throws PlatformException {
		try {
			if (messageObj != null) {
				final String contents = messageObj.toString();
				// final String contents = (String) (storageResponse.getData());
				if (contents != null && contents.trim().length() > 0) {
					new Thread("FetchStandardRuleListener.FetchSysLogStandardRuleAction.anaylzeStandardRule") {
						public void run() {
							_fetchSysLogStandardRuleAction.analyzeStandardRuleOK(contents);
						}
					}.start();
					return;
				}
			}
		} catch (Exception e) {
			theLogger.exception(e);
		}

	}

	@Override
	public void fireError(Object messageObj) throws PlatformException {
		try {
			if (messageObj != null) {
				final String contents = messageObj.toString();
				if (contents != null && contents.trim().length() > 0) {
					new Thread("DPUStorageListener.taskProcessingAction.recycle") {
						public void run() {
							_fetchSysLogStandardRuleAction.analyzeStandardRuleOK(contents);
						}
					}.start();
				}
				theLogger.exception(new Exception(messageObj.toString()));
			}
		} catch (Exception e) {
			theLogger.exception(e);
		}

	}
}