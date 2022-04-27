open module com.oracle.coherence.functional
    {
    requires com.oracle.coherence;
    requires static com.oracle.coherence.testing;

    requires com.oracle.bedrock.runtime.coherence;
    requires com.oracle.bedrock.testsupport;

    requires org.junit.jupiter.api;
    requires org.junit.platform.commons;

    requires org.hamcrest;
    requires org.mockito;

    requires java.net.http;
    requires java.management;
    }
