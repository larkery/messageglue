package uk.org.cse.messageglue.export;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

public @interface Exchange {
	public String value();
	public Type type() default Type.Fanout;
	
	public enum Type {
		Direct,
		Topic,
		Headers,
		Fanout
	}
}
