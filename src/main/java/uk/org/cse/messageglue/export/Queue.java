package uk.org.cse.messageglue.export;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

/**
 * Defines a queue for use in {@link Declare} annotation
 *
 */
public @interface Queue {
	/**
	 * @return The name of the queue
	 */
	public String value();
	public boolean persistent() default true;
	public boolean exclusive() default false;
	public boolean autodelete() default false;
	/**
	 * @return initial bindings for the queue
	 */
	public QueueBinding[] bindings() default {};
}
