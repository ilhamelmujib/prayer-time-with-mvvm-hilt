package id.ilhamelmujib.prayertime.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import id.ilhamelmujib.prayertime.R
import id.ilhamelmujib.prayertime.receiver.PrayAlarmActionReceiver
import id.ilhamelmujib.prayertime.ui.component.DashboardActivity
import id.ilhamelmujib.prayertime.utils.*
import id.ilhamelmujib.prayertime.utils.praytimes.AppSettings

class PrayAlarmService : Service(), MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    companion object {
        private const val TAG = "msm.AdhanService"
        private const val EXTRA_KEY = "extra_key"
        private const val EXTRA_TIME = "extra_time"
        private const val EXTRA_ACTION = "extra_id"

        private const val RECEIVER_ACTION = "receiver_action"
        private const val ACTION_SNOOZE = "action_snooze"

        fun startService(context: Context, key: String, time: String) {
            context.apply {
                val intent = Intent(this, PrayAlarmService::class.java).apply {
                    val bundle = bundleOf(
                        EXTRA_KEY to key,
                        EXTRA_TIME to time
                    )
                    putExtras(bundle)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }

        fun sendBroadcast(context: Context, intent: Intent) {
            val intentFilter = Intent(RECEIVER_ACTION).apply {
                putExtra(EXTRA_ACTION, intent.action)
            }
            context.sendBroadcast(intentFilter)
        }
    }

    private val settings by lazy { AppSettings.getInstance() }
    private val player by lazy { MediaPlayer() }

    private var mNotificationManager: NotificationManager? = null
    private var key = ""
    private var time = ""
    private var title = ""
    private var notificationType = 0
    private var notificationId = -1

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registerReceiver(receiverAction, IntentFilter(RECEIVER_ACTION))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras?.let {
            key = it.getString(EXTRA_KEY).notNull()
            time = it.getString(EXTRA_TIME).notNull()
            notificationType = settings.getInt(ALARM_FOR + key)
            notificationId = generateRandom()
            val position = KEYS.indexOf(key)
            title = resources.getStringArray(R.array.title_prayer_time)[position]
            logDebug(TAG, "onStartCommand= key: $key, time: $time")
            sendNotification()
        }
        return START_NOT_STICKY
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        logDebug(TAG, "WHAT: $what EXTRA: $extra")
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(notificationId)
        }
        sendNotification(false)
    }

    override fun onPrepared(mp: MediaPlayer?) {
        player.start()
    }

    @SuppressLint("RestrictedApi")
    private fun sendNotification(ongoing: Boolean = true) {
        val soundUri = if (notificationType == USE_ADHAN) {
            if (key == FAJR)
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + R.raw.adhan_fajr_trimmed)
            else
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + R.raw.adhan_trimmed)
        } else {
            null
        }

        val intent = Intent(baseContext, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_FROM_NOTIFICATION_ADHAN, true)
        }

        val resultPendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val autoCancel = !(ongoing && soundUri != null)

        mNotificationManager = getSystemService<NotificationManager>()?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(key, title, importance).apply {
                    description = "channel $title description"
                }
                createNotificationChannel(channel)
            }
        }

        var text = "Lihat informasi waktu sholat"
        val lastLoc: String? = settings.getString(AppSettings.Key.LAST_LOC)
        if (!lastLoc.isNullOrEmpty()) {
            text += " di $lastLoc"
        }

        val notification = NotificationCompat.Builder(this, key)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(Color.TRANSPARENT)
            .setAutoCancel(autoCancel)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setContentTitle("Waktunya $title pukul $time")
            .setContentText(text)
            .setContentIntent(resultPendingIntent)
            .setOngoing(!autoCancel)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (!autoCancel) {
            val stopIntent =
                Intent(this, PrayAlarmActionReceiver::class.java).setAction(
                    ACTION_SNOOZE
                )
            val stopPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(this, 0, stopIntent, 0)
            notification.apply {
                priority = NotificationCompat.PRIORITY_HIGH
                addAction(R.drawable.ic_stop_alarm, getString(R.string.text_stop), stopPendingIntent)
            }

            soundUri?.let { prepareAdhan(it) }
        } else {
            try {
                notification.apply {
                    stop()
                    if (notificationType == USE_VIBRATE) vibrate()
                    priority = NotificationCompat.PRIORITY_LOW
                    mActions.clear()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!autoCancel && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(notificationId, notification.build())
        } else {
            mNotificationManager?.notify(notificationId, notification.build())
        }
    }

    private fun prepareAdhan(soundUri: Uri) {
        player.apply {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            setAudioAttributes(audioAttributes)
            setOnCompletionListener(this@PrayAlarmService)
            setOnPreparedListener(this@PrayAlarmService)
            setOnErrorListener(this@PrayAlarmService)
            reset()
            setVolume(1.0f, 1.0f)
            try {
                setDataSource(applicationContext, soundUri)
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    private val receiverAction = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.extras?.getString(EXTRA_ACTION)?.let {
                onCompletion(player)
            }
        }
    }

    private fun stop() {
        if (player.isPlaying) {
            player.stop()
        }
    }

    private fun vibrate() {
        val mVibrator = getSystemService<Vibrator>()
        if (mVibrator != null) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val time = 1000L
            if (Build.VERSION.SDK_INT >= 26) {
                val effect = VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE)
                mVibrator.vibrate(effect, audioAttributes)
            } else {
                mVibrator.vibrate(time, audioAttributes)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onCompletion(player)
        unregisterReceiver(receiverAction)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mNotificationManager?.cancelAll()
    }

}