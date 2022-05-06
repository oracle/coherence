/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package util;

import com.oracle.coherence.common.base.Associated;

import com.oracle.coherence.common.util.AssociationPile;
import com.tangosol.net.cache.KeyAssociation;

/**
 * Simple {@link KeyAssociation} implementation.
 *
 * @author jh  2014.06.02
 */
public class SimpleAssociation
        implements Associated
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public SimpleAssociation()
        {
        }

    /**
     * Construct SimpleAssociation for the specified key.
     */
    public SimpleAssociation(Object oKey)
        {
        associate(oKey);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Associate this object with the specified key
     *
     * @param oKey  the key to associate with this object
     *
     * @return this object
     */
    public SimpleAssociation associate(Object oKey)
        {
        m_oKey = oKey;
        return this;
        }

    // ----- Associated implementation --------------------------------------

    @Override
    public Object getAssociatedKey()
        {
        return m_oKey;
        }

    @Override
    public String toString()
        {
        Object oKey = m_oKey;
        return "SimpleAssociation: " +
            (oKey == AssociationPile.ASSOCIATION_ALL ? "ALL" : oKey);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The key associated with this object or null if this object has no
     * association.
     */
    private Object m_oKey;
    }
