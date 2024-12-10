/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.Serializer;


/**
* The PofContext interface represents a set of user types that can be
* serialized to and deserialized from a POF stream.
*
* @author cp/jh  2006.07.11
*
* @since Coherence 3.2
*/
public interface PofContext
        extends Serializer
    {
    /**
    * Return a PofSerializer that can be used to serialize and deserialize an
    * object of the specified user type to and from a POF stream.
    *
    * @param nTypeId  the type identifier of the user type that can be
    *                 serialized and deserialized using the returned
    *                 PofSerializer; must be non-negative
    *
    * @return a PofSerializer for the specified user type
    *
    * @throws IllegalArgumentException if the specified user type is
    *         negative or unknown to this PofContext
    */
    public PofSerializer getPofSerializer(int nTypeId);

    /**
    * Determine the user type identifier associated with the given object.
    *
    * @param o  an instance of a user type; must not be null
    *
    * @return the type identifier of the user type associated with the given
    *         object
    *
    * @throws IllegalArgumentException if the user type associated with the
    *         given object is unknown to this PofContext or if the object is
    *         null
    */
    public int getUserTypeIdentifier(Object o);

    /**
    * Determine the user type identifier associated with the given class.
    *
    * @param clz  a user type class; must not be null
    *
    * @return the type identifier of the user type associated with the given
    *         class
    *
    * @throws IllegalArgumentException if the user type associated with the
    *         given class is unknown to this PofContext or if the class is
    *         null
    */
    public int getUserTypeIdentifier(Class clz);

    /**
    * Determine the user type identifier associated with the given class
    * name.
    *
    * @param sClass  the name of a user type class; must not be null
    *
    * @return the type identifier of the user type associated with the given
    *         class name
    *
    * @throws IllegalArgumentException if the user type associated with the
    *         given class name is unknown to this PofContext or if the class
    *         name is null
    */
    public int getUserTypeIdentifier(String sClass);

    /**
    * Determine the name of the class associated with the given user type
    * identifier.
    *
    * @param nTypeId  the user type identifier; must be non-negative
    *
    * @return the name of the class associated with the specified user type
    *         identifier
    *
    * @throws IllegalArgumentException if the specified user type is
    *         negative or unknown to this PofContext
    */
    public String getClassName(int nTypeId);

    /**
    * Determine the class associated with the given user type identifier.
    *
    * @param nTypeId  the user type identifier; must be non-negative
    *
    * @return the class associated with the specified user type identifier
    *
    * @throws IllegalArgumentException if the specified user type is
    *         negative or unknown to this PofContext
    */
    public Class getClass(int nTypeId);

    /**
    * Determine if the given object is of a user type known to this
    * PofContext.
    *
    * @param o  the object to test; must not be null
    *
    * @return true iff the specified object is of a valid user type
    *
    * @throws IllegalArgumentException if the given object is null
    */
    public boolean isUserType(Object o);

    /**
    * Determine if the given class is a user type known to this PofContext.
    *
    * @param clz  the class to test; must not be null
    *
    * @return true iff the specified class is a valid user type
    *
    * @throws IllegalArgumentException if the given class is null
    */
    public boolean isUserType(Class clz);

    /**
    * Determine if the class with the given name is a user type known to this
    * PofContext.
    *
    * @param sClass  the name of the class to test; must not be null
    *
    * @return true iff the class with the specified name is a valid user type
    *
    * @throws IllegalArgumentException if the given class name is null
    */
    public boolean isUserType(String sClass);

    /**
     * Return <code>true</code> if {@link PofReader#readObject(int)} method should
     * return the appropriate Java 8 date/time type, or <code>false</code> if a legacy
     * date/time types should be returned in order to preserve backwards
     * compatibility.
     *
     * @return <code>true</code> if Java 8 date/time types should be preferred over
     *         legacy types
     */
    public default boolean isPreferJavaTime()
        {
        return false;
        }
    }
