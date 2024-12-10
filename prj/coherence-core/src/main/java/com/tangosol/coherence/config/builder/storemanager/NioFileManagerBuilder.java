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

import com.tangosol.io.nio.MappedStoreManager;

import java.io.File;

/**
 * The NioFileManagerBuilder class builds an instance of a MappedStoreManager.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class NioFileManagerBuilder
        extends AbstractNioManagerBuilder<MappedStoreManager>
    {
    // ----- StoreManagerBuilder interface ----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public MappedStoreManager realize(ParameterResolver resolver, ClassLoader loader, boolean fPaged)
        {
        validate(resolver);

        MappedStoreManager                       manager       = null;

        int                                      cbMaxSize     = (int) getMaximumSize(resolver);
        int                                      cbInitialSize = (int) getInitialSize(resolver);
        String                                   sPath         = getDirectory(resolver);
        File                                     fileDir       = sPath.length() == 0 ? null : new File(sPath);

        ParameterizedBuilder<MappedStoreManager> bldrCustom    = getCustomBuilder();

        if (bldrCustom == null)
            {
            // create the NIO manager
            manager = new MappedStoreManager(cbInitialSize, cbMaxSize, fileDir);
            }
        else
            {
            // create the custom object that is implementing MappedStoreManager.
            ParameterList listArgs = new ResolvableParameterList();

            listArgs.add(new Parameter("initial-size", cbInitialSize));
            listArgs.add(new Parameter("max-size", cbMaxSize));
            listArgs.add(new Parameter("fileDir", fileDir));
            manager = bldrCustom.realize(resolver, loader, listArgs);
            }

        return manager;
        }

    // ----- NioFileManagerBuilder methods ----------------------------------

    /**
     * Return the path name for the root directory that the manager uses to
     * store files in. If not specified or specifies a non-existent directory,
     * a temporary file in the default location is used.
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
     * Set the root directory where the manager stores files.
     *
     * @param expr  the directory name
     */
    @Injectable
    public void setDirectory(Expression<String> expr)
        {
        m_exprDirectory = expr;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The directory.
     */
    private Expression<String> m_exprDirectory = new LiteralExpression<String>(String.valueOf(""));
    }
