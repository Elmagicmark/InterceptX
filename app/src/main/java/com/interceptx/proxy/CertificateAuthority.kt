package com.interceptx.proxy

import android.content.Context
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
 * enabling TLS decryption (MITM) the same way Burp/mitmproxy/HTTP Toolkit do.
 *
 * The user must install the exported root CA as a trusted certificate on the
 * device (see Certificates screen) for target apps to trust intercepted HTTPS
 * traffic. InterceptX never ships a pre-trusted CA — one is generated locally
 * per install so no two installs share a private key.
 */
class CertificateAuthority(private val context: Context) {

    private val provider = BouncyCastleProvider().also { Security.addProvider(it) }
    private val keyStoreFile = File(context.filesDir, "interceptx_ca.jks")
    private val storePassword = "interceptx".toCharArray()
    private val leafCache = ConcurrentHashMap<String, Pair<PrivateKey, X509Certificate>>()

    private lateinit var caKeyPair: KeyPair
    private lateinit var caCert: X509Certificate

    fun init() {
        val ks = KeyStore.getInstance("BKS", "BC")
        if (keyStoreFile.exists()) {
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
        builder.addExtension(
            Extension.keyUsage, true,
            KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign)
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
}
