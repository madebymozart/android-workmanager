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

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import androidx.work.Worker;
import com.prodbymozart.workmanager.Constants;

import java.io.File;

public class CleanupWorker extends Worker {
    private static final String TAG = CleanupWorker.class.getSimpleName();

    @NonNull
    @Override
    public WorkerResult doWork() {
        final Context context = getApplicationContext();

        // Intentionally slowing down work to simulate long process
        WorkerUtils.makeStatusNotification("Doing Cleanup", context);
        WorkerUtils.sleep();

        try {
            File outputDirectory = new File(context.getFilesDir(), Constants.OUTPUT_PATH);
            if (!outputDirectory.exists()) return WorkerResult.SUCCESS;

            // Retrieve the list of files
            final File[] entries = outputDirectory.listFiles();
            if (entries == null || entries.length == 0) return WorkerResult.SUCCESS;

            for (File entry : entries) {
                // Verify the file ends with .jpg
                final String name = entry.getName();
                if (TextUtils.isEmpty(name) || !name.endsWith(".jpg")) continue;

                // Delete file.
                boolean deleted = entry.delete();
                Log.i(TAG, String.format("Deleted %s - %s", name, deleted));
            }

            Log.d(TAG, "Worker was successful!");
            return WorkerResult.SUCCESS;
        } catch (Exception exception) {
            Log.e(TAG, "Error cleaning up...", exception);
            return Worker.WorkerResult.FAILURE;
        }
    }
}