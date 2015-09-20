/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.WeakHashMap;

/**
 * The default {@link ChannelPipeline} implementation.  It is usually created
 * by a {@link Channel} implementation when the {@link Channel} is created.
 */
public final class DefaultChannelPipeline extends AbstractChannelPipeline {

	static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultChannelPipeline.class);

	@SuppressWarnings("unchecked")
	private static final WeakHashMap<Class<?>, String>[] nameCaches =
		new WeakHashMap[Runtime.getRuntime().availableProcessors()];

	static {
		for (int i = 0; i < nameCaches.length; i ++) {
			nameCaches[i] = new WeakHashMap<Class<?>, String>();
		}
	}

	DefaultChannelPipeline(AbstractChannel channel) {
		super(channel);
	}
}
