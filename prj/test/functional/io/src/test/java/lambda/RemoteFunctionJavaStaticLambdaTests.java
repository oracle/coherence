/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package lambda;

/**
 * RemoteFunctionTests with static lambdas and test classpath added to server side to allow static lambdas to work
 *
 * @author jf  2020.06.02
 */
public class RemoteFunctionJavaStaticLambdaTests
    extends AbstractRemoteFunctionTests
    {
    public RemoteFunctionJavaStaticLambdaTests()
        {
        super(/*fPof*/false, /*fServerDisableDynamicLambdas*/true, /*fClientDisableDynamicLambdas*/true, /*fIncludeTestClassInServerClasspath*/ true);
        }
    }
