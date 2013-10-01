package uk.org.cse.messageglue;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import uk.org.cse.messageglue.export.IAsyncOutput;
import uk.org.cse.messageglue.export.Multiple;
import uk.org.cse.messageglue.export.Timeout;
import uk.org.cse.messageglue.export.UseExchange;
import uk.org.cse.messageglue.export.UseQueue;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

class Model<MT> {
	private final String submissionExchange;
	private final String submissionRoutingKey;
	private final String namedQueue;
	private final boolean fromNamedQueue;
	
	private final Optional<Output<?>> directOutput;
	
	private final List<Output<?>> asyncOutputs;
	
	private final Class<MT> messageType;
	
	private final Optional<Long> timeout;
	
	private final Optional<Integer> multiMaximum;
	
	private Model(
			final Class<MT> messageType,
			final String submissionExchange, 
			final String submissionRoutingKey,
			final String namedQueue,
			final Optional<Output<?>> directOutput, 
			final Optional<Long> timeout,
			final Optional<Integer> multiMaximum,
			final List<Output<?>> asyncOutputs) {
		super();
		this.timeout = timeout;
		this.multiMaximum = multiMaximum;
		this.fromNamedQueue = namedQueue != null;
		this.messageType = messageType;
		this.submissionExchange = submissionExchange;
		this.submissionRoutingKey = submissionRoutingKey;
		this.namedQueue = namedQueue;
		this.directOutput = directOutput;
		this.asyncOutputs = asyncOutputs;
	}
	
	public String getSubmissionExchange() {
		return submissionExchange;
	}
	
	public String getSubmissionRoutingKey() {
		return submissionRoutingKey;
	}
	
	public String getNamedQueue() {
		return namedQueue;
	}
	
	public boolean isFromNamedQueue() {
		return fromNamedQueue;
	}
	
	public Optional<Output<?>> getDirectOutput() {
		return directOutput;
	}
	
	public List<Output<?>> getAsyncOutputs() {
		return asyncOutputs;
	}
	
	public Class<MT> getMessageType() {
		return messageType;
	}

	public boolean isReplyQueueNeeded() {
		return directOutput.isPresent() || !asyncOutputs.isEmpty();
	}
	
	public Optional<Long> getTimeout() {
		return timeout;
	}
	
	public Optional<Integer> getMultipleReturnLimit() {
		return multiMaximum;
	}
	
	public boolean isMultipleReturn() {
		return multiMaximum.isPresent();
	}
	
	@SuppressWarnings("unchecked")
	public static <MT> Model<MT> of(final Method m) {
		final UseExchange fe = m.getAnnotation(UseExchange.class);
		final UseQueue fq = m.getAnnotation(UseQueue.class);
		
		final boolean noFe = fe == null;
		final boolean noFq = fq == null;
		
		if (noFe && noFq) {
			throw new GlueDefinitionException(String.format("%s should have at least one FromExchange or FromQueue annotation", m));
		}
		
		final String submissionExchange = 
				(fe == null) ? "" : fe.value();
		final String submissionRoutingKey = 
				(fe == null) ? fq.value() : fe.key();
		final Output<?> directOutput;
		
		
		final Timeout timeout = m.getAnnotation(Timeout.class);
		final Long timeoutLong;
		
		if (timeout != null) {
			timeoutLong = timeout.value();
			
			boolean hasException = false;
			for (final Class<?> e : m.getExceptionTypes()) {
				if (e.equals(TimeoutException.class)) {
					hasException = true;
					break;
				}
			}
			
			if (!hasException) {
				throw new GlueDefinitionException(String.format("%s declares a timeout but does not throw TimeoutException, which it should", m));
			}
		} else {
			timeoutLong = null;
		}
		
		final Multiple multiple = m.getAnnotation(Multiple.class);
		final Optional<Integer> multimax;
		
		if (multiple != null) {
			multimax = Optional.of(multiple.max());
		} else {
			multimax = Optional.absent();
		}
		
		if (m.getReturnType() == Void.TYPE) {
			directOutput = null;
			if (multiple != null) {
				throw new GlueDefinitionException(String.format("%s is marked as multiple but doesn't return anything, which makes no sense", m));
			}
			if (timeout != null) {
				throw new GlueDefinitionException(String.format("%s declares a timeout but doesn't return anything, which makes no sense", m));
			}
		} else {
			if (multimax.isPresent()) {
				// check return type is a list or a set
				final TypeToken<?> tok = TypeToken.of(m.getGenericReturnType());
				if (List.class != tok.getRawType()) {
					throw new GlueDefinitionException(String.format("%s is marked as multiple, but does not return List<?>", m));
				}
				
				final TypeToken<?> clazz = tok.resolveType(
						((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0]
						);
				
				// extract collection type parameter
				directOutput = Output.to(clazz.getRawType());
			} else {
				directOutput = Output.to(m.getReturnType());
			}
		}
		
		final ImmutableList.Builder<Output<?>> asyncOutputs = ImmutableList.builder();
		
		final Class<?>[] parameterTypes = m.getParameterTypes();
		final Type[] genericParameterTypes = m.getGenericParameterTypes();
		
		final Class<?> messageType;
		
		if (parameterTypes.length == 0) {
			throw new GlueDefinitionException(String.format("%s has no arguments; at least one argument is required (the message body).", m));
		} else {
			messageType = parameterTypes[0];
		}
		
		for (int i = 1; i<parameterTypes.length; i++) {			
			final Type t = genericParameterTypes[i];
			
			if ((t instanceof ParameterizedType) && parameterTypes[i].equals(IAsyncOutput.class)) {
				final TypeToken<?> token = TypeToken.of(t);
				
				final TypeToken<?> parameter = token.resolveType(
						((ParameterizedType) t).getActualTypeArguments()[0]
						);
				
				final Output<?> out = Output.to(parameter.getRawType());
				
				asyncOutputs.add(out);
			} else {
				throw new GlueDefinitionException(String.format("parameter %d of %s is not an IAsyncOutput<T>", i, m));
			}
		}
		
		return new Model<MT>(
				(Class<MT>) messageType, 
				submissionExchange, 
				submissionRoutingKey, 
				fq == null ? null : fq.value(),
				Optional.<Output<?>>fromNullable(directOutput), 
				Optional.fromNullable(timeoutLong),
				multimax,
				asyncOutputs.build());
	}

	static class Output<T> {
		private final Class<T> messageType;

		Output(Class<T> clazz) {
			super();
			this.messageType = clazz;
		}

		public static <T> Output<T> to(final Class<T> clazz) {
			return new Output<T>(
					clazz
					);
		}

		public Class<T> getMessageType() {
			return messageType;
		}
	}

	public static Set<Method> getModelMethods(final Class<?> clazz) {
		final ImmutableSet.Builder<Method> builder = ImmutableSet.builder();
		
		for (final Method m : clazz.getMethods()) {
			if (m.getAnnotation(UseQueue.class) != null || m.getAnnotation(UseExchange.class) != null) {
				builder.add(m);
			}
		}
		
		return builder.build();
	}
}
