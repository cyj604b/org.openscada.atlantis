/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2008 inavare GmbH (http://inavare.com)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.openscada.da.server.common.chain;

import org.openscada.core.Variant;
import org.openscada.sec.UserInformation;

public interface WriteHandler
{
    /**
     * Handle the write call
     * <p>
     * e.g. performs a write call to a subsystem
     * @param userInformation the user information of the write call.
     * May also be <code>null<code> if there is no user information.
     * @param value the value to write
     * @throws Exception if anything goes wrong
     */
    public abstract void handleWrite ( UserInformation userInformation, Variant value ) throws Exception;
}
