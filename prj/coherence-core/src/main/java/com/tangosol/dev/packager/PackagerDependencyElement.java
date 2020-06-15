/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import java.util.List;


/**
* This interface defines the required behavior of particpants in a
* packaging traversal.  A Packager will specify what to package up in
* terms of a root set of PackagerDependencyElements and a set of
* PackagerSets that contain things that should not be included in the
* new PackagerSet.
*
* All classes that implement PackagerDependencyElement must implement
* an equals() method and a hashCode() method that define an equivalence
* relation based on content, so that cyclic dependencies can be detected
* properly.
*/
public interface PackagerDependencyElement
    {
    /**
    * Return the immediate dependents of this PackagerDependencyElement
    * in the context of the specified ClassLoader.
    */
    public abstract List getDependents(ClassLoader classLoader);

    /**
    * Return the direct PackagerEntries of this PackagerDependencyElement.
    */
    public abstract List getPackagerEntries();
    }
