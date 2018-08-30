// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.firebasemessaging;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Random;

public class FlutterFirebaseMessagingService extends FirebaseMessagingService {

  public static final String ACTION_REMOTE_MESSAGE =
      "io.flutter.plugins.firebasemessaging.NOTIFICATION";
  public static final String EXTRA_REMOTE_MESSAGE = "notification";

  private NotificationManager notificationManager;
  private static final String CHANNEL_ID = "media_playback_channel";
  private static final String SELECT_NOTIFICATION = "SELECT_NOTIFICATION";

  /**
   * Called when message is received.
   *
   * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
   */
  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    Intent intent = new Intent(ACTION_REMOTE_MESSAGE);
    intent.putExtra(EXTRA_REMOTE_MESSAGE, remoteMessage);
    if(isMainActivityRunning("com.shuttertop.android"))
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    else {
        Log.i("MessagingService", "NOTIFICAAAAA");


        int notificationId = new Random().nextInt(60000);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setupChannels();
        }

        Resources resources = getApplicationContext().getResources();
        final int resourceId = resources.getIdentifier("ic_stat_camera", "drawable",
                getApplicationContext().getPackageName());
        int color = Color.parseColor("#E91E63");

        intent = new Intent(getApplicationContext(), getMainActivityClass(getApplicationContext()));
        intent.setAction(SELECT_NOTIFICATION);
        intent.putExtra("NOTIFICATION", remoteMessage);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(resourceId)
                .setColor(color)
                .setContentTitle(remoteMessage.getData().get("title") == null ?
                        "Shuttertop" : remoteMessage.getData().get("title"))
                .setContentText(remoteMessage.getData().get("message"))
                .setContentIntent(pendingIntent)//ditto
                .setAutoCancel(true)  //dismisses the notification on click
                .setSound(defaultSoundUri);

        try {
            String upload = remoteMessage.getData().get("user_upload");
            Log.i("MessagingService", "user_upload: " + upload);
            if (upload != null) {
                URL url = new URL("https://img.shuttertop.com/70s70/" + upload);
                Bitmap image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                notificationBuilder.setLargeIcon(image);
            }
        } catch (IOException e) {
            Log.e("MessagingService", "Errore user_upload: " + e.getMessage());
        }


        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId /* ID of notification */, notificationBuilder.build());
    }
  }

  public boolean isMainActivityRunning(String packageName) {
    try {

      ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
      List<ActivityManager.RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(Integer.MAX_VALUE);

      for (int i = 0; i < tasksInfo.size(); i++) {
        Log.i("MessagingService", "isMainActivityRunning: " + tasksInfo.get(i).baseActivity.getPackageName());
        if (tasksInfo.get(i).baseActivity.getPackageName().equals(packageName))
          return true;
      }
    } catch (Exception ex) {
      Log.e("MessagingService", "isMainActivityRunning: " + ex.getMessage());
    }

    return false;
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private void setupChannels(){
    CharSequence adminChannelName = "SHUTTERTOP_CHANNEL";
    String adminChannelDescription = "Shuttertop notifiche";

    NotificationChannel adminChannel;
    adminChannel = new NotificationChannel(CHANNEL_ID, adminChannelName, NotificationManager.IMPORTANCE_LOW);
    adminChannel.setDescription(adminChannelDescription);
    adminChannel.enableLights(true);
    adminChannel.setLightColor(Color.RED);
    adminChannel.enableVibration(true);
    if (notificationManager != null) {
      notificationManager.createNotificationChannel(adminChannel);
    }
  }

  private static Class getMainActivityClass(Context context) {
    String packageName = context.getPackageName();
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
    String className = launchIntent.getComponent().getClassName();
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }


}
