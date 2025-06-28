package com.sys4soft.deldia.localdatabase

import android.content.Context
import android.content.SharedPreferences

class Preference {
    private val PREFERENCE_NAME = "MY_PREFERENCE"
    lateinit var context: Context
    lateinit var preferences: SharedPreferences

    constructor(context: Context?){
        this.context = context!!
        preferences = context.getSharedPreferences("MY_PREFERENCE", Context.MODE_PRIVATE)
    }

    public fun saveData(key:String, value: String){
        var editor = preferences.edit()
        editor.putString(key, value)
        editor.commit()
    }

    public fun getData(key:String): String{
        return preferences.getString(key, "")!!
    }

    public fun clearPreference(){
        var editor = preferences.edit()
        editor.clear()
        editor.commit()
    }
}