package clover.studio.thinws

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.WorkerThread
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.util.*

class ThinWS(
    socketUrl: String,
    private val connectionID: String,
    private val listener: Listener
) : WebSocketTransport.Listener {

    interface Listener {
        fun onOpen()
        fun onFail()
        fun onRequest(request: String)
        fun onMessage(message: String)
        fun onDisconnected()
        fun onClose()
    }

    interface RequestHandler {
        fun resolve(response: Message)
        fun reject(errorReason: String)
    }

    private val mTimerCheckHandler: Handler = Handler(Looper.getMainLooper())

    // Map of pending sent request objects indexed by request id.
    private val mSends: HashMap<String, RequestHandlerProxy> = HashMap()

    // Closed flag.
    private var mClosed = false

    // Connected flag.
    private var mConnected = false

    private val transport = WebSocketTransport(socketUrl)

    init {
        handleTransport()
    }

    fun close() {
        if (!transport.isClosed()){
            transport.close()
        }
    }

    @WorkerThread
    @Throws(ThinWSException::class)
    fun syncRequest(message: Message): Message {
        Log.d(TAG, "syncRequest(), type: ${message.type}")
        return try {
            request(message).blockingFirst()
        } catch (throwable: Throwable) {
            throw ThinWSException(throwable.message)
        }
    }

    fun request(message: Message): Observable<Message> {
        Log.d(TAG, "request(), method: $message")
        return Observable.create { emitter: ObservableEmitter<Message> ->
            request(
                message,
                object : RequestHandler {
                    override fun resolve(response: Message) {
                        if (!emitter.isDisposed) {
                            emitter.onNext(response)
                        }
                    }

                    override fun reject(errorReason: String) {
                        if (!emitter.isDisposed) {
                            emitter.onError(ThinWSException(errorReason))
                        }
                    }
                })
        }
    }

    private fun request(
        message: Message,
        requestHandler: RequestHandler
    ) {
        val requestId = message.messageID
        val payload: String = transport.sendMessage(message)
        val timeout = (1500 * (15 + 0.1 * payload.length)).toLong()
        mSends[requestId] =
            RequestHandlerProxy(requestId, timeout, requestHandler)
    }

    inner class RequestHandlerProxy(
        var mRequestId: String,
        timeoutDelayMillis: Long,
        var requestHandler: RequestHandler?
    ) : RequestHandler,
        Runnable {
        override fun run() {
            mSends.remove(mRequestId)
            if (requestHandler != null) {
                requestHandler?.reject("request timeout")
            }
        }

        override fun resolve(response: Message) {
            if (requestHandler != null) {
                requestHandler?.resolve(response)
            }
        }

        override fun reject(errorReason: String) {
            if (requestHandler != null) {
                requestHandler?.reject(errorReason)
            }
        }

        fun close() {
            // stop timeout check.
            mTimerCheckHandler.removeCallbacks(this)
        }

        init {
            mTimerCheckHandler.postDelayed(this, timeoutDelayMillis)
        }
    }

    private fun handleTransport() {
        if (transport.isClosed()) {
            if (mClosed) {
                return
            }
            mConnected = false
            listener.onClose()
            return
        }
        transport.connect(this)
    }

    override fun onOpen() {
        if (mClosed) {
            return
        }
        Log.d(TAG, "onOpen()")
        mConnected = true

        val connectMessage = Message(
            type = MESSAGE_CONNECT,
            connectionID = connectionID,
            messageID = UUID.randomUUID().toString()
        )

        transport.sendMessage(connectMessage)

        listener.onOpen()
    }

    override fun onFail() {
        if (mClosed) {
            return
        }
        Log.e(TAG, "onFail()")
        mConnected = false
        listener.onFail()
    }

    override fun onMessage(message: Message) {
        if (mClosed) {
            return
        }
        Log.d(TAG, "onMessage() $message")
        handleResponse(message)
    }

    private fun handleResponse(response: Message) {
        val sent: RequestHandlerProxy? = mSends.remove(response.messageID)
        if (sent == null) {
            Log.e(
                TAG,
                "received response does not match any sent request [id:" + response.messageID + "]"
            )
            return
        }
        sent.close()
        if (response.type == MESSAGE_ERROR) {
            sent.reject(response.type)
        } else {
            sent.resolve(response)
        }
    }

    override fun onDisconnected() {
        if (mClosed) {
            return
        }
        Log.w(TAG, "onDisconnected()")
        mConnected = false
        listener.onDisconnected()
    }

    override fun onClose() {
        if (mClosed) {
            return
        }
        Log.w(TAG, "onClose()")
        mClosed = true
        mConnected = false
        listener.onClose()
    }

    companion object {
        // Log tag.
        private const val TAG = "ThinWS"
    }
}
