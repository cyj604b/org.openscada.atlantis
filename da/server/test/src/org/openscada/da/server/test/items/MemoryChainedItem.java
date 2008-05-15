/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006 inavare GmbH (http://inavare.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.openscada.da.server.test.items;

import java.util.EnumSet;
import java.util.Map;

import org.openscada.core.Variant;
import org.openscada.da.core.IODirection;
import org.openscada.da.server.common.DataItemInformationBase;
import org.openscada.da.server.common.HiveServiceRegistry;
import org.openscada.da.server.common.chain.AttributeBinder;
import org.openscada.da.server.common.chain.BaseChainItemCommon;
import org.openscada.da.server.common.chain.ChainItem;
import org.openscada.da.server.common.chain.ChainProcessEntry;
import org.openscada.da.server.common.chain.MemoryItemChained;
import org.openscada.da.server.common.chain.item.SumErrorChainItem;
import org.openscada.da.server.common.factory.FactoryHelper;
import org.openscada.da.server.common.impl.HiveCommon;

public class MemoryChainedItem extends MemoryItemChained
{
    
    private class AddClassAttributeBinder implements AttributeBinder
    {
        private MemoryChainedItem _item = null;
        private IODirection _direction = null;
        
        public AddClassAttributeBinder ( MemoryChainedItem item, IODirection direction )
        {
            super ();
            _item = item;
            _direction = direction;
        }
        
        public void bind ( Variant value ) throws Exception
        {
           if ( value != null )
               if ( !value.isNull () )
                   _item.addChainElement ( _direction, value.asString () );
        }

        public Variant getAttributeValue ()
        {
            return null;
        }
        
    }
    
    private class RemoveClassAttributeBinder implements AttributeBinder
    {
        private MemoryChainedItem _item = null;
        private IODirection _direction = null;
        
        public RemoveClassAttributeBinder ( MemoryChainedItem item, IODirection direction )
        {
            super ();
            _item = item;
            _direction = direction;
        }
        
        public void bind ( Variant value ) throws Exception
        {
            if ( value != null )
                if ( !value.isNull () )
                    _item.removeChainElement ( _direction, value.asString () );
        }

        public Variant getAttributeValue ()
        {
            return null;
        }
    }
    
    private class InjectChainItem extends BaseChainItemCommon
    {
        private MemoryChainedItem _item = null;
        
        public InjectChainItem ( HiveServiceRegistry serviceRegistry, MemoryChainedItem item )
        {
            super ( serviceRegistry );
            _item = item;
            
            addBinder ( "org.openscada.da.test.chain.input.add", new AddClassAttributeBinder ( item, IODirection.INPUT ) );
            addBinder ( "org.openscada.da.test.chain.input.remove", new RemoveClassAttributeBinder ( item, IODirection.INPUT ) );
            addBinder ( "org.openscada.da.test.chain.outpt.add", new AddClassAttributeBinder ( item, IODirection.OUTPUT ) );
            addBinder ( "org.openscada.da.test.chain.output.remove", new RemoveClassAttributeBinder ( item, IODirection.OUTPUT ) );
            setReservedAttributes ( "org.openscada.da.test.chain.value" );
        }
        
        @Override
        public boolean isPersistent ()
        {
            return false;
        }
        
        public void process ( Variant value, Map<String, Variant> attributes )
        {
            int i = 0;
            StringBuilder str = new StringBuilder ();
            for ( ChainProcessEntry item : _item.getChainCopy () )
            {
                if ( i > 0 )
                    str.append ( ", " );
                
                str.append ( item.getWhat ().getClass ().getCanonicalName () );
                str.append ( "(" );
                str.append ( item.getWhen ().toString () );
                str.append ( ")" );
                
                i++;
            }
            attributes.put ( "org.openscada.da.test.chain.value", new Variant ( str.toString () ) );
        }
        
    }

    private HiveCommon hive;
    
    public MemoryChainedItem ( HiveCommon hive, String id )
    {
        super ( new DataItemInformationBase ( id, EnumSet.of ( IODirection.INPUT, IODirection.OUTPUT ) ) );
        this.hive = hive;
        addChainElement ( IODirection.INPUT, new InjectChainItem ( hive, this ) );
        addChainElement ( IODirection.INPUT, new SumErrorChainItem ( hive ) );
    }
   
    public void addChainElement ( IODirection direction, String className ) throws Exception
    {
        Class<?> itemClass = Class.forName ( className );
        Object o = itemClass.newInstance ();
        
       FactoryHelper.createChainItem ( this.hive, Class.forName ( className ) );

        addChainElement ( direction, (ChainItem )o );
    }
    
    synchronized public void removeChainElement ( IODirection direction, String className ) throws Exception
    {
        for ( ChainProcessEntry entry : getChainCopy () )
        {
            if ( entry.getWhat ().getClass ().getCanonicalName ().equals ( className ) )
            {
                if ( entry.getWhen ().equals ( EnumSet.of ( direction ) ) )
                    removeChainElement ( entry.getWhen (), entry.getWhat () );
                return;
            }
        }
        throw new Exception ( "Item not found" );
    }

}
