package clover.studio.thinws

import java.lang.Exception

class ThinWSException(
    val errorReason: String?
) : Exception()