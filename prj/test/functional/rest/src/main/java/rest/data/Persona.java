/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest.data;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import data.pof.Address;

import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Persona")
public class Persona
        implements PortableObject, Serializable
    {
    public Persona()
        {
        }

    public Persona(String name, int age)
        {
        m_name = name;
        m_age = age;
        }

    public String getName()
        {
        return m_name;
        }

    public void setName(String name)
        {
        m_name = name;
        }

    public int getAge()
        {
        return m_age;
        }

    public void setAge(int age)
        {
        m_age = age;
        }

    public List<Persona> getChildren()
        {
        return m_children;
        }

    public void setChildren(List<Persona> children)
        {
        m_children = children;
        }

    public Map<String, Address> getAddresses()
        {
        return m_addresses;
        }

    public void setAddresses(Map<String, Address> addresses)
        {
        m_addresses = addresses;
        }

    // ----- PortableObject interface ---------------------------------------

    public void readExternal(PofReader in)
            throws IOException
        {
        m_name      = in.readString(0);
        m_age       = in.readInt(1);
        m_children  = (List<Persona>) in.readCollection(2, new ArrayList<Persona>());
        m_addresses = (Map<String, Address>) in.readMap(3, new HashMap<String, Address>());
        }

    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_name);
        out.writeInt(1, m_age);
        out.writeCollection(2, m_children);
        out.writeMap(3, m_addresses);
        }

    private String m_name;
    private int m_age;
    private List<Persona> m_children;
    private Map<String, Address> m_addresses = new HashMap<String, Address>();
    }
