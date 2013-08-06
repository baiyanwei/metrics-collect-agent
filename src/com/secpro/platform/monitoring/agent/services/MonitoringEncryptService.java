package com.secpro.platform.monitoring.agent.services;

import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.log.utils.PlatformLogger;

@ServiceInfo(description = "Monitoring encrypt service", configurationPath = "mca/services/MonitoringEncryptService/")
public class MonitoringEncryptService implements IService {
	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(MonitoringService.class);
	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub
		theLogger.info("startUp");
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * 将明文加密成密文，将密文解密成明文
	 * @param strTask
	 * @return
	 */
	public String encode(String strTask) {
		String strEncode = new String("");
		String des = new String();
		String strKey = new String();
		if ((strTask == null) | (strTask.length() == 0)) {
			strEncode = "";
			return strEncode;
		}
		strKey = "zxcvbnm,./asdfghjkl;'qwertyuiop[]\\1234567890-=` ZXCVBNM<>?:LKJHGFDSAQWERTYUIOP{}|+_)(*&^%$#@!~";
		for (; strTask.length() < 8; strTask = strTask + '\001')
			;
		des = "";
		for (int n = 0; n <= strTask.length() - 1; n++) {
			char code;
			char mid;
			do {
				for (code = (char) (int) Math.rint(Math.random() * 100D); code > 0
						&& ((code ^ strTask.charAt(n)) < 0 || (code ^ strTask
								.charAt(n)) > 90); code--)
					;
				mid = '\0';
				int flag = code ^ strTask.charAt(n);
				if (flag > 93)
					mid = '\0';
				else
					mid = strKey.charAt(flag);
			} while (!((code > '#') & (code < 'z') & (code != '|')
					& (code != '\'') & (code != ',') & (mid != '|')
					& (mid != '\'') & (mid != ',')));
			char temp = '\0';
			temp = strKey.charAt(code ^ strTask.charAt(n));
			des = des + code + temp;
		}
		strEncode = des;
		return strEncode;
	}

	/**
	 * 将密文解密成明文
	 * 
	 * @param varCode
	 *            待解密密文
	 * @return 返回解密后原文
	 */
	public String decode(String varCode) {
		String des = new String();
		String strKey = new String();
		if (varCode == null || varCode.length() == 0)
			return "";
		strKey = "zxcvbnm,./asdfghjkl;'qwertyuiop[]\\1234567890-=` ZXCVBNM<>?:LKJHGFDSAQWERTYUIOP{}|+_)(*&^%$#@!~";
		if (varCode.length() % 2 == 1)
			varCode = varCode + "?";
		des = "";
		int n;
		for (n = 0; n <= varCode.length() / 2 - 1; n++) {
			char b = varCode.charAt(n * 2);
			int a = strKey.indexOf(varCode.charAt(n * 2 + 1));
			des = des + (char) (b ^ a);
		}
		n = des.indexOf(1);
		if (n > 0){
			return des.substring(0, n);
		}else{
			return des;
		}
	}
}
