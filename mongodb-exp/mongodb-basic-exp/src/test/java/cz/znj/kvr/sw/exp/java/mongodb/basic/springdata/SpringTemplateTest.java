package cz.znj.kvr.sw.exp.java.mongodb.basic.springdata;

import com.google.common.collect.Iterables;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.ReflectionDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import cz.znj.kvr.sw.exp.java.mongodb.basic.config.MongoEmbeddedConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;


/**
 * @author
 * 	Zbyněk Vyškovský
 */
@Log4j2
@Test(groups =  "unit")
@ContextConfiguration(classes = SpringTemplateTest.TestConfig.class)
public class SpringTemplateTest extends AbstractTestNGSpringContextTests
{
	@Inject
	private MongoClient mongoClient;

	@Inject
	private MongoTemplate mongoTemplate;

	private MongoDatabase db;

	private MongoCollection<BasicDBObject> table;

	@BeforeClass
	public void createStructure()
	{
		db = mongoClient.getDatabase("local");
		db.createCollection("SpringTable", new CreateCollectionOptions().autoIndex(true));
		table = db.getCollection("SpringTable", BasicDBObject.class);
	}

	@Test
	public void testReadWrite()
	{
		BasicDBObject query = new BasicDBObject();
		query.put("myId", 1);
		FindIterable<BasicDBObject> rs0 = table.find(query);
		Assert.assertEquals(0, Iterables.size(rs0));
		SpringTable sample1 = new SpringTable();
		sample1.setMyId(1).setName("Hello");
		mongoTemplate.insert(sample1);
		FindIterable<BasicDBObject> rs1 = table.find(query);
		for (Object myTable: rs1) {
			System.out.println(myTable);
		}
		Assert.assertEquals(1, Iterables.size(rs1));
		Assert.assertEquals(1L, Iterables.getFirst(rs1, null).get("myId"));
	}

	@Document(collection = "SpringTable")
	@Data
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class SpringTable
	{
		public static final String MYID = "myId";
		public static final String NAME = "name";

		private long myId;

		private String name;
	}

	@Configuration
	@Import(MongoEmbeddedConfig.class)
	public static class TestConfig
	{
		@Inject
		private MongoClient mongoClient;

		@Bean
		public MongoTemplate mongoTemplate()
		{
			return new MongoTemplate(new SimpleMongoDbFactory(mongoClient, "local"));
		}
	}
}
