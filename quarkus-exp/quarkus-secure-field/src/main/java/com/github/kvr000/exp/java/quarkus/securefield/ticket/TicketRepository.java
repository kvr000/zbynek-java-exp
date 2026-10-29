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
		create(new Ticket(null, "billing", "Invoice not generated", 3, "Monthly invoice for customer 42 is missing."));
		create(new Ticket(null, "backup", "Backup job failing", 10, "Nightly backup fails with timeout since Monday."));
		create(new Ticket(null, "portal", "Typo on login page", 0, "'Pasword' instead of 'Password'."));
	}

	public List<Ticket> list()
	{
		return tickets.values().stream()
			.sorted(Comparator.comparing(t -> t.id))
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
			++t.views;
			return t;
		});
	}

	public Ticket create(Ticket ticket)
	{
		Ticket stored = ticket.copy();
		stored.id = sequence.incrementAndGet();
		tickets.put(stored.id, stored);
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
			result.id = key;
			return result;
		});
		return Optional.ofNullable(updated).map(Ticket::copy);
	}
}
