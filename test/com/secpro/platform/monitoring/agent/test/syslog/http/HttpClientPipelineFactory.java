package com.secpro.platform.monitoring.agent.test.syslog.http;

import static org.jboss.netty.channel.Channels.*;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;

public class HttpClientPipelineFactory implements ChannelPipelineFactory {
	private HttpClient client = null;

	public HttpClientPipelineFactory(HttpClient client) {
		this.client = client;
	}

	public ChannelPipeline getPipeline() throws Exception {
		// Create a default pipeline implementation.
		ChannelPipeline pipeline = pipeline();

		pipeline.addLast("codec", new HttpClientCodec());

		// Remove the following line if you don't want automatic content
		// decompression.
		pipeline.addLast("inflater", new HttpContentDecompressor());

		// Uncomment the following line if you don't want to handle HttpChunks.
		// pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));

		pipeline.addLast("handler", new HttpResponseHandler(this.client));
		return pipeline;
	}
}
