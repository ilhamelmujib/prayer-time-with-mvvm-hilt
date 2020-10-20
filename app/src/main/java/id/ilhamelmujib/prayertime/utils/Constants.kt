package id.ilhamelmujib.prayertime.utils

// REQUEST CODES
const val REQUEST_LOCATION = 100

//PRAYER TIMES
const val ALARM_FOR = "alarm_for_"
const val ALARM_REMINDER_FOR = "alarm_reminder_for_"
const val LOCATION_FRAGMENT = "location_fragment"
const val USE_ADHAN = 0
const val USE_VIBRATE = 1
const val USE_SILENT = 2
const val USE_NONACTIVE = 3
const val EXTRA_FROM_NOTIFICATION_ADHAN = "from_notification_adhan"

const val IMSAK = "imsak"
const val FAJR = "fajr"
const val SUNRISE = "sunrise"
const val SUNSET = "sunset"
private const val DHUHR = "dhuzur"
private const val ASR = "asr"
private const val MAGHRIB = "maghrib"
private const val ISHA = "isha"
val KEYS = arrayOf(IMSAK, FAJR, SUNRISE, DHUHR, ASR, SUNSET, MAGHRIB, ISHA)


private const val ONE_MINUTE: Long = 60000
const val THREE_MINUTES = ONE_MINUTE * 3
const val FIVE_MINUTES = ONE_MINUTE * 5
