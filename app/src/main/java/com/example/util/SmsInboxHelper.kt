package com.example.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.example.data.model.SmsInboxItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsInboxHelper {
    private const val TAG = "SmsInboxHelper"

    suspend fun readDeviceInboxSms(context: Context, limit: Int = 100): List<SmsInboxItem> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<SmsInboxItem>()
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI

        val projection = arrayOf(
            Telephony.Sms.Inbox._ID,
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE,
            Telephony.Sms.Inbox.READ
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.Inbox.DATE} DESC LIMIT $limit"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(Telephony.Sms.Inbox._ID)
                val addressCol = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
                val bodyCol = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                val dateCol = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)
                val readCol = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.READ)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val address = it.getString(addressCol) ?: "Unknown"
                    val body = it.getString(bodyCol) ?: ""
                    val date = it.getLong(dateCol)
                    val read = it.getInt(readCol) == 1

                    smsList.add(
                        SmsInboxItem(
                            id = id,
                            address = address,
                            body = body,
                            date = date,
                            read = read,
                            isSelected = false
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing READ_SMS permission: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SMS inbox: ${e.message}", e)
        }

        return@withContext smsList
    }
}
