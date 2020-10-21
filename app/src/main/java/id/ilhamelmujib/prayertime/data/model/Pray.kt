package id.ilhamelmujib.prayertime.data.model

import com.google.gson.annotations.SerializedName

class Pray(
    var key: String = "",
    var name: String = "",
    var time: String? = "",
    var notificationType: Int = 0,
    var reminderBefore: Int = 0
)