open module com.oracle.coherence.functional
    {
    requires com.oracle.coherence;
    requires com.oracle.coherence.grpc.proxy;

    requires io.grpc;
    requires io.helidon.grpc.server;
    requires io.helidon.microprofile.server;
    requires io.helidon.microprofile.grpc.server;

    requires com.oracle.bedrock.runtime;
    requires com.oracle.bedrock.testsupport;

    requires transitive org.junit.jupiter.api;
    requires transitive org.junit.platform.commons;

    requires org.hamcrest;
    requires org.mockito;

    requires java.net.http;
    requires java.management;
    }
