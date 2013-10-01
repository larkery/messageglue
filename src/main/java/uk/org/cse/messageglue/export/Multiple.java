package uk.org.cse.messageglue.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Glued methods which return {@link List} can have this associated to them;
 * this will cause a special behaviour where the collection is not sent over the wire,
 * but instead multiple results are transmitted in distinct messages;
 * 
 * This must be used with {@link Timeout} or {@link #max()} if you want it to terminate.
 * 
 * The method type must be exactly List<T> where T is concrete enough to have a raw type
 * (so you can have List<Thing<Q>> but not List<Q>).
 * 
 * @author hinton
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Multiple {
	/**
	 * @return only this many results will get collected before the method returns.
	 */
	public int max() default Integer.MAX_VALUE;
}
