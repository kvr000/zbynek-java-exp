package cz.znj.kvr.sw.exp.java.dynamodb.basic.data;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Getter;
import lombok.Setter;


/**
 * Product data
 */
@DynamoDBTable(tableName = "Product")
@Getter
@Setter
public class Product implements Productable {
	@DynamoDBHashKey
	private long productId;

	private String sin;

	private String title;

	private Validity validity;
}
