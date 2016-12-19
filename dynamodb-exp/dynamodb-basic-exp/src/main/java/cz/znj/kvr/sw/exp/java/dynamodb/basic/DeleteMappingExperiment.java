package cz.znj.kvr.sw.exp.java.dynamodb.basic;

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import cz.znj.kvr.sw.exp.java.dynamodb.basic.data.Product;


/**
 * @author
 * 	Zbyněk Vyškovský
 */
public class DeleteMappingExperiment extends GenericExperimentBase {
	public static void main(String[] args) {
		System.exit(new DeleteMappingExperiment().run(args));
	}

	protected void doTableExperiment() {
		{
			logger.info("Getting from primary key:");
			Product product = dynamoDbMapper.load(Product.class, 1);
			logger.info("Got "+product.getProductId()+": "+ReflectionToStringBuilder.toString(product));

			if (product.getVersion() != 1)
				throw new IllegalArgumentException("Wrong version: "+product.getVersion());

			dynamoDbMapper.delete(product);

			product = dynamoDbMapper.load(Product.class, 1);

			Preconditions.checkArgument(product == null, "Product was not deleted");
		}

		{
			logger.info("Getting from primary key:");
			Product product = dynamoDbMapper.load(Product.class, 2);
			logger.info("Got "+product.getProductId()+": "+ReflectionToStringBuilder.toString(product));

			if (product.getVersion() != 1)
				throw new IllegalArgumentException("Wrong version: "+product.getVersion());

				Product productUpd = dynamoDbMapper.load(Product.class, 2);
			productUpd.setTitle(productUpd.getTitle()+" updated");
			dynamoDbMapper.save(productUpd);

			try {
				dynamoDbMapper.delete(product);
				throw new IllegalArgumentException("Expected ConditionalCheckFailedException");
			}
			catch (ConditionalCheckFailedException ex) {
			}

			product = dynamoDbMapper.load(Product.class, 2);
			Preconditions.checkArgument(product != null, "Product not found");

			dynamoDbMapper.delete(product);

			product = dynamoDbMapper.load(Product.class, 2);
			Preconditions.checkArgument(product == null, "Product was not deleted");
		}

	}
}
