/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics.data;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.topic.Publisher;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A message that determines its own ordering.
 *
 * @param <V>  the type of wrapped value
 */
public class OrderableMessage<V>
        implements Publisher.Orderable, PortableObject, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public OrderableMessage()
        {
        }

    /**
     * Create an {@link OrderableMessage}.
     *
     * @param nOrder  the message order identifier
     * @param oValue  the message value
     */
    public OrderableMessage(int nOrder, V oValue)
        {
        m_nOrder = nOrder;
        m_oValue = oValue;
        }

    // ----- accessors ------------------------------------------------------

    public V getValue()
        {
        return m_oValue;
        }

    // ----- Orderable methods ----------------------------------------------

    @Override
    public int getOrderId()
        {
        return m_nOrder;
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nOrder = in.readInt();
        m_oValue = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(m_nOrder);
        ExternalizableHelper.writeObject(out, m_oValue);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nOrder = in.readInt(0);
        m_oValue = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nOrder);
        out.writeObject(1, m_oValue);
        }

    // ----- data members ---------------------------------------------------

    private int m_nOrder;

    private V m_oValue;
    }
