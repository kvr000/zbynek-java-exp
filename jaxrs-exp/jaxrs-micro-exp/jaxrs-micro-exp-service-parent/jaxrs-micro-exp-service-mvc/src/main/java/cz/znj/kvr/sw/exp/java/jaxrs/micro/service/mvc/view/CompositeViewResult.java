package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import net.dryuf.bigio.compare.FilenameVersionComparators;

import javax.ws.rs.core.Link;
import java.io.InputStream;
import java.util.List;
import java.util.Map;


@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
@Value
public class CompositeViewResult implements ViewResult
{
	int status;

	boolean isFinal;

	Map<String, Object> metadata;

	Map<String, Link> links;

	InputStream content;

	public static CompositeViewResult compose(ViewResult main, List<Map<String, Object>> metadata, List<Map<String, Link>> links, InputStream content)
	{
		return new CompositeViewResult(
			main.status(),
			main.isFinal(),
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
