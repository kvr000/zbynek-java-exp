package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import com.google.common.collect.ImmutableMap;
import jakarta.ws.rs.core.Link;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import net.dryuf.bigio.compare.FilenameVersionComparators;

import java.io.Reader;
import java.util.List;
import java.util.Map;


@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
@Value
public class CompositeWidget implements Widget
{
	int status;

	boolean isFinal;

	long size;

	Map<String, Object> metadata;

	Map<String, Link> links;

	Reader content;

	public static CompositeWidget compose(
		Widget main,
		List<Map<String, Object>> metadata,
		List<Map<String, Link>> links,
		Reader content)
	{
		return new CompositeWidget(
			main.status(),
			main.isFinal(),
			main.size(),
			metadata.stream().flatMap(map -> map.entrySet().stream())
				.collect(ImmutableMap.toImmutableMap(
					Map.Entry::getKey,
					Map.Entry::getValue,
					(a, b) -> a
				)),
			links.stream().flatMap(map -> map.entrySet().stream())
				.collect(ImmutableMap.toImmutableMap(
					Map.Entry::getKey,
					Map.Entry::getValue,
					(a, b) -> compareVersionedFilenames(a, b) <= 0 ? a : b
				)),
			content
		);
	}

	public static int compareVersionedFilenames(Link a, Link b)
	{
		String ap = a.getUri().getPath();
		String bp = b.getUri().getPath();

		return FilenameVersionComparators.FILENAME_ONLY_COMPARATOR.compare(ap, bp);
	}
}
