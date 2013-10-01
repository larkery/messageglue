MessageGlue
===========

This is a library for gluing your java classes together using a message queue (pun, yes).

It uses a small handful of annotations to mark up interfaces for bridging over a network,
and then lets you easily serve up instances that you have or use those instances remotely
using a proxy synthesized at runtime.

Each bridged method should have a first parameter which is a "payload" type, an optional
synchronous return value, and then as many asynchronous return values as you like as extra
method arguments.

For example:

```java
@Declare(exchanges=@Exchange("my-exchange"))
public interface MyService {
  @UseExchange("my-exchange")
  public UUID startProcessing(final String inputString, final IAsyncOutput<Double> doubles);
}

// later on, on a "server" somewhere

final MyServiceImpl service = ...;

final IMessageGlue glue = Glue.create(errorHandler, channelProvider, objectMapper);

glue.bind(MyService.class, service);

glue.start();

// and then even later, on a "client" somewhere

final IMessageGlue glue = Glue.create(errorHandler, channelProvider, objectMapper);

final MyService client = glue.implement(MyService.class);

final UUID synchronousResult = client.startProcessing("someInput", asynchronousOutput);

// stuff passed into the async output on the server will be passed through to the client
```

The key annotations are in the export package, and should be documented.

Messages are serialized using Jackson, so you need to provide an ObjectMapper
which is configured in such a way that your message types can be serialized with it.
