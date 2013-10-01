package uk.org.cse.messageglue.export;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

public @interface QueueBinding {
	public String value();
	public String routingKey() default "";
}
