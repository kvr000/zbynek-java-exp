package cz.znj.kvr.sw.exp.java.mongodb.basic;

import com.mongodb.MongoClient;
import cz.znj.kvr.sw.exp.java.mongodb.basic.config.MongoEmbeddedConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import javax.inject.Inject;


/**
 * @author
 * 	Zbyněk Vyškovský
 */
@Log4j2
@Test(groups =  "unit")
@ContextConfiguration(classes = MongoEmbeddedConfig.class)
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
}
