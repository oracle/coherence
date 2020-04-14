/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import java.util.EventListener;


/**
* The SubChangeListener event interface provides notification of changes to
* sub-traits.
*
* This interface represents non-vetoable events; the change notifications
* are made only after the changes are made.
*
* @version 0.50, 07/24/98  created from TraitChangeListener
* @author  Cameron Purdy
*/
public interface SubChangeListener
        extends EventListener
    {
    /**
    * Invoked after a sub-trait is modified, added, removed, or un-removed.
    *
    * @param evt  a SubChangeEvent object describing the event source and
    *             the sub trait as well as the action being taken
    */
    void subChange(SubChangeEvent evt);
    }
