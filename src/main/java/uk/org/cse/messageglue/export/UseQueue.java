package uk.org.cse.messageglue.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind a method so that served classes read directly from some named queue
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface UseQueue {
	/**
	 * @return the queue name - this can be an anonymous queue's name (see {@link Queue}),
	 * but in that case you also want {@link UseExchange} or nobody will be able to publish
	 * to you.
	 */
	public String value();
}
