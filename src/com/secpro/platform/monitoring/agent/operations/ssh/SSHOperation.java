package com.secpro.platform.monitoring.agent.operations.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;


import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.operations.MonitorOperation;
import com.secpro.platform.monitoring.agent.operations.OperationError;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;

/**
 * @author baiyanwei Aug 4, 2013
 * 
 *         SSH collecting operation
 * 
 */
public class SSHOperation extends MonitorOperation {
	private static PlatformLogger theLogger = PlatformLogger.getLogger(SSHOperation.class);
	private Connection _sshConnection = null;

	@Override
	public void doIt(MonitoringTask task) throws PlatformException {

		// make sure the connection is closed and release.
		long currentTimePoint = System.currentTimeMillis();
		if (_sshConnection != null) {
			try {
				closeSSHConnection(_sshConnection);
			} catch (Exception e) {
			}
			_sshConnection = null;
		}
		if (task == null) {
			throw new PlatformException("invalid MonitoringTask in SSH operation.");
		}
		theLogger.debug("doTask", task.getTaskObj().toString());
		//
		HashMap<String, String> metaMap = task.getTaskMetaData();
		// get Meta Data
		String username = metaMap.get("username");
		if (Assert.isEmptyString(username) == true) {
			throw new PlatformException("invalid username in SSH operation.");
		}
		String password = metaMap.get("password");
		if (Assert.isEmptyString(password) == true) {
			throw new PlatformException("invalid password in SSH operation.");
		}
		String hostIp = task.getTargetIP();
		if (Assert.isEmptyString(hostIp) == true) {
			throw new PlatformException("invalid target_ip in SSH operation.");
		}
		String protStr = task.getTargetPort();
		if (Assert.isEmptyString(username) == true) {
			throw new PlatformException("invalid port in SSH operation.");
		}
		int port = Integer.parseInt(protStr);
		//
		String shellCommands = task.getContent();
		if (Assert.isEmptyString(username) == true) {
			throw new PlatformException("invalid content in SSH operation.");
		}
		// 伪终端类型，不需要判断空
		String terminalType = metaMap.get("terminal_type");
		// SSH操作返回值所需的过滤字符串，不需要判断空
		String filterString = metaMap.get("filter_string");
		//

		try {

			// "ssh":'{'"s":"{0}","c":"{1}"'}'
			// package metric content.
			_sshConnection = createSSHConnection(hostIp, username, password, port);
			String metricContent = executeShellCommand(_sshConnection, shellCommands, terminalType, filterString);
			HashMap<String, String> messageInputAndRequestHeaders = this._monitoringWorkflow.getMessageInputAndRequestHeaders(this._operationID, shellCommands, metricContent);
			this._monitoringWorkflow.createResultsMessage(this._operationID, messageInputAndRequestHeaders);
			this.fireCompletedSuccessfully();
		} catch (Exception e) {
			theLogger.exception(e);
			this._operationError._type = OperationError.ErrorType.operation;
			this._operationError._code = 0;
			this._operationError._message = e.getMessage();
			if (task.getTaskObj() != null) {
				this._operationError._entry = task.getTaskObj().toString();
			}
			this._operationError._exception = new PlatformException(e.getMessage(), e);
			this.fireError(this._operationError);
			return;

		} finally {
			if (_sshConnection != null) {
				try {
					closeSSHConnection(_sshConnection);
				} catch (Exception e) {
				}
				_sshConnection = null;
			}
		}
		theLogger.debug("finishOperation", task.getTaskObj().toString(), String.valueOf(System.currentTimeMillis() - currentTimePoint));

	}

	@Override
	public void stopIt(MonitoringTask task) throws PlatformException {
		//
		if (_sshConnection != null) {
			try {
				closeSSHConnection(_sshConnection);
			} catch (Exception e) {
			}
		}
		//
		this._operationError._message = "stop the operation on stopIt";
		this.fireError(this._operationError);
	}

	@Override
	public void start() throws PlatformException {
		// TODO Auto-generated method stub
	}

	@Override
	public void destroy() throws PlatformException {
		this._operationError = null;
	}

	/**
	 * 通过SSH远程连接设备
	 * 
	 * @param hostname
	 *            IP地址
	 * @param username
	 *            用户名
	 * @param password
	 *            密码
	 * @param port
	 *            SSH端口号
	 * @return Connection SSH远程连接
	 */
	private Connection createSSHConnection(String hostname, String username, String password, int port) throws PlatformException {
		Connection conn = null;
		try {
			conn = new Connection(hostname, port);
			conn.connect();
			boolean isAuthenticated = conn.authenticateWithPassword(username, password);
			if (isAuthenticated == false)
				throw new PlatformException("Authentication failed.");
		} catch (IOException e) {
			theLogger.exception(e);
		}
		return conn;
	}

	/**
	 * 执行命令行
	 * 
	 * @param conn
	 *            SSH连接
	 * @param command
	 *            远程执行命令行代码
	 */
	private String executeShellCommand(Connection connection, String commands, String terminalType, String filterString) throws PlatformException {
		if (connection == null) {
			return null;
		}

		Session sshSession = null;
		BufferedReader bufferedReader = null;
		String[] command = commands.split("\\^");
		String terminalTypeDefault = "vt100";
		if (Assert.isEmptyString(terminalType) == false) {
			terminalTypeDefault = terminalType;
		}
		PrintWriter sessionOut = null;
		StringBuilder metricContent = new StringBuilder();
		try {
			sshSession = connection.openSession();
			// 创建伪终端
			sshSession.requestPTY(terminalTypeDefault);
			sshSession.startShell();
			// 获得session输出流
			sessionOut = new PrintWriter(sshSession.getStdin());
			for (int i = 0; i < command.length; i++) {
				sessionOut.println(command[i]);
			}
			// 在执行完所有命令后，必须执行exit命令，退出。
			sessionOut.println("exit");
			// 需要在采集命令都设置完毕后，立刻关闭输出流。
			sessionOut.close();
			sessionOut = null;
			sshSession.waitForCondition(ChannelCondition.CLOSED | ChannelCondition.EOF | ChannelCondition.EXIT_STATUS, 30000);
			// 获得session输入流
			bufferedReader = new BufferedReader(new InputStreamReader(new StreamGobbler(sshSession.getStdout()),"utf-8"));
			String lineStr = null;
			if (Assert.isEmptyString(filterString) == true) {
				while ((lineStr = bufferedReader.readLine()) != null) {
					metricContent.append(lineStr);
					metricContent.append("^");
				}
			} else {
				// 将结果中部分无用数据过滤掉
				int isSave = 0;
				while ((lineStr = bufferedReader.readLine()) != null) {
					if (lineStr.indexOf(filterString) >= 0) {
						isSave = 1;
					}
					if (lineStr.indexOf(filterString) >= 0 && lineStr.indexOf("exit") >= 0) {
						isSave = 0;
					}
					if (isSave == 1 && lineStr.indexOf(filterString) == -1) {
						metricContent.append(lineStr);
						metricContent.append("^");
					}

				}
			}
			//System.out.println(metricContent.toString());
			// System.out.println("ExitCode: " + sshSession.getExitStatus());

		} catch (IOException e) {
			theLogger.exception(e);
		} finally {
			if (sessionOut != null) {
				sessionOut.close();
			}
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (Exception e) {
				}
			}
			if (sshSession != null) {
				sshSession.close();
			}
		}
		return metricContent.toString();

	}

	/**
	 * 关闭SSH连接
	 * 
	 * @param conn
	 *            连接
	 * @param sess
	 *            SSH session
	 */
	private synchronized void closeSSHConnection(Connection conn) {
		if (conn != null) {
			conn.close();
		}
	}

}
