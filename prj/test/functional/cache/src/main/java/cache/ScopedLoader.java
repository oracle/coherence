/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;


/**
* {@link ClassLoader} implementation used for functional testing.
*
* @author pp 2011.7.29
*/
public class ScopedLoader
        extends ClassLoader
    {
    /**
    * Create a new class loader with the specified scope.
    *
    * @param sScope  the name of the scope to create a class loader for
    * @param parent  the parent class loader
    */
    ScopedLoader(String sScope, ClassLoader parent)
        {
        super(parent);
        m_sScope = sScope;
        }

    /**
    * Return the scope name of this loader.
    *
    * @return the scope name of this loader
    */
    protected String getScopeName()
        {
        return m_sScope;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString() + "(Scope: " + m_sScope + ")";
        }

    /**
    * The name of the logical scope that this class-loader defines.
    */
    protected String m_sScope;
    }
