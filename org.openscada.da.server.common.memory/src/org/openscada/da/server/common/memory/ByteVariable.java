/*******************************************************************************
 * Copyright (c) 2010, 2013 TH4 SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     TH4 SYSTEMS GmbH - initial API and implementation
 *     IBH SYSTEMS GmbH - refactor for generic memory devices
 *******************************************************************************/
package org.openscada.da.server.common.memory;

import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.mina.core.buffer.IoBuffer;
import org.eclipse.scada.core.Variant;
import org.eclipse.scada.utils.concurrent.InstantErrorFuture;
import org.eclipse.scada.utils.concurrent.InstantFuture;
import org.eclipse.scada.utils.concurrent.NotifyFuture;
import org.eclipse.scada.utils.osgi.pool.ManageableObjectPool;
import org.openscada.da.core.WriteResult;
import org.openscada.da.server.common.DataItem;

public class ByteVariable extends ScalarVariable
{
    public ByteVariable ( final String name, final int index, final Executor executor, final ManageableObjectPool<DataItem> itemPool, final Attribute... attributes )
    {
        super ( name, index, executor, itemPool, attributes );
    }

    @Override
    protected NotifyFuture<WriteResult> handleWrite ( final Variant value )
    {
        final MemoryRequestBlock block = this.block;
        if ( block == null )
        {
            return new InstantErrorFuture<> ( new IllegalStateException ( "Device is not connected" ) );
        }

        final Integer i = value.asInteger ( null );
        if ( i != null )
        {
            block.writeData ( toAddress ( this.index ), new byte[] { i.byteValue () } );
            return new InstantFuture<WriteResult> ( new WriteResult () );
        }
        else
        {
            return new InstantErrorFuture<WriteResult> ( new IllegalArgumentException ( String.format ( "Can only write doubles: %s is not a double", value ) ) );
        }
    }

    @Override
    protected Variant extractValue ( final IoBuffer data, final Map<String, Variant> attributes )
    {
        return Variant.valueOf ( data.get ( toAddress ( this.index ) ) );
    }

}
