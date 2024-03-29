/*-
 * ========================LICENSE_START=================================
 * Scribe :: Core
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
package org.smooks.cartridges.persistence.scribe.register;

/**
 * The DAO Register
 * <p>
 * Makes it possible to retrieve a default unnamed DAO or
 * one or more named DAO's.
 * <p>
 * DAO's retrieved from a DaoRegister should always be returned
 * to the DaoRegister by calling the {@link org.smooks.scribe.register.DaoRegister#returnDao(Object)}
 * method.
 *
 * @param <T> the DAO type
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 */
public interface DaoRegister<T> {

    /**
     * Returns the default DAO .
     *
     * @return the default DAO
     * @throws UnsupportedOperationException if the <tt>getDao()</tt> operation is
     *                                       not supported by this DaoRegister.
     */
    T getDefaultDao();

    /**
     * Returns the DAO with the specified name.
     *
     * @param name the name of the DAO
     * @return the DAO with the specified name
     * @throws UnsupportedOperationException if the <tt>getDao(String)</tt> operation is
     *                                       not supported by this DaoRegister.
     */
    T getDao(String name);


    /**
     * Returns the DAO to the register. This is
     * useful if the register has some
     * locking or pooling mechanism. If it isn't necessary
     * for DAO to be returned to the register then this
     * method shouldn't do anything.
     *
     * @param dao the DAO to return
     */
    void returnDao(T dao);

}
