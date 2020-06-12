/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;

import com.oracle.coherence.common.internal.util.CanonicalNames;

import com.tangosol.internal.util.extractor.TargetReflectionDescriptor;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotActiveException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Map;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

/**
 * Universal ValueExtractor implementation.
 * <p>
 * Either a property or method based extractor based on parameters passed to
 * constructor {@link #UniversalExtractor(String, Object[], int)}.
 * Generally, the name value passed to the {@link UniversalExtractor} constructor
 * represents a property unless <code>sName</code> value ends in {@link #METHOD_SUFFIX},
 * <code>"()"</code>,
 * then this instance is a reflection based method extractor.
 * Special cases are described in the constructor documentation.
 * <p>
 * {@link AbstractExtractor#equals(Object)} and {@link AbstractExtractor#hashCode()}
 * describe how this Extractor can be equivalent to other {@link ValueExtractor}
 * implementations.
 *
 * @param <T>  the type of the value to extract from
 * @param <E>  the type of value that will be extracted
 *
 * @author cp/gg 2002.11.01, ew 2007.02.01, jf 2017.11.20
 *
 * @since 12.2.1.4
 *
 * @see ChainedExtractor
 */
public class UniversalExtractor<T, E>
        extends    AbstractExtractor<T, E>
        implements ValueExtractor<T, E>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public UniversalExtractor()
        {
        }

    /**
     * Construct a UniversalExtractor based on syntax of <code>sName</code>.
     * <p>
     * If <code>sName</code> does not end in {@link #METHOD_SUFFIX},
     * <code>"()"</code>, this extractor is a property extractor. If <code>sName</code> is prefixed with
     * one of the {@link #BEAN_ACCESSOR_PREFIXES} and ends in the {@link #METHOD_SUFFIX},
     * this extractor is a property extractor. Otherwise,
     * if the <code>sName</code> just ends in {#link #METHOD_SUFFIX},
     * this extractor is considered a method extractor.
     *
     * @param sName  a method or property name
     */
    public UniversalExtractor(String sName)
        {
        this(sName, null, VALUE);
        }

    /**
     * Construct a UniversalExtractor based on a name and optional
     * parameters.
     * <p>
     * If <code>sName</code> does not end in {@link #METHOD_SUFFIX}, <code>"()"</code>,
     * and has no <code>aoParams</code>,this extractor is a property extractor.
     * If <code>sName</code> is prefixed with
     * one of the {@link #BEAN_ACCESSOR_PREFIXES}, ends in {@link #METHOD_SUFFIX}
     * and has no <code>aoParams</code>,this extractor is a property extractor.
     * Otherwise, if the <code>sName</code>just ends in {#link #METHOD_SUFFIX},
     * this extractor is considered a method extractor.
     *
     * @param sName    a method or property name
     * @param aoParam  the array of arguments to be used in the method
     *                 invocation; may be null
     *
     * @throws IllegalArgumentException when <code>sName</code> does not end in
     *                                  {@link #METHOD_SUFFIX} and aoParam array length is one or more.
     */
    public UniversalExtractor(String sName, Object[] aoParam)
        {
        this(sName, aoParam, VALUE);
        }

    /**
     * Construct a UniversalExtractor based on a name, optional
     * parameters and the entry extraction target.
     * <p>
     * If <code>sName</code> does not end in {@link #METHOD_SUFFIX}, <code>"()"</code>,
     * this extractor is a property extractor. If <code>sName</code> is prefixed with
     * one of the {@link #BEAN_ACCESSOR_PREFIXES} and ends in {@link #METHOD_SUFFIX},
     * this extractor is a property extractor. If the <code>sName</code>
     * just ends in {@link #METHOD_SUFFIX}, this extractor is considered a method
     * extractor.
     *
     * @param sName    a method or property name
     * @param aoParam  the array of arguments to be used in the method
     *                 invocation; may be null
     * @param nTarget  one of the {@link #VALUE} or {@link #KEY} values
     *
     * @throws IllegalArgumentException when <code>sName</code> does not end in
     *                                  {@link #METHOD_SUFFIX} and aoParam array length is one or more.
     */
    @JsonbCreator
    public UniversalExtractor(@JsonbProperty("name")
                                      String sName,
                              @JsonbProperty("params")
                                      Object[] aoParam,
                              @JsonbProperty("target")
                                      int nTarget)
        {
        azzert(sName != null);
        if (aoParam != null && aoParam.length > 0 && !sName.endsWith(METHOD_SUFFIX))
            {
            throw new IllegalArgumentException("UniversalExtractor constructor: parameter sName[value:" + sName + "] must end with method suffix \"" + METHOD_SUFFIX + "\" when optional parameters provided");
            }

        m_sName       = sName;
        m_aoParam     = aoParam;
        m_nTarget     = nTarget;

        init();
        }

    // ----- ValueExtractor interface ---------------------------------------

    /**
     * Extract from target using reflection or map access.
     * <p>
     * If name is a property, reflection accessor method lookup on {@code T} fails and
     * {@code oTarget} is an instance {@link Map}, use canonical name to get value from
     * target.
     *
     * @param oTarget  the target
     *
     * @return value extracted from target
     */
    public E extract(T oTarget)
        {
        if (oTarget == null)
            {
            return null;
            }

        TargetReflectionDescriptor targetPrev = m_cacheTarget;
        try
            {
            if (targetPrev != null && oTarget.getClass() == targetPrev.getTargetClass())
                {
                return (E) (targetPrev.isMap()
                        ? (((Map) oTarget).get(getCanonicalName()))
                        : targetPrev.getMethod().invoke(oTarget, m_aoParam));
                }

            return extractComplex(oTarget);
            }
        catch (NullPointerException e)
            {
            throw new RuntimeException(suggestExtractFailureCause(oTarget.getClass(), false));
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e,
                oTarget.getClass().getName() + this + '(' + oTarget +')');
            }
        }

    @Override
    public String getCanonicalName()
        {
        String sCName = Lambdas.getValueExtractorCanonicalName(this);
        if (sCName == null)
            {
            sCName = m_sNameCanon = CanonicalNames.computeValueExtractorCanonicalName(m_sName, m_aoParam);
            }
        return sCName;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Compare the {@code UniversalExtractor} with another object to determine equality.
     * <p>
     * {@link AbstractExtractor#equals(Object)} contract takes precedence when applicable,
     * falling back to implementation specific equals.
     * <p>
     * Two UniversalExtractor objects, <i>re1</i> and <i>re2</i> are considered
     * equal if <tt>re1.extract(o)</tt> equals <tt>re2.extract(o)</tt> for
     * all values of <tt>o</tt>.
     *
     * @return {@code true} iff this {@code UniversalExtractor} and the passed object are
     *         equivalent
     */
    @Override
    public boolean equals(Object o)
        {
        // the super.equals() uses the canonical name comparison (if applies);
        // if that succeeds, no other checks are to be made.
        if (super.equals(o))
            {
            return true;
            }
        else if (isCanonicallyEquatable(o))
            {
            return false;
            }

        if (o instanceof UniversalExtractor)
            {
            UniversalExtractor that = (UniversalExtractor) o;
            return this.m_nTarget == that.m_nTarget &&
                   equals(this.m_sName, that.m_sName) &&
                   equalsDeep(this.m_aoParam, that.m_aoParam);
            }

        return false;
        }

    /**
     * HashCode value is hashCode of non-null {@link ValueExtractor#getCanonicalName() canonical name};
     * otherwise, it is the hashCode of {@code sName} passed to {#link UniversalExtractor(String)}.
     *
     * @return an integer hash value for this UniversalExtractor object
     */
    @Override
    public int hashCode()
        {
        String sCName = getCanonicalName();
        return sCName == null ? m_sName.hashCode(): super.hashCode();
        }

    /**
     * Provide a human-readable description of this {@code UniversalExtractor} object.
     *
     * @return a human-readable description of this {@code UniversalExtractor} object
     */
    @Override
    public String toString()
        {
        Object[]                    aoParam    = m_aoParam;
        int                         cParams    = aoParam == null ? 0 : aoParam.length;
        StringBuilder               sb         = new StringBuilder();


        if (m_nTarget == KEY)
            {
            sb.append(".getKey()");
            }

        if (isPropertyExtractor())
            {
            String sCName = getCanonicalName();
            if (sCName != null && sCName.length() > 0)
                {
                sb.append("." + sCName);
                return sb.toString();
                }
            else
                {
                return "";
                }
            }
        else if (isMethodExtractor())
            {
            sb.append('.').append(getMethodName()).append('(');
            for (int i = 0; i < cParams; i++)
                {
                if (i != 0)
                    {
                    sb.append(", ");
                    }
                sb.append(aoParam[i]);
                }
            sb.append(')');
            }

        return sb.toString();
        }

    // ----- accessors and helpers ------------------------------------------

    /**
     * Called in constructor and deserializers.
     */
    protected void init()
        {
        String sCName = getCanonicalName();
        m_fMethod = sCName == null || sCName.endsWith(METHOD_SUFFIX);
        }

    /**
     * Return the method name that this extractor is configured to invoke.
     * If a reflection-based method extractor, {@link #isMethodExtractor()}, return
     * the method name to be invoked. If a property extractor, return the likely JavaBean accessor
     * method name.
     *
     * @return method name
     */
    public String getMethodName()
        {
        final int METHOD_SUFFIX_LENGTH = METHOD_SUFFIX.length();

        String sCName = getCanonicalName();
        String sName  = sCName == null ? m_sName : sCName;
        return isMethodExtractor() ?
                sName.substring(0, sName.length() - METHOD_SUFFIX_LENGTH) :
                "get" + Character.toUpperCase(sCName.charAt(0)) + sCName.substring(1);
        }

    /**
     * Return the name passed into {@link #UniversalExtractor(String)}.
     *
     * @return the name of extraction attribute.
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Return the property name of this extractor.
     *
     * @return property name of this extractor; otherwise, return null.
     */
    public String getPropertyName()
        {
        return isPropertyExtractor() ? getCanonicalName() : null;
        }

    /**
     * Return true if this is a Property extractor.
     *
     * @return true if this a property extractor.
     */
    public boolean isPropertyExtractor()
        {
        return !m_fMethod;
        }

    /**
     * Return true if this a method extractor.
     *
     * @return true if this is a method extractor.
     */
    public boolean isMethodExtractor()
        {
        return m_fMethod;
        }

    /**
     * Return the array of arguments used to invoke the method.
     *
     * @return the array of arguments used to invoke the method
     */
    public Object[] getParameters()
        {
        return m_aoParam;
        }

    /**
     * Extract from target using reflection or map access.
     * If name is a property, reflection accessor method lookup on {@code T} fails and
     * {@code T} is an instance {@link Map}, use canonical name to get value from
     * target. If successful, cache the reflection computation.
     *
     * @param oTarget  the target
     *
     * @return value extracted from target
     *
     * @throws InvocationTargetException if reflection method lookup fails
     * @throws IllegalAccessException    if reflection method lookup fails
     */
    protected E extractComplex(T oTarget)
            throws InvocationTargetException, IllegalAccessException
        {
        Class    clzTarget = oTarget.getClass();
        Object[] aoParam   = m_aoParam;
        Class[]  clzParam  = ClassHelper.getClassArray(aoParam);
        String   sCName    = getCanonicalName();
        boolean  fProperty = isPropertyExtractor();
        Method   method    = null;

        // check for javabean accessors
        if (fProperty)
            {
            String sBeanAttribute = Character.toUpperCase(sCName.charAt(0)) + sCName.substring(1);

            for (int cchPrefix = 0; cchPrefix < BEAN_ACCESSOR_PREFIXES.length && method == null; cchPrefix++)
                {
                method = ClassHelper.findMethod(clzTarget,
                        BEAN_ACCESSOR_PREFIXES[cchPrefix] + sBeanAttribute, clzParam, false);
                }
            }
        else
            {
            // lookup method via reflection
            method = ClassHelper.findMethod(clzTarget, getMethodName(), clzParam, false);
            }

        if (method == null)
            {
            if (fProperty && oTarget instanceof Map)
                {
                m_cacheTarget = new TargetReflectionDescriptor(clzTarget);
                return (E) ((Map) oTarget).get(sCName);
                }
            }
        else
            {
            // only check if reflection is allowed when method is non null and not cached from previous extract
            if (!ClassHelper.isReflectionAllowed(oTarget))
                {
                m_cacheTarget = null;
                throw new IllegalArgumentException(suggestExtractFailureCause(clzTarget, true));
                }
            m_cacheTarget = new TargetReflectionDescriptor(clzTarget, method);
            }
        return (E) method.invoke(oTarget, aoParam);
        }

    /**
     * Return a message suggesting a possible cause for a failure to extract a
     * value.
     *
     * @param clzTarget  the target object's class
     * @param fFiltered  pass {@code true} if the type was an invalid reflection target type
     *
     * @return the suggested reason for the failure of the extraction
     */
    private String suggestExtractFailureCause(Class clzTarget, boolean fFiltered)
        {
        TargetReflectionDescriptor targetPrev = m_cacheTarget;
        if (targetPrev != null && targetPrev.isMap())
            {
            return "Failed accessing target of class " + clzTarget.getCanonicalName() + " using property " + getCanonicalName();
            }

        String sMsg = "Missing or inaccessible method: " +
            clzTarget.getName() + this;

        if (com.tangosol.util.MapEvent.class.isAssignableFrom(clzTarget))
            {
            sMsg += " (the object is a com.oracle.coherence.util.MapEvent, which may "
                 + "suggest that a raw com.oracle.coherence.util.Filter is "
                 + "being used to filter map events rather than a "
                 + "com.oracle.coherence.util.filter.MapEventFilter)";
            }

        if (fFiltered)
            {
            sMsg += " (The type, " + clzTarget.getName() + ", is disallowed as a reflection target by the current "
                    + "reflection filter configuration)";
            }

        return sMsg;
        }

    /**
     * Return a ValueExtractor representing dot separated list of property
     * and/or method names.
     *
     * @param sNames  dot-delimited property and/or methods name(s)
     *
     * @param <T>  the type of the value to extract from
     * @param <E>  the type of value that will be extracted
     *
     * @return {@link UniversalExtractor} if only one name in parameter; otherwise, return
     * a {@link ChainedExtractor} with a UniversalExtractor for each name.
     */
    static public <T, E> ValueExtractor<T, E> createExtractor(String sNames)
        {
        if (sNames == null || sNames.length() == 0)
            {
            return IdentityExtractor.INSTANCE;
            }
        else
            {
            return sNames.indexOf('.') < 0 ?
                    new UniversalExtractor<>(sNames) :
                    new ChainedExtractor<>(ChainedExtractor.createExtractors(sNames));
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sName = readUTF(in);
        int      cParams = readInt(in);
        Object[] aoParam = cParams == 0 ? null : new Object[cParams];

        for (int i = 0; i < cParams; i++)
            {
            aoParam[i] = readObject(in);
            }
        m_aoParam = aoParam;
        m_nTarget = readInt(in);
        init();
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        String sMethod = m_sName;
        if (sMethod == null)
            {
            throw new NotActiveException(
                "UniversalExtractor was constructed without a method name");
            }
        Object[] aoParam = m_aoParam;
        int      cParams = aoParam == null ? 0 : aoParam.length;

        writeUTF(out, sMethod);
        writeInt(out, cParams);
        for (int i = 0; i < cParams; i++)
            {
            writeObject(out, aoParam[i]);
            }
        writeInt(out, m_nTarget);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sName   = in.readString(0);
        m_aoParam = in.readArray(1, Object[]::new);
        m_nTarget = in.readInt(2);
        init();
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        String sMethod = m_sName;
        if (sMethod == null)
            {
            throw new NotActiveException(
                "UniversalExtractor was constructed without a method name");
            }
        out.writeString(0, sMethod);
        out.writeObjectArray(1, m_aoParam);
        out.writeInt(2, m_nTarget);
        }

    // ----- constants ------------------------------------------------------

    /**
     * JavaBean accessor prefixes.
     */
    public final static String[] BEAN_ACCESSOR_PREFIXES = CanonicalNames.VALUE_EXTRACTOR_BEAN_ACCESSOR_PREFIXES;

    /**
     * If {@link #m_sName} ends with this suffix, it represents a method name.
     */
    public final static String METHOD_SUFFIX = CanonicalNames.VALUE_EXTRACTOR_METHOD_SUFFIX;

    // ----- data members --------------------------------------------------

    /**
     * A method or property name.
     */
    @JsonbProperty("name")
    protected String m_sName;

    /**
    * The parameter array. Must be null or zero length for a property based extractor.
    */
    @JsonbProperty("params")
    protected Object[] m_aoParam;

    /**
     * Canonical name for this extractor.
     * <p>
     * <b>Note:</b> subclasses are responsible for initialization and POF and/or
     * Lite serialization of this field.
     */
    protected transient String m_sNameCanon = null;

    /**
     * Cached reflection computations for previous target parameter.
     * This cache enables very fast reference based matching in a homogeneous cache.
     *
     * @see #extractComplex(T)
     */
    private transient TargetReflectionDescriptor m_cacheTarget;

    /**
     * True if a method extractor; false if a property extractor.
     */
    private transient boolean m_fMethod;
    }
