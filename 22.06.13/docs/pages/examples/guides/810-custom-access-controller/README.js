<doc-view>

<h2 id="_certificate_based_security">Certificate-Based Security</h2>
<div class="section">
<p>Coherence allows you to enable an access controller to help protect against unauthorized use of cluster resources.
The default access controller implementation is based on the key management infrastructure that is part of the JDK
and uses Java Authentication and Authorization Service (JAAS) for authentication.
The default implementation is documented in the
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/secure/using-access-controller.html#GUID-67150521-FD59-446F-9A00-A9647DFEE476">Using an Access Controller</a> section of the Coherence documentation.</p>

<p>Coherence Extend can also be configured to use the same certificate-based security mechanism to secure client access.
This requires custom implementations and configuration of the standard Extend <code>IdentityTransformer</code> and <code>IdentityAsserter</code> interfaces. Implementations of these classes are included in this example.</p>


<h3 id="_what_you_will_build">What You Will Build</h3>
<div class="section">
<p>The controller built in this example will use a different mechanism to verify the identity and trust of a caller.
Instead of requiring a key store containing all the possible caller&#8217;s public certificates, this controller will
use a trust store to verify that the caller&#8217;s certificate has been signed by a trusted CA.</p>

</div>

<h3 id="_what_you_need">What You Need</h3>
<div class="section">
<ul class="ulist">
<li>
<p>About 15 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 11</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://maven.apache.org/download.cgi">Maven 3.8+</a> or <a id="" title="" target="_blank" href="http://www.gradle.org/downloads">Gradle 4+</a>
Although the source comes with the Maven and Gradle wrappers included so they can be built without first installing
either build tool.</p>

</li>
<li>
<p>You can also import the code straight into your IDE:</p>
<ul class="ulist">
<li>
<p><router-link to="/examples/setup/intellij">IntelliJ IDEA</router-link></p>

</li>
</ul>
</li>
</ul>

<h4 id="_building_the_example_code">Building the Example Code</h4>
<div class="section">
<p>The source code for the guides and tutorials can be found in the
<a id="" title="" target="_blank" href="http://github.com/oracle/coherence/tree/master/prj/examples">Coherence CE GitHub repo</a></p>

<p>The example source code is structured as both a Maven and a Gradle project and can be easily built with either
of those build tools. The examples are stand-alone projects so each example can be built from the
specific project directory without needing to build the whole Coherence project.</p>

<ul class="ulist">
<li>
<p>Build with Maven</p>

</li>
</ul>
<p>Using the included Maven wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./mvnw clean package</markup>

<ul class="ulist">
<li>
<p>Build with Gradle</p>

</li>
</ul>
<p>Using the included Gradle wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./gradlew build</markup>

</div>
</div>
</div>

<h2 id="_custom_access_controller">Custom Access Controller</h2>
<div class="section">
<p>The default access controller has some very specific requirements and limitations regarding how it works:</p>

<ul class="ulist">
<li>
<p>The default controller requires a Java key store that contains all the public certificates for any cluster member
that may join the cluster. This key store is loaded once when the cluster member starts.
This is straightforward in an environment where all members share a common key store, but is less practical
in environments where keys and certs are provided for individual server hosts, or are provided dynamically at runtime.</p>

</li>
<li>
<p>The identity of a calling client is verified by finding its corresponding public certificate in the configured
key store using the principal name to look up the certificate. This is then used to decrypt a value that was encrypted
by the caller using its private key.</p>

</li>
<li>
<p>The default controller uses an XML file to specify the permissions to apply to different principals.
This file is loaded by the default controller when the cluster member starts and cannot be altered at runtime.</p>

</li>
</ul>
<p>For some use cases, these restrictions make the default controller impractical to use, so a custom controller is required.
The Coherence documentation does not go into details on how a custom controller has to be implemented, so that is what
this example will cover.</p>


<h3 id="_creating_a_custom_access_controller">Creating a Custom Access Controller</h3>
<div class="section">
<p>An access controller has to implement three methods from the <code>com.tangosol.net.security.AccessController</code> interface.</p>

<markup
lang="java"

>void checkPermission(ClusterPermission permission, Subject subject);

SignedObject encrypt(Object o, Subject subject)
        throws IOException, GeneralSecurityException;

Object decrypt(SignedObject so, Subject subjectRemote, Subject subjectThis)
        throws ClassNotFoundException, IOException, GeneralSecurityException;</markup>

<p>In this example, the required functionality has been separated out into different classes.
This makes it easier to use this example as a base for other custom controllers.</p>


<h4 id="_permission_checking">Permission Checking</h4>
<div class="section">
<p>The <code>checkPermission</code> method is required to verify that a specified <code>Principal</code> has a requested permission.
If not, the method throws an exception. In this example permission checks have been extracted into a
separate interface named <code>PermissionChecker</code> with a single <code>checkPermission</code> method.</p>

<p>The example contains an implementation of <code>PermissionsChecker</code> that uses the exact same XML permissions file
checking method as the Coherence default controller.</p>

<p>It would be simple to then extend this example to use other custom implementations of <code>PermissionChecker</code> that used
other checking methods, for example, LDAP lookups, security groups, certificate usages, etc.</p>

</div>

<h4 id="_encryption">Encryption</h4>
<div class="section">
<p>The access controller <code>encrypt</code> method is used by a calling member to encrypt a value.
Coherence calls this method and passes in a value to be encrypted. The resulting <code>SignedObject</code> is sent to
the remote Coherence cluster member and decrypted using the caller&#8217;s public key as part of the verification process.
In this way, the remote cluster member can assume that the caller was in possession of the subjects private and
public keys.</p>

<p>The <code>encrypt</code> method is quite straight forward. A private key is obtained for the specified <code>Subject</code>.
In this example, the private key comes from the subject&#8217;s private credentials loaded as part of the
JAAS login process. The private keys could easily be provided from another external security system.</p>

</div>

<h4 id="_decryption">Decryption</h4>
<div class="section">
<p>The access controller <code>decrypt</code> method is used by a Coherence cluster member as part of the process of verifying
a remote calling subject. The token encrypted by the caller using its private key will be decrypted by the
<code>decrypt</code> method using a public key. As part of this process, trust must also be verified for the caller&#8217;s subject.
The public key must be obtained for the calling subject. In the default access controller, the public key must be in the
key store the access controller is using, which also verifies trust. In this example the public key is sent by the
caller, so trust is verified using a different method.</p>

</div>

<h4 id="_the_encryptdecrypt_algorithm">The Encrypt/Decrypt Algorithm</h4>
<div class="section">
<p>The default access controller uses the <code>SHA1withDSA</code> encryption algorithm, which will therefore only work with
keys and certs that also use that algorithm. In this example, the keys and certs use the <code>SHA256withRSA</code> algorithm.
It is possible to change the default algorithm, but this is not well documented and requires an additional
XML configuration file. The custom controller created in this example uses a constructor parameter to allow the
algorithm to be overridden, which is much more flexible than the default controller.</p>

</div>

<h4 id="_verifying_trust">Verifying Trust</h4>
<div class="section">
<p>In the default access controller, trust is verified for a caller by only using public keys that are contained in the
key store loaded by the access controller. Even if the calling subject sent public credentials, these are ignored and
only public keys in the default controller&#8217;s key store are used for the decrypt process.
As already stated, this is quite restrictive and inflexible.</p>

<p>The custom access controller in this example verifies trust by ensuring that the caller&#8217;s public key has been signed
by a trusted CA certificate contained in a trust store. A trust store is just a Java key store that contains
one or more CA certificates.</p>

<p>This is much more flexible as it is easier to use a known set of acceptable CA certificates and then allow any
client using a properly signed certificate to connect. As long as that client has a principal with a name that
is authorized by the <code>PermissionCheck</code> then it will be able to access the cluster.</p>

</div>
</div>

<h3 id="_the_baseaccesscontroller_class">The BaseAccessController Class</h3>
<div class="section">
<p>The example contains an abstract <code>BaseAccessController</code> class which contains a lot of the boilerplate code
required to write an access controller. This then makes it simpler to extend this example to build other
custom access controllers. The default controller built class into Coherence is final, so it is not possible
to extend it and reuse any of its code. The <code>BaseAccessController</code> contains much of the code from the
default controller extracted out into a simple base class.</p>

<p>The <code>BaseAccessController</code> does not contain any of the permission checking code from the default controller.
As already mentioned, this code has been extracted out into the <code>PermissionChecker</code> interface.
The <code>BaseAccessController</code> requires an implementation of a <code>PermisssionChecker</code> passed as a constructor
parameter, which is then called by the <code>BaseAccessController.checkPermission</code> method.
This again makes it simple to plug other permission checking implementations into the example.</p>

<p>The <code>BaseAccessController</code> has two abstract methods that subclasses must implement:</p>

<markup
lang="java"

>protected abstract PrivateKey getPrivateKey(Subject subject);

protected abstract Set&lt;PublicKey&gt; verifyTrust(Subject subject) throws GeneralSecurityException;</markup>

<p>The <code>getPrivateKey</code> method uses some mechanism to supply a <code>PrivateKey</code> for a given <code>Subject</code>.</p>

<p>The <code>verifyTrust</code> method uses some mechanism to verify trust in a specified subject and return the set of
<code>PublicKey</code> instances to use for that <code>Subject</code>. If trust cannot be verified a  <code>GeneralSecurityException</code>
is thrown.</p>

</div>

<h3 id="_the_certaccesscontroller_class">The CertAccessController Class</h3>
<div class="section">
<p>The access controller implementation built in this example is the <code>CertAccessController</code> class.</p>


<h4 id="_private_keys">Private Keys</h4>
<div class="section">
<p>The <code>CertAccessController.getPrivateKey()</code> method just extracts and returns the first <code>PrivateKey</code> contained
in the subject&#8217;s set of private credentials.
In this example the <code>Subject</code> will have been created as part of the JAAS login process where the principal,
private and public keys are loaded from a key store using a custom JAAS <code>LoginModule</code>.</p>

<markup
lang="java"
title="CertAccessController.java"
>@Override
protected PrivateKey getPrivateKey(Subject subject)
    {
    Set&lt;Object&gt; setPrivateCreds = subject.getPrivateCredentials();
    if (setPrivateCreds == null)
        {
        return null;
        }

    for (Object oCred : setPrivateCreds)
        {
        PrivateKey keyPrivate = null;
        if (oCred instanceof PrivateKey)
            {
            keyPrivate = (PrivateKey) oCred;
            }
        else if (oCred instanceof X500PrivateCredential)
            {
            keyPrivate = ((X500PrivateCredential) oCred).getPrivateKey();
            }

        if (keyPrivate != null)
            {
            return keyPrivate;
            }
        }
    return null;
    }</markup>

<ul class="ulist">
<li>
<p>If the <code>Subject</code> passed to the method has no private credentials, then a <code>null</code> result is returned.</p>

</li>
<li>
<p>The <code>getPrivateKey</code> method iterates over the subjects' private credentials (which is a set of Object) looking for
an entry implementing either <code>PrivateKey</code> or an <code>X500PrivateCredential</code> containing a private key.
The first private key found is then returned.</p>

</li>
</ul>
<p>As long as the Subject produced by the JAAS login contains all the required parts (private key, public key and principal), it could have been loaded by any suitable login module.</p>

</div>

<h4 id="_trust_verification">Trust Verification</h4>
<div class="section">
<p>The <code>CertAccessController</code> uses a trust store to verify a public key was signed by a trusted CA certificate.
The trust store is loaded when the <code>CertAccessController</code> class is constructed.</p>

<p>The file name of the trust store, its type (JKS or PKCS12) and its password are provided as constructor parameters.
Typically, as a trust store only contains CA certificates, it is not really necessary to password protect them.
The trust store is just a normal Java key store file, and is loaded from the file system in the usual way.</p>

<markup
lang="java"
title="CertAccessController.java"
>private KeyStore loadKeyStore(File file, char[] pwd, String sType)
    {
    if (file == null)
        {
        throw new IllegalArgumentException("file cannot be null");
        }
    if (sType == null || sType.isEmpty())
        {
        sType = DefaultController.KEYSTORE_TYPE;
        }
    try
        {
        KeyStore store = KeyStore.getInstance(sType);
        try (InputStream in = new FileInputStream(file))
            {
            store.load(in, pwd == null ? new char[0] : pwd);
            }
        return store;
        }
    catch (Exception e)
        {
        throw Exceptions.ensureRuntimeException(e, "Failed to load keystore: " + file.getAbsolutePath());
        }
    }</markup>

<p>Once the trust store is loaded, it can be used to verify the public credentials in a Subject.
The process for this is:</p>

<ul class="ulist">
<li>
<p>Get the list of <code>CertPath</code> instances for the <code>Subject</code></p>

</li>
</ul>
<markup
lang="java"
title="CertAccessController.java"
>private List&lt;CertPath&gt; findCertPaths(Subject subject)
    {
    if (subject == null)
        {
        return List.of();
        }
    return subject.getPublicCredentials()
              .stream()
              .filter(CertPath.class::isInstance)
              .map(CertPath.class::cast)
              .collect(Collectors.toList());
    }</markup>

<ul class="ulist">
<li>
<p>Use Java&#8217;s built in <code>CertPathValidator</code> to validate the certificate paths using the trust store
In the code below, a <code>java.security.cert.CertPathValidator</code> is created to perform the trust validation.
The way this works using a <code>PKIXParameters</code> instance to hold the trust store is taken from Java&#8217;s documentation
on using a <code>CertPathValidator</code>.</p>

</li>
</ul>
<p>The <code>CertAccessController.verifyTrust()</code> method will iterate over the list of <code>CertPath</code> instances from the <code>Subject</code>.
A <code>Subject</code> could contain multiple <code>CertPath</code> instances, and as long as one of those passes validation the subject
is considered as trusted.</p>

<markup
lang="java"
title="CertAccessController.java"
>@Override
protected Set&lt;PublicKey&gt; verifyTrust(Subject subject) throws GeneralSecurityException
    {
    List&lt;CertPath&gt; list = findCertPaths(subject);
    if (list.isEmpty())
        {
        throw new GeneralSecurityException("No certificates found");
        }
    PKIXParameters parameters = new PKIXParameters(f_trustStore);
    parameters.setRevocationEnabled(false);

    CertPathValidator validator = CertPathValidator.getInstance("PKIX");
    Throwable error = null;
    for (CertPath certPath : list)
        {
        try
            {
            PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.validate(certPath, parameters);
            return Set.of(result.getPublicKey());
            }
        catch (Throwable t)
            {
            error = t;
            }
        }
    throw new GeneralSecurityException("Failed to verify subject " + subject.getPrincipals()
            .stream()
            .map(Principal::getName)
            .collect(Collectors.joining(",")), error);
    }</markup>

</div>
</div>

<h3 id="_jaas_login">JAAS Login</h3>
<div class="section">
<p>To use an access controller, the JVM must be configured to be able to perform a JAAS login to create a <code>Subject</code> containing
a <code>Principal</code>, a set of private credentials and a set of public credentials.
In this example, a Java keystore containing a signed public/private key will be used with a custom JAAS login module.</p>

<p>The example code contains a custom login module class <code>com.oracle.coherence.guides.security.KeystoreLogin</code>.
The <code>coherence-login.jar</code> module contains an almost identical class, but at the time of writing this example that class
contains a bug. The <code>KeystoreLogin</code> class in the <code>coherence-login</code> module uses the name of the certificate issuer as the principal name instead of the actual certificate name as the principal name. The <code>KeystoreLogin</code> class in this example fixes that issue.</p>

<p>To perform a JAAS login, a JAAS configuration file is required that specifies the configuration for the <code>Coherence</code> login module. For example, the test code contains the following file:</p>

<markup

title="src/test/resources/cert-login.config"
>Coherence {
    com.oracle.coherence.guides.security.KeystoreLogin required
      keyStorePath="${coherence.security.keystore}"
      keyStoreType="PKCS12"
};</markup>

<p>This files configures the login module named <code>Coherence</code> followed by its configuration inside curly brackets.
This is all standard JAAS configuration.
In this example the login module class to use is <code>com.oracle.coherence.guides.security.KeystoreLogin</code> and is marked as required.
This is followed by key/value pairs of configuration to be passed to the <code>KeystoreLogin</code> class.
The <code>KeystoreLogin</code> class requires the location of the key store and optionally the key store type.
In this example the key store location is passed in as a system property inside <code>${}</code>, so the location will
be read from the <code>coherence.security.keystore</code> system property at runtime.
This example uses PKCS12 key store types rather than the older JKS type.</p>


<h4 id="_callback_handlers">Callback Handlers</h4>
<div class="section">
<p>A JAAS login module, such as the example <code>KeystoreLogin</code> uses callback handlers to provide it with values
during the login process. Typically, these are values such as a username or passwords, but could be anything
required by the login module. A login module is supplied with a callback handler and calls this with different types
of <code>Callback</code> to request the information it required.</p>

<p>In the case of the example <code>KeystoreLogin</code> class it requires two values at runtime.</p>

<ul class="ulist">
<li>
<p>First is the name of the alias to use to retrieve the key and cert from the keystore, so it calls the callback handler with a <code>NameCallback</code> to retrieve the alias name.</p>

</li>
<li>
<p>The second is the password to use to read the key and cert from the keystore, so it calls the callback handler with a <code>PasswordCallback</code> to retrieve the password.</p>

</li>
</ul>
<p>As this example uses PKCS12 key stores, there is only a single password required, because PKCS12 requires any protected keys
inside the keystore to use the same password as the key store itself.</p>

<p>This example included a callback handler implementation in the  <code>src/test/java/com/oracle/coherence/guides/security/TestCallBackHandler.java</code> file.
This gets the password from a system property, which is ok for testing, but a more secure method may be required for production. The alias name is taken from another system property, or if that is not provided then from the Coherence member name.</p>

</div>

<h4 id="_performing_a_jaas_login">Performing a JAAS Login</h4>
<div class="section">
<p>When security is enabled and when Coherence needs to execute code that requires a Subject it will first check to see
whether the current thread has a <code>Subject</code> attached. If it does that <code>Subject</code> is used, if not a JAAS login is performed
to obtain a <code>Subject</code>. This means that Coherence will work securely regardless of how it was actually started.
For example, if Coherence is started using the <code>com.tangosol.net.Coherence</code> class there will be no <code>Subject</code> initially and
Coherence will perform a JAAS login each time one is required. This could mean that a login is performed a number of times
as different threads execute that require permissions. A workaround to this is to create a custom main class that
bootstraps Coherence inside a <code>Subject</code> context.</p>

<p>For example:</p>

<markup
lang="java"

>import com.taongosol.net.Coherence;
import com.tangosol.net.security.Security;

public class SecureCoherence
    {
    public static void main (String[] args)
        {
        Subject subject = Security.login(new TestCallBackHandler());
        Subject.doAs(subject, (PrivilegedAction&lt;Void&gt;) () -&gt;
            {
            Coherence.main(args);
            return null;
            });
        }
    }</markup>

<p>When a login is performed using the example, the <code>KeystoreLogin</code> will call the <code>TestCallBackHandler</code> to get the
alias name and key password. It then gets the key and certificate pair for that alias from the configured key store.
These are then used to create a <code>Subject</code>.</p>

<p>This is all standard JAAS functionality and nothing specific to Coherence is involved.</p>

</div>
</div>

<h3 id="_configuring_the_custom_access_controller">Configuring the Custom Access Controller</h3>
<div class="section">
<p>To make Coherence use the access controller, the <code>&lt;security&gt;</code> section of the operation configuration file
(also known as the override file) must be configured.
This configuration is documented in the
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/secure/using-access-controller.html#GUID-67150521-FD59-446F-9A00-A9647DFEE476">Using an Access Controller</a> section of the Coherence documentation.
The XML below is the configuration for the custom access controller used in this example.</p>

<markup
lang="xml"
title="cert-override.xml"
>&lt;coherence xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns="http://xmlns.oracle.com/coherence/coherence-operational-config"
           xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-operational-config coherence-operational-config.xsd"&gt;

    &lt;security-config&gt;
        &lt;enabled system-property="coherence.security"&gt;true&lt;/enabled&gt;
        &lt;access-controller&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.security.CertAccessController&lt;/class-name&gt;
            &lt;init-params&gt;
                &lt;init-param id="1"&gt;
                    &lt;param-type&gt;java.io.File&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.truststore"/&gt;
                &lt;/init-param&gt;
                &lt;init-param id="2"&gt;
                    &lt;param-type&gt;java.io.File&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.permissions"/&gt;
                &lt;/init-param&gt;
                &lt;init-param id="3"&gt;
                    &lt;param-type&gt;java.lang.Boolean&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.log"&gt;true&lt;/param-value&gt;
                &lt;/init-param&gt;
                &lt;init-param id="4"&gt;
                    &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.truststore.password"/&gt;
                &lt;/init-param&gt;
                &lt;init-param id="5"&gt;
                    &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.truststore.type"&gt;PKCS12&lt;/param-value&gt;
                &lt;/init-param&gt;
                &lt;init-param id="6"&gt;
                    &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.signature"&gt;SHA256withRSA&lt;/param-value&gt;
                &lt;/init-param&gt;
            &lt;/init-params&gt;
        &lt;/access-controller&gt;
        &lt;callback-handler&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.security.TestCallBackHandler&lt;/class-name&gt;
        &lt;/callback-handler&gt;
    &lt;/security-config&gt;
&lt;/coherence&gt;</markup>

<ul class="ulist">
<li>
<p>The <code>&lt;anabled&gt;</code> element is used to enable or disable Coherence security. In this case it is enabled by default
but could be disabled by setting the <code>coherence.security</code> system property to <code>false</code></p>

</li>
<li>
<p>The <code>&lt;access-controller&gt;</code> element contains the configuration for the custom access controller.</p>
<ul class="ulist">
<li>
<p>The <code>&lt;class-name&gt;</code> element tells Coherence to use the custom access controller class <code>com.oracle.coherence.guides.security.CertAccessController</code></p>

</li>
<li>
<p>The <code>&lt;init-params&gt;</code> element contains the <code>&lt;init-param&gt;</code> that will be converted to constructor parameters when Coherence creates an instance of <code>CertAccessController</code></p>
<ul class="ulist">
<li>
<p>The first parameter is the location of the trust store file</p>

</li>
<li>
<p>The second parameter is the location of the permissions XML file</p>

</li>
<li>
<p>The third parameter is a boolean flag to enable or disable logging of permission checks</p>

</li>
<li>
<p>The fourth parameter is the trust store password, this is optional and blank by default</p>

</li>
<li>
<p>The fifth parameter is the trust store type, in this example we use PKCS12 key stores</p>

</li>
<li>
<p>The sixth parameter is the encryption algorithm to use. This must match the type of keys used, and in this example they
keys are created using the <code>SHA256withRSA</code> algorithm.</p>

</li>
</ul>
</li>
</ul>
</li>
<li>
<p>The <code>&lt;callback-handler&gt;</code> element specifies the class name of the callback handler implementation to use
when Coherence performs a JAAS login.</p>

</li>
</ul>
</div>

<h3 id="_using_coherence_extend_security">Using Coherence Extend Security</h3>
<div class="section">
<p>Coherence Extend can be secured using the same certificate-based security mechanism and hook into the same permissions
mechanism used by the access controller. This is not really how the access controller was originally designed to be used,
it was primarily for securing access to cluster side resources.
Coherence Extend security is well documented in the
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/secure/securing-extend-client-connections.html">Securing Extend Client Connections</a>
section of the documentation.</p>

<p>This example will show how to create and configure an <code>IdentityTransformer</code> and <code>IdentityAsserter</code> implementation that will
use the same custom access controller created earlier to secure client access.</p>


<h4 id="_custom_identity_provider">Custom Identity Provider</h4>
<div class="section">
<p>The Extend client must use an <code>IdentityTransformer</code> to convert a <code>Subject</code> into a serializable token which is sent to the
Extend proxy server. This token it then converted back to a <code>Subject</code> by the server side <code>IdentityAsserter</code>.
The <code>IdentityTransformer</code> has a single method to implement:</p>

<markup
lang="java"

>Object transformIdentity(Subject subject, Service service) throws SecurityException</markup>

<ul class="ulist">
<li>
<p>The first parameter is the current client <code>Subject</code>, which could be <code>null</code> if security is not enabled or the calling thread is not running in the context of a <code>Subject</code>.</p>

</li>
<li>
<p>The second parameter is the client side remote service, i.e. a remote cache service, remote invocation service, etc.</p>

</li>
<li>
<p>The return value can be any Object that the corresponding <code>IdentiyAsserter</code> on the proxy will understand.
The token must be serializable by whichever serializer the remote service and proxy are using.</p>

</li>
</ul>
<p>We need to decide what to use as the token in this example.
By default, if we did nothing and configured no Extend security, Coherence will serialize the Subject as the token
and deserialize it on the proxy. The problem with this is that Coherence only serializes the set of principals contained in a <code>Subject</code>, so on the proxy the <code>Subject</code> would have the correct principal name, but no certificates to allow the proxy
to authenticate and authorize the client. This means anyone could create a <code>Subject</code> with a valid principal name and the proxy would allow it. This is certainly not what we require.</p>

<p>The token we send needs to do three things:</p>

<ul class="ulist">
<li>
<p>Identify the client principal name</p>

</li>
<li>
<p>Send the clients public key certificate to verify trust</p>

</li>
<li>
<p>Allow the proxy to verify that the client is also in possession of the client&#8217;s private key</p>

</li>
</ul>
<p>We could create a custom class that allows us to send these to the client, but Coherence already has a class
that we can re-use for this purpose, the <code>com.tangosol.net.security.PermissionInfo</code> class.
This class is actually used by cluster members for a similar task to transfer the same information between cluster members.
As the <code>com.tangosol.net.security</code> package is part of the Coherence public API we can safely use it.</p>

<p>The <code>PermissionInfo</code> class contains a <code>ClusterPermission</code>, the name of the remote service, a <code>SignedObject</code> and a <code>Subject</code>.
The <code>SignedObject</code> is actually the <code>ClusterPermission</code> encrypted using the subjects private key.
When the <code>PermissionInfo</code> is serialized by the client, the subject&#8217;s principal and any public credentials are also
serialized, which is exactly what we want. The subjects' private credentials are never serialized or sent to the server.</p>

<p>We can now use this to write a custom <code>IdentityTransformer</code> as shown below:</p>

<markup
lang="java"
title="CertIdentityTransformer"
>package com.oracle.coherence.guides.security;

import com.tangosol.coherence.component.net.Security;

import com.tangosol.coherence.component.net.security.Standard;

import com.tangosol.net.ClusterPermission;
import com.tangosol.net.Service;

import com.tangosol.net.security.AccessController;
import com.tangosol.net.security.IdentityTransformer;
import com.tangosol.net.security.PermissionInfo;

import javax.security.auth.Subject;
import java.security.SignedObject;

public class CertIdentityTransformer
        implements IdentityTransformer
    {
    @Override
    public Object transformIdentity(Subject subject, Service service) throws SecurityException
        {
        try
            {
            Object oToken = null;
            if (Security.isSecurityEnabled())   <span class="conum" data-value="1" />
                {
                Standard          security     = (Standard) Security.getInstance();
                AccessController  controller   = security.getDependencies().getAccessController();  <span class="conum" data-value="2" />
                String            sServiceName = service.getInfo().getServiceName();
                String            sTarget      = "service=Proxy";// + sServiceName;
                ClusterPermission permission   = new ClusterPermission(null, sTarget, "join");  <span class="conum" data-value="3" />
                SignedObject      signedObject = controller.encrypt(permission, subject);       <span class="conum" data-value="4" />
                oToken = new PermissionInfo(permission, sServiceName, signedObject, subject);   <span class="conum" data-value="5" />
                }
            return oToken;
            }
        catch (Exception e)
            {
            throw new SecurityException("Failed to create identity token", e);
            }
        }
    }</markup>

<ol style="margin-left: 15px;">
<li>
If security is not enabled the token returned will be <code>null</code>

</li>
<li>
The identity asserter obtains the current <code>Security</code> instance, which will be an instance of
<code>com.tangosol.coherence.component.net.security.Standard</code>. This can then be used to get the current access controller.

</li>
<li>
A <code>ClusterPermission</code> is created, the contents of this are not particularly important as all it is really used for in this case is as a token to be encrypted to prove to the proxy that the client has the private key. Although the format of the service name is important and must be <code>service=&lt;name&gt;</code>.

</li>
<li>
The <code>ClusterPermission</code> is encrypted into a <code>SignedObject</code> using the access controller encrypt method.

</li>
<li>
A <code>PermissionInfo</code> instance is created and returned as the token, which Coherence will serialize and send to the proxy as part of the connection request.

</li>
</ol>
</div>

<h4 id="_custom_identity_asserter">Custom Identity Asserter</h4>
<div class="section">
<p>Now the Extend client has a custom <code>IdentityTransformer</code> the corresponding server side <code>IdentityAsserter</code> can be created.
An <code>IdentityAsserter</code> has a single method to implement:</p>

<markup
lang="java"

>Subject assertIdentity(Object oToken, Service service) throws SecurityException</markup>

<ul class="ulist">
<li>
<p>The first parameter for the <code>assertIdentity</code> method is the deserialized token from the client, in this case we expect it to be a <code>PermissionInfo</code> instance.</p>

</li>
<li>
<p>The second parameter is a reference to the proxy service that the client is connecting to.</p>

</li>
</ul>
<p>The <code>IdentityAsserter</code> uses the token to produce a <code>Subject</code> and assert that the subject is allowed to connect to the server, throwing a <code>SecurityException</code> if these checks fail.</p>

<p>The <code>PermissionInfo</code> instance passed to our custom <code>IdentityAsserter</code> will contain all the values from the client,
the <code>ClusterPermission</code>, the service name, the <code>SignedObject</code> encrypted permission, and the client <code>Subject</code> containing
the principal and any public credentials (i.e. the public key and certificate).</p>

<p>This can be used to write the custom <code>IdentityAsserter</code> as shown below:</p>

<markup
lang="java"
title="CertIdentityAsserter"
>package com.oracle.coherence.guides.security;

import com.tangosol.coherence.component.net.Security;

import com.tangosol.coherence.component.net.security.Standard;

import com.tangosol.net.ClusterPermission;
import com.tangosol.net.Service;

import com.tangosol.net.security.AccessController;
import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.PermissionInfo;

import javax.security.auth.Subject;

import java.security.SignedObject;

import java.util.Set;

public class CertIdentityAsserter
        implements IdentityAsserter
    {
    @Override
    public Subject assertIdentity(Object oToken, Service service) throws SecurityException
        {
        if (!Security.isSecurityEnabled())    <span class="conum" data-value="1" />
            {
            return null;
            }

        if (!(oToken instanceof PermissionInfo))  <span class="conum" data-value="2" />
            {
            throw new SecurityException("Unauthorized");
            }

        PermissionInfo info         = (PermissionInfo) oToken;     <span class="conum" data-value="3" />
        Subject        subject      = info.getSubject();
        SignedObject   signedObject = info.getSignedPermission();

        if (subject == null)   <span class="conum" data-value="4" />
            {
            throw new SecurityException("Unauthorized");
            }

        if (signedObject == null)  <span class="conum" data-value="5" />
            {
            throw new SecurityException("Unauthorized");
            }

        try
            {
            Standard          security     = (Standard) Security.getInstance();                   <span class="conum" data-value="6" />
            AccessController  controller   = security.getDependencies().getAccessController();

            if (!(controller.decrypt(signedObject, subject, null) instanceof ClusterPermission))   <span class="conum" data-value="7" />
                {
                throw new SecurityException("Unauthorized");
                }

            String sClusterName = service.getCluster().getClusterName();
            String sServiceName = service.getInfo().getServiceName();
            String sTarget      = "service=" + sServiceName;
            controller.checkPermission(new ClusterPermission(sClusterName, sTarget, "join"), subject);  <span class="conum" data-value="8" />

            return new Subject(false, subject.getPrincipals(), Set.of(), Set.of()); <span class="conum" data-value="9" />
            }
        catch (Exception e)
            {
            throw new SecurityException("Failed to verify identity token");
            }
        }
    }</markup>

<ol style="margin-left: 15px;">
<li>
If Coherence security is not enabled there is nothing to do so return a <code>null</code> subject

</li>
<li>
If the received token is not a <code>PermissionInfo</code> throw a <code>SecurityException</code> to refuse the connection

</li>
<li>
Cast the token to a <code>PermissionInfo</code> and get the <code>Subject</code> and <code>SignedObject</code> from it

</li>
<li>
If the <code>Subject</code> is null, refuse the connection

</li>
<li>
If the <code>SignedObject</code> is null, refuse the connection

</li>
<li>
Obtain the current Coherence <code>Security</code> instance, from which the curren access controller can be obtained.

</li>
<li>
Use the access controller to decrypt the <code>SignedObject</code> and assert that it is a <code>ClusterPermission</code> instance

</li>
<li>
Create a new <code>ClusterPermission</code> to representing a permission request to join the Proxy service and call the
access controller <code>checkPermission</code> method to ensure the <code>Subject</code> has permissions to connect.

</li>
<li>
Return a <code>Subject</code> representing the client. For Extend the <code>Subject</code> should only contain the principals, any public credentials are not required and should not be included.

</li>
</ol>
<p>A few of the steps above need to be covered in more details to explain exactly where the client is authenticated
and authorized.</p>

<ul class="ulist">
<li>
<p>In step 7, the <code>SignedObject</code> from the client is decrypted. This must produce an instance of a <code>ClusterPermission</code>
as that is what our corresponding <code>IdentityTransformer would have encrypted. But more is also going on here as it
it our custom access controller `CertAccessController</code> that also verifies trust of the client&#8217;s public key and cert
during decryption. So as long as <code>decrypt</code> returns a <code>ClusterPermission</code> we can be sure that the client has the
private key corresponding to the public key we received and that the <code>CertAccessController</code> has verified trust
for that public key. If the public key was not trusted, the decrypt method would have thrown a security exception.</p>

</li>
<li>
<p>In step 8, the access controller is used to check the client has permission to connect.
In the example, this is just using the same XML file permissions mechanism.
So for each client that has permission to connect, there must be an entry in the XML file with the client principal
granting it join permission to the proxy.</p>

</li>
</ul>
<p>For example, if the proxy service is named Proxy, and the client principal name is <code>CN=client-one</code> then at a minimum
the permissions file must grant that principal "join" permissions for the Proxy service:</p>

<markup
lang="xml"

>   &lt;grant&gt;
      &lt;principal&gt;
         &lt;class&gt;javax.security.auth.x500.X500Principal&lt;/class&gt;
         &lt;name&gt;CN=client-one&lt;/name&gt;
      &lt;/principal&gt;
      &lt;permission&gt;
         &lt;target&gt;service=Proxy&lt;/target&gt;
         &lt;action&gt;join&lt;/action&gt;
      &lt;/permission&gt;
   &lt;/grant&gt;</markup>

</div>

<h4 id="_configuring_extend_security">Configuring Extend Security</h4>
<div class="section">
<p>Coherence Extend must be configured to use the custom <code>IdentiyTransformer</code> and <code>IdentityAsserter</code> created above.
This is done in the same <code>&lt;security&gt;</code> section of the operational configuration file that the access controller was configured in.</p>

<markup
lang="xml"
title="cert-override.xml"
>&lt;coherence xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns="http://xmlns.oracle.com/coherence/coherence-operational-config"
           xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-operational-config coherence-operational-config.xsd"&gt;

    &lt;security-config&gt;
        &lt;enabled system-property="coherence.security"&gt;true&lt;/enabled&gt;
        &lt;access-controller&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.security.CertAccessController&lt;/class-name&gt;
            &lt;init-params&gt;
                &lt;init-param id="1"&gt;
                    &lt;param-type&gt;java.io.File&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.truststore"/&gt;
                &lt;/init-param&gt;
                &lt;init-param id="2"&gt;
                    &lt;param-type&gt;java.io.File&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.permissions"/&gt;
                &lt;/init-param&gt;
                &lt;init-param id="3"&gt;
                    &lt;param-type&gt;java.lang.Boolean&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.log"&gt;true&lt;/param-value&gt;
                &lt;/init-param&gt;
                &lt;init-param id="4"&gt;
                    &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.truststore.password"/&gt;
                &lt;/init-param&gt;
                &lt;init-param id="5"&gt;
                    &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.truststore.type"&gt;PKCS12&lt;/param-value&gt;
                &lt;/init-param&gt;
                &lt;init-param id="6"&gt;
                    &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                    &lt;param-value system-property="coherence.security.signature"&gt;SHA256withRSA&lt;/param-value&gt;
                &lt;/init-param&gt;
            &lt;/init-params&gt;
        &lt;/access-controller&gt;
        &lt;callback-handler&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.security.TestCallBackHandler&lt;/class-name&gt;
        &lt;/callback-handler&gt;
        &lt;identity-asserter&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.security.CertIdentityAsserter&lt;/class-name&gt;
        &lt;/identity-asserter&gt;
        &lt;identity-transformer&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.security.CertIdentityTransformer&lt;/class-name&gt;
        &lt;/identity-transformer&gt;
    &lt;/security-config&gt;
&lt;/coherence&gt;</markup>

<ul class="ulist">
<li>
<p>The <code>IdentityAsserter</code> is configured just after the callback handler in the XML.
The custom <code>CertIdentityAsserter</code> class name is specified inside the <code>&lt;identity-asserter&gt;</code> element.</p>

</li>
<li>
<p>The <code>IdentityTransformer</code> is then specified in the same way putting the <code>CertIdentityTransformer</code> class
name in the <code>&lt;identity-transformer&gt;</code> element.</p>

</li>
</ul>
<p>Both the Coherence Extend client and the Proxy server must be configured to use this operational configuration.</p>

</div>
</div>

<h3 id="_running_the_example">Running the Example</h3>
<div class="section">
<p>Now that all the parts have been created, it is possible to test certificate-based security by running Coherence
and trying various test scenarios.</p>


<h4 id="_create_keys_and_certificates">Create Keys and Certificates</h4>
<div class="section">
<p>As this example is all about certificate-based security, some keys and certificates will be required for testing.
In a lot of examples with keys and certs, simple self-signed certs are used.
This is not going to work for this example because the custom access controller verifies trust be ensuring
the caller&#8217;s public key was signed by a trusted CA. This means the test certificates must also be signed by a CA.
We can still do this without requiring a real CA, it just requires the use of OpenSSL a few extra commands.
OpenSSL is well documented with a number of examples of how to do this available.
This example includes a shell script file <code>keys.sh</code> which will generate the files required to run the example.</p>

<p>From the root director of the project run</p>

<markup
lang="bash"

>sh keys.sh</markup>

<p>This will generate a number of keys, certificates and key stores in the <code>certs/</code> directory.</p>

</div>

<h4 id="_test_scenarios">Test Scenarios</h4>
<div class="section">
<p>To properly test the custom access controller and Extend identity transformer and asserter, there
are a number of scenarios to run.</p>

<p>The custom access controller will be configured to only trust certificates signed by a specific trusted CA ("/CN=test-ca1")
that will be in the trust store.</p>

<p>Cluster Member Scenarios:</p>

<ol style="margin-left: 15px;">
<li>
Start the first cluster member using a principal name ("/CN=member-1") that is in the permissions XML file grant list and a certificate signed by the trusted CA ("/CN=test-ca1") this should start successfully.

</li>
<li>
Start a cluster member using a different principal name ("/CN=member-2") that is in the permissions XML file grant list for itself and the senior cluster member. This member uses a certificate signed by the trusted CA. This new member should start successfully and form a cluster with the senior member.

</li>
<li>
Start a cluster member using a principal name ("/CN=member-4"), signed by the trusted CA but is in the permissions XML file deny list for both the new member and the existing senior member. This member should fail to join the cluster.

</li>
<li>
Start a cluster member using a principal name ("/CN=member-5"), signed by the trusted CA but is in not in the permissions XML file for the new member nor the senior member. This member should fail to join the cluster.

</li>
<li>
Start a cluster member using a principal name ("/CN=member-4"), signed by the trusted CA which is in the new members permissions XML file grant list but in the senior member&#8217;s "deny" list. This new member should fail to join the cluster.

</li>
<li>
Start a cluster member using a principal name ("/CN=member-5"), signed by the trusted CA which is in the new members permissions XML file grant list but not in the senior member&#8217;s permissions XML file. This new member should fail to join the cluster.

</li>
<li>
Start a cluster member using a principal name ("/CN=member-1") that is in both its own and the senior member&#8217;s permissions file grant list, but has a certificate signed by the untrusted CA. This member should fail to join the cluster as it is untrusted.

</li>
</ol>
<p>Extend Client Scenarios:</p>

<p>Start two storage enabled cluster members running Extend proxies. These will run with two different principals
("/CN=member-1" and "/CN=member-2") both signed by the trusted CA. These should start and form a cluster, as verified by the cluster member scenarios.</p>

<ol style="margin-left: 15px;">
<li>
Start an Extend client using a different principal name ("/CN=member-3"), using a certificate signed by the trusted CA. The client principal is in both the client&#8217;s and cluster member&#8217;s permissions XML file grant list. The client should start successfully and connect to the proxy.

</li>
<li>
Start an Extend client using a principal name ("/CN=member-4"), using a certificate signed by the trusted CA. The client principal is in both the client and cluster permissions XML file deny list. This client should fail to connect to the proxy.

</li>
<li>
Start an Extend client using a principal name ("/CN=member-5"), using a certificate signed by the trusted CA. The client principal is in neither the client nor cluster permissions XML file. This client should fail to connect to the proxy.

</li>
<li>
Start an Extend client using a principal name ("/CN=member-4"), signed by the trusted CA which is in the client&#8217;s permissions XML file grant list, but in the cluster deny list. This client should fail to connect to the proxy.

</li>
<li>
Start an Extend client using a principal name ("/CN=member-5"), signed by the trusted CA which is in the client&#8217;s permissions XML file grant list, but not in the cluster permissions XML file. This client should fail to connect to the proxy.

</li>
<li>
Start an Extend client using a principal name ("/CN=member-1")using a certificate signed by the trusted CA. The client principal is in both the client&#8217;s and cluster permissions XML file. This client should fail to connect to the proxy as it is untrusted.

</li>
</ol>
</div>

<h4 id="_before_running_the_tests">Before Running the Tests</h4>
<div class="section">
<p>Before running the tests, build the project using the following command:</p>

<markup
lang="bash"

>mvn clean package -DskipTests</markup>

<p>This will build the project jar file under tha <code>target/</code> directory and put the project dependencies
(<code>coherence.jar</code>) into a the <code>target/libs</code> directory</p>

<p>Run the script to generate the keys, certificates and key stores</p>

<markup
lang="bash"

>sh keys.sh</markup>

</div>

<h4 id="_start_a_cluster_member">Start A Cluster Member</h4>
<div class="section">
<p>The first cluster member in the tests will be configured to start correctly.
The example contains a shell script named <code>server.sh</code> that will start a Coherence cluster member.
The script takes three parameters:</p>

<ul class="ulist">
<li>
<p>the name of the principal that the server will use</p>

</li>
<li>
<p>the name of the PKCS12 key store</p>

</li>
<li>
<p>a boolean to set whether the cluster member is storage enabled</p>

</li>
</ul>
<p>In a terminal window, run the following command:</p>

<markup
lang="bash"

>./server.sh member-1 member-1.p12 true</markup>

<p>This will start a storage enable cluster member using the principal <code>CN=member-1</code> loaded from the key store
<code>certs/member-1.p12</code></p>

<p>In the log displayed in the terminal, the custom access controller will log various permission requests that will
show the principal name, for example:</p>

<markup


>2025-04-22 16:12:40.332/4.118 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=Coherence, member=1): Allowed request for ("com.tangosol.net.ClusterPermission" "service=Management" "join") on behalf of CN=member-1</markup>

</div>

<h4 id="_start_a_second_cluster_member">Start A Second Cluster Member</h4>
<div class="section">
<p>Now the first cluster member is running we can start a second member that should join the cluster.
This member will run as principal "/CN=member-2" to test scenario 2 above, where a new member in the
grant list can join the cluster.</p>

<p>In another terminal window, run the following command from the root directory for the project:</p>

<markup
lang="bash"

>./server.sh member-2 member-2.p12 true</markup>

<p>This time in the console log we should see the access controller allowing permissions for principal "CN=member-2"</p>

<markup


>2025-04-22 16:21:44.105/1.333 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=Coherence, member=2): Allowed request for ("com.tangosol.net.ClusterPermission" "service=Management" "join") on behalf of CN=member-2</markup>

<p>If we look at the log for the senior cluster member that the new member joined with, we should also see the access controller
in the senior member allowing the new member to join with the principal "CN=member-2"</p>

<markup


>2025-04-22 16:21:44.638/548.426 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=PagedTopic:PartitionedTopic, member=1): Allowed request for ("com.tangosol.net.ClusterPermission" "service=PartitionedTopic,cache=*" "join") on behalf of CN=member-2</markup>

</div>

<h4 id="_cluster_member_with_denied_principal">Cluster Member With Denied Principal</h4>
<div class="section">
<p>To test scenario three, we can run a cluster member using principal "member-4" which is in the deny list of the permissions.xml file.
This member should fail to start, the log should show a PermissionException.
This member will not even have contacted the senior member because its own permissions XML file causes it to fail to start.</p>

<markup
lang="bash"

>./server.sh member-4 member-4.p12 false</markup>

<p>This member should fail to start, the log should show a PermissionException.</p>

<markup


>2025-04-22 16:34:48.594/1.361 Oracle Coherence GE 14.1.1.2206.13 &lt;Error&gt; (thread=Coherence, member=2): com.tangosol.net.security.PermissionException: Insufficient rights to perform the operation</markup>

</div>

<h4 id="_cluster_member_with_missing_principal">Cluster Member With Missing Principal</h4>
<div class="section">
<p>To test scenario four, we can run a cluster member using principal "member-5" which is missing the permissions.xml file.
This member will not even have contacted the senior member because its own permissions XML file causes it to fail to start.</p>

<markup
lang="bash"

>./server.sh member-5 member-5.p12 false</markup>

<p>This member should fail to start, the log should show a PermissionException.</p>

<markup


>2025-04-22 16:36:45.762/1.337 Oracle Coherence GE 14.1.1.2206.13 &lt;Error&gt; (thread=main, member=3): java.util.concurrent.CompletionException: com.tangosol.net.security.PermissionException: Insufficient rights to perform the operation</markup>

</div>

<h4 id="_cluster_member_allowed_in_own_permissions_file_denied_by_the_senior_member">Cluster Member Allowed In Own Permissions File Denied By The Senior Member</h4>
<div class="section">
<p>Test scenario five is where we run a cluster member where the principal name is in the grant list of its own
permissions XML file, but is in the deny list of the senior running cluster member.</p>

<p>We can run the <code>server.sh</code> script with a fourth parameter to specify the name of the permissions XML file to use.
For this scenario we will use the <code>cert-permissions-all.xml</code> file which grants access to all the test principals.</p>

<markup
lang="bash"

>./server.sh member-4 member-4.p12 false cert-permissions-all.xml</markup>

<p>We can see in the console logs that this member allows permissions for principal "CN=member-4"</p>

<markup


>2025-04-22 16:44:36.938/1.874 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=Coherence, member=5): Allowed request for ("com.tangosol.net.ClusterPermission" "service=$SYS:Config,cache=*" "join") on behalf of CN=member-4</markup>

<p>But the member fails to join the cluster because it will be rejected by the senior member.
In the senior member log, there will be messages similar to this:</p>

<markup


>2025-04-22 16:44:36.956/616.308 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=DistributedCache:$SYS:Config, member=1): Denied request for ("com.tangosol.net.ClusterPermission" "service=$SYS:Config,cache=*" "join") on behalf of CN=member-4</markup>

</div>

<h4 id="_cluster_member_allowed_in_own_permissions_file_missing_from_the_senior_member">Cluster Member Allowed In Own Permissions File Missing From The Senior Member</h4>
<div class="section">
<p>Test scenario six is where we run a cluster member where the principal name is in the grant list of its own
permissions XML file, but missing from the permissions XML file on the senior running cluster member.</p>

<p>We can run the <code>server.sh</code> script with a fourth parameter to specify the name of the permissions XML file to use.
For this scenario we will use the <code>cert-permissions-all.xml</code> file which grants access to all the test principals.</p>

<markup
lang="bash"

>./server.sh member-5 member-5.p12 false cert-permissions-all.xml</markup>

<p>We can see in the console logs that this member allows permissions for principal "CN=member-5"</p>

<markup


>2025-04-22 17:32:11.360/2.021 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=Coherence, member=3): Allowed request for ("com.tangosol.net.ClusterPermission" "service=$SYS:Config,cache=*" "join") on behalf of CN=member-5</markup>

<p>But the member fails to join the cluster because it will be rejected by the senior member.
In the senior member log, there will be messages similar to this:</p>

<markup


>2025-04-22 17:32:11.378/3470.744 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=DistributedCache:$SYS:Config, member=1): Denied request for ("com.tangosol.net.ClusterPermission" "service=$SYS:Config,cache=*" "join") on behalf of CN=member-5</markup>

</div>

<h4 id="_cluster_member_using_untrusted_cert">Cluster Member Using Untrusted Cert</h4>
<div class="section">
<p>In test scenario seven, we are testing where an attacker may create a cluster member that tries to join the cluster using a principal name that is known to be in the senior members permissions XML file.
As the attacker is unable to use a certificate signed by the trusted CA, the cluster member should fail to join because the custom access controller will not verify trust in the certificate being used.</p>

<p>The bad server will use "CN=member-1" as the principal name, which we know
is allowed to join the cluster because that is the same principal the senior member is using.
But the cert stored in the <code>untrusted.p12</code> key store was signed by the untrusted CA.</p>

<p>The example contains a script <code>bad-server.sh</code> which will run a cluster member using the bad configuration.
This member will use a trust store that contains both the trusted and untrusted CA certs.
It will also use a permissions XML file that gives its principal permissions to do anything.</p>

<p>In a console window, start the bad server:</p>

<markup
lang="bash"

>./bad-server.sh</markup>

<p>In the console output, we can see the bad server fails to start. It will attempt to join with the senior member,
and the log appears to show the join, but then the senior member ejects the bad server from the cluster.</p>

<p>We can see the rejection in the senior member log output:</p>

<markup


>2025-04-22 18:01:17.644/41.492 Oracle Coherence GE 14.1.1.2206.13 &lt;Error&gt; (thread=DistributedCache:$SYS:Config, member=1): Security configuration mismatch or break-in attempt: (Wrapped: Remote permission check failed) Path does not chain with any of the trust anchors
2025-04-22 18:01:17.645/41.492 Oracle Coherence GE 14.1.1.2206.13 &lt;Error&gt; (thread=Cluster, member=1): member left due to security exception</markup>

</div>

<h4 id="_extend_client_using_principal_with_permissions">Extend Client Using Principal With Permissions</h4>
<div class="section">
<p>The first Extend client scenario is to test an Extend client using a principal name in the permissions XML grant list
can connect to the Proxy.</p>

<p>The example contains a script named <code>client.sh</code> that runs a Coherence Extend client.
This client will connect to the cluster and put a value into a cache.
It should then be possible to see in the logs which principal names were used on the cluster members.</p>

<p>With the Coherence cluster running that was started for the previous cluster member scenarios, run the client
using the following command in a console window:</p>

<markup
lang="bash"

>./client.sh member-3 member-3.p12</markup>

<p>The client will run as principal "/CN=member-3" which is in the grant list of the permissions XML file and uses
a trusted certificate. The client runs, connects to the cluster, updates a cache and exits.
The console log should not show any errors. It will show the access controller allowing the client requests.</p>

<markup


>2025-04-22 18:41:54.328/0.760 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=main, member=n/a): Allowed request for ("com.tangosol.net.ClusterPermission" "service=RemoteCache,cache=*" "join") on behalf of CN=member-3</markup>

<p>The cluster member will also show log messages when the client connects showing the principal "/CN=member-3":</p>

<markup


>2025-04-22 18:41:54.528/10.080 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=Proxy:TcpAcceptor, member=1): Allowed request for ("com.tangosol.net.ClusterPermission" "service=Proxy" "join") on behalf of CN=member-3</markup>

<p>The server is configured with a storage authorizer that logs cache access and the principal name that is performing that cache operation. We can see below that "CN=member-3" was used to update the cache.</p>

<markup


>2025-04-22 18:41:54.610/10.163 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=PartitionedCacheWorker:0x0011:4, member=1): CapturingAuthorizer: checkWrite for principals CN=member-3</markup>

<p>So we can see that the correct client principal was used when executing the cache put.</p>

</div>

<h4 id="_extend_client_using_a_principal_in_the_deny_list">Extend Client Using A Principal In The Deny List</h4>
<div class="section">
<p>The second Extend client test is where the client principal ("CN=member-4") is in the permission XML deny list.
The client should fail to start and not connect to the server.</p>

<p>With the Coherence cluster running that was started for the previous cluster member scenarios, run the client
using the following command in a console window:</p>

<markup
lang="bash"

>./client.sh member-4 member-4.p12</markup>

<p>The client log will show that it fails to start with permission exceptions:</p>

<markup


>2025-04-22 19:19:29.463/0.774 Oracle Coherence GE 14.1.1.2206.13 &lt;Info&gt; (thread=main, member=n/a): Denied request for ("com.tangosol.net.ClusterPermission" "service=RemoteCache,cache=*" "join") on behalf of CN=member-4
2025-04-22 19:19:29.465/0.776 Oracle Coherence GE 14.1.1.2206.13 &lt;Error&gt; (thread=main, member=n/a): Caught exception in SecureClient: com.tangosol.net.security.PermissionException: Insufficient rights to perform the operation</markup>

<p>In this case, the client will not have attempted to connect to the proxy because the client&#8217;s own permissions file
does not have the required permissions.</p>

</div>

<h4 id="_extend_client_using_a_principal_missing_from_the_permissions_file">Extend Client Using A Principal Missing From The Permissions File</h4>
<div class="section">
<p>The third Extend client test is where the client principal  is missing ("CN=member-5") from the permission XML file.
The client should fail to start and not connect to the server.</p>

<p>With the Coherence cluster running that was started for the previous cluster member scenarios, run the client
using the following command in a console window:</p>

<markup
lang="bash"

>./client.sh member-5 member-5.p12</markup>

<p>The client log will show that it fails to start with permission exceptions:</p>

<markup


>2025-04-22 19:24:21.520/0.760 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Info&gt; (thread=main, member=n/a): Denied request for ("com.tangosol.net.ClusterPermission" "service=RemoteCache,cache=*" "join") on behalf of CN=member-5
2025-04-22 19:24:21.523/0.763 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Error&gt; (thread=main, member=n/a): Caught exception in SecureClient: com.tangosol.net.security.PermissionException: Insufficient rights to perform the operation</markup>

<p>In this case, the client will not have attempted to connect to the proxy because the client&#8217;s own permissions file
does not have the required permissions.</p>

</div>

<h4 id="_extend_client_using_principal_in_own_grant_list_but_in_server_deny_list">Extend Client Using Principal In Own Grant List But In Server Deny List</h4>
<div class="section">
<p>The fourth client scenario is where the Extend client principal ("CN=member-4") in the client&#8217;s own permissions XML file,
but the principal is in the deny list in the Extend proxy permissions XML file.</p>

<p>We can run the <code>client.sh</code> script with a third parameter to specify the name of the permissions XML file to use.
For this scenario we will use the <code>cert-permissions-all.xml</code> file which grants access to all the test principals.</p>

<markup
lang="bash"

>./client.sh member-4 member-4.p12 cert-permissions-all.xml</markup>

<p>The client console log will show the client logging allowed permissions for the client&#8217;s principal to start the remote cache service locally, as the client is configured to allow access.</p>

<markup


>2025-04-24 10:03:07.254/5.794 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Info&gt; (thread=main, member=n/a): Allowed request for ("com.tangosol.net.ClusterPermission" "service=RemoteCache,cache=*" "join") on behalf of CN=member-4</markup>

<p>But then the client will fail to connect to the proxy, because the proxy does not allow access.</p>

<markup


>2025-04-24 10:03:07.445/5.985 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Error&gt; (thread=main, member=n/a): Error while starting service "RemoteCache": com.tangosol.net.messaging.ConnectionException: could not establish a connection to one of the following addresses: [127.0.0.1:51028.63209]</markup>

<p>The console log for the proxy that the client connected to should show the permission check failed for the client principal.</p>

<markup


>2025-04-24 10:03:07.439/22.242 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Info&gt; (thread=Proxy:TcpAcceptor, member=1): Denied request for ("com.tangosol.net.ClusterPermission" "service=Proxy" "join") on behalf of CN=member-4
2025-04-24 10:03:07.439/22.242 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Error&gt; (thread=Proxy:TcpAcceptor, member=1): Failed to create identity token: com.tangosol.net.security.PermissionException: Insufficient rights to perform the operation</markup>

</div>

<h4 id="_extend_client_using_principal_in_own_grant_list_but_missing_from_server">Extend Client Using Principal In Own Grant List But Missing From Server</h4>
<div class="section">
<p>The fifth client scenario is where the Extend client principal ("CN=member-5") in the client&#8217;s own permissions XML file,
but the principal is missing from the Extend proxy permissions XML file.</p>

<p>We can run the <code>client.sh</code> script with a third parameter to specify the name of the permissions XML file to use.
For this scenario we will use the <code>cert-permissions-all.xml</code> file which grants access to all the test principals.</p>

<markup
lang="bash"

>./client.sh member-5 member-5.p12 cert-permissions-all.xml</markup>

<p>The client console log will show the client logging allowed permissions for the client&#8217;s principal to start the remote cache service locally, as the client is configured to allow access.</p>

<markup


>2025-04-24 10:10:37.248/2.961 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Info&gt; (thread=main, member=n/a): Allowed request for ("com.tangosol.net.ClusterPermission" "service=RemoteCache,cache=*" "join") on behalf of CN=member-5</markup>

<p>But then the client will fail to connect to the proxy, because the proxy does not allow access.</p>

<markup


>2025-04-24 10:10:37.439/3.153 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Error&gt; (thread=main, member=n/a): Error while starting service "RemoteCache": com.tangosol.net.messaging.ConnectionException: could not establish a connection to one of the following addresses: [127.0.0.1:51111.53504]</markup>

<p>The console log for the proxy that the client connected to should show the permission check failed for the client principal.</p>

<markup


>2025-04-24 10:10:37.433/218.811 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Info&gt; (thread=Proxy:TcpAcceptor, member=1): Denied request for ("com.tangosol.net.ClusterPermission" "service=Proxy" "join") on behalf of CN=member-5
2025-04-24 10:10:37.433/218.811 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Error&gt; (thread=Proxy:TcpAcceptor, member=1): Failed to create identity token: com.tangosol.net.security.PermissionException: Insufficient rights to perform the operation</markup>

</div>

<h4 id="_extend_client_using_principal_with_untrusted_certificate">Extend Client Using Principal With Untrusted Certificate</h4>
<div class="section">
<p>In client test scenario seven, we are testing where an attacker may create a client member that tries to connect to the cluster using a principal name that is known to be in the proxy member&#8217;s permissions XML file.
As the attacker is unable to use a certificate signed by the trusted CA, the client should fail to connect because the custom access controller will not verify trust in the certificate being used.</p>

<p>The bad client will use "CN=member-1" as the principal name, which we know
is allowed to connect to the cluster because that is the same principal the senior member is using.
But the cert stored in the <code>untrusted.p12</code> key store was signed by the untrusted CA.</p>

<p>The example contains a script <code>bad-client.sh</code> which will run a client using the bad configuration.
This client will use a trust store that contains both the trusted and untrusted CA certs.
It will also use a permissions XML file that gives the client principal permissions to do anything.</p>

<p>The client can be run using the following command:</p>

<markup
lang="bash"

>./bad-client.sh</markup>

<p>The client console log will show the client logging allowed permissions for the client&#8217;s principal to start the remote cache service locally, as the client is configured to allow access. The untrusted CA is in the trust store used
by the custom access controller on the client, so the attacker&#8217;s certificate is trusted.</p>

<markup


>2025-04-24 10:31:58.814/0.744 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Info&gt; (thread=main, member=n/a): Allowed request for ("com.tangosol.net.ClusterPermission" "service=RemoteCache,cache=*" "join") on behalf of CN=member-1</markup>

<p>But then the client will fail to connect to the proxy, because the proxy does not allow access.</p>

<markup


>2025-04-24 10:31:59.012/0.942 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Error&gt; (thread=main, member=n/a): Error while starting service "RemoteCache": com.tangosol.net.messaging.ConnectionException: could not establish a connection to one of the following addresses: [127.0.0.1:52049.64705]</markup>

<p>The console log for the proxy that the client connected to should show the permission check failed for the client principal.</p>

<markup


>2025-04-24 10:31:59.006/15.367 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Error&gt; (thread=Proxy:TcpAcceptor, member=1): Failed to create identity token: java.security.GeneralSecurityException: Failed to verify subject CN=member-1
2025-04-24 10:31:59.006/15.367 Oracle Coherence GE 14.1.1.2206.13 (dev-jonathanknight) &lt;Error&gt; (thread=Proxy:TcpAcceptor, member=1): java.security.GeneralSecurityException: Failed to verify subject CN=member-1</markup>

</div>
</div>

<h3 id="_example_functional_tests">Example Functional Tests</h3>
<div class="section">
<p>This example includes a functional test <code>CertSecurityTests</code> that verifies all the cluster member and Extend client
scenarios discussed above. This test uses the Coherence Bedrock test framework and generates all the required keys,
certificates, and key stores when the test runs. This test requires OpenSSL to be on the system path to run.</p>

</div>

<h3 id="_summary">Summary</h3>
<div class="section">
<p>This example has shown how it is possible to use certificate base security with Coherence to secure both cluster
members and Extend clients. It should be possible to use this example as a starting point for other custom
access controller implementations.</p>

</div>
</div>
</doc-view>
