package cz.znj.kvr.sw.exp.java.camel.camone;

import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.impl.RoutePolicySupport;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.stereotype.Component;


/**
 * Route definition for timing test.
 */
@Component
public class TimingRoute extends SpringRouteBuilder
{
	@Override
	public void                     configure() throws CamelException
	{ 
		from("timer://foo?fixedRate=true&period="+period+"")
			.routeId("Timing route 1")
                        .routePolicy(new RoutePolicySupport() {
                                @Override
                                public void onStart(Route route) {
                                        super.onStart(route);
                                        System.out.println("starting "+route);
                                }

                                @Override
                                public void onStop(Route route) {
                                        System.out.println("stopping "+route);
                                        super.onStop(route);
                                }
                        })
			.startupOrder(1)
			.to("seda:timerPublish");

		from("timer://foo?fixedRate=true&period="+(period*3/2))
			.routeId("Timing route 1.5")
			.startupOrder(2)
			.to("seda:timerPublish");

		from("seda:timerPublish")
			.processRef("messageProcessor");

		from("seda:messageProcessorProcessed")
			.process(new Processor() {
				@Override
				public void process(Exchange exch)
				{
					System.out.println("tick: "+exch.getExchangeId());
					exch.getOut().setBody(exch.getProperty("CamelTimerCounter"));
					if (((Number)exch.getProperty("CamelTimerCounter")).longValue()%3 == 2)
					throw new RuntimeException("failed %3");
				}
			})
		.to("stream:out");
	}

	public void			setPeriod(int period)
	{
		this.period = period;
	}

	private int			period = 1000;
}
