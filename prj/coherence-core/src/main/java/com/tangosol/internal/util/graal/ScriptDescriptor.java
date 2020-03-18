/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.graal;

import com.tangosol.util.ScriptException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.util.jar.JarEntry;

import java.util.stream.Collectors;
import org.graalvm.polyglot.Source;


/**
 * ScriptDescriptor holds details (like language, name, directory etc.) about a
 * script. The entry represented by this descriptor could be a file or a
 * directory and may exist in a file system, jar or even in a remote site.
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class ScriptDescriptor
    {
    // ----- constructors -----------------------------------------------------

    /**
     * Create a {@link ScriptDescriptor} with the specified language and path to
     * a script. All scripts are resolved relative to {@code /scripts/language}.
     *
     * @param sLanguage    the language in which it is implemented
     * @param sScriptPath  the path to the script
     */
    public ScriptDescriptor(String sLanguage, String sScriptPath)
        {
        String sResourcePath = toResourcePath(sLanguage, sScriptPath);
        URL    scriptURL     = Thread.currentThread().getContextClassLoader().getResource(sResourcePath);

        initialize(sLanguage, sResourcePath, scriptURL);
        }

    /**
     * Create a {@link ScriptDescriptor} with the specified language, path and
     * url. The script must be loaded using the specified {@code scriptUrl}
     * rather than loaded using context class loader. This allows loading of
     * scripts from a path that is relative to another script.
     *
     * @param sLanguage      the language in which it is implemented
     * @param sResourcePath  the full path (from "/scripts") to the resource
     * @param scriptUrl      the {@link URL} of the script
     */
    private ScriptDescriptor(String sLanguage, String sResourcePath, URL scriptUrl)
        {
        initialize(sLanguage, sResourcePath, scriptUrl);
        }

    //----- ScriptDescriptor methods -----------------------------------------


    /**
     * Returns {@code true} if the entry indicated by this descriptor exists,
     * {@code false} otherwise.
     *
     * @return {@code true} if the entry indicated by this descriptor exists,
     *         {@code false} otherwise
     */
    public boolean exists()
        {
        return m_fExists;
        }

    /**
     * Returns {@code true} if the path indicated by this descriptor is a
     * directory or not.
     *
     * @return {@code true} if the path indicated by this descriptor is a
     *         directory, {@code false} otherwise
     */
    public boolean isDirectory()
        {
        return m_fDirEntry;
        }

    /**
     * Returns {@code true} if the path indicated by this descriptor is the
     * root, {@code false} otherwise.
     *
     * @return {@code true} if the path indicated by this descriptor is the
     *         root, {@code false} otherwise
     */
    public boolean isRoot()
        {
        return m_sResourcePath.equals(scriptRoot());
        }

    /**
     * Resolves the specified script relative to the path of this descriptor and
     * returns a new {@link ScriptDescriptor} for the resolved script.
     *
     * @param sScript  The script whose path needs to be resolved with
     *               respect to the path of this descriptor
     *
     * @return A new {@link ScriptDescriptor} representing the specified script
     */
    public ScriptDescriptor resolve(String sScript)
        {
        if (sScript.startsWith("/"))
            {
            return new ScriptDescriptor(m_sLanguage, sScript);
            }

        try
            {
            // First resolve m_sResourcePath w.r.t. m_rootUri and then resolve
            // the specified script against the result. This will ensure that
            // directories are handled properly.
            //
            URI    uri     = m_rootUri.resolve(m_sResourcePath).resolve(sScript).normalize();
            String resPath = m_rootUri.relativize(uri).normalize().getRawPath();
            if (!resPath.startsWith(scriptRoot()))
                {
                throw new ScriptException("invalid path: " + sScript);
                }

            URI fullURI = new URI(m_rootUri.toString() + resPath);
            return new ScriptDescriptor(m_sLanguage, resPath, fullURI.toURL());
            }
        catch (URISyntaxException | MalformedURLException e)
            {
            throw new ScriptException(e.getMessage(), e);
            }
        }

    /**
     * Returns the path that represents the directory component of this
     * descriptor. More specifically, if this descriptor represents a file then
     * this method returns the directory containing this file, else it returns
     * the path to this descriptor.
     *
     * @return the path to the directory of this descriptor
     */
    public String getDirectory()
        {
        return m_sDirectoryName;
        }

    /**
     * If this descriptor is not the root, then this method returns the parent
     * of this {@link ScriptDescriptor}, else it returns {code null}.
     *
     * @return the parent of this {@link ScriptDescriptor} if it is not the
     * root, else it returns {@code null}
     */
    public ScriptDescriptor getParentDescriptor()
        {
        return isRoot() ? null : isDirectory() ? resolve("..") : resolve(".");
        }

    /**
     * Returns the script path.
     *
     * @return the path to the script
     */
    public String getScriptPath()
        {
        return m_sResourcePath.substring(scriptRoot().length());
        }

    /**
     * Returns the script path that can be used to load the resource using
     * {@code ClassLoader.getResource(getResourcePath()}.
     *
     * @return the script path to the resource that can be used to load by
     *         calling using {@code ClassLoader.getResource(getResourcePath()}
     */
    public String getResourcePath()
        {
        return m_sResourcePath;
        }

    /**
     * Returns the {@link URL} to the script. Could be {@code null} if
     * {@code exists()} returns false.
     *
     * @return the {@link URL} to the script. Could be {@code null} if
     *      {@code exists()} returns false
     */
    public URL getScriptUrl()
        {
        return m_scriptUrl;
        }

    /**
     * Returns the {@link Source} to the script.
     *
     * @return the {@link Source} to the script
     *
     * @throws ScriptException if the specified script cannot be loaded
     */
    public Source getScriptSource()
        {
        Source scriptSource = m_scriptSource;
        if (scriptSource == null)
            {
            try
                {
                scriptSource = m_scriptSource = Source.newBuilder(m_sLanguage, m_scriptUrl).build();
                }
            catch (IOException ex)
                {
                throw new ScriptException("exception while loading the script source: " + m_scriptUrl, ex);
                }
            }

        return scriptSource;
        }

    /**
     * Return the simple name of the script. This does not include the directory
     * part that contains this script.
     *
     * @return the simple name of the script. This does not include the directory
     *         part that contains this script
     */
    public String getSimpleName()
        {
        String[] components = m_sResourcePath.split("/");
        return components.length > 0 ? components[components.length-1] : null;
        }

    public Collection<String> listScripts()
        {
        try
            {
            ScriptUrlConnectionHandler handler = s_handlers.get(getScriptUrl().toURI().getScheme());
            return handler.listScripts(getScriptUrl().openConnection());
            }
        catch (URISyntaxException | IOException e)
            {
            throw new ScriptException("error while preloading scrips", e);
            }
        }

    // ----- helpers ---------------------------------------------------------

    /**
     * Sets if this descriptor by this descriptor represents a
     * directory (true) or not (false).
     *
     * @param isDir  {@code true} if the path indicated by this descriptor
     *               exists, {@code false} otherwise
     */
    private void setIsDirectory(boolean isDir)
        {
        m_fDirEntry = isDir;
        }

    /**
     * Sets the flag to indicate if the script exists ({@code true}) or
     * not ({@code false})
     *
     * @param fExists  the flag to indicate if the script exists ({@code true})
     *                 or not ({@code false})
     */
    private void setExists(boolean fExists)
        {
        m_fExists = fExists;
        }

    /**
     * Normalized full paths start with {@code /scripts/<language>}.
     *
     * @param sPathToScript  the path to the script
     *
     * @return the path to the script
     */
    private static String toResourcePath(String sLanguage, String sPathToScript)
        {
        try
            {
            if (sPathToScript.startsWith("/"))
                {
                sPathToScript = sPathToScript.substring(1);
                }
            String scriptRoot  = "/scripts/" + sLanguage + "/";

            // We use the "file" scheme just to be able to create a
            // normalized raw path.
            URI    uri   = new URI("file:" + scriptRoot + sPathToScript).normalize();
            String path  = uri.getRawPath();
            if (!path.startsWith(scriptRoot))
                {
                throw new IllegalArgumentException("Invalid scriptName: " + sPathToScript);
                }
            return path.substring(1);   // Without the leading "/"
            }
        catch (URISyntaxException e)
            {
            throw new ScriptException(e.getMessage(), e);
            }
        }

    /**
     * Internal helper method to return the script root for the specified language.
     *
     * @return the script root for the specified language
     */
    private String scriptRoot()
        {
        return "scripts/" + m_sLanguage + "/";
        }

    /**
     * Internal helper method to return the script root for the specified language.
     *
     * @return the script root for the specified language
     */
    private String stripScriptRootPrefix(String sPath)
        {
        if (sPath.startsWith(scriptRoot()))
            {
            sPath = sPath.substring(scriptRoot().length());
            }
        return sPath;
        }

    /**
     * Initialize this descriptor.
     *
     * @param sLanguage      the language in which the script has been implemented
     * @param sResourcePath  the full path to the resource
     * @param sScriptUrl     the path to the script
     */
    private void initialize(String sLanguage, String sResourcePath, URL sScriptUrl)
        {
        m_sLanguage     = sLanguage;
        m_sResourcePath = sResourcePath;
        m_scriptUrl     = sScriptUrl;

        if (m_sResourcePath.endsWith("/"))
            {
            m_sResourcePath = m_sResourcePath.substring(0, m_sResourcePath.length() - 1);
            }

        if (sScriptUrl != null)
            {
            try
                {
                URLConnection urlConn = sScriptUrl.openConnection();
                m_urlConnectionHandler = s_handlers.get(m_scriptUrl.toURI().getScheme());
                if (m_urlConnectionHandler == null)
                    {
                    throw new UnsupportedOperationException("protocol (" + sScriptUrl.getProtocol() + ") not supported");
                    }
                m_urlConnectionHandler.initializeDescriptor(this, urlConn);

                String rootPath = sScriptUrl.toURI().toString();
                int    index    = rootPath.lastIndexOf(m_sResourcePath);
                m_rootUri = new URI(rootPath.substring(0, index));

                // Important: If this is a directory, ensure that m_sResourcePath
                // ends with a "/" so that resolve() (actually URI) treats this
                // as a directory and resolves files properly.
                if (isDirectory())
                    {
                    m_sResourcePath  = m_sResourcePath + "/";
                    m_sDirectoryName = stripScriptRootPrefix(sResourcePath);
                    }
                else
                    {
                    int slash        = sResourcePath.lastIndexOf('/');
                    m_sDirectoryName = sResourcePath.substring(0, slash) + "/";
                    m_sDirectoryName = stripScriptRootPrefix(m_sDirectoryName);
                    }
                }
            catch (IOException | URISyntaxException e)
                {
                throw new ScriptException("error while initializing descriptor. scriptURL: " + sScriptUrl, e);
                }
            }
        }

    // --- inner interface UrlConnectionHandler ------------------------------

    /**
     * An interface that defines the set of methods that a handler must
     * implement in order to initialize a {@link ScriptDescriptor} and to list
     * scripts from a {@link URLConnection}.
     */
    public interface ScriptUrlConnectionHandler
        {
        /**
         * Initialize the descriptor from the specified {@link URLConnection}.
         *
         * @param descriptor  the descriptor to initialize
         * @param urlConn     the {@link URLConnection}
         *
         * @throws IOException thrown if there are any exceptions during
         *                     initialization
         */
        void initializeDescriptor(ScriptDescriptor descriptor, URLConnection urlConn)
                throws IOException;

        /**
         * List the scripts to be pre-loaded using the specified {@link URLConnection}.
         *
         * @param urlConn the {@link URLConnection}
         *
         * @throws IOException thrown if there are any exceptions during
         *                     initialization
         */
        Collection<String> listScripts(URLConnection urlConn)
                throws IOException;
        }

    // --- inner class JarURLConnectionHandler -------------------------------

    /**
     * An implementation of {@link ScriptUrlConnectionHandler} that handles
     * initialization of a {@link ScriptDescriptor} from a
     * {@link JarURLConnection}.
     */
    private static class JarUrlConnectionHandler
            implements ScriptUrlConnectionHandler
        {
        @Override
        public void initializeDescriptor(ScriptDescriptor descriptor, URLConnection urlConn)
                throws IOException
            {
            try
                {
                JarURLConnection jarUrlConn = (JarURLConnection) urlConn;
                JarEntry         jarEntry   = jarUrlConn.getJarEntry();
                if (jarEntry != null)
                    {
                    descriptor.setIsDirectory(jarEntry.isDirectory());
                    descriptor.setExists(true);
                    }
                }
            catch (FileNotFoundException fnfEx)
                {
                // This jar entry doesn't exist. Since descriptor.setDoesExist()
                // was not called, it is ok to ignore this exception.
                }
            }

        @Override
        public Collection<String> listScripts(URLConnection urlConn)
                throws IOException
            {
            JarURLConnection jarUrlConn = (JarURLConnection) urlConn;

            Collection<String> list = jarUrlConn.getJarFile().stream()
                    .map(je -> je.getName())
                    .map(n -> Paths.get(n))
                    .filter(p -> p.getNameCount() == 3)
                    .filter(p -> {
                                 String simpleName = p.getName(2).toString();
                                 return (simpleName.endsWith("js") || simpleName.endsWith(".mjs"));
                                 })
                    .map(p -> p.getName(2).toString())
                    .collect(Collectors.toList());

            return list;
            }
        }

    // --- inner class FileURLConnectionHandler ------------------------------

    /**
     * An implementation of {@link ScriptUrlConnectionHandler} that handles
     * initialization of a {@link ScriptDescriptor} from a {@link URLConnection}
     * that uses {@code file} protocol.
     */
    private static class FileURLConnectionHandler
            implements ScriptUrlConnectionHandler
        {
        @Override
        public void initializeDescriptor(ScriptDescriptor descriptor, URLConnection urlConn)
            {
            File entry = new File(urlConn.getURL().getFile());
            descriptor.setIsDirectory(entry.isDirectory());
            descriptor.setExists(entry.exists());
            }

        @Override
        public Collection<String> listScripts(URLConnection urlConn)
            {
            File   entry   = new File(urlConn.getURL().getFile());
            File[] scripts = entry.listFiles((dir, name) -> name.endsWith(".js") || name.endsWith(".mjs"));
            if (scripts != null)
                {
                return Arrays.stream(scripts)
                        .map(f -> f.getName())
                        .collect(Collectors.toList());
                }
            return Collections.emptyList();
            }
        }

    // --- Object methods ----------------------------------------------------

    @Override
    public String toString()
        {
        return "ScriptDescriptor{" +
               "language='" + m_sLanguage + '\'' +
               ", doesExist=" + m_fExists +
               ", dirEntry=" + m_fDirEntry +
               ", resourcePath='" + m_sResourcePath + '\'' +
               ", directoryName='" + m_sDirectoryName + '\'' +
               ", rootUri=" + m_rootUri +
               ", scriptUrl=" + m_scriptUrl +
               ", scriptSource=" + m_scriptSource +
               '}';
        }

    // ----- data members ----------------------------------------------------

    /**
     * The language in which the script is implemented.
     */
    private String m_sLanguage;

    /**
     * Indicates if the entry denoted by this descriptor exists.
     */
    private boolean m_fExists;

    /**
     * Indicates if the entry denoted by this descriptor is a directory.
     */
    private boolean m_fDirEntry;

    /**
     * The full path to the script.
     */
    private String m_sResourcePath;

    /**
     * The name of the directory if this {@link ScriptDescriptor} represents a
     * script, else the path to this descriptor itself.
     */
    private String m_sDirectoryName;

    /**
     * Just for internal use. The {@link URI} to the parent of
     * {@code "/scripts/" + m_sLanguage}. This allows resolving relative paths
     * much easier.
     */
    private URI m_rootUri;

    /**
     * The {@link URL} of the script.
     */
    private URL m_scriptUrl;

    /**
     * The Graal {@link Source} for this entry. Valid only if this entry is
     * <b>not</b> a directory. Loaded lazily.
     */
    private Source m_scriptSource;

    /**
     * The {@link ScriptUrlConnectionHandler} that was used to initialize this
     * {@link ScriptDescriptor}.
     */
    private ScriptUrlConnectionHandler m_urlConnectionHandler;

    /**
     * A Map of protocol name to {@link ScriptUrlConnectionHandler}.
     */
    private static Map<String, ScriptUrlConnectionHandler> s_handlers = new HashMap<>();

    static
        {
        s_handlers.put("jar", new JarUrlConnectionHandler());
        s_handlers.put("file", new FileURLConnectionHandler());
        }
    }

