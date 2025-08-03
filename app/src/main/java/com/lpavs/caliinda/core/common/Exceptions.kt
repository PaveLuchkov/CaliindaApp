package com.lpavs.caliinda.core.common

class ApiException(val code: Int, override val message: String?) : Throwable(message)
class NetworkException(override val message: String?) : Throwable(message)
class UnknownException(override val message: String?) : Throwable(message)