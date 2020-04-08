/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.bdb.BerkeleyDBBinaryStoreManager;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.io.File;

/**
 * The BdbStoreManagerBuilder class builds an instance of a
 * BerkeleyDBBinaryStoreManager.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class BdbStoreManagerBuilder
        extends AbstractStoreManagerBuilder<BerkeleyDBBinaryStoreManager>
    {
    // ----- StoreManagerBuilder interface ----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public BerkeleyDBBinaryStoreManager realize(ParameterResolver resolver, ClassLoader loader, boolean fPaged)
        {
        validate(resolver);

        String sStoreName = getStoreName(resolver);
        String sPath      = getDirectory(resolver);
        File   fileDir    = sPath.length() == 0 ? null : new File(sPath);

        try
            {
            ParameterizedBuilder<BerkeleyDBBinaryStoreManager> bldrCustom = getCustomBuilder();

            if (bldrCustom == null)
                {
                // create the default BDb manager
                BerkeleyDBBinaryStoreManager bdbMgr = new BerkeleyDBBinaryStoreManager(fileDir, sStoreName);

                // load XML init params if they exist
                ResolvableParameterList listParams = getInitParams();
                if (listParams != null && !listParams.isEmpty())
                    {
                    XmlElement xmlInit = new SimpleElement("config");

                    for (Parameter param : listParams)
                        {
                        xmlInit.ensureElement(param.getName()).setString(param.evaluate(resolver).as(String.class));
                        }
                    bdbMgr.setConfig(xmlInit);
                    }

                return bdbMgr;
                }
            else
                {
                // create the custom object that is implementing BinaryStoreManager.
                // populate the relevant constructor arguments then create the cache
                ParameterList listArgs = new ResolvableParameterList();

                listArgs.add(new Parameter("fileDir", fileDir));
                listArgs.add(new Parameter("storeName", sStoreName));

                return bldrCustom.realize(resolver, loader, listArgs);
                }
            }
        catch (NoClassDefFoundError e)
            {
            String sMsg = "Berkeley DB JE libraries are required to utilize a "
                          + "'bdb-store-manager', visit www.sleepycat.com for additional information.";

            throw Base.ensureRuntimeException(e, sMsg);
            }
        }

    // ----- BdbStoreManagerBuilder methods ---------------------------------

    /**
     * Return the path name for the root directory that the BDB file manager
     * uses to store files in. If not specified or specifies a non-existent
     * directory, a temporary file in the default location is used.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the root directory
     */
    public String getDirectory(ParameterResolver resolver)
        {
        return m_exprDirectory.evaluate(resolver);
        }

    /**
     * Set the BDB root directory where BDB stores files.
     *
     * @param expr  the directory name
     */
    @Injectable
    public void setDirectory(Expression<String> expr)
        {
        m_exprDirectory = expr;
        }

    /**
     * Specifies the name for a database table that the Berkeley Database JE
     * store manager uses to store data in. Specifying this parameter causes
     * the bdb-store-manager to use non-temporary (persistent) database instances.
     * This is intended only for local caches that are backed by a cache loader
     * from a non-temporary store, so that the local cache can be pre-populated
     * from the disk on startup. This setting should not be enabled with replicated
     * or distributed caches. Normally, the store name should be left unspecified,
     * indicating that temporary storage is to be used.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the store name
     */
    public String getStoreName(ParameterResolver resolver)
        {
        return m_exprStoreName.evaluate(resolver);
        }

    /**
     * Set the BDB store (database table) name.
     *
     * @param expr  the store name
     */
    @Injectable
    public void setStoreName(Expression<String> expr)
        {
        m_exprStoreName = expr;
        }

    /**
     * Return the BDB init params needed to construct a BerkeleyDBBinaryStoreManager.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the init params
     */
    public String getXmlInitParams(ParameterResolver resolver)
        {
        return m_exprXmlInitParams.evaluate(resolver);
        }

    /**
     * Set the BDB init params needed to construct a BerkeleyDBBinaryStoreManager.
     *
     * @param expr  the XML init params
     *
     * @see com.sleepycat.je.EnvironmentConfig for je.* properties that can be configured.
     */
    @Injectable
    public void setXmlInitParams(Expression<String> expr)
        {
        m_exprXmlInitParams = expr;
        }

    /**
     * Return the BDB init params needed to construct a BerkeleyDBBinaryStoreManager.
     *
     * @return the init params
     */
    public ResolvableParameterList getInitParams()
        {
        return m_listResolvableInitParameters;
        }

    /**
     * Set the BDB init params needed to construct a BerkeleyDBBinaryStoreManager.
     *
     * @param listInitParams  list of resolvable init-params
     *
     * @see com.sleepycat.je.EnvironmentConfig for je.* properties that can be configured.
     */
    @Injectable("init-params")
    public void setInitParams(ResolvableParameterList listInitParams)
        {
        m_listResolvableInitParameters = listInitParams;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The directory.
     */
    private Expression<String> m_exprDirectory = new LiteralExpression<String>(String.valueOf(""));

    /**
     * The store name.
     */
    private Expression<String> m_exprStoreName = new LiteralExpression<String>(String.valueOf(""));

    /**
     * The init params in XML format.
     */
    private Expression<String> m_exprXmlInitParams = new LiteralExpression<String>(String.valueOf(""));

    /**
     * BDB parameters
     */
    private ResolvableParameterList m_listResolvableInitParameters;
    }
