package uk.org.cse.messageglue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import uk.org.cse.messageglue.Glue;
import uk.org.cse.messageglue.export.Declare;
import uk.org.cse.messageglue.export.IAsyncOutput;
import uk.org.cse.messageglue.export.IErrorHandler;
import uk.org.cse.messageglue.export.IMessageGlue;
import uk.org.cse.messageglue.export.Queue;
import uk.org.cse.messageglue.export.UseQueue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class AsynchronousReturnIntegrationTest {
	private static final String Q = "q";
	
	@Test
	public void asyncServiceWorks() throws IOException, InterruptedException {
		final StringAndNumberSource tsp = new StringAndNumberSource();
		
		final IMessageGlue binder = 
				Glue.create(
						IErrorHandler.STANDARD_ERROR, 
						new TestChannelProvider(), 
						new ObjectMapper());
		
		binder.bind(IAsyncProcess.class, tsp);
		
		binder.start();
		
		final IAsyncProcess client = binder.implement(IAsyncProcess.class);
		
		final List<Object> things = new ArrayList<Object>();;
		final List<Long> timings = new ArrayList<Long>();
		
		final AtomicInteger closes = new AtomicInteger();
		
		final IAsyncOutput<String> first = new IAsyncOutput<String>() {

			@Override
			public void close() throws Exception {
				closes.incrementAndGet();
			}

			@Override
			public void write(String value) {
				things.add(value);
				timings.add(System.currentTimeMillis());
			}

			@Override
			public void error(final String message, final Optional<Throwable> throwable) {
			
			}
			
			@Override
			public boolean isOpen() {
				return true;
			}
		};
		
		final IAsyncOutput<Integer> second = new IAsyncOutput<Integer>() {

			@Override
			public void close() throws Exception {
				closes.incrementAndGet();
			}

			@Override
			public void write(Integer value) {
				things.add(value);
				timings.add(System.currentTimeMillis());
			}

			@Override
			public void error(final String message, final Optional<Throwable> throwable) {
				
			}
			
			@Override
			public boolean isOpen() {
				return true;
			}
		};
		
		client.handle("hello", first, second);
		
		Thread.sleep(3000);
		Assert.assertEquals(
				ImmutableList.of("hello", 1, 100, "boo", 99, "bye"),
				things);

		Assert.assertEquals(2, closes.get());
		
		binder.stop();
	}
	
	static class StringAndNumberSource implements IAsyncProcess {
		@Override
		public void handle(final String s, final IAsyncOutput<String> first,final IAsyncOutput<Integer> second) {
			new Thread(
					new Runnable() {
						@Override
						public void run() {
							try {
								first.write(s);
								Thread.sleep(1000);
								
								second.write(1);
								second.write(100);
								first.write("boo");
								second.write(99);
								Thread.sleep(1000);
								first.write("bye");
								
								try {
									first.close();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								try {
									second.close();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						}
					}).start();
		}
	}
	
	@Declare(queues=@Queue(Q))
	public interface IAsyncProcess {
		@UseQueue(Q)
		public void handle(
				final String command,
				final IAsyncOutput<String> first, 
				final IAsyncOutput<Integer> second);
	}
}
