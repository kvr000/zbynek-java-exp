package cz.znj.kvr.sw.exp.java.camel.camone;

import org.apache.camel.*;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.stereotype.Component;


/**
 * Route definition for timing test.
 */
@Component
public class MessageProcessorImpl implements Processor
{
	@Override
	public void                     process(Exchange exch) throws CamelException
	{ 
		producer.sendBody(exch.getOut().getBody());
	}

	@Produce(uri = "seda:messageProcessorProcessed")
	public void			setProducer(ProducerTemplate producer)
	{
		this.producer = producer;
	}

	ProducerTemplate		producer;
}
