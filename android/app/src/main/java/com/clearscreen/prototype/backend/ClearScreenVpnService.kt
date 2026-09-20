package com.clearscreen.prototype.backend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.clearscreen.prototype.MainActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class ClearScreenVpnService : VpnService() {
  private var vpnInterface: ParcelFileDescriptor? = null
  private var worker: Thread? = null
  private val stopping = AtomicBoolean(false)
  private lateinit var store: ClearScreenStore

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopVpn()
      return START_NOT_STICKY
    }
    store = ClearScreenStore(this)
    createNotificationChannel()
    startForeground(NOTIFICATION_ID, buildNotification())
    if (vpnInterface == null) establishVpn()
    return START_STICKY
  }

  override fun onDestroy() {
    stopVpn()
    super.onDestroy()
  }

  private fun establishVpn() {
    stopping.set(false)
    vpnInterface = Builder()
      .setSession("净屏")
      .setMtu(1500)
      .addAddress("10.8.0.2", 32)
      .addDnsServer("10.8.0.1")
      .addRoute("10.8.0.1", 32)
      .setBlocking(true)
      .establish()
    if (vpnInterface == null) return
    running = true
    worker = Thread({ runDnsLoop() }, "clearscreen-dns-filter").also { it.start() }
  }

  private fun runDnsLoop() {
    val descriptor = vpnInterface ?: return
    val input = FileInputStream(descriptor.fileDescriptor)
    val output = FileOutputStream(descriptor.fileDescriptor)
    val buffer = ByteArray(32767)
    try {
      while (!stopping.get()) {
        val length = input.read(buffer)
        if (length <= 0) continue
        val response = handleDnsPacket(buffer, length) ?: continue
        output.write(response)
        output.flush()
      }
    } catch (_: Exception) {
      // The service will be restarted by the system if the process is reclaimed.
    } finally {
      input.close()
      output.close()
    }
  }

  private fun handleDnsPacket(packet: ByteArray, length: Int): ByteArray? {
    if (length < 28) return null
    val version = (packet[0].toInt() ushr 4) and 0x0F
    val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
    if (version != 4 || ipHeaderLength < 20 || length < ipHeaderLength + 8) return null
    if ((packet[9].toInt() and 0xFF) != 17) return null
    val udpOffset = ipHeaderLength
    val destinationPort = readU16(packet, udpOffset + 2)
    if (destinationPort != 53) return null
    val udpLength = readU16(packet, udpOffset + 4)
    val dnsOffset = udpOffset + 8
    val dnsLength = minOf(udpLength - 8, length - dnsOffset)
    if (dnsLength < 12) return null
    val query = packet.copyOfRange(dnsOffset, dnsOffset + dnsLength)
    val domain = readDomain(query) ?: return null
    val dnsResponse = if (store.shouldBlockDomain(domain)) {
      store.appendEvent("network", null, domain, "success", "已拦截广告请求")
      blockedDnsResponse(query)
    } else {
      forwardDns(query)
    } ?: return null
    return buildResponsePacket(packet, length, ipHeaderLength, dnsOffset, dnsResponse)
  }

  private fun forwardDns(query: ByteArray): ByteArray? {
    return try {
      DatagramSocket().use { socket ->
        if (!protect(socket)) return null
        socket.soTimeout = 1500
        val upstream = DatagramPacket(query, query.size, InetAddress.getByName("1.1.1.1"), 53)
        socket.send(upstream)
        val responseBuffer = ByteArray(4096)
        val response = DatagramPacket(responseBuffer, responseBuffer.size)
        socket.receive(response)
        response.data.copyOf(response.length)
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun blockedDnsResponse(query: ByteArray): ByteArray {
    val response = query.copyOf()
    val flags = readU16(query, 2)
    writeU16(response, 2, (flags and 0x0100) or 0x8000 or 0x0003)
    writeU16(response, 4, readU16(query, 4))
    writeU16(response, 6, 0)
    writeU16(response, 8, 0)
    writeU16(response, 10, 0)
    return response
  }

  private fun buildResponsePacket(
    request: ByteArray,
    requestLength: Int,
    ipHeaderLength: Int,
    dnsOffset: Int,
    dnsResponse: ByteArray,
  ): ByteArray {
    val udpOffset = ipHeaderLength
    val result = ByteArray(ipHeaderLength + 8 + dnsResponse.size)
    request.copyInto(result, 0, 0, minOf(ipHeaderLength + 8, requestLength))
    for (index in 0 until 4) {
      result[12 + index] = request[16 + index]
      result[16 + index] = request[12 + index]
    }
    result[udpOffset] = request[udpOffset + 2]
    result[udpOffset + 1] = request[udpOffset + 3]
    result[udpOffset + 2] = request[udpOffset]
    result[udpOffset + 3] = request[udpOffset + 1]
    val totalLength = result.size
    writeU16(result, 2, totalLength)
    writeU16(result, udpOffset + 4, 8 + dnsResponse.size)
    result[udpOffset + 6] = 0
    result[udpOffset + 7] = 0
    dnsResponse.copyInto(result, ipHeaderLength + 8)
    result[10] = 0
    result[11] = 0
    writeU16(result, 10, checksum(result, 0, ipHeaderLength))
    writeU16(result, udpOffset + 6, udpChecksum(result, udpOffset, 8 + dnsResponse.size))
    return result
  }

  private fun readDomain(query: ByteArray): String? {
    var offset = 12
    val labels = mutableListOf<String>()
    while (offset < query.size) {
      val size = query[offset].toInt() and 0xFF
      offset += 1
      if (size == 0) break
      if (size > 63 || offset + size > query.size) return null
      labels += String(query, offset, size, Charsets.US_ASCII)
      offset += size
    }
    return labels.joinToString(".").takeIf { it.isNotBlank() }
  }

  private fun udpChecksum(packet: ByteArray, offset: Int, length: Int): Int {
    var sum = 0L
    for (index in 0 until 4 step 2) {
      sum += ((packet[12 + index].toInt() and 0xFF) shl 8) or (packet[12 + index + 1].toInt() and 0xFF)
      sum += ((packet[16 + index].toInt() and 0xFF) shl 8) or (packet[16 + index + 1].toInt() and 0xFF)
    }
    sum += 17
    sum += length
    sum += checksumSum(packet, offset, length)
    return foldChecksum(sum)
  }

  private fun checksum(packet: ByteArray, offset: Int, length: Int): Int =
    foldChecksum(checksumSum(packet, offset, length))

  private fun checksumSum(packet: ByteArray, offset: Int, length: Int): Long {
    var sum = 0L
    var index = offset
    val end = offset + length
    while (index + 1 < end) {
      sum += ((packet[index].toInt() and 0xFF) shl 8) or (packet[index + 1].toInt() and 0xFF)
      index += 2
    }
    if (index < end) sum += (packet[index].toInt() and 0xFF) shl 8
    return sum
  }

  private fun foldChecksum(value: Long): Int {
    var sum = value
    while ((sum ushr 16) != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
    return (sum.inv() and 0xFFFF).toInt()
  }

  private fun readU16(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

  private fun writeU16(bytes: ByteArray, offset: Int, value: Int) {
    bytes[offset] = (value ushr 8).toByte()
    bytes[offset + 1] = value.toByte()
  }

  private fun stopVpn() {
    stopping.set(true)
    worker?.interrupt()
    worker = null
    vpnInterface?.close()
    vpnInterface = null
    running = false
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java)
      manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "净屏网络过滤", NotificationManager.IMPORTANCE_LOW),
      )
    }
  }

  private fun buildNotification(): Notification {
    val intent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification.Builder(this, CHANNEL_ID)
        .setContentTitle("净屏正在运行")
        .setContentText("本地网络过滤已开启")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentIntent(intent)
        .setOngoing(true)
        .build()
    } else {
      Notification.Builder(this)
        .setContentTitle("净屏正在运行")
        .setContentText("本地网络过滤已开启")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentIntent(intent)
        .setOngoing(true)
        .build()
    }
  }

  companion object {
    const val ACTION_STOP = "com.clearscreen.prototype.action.STOP_VPN"
    private const val CHANNEL_ID = "clearscreen_network_filter"
    private const val NOTIFICATION_ID = 801

    @Volatile
    var running: Boolean = false
  }
}
