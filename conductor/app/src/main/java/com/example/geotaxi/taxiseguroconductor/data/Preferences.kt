package com.example.geotaxi.taxiseguroconductor.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.example.geotaxi.taxiseguroconductor.ui.MainActivity

/**
 * Created by sebas on 1/14/18.
 */

class DataHandler {

    companion object {
        fun pref(context : Context) : SharedPreferences {
            return context.getSharedPreferences("TAXI-SEGURO", 0)
        }
        fun editor(context : Context) : SharedPreferences.Editor {
            return DataHandler.pref(context).edit()
        }

        fun saveUser(context: Context, _id: String) {
            /* DataHandler.editor(context).putString("_id", _id)
            DataHandler.editor(context).commit() */
            User.instance._id = _id
        }

        fun getUserID(context: Context): String {
            /*val preference = PreferenceManager.getDefaultSharedPreferences(context)
            return preference.getString("_id", null)*/
            return User.instance._id
        }

        fun getUserRole(context: Context): String {
            return User.instance.role
        }
    }
}
