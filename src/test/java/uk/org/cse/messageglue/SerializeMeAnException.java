package uk.org.cse.messageglue;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import uk.org.cse.messageglue.Msg;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializeMeAnException {
	@Test
	public void go() throws IOException {
		final ObjectMapper om = new ObjectMapper();
		
		final Msg.Error e = new Msg.Error("test", new RuntimeException("blam"));
		
		final Msg.Error e2 = om.readValue(
				om.writeValueAsBytes(e)
				, Msg.Error.class);
		
		e2.getCause().printStackTrace();
		Assert.assertTrue(e2.getCause() instanceof RuntimeException);
		Assert.assertEquals("blam", e2.getCause().getMessage());
	}
}
