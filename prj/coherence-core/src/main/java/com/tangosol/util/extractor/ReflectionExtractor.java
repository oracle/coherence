/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.oracle.coherence.common.internal.util.CanonicalNames;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotActiveException;

import java.lang.reflect.Method;

import javax.json.bind.annotation.JsonbProperty;


/**
* Reflection-based ValueExtractor implementation.
*
* @author cp/gg 2002.11.01, ew 2007.02.01, jf 2016.08.11
*
* @see ChainedExtractor
*/
public class ReflectionExtractor<T, E>
        extends    AbstractExtractor<T, E>
        implements ValueExtractor<T, E>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ReflectionExtractor()
        {
        }

    /**
    * Construct a ReflectionExtractor based on a method name.
    *
    * @param sMethod  the name of the method to invoke via reflection
    */
    public ReflectionExtractor(String sMethod)
        {
        this(sMethod, null, VALUE);
        }

    /**
    * Construct a ReflectionExtractor based on a method name and optional
    * parameters.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param aoParam  the array of arguments to be used in the method
    *                 invocation; may be {@code null}
    *
    * @since Coherence 3.3
    */
    public ReflectionExtractor(String sMethod, Object[] aoParam)
        {
        this(sMethod, aoParam, VALUE);
        }

    /**
    * Construct a ReflectionExtractor based on a method name, optional
    * parameters and the entry extraction target.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param aoParam  the array of arguments to be used in the method
    *                 invocation; may be {@code null}
    * @param nTarget  one of the {@link #VALUE} or {@link #KEY} values
    *
    * @since Coherence 3.5
    */
    public ReflectionExtractor(String sMethod, Object[] aoParam, int nTarget)
        {
        azzert(sMethod != null);
        m_sMethod = sMethod;
        m_aoParam = aoParam;
        m_nTarget = nTarget;
        }

    // ----- ValueExtractor interface ---------------------------------------

    /**
    * Extract from target using reflection.
    * <p>
    * @param oTarget  the target
    *
    * @return value extracted from target
    */
    @SuppressWarnings("unchecked")
    @Override
    public E extract(T oTarget)
        {
        if (oTarget == null)
            {
            return null;
            }

        Class clz = oTarget.getClass();

        try
            {
            Method method = m_methodPrev;

            if (method == null || method.getDeclaringClass() != clz)
                {
                if (!ClassHelper.isReflectionAllowed(oTarget))
                    {
                    throw new IllegalArgumentException(suggestExtractFailureCause(clz, true));
                    }

                m_methodPrev = method = ClassHelper.findMethod(
                    clz, getMethodName(), ClassHelper.getClassArray(m_aoParam), false);
                }

            //noinspection ConstantConditions
            return (E) method.invoke(oTarget, m_aoParam);
            }
        catch (NullPointerException e)
            {
            throw new RuntimeException(suggestExtractFailureCause(clz, false));
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e,
                clz.getName() + this + '(' + oTarget +')');
            }
        }

    @Override
    public String getCanonicalName()
        {
        String sCName = super.getCanonicalName();
        if (sCName == null)
            {
            sCName = m_sNameCanon = computeCanonicalName(m_sMethod, m_aoParam);
            }
        return sCName;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ReflectionExtractor with another object to determine equality.
    * <p>
    * {@link AbstractExtractor#equals(Object)} contract takes precedence when applicable,
    * falling back to implementation specific equals.
    * <p>
    * Two ReflectionExtractor objects, <i>re1</i> and <i>re2</i> are considered
    * equal iff <tt>re1.extract(o)</tt> equals <tt>re2.extract(o)</tt> for
    * all values of <tt>o</tt>.
    *
    * @return true iff this ReflectionExtractor and the passed object are
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

        if (o instanceof ReflectionExtractor)
            {
            ReflectionExtractor that = (ReflectionExtractor) o;
            return this.m_nTarget == that.m_nTarget &&
                   Base.equals(m_sMethod, that.m_sMethod) &&
                   equalsDeep(this.m_aoParam, that.m_aoParam);
            }

        return false;
        }

    /**
    * HashCode value is hashCode of non-null {@link ValueExtractor#getCanonicalName() canonical name};
    * otherwise, it is the hashCode of <code>sName</code> passed to {#link ReflectionExtractor(String)}.
    *
    * @return an integer hash value for this ReflectionExtractor object
    */
    @Override
    public int hashCode()
        {
        String sCName = getCanonicalName();
        return sCName == null ? m_sMethod.hashCode(): sCName.hashCode();
        }

    /**
    * Provide a human-readable description of this ReflectionExtractor object.
    *
    * @return a human-readable description of this ReflectionExtractor object
    */
    @Override
    public String toString()
        {
        Object[]                    aoParam    = m_aoParam;
        int                         cParams    = aoParam == null ? 0 : aoParam.length;
        Method                      methodPrev = m_methodPrev;
        String                      sName      = methodPrev == null ? m_sMethod : methodPrev.getName();
        StringBuilder               sb         = new StringBuilder();

        if (m_nTarget == KEY)
            {
            sb.append(".getKey()");
            }

        sb.append('.').append(sName).append('(');
        for (int i = 0; i < cParams; i++)
            {
            if (i != 0)
                {
                sb.append(", ");
                }
            sb.append(aoParam[i]);
            }
        sb.append(')');

        return sb.toString();
        }

    // ----- accessors and helpers ------------------------------------------

    /**
    * Determine the name of the method that this extractor is configured to
    * invoke.
    *
    * @return method name, property name or key
    */
    public String getMethodName()
        {
        return m_sMethod;
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
        String sMsg = "Missing or inaccessible method: " +
            clzTarget.getName() + this;

        if (com.tangosol.util.MapEvent.class.isAssignableFrom(clzTarget))
            {
            sMsg += " (the object is a com.tangosol.util.MapEvent, which may "
                 + "suggest that a raw com.tangosol.util.Filter is "
                 + "being used to filter map events rather than a "
                 + "com.tangosol.util.filter.MapEventFilter)";
            }

        if (fFiltered)
            {
            sMsg += " (The type, " + clzTarget.getName() + ", is disallowed as a reflection target by the current "
                    + "reflection filter configuration)";
            }


        return sMsg;
        }

    /**
    * Compute the canonical name for this extractor.
    * <p>
    * Steps to compute canonical name from a provided name:
    * <ol>
    *   <li>If parameter aoParam has one or more parameters, the canonical name is null.</li>
    *   <li>if name begins with a JavaBean accessor prefixes {@link CanonicalNames#VALUE_EXTRACTOR_BEAN_ACCESSOR_PREFIXES}
    *   "get" or "is, it is a property. The canonical name is formed by removing that prefix
    *   and converting the first letter to lowercase.</li>
    *   <li>Otherwise, sName is consider a no-arg method and its canonical form
    *   is the sName with a suffix of "()" appended.</li>
    * </ol>
    *
    * The following examples are properties that resolve to the canonical name {@code foo}:
    * <ul>
    *   <li>getFoo</li>
    *   <li>getfoo</li>
    *   <li>isFoo</li>
    *   <li>isfoo</li>
    * </ul>
    *
    * A No-arg method name "aMethod" has a canonical name of "aMethod()".
    *
    * @param sName    a method name unless it starts with "get" or "is", then treated as a property
    * @param aoParam  optional parameters
    *
    * @return canonical name of sName if it exist; otherwise, {@code null}
    *
    * @since 12.2.1.4
    */
    protected String computeCanonicalName(String sName, Object[] aoParam)
        {
        String      sNameCanonical = null;
        int            nNameLength = sName == null ? 0 : sName.length();
        String [] accessorPrefixes = CanonicalNames.VALUE_EXTRACTOR_BEAN_ACCESSOR_PREFIXES;

        if (aoParam == null || aoParam.length == 0)
            {
            for (int cchPrefix = 0, len = accessorPrefixes.length; cchPrefix < len && sNameCanonical == null; cchPrefix++)
                {
                int nPrefixLength = accessorPrefixes[cchPrefix].length();
                if (nNameLength > nPrefixLength && nNameLength > 0 && sName.startsWith(accessorPrefixes[cchPrefix]))
                    {
                    sNameCanonical = Character.toLowerCase(sName.charAt(nPrefixLength)) +
                                     sName.substring(nPrefixLength + 1);
                    }
                }

            if (sNameCanonical == null)
                {
                // method name with no parameters
                sNameCanonical = sName + "()";
                }
            }

        return sNameCanonical;
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sMethod = readUTF(in);
        int      cParams = readInt(in);
        Object[] aoParam = cParams == 0 ? null : new Object[cParams];

        for (int i = 0; i < cParams; i++)
            {
            aoParam[i] = readObject(in);
            }
        m_aoParam = aoParam;
        m_nTarget = readInt(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        String sMethod = m_sMethod;
        if (sMethod == null)
            {
            throw new NotActiveException(
                "ReflectionExtractor was constructed without a method name");
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
        m_sMethod = in.readString(0);

        // slot #1 is taken by pre-Coherence 3.5 versions for the number of
        // arguments in the parameter array
        int cParams = in.readInt(1);
        if (cParams > 0)
            {
            // fully backwards compatible implementation
            Object[] aoParam = new Object[cParams];
            for (int i = 0; i < cParams; i++)
                {
                aoParam[i] = in.readObject(i + 2);
                }
            m_aoParam = aoParam;
            }
        else
            {
            // slot #2 is used @since Coherence 3.5 to store the entirety
            // of the arguments (as opposed to the first of the arguments)
            m_aoParam = in.readArray(2, Object[]::new);

            m_nTarget = in.readInt(3);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        String sMethod = m_sMethod;
        if (sMethod == null)
            {
            throw new NotActiveException(
                    "ReflectionExtractor was constructed without a method name");
            }
        out.writeString(0, sMethod);
        // slot #1 is not used @since Coherence 3.5
        out.writeObjectArray(2, m_aoParam);
        out.writeInt(3, m_nTarget);
        }

    // ----- data members ---------------------------------------------------

    /**
    * The name of the method to invoke.
    */
    @JsonbProperty("method")
    protected String m_sMethod;

    /**
    * The parameter array.
    */
    @JsonbProperty("args")
    protected Object[] m_aoParam;

    /**
    * A cached reflection method (to avoid repetitive look-ups).
    */
    private transient Method m_methodPrev;
    }