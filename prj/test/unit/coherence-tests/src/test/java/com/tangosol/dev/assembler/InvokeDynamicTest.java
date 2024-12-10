/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.assembler;

import org.junit.Test;

import java.io.IOException;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
* Tests the ability for the dis/assembler to correctly interpret bytecodes
* related to {@code invokedynamic}.
*
* @author hr  2012.08.06
*/
public class InvokeDynamicTest
    {

    /**
    * Test the ability to assemble a class file with a method's instruction
    * set including an {@code invokedynamic} instruction. Once assembled
    * ensure disassembly returns the expected outcomes.
    *
    * @throws IOException
    * @throws URISyntaxException
    */
    @Test
    public void testAssembleDisassemble()
            throws IOException, URISyntaxException
        {
        String    sPkgName         = "com/tangosol/dev";
        String    sClassSimpleName = "InvokeDynamicInstTest";
        String    sClassName       = sPkgName + "/" + sClassSimpleName;
        ClassFile cf               = new ClassFile(sClassName, "java/lang/Object", false);
        cf.setMajorVersion(51);
        cf.setMinorVersion(0);

        // public InvokeDynamicInstTest() { super(); }
        Method        cons = cf.addMethod("<init>()V");
        CodeAttribute code = cons.getCode();
        cons.setPublic();
        code.add(new Begin());
        Avar varThis = new Avar("this", "L" + sClassName + ";");
        code.add(varThis);
        code.add(new Aload(varThis));
        code.nextLine();
        code.add(new Invokespecial(new MethodConstant("java/lang/Object", "<init>", "()V")));
        code.nextLine();
        code.add(new Return());
        code.nextLine();
        code.add(new End());

        // public static void main(String[] asArgs) { ... }
        BootstrapMethodsAttribute bsMethods = (BootstrapMethodsAttribute) cf.addAttribute(Constants.ATTR_BOOTSTRAPMETHODS);
        bsMethods.addBootstrapMethod(new MethodHandleConstant(true, MethodHandleConstant.KIND_REF_INVOKESTATIC,
                new BootstrapMethodConstant("com/tangosol/dev/assembler/InvokeDynamicLinker", "invoke")));

        Method main = cf.addMethod("main([Ljava/lang/String;)V");
               code = main.getCode();

        main.setPublic();
        main.setStatic(true);
        code.add(new Begin());
        code.add(new Avar("this", "L" + sClassName + ";"));
        code.add(new Invokedynamic(new InvokeDynamicConstant(0, new SignatureConstant("runCalculation", "()V"))));
        code.add(new Return());
        code.add(new End());

        // assemble
        byte[] ab = cf.getBytes();
        cf = new ClassFile(ab);

        // ensure we can disassemble the class file
        main = cf.getMethod("main([Ljava/lang/String;)V");
        code = main.getCode();
        Invokedynamic opId = null;
        for (Op op = code.getFirstOp(); op != null; op = op.getNext())
            {
            if (op instanceof Invokedynamic)
                {
                opId = (Invokedynamic) op;
                }
            }
        assertNotNull(opId);
        assertTrue(opId.getConstant() instanceof InvokeDynamicConstant);

        InvokeDynamicConstant constId = (InvokeDynamicConstant) opId.getConstant();
        assertEquals(0, constId.getBootstrapMethodIndex());
        assertNotNull(constId.getMethodNameAndDescription());

        BootstrapMethodsAttribute bsAttr = (BootstrapMethodsAttribute) cf.getAttribute(Constants.ATTR_BOOTSTRAPMETHODS);
        assertNotNull(bsAttr);
        assertEquals(1, bsAttr.getBootstrapMethods().size());
        }
    }
