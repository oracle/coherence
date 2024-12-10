/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest.data;

import com.tangosol.util.processor.NumberMultiplier;

/**
 * Simple extension of NumberMultiplier that multiplies by 2 for testing
 * custom processors via REST.
 *
 * @author jh  2012.02.27
 */
public class CustomNumberDoubler
        extends NumberMultiplier
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for EL and POF serialization).
     */
    public CustomNumberDoubler()
        {
        super();
        }

    /**
     * Construct a CustomNumberDoubler for the specified property name.
     *
     * @param sName  a property name
     */
    public CustomNumberDoubler(String sName)
        {
        super(sName, Integer.valueOf(2), false);
        }
    }
