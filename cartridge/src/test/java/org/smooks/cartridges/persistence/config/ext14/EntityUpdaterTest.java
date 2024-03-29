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
package org.smooks.cartridges.persistence.config.ext14;

import org.junit.Test;
import org.mockito.Mock;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.cartridges.persistence.test.util.BaseTestCase;
import org.smooks.cartridges.persistence.util.PersistenceUtil;
import org.smooks.engine.report.HtmlReportGenerator;
import org.smooks.io.payload.JavaResult;
import org.smooks.io.payload.StringSource;
import org.smooks.scribe.Dao;
import org.smooks.scribe.register.SingleDaoRegister;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
public class EntityUpdaterTest extends BaseTestCase {

    private static final boolean ENABLE_REPORTING = false;

    private static final String SIMPLE_XML = "<root />";

    @Mock
    private Dao<String> dao;

    @Test
    public void test_entity_update_no_selector() throws Exception {

        Smooks smooks = new Smooks(getResourceAsStream("entity-updater-no-selector.xml"));

        try {
            ExecutionContext executionContext = smooks.createExecutionContext();

            PersistenceUtil.setDAORegister(executionContext, new SingleDaoRegister<Object>(dao));

            enableReporting(executionContext, "report_test_entity_update_no_selector.html");

            JavaResult result = new JavaResult();
            smooks.filterSource(executionContext, new StringSource(SIMPLE_XML), result);

            verify(dao).update(same((String) result.getBean("toUpdate")));
        } finally {
            smooks.close();
        }
    }

    /**
     * @param resource
     * @return
     */
    private InputStream getResourceAsStream(String resource) {
        return EntityUpdaterTest.class.getResourceAsStream(resource);
    }

    private void enableReporting(ExecutionContext executionContext, String reportFilePath) throws IOException {
        if (ENABLE_REPORTING) {
            executionContext.getContentDeliveryRuntime().addExecutionEventListener(new HtmlReportGenerator("target/" + reportFilePath, executionContext.getApplicationContext()));
        }
    }

}
