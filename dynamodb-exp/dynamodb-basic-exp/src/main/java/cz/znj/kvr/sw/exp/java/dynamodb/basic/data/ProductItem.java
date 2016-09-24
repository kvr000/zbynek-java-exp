package cz.znj.kvr.sw.exp.java.dynamodb.basic.data;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Getter;
import lombok.Setter;


/**
 * Product Item data
 */
@DynamoDBTable(tableName = "ProductItem")
@Getter
@Setter
public class ProductItem {
	@DynamoDBHashKey
	private long productId;

	@DynamoDBRangeKey
	private String itemCode;

	private String title;
}
