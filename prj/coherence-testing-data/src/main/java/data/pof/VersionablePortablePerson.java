/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.pof;

import com.tangosol.util.Versionable;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * @author jh  2012.01.18
 */
@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class VersionablePortablePerson
        extends PortablePerson
        implements Versionable
    {

    // ---- constructors ----------------------------------------------------

    public VersionablePortablePerson()
        {
        }

    public VersionablePortablePerson(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
        }

    public VersionablePortablePerson(String sName, Date dtDOB, int nAge)
        {
        super(sName, dtDOB, nAge);
        }

    // ---- Versionable interface -------------------------------------------

    public Comparable getVersionIndicator()
        {
        return m_nVersion;
        }

    public void setVersionIndicator(int nVersion)
        {
        m_nVersion = nVersion;
        }

    public void incrementVersion()
        {
        m_nVersion++;
        }

    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        super.readExternal(reader);
        m_nVersion = reader.readInt(VERSION);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        super.writeExternal(writer);
        writer.writeInt(VERSION, m_nVersion);
        }


    // ----- factory methods ------------------------------------------------

    /**
     * Creates a populated instance of a VersionablePortablePerson class to
     * be used in tests.
     *
     * @return a populated instance of a VersionablePortablePerson class to
     *         be used in tests
     */
    public static VersionablePortablePerson create()
        {
        VersionablePortablePerson p =
                new VersionablePortablePerson("Aleksandar Seovic", new Date(74, 7, 24), 36);
        initPerson(p);
        return p;
        }

    // ----- constants ------------------------------------------------------

    public static final int VERSION = Person.PHONE + 1;

    // ----- data members ---------------------------------------------------

    protected volatile int m_nVersion = 1;
    }
