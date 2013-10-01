package uk.org.cse.messageglue;

import javax.inject.Provider;

import uk.org.cse.messageglue.export.IErrorHandler;
import uk.org.cse.messageglue.export.IMessageGlue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

public class Glue {
	public static IMessageGlue create(
			IErrorHandler errorHandler,
			Provider<Channel> channels, 
			ObjectMapper mapper) {
		return new MessageGlue(errorHandler, channels, mapper);
	}
}
