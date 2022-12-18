package cz.znj.kvr.sw.exp.java.message.pubsub.redis.benchmark;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class LettuceSubscribeCommand extends AbstractSubscribeCommand
{
	private final LettuceCommon common;

	@Override
	public int execute() throws Exception
	{
		CountDownLatch exitLatch = new CountDownLatch(options.count);
		List<LettuceCommon.Subscription> listeners = new ArrayList<>();
		for (int i = 0; i < options.count; ++i) {
			LettuceCommon.Subscription subscription = common.createSubscription();
			subscription.subscribe("Channel-1", (message) -> {}).subscribe();
			listeners.add(subscription);
		}
		exitLatch.await();
		return 0;
	}
}
