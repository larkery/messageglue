package uk.org.cse.messageglue.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind a method so that (a) if {@link UseQueue} is not also used, it will
 * read from a temporary queue bound to the given exchange with the given routing key,
 * and (b) whether or not {@link UseQueue} is also used, client invocations
 * will publish their requests to the given exchange with the given routing key.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface UseExchange {
	/**
	 * @return the name of the exchange
	 */
	public String value();
	/**
	 * @return the binding key for reading from the exchange
	 */
	public String key() default "";
}
