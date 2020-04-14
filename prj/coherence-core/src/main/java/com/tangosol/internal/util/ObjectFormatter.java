/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.coherence.config.scheme.Scheme;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.security.PrivilegedAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;

/**
 * The {@link ObjectFormatter} will format the contents of an object into a human
 * readable string.  All of the fields of the object, including inherited fields, are
 * examined regardless of the visibility.  Fields that are null, zero, etc are filtered
 * out so that are not included in the output String.  If a field's value implements
 * toString, then toString is called to format the value, otherwise the code will
 * recurse to format the individual fields nested object.
 *
 * Below is a sample of formatted output for a DistributedScheme that uses a RWBM.
 *
 * Cache Configuration: rwbm-bin-entry-expiry
 *   SchemeName: distributed-rwbm-bin-entry-expiry
 *   AutoStart: true
 *   ServiceName: DistributedCache
 *   BackingMapScheme
 *     InnerScheme (ReadWriteBackingMapScheme)
 *       CacheStoreScheme
 *         CacheStoreBuilder (ClassScheme)
 *           CustomBuilder
 *             ClassName: common.TestBinaryCacheStore
 *             ConstructorParametersList (ResolvableParameterList of Value)
 *               [0] : Value{2000}
 *       InternalMapBuilder (LocalScheme)
 *         HighUnits: 2147483647
 *         UnitFactor: 1
 *       WriteDelaySeconds: 6
 *       WriteMaxBatchSize: 128
 *   BackupConfig
 *     InitialSize: 1MB
 *     MaximumSize: 1GB
 *     Type: on-heap
 *   DistributedService
 *     ThreadPriority: 10
 *     EventDispatcherThreadPriority: 10
 *     WorkerPriority: 5
 *     ActionPolicy: {NullActionPolicy allowed-actions=*}
 *     GuardTimeoutMillis: 60000
 *     BackupsPreferred: 1
 *     DistributionAggressiveness: 20
 *     DistributionSynchronized: true
 *     OwnershipCapable: true
 *     PartitionsPreferred: 257
 *     TransferThreshold: 524288
 *     BackupAfterWriteBehind: 1
 *     StrictPartitioning: true
 *
 * @author pfm  2012.03.30
 * @since Coherence 12.1.2
 */
public class ObjectFormatter
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Construct an {@link ObjectFormatter}.
     */
    public ObjectFormatter()
        {
        // add the classes whose fields will be ignored
        m_setClassesToIgnore.add("com.tangosol.util.Base");
        m_setClassesToIgnore.add("java.lang.Object");
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Set the number of spaces to indent each field.
     *
     * @param cIndent  the number of spaces to indent
     *
     * @return this object
     */
    public ObjectFormatter setIndent(int cIndent)
        {
        m_cIndent = cIndent;
        return this;
        }

    /**
     * Return the number of spaces to indent each field.
     *
     * @return the number of spaces to indent
     */
    public int getIndent()
        {
        return m_cIndent;
        }

    /**
     * Set the flag specifying if CacheMaps are included in the output.
     *
     * @param flag  the rendered flag
     *
     * @return this object
     */
    public ObjectFormatter setCacheMapRendered(boolean flag)
        {
        m_fCacheMapRendered = flag;
        return this;
        }

    /**
     * Return the flag specifying if CacheMaps are included in the output.
     *
     * @return the rendered flag
     */
    public boolean isCacheMapRendered()
        {
        return m_fCacheMapRendered;
        }

    /**
     * Set the flag specifying if null objects and zero numbers are included in the output.
     *
     * @param flag  the rendered flag
     *
     * @return this object
     */
    public ObjectFormatter setNullRendered(boolean flag)
        {
        m_fNullRendered = flag;
        return this;
        }

    /**
     * Return the flag specifying if null objects and zero numbers are included in the output.
     *
     * @return the rendered flag
     */
    public boolean isNullRendered()
        {
        return m_fNullRendered;
        }

    /**
     * Set the flag specifying that XML elements are included in the output.
     *
     * @param flag  the rendered flag
     *
     * @return this object
     */
    public ObjectFormatter setXmlRendered(boolean flag)
        {
        m_fXmlRendered = flag;
        return this;
        }

    /**
     * Return the flag specifying that XML elements are included in the output.
     *
     * @return the rendered flag
     */
    public boolean isXmlRendered()
        {
        return m_fXmlRendered;
        }

    // ----- ObjectFormatter methods ----------------------------------------

    /**
     * Format the object into a human readable string.
     *
     * @param sTargetName  the display name for the object
     * @param oTarget      the object to format
     *
     * @return the String containing the formatted string.
     */
    public String format(String sTargetName, Object oTarget)
        {
        return format(sTargetName, oTarget, null);
        }

    /**
     * Format the object into a human readable string.
     *
     * @param sTargetName  the display name for the object
     * @param oTarget      the object to format
     * @param resolver     the {@link ParameterResolver} to resolve {@link Expression}s
     *
     * @return the String containing the formatted string
     */
    public String format(String sTargetName, Object oTarget, ParameterResolver resolver)
        {
        StringBuilder buf = m_buf = new StringBuilder();

        try
            {
            m_resolver = resolver == null ? new NullParameterResolver() : resolver;

            buf.append("\n").append(sTargetName);

            formatAllFields(oTarget, getIndent());

            return buf.toString();
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Exception formatting output for " + oTarget);
            }
        }

    /**
     * Return a PrivilegedAction that will perform the
     * {@link #format(String, Object, ParameterResolver) formatting}.
     *
     * @param sTargetName  the display name for the object
     * @param oTarget      the object to format
     * @param resolver     the {@link ParameterResolver} to resolve {@link Expression}s
     *
     * @return the PrivilegedAction that will perform the formatting
     */
    public PrivilegedAction<String> asPrivilegedAction(
            final String sTargetName, final Object oTarget, final ParameterResolver resolver)
        {
        return new PrivilegedAction()
            {
            public String run()
                {
                return format(sTargetName, oTarget, resolver);
                }
            };
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Format all of the fields of an object into a human readable string.
     * This method is called recursively to format nested objects.
     *
     * @param oTarget  the object to format
     * @param cIndent  the number of spaces to indent all of the fields
     */
    protected void formatAllFields(Object oTarget, int cIndent)
        {
        StringBuilder buf = m_buf;

        if (!m_setRecursingObjects.add(oTarget))
            {
            return;
            }

        // get all of the fields including super class fields
        ArrayList<Field> fields = new ArrayList<>();
        getFields(oTarget.getClass(), fields);

        // get the value of each field and format it, skipping over values that
        // can be ignored (null, etc)
        for (Field field : fields)
            {
            Object oValue = getFieldValue(oTarget, field);
            if (oValue != null)
                {
                // save the index in case we need to rollback to this position in the buffer
                int nCheckpoint = buf.length();

                // display the field name and the type of Scheme if applicable
                newline(cIndent);
                String sFieldName = cleanFieldName(field.getName());
                buf.append(sFieldName);

                Class<?> clzValue = oValue.getClass();
                String sValueClassName = clzValue.getSimpleName();

                if ((oValue instanceof Scheme) && (!sFieldName.equals(sValueClassName)))
                    {
                    buf.append(" (").append(clzValue.getSimpleName()).append(") ");
                    }

                // convert an array to iterable list
                oValue = tryConvertToList(oValue);

                if (oValue instanceof Iterable)
                    {
                    int cIterIndent = cIndent + getIndent();
                    int i           = 0;

                    for (Object oEntry : ((Iterable<?>) oValue))
                        {
                        oEntry = translateValue(oTarget, oEntry);

                        if (oEntry != null)
                            {
                            // output type of collection like (ArrayList of String)
                            if (i == 0)
                                {
                                if (clzValue.isArray())
                                    {
                                    buf.append(" (").append(sValueClassName).append(")");
                                    }
                                else
                                    {
                                    buf.append(" (").append(sValueClassName).append(" of ")
                                        .append(oEntry.getClass().getSimpleName()).append(")");
                                    }
                                }
                            formatArrayDimensions(i++, oEntry, cIterIndent, nCheckpoint);
                            }
                        }
                    }
                else
                    {
                    formatOneField(oValue, cIndent, nCheckpoint);
                    }
                }
            }

        m_setRecursingObjects.remove(oTarget);
        }

    /**
     * Format all of the dimensions or an array.  This method is called recursively
     * to format multi-dimensional arrays.
     *
     * @param nIndex       the array index
     * @param oValue       the object to format
     * @param cIndent      the number of spaces to indent
     * @param nCheckpoint  the checkpoint index to rollback for an empty object
     */
    protected void formatArrayDimensions(int nIndex, Object oValue, int cIndent, int nCheckpoint)
        {
        StringBuilder buf = m_buf;

        // output the next entry in the collection
        newline(cIndent);
        buf.append("[").append(nIndex).append("] ");

        oValue = tryConvertToList(oValue);
        if (oValue instanceof Iterable)
            {
            // this is a multi-dimensional array
            int cInnerIndent = cIndent + getIndent();
            int i            = 0;

            for (Object oEntry : (Iterable<?>) oValue)
                {
                formatArrayDimensions(i++, oEntry, cInnerIndent, nCheckpoint);
                }
            }
        else
            {
            // only rollback a single entry - leave collection field name
            nCheckpoint = buf.length();
            formatOneField(oValue, cIndent, nCheckpoint);
            }
        }

    /**
     * Format a single field, recursing if the object doesn't declare toString() in
     * the object itself (i.e. toString in all super objects is not checked).
     *
     * @param oValue       the object to format
     * @param cIndent      the number of spaces to indent
     * @param nCheckpoint  the checkpoint index to rollback for an empty object
     */
    protected void formatOneField(Object oValue, int cIndent, int nCheckpoint)
        {
        StringBuilder buf = m_buf;

        // Determine if toString should be used or the object should be formatted recursively.
        boolean fNested;
        try
            {
            fNested = oValue.getClass().getDeclaredMethod("toString") == null;
            }
        catch (NoSuchMethodException e)
            {
            fNested = true;
            }

        if (fNested)
            {
            int nBufLen = buf.length();

            if (!oValue.getClass().getName().startsWith("com.tangosol.coherence.component"))
                {
                formatAllFields(oValue, cIndent + getIndent());
                }
            if (nBufLen == buf.length())
                {
                // nothing was added by the nested object.  Roll-back to the checkpoint
                if (nCheckpoint > 0)
                    {
                    m_buf.delete(nCheckpoint, nBufLen);
                    }
                }
            }
        else
            {
            buf.append(": ").append(oValue.toString().trim());
            }
        }

    /**
     * Clean up the field name to make it more readable.
     *
     * @param sFieldName  the field name
     *
     * @return the cleaned up field name
     */
    protected String cleanFieldName(String sFieldName)
        {
        StringBuilder buf = null;
        if (sFieldName.startsWith("m_") || sFieldName.startsWith("s_"))
            {
            buf = new StringBuilder();
            buf.append(sFieldName.substring(2));

            // see if beginning of the input string matches any of of the prefixes
            for (int i = 0; i < m_aPrefixesToModify.length; i++)
                {
                String sPrefix    = m_aPrefixesToModify[i][0];
                int    nPrefixLen = sPrefix.length();

                if ((buf.length() > nPrefixLen) &&
                    (buf.substring(0, nPrefixLen).equals(sPrefix)))
                    {
                    // prefix matches, make sure next char is capital letter
                    if (Character.isUpperCase(buf.charAt(nPrefixLen)))
                        {
                        // remove the prefix and append the new suffix
                        return buf.substring(nPrefixLen) + m_aPrefixesToModify[i][1];
                        }
                    }
                }

            // no prefix match other than m_ or s_, so capitalize the first letter
            buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));
            }

        return buf == null ? sFieldName : buf.toString().trim();
        }

    /**
     * Get the field value, filtering out the fields that should be hidden.
     *
     * @param oTarget  the target object
     * @param field    the field whose value should be returned
     *
     * @return the value of the field, else NULL if the field is ignored
     */
    protected Object getFieldValue(Object oTarget, Field field)
        {
        if (!field.isAccessible())
            {
            try
                {
                field.setAccessible(true);
                }
            catch (RuntimeException e)
                {
                return null;
                }
            }

        if (Modifier.isFinal(field.getModifiers()) ||
            Modifier.isStatic(field.getModifiers()))
            {
            return null;
            }

        try
            {
            return translateValue(oTarget, field.get(oTarget));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Translate the field value, filtering out the fields that should be hidden.
     *
     * @param oTarget  the target object
     * @param oValue   the value to translate
     *
     * @return the value of the field, or null if the field should be ignored
     */
    protected Object translateValue(Object oTarget, Object oValue)
        {
        // ignore the member if it is the same object as target (like LocalScheme m_mapBuilder)
        if (oValue == oTarget)
            {
            oValue = null;
            }
        else if (oValue instanceof CacheMap && !isCacheMapRendered())
            {
            oValue = null;
            }
        else if (oValue instanceof XmlElement)
            {
            if (isXmlRendered())
                {
                // convert XML to String and remove empty elements
                XmlElement xml = (XmlElement) ((XmlElement) oValue).clone();
                xml            = XmlHelper.removeEmptyElements(xml);
                oValue = XmlHelper.isEmpty(xml) ? null : xml;
                }
            else
                {
                oValue = null;
                }
            }
        else if (oValue instanceof Expression)
            {
            // evaluate the expression
            try
                {
                oValue = ((Expression<?>) oValue).evaluate(m_resolver);
                }
            catch (Exception e)
                {
                // Ignore the exception - the expression string will be output
                // An example of this happening is when ObjectFormatter is
                // called to format the CacheConfig and there are {cache-name}
                // expressions, but no way to resolve them.
                }
            }

        // check if field should be hidden because it is null, zero value, etc
        if (oValue instanceof Float)
            {
            oValue = ((Float) oValue).floatValue() == 0.0F ? null : oValue;
            }
        else if (oValue instanceof Double)
            {
            oValue = ((Double) oValue).doubleValue() == 0.0D ? null : oValue;
            }
        else if (oValue instanceof Number)
            {
            oValue = oValue.toString().equals("0")     ? null
                   : oValue.toString().startsWith("-") ? null
                   : oValue;
            }
        else if (oValue instanceof Boolean)
            {
            oValue = ((Boolean) oValue).booleanValue() ? oValue : null;
            }
        else if (oValue instanceof String)
            {
            oValue = ((String) oValue).length() == 0 ? null : oValue;
            }
        else if (oValue instanceof Duration)
            {
            long cNanos = ((Duration) oValue).as(Duration.Magnitude.NANO);
            oValue = cNanos == 0 ? null : oValue.toString();
            }
        else if (oValue instanceof MemorySize)
            {
            long cb = (long) ((MemorySize) oValue).as(MemorySize.Magnitude.BYTES);
            oValue = cb == 0 ? null : oValue.toString();
            }

        return oValue;
        }

    /**
     * Get the fields for the class and all super classes.
     *
     * @param clz     the class that contains the fields
     * @param fields  the ArrayList that will be filled with the fields
     */
    protected void getFields(Class<?> clz, ArrayList<Field> fields)
        {
        Class<?> clzSuper = clz.getSuperclass();
        if (clzSuper != null)
            {
            getFields(clzSuper, fields);
            }

        if (!m_setClassesToIgnore.contains(clz.getName()))
            {
            fields.addAll(Arrays.asList(clz.getDeclaredFields()));
            }
        }

    /**
     * Attempt to convert the object into a list.
     *
     * @param oValue  the input object
     *
     * @return a list if the Object is an array, otherwise return the input object
     */
    protected Object tryConvertToList(Object oValue)
        {
        try
            {
            oValue = oValue.getClass().isArray()
                ? Arrays.asList((Object[]) oValue) : oValue;
            }
        catch (ClassCastException e)
            {
            oValue = convertPrimitiveArrayToList(oValue);
            }

        return oValue;
        }

    /**
     * Convert an array of primitives to a list of Objects.  This method is
     * called recursively to handle multi-dimensional arrays.
     *
     * @param oValue  the primitive array
     *
     * @return the list of Objects
     */
    protected List<Object> convertPrimitiveArrayToList(Object oValue)
        {
        ArrayList<Object> list = new ArrayList<>();

        for (int i = 0, c = Array.getLength(oValue); i < c; i++)
            {
            Object oEntry = Array.get(oValue, i);
            if (oEntry.getClass().isArray())
                {
                // handle multi-dimensional arrays
                list.add(convertPrimitiveArrayToList(oValue));
                }

            list.add(oEntry);
            }
        return list;
        }

    /**
     * Add the indent spaces to the buffer.
     *
     * @param cIndent  the number of spaces to indent
     */
    protected void indent(int cIndent)
        {
        StringBuilder sb = m_buf;
        for (int i = 0; i < cIndent; i++)
            {
            sb.append(' ');
            }
        }

    /**
     * Add a new line, then indent.
     *
     * @param cIndent  the number of spaces to indent
     */
    protected void newline(int cIndent)
        {
        m_buf.append('\n');
        indent(cIndent);
        }

    // ----- data fields ----------------------------------------------------

    /**
     * The number of spaces to indent fields.
     */
    private int m_cIndent = 2;

    /**
     * The output buffer.
     */
    private StringBuilder m_buf;

    /**
     * A flag indicating if CacheMaps are rendered (included in the output).
     */
    private boolean m_fCacheMapRendered;

    /**
     * A flag indicating that null and zero values are rendered (included in the output).
     */
    private boolean m_fNullRendered;

    /**
     * A flag indicating that XML elements are rendered (included in the output).
     */
    private boolean m_fXmlRendered;

    /**
     * The {@link ParameterResolver} used to resolve {@link Expression}s.
     */
    private ParameterResolver m_resolver;

    /**
     * The classes to ignore, such as Base.
     */
    private HashSet<String> m_setClassesToIgnore = new HashSet<>();

    /**
     * The prefixes to remove or swap. For example m_nSize becomes Size, and mapFoo becomes FooMap.
     * The first string is the prefix (not including m_ or s_) that will be removed. The second
     * string will be appended to the field name.
     */
    private String[][] m_aPrefixesToModify =
        {
            {"a",      "Array"},
            {"bldr",   "Builder"},
            {"b",      ""},
            {"c",      ""},
            {"cb",     ""},
            {"clz",    "Class"},
            {"col",    "Collection"},
            {"config", "Config"},
            {"d",      ""},
            {"date",   "Date"},
            {"dfl",    ""},
            {"e",      ""},
            {"expr",   ""},
            {"evt",    "Event"},
            {"f",      ""},
            {"fl",     ""},
            {"l",      ""},
            {"ldt",    "Date"},
            {"list",   "List"},
            {"map",    "Map"},
            {"n",      ""},
            {"o",      ""},
            {"of",     "Offset"},
            {"scheme", "Scheme"},
            {"set",    "Set"},
            {"s",      ""}
        };

    /**
     * The set of objects that are being called recursively.  This is needed to
     * prevent unnecessary recursion.
     */
    private HashSet<Object> m_setRecursingObjects = new HashSet<>();
    }
