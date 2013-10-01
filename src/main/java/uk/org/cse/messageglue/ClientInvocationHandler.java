package uk.org.cse.messageglue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.inject.Provider;

import uk.org.cse.messageglue.export.IAsyncOutput;
import uk.org.cse.messageglue.export.IErrorHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

public class ClientInvocationHandler implements InvocationHandler {
	final Map<Method, MethodClient<Object>> clients = new HashMap<>();
	
	public ClientInvocationHandler(final IErrorHandler errorHandler, Set<Method> modelMethods, final Provider<Channel> channelProvider, final ObjectMapper mapper) {
		for (final Method m : modelMethods) {
			clients.put(m, new MethodClient<>(errorHandler, channelProvider, mapper, m));
		}
	}

	@Override
	public Object invoke(
			Object arg0, 
			Method arg1, 
			Object[] arg2)
			throws Throwable {
		if (clients.containsKey(arg1)) {
			final Object message = arg2[0];
			final IAsyncOutput<?>[] outputs = new IAsyncOutput<?>[arg2.length-1];
			
			for (int i = 1; i<arg2.length; i++) {
				outputs[i-1] = (IAsyncOutput<?>) arg2[i];
			}
			
			return invokeClient(clients.get(arg1), message, outputs);
		} else {
			throw new RuntimeException("Binder-implemented proxy class does not handle method " + arg1);
		}
	}

	
	private static <T> Object invokeClient(final MethodClient<T> client, final T message, final IAsyncOutput<?>[] outputs) throws TimeoutException {
		return client.invoke(message, outputs);
	}
}
