MessageGlue
===========

A library which allows you to automatically connect a RabbitMQ up to some classes. It's kind of like a very lightweight restricted form of RMI

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
