package com.secpro.platform.monitoring.agent.bri.syslog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author sxf
 * this class provides the standard methods of syslog 
 * 
 */
public class SyslogStandard {
	//save all regexs of standard syslog
	public static Map<String,SyslogBean> rules=new HashMap<String,SyslogBean>();
	/**
	 * create the rule of syslog
	 * @param ip
	 * @param regexs
	 * @return
	 */
	public static boolean CreatRule(String ip,List<String> regexs){
		if(ip!=null&&!("".equals(ip))){
			SyslogBean sys=new SyslogBean();
			sys.setIPAdd(ip);
			sys.setRegexs(regexs);
			rules.put(ip, sys);
			return true;
		}
		
		return false;
		
	}
	/**
	 * change the rule of syslog
	 * @param ip
	 * @param regexs
	 * @return
	 */
	public static boolean ChangeRule(String ip,List<String> regexs){
		if(ip!=null&&!("".equals(ip))){
			SyslogBean sys=rules.get(ip);
			if(sys!=null){
			sys.setRegexs(regexs);
			return true;
		
			}
			else
			{
				sys=new SyslogBean();
				sys.setIPAdd(ip);
				sys.setRegexs(regexs);
				rules.put(ip, sys);
				return true;
			}
		}
		return false;
		
	}
	/**
	 * delete the rule of syslog,by the given ipAddress
	 * @param ip
	 * @return
	 */
	public static boolean deleteRule(String ip)
	{
		if(ip!=null&&!("".equals(ip))){
			SyslogBean sys=rules.get(ip);
			if(sys!=null){
			rules.remove(ip);
			return true;
		
			}
		}
		return false;
		
	}
	public static List<String> lookupRegexs(String ip){
		if(ip!=null&&!("".equals(ip))){
			SyslogBean sys=rules.get(ip);
			if(sys!=null){
			return sys.getRegexs();
		
			}
		}
		return null;
		
	}
	/**
	 * Based on regular expressions to format the given syslog
	 * @param ip
	 * @param syslog
	 * @return
	 */
	public static Map<String,String> matcher(String ip,String syslog){
		List<String> regexs=lookupRegexs(ip);
		Map<String,String> result=null;
		int flag=0;
		if(regexs!=null&&regexs.size()>0)
		{
			for(int i=0;i<regexs.size();i++)
			{
				result=matcherSyslog(syslog,regexs.get(i));
				if(result!=null&&result.size()>0)
				{
					flag=1;
					i=regexs.size();
				}
			}
		}
		if(flag==0)
		{
			System.out.println("all of regular expressions is not matcher the syslog,format failed");
		}
		return result;
		
	}
	private static Map<String,String> matcherSyslog(String syslog,String regex){
		Map<String,String> resultMap=null;
		if(syslog.trim().length()>0){
			List<String> keys=new ArrayList<String>();
			Pattern pattern=Pattern.compile("(<\\w+>)");
			Matcher mat=pattern.matcher(regex);
			int i=0;
			if(mat.find()){
				while(mat.find(i))
				{
					String key=mat.group(1);
					keys.add(key.substring(key.indexOf("<")+1,key.lastIndexOf(">")));
					i=mat.end();
				}
				String newRegex=mat.replaceAll("");
				resultMap= matcherSyslogC(syslog,newRegex,keys);
			}

		}
		return resultMap;
	}	
	private static Map<String,String> matcherSyslogC(String syslog,String regex,List<String> keys)
	{
		Map<String,String> resultMap=null;
		Pattern pattern=Pattern.compile(regex);
		Matcher mat=pattern.matcher(syslog);
		if(mat.find()){
			int count=mat.groupCount();
			if(keys.size()>=count){
				resultMap=new HashMap<String,String>();
				for(int i=0;i<count;i++)
				{
					resultMap.put(keys.get(i), mat.group(i+1));
				}
			}
		}
		return resultMap;
	
	}
}