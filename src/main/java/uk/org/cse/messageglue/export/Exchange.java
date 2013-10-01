package uk.org.cse.messageglue.export;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

/**
 * Defines an exchange, which in {@link Declare} will cause the creation of an exchange.
 */
public @interface Exchange {
	/**
	 * @return the exchange's name
	 */
	public String value();
	/**
	 * @return the type of exchange
	 */
	public Type type() default Type.Fanout;
	
	public enum Type {
		Direct,
		Topic,
		Headers,
		Fanout
	}
}
