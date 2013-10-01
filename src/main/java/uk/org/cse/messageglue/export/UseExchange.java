package uk.org.cse.messageglue.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind a method so that the bridged class will be passed messages
 * from a named exchange.
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
