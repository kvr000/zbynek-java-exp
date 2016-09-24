package cz.znj.kvr.sw.exp.java.dynamodb.basic;

import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author
 * 	Zbyněk Vyškovský
 */
public class ParallelUpdateExperiment extends TableExperimentBase {
	private static final String tableName = "ZbynekParallel";

	Table table;

	public static void main(String[] args) {
		System.exit(new ParallelUpdateExperiment().run(args));
	}

	@Override
	protected Collection<CreateTableRequest> getExperimentTablesDefinitions() {
		ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
		attributeDefinitions.add(new AttributeDefinition()
				.withAttributeName("productId")
				.withAttributeType("N"));

		ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
		keySchema.add(new KeySchemaElement()
				.withAttributeName("productId")
				.withKeyType(KeyType.HASH)); //Partition key

		CreateTableRequest request = new CreateTableRequest()
				.withTableName(tableName)
				.withKeySchema(keySchema)
				.withAttributeDefinitions(attributeDefinitions)
				.withProvisionedThroughput(new ProvisionedThroughput()
						.withReadCapacityUnits(1000L)
						.withWriteCapacityUnits(1000L));

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
		);
	}

	protected void doTableExperiment() {
		final long UPDATES = 100;
		final int processorCount = Runtime.getRuntime().availableProcessors() * 1;
		List<Thread> workers = new ArrayList<>(4);

		Stopwatch stopWatch = Stopwatch.createStarted();
		for (int i = 0; i < processorCount; ++i) {
			Thread worker = new Thread(() -> {
				for (int n = 0; n < UPDATES; ++n) {
					for (; ; ) {
						try {
							long oldValue = table.getItem("productId", 1).getLong("value");
							table.updateItem("productId", 1, Collections.singleton(new Expected("value").eq(oldValue)), new AttributeUpdate("value").put(oldValue + 1));
							break;
						} catch (ConditionalCheckFailedException ex) {
							// give it next try
						}
					}
				}
			});
			worker.start();
			workers.add(worker);
		}

		workers.forEach((Thread thread) -> {
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw Throwables.propagate(e);
			}
		});

		long value = table.getItem("productId", 1).getLong("value");
		if (value != UPDATES * processorCount) {
			throw new IllegalArgumentException("Unexpected value for value: " + value);
		}

		logger.info("Done in " + stopWatch.toString() + ", " + UPDATES * processorCount / stopWatch.elapsed(TimeUnit.SECONDS) + "/s");
	}
}
