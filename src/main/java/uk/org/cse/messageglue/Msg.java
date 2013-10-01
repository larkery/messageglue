package uk.org.cse.messageglue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use=Id.CLASS)
abstract class Msg {
	static class Error extends Msg {
		final String message;
		final Throwable cause;
		
		@JsonCreator
		public Error(
				@JsonProperty("message") String message, 
				@JsonProperty("cause") Throwable cause) {
			super();
			this.message = message;
			this.cause = cause;
		}

		public String getMessage() {
			return message;
		}
		
		@JsonTypeInfo(use=Id.CLASS)
		public Throwable getCause() {
			return cause;
		}
	}
	
	static class Close extends Msg {
		
	}
	
	static class Value<T> extends Msg {
		private final T value;

		@JsonCreator
		public Value(@JsonProperty("value") T value) {
			this.value = value;
		}
		
		public T getValue() {
			return value;
		}
	}
}
