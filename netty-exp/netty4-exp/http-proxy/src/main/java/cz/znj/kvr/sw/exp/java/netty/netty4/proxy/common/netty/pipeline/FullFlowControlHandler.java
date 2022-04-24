package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.netty.pipeline;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayDeque;


/**
 * Copy of {@link FlowControlHandler}, with additional control of user triggerred events.
 */
@Log4j2
public class FullFlowControlHandler extends ChannelDuplexHandler
{
    private ArrayDeque<Object> queue = new ArrayDeque<>();

    private ChannelConfig config;

    private boolean shouldConsume = false;

    public FullFlowControlHandler()
    {
    }

    private boolean isQueueEmpty()
    {
        return queue.isEmpty();
    }

    /**
     * Releases all messages and destroys the {@link #queue}.
     */
    private void destroy() {
        if (!isQueueEmpty()) {
            log.trace("Non-empty queue: {}", queue);

            Object msg;
            while ((msg = queue.poll()) != null) {
                ReferenceCountUtil.safeRelease(msg);
            }
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        config = ctx.channel().config();
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        if (!isQueueEmpty()) {
            dequeue(ctx, queue.size());
        }
        destroy();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        ctx.fireChannelInactive();
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        if (dequeue(ctx, 1) == 0) {
            // It seems no messages were consumed. We need to read() some
            // messages from upstream and once one arrives it needs to be
            // relayed to downstream to keep the flow going.
            shouldConsume = true;
            ctx.read();
        }
        else if (config.isAutoRead()) {
            ctx.read();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        queue.offer(msg);

        // We just received one message. Do we need to relay it regardless
        // of the auto reading configuration? The answer is yes if this
        // method was called as a result of a prior read() call.
        int minConsume = shouldConsume ? 1 : 0;
        shouldConsume = false;

        dequeue(ctx, minConsume);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (isQueueEmpty()) {
            ctx.fireChannelReadComplete();
        } else {
            // Don't relay completion events from upstream as they
            // make no sense in this context. See dequeue() where
            // a new set of completion events is being produced.
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        queue.offer(new UserEventHolder(evt));
        if (shouldConsume) {
            shouldConsume = false;
            dequeue(ctx, 1);
        }
    }

    /**
     * Dequeues one or many (or none) messages depending on the channel's auto
     * reading state and returns the number of messages that were consumed from
     * the internal queue.
     *
     * The {@code minConsume} argument is used to force {@code dequeue()} into
     * consuming that number of messages regardless of the channel's auto
     * reading configuration.
     *
     * @see #read(ChannelHandlerContext)
     * @see #channelRead(ChannelHandlerContext, Object)
     */
    private int dequeue(ChannelHandlerContext ctx, int minConsume) {
        int consumed = 0;

        // fireChannelRead(...) may call ctx.read() and so this method may reentrance. Because of this we need to
        // check if queue was set to null in the meantime and if so break the loop.
        while (queue != null && (consumed < minConsume || config.isAutoRead())) {
            Object msg = queue.poll();
            if (msg == null) {
                break;
            }

            ++consumed;
            if (msg instanceof UserEventHolder eventHolder) {
                ctx.fireUserEventTriggered(eventHolder.event);
            }
            else {
                ctx.fireChannelRead(msg);
            }
        }

        // We're firing a completion event every time one (or more)
        // messages were consumed and the queue ended up being drained
        // to an empty state.
        if (consumed > 0) {
            ctx.fireChannelReadComplete();
        }

        return consumed;
    }

    @RequiredArgsConstructor
    static class UserEventHolder
    {
        final Object event;
    }
}
