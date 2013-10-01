package uk.org.cse.messageglue.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import uk.org.cse.messageglue.MessageGlue;

/**
 * Added to a class which is being processed by {@link MessageGlue}, this 
 * defines a set of exchanges and queues which should be declared to exist
 * before anything else happens
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Declare {
	/**
	 * Exchanges to declare
	 */
	public Exchange[] exchanges() default {};
	/**
	 * Queues to declare
	 */
	public Queue[] queues() default {};
}
