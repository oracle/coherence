/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.EventListener;


/**
* An AnyEvent is an event used when no specific event implementation fits
* and it is not worth making one.
*
* @author cp  1999.08.24
*/
public interface AnyListener
        extends EventListener
    {
    public void eventOccurred(AnyEvent e);
    }
