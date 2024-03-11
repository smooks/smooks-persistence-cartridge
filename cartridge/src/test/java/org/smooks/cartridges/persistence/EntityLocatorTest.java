/*-
 * ========================LICENSE_START=================================
 * smooks-persistence-cartridge
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
package org.smooks.cartridges.persistence;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.mockito.Mock;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksException;
import org.smooks.cartridges.persistence.test.dao.FullInterfaceDao;
import org.smooks.cartridges.persistence.test.util.BaseTestCase;
import org.smooks.cartridges.persistence.util.PersistenceUtil;
import org.smooks.engine.report.HtmlReportGenerator;
import org.smooks.scribe.register.MapDaoRegister;
import org.smooks.scribe.register.SingleDaoRegister;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
public class EntityLocatorTest extends BaseTestCase {
    private static final boolean ENABLE_REPORTING = false;

    @Mock
    private FullInterfaceDao<Object> dao;

    @SuppressWarnings("unchecked")
    @Test
    public void test_entity_locate() throws Exception {
        Object result = new Object();

        HashMap<String, Object> expectedArg3 = new HashMap<String, Object>();
        expectedArg3.put("d", new Integer(2));
        expectedArg3.put("e", new Integer(3));

        HashMap<String, Object> expectedMap = new HashMap<String, Object>();
        expectedMap.put("arg1", new Integer(1));
        expectedMap.put("arg2", new Integer(5));
        expectedMap.put("arg3", expectedArg3);
        expectedMap.put("arg4", "value");
        expectedMap.put("arg5", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2009-02-11 23:15:11"));

        when(dao.lookup(anyString(), anyMap())).thenReturn(result);

        Smooks smooks = new Smooks(getResourceAsStream("entity-locator-01.xml"));

        try {
            ExecutionContext executionContext = smooks.createExecutionContext();

            PersistenceUtil.setDAORegister(executionContext, new SingleDaoRegister<Object>(dao));

            enableReporting(executionContext, "test_entity_locate.html");

            Source source = new StreamSource(getClass().getResourceAsStream("input-message-01.xml"));
            smooks.filterSource(executionContext, source);

            assertSame(result, executionContext.getBeanContext().getBean("entity"));
        } finally {
            smooks.close();
        }

        verify(dao).lookup("something", expectedMap);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_entity_locate_query_no_result() throws Exception {
        Collection<?> result = Collections.emptyList();

        when(dao.lookupByQuery(anyString(), anyMap())).thenReturn(result);

        Smooks smooks = new Smooks(getResourceAsStream("entity-locator-02.xml"));

        try {
            ExecutionContext executionContext = smooks.createExecutionContext();

            //We put an object on the 'entity' location to check if the locater removes it because it found
            //no result
            executionContext.getBeanContext().addBean("entity", new Object(), null);

            PersistenceUtil.setDAORegister(executionContext, new SingleDaoRegister<Object>(dao));

            enableReporting(executionContext, "test_entity_locate_query_no_result.html");

            Source source = new StreamSource(getClass().getResourceAsStream("input-message-01.xml"));
            smooks.filterSource(executionContext, source);

            assertNull(executionContext.getBeanContext().getBean("entity"));
        } finally {
            smooks.close();
        }

        verify(dao).lookupByQuery(eq("from SomeThing"), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_entity_locate_no_result_but_expected() throws Exception {
        Collection<?> result = Collections.emptyList();

        when(dao.lookupByQuery(anyString(), anyMap())).thenReturn(result);

        Smooks smooks = new Smooks(getResourceAsStream("entity-locator-03.xml"));

        try {
            ExecutionContext executionContext = smooks.createExecutionContext();

            PersistenceUtil.setDAORegister(executionContext, new SingleDaoRegister<Object>(dao));

            Source source = new StreamSource(getClass().getResourceAsStream("input-message-01.xml"));

            smooks.filterSource(executionContext, source);

        } catch (SmooksException e) {
            assertSame(ExceptionUtils.getCause(e).getClass(), NoLookupResultException.class);

            return;
        } finally {
            smooks.close();
        }

        fail("NoLookupResultException was not thrown.");
    }

    @Test
    public void test_entity_locate_query_positional_parameter() throws Exception {
        Collection<?> result = Collections.emptyList();

        when(dao.lookupByQuery(anyString(), anyString(), anyString())).thenReturn(result);

        Smooks smooks = new Smooks(getResourceAsStream("entity-locator-04.xml"));

        try {
            ExecutionContext executionContext = smooks.createExecutionContext();

            PersistenceUtil.setDAORegister(executionContext, new SingleDaoRegister<Object>(dao));

            Source source = new StreamSource(getClass().getResourceAsStream("input-message-01.xml"));

            enableReporting(executionContext, "test_entity_locate_query_positional_parameter.html");

            smooks.filterSource(executionContext, source);

        } finally {
            smooks.close();
        }

        verify(dao).lookupByQuery(eq("from SomeThing where arg1=:1 and arg2=:2"), eq("value-1"), eq("value-2"));
    }

    @Test
    public void test_entity_locate_positional_parameter() throws Exception {
        Collection<?> result = Collections.emptyList();

        when(dao.lookup(anyString(), anyString(), anyString())).thenReturn(result);

        Smooks smooks = new Smooks(getResourceAsStream("entity-locator-05.xml"));

        try {
            ExecutionContext executionContext = smooks.createExecutionContext();

            PersistenceUtil.setDAORegister(executionContext, MapDaoRegister.builder().put("some", dao).build());

            Source source = new StreamSource(getClass().getResourceAsStream("input-message-01.xml"));

            enableReporting(executionContext, "test_entity_locate_positional_parameter.html");

            smooks.filterSource(executionContext, source);

        } finally {
            smooks.close();
        }

        verify(dao).lookup(eq("test"), eq("value-1"), eq("value-2"));
    }

    /**
     * @param resource
     * @return
     */
    private InputStream getResourceAsStream(String resource) {
        return EntityLocatorTest.class.getResourceAsStream(resource);
    }

    private void enableReporting(ExecutionContext executionContext, String reportFilePath) throws IOException {
        if (ENABLE_REPORTING) {
            executionContext.getContentDeliveryRuntime().addExecutionEventListener(new HtmlReportGenerator("target/" + reportFilePath, executionContext.getApplicationContext()));
        }
    }
}
