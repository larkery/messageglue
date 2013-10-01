package uk.org.cse.messageglue;

import java.io.IOException;

import javax.inject.Provider;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

class TestChannelProvider implements Provider<Channel> {
	private Connection connection;
	
	public TestChannelProvider() throws IOException {
		final ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		this.connection = factory.newConnection();
	}
	
	@Override
	public Channel get() {
		try {
			return connection.createChannel();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
