/*-
 * ========================LICENSE_START=================================
 * smooks-camel-cartridge
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 * 
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 * 
 * ======================================================================
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.cartridges.camel.processor;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.ManagementAgent;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.smooks.support.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.*;
import java.util.Set;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.camel.test.junit5.TestSupport.assertIsInstanceOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link SmooksProcessor}.
 *
 * @author Christian Mueller
 * @author Daniel Bevenius
 */
public class SmooksProcessorTest extends CamelTestSupport {
    @EndpointInject(value = "mock:result")
    private MockEndpoint result;
    private MBeanServer mbeanServer;

    @BeforeEach
    public void beforeEach() {
        ManagementAgent managementAgent = context.getManagementStrategy().getManagementAgent();
        mbeanServer = managementAgent.getMBeanServer();
    }

    @Override
    protected boolean useJmx() {
        return true;
    }

    @Test
    public void process() throws Exception {
        assertOneProcessedMessage();
    }

    private void assertOneProcessedMessage() throws Exception {
        result.expectedMessageCount(1);
        template.sendBody("direct://input", getOrderEdi());

        assertIsSatisfied();

        Exchange exchange = result.assertExchangeReceived(0);
        assertIsInstanceOf(Document.class, exchange.getIn().getBody());
        assertFalse(DiffBuilder.compare(getExpectedOrderXml()).withTest(exchange.getIn().getBody(String.class)).
                ignoreComments().
                ignoreWhitespace().
                build().
                hasDifferences());
    }

    @Test
    public void processWithAttachment() throws CamelExecutionException, IOException {
        final DefaultExchange exchange = new DefaultExchange(context);
        final String attachmentContent = "A dummy attachment";
        final String attachmentId = "testAttachment";
        addAttachment(attachmentContent, attachmentId, exchange);
        exchange.getIn().setBody(getOrderEdi());

        template.send("direct://input", exchange);

        final DataHandler datahandler = result.assertExchangeReceived(0).getIn(AttachmentMessage.class).getAttachment(attachmentId);
        assertThat(datahandler, is(notNullValue()));
        assertThat(datahandler.getContent(), is(instanceOf(ByteArrayInputStream.class)));

        final String actualAttachmentContent = getAttachmentContent(datahandler);
        assertThat(actualAttachmentContent, is(equalTo(attachmentContent)));
    }

    private void addAttachment(final String attachment, final String id, final Exchange exchange) {
        final DataSource ds = new StringDataSource(attachment);
        final DataHandler dataHandler = new DataHandler(ds);
        exchange.getIn(AttachmentMessage.class).addAttachment(id, dataHandler);
    }

    private String getAttachmentContent(final DataHandler datahandler) throws IOException {
        final ByteArrayInputStream bs = (ByteArrayInputStream) datahandler.getContent();
        return new String(StreamUtils.readStream(bs));
    }

    @Test
    public void assertSmooksReportWasCreated() throws Exception {
        assertOneProcessedMessage();

        File report = new File("target/smooks-report.html");
        report.deleteOnExit();
        assertTrue(report.exists(), "Smooks report was not generated.");
    }

    @Test
    @Disabled
    public void stopStartContext() throws Exception {
        ObjectInstance smooksProcessorMBean = getSmooksProcessorObjectInstance();

        assertOneProcessedMessage();
        stopSmooksProcessor(smooksProcessorMBean.getObjectName());
        Thread.sleep(500);

        startSmooksProcessor(smooksProcessorMBean.getObjectName());
        Thread.sleep(500);

        assertOneProcessedMessage();
    }

    private void stopSmooksProcessor(ObjectName objectName) throws Exception {
        invokeVoidNoArgsMethod(objectName, "stop");
    }

    private void invokeVoidNoArgsMethod(ObjectName objectName, String methodName) throws Exception {
        mbeanServer.invoke(objectName, methodName, null, null);
    }

    private void startSmooksProcessor(ObjectName objectName) throws Exception {
        invokeVoidNoArgsMethod(objectName, "start");
    }

    private ObjectInstance getSmooksProcessorObjectInstance() throws Exception {
        ObjectInstance mbean = null;
        Set<ObjectInstance> queryMBeans = mbeanServer.queryMBeans(new ObjectName("*:*,type=processors"), null);
        for (ObjectInstance objectInstance : queryMBeans) {
            if (objectInstance.getObjectName().toString().contains(SmooksProcessor.class.getSimpleName())) {
                mbean = objectInstance;
            }
        }
        assertNotNull(mbean);
        return mbean;
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                SmooksProcessor processor = new SmooksProcessor("edi-to-xml-smooks-config.xml", context);
                processor.setReportPath("target/smooks-report.html");

                from("direct:input").process(processor).convertBodyTo(Node.class).to("mock:result");
            }
        };
    }

    private String getExpectedOrderXml() throws IOException {
        return StreamUtils.readStream(new InputStreamReader(getClass().getResourceAsStream("/xml/expected-order.xml")));
    }

    private String getOrderEdi() throws IOException {
        return StreamUtils.readStream(new InputStreamReader(getClass().getResourceAsStream("/data/order.edi")));
    }

    private static class StringDataSource implements DataSource {
        private final String string;

        private StringDataSource(final String string) {
            this.string = string;

        }

        public String getContentType() {
            return "text/plain";
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(string.getBytes());
        }

        public String getName() {
            return "StringDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Method 'getOutputStream' is not implmeneted");
        }

    }
}
