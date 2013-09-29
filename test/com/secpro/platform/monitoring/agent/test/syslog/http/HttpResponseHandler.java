package com.secpro.platform.monitoring.agent.test.syslog.http;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;

public class HttpResponseHandler extends SimpleChannelUpstreamHandler {
	private HttpClient client = null;
	private boolean readingChunks;

	public HttpResponseHandler(HttpClient client) {
		this.client = client;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!readingChunks) {
			HttpResponse response = (HttpResponse) e.getMessage();
			//
			this.client.setResponseStatus(response.getStatus());
			//
			this.client.setHttpResponseContentSize(response.getContentLength());

			if (response.getStatus().getCode() == 200 && response.isChunked()) {
				readingChunks = true;
			} else {
				ChannelBuffer content = response.getContent();
				if (content.readable()) {
					content.toString(CharsetUtil.UTF_8).length();
				}
				//
				this.client.setReponseTiming( System.nanoTime() - this.client.getStartTime());
				ctx.getChannel().close();
			}
		} else {
			HttpChunk chunk = (HttpChunk) e.getMessage();
			if (chunk.isLast()) {
				readingChunks = false;
				//
				this.client.setReponseTiming(System.currentTimeMillis() - this.client.getStartTime());
				ctx.getChannel().close();
			} else {
				chunk.getContent().toString(CharsetUtil.UTF_8);
			}
		}

	}
}
