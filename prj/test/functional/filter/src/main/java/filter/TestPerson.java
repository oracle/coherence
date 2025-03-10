/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package filter;


import java.io.Serializable;
import java.util.Collection;

/**
 * Test class for filters.
 */
public class TestPerson
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    public TestPerson()
        {
        }

    public TestPerson(int nId, String sName, Collection<String> languages)
        {
        m_nId       = nId;
        m_sName     = sName;
        m_languages = languages;
        }

    // ----- accessors ------------------------------------------------------

    public int getId()
        {
        return m_nId;
        }

    public void setId(int nId)
        {
        m_nId = nId;
        }

    public String getName()
        {
        return m_sName;
       }

    public void setName(String sName)
        {
        m_sName = sName;
        }

    public Collection<String> getLanguages()
        {
        return m_languages;
        }

    public String[] getPets()
        {
        return m_aPets;
        }

    // ----- data members ---------------------------------------------------

    private int m_nId;
    private String m_sName;
    private Collection<String> m_languages;
    private String[] m_aPets;
    }
