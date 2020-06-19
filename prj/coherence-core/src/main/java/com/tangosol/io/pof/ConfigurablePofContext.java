/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.coherence.config.Config;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.Evolvable;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.annotation.Portable;

import com.tangosol.io.pof.schema.annotation.PortableType;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.CopyOnWriteMap;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.SafeHashMap;

import java.io.IOException;

import java.lang.ref.WeakReference;

import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


/**
* This class implements the {@link PofContext} interface using information
* provided in a configuration file (or in a passed XML configuration).
* <p>
* For each user type supported by this POF context, it must be provided with:
* <ul>
* <li>A valid user type ID that is unique within this POF context;</li>
* <li>A Java class name that identifies a Java class or interface that all
*     values of the user type are type-assignable to (and that no values of
*     other user types are type-assignable to); in other words, all values of
*     the user type (and no values of other user types) are instances of the
*     specified class, instances of a sub-class of the specified class, or
*     (if it is an interface) instances of a class that implements the
*     specified interface;</li>
* <li>A Java class name that identifies a non-abstract implementation of
*     the PofSerializer interface.</li>
* </ul>
* <p>
* The format of the configuration XML is as follows:
* <pre>{@code
* &lt;pof-config&gt;
*   &lt;user-type-list&gt;
*     ..
*     &lt;user-type&gt;
*       &lt;type-id&gt;53&lt;/type-id&gt;
*       &lt;class-name&gt;com.mycompany.data.Trade&lt;/class-name&gt;
*       &lt;serializer&gt;
*         &lt;class-name&gt;com.tangosol.io.pof.PortableObjectSerializer&lt;/class-name&gt;
*         &lt;init-params&gt;
*           &lt;init-param&gt;
*             &lt;param-type&gt;int&lt;/param-type&gt;
*             &lt;param-value&gt;{type-id}&lt;/param-value&gt;
*           &lt;/init-param&gt;
*         &lt;/init-params&gt;
*       &lt;/serializer&gt;
*     &lt;/user-type&gt;

*     &lt;user-type&gt;
*       &lt;type-id&gt;54&lt;/type-id&gt;
*       &lt;class-name&gt;com.mycompany.data.Position&lt;/class-name&gt;
*     &lt;/user-type&gt;
*
*     ..
*     &lt;include&gt;file:/my-pof-config.xml&lt;/include&gt;
*
*     ..
*   &lt;/user-type-list&gt;
*
*   &lt;allow-interfaces&gt;false&lt;/allow-interfaces&gt;
*   &lt;allow-subclasses&gt;false&lt;/allow-subclasses&gt;
* &lt;/pof-config&gt;
* }</pre>
* For each user type, a <tt>user-type</tt> element must exist inside the
* <tt>user-type-list</tt> element. The <tt>user-type-list</tt> element
* contains up to three elements, in the following order:
* <ul>
* <li>The <tt>user-type</tt> element should contain a <tt>type-id</tt>
*     element whose value specifies the unique integer type ID; if none of
*     the <tt>user-type</tt> elements contains a <tt>type-id</tt> element,
*     then the type IDs for the user types will be based on the order in
*     which they appear in the configuration, with the first user type being
*     assigned the type ID 0, the second user type being assigned the type ID
*     1, and so on. (It is strongly recommended that user types IDs always be
*     specified, in order to support schema versioning and evolution.)</li>
* <li>The <tt>class-name</tt> element is required, and specifies the fully
*     qualified name of the Java class or interface that all values of the
*     user type are type-assignable to.</li>
* <li>The <tt>serializer</tt> element is used to specify an implementation of
*     PofSerializer to use to serialize and deserialize user type values to
*     and from a POF stream. Within the <tt>serializer</tt> element, the
*     <tt>class-name</tt> element is required, and zero or more constructor
*     parameters can be defined within an <tt>init-params</tt> block. If no
*     <tt>serializer</tt> is specified, either implement the PortableObject
*     interface or have a {@link Portable} annotation. If the former, a
*     {@link PortableObjectSerializer} will be used. If the later, a
*     {@link PofAnnotationSerializer} will be used.</li>
* </ul>
* <p>
* The optional <tt>include</tt> element allows <tt>user-type</tt> elements
* defined in another configuration XML to be added to the user type list.
* The value of this element is a locator string (either a valid path or URL)
* that specifies the location of the target PofContext configuration file.
* The <tt>user-type</tt> elements of the target file are imported verbatum;
* therefore, if the included elements contain explicit type identifiers, each
* identifier must be unique with respect to the the user type identifiers
* (either explicit or generated) defined within the including file. If the
* included user types do not contain explicit type identifiers, then the type
* identifiers will be based on the order in which the user types appear in
* the composite configuration file. Multiple <tt>include</tt> elements may
* be used within a single <tt>user-type-list</tt> element.
* <p>
* The ConfigurablePofContext is truly ClassLoader-aware. It is conceivable
* that the ConfigurablePofContext is loaded by the system ClassLoader (or
* some other relatively global ClassLoader), while the objects deserialized
* by the PofContext are loaded by an application-specific ClassLoader, such
* as is typical within an application server. The ConfigurablePofContext
* is designed to load the configuration, the POF-able object classes and the
* PofSerializer classes from within a specified ClassLoader context, and to
* pass the ClassLoader information on to the PofSerializer instances, just in
* case they are not loaded from within the application's ClassLoader context.
* In other words, the ConfigurablePofContext, its configuration, the
* PofSerializer classes and the POF-able classes can all be loaded by the
* same ClassLoader, or they can all be loaded by different ClassLoaders, so
* long as the configuration, the POF-able classes and the PofSerializer
* classes can be loaded by either the specified ClassLoader or by the
* ClassLoader that loaded the ConfigurablePofContext itself.
* <p>
* In order to be used by the ConfigurablePofContext, a PofSerializer
* implementation must provide a public constructor that accepts the
* parameters detailed by the <tt>init-params</tt> element. The parameter
* values, as specified by the <tt>param-value</tt> element, can specify one
* of the following substitutable values:
* <ul>
* <li><tt>{type-id}</tt> - replaced with the Type ID of the User Type;</li>
* <li><tt>{class-name}</tt> - replaced with the name of the class for the
*     User Type;</li>
* <li><tt>{class}</tt> - replaced with the Class for the User Type;</li>
* <li><tt>{class-loader}</tt> - replaced with the ConfigurablePofContext's
*     ContextClassLoader.</li>
* </ul>
* If the <tt>init-params</tt> element is not present, then the
* ConfigurablePofContext attempts to construct the PofSerializer by searching
* for one of the following constructors in the same order as they appear
* here:
* <ul>
* <li>(int nTypeId, Class clz, ClassLoader loader)</li>
* <li>(int nTypeId, Class clz)</li>
* <li>(int nTypeId)</li>
* <li>()</li>
* </ul>
* <p>
* Once constructed, if the PofSerializer implements the XmlConfigurable
* interface, the {@link XmlConfigurable#setConfig setConfig} method is
* invoked, and it is passed the parameter XML information, transposed as
* described by {@link XmlHelper#transformInitParams transformInitParams}, and
* as described in the coherence-pof-config.xsd file.
* <p>
* Finally, if the PofSerializer implements the ClassLoaderAware interface and
* a ClassLoader has been specified, then the
* {@link ClassLoaderAware#setContextClassLoader setContextClassLoader} method
* is invoked with the reference to the specified ClassLoader.
* <p>
* Conceptually, the identity of a ConfigurablePofContext is a combination of
* a configuration locator and a ClassLoader. The ClassLoader is used to
* resolve and load the configuration details whose location is specified by
* the configuration locator, and to load all of the classes specified by the
* configuration. To achieve acceptable performance, and to limit the
* redundant use of resources, the ConfigurablePofContext maintains a
* WeakHashMap keyed by ClassLoader, whose corresponding values are each a
* SafeHashMap keyed by configuration locator, whose corresponding values
* contain the data necessary to efficiently perform the operations prescribed
* by the PofContext interface.
* <p>
* <b>Note:</b> The configuration for the default
* {@link #ConfigurablePofContext() constructor} can be specified using the
* {@link #PROPERTY_CONFIG tangosol.pof.config} system property.
*
* @author jh/cp  2006.07.24
*
* @since Coherence 3.2
*/
public class ConfigurablePofContext
        implements PofContext, ClassLoaderAware, XmlConfigurable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    * <p>
    * Create a default ConfigurablePofContext that will load configuration
    * information from the locator specified in {@link #DEFAULT_RESOURCE}.
    */
    public ConfigurablePofContext()
        {
        this((String) null);
        }

    /**
    * Create a ConfigurablePofContext that will load configuration
    * information from the specified locator.
    *
    * @param sLocator  the locator that specifies the location of the
    *                  PofContext configuration file; the locator is either
    *                  a valid path or a URL
    */
    public ConfigurablePofContext(String sLocator)
        {
        m_sUri = sLocator;
        }

    /**
    * Create a ConfigurablePofContext that will use the passed configuration
    * information.
    *
    * @param xml  an XmlElement containing information in the format of a
    *             configuration file used by ConfigurablePofContext
    */
    public ConfigurablePofContext(XmlElement xml)
        {
        setConfig(xml);
        }

    /**
    * Copy constructor for a ConfigurablePofContext.
    *
    * @param that  the ConfigurablePofContext to (shallow) copy from
    */
    public ConfigurablePofContext(ConfigurablePofContext that)
        {
        this.m_cfg                = that.m_cfg;
        this.m_fReferenceEnabled  = that.m_fReferenceEnabled;
        this.m_refLoader          = that.m_refLoader;
        this.m_sUri               = that.m_sUri;
        this.m_xml                = that.m_xml;
        }

    // ----- XmlConfigurable interface --------------------------------------

    /**
    * {@inheritDoc}
    */
    public XmlElement getConfig()
        {
        return m_xml;
        }

    /**
    * {@inheritDoc}
    * <p>
    * Note that the configuration cannot be set after the
    * ConfigurablePofContext is fully initialized.
    *
    * @throws IllegalStateException  if the ConfigurablePofContext is already
    *         fully initialized
    */
    public synchronized void setConfig(XmlElement xml)
        {
        if (xml != null && !XmlHelper.isEmpty(xml))
            {
            checkNotInitialized();

            if (m_sUri == null)
                {
                // generate a fake locator to use as a unique name for the
                // configuration, as if it were a URI
                m_sUri = "xml:" + Base.toDecString(
                        xml.toString().hashCode() & 0x7FFFFFFF, 8);
                }

            m_xml = xml;
            }
        }


    // ----- ClassLoaderAware interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public ClassLoader getContextClassLoader()
        {
        ClassLoader loader = null;

        WeakReference ref = m_refLoader;
        if (ref != null)
            {
            loader = (ClassLoader) ref.get();
            if (loader == null)
                {
                throw new IllegalStateException(
                        "ClassLoader is no longer available");
                }
            }

        return loader;
        }

    /**
    * {@inheritDoc}
    * <p>
    * Note that the ConfigurablePofContext will fully initialize when it is
    * provided a ClassLoader.
    *
    * @throws IllegalStateException  if the ConfigurablePofContext is already
    *         fully initialized
    */
    public synchronized void setContextClassLoader(ClassLoader loader)
        {
        checkNotInitialized();

        m_refLoader = loader == null ? null : new WeakReference<>(loader);

        initialize();
        }


    // ----- Serializer interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void serialize(WriteBuffer.BufferOutput out, Object o)
            throws IOException
        {
        ensureInitialized();

        PofBufferWriter writer = new PofBufferWriter(out, this);

        // COH-5065: due to the complexity of maintaining references
        // in future data, we won't support them for Evolvable objects
        if (isReferenceEnabled() && !(o instanceof Evolvable))
            {
            writer.enableReference();
            }

        try
            {
            writer.writeObject(-1, o);
            }
        catch (RuntimeException e)
            {
            // Guarantee that runtime exceptions from called methods are
            // IOException
            IOException ioex = new IOException(e.getMessage());

            ioex.initCause(e);
            throw ioex;
            }
        }

    /**
    * {@inheritDoc}
    */
    public Object deserialize(ReadBuffer.BufferInput in)
            throws IOException
        {
        ensureInitialized();

        PofBufferReader reader = new PofBufferReader(in, this);
        try
            {
            return reader.readObject(-1);
            }
        catch (RuntimeException e)
            {
            // Guarantee that runtime exceptions from called methods are
            // IOException
            IOException ioex = new IOException(e.getMessage());

            ioex.initCause(e);
            throw ioex;
            }
        }


    // ----- PofContext implementation --------------------------------------

    /**
    * {@inheritDoc}
    */
    public PofSerializer getPofSerializer(int nTypeId)
        {
        ensureInitialized();

        PofSerializer serializer;
        try
            {
            serializer = m_cfg.m_aserByTypeId[nTypeId];
            }
        catch (IndexOutOfBoundsException e)
            {
            serializer = null;
            }

        if (serializer == null)
            {
            throw new IllegalArgumentException("unknown user type: " + nTypeId);
            }

        return serializer;
        }

    /**
    * {@inheritDoc}
    */
    public int getUserTypeIdentifier(Object o)
        {
        if (o == null)
            {
            throw new IllegalArgumentException("Object cannot be null");
            }

        return getUserTypeIdentifier(o.getClass());
        }

    /**
    * {@inheritDoc}
    */
    public int getUserTypeIdentifier(Class clz)
        {
        int nTypeId = getUserTypeIdentifierInternal(clz);
        if (nTypeId < 0)
            {
            throw new IllegalArgumentException("unknown user type: " + clz.getName());
            }

        return nTypeId;
        }

    /**
    * {@inheritDoc}
    */
    public int getUserTypeIdentifier(String sClass)
        {
        int nTypeId = getUserTypeIdentifierInternal(sClass);
        if (nTypeId < 0)
            {
            throw new IllegalArgumentException("unknown user type: " + sClass);
            }

        return nTypeId;
        }

    /**
    * {@inheritDoc}
    */
    public String getClassName(int nTypeId)
        {
        return (String) m_cfg.m_mapClassNameByTypeId.get(Integer.valueOf(nTypeId));
        }

    /**
    * {@inheritDoc}
    */
    public Class getClass(int nTypeId)
        {
        ensureInitialized();

        Class clz;
        try
            {
            clz = (Class) m_cfg.m_aClzByTypeId[nTypeId].get();
            }
        catch (IndexOutOfBoundsException e)
            {
            clz = null;
            }

        if (clz == null)
            {
            String sClass = (String) m_cfg.m_mapClassNameByTypeId.get(Integer.valueOf(nTypeId));
            if (sClass != null && !sClass.isEmpty())
                {
                // since we hold a weak reference, Class may have been GC'd;
                // try loading again
                clz = loadClass(sClass);
                }
            }

        if (clz == null)
            {
            throw new IllegalArgumentException("unknown user type: " + nTypeId);
            }

        return clz;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isUserType(Object o)
        {
        if (o == null)
            {
            throw new IllegalArgumentException("Object cannot be null");
            }
        return isUserType(o.getClass());
        }

    /**
    * {@inheritDoc}
    */
    public boolean isUserType(Class clz)
        {
        return getUserTypeIdentifierInternal(clz) >= 0;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isUserType(String sClass)
        {
        return getUserTypeIdentifierInternal(sClass) >= 0;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isPreferJavaTime()
        {
        return m_fPreferJavaTime;
        }

    // ----- internal helpers -----------------------------------------------

    /**
    * Determine the user type identifier associated with the given class.
    *
    * @param clz  a user type class; must not be null
    *
    * @return the type identifier of the user type associated with the given
    *         class or -1 if the user type is unknown to this PofContext
    */
    protected int getUserTypeIdentifierInternal(Class clz)
        {
        ensureInitialized();

        Integer ITypeId = (Integer) m_cfg.m_mapTypeIdByClass.get(clz);
        return ITypeId == null
                ? getInheritedUserTypeIdentifier(clz)
                : ITypeId.intValue();
        }

    /**
    * Helper method for determining the user type identifier associated with
    * a given class that does not have a direct configured association.
    *
    * @param clz  a user type class; must not be null
    *
    * @return the type identifier of the user type associated with the given
    *         class or -1 if the user type and its superclass(es) and
    *         implemented interface(s) are unknown to this PofContext
    */
    protected int getInheritedUserTypeIdentifier(Class clz)
        {
        Map mapClzToId = m_cfg.m_mapTypeIdByClass;

        if (clz == null)
            {
            throw new IllegalArgumentException("class is required");
            }

        if (isSubclassAllowed())
            {
            Class clzSuper = clz.getSuperclass();
            while (clzSuper != null)
                {
                Integer ITypeId = (Integer) mapClzToId.get(clzSuper);
                if (ITypeId != null)
                    {
                    int nTypeId = ITypeId.intValue();

                    // update the mapping so that we don't have to
                    // brute-force search again
                    mapClzToId.put(clz, ITypeId);
                    return nTypeId;
                    }

                clzSuper = clzSuper.getSuperclass();
                }
            }

        if (isInterfaceAllowed())
            {
            // check each user type interface to see if the passed class
            // implements it
            synchronized (mapClzToId)
                {
                for (Iterator iter = mapClzToId.entrySet().iterator();
                     iter.hasNext(); )
                    {
                    Map.Entry entry  = (Map.Entry) iter.next();
                    Class     clzCur = (Class) entry.getKey();
                    Integer   ICurId = (Integer) entry.getValue();
                    if (clzCur != null && ICurId != null
                            && clzCur.isInterface()
                            && clzCur.isAssignableFrom(clz))
                        {
                        int nTypeId = ICurId.intValue();

                        // update the mapping so that we don't have to
                        // brute-force search again
                        mapClzToId.put(clz, ICurId);
                        return nTypeId;
                        }
                    }
                }
            }

        // update the mapping with the miss so that we don't have to
        // brute-force search again
        mapClzToId.put(clz, Integer.valueOf(-1));
        return -1;
        }

    /**
    * Determine the user type identifier associated with the given class
    * name.
    *
    * @param sClass  the name of a user type class; must not be null
    *
    * @return the type identifier of the user type associated with the given
    *         class name or -1 if the user type is unknown to this PofContext
    */
    protected int getUserTypeIdentifierInternal(String sClass)
        {
        ensureInitialized();

        int     nTypeId = -1;
        Map     mapNameToId = m_cfg.m_mapTypeIdByClassName;
        Integer ITypeId     = (Integer) mapNameToId.get(sClass);
        if (ITypeId == null)
            {
            if (sClass == null || sClass.length() == 0)
                {
                throw new IllegalArgumentException("class name is required");
                }

            // special cases: the class name is a sub-class of a user type
            // or a class that implements an interface that is a user type
            if (isSubclassAllowed() || isInterfaceAllowed() || isLambdaAllowed())
                {
                nTypeId = getUserTypeIdentifierInternal(loadClass(sClass));
                if (nTypeId >= 0)
                    {
                    mapNameToId.put(sClass, Integer.valueOf(nTypeId));
                    }
                }
            }
        else
            {
            nTypeId = ITypeId.intValue();
            }

        return nTypeId;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine if the ConfigurablePofContext has completed its
    * initialization.
    *
    * @return true iff the initialization is complete
    */
    protected boolean isInitialized()
        {
        return m_cfg != null;
        }

    /**
    * Obtain the location of the configuration that the
    * ConfigurablePofContext used to configure itself.
    *
    * @return the location information for the configuration for the
    *         ConfigurablePofContext, or null if not yet initialized and no
    *         location was specified
    */
    protected String getConfigLocation()
        {
        return m_sUri;
        }

    /**
    * Obtain the PofConfig that represents the initialized state of the
    * ConfigurablePofContext.
    *
    * @return the PofConfig for the ConfigurablePofContext, or null if not
    *         yet initialized
    */
    protected PofConfig getPofConfig()
        {
        return m_cfg;
        }

    /**
    * Determine if the ConfigurablePofContext supports the configuration of
    * user types by specifying an interface (instead of a class) for the
    * Java type.
    *
    * @return true iff an interface name is acceptable in the configuration
    *         as the class of a user type
    */
    protected boolean isInterfaceAllowed()
        {
        PofConfig cfg = m_cfg;
        return cfg != null && cfg.m_fInterfaceAllowed;
        }

    /**
    * Determine if the ConfigurablePofContext supports the serialization of
    * an object that is an instance of a sub-class of a configured type,
    * but not actually an instance of a class of a configured type.
    *
    * @return true iff serialization of sub-classes is explicitly enabled
    */
    protected boolean isSubclassAllowed()
        {
        PofConfig cfg = m_cfg;
        return cfg != null && cfg.m_fSubclassAllowed;
        }

    /**
     * Determine if implicit root lambda class processing is allowed.
     *
     * @return true iff implicit root lambda class processing is allowed.
     */
    protected boolean isLambdaAllowed()
        {
        PofConfig cfg = m_cfg;
        return cfg != null && cfg.m_mapTypeIdByClass.containsKey(ROOT_LAMBDA_CLASS);
        }

    /**
    * Determine if Identity/Reference type support is enabled for this
    * ConfigurablePofContext.
    *
    * @return true if Identity/Reference type support is enabled
    */
    public boolean isReferenceEnabled()
        {
        return m_fReferenceEnabled;
        }

    /**
    * Set the referenceEnabled flag.
    *
    * @param fReferenceEnabled  the referenceEnabled flag to set
    */
    public void setReferenceEnabled(boolean fReferenceEnabled)
        {
        m_fReferenceEnabled = fReferenceEnabled;
        }

    /**
     * Set the flag specifying if Java 8 date/time types (java.time.*) should be
     * preferred over legacy types.
     *
     * @param fPreferJavaTime  whether Java 8 data/time types
     */
    public void setPreferJavaTime(boolean fPreferJavaTime)
        {
        m_fPreferJavaTime = fPreferJavaTime;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Return a description of this ConfigurablePofContext.
    *
    * @return a String representation of the ConfigurablePofContext object
    */
    public String toString()
        {
        return getClass().getName() + " {location=" + m_sUri +'}';
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Verify that the ConfigurablePofContext has not already been
    * initialized.
    *
    * @throws IllegalStateException  if the ConfigurablePofContext is already
    *         fully initialized
    */
    protected void checkNotInitialized()
        {
        if (m_cfg != null)
            {
            throw new IllegalStateException("already initialized");
            }
        }

    /**
    * Fully initialize the ConfigurablePofContext if it has not already been
    * initialized.
    */
    protected void ensureInitialized()
        {
        if (m_cfg == null)
            {
            initialize();
            }
        }

    /**
    * Bind the ConfigurablePofContext to a ClassLoader, resolving all class
    * names, etc.
    */
    protected synchronized void initialize()
        {
        if (m_cfg == null)
            {
            // dereference by ClassLoader
            Map mapConfigByLoader = s_mapConfigurations;
            Map mapConfigByURI;
            synchronized (mapConfigByLoader)
                {
                ClassLoader loader = getContextClassLoader();
                mapConfigByURI = (Map) mapConfigByLoader.get(loader);
                if (mapConfigByURI == null)
                    {
                    mapConfigByURI = new SafeHashMap();
                    mapConfigByLoader.put(loader, mapConfigByURI);
                    }
                }

            // dereference by URI
            String sURI = m_sUri;
            if (sURI == null)
                {
                m_sUri = sURI = DEFAULT_RESOURCE;
                }
            PofConfig cfg = (PofConfig) mapConfigByURI.get(sURI);
            if (cfg == null)
                {
                cfg = createPofConfig();

                // now that a PofConfig has been created for the ClassLoader
                // and URI combination, store it for future use (assuming
                // that another thread didn't beat this thread to it)
                synchronized (mapConfigByURI)
                    {
                    if (mapConfigByURI.containsKey(sURI))
                        {
                        cfg = (PofConfig) mapConfigByURI.get(sURI);
                        }
                    else
                        {
                        mapConfigByURI.put(sURI, cfg);
                        }
                    }
                }

            // store configuration
            m_cfg = cfg;
            m_fReferenceEnabled = cfg.m_fReferenceEnabled;
            m_fPreferJavaTime   = cfg.m_fPreferJavaTime;
            }
        }

    /**
    * Create a PofConfig object based on a configuration that was either
    * provided as XML, or can be loaded from the specified (or default) URI
    * using the provided ClassLoader.
    *
    * @return a PofConfig for this ConfigurablePofContext
    */
    protected PofConfig createPofConfig()
        {
        // load the XML configuration if it is not already provided
        String     sURI      = m_sUri;
        XmlElement xmlConfig = m_xml;
        if (xmlConfig == null)
            {
            xmlConfig = XmlHelper.loadFileOrResource(sURI, "POF configuration", getContextClassLoader());
            }

        // get the type configuration information
        XmlElement xmlAllTypes = xmlConfig.getElement("user-type-list");
        if (xmlAllTypes == null)
            {
            report(sURI, -1, null, null, "Missing <user-type-list> element");
            }

        mergeIncludes(sURI, xmlConfig, getContextClassLoader());

        // extract options
        boolean fAllowInterfaces  = xmlConfig.getSafeElement("allow-interfaces").getBoolean();
        boolean fAllowSubclasses  = xmlConfig.getSafeElement("allow-subclasses").getBoolean();
        boolean fEnableReferences = xmlConfig.getSafeElement("enable-references").getBoolean();
        boolean fPreferJavaTime   = xmlConfig.getSafeElement("prefer-java-time").getBoolean();

        // scan the types for the highest type-id
        List    listTypes    = xmlAllTypes.getElementList();
        int     nMaxTypeId   = -1;
        boolean fSomeMissing = false;
        boolean fSomePresent = false;
        for (Iterator iter = listTypes.iterator(); iter.hasNext(); )
            {
            XmlElement xmlType = (XmlElement) iter.next();
            if (!xmlType.getName().equals("user-type"))
                {
                report(sURI, -1, null, null,
                       "<user-type-list> contains an illegal element: "
                       + xmlType.getName());
                }

            XmlElement xmlId = xmlType.getElement("type-id");
            if (xmlId == null)
                {
                fSomeMissing = true;
                if (fSomePresent)
                    {
                    report(sURI, -1, null, null, "<user-type-list> contains a"
                            + " <user-type> that is missing a type ID value");
                    }
                }
            else
                {
                int nTypeId = xmlId.getInt(-1);
                if (nTypeId < 0)
                    {
                    report(sURI, -1, null, null, "<user-type-list> contains a"
                            + " <user-type> that has a missing or invalid type"
                            + " ID value: " + xmlId.getString(null));
                    }

                fSomePresent = true;
                if (fSomeMissing)
                    {
                    report(sURI, -1, null, null, "<user-type-list> contains a"
                            + " <user-type> that is missing a type ID value");
                    }

                if (nTypeId > nMaxTypeId)
                    {
                    nMaxTypeId = nTypeId;
                    }
                }
            }

        boolean fAutoNumber = fSomeMissing;
        int     cElements   = fAutoNumber ? listTypes.size() : nMaxTypeId + 1;

        // create the relationships between type ids, class names and
        // classes
        Map mapTypeIdByClass         = new WeakHashMap();
        Map mapTypeIdByClassName     = new SafeHashMap();
        Map mapClassNameByTypeId     = new SafeHashMap();
        WeakReference[] aClzByTypeId = new WeakReference[cElements];
        PofSerializer[] aserByTypeId = new PofSerializer[cElements];

        int cTypeIds = 0;
        for (Iterator iter = listTypes.iterator(); iter.hasNext(); )
            {
            XmlElement xmlType = (XmlElement) iter.next();

            // determine the user type ID
            int nTypeId = fAutoNumber ? cTypeIds : xmlType.getElement("type-id").getInt();
            if (aClzByTypeId[nTypeId] != null)
                {
                report(sURI, nTypeId, null, null, "Duplicate user type id");
                }

            // determine the class name for the user type, and register it
            final String sClass = xmlType.getSafeElement("class-name").getString();
            if (sClass == null || sClass.length() == 0)
                {
                report(sURI, nTypeId, null, null, "Missing class name");
                }
            final Integer ITypeId = Integer.valueOf(nTypeId);

            // load the class for the user type, and register it
            final Class<?> clz;
            try
                {
                clz = loadClass(sClass);
                }
            catch (RuntimeException e)
                {
                throw report(sURI, nTypeId, sClass, e,
                             "Unable to load class for user type");
                }

            // check if it is an interface or abstract class
            if (clz.isInterface())
                {
                if (!fAllowInterfaces)
                    {
                    throw report(sURI, nTypeId, sClass, null,
                            "User Type cannot be an interface (allow-interfaces=false)");
                    }
                }
            else if (Modifier.isAbstract(clz.getModifiers()))
                {
                if (!fAllowSubclasses)
                    {
                    throw report(sURI, nTypeId, sClass, null,
                            "User Type cannot be an abstract class (allow-subclasses=false)");
                    }
                }

            // determine the serializer implementation, and register it
            XmlElement    xmlSer     = xmlType.getElement("serializer");
            PofSerializer serializer;

            if (xmlSer == null)
                {
                if (PortableObject.class.isAssignableFrom(clz))
                    {
                    serializer = clz.isAnnotationPresent(PortableType.class)
                                 ? new PortableTypeSerializer<>(nTypeId, clz)
                                 : new PortableObjectSerializer(nTypeId);
                    }
                else if (clz.getAnnotation(Portable.class) == null)
                    {
                    throw report(sURI, nTypeId, clz.getName(), null,
                            "Missing PofSerializer configuration");
                    }
                else
                    {
                    serializer = new PofAnnotationSerializer(nTypeId, clz);
                    }
                }
            else
                {
                serializer = instantiateSerializer(xmlSer, nTypeId, clz);
                }

            // PofSerializer initialization: XmlConfigurable
            if (serializer instanceof XmlConfigurable)
                {
                try
                    {
                    XmlElement xmlParams = new SimpleElement("config");
                    XmlHelper.transformInitParams(xmlParams,
                            xmlSer.getSafeElement("init-params"));
                    ((XmlConfigurable) serializer).setConfig(xmlParams);
                    }
                catch (RuntimeException e)
                    {
                    report(sURI, nTypeId, sClass, e,
                          "Unable to configure PofSerializer");
                    }
                }

            // PofSerializer initialization: ClassLoaderAware
            if (serializer instanceof ClassLoaderAware)
                {
                try
                    {
                    ((ClassLoaderAware) serializer).setContextClassLoader(
                            getContextClassLoader());
                    }
                catch (RuntimeException e)
                    {
                    report(sURI, nTypeId, sClass, e,
                           "Unable to set ContextClassLoader for PofSerializer");
                    }
                }

            // store information related to the user type
            mapTypeIdByClass.put(clz, ITypeId);
            mapTypeIdByClassName.put(sClass, ITypeId);
            mapClassNameByTypeId.put(ITypeId, sClass);
            aClzByTypeId[nTypeId] = new WeakReference(clz);
            aserByTypeId[nTypeId] = serializer;

            ++cTypeIds;
            }

        // store off the reusable configuring in a PofConfig object
        PofConfig cfg = new PofConfig();
        cfg.m_mapTypeIdByClass     = new CopyOnWriteMap(mapTypeIdByClass);
        cfg.m_mapTypeIdByClassName = mapTypeIdByClassName;
        cfg.m_mapClassNameByTypeId = mapClassNameByTypeId;
        cfg.m_aClzByTypeId         = aClzByTypeId;
        cfg.m_aserByTypeId         = aserByTypeId;
        cfg.m_fInterfaceAllowed    = fAllowInterfaces;
        cfg.m_fSubclassAllowed     = fAllowSubclasses;
        cfg.m_fReferenceEnabled    = fEnableReferences;
        cfg.m_fPreferJavaTime      = fPreferJavaTime;
        return cfg;
        }

    /**
     * Create a {@link PofSerializer} from the provided XML serializer
     * element.
     *
     * @param xmlSer   xml defining the serializer to create
     * @param nTypeId  the user type id this class is registered with
     * @param clz      the class of the user type
     *
     * @return a PofSerializer implementation capable of (de)serializing
     *         <tt>clz</tt>
     */
    protected PofSerializer instantiateSerializer(XmlElement xmlSer, int nTypeId, final Class clz)
        {
        final Integer ITypeId    = Integer.valueOf(nTypeId);
        PofSerializer serializer = null;
        String        sURI       = m_sUri;

        if (xmlSer != null)
            {
            String sSerClass = xmlSer.getElement("class-name").getString();
            if (sSerClass == null || sSerClass.length() == 0)
                {
                report(sURI, nTypeId, clz.getName(), null,
                       "Missing PofSerializer class name");
                }

            // load the class for the user type, and register it
            Class clzSer;
            try
                {
                clzSer = loadClass(sSerClass);
                }
            catch (RuntimeException e)
                {
                throw report(sURI, nTypeId, clz.getName(), e,
                        "Unable to load PofSerializer class: " + sSerClass);
                }

            if (!PofSerializer.class.isAssignableFrom(clzSer))
                {
                throw report(sURI, nTypeId, clz.getName(), null,
                        "Class is not a PofSerializer: " + sSerClass);
                }

            // only attempt the default PofSerializer constructors if
            // there are no parameters specified, or if there is at least
            // one parameter specified, but it doesn't have a type (which
            // indicates that the serializer is XmlConfigurable using a
            // transposed form of the parameters)
            XmlElement xmlParams = xmlSer.getElement("init-params");
            Iterator   iterParam;
            InstantiateSerializer: if (xmlParams == null
                    || (iterParam = xmlParams.getElements("init-param")).hasNext()
                    && ((XmlElement) iterParam.next()).getElement("param-type") == null)
                {
                // try the four prescribed constructors
                try
                    {
                    serializer = (PofSerializer) ClassHelper.newInstance(clzSer,
                            new Object[] {ITypeId, clz, getContextClassLoader()});
                    break InstantiateSerializer;
                    }
                catch (Throwable e) {}

                try
                    {
                    serializer = (PofSerializer) ClassHelper.newInstance(clzSer,
                            new Object[] {ITypeId, clz});
                    break InstantiateSerializer;
                    }
                catch (Throwable e) {}

                try
                    {
                    serializer = (PofSerializer) ClassHelper.newInstance(clzSer,
                            new Object[] {ITypeId});
                    break InstantiateSerializer;
                    }
                catch (Throwable e) {}

                try
                    {
                    serializer = (PofSerializer) clzSer.newInstance();
                    }
                catch (Throwable e)
                    {
                    // all four failed, so use the exception from this
                    // most recent failure as the basis for reporting
                    // the failure
                    throw report(sURI, nTypeId, clz.getName(), e,
                            "Unable to instantiate PofSerializer class using"
                            + " predefined constructors: " + sSerClass);
                    }
                }
            else
                {
                // create a parameter resolver for the substitutable
                // parameters
                XmlHelper.ParameterResolver resolver = new XmlHelper.ParameterResolver()
                    {
                    public Object resolveParameter(String sType, String sValue)
                        {
                        if (sValue.equals("{type-id}"))
                            {
                            return ITypeId;
                            }
                        else if (sValue.equals("{class-name}"))
                            {
                            return clz.getName();
                            }
                        else if (sValue.equals("{class}"))
                            {
                            return clz;
                            }
                        else if (sValue.equals("{class-loader}"))
                            {
                            return getContextClassLoader();
                            }
                        else
                            {
                            return sValue;
                            }
                        }
                    };

                // parse the constructor parameters
                Object[] aoParams;
                try
                    {
                    aoParams = XmlHelper.parseInitParams(xmlParams, resolver);
                    }
                catch (RuntimeException e)
                    {
                    throw report(sURI, nTypeId, clz.getName(), e,
                            "Error parsing constructor parameters for PofSerializer:"
                            + sSerClass);
                    }

                // instantiate the serializer
                try
                    {
                    serializer = (PofSerializer) ClassHelper.newInstance(clzSer, aoParams);
                    }
                catch (Throwable e)
                    {
                    throw report(sURI, nTypeId, clz.getName(), e,
                            "Unable to instantiate PofSerializer class: " + sSerClass);
                    }
                }
            }

        return serializer;
        }

    /**
    * Find the specified class, return a Java Class object for it.
    *
    * @param sClass  the fully qualified class name
    *
    * @return the Class object for the specified class name, never null
    *
    * @throws RuntimeException  a RuntimeException (or a subclass thereof)
    *         is thrown if the specified Class could not be loaded
    */
    protected Class loadClass(String sClass)
        {
        try
            {
            return ExternalizableHelper.loadClass(sClass, getContextClassLoader(), null);
            }
        catch (ClassNotFoundException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Assemble and throw an informative exception based on the passed
    * details.
    *
    * @param sURI     the URI of the configuration
    * @param nTypeId  the type ID (if applicable and if known)
    * @param sClass   the user type class name (if applicable and if known)
    * @param e        the underlying exception, if any
    * @param sText    the detailed description of the problem
    *
    * @return this method does not return; it always throws an exception
    *
    * @throws IllegalStateException  always thrown
    */
    protected RuntimeException report(String sURI, int nTypeId, String sClass,
                                      Throwable e, String sText)
        {
        StringBuffer sb = new StringBuffer();

        if (sURI != null && sURI.length() > 0)
            {
            sb.append("Config=")
              .append(sURI);
            }

        if (nTypeId >= 0)
            {
            if (sb.length() > 0)
                {
                sb.append(", ");
                }

            sb.append("Type-Id=")
              .append(nTypeId);
            }

        if (sClass != null && sClass.length() > 0)
            {
            if (sb.length() > 0)
                {
                sb.append(", ");
                }

            sb.append("Class-Name=")
              .append(sClass);
            }

        if (sb.length() > 0)
            {
            sText = sText + " (" + sb + ')';
            }

        throw e == null
              ? new IllegalStateException(sText)
              : Base.ensureRuntimeException(e, sText);
        }

    /**
     * Merge all included POF configuration files into the given xml configuration.
     *
     * @param sURI       the URI of the POF configuration file
     * @param xmlConfig  the base POF configuration
     * @param loader     the {@link ClassLoader} used to find the included
     *                   POF configuration resources
     */
    public static void mergeIncludes(String sURI, XmlElement xmlConfig, ClassLoader loader)
        {
        // extract options
        boolean    fAllowInterfaces  = xmlConfig.getSafeElement("allow-interfaces").getBoolean();
        boolean    fAllowSubclasses  = xmlConfig.getSafeElement("allow-subclasses").getBoolean();
        boolean    fEnableReferences = xmlConfig.getSafeElement("enable-references").getBoolean();
        XmlElement xmlAllTypes       = xmlConfig.getElement("user-type-list");

        // add default-serializer to each user-type
        appendDefaultSerializerToUserTypes(xmlConfig);

        // locate and add all included user types
        for (List listURI = null; xmlAllTypes.getElement("include") != null; )
            {
            if (listURI == null)
                {
                listURI = new ArrayList();
                listURI.add(sURI);
                }

            // load included URIs, checking for duplicates
            List listInclude = new ArrayList();
            for (Iterator iter = xmlAllTypes.getElements("include"); iter.hasNext(); )
                {
                String sIncludeURI = ((XmlElement) iter.next()).getString();
                iter.remove();

                if (!listURI.contains(sIncludeURI))
                    {
                    listURI.add(sIncludeURI);
                    listInclude.add(XmlHelper.loadFileOrResource(
                            sIncludeURI, "included POF configuration", loader));
                    }
                }

            // add the user types from all included URIs and adjust options
            for (Iterator iter = listInclude.iterator(); iter.hasNext(); )
                {
                XmlElement xmlInclude      = (XmlElement) iter.next();
                XmlElement xmlIncludeTypes = xmlInclude.getSafeElement("user-type-list");

                appendDefaultSerializerToUserTypes(xmlInclude);
                fAllowInterfaces  |= xmlInclude.getSafeElement("allow-interfaces").getBoolean();
                fAllowSubclasses  |= xmlInclude.getSafeElement("allow-subclasses").getBoolean();
                fEnableReferences |= xmlInclude.getSafeElement("enable-references").getBoolean();

                XmlHelper.addElements(xmlAllTypes, xmlIncludeTypes.getElements("user-type"));
                XmlHelper.addElements(xmlAllTypes, xmlIncludeTypes.getElements("include"));
                }
            }

        xmlConfig.ensureElement("allow-interfaces").setBoolean(fAllowInterfaces);
        xmlConfig.ensureElement("allow-subclasses").setBoolean(fAllowSubclasses);
        xmlConfig.ensureElement("enable-references").setBoolean(fEnableReferences);
        }

    /**
    * Process &lt;default-serializer&gt; element from the specified xml
    * configuration and append information about the serializer to each
    * &lt;user-type&gt; element within &lt;user-type-list&gt; unless
    * user type already has a serializer specified.
    * <p>
    * This method could be overridden to add new custom configuration
    * elements to pof-config.
    *
    * @param xmlConfig  the XmlElement containing pof configuration
    */
    protected static void appendDefaultSerializerToUserTypes(XmlElement xmlConfig)
        {
        XmlElement xmlDefaultSerializer = xmlConfig.getElement("default-serializer");

        if (xmlDefaultSerializer != null)
            {
            XmlElement xmlAllTypes = xmlConfig.getElement("user-type-list");

            for (Iterator iter = xmlAllTypes.getElements("user-type");
                 iter.hasNext(); )
                {
                XmlElement xmlType = (XmlElement) iter.next();
                XmlElement xmlSer  = xmlType.getElement("serializer");

                if (xmlSer == null)
                    {
                    // add the default-serializer to this user-type
                    XmlElement xmlNewSer = (XmlElement) xmlDefaultSerializer.clone();
                    xmlNewSer.setName("serializer");
                    xmlType.getElementList().add(xmlNewSer);
                    }
                }
            }
        }


    // ----- inner class: PofConfig -----------------------------------------

    /**
    * The information related to the configuration of a particular PofContext
    * for a specific URI and ClassLoader.
    */
    protected static class PofConfig
        {
        /**
        * Once initialized, this references a thread-safe Map that contains
        * mappings from Java classes to POF type identifiers (wrapped as
        * Integer objects). The initial contents of the Map reflect the
        * configuration, but the contents can increase over time as
        * sub-classes of the contained classes are resolved to type IDs (and
        * those mappings are added).
        */
        public Map m_mapTypeIdByClass;

        /**
        * Once initialized, this references a thread-safe Map that contains
        * mappings from Java class names to POF type identifiers (wrapped as
        * Integer objects). The initial contents of the Map reflect the
        * configuration, but the contents can increase over time as the names
        * of sub-classes (i.e. of the classes corresponding to the contained
        * class names) are resolved to type IDs (and those mappings are
        * added).
        */
        public Map m_mapTypeIdByClassName;

        /**
        * An array of WeakReferences to user type classes, indexed by type identifier.
        */
        public WeakReference[] m_aClzByTypeId;

        /**
        * An array of PofSerializer objects, indexed by type identifier.
        */
        public PofSerializer[] m_aserByTypeId;

        /**
        * True iff an interface name is acceptable in the configuration as
        * the class of a user type.
        */
        public boolean m_fInterfaceAllowed;

        /**
        * True iff serialization of sub-classes is explicitly enabled.
        */
        public boolean m_fSubclassAllowed;

        /**
        * True iff POF Identity/Reference type support is enabled.
        */
        public boolean m_fReferenceEnabled;

        /**
         * True if Java 8 date/time types (java.time.*) should be preferred over
         * legacy types.
         */
        public boolean m_fPreferJavaTime;

        /**
        * Once initialized, this references a thread-safe Map that contains
        * mappings from POF type identifiers (wrapped as Integer objects) to
        * Java class names. The initial contents of the Map reflect the
        * configuration, but the contents can increase over time as the names
        * of sub-classes (i.e. of the classes corresponding to the contained
        * class names) are resolved to type IDs (and those mappings are
        * added).
        */
        public Map m_mapClassNameByTypeId;
        }


    // ----- constants ------------------------------------------------------

    /**
    * The name of the system property (<tt>"tangosol.pof.config"</tt>) that
    * can be used to override the location of the default POF configuration
    * file.
    * <p>
    * The value of this property must be the name of a resource that contains
    * an XML document with the structure defined in <tt>/coherence-pof-config.xsd</tt>
    * (deployed in <tt>coherence.jar</tt>).
    * <p>
    * The default value for the <tt>"coherence.pof.config"</tt> system
    * property is <tt>"coherence-pof-config.xml"</tt>.
    */
    public static final String PROPERTY_CONFIG = "coherence.pof.config";

    /**
    * The name of the application resource that contains the default set of
    * wire-format-to-object bindings.
    * <p>
    * The default value for the resource name is
    * <tt>"pof-config.xml"</tt>. The default can be overriden by
    * specifying a value for the {@link #PROPERTY_CONFIG tangosol.pof.config}
    * system property.
    */
    public static final String DEFAULT_RESOURCE =
            Config.getProperty(PROPERTY_CONFIG, "pof-config.xml");

    /**
     * Marker serving as the implicit root class for all lambdas.
     */
    protected static final Class ROOT_LAMBDA_CLASS = FunctionalInterface.class;


    // ----- data members ---------------------------------------------------

    /**
    * Map of configuration information, keyed by ClassLoader.
    */
    private static final Map s_mapConfigurations = new WeakHashMap();

    /**
    * A WeakReference to the ClassLoader specified for this PofContext to
    * use.
    */
    private WeakReference<ClassLoader> m_refLoader;

    /**
    * The URI that specifies the location of the configuration file.
    */
    private String m_sUri;

    /**
    * The XML configuration, if supplied by constructor, or by the
    * XmlConfigurable interface.
    */
    private XmlElement m_xml;

    /**
    * True if POF Identity/Reference type support is enabled. Allows us to
    * override the static one in PofConfig.
    */
    private boolean m_fReferenceEnabled;

    /**
     * True if Java 8 date/time types (java.time.*) should be preferred over
     * legacy types. Allows us to override the static one in PofConfig.
     */
    private boolean m_fPreferJavaTime;

    /**
    * The PofConfig for this PofContext to use.
    */
    private volatile PofConfig m_cfg;
    }
