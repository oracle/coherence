/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* Optimized serialization.
* <p>
* The readExternal and writeExternal methods of the ExternalizableLite
* interface are implemented by a class to give the class complete
* control over the format and contents of the stream for an object. Unlike 
* the {@link java.io.Externalizable} interface, these methods operate on more
* primitive DataInput and DataOutput streams, allowing for significantly
* better performance, but often requiring more effort to implement.
* <p>
* These methods must explicitly coordinate with the supertype to save its
* state. When an ExternalizableLite object is reconstructed, an instance is
* created using the public no-arg constructor, then the readExternal method
* called. Therefore, in addition to the methods listed below, a public
* default constructor is required.
* <p>
* Since a graph traversal is not managed by the stream, only uni-directional
* object graphs (e.g. object trees) can be serialized using this approach.
*
* @author gg 2003.02.08
*
* @deprecated As of release 3.0, replaced by {@link com.tangosol.io.ExternalizableLite}
*
* @since Coherence 2.1
*/
public interface ExternalizableLite
        extends com.tangosol.io.ExternalizableLite
    {
    }
