package com.secpro.platform.monitoring.agent.services;

import it.sauronsoftware.base64.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;

import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.core.utils.Assert;
import com.secpro.platform.log.utils.PlatformLogger;

@ServiceInfo(description = "Monitoring encrypt service", configurationPath = "mca/services/MonitoringEncryptService/")
public class MonitoringEncryptService implements IService {
	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(MonitoringEncryptService.class);
	private final String KEY_ALGORITHM = "RSA";
	private final String SIGNATURE_ALGORITHM = "MD5withRSA";
	private final String ENCODE_STR_KEY = "zxcvbnm,./asdfghjkl;'qwertyuiop[]\\1234567890-=` ZXCVBNM<>?:LKJHGFDSAQWERTYUIOP{}|+_)(*&^%$#@!~";
	// private final String PUBLIC_KEY = "RSAPublicKey";
	// private final String PRIVATE_KEY = "RSAPrivateKey";
	private final long KEY_PASS_LIMIT = 24 * 60 * 60 * 1000L;
	// 最大加密明文大小
	private static final int MAX_ENCRYPT_BLOCK = 117;
	// 最大解密密文大小
	private static final int MAX_DECRYPT_BLOCK = 128;

	private HashMap<String, String> _encryptKeyMap = new HashMap<String, String>();
	private ArrayList<Object[]> _encryptKeyPool = new ArrayList<Object[]>();
	private Thread _encryptKeyThread = null;
	private boolean __encryptKeyThreadRunnable = true;

	@Override
	public void start() throws Exception {
		//
		theLogger.info("startUp");
		//
		synchronized (_encryptKeyPool) {
			this._encryptKeyMap.clear();
			this._encryptKeyPool.clear();
		}
		//
		_encryptKeyThread = new Thread("MonitoringEncryptService._encryptKeyThread") {
			public void run() {
				while (__encryptKeyThreadRunnable) {
					try {
						sleep(KEY_PASS_LIMIT);
						clearPassEncryptKey();
					} catch (Exception e) {
						theLogger.exception(e);
					}
				}

			}
		};
		_encryptKeyThread.start();
		
		//
		buildEncryptKey();
		//
	}

	@SuppressWarnings("deprecation")
	@Override
	public void stop() throws Exception {
		// clear key cache
		synchronized (_encryptKeyPool) {
			this._encryptKeyMap.clear();
			this._encryptKeyPool.clear();
		}
		// stop the Thread.
		__encryptKeyThreadRunnable = false;
		try {
			_encryptKeyThread.stop();
		} catch (Exception e) {
			theLogger.exception(e);
		}
	}

	/**
	 * build a new encrypt key into poll and according to KEY_PASS_LIMIT set
	 * it's limiting time
	 */
	private void buildEncryptKey() {

		// make a encrypt key
		Key[] encryptKeys = CreateEncryptKey();
		if (encryptKeys == null || encryptKeys.length != 2) {
			theLogger.warn("createEncryptFail");
			return;
		}
		// TODO Is key to string right or not?
		String newPublicKey = gegKeyString(encryptKeys[0]);
		String newPrivateKey = gegKeyString(encryptKeys[1]);
		// package the key object into pool
		Object[] keyObjs = new Object[5];
		keyObjs[0] = newPublicKey;
		keyObjs[1] = newPrivateKey;
		keyObjs[2] = System.currentTimeMillis();
		keyObjs[3] = ((Long) keyObjs[2]).longValue() + KEY_PASS_LIMIT;
		keyObjs[4] = 0;
		// put new key object into pool.
		synchronized (this._encryptKeyPool) {
			this._encryptKeyPool.add(0, keyObjs);
			this._encryptKeyMap.put(newPublicKey, newPublicKey);
		}
		//
		theLogger.debug("buildEncryptSN", newPublicKey);
	}

	/**
	 * clear the all passing encrypt key from pool and map.
	 */
	private void clearPassEncryptKey() {
		synchronized (this._encryptKeyPool) {
			long currentTimePoint = System.currentTimeMillis();
			int size = this._encryptKeyPool.size();
			for (int i = size - 1; i >= 0; i--) {
				Object[] checkObj = this._encryptKeyPool.get(i);
				// if the limit time in pool is over the current time ,so we
				// need to remove the key from pool and mapping.
				if (currentTimePoint - ((Long) checkObj[3]).longValue() >= 0) {
					this._encryptKeyPool.remove(i);
					this._encryptKeyMap.remove((String) checkObj[0]);
				}
			}
			if (this._encryptKeyPool.size() == 0) {
				// if the pool is empty after the clear action.
				// We crate new one and put it into pool.
				buildEncryptKey();
			}
		}
	}

	/**
	 * 将明文加密成密文，将密文解密成明文
	 * 
	 * @param strTask
	 * @return
	 */
	public String encode(String strTask) {
		if (Assert.isEmptyString(strTask)) {
			return "";
		}
		//String strEncode = "";
		String des = "";
		// String strKey =
		// "zxcvbnm,./asdfghjkl;'qwertyuiop[]\\1234567890-=` ZXCVBNM<>?:LKJHGFDSAQWERTYUIOP{}|+_)(*&^%$#@!~";
		//
		for (; strTask.length() < 8; strTask = strTask + '\001')
			;
		//
		for (int n = 0; n <= strTask.length() - 1; n++) {
			char code;
			char mid;
			do {
				for (code = (char) (int) Math.rint(Math.random() * 100D); code > 0 && ((code ^ strTask.charAt(n)) < 0 || (code ^ strTask.charAt(n)) > 90); code--)
					;
				mid = '\0';
				int flag = code ^ strTask.charAt(n);
				if (flag > 93)
					mid = '\0';
				else
					mid = ENCODE_STR_KEY.charAt(flag);
			} while (!((code > '#') & (code < 'z') & (code != '|') & (code != '\'') & (code != ',') & (mid != '|') & (mid != '\'') & (mid != ',')));
			char temp = '\0';
			temp = ENCODE_STR_KEY.charAt(code ^ strTask.charAt(n));
			des = des + code + temp;
		}
		//strEncode = des;
		return des;
	}

	/**
	 * 将密文解密成明文
	 * 
	 * @param varCode
	 *            待解密密文
	 * @return 返回解密后原文
	 */
	public String decode(String varCode) {
		if (Assert.isEmptyString(varCode)) {
			return "";
		}
		String des = "";
		// String strKey = new String();
		// strKey =
		// "zxcvbnm,./asdfghjkl;'qwertyuiop[]\\1234567890-=` ZXCVBNM<>?:LKJHGFDSAQWERTYUIOP{}|+_)(*&^%$#@!~";
		if (varCode.length() % 2 == 1)
			varCode = varCode + "?";
		int n;
		for (n = 0; n <= varCode.length() / 2 - 1; n++) {
			char b = varCode.charAt(n * 2);
			int a = ENCODE_STR_KEY.indexOf(varCode.charAt(n * 2 + 1));
			des = des + (char) (b ^ a);
		}
		n = des.indexOf(1);
		if (n > 0) {
			return des.substring(0, n);
		} else {
			return des;
		}
	}

	/**
	 * 初始化密钥
	 * 
	 * @return
	 */
	private Key[] CreateEncryptKey() {
		//
		try {
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
			keyPairGen.initialize(1024);
			KeyPair keyPair = keyPairGen.generateKeyPair();
			// {公钥,私钥}
			return new Key[] { keyPair.getPublic(), keyPair.getPrivate() };
		} catch (NoSuchAlgorithmException e) {
			theLogger.exception(e);
		}
		return null;

	}

	/**
	 * 取得密钥字符串
	 * 
	 * @param Key
	 * @return
	 */
	private String gegKeyString(Key key) {
		return encryptBASE64(key.getEncoded());

	}

	/**
	 * BASE64加密
	 * 
	 * @param bytes
	 * @return
	 */
	private String encryptBASE64(byte[] bytes) {
		return new String(Base64.encode(bytes));
	}

	/**
	 * BASE64解密
	 * 
	 * @param base64
	 * @return
	 */
	private byte[] decryptBASE64(String base64) {
		return Base64.decode(base64.getBytes());
	}

	/**
	 * 用公钥加密data
	 * 
	 * @param data
	 * @param key
	 * @return
	 */
	public byte[] encryptByPublicKey(byte[] data, String publicKey) {
		if (Assert.isNull(data) == true || Assert.isEmptyString(publicKey) == true) {
			return null;
		}
		// 对公钥解密
		byte[] keyBytes = decryptBASE64(publicKey);
		// 取得公钥
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = null;
		byte[] encryptedData = null;
		ByteArrayOutputStream out = null;
		try {
			keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			Key publicK = keyFactory.generatePublic(x509KeySpec);
			// 对数据加密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, publicK);
			int inputL = data.length;
			out = new ByteArrayOutputStream();
			int offset = 0;
			byte[] cache = null;
			int i = 0;
			// 对数据进行分段加密
			while (inputL - offset > 0) {
				if (inputL - offset > MAX_ENCRYPT_BLOCK) {
					cache = cipher.doFinal(data, offset, MAX_ENCRYPT_BLOCK);
				} else {
					cache = cipher.doFinal(data, offset, inputL - offset);
				}
				out.write(cache, 0, cache.length);
				i++;
				offset = i * MAX_ENCRYPT_BLOCK;
			}
			encryptedData = out.toByteArray();
		} catch (Exception e) {
			theLogger.exception(e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
		return encryptedData;
	}

	/**
	 * 用私钥解密
	 * 
	 * @param encodeData
	 * @param privateKey
	 * @return
	 */
	public byte[] decryptByPrivateKey(byte[] encodeData, String privateKey) {
		if (Assert.isNull(encodeData) == true || Assert.isEmptyString(privateKey) == true) {
			return null;
		}
		// 对密钥进行解密
		byte[] keyBytes = decryptBASE64(privateKey);
		// 取得私钥
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = null;
		byte[] decryptedData = null;
		ByteArrayOutputStream out = null;
		try {
			keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, privateK);
			int inputL = encodeData.length;
			out = new ByteArrayOutputStream();
			int offSet = 0;
			byte[] cache = null;
			int i = 0;
			// 对数据分段解密
			while (inputL - offSet > 0) {
				if (inputL - offSet > MAX_DECRYPT_BLOCK) {
					cache = cipher.doFinal(encodeData, offSet, MAX_DECRYPT_BLOCK);
				} else {
					cache = cipher.doFinal(encodeData, offSet, inputL - offSet);
				}
				out.write(cache, 0, cache.length);
				i++;
				offSet = i * MAX_DECRYPT_BLOCK;
			}
			decryptedData = out.toByteArray();
			//
		} catch (Exception e) {
			theLogger.exception(e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
		return decryptedData;

	}

	/**
	 * 用私钥加密data
	 * 
	 * @param data
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public byte[] encryptByPrivateKey(byte[] data, String PrivateKey) {
		if (Assert.isNull(data) == true || Assert.isEmptyString(PrivateKey) == true) {
			return null;
		}
		// 对密钥解密
		byte[] keyBytes = decryptBASE64(PrivateKey);

		// 取得私钥
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = null;
		byte[] encryptedData = null;
		ByteArrayOutputStream out = null;
		try {
			keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);

			// 对数据加密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, privateK);
			int inputL = data.length;
			out = new ByteArrayOutputStream();
			int offSet = 0;
			byte[] cache = null;
			int i = 0;
			while (inputL - offSet > 0) {
				if (inputL - offSet > MAX_ENCRYPT_BLOCK) {
					cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
				} else {
					cache = cipher.doFinal(data, offSet, inputL - offSet);
				}
				out.write(cache, 0, cache.length);
				i++;
				offSet = i * MAX_ENCRYPT_BLOCK;
			}
			encryptedData = out.toByteArray();
		} catch (Exception e) {
			theLogger.exception(e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
		return encryptedData;
	}

	/**
	 * 用公钥解密
	 * 
	 * @param encodedData
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public byte[] decryptByPublicKey(byte[] encodedData, String publicKey) {
		if (Assert.isNull(encodedData) == true || Assert.isEmptyString(publicKey) == true) {
			return null;
		}
		byte[] keyBytes = decryptBASE64(publicKey);
		// 取得公钥
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = null;
		byte[] decryptedData = null;
		ByteArrayOutputStream out = null;
		try {
			keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

			Key publicK = keyFactory.generatePublic(x509KeySpec);

			// 对数据解密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, publicK);
			int inputL = encodedData.length;
			out = new ByteArrayOutputStream();
			int offSet = 0;
			byte[] cache = null;
			int i = 0;
			// 对数据分段解密
			while (inputL - offSet > 0) {
				if (inputL - offSet > MAX_DECRYPT_BLOCK) {
					cache = cipher.doFinal(encodedData, offSet, MAX_DECRYPT_BLOCK);
				} else {
					cache = cipher.doFinal(encodedData, offSet, inputL - offSet);
				}
				out.write(cache, 0, cache.length);
				i++;
				offSet = i * MAX_DECRYPT_BLOCK;
			}
			decryptedData = out.toByteArray();
		} catch (Exception e) {
			theLogger.exception(e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}
		return decryptedData;
	}

	/**
	 * 用私钥产生数字签名
	 * 
	 * @param encodedData
	 * @param privateKey
	 * @return
	 */
	public String sign(byte[] data, String privateKey) throws Exception {
		if (Assert.isNull(data) == true || Assert.isEmptyString(privateKey) == true) {
			return null;
		}
		// 解密由base64编码的私钥
		byte[] keyBytes = decryptBASE64(privateKey);
		// 构造PKCS8EncodedKeySpec对象
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);

		// KEY_ALGORITHM 指定的加密算法
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

		// 取私钥匙对象
		PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);
		// 用私钥对信息生成数字签名
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initSign(priKey);
		signature.update(data);
		return encryptBASE64(signature.sign());
	}

	/**
	 * 用公钥和数字签名验证加密数据
	 * 
	 * @param encodedData
	 * @param publicKey
	 * @param sign
	 * @return
	 */
	public boolean verify(byte[] data, String publicKey, String sign) throws Exception {
		if (Assert.isNull(data) == true || Assert.isEmptyString(sign) == true) {
			return false;
		}
		// 解密由base64编码的公钥
		byte[] keyBytes = decryptBASE64(publicKey);

		// 构造X509EncodedKeySpec对象
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

		// KEY_ALGORITHM 指定的加密算法
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

		// 取公钥匙对象
		PublicKey pubKey = keyFactory.generatePublic(keySpec);

		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initVerify(pubKey);
		signature.update(data);

		// 验证签名是否正常
		return signature.verify(decryptBASE64(sign));
	}
}
