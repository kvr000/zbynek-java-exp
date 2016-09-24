package cz.znj.kvr.sw.exp.java.dynamodb.basic;

import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableCollection;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author
 * 	Zbyněk Vyškovský
 */
public class ConnectExperiment extends ExperimentBase {
	private static final String tableName = "ZbynekDynamoTable";

	public static void main(String[] args) {
		System.exit(new ConnectExperiment().run(args));
	}

	public int process() {
		createExampleTable();
		listMyTables();
		getTableInformation();
		updateExampleTable();

		deleteExampleTable();

		return 0;
	}

	private void createExampleTable() {
		try {

			ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition()
					.withAttributeName("Id")
					.withAttributeType("N"));

			ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
			keySchema.add(new KeySchemaElement()
					.withAttributeName("Id")
					.withKeyType(KeyType.HASH)); //Partition key

			CreateTableRequest request = new CreateTableRequest()
					.withTableName(tableName)
					.withKeySchema(keySchema)
					.withAttributeDefinitions(attributeDefinitions)
					.withProvisionedThroughput(new ProvisionedThroughput()
							.withReadCapacityUnits(5L)
							.withWriteCapacityUnits(6L));

			System.out.println("Issuing CreateTable request for " + tableName);
			dynamoDb.createTable(request);

			System.out.println("Waiting for " + tableName
					+ " to be created...this may take a while...");

			getTableInformation();

		} catch (Exception e) {
			System.err.println("CreateTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}

	}

	private void listMyTables() {

		List<String> tables = dynamoDb.listTables().getTableNames();

		System.out.println("Listing productTable names");

		for (String table: tables) {
			System.out.println(table);
		}
	}

	private void getTableInformation() {

		System.out.println("Describing " + tableName);

		TableDescription tableDescription = new Table(dynamoDb, tableName).getDescription();
		System.out.format("Name: %s:\n" + "Status: %s \n"
						+ "Provisioned Throughput (read capacity units/sec): %d \n"
						+ "Provisioned Throughput (write capacity units/sec): %d \n",
				tableDescription.getTableName(),
				tableDescription.getTableStatus(),
				tableDescription.getProvisionedThroughput().getReadCapacityUnits(),
				tableDescription.getProvisionedThroughput().getWriteCapacityUnits());
	}

	private void updateExampleTable() {

		Table table = new Table(dynamoDb, tableName);
		System.out.println("Modifying provisioned throughput for " + tableName);

		try {
			table.updateTable(new ProvisionedThroughput()
					.withReadCapacityUnits(6L).withWriteCapacityUnits(7L));

			table.waitForActive();
		} catch (Exception e) {
			System.err.println("UpdateTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}
	}

	private void deleteExampleTable() {

		Table table = new Table(dynamoDb, tableName);
		try {
			System.out.println("Issuing DeleteTable request for " + tableName);
			table.delete();

			System.out.println("Waiting for " + tableName
					+ " to be deleted...this may take a while...");

			table.waitForDelete();
		} catch (Exception e) {
			System.err.println("DeleteTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}
	}
}
