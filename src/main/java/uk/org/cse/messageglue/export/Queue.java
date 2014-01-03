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
	 * @return The name of the queue if not anonymous; otherwise, a name which 
	 * will not the be queue's name, but will be usable in other annotations
	 */
	public String value();
	
	/**
	 * @return true if this should really be a temporary anonymous queue
	 * Implies persistent=false, exclusive=true, autodelete=true
	 */
	public boolean anonymous() default false;
	
	public boolean persistent() default true;
	public boolean exclusive() default false;
	public boolean autodelete() default false;
	/**
	 * @return initial bindings for the queue
	 */
	public QueueBinding[] bindings() default {};
}
