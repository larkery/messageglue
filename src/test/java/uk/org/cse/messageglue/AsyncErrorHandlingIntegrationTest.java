package uk.org.cse.messageglue;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import uk.org.cse.messageglue.export.Declare;
import uk.org.cse.messageglue.export.Exchange;
import uk.org.cse.messageglue.export.IAsyncOutput;
import uk.org.cse.messageglue.export.IErrorHandler;
import uk.org.cse.messageglue.export.IMessageGlue;
import uk.org.cse.messageglue.export.UseExchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

public class AsyncErrorHandlingIntegrationTest {
	@Test
	public void errorsArePassedOverTheNetwork() throws IOException, InterruptedException {
		final IMessageGlue binder = 
				Glue.create(
						IErrorHandler.STANDARD_ERROR,
						new TestChannelProvider(), 
						new ObjectMapper());
		binder.bind(I.class, new I() {
			@Override
			public void go(String s, IAsyncOutput<String> outs) {
				try {
					throw new IllegalStateException("oh no!");
				} catch (Throwable th) {
					outs.error("An error!!!", Optional.of(th));
					try {
						outs.close();
					} catch (Exception e) {
					}
				}
			}
		});
		
		binder.start();
		
		final I client = binder.implement(I.class);
		
		final boolean[] closed = new boolean[1];
		final String[] wrote = new String[1];
		
		final String[] errors = new String[1];
		final Throwable[] thrown = new Throwable[1];
		
		client.go("bork", new IAsyncOutput<String>() {
			@Override
			public void close() throws Exception {
				closed[0] = true;
			}

			@Override
			public void write(String value) {
				wrote[0] = value;
			}

			@Override
			public void error(String message, Optional<Throwable> throwable) {
				errors[0] = message;
				thrown[0] = throwable.orNull();
			}

			@Override
			public boolean isOpen() {
				return false;
			}
		});
		
		// since we are async we want to hang around for a bit
		Thread.sleep(1000);
		
		binder.stop();
		
		Assert.assertTrue("was closed", closed[0]);
		Assert.assertEquals("An error!!!", errors[0]);
		Assert.assertTrue(thrown[0] instanceof IllegalStateException);
	}
	
	@Declare(exchanges=@Exchange("E"))
	public interface I {
		@UseExchange("E")
		public void go(final String s, final IAsyncOutput<String> outs);
	}
}
