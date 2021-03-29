package io.hackle.android.internal.http

import android.content.Context
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import io.hackle.sdk.core.internal.log.Logger
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.net.ssl.*

internal object Tls {

    private val log = Logger<Tls>()

    const val V_1_2 = "TLSv1.2"

    fun update(context: Context) {
        try {
            SSLContext.getInstance(V_1_2)
        } catch (_: NoSuchAlgorithmException) {
            log.info { "TLS v1.2 is not available, start installation" }
            try {
                ProviderInstaller.installIfNeeded(context)
            } catch (_: GooglePlayServicesRepairableException) {
                log.error { "Failed to install TLS v1.2, Google Play Service version is very old" }
            } catch (_: GooglePlayServicesNotAvailableException) {
                log.error { "Failed to install TLS v1.2, Google Play Service is not available" }
            }
        }
    }

    fun defaultTrustManager(): X509TrustManager {
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                init(null as KeyStore?)
            }
        val trustManagers = trustManagerFactory.trustManagers
        require(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            "Unexpected default trust managers: ${Arrays.toString(trustManagers)}"
        }
        return trustManagers[0] as X509TrustManager
    }

    fun tls12SocketFactory(): SSLSocketFactory {
        val context = SSLContext.getInstance("TLS").apply { init(null, null, null) }
        return Tls12SocketFactory(context.socketFactory)
    }
}

internal class Tls12SocketFactory(
    private val delegate: SSLSocketFactory,
) : SSLSocketFactory() {

    override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
        return patch(delegate.createSocket(s, host, port, autoClose))
    }

    override fun createSocket(host: String?, port: Int): Socket {
        return patch(delegate.createSocket(host, port))
    }

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int,
    ): Socket {
        return patch(delegate.createSocket(host, port, localHost, localPort))
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        return patch(delegate.createSocket(host, port))
    }

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket {
        return patch(delegate.createSocket(address, port, localAddress, localPort))
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return delegate.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return delegate.supportedCipherSuites
    }

    private fun patch(socket: Socket): Socket {
        if (socket is SSLSocket) {
            socket.enabledProtocols = TLS_V_1_2_ONLY
        }
        return socket
    }

    companion object {
        private val TLS_V_1_2_ONLY = arrayOf(Tls.V_1_2)
    }
}
