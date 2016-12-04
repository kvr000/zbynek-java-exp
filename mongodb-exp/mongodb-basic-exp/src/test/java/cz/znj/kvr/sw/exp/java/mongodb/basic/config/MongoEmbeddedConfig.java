package cz.znj.kvr.sw.exp.java.mongodb.basic.config;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.UnknownHostException;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
@Configuration
public class MongoEmbeddedConfig
{
	@PostConstruct
	public MongoEmbeddedConfig init() throws IOException
	{
		mongodbFactory = new MongodForTestsFactory(Version.V3_3_1);
		return this;
	}

	@PreDestroy
	public void destroy()
	{
		mongodbFactory.shutdown();
	}

	@Bean
	public MongoClient mongoClient() throws UnknownHostException
	{
		return mongodbFactory.newMongo();
	}

	private MongodForTestsFactory mongodbFactory;
}
