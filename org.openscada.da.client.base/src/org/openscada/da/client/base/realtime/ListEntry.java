/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2008 inavare GmbH (http://inavare.com)
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

package org.openscada.da.client.base.realtime;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.openscada.core.Variant;
import org.openscada.core.subscription.SubscriptionState;
import org.openscada.da.client.DataItem;
import org.openscada.da.client.DataItemValue;
import org.openscada.da.client.ItemManager;
import org.openscada.da.ui.connection.data.Item;

public class ListEntry extends Observable implements Observer, IAdaptable, IPropertySource
{

    private enum Properties
    {
        ITEM_ID,
        CONNECTION_URI,
        VALUE,
        SUBSCRIPTION_STATE
    };

    public class AttributePair
    {
        public String key;

        public Variant value;

        public AttributePair ( final String key, final Variant value )
        {
            super ();
            this.key = key;
            this.value = value;
        }

        @Override
        public int hashCode ()
        {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ( this.key == null ? 0 : this.key.hashCode () );
            result = PRIME * result + ( this.value == null ? 0 : this.value.hashCode () );
            return result;
        }

        @Override
        public boolean equals ( final Object obj )
        {
            if ( this == obj )
            {
                return true;
            }
            if ( obj == null )
            {
                return false;
            }
            if ( getClass () != obj.getClass () )
            {
                return false;
            }
            final AttributePair other = (AttributePair)obj;
            if ( this.key == null )
            {
                if ( other.key != null )
                {
                    return false;
                }
            }
            else if ( !this.key.equals ( other.key ) )
            {
                return false;
            }
            if ( this.value == null )
            {
                if ( other.value != null )
                {
                    return false;
                }
            }
            else if ( !this.value.equals ( other.value ) )
            {
                return false;
            }
            return true;
        }
    }

    private DataItem dataItem;

    private Item item;

    public Item getItem ()
    {
        return this.item;
    }

    public DataItem getDataItem ()
    {
        return this.dataItem;
    }

    public synchronized void setDataItem ( final Item item, final ItemManager itemManager )
    {
        clear ();
        this.item = item;
        this.dataItem = new DataItem ( item.getId (), itemManager );
        this.dataItem.addObserver ( this );
    }

    public synchronized void clear ()
    {
        this.item = null;
        if ( this.dataItem != null )
        {
            this.dataItem.deleteObserver ( this );
            this.dataItem.unregister ();
        }
    }

    public Variant getValue ()
    {
        return this.dataItem.getSnapshotValue ().getValue ();
    }

    public SubscriptionState getSubscriptionChange ()
    {
        return this.dataItem.getSnapshotValue ().getSubscriptionState ();
    }

    public synchronized List<AttributePair> getAttributes ()
    {
        final DataItemValue value = this.dataItem.getSnapshotValue ();

        final List<AttributePair> pairs = new ArrayList<AttributePair> ( value.getAttributes ().size () );
        for ( final Map.Entry<String, Variant> entry : value.getAttributes ().entrySet () )
        {
            pairs.add ( new AttributePair ( entry.getKey (), entry.getValue () ) );
        }
        return pairs;
    }

    /**
     * check if attributes are in the list
     * @return <code>true</code> if the attributes list is not empty
     */
    public synchronized boolean hasAttributes ()
    {
        return !this.dataItem.getSnapshotValue ().getAttributes ().isEmpty ();
    }

    public Throwable getSubscriptionError ()
    {
        return this.dataItem.getSnapshotValue ().getSubscriptionError ();
    }

    public void update ( final Observable o, final Object arg )
    {
        setChanged ();
        notifyObservers ( arg );
    }

    @SuppressWarnings ( "unchecked" )
    public Object getAdapter ( final Class adapter )
    {
        if ( adapter == Item.class )
        {
            return new Item ( this.item );
        }
        return null;
    }

    // IPropertySource Methods

    public Object getEditableValue ()
    {
        return this.item.getId ();
    }

    public IPropertyDescriptor[] getPropertyDescriptors ()
    {
        final List<IPropertyDescriptor> result = new LinkedList<IPropertyDescriptor> ();

        {
            final PropertyDescriptor pd = new PropertyDescriptor ( Properties.ITEM_ID, "ID" );
            pd.setCategory ( "Item" );
            pd.setAlwaysIncompatible ( true );
            result.add ( pd );
        }
        {
            final PropertyDescriptor pd = new PropertyDescriptor ( Properties.CONNECTION_URI, "Connection" );
            pd.setCategory ( "Item" );
            pd.setAlwaysIncompatible ( true );
            result.add ( pd );
        }
        {
            final PropertyDescriptor pd = new PropertyDescriptor ( Properties.VALUE, "Value" );
            pd.setCategory ( "State" );
            pd.setAlwaysIncompatible ( true );
            result.add ( pd );
        }
        {
            final PropertyDescriptor pd = new PropertyDescriptor ( Properties.SUBSCRIPTION_STATE, "Subscription" );
            pd.setCategory ( "State" );
            pd.setAlwaysIncompatible ( true );
            result.add ( pd );
        }

        return result.toArray ( new IPropertyDescriptor[0] );
    }

    public Object getPropertyValue ( final Object id )
    {
        if ( id instanceof Properties )
        {
            switch ( (Properties)id )
            {
            case ITEM_ID:
                return this.item.getId ();
            case CONNECTION_URI:
                return this.item.getConnectionString ();
            case VALUE:
                return this.dataItem.getSnapshotValue ();
            case SUBSCRIPTION_STATE:
                return this.dataItem.getSnapshotValue ().getSubscriptionState ();
            }
        }
        return null;
    }

    public boolean isPropertySet ( final Object id )
    {
        return false;
    }

    public void resetPropertyValue ( final Object id )
    {
    }

    public void setPropertyValue ( final Object id, final Object value )
    {
    }
}
