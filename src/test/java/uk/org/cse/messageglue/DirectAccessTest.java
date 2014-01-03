package uk.org.cse.messageglue;

import org.junit.Test;
import java.util.concurrent.TimeoutException;

import uk.org.cse.messageglue.export.Declare;
import uk.org.cse.messageglue.export.Exchange;
import uk.org.cse.messageglue.export.Queue;
import uk.org.cse.messageglue.export.QueueBinding;
import uk.org.cse.messageglue.export.Timeout;
import uk.org.cse.messageglue.export.UseExchange;
import uk.org.cse.messageglue.export.UseQueue;
import uk.org.cse.messageglue.export.IDirectAccess;
import org.junit.Test;
import java.util.Set;
import java.util.HashSet;
import uk.org.cse.messageglue.export.IMessageGlue;
import uk.org.cse.messageglue.export.IErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import com.google.common.collect.ImmutableSet;

public class DirectAccessTest {
	public static final String Q = "q";
	public static final String E = "e";
	public static final String R = "r";
	
	@Declare(exchanges = {@Exchange(E), @Exchange(R)}, 
			 queues = { @Queue(value = Q, anonymous = true, bindings = @QueueBinding(E)) })
	public interface IStringProcessor {
		@UseQueue(Q)
		@UseExchange(E)
		public void addString(final String in);
		
		@UseExchange(R)
		public void recoverStrings(final String destination);
	}
	
	static class TestStringProcessor implements IStringProcessor, IStartStoppable
	{
		static int counter = 0;
		int me = counter++;
		private IDirectAccess access;
		private final IStringProcessor others;
		public final Set<String> strings = new HashSet<String>();

		public TestStringProcessor(final IStringProcessor others) {
			super();
			if (others == null) throw new IllegalArgumentException("Others should not be null");
			this.others = others;
		}

		public void setDirectAccess(final IDirectAccess access) {
			if (access == null) {
				throw new IllegalArgumentException("Direct access should not be null");
			}
			this.access = access;
		}

		@Override
		public void addString(final String in) {
			System.err.println(me + " Adding a string " + in);
			strings.add(in);
		}

		public void recoverStrings(final String destination) {
			System.err.println(me + " Sending on my strings : " + strings + " to " + destination);
			access.sendMessagesToQueue(destination, new HashSet<Object>(strings));
		}
	
		@Override
		public void start() {
			if (access == null) {
				throw new IllegalStateException("Direct access to MQ not provided");
			}
			System.err.println(me + " Asking others to recover strings");
			others.recoverStrings(access.getAnonymousQueue(Q));
		}

		@Override
		public void stop() {
			
		}
	}

	@Test
	public void stringRecoveryWorks() throws Exception {
		final IMessageGlue binder = Glue.create(IErrorHandler.STANDARD_ERROR,
												new TestChannelProvider(),
												new ObjectMapper());
		binder.start();

		final IStringProcessor handle = binder.implement(IStringProcessor.class);

		final TestStringProcessor first = new TestStringProcessor(handle);

		binder.bind(IStringProcessor.class, first);
		
		Thread.sleep(500);

		handle.addString("hello");
		handle.addString("world");

		Thread.sleep(500);

		final TestStringProcessor second = new TestStringProcessor(handle);
		Thread.sleep(1000);
		binder.bind(IStringProcessor.class, second);
		Thread.sleep(1000);


		Assert.assertEquals(ImmutableSet.of("hello", "world"), second.strings);

		binder.stop();
	}
}
