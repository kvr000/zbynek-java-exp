package cz.znj.kvr.sw.exp.java.dynamodb.basic;

import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.base.Throwables;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author
 * 	Zbyněk Vyškovský
 */
public abstract class TableExperimentBase extends ExperimentBase {
	protected Collection<String> getExampleTableNames() {
		return getExperimentTablesDefinitions()
				.stream()
				.map((CreateTableRequest request) -> request.getTableName())
				.collect(Collectors.toList());
	}

	protected Collection<CreateTableRequest> getExperimentTablesDefinitions() {
		return Collections.<CreateTableRequest>emptyList();
	}

	protected void consumeExperimentTablesCreations(List<Table> tables) {
	}

	protected void initData() {
	}

	protected void doTableExperiment() {
	}

	public int process() {
		try {
			deleteExperimentTables();
		} catch (ResourceNotFoundException ex) {
			// ignore if the productTable didnot exist
		}
		createExperimentTables();
		try {
			initData();
			doTableExperiment();
		} finally {
			deleteExperimentTables();
		}

		return 0;
	}

	protected void printExperimentTablesInformation() {
		for (String tableName : getExampleTableNames()) {
			logger.info("Describing " + tableName);

			TableDescription tableDescription = new Table(dynamoDb, tableName).describe();
			logger.info(String.format("Name: %s:\n" + "Status: %s \n"
							+ "Provisioned Throughput (read capacity units/sec): %d \n"
							+ "Provisioned Throughput (write capacity units/sec): %d \n",
					tableDescription.getTableName(),
					tableDescription.getTableStatus(),
					tableDescription.getProvisionedThroughput().getReadCapacityUnits(),
					tableDescription.getProvisionedThroughput().getWriteCapacityUnits()));
		}
	}

	protected void createExperimentTables() {
		consumeExperimentTablesCreations(getExperimentTablesDefinitions().stream()
				.map((CreateTableRequest request) -> {
					logger.info("Issuing CreateTable request for " + request.getTableName());
					return dynamoDb.createTable(request);
				})
				.map((CreateTableResult createResult) -> {
					Table table = new Table(dynamoDb, createResult.getTableDescription().getTableName());
					logger.info("Waiting for " + table.getTableName() + " to be created...this may take a while...");
					try {
						table.waitForActive();
					} catch (InterruptedException e) {
						throw Throwables.propagate(e);
					}
					return table;
				})
				.collect(Collectors.toList())
		);
		logger.info("Tables created");
	}

	protected void deleteExperimentTables() {
		getExampleTableNames().stream()
				.map((String tableName) -> {
					Table table = new Table(dynamoDb, tableName);
					logger.info("Issuing DeleteTable request for " + tableName);
					table.delete();
					return table;
				})
				.forEach((Table table) -> {
					logger.info("Waiting for " + table.getTableName() + " to be deleted...this may take a while...");
					try {
						table.waitForDelete();
					} catch (ResourceNotFoundException e) {
						// ignore non-existing productTable
					} catch (InterruptedException e) {
						throw Throwables.propagate(e);
					}
				});
		logger.info("Tables deleted");
	}
}
