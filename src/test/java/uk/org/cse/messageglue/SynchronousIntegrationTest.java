package uk.org.cse.messageglue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import uk.org.cse.messageglue.Glue;
import uk.org.cse.messageglue.export.Declare;
import uk.org.cse.messageglue.export.Exchange;
import uk.org.cse.messageglue.export.IErrorHandler;
import uk.org.cse.messageglue.export.IMessageGlue;
import uk.org.cse.messageglue.export.Multiple;
import uk.org.cse.messageglue.export.Queue;
import uk.org.cse.messageglue.export.QueueBinding;
import uk.org.cse.messageglue.export.Timeout;
import uk.org.cse.messageglue.export.UseExchange;
import uk.org.cse.messageglue.export.UseQueue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

public class SynchronousIntegrationTest {
	public static final String Q = "q";
	public static final String E = "e";
	
	@Test
	public void synchronousMethodReturnWorks() throws IOException, InterruptedException, TimeoutException {
		final TestStringProcessor tsp = new TestStringProcessor(0);
		
		final IMessageGlue binder = 
				Glue.create(IErrorHandler.STANDARD_ERROR, new TestChannelProvider(), new ObjectMapper());
	
		
		binder.bind(IStringProcessor.class, tsp);
		
		binder.start();
		
		final IStringProcessor client = binder.implement(IStringProcessor.class);
		
		final String upper = client.process("lower");
		
		Assert.assertEquals("LOWER", upper);
		
		binder.stop();
	}
	
	@Test(expected=TimeoutException.class)
	public void exceedingSynchronousTimeoutCausesException() throws IOException, TimeoutException {
		final TestStringProcessor tsp = new TestStringProcessor(2000);
		
		final IMessageGlue binder = 
				Glue.create(IErrorHandler.STANDARD_ERROR, new TestChannelProvider(), new ObjectMapper());
	
		binder.bind(IStringProcessor.class, tsp);
		
		try {
			binder.start();
			
			final IStringProcessor client = binder.implement(IStringProcessor.class);
			
			client.process("lower");
		} finally {
			binder.stop();
		}
	}
	
	
	@Test
	public void multipleReturnMethodsGatherUpValues() throws TimeoutException, IOException, InterruptedException {
		final IMessageGlue binder = 
				Glue.create(IErrorHandler.STANDARD_ERROR, new TestChannelProvider(), new ObjectMapper());
	
		binder.bind(IMultipleProcessor.class, new TestMultipleProcessor(0));
		
		binder.start();
		final IMultipleProcessor multi = binder.implement(IMultipleProcessor.class);
		
		Assert.assertEquals(ImmutableList.of("LOWER"), multi.process("lower"));
		
		binder.bind(IMultipleProcessor.class, new TestMultipleProcessor(0));
		
		Assert.assertEquals(ImmutableList.of("LOWER", "LOWER"), multi.process("lower"));
		
		binder.bind(IMultipleProcessor.class, new TestMultipleProcessor(4000));
		
		Assert.assertEquals(ImmutableList.of("LOWER", "LOWER"), multi.process("lower"));
		
		binder.bind(IMultipleProcessor.class, new TestMultipleProcessor(0));
		
		Assert.assertEquals(ImmutableList.of("LOWER", "LOWER"), multi.process("lower"));
		
		binder.stop();
		
		// let us also test a restart
		
		Assert.assertTrue("With service off, no returns come out", multi.process("lower").isEmpty());
		
		binder.start();
		
		Assert.assertEquals("Service back on, works as before!", 
				ImmutableList.of("LOWER", "LOWER"), multi.process("lower"));
		
		binder.stop();
	}
	
	@Declare(exchanges=@Exchange(E),
			 queues=@Queue(value=Q, bindings=@QueueBinding(E)))
	public interface IStringProcessor {
		@UseQueue(Q)
		@UseExchange(E)
		@Timeout
		public String process(
				final String in) throws TimeoutException;
	}
	
	static class TestStringProcessor implements IStringProcessor {
		private final int delay;
		
		public TestStringProcessor(int delay) {
			super();
			this.delay = delay;
		}

		@Override
		public String process(String in) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return in.toUpperCase();
		}
	}
	
	@Declare(exchanges=@Exchange(E+"1"))
	public interface IMultipleProcessor {
		@UseExchange(E+"1")
		@Timeout
		@Multiple(max=2)
		public List<String> process(final String in) throws TimeoutException;
	}
	
	static class TestMultipleProcessor implements IMultipleProcessor {
		private final int delay;
		
		public TestMultipleProcessor(int delay) {
			super();
			this.delay = delay;
		}

		@Override
		public List<String> process(String in) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return ImmutableList.of(in.toUpperCase());
		}
	}
}
