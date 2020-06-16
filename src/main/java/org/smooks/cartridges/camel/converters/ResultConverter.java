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
package org.smooks.cartridges.camel.converters;

import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.spi.TypeConverterRegistry;
import org.smooks.payload.JavaResult;
import org.smooks.payload.StringResult;
import org.w3c.dom.Node;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * ResultConverter converts from different {@link Result} types.
 * 
 * @author Daniel Bevenius
 */
@Converter(generateLoader = true)
public class ResultConverter
{
    public static final String SMOOKS_RESULT_KEY = "SmooksResultKeys";
    
    private ResultConverter()
    {
    }

    @Converter
    public static Node toDocument(DOMResult domResult)
    {
        return domResult.getNode();
    }

    @SuppressWarnings("rawtypes")
    @Converter
    public static List toList(JavaResult.ResultMap javaResult, Exchange exchange)
    {
        String resultKey = (String) exchange.getProperty(SMOOKS_RESULT_KEY);
        if (resultKey != null)
        {
            return (List) getResultsFromJavaResult(javaResult, resultKey);
        } else
        {
            return (List) getSingleObjectFromJavaResult(javaResult);
        }
    }

    @SuppressWarnings("rawtypes")
	@Converter
    public static Integer toInteger(JavaResult.ResultMap result)
    {
        return (Integer) getSingleObjectFromJavaResult(result);
    }

    @SuppressWarnings("rawtypes")
	@Converter
    public static Double toDouble(JavaResult.ResultMap result)
    {
        return (Double) getSingleObjectFromJavaResult(result);
    }

    @Converter
    public static String toString(StringResult result)
    {
        return result.getResult();
    }

    
    @SuppressWarnings("rawtypes")
	public static Map toMap(JavaResult.ResultMap resultBeans, Exchange exchange)
    {
        Message outMessage = exchange.getOut();
        outMessage.setBody(resultBeans);

        @SuppressWarnings("unchecked")
		Set<Entry<String, Object>> entrySet = resultBeans.entrySet();
        for (Entry<String, Object> entry : entrySet)
        {
            outMessage.setBody(entry.getValue(), entry.getValue().getClass());
        }
        return resultBeans;
    }

    @SuppressWarnings("rawtypes")
	private static Object getResultsFromJavaResult(JavaResult.ResultMap resultMap, String resultKey)
    {
        return resultMap.get(resultKey);
    }

    private static Object getSingleObjectFromJavaResult(@SuppressWarnings("rawtypes") JavaResult.ResultMap resultMap)
    {
        if (resultMap.size() == 1)
        {
            return resultMap.values().iterator().next();
        }
        return null;
    }

    @Converter
    public static StreamSource toStreamSource(StringResult stringResult)
    {
        String result = stringResult.getResult();
        if (result != null)
        {
            StringReader stringReader = new StringReader(result);
            return new StreamSource(stringReader);
        }
        
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Converter(fallback = true)
    public static <T> T convertTo(Class<T> type, Exchange exchange, Object value, TypeConverterRegistry registry) {
        if(value instanceof JavaResult.ResultMap) {
            for(Object mapValue : ((Map) value).values()) {
                if(type.isInstance(mapValue)) {
                    return type.cast(mapValue);
                }
            }
        } 

        return null;
    }
}
