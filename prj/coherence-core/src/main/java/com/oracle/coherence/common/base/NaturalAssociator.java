/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


import java.util.LinkedList;
import java.util.List;


/**
 * NaturalAssociator provides an Associator implementation for objects that
 * implement the {@link Associated} interface.
 *
 * @author gg 2012.03.11
 */
public class NaturalAssociator
        implements Associator
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAssociatedKey(Object o)
        {
        if (o instanceof Associated)
            {
            for (int i = 0; i < 7; i++)
                {
                o = ((Associated) o).getAssociatedKey();

                if (!(o instanceof Associated))
                    {
                    return o;
                    }
                }

            // we should never get here, but since we did,
            // it is almost definitely a circular association case
            return validateAssociated((Associated) o);
            }
        else
            {
            return null;
            }
        }


    // ----- internal helpers -----------------------------------------------

    /**
     * Check if given Associated object generates a circular association.
     *
     * @param assoc  an Associated object to check
     *
     * @return the key object that is associated with the specified object,
     *         or null if there is no association
     *
     * @throws RuntimeException if there is a circular key association chain,
     *         or if the maximum association depth has been reached
     */
    protected Object validateAssociated(Associated assoc)
        {
        List<Associated> listKeys = new LinkedList<Associated>();
        int  cDepth   = 0;

        while (true)
            {
            if (listKeys.contains(assoc))
                {
                throw new RuntimeException(cDepth == 1 ?
                        "self-associated object: " + assoc :
                        "circular association chain: " + listKeys);
                }
            if (++cDepth > 777)
                {
                throw new RuntimeException("maximum association depth reached");
                }
            listKeys.add(assoc);

            Object oAssocNext = assoc.getAssociatedKey();
            if (oAssocNext instanceof Associated)
                {
                assoc = (Associated) oAssocNext;
                }
            else
                {
                return oAssocNext;
                }
            }
        }
    }