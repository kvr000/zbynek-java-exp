package cz.znj.kvr.sw.exp.java.dynamodb.basic.data;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @author
 * 	Zbynek Vyskovsky
 */
@Getter
@Setter
@ToString
@DynamoDBDocument
public class Validity {
	private Date effectiveDate;

	private Date untilDate;
}
