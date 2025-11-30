package cz.znj.kvr.sw.exp.java.mongodb.basic;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertOneModel;
import cz.znj.kvr.sw.exp.java.mongodb.basic.config.MongoEmbeddedConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.util.MongoDbErrorCodes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import jakarta.inject.Inject;
import java.util.LinkedHashSet;


/**
 * MongoDb tests.
 *
 * @author
 * 	Zbyněk Vyškovský, kvr000@gmail.com, https://github.com/kvr000/
 */
@Log4j2
@Test(groups =  "unit")
@ContextConfiguration(classes = MongoEmbeddedConfig.class)
public class PojoTest extends AbstractTestNGSpringContextTests
{
	@Inject
	private MongoClient mongoClient;

	private MongoDatabase db;

	private MongoCollection<PojoTable> table;

	@BeforeClass
	public void createStructure()
	{
		db = mongoClient.getDatabase("local")
				.withCodecRegistry(CodecRegistries.fromRegistries(
						MongoClientSettings.getDefaultCodecRegistry(),
						CodecRegistries.fromProviders(
								PojoCodecProvider.builder()
										.automatic(true)
										.register(PojoTable.class)
										.build()
						)
				));
		db.createCollection("JacksonTable", new CreateCollectionOptions());
		table = db.getCollection("JacksonTable", PojoTable.class).withCodecRegistry(db.getCodecRegistry());
		table.createIndex(new BasicDBObject("myId", 1), new IndexOptions().unique(true));
	}

	/**
	 * Tests POJO serialization.
	 *
	 * The issue with native MongoDb POJO serialization is that first it does not deserialize into POJO, and
	 * secondly it does not follow lower camelCase convention.
	 */
	@Test
	public void testTable()
	{
		FindIterable<PojoTable> rs0 = table.find(Filters.eq("myId", 1L), PojoTable.class);
		Assert.assertEquals(0, Iterables.size(rs0));
		PojoTable sample1 = PojoTable.builder()
				.myId(1)
				.name("Hello")
				.build();
		table.insertOne(sample1);
		FindIterable<PojoTable> rs1 = table.find(Filters.eq("myId", 1));
		for (Object myTable: rs1) {
			System.out.println(myTable);
		}
		PojoTable[] entries = Iterables.toArray(rs1, PojoTable.class);
		Assert.assertEquals(1, entries.length);
		Assert.assertEquals(1L, entries[0].myId);
		Assert.assertEquals("Hello", entries[0].name);
	}

	/**
	 * Tests POJO serialization.
	 *
	 * The issue with native MongoDb POJO serialization is that first it does not deserialize into POJO, and
	 * secondly it does not follow lower camelCase convention.
	 */
	@Test
	public void testUpdate()
	{
		FindIterable<PojoTable> rs0 = table.find(Filters.eq("myId", 2L), PojoTable.class);
		Assert.assertEquals(0, Iterables.size(rs0));
		PojoTable sample1 = PojoTable.builder()
				.myId(2)
				.name("Hello")
				.build();
		table.insertOne(sample1);
		PojoTable written = readEntry(2);
		PojoTable sample2 = PojoTable.builder()
				.id(sample1.getId())
				.myId(2)
				.name("World")
				.build();
		table.replaceOne(Filters.eq("_id", written.getId()), sample2);

		FindIterable<PojoTable> rs1 = table.find(Filters.eq("_id", written.getId()));
		for (Object myTable: rs1) {
			System.out.println(myTable);
		}
		PojoTable[] entries = Iterables.toArray(rs1, PojoTable.class);
		Assert.assertEquals(1, entries.length);
		Assert.assertEquals(2L, entries[0].myId);
		Assert.assertEquals("World", entries[0].name);
	}

	@Test
	public void testInsertUnique()
	{
		FindIterable<PojoTable> rs0 = table.find(Filters.eq("myId", 3L), PojoTable.class);
		AssertJUnit.assertEquals(0, Iterables.size(rs0));
		PojoTable sample1 = PojoTable.builder()
				.myId(3)
				.name("Hello")
				.build();
		table.insertOne(sample1);
		MongoWriteException ex = Assert.expectThrows(MongoWriteException.class, () -> table.insertOne(sample1));
		Assert.assertTrue(MongoDbErrorCodes.isDuplicateKeyCode(ex.getCode()));
	}

	@Test
	public void testGetInsertedId()
	{
		FindIterable<PojoTable> rs0 = table.find(Filters.eq("myId", 4L), PojoTable.class);
		Assert.assertEquals(0, Iterables.size(rs0));
		PojoTable sample1 = PojoTable.builder()
				.myId(4)
				.name("Hello")
				.build();
		BulkWriteResult insertResult = table.bulkWrite(ImmutableList.of(new InsertOneModel<>(sample1)));

		// actually, no way to retrieve inserted id, insert returns empty list for upsert:
		Assert.assertEquals(insertResult.getInsertedCount(), 1);
		Assert.assertEquals(insertResult.getUpserts(), ImmutableList.of());
	}

	@Test
	public void testClientGeneratedId()
	{
		FindIterable<PojoTable> rs0 = table.find(Filters.eq("myId", 5L), PojoTable.class);
		Assert.assertEquals(0, Iterables.size(rs0));
		PojoTable sample1 = PojoTable.builder()
				.id(ObjectId.get())
				.myId(5)
				.name("Hello")
				.build();
		table.insertOne(sample1);

		PojoTable copy = readEntry(5L);
		Assert.assertEquals(copy.getMyId(), 5L);
		Assert.assertEquals(copy.getId(), sample1.getId());
	}

	@Test
	public void openTable_notExists_createsAutomatically()
	{
		MongoCollection<PojoTable> notfound = db.getCollection("NotFound", PojoTable.class).withCodecRegistry(db.getCodecRegistry());
		notfound.insertOne(PojoTable.builder().build());
	}

	@Test
	public void createTable_duplicate_fails()
	{
		db.createCollection("Duplicate");
		MongoCommandException ex = Assert.expectThrows(MongoCommandException.class, () -> db.createCollection("Duplicate"));
		Assert.assertEquals(ex.getCode(), 48);
	}

	private PojoTable readEntry(long myId)
	{
		FindIterable<PojoTable> rs = table.find(Filters.eq("myId", myId));
		return Iterables.getOnlyElement(rs);
	}

	@Builder(builderClassName = "Builder", toBuilder = true)
	@Value
	@JsonDeserialize(builder = PojoTable.Builder.class)
	@BsonDiscriminator
	@AllArgsConstructor(onConstructor = @__(@BsonCreator))
	public static class PojoTable
	{
		@BsonId
		ObjectId id;

		@BsonProperty("myId")
		long myId;

		@BsonProperty("name")
		String name;

		@BsonProperty("data")
		@lombok.Builder.Default
		LinkedHashSet<String> data = new LinkedHashSet<>();
	}
}
