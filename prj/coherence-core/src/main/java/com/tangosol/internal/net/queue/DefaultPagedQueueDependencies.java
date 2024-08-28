/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.queue;

/**
 * A default implementation of {@link PagedQueueDependencies}.
 *
 * @author Jonathan Knight 2002.09.10
 * @since 23.03
 */
public class DefaultPagedQueueDependencies
        extends DefaultNamedQueueDependencies
        implements PagedQueueDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link DefaultPagedQueueDependencies}.
     */
    public DefaultPagedQueueDependencies()
        {
        }

    /**
     * A copy constructor to create a {@link DefaultPagedQueueDependencies}
     * from another {@link PagedQueueDependencies} instance.
     *
     * @param deps  the {@link PagedQueueDependencies} to copy
     */
    public DefaultPagedQueueDependencies(PagedQueueDependencies deps)
        {
        super(deps);
        setPageCapacity(deps.getPageCapacity());
        }

    /**
     * Obtain the page capacity in bytes.
     *
     * @return the capacity
     */
    @Override
    public int getPageCapacity()
        {
        return m_cbPageCapacity;
        }

    /**
     * Set the page capacity in bytes.
     *
     * @param cbPageCapacity  the capacity
     */
    public void setPageCapacity(int cbPageCapacity)
        {
        m_cbPageCapacity = cbPageCapacity;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "PageQueueScheme Configuration: Page=" + m_cbPageCapacity + "b";
        }

    // ----- data members ---------------------------------------------------

    /**
     * The maximum number of elements that pages
     * in this queue can contain.
     */
    private int m_cbPageCapacity = 0;
    }
