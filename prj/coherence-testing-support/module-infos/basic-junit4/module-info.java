open module com.oracle.coherence.functional
    {
    requires com.oracle.coherence;
    requires static com.oracle.coherence.testing;

    requires static com.oracle.bedrock.runtime.coherence;
    requires static com.oracle.bedrock.testsupport;

    requires junit;
    requires static org.junit.jupiter.api;
    requires static org.junit.platform.commons;

    requires org.hamcrest;
    requires org.mockito;

    requires java.net.http;
    requires java.management;
    }
