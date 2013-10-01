/**
 * <h1>
 * A utility library for serving up your classes on a message queue.
 * </h1>
 * <p>
 * 	The intention is that you define a common interface, which you want to serve up
 *  over a network, and mark that interface up with some annotations that indicate
 *  how it should be served. Then you use an {@link IMessageGlue} to serve implementations,
 *  or to create proxies which consume served implementations remotely.
 * </p>
 * <p>
 * 	Every method in a served interface ought to be annotated with {@link UseQueue} or {@link UseExchange},
 *  and should have a single "payload" as its first argument. Methods can return values directly (in which
 *  case they operate synchronously), or they can return values asynchronously by accepting {@link IAsyncOutput}
 *  arguments which are transparently bridged between client and server. 
 * </p>
 * 
 * <h2>The main parts</h2>
 * 
 * <ul>
 * 	<li>A bunch of annotations:
 * 		<ul>
 * 			<li>{@link Declare}, which you can use to specify queues or exchanges that you want declared
 *  			before anything else</li>
 * 			<li>{@link UseQueue}, which indicates that a method should consume from a named queue in servers</li>
 * 			<li>{@link UseExchange}, which indicates that a method should submit to a given exchange from
 * 				clients, and read from a queue bound to that exchange in servers, in the absence of {@link UseQueue}.</li>
 * 			<li>{@link Timeout}, which indicates that a synchronous method (with a return value) can timeout</li>
 * 			<li>{@link Multiple}, which indicates that a synchronous method can yield multiple values;
 * 				these values will be collected up from multiple servers</li>
 * 		</ul>
 *  </li>
 *  	<li>{@link uk.org.cse.messageglue.Glue}, which allows you to create an {@link IMessageGlue} implementation</li>
 *  	<li>{@link IMessageGlue}, which allows you to serve implementations of marked up interfaces onto the network,
 *  			or to create {@link java.lang.reflect.Proxy} based implementations of given interfaces which
 *  			will talk to served instances elsewhere.
 *  	</li>
 *  </ul>
 *  
 *  <h2>Requirements</h2>
 *  <p>When you construct an {@link IMessageGlue} it will expect an {@link ObjectMapper} to transform objects
 *  which are being sent over the wire; consequently, anything which you use as a message type, return type, or 
 *  asynchronous return type must be reliably serializable and deserializable using that object mapper.
 *  </p>
 *  
 *  <h2>Behaviour</h2>
 *  <p>Each "served" instance gets a channel <em>per method</em>. Consequently, the ordering of method
 *  	calls may not be consistent. Your server should probably not have complex ordering requirements
 *  	between methods<p>
 *  <p>Each call to a served method also gets a single channel; every communication is transmitted on that
 *  	channel, so the sequencing of return values is preserved across the network</p>
 *  <p>Return values are conveyed over a temporary queue; method calls are transmitted with a reply-to
 *  	for that temporary queue.</p>
 *  <p>Asynchronous outputs are closeable; the channel and queue will remain live until <em>all</em> of the
 *  	<em>server-side</em> asynchronous outputs have been closed</p>
 */
package uk.org.cse.messageglue.export;

import com.fasterxml.jackson.databind.ObjectMapper;

