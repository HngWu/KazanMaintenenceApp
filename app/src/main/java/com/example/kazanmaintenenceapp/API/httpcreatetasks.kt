package com.example.kazanmaintenenceapp.API

import android.os.Handler
import android.os.Looper
import com.example.kazanmaintenenceapp.Models.CreateTask
import com.example.kazanmaintenenceapp.Models.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class httpcreatetasks {
    fun postTask(tasksList: List<CreateTask>, onSuccess: (Boolean) -> Unit, onFailure: (Throwable) -> Unit) {


        val url = URL("http://10.0.2.2:5072/api/Maintenance/createtask")
        try {
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.setRequestProperty("Content-Type", "application/json; utf-8")
            con.setRequestProperty("Accept", "application/json")
            con.doOutput = true


            val json = Json.encodeToString(tasksList)
            val os = OutputStreamWriter(con.outputStream)

            os.write(json)
            os.flush()
            os.close()

            val status = con.responseCode
            if (status == 200) {

                Handler(Looper.getMainLooper()).post() {
                    onSuccess(true)
                }
            } else {
                Handler(Looper.getMainLooper()).post{
                    onFailure(Throwable("Post asset failed"))
                }
            }



        }catch (e: Exception) {
            onFailure(e)
        }

    }

}