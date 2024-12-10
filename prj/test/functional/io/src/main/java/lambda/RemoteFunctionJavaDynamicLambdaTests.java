/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package lambda;

/**
 * RemoteFunctionTests to ensure dynamic lambda serialization functions with Java serialization.
 *
 * @author jf  2020.06.02
 */
public class RemoteFunctionJavaDynamicLambdaTests
    extends AbstractRemoteFunctionTests
    {
    public RemoteFunctionJavaDynamicLambdaTests()
        {
        super(/*fPof*/false, /*fServerDisableDynamicLambdas*/false, /*fClientDisableDynamicLambdas*/false);
        }
    }
