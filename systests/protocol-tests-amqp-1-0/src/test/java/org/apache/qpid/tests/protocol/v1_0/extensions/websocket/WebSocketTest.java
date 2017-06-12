/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.qpid.tests.protocol.v1_0.extensions.websocket;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertArrayEquals;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import org.apache.qpid.server.protocol.v1_0.type.UnsignedInteger;
import org.apache.qpid.server.protocol.v1_0.type.UnsignedShort;
import org.apache.qpid.server.protocol.v1_0.type.transport.Open;
import org.apache.qpid.tests.protocol.v1_0.BrokerAdmin;
import org.apache.qpid.tests.protocol.v1_0.FrameTransport;
import org.apache.qpid.tests.protocol.v1_0.HeaderResponse;
import org.apache.qpid.tests.protocol.v1_0.ProtocolTestBase;
import org.apache.qpid.tests.protocol.v1_0.SpecificationTest;

public class WebSocketTest extends ProtocolTestBase
{

    public static final byte[] AMQP_HEADER = "AMQP\0\1\0\0".getBytes(StandardCharsets.UTF_8);

    @Test
    @SpecificationTest(section = "2.1", description = "Opening a WebSocket Connection")
    public void protocolHeader() throws Exception
    {
        final InetSocketAddress addr = getBrokerAdmin().getBrokerAddress(BrokerAdmin.PortType.ANONYMOUS_AMQPWS);
        try (FrameTransport transport = new WebSocketFrameTransport(addr).connect())
        {
            transport.sendProtocolHeader(AMQP_HEADER);
            HeaderResponse response = transport.getNextResponse();
            assertArrayEquals("Unexpected protocol header response", AMQP_HEADER, response.getBody());
        }
    }

    @Test
    @SpecificationTest(section = "2.4", description = "[...] a single AMQP frame MAY be split over one or more consecutive WebSocket messages. ")
    public void amqpFramesSplitOverManyWebSocketFrames() throws Exception
    {
        final InetSocketAddress addr = getBrokerAdmin().getBrokerAddress(BrokerAdmin.PortType.ANONYMOUS_AMQPWS);
        try (FrameTransport transport = new WebSocketFrameTransport(addr).splitAmqpFrames().connect())
        {
            transport.sendProtocolHeader(AMQP_HEADER);
            HeaderResponse response = transport.getNextResponse(HeaderResponse.class);
            assertArrayEquals("Unexpected protocol header response", AMQP_HEADER, response.getBody());

            Open open = new Open();
            open.setContainerId("testContainerId");
            transport.sendPerformative(open, UnsignedShort.ZERO);
            Open responseOpen = transport.getNextResponseBody(Open.class);

            assertThat(responseOpen.getContainerId(), is(notNullValue()));
            assertThat(responseOpen.getMaxFrameSize().longValue(),
                       is(both(greaterThanOrEqualTo(0L)).and(lessThan(UnsignedInteger.MAX_VALUE.longValue()))));
            assertThat(responseOpen.getChannelMax().intValue(),
                       is(both(greaterThanOrEqualTo(0)).and(lessThan(UnsignedShort.MAX_VALUE.intValue()))));

            transport.doCloseConnection();
        }
    }

    @Test
    @SpecificationTest(section = "2.1", description = "Opening a WebSocket Connection")
    public void successfulOpen() throws Exception
    {
        final InetSocketAddress addr = getBrokerAdmin().getBrokerAddress(BrokerAdmin.PortType.ANONYMOUS_AMQPWS);
        try (FrameTransport transport = new WebSocketFrameTransport(addr).connect())
        {
            transport.doProtocolNegotiation();

            Open open = new Open();
            open.setContainerId("testContainerId");
            transport.sendPerformative(open, UnsignedShort.ZERO);
            Open responseOpen = transport.getNextResponseBody(Open.class);

            assertThat(responseOpen.getContainerId(), is(notNullValue()));
            assertThat(responseOpen.getMaxFrameSize().longValue(),
                       is(both(greaterThanOrEqualTo(0L)).and(lessThan(UnsignedInteger.MAX_VALUE.longValue()))));
            assertThat(responseOpen.getChannelMax().intValue(),
                       is(both(greaterThanOrEqualTo(0)).and(lessThan(UnsignedShort.MAX_VALUE.intValue()))));

            transport.doCloseConnection();
        }
    }
}
