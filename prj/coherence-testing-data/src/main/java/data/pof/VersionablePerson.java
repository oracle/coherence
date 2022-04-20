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

/**
 * @author ic  2011.06.29
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class VersionablePerson
        extends Person
        implements Versionable
    {

    // ---- constructors ----------------------------------------------------

    public VersionablePerson()
        {
        }

    public VersionablePerson(String sName, Date dtDOB)
        {
        super(sName, dtDOB);
        }

    // ---- Versionable interface -------------------------------------------

    public Comparable getVersionIndicator()
        {
        return m_nVersion;
        }

    public void incrementVersion()
        {
        m_nVersion++;
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Creates a populated instance of a VersionablePerson class to be used
     * in tests.
     *
     * @return a populated instance of a VersionablePerson class to be used
     *         in tests
     */
    public static VersionablePerson create()
        {
        VersionablePerson p = new VersionablePerson("Aleksandar Seovic",
                new Date(74, 7, 24));
        initPerson(p);
        return p;
        }

    // ---- data members ----------------------------------------------------

    protected volatile int m_nVersion = 1;
    }
