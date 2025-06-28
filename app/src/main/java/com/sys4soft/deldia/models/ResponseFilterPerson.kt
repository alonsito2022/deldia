package com.sys4soft.deldia.models

import com.google.gson.annotations.SerializedName

data class ResponseFilterPerson(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("results")
    val results: ArrayList<Person>? = null
)