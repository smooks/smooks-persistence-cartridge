/*-
 * ========================LICENSE_START=================================
 * Scribe :: MyBatis adapter
 * %%
 * Copyright (C) 2020 - 2023 Smooks
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
package org.smooks.scribe.adapter.mybatis;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.smooks.scribe.adapter.mybatis.test.util.BaseTestCase;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
public class SqlSessionDaoAdapterTestCase extends BaseTestCase {

    @Mock
    private SqlSession sqlSession;

    private SqlSessionDaoAdapter adapter;

    @Test
    public void test_persist() throws SQLException {

        // EXECUTE

        Object toPersist = new Object();

        // VERIFY

        adapter.insert("id", toPersist);

        verify(sqlSession).insert(eq("id"), same(toPersist));

    }

    @Test
    public void test_merge() throws SQLException {

        // EXECUTE

        Object toMerge = new Object();

        Object merged = adapter.update("id", toMerge);

        // VERIFY

        verify(sqlSession).update(eq("id"), same(toMerge));

        assertNull(merged);

    }


    @Test
    public void test_lookup_map_parameters() throws SQLException {

        // STUB

        List listResult = Collections.emptyList();

        when(sqlSession.selectList(anyString(), any())).thenReturn(listResult);

        // EXECUTE

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("key1", "value1");
        params.put("key2", "value2");

        Collection<Object> result = adapter.lookup("name", params);

        // VERIFY

        assertSame(listResult, result);

        verify(sqlSession).selectList(eq("name"), same(params));


    }

    @Test
    public void test_lookup_array_parameters() throws SQLException {

        // STUB

        List listResult = Collections.emptyList();

        when(sqlSession.selectList(anyString(), any())).thenReturn(listResult);

        // EXECUTE

        Object[] params = new Object[2];
        params[0] = "value1";
        params[1] = "value2";

        Collection<Object> result = adapter.lookup("name", params);

        // VERIFY

        assertSame(listResult, result);

        verify(sqlSession).selectList(eq("name"), same(params));

    }


    /* (non-Javadoc)
     * @see org.smooks.scribe.test.util.BaseTestCase#beforeMethod()
     */
    @Before
    @Override
    public void beforeMethod() {
        super.beforeMethod();

        adapter = new SqlSessionDaoAdapter(sqlSession);
    }

}
