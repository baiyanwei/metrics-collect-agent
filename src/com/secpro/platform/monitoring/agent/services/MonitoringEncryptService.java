package com.secpro.platform.monitoring.agent.services;

import it.sauronsoftware.base64.Base64;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.log.utils.PlatformLogger;

@ServiceInfo(description = "Monitoring encrypt service", configurationPath = "mca/services/MonitoringEncryptService/")
public class MonitoringEncryptService implements IService {
	private final String KEY_ALGORITHM="RSA";
	private final String SIGNATURE_ALGORITHM="MD5withRSA";
	private final String PUBLIC_KEY="RSAPublicKey";
	private final String PRIVATE_KEY="RSAPrivateKey";
	//最大加密明文大小
	private static final int MAX_ENCRYPT_BLOCK = 117;
	//最大解密密文大小
	private static final int MAX_DECRYPT_BLOCK = 128;
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
	 /**
		 * 初始化密钥
		 * @return
		 */
		public Map<String,Object> initKey(){
			Map<String,Object> keyMap=null;
			try {
				KeyPairGenerator keyPairGen=KeyPairGenerator.getInstance(KEY_ALGORITHM);
				keyPairGen.initialize(1024);
				KeyPair keyPair=keyPairGen.generateKeyPair();
				//公钥
				RSAPublicKey publicKey=(RSAPublicKey)keyPair.getPublic();
				//私钥
				RSAPrivateKey privateKey=(RSAPrivateKey)keyPair.getPrivate();
				keyMap=new HashMap<String,Object>(2);
				keyMap.put(PUBLIC_KEY, publicKey);
				keyMap.put(PRIVATE_KEY, privateKey);
				
				
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return keyMap;
			
		}
		/**
		 * 取得公钥
		 * @param keyMap
		 * @return
		 */
		public String getPublicKey(Map<String,Object> keyMap)
		{
			Key key=(Key) keyMap.get(PUBLIC_KEY);
			return encryptBASE64(key.getEncoded());
			
		}
		/**
		 * 取得私钥
		 * @param keyMap
		 * @return
		 */
		public String getPrivateKey(Map<String,Object> keyMap)
		{
			Key key=(Key) keyMap.get(PRIVATE_KEY);
			return encryptBASE64(key.getEncoded());
		}
		/**
		 * BASE64加密 
		 * @param bytes
		 * @return
		 */
		public String encryptBASE64(byte[] bytes) {   
			 return new String(Base64.encode(bytes)); 
		}  
		/**
		 * BASE64解密
		 * @param base64
		 * @return
		 */
		public static byte[] decryptBASE64(String base64) { 
			return Base64.decode(base64.getBytes());  
		} 
		/**
		 * 用公钥加密data
		 * @param data
		 * @param key
		 * @return
		 */
		public byte[] encryptByPublicKey(byte[] data,String publicKey){
			//对公钥解密
			byte[] keyBytes=decryptBASE64(publicKey);
			//取得公钥
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes); 
			KeyFactory keyFactory;
			byte[] encryptedData = null;
			try {
				keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
				Key publicK = keyFactory.generatePublic(x509KeySpec);
				//对数据加密
				Cipher cipher=Cipher.getInstance(keyFactory.getAlgorithm());
				cipher.init(Cipher.ENCRYPT_MODE,publicK);
				int inputL=data.length;
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int offset=0;
				byte[] cache;
				int i=0;
				//对数据进行分段加密
				while(inputL-offset>0)
				{
					if(inputL-offset>MAX_ENCRYPT_BLOCK){
						cache=cipher.doFinal(data, offset, MAX_ENCRYPT_BLOCK);
					}else{
						cache=cipher.doFinal(data, offset, inputL-offset);
					}
					out.write(cache, 0, cache.length);
					i++;
					offset=i*MAX_ENCRYPT_BLOCK;
				}
				encryptedData=out.toByteArray();
				out.close();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return encryptedData;
			
		}
		/**
		 * 用私钥解密
		 * @param encodeData
		 * @param privateKey
		 * @return
		 */
		public byte[] decryptByPrivateKey(byte[] encodeData,
				String privateKey) {
			//对密钥进行解密
			byte[] keyBytes=decryptBASE64(privateKey);
			//取得私钥
			PKCS8EncodedKeySpec pkcs8KeySpec=new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory keyFactory;
			byte[] decryptedData=null;
			try {
				keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
				 Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);  
				 Cipher cipher=Cipher.getInstance(keyFactory.getAlgorithm());
				 cipher.init(Cipher.DECRYPT_MODE, privateK);
				 int inputL = encodeData.length;
			        ByteArrayOutputStream out = new ByteArrayOutputStream();
			        int offSet = 0;
			        byte[] cache;
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
			        out.close();
			       } catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}   
			        return decryptedData;  
		
		}
		/**
		 * 用私钥加密data
		 * @param data
		 * @param key
		 * @return
		 * @throws Exception
		 */
		public byte[] encryptByPrivateKey(byte[] data, String PrivateKey) {
			 // 对密钥解密   
	        byte[] keyBytes = decryptBASE64(PrivateKey);   

	        // 取得私钥   
	        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);   
	        KeyFactory keyFactory;
			byte[] encryptedData = null;
			try {
	        keyFactory=	KeyFactory.getInstance(KEY_ALGORITHM);   
	        Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);   

	        // 对数据加密   
	        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());   
	        cipher.init(Cipher.ENCRYPT_MODE, privateK);   
	        int inputL = data.length;
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        int offSet = 0;
	        byte[] cache;
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
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return encryptedData;
		}
		/**
		 * 用公钥解密
		 * @param encodedData
		 * @param key
		 * @return
		 * @throws Exception
		 */
		public byte[] decryptByPublicKey(byte[] encodedData, String publicKey){
				byte[] keyBytes = decryptBASE64(publicKey);   
				// 取得公钥   
		        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);   
		        KeyFactory keyFactory;
		        byte[] decryptedData =null;
				try {
					keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

					Key publicK = keyFactory.generatePublic(x509KeySpec);   

		        // 对数据解密   
		        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());   
		        cipher.init(Cipher.DECRYPT_MODE, publicK);  
		        int inputL = encodedData.length;
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        int offSet = 0;
		        byte[] cache;
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
		        out.close();
		       } catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}   
		        return decryptedData;   
		}
		/**
		 * 用私钥产生数字签名
		 * @param encodedData
		 * @param privateKey
		 * @return
		 */
		public String sign(byte[] data, String privateKey)throws Exception {
			 // 解密由base64编码的私钥   
	        byte[] keyBytes = decryptBASE64(privateKey);  
	        // 构造PKCS8EncodedKeySpec对象   
	        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);   

	        // KEY_ALGORITHM 指定的加密算法   
	        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);   

	        // 取私钥匙对象   
	        PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);
	     // 用私钥对信息生成数字签名
	        Signature signature=Signature.getInstance(SIGNATURE_ALGORITHM);
	        signature.initSign(priKey);
	        signature.update(data);
			return encryptBASE64(signature.sign());
		}
		/**
		 * 用公钥和数字签名验证加密数据
		 * @param encodedData
		 * @param publicKey
		 * @param sign
		 * @return
		 */
		public boolean verify(byte[] data, String publicKey,
				String sign) throws Exception{
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
