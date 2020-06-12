/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ClassHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.lang.reflect.Method;

import javax.json.bind.annotation.JsonbProperty;


/**
* Reflection-based ValueUpdater implementation.
*
* @author gg 2005.10.27
*
* @see CompositeUpdater
*/
public class ReflectionUpdater
        extends    AbstractUpdater
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ReflectionUpdater()
        {
        }

    /**
    * Construct a ReflectionUpdater for a given method name.
    * This implementation assumes that the corresponding classes will have
    * one and only one method with a specified name and this method will have
    * exactly one parameter.
    *
    * @param sMethod  the name of the method to invoke via reflection
    */
    public ReflectionUpdater(String sMethod)
        {
        azzert(sMethod != null);
        m_sMethod = sMethod;
        }


    // ----- ValueUpdater interface -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void update(Object oTarget, Object oValue)
        {
        if (oTarget == null)
            {
            throw new IllegalArgumentException(
                "Target object is missing for the Updater: " + this);
            }

        Class  clzTarget = oTarget.getClass();
        String sMethod   = getMethodName();

        try
            {
            Method method = m_methodPrev;

            if (method == null || method.getDeclaringClass() != clzTarget)
                {
                Class[] aclzParam = new Class[1];
                if (oValue != null)
                    {
                    aclzParam[0] = oValue.getClass();
                    }

                if (!ClassHelper.isReflectionAllowed(oTarget))
                    {
                    throw new IllegalArgumentException("The type, " + clzTarget.getName() +
                        ", is disallowed as a reflection target by the current " +
                        "reflection filter configuration");
                    }
                 m_methodPrev = method =
                     ClassHelper.findMethod(clzTarget, sMethod, aclzParam, false);
                }

            method.invoke(oTarget, oValue);
            }
        catch (NullPointerException e)
            {
            throw new RuntimeException("Missing or inaccessible method: " +
                clzTarget.getName() + '#' + sMethod);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e,
                clzTarget.getName() + '#' + sMethod + '(' + oTarget +')');
            }
        }


    // ----- accessors and helpers ------------------------------------------

    /**
    * Determine the name of the method that this extractor is configured to
    * invoke.
    *
    * @return the name of the method to invoke using reflection
    */
    public String getMethodName()
        {
        return m_sMethod;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ReflectionUpdater with another object to determine
    * equality.
    *
    * @return true iff this ReflectionUpdater and the passed object are
    *         equivalent ReflectionUpdaters
    */
    public boolean equals(Object o)
        {
        if (o instanceof ReflectionUpdater)
            {
            ReflectionUpdater that = (ReflectionUpdater) o;
            return this.m_sMethod.equals(that.m_sMethod);
            }

        return false;
        }

    /**
    * Determine a hash value for the ReflectionUpdater object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ReflectionUpdater object
    */
    public int hashCode()
        {
        return m_sMethod.hashCode();
        }

    /**
    * Provide a human-readable description of this ValueUpdater object.
    *
    * @return a human-readable description of this ValueUpdater object
    */
    public String toString()
        {
        return '&' + m_sMethod;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sMethod = in.readUTF();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeUTF(m_sMethod);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sMethod = in.readString(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_sMethod);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of the method to invoke.
    */
    @JsonbProperty("method")
    protected String m_sMethod;

    /**
    * A cached reflection method (to avoid repeated look-ups).
    */
    @JsonbProperty("methodPrev")
    private transient Method m_methodPrev;
    }