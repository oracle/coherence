/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;

import java.io.IOException;


/**
* A custom key class used by the Coherence*Extend tests.
*
* @author phf  2011.08.24
*/
public class CustomKeyClass
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor. Needed for PortableObject.
    */
    public CustomKeyClass()
        {
        }

    /**
    * Construct an ExtendTestsCustomKeyClass object from an Object.
    *
    * @param o  an object
    */
    public CustomKeyClass(Object o)
        {
        m_o = o;
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in) throws IOException
        {
        m_o = in.readObject(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_o);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public String toString()
        {
        return new String("CustomKeyClass(" + m_o + ')');
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (o instanceof CustomKeyClass)
            {
            CustomKeyClass that = (CustomKeyClass) o;
            return Base.equals(m_o, that.m_o);
            }
        return false;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public int hashCode()
        {
        Object o = m_o;
        return o == null ? 0: o.hashCode();
        }


    // ----- data members ---------------------------------------------------

    /**
    * An object.
    */
    protected Object m_o;
    }
