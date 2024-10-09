package com.example.kazaninventoryapp.httpservice

import com.example.kazaninventoryapp.Models.Asset
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class httpgetassets {
    fun getAssets(): MutableList<Asset>? {
        val url = URL("http://10.0.2.2:5072/api/Maintenance/getassets")

        try {
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "GET"
            con.setRequestProperty("Content-Type", "application/json; utf-8")
            con.setRequestProperty("Accept", "application/json")



            if (con.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(con.inputStream))
                val jsonData = reader.use { it.readText() }
//                var line: String?
//                while (reader.readLine().also { line = it } != null) {
//                    jsonData.append(line)
//                }
                reader.close()
                val jsonArray = JSONArray(jsonData)
                val assetsList = mutableListOf<Asset>()
                for (i in 0 until jsonArray.length()) {
                    val assetObject = jsonArray.getJSONObject(i)
                    val asset = Asset(
                        assetObject.getInt("id"),
                        assetObject.getString("assetSn"),
                        assetObject.getString("assetName"),
                        assetObject.getInt("departmentLocationId"),
                        assetObject.getInt("employeeId"),
                        assetObject.getInt("assetGroupId"),
                        assetObject.getString("description"),
                        assetObject.getString("warrantyDate"),
                        assetObject.getString("readDate"),
                        assetObject.getString("odometerAmount"),
                    )
                    assetsList.add(asset)
                }

                return assetsList
            }

            con.disconnect()
        } catch (e: Exception) {
            return null
        }

        return null


    }
}