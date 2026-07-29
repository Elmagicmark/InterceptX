package com.interceptx.proxy

import android.content.Context
import org.bouncycastle.asn1.DERIA5String
import org.bouncycastle.asn1.misc.MiscObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

/**
 * Manages the InterceptX root CA used to sign per-host leaf certificates on the fly,
 * enabling TLS decryption (MITM) the same way Burp/mitmproxy/Reqable do.
 *
 * The user must install the exported root CA as a trusted certificate on the
 * device (see Certificates screen) for target apps to trust intercepted HTTPS
 * traffic. InterceptX never ships a pre-trusted CA — one is generated locally
 * per install so no two installs share a private key.
 *
 * Certificate shape (extensions, key identifiers, EKU) mirrors what mature MITM
 * tools like Reqable/Charles/mitmproxy generate, for maximum acceptance by
 * modern OS/browser certificate validators.
 */
class CertificateAuthority(private val context: Context) {

    private val provider = BouncyCastleProvider().also {
        // Android may already have a limited provider registered under the name
        // "BC" (or none at all, depending on OS version). Security.addProvider()
        // silently no-ops if a provider with that name already exists, which
        // would leave us using Android's stripped-down implementation instead
        // of the full BouncyCastle we depend on for BKS keystores and custom
        // extensions. Forcibly remove any existing "BC" and install ours first.
        Security.removeProvider("BC")
        Security.insertProviderAt(it, 1)
    }
    private val keyStoreFile = File(context.filesDir, "interceptx_ca.jks")
    private val versionFile = File(context.filesDir, "interceptx_ca_version.txt")
    private val storePassword = "interceptx".toCharArray()
    private val leafCache = ConcurrentHashMap<String, Pair<PrivateKey, X509Certificate>>()
    private val extUtils = JcaX509ExtensionUtils()

    private lateinit var caKeyPair: KeyPair
    private lateinit var caCert: X509Certificate

    companion object {
        // Bump this whenever generateRootCa()/generateLeaf() extensions change —
        // it forces a fresh CA (and fresh leaf cache) instead of silently reusing
        // an old certificate that was generated before the change.
        private const val CA_SCHEMA_VERSION = 2
    }

    fun init() {
        val ks = KeyStore.getInstance("BKS", "BC")
        val storedVersion = if (versionFile.exists()) versionFile.readText().trim().toIntOrNull() else null
        val needsRegeneration = storedVersion != CA_SCHEMA_VERSION

        if (keyStoreFile.exists() && !needsRegeneration) {
            keyStoreFile.inputStream().use { ks.load(it, storePassword) }
            caKeyPair = KeyPair(
                ks.getCertificate("ca").publicKey,
                ks.getKey("ca", storePassword) as PrivateKey
            )
            caCert = ks.getCertificate("ca") as X509Certificate
        } else {
            generateRootCa()
            ks.load(null, null)
            ks.setKeyEntry("ca", caKeyPair.private, storePassword, arrayOf<Certificate>(caCert))
            keyStoreFile.outputStream().use { ks.store(it, storePassword) }
            versionFile.writeText(CA_SCHEMA_VERSION.toString())
        }
    }

    private fun generateRootCa() {
        val kpg = KeyPairGenerator.getInstance("RSA", "BC")
        kpg.initialize(2048)
        caKeyPair = kpg.generateKeyPair()

        val subject = X500Name("CN=InterceptX Root CA, O=InterceptX, OU=Security Research")
        val serial = BigInteger(160, SecureRandom())
        val notBefore = Date(System.currentTimeMillis() - 24L * 3600 * 1000)
        val notAfter = Date(System.currentTimeMillis() + 10L * 365 * 24 * 3600 * 1000)

        val builder = X509v3CertificateBuilder(
            subject, serial, notBefore, notAfter, subject,
            org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(caKeyPair.public.encoded)
        )
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))
        // Key identifiers let validators build/verify the chain reliably instead of
        // matching on subject name alone — every mature MITM tool (Reqable, Charles,
        // mitmproxy) includes these on its root.
        builder.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(caKeyPair.public))
        builder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caKeyPair.public))
        // Purely informational — shows up if someone inspects the chain, same idea
        // as Reqable's own root comment.
        builder.addExtension(
            MiscObjectIdentifiers.netscapeCertComment, false,
            DERIA5String(
                "This root certificate was generated locally by InterceptX for HTTPS " +
                    "interception during security testing. If you see this certificate in a " +
                    "chain, traffic to this host is being decrypted by InterceptX on this device."
            )
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(caKeyPair.private)
        caCert = JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer))
    }

    /** Returns (or lazily generates + caches) a leaf certificate for [host], signed by our root CA. */
    fun leafCertificateFor(host: String): Pair<PrivateKey, X509Certificate> =
        leafCache.getOrPut(host) { generateLeaf(host) }

    private fun generateLeaf(host: String): Pair<PrivateKey, X509Certificate> {
        val kpg = KeyPairGenerator.getInstance("RSA", "BC")
        kpg.initialize(2048)
        val leafKeyPair = kpg.generateKeyPair()

        val subject = X500Name("CN=$host")
        val serial = BigInteger(160, SecureRandom())
        val notBefore = Date(System.currentTimeMillis() - 24L * 3600 * 1000)
        val notAfter = Date(System.currentTimeMillis() + 825L * 24 * 3600 * 1000)

        val builder = X509v3CertificateBuilder(
            X500Name(caCert.subjectX500Principal.name), serial, notBefore, notAfter, subject,
            org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(leafKeyPair.public.encoded)
        )
        val sanNames = GeneralNames(arrayOf(GeneralName(GeneralName.dNSName, host)))
        builder.addExtension(Extension.subjectAlternativeName, false, sanNames)
        builder.addExtension(Extension.basicConstraints, false, BasicConstraints(false))
        builder.addExtension(
            Extension.keyUsage, true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment)
        )
        // Modern Chromium/Android validators expect a TLS server leaf to explicitly
        // declare serverAuth EKU — without it some validators reject or warn even
        // though the chain otherwise checks out.
        builder.addExtension(
            Extension.extendedKeyUsage, false,
            ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
        )
        // Chain-building identifiers, matching the root's.
        builder.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(leafKeyPair.public))
        builder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caKeyPair.public))

        val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(caKeyPair.private)
        val leafCert = JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer))
        return leafKeyPair.private to leafCert
    }

    /** Builds an SSLContext presenting a host-specific leaf cert for the CONNECT tunnel. */
    fun sslContextFor(host: String): SSLContext {
        val (privateKey, leafCert) = leafCertificateFor(host)
        val ks = KeyStore.getInstance("BKS", "BC")
        ks.load(null, null)
        ks.setKeyEntry("leaf", privateKey, storePassword, arrayOf<Certificate>(leafCert, caCert))

        val kmf = KeyManagerFactory.getInstance("X509")
        kmf.init(ks, storePassword)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, null, SecureRandom())
        return sslContext
    }

    fun rootCertificate(): X509Certificate = caCert

    /** SHA-256 fingerprint of the root CA, formatted for display/verification. */
    fun rootFingerprintSha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(caCert.encoded)
        return bytes.joinToString(":") { String.format("%02X", it) }
    }

    fun exportRootCertPem(): String {
        val encoder = Base64.getMimeEncoder(64, "\n".toByteArray())
        val encoded = encoder.encodeToString(caCert.encoded)
        return "-----BEGIN CERTIFICATE-----\n$encoded\n-----END CERTIFICATE-----\n"
    }

    /**
     * Writes the root CA as a DER-encoded .crt file in the app cache dir, ready to be
     * handed to Android's system certificate installer (or shared to a browser/file
     * manager) via a FileProvider URI. DER is used — rather than PEM — because
     * Android's built-in "Install certificate" flow recognizes it more reliably
     * across OEM skins.
     */
    fun exportRootCertFile(context: Context): File {
        val file = File(context.cacheDir, "interceptx_ca.crt")
        file.writeBytes(caCert.encoded)
        return file
    }
}
