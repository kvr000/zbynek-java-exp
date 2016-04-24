package cz.znj.kvr.sw.exp.java.process.jobrunner.util;

import lombok.AllArgsConstructor;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;


/**
 * Queue which allows reading by one consumer.
 */
public class SingleConsumerQueue<T>
{
	@SuppressWarnings("rawtypes")
	private static final AtomicReferenceFieldUpdater<SingleConsumerQueue, Node> STACK_UPDATER =
		AtomicReferenceFieldUpdater.newUpdater(SingleConsumerQueue.class, Node.class, "stack");

	private static final Node<?> LOCK = new Node<>(null, null);

	private final Runnable restarter;

	volatile private Node<T> stack;

	private Node<T> pending;

	/**
	 * Constructs new {@link SingleConsumerQueue}.
	 **
	 * @param restarter
	 * 	function to call when item is pending and no consumer is running.  Note it is run directly when either
	 * 	adding an item or closing the consumer, therefore it is supposed to schedule consumer asynchronously to
	 * 	avoid recursion.  It can be synchronous only when using {@link Consumer#nextOrClose()}.
	 */
	public SingleConsumerQueue(Runnable restarter)
	{
		this.restarter = restarter;
	}

	/**
	 * Consumes the pending item from queue.
	 *
	 * @return
	 * 	queue reader
	 */
	public Consumer consume()
	{
		return new Consumer();
	}

	/**
	 * Adds new item to queue.
	 *
	 * @param item
	 * 	item to be added
	 */
	public void add(T item)
	{
		Objects.requireNonNull(item, "item must not be null");
		for (;;) {
			Node<T> last = stack;
			Node<T> node = new Node<T>(item, last == LOCK ? null : last);
			if (STACK_UPDATER.compareAndSet(this, last, node)) {
				if (last == null) {
					restarter.run();
				}
				break;
			}
		}
	}

	/**
	 * Queue consumer.
	 */
	public class Consumer implements Closeable
	{
		private boolean closed = false;

		/**
		 * Reads next item from queue.
		 *
		 * @return
		 * 	next item from queue.
		 */
		public T next()
		{
			if (pending == null) {
				@SuppressWarnings("unchecked")
				Node<T> next = STACK_UPDATER.getAndSet(SingleConsumerQueue.this, LOCK);
				if (next == null)
					return null;
				Node<T> last = null;
				for (;;) {
					Node<T> previous = next.next;
					next.next = last;
					if (previous == null)
						break;
					last = next;
					next = previous;
				}
				if (pending == null) {
					pending = next;
				}
				else {
					Node<T> tail = pending;
					while (tail.next != null)
						tail = tail.next;
					tail.next = next;
				}
			}
			T item = pending.item;
			pending = pending.next;
			return item;
		}

		/**
		 * Returns next item or closes the consumer, so new consumer can start running.
		 *
		 * @return
		 * 	next item or null of consumer was closed.
		 */
		public T nextOrClose()
		{
			for (;;) {
				T next = next();
				if (next == null) {
					if (!STACK_UPDATER.compareAndSet(SingleConsumerQueue.this, LOCK, null)) {
						continue;
					}
					closed = true;
				}
				return next;
			}
		}

		/**
		 * @inheritDoc
		 *
		 * Closes the reader and unregisters the consumer.
		 */
		@Override
		public void close()
		{
			if (!closed) {
				closed = true;
				if (!STACK_UPDATER.compareAndSet(SingleConsumerQueue.this, LOCK, null)) {
					restarter.run();
				}
			}
		}
	}

	@AllArgsConstructor
	private static class Node<T>
	{
		final T item;

		Node<T> next;
	}
}
