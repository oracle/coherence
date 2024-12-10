/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package data;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.run.xml.XmlBean;

import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ListMap;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;


/**
* @author cp Feb 2, 2006
*/
public class Person
        extends XmlBean
        implements Comparable, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor for Externalizable.
    */
    public Person() {}


    public Person(String sSSN)
        {
        setId(sSSN);
        }

    public Person(String sSSN, String sFirst, String sLast, int nYear,
                  String sMotherId, String[] asChildrenId)
        {
        setId(sSSN);
        setFirstName(sFirst);
        setLastName(sLast);
        setBirthYear(nYear);
        setMotherId(sMotherId);
        setChildrenIds(new ImmutableArrayList(asChildrenId));
        }


    // ----- accessors ------------------------------------------------------

    public String getId()
        {
        return m_sSSN;
        }

    public void setId(String sSSN)
        {
        azzert(m_sSSN == null, "SSN is not resettable");
        m_sSSN = sSSN;
        }

    public String getFirstName()
        {
        return m_sFirstName;
        }
    public void setFirstName(String sFirstName)
        {
        m_sFirstName = sFirstName;
        }

    public String getLastName()
        {
        return m_sLastName;
        }
    public void setLastName(String sLastName)
        {
        m_sLastName = sLastName;
        }

    public int getBirthYear()
        {
        return m_nYear;
        }
    public void setBirthYear(int nYear)
        {
        m_nYear = nYear;
        }

    public String getMotherId()
        {
        return m_sMotherSSN;
        }
    public void setMotherId(String sSSN)
        {
        m_sMotherSSN = sSSN == null ? "" : sSSN;
        }

    public List getChildrenIds()
        {
        return new ArrayList(m_listChildren);
        }
    public void setChildrenIds(List listChildrenIds)
        {
        m_listChildren = new ArrayList(listChildrenIds);
        }
    public void addChildId(String sChildSSN)
        {
        m_listChildren.add(sChildSSN);
        }


    // ----- calculated -----------------------------------------------------

    public int getAge()
        {
        return getAge(Calendar.getInstance().get(Calendar.YEAR));
        }


    public int getAge(int nYear)
        {
        return nYear - getBirthYear();
        }

    public Key key()
        {
        return new Key(m_sSSN, m_sFirstName, m_sLastName);
        }

    // ----- Comparable interface -------------------------------------------

    public int compareTo(Object o)
        {
        Person that = (Person) o;
        return this.getId().compareTo(that.getId());
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof Person))
            {
            return false;
            }
        Person person = (Person) o;
        return m_nYear == person.m_nYear &&
               Objects.equals(m_sSSN, person.m_sSSN) &&
               Objects.equals(m_sFirstName, person.m_sFirstName) &&
               Objects.equals(m_sLastName, person.m_sLastName) &&
               (m_sMotherSSN.equals(person.m_sMotherSSN)) &&
               m_listChildren.equals(person.m_listChildren);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(super.hashCode(), m_sSSN, m_sFirstName, m_sLastName,
                            m_nYear, m_sMotherSSN, m_listChildren);
        }


    // ----- PortableObject interface ---------------------------------------

    public void readExternal(PofReader in)
            throws IOException
        {
        m_sSSN         = in.readString(SSN);
        m_sFirstName   = in.readString(FIRST_NAME);
        m_sLastName    = in.readString(LAST_NAME);
        m_nYear        = in.readInt(BIRTH_YEAR);
        m_sMotherSSN   = in.readString(MOTHER_SSN);
        m_listChildren = in.readCollection(CHILDREN, new ArrayList<>());
        }

    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(SSN, m_sSSN);
        out.writeString(FIRST_NAME, m_sFirstName);
        out.writeString(LAST_NAME, m_sLastName);
        out.writeInt(BIRTH_YEAR, m_nYear);
        out.writeString(MOTHER_SSN, m_sMotherSSN);
        out.writeCollection(CHILDREN, m_listChildren);
        }


    // ----- unit test ------------------------------------------------------

    public static void main(String[] asArg)
        {
        Map map = new ListMap();
        fillRandom(map, asArg.length == 0 ? 1 : Integer.parseInt(asArg[0]));
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();
            out(entry.getKey());
            out(entry.getValue());
            out();
            }
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Composite key.
    */
    public static class Key
            extends XmlBean
            implements PortableObject
        {
        /**
        * Default constructor for Externalizable.
        */
        public Key() {}


        public Key(String sSSN, String sFirstName, String sLastName)
            {
            m_sSSN = sSSN;
            m_sFirstName = sFirstName;
            m_sLastName = sLastName;
            }

        public String getId()
            {
            return m_sSSN;
            }

        public void setId(String sSSN)
            {
            azzert(m_sSSN == null, "Key is not resettable");
            m_sSSN = sSSN;
            }

        public String getFirstName()
            {
            return m_sFirstName;
            }
        public void setFirstName(String sFirstName)
            {
            azzert(m_sFirstName == null, "Key is not resettable");
            m_sFirstName = sFirstName;
            }

        public String getLastName()
            {
            return m_sLastName;
            }
        public void setLastName(String sLastName)
            {
            azzert(m_sLastName == null, "Key is not resettable");
            m_sLastName = sLastName;
            }

        // ----- PortableObject interface ---------------------------------

        public void readExternal(PofReader in)
                throws IOException
            {
            m_sSSN       = in.readString(0);
            m_sFirstName = in.readString(1);
            m_sLastName  = in.readString(2);
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sSSN);
            out.writeString(1, m_sFirstName);
            out.writeString(2, m_sLastName);
            }


        // ----- data members ---------------------------------------------

        private String m_sSSN;
        private String m_sFirstName;
        private String m_sLastName;
        }


    // ----- data members ---------------------------------------------------

    private String m_sSSN;
    private String m_sFirstName = "";
    private String m_sLastName  = "";
    private int    m_nYear;
    private String m_sMotherSSN = "";
    private List   m_listChildren = new ArrayList();


    // ----- testing helpers ------------------------------------------------

    /**
    * Fill the specified Map with random data.
    */
    public static void fillRandom(Map map, int cnt)
        {
        final Random RND = new Random();

        // generate test data;
        // for every 16 entries there will be one parent with one child
        // and one parent with two children
        for (int i = 0; i < cnt; ++i)
            {
            String sSSN   = String.valueOf(i);
            String sFirst = FIRST_NAMES[RND.nextInt(FIRST_NAMES.length)];
            String sLast  = LAST_NAMES[RND.nextInt(LAST_NAMES.length)];
            Person person = new Person(sSSN);
            person.setFirstName(sFirst);
            person.setLastName(sLast);

            if ((i % 8) == 0)
                {
                person.addChildId(String.valueOf(i + 1));
                }
            if ((i % 8) == 1)
                {
                person.setMotherId(String.valueOf(i - 1));
                }
            if ((i % 16) == 0)
                {
                person.addChildId(String.valueOf(i + 2));
                }
            if ((i % 16) == 2)
                {
                person.setMotherId(String.valueOf(i - 2));
                }

            person.setBirthYear(1900 + i % 100 % 10 * 10 + i % 100 / 10);

            addPerson(map, person);
            }
        }

    private static void addPerson(Map map, Person person)
        {
        map.put(person.key(), person);
        }


    // ----- POF index constants --------------------------------------------

    public static final int SSN        = 0;
    public static final int FIRST_NAME = 1;
    public static final int LAST_NAME  = 2;
    public static final int BIRTH_YEAR = 3;
    public static final int MOTHER_SSN = 4;
    public static final int CHILDREN   = 5;


    // ----- Name generation constants --------------------------------------

    public static final String[] FIRST_NAMES = parseDelimitedString("Bob,Dave,Sam,Sue"
        + ",Carla,Sally,Biff,Ted,Mary,Bill,Fred,Fanny,Lilly,Arjun,Betty,Jon"
        + ",Yakim,Valerie,Tina,Steve,Pam,Octavius,Underscore_Test", ',');
    public static final String[] LAST_NAMES = parseDelimitedString("Adams,Anderson"
        + ",Bates,Calverson,Davies,Everton,Fitts,Geiger,Hingham,Ingerson"
        + ",Jones,Kelly,Lindhurst,Madison,Neward,Oppenheimer,Pearl,Ringwald"
        + ",Smith,Tyson,Unger,Valentine,Wyles,Zetikov,Wildcard%Test", ',');
    }
