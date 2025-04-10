package com.example.caliindar.util

import android.util.Log
import org.json.JSONObject
import org.json.JSONException


private const val TAG = "ErrorUtils" // Use a relevant tag

fun parseErrorDetail(responseBodyString: String?): String? {
    if (responseBodyString.isNullOrBlank()) {
        return null // Nothing to parse
    }

    return try {
        // --- Adapt this logic based on YOUR backend's error format ---
        // Example: Assuming JSON like {"detail": "Error message"} or {"message": "Error"}
        val jsonObject = JSONObject(responseBodyString)

        when {
            jsonObject.has("detail") -> jsonObject.getString("detail")
            jsonObject.has("message") -> jsonObject.getString("message")
            jsonObject.has("error_description") -> jsonObject.getString("error_description")
            jsonObject.has("error") -> {
                // Check if "error" itself is an object with a message
                val errorObj = jsonObject.optJSONObject("error")
                if (errorObj != null && errorObj.has("message")) {
                    errorObj.getString("message")
                } else {
                    // Otherwise, treat "error" as a string field
                    jsonObject.getString("error")
                }
            }
            // Add other potential error fields your backend might use
            else -> null // Could not find a known error detail field
        }
        // --- End of adaptable logic ---

    } catch (e: JSONException) {
        Log.w(TAG, "Response body was not valid JSON or field not found: $responseBodyString")
        // Optionally, return the raw string if it's not JSON but might be human-readable
        // return responseBodyString
        null // Parsing failed or field not found
    } catch (e: Exception) {
        Log.e(TAG,"Unexpected error parsing error detail: $responseBodyString", e)
        null // General parsing error
    }
}




