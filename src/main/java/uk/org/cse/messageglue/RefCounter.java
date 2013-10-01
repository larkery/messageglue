package uk.org.cse.messageglue;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.rabbitmq.client.AMQP.Basic.RecoverOk;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Channel.FlowOk;
import com.rabbitmq.client.AMQP.Exchange.BindOk;
import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.AMQP.Exchange.DeleteOk;
import com.rabbitmq.client.AMQP.Exchange.UnbindOk;
import com.rabbitmq.client.AMQP.Queue.PurgeOk;
import com.rabbitmq.client.AMQP.Tx.CommitOk;
import com.rabbitmq.client.AMQP.Tx.RollbackOk;
import com.rabbitmq.client.AMQP.Tx.SelectOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.FlowListener;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.Method;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

class RefCountedChannel implements AutoCloseable, Channel {
	private final Channel channel;
	private final AtomicInteger counter;
	
	public RefCountedChannel(Channel closeable, int counter) {
		this.channel = closeable;
		this.counter = new AtomicInteger(counter);
	}
	
	@Override
	public void close() throws IOException {
		if (counter.decrementAndGet() <= 0) {
			if (channel.isOpen()) {
				channel.close();
			}
		}
	}

	public void addShutdownListener(ShutdownListener listener) {
		channel.addShutdownListener(listener);
	}

	public void removeShutdownListener(ShutdownListener listener) {
		channel.removeShutdownListener(listener);
	}

	public ShutdownSignalException getCloseReason() {
		return channel.getCloseReason();
	}

	public void notifyListeners() {
		channel.notifyListeners();
	}

	public boolean isOpen() {
		return channel.isOpen();
	}

	public int getChannelNumber() {
		return channel.getChannelNumber();
	}

	public Connection getConnection() {
		return channel.getConnection();
	}

	public void close(int closeCode, String closeMessage) throws IOException {
		channel.close(closeCode, closeMessage);
	}

	public FlowOk flow(boolean active) throws IOException {
		return channel.flow(active);
	}

	public FlowOk getFlow() {
		return channel.getFlow();
	}

	public void abort() throws IOException {
		channel.abort();
	}

	public void abort(int closeCode, String closeMessage) throws IOException {
		channel.abort(closeCode, closeMessage);
	}

	public void addReturnListener(ReturnListener listener) {
		channel.addReturnListener(listener);
	}

	public boolean removeReturnListener(ReturnListener listener) {
		return channel.removeReturnListener(listener);
	}

	public void clearReturnListeners() {
		channel.clearReturnListeners();
	}

	public void addFlowListener(FlowListener listener) {
		channel.addFlowListener(listener);
	}

	public boolean removeFlowListener(FlowListener listener) {
		return channel.removeFlowListener(listener);
	}

	public void clearFlowListeners() {
		channel.clearFlowListeners();
	}

	public void addConfirmListener(ConfirmListener listener) {
		channel.addConfirmListener(listener);
	}

	public boolean removeConfirmListener(ConfirmListener listener) {
		return channel.removeConfirmListener(listener);
	}

	public void clearConfirmListeners() {
		channel.clearConfirmListeners();
	}

	public Consumer getDefaultConsumer() {
		return channel.getDefaultConsumer();
	}

	public void setDefaultConsumer(Consumer consumer) {
		channel.setDefaultConsumer(consumer);
	}

	public void basicQos(int prefetchSize, int prefetchCount, boolean global)
			throws IOException {
		channel.basicQos(prefetchSize, prefetchCount, global);
	}

	public void basicQos(int prefetchCount) throws IOException {
		channel.basicQos(prefetchCount);
	}

	public void basicPublish(String exchange, String routingKey,
			BasicProperties props, byte[] body) throws IOException {
		channel.basicPublish(exchange, routingKey, props, body);
	}

	public void basicPublish(String exchange, String routingKey,
			boolean mandatory, BasicProperties props, byte[] body)
			throws IOException {
		channel.basicPublish(exchange, routingKey, mandatory, props, body);
	}

	public void basicPublish(String exchange, String routingKey,
			boolean mandatory, boolean immediate, BasicProperties props,
			byte[] body) throws IOException {
		channel.basicPublish(exchange, routingKey, mandatory, immediate,
				props, body);
	}

	public DeclareOk exchangeDeclare(String exchange, String type)
			throws IOException {
		return channel.exchangeDeclare(exchange, type);
	}

	public DeclareOk exchangeDeclare(String exchange, String type,
			boolean durable) throws IOException {
		return channel.exchangeDeclare(exchange, type, durable);
	}

	public DeclareOk exchangeDeclare(String exchange, String type,
			boolean durable, boolean autoDelete, Map<String, Object> arguments)
			throws IOException {
		return channel.exchangeDeclare(exchange, type, durable, autoDelete,
				arguments);
	}

	public DeclareOk exchangeDeclare(String exchange, String type,
			boolean durable, boolean autoDelete, boolean internal,
			Map<String, Object> arguments) throws IOException {
		return channel.exchangeDeclare(exchange, type, durable, autoDelete,
				internal, arguments);
	}

	public DeclareOk exchangeDeclarePassive(String name) throws IOException {
		return channel.exchangeDeclarePassive(name);
	}

	public DeleteOk exchangeDelete(String exchange, boolean ifUnused)
			throws IOException {
		return channel.exchangeDelete(exchange, ifUnused);
	}

	public DeleteOk exchangeDelete(String exchange) throws IOException {
		return channel.exchangeDelete(exchange);
	}

	public BindOk exchangeBind(String destination, String source,
			String routingKey) throws IOException {
		return channel.exchangeBind(destination, source, routingKey);
	}

	public BindOk exchangeBind(String destination, String source,
			String routingKey, Map<String, Object> arguments)
			throws IOException {
		return channel.exchangeBind(destination, source, routingKey,
				arguments);
	}

	public UnbindOk exchangeUnbind(String destination, String source,
			String routingKey) throws IOException {
		return channel.exchangeUnbind(destination, source, routingKey);
	}

	public UnbindOk exchangeUnbind(String destination, String source,
			String routingKey, Map<String, Object> arguments)
			throws IOException {
		return channel.exchangeUnbind(destination, source, routingKey,
				arguments);
	}

	public com.rabbitmq.client.AMQP.Queue.DeclareOk queueDeclare()
			throws IOException {
		return channel.queueDeclare();
	}

	public com.rabbitmq.client.AMQP.Queue.DeclareOk queueDeclare(String queue,
			boolean durable, boolean exclusive, boolean autoDelete,
			Map<String, Object> arguments) throws IOException {
		return channel.queueDeclare(queue, durable, exclusive, autoDelete,
				arguments);
	}

	public com.rabbitmq.client.AMQP.Queue.DeclareOk queueDeclarePassive(
			String queue) throws IOException {
		return channel.queueDeclarePassive(queue);
	}

	public com.rabbitmq.client.AMQP.Queue.DeleteOk queueDelete(String queue)
			throws IOException {
		return channel.queueDelete(queue);
	}

	public com.rabbitmq.client.AMQP.Queue.DeleteOk queueDelete(String queue,
			boolean ifUnused, boolean ifEmpty) throws IOException {
		return channel.queueDelete(queue, ifUnused, ifEmpty);
	}

	public com.rabbitmq.client.AMQP.Queue.BindOk queueBind(String queue,
			String exchange, String routingKey) throws IOException {
		return channel.queueBind(queue, exchange, routingKey);
	}

	public com.rabbitmq.client.AMQP.Queue.BindOk queueBind(String queue,
			String exchange, String routingKey, Map<String, Object> arguments)
			throws IOException {
		return channel.queueBind(queue, exchange, routingKey, arguments);
	}

	public com.rabbitmq.client.AMQP.Queue.UnbindOk queueUnbind(String queue,
			String exchange, String routingKey) throws IOException {
		return channel.queueUnbind(queue, exchange, routingKey);
	}

	public com.rabbitmq.client.AMQP.Queue.UnbindOk queueUnbind(String queue,
			String exchange, String routingKey, Map<String, Object> arguments)
			throws IOException {
		return channel.queueUnbind(queue, exchange, routingKey, arguments);
	}

	public PurgeOk queuePurge(String queue) throws IOException {
		return channel.queuePurge(queue);
	}

	public GetResponse basicGet(String queue, boolean autoAck)
			throws IOException {
		return channel.basicGet(queue, autoAck);
	}

	public void basicAck(long deliveryTag, boolean multiple) throws IOException {
		channel.basicAck(deliveryTag, multiple);
	}

	public void basicNack(long deliveryTag, boolean multiple, boolean requeue)
			throws IOException {
		channel.basicNack(deliveryTag, multiple, requeue);
	}

	public void basicReject(long deliveryTag, boolean requeue)
			throws IOException {
		channel.basicReject(deliveryTag, requeue);
	}

	public String basicConsume(String queue, Consumer callback)
			throws IOException {
		return channel.basicConsume(queue, callback);
	}

	public String basicConsume(String queue, boolean autoAck, Consumer callback)
			throws IOException {
		return channel.basicConsume(queue, autoAck, callback);
	}

	public String basicConsume(String queue, boolean autoAck,
			String consumerTag, Consumer callback) throws IOException {
		return channel.basicConsume(queue, autoAck, consumerTag, callback);
	}

	public String basicConsume(String queue, boolean autoAck,
			String consumerTag, boolean noLocal, boolean exclusive,
			Map<String, Object> arguments, Consumer callback)
			throws IOException {
		return channel.basicConsume(queue, autoAck, consumerTag, noLocal,
				exclusive, arguments, callback);
	}

	public void basicCancel(String consumerTag) throws IOException {
		channel.basicCancel(consumerTag);
	}

	public RecoverOk basicRecover() throws IOException {
		return channel.basicRecover();
	}

	public RecoverOk basicRecover(boolean requeue) throws IOException {
		return channel.basicRecover(requeue);
	}

	@SuppressWarnings("deprecation")
	public void basicRecoverAsync(boolean requeue) throws IOException {
		channel.basicRecoverAsync(requeue);
	}

	public SelectOk txSelect() throws IOException {
		return channel.txSelect();
	}

	public CommitOk txCommit() throws IOException {
		return channel.txCommit();
	}

	public RollbackOk txRollback() throws IOException {
		return channel.txRollback();
	}

	public com.rabbitmq.client.AMQP.Confirm.SelectOk confirmSelect()
			throws IOException {
		return channel.confirmSelect();
	}

	public long getNextPublishSeqNo() {
		return channel.getNextPublishSeqNo();
	}

	public boolean waitForConfirms() throws InterruptedException {
		return channel.waitForConfirms();
	}

	public boolean waitForConfirms(long timeout) throws InterruptedException,
			TimeoutException {
		return channel.waitForConfirms(timeout);
	}

	public void waitForConfirmsOrDie() throws IOException, InterruptedException {
		channel.waitForConfirmsOrDie();
	}

	public void waitForConfirmsOrDie(long timeout) throws IOException,
			InterruptedException, TimeoutException {
		channel.waitForConfirmsOrDie(timeout);
	}

	public void asyncRpc(Method method) throws IOException {
		channel.asyncRpc(method);
	}

	public Command rpc(Method method) throws IOException {
		return channel.rpc(method);
	}

	public void closeImmediately() throws IOException {
		counter.set(0);
		channel.close();
	}


}
