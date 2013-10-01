package uk.org.cse.messageglue.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind a method so the bridged class reads directly from a named queue
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface UseQueue {
	/**
	 * @return the queue name
	 */
	public String value();
}
