package cz.znj.kvr.sw.exp.java.mongodb.basic;

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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.Assert;
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
@ContextConfiguration(classes = MongoEmbeddedConfig.class)
public class DbObjectTableTest extends AbstractTestNGSpringContextTests
{
	@Inject
	private MongoClient mongoClient;

	private MongoDatabase db;

	private MongoCollection<MyTable> table;

	@BeforeClass
	public void createStructure()
	{
		db = mongoClient.getDatabase("local");
		db.createCollection("SpringTable", new CreateCollectionOptions().autoIndex(true));
		table = db.getCollection("SpringTable", MyTable.class);
	}

	@Test
	public void testTable()
	{
		BasicDBObject query = new BasicDBObject();
		query.put("myId", 1);
		FindIterable<MyTable> rs0 = table.find(MyTable.class);
		Assert.assertEquals(0, Iterables.size(rs0));
		MyTable sample1 = new MyTable();
		sample1.setMyId(1).setName("Hello");
		table.insertOne(sample1);
		FindIterable<MyTable> rs1 = true ? table.find() : table.find(Filters.eq("myId", 1));
		for (Object myTable: rs1) {
			System.out.println(myTable);
		}
		Assert.assertEquals(1, Iterables.size(rs1));
	}

	@Data
	@NoArgsConstructor
	@Accessors(chain = true)
	@EqualsAndHashCode(callSuper = true)
	public static class MyTable extends ReflectionDBObject
	{
		public static final String MYID = "myId";
		public static final String NAME = "name";

		private long myId;

		private String name;
	}
}
