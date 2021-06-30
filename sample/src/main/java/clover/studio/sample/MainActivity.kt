package clover.studio.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import clover.studio.sample.databinding.ActivityMainBinding
import clover.studio.thinws.*
import org.json.JSONObject
import java.util.*

private const val SOCKET_URL = "ws://192.168.1.119:8000"

class MainActivity : AppCompatActivity(), ThinWS.Listener {
    private lateinit var binding: ActivityMainBinding
    private val connectionID = UUID.randomUUID().toString()
    private val socketManager =  ThinWS(SOCKET_URL, connectionID,this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.subscribe.setOnClickListener {
            socketManager.request(
                Message(
                    type = MESSAGE_SUBSCRIBE,
                    roomID = "roomID",
                    messageID = UUID.randomUUID().toString()
                )
            ).subscribe()
        }

        binding.sendMessage.setOnClickListener {
            socketManager.request(
                Message(
                    type = MESSAGE_GENERAL,
                    roomID = "roomID",
                    messageID = UUID.randomUUID().toString(),
                    payload = JSONObject().apply {
                        put("messageText", "Ovo je tekst")
                    }
                )
            ).subscribe()
        }

    }

    override fun onOpen() {
        Log.d(TAG, "onOpen")
    }

    override fun onFail() {
        Log.d(TAG, "onFail")
    }

    override fun onMessage(message: String) {
        Log.d(TAG, "onMessage: $message")
    }

    override fun onRequest(request: String) {
        Log.d(TAG, "onRequest")
    }

    override fun onDisconnected() {
        Log.d(TAG, "onDisconnect")
    }

    override fun onClose() {
        socketManager.close()
    }

    companion object {
        const val TAG = "ThinWS"
    }
}