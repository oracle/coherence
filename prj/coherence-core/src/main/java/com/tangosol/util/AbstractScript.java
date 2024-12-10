/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import javax.json.bind.annotation.JsonbProperty;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * AbstractScriptBase is the base class for script based processing.
 *
 * @author mk 2019.09.25
 * @since 14.1.1.0
 */
public class AbstractScript
        implements ExternalizableLite, PortableObject
    {
    // ------ constructors ---------------------------------------------------

    /**
     * Default constructor for ExternalizableLite.
     */
    public AbstractScript()
        {
        }

    /**
     * Create a AbstractScriptBase.
     *
     * @param language  the language the script is written
     * @param name      the name of the script that needs to be evaluated
     * @param args      the arguments to be passed to the script during evaluation
     */
    public AbstractScript(String language, String name, Object... args)
        {
        m_sLanguage = language;
        m_sName     = name;
        m_aoArgs    = args;
        }

    // ----- accessors -------------------------------------------------------

    /**
     * Return the language the script is written.
     *
     * @return the language the script is written
     */
    public String getLanguage()
        {
        return m_sLanguage;
        }

    /**
     * Return the name of the script that needs to be evaluated.
     *
     * @return the name of the script that needs to be evaluated
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Return the arguments to be passed to the script during evaluation.
     *
     * @return the arguments to be passed to the script during evaluation
     */
    public Object[] getArgs()
        {
        return m_aoArgs;
        }

    // ----- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sLanguage = in.readUTF();
        m_sName     = in.readUTF();
        int numArgs = in.readInt();
        Base.azzert(numArgs < 256, "Unexpected number of arguments");

        m_aoArgs    = new Object[numArgs];
        for (int i = 0; i< numArgs; i++)
            {
            m_aoArgs[i] = ExternalizableHelper.readObject(in);
            }
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeUTF(m_sLanguage);
        out.writeUTF(m_sName);

        int numArgs = m_aoArgs.length;
        out.writeInt(numArgs);
        for (Object arg : m_aoArgs)
            {
            ExternalizableHelper.writeObject(out, arg);
            }
        }

    // ----- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sLanguage = in.readString(0);
        m_sName     = in.readString(1);
        m_aoArgs    = in.readArray(2, Object[]::new);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sLanguage);
        out.writeString(1, m_sName);
        out.writeObjectArray(2, m_aoArgs);
        }

    // ------ data members ---------------------------------------------------

    /**
     * The language the script is written.
     */
    @JsonbProperty("language")
    protected String m_sLanguage;

    /**
     * The name of the script object to execute.
     */
    @JsonbProperty("name")
    protected String m_sName;

    /**
     * The arguments to be passed to the script during evaluation.
     */
    @JsonbProperty("args")
    protected Object[] m_aoArgs;
    }
