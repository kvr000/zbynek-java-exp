package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.path;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.util.StringPart;
import lombok.Value;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;


/**
 *
 */
public class JaxRsPathResolverTest
{
	@Test
	public void testConstructionExact()
	{
		Fixture fixture = new Fixture(Arrays.asList("", "path/", "other/", "other/sub", "nonslashed"));
		Assert.assertEquals(fixture.pathResolver.rootNode.handlersWithSeparator, Arrays.asList(Handler.of("")));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.size(), 3);
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("path")).handlersWithSeparator, Arrays.asList(Handler.of("path/")));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("other")).handlersWithSeparator, Arrays.asList(Handler.of("other/")));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("nonslashed")).handlersNoSeparator, Arrays.asList(Handler.of("nonslashed")));
	}

	@Test
	public void testConstructionPlaceholder()
	{
		Fixture fixture = new Fixture(Arrays.asList("", "{pcrootslashed}/", "{pcrootnonslashed}", "sub/{slashed}/", "sub/{nonslashed}"));
		Assert.assertEquals(fixture.pathResolver.rootNode.handlersWithSeparator, Arrays.asList(Handler.of("")));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.size(), 1);
		Assert.assertEquals(fixture.pathResolver.rootNode.placeholderSubs.get("pcrootslashed").handlersWithSeparator, Arrays.asList(Handler.of("{pcrootslashed}/")));
		Assert.assertEquals(fixture.pathResolver.rootNode.placeholderSubs.get("pcrootnonslashed").handlersNoSeparator, Arrays.asList(Handler.of("{pcrootnonslashed}")));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("sub")).handlersWithSeparator, null);
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("sub")).handlersNoSeparator, null);
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("sub")).placeholderSubs.get("slashed").handlersWithSeparator, Arrays.asList(Handler.of("sub/{slashed}/")));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("sub")).placeholderSubs.get("nonslashed").handlersNoSeparator, Arrays.asList(Handler.of("sub/{nonslashed}")));
	}

	@Test
	public void testConstructionPattern()
	{
		Fixture fixture = new Fixture(Arrays.asList("users/-new-/", "users/{id: \\d+}/"));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.size(), 1);
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("users")).handlersWithSeparator, null);
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("users")).handlersNoSeparator, null);
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("users")).exactSubs.get(new StringPart("-new-")).handlersWithSeparator, Arrays.asList(Handler.of("users/-new-/")));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("users")).exactSubs.get(new StringPart("-new-")).handlersNoSeparator, null);
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("users")).placeholderSubs.get("id: \\d+").handlersWithSeparator, Arrays.asList(Handler.of("users/{id: \\d+}/")));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("users")).placeholderSubs.get("id: \\d+").handlersNoSeparator, null);
	}

	@Test
	public void testConstructionConsumer()
	{
		Fixture fixture = new Fixture(Arrays.asList("file/{path: .+}"));
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.size(), 1);
		Assert.assertNotNull(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("file")).placeholderSubs.get("path: .+"), "placeholder {path: .+} must exist");
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("file")).placeholderSubs.get("path: .+").handlersWithSeparator, null);
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("file")).placeholderSubs.get("path: .+").handlersNoSeparator, null);
		Assert.assertEquals(fixture.pathResolver.rootNode.exactSubs.get(new StringPart("file")).placeholderSubs.get("path: .+").handlersConsumers, Arrays.asList(Handler.of("file/{path: .+}")));
	}

	@Test
	public void testResolveRoot()
	{
		Fixture fixture = standardResolveFixture();

		PathResolver.Match<Handler> mroot = fixture.pathResolver.resolvePath("", null);
		Assert.assertEquals(mroot.placeholderValues().size(), 0);
		Assert.assertEquals(mroot.handler(), Handler.of(""));
	}

	@Test
	public void testResolveNonexistent()
	{
		Fixture fixture = standardResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("other/nonexistent", null);
		Assert.assertNull(m);
	}

	@Test
	public void testResolveUnfinished()
	{
		Fixture fixture = standardResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("other", null);
		Assert.assertNull(m);
		PathResolver.Match<Handler> ms = fixture.pathResolver.resolvePath("other/", null);
		Assert.assertNull(ms);
	}

	@Test
	public void testResolvePlaceholder()
	{
		Fixture fixture = standardResolveFixture();

		// 800ns per lookup
		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("sub/value/", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.placeholderValues(), Collections.singletonMap("slashed", "value"));
		Assert.assertEquals(m.matchesFully(), true);
		Assert.assertEquals(m.separatorStatus(), 0);
	}

	@Test
	public void testResolveConsumer()
	{
		Fixture fixture = standardResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("sub/value/with/more", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.placeholderValues(), Collections.singletonMap("consumer", "value/with/more"));
		Assert.assertEquals(m.separatorStatus(), 0);
	}

	@Test
	public void testResolveEmptyConsumer()
	{
		Fixture fixture = standardResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("consumer/", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.placeholderValues(), Collections.singletonMap("consumer", ""));
		Assert.assertEquals(m.separatorStatus(), 0);
	}

	@Test
	public void testResolveUnfinishedConsumer()
	{
		Fixture fixture = standardResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("consumer", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.placeholderValues(), Collections.singletonMap("consumer", ""));
		Assert.assertEquals(m.separatorStatus(), -1);
	}

	@Test
	public void testResolveDecodingPlaceholder()
	{
		Fixture fixture = standardResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("sub/value%2f/", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.placeholderValues(), Collections.singletonMap("slashed", "value/"));
	}

	@Test
	public void testResolveDecodingConsumer()
	{
		Fixture fixture = standardResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("sub/value%2f/a/b", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.placeholderValues(), Collections.singletonMap("consumer", "value//a/b"));
	}

	@Test
	public void testResolveExpectedSep()
	{
		Fixture fixture = slashNoSlashResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("slash/", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.handler(), Handler.of("slash/"));
		Assert.assertEquals(m.separatorStatus(), 0);
	}

	@Test
	public void testResolveExpectedNoSep()
	{
		Fixture fixture = slashNoSlashResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("noslash", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.handler(), Handler.of("noslash"));
		Assert.assertEquals(m.separatorStatus(), 0);
	}

	@Test
	public void testResolveMissingSep()
	{
		Fixture fixture = slashNoSlashResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("slash", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.handler(), Handler.of("slash/"));
		Assert.assertEquals(m.separatorStatus(), -1);
	}

	@Test
	public void testResolveExtraSep()
	{
		Fixture fixture = slashNoSlashResolveFixture();

		PathResolver.Match<Handler> m = fixture.pathResolver.resolvePath("noslash/", null);
		Assert.assertNotNull(m);
		Assert.assertEquals(m.handler(), Handler.of("noslash"));
		Assert.assertEquals(m.separatorStatus(), 1);
	}

	private Fixture standardResolveFixture()
	{
		return new Fixture(Arrays.asList("", "sub/{consumer: .+}", "sub/{nonslashed}", "sub/{slashed}/", "other/more/", "consumer/{consumer: .*}"));
	}

	private Fixture slashNoSlashResolveFixture()
	{
		return new Fixture(Arrays.asList("slash/", "noslash"));
	}

	private static class Fixture
	{
		final JaxRsPathResolver<Object, JaxRsPathResolverTest.Handler> pathResolver;

		public Fixture(List<String> paths)
		{
			JaxRsPathResolver.Builder<Object, JaxRsPathResolverTest.Handler> builder = new JaxRsPathResolver.Builder<Object, JaxRsPathResolverTest.Handler>()
					.separator('/')
					.placeholderStart('{')
					.placeholderEnd('}');

			for (String path: paths) {
				builder.registerPath(path, JaxRsPathResolverTest.Handler.of(path));
			}
			this.pathResolver = builder.build();
		}
	}

	@Value
	private static class Handler implements Predicate<Object>
	{
		public static Handler of(String value)
		{
			return new Handler(value);
		}

		private final String value;

		@Override
		public boolean test(Object o)
		{
			return true;
		}
	}
}
