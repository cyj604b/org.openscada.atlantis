package org.openscada.net.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openscada.net.base.data.LongValue;
import org.openscada.net.base.data.Message;
import org.openscada.net.io.net.Connection;

public class LongRunningController implements MessageListener
{
    private static Logger _log = Logger.getLogger ( LongRunningController.class );
    
    public enum State
    {
        REQUESTED,
        RUNNING,
        FAILURE,
        SUCCESS,
    }
    
    public interface Listener
    {
        void stateChanged ( State state, Message reply, Throwable error );
    }
    
    private Set<Integer> _commandCodes = new HashSet<Integer> ();
    private int _stopCommandCode = 0; 
    private ConnectionHandlerBase _connectionHandler = null;
    
    private Map<Long, LongRunningOperation> _opMap = new HashMap<Long, LongRunningOperation> ();
    
    public LongRunningController ( ConnectionHandlerBase connectionHandler, int stopCommandCode, int commandCode )
    {
        _connectionHandler = connectionHandler;
        _stopCommandCode = stopCommandCode;
        _commandCodes.add ( commandCode );
    }
    
    public LongRunningController ( ConnectionHandlerBase connectionHandler, int stopCommandCode, Set<Integer> commandCodes )
    {
        _connectionHandler = connectionHandler;
        _stopCommandCode = stopCommandCode;
        _commandCodes.addAll ( commandCodes );
    }
    
    public LongRunningController ( ConnectionHandlerBase connectionHandler, int stopCommandCode, Integer... commandCodes )
    {
        _connectionHandler = connectionHandler;
        _stopCommandCode = stopCommandCode;
        _commandCodes.addAll ( Arrays.asList ( commandCodes ) );
    }
    
    public void register ( MessageProcessor processor )
    {
        for ( Integer commandCode : _commandCodes )
        {
            processor.setHandler ( commandCode, this );
        }
    }
    
    public void unregister ( MessageProcessor processor )
    {
        for ( Integer commandCode : _commandCodes )
        {
            processor.unsetHandler ( commandCode );
        }
    }
    
    synchronized public LongRunningOperation start ( Message message, Listener listener )
    {
        if ( message == null )
            return null;
        
        final LongRunningOperation op = new LongRunningOperation ( this, listener, _stopCommandCode );
        
        _connectionHandler.getConnection ().sendMessage ( message, new MessageStateListener () {

            public void messageReply ( Message message )
            {
                if ( message.getValues ().containsKey ( "id" ) )
                    if ( message.getValues ().get ( "id" ) instanceof LongValue )
                    {
                        long id = ((LongValue)message.getValues ().get ( "id" )).getValue ();
                        op.granted ( id );
                        assignOperation ( id, op );
                        return;
                    }
                // else
                op.fail ();
            }

            public void messageTimedOut ()
            {
                op.fail ();
            }} );
        
        if ( listener != null )
            listener.stateChanged ( State.REQUESTED, null, null );
        
        return op;
    }
    
    synchronized public void stop ( LongRunningOperation op )
    {
        if ( op == null )
            return;
        
        op.stop ();
        
        _opMap.remove ( op.getId () );
    }
    
    synchronized private void assignOperation ( long id, LongRunningOperation op )
    {
        _opMap.put ( id, op );
    }

    public void messageReceived ( Connection connection, Message message )
    {
        long id = 0;
        
        if ( message.getValues ().containsKey ( "id" ) )
            if ( message.getValues ().get ( "id" ) instanceof LongValue )
                id = ((LongValue)message.getValues ().get ("id") ).getValue ();
        
        _log.info ( String.format ( "Received long-op reply with id %d", id ) );
        
        if ( id != 0 )
        {
            LongRunningOperation op = null;
            synchronized ( _opMap )
            {
                op = _opMap.get ( id );
                _opMap.remove ( id );
            }
            
            if ( op != null )
            {
                op.result ( message );
            }
            else
            {
                _log.warn ( "Received long-op message for unregistered operation" );
            }
        }
    }
    
    
}
