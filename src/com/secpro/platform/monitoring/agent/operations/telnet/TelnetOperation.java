package com.secpro.platform.monitoring.agent.operations.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.commons.net.telnet.TelnetClient;

import com.secpro.platform.core.exception.PlatformException;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.operations.MonitorOperation;
import com.secpro.platform.monitoring.agent.operations.OperationError;
import com.secpro.platform.monitoring.agent.workflow.MonitoringTask;

/**
 * 
 * @author liyan 2013-9-9
 * 
 */
public class TelnetOperation extends MonitorOperation {
	private static PlatformLogger theLogger = PlatformLogger.getLogger(TelnetOperation.class);
	private TelnetClient _telnetClient = null;
	private InputStream _telnetIn;
	private PrintStream _telnetOut;
	private String _prompt;

	public TelnetOperation() {
	}

	public void init(String ip, int port, String username, String password, String prompt, String userPrompt, String passwdPrompt) throws Exception {
		_telnetClient = new TelnetClient();
		try {
			_telnetClient.setConnectTimeout(10000);
			_telnetClient.connect(ip, port);
			_telnetIn = _telnetClient.getInputStream();
			_telnetOut = new PrintStream(_telnetClient.getOutputStream());
			this._prompt = prompt;
			this.login(username, password, userPrompt, passwdPrompt);
		} catch (SocketException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
	}

	public void login(String username, String password, String userPrompt, String passwdPrompt) {
		readUntil(userPrompt);
		write(username);
		readUntil(passwdPrompt);
		write(password);
		readUntil(_prompt + "");
	}

	public String readUntil(String pattern) {
		try {
			char lastChar = pattern.charAt(pattern.length() - 1);
			StringBuffer sb = new StringBuffer();
			char ch = (char) _telnetIn.read(); 
			StringBuffer temp = new StringBuffer();
			while (true) {
				if (ch == '\r') {

				} else if (ch == '\n') {
					sb.append('^');
					temp.delete(0, temp.length());
				} else {
					sb.append(ch);
					temp.append(ch);
				}
				// System.out.println(ch+"-------"+lastChar);
				if (ch == lastChar) {
					if (sb.toString().endsWith(pattern)) {
						return sb.toString();
					}
				}
				ch = (char) _telnetIn.read();
				System.out.print(ch);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String readUntil(String pattern, String fenge, String fengeci) {
		try {
			char lastChar = pattern.charAt(pattern.length() - 1);
			StringBuffer sb = new StringBuffer();
			char ch = (char) _telnetIn.read();
			StringBuffer temp = new StringBuffer();
			while (true) {
				if (ch == '\r') {
				} else if (ch == '\n') {
					sb.append('^');
					temp.delete(0, temp.length());
				} else {
					sb.append(ch);
					temp.append(ch);
				}
				if (ch == lastChar) {
					if (sb.toString().endsWith(pattern)) {
						return sb.toString();
					}
				}
				if ((ch + "").equals(fenge)) {
					if (temp.toString().toLowerCase().contains(fengeci)) {
						write(" ");
					}
				}
				ch = (char) _telnetIn.read();
				System.out.print(ch);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void write(String value) {
		try {
			_telnetOut.println(value);
			_telnetOut.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String sendCommand(String command, String openCommand, String execPrompt, String fenge, String fengeci) {
		try {
			write(command);
			if (command.equals(openCommand)) {
				this._prompt = execPrompt;
			}
			return readUntil(_prompt + "", fenge, fengeci);
		} catch (Exception e) {
			theLogger.exception(e);
		}
		return null;
	}

	public void disconnect() {
		try {
			_telnetClient.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start() throws PlatformException {
		// TODO Auto-generated method stub
	}

	@Override
	public void destroy() throws PlatformException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doIt(MonitoringTask task) throws PlatformException {
		// TODO Auto-generated method stub
		long currentTimePoint = System.currentTimeMillis();
		if (_telnetClient != null) {
			try {
				this.disconnect();
			} catch (Exception e) {
			}
			_telnetClient = null;
		}
		if (task == null) {
			throw new PlatformException("invalid MonitoringTask in TELNET operation.");
		}
		theLogger.debug("doTask", task.getTaskObj().toString());
		HashMap<String, String> metaMap = task.getTaskMetaData();
		String username = metaMap.get("username");
		if (Assert.isEmptyString(username) == true) {
			throw new PlatformException("invalid username in TELNET operation.");
		}
		String password = metaMap.get("password");
		if (Assert.isEmptyString(password) == true) {
			throw new PlatformException("invalid password in TELNET operation.");
		}
		String ip = task.getTargetIP();
		if (Assert.isEmptyString(ip) == true) {
			throw new PlatformException("invalid ip in TELNET operation.");
		}
		String port = task.getTargetPort();
		if (Assert.isEmptyString(port) == true) {
			throw new PlatformException("invalid port in TELNET operation.");
		}
		Pattern pattern = Pattern.compile("[0-9]*");
		if (!pattern.matcher(port).matches()) {
			throw new PlatformException("the port is not a number in TELNET operation.");
		}
		String shellCommand = task.getContent();
		if (Assert.isEmptyString(shellCommand) == true) {
			throw new PlatformException("invalid shellCommand in TELNET operation.");
		}
		String openCommand = metaMap.get("openCommand");
		if (Assert.isEmptyString(openCommand) == true) {
			throw new PlatformException("invalid openCommand in TELNET operation.");
		}
		String prompt = metaMap.get("prompt");
		if (Assert.isEmptyString(prompt) == true) {
			throw new PlatformException("invalid prompt in TELNET operation.");
		}
		String execPrompt = metaMap.get("execPrompt");
		if (Assert.isEmptyString(execPrompt) == true) {
			throw new PlatformException("invalid execPrompt in TELNET operation.");
		}
		String userPrompt = metaMap.get("userPrompt");
		if (Assert.isEmptyString(userPrompt) == true) {
			throw new PlatformException("invalid userPrompt in TELNET operation.");
		}
		String passwdPrompt = metaMap.get("passwdPrompt");
		if (Assert.isEmptyString(passwdPrompt) == true) {
			throw new PlatformException("invalid passwdPrompt in TELNET operation.");
		}
		String separPrompt = metaMap.get("separPrompt");
		if (Assert.isEmptyString(separPrompt) == true) {
			separPrompt = "^";
		}
		String separWrod = metaMap.get("separWrod");
		if (Assert.isEmptyString(separWrod) == true) {
			separWrod = "^";
		}
		try {
			String shellCommands[] = shellCommand.split("\\^");
			this.init(ip, Integer.parseInt(port), username, password, prompt, userPrompt, passwdPrompt);
			StringBuilder results = new StringBuilder();
			for (int i = 0; i < shellCommands.length; i++) {
				String res = this.sendCommand(shellCommands[i], openCommand, execPrompt, separPrompt, separWrod);
				if (res != null && (!res.equals(""))) {
					if (!res.equals(shellCommands[i]))
						results.append(res);
				}
			}
			// "telnet":'{'"mid": "{0}","t": "{1}","ip": "{2}","s":
			// "{3}","c":"{4}"'}'
			HashMap<String, String> messageInputAndRequestHeaders = this._monitoringWorkflow.getMessageInputAndRequestHeaders(this._operationID, shellCommand, results.toString());
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
			if (_telnetClient != null) {
				try {
					this.disconnect();
				} catch (Exception e) {
				}
				_telnetClient = null;
			}
		}
		theLogger.debug("finishOperation", task.getTaskObj().toString(), String.valueOf(System.currentTimeMillis() - currentTimePoint));
	}

	@Override
	public void stopIt(MonitoringTask task) throws PlatformException {
		// TODO Auto-generated method stub
		if (_telnetClient != null) {
			try {
				disconnect();
			} catch (Exception e) {
			}
		}
		//
		this._operationError._message = "stop the operation on stopIt";
		this.fireError(this._operationError);
	}
}
