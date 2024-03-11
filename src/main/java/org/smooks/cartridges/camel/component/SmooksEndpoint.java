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
package org.smooks.cartridges.camel.component;

import org.apache.camel.Component;
import org.apache.camel.Service;
import org.apache.camel.support.ProcessorEndpoint;
import org.smooks.api.SmooksException;
import org.smooks.cartridges.camel.processor.SmooksProcessor;

/**
 * SmooksEndpoint is a wrapper around a {@link SmooksProcessor} instance and
 * adds lifecycle support by implementing Service. This enables a SmooksEndpoint
 * to be stopped and started.
 * <p/>
 *
 * @author Daniel Bevenius
 */
public class SmooksEndpoint extends ProcessorEndpoint implements Service {
    private final SmooksProcessor smooksProcesor;

    public SmooksEndpoint(String endpointUri, Component component, SmooksProcessor processor) {
        super(endpointUri, component, processor);
        this.smooksProcesor = processor;
    }

    public void start() {
        try {
            smooksProcesor.start();
        } catch (Exception e) {
            throw new SmooksException(e.getMessage(), e);
        }
    }

    public void stop() {
        try {
            smooksProcesor.stop();
        } catch (Exception e) {
            throw new SmooksException(e.getMessage(), e);
        }
    }

}
