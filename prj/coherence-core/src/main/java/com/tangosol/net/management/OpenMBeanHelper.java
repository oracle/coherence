/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.oracle.coherence.common.base.Exceptions;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

/**
 * A set of utility method for dealing with OpenMBean APIs
 * because they stupidly have constructors that throw exceptions.
 *
 * @author Jonathan Knight  2022.03.23
 * @since 21.12.4
 */
public class OpenMBeanHelper
    {
    /**
     * Constructs a <code>CompositeType</code> instance, checking for the validity of the given parameters.
     * The validity constraints are described below for each parameter.
     * <p>
     * Note that the contents of the three array parameters
     * <var>itemNames</var>, <var>itemDescriptions</var> and <var>itemTypes</var>
     * are internally copied so that any subsequent modification of these arrays by the caller of this constructor
     * has no impact on the constructed <code>CompositeType</code> instance.
     * <p>
     * The Java class name of composite data values this composite type represents
     * (ie the class name returned by the {@link OpenType#getClassName() getClassName} method)
     * is set to the string value returned by <code>CompositeData.class.getName()</code>.
     *
     * @param  typeName  The name given to the composite type this instance represents; cannot be a null or empty string.
     *
     * @param  description  The human readable description of the composite type this instance represents;
     *                      cannot be a null or empty string.
     *
     * @param  itemNames  The names of the items contained in the
     *                    composite data values described by this <code>CompositeType</code> instance;
     *                    cannot be null and should contain at least one element; no element can be a null or empty string.
     *                    Note that the order in which the item names are given is not important to differentiate a
     *                    <code>CompositeType</code> instance from another;
     *                    the item names are internally stored sorted in ascending alphanumeric order.
     *
     * @param  itemDescriptions  The descriptions, in the same order as <var>itemNames</var>, of the items contained in the
     *                           composite data values described by this <code>CompositeType</code> instance;
     *                           should be of the same size as <var>itemNames</var>;
     *                           no element can be null or an empty string.
     *
     * @param  itemTypes  The open type instances, in the same order as <var>itemNames</var>, describing the items contained
     *                    in the composite data values described by this <code>CompositeType</code> instance;
     *                    should be of the same size as <var>itemNames</var>;
     *                    no element can be null.
     *
     * @throws IllegalArgumentException  If <var>typeName</var> or <var>description</var> is a null or empty string,
     *                                   or <var>itemNames</var> or <var>itemDescriptions</var> or <var>itemTypes</var> is null,
     *                                   or any element of <var>itemNames</var> or <var>itemDescriptions</var>
     *                                   is a null or empty string,
     *                                   or any element of <var>itemTypes</var> is null,
     *                                   or <var>itemNames</var> or <var>itemDescriptions</var> or <var>itemTypes</var>
     *                                   are not of the same size.
     */
    public static CompositeType createCompositeType(String        typeName,
                                                    String        description,
                                                    String[]      itemNames,
                                                    String[]      itemDescriptions,
                                                    OpenType<?>[] itemTypes)
        {
        try
            {
            return new CompositeType(typeName, description, itemNames, itemDescriptions, itemTypes);
            }
        catch (OpenDataException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Constructs a <code>TabularType</code> instance, checking for the validity of the given parameters.
     * The validity constraints are described below for each parameter.
     * <p>
     * The Java class name of tabular data values this tabular type represents
     * (ie the class name returned by the {@link OpenType#getClassName() getClassName} method)
     * is set to the string value returned by <code>TabularData.class.getName()</code>.
     *
     * @param  typeName  The name given to the tabular type this instance represents; cannot be a null or empty string.
     * <br>&nbsp;
     * @param  description  The human readable description of the tabular type this instance represents;
     *                      cannot be a null or empty string.
     * <br>&nbsp;
     * @param  rowType  The type of the row elements of tabular data values described by this tabular type instance;
     *                  cannot be null.
     * <br>&nbsp;
     * @param  indexNames  The names of the items the values of which are used to uniquely index each row element in the
     *                     tabular data values described by this tabular type instance;
     *                     cannot be null or empty. Each element should be an item name defined in <var>rowType</var>
     *                     (no null or empty string allowed).
     *                     It is important to note that the <b>order</b> of the item names in <var>indexNames</var>
     *                     is used by the methods {@link TabularData#get(java.lang.Object[]) get} and
     *                     {@link TabularData#remove(java.lang.Object[]) remove} of class
     *                     <code>TabularData</code> to match their array of values parameter to items.
     * <br>&nbsp;
     * @throws IllegalArgumentException  if <var>rowType</var> is null,
     *                                   or <var>indexNames</var> is a null or empty array,
     *                                   or an element in <var>indexNames</var> is a null or empty string,
     *                                   or <var>typeName</var> or <var>description</var> is a null or empty string.
     * <br>&nbsp;
     */
    public static TabularType createTabularType(String         typeName,
                                                String         description,
                                                CompositeType  rowType,
                                                String[]       indexNames)
        {
        try
            {
            return new TabularType(typeName, description, rowType, indexNames);
            }
        catch (OpenDataException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    }
