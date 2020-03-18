/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.util.EventListener;
import java.beans.PropertyVetoException;


/**
* The VetoableSubChangeListener event interface provides notification of
* impending (vetoable) changes to sub-traits and is also used to notify
* listeners of vetoed changes ("undos").
*
* This interface represents vetoable events; the change notifications
* are made before the changes are made.
*
* @version 0.50, 07/27/98  created from SubChangeListener
* @author  Cameron Purdy
*/
public interface VetoableSubChangeListener
        extends EventListener
    {
    /**
    * Invoked before a sub-trait is modified, added, removed, or un-removed.
    *
    * @param evt  a SubChangeEvent object describing the event source and
    *             the sub trait as well as the action being taken
    */
    void vetoableSubChange(SubChangeEvent evt)
        throws PropertyVetoException;
    }
