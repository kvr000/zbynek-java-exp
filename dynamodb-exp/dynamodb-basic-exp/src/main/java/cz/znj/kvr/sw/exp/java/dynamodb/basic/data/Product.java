package cz.znj.kvr.sw.exp.java.dynamodb.basic.data;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


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

	private BigDecimal value;

	private Validity validity;

	@DynamoDBVersionAttribute
	private Long version;
}
