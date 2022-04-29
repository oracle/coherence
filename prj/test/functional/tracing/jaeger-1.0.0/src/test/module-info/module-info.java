open module com.oracle.coherence.functional
    {
    requires com.oracle.coherence;
    requires com.oracle.coherence.testing;
    requires com.oracle.coherence.testing.tracing.jaeger;

    requires com.oracle.bedrock;
    requires com.oracle.bedrock.runtime.coherence;
    requires com.oracle.bedrock.runtime.coherence.testing;
    requires com.oracle.bedrock.testsupport;

    requires io.opentracing.util;
    requires io.opentracing.api;
    requires io.opentracing.contrib.tracerresolver;
    requires jaeger.core;

    requires transitive junit;

    requires org.hamcrest;
    requires org.mockito;

    requires java.net.http;
    requires java.management;
    }
