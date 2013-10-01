package uk.org.cse.messageglue.export;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

/**
 * Binds a queue to a named exchange, using a given routing key.
 *
 */
public @interface QueueBinding {
	/**
	 * @return the name of the exchange to bind to
	 */
	public String value();
	/**
	 * @return the routing key to bind with
	 */
	public String routingKey() default "";
}
