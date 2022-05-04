/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package lambda;

/**
 * RemoteFunctionTests using Java serialization, dynamic lambdas disabled and test classpath not added to server.
 * Static lambdas can not work for this configuration. Test validates these fail to work.
 *
 * @author jf  2020.06.02
 */
public class RemoteFunctionPofStaticLambdaMissingTestClassTests
    extends AbstractRemoteFunctionTests
    {
    public RemoteFunctionPofStaticLambdaMissingTestClassTests()
        {
        super(/*fPof*/true, /*fServerDisableDynamicLambdas*/true, /*fClientDisableDynamicLambdas*/true, /*fIncludeTestClassInServerClasspath*/ false);
        }
    }
