/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.disassembler;

import com.tangosol.dev.compiler.java.*;
import com.tangosol.util.*;
import java.io.*;

public abstract class Access
        extends Base
    {
    public static final int ACC_PUBLIC       = 0x0001;
    public static final int ACC_PRIVATE      = 0x0002;
    public static final int ACC_PROTECTED    = 0x0004;
    public static final int ACC_STATIC       = 0x0008;
    public static final int ACC_FINAL        = 0x0010;
    public static final int ACC_SYNCHRONIZED = 0x0020;
    public static final int ACC_VOLATILE     = 0x0040;
    public static final int ACC_TRANSIENT    = 0x0080;
    public static final int ACC_NATIVE       = 0x0100;
    public static final int ACC_INTERFACE    = 0x0200;
    public static final int ACC_ABSTRACT     = 0x0400;

    public static final int ACC_ALL    = ACC_PUBLIC       |
                                         ACC_PRIVATE      |
                                         ACC_PROTECTED    |
                                         ACC_STATIC       |
                                         ACC_FINAL        |
                                         ACC_SYNCHRONIZED |
                                         ACC_VOLATILE     |
                                         ACC_TRANSIENT    |
                                         ACC_NATIVE       |
                                         ACC_INTERFACE    |
                                         ACC_ABSTRACT;

    public static final int ACC_CLASS  = ACC_PUBLIC       |
                                         ACC_FINAL        |
                                         ACC_SYNCHRONIZED |
                                         ACC_ABSTRACT;

    public static final int ACC_IFACE  = ACC_PUBLIC       |
                                         ACC_FINAL;

    public static final int ACC_FIELD  = ACC_PUBLIC       |
                                         ACC_PRIVATE      |
                                         ACC_PROTECTED    |
                                         ACC_STATIC       |
                                         ACC_FINAL        |
                                         ACC_VOLATILE     |
                                         ACC_TRANSIENT;

    public static final int ACC_METHOD = ACC_PUBLIC       |
                                         ACC_PRIVATE      |
                                         ACC_PROTECTED    |
                                         ACC_STATIC       |
                                         ACC_FINAL        |
                                         ACC_SYNCHRONIZED |
                                         ACC_NATIVE       |
                                         ACC_ABSTRACT;

    private int m_nAccess;

    protected Access()
        {
        }

    protected Access(DataInput stream)
            throws IOException
        {
        m_nAccess = stream.readUnsignedShort();
        }

    protected void readAccess(DataInput stream)
            throws IOException
        {
        m_nAccess = stream.readUnsignedShort();
        }

    public boolean isPublic()
        {
        return ((m_nAccess & ACC_PUBLIC) != 0);
        }

    public boolean isPrivate()
        {
        return ((m_nAccess & ACC_PRIVATE) != 0);
        }

    public boolean isProtected()
        {
        return ((m_nAccess & ACC_PROTECTED) != 0);
        }

    public boolean isStatic()
        {
        return ((m_nAccess & ACC_STATIC) != 0);
        }

    public boolean isFinal()
        {
        return ((m_nAccess & ACC_FINAL) != 0);
        }

    public boolean isSynchronized()
        {
        return ((m_nAccess & ACC_SYNCHRONIZED) != 0);
        }

    public boolean isVolatile()
        {
        return ((m_nAccess & ACC_VOLATILE) != 0);
        }

    public boolean isTransient()
        {
        return ((m_nAccess & ACC_TRANSIENT) != 0);
        }

    public boolean isNative()
        {
        return ((m_nAccess & ACC_NATIVE) != 0);
        }

    public boolean isInterface()
        {
        return ((m_nAccess & ACC_INTERFACE) != 0);
        }

    public boolean isAbstract()
        {
        return ((m_nAccess & ACC_ABSTRACT) != 0);
        }

    public String toString()
        {
        return toString(ACC_ALL);
        }

    public String toString(int nMask)
        {
        StringBuffer sb = new StringBuffer();
        int nAccess = m_nAccess & nMask;
        if ((nAccess & ACC_PUBLIC      ) != 0) {sb.append(" public");      }
        if ((nAccess & ACC_PRIVATE     ) != 0) {sb.append(" private");     }
        if ((nAccess & ACC_PROTECTED   ) != 0) {sb.append(" protected");   }
        if ((nAccess & ACC_STATIC      ) != 0) {sb.append(" static");      }
        if ((nAccess & ACC_FINAL       ) != 0) {sb.append(" final");       }
        if ((nAccess & ACC_SYNCHRONIZED) != 0) {sb.append(" synchronized");}
        if ((nAccess & ACC_VOLATILE    ) != 0) {sb.append(" volatile");    }
        if ((nAccess & ACC_TRANSIENT   ) != 0) {sb.append(" transient");   }
        if ((nAccess & ACC_NATIVE      ) != 0) {sb.append(" native");      }
        if ((nAccess & ACC_INTERFACE   ) != 0) {sb.append(" interface");   }
        if ((nAccess & ACC_ABSTRACT    ) != 0) {sb.append(" abstract");    }
        return (sb.length() == 0 ? "" : sb.toString().substring(1));
        }
    }
