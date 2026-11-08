package com.clearscreen.prototype.backend

import android.content.Context
import androidx.annotation.Keep
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date

/**
 * Locally generated ADB key pair. It is only used for the loopback/wireless ADB
 * connection this app initiates, and never leaves the app's private directory.
 * Pairing codes, the private key and the certificate are never logged.
 */
@Keep
internal object ClearScreenAdbKeys {
  private const val DIR = "adb"
  private const val KEY_FILE = "adbkey"
  private const val CERT_FILE = "adbcert"

  @Volatile private var cachedPrivate: PrivateKey? = null
  @Volatile private var cachedCertificate: Certificate? = null

  fun privateKey(context: Context): PrivateKey = cachedPrivate ?: synchronized(this) {
    cachedPrivate ?: loadOrCreate(context).first.also { cachedPrivate = it }
  }

  fun certificate(context: Context): Certificate = cachedCertificate ?: synchronized(this) {
    cachedCertificate ?: loadOrCreate(context).second.also { cachedCertificate = it }
  }

  private fun loadOrCreate(context: Context): Pair<PrivateKey, Certificate> {
    val dir = File(context.filesDir, DIR)
    if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("无法创建授权密钥目录")
    dir.setReadable(false, false)
    dir.setWritable(false, false)
    dir.setExecutable(false, false)
    dir.setReadable(true, true)
    dir.setWritable(true, true)
    dir.setExecutable(true, true)
    val keyFile = File(dir, KEY_FILE)
    val certFile = File(dir, CERT_FILE)
    if (keyFile.isFile && certFile.isFile) {
      runCatching { return readPair(keyFile, certFile) }
    }
    val generated = generate()
    writePrivate(keyFile, generated.first)
    writePrivate(certFile, generated.second)
    return generated
  }

  private fun readPair(keyFile: File, certFile: File): Pair<PrivateKey, Certificate> {
    val keyFactory = KeyFactory.getInstance("RSA")
    val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
    val certificate = certFile.inputStream().use { CertificateFactory.getInstance("X.509").generateCertificate(it) }
    return privateKey to certificate
  }

  private fun generate(): Pair<PrivateKey, Certificate> {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048, SecureRandom())
    val pair = generator.generateKeyPair()
    val now = Date()
    val subject = X500Name("CN=ClearScreen Local Adb")
    val builder = JcaX509v3CertificateBuilder(
      subject,
      BigInteger.valueOf(now.time),
      now,
      Date(now.time + 30L * 365 * 24 * 3600 * 1000),
      subject,
      pair.public,
    )
    val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)
    val certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))
    return pair.private to certificate as X509Certificate
  }

  private fun writePrivate(file: File, value: Any) {
    val bytes = when (value) {
      is PrivateKey -> value.encoded
      is Certificate -> value.encoded
      else -> throw IllegalArgumentException("不支持的授权密钥内容")
    }
    file.writeBytes(bytes)
    file.setReadable(false, false)
    file.setWritable(false, false)
    file.setExecutable(false, false)
    file.setReadable(true, true)
    file.setWritable(true, true)
  }
}
