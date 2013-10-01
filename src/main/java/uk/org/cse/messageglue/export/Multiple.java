package uk.org.cse.messageglue.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Glued methods which return collection types can have this associated to them;
 * this will cause a special behaviour where the collection is not sent over the wire,
 * but instead multiple results are transmitted in distinct messages;
 * 
 * This must be used with {@link Timeout} or {@link #max()} if you want it to terminate.
 * 
 * @author hinton
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Multiple {
	public int max() default Integer.MAX_VALUE;
}
