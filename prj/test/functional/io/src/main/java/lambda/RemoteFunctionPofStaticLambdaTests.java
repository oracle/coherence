/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package lambda;

/**
 * RemoteFunctionTests with POF serialization and dynamic lambdas disabled and test classpath added to server side to allow static lambdas to work
 *
 * @author jf  2020.06.02
 */
public class RemoteFunctionPofStaticLambdaTests
    extends AbstractRemoteFunctionTests
    {
    public RemoteFunctionPofStaticLambdaTests()
        {
        super(/*fPof*/true, /*fServerDisableDynamicLambdas*/true, /*fClientDisableDynamicLambdas*/true, /*fIncludeTestClassInServerClasspath*/ true);
        }
    }
