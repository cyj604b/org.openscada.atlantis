/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2009 inavare GmbH (http://inavare.com)
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

package org.openscada.core.subscription;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Subscription
{
    private final Map<SubscriptionInformation, Object> listeners = new HashMap<SubscriptionInformation, Object> ();

    private SubscriptionSource source = null;

    private Object topic = null;

    public Subscription ( final Object topic )
    {
        super ();
        this.topic = topic;
    }

    /**
     * Check if the subscription is empty or nor.
     * 
     * A subscription is empty if it neither has a subcription source set nor listeners
     * attached to it.
     * 
     * @return <code>true</code> if the subscription is empty, <code>false</code> otherwise
     */
    public synchronized boolean isEmpty ()
    {
        return ( this.source == null ) && this.listeners.isEmpty ();
    }

    /**
     * Check if the subscription is in granted state. This means that no source
     * is connected but there are listeners attached.
     * @return <code>true</code> if the subscription is in granted state, <code>false</code> otherwise
     */
    public synchronized boolean isGranted ()
    {
        return ( this.source == null ) && !this.listeners.isEmpty ();
    }

    public synchronized void subscribe ( final SubscriptionListener listener, final Object hint )
    {
        final SubscriptionInformation subscriptionInformation = new SubscriptionInformation ( listener, hint );

        if ( this.listeners.containsKey ( subscriptionInformation ) )
        {
            return;
        }
        this.listeners.put ( subscriptionInformation, hint );

        if ( this.source == null )
        {
            listener.updateStatus ( this.topic, SubscriptionState.GRANTED );
        }
        else
        {
            listener.updateStatus ( this.topic, SubscriptionState.CONNECTED );
            this.source.addListener ( Arrays.asList ( new SubscriptionInformation[] { subscriptionInformation } ) );
        }
    }

    public synchronized void unsubscribe ( final SubscriptionListener listener )
    {
        final SubscriptionInformation subscriptionInformation = new SubscriptionInformation ( listener, null );
        if ( this.listeners.containsKey ( subscriptionInformation ) )
        {
            final Object hint = this.listeners.remove ( subscriptionInformation );
            subscriptionInformation.setHint ( hint );

            if ( this.source != null )
            {
                this.source.removeListener ( Arrays.asList ( new SubscriptionInformation[] { subscriptionInformation } ) );
            }

            listener.updateStatus ( this.topic, SubscriptionState.DISCONNECTED );
        }
    }

    public synchronized void setSource ( final SubscriptionSource source )
    {
        // We only act on changes
        if ( this.source == source )
        {
            return;
        }

        if ( this.source != null )
        {
            this.source.removeListener ( this.listeners.keySet () );
        }

        final Set<SubscriptionInformation> keys = this.listeners.keySet ();
        if ( source != null )
        {
            for ( final SubscriptionInformation information : keys )
            {
                information.getListener ().updateStatus ( this.topic, SubscriptionState.CONNECTED );
            }
            if ( !keys.isEmpty () )
            {
                source.addListener ( keys );
            }
        }
        else
        {
            for ( final SubscriptionInformation information : keys )
            {
                information.getListener ().updateStatus ( this.topic, SubscriptionState.GRANTED );
            }
        }

        this.source = source;
    }
}
