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
package org.smooks.cartridges.persistence.parameter;


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
public class NamedParameterContainer implements ParameterContainer<NamedParameter> {

    private HashMap<String, Object> parameterMap;

    private Entry<String, Object>[] parameterEntries;

    @SuppressWarnings("unchecked")
    public NamedParameterContainer(final NamedParameterIndex index) {
        parameterEntries = new Entry[index.size()];
        parameterMap = new HashMap<String, Object>();

        updateParameterMap(index);
    }

    public void put(NamedParameter param, Object bean) {
        parameterEntries[param.getIndex()].setValue(bean);
    }

    public boolean containsParameter(NamedParameter param) {
        int index = param.getIndex();

        return parameterEntries.length > index && parameterEntries[index].getValue() != null;
    }


    public Object get(NamedParameter param) {
        return parameterEntries[param.getIndex()].getValue();
    }

    public Object get(String name) {
        return parameterMap.get(name);
    }

    public Object remove(NamedParameter param) {

        Object old = get(param);

        if (old != null) {
            parameterEntries[param.getIndex()].setValue(null);
        }

        return old;
    }

    public void clear() {
        for (Entry<String, Object> parameter : parameterEntries) {
            parameter.setValue(null);
        }

    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getParameterMap() {
        return (Map<String, Object>) parameterMap.clone();
    }

    private void updateParameterMap(final NamedParameterIndex index) {

        for (String name : index.getIndexMap().keySet()) {

            if (!parameterMap.containsKey(name)) {
                parameterMap.put(name, null);
            }
        }
        updateParameterEntries(index);
    }

    private void updateParameterEntries(final NamedParameterIndex index) {

        for (Entry<String, Object> parameterMapEntry : parameterMap.entrySet()) {

            NamedParameter parameter = index.getParameter(parameterMapEntry.getKey());

            parameterEntries[parameter.getIndex()] = parameterMapEntry;
        }
    }
}
