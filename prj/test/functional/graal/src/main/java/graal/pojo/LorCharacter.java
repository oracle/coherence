/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package graal.pojo;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;

/**
 * A simple POJO used in functional tests.
 */
public class LorCharacter
        implements PortableObject, ExternalizableLite
    {
    // ---- constructors -----------------------------------------------------

    /**
     * Default constructor.
     */
    public LorCharacter()
        {
        }

    /**
     * Create a Lor character.
     *
     * @param name the name of the Lor character
     * @param age the age of the Lor character
     * @param gender the gender of the Lor character
     * @param hobbies the hobbies of the Lor character
     */
    public LorCharacter(String name, int age, String gender, String[] hobbies)
        {
        this.m_sName = name;
        this.m_age = age;
        this.m_sGender = gender;
        this.m_hobbies = hobbies;
        }

    /**
     * Get the name of the person.
     *
     * @return the name of the person
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Set the name of the person
     *
     * @param name the name of the person
     */
    public void setName(String name)
        {
        this.m_sName = name;
        }

    /**
     *
     * Get the age of the person.
     *
     * @return the age of the person
     */
    public int getAge()
        {
        return m_age;
        }

    /**
     * Set the age of the person.
     *
     * @param age the age of the person
     */
    public void setAge(int age)
        {
        this.m_age = age;
        }

    /**
     * Get the gender of the person.
     *
     * @return the gender of the person
     */
    public String getGender()
        {
        return m_sGender;
        }

    /**
     * Set the gender og the person.
     *
     * @param gender the gender of the pserson
     */
    public void setGender(String gender)
        {
        this.m_sGender = gender;
        }

    /**
     * Get the hobbies of the person.
     *
     * @return the hobbies of the person
     */
    public String[] getHobbies()
        {
        return m_hobbies;
        }

    /**
     * Set the hobbies of the person.
     *
     * @param hobbies the hobbies of the person
     */
    public void setHobbies(String[] hobbies)
        {
        this.m_hobbies = hobbies;
        }

    // ----- PortableObject methods ------------------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        m_sName   = in.readString(0);
        m_sGender = in.readString(1);
        m_age     = in.readInt(2);
        m_hobbies = in.readArray(3, String[]::new);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sName);
        out.writeString(1, m_sGender);
        out.writeInt(2, m_age);
        out.writeObjectArray(3, m_hobbies, String.class);
        }

    // ----- ExternalizableLite methods --------------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        m_sName   = in.readUTF();
        m_sGender = in.readUTF();
        m_age     = in.readInt();
        m_hobbies = ExternalizableHelper.readStringArray(in);
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeUTF(m_sName);
        out.writeUTF(m_sGender);
        out.writeInt(m_age);
        ExternalizableHelper.writeStringArray(out, m_hobbies);
        }

    // ----- Object methods --------------------------------------------------

    @Override
    public String toString()
        {
        return "LorCharacter{" +
               "name='" + m_sName + '\'' +
               ", age=" + m_age +
               ", gender='" + m_sGender + '\'' +
               ", hobbies=" + Arrays.toString(m_hobbies) +
               '}';
        }

    // ----- data members ----------------------------------------------------

    /**
     * Name of the Lor character.
     */
    private String m_sName;

    /**
     * The age of the Lor character.
     */
    private int m_age;

    /**
     * The gender of the Lor character.
     */
    private String m_sGender;

    /**
     * The hobbies of the Lor character.
     */
    private String[] m_hobbies;
    }
