/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.net.management.model;

import com.tangosol.coherence.component.net.management.Connector;
import com.tangosol.coherence.component.net.management.gateway.Local;
import com.tangosol.coherence.component.net.management.model.localModel.WrapperModel;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for RemoteModel management invocation validation.
 *
 * @author as 2026.05.14
 * @since 26.04
 */
public class RemoteModelTest
    {
    @Before
    public void setUp()
        {
        m_model     = new TestLocalModel();
        m_connector = new TestConnector(m_model, m_info);
        }

    @Test
    public void shouldRejectUnexposedLocalModelMethodBeforeInvocation()
        {
        RemoteModel task = task(RemoteModel.OP_INVOKE, "hidden", new Object[] {"value"},
                new String[] {String.class.getName()});

        task.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.m_counter.get(), is(0));
        }

    @Test
    public void shouldPreserveExposedRemoteModelGetSetAndInvoke()
        {
        RemoteModel taskGet = task(RemoteModel.OP_GET, "getValue", null, null);
        taskGet.run();
        assertThat(m_model.get_InvocationResult(), is("initial"));

        RemoteModel taskSet = task(RemoteModel.OP_SET, "setValue", new Object[] {"updated"}, null);
        taskSet.run();
        assertThat(m_model.get_InvocationResult(), is((Object) null));
        assertThat(m_model.getValue(), is("updated"));

        RemoteModel taskSetExplicit = task(RemoteModel.OP_SET, "setValue", new Object[] {"explicit"},
                new String[] {String.class.getName()});
        taskSetExplicit.run();
        assertThat(m_model.get_InvocationResult(), is((Object) null));
        assertThat(m_model.getValue(), is("explicit"));

        RemoteModel taskInvoke = task(RemoteModel.OP_INVOKE, "echo", new Object[] {"ok"},
                null);
        taskInvoke.run();
        assertThat(m_model.get_InvocationResult(), is("ok"));

        RemoteModel taskInvokeExplicit = task(RemoteModel.OP_INVOKE, "echo", new Object[] {"signed"},
                new String[] {String.class.getName()});
        taskInvokeExplicit.run();
        assertThat(m_model.get_InvocationResult(), is("signed"));
        }

    @Test
    public void shouldRejectMismatchedRemoteModelSetSignature()
        {
        RemoteModel task = task(RemoteModel.OP_SET, "setValue", new Object[] {"updated"},
                new String[] {Integer.class.getName()});

        task.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.getValue(), is("initial"));
        }

    @Test
    public void shouldRejectNullSignatureInvokeWhenRuntimeArgumentDoesNotMatchDescriptor()
        {
        RemoteModel task = task(RemoteModel.OP_INVOKE, "echo", new Object[] {Integer.valueOf(7)}, null);

        task.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.m_counter.get(), is(0));
        }

    @Test
    public void shouldRejectSuppliedSignatureInvokeWhenRuntimeArgumentDoesNotMatchDescriptor()
        {
        RemoteModel task = task(RemoteModel.OP_INVOKE, "echo", new Object[] {Integer.valueOf(7)},
                new String[] {String.class.getName()});

        task.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.m_counter.get(), is(0));
        }

    @Test
    public void shouldRejectNullSignatureSetWhenRuntimeArgumentDoesNotMatchDescriptor()
        {
        RemoteModel task = task(RemoteModel.OP_SET, "setValue", new Object[] {Integer.valueOf(7)}, null);

        task.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.getValue(), is("initial"));
        assertThat(m_model.m_counter.get(), is(0));
        }

    @Test
    public void shouldRejectSuppliedSignatureSetWhenRuntimeArgumentDoesNotMatchDescriptor()
        {
        RemoteModel task = task(RemoteModel.OP_SET, "setValue", new Object[] {Integer.valueOf(7)},
                new String[] {String.class.getName()});

        task.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.getValue(), is("initial"));
        assertThat(m_model.m_counter.get(), is(0));
        }

    @Test
    public void shouldRejectNullRuntimeArgumentWhenHiddenOverloadCouldMatch()
        {
        RemoteModel task = task(RemoteModel.OP_INVOKE, "echo", new Object[] {null},
                new String[] {String.class.getName()});

        task.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.m_counter.get(), is(0));
        }

    @Test
    public void shouldRejectPrimitiveInvokeWhenWrapperOverloadWouldWin()
        {
        RemoteModel taskNullSignature = task(RemoteModel.OP_INVOKE, "primitiveEcho",
                new Object[] {Integer.valueOf(7)}, null);

        taskNullSignature.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.m_counter.get(), is(0));

        RemoteModel taskExplicitSignature = task(RemoteModel.OP_INVOKE, "primitiveEcho",
                new Object[] {Integer.valueOf(7)}, new String[] {Integer.TYPE.getName()});

        taskExplicitSignature.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.m_counter.get(), is(0));
        }

    @Test
    public void shouldRejectPrimitiveSetWhenWrapperOverloadWouldWin()
        {
        RemoteModel taskNullSignature = task(RemoteModel.OP_SET, "setPrimitiveValue",
                new Object[] {Integer.valueOf(7)}, null);

        taskNullSignature.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.getPrimitiveValue(), is(1));
        assertThat(m_model.m_counter.get(), is(0));

        RemoteModel taskExplicitSignature = task(RemoteModel.OP_SET, "setPrimitiveValue",
                new Object[] {Integer.valueOf(7)}, new String[] {Integer.TYPE.getName()});

        taskExplicitSignature.run();

        assertThat(m_model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(m_model.getPrimitiveValue(), is(1));
        assertThat(m_model.m_counter.get(), is(0));
        }

    @Test
    public void shouldPreservePrimitiveInvokeAndSetWhenOnlyPrimitiveMethodExists()
        {
        TestPrimitiveOnlyLocalModel model     = new TestPrimitiveOnlyLocalModel();
        TestConnector               connector = new TestConnector(model, m_info);

        RemoteModel taskInvoke = task(connector, RemoteModel.OP_INVOKE, "primitiveEcho",
                new Object[] {Integer.valueOf(7)}, null);
        taskInvoke.run();

        assertThat(model.get_InvocationResult(), is("primitive-7"));
        assertThat(model.m_counter.get(), is(1));

        RemoteModel taskSet = task(connector, RemoteModel.OP_SET, "setPrimitiveValue",
                new Object[] {Integer.valueOf(9)}, new String[] {Integer.TYPE.getName()});
        taskSet.run();

        assertThat(model.get_InvocationResult(), is((Object) null));
        assertThat(model.getPrimitiveValue(), is(9));
        assertThat(model.m_counter.get(), is(2));

        TestPrimitiveOnlyLocalModel wrapperModel     = new TestPrimitiveOnlyLocalModel();
        TestConnector               wrapperConnector = new TestConnector(wrapperModel, m_wrapperInfo);

        RemoteModel taskWrapperInvoke = task(wrapperConnector, RemoteModel.OP_INVOKE, "primitiveEcho",
                new Object[] {Integer.valueOf(11)}, new String[] {Integer.class.getName()});
        taskWrapperInvoke.run();

        assertThat(wrapperModel.get_InvocationResult(), is("primitive-11"));
        assertThat(wrapperModel.m_counter.get(), is(1));

        RemoteModel taskWrapperSet = task(wrapperConnector, RemoteModel.OP_SET, "setPrimitiveValue",
                new Object[] {Integer.valueOf(13)}, new String[] {Integer.class.getName()});
        taskWrapperSet.run();

        assertThat(wrapperModel.get_InvocationResult(), is((Object) null));
        assertThat(wrapperModel.getPrimitiveValue(), is(13));
        assertThat(wrapperModel.m_counter.get(), is(2));
        }

    @Test
    public void shouldRejectStandardWrapperModelWhenWrapperOverloadWouldWin()
        {
        TestWrappedBean bean  = new TestWrappedBean();
        WrapperModel    model = wrapperModel(bean);
        TestConnector   connector = new TestConnector(model, model.getMBeanInfo());

        RemoteModel task = task(connector, RemoteModel.OP_INVOKE, "compact",
                new Object[] {Boolean.TRUE}, null);
        task.run();

        assertThat(model.get_InvocationResult(), instanceOf(SecurityException.class));
        assertThat(bean.m_counter.get(), is(0));
        }

    @Test
    public void shouldPreserveStandardWrapperModelPrimitiveOperation()
        {
        TestPrimitiveOnlyWrappedBean bean  = new TestPrimitiveOnlyWrappedBean();
        WrapperModel                 model = wrapperModel(bean);
        TestConnector                connector = new TestConnector(model, model.getMBeanInfo());

        RemoteModel task = task(connector, RemoteModel.OP_INVOKE, "compact",
                new Object[] {Boolean.TRUE}, null);
        task.run();

        assertThat(model.get_InvocationResult(), is((Object) null));
        assertThat(bean.m_counter.get(), is(1));
        }

    @Test
    public void shouldPreserveRemoteModelRefreshRequest()
        {
        RemoteModel task = task(RemoteModel.OP_GET, null, null, null);

        task.run();

        assertThat(m_model.get_InvocationResult(), is((Object) null));
        assertThat(m_model.m_counter.get(), is(0));
        }

    @Test
    public void shouldValidateWithGeneratedMBeanInfoWhenLocalGatewayIsMissing()
        {
        TestStorageManagerModel model     = new TestStorageManagerModel();
        TestConnector           connector = new TestConnector(model, null);
        RemoteModel             task      = new RemoteModel();

        task.set_ModelName(MODEL_NAME);
        task.setConnector(connector);
        task.setInvokeOp(RemoteModel.OP_INVOKE);
        task.setInvokeName("resetStatistics");
        task.setInvokeParam(null);
        task.setInvokeSignature(null);

        task.run();

        assertThat(model.get_InvocationResult(), is((Object) null));
        assertThat(model.m_counter.get(), is(1));
        }

    private RemoteModel task(int nOp, String sMethod, Object[] aoParam, String[] asSignature)
        {
        return task(m_connector, nOp, sMethod, aoParam, asSignature);
        }

    private RemoteModel task(Connector connector, int nOp, String sMethod, Object[] aoParam, String[] asSignature)
        {
        RemoteModel task = new RemoteModel();
        task.set_ModelName(MODEL_NAME);
        task.setConnector(connector);
        task.setInvokeOp(nOp);
        task.setInvokeName(sMethod);
        task.setInvokeParam(aoParam);
        task.setInvokeSignature(asSignature);
        return task;
        }

    private WrapperModel wrapperModel(Object oBean)
        {
        WrapperModel model = new WrapperModel();
        model.set_ModelName(MODEL_NAME);
        model.setMBean(oBean);
        return model;
        }

    public static class TestLocalModel
            extends LocalModel
        {
        public TestLocalModel()
            {
            super("TestModel", null, false);
            set_Snapshot(true);
            set_ModelName(MODEL_NAME);
            }

        public String getValue()
            {
            return m_sValue;
            }

        public void setValue(String sValue)
            {
            m_sValue = sValue;
            }

        public String echo(String sValue)
            {
            return sValue;
            }

        public String echo(Integer nValue)
            {
            m_counter.incrementAndGet();
            return String.valueOf(nValue);
            }

        public String primitiveEcho(int nValue)
            {
            m_counter.incrementAndGet();
            return "primitive-" + nValue;
            }

        public String primitiveEcho(Integer nValue)
            {
            m_counter.incrementAndGet();
            return "wrapper-" + nValue;
            }

        public String hidden(String sValue)
            {
            m_counter.incrementAndGet();
            return sValue;
            }

        public void setValue(Integer nValue)
            {
            m_counter.incrementAndGet();
            m_sValue = String.valueOf(nValue);
            }

        public int getPrimitiveValue()
            {
            return m_nPrimitiveValue;
            }

        public void setPrimitiveValue(int nValue)
            {
            m_counter.incrementAndGet();
            m_nPrimitiveValue = nValue;
            }

        public void setPrimitiveValue(Integer nValue)
            {
            m_counter.incrementAndGet();
            m_nPrimitiveValue = nValue.intValue();
            }

        private String m_sValue = "initial";

        private int m_nPrimitiveValue = 1;

        private final AtomicInteger m_counter = new AtomicInteger();
        }

    public static class TestPrimitiveOnlyLocalModel
            extends LocalModel
        {
        public TestPrimitiveOnlyLocalModel()
            {
            super("TestPrimitiveOnlyModel", null, false);
            set_Snapshot(true);
            set_ModelName(MODEL_NAME);
            }

        public String primitiveEcho(int nValue)
            {
            m_counter.incrementAndGet();
            return "primitive-" + nValue;
            }

        public int getPrimitiveValue()
            {
            return m_nPrimitiveValue;
            }

        public void setPrimitiveValue(int nValue)
            {
            m_counter.incrementAndGet();
            m_nPrimitiveValue = nValue;
            }

        private int m_nPrimitiveValue = 1;

        private final AtomicInteger m_counter = new AtomicInteger();
        }

    public static class TestStorageManagerModel
            extends LocalModel
        {
        public TestStorageManagerModel()
            {
            super("StorageManagerModel", null, false);
            set_Snapshot(true);
            set_ModelName(MODEL_NAME);
            }

        @Override
        public String get_MBeanComponent()
            {
            return "Component.Manageable.ModelAdapter.StorageManagerMBean";
            }

        public void resetStatistics()
            {
            m_counter.incrementAndGet();
            }

        private final AtomicInteger m_counter = new AtomicInteger();
        }

    public interface TestWrappedBeanMBean
        {
        void compact(boolean fRegular);
        }

    public static class TestWrappedBean
            implements TestWrappedBeanMBean
        {
        @Override
        public void compact(boolean fRegular)
            {
            m_counter.incrementAndGet();
            }

        public void compact(Boolean fRegular)
            {
            m_counter.incrementAndGet();
            }

        private final AtomicInteger m_counter = new AtomicInteger();
        }

    public interface TestPrimitiveOnlyWrappedBeanMBean
        {
        void compact(boolean fRegular);
        }

    public static class TestPrimitiveOnlyWrappedBean
            implements TestPrimitiveOnlyWrappedBeanMBean
        {
        @Override
        public void compact(boolean fRegular)
            {
            m_counter.incrementAndGet();
            }

        private final AtomicInteger m_counter = new AtomicInteger();
        }

    public static class TestConnector
            extends Connector
        {
        public TestConnector(LocalModel model, MBeanInfo info)
            {
            m_gateway = info == null ? null : new TestLocal(info);
            m_mapRegistry.put(MODEL_NAME, model);
            }

        @Override
        public Local getLocalGateway()
            {
            return m_gateway;
            }

        @Override
        public Map getLocalRegistry()
            {
            return m_mapRegistry;
            }

        private final TestLocal m_gateway;

        private final Map<String, LocalModel> m_mapRegistry = new HashMap<>();
        }

    public static class TestLocal
            extends Local
        {
        public TestLocal(MBeanInfo info)
            {
            m_info = info;
            }

        @Override
        public MBeanInfo getMBeanInfo(String sName)
            {
            return m_info;
            }

        private final MBeanInfo m_info;
        }

    private static final String MODEL_NAME = "Coherence:type=RemoteModelTest";

    private static final MBeanInfo m_info = new MBeanInfo(
            TestLocalModel.class.getName(),
            "RemoteModel test MBean",
            new MBeanAttributeInfo[] {
                    new MBeanAttributeInfo("Value", String.class.getName(), "value", true, true, false),
                    new MBeanAttributeInfo("PrimitiveValue", Integer.TYPE.getName(), "primitive value",
                            true, true, false)
            },
            null,
            new MBeanOperationInfo[] {
                    new MBeanOperationInfo("echo", "echo",
                            new MBeanParameterInfo[] {
                                    new MBeanParameterInfo("value", String.class.getName(), "value")
                            },
                            String.class.getName(), MBeanOperationInfo.ACTION),
                    new MBeanOperationInfo("primitiveEcho", "primitiveEcho",
                            new MBeanParameterInfo[] {
                                    new MBeanParameterInfo("value", Integer.TYPE.getName(), "value")
                            },
                            String.class.getName(), MBeanOperationInfo.ACTION)
            },
            null);

    private static final MBeanInfo m_wrapperInfo = new MBeanInfo(
            TestLocalModel.class.getName(),
            "RemoteModel wrapper descriptor test MBean",
            new MBeanAttributeInfo[] {
                    new MBeanAttributeInfo("PrimitiveValue", Integer.class.getName(), "primitive value",
                            true, true, false)
            },
            null,
            new MBeanOperationInfo[] {
                    new MBeanOperationInfo("primitiveEcho", "primitiveEcho",
                            new MBeanParameterInfo[] {
                                    new MBeanParameterInfo("value", Integer.class.getName(), "value")
                            },
                            String.class.getName(), MBeanOperationInfo.ACTION)
            },
            null);

    private TestLocalModel m_model;

    private TestConnector m_connector;
    }
