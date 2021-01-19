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

import org.mockito.Mock;
import org.smooks.Smooks;
import org.smooks.cartridges.persistence.test.dao.FullInterfaceDao;
import org.smooks.cartridges.persistence.test.dao.FullInterfaceMappedDao;
import org.smooks.cartridges.persistence.test.util.BaseTestCase;
import org.smooks.cartridges.persistence.util.PersistenceUtil;
import org.smooks.container.ExecutionContext;
import org.smooks.event.report.HtmlReportGenerator;
import org.smooks.payload.StringSource;
import org.smooks.scribe.register.MapDaoRegister;
import org.smooks.scribe.register.SingleDaoRegister;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
@Test(groups = "unit")
public class DaoFlusherTest extends BaseTestCase {

    private static final boolean ENABLE_REPORTING = false;

    private static final String SIMPLE_XML = "<root />";

    @Mock
    private FullInterfaceDao<Object> dao;

    @Mock
    private FullInterfaceMappedDao<Object> mappedDao;

    @Test
    public void test_dao_flush() throws Exception {
        Smooks smooks = new Smooks(getResourceAsStream("doa-flusher-01.xml"));

        try {
            ExecutionContext executionContext = smooks.createExecutionContext();

            PersistenceUtil.setDAORegister(executionContext, new SingleDaoRegister<Object>(dao));

            enableReporting(executionContext, "report_test_dao_flush.html");

            smooks.filterSource(executionContext, new StringSource(SIMPLE_XML), null);

            verify(dao).flush();
        } finally {
            smooks.close();
        }
    }

    @Test
    public void test_dao_flush_with_named_dao() throws Exception {

        Smooks smooks = new Smooks(getResourceAsStream("doa-flusher-02.xml"));

        try {
            Map<String, Object> daoMap = new HashMap<String, Object>();
            daoMap.put("dao1", dao);

            ExecutionContext executionContext = smooks.createExecutionContext();

            PersistenceUtil.setDAORegister(executionContext, MapDaoRegister.newInstance(daoMap));

            enableReporting(executionContext, "report_test_dao_flush_with_named_dao.html");

            smooks.filterSource(executionContext, new StringSource(SIMPLE_XML), null);

            verify(dao).flush();
        } finally {
            smooks.close();
        }
    }


    @Test
    public void test_dao_flush_with_flushBefore() throws Exception {
        Smooks smooks = new Smooks(getResourceAsStream("doa-flusher-03.xml"));

        try {
            Map<String, Object> daoMap = new HashMap<String, Object>();
            daoMap.put("mappedDao", mappedDao);
            daoMap.put("dao", dao);

            ExecutionContext executionContext = smooks.createExecutionContext();

            PersistenceUtil.setDAORegister(executionContext, MapDaoRegister.newInstance(daoMap));

            enableReporting(executionContext, "report_test_dao_flush_with_flushBefore.html");

            smooks.filterSource(executionContext, new StringSource(SIMPLE_XML), null);

            verify(dao).flush();
        } finally {
            smooks.close();
        }
    }


    /**
     * @param resource
     * @return
     */
    private InputStream getResourceAsStream(String resource) {
        return DaoFlusherTest.class.getResourceAsStream(resource);
    }

    private void enableReporting(ExecutionContext executionContext, String reportFilePath) throws IOException {
        if (ENABLE_REPORTING) {
            executionContext.getContentDeliveryRuntime().getExecutionEventListeners().add(new HtmlReportGenerator("target/" + reportFilePath));
        }
    }
}
