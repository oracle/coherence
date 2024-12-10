/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import org.junit.Test;

import java.io.PrintWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2014.08.31
 */
public class DependenciesHelperTest
    {
    @Test
    public void shouldDefaultGarFileNameToNull() throws Exception
        {
        String[]               asArgs = {};
        QueryPlus.Dependencies deps   = QueryPlus.DependenciesHelper.newInstance(f_writer, System.in, null, asArgs);

        assertThat(deps.getGarFileName(), is(nullValue()));
        }

    @Test
    public void shouldSetGarFileName() throws Exception
        {
        String[]               asArgs = {"-g", "test.gar"};
        QueryPlus.Dependencies deps   = QueryPlus.DependenciesHelper.newInstance(f_writer, System.in, null, asArgs);

        assertThat(deps.getGarFileName(), is("test.gar"));
        }

    @Test
    public void shouldDefaultApplicationNameToNull() throws Exception
        {
        String[]               asArgs = {};
        QueryPlus.Dependencies deps   = QueryPlus.DependenciesHelper.newInstance(f_writer, System.in, null, asArgs);

        assertThat(deps.getApplicationName(), is(nullValue()));
        }

    @Test
    public void shouldSetApplicationName() throws Exception
        {
        String[]               asArgs = {"-g", "foo.gar", "-a", "testApp"};
        QueryPlus.Dependencies deps   = QueryPlus.DependenciesHelper.newInstance(f_writer, System.in, null, asArgs);

        assertThat(deps.getApplicationName(), is("testApp"));
        }

    @Test
    public void shouldDefaultTenantNamesToNull() throws Exception
        {
        String[]               asArgs = {};
        QueryPlus.Dependencies deps   = QueryPlus.DependenciesHelper.newInstance(f_writer, System.in, null, asArgs);

        assertThat(deps.getDomainPartitions(), is(nullValue()));
        }

    @Test
    public void shouldSetSingleTenantName() throws Exception
        {
        String[]               asArgs = {"-dp", "tenantA"};
        QueryPlus.Dependencies deps   = QueryPlus.DependenciesHelper.newInstance(f_writer, System.in, null, asArgs);

        assertThat(deps.getDomainPartitions(), is(new String[] {"tenantA"}));
        }

    @Test
    public void shouldSetMultipleTenantNames() throws Exception
        {
        String[]               asArgs = {"-dp", "tenantA,tenantB"};
        QueryPlus.Dependencies deps   = QueryPlus.DependenciesHelper.newInstance(f_writer, System.in, null, asArgs);

        assertThat(deps.getDomainPartitions(), is(new String[] {"tenantA", "tenantB"}));
        }

    protected final PrintWriter f_writer = new PrintWriter(System.out);
    }
