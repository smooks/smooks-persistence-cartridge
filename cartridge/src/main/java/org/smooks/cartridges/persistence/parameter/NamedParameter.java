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


import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
public class NamedParameter implements Parameter<NamedParameterIndex> {

    private final int index;

    private final String name;

    private final NamedParameterIndex containerIndex;

    private int hashCode;

    protected NamedParameter(NamedParameterIndex repositoryIndex, int index, String name) {
        this.containerIndex = repositoryIndex;
        this.index = index;
        this.name = name;

        hashCode = new HashCodeBuilder()
                .append(index)
                .append(containerIndex)
                .toHashCode();
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }


    public NamedParameterIndex getContainerIndex() {
        return containerIndex;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof NamedParameter == false) {
            return false;
        }
        NamedParameter rhs = (NamedParameter) obj;
        if (hashCode != hashCode) {
            return false;
        } else if (index != rhs.index) {
            return false;
        } else if (containerIndex.equals(rhs.containerIndex)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }

}
