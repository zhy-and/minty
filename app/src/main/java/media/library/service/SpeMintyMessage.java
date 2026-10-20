package media.library.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.LogUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rapid.prominence.minty.R;
import com.rapid.prominence.minty.runtime.MintyRuntime;
import com.rapid.prominence.minty.view.AppStart;


public class SpeMintyMessage extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        try {
            MintyRuntime.onFcmMessage(this, message);
        } catch (Exception error) {
            LogUtils.e("SpeMintyMessage", "Runtime wake failed: " + error.getMessage());
        }
        try {
            if (message.getNotification() != null && !TextUtils.isEmpty(message.getNotification().getTitle()) && !TextUtils.isEmpty(message.getNotification().getBody())) {
                createNotification(message.getNotification().getTitle(), message.getNotification().getBody(), message.getNotification().getTitle());
            }

        }catch (Exception e ){
            e.printStackTrace();
        }


    }



    @Override
    public void onNewToken(@NonNull String token) {
        LogUtils.e("测试", "onNewToken =" + token);
        MintyRuntime.bootstrap(this, MintyRuntime.Trigger.MANUAL, false);
    }
    private String chanleId = "10000";
    private String chanleName = "push_music";
    /**
     * 自定义通知
     *
     * @param messageBody
     */
    public void createNotification(String messageTitle, String messageBody, String title) {
            try {

                Intent intent = new Intent(this, AppStart.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                String channelID = chanleId;//渠道ID
                String channelName = chanleName;//渠道名称

                int uniqueInt = (int) (System.currentTimeMillis() & 0xff);
                PendingIntent pendingIntent;
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  pendingIntent =    PendingIntent.getActivity(this, uniqueInt,  intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE );  }
                else {
                  pendingIntent =  PendingIntent.getActivity(this,
                        uniqueInt, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                }

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                NotificationCompat.Builder notificationBuilder;
                //android 8 开始要 创建通知渠道，否则通知栏不弹出
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationBuilder = new NotificationCompat.Builder(this, channelID);
                    NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_HIGH);
                    notificationManager.createNotificationChannel(channel);
                } else {
                    notificationBuilder = new NotificationCompat.Builder(this);
                }

                //设置title
                if (messageTitle != null && !messageTitle.isEmpty()) {
                    notificationBuilder.setContentTitle(messageTitle);
                } else {
                    notificationBuilder.setContentTitle(getResources().getString(R.string.app_name));
                }

                //设置body
                if (messageBody != null && !messageBody.isEmpty()) {
                    notificationBuilder.setContentText(messageBody);
                }
//        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                notificationBuilder
                        .setSmallIcon(R.mipmap.minty_icon)//设置通知栏的小图标,必需设置，否则crash
                        .setAutoCancel(false)//点击通知后，通知自动消失
                        .setWhen(System.currentTimeMillis())// 设置通知时间，此事件用于通知栏排序
                        .setPriority(NotificationCompat.PRIORITY_HIGH)// 设置优先级，低优先级可能被隐藏
//                .setSound(defaultSoundUri)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setContentIntent(pendingIntent);//设置通知栏被点击时的事件

                notificationManager.notify(uniqueInt /* ID of notification */, notificationBuilder.build());
            }catch (Exception e){
                e.printStackTrace();
            }


    }
}
