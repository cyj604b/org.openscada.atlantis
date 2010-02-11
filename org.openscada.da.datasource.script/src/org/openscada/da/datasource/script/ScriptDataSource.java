package org.openscada.da.datasource.script;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.openscada.ca.ConfigurationDataHelper;
import org.openscada.core.OperationException;
import org.openscada.core.Variant;
import org.openscada.da.client.DataItemValue;
import org.openscada.da.client.DataItemValue.Builder;
import org.openscada.da.core.WriteAttributeResults;
import org.openscada.da.core.WriteResult;
import org.openscada.da.datasource.WriteInformation;
import org.openscada.da.datasource.base.AbstractDataSource;
import org.openscada.utils.concurrent.InstantErrorFuture;
import org.openscada.utils.concurrent.NotifyFuture;
import org.openscada.utils.osgi.pool.ObjectPoolTracker;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptDataSource extends AbstractDataSource
{

    private final static Logger logger = LoggerFactory.getLogger ( ScriptDataSource.class );

    private final Executor executor;

    private final Map<String, DataSourceHandler> sources = new HashMap<String, DataSourceHandler> ();

    private final ScriptEngineManager manager;

    private SimpleScriptContext scriptContext;

    private String updateCommand;

    private ScriptEngine scriptEngine;

    private final ObjectPoolTracker poolTracker;

    private boolean disposed;

    public ScriptDataSource ( final ObjectPoolTracker poolTracker, final Executor executor )
    {
        this.poolTracker = poolTracker;
        this.executor = executor;

        this.manager = new ScriptEngineManager ();
    }

    @Override
    protected Executor getExecutor ()
    {
        return this.executor;
    }

    @Override
    public NotifyFuture<WriteAttributeResults> startWriteAttributes ( final WriteInformation writeInformation, final Map<String, Variant> attributes )
    {
        return new InstantErrorFuture<WriteAttributeResults> ( new OperationException ( "Not supported" ) );
    }

    @Override
    public NotifyFuture<WriteResult> startWriteValue ( final WriteInformation writeInformation, final Variant value )
    {
        return new InstantErrorFuture<WriteResult> ( new OperationException ( "Not supported" ) );
    }

    public synchronized void update ( final Map<String, String> parameters ) throws Exception
    {
        setScript ( parameters );
        setDataSources ( parameters );

        handleChange ();
    }

    private void setScript ( final Map<String, String> parameters ) throws ScriptException
    {
        final ConfigurationDataHelper cfg = new ConfigurationDataHelper ( parameters );
        final String engine = cfg.getString ( "engine", "JavaScript" );

        this.scriptContext = new SimpleScriptContext ();

        this.scriptEngine = this.manager.getEngineByName ( engine );
        if ( this.scriptEngine == null )
        {
            throw new IllegalArgumentException ( String.format ( "'%s' is not a valid script engine", engine ) );
        }

        // trigger init script
        final String initScript = parameters.get ( "init" );
        if ( initScript != null )
        {
            this.scriptEngine.eval ( initScript, this.scriptContext );
        }

        this.updateCommand = parameters.get ( "updateCommand" );
    }

    private void setDataSources ( final Map<String, String> parameters ) throws InvalidSyntaxException
    {
        clearSources ();

        for ( final Map.Entry<String, String> entry : parameters.entrySet () )
        {
            final String key = entry.getKey ();
            final String value = entry.getValue ();
            if ( key.startsWith ( "datasource." ) )
            {
                addDataSource ( key.substring ( "datasource.".length () ), value );
            }
        }
    }

    private void addDataSource ( final String datasourceKey, final String datasourceId ) throws InvalidSyntaxException
    {
        logger.info ( "Adding data source: {} -> {}", new Object[] { datasourceKey, datasourceId } );

        final DataSourceHandler dsHandler = new DataSourceHandler ( this.poolTracker, datasourceId, new DataSourceHandlerListener () {

            @Override
            public void handleChange ()
            {
                ScriptDataSource.this.handleChange ();
            }
        } );
        this.sources.put ( datasourceKey, dsHandler );
    }

    /**
     * Handle data change
     */
    protected synchronized void handleChange ()
    {
        if ( this.disposed )
        {
            return;
        }

        // calcuate
        if ( this.updateCommand != null )
        {
            // gather all data
            final Map<String, DataItemValue> values = new HashMap<String, DataItemValue> ();
            for ( final Map.Entry<String, DataSourceHandler> entry : this.sources.entrySet () )
            {
                values.put ( entry.getKey (), entry.getValue ().getValue () );
            }

            try
            {
                this.scriptContext.setAttribute ( "data", values, ScriptContext.ENGINE_SCOPE );
                setResult ( this.scriptEngine.eval ( this.updateCommand, this.scriptContext ) );
            }
            catch ( final Throwable e )
            {
                logger.warn ( "Failed to evaluate", e );
                logger.debug ( "Failed script: {}", this.updateCommand );
                setError ( e );
            }
        }
    }

    private synchronized void setError ( final Throwable e )
    {
        final Builder builder = new DataItemValue.Builder ();
        builder.setValue ( Variant.NULL );
        builder.setTimestamp ( Calendar.getInstance () );
        builder.setAttribute ( "script.error", Variant.TRUE );
        builder.setAttribute ( "script.error.message", new Variant ( e.getMessage () ) );
        this.updateData ( builder.build () );
    }

    private synchronized void setResult ( final Object result )
    {
        logger.debug ( "Setting result: {}", result );

        if ( result instanceof Builder )
        {
            logger.debug ( "Using builder" );
            this.updateData ( ( (Builder)result ).build () );
        }
        else if ( result instanceof DataItemValue )
        {
            logger.debug ( "Using data item value" );
            this.updateData ( ( (DataItemValue)result ) );
        }
        else
        {
            logger.debug ( "Falling back to plain value" );
            final Builder builder = new DataItemValue.Builder ();
            builder.setValue ( new Variant ( result ) );
            builder.setTimestamp ( Calendar.getInstance () );
            this.updateData ( builder.build () );
        }
    }

    public synchronized void dispose ()
    {
        this.disposed = true;
        clearSources ();
    }

    /**
     * Clear all datasources
     */
    protected void clearSources ()
    {
        for ( final DataSourceHandler source : this.sources.values () )
        {
            source.dispose ();
        }
        this.sources.clear ();
    }

}
