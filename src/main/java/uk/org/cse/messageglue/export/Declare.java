package uk.org.cse.messageglue.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import uk.org.cse.messageglue.MessageGlue;

/**
 * Added to a class which is being processed by {@link MessageGlue}, this 
 * @author hinton
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Declare {
	public Exchange[] exchanges() default {};
	public Queue[] queues() default {};
}
