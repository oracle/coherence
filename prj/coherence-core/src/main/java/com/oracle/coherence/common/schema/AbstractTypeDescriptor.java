/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;


import com.oracle.coherence.common.schema.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * An abstract base class for {@link TypeDescriptor} implementations.
 *
 * @author as  2013.08.28
 */
public abstract class AbstractTypeDescriptor<T extends TypeDescriptor>
        implements TypeDescriptor<T>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code AbstractTypeDescriptor} instance.
     *
     * @param name  the type name
     */
    protected AbstractTypeDescriptor(String name)
        {
        this(null, name, false);
        }

    /**
     * Construct {@code AbstractTypeDescriptor} instance.
     *
     * @param namespace  the type namespace
     * @param name       the type name
     */
    protected AbstractTypeDescriptor(String[] namespace, String name)
        {
        this(namespace, name, false);
        }

    /**
     * Construct {@code AbstractTypeDescriptor} instance.
     *
     * @param namespace  the type namespace
     * @param name       the type name
     * @param fArray     the flag specifying whether the type is an array type
     */
    protected AbstractTypeDescriptor(String[] namespace, String name, boolean fArray)
        {
        this(namespace, name, fArray, null);
        }

    /**
     * Construct {@code AbstractTypeDescriptor} instance.
     *
     * @param namespace    the type namespace
     * @param name         the type name
     * @param fArray       the flag specifying whether the type is an array type
     * @param genericArgs  the list of generic argument descriptors
     */
    protected AbstractTypeDescriptor(String[] namespace, String name,
                                     boolean fArray, List<T> genericArgs)
        {
        m_namespace   = namespace;
        m_name        = name;
        m_fArray      = fArray;
        m_genericArgs = genericArgs;

        if (!fArray)
            {
            m_arrayType = createArrayType(namespace, name);
            }
        }

    // ---- abstract methods ------------------------------------------------

    /**
     * Create an array type for a given namespace and type name.
     *
     * @param namespace  the type namespace
     * @param name       the type name
     *
     * @return an array type for a given namespace and type name
     */
    protected abstract T createArrayType(String[] namespace, String name);

    /**
     * Return a parser for a string representation of this type descriptor.
     *
     * @return a parser for a string representation of this type descriptor
     */
    protected abstract Parser getParser();

    // ---- TypeDescriptor implementation -----------------------------------

    @Override
    public String getNamespace()
        {
        return m_namespace == null
               ? null
               : String.join(getParser().getSeparator(), m_namespace);
        }

    @Override
    public String getName()
        {
        return m_name;
        }

    @Override
    public String getFullName()
        {
        String ns = getNamespace();
        return ns == null ? m_name : ns + getParser().getSeparator() + m_name;
        }

    @Override
    public boolean isArray()
        {
        return m_fArray;
        }

    @Override
    public boolean isGenericType()
        {
        return m_genericArgs != null && m_genericArgs.size() > 0;
        }

    @Override
    public List<T> getGenericArguments()
        {
        return m_genericArgs;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        AbstractTypeDescriptor<?> that = (AbstractTypeDescriptor<?>) o;

        if (m_fArray != that.m_fArray)
            {
            return false;
            }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(m_namespace, that.m_namespace))
            {
            return false;
            }
        if (m_name != null ? !m_name.equals(that.m_name) : that.m_name != null)
            {
            return false;
            }
        if (m_arrayType != null ? !m_arrayType.equals(that.m_arrayType) : that.m_arrayType != null)
            {
            return false;
            }
        return m_genericArgs != null ? m_genericArgs.equals(that.m_genericArgs) : that.m_genericArgs == null;
        }

    @Override
    public int hashCode()
        {
        int result = Arrays.hashCode(m_namespace);
        result = 31 * result + (m_name != null ? m_name.hashCode() : 0);
        result = 31 * result + (m_fArray ? 1 : 0);
        result = 31 * result + (m_arrayType != null ? m_arrayType.hashCode() : 0);
        result = 31 * result + (m_genericArgs != null ? m_genericArgs.hashCode() : 0);
        return result;
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Return the namespace components as an array.
     *
     * @return the namespace components as an array
     */
    public String[] getNamespaceComponents()
        {
        return m_namespace;
        }

    /**
     * Set the type namespace.
     *
     * @param namespace  an array of the type namespace components
     */
    public void setNamespace(String[] namespace)
        {
        m_namespace = namespace;
        }

    /**
     * Set the type namespace.
     *
     * @param namespace  the type namespace
     */
    public void setNamespace(String namespace)
        {
        m_namespace = namespace == null
                      ? null
                      : StringUtils.split(namespace, getParser().getSeparator());
        }

    /**
     * Set the type name.
     *
     * @param name  the type name
     */
    public void setName(String name)
        {
        m_name = name;
        }

    /**
     * Return {@code true} if the full name of the specified type is equal to
     * the full name of the type represented by this type descriptor.
     *
     * @param other  the type descriptor to compare with
     *
     * @return {@code true} if the full name of the specified type is equal to
     *         the full name of the type represented by this type descriptor;
     *         {@code false} otherwise
     */
    public boolean isNameEqual(AbstractTypeDescriptor other)
        {
        return m_name.equals(other.m_name)
                && Arrays.equals(m_namespace, other.m_namespace);
        }

    /**
     * Return the array type represented by this type descriptor.
     *
     * @return the array type represented by this type descriptor
     */
    protected T getArrayType()
        {
        return m_arrayType;
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        if (m_namespace != null)
            {
            sb.append(getNamespace());
            sb.append(getParser().getSeparator());
            }

        sb.append(m_name);

        if (m_genericArgs != null)
            {
            sb.append('(');
            for (int i = 0; i < m_genericArgs.size(); i++)
                {
                if (i > 0)
                    {
                    sb.append(", ");
                    }
                sb.append(m_genericArgs.get(i));
                }
            sb.append(')');
            }

        if (m_fArray)
            {
            sb.append("[]");
            }

        return sb.toString();
        }

    // ---- abstract class: Parser ------------------------------------------

    /**
     * An abstract base class for type descriptor parser implementations.
     *
     * @param <T>  the type of {@code TypeDescriptor}; must extend {@code
     *             AbstractTypeDescriptor}
     */
    @SuppressWarnings("unchecked")
    protected static abstract class Parser<T extends AbstractTypeDescriptor>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code Parser} instance.
         *
         * @param separator  the namespace separator
         */
        protected Parser(String separator)
            {
            m_separator = separator;
            }

        // ---- abstract methods --------------------------------------------

        /**
         * Return the descriptor for a standard (built-in) data type.
         *
         * @param type  the fully qualified type name
         *
         * @return the descriptor for a standard (built-in) data type
         */
        protected abstract T getStandardType(String type);

        /**
         * Create the {@link TypeDescriptor} instance.
         *
         * @param namespace    the type namespace
         * @param name         the type name
         * @param fArray       the flag specifying whether the type is an array type
         * @param genericArgs  the list of generic argument descriptors
         *
         * @return the {@code TypeDescriptor} instance
         */
        protected abstract T createTypeDescriptor(
                String[] namespace, String name, boolean fArray, List<T> genericArgs);

        // ---- public API --------------------------------------------------

        /**
         * Return the namespace separator.
         *
         * @return the namespace separator
         */
        public String getSeparator()
            {
            return m_separator;
            }

        /**
         * Parse string representation of a type name in a format used in XML
         * schema definition.
         *
         * @param type  the fully qualified type name in XML schema-compatible
         *              format
         *
         * @return the {@code TypeDescriptor} for the specified type name
         */
        public T parse(String type)
            {
            boolean fArray = type.endsWith("[]");
            if (fArray)
                {
                type = type.substring(0, type.length() - 2);
                }

            List<T> genericArgs = null;
            boolean fGeneric = type.contains("(");
            if (fGeneric)
                {
                genericArgs = parseGenericArgs(
                        type.substring(type.indexOf("(") + 1, type.lastIndexOf(")")));
                type = type.substring(0, type.indexOf("("));
                }

            return createTypeDescriptor(type, fArray, genericArgs);
            }

        /**
         * Parse string representation of a type name in a Java internal format.
         *
         * @param type  the fully qualified type name in internal format
         *
         * @return the {@code TypeDescriptor} for the specified type name
         */
        public T parseInternal(String type)
            {
            boolean fArray = type.charAt(0) == '[';
            if (fArray)
                {
                type = type.substring(1);
                }

            List<T> genericArgs = null;
            boolean fGeneric = type.contains("<");
            if (fGeneric)
                {
                genericArgs = parseGenericArgsInternal(
                        type.substring(type.indexOf("<") + 1, type.lastIndexOf(">")));
                type = type.substring(0, type.indexOf("<")) + ";";
                }

            if (type.startsWith("L") && type.endsWith(";"))
                {
                type = type.substring(1, type.length() - 1);
                }
            type = type.replaceAll("/", getSeparator());

            if (!fGeneric && type.length() == 1)
                {
                T primitiveType = getPrimitiveType(type);
                if (primitiveType != null)
                    {
                    return fArray ? (T) primitiveType.getArrayType() : primitiveType;
                    }
                }

            return createTypeDescriptor(type, fArray, genericArgs);
            }

        // ---- helper methods ----------------------------------------------

        /**
         * Return the {@link TypeDescriptor} for a primitive type based on the
         * internal type name.
         *
         * @param type  the internal Java type name of a primitive type
         *
         * @return the {@link TypeDescriptor} for a primitive type based on the
         *         internal type name
         */
        protected T getPrimitiveType(String type)
            {
            if ("Z".equals(type))
                {
                return getStandardType("boolean");
                }
            if ("B".equals(type))
                {
                return getStandardType("byte");
                }
            if ("C".equals(type))
                {
                return getStandardType("char");
                }
            if ("S".equals(type))
                {
                return getStandardType("short");
                }
            if ("I".equals(type))
                {
                return getStandardType("int");
                }
            if ("J".equals(type))
                {
                return getStandardType("long");
                }
            if ("F".equals(type))
                {
                return getStandardType("float");
                }
            if ("D".equals(type))
                {
                return getStandardType("double");
                }

            return null;
            }

        /**
         * Create the {@link TypeDescriptor} instance.
         *
         * @param type         the fully qualified type name
         * @param fArray       the flag specifying whether the type is an array type
         * @param genericArgs  the list of generic argument descriptors
         *
         * @return the {@code TypeDescriptor} instance
         */
        protected T createTypeDescriptor(
                String type, boolean fArray, List<T> genericArgs)
            {
            T standardType = getStandardType(type);
            if (standardType != null)
                {
                return fArray ? (T) standardType.getArrayType() : standardType;
                }

            String[] tmp = StringUtils.split(type, getSeparator());
            String[] namespace = null;
            if (tmp.length > 1)
                {
                namespace = new String[tmp.length - 1];
                System.arraycopy(tmp, 0, namespace, 0, tmp.length - 1);
                }
            String name = tmp[tmp.length - 1];

            return createTypeDescriptor(namespace, name, fArray, genericArgs);
            }

        /**
         * Parse the generic arguments represented in a format used in XML
         * schema definition.
         *
         * @param sArgs  the generic arguments in a format used in XML schema
         *               definition
         *
         * @return a list of type descriptors for the generic arguments
         */
        protected List<T> parseGenericArgs(String sArgs)
            {
            List<T> args = new ArrayList<>();

            int i = 0;
            while (i < sArgs.length())
                {
                int start = i;
                int c = 0;
                while (i < sArgs.length() && (c > 0 || sArgs.charAt(i) != ','))
                    {
                    if (sArgs.charAt(i) == '(')
                        {
                        c++;
                        }
                    else if (sArgs.charAt(i) == ')')
                        {
                        c--;
                        }
                    i++;
                    }
                T ref = parse(sArgs.substring(start, i));
                args.add(ref);
                }

            return args;
            }

        /**
         * Parse the generic arguments represented in a Java internal format.
         *
         * @param sArgs  the generic arguments in a Java internal format
         *
         * @return a list of type descriptors for the generic arguments
         */
        protected List<T> parseGenericArgsInternal(String sArgs)
            {
            List<T> args = new ArrayList<>();

            int i = 0;
            while (i < sArgs.length())
                {
                int start = i;
                int c = 0;
                while (c > 0 || sArgs.charAt(i) != ';')
                    {
                    if (sArgs.charAt(i) == '<')
                        {
                        c++;
                        }
                    else if (sArgs.charAt(i) == '>')
                        {
                        c--;
                        }
                    i++;
                    }
                i++;
                T ref = parseInternal(sArgs.substring(start, i));
                args.add(ref);
                }

            return args;
            }

            protected final String m_separator;
        }

    // ---- data members ----------------------------------------------------

    private final boolean m_fArray;

    private final List<T> m_genericArgs;

    private String[] m_namespace;

    private String m_name;

    private T m_arrayType;
    }
