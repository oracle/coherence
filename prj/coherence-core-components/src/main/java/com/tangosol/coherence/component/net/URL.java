
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.URL

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.util.FileHelper;
import com.tangosol.util.WrapperException;
import java.net.MalformedURLException;

/*
* Integrates
*     java.net.URL
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class URL
        extends    com.tangosol.coherence.component.Net
    {
    // ---- Fields declarations ----
    
    /**
     * Property _URL
     *
     */
    private transient java.net.URL __m__URL;
    
    /**
     * Property BaseURL
     *
     * The base URL used to provide insufficient data in order to resolve this
     * URL component. Values of Protocol, Port and Host properties of this URL
     * component, if specified, take precedence over the corresponding values
     * of the BaseURL. The value of the File property, which is required, gets
     * concatenated to the corresponding value of the BaseURL.
     * 
     * @see get_URL
     */
    private URL __m_BaseURL;
    
    /**
     * Property File
     *
     * "File name" portion of the URL.
     */
    private String __m_File;
    
    /**
     * Property Host
     *
     * "Host" portion of the URL.
     */
    private String __m_Host;
    
    /**
     * Property Port
     *
     * "Port" portion of the URL.
     */
    private int __m_Port;
    
    /**
     * Property Protocol
     *
     * "Protocol" portion of the URL.
     */
    private String __m_Protocol;
    
    // Default constructor
    public URL()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public URL(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        
        if (fInit)
            {
            __init();
            }
        }
    
    // Main initializer
    public void __init()
        {
        // private initialization
        __initPrivate();
        
        // state initialization: public and protected properties
        try
            {
            setFile("");
            setPort(-1);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.net.URL();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/net/URL".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    
    //++ java.net.URL integration
    // Access optimization
    // properties integration
    // methods integration
    public Object getContent()
            throws java.io.IOException
        {
        return get_URL().getContent();
        }
    /**
     * Returns a URLConnection object that represents a connection to the remote
    * object referred to by the URL. A new connection is opened every time by
    * calling the openConnection method of the protocol handler for this URL.
    * 
    * If for the URL's protocol (such as HTTP or JAR), there exists a public,
    * specialized URLConnection subclass belonging to one of the following
    * packages or one of their subpackages: java.lang, java.io, java.util,
    * java.net, the connection returned will be of that subclass. For example,
    * for HTTP an HttpURLConnection will be returned, and for JAR a
    * JarURLConnection will be returned.
     */
    public java.net.URLConnection openConnection()
            throws java.io.IOException
        {
        return get_URL().openConnection();
        }
    /**
     * Opens a connection to this URL and returns an InputStream for reading
    * from that connection.
    * This method is a shorthand for: 
    * 
    *        openConnection().getInputStream()
     */
    public java.io.InputStream openStream()
            throws java.io.IOException
        {
        return get_URL().openStream();
        }
    /**
     * Constructs a string representation of this URL. The string is created by
    * calling the toExternalForm method of the stream protocol handler for this
    * object.
     */
    public String toExternalForm()
        {
        return get_URL().toExternalForm();
        }
    //-- java.net.URL integration
    
    // Declared at the super level
    /**
     * Apply configuration information about this component from the specified
    * property table using the specified string to prefix the property names.
    * 
    * Looks for the following properties:
    * - sPrefix + ".Protocol": a string giving the URL's protocol
    * - sPrefix + ".Host": a string giving the URL's host
    * - sPrefix + ".Port": an int defining the URL's port
    * - sPrefix + ".File": a string giving the URL's file
     */
    public void applyConfig(com.tangosol.coherence.component.util.Config config, String sPrefix)
        {
        // only override properties that are defined in config
        setProtocol(config.getString(sPrefix + ".Protocol", getProtocol()));
        setHost    (config.getString(sPrefix + ".Host"    , getHost()));
        setPort    (config.getInt   (sPrefix + ".Port"    , getPort()));
        setFile    (config.getString(sPrefix + ".File"    , getFile()));
        
        super.applyConfig(config, sPrefix);
        }
    
    // Accessor for the property "_URL"
    /**
     * Getter for property _URL.<p>
     */
    public java.net.URL get_URL()
        {
        // import java.net.MalformedURLException;
        // import java.net.URL as java.net.URL;
        // import com.tangosol.util.WrapperException;
        
        java.net.URL _url = __m__URL;
        if (_url == null)
            {
            String sProtocol  = getProtocol();
            String sHost      = getHost();
            int    iPort      = getPort();
            String sFile      = getFile();
            
            if (sProtocol == null && sHost == null)
                {
                // use base's URL parts if there
                URL urlBase = getBaseURL();
                if (urlBase != null && urlBase.isBaseFor(this))
                    {
                    sProtocol = urlBase.getProtocol();
                    sHost     = urlBase.getHost();
                    iPort     = urlBase.getPort();
        
                    String sFileBase = urlBase.getFile();
                    if (sFileBase != null)
                        {
                        // append this URLs file to base
                        sFile = sFileBase + sFile;
                        }
                    }
                }
        
            // create the java URL (note, sFile cannot be null!)
            try
                {
                if (sFile == null)
                    {
                    sFile = "";
                    }
                set_URL(_url = new java.net.URL(sProtocol, sHost, iPort, sFile));
                }
            catch (MalformedURLException e)
                {
                throw new WrapperException(e);
                }
            }
        
        return _url;
        }
    
    // Accessor for the property "BaseURL"
    /**
     * Getter for property BaseURL.<p>
    * The base URL used to provide insufficient data in order to resolve this
    * URL component. Values of Protocol, Port and Host properties of this URL
    * component, if specified, take precedence over the corresponding values of
    * the BaseURL. The value of the File property, which is required, gets
    * concatenated to the corresponding value of the BaseURL.
    * 
    * @see get_URL
     */
    public URL getBaseURL()
        {
        return __m_BaseURL;
        }
    
    // Accessor for the property "File"
    /**
     * Getter for property File.<p>
    * "File name" portion of the URL.
     */
    public String getFile()
        {
        return __m_File;
        }
    
    // Accessor for the property "Host"
    /**
     * Getter for property Host.<p>
    * "Host" portion of the URL.
     */
    public String getHost()
        {
        return __m_Host;
        }
    
    // Accessor for the property "Port"
    /**
     * Getter for property Port.<p>
    * "Port" portion of the URL.
     */
    public int getPort()
        {
        return __m_Port;
        }
    
    // Accessor for the property "Protocol"
    /**
     * Getter for property Protocol.<p>
    * "Protocol" portion of the URL.
     */
    public String getProtocol()
        {
        return __m_Protocol;
        }
    
    // Accessor for the property "RelativePath"
    /**
     * Returns a string that represents this URL's path relatively to the base
    * URL's path.
    * 
    * For example: if this URL is "http://www.tangosol.com/doc/index.html" and
    * the base URL is "http://www.tangosol.com" then the relative path is
    * "/doc/index.html"
     */
    public String getRelativePath()
        {
        URL urlBase = getBaseURL();
        if (urlBase != null && urlBase.isBaseFor(this))
            {
            return getFile().substring(urlBase.getFile().length());
            }
        else
            {
            return get_URL().toString();
            }
        }
    
    // Accessor for the property "RelativePath"
    /**
     * Returns a string that represents the specified URL's path relatively to
    * this URL's path. The specified URL must have the same values of Protocol,
    * Host and Port properties.
    * 
    * For example: if this URL is "http:///www.tangosol.com/doc/index.html" and
    * the specified URL is "http:///www.tangosol.com/doc/tools.html" then the
    * retrun value is "./tool.html"
     */
    public String getRelativePath(URL url)
        {
        // import Component.Util.FileHelper;
        
        if (url.getProtocol().equals(getProtocol()) &&
            url.getHost()    .equals(getHost())     &&
            url.getPort() == getPort())
            {
            String sPathThis = getFile();
            String sPathThat = url.getFile();
        
            return FileHelper.getRelativePath(sPathThat, sPathThis);
            }
        else
            {
            return url.get_URL().toString();
            }
        }
    
    /**
     * Determines whether this component could be a base URL for the specified
    * URL.
    * 
    * @return true if one of the following conditions is satisfied:
    * 1) The specified URL has no values set for Protocol, Host and Port
    * properties.
    * 2)  The specified URL has the same values for Protocol, Host and Port
    * properties as this URL and the value of the File property of the
    * specified URL starts with the value of the File property for this URL.
     */
    private boolean isBaseFor(URL url)
        {
        String sProtocol = url.getProtocol();
        String sHost     = url.getHost();
        int    iPort     = url.getPort();
        String sFile     = url.getFile();
        
        if (sProtocol == null && sHost == null && iPort == -1)
            {
            return true;
            }
        else
            {
            return sProtocol != null  && sProtocol.equals(getProtocol()) &&
                   sHost     != null  && sHost    .equals(getHost())     &&
                   iPort == getPort() &&
                   sFile     != null  && sFile.startsWith(getFile());
            }
        }
    
    // Declared at the super level
    /**
     * Save configuration information about this component into the specified
    * Config component using the specified string to prefix the property names.
    * 
    * Puts out the following properties if non-default values are defined by
    * the component:
    * - sPrefix + ".Protocol": a string giving the URL's protocol
    * - sPrefix + ".Host": a string giving the URL's host
    * - sPrefix + ".Port": an int defining the URL's port
    * - sPrefix + ".File": a string giving the URL's file
     */
    public void saveConfig(com.tangosol.coherence.component.util.Config config, String sPrefix)
        {
        // only output properties that have non-default values
        String sProtocol = getProtocol();
        if (sProtocol != null)
            {
            config.putString(sPrefix + ".Protocol", sProtocol);
            }
        String sHost = getHost();
        if (sHost != null)
            {
            config.putString(sPrefix + ".Host", sHost);
            }
        int iPort = getPort();
        if (iPort != -1)
            {
            config.putInt(sPrefix + ".Port", iPort);
            }
        String sFile = getFile();
        if (sFile != null)
            {
            config.putString(sPrefix + ".File", sFile);
            }
        
        // let super do its thing
        // note, there is no need to save BaseURL
        super.saveConfig(config, sPrefix);
        }
    
    // Accessor for the property "_URL"
    /**
     * Setter for property _URL.<p>
     */
    public void set_URL(java.net.URL p_URL)
        {
        if (p_URL != null)
            {
            setProtocol(p_URL.getProtocol());
            setHost    (p_URL.getHost());
            setPort    (p_URL.getPort());
            setFile    (p_URL.getFile());
            }
        __m__URL = (p_URL);
        }
    
    // Accessor for the property "BaseURL"
    /**
     * Setter for property BaseURL.<p>
    * The base URL used to provide insufficient data in order to resolve this
    * URL component. Values of Protocol, Port and Host properties of this URL
    * component, if specified, take precedence over the corresponding values of
    * the BaseURL. The value of the File property, which is required, gets
    * concatenated to the corresponding value of the BaseURL.
    * 
    * @see get_URL
     */
    public void setBaseURL(URL pBaseURL)
        {
        __m_BaseURL = pBaseURL;
        }
    
    // Accessor for the property "File"
    /**
     * Setter for property File.<p>
    * "File name" portion of the URL.
     */
    public void setFile(String pFile)
        {
        __m_File = (pFile);
        set_URL(null);
        }
    
    // Accessor for the property "Host"
    /**
     * Setter for property Host.<p>
    * "Host" portion of the URL.
     */
    public void setHost(String pHost)
        {
        __m_Host = (pHost);
        set_URL(null);
        }
    
    // Accessor for the property "Port"
    /**
     * Setter for property Port.<p>
    * "Port" portion of the URL.
     */
    public void setPort(int pPort)
        {
        __m_Port = (pPort);
        set_URL(null);
        }
    
    // Accessor for the property "Protocol"
    /**
     * Setter for property Protocol.<p>
    * "Protocol" portion of the URL.
     */
    public void setProtocol(String pProtocol)
        {
        __m_Protocol = (pProtocol);
        set_URL(null);
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_URL().toString();
        }
    }
