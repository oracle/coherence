/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke.lambda;

import com.tangosol.util.Base;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.invoke.SerializedLambda;

import java.net.URL;
import java.net.URLConnection;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@code AnonymousLambdaIdentity} is a specialization of {@link LambdaIdentity}
 * that is used to identify anonymous/synthetic lambdas.
 * <p>
 * This class derives uniqueness based upon three characteristics: capturing
 * class, synthetic/implementation method name, and MD5(capturing class).
 *
 * @author hr/as  2015.06.01
 * @since 12.2.1
 *
 * @see MethodReferenceIdentity
 */
public class AnonymousLambdaIdentity
        extends LambdaIdentity
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public AnonymousLambdaIdentity()
        {
        }

    /**
     * Construct a AnonymousLambdaIdentity that represents the provided
     * {@link SerializedLambda function metadata}.
     *
     * @param lambdaMetadata  function metadata
     * @param loader          ClassLoader used to load the capturing class file
     */
    public AnonymousLambdaIdentity(SerializedLambda lambdaMetadata, ClassLoader loader)
        {
        super(lambdaMetadata.getImplClass(),
                lambdaMetadata.getImplMethodName(),
                createVersion(lambdaMetadata, loader));
        }

    /**
     * Create unique version string for the specified lambda.
     *
     * @param lambdaMetadata  function metadata
     * @param loader          ClassLoader used to load the capturing class file
     *
     * @return a unique version string for the specified lambda
     */
    protected static String createVersion(SerializedLambda lambdaMetadata, ClassLoader loader)
        {
        String   sBinaryClassName = lambdaMetadata.getImplClass();
        String   sVersion         = null;
        Class<?> clzImplClass     = null;
        long     nLastModified    = 0;

        try
            {
            clzImplClass = loader.loadClass(sBinaryClassName.replace('/', '.'));
            sVersion     = f_mapVersionCache.get(clzImplClass);

            if (sVersion == null || !sVersion.startsWith("0:")) // 0 timestamp used to indicate cached non-jshell based class
                {
                // COH-16891: jshell doesn't change the Class when the class changes, so we have to look at the timestamp as well
                // we don't do this for non-jshell as it is involves file system access.

                URL urlClass = loader.getResource(sBinaryClassName + ".class");
                if (urlClass != null && "jshell".equals(urlClass.getProtocol()))
                    {
                    URLConnection conn = urlClass.openConnection();
                    nLastModified = conn.getLastModified();
                    conn.getInputStream().close(); // if we don't do this we leak file descriptors (James Gosling's fault)

                    // determine whether the cached class has changed based on timestamp
                    if (sVersion != null && Long.parseLong(sVersion.substring(0, sVersion.indexOf(":"))) != nLastModified)
                        {
                        sVersion = null; // we have a new version
                        }
                    }
                }
            }
        catch (IOException | ClassNotFoundException ignore) {}

        if (sVersion == null)
            {
            try (InputStream is = loader.getResourceAsStream(sBinaryClassName + ".class"))
                {
                sVersion = nLastModified + ":" + Base.toHex(md5(is));
                if (clzImplClass != null)
                    {
                    f_mapVersionCache.put(clzImplClass, sVersion);
                    }
                }
            catch (IOException ignore) {} // only possible due to is.close()
            }

        return sVersion;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A cache of lambda implementation class (holder) to version formatted as
     * last-modified-time-stamp:MD5
     */
    // The cost of an MD5 is not significant in isolation, however with significant
    // load it becomes an observable cost. This cache reduces the number of
    // MD5 hashes by two factors:
    //   1. the number of lambdas in the same implementation class / holder
    //   2. the number of instances of the same lambda definition
    protected static final Map<Class, String> f_mapVersionCache = Collections.synchronizedMap(new WeakHashMap<>());
    }
