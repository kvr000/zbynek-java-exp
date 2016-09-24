package cz.znj.kvr.sw.exp.java.dynamodb.basic;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author
 * 	Zbyněk Vyškovský
 */
public abstract class ExperimentBase {
	protected Logger logger = LogManager.getLogger(getClass());

	protected AmazonDynamoDB dynamoDb;

	protected DynamoDBMapper dynamoDbMapper;

	{
		if (false) {
			AmazonDynamoDBClient client = new AmazonDynamoDBClient(new ProfileCredentialsProvider());
			client.withEndpoint("http://localhost:8000");
			dynamoDb = client;
		}
		else {
			dynamoDb = DynamoDBEmbedded.create().amazonDynamoDB();
		}
		dynamoDbMapper = new DynamoDBMapper(dynamoDb);
	}

	public int run(String[] args) {
		int error;
		if ((error = setup(args)) != 0)
			return error;
		return process();
	}

	public int setup(String[] args) {
		return 0;
	}

	public abstract int process();
}
