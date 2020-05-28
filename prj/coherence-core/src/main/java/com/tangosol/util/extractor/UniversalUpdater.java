/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;

import com.tangosol.internal.util.extractor.TargetReflectionDescriptor;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ValueUpdater;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Universal ValueUpdater implementation.
 * <p>
 * Either a property-based and method-based {@link com.tangosol.util.ValueUpdater}
 * based on whether constructor parameter <code>sName</code> is evaluated to be a property or method.
 * Depending on the <code>target</code> parameter of {@link #update(Object, Object)} <code>target</code>,
 * the property can reference a JavaBean property or {@link Map} key.
 *
 * @author gg 2005.10.27, jf 2017.11.28
 * @see CompositeUpdater
 * @since Coherence 19.1.0.0
 */
public class UniversalUpdater
        extends    AbstractUpdater
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public UniversalUpdater()
        {
        }

    /**
     * Construct a UniversalUpdater for the provided name.
     * If <code>sName</code> ends in a {@link #METHOD_SUFFIX},
     * then the name is a method name. This implementation assumes that a
     * target's class will have one and only one method with the
     * specified name and this method will have exactly one parameter;
     * if the name is a property name, there should be a corresponding
     * JavaBean property modifier method or it will be used as a key in a {@link Map}.
     *
     * @param sName  a method or property name
     *
     * @see #computeCanonicalName(String)
     */
    public UniversalUpdater(String sName)
        {
        azzert(sName != null);
        m_sName = sName;
        }

    // ----- ValueUpdater interface -----------------------------------------

    /**
     * {@inheritDoc}
     *
     * @see #updateComplex(Object, Object)
     */
    public void update(Object oTarget, Object oValue)
        {
        if (oTarget == null)
            {
            throw new IllegalArgumentException(
                "Target object is missing for the Updater: " + this);
            }

        Class<?> clzTarget = oTarget.getClass();
        TargetReflectionDescriptor targetPrev = m_targetPrev;
        try
            {
            if (targetPrev != null && clzTarget == targetPrev.getTargetClass())
                {
                if (targetPrev.isMap())
                    {
                    ((Map) oTarget).put(getCanonicalName(), oValue);
                    }
                else
                    {
                    targetPrev.getMethod().invoke(oTarget, oValue);
                    }
                }
            else
                {
                updateComplex(oTarget, oValue);
                }
            }
        catch (NullPointerException e)
            {
            throw new RuntimeException("Missing or inaccessible method: " + clzTarget.getName() + '#' + m_sName);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, clzTarget.getName() + '#' + m_sName + '(' + oTarget + ')');
            }
        }


    // ----- accessors and helpers ------------------------------------------

    /**
     * Return true iff this updater references a setter method.
     *
     * @return true iff this is setter method updater.
     */
    public boolean isMethodUpdater()
        {
        return m_sName.endsWith(METHOD_SUFFIX);
        }

    /**
     * Determine the name of the method that this extractor is configured to
     * invoke.
     *
     * @return method name without the {@link #METHOD_SUFFIX}
     */
    public String getMethodName()
        {
        final int METHOD_SUFFIX_LENGTH = METHOD_SUFFIX.length();

        String sName = m_sName;

        return isMethodUpdater() ?
                sName.substring(0, sName.length() - METHOD_SUFFIX_LENGTH) :
                "set" + Character.toUpperCase(sName.charAt(0)) +
                        (sName.length() > 1 ? sName.substring(1) : "");
        }

    /**
     * Return the canonical name for this updater.
     *
     * A canonical name uniquely identifies an updater, but not how it
     * is to be updated.  Thus, two different updater implementations with the same
     * non-null canonical name are considered to be equal, and should reflect this in
     * their implementations of hashCode and equals.
     *
     * Canonical names for properties are designated by their property name in camel case,
     * for instance a Java Bean with method setFooBar would have a property named fooBar,
     * and fooBar would also be its canonical name.
     *
     * @return the updater's canonical name
     *
     * @since Coherence 12.3.1
     */
    public String getCanonicalName()
        {
        String sCName = m_sNameCanon;
        if (sCName == null)
            {
            sCName = m_sNameCanon = computeCanonicalName(m_sName);
            }
        return sCName;
        }

    /**
     * Implement update of target using reflection or property setter.
     * Cache the reflection computation to enable avoiding
     * reflection lookup if next target has same class type.
     * <p>
     * If unable to find method name via reflection and {@code oTarget}
     * is a {@link Map}, use canonical name as a key
     * to update target.
     *
     * @param oTarget  the Object to update the state of
     * @param oValue   the new value to update the state with
     *
     * @throws InvocationTargetException if reflection method lookup fails
     * @throws IllegalAccessException    if reflection method lookup fails
     *
     * @since Coherence 12.3.1
     */
    protected void updateComplex(Object oTarget, Object oValue)
            throws InvocationTargetException, IllegalAccessException
        {
        String  sCName    = getCanonicalName();
        Class   clzTarget = oTarget.getClass();
        Class[] aclzParam = new Class[]{oValue == null ? Object.class : oValue.getClass()};
        Method  method    = isMethodUpdater()
                ? ClassHelper.findMethod(clzTarget, getMethodName(), aclzParam, false)
                : null;

        // check for case that name is a property modifier.
        if (method == null)
            {
            String sBeanAttribute = sCName.length() == 0
                    ? sCName
                    : Character.toUpperCase(sCName.charAt(0)) + sCName.substring(1);
            String sBeanSetter = "set" + sBeanAttribute;

            method = ClassHelper.findMethod(
                    clzTarget, sBeanSetter, aclzParam, false);
            }

        if (method == null)
            {
            if (oTarget instanceof Map)
                {
                m_targetPrev = new TargetReflectionDescriptor(clzTarget);
                ((Map) oTarget).put(sCName, oValue);
                return;
                }
            }
        else
            {
            // only check if reflection is allowed when method is non null and not cached from previous update
            if (!ClassHelper.isReflectionAllowed(oTarget))
                {
                throw new IllegalArgumentException("The type, " + clzTarget.getName() +
                    ", is disallowed as a reflection target by the current " +
                    "reflection filter configuration");
                }

            m_targetPrev = new TargetReflectionDescriptor(clzTarget, method);
            }
        method.invoke(oTarget, oValue);
        }

    /**
     * Compute the canonical name for this updater.
     *
     * If <code>sName</code> does not end with a {@link #METHOD_SUFFIX}, it
     * is the canonical name of a property.
     * If <code>sName</code> begins with {@link #BEAN_MODIFIER_PREFIX} and
     * ends with {@link #METHOD_SUFFIX}, the canonical name is <code>sName</code>
     * value with prefix and suffix removed and the canonical name is for a property.
     * Otherwise, the canonical name is the <code>sName</code> and refers to
     * method name.
     *
     * @param sName  a method or property name
     *
     * @return return canonical name of sName
     */
    public static String computeCanonicalName(String sName)
        {
        final int PREFIX_LEN = BEAN_MODIFIER_PREFIX.length();
        final int SUFFIX_LEN = METHOD_SUFFIX.length();

        String sNameCanonical = null;
        int    nNameLength    = sName == null ? 0 : sName.length();

        // map the "setFoo()" case to property "foo"
        if ((nNameLength > PREFIX_LEN + SUFFIX_LEN) && sName.startsWith(BEAN_MODIFIER_PREFIX) && sName.endsWith(METHOD_SUFFIX))
            {
            sNameCanonical = Character.toLowerCase(sName.charAt(PREFIX_LEN)) +
                    sName.substring(PREFIX_LEN + 1, nNameLength - SUFFIX_LEN);
            }

        if (sNameCanonical == null)
            {
            sNameCanonical = sName;
            }

        return sNameCanonical;
        }

    /**
     * Return a {@link ValueUpdater} for <code>sNames</code>
     *
     * @param sNames  property or setter method name;
     *                this parameter can also be a dot-delimited sequence
     *                of property and/or method names which would result in using a
     *                {@link com.tangosol.util.extractor.CompositeUpdater}
     * @return ValueUpdater for <code>sNames</code>
     */
    public static ValueUpdater createUpdater(String sNames)
        {
        return sNames.indexOf('.') < 0
                ? new UniversalUpdater(sNames)
                : new CompositeUpdater(sNames);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Compare the UniversalUpdater with another object to determine
     * equality. Compare by canonical names when both are non-null.
     *
     * @return true iff this UniversalUpdater and the passed object are
     * equivalent ReflectionUpdaters
     */
    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }
        else if (o instanceof UniversalUpdater)
            {
            String sCNameThat = ((UniversalUpdater) o).getCanonicalName();
            String sCNameThis = getCanonicalName();

            // optimization of String.equals() - see the assignment below
            if (sCNameThis == sCNameThat)
                {
                return true;
                }
            else if (Base.equals(sCNameThis, sCNameThat))
                {
                // encourage future comparisons to succeed via simple reference equality checks
                m_sNameCanon = sCNameThat;
                return true;
                }
            }

        return false;
        }

    /**
     * Determine a hash value for the UniversalUpdater object according to
     * the general {@link Object#hashCode()} contract.
     *
     * @return an integer hash value for this UniversalUpdater object
     */
    public int hashCode()
        {
        return getCanonicalName().hashCode();
        }

    /**
     * Provide a human-readable description of this ValueUpdater object.
     *
     * @return a human-readable description of this ValueUpdater object
     */
    public String toString()
        {
        return getCanonicalName();
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sName = in.readUTF();
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeUTF(m_sName);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sName = in.readString(0);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_sName);
        }

    // ----- constants ------------------------------------------------------

    /**
     * JavaBean property modifier prefix.
     */
    static final public String BEAN_MODIFIER_PREFIX = "set";

    /**
     * If {@link #m_sName} ends with this suffix, it represents a method name.
     */
    public final static String METHOD_SUFFIX = "()";

    // ----- data members ---------------------------------------------------

    /**
     * A method name, or a property name.
     */
    @JsonbProperty("name")
    protected String m_sName;

    /**
     * Canonical name for {@link UniversalUpdater}.
     */
    private transient String m_sNameCanon;

    /**
     * Cached reflection computations for previous target parameter.
     * This cache enables very fast reference based matching in a homogeneous cache.
     */
    private transient TargetReflectionDescriptor m_targetPrev;
    }
