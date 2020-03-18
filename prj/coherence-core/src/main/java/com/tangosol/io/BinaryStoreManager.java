/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


/**
* A manager that is capable of creating and destroying BinaryStore objects.
*
* @since Coherence 2.4
* @author cp 2004.05.05
*/
public interface BinaryStoreManager
    {
    /**
    * Factory method: Returns a new BinaryStore.
    *
    * @return a new BinaryStore object
    */
    public BinaryStore createBinaryStore();

    /**
    * Lifecycle method: Destroy a BinaryStore previously created by this
    * manager.
    *
    * @param store  a BinaryStore object previously created by this
    *               manager
    */
    public void destroyBinaryStore(BinaryStore store);
    }