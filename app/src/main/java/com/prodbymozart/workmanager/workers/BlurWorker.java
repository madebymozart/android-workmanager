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

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;

import static com.prodbymozart.workmanager.Constants.KEY_BLUR_ITERATION;
import static com.prodbymozart.workmanager.Constants.KEY_IMAGE_URI;
import static com.prodbymozart.workmanager.Constants.KEY_SHOW_NOTIFICATION;

public class BlurWorker extends Worker {

    private static final String TAG = BlurWorker.class.getSimpleName();

    @NonNull
    @Override
    public WorkerResult doWork() {
        final Context context = getApplicationContext();
        final String resourceUri = getInputData().getString(KEY_IMAGE_URI, null);
        final boolean showNotification = getInputData().getBoolean(KEY_SHOW_NOTIFICATION, false);
        final int blurIteration = getInputData().getInt(KEY_BLUR_ITERATION, 0);
        final ContentResolver resolver = context.getContentResolver();

        // Intentionally slowing down work to simulate long process
        if (showNotification) {
            WorkerUtils.makeStatusNotification("Blurring Image: " + resourceUri, context);
        }

        try {
            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "Invalid input uri");
                throw new IllegalArgumentException("Invalid input uri");
            }

            // Create a bitmap
            Bitmap picture = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri)));

            // Blur the bitmap
            Bitmap output = WorkerUtils.blurBitmap(picture, context);

            // Write bitmap to a temp file
            Uri outputUri = WorkerUtils.writeBitmapToFile(context, output, blurIteration);

            // Set output data for the next blur agent to use. This allows us to make sure that same
            // uri is passed between the blur agents.
            setOutputData(new Data.Builder()
                    .putString(KEY_IMAGE_URI, outputUri.toString())
                    .putBoolean(KEY_SHOW_NOTIFICATION, false)
                    .putInt(KEY_BLUR_ITERATION, blurIteration + 1).build());

            // Worker was Successful
            Log.d(TAG, "Worker was successful!");
            return WorkerResult.SUCCESS;
        } catch (Throwable throwable) {

            // WorkManager will return WorkerResult.FAILURE by default, but it but it's best to be
            // explicit about it. Thus if there were errors, we're return FAILURE.
            Log.e(TAG, "Error applying blur", throwable);
            return WorkerResult.FAILURE;
        }
    }
}