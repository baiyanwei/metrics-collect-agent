package com.secpro.platform.monitoring.agent.operations.snmp;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.MessageProcessingModel;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.secpro.platform.log.utils.PlatformLogger;

/**
 * 
 * @author sxf  
 * MOD Sep 19, 2013
 *         This class provides the methods of snmp all versions include the
 *         snmpv1,v2c,v3
 */
public class SNMPCollectAdapter {
	private final static PlatformLogger theLogger = PlatformLogger.getLogger(SNMPCollectAdapter.class);
	private TransportMapping _transportMapping = null;
	private Snmp _snmp = null;
	private boolean _flag = false;
	private long _timeout=5000;
	private int _retries=2;

	/**
	 * the method is based on snmp version to call other methods
	 * 
	 * @param snmp
	 * @return
	 */
	public HashMap<String, String> snmpAllVer(SNMPReferentBean snmpBean) {
		String targetIP = snmpBean.getTargetIP();
		HashMap<String, String> resultMap = null;

		if (isboolIP(targetIP)) {
			int portDefault = 161;
			List<String> mibList = snmpBean.getMibList();
			int port = snmpBean.getPort();
			if (port != 0) {
				portDefault = port;
			}
			Address targetAddress = GenericAddress.parse("udp:" + targetIP
					+ "/" + portDefault);
			if (mibList != null && mibList.size() > 0) {
				int version = snmpBean.getVersion();

				if (version == 1) {
					String community = snmpBean.getCommunity();
					if (community != null) {
						resultMap = snmpV1(targetAddress, community, mibList);
					}
				} else if (version == 2) {
					String community = snmpBean.getCommunity();
					if (community != null) {
						resultMap = snmpV2(targetAddress, community, mibList);
					}
				} else if (version == 3) {
					String userName = snmpBean.getUserName();
					if (userName != null) {
						String auth = snmpBean.getAuth();
						String authPass = snmpBean.getAuthPass();
						String priv = snmpBean.getPriv();
						String privPass = snmpBean.getPrivPass();
						resultMap = snmpV3(targetAddress, mibList, userName,
								auth, authPass, priv, privPass);
					}
				} else {
					theLogger.warn("versionError", version);
				}

			} else {
				theLogger.warn("invalidOID", "null");
			}
		} else {
			theLogger.warn("invalidIp", targetIP);
		}

		return resultMap;

	}

	/**
	 * stop the SNMP listener
	 */
	public void stopListener() {
		// close it anyway.
		if (_snmp != null) {
			try {
				_snmp.close();
			} catch (IOException e) {
			}
		}
		// remember to release the resource when you don't need it again.
		if (_transportMapping != null) {
			try {
				_transportMapping.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * the method process snmp version3
	 * 
	 * @param targetAddress
	 * @param mibList
	 * @param userName
	 * @param auth
	 * @param authPass
	 * @param priv
	 * @param privPass
	 * @return
	 * @throws IOException
	 */
	private HashMap<String, String> snmpV3(Address targetAddress,
			List<String> mibList, String userName, String auth,
			String authPass, String priv, String privPass) {
		HashMap<String, String> resultMap = null;
		OID authID = AuthMD5.ID;
		OID privID = PrivDES.ID;
		int securityLevel = SecurityLevel.AUTH_PRIV;
		if (auth != null && !("".equals(auth))) {
			if (auth.toLowerCase().equals("sha")) {
				authID = AuthSHA.ID;
			}
			if (priv != null && !("".equals(priv))) {
				if (priv.toLowerCase().equals("aes128")) {
					privID = PrivAES128.ID;
				} else if (priv.toLowerCase().equals("aes192")) {
					privID = PrivAES192.ID;
				} else if (priv.toLowerCase().equals("aes256")) {
					privID = PrivAES256.ID;
				} else if (priv.toLowerCase().equals("aes")) {
					privID = PrivAES128.ID;
				}
			} else {
				securityLevel = SecurityLevel.AUTH_NOPRIV;
			}
		} else {
			securityLevel = SecurityLevel.NOAUTH_NOPRIV;
		}
		if (authPass == null || "".equals(authPass)) {
			authPass = "aaaaaaaa";
		}
		if (privPass == null || "".equals(privPass)) {
			privPass = "aaaaaaaa";
		}
		// System.out.println(securityLevel);
		try {
			_transportMapping = new DefaultUdpTransportMapping();
			_snmp = new Snmp(_transportMapping);
			MPv3 mpv3 = (MPv3) _snmp
					.getMessageProcessingModel(MessageProcessingModel.MPv3);
			USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(mpv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(usm);
			_transportMapping.listen();
			// add user to the USM
			_snmp.getUSM().addUser(
					new OctetString(userName),
					new UsmUser(new OctetString(userName), authID,
							new OctetString(authPass), privID, new OctetString(
									privPass)));
			// create the target
			UserTarget target = new UserTarget();
			target.setAddress(targetAddress);
			// set the retries
			target.setRetries(_retries);
			// set the timeout
			target.setTimeout(_timeout);
			target.setVersion(SnmpConstants.version3);
			target.setSecurityLevel(securityLevel);
			target.setSecurityName(new OctetString(userName));

			// create the PDU
			PDU pdu = new ScopedPDU();
			for (int i = 0; i < mibList.size(); i++) {
				pdu.add(new VariableBinding(new OID(mibList.get(i))));
			}
			pdu.setType(PDU.GET);
			ResponseEvent respEvt = _snmp.send(pdu, target);
			// ResponseEvent response = _snmp.send(pdu, target);
			if (respEvt != null && respEvt.getResponse() != null) {
				Vector<VariableBinding> recVBs = (Vector<VariableBinding>) respEvt.getResponse().getVariableBindings();
				if (recVBs != null) {
					if(recVBs.size()==mibList.size()){
						resultMap=new HashMap<String, String>();
						for (int i = 0; i < recVBs.size(); i++) {
							VariableBinding recVB = recVBs.elementAt(i);
							if (recVB != null && recVB.getVariable() != null
									&& recVB.getOid() != null) {
								String OIDKey = recVB.getOid().toString();
								String OIDValue = recVB.getVariable().toString();
								if(mibList.indexOf(OIDKey)==-1)
								{
									_flag=false;
									break;
								}
								if ("null".equalsIgnoreCase(OIDValue)
										|| "nosuchobject"
										.equalsIgnoreCase(OIDValue)
										|| "nosuchinstance"
										.equalsIgnoreCase(OIDValue)) {
									resultMap.put(recVB.getOid().toString(),
											"nosuchobject");
									_flag = true;
								} else if ("1.3.6.1.6.3.15.1.1.3.0".equals(OIDKey)
										|| "1.3.6.1.6.3.15.1.1.5.0".equals(OIDKey)||"1.3.6.1.6.3.15.1.1.1.0".equals(OIDKey)) {
									_flag = false;
								} else {
									resultMap.put(OIDKey, OIDValue);
									_flag = true;
								}

							}
						}
					}
				}
			}
			if (_flag == false) {
				resultMap=new HashMap<String, String>();
				for (int i = 0; i < mibList.size(); i++) {
					resultMap.put(mibList.get(i), "timeout");
				}
				theLogger.warn("SNMPTimeout", targetAddress.toString());
			}
		} catch (IOException e) {
			theLogger.exception(e);
		} finally {
			// close it anyway.
			if (_snmp != null) {
				try {
					_snmp.close();
				} catch (IOException e) {
				}
			}
			// remember to release the resource when you don't need it again.
			if (_transportMapping != null) {
				try {
					_transportMapping.close();
				} catch (IOException e) {
				}
			}
		}

		return resultMap;

	}

	/**
	 * the method process snmp version2c
	 * 
	 * @param targetAddress
	 * @param community
	 * @param mibList
	 * @return
	 * @throws IOException
	 */
	private HashMap<String, String> snmpV2(Address targetAddress,
			String community, List<String> mibList) {
		HashMap<String, String> resultMap = new HashMap<String, String>();
		try {
			_transportMapping = new DefaultUdpTransportMapping();
			_snmp = new Snmp(_transportMapping);
			_transportMapping.listen();
			CommunityTarget target = new CommunityTarget();
			target.setCommunity(new OctetString(community));
			target.setAddress(targetAddress);
			// set the retries
			target.setRetries(_retries);
			// set the timeout
			target.setTimeout(_timeout);
			target.setVersion(SnmpConstants.version2c);
			PDU pdu = new PDU();
			for (int i = 0; i < mibList.size(); i++) {
				pdu.add(new VariableBinding(new OID(mibList.get(i))));
			}
			pdu.setType(PDU.GET);
			ResponseEvent respEvt = _snmp.send(pdu, target);
			if (respEvt != null && respEvt.getResponse() != null) {
				Vector<VariableBinding> recVBs = (Vector<VariableBinding>) respEvt.getResponse().getVariableBindings();
				if (recVBs != null) {
					for (int i = 0; i < recVBs.size(); i++) {
						VariableBinding recVB = recVBs.elementAt(i);
						if (recVB != null && recVB.getVariable() != null
								&& recVB.getOid() != null) {
							String OIDKey = recVB.getOid().toString();
							String OIDValue = recVB.getVariable().toString();
							if ("null".equalsIgnoreCase(OIDValue)
									|| "nosuchobject"
											.equalsIgnoreCase(OIDValue)
									|| "nosuchinstance"
											.equalsIgnoreCase(OIDValue)) {
								resultMap.put(OIDKey, "nosuchobject");
							} else {
								resultMap.put(OIDKey, OIDValue);
							}
							_flag = true;
						}
					}
				}
			}
			if (_flag == false) {
				for (int i = 0; i < mibList.size(); i++) {
					resultMap.put(mibList.get(i), "timeout");
				}
				theLogger.warn("SNMPTimeout", targetAddress.toString());
			}
		} catch (IOException e) {
			theLogger.exception(e);
		} finally {
			// close it anyway.
			if (_snmp != null) {
				try {
					_snmp.close();
				} catch (IOException e) {
				}
			}
			// remember to release the resource when you don't need it again.
			if (_transportMapping != null) {
				try {
					_transportMapping.close();
				} catch (IOException e) {
				}
			}
		}
		return resultMap;
	}

	/**
	 * the method process snmp version1
	 * 
	 * @param targetAddress
	 * @param community
	 * @param mibList
	 * @return
	 * @throws IOException
	 */
	private HashMap<String, String> snmpV1(Address targetAddress,
			String community, List<String> mibList) {
		HashMap<String, String> resultMap = new HashMap<String, String>();
		try {
			_transportMapping = new DefaultUdpTransportMapping();
			_snmp = new Snmp(_transportMapping);
			_transportMapping.listen();
			CommunityTarget target = new CommunityTarget();
			target.setCommunity(new OctetString(community));
			target.setAddress(targetAddress);
			// set the retries
			target.setRetries(_retries);
			// set the timeout
			target.setTimeout(_timeout);
			target.setVersion(SnmpConstants.version1);
			for (int i = 0; i < mibList.size(); i++) {
				PDU pdu = new PDU();
				pdu.add(new VariableBinding(new OID(mibList.get(i))));
				pdu.setType(PDU.GET);
				ResponseEvent respEvt = _snmp.send(pdu, target);
				if (respEvt != null && respEvt.getResponse() != null) {
					Vector<VariableBinding> recVBs = (Vector<VariableBinding>) respEvt.getResponse().getVariableBindings();
					if (recVBs != null) {
						VariableBinding recVB = recVBs.elementAt(0);
						if (recVB != null && recVB.getVariable() != null
								&& recVB.getOid() != null) {
							String OIDKey = recVB.getOid().toString();
							String OIDValue = recVB.getVariable().toString();
							if ("null".equalsIgnoreCase(OIDValue)
									|| "nosuchobject"
											.equalsIgnoreCase(OIDValue)
									|| "nosuchinstance"
											.equalsIgnoreCase(OIDValue)) {
								resultMap.put(OIDKey, "nosuchobject");
							} else {
								resultMap.put(OIDKey, OIDValue);
							}
							_flag = true;
						}

					}
				}
				if (_flag == false) {
					resultMap.put(mibList.get(i), "timeout");
					theLogger.warn("SNMPTimeout", targetAddress.toString());
				}
				_flag = false;
			}
		} catch (IOException e) {
			theLogger.exception(e);
		} finally {
			// close it anyway.
			if (_snmp != null) {
				try {
					_snmp.close();
				} catch (IOException e) {
				}
			}
			// remember to release the resource when you don't need it again.
			if (_transportMapping != null) {
				try {
					_transportMapping.close();
				} catch (IOException e) {
				}
			}
		}
		return resultMap;
	}

	/**
	 * march the ip address
	 * 
	 * @param ipAddress
	 * @return
	 */
	private boolean isboolIP(String ipAddress) {
		String ip = "(2[5][0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})";
		Pattern pattern = Pattern.compile(ip);
		Matcher matcher = pattern.matcher(ipAddress);
		return matcher.matches();
	}

}
