/*
 * Copyright (C) 2018 Mozart Alexander Louis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prodbymozart.workmanager.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import com.prodbymozart.workmanager.Constants;
import com.prodbymozart.workmanager.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import static com.prodbymozart.workmanager.Constants.CHANNEL_ID;
import static com.prodbymozart.workmanager.Constants.DELAY_TIME_MILLIS;

public final class WorkerUtils {
    private static final String TAG = WorkerUtils.class.getSimpleName();

    /**
     * Create a Notification that is shown as a heads-up notification if possible.
     *
     * @param message ~ Message shown on the notification.
     * @param context ~ Context needed to create Toast.
     */
    public static void makeStatusNotification(String message, Context context) {
        // Create the NotificationChannel, but only on API 26+ because the NotificationChannel
        // class is new and not in the support library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final CharSequence name = Constants.VERBOSE_NOTIFICATION_CHANNEL_NAME;
            final String description = Constants.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION;
            final int importance = NotificationManager.IMPORTANCE_HIGH;
            final NotificationChannel channel =
                    new NotificationChannel(Constants.CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Add the channel
            final NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create the notification
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(Constants.NOTIFICATION_TITLE)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(new long[0]);

        // Show the notification
        NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_ID, builder.build());
    }

    /**
     * Method for sleeping for a fixed about of time to emulate slower work
     */
    static void sleep() {
        sleep(DELAY_TIME_MILLIS);
    }

    /**
     * Method for sleeping for a fixed about of time to emulate slower work
     */
    static void sleep(final long amount) {
        try {
            Thread.sleep(amount, 0);
        } catch (InterruptedException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * Blurs the given Bitmap image
     *
     * @param bitmap ~ Image to blur
     * @param context ~ Application context
     * @return Blurred bitmap image
     */
    @WorkerThread
    static Bitmap blurBitmap(@NonNull Bitmap bitmap, @NonNull Context context) {
        RenderScript rsContext = null;
        try {
            // Create the output bitmap
            final Bitmap output = Bitmap.createBitmap(
                    bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
            rsContext = RenderScript.create(context, RenderScript.ContextType.DEBUG);

            // Blur the image
            final Allocation inAlloc = Allocation.createFromBitmap(rsContext, bitmap);
            final Allocation outAlloc = Allocation.createTyped(rsContext, inAlloc.getType());
            final ScriptIntrinsicBlur theIntrinsic =
                    ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext));
            theIntrinsic.setRadius(3.0f);
            theIntrinsic.setInput(inAlloc);
            theIntrinsic.forEach(outAlloc);
            outAlloc.copyTo(output);
            return output;
        } finally {
            if (rsContext != null) {
                rsContext.finish();
            }
        }
    }

    /**
     * Writes bitmap to a temporary file and returns the Uri for the file
     *
     * @param applicationContext Application context
     * @param bitmap Bitmap to write to temp file
     * @return Uri for temp file with bitmap
     */
    static Uri writeBitmapToFile(@NonNull Context applicationContext, @NonNull Bitmap bitmap,
            int number) {
        // final String name = String.format("blur-output-%s.jpg", String.valueOf(number));
        final String name = String.format("blur-output-%s.jpg", UUID.randomUUID().toString());
        final File outputDir = new File(applicationContext.getFilesDir(), Constants.OUTPUT_PATH);

        if (!outputDir.exists()) outputDir.mkdirs();
        final File outputFile = new File(outputDir, name);

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Uri.fromFile(outputFile);
    }

    // Disallow instantiation
    private WorkerUtils() {
    }
}