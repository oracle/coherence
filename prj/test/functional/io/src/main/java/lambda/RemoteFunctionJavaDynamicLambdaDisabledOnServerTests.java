/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package lambda;

/**
 * RemoteFunctionTests with Java serialization and misconfiguration of static lambdas enabled in server and
 * dynamic lambdas enabled in client.
 * test validates that server disallows dynamic lambda from client.
 *
 * @author jf  2020.06.02
 */
public class RemoteFunctionJavaDynamicLambdaDisabledOnServerTests
    extends AbstractRemoteFunctionTests
    {
    public RemoteFunctionJavaDynamicLambdaDisabledOnServerTests()
        {
        super(/*fPof*/false, /*fServerDisableDynamicLambdas*/true, /*fClientDisableDynamicLambdas*/false);
        }
    }
