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
import org.smooks.cartridges.persistence.test.dao.FullInterfaceDao;
import org.smooks.cartridges.persistence.test.util.BaseTestCase;
import org.smooks.cartridges.persistence.util.PersistenceUtil;
import org.smooks.engine.report.HtmlReportGenerator;
import org.smooks.io.payload.JavaResult;
import org.smooks.io.payload.StringSource;
import org.smooks.scribe.register.MapDaoRegister;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 *
 */
@SuppressWarnings("unchecked")
public class EntityLocatorTest extends BaseTestCase {
	private static final boolean ENABLE_REPORTING = false;

    private static final String SIMPLE_XML =  "<root />";

	@Mock
	private FullInterfaceDao<Object> dao;

	@Test
	public void test_entity_locate_no_selector() throws Exception {
		String searchResult = "Test";
        List<String> searchResultList = new ArrayList<String>();
        searchResultList.add(searchResult);

		when(dao.lookup(anyString(), anyString())).thenReturn(searchResultList);

		Smooks smooks = new Smooks(getResourceAsStream("entity-locator-no-selector.xml"));


        try {
            ExecutionContext executionContext = smooks.createExecutionContext();

            PersistenceUtil.setDAORegister(executionContext, MapDaoRegister.builder().put("some", dao).build());

            enableReporting(executionContext, "test_entity_locate_no_selector.html");

            JavaResult result = new JavaResult();

            smooks.filterSource(executionContext, new StringSource(SIMPLE_XML), result);

            verify(dao).lookup(eq("test"), eq("value-1"));

            List<String> resultList = (List<String>) result.getBean("theList");

            assertNotNull(resultList);
            assertSame(searchResult, resultList.get(0));

        }finally {
            smooks.close();
        }


	}

	private InputStream getResourceAsStream(String resource) {
		return EntityLocatorTest.class.getResourceAsStream(resource);
	}

	private void enableReporting(ExecutionContext executionContext, String reportFilePath) throws IOException {
		if(ENABLE_REPORTING) {
			executionContext.getContentDeliveryRuntime().getExecutionEventListeners().add(new HtmlReportGenerator("target/" + reportFilePath));
		}
	}
}
