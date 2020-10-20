package id.ilhamelmujib.prayertime.data.model

import com.google.gson.annotations.SerializedName

class Pray(
    var key: String = "",
    var name: String = "",
    var time: String? = "",
    @SerializedName("notification_type")
    var notificationType: Int = 0,
    @SerializedName("reminder_before")
    var reminderBefore: Int = 0
)