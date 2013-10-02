package uk.org.cse.messageglue;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.Test;

import uk.org.cse.messageglue.export.Multiple;
import uk.org.cse.messageglue.export.Timeout;
import uk.org.cse.messageglue.export.UseExchange;

public class ModelErrorTest {
	interface TestInterface {
		@UseExchange(value="x")
		public void noPayload();
		
		public String noMarkup(String payload);
		
		@UseExchange(value="x")
		@Timeout
		public void badTimeout(String payload);
		
		@Multiple
		@UseExchange(value="x")
		public void badMultiple1(String payload);
		
		@Multiple
		@UseExchange(value="x")
		public Set<String> badMultiple2(String payload);
		
		@UseExchange(value="x")
		public Set<String> badAsync(String payload, String badThing);
	}
	
	private static Method methodNamed(String string) {
		for (final Method m : TestInterface.class.getMethods()) {
			if (m.getName().equals(string)) return m;
		}
		throw new RuntimeException("No method in TestInterface called " + string + ", which means you did the test wrong.");
	}
	
	@Test(expected=GlueDefinitionException.class)
	public void modelInvalidIfNoPayload() {
		Model.of(methodNamed("noPayload"));
	}

	@Test(expected=GlueDefinitionException.class)
	public void modelInvalidIfNoMarkupOnMethod() {
		Model.of(methodNamed("noMarkup"));
	}
	
	@Test(expected=GlueDefinitionException.class)
	public void modelInvalidIfTimeoutButVoid() {
		Model.of(methodNamed("badTimeout"));
	}
	
	@Test(expected=GlueDefinitionException.class)
	public void modelInvalidIfMultipleButNotListReturning() {
		Model.of(methodNamed("badMultiple2"));
	}
	
	@Test(expected=GlueDefinitionException.class)
	public void modelInvalidIfMultipleButVoid() {
		Model.of(methodNamed("badMultiple1"));
	}
	
	@Test(expected=GlueDefinitionException.class)
	public void modelInvalidIfNonPayloadArgumentsAreNotAsyncOutputs() {
		Model.of(methodNamed("badAsync"));
	}
}
