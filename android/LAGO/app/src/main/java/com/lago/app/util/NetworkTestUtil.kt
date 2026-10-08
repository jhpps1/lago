package com.lago.app.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object NetworkTestUtil {
    
    suspend fun testNetworkConnection(): String = withContext(Dispatchers.IO) {
        try {
            val baseUrl = Constants.BASE_URL
            Log.d("NetworkTest", "Testing connection to: $baseUrl")
            
            val url = URL(baseUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage
            
            Log.d("NetworkTest", "Response Code: $responseCode")
            Log.d("NetworkTest", "Response Message: $responseMessage")
            
            val result = "Base URL: $baseUrl\nResponse Code: $responseCode\nResponse Message: $responseMessage"
            connection.disconnect()
            result
        } catch (e: IOException) {
            val errorMessage = "Network connection failed: ${e.message}"
            Log.e("NetworkTest", errorMessage, e)
            errorMessage
        } catch (e: Exception) {
            val errorMessage = "Unexpected error: ${e.message}"
            Log.e("NetworkTest", errorMessage, e)
            errorMessage
        }
    }
    
    suspend fun testTransactionApi(userId: Long): String = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "${Constants.BASE_URL}api/accounts/$userId/transactions"
            Log.d("NetworkTest", "Testing transaction API: $apiUrl")
            
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Content-Type", "application/json")
            
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage
            
            Log.d("NetworkTest", "Transaction API Response Code: $responseCode")
            Log.d("NetworkTest", "Transaction API Response Message: $responseMessage")
            
            val responseBody = if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
            }
            
            Log.d("NetworkTest", "Response Body: $responseBody")
            
            val result = "Transaction API URL: $apiUrl\nResponse Code: $responseCode\nResponse Message: $responseMessage\nResponse Body: $responseBody"
            connection.disconnect()
            result
        } catch (e: IOException) {
            val errorMessage = "Transaction API connection failed: ${e.message}"
            Log.e("NetworkTest", errorMessage, e)
            errorMessage
        } catch (e: Exception) {
            val errorMessage = "Transaction API unexpected error: ${e.message}"
            Log.e("NetworkTest", errorMessage, e)
            errorMessage
        }
    }
}