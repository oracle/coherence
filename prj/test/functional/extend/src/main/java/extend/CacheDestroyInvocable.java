/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;

import java.io.IOException;

import java.lang.InterruptedException;
import java.lang.Thread;

/**
 * Invocable implementation that destroys a specific cache.
 *
 * @author par  2013.3.29
 *
 * @since @BUILDVERSION@
 */
public class CacheDestroyInvocable
        implements Invocable, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public CacheDestroyInvocable()
        {
        super();
        }

    // ----- Invocable interface --------------------------------------------

     /**
    * {@inheritDoc}
     */
    public void init(InvocationService service)
        {
        }

    /**
     * {@inheritDoc}
     */
    public void run()
        {
        NamedCache cache = CacheFactory.getCache(getCacheName());
        CacheFactory.destroyCache(cache);

        try
            {
            // wait for cache to die
            Thread.sleep(2000);
            }
        catch (InterruptedException e)
            {
            // ignore
            }
        }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
        {
        return "Test";
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in)
            throws IOException
        {
        setCacheName(in.readString(0));
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, getCacheName());
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the name of the cache to destroy.
     *
     * @return the cache name
     */
    public String getCacheName()
        {
        return m_sCacheName;
        }

    /**
     * Configure the cache to destroy.
     *
     * @param name  the name of the cache to destroy
     */
    public void setCacheName(String sName)
        {
        m_sCacheName = sName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of cache to destroy
     */
    protected String m_sCacheName;
    }