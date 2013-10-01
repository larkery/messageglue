package uk.org.cse.messageglue.export;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

public @interface Queue {
	public String value();
	public boolean persistent() default true;
	public boolean exclusive() default false;
	public boolean autodelete() default false;
	public QueueBinding[] bindings() default {};
}
