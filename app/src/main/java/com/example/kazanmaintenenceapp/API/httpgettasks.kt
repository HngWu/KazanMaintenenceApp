package com.example.kazanmaintenenceapp.API

import com.example.kazanmaintenenceapp.Models.Task
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class httpgettasks {
    fun getTasks(): MutableList<Task>? {
        val url = URL("http://10.0.2.2:5072/api/Maintenance/gettasks")

        try {
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("Content-Type", "application/json; utf-8")
            con.setRequestProperty("Accept", "application/json")


            val status = con.responseCode
            if (status == 200) {
                val reader = BufferedReader(InputStreamReader(con.inputStream))
                val jsonData = reader.use { it.readText() }
//                var line: String?
//                while (reader.readLine().also { line = it } != null) {
//                    jsonData.append(line)
//                }
                reader.close()
                val jsonArray = JSONArray(jsonData)
                val taskList = mutableListOf<Task>()
                for (i in 0 until jsonArray.length()) {
                    val taskObject = jsonArray.getJSONObject(i)
                    val scheduleKilometer = if (taskObject.isNull("scheduleKilometer")) null else taskObject.getInt("scheduleKilometer")
                    val asset = Task(
                        taskObject.getInt("id"),
                        taskObject.getString("assetName"),
                        taskObject.getString("assetSn"),
                        taskObject.getString("name"),
                        taskObject.getString("scheduleType"),
                        taskObject.getString("scheduleDate"),
                        scheduleKilometer,
                        taskObject.getBoolean("taskDone"),
                    )
                    taskList.add(asset)
                }

                return taskList
            }

            con.disconnect()
        } catch (e: Exception) {
            return null
        }

        return null


    }
}