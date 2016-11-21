package cz.znj.kvr.sw.exp.java.mongodb.basic;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.net.UnknownHostException;


/**
 * @author
 * 	Zbyněk Vyškovský
 */
@Log4j2
@Test(groups =  "unit")
@Import(value = ConnectTest.MongoEmbeddedConfig.class)
public class ConnectTest extends AbstractTestNGSpringContextTests
{
	@Inject
	private MongoClient mongoClient;

	@Test
	public void testConnect()
	{
		for (String dbName: mongoClient.listDatabaseNames()) {
			log.info("Db: "+dbName);
		}
	}

	@Configuration
	public static class MongoEmbeddedConfig
	{
		@PostConstruct
		public MongoEmbeddedConfig init() throws IOException
		{
			mongodFactory = new MongodForTestsFactory(Version.V3_3_1);
			return this;
		}

		@PreDestroy
		public void destroy()
		{
			mongodFactory.shutdown();
		}

		@Bean
		public Mongo mongoClient() throws UnknownHostException
		{
			return mongodFactory.newMongo();
		}

		private MongodForTestsFactory mongodFactory;
	}
}
