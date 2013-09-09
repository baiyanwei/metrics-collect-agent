package com.secpro.platform.monitoring.agent.operations.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.operations.MonitorOperation;
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
			throw new PlatformException("invaild MonitoringTask in SSH operation.");
		}
		theLogger.debug("doTask", task.getTaskObj().toString());
		//
		HashMap<String, String> metaMap = task.getTaskMetaData();
		// get Meta Data
		String username = metaMap.get("username");
		if (Assert.isEmptyString(username) == true) {
			throw new PlatformException("invaild username in SSH operation.");
		}
		String password = metaMap.get("password");
		if (Assert.isEmptyString(username) == true) {
			throw new PlatformException("invaild password in SSH operation.");
		}
		String hostIp = metaMap.get("host_ip");
		if (Assert.isEmptyString(username) == true) {
			throw new PlatformException("invaild host_ip in SSH operation.");
		}
		String protStr = metaMap.get("port");
		if (Assert.isEmptyString(username) == true) {
			throw new PlatformException("invaild port in SSH operation.");
		}
		int port = Integer.parseInt(protStr);
		//
		String shellCommand = metaMap.get("shell_command");
		if (Assert.isEmptyString(username) == true) {
			throw new PlatformException("invaild shell_command in SSH operation.");
		}
		//

		try {
			String shellCommands[] = shellCommand.split("\\^");
			_sshConnection = createSSHConnection(hostIp, username, password, port);
			StringBuilder results=new StringBuilder();
			for(int i=0;i<shellCommands.length;i++){
				String res=executeShellCommand(_sshConnection, shellCommands[i]);
				if(res!=null&&(!res.equals(""))){
					results.append(res);
				}
			}
			// "ssh": '{' "mid": "{0}", "t": "{1}", "ip": "{2}",
			// "s":"{3}","c":"{4}"'}'
			// package metric content.
			String metricContent =results.toString();
			HashMap<String, String> messageInputAndRequestHeaders = this._monitoringWorkflow.getMessageInputAndRequestHeaders(this._operationID, task.getMonitorID(),
					task.getTimestamp(), task.getPropertyString(MonitoringTask.TASK_TARGET_IP_PROPERTY_NAME), shellCommand, metricContent);
			this._monitoringWorkflow.createResultsMessage(this._operationID, messageInputAndRequestHeaders);
			this.fireCompletedSuccessfully();
		} catch (Exception e) {
			theLogger.exception(e);
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
	private String executeShellCommand(Connection connection, String command) throws PlatformException {
		if (connection == null) {
			return null;
		}
		Session sshSession = null;
		BufferedReader bufferedReader = null;
		StringBuilder metricContent = new StringBuilder();
		try {

			sshSession = connection.openSession();
			sshSession.execCommand(command);
			System.out.println("Here is some information about the remote host:");
			bufferedReader = new BufferedReader(new InputStreamReader(new StreamGobbler(sshSession.getStdout())));
			String lineStr = null;
			while ((lineStr = bufferedReader.readLine()) != null) {
				metricContent.append(lineStr);
				metricContent.append("^");
			}
			System.out.println(metricContent.toString());
			System.out.println("ExitCode: " + sshSession.getExitStatus());

		} catch (IOException e) {
			theLogger.exception(e);
		} finally {
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
