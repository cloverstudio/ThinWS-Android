package clover.studio.thinws

import org.json.JSONObject

const val MESSAGE_ACK = "ack"
const val MESSAGE_ERROR = "error"
const val MESSAGE_GENERAL = "message"
const val MESSAGE_CONNECT = "connect"
const val MESSAGE_SUBSCRIBE = "subscribe"

data class Message(
    val type: String,
    val roomID: String? = null,
    val connectionID: String? = null,
    val messageID: String,
    val payload: JSONObject? = null
)

object MessageMapper{

    @Throws(IllegalArgumentException::class)
    fun mapMessage(message: String): Message{

        if (message.isEmpty()){
            throw IllegalArgumentException()
        }

        val jsonObject = JSONObject(message)
        val type = jsonObject.getString("type")

        val roomID = if (jsonObject.has("roomID")){
            jsonObject.getString("roomID")
        } else {
            null
        }

        val connectionID = if (jsonObject.has("connectionID")){
            jsonObject.getString("connectionID")
        } else {
            null
        }

        val messageID = jsonObject.getString("messageID")

        val payload = if (jsonObject.has("payload")) {
            jsonObject.getJSONObject("payload")
        } else {
            null
        }

        return Message(
            type,
            roomID,
            connectionID,
            messageID,
            payload
        )
    }
}
