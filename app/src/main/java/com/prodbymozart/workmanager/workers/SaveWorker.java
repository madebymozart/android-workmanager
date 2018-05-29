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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.prodbymozart.workmanager.Constants.KEY_IMAGE_URI;

public class SaveWorker extends Worker {
    private static final String TAG = SaveWorker.class.getSimpleName();
    private static final String TITLE = "Blurred Image";

    /**
     * Suppress since Android Studio will complain that we shouldn't be using a final here
     */
    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z", Locale.getDefault());

    @NonNull
    @Override
    public WorkerResult doWork() {
        final Context context = getApplicationContext();
        final ContentResolver resolver = context.getContentResolver();
        final String resourceUri = getInputData().getString(KEY_IMAGE_URI, null);

        // Simulate long running process
        WorkerUtils.makeStatusNotification("Saving Image...", context);
        WorkerUtils.sleep();

        try {
            final Bitmap bitmap = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri)));
            final String imageUrl = MediaStore.Images.Media.insertImage(resolver, bitmap, TITLE,
                    DATE_FORMATTER.format(new Date()));

            // Fail the worker if the imageUrl is null.
            if (TextUtils.isEmpty(imageUrl)) {
                Log.e(TAG, "Writing to MediaStore failed");
                return WorkerResult.FAILURE;
            }

            // Saving the imageUri to the output data. The view model should get notified since
            // the live data is observing the tag associated with this worker.
            final Data data = new Data.Builder().putString(KEY_IMAGE_URI, imageUrl).build();
            setOutputData(data);

            // Worker was successful
            Log.d(TAG, "Worker was successful!");
            return WorkerResult.SUCCESS;
        } catch (Exception exception) {
            Log.e(TAG, "Unable to save image to gallery...", exception);
            return WorkerResult.FAILURE;
        }
    }
}