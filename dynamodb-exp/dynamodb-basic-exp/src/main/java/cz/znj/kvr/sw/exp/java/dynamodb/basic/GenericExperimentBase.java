package cz.znj.kvr.sw.exp.java.dynamodb.basic;

import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.collect.ImmutableMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author
 * 	Zbyněk Vyškovský
 */
public abstract class GenericExperimentBase extends TableExperimentBase {
	Table productTable;

	Table productItemTable;

	@Override
	protected Collection<CreateTableRequest> getExperimentTablesDefinitions() {
		List<CreateTableRequest> requests = new ArrayList<>();

		{
			ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition()
					.withAttributeName("productId")
					.withAttributeType("N"));
			attributeDefinitions.add(new AttributeDefinition()
					.withAttributeName("sin")
					.withAttributeType("S"));
			attributeDefinitions.add(new AttributeDefinition()
					.withAttributeName("title")
					.withAttributeType("S"));
//        attributeDefinitions.add(new AttributeDefinition()
//                        .withAttributeName("date")
//                        .withAttributeType("S"));
//        attributeDefinitions.add(new AttributeDefinition()
//                        .withAttributeName("price")
//                        .withAttributeType("N"));

			requests.add(new CreateTableRequest()
					.withTableName("Product")
					.withKeySchema(
							new KeySchemaElement()
									.withAttributeName("productId")
									.withKeyType(KeyType.HASH)
					)
					.withAttributeDefinitions(attributeDefinitions)
					.withGlobalSecondaryIndexes(
							new GlobalSecondaryIndex()
									.withIndexName("SinUdx")
									.withKeySchema(
											new KeySchemaElement("sin", KeyType.HASH)
									)
									.withProjection(new Projection().withProjectionType("KEYS_ONLY"))
									.withProvisionedThroughput(new ProvisionedThroughput(1000L, 1000L)),
							new GlobalSecondaryIndex()
									.withIndexName("TitleUdx")
									.withKeySchema(
											new KeySchemaElement("title", KeyType.HASH)
									)
									.withProjection(new Projection().withProjectionType("KEYS_ONLY"))
									.withProvisionedThroughput(new ProvisionedThroughput(1000L, 1000L))

					)
					.withProvisionedThroughput(new ProvisionedThroughput()
							.withReadCapacityUnits(1000L)
							.withWriteCapacityUnits(1000L)
					)
			);
		}

		{
			ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition()
					.withAttributeName("productId")
					.withAttributeType("N"));
			attributeDefinitions.add(new AttributeDefinition()
					.withAttributeName("itemCode")
					.withAttributeType("S"));

			requests.add(new CreateTableRequest()
					.withTableName("ProductItem")
					.withKeySchema(
							new KeySchemaElement()
									.withAttributeName("productId")
									.withKeyType(KeyType.HASH),
							new KeySchemaElement()
									.withAttributeName("itemCode")
									.withKeyType(KeyType.RANGE)
					)
					.withAttributeDefinitions(attributeDefinitions)
					.withProvisionedThroughput(new ProvisionedThroughput()
							.withReadCapacityUnits(1000L)
							.withWriteCapacityUnits(1000L)
					)
			);
		}

		return requests;
	}

	@Override
	protected void consumeExperimentTablesCreations(List<Table> tables) {
		productTable = tables.get(0);
		productItemTable = tables.get(1);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void initData() {
		productTable.putItem(new Item()
				.withPrimaryKey("productId", 1)
				.withString("sin", "SIN1000")
				.withString("title", "Hello")
				.with("value", new BigDecimal("1.1"))
				.withDouble("price", 5.0)
				.withMap("validity", ImmutableMap.<String, Object>builder().put("effectiveDate", "2015-01-01T00:00:00Z").put("untilDate", "2016-01-01T00:00:00Z").build())
				.withLong("version", 1)
		);
		productTable.putItem(new Item()
				.withPrimaryKey("productId", 2)
				.withString("sin", "SIN1001")
				.withString("title", "World")
				.withDouble("price", 6.0)
				.withLong("version", 1)
		);
		productItemTable.putItem(new Item()
				.withPrimaryKey("productId", 2, "itemCode", "Zbynek")
				.withString("title", "World")
				.withLong("version", 1)
		);
	}

	protected void doTableExperiment() {
	}
}
