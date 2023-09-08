/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.DataOutput;
import java.io.DataInput;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.Serializable;


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
* <p>
* <b>Note:</b> it is extremely important that objects that are equivalent
* according to their "equals()" implementation produce equivalent serialized
* streams. Violating this relationship will result in non-deterministic behavior
* for many Coherence services.
* <p>
* Here is an example of how to write a custom ExternalizableLite deserialization method
* that incorporates calling a constructor to set members that do not have a setter.
* <pre>
* public class NotAPojo {
*     public NotAPojo(int field) {this.field = field;}
*     public int getField()      {return field;}
*     private int field;
* }
* </pre>
* <pre>
* {@code @ExternalizableType(serializer = Serializer.class)}
* public class NotAPojoSerializable extends NotAPojo implements ExternalizableLite {
*     public NotAPojoSerializable(int field) { super(field); }
*
*     static public class Serializer implements ExternalizableLiteSerializer {
*         public T deserialize(DataInput in) throws IOException {
*             return new NotAPojoSerializable(in.readInt());
*         }
*
*         public void serialize(DataOutput out,T value) throws IOException {
*             out.writeInt(value.getField());
*        }
* }
* </pre>
*
* @see ExternalizableLiteSerializer
* @see ExternalizableType
*
* @author gg 2003.02.08
*
* @since Coherence 2.1
*/
public interface ExternalizableLite
        extends Serializable
    {
    /**
    * Restore the contents of this object by loading the object's state from
    * the passed DataInput object.
    *
    * @param in  the DataInput stream to read data from in order to restore
    *            the state of this object
    *
    * @exception IOException         if an I/O exception occurs
    * @exception NotActiveException  if the object is not in its initial
    *            state, and therefore cannot be deserialized into
    */
    default public void readExternal(DataInput in) throws IOException
        {}

    /**
    * Save the contents of this object by storing the object's state into
    * the passed DataOutput object.
    *
    * @param out  the DataOutput stream to write the state of this object to
    *
    * @exception IOException if an I/O exception occurs
    */
    default public void writeExternal(DataOutput out) throws IOException
        {}
    }
