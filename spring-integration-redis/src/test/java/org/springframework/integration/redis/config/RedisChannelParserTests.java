/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.redis.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class RedisChannelParserTests extends RedisAvailableTests{

	@Test
	@RedisAvailable
	public void testPubSubChannelConfig(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("RedisChannelParserTests-context.xml", this.getClass());
		SubscribableChannel redisChannel = context.getBean("redisChannel", SubscribableChannel.class);
		RedisConnectionFactory connectionFactory =
			TestUtils.getPropertyValue(redisChannel, "connectionFactory", RedisConnectionFactory.class);
		RedisSerializer<?> redisSerializer = TestUtils.getPropertyValue(redisChannel, "serializer", RedisSerializer.class);
		assertEquals(connectionFactory, context.getBean("redisConnectionFactory"));
		assertEquals(redisSerializer, context.getBean("redisSerializer"));
		assertEquals("si.test.topic", TestUtils.getPropertyValue(redisChannel, "topicName"));
		assertEquals(Integer.MAX_VALUE, TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(redisChannel, "dispatcher"), "maxSubscribers", Integer.class).intValue());
		redisChannel = context.getBean("redisChannelWithSubLimit", SubscribableChannel.class);
		assertEquals(1, TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(redisChannel, "dispatcher"), "maxSubscribers", Integer.class).intValue());
		context.stop();
	}

	@Test
	@RedisAvailable
	public void testPubSubChannelUsage() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("RedisChannelParserTests-context.xml", this.getClass());
		SubscribableChannel redisChannel = context.getBean("redisChannel", SubscribableChannel.class);
		final Message<?> m = new GenericMessage<String>("Hello Redis");

		final CountDownLatch latch = new CountDownLatch(1);
		redisChannel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				assertEquals(m.getPayload(), message.getPayload());
				latch.countDown();
			}
		});
		redisChannel.send(m);

		assertTrue(latch.await(2, TimeUnit.SECONDS));
		context.stop();
	}

}
