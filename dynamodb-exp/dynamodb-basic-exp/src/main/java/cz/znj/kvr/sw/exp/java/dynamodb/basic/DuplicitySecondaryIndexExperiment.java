package cz.znj.kvr.sw.exp.java.dynamodb.basic;

import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author
 * 	Zbyněk Vyškovský
 */
public class DuplicitySecondaryIndexExperiment extends TableExperimentBase {
	private static final String tableName = "ZbynekDuplicitySecondary";

	Table table;

	public static void main(String[] args) {
		System.exit(new DuplicitySecondaryIndexExperiment().run(args));
	}

	@Override
	protected Collection<CreateTableRequest> getExperimentTablesDefinitions() {
		ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
		attributeDefinitions.add(new AttributeDefinition()
				.withAttributeName("productId")
				.withAttributeType("N"));
		attributeDefinitions.add(new AttributeDefinition()
				.withAttributeName("vendor")
				.withAttributeType("S"));
		attributeDefinitions.add(new AttributeDefinition()
				.withAttributeName("sin")
				.withAttributeType("S"));

		ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
		keySchema.add(new KeySchemaElement()
				.withAttributeName("productId")
				.withKeyType(KeyType.HASH)); //Partition key

		CreateTableRequest request = new CreateTableRequest()
				.withTableName(tableName)
				.withKeySchema(keySchema)
				.withAttributeDefinitions(attributeDefinitions)
				.withGlobalSecondaryIndexes(
						new GlobalSecondaryIndex()
								.withIndexName("SinVendorUdx")
								.withKeySchema(
										new KeySchemaElement("sin", KeyType.HASH),
										new KeySchemaElement("vendor", KeyType.RANGE)
								)
								.withProjection(new Projection().withProjectionType("KEYS_ONLY"))
								.withProvisionedThroughput(new ProvisionedThroughput(1000L, 1000L)),
						new GlobalSecondaryIndex()
								.withIndexName("VendorSinUdx")
								.withKeySchema(
										new KeySchemaElement("vendor", KeyType.HASH),
										new KeySchemaElement("sin", KeyType.RANGE)
								)
								.withProjection(new Projection().withProjectionType("KEYS_ONLY"))
								.withProvisionedThroughput(new ProvisionedThroughput(1000L, 1000L))

				)
				.withProvisionedThroughput(new ProvisionedThroughput()
						.withReadCapacityUnits(1000L)
						.withWriteCapacityUnits(1000L)
				);

		return Collections.singletonList(request);
	}

	@Override
	protected void consumeExperimentTablesCreations(List<Table> tables) {
		table = tables.get(0);
	}

	@Override
	protected void initData() {
		table.putItem(new Item()
				.withPrimaryKey("productId", 1)
				.withInt("value", 0)
				.withString("vendor", "me")
				.withString("sin", "SIN1234")
		);
		table.putItem(new Item()
				.withPrimaryKey("productId", 2)
				.withInt("value", 1)
				.withString("vendor", "him")
				.withString("sin", "SIN1234")
		);
	}

	protected void doTableExperiment() {
		table.putItem(new Item()
				.withPrimaryKey("productId", 3)
				.withInt("value", 3)
				.withString("vendor", "him")
				.withString("sin", "SIN1234")
		);
		{
			logger.info("Running primary key query:");
			ItemCollection<QueryOutcome> result = table.query(new QuerySpec().withHashKey("productId", 1));
			result.forEach((Item item) -> {
				logger.info("Got item: " + item.toJSONPretty());
			});
		}

		{
			logger.info("Running secondary key query:");
			ItemCollection<ScanOutcome> result = table.scan("vendor = :vendor", null, Collections.singletonMap(":vendor", "me"));
			result.forEach((Item item) -> {
				logger.info("Got item: " + item.toJSONPretty());
			});
		}

		{
			logger.info("Running secondary key query dup:");
			ItemCollection<ScanOutcome> result = table.scan("sin = :sin", null, Collections.singletonMap(":sin", "SIN1234"));
			result.forEach((Item item) -> {
				logger.info("Got item: " + item.toJSONPretty());
			});
		}
	}
}
