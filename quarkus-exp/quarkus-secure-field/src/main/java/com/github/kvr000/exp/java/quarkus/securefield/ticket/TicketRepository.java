package com.github.kvr000.exp.java.quarkus.securefield.ticket;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;


/**
 * In-memory ticket storage.  Returns copies, so callers cannot modify stored objects.
 */
@ApplicationScoped
public class TicketRepository
{
	private final ConcurrentMap<Long, Ticket> tickets = new ConcurrentHashMap<>();

	private final AtomicLong sequence = new AtomicLong();

	public TicketRepository()
	{
		create(Ticket.builder().system("billing").title("Invoice not generated").views(3)
			.description("Monthly invoice for customer 42 is missing.").build());
		create(Ticket.builder().system("backup").title("Backup job failing").views(10)
			.description("Nightly backup fails with timeout since Monday.").build());
		create(Ticket.builder().system("portal").title("Typo on login page").views(0)
			.description("'Pasword' instead of 'Password'.").build());
	}

	public List<Ticket> list()
	{
		return tickets.values().stream()
			.sorted(Comparator.comparing(Ticket::getId))
			.map(Ticket::copy)
			.toList();
	}

	public Optional<Ticket> get(long id)
	{
		return Optional.ofNullable(tickets.get(id)).map(Ticket::copy);
	}

	/** Returns ticket and increments its view counter. */
	public Optional<Ticket> view(long id)
	{
		return update(id, t -> {
			t.setViews(t.getViews() + 1);
			return t;
		});
	}

	public Ticket create(Ticket ticket)
	{
		Ticket stored = ticket.toBuilder()
			.id(sequence.incrementAndGet())
			.build();
		tickets.put(stored.getId(), stored);
		return stored.copy();
	}

	/**
	 * Atomically updates the ticket.  The updater receives a copy, an exception thrown from it leaves the ticket
	 * unchanged.
	 */
	public Optional<Ticket> update(long id, UnaryOperator<Ticket> updater)
	{
		Ticket updated = tickets.computeIfPresent(id, (key, old) -> {
			Ticket result = updater.apply(old.copy());
			result.setId(key);
			return result;
		});
		return Optional.ofNullable(updated).map(Ticket::copy);
	}
}
