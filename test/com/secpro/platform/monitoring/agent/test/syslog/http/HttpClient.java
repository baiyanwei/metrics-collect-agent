package com.secpro.platform.monitoring.agent.test.syslog.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.secpro.platform.core.utils.Constants;

public class HttpClient {
	private URI targetURI = null;

	private long startTime = -1;

	private long reponseTiming = -1;

	private HttpResponseStatus responseStatus = null;

	private long httpResponseContentSize = 0;

	public HttpClient(URI targetURI) {
		this.targetURI = targetURI;
	}

	public void start(String content) throws Exception {

		if (this.targetURI == null) {
			throw new Exception("ivalid target.");
		}
		String scheme = targetURI.getScheme() == null ? "http" : targetURI.getScheme();
		String host = targetURI.getHost() == null ? "localhost" : targetURI.getHost();
		int port = targetURI.getPort();
		if (port == -1) {
			if (scheme.equalsIgnoreCase("http")) {
				port = 80;
			} else if (scheme.equalsIgnoreCase("https")) {
				port = 443;
			}
		}

		if (!scheme.equalsIgnoreCase("http")) {
			System.err.println("Only HTTP is supported.");
			return;
		}
		String path = targetURI.getPath();
		if (path == null || path.trim().equals("")) {
			path = "/";
		}
		// Configure the client.
		ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new HttpClientPipelineFactory(this));

		// Start the connection attempt.
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));

		// Wait until the connection attempt succeeds or fails.
		Channel channel = future.awaitUninterruptibly().getChannel();
		if (!future.isSuccess()) {
			future.getCause().printStackTrace();
			bootstrap.releaseExternalResources();
			return;
		}
		HttpRequest request = createHttpMessage(host, path, HttpMethod.POST, content);
		// Prepare the HTTP request.
		// HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
		// HttpMethod.PUT, path);
		// request.setHeader(HttpHeaders.Names.HOST, host);
		// request.setHeader(HttpHeaders.Names.CONNECTION,
		// HttpHeaders.Values.CLOSE);
		// //request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING,
		// HttpHeaders.Values.GZIP + "," + HttpHeaders.Values.DEFLATE);
		// request.setHeader(HttpHeaders.Names.CONTENT_ENCODING, "UTF-8");
		// String content=">>>>>>>>>>>>>>>>>>>>>>>>>";
		// byte[] bytes = content.getBytes(Constants.DEFAULT_CHARSET);
		// System.out.println("put:" + content);
		//
		// request.addHeader(HttpHeaders.Names.CONTENT_LENGTH,
		// String.valueOf(bytes.length));
		// request.addHeader(HttpHeaders.Names.USER_AGENT,
		// "Mectrics-Collect-Agent");
		// System.out.println(HttpHeaders.Names.CONTENT_LENGTH + ":" +
		// bytes.length);
		// ChannelBuffer channelBuffer = ChannelBuffers.buffer(bytes.length);
		// channelBuffer.writeBytes(bytes);
		// request.setContent(channelBuffer);
		// Set some example cookies.
		// CookieEncoder httpCookieEncoder = new CookieEncoder(false);
		// httpCookieEncoder.addCookie("my-cookie", "foo");
		// httpCookieEncoder.addCookie("another-cookie", "bar");
		// request.setHeader(HttpHeaders.Names.COOKIE,
		// httpCookieEncoder.encode());

		this.startTime = System.nanoTime();
		// Send the HTTP request.
		channel.write(request);

		// Wait for the server to close the connection.
		channel.getCloseFuture().awaitUninterruptibly();

		// Shut down executor threads to exit.
		bootstrap.releaseExternalResources();
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getReponseTiming() {
		return reponseTiming;
	}

	public void setReponseTiming(long reponseTiming) {
		this.reponseTiming = reponseTiming;
		setDataReady();
	}

	public HttpResponseStatus getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(HttpResponseStatus responseStatus) {
		this.responseStatus = responseStatus;
	}

	public long getHttpResponseContentSize() {
		return httpResponseContentSize;
	}

	public void setHttpResponseContentSize(long httpResponseContentSize) {
		this.httpResponseContentSize = httpResponseContentSize;
	}

	private void setDataReady() {
		//
		System.out.println("target:" + this.targetURI + " , response status:" + this.responseStatus + " , content size:" + this.httpResponseContentSize + " , response timing(ms):"
				+ (this.reponseTiming / 1000000));
	}

	public static void main(String[] args) {
		try {
			URI uri = new URI("http://localhost:8080/tss/samples/push");
			new HttpClient(uri).start("");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(">>>>>>>?>?");
	}

	private DefaultHttpRequest createHttpMessage(String host, String accessPath, HttpMethod httpMethod, String content) throws NoSuchAlgorithmException, IOException, Exception {
		if (content == null) {
			content = "";
		}

		// fill the request parameters in to query string.
		// StringBuilder parametersBuilder = new StringBuilder("?");
		// parametersBuilder.append("c=&l=&o=");
		// create HTTP request with query parameter.

		DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, httpMethod, accessPath);
		// identify HTTP port we use
		request.addHeader(HttpHeaders.Names.HOST, host);
		TreeMap<String, String> requestHeaders = new TreeMap<String, String>(new Comparator<String>() {
			public int compare(String string0, String string1) {
				return string0.compareToIgnoreCase(string1);
			}
		});
		//
		requestHeaders.put(HttpHeaders.Names.DATE, new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(new Date()));
		// md5 coding hash string.
		// requestHeaders.put(HttpHeaders.Names.CONTENT_MD5,
		// computeMD5Hash(content));
		requestHeaders.put(HttpHeaders.Names.CONTENT_TYPE, "text/plain");

		// We need to set the content encoding to be UTF-8 in order to have the
		// message properly decoded.
		requestHeaders.put(HttpHeaders.Names.CONTENT_ENCODING, Constants.DEFAULT_ENCODING);
		// Create the security signature.
		// String signature = this.createSignature(content, requestHeaders,
		// httpMethod, Constants.HMAC_SHA1_ALGORITHM);

		// Add the security parameters
		// request.addHeader("SignatureMethod", Constants.HMAC_SHA1_ALGORITHM);
		// request.addHeader("SignatureVersion", "2");
		// request.addHeader("Authorization", "AWS " + this._username + ":" +
		// signature);
		// Add the customer headers to the request.
		Iterator<String> iterator = requestHeaders.keySet().iterator();
		while (iterator.hasNext() == true) {
			String name = iterator.next();
			String value = requestHeaders.get(name);
			request.addHeader(name, value);
		}

		// Needs to use the size of the bytes in the string.
		byte[] bytes = content.getBytes(Constants.DEFAULT_CHARSET);
		System.out.println("put:" + content);

		request.addHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(bytes.length));
		request.addHeader(HttpHeaders.Names.USER_AGENT, "Mectrics-Collect-Agent");
		System.out.println(HttpHeaders.Names.CONTENT_LENGTH + ":" + bytes.length);
		ChannelBuffer channelBuffer = ChannelBuffers.buffer(bytes.length);
		channelBuffer.writeBytes(bytes);
		request.setContent(channelBuffer);
		return request;
	}
}
