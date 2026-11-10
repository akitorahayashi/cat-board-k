package com.akitorahayashi.catboard.catapi

class HttpFailure(val statusCode: Int) : Exception("The Cat API returned HTTP $statusCode")
