package cz.znj.kvr.sw.exp.java.dynamodb.basic;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.dynamodb.basic.data.ProductItem;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import cz.znj.kvr.sw.exp.java.dynamodb.basic.data.Product;

import java.util.Collections;


/**
 * @author
 * 	Zbyněk Vyškovský
 */
public class MappingExperiment extends GenericExperimentBase {
	public static void main(String[] args) {
		System.exit(new MappingExperiment().run(args));
	}

	protected void doTableExperiment() {
		{
			logger.info("Getting from primary key:");
			Product product = dynamoDbMapper.load(Product.class, 0);
			logger.info("Got empty(?): " + ReflectionToStringBuilder.toString(product));
		}

		{
			logger.info("Getting from primary key:");
			Product product = dynamoDbMapper.load(Product.class, 1);
			logger.info("Got " + product.getProductId() + ": " + ReflectionToStringBuilder.toString(product));
		}

		{
			logger.info("Getting from secondary key:");
			PaginatedScanList<Product> list = dynamoDbMapper.scan(Product.class, new DynamoDBScanExpression().withFilterExpression("sin = :sin").withExpressionAttributeValues(Collections.singletonMap(":sin", new AttributeValue("SIN1001"))));
			list.forEach((Product product) -> logger.info("Got: " + ReflectionToStringBuilder.toString(product)));
		}

		{
			logger.info("Getting from composite key:");
			PaginatedScanList<ProductItem> list = dynamoDbMapper.scan(ProductItem.class, new DynamoDBScanExpression().withFilterExpression("productId = :productId AND itemCode = :itemCode").withExpressionAttributeValues(ImmutableMap.<String, AttributeValue>builder().put(":productId", new AttributeValue().withN("2")).put(":itemCode", new AttributeValue("Zbynek")).build()));
			list.forEach((ProductItem product) -> logger.info("Got: " + ReflectionToStringBuilder.toString(product)));
		}
	}
}
