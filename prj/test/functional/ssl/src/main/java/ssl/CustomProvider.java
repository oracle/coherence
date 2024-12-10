/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ssl;


import com.tangosol.util.Base;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;


/**
* Custom provider extension that delegates to an instance of Sun's SSL
* provider.
*
* @author jh  2010.04.29
*/
public class CustomProvider
        extends Provider
    {
    public CustomProvider(String sName)
        {
        super(sName, 1.0, "CustomProvider info.");
        try
            {
            SSLContext instance = SSLContext.getInstance("TLS", "SunJSSE");
            m_delegate = instance.getProvider();
            }
        catch (NoSuchAlgorithmException | NoSuchProviderException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    public void clear()
        {
        m_delegate.clear();
        }

    public Enumeration<Object> elements()
        {
        return m_delegate.elements();
        }

    public Set<Map.Entry<Object, Object>> entrySet()
        {
        return m_delegate.entrySet();
        }

    public Object get(Object key)
        {
        return m_delegate.get(key);
        }

    public String getProperty(String key)
        {
        return m_delegate.getProperty(key);
        }

    public Service getService(String type, String algorithm)
        {
        return m_delegate.getService(type, algorithm);
        }

    public Set<Service> getServices()
        {
        return m_delegate.getServices();
        }

    public Enumeration<Object> keys()
        {
        return m_delegate.keys();
        }

    public Set<Object> keySet()
        {
        return m_delegate.keySet();
        }

    public void load(InputStream inStream)
            throws IOException
        {
        m_delegate.load(inStream);
        }

    public Object put(Object key, Object value)
        {
        return m_delegate.put(key, value);
        }

    public void putAll(Map<?, ?> t)
        {
        m_delegate.putAll(t);
        }

    public Object remove(Object key)
        {
        return m_delegate.remove(key);
        }

    public Collection<Object> values()
        {
        return m_delegate.values();
        }

    public String getProperty(String key, String defaultValue)
        {
        return m_delegate.getProperty(key, defaultValue);
        }

    public void list(PrintStream out)
        {
        m_delegate.list(out);
        }

    public void list(PrintWriter out)
        {
        m_delegate.list(out);
        }

// 1.6-only
//    public void load(Reader reader)
//            throws IOException
//        {
//        m_delegate.load(reader);
//        }

    public void loadFromXML(InputStream in)
            throws IOException
        {
        m_delegate.loadFromXML(in);
        }

    public Enumeration<?> propertyNames()
        {
        return m_delegate.propertyNames();
        }

//    public void save(OutputStream out, String comments)
//        {
//        m_delegate.save(out, comments);
//        }

    public Object setProperty(String key, String value)
        {
        return m_delegate.setProperty(key, value);
        }

    public void store(OutputStream out, String comments)
            throws IOException
        {
        m_delegate.store(out, comments);
        }

// 1.6-only
//    public void store(Writer writer, String comments)
//            throws IOException
//        {
//        m_delegate.store(writer, comments);
//        }

    public void storeToXML(OutputStream os, String comment)
            throws IOException
        {
        m_delegate.storeToXML(os, comment);
        }

    public void storeToXML(OutputStream os, String comment,
            String encoding)
            throws IOException
        {
        m_delegate.storeToXML(os, comment, encoding);
        }

// 1.6-only
//    public Set<String> stringPropertyNames()
//        {
//        return m_delegate.stringPropertyNames();
//        }

    public boolean contains(Object value)
        {
        return m_delegate.contains(value);
        }

    public boolean containsKey(Object key)
        {
        return m_delegate.containsKey(key);
        }

    public boolean containsValue(Object value)
        {
        return m_delegate.containsValue(value);
        }

    public boolean equals(Object o)
        {
        return o instanceof CustomProvider &&
               m_delegate.equals(((CustomProvider) o).m_delegate);
        }

    public int hashCode()
        {
        return m_delegate.hashCode();
        }

    public boolean isEmpty()
        {
        return m_delegate.isEmpty();
        }

    public int size()
        {
        return m_delegate.size();
        }

    protected final Provider m_delegate;
    }
