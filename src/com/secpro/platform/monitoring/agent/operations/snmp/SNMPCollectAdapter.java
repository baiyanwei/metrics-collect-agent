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
/**
 * 
 * @author sxf
 * This class provides the methods of snmp all versions 
 * include the snmpv1,v2c,v3
 */
public class SNMPCollectAdapter {
	/**
	 * the method is based on snmp version to call other methods
	 * @param snmp
	 * @return
	 */
	public static HashMap<String,String> snmpAllVer(SNMPReferentBean snmp){
		String targetIP=snmp.getTargetIP();
		HashMap<String,String> resultMap=null;
		
		if(isboolIP(targetIP))
		{
			int portDefault=161;
			List<String> mibList=snmp.getMibList();
			int port=snmp.getPort();
			if(port!=0){
				portDefault=port;
			}
			Address targetAddress=GenericAddress.parse("udp:"+targetIP+"/"+portDefault);
			if(mibList!=null&&mibList.size()>0){
				int version=snmp.getVersion();
				try{
					if(version==1){
						String community=snmp.getCommunity();
						if(community!=null){
							resultMap=snmpV1(targetAddress,community,mibList);
						}
					}
					else if(version==2)
					{
						String community=snmp.getCommunity();
						if(community!=null){
							resultMap=snmpV2(targetAddress,community,mibList);
						}
					}
					else if(version==3)
					{
						String userName=snmp.getUserName();
						if(userName!=null){
							String auth=snmp.getAuth();
							String authPass=snmp.getAuthPass();
							String priv=snmp.getPriv();
							String privPass=snmp.getPrivPass();
							resultMap=snmpV3(targetAddress,mibList,userName,auth,authPass,priv,privPass);
						}
					}
					else{
						System.out.println("snmp version error,without this version");
					}
				}catch(Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}else{
				System.out.println("mib OID is null");
			}
		}
		else{
			System.out.println("IP address is error");
		}

		return resultMap;
		
	}


	/**
	 * the method process snmp version3
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
	private static HashMap<String, String> snmpV3(Address targetAddress, List<String> mibList,
			String userName, String auth, String authPass, String priv,
			String privPass) throws IOException {
		HashMap<String,String> resultMap=new HashMap<String,String>();
		OID authID=AuthMD5.ID;
		OID privID=PrivDES.ID;
		int securityLevel=SecurityLevel.AUTH_PRIV;
		if(auth!=null&&!("".equals(auth)))
		{
			if(auth.toLowerCase().equals("sha")){
				authID=AuthSHA.ID;
			}
			if(priv!=null&&!("".equals(priv))){
				if(priv.toLowerCase().equals("aes128")){
					privID=PrivAES128.ID;
				}else if(priv.toLowerCase().equals("aes192")){
					privID=PrivAES192.ID;
				}else if(priv.toLowerCase().equals("aes256")){
					privID=PrivAES256.ID;
				}else if(priv.toLowerCase().equals("aes")){
					privID=PrivAES128.ID;
				}
			}else
			{
				securityLevel=SecurityLevel.AUTH_NOPRIV;
			}
		}
		else{
			securityLevel=SecurityLevel.NOAUTH_NOPRIV;
		}
		if(authPass==null||"".equals(authPass))
		{
			authPass="aaaaaaaa";
		}
		if(privPass==null||"".equals(privPass))
		{
			privPass="aaaaaaaa";
		}
		//System.out.println(securityLevel);
		TransportMapping transport = new DefaultUdpTransportMapping();
		Snmp snmp = new Snmp(transport);
		MPv3 mpv3 =
			(MPv3)snmp.getMessageProcessingModel(MessageProcessingModel.MPv3);
		USM usm = new USM(SecurityProtocols.getInstance(),
				new OctetString(mpv3.createLocalEngineID()), 0);
		SecurityModels.getInstance().addSecurityModel(usm);
		transport.listen();
		// add user to the USM
		try{
			snmp.getUSM().addUser(new OctetString(userName),
					new UsmUser(new OctetString(userName),
							authID,
							new OctetString(authPass),
							privID,
							new OctetString(privPass)));
			// create the target
			UserTarget target = new UserTarget();
			target.setAddress(targetAddress);
			//set the retries
			target.setRetries(2);
			//set the timeout
			target.setTimeout(1000);
			target.setVersion(SnmpConstants.version3);
			target.setSecurityLevel(securityLevel);
			target.setSecurityName(new OctetString(userName));

			// create the PDU
			PDU pdu = new ScopedPDU();
			for(int i=0;i<mibList.size();i++)
			{
				pdu.add(new VariableBinding(new OID(mibList.get(i))));
			}
			pdu.setType(PDU.GET);
			ResponseEvent respEvt=snmp.send(pdu, target);
			ResponseEvent response = snmp.send(pdu, target);
			if(respEvt !=null&& respEvt.getResponse()!=null){
				Vector<VariableBinding> recVBs=(Vector<VariableBinding>) respEvt.getResponse().getVariableBindings();
				for(int i=0;i<recVBs.size();i++){
					VariableBinding recVB=recVBs.elementAt(i);
					if("nosuchinstance".equals(recVB.getVariable().toString().toLowerCase())){
						resultMap.put(recVB.getOid().toString(),null);
					}else{
						resultMap.put(recVB.getOid().toString(), recVB.getVariable().toString());
					}
				}
			}
			else{
				System.out.println("snmp request is timeout");
			}
		}catch(IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		finally {
			snmp.close();
		}

		return resultMap;
		
	}

	/**
	 * the method process snmp version2c
	 * @param targetAddress
	 * @param community
	 * @param mibList
	 * @return
	 * @throws IOException
	 */
	private static HashMap<String, String> snmpV2(Address targetAddress, String community,
			List<String> mibList) throws IOException {
		HashMap<String,String> resultMap=new HashMap<String,String>();
		TransportMapping transport=new DefaultUdpTransportMapping();
		Snmp snmp=new Snmp(transport);
		transport.listen();
		CommunityTarget target=new CommunityTarget();
		target.setCommunity(new OctetString(community));
		target.setAddress(targetAddress);
		//set the retries
		target.setRetries(2);
		//set the timeout
		target.setTimeout(1000);
		target.setVersion(SnmpConstants.version2c);
		PDU pdu = new PDU();
		for(int i=0;i<mibList.size();i++)
		{
			pdu.add(new VariableBinding(new OID(mibList.get(i))));
		}
		pdu.setType(PDU.GET);
		ResponseEvent respEvt=snmp.send(pdu, target);
		if(respEvt !=null&& respEvt.getResponse()!=null){

			Vector<VariableBinding> recVBs=(Vector<VariableBinding>) respEvt.getResponse().getVariableBindings();
			for(int i=0;i<recVBs.size();i++){
				VariableBinding recVB=recVBs.elementAt(i);
				if("nosuchinstance".equals(recVB.getVariable().toString().toLowerCase())){
					resultMap.put(recVB.getOid().toString(),null);
				}else{
					resultMap.put(recVB.getOid().toString(), recVB.getVariable().toString());
				}
			}
		}
		else{
			System.out.println("snmp request is timeout");
		}

		snmp.close();
		return resultMap;
	}

	/**
	 * the method process snmp version1
	 * @param targetAddress
	 * @param community
	 * @param mibList
	 * @return
	 * @throws IOException
	 */
	private static HashMap<String, String> snmpV1(Address targetAddress, String community,
			List<String> mibList) throws IOException {
		HashMap<String,String> resultMap=new HashMap<String,String>();
		TransportMapping transport=new DefaultUdpTransportMapping();
		Snmp snmp=new Snmp(transport);
		transport.listen();
		CommunityTarget target=new CommunityTarget();
		target.setCommunity(new OctetString(community));
		target.setAddress(targetAddress);
		//set the retires
		target.setRetries(2);
		//set the timeout
		target.setTimeout(1000);
		target.setVersion(SnmpConstants.version1);
		for(int i=0;i<mibList.size();i++)
		{
			PDU pdu = new PDU();
			pdu.add(new VariableBinding(new OID(mibList.get(i)))); 
			pdu.setType(PDU.GET);
			ResponseEvent respEvt=snmp.send(pdu, target);
			if(respEvt !=null&& respEvt.getResponse()!=null){
				Vector<VariableBinding> recVBs=(Vector<VariableBinding>) respEvt.getResponse().getVariableBindings();
				VariableBinding recVB=recVBs.elementAt(0);
				if("null".equals(recVB.getVariable().toString().toLowerCase())){
					resultMap.put(recVB.getOid().toString(),null);
				}else{
					resultMap.put(recVB.getOid().toString(), recVB.getVariable().toString());
				}
			}
			else{
				System.out.println("snmp request is timeout");
			}
		}
		snmp.close();
		return resultMap;
	}
	
	/**
	 * march the ip address
	 * @param ipAddress
	 * @return
	 */
	public static boolean isboolIP(String ipAddress){ 
		String ip="(2[5][0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})"; 
		Pattern pattern = Pattern.compile(ip); 
		Matcher matcher = pattern.matcher(ipAddress); 
		return matcher.matches(); 
	} 

}

