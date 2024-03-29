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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.smooks.io.payload.Exports;
import org.smooks.io.payload.StringResult;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.camel.test.junit5.TestSupport.deleteDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class SmooksProcessor_File_In_StringResult_Test extends CamelTestSupport {

    @Test
    public void test() throws Exception {
        deleteDirectory("target/smooks");
        template.sendBody("file://target/smooks", "<blah />");

        MockEndpoint mockEndpoint = getMockEndpoint("mock:a");
        mockEndpoint.expectedMessageCount(1);

        assertIsSatisfied(mockEndpoint);

        assertEquals("<blah/>", mockEndpoint.getExchanges().get(0).getIn().getBody(String.class));
    }

    /* (non-Javadoc)
     * @see org.apache.camel.test.junit4.CamelTestSupport#createRouteBuilder()
     */
    @Override
    protected RouteBuilder createRouteBuilder() {

        return new RouteBuilder() {
            public void configure() {
                from("file://target/smooks").
                        process(new SmooksProcessor(new Smooks().setExports(new Exports(StringResult.class)), context)).
                        to("mock:a");
            }
        };
    }
}
