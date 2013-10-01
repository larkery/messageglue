package uk.org.cse.messageglue.export;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeoutException;

/**
 * If you are gluing a synchronous method (i.e. one with a non-void return type),
 * you can apply this annotation to make it fail with a {@link TimeoutException}
 * if no answer is received in a given amount of time.
 * 
 * When used with {@link Multiple}, the {@link TimeoutException} will not get thrown,
 * instead a list with zero elements will be returned.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Timeout {
	public long value() default 1000;
}
