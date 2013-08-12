package com.secpro.platform.monitoring.agent.bri;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

import org.json.JSONObject;

import com.secpro.platform.core.services.IService;
import com.secpro.platform.core.services.ServiceInfo;
import com.secpro.platform.log.utils.PlatformLogger;
import com.secpro.platform.monitoring.agent.storages.file.local.SyslogStore;

/**
 * @author baiyanwei Jul 21, 2013
 * 
 *         SysLog listenter
 * 
 */
@ServiceInfo(description = "metric upload service, upload the metric to data process server.", configurationPath = "mca/services/SysLogBeaconInterface/")
public class SysLogBeaconInterface implements IService {
	//
	// Logging Object
	//
	private static PlatformLogger theLogger = PlatformLogger.getLogger(SysLogBeaconInterface.class);
	@XmlElement(name = "sysLogPath", defaultValue = "data/mca/syslog/")
	public String _sysLogPath;
	@XmlElement(name = "sysLogMax", type = Long.class, defaultValue = "50")
	public long _sysLogMax;
	@XmlElement(name = "sysLogListenerPort", type = Integer.class, defaultValue = "514")
	public int _sysLogListenerPort = 514;
	@XmlElement(name = "sysLogName", defaultValue = "syslog")
	public String _sysLogName;
	private SimpleDateFormat timedf = new SimpleDateFormat("yyyyMMddHHmmss");
	private SyslogStore _sysLogStore = null;

	@Override
	public void start() throws Exception {
		// TODO Auto-generated method stub
		if (_sysLogMax == 0) {
			_sysLogMax = 50;
		}
		//
		_sysLogStore = new SyslogStore(this);
		//
		if (_sysLogListenerPort < 0) {
			_sysLogListenerPort = 514;
		}
		new Thread(){
			public void run(){
				launchingSyslogReceive();
				//
				try {
					sleep(10000L);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				while(true){
					try{
						String ip = new String("10.1.1.1");
						String cdate = timedf.format(new Date());
						String msg = "firewall_ip:" + ip + "," + "reseiveDate:" + cdate + ",msg=" + "test";
						System.out.println(msg);
						JSONObject syslog = new JSONObject();
						syslog.put("hostIP", ip);
						syslog.put("cdate", cdate);
						syslog.put("msg", "test");
						// add the syslog to queue
						System.out.println(">>>>>>>>>add syslog");
						_sysLogStore.addSyslog(syslog);
						sleep(1000l);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}.start();
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		new Thread(){
			public void run(){
				//
				try {
					sleep(10000L);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				while(true){
					try{
						String ip = new String("10.1.1.1");
						String cdate = timedf.format(new Date());
						String msg = "firewall_ip:" + ip + "," + "reseiveDate:" + cdate + ",msg=" + "test";
						System.out.println(msg);
						JSONObject syslog = new JSONObject();
						syslog.put("hostIP", ip);
						syslog.put("cdate", cdate);
						syslog.put("msg", "test");
						// add the syslog to queue
						System.out.println(">>>>>>>>>add syslog");
						_sysLogStore.addSyslog(syslog);
						sleep(1000l);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 * @param port
	 *            the syslog listener port
	 */
	private void launchingSyslogReceive() {
		theLogger.debug("=============syslog listener start==============");
		byte[] log_buffer = new byte[2048];

		DatagramSocket socket = null;
		try {

			// Create a DatagramPacket to receive the incoming syslog data
			DatagramPacket packet = new DatagramPacket(log_buffer, log_buffer.length);
			theLogger.debug("============the listener port is " + _sysLogListenerPort + "=============");
			// Create a socket that listens on the net
			socket = new DatagramSocket(_sysLogListenerPort);
			// Create a Thread that get syslog from queue to stroe
			new Thread() {
				public void run() {
					theLogger.debug("---------------syslog local Storage start--------------");
					while (true) {
						try {
							Thread.sleep(1000L);
							_sysLogStore.storeSyslog();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				}
			}.start();
			while (1 == 1) {
				// Wait until some data arrives. Aren’t threads great?

				socket.receive(packet);

				String packet_string = new String(log_buffer, 0, 0, packet.getLength());
				if (packet_string != null
						&& !(packet_string.trim().equals("") || packet_string.trim().equals("\r") || packet_string.trim().equals("\n") || packet_string.trim().equals("\r\n"))) {
					String ip = new String(packet.getAddress().getHostAddress());
					String cdate = timedf.format(new Date());
					String msg = "firewall_ip:" + ip + "," + "reseiveDate:" + cdate + ",msg=" + packet_string;
					System.out.println(msg);
					JSONObject syslog = new JSONObject();
					syslog.put("hostIP", ip);
					syslog.put("cdate", cdate);
					syslog.put("msg", packet_string);
					// add the syslog to queue
					_sysLogStore.addSyslog(syslog);

				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			socket.close();
		}

	}
}
