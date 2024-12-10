/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import java.util.Comparator;

/**
 * ComparatorHelper is a utility class that provides a factory method used to
 * create a <tt>java.util.Comparator</tt> instance from a string expression.
 * <p>
 * The string expression should be in the form of comma-separated values,
 * where each value represents the name of property whose value is used as
 * input to the <tt>Comparator#compare(Object, Object)</tt> method and an
 * optional order string ("asc" or "desc"). If not specified, ascending order
 * will be assumed.
 * <p>
 * For example, the string <tt>name:asc,dateOfBirth:desc</tt> will create
 * the following comparator chain:
 * <p>
 * <code>
 * new ChainedComparator(new Comparator[]
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;{
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;new ExtractorComparator(new ReflectionExtractor("getName")),
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;new InverseComparator(new ExtractorComparator(new ReflectionExtractor("getDateOfBirth")))
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;});
 * </code>
 *
 * @author ic  2011.06.30
 */
public class ComparatorHelper
    {

    // ----- factory methods ------------------------------------------------

    /**
     * Create a new Comparator from the specified string.
     *
     * @param sExpr  a string expression representing a Comparator. Contains
     *               a list of property name/ordering pairs, separated by a
     *               colon. For example:
     *               <ul>
     *                 <li><tt>name:asc,dateOfBirth:desc</tt></li>
     *                 <li><tt>address.state,address.zip:desc</tt></li>
     *               </ul>
     *
     * @return Comparator representing specified string expression
     */
    public static Comparator createComparator(String sExpr)
        {
        if (sExpr == null)
            {
            return null;
            }
        ComparatorBuilder builder = new ComparatorBuilder();

        String[] asProperties = sExpr.trim().split(",");
        for (String sProperty : asProperties)
            {
            String[] asSort = sProperty.trim().split(":");
            switch (asSort.length)
                {
                case 1:
                    builder.asc(asSort[0]);
                    break;

                case 2:
                    String sOrder = asSort[1].trim();
                    if ("asc".equalsIgnoreCase(sOrder))
                        {
                        builder.asc(asSort[0].trim());
                        }
                    else if ("desc".equalsIgnoreCase(sOrder))
                        {
                        builder.desc(asSort[0].trim());
                        }
                    else
                        {
                        throw new IllegalArgumentException(
                                "bad ordering expression [" + sOrder +
                                "]. Valid values are (asc|desc)+");
                        }
                    break;

                    default:
                        throw new IllegalArgumentException(
                                "unable to create comparator from expression [" +
                                sExpr + "]");
                }
            }

        return builder.build();
        }
    }