/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

/**
 * {@link NameManglers} contain singleton access to both a
 * {@link FieldMangler} and {@link MethodMangler}. NameManglers provide the
 * ability to derive the same name of a property regardless of their access
 * or inspection methodology.
 *
 * @author hr
 *
 * @since 3.7.1
 */
public class NameManglers
    {
    // ----- inner class: FieldMangler --------------------------------------

    /**
     * A {@link NameMangler} implementation that is aware of field naming
     * conventions and is able to convert from a field name to a generic
     * name.
     * <p>
     * The conventions this mangler is aware of are prefixing variables with
     * {@code m_} or {@code f}. For example {@code m_bar} and {@code fBar}
     * would both be converted to a mangled name of {@code bar}.
     *
     * @author hr
     *
     * @since 3.7.1
     */
    public static class FieldMangler
            implements NameMangler
        {

        // ----- NamedMangler interface -------------------------------------

        /**
         * {@inheritDoc}
         */
        public String mangle(String sName)
            {
            if (sName == null)
                {
                return sName;
                }

            String sMangled = sName;
            int    cName    = sName.length();
            if ((sName.startsWith("m_") || sName.startsWith("f_")) && cName > 2)
                {
                // handle the case where we have a variable name of m_fFoo
                // which should evaluate the name to be foo
                int iName = cName > 3 && Character.isUpperCase(sName.charAt(3))
                                ? 3 : 2;
                sMangled = Character.toLowerCase(sName.charAt(iName)) +
                            (cName > iName + 1 ? sName.substring(iName + 1) : "");
                }
            else if (sName.charAt(0) == 'f' && Character.isUpperCase(sName.charAt(1)))
                {
                // if this is fBar then strip the f and lowercase the B
                sMangled = Character.toLowerCase(sName.charAt(1)) +
                            (sName.length() > 2 ? sName.substring(2) : "");
                }

            return sMangled;
            }
        }

    // ----- inner class: MethodMangler -------------------------------------

    /**
     * A {@link NameMangler} implementation that is aware of method naming
     * conventions and is able to convert from a method name to a generic
     * name.
     * <p>
     * The conventions this mangler is aware of are the getter and setter
     * style methods, e.g. {@code getBar} or {@code setBar} would both be
     * converted to a mangled name of {@code bar}.
     *
     * @author hr
     * 
     * @since 3.7.1
     */
    public static class MethodMangler
            implements NameMangler
        {

        // ----- NamedMangler interface -------------------------------------

        /**
         * {@inheritDoc}
         */
        public String mangle(String sName)
            {
            if (sName == null)
                {
                return sName;
                }

            String sMangled = sName;
            int    cName    = sName.length();
            if (cName > 3 && (sName.startsWith("get") || sName.startsWith("set")))
                {
                // lowercase the char after get or set
                sMangled = Character.toLowerCase(sName.charAt(3)) +
                            (cName > 4 ? sName.substring(4) : "");
                }
            else if(cName > 2 && sName.startsWith("is"))
                {
                // lowercase the char after is
                sMangled = Character.toLowerCase(sName.charAt(2)) +
                            (cName > 3 ? sName.substring(3) : "");
                }
            return sMangled;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton FieldMangler reference.
     */
    public static final FieldMangler FIELD_MANGLER = new FieldMangler();

    /**
     * Singleton MethodMangler reference.
     */
    public static final MethodMangler METHOD_MANGLER = new MethodMangler();
    }
