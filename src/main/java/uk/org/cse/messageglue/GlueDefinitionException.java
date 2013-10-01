package uk.org.cse.messageglue;

public class GlueDefinitionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public GlueDefinitionException(String message, Throwable cause) {
		super(message, cause);
	}

	public GlueDefinitionException(String message) {
		super(message);
	}
}
