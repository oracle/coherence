/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jk 2015.07.29
 */
public class AddInvocable<E>
        extends AbstractInvocable
        implements PortableObject, ExternalizableLite
    {
    public AddInvocable()
        {
        }

    public AddInvocable(String sName, boolean fIsQueue, List<E> listElements)
        {
        m_sName        = sName;
        m_fIsQueue     = fIsQueue;
        m_listElements = listElements;
        }

    @Override
    public void run()
        {
        ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory)
                CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(null);

        if (m_fIsQueue)
            {
            NamedTopic<E> queue    = eccf.ensureTopic(m_sName, ValueTypeAssertion.withoutTypeChecking());
            Publisher<E> publisher = queue.createPublisher();

            m_listElements.forEach(publisher::publish);
            }
        else
            {
            NamedTopic<E> queue    = eccf.ensureTopic(m_sName, ValueTypeAssertion.withoutTypeChecking());
            Publisher<E> publisher = queue.createPublisher();

            m_listElements.forEach(publisher::publish);
            }
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sName = in.readString(0);
        m_fIsQueue = in.readBoolean(1);
        m_listElements = in.readCollection(2, new ArrayList<>());
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sName);
        out.writeBoolean(1, m_fIsQueue);
        out.writeCollection(2, m_listElements);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sName = ExternalizableHelper.readSafeUTF(in);
        m_fIsQueue = in.readBoolean();
        m_listElements = new ArrayList<>();

        ExternalizableHelper.readCollection(in, m_listElements, null);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sName);
        out.writeBoolean(m_fIsQueue);
        ExternalizableHelper.writeCollection(out, m_listElements);
        }

    private String m_sName;

    private boolean m_fIsQueue;

    private List<E> m_listElements;
    }
