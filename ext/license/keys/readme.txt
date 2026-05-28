The contents of this directory contain two important files:

tangosol.keystore  *** NEVER SHIP, OR DELETE THIS FILE ***

	Keystore including the private key used to sign all Coherence licenses.  The private key and
	associated certificate are stored under the alias "tangosol" with the password "coherence",
	which is also the keystore password.  Any update to this file must retain the existing public
	and private keys, otherwise existing licenses would be incompatible.

tangosol.cer

	This file is the extracted certificate which contains the public key used to validate license
	signatures. It is shiped as part of tangosol.jar.  The certificate information is as follows:

	Owner: CN="Tangosol, Inc.", OU=Unknown, O="Tangosol, Inc.", L=Somerville, ST=MA, C=US
	Issuer: CN="Tangosol, Inc.", OU=Unknown, O="Tangosol, Inc.", L=Somerville, ST=MA, C=US
	Serial number: 4511a841
	Valid from: Wed Sep 20 16:44:49 EDT 2006 until: Mon Jul 05 16:44:49 EDT 2280
	Certificate fingerprints:
        	 MD5:  7A:28:2C:3C:DF:E6:F4:85:00:28:1A:18:29:DB:D0:15
	         SHA1: B1:93:DC:5C:F3:6D:D8:61:56:E5:88:9C:B6:E3:EA:A5:C2:BA:5E:23