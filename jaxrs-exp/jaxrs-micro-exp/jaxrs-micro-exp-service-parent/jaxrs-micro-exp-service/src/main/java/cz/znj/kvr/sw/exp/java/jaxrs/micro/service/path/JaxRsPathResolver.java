package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.path;

import com.google.common.annotations.VisibleForTesting;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.util.StringPart;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * JaxRs path parser and resolver. It supports part of JaxRs specification which should be generally enough to implement
 * common REST services.
 *
 * For example, it does not do matching path params in part of segments. It does not support matching across several
 * segments as it is not useful anyway and would lead into non-unique and error prone URLs. It does support matching
 * across properly encoded separator though (which does not suffer from the above).
 */
@RequiredArgsConstructor
public class JaxRsPathResolver<C, H extends Predicate<C>> implements PathResolver<C, H>
{
	@Override
	public Match<H> resolvePath(String path, C context)
	{
		return processSubMatch(new StringPart(path), context, rootNode, true);
	}

	public static class Builder<C, H extends Predicate<C>>
	{
		@Setter()
		@Accessors(fluent = true)
		private char separator = '/';

		@Setter()
		@Accessors(fluent = true)
		private char placeholderStart = '{';

		@Setter()
		@Accessors(fluent = true)
		private char placeholderEnd = '}';

		@Setter
		@Accessors
		private Function<StringPart , StringPart> decoder = JaxRsPathResolver::decodeUrl;

		public Builder()
		{
		}

		public Builder<C, H> registerPath(String path, H handler)
		{
			Node<H> currentNode = rootNode;
			StringPart sp = new StringPart(path.toCharArray());
			int restart = 0;
			boolean endSeparator = true;
			for (int cp = restart; ; ) {
				if (cp == path.length()) {
					if (cp > restart) {
						currentNode = currentNode.exactSubs.computeIfAbsent(sp.subpart(restart), Node::createNew);
						endSeparator = false;
					}
					break;
				}
				else if (path.charAt(cp) == separator) {
					currentNode = currentNode.exactSubs.computeIfAbsent(sp.subpart(restart, cp), Node::createNew);
					restart = cp+1;
					cp = restart;
					endSeparator = true;
				}
				else if (path.charAt(cp) == placeholderStart) {
					if (cp > 0 && path.charAt(cp-1) != separator) {
						throw new IllegalArgumentException("Placeholder expected right after separator, at "+cp);
					}
					int end = path.indexOf(placeholderEnd, cp+1);
					if (end < 0) {
						throw new IllegalArgumentException("Cannot find placeholder end starting at "+cp+": "+path);
					}
					if (end+1 != path.length() && path.charAt(end+1) != separator) {
						throw new IllegalArgumentException("Placeholder must be followed by separator or at the end: "+path);
					}
					String placeholder = path.substring(cp+1, end);
					++end;
					Node<H> parrentNode = currentNode;
					if (end < path.length()) {
						++end;
						endSeparator = true;
					}
					else {
						endSeparator = false;
					}
					try {
						currentNode = currentNode.placeholderSubs.computeIfAbsent(placeholder, Node::createFromPlaceholder);
					}
					catch (IllegalArgumentException ex) {
						throw new IllegalArgumentException("Failed parsing placeholder '"+placeholder+"' in path: "+path, ex);
					}
					if (!endSeparator && currentNode.placeholderPattern != null) {
						currentNode.handlersConsumers = mergeIntoList(currentNode.handlersConsumers, handler);
						if (currentNode.placeholderPattern.matcher("").matches()) {
							// special case, if path accepts empty string, add to list of consumers on parent node too
							parrentNode.handlersEmptyConsumers = mergeIntoList(parrentNode.handlersEmptyConsumers, new NamedHandler<>(currentNode.placeholderName, handler));
						}
						return this;
					}
					cp = restart = end;
				}
				else {
					++cp;
				}
			}
			if (currentNode.path == null) {
				currentNode.path = path;
			}
			if (endSeparator) {
				if (currentNode.handlersWithSeparator == null) {
					currentNode.handlersWithSeparator = new ArrayList<>();
				}
				currentNode.handlersWithSeparator = mergeIntoList(currentNode.handlersWithSeparator, handler);
			}
			else {
				if (currentNode.handlersNoSeparator == null) {
					currentNode.handlersNoSeparator = new ArrayList<>();
				}
				currentNode.handlersNoSeparator = mergeIntoList(currentNode.handlersNoSeparator, handler);
			}
			return this;
		}

		public String concatPaths(String first, String second)
		{
			if (!first.isEmpty() && first.charAt(0) == separator) {
				first = first.substring(1);
			}
			if (first.isEmpty() || first.charAt(first.length()-1) == separator) {
				if (!second.isEmpty() && second.charAt(0) == separator) {
					return first+second.substring(1);
				}
				else {
					return first+second;
				}
			}
			else {
				if (!second.isEmpty() && second.charAt(0) == separator) {
					return first+second;
				}
				else if (second.isEmpty()) {
					return first;
				}
				else {
					return first+separator+second;
				}
			}
		}

		public JaxRsPathResolver<C, H> build()
		{
			return new JaxRsPathResolver<>(separator, decoder, rootNode);
		}

		private Node<H> rootNode = new Node<>();
	}

	private MatchImpl<H> processSubMatch(StringPart sp, C context, Node<H> parentNode, boolean hasSeparator)
	{
		if (sp.length() == 0) {
			H handler;
			NamedHandler<H> namedHandler;
			if (hasSeparator) {
				if ((handler = parentNode.handlersWithSeparator == null ? null : findMatchingHandler(parentNode.handlersWithSeparator, context)) != null) {
					return new MatchImpl<>(
							handler,
							Collections.emptyMap(),
							0,
							true,
							parentNode.path,
							MatchImpl.PRIO_FULL
					);
				}
				else if ((handler = parentNode.handlersNoSeparator == null ? null : findMatchingHandler(parentNode.handlersNoSeparator, context)) != null) {
					return new MatchImpl<>(
							handler,
							Collections.emptyMap(),
							1,
							true,
							parentNode.path,
							MatchImpl.PRIO_BAD_SEP
					);
				}
				else if ((namedHandler = parentNode.handlersEmptyConsumers == null ? null: findMatchingNamedHandler(parentNode.handlersEmptyConsumers, context)) != null) {
					return new MatchImpl<>(
							handler,
							Collections.singletonMap(namedHandler.placeholderName, ""),
							0,
							true,
							parentNode.path,
							MatchImpl.PRIO_FULL
					);
				}
			}
			else {
				if ((handler = parentNode.handlersNoSeparator == null ? null : findMatchingHandler(parentNode.handlersNoSeparator, context)) != null) {
					return new MatchImpl<>(
							handler,
							Collections.emptyMap(),
							0,
							true,
							parentNode.path,
							MatchImpl.PRIO_FULL
					);
				}
				else if ((handler = parentNode.handlersWithSeparator == null ? null : findMatchingHandler(parentNode.handlersWithSeparator, context)) != null) {
					return new MatchImpl<>(
							handler,
							Collections.emptyMap(),
							-1,
							true,
							parentNode.path,
							MatchImpl.PRIO_BAD_SEP
					);
				}
				else if ((namedHandler = parentNode.handlersEmptyConsumers == null ? null: findMatchingNamedHandler(parentNode.handlersEmptyConsumers, context)) != null) {
					return new MatchImpl<>(
							handler,
							Collections.singletonMap(namedHandler.placeholderName, ""),
							-1,
							true,
							parentNode.path,
							MatchImpl.PRIO_BAD_SEP
					);
				}
			}
			if (parentNode.handlersWithSeparator != null || parentNode.handlersNoSeparator != null) {
				return new MatchImpl<>(
						null,
						Collections.emptyMap(),
						1,
						true,
						parentNode.path,
						MatchImpl.PRIO_UNMET_CONDITION
				);
			}
			return null;
		}
		int sep = sp.indexOf(separator, 0);
		int end = sep;
		if (sep < 0) {
			end = sp.length();
		}
		StringPart value = decoder.apply(sp.subpart(0, end));
		StringPart remaining = sep < 0 ? StringPart.EMPTY : sp.subpart(sep+1);
		Node<H> exactNode = parentNode.exactSubs.get(value);

		MatchImpl<H> bestMatch = null;
		if (exactNode != null) {
			bestMatch = processSubMatch(remaining, context, exactNode, sep >= 0);
			if (bestMatch != null && bestMatch.matchesFully()) {
				return bestMatch;
			}
		}

		for (Node<H> placeholder: parentNode.placeholderSubs.values()) {
			if (placeholder.placeholderPattern != null) {
				if (!placeholder.placeholderPattern.matcher(value).matches()) {
					MatchImpl<H> subMatch = processSubMatch(remaining, context, placeholder, sep >= 0);
					if (subMatch != null) {
						MatchImpl<H> match = new MatchImpl<>(
								subMatch.handler(),
								mergeIntoMap(subMatch.placeholderValues, placeholder.placeholderName, value.toString()),
								subMatch.separatorStatus,
								subMatch.matchesFully,
								subMatch.pattern,
								subMatch.priority
						);
						if (match.priority == 0) {
							return match;
						}
						if (bestMatch == null || match.priority < bestMatch.priority) {
							bestMatch = match;
						}
					}
				}
				if (placeholder.handlersConsumers != null && placeholder.placeholderPattern.matcher(decoder.apply(sp)).matches()) {
					H handler = findMatchingHandler(placeholder.handlersConsumers, context);
					if (handler != null) {
						if (bestMatch == null || bestMatch.priority > MatchImpl.PRIO_CONSUMER) {
							bestMatch = new MatchImpl<>(
									handler,
									Collections.singletonMap(placeholder.placeholderName, decoder.apply(sp).toString()),
									0,
									true,
									placeholder.path,
									MatchImpl.PRIO_CONSUMER
							);
						}
					}
					else if (bestMatch == null || bestMatch.priority > MatchImpl.PRIO_UNMET_CONDITION) {
						bestMatch = new MatchImpl<>(
								null,
								Collections.singletonMap(placeholder.placeholderName, decoder.apply(sp).toString()),
								0,
								false,
								placeholder.path,
								MatchImpl.PRIO_UNMET_CONDITION
						);
					}
				}
			}
			else {
				MatchImpl<H> subMatch = processSubMatch(remaining, context, placeholder, sep >= 0);
				if (subMatch != null) {
					MatchImpl<H> match = new MatchImpl<>(
							subMatch.handler(),
							mergeIntoMap(subMatch.placeholderValues, placeholder.placeholderName, value.toString()),
							subMatch.separatorStatus,
							subMatch.matchesFully,
							subMatch.pattern,
							subMatch.priority
					);
					if (match.priority == 0) {
						return match;
					}
					if (bestMatch == null || match.priority < bestMatch.priority) {
						bestMatch = match;
					}
				}
			}
		}
		return bestMatch;
	}

	private H findMatchingHandler(List<H> handlers, C context)
	{
		for (H handler: handlers) {
			if (handler.test(context)) {
				return handler;
			}
		}
		return null;
	}

	private NamedHandler<H> findMatchingNamedHandler(List<NamedHandler<H>> handlers, C context)
	{
		for (NamedHandler<H> handler: handlers) {
			if (handler.handler.test(context)) {
				return handler;
			}
		}
		return null;
	}

	private static <K, V> Map<K, V> mergeIntoMap(Map<K, V> map, K key, V value)
	{
		if (map == null || map.isEmpty()) {
			map = Collections.singletonMap(key, value);
		}
		else {
			if (map.size() == 1) {
				map = new HashMap<>(map);
			}
			map.put(key, value);
		}
		return map;
	}

	private static <V> List<V> mergeIntoList(List<V> list, V value)
	{
		if (list == null || list.isEmpty()) {
			list = Collections.singletonList(value);
		}
		else {
			if (list.size() == 1) {
				list = new ArrayList<>(list);
			}
			list.add(value);
		}
		return list;
	}

	private static StringPart decodeUrl(StringPart s)
	{
		if (s.indexOf('%') < 0) {
			return s;
		}
		try {
			return new StringPart(URLDecoder.decode(s.toString(), "UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	static class Node<H>
	{
		public static <O, H> Node<H> createNew(O o)
		{
			return new Node<>();
		}

		public static <H> Node<H> createFromPlaceholder(String placeholder)
		{
			Node<H> node = new Node<H>();
			if (placeholder.indexOf(':') >= 0) {
				Matcher match = placeholderRegexp.matcher(placeholder);
				if (!match.matches()) {
					throw new IllegalArgumentException("Cannot match placeholder for "+placeholderRegexp.pattern()+": "+placeholder);
				}
				node.placeholderName = match.group(1);
				node.placeholderPattern = Pattern.compile(match.group(2), Pattern.DOTALL);
			}
			else {
				node.placeholderName = placeholder;
			}
			return node;
		}

		List<H> handlersWithSeparator;

		List<H> handlersNoSeparator;

		List<H> handlersConsumers;

		List<NamedHandler<H>> handlersEmptyConsumers;

		Map<StringPart, Node<H>> exactSubs = new HashMap<>();

		Map<String, Node<H>> placeholderSubs = new LinkedHashMap<>();

		String placeholderName;

		Pattern placeholderPattern;

		String path;

		private static final Pattern placeholderRegexp = Pattern.compile("^(\\w+)\\s*:\\s*(.*)$");
	}

	@Getter
	@Accessors(fluent = true)
	static class MatchImpl<H> implements Match<H>
	{
		public static final int PRIO_FULL = 0;
		public static final int PRIO_BAD_SEP = 1;
		public static final int PRIO_CONSUMER = 2;
		public static final int PRIO_UNMET_CONDITION = 3;

		MatchImpl(
				H handler,
				Map<String, String> placeholderValues,
				int separatorStatus,
				boolean matchesFully,
				String pattern,
				int priority
		)
		{
			this.handler = handler;
			this.placeholderValues = placeholderValues;
			this.separatorStatus = separatorStatus;
			this.matchesFully = matchesFully;
			this.pattern = pattern;
			this.priority = priority;
		}

		private final H handler;

		private final Map<String, String> placeholderValues;

		@Accessors(fluent = true)
		private final int separatorStatus;

		@Accessors(fluent = true)
		private final boolean matchesFully;

		private final String pattern;

		private final int priority;
	}

	@RequiredArgsConstructor
	private static class NamedHandler<H>
	{
		private final String placeholderName;

		private final H handler;
	}

	@VisibleForTesting
	final char separator;

	@VisibleForTesting
	final Function<StringPart, StringPart> decoder;

	@VisibleForTesting
	final Node<H> rootNode;
}
