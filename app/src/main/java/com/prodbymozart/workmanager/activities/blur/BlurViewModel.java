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

package com.prodbymozart.workmanager.activities.blur;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.net.Uri;
import android.text.TextUtils;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;
import com.prodbymozart.workmanager.Constants;
import com.prodbymozart.workmanager.workers.BlurWorker;
import com.prodbymozart.workmanager.workers.CleanupWorker;
import com.prodbymozart.workmanager.workers.SaveWorker;

import java.util.List;

import static com.prodbymozart.workmanager.Constants.IMAGE_MANIPULATION_WORK_NAME;
import static com.prodbymozart.workmanager.Constants.KEY_IMAGE_URI;
import static com.prodbymozart.workmanager.Constants.KEY_SHOW_NOTIFICATION;
import static com.prodbymozart.workmanager.Constants.SAVE_IMAGE;

public class BlurViewModel extends ViewModel {

    private Uri mImageUri;
    private Uri mOutputUri;
    private WorkManager mWorkManager;

    /**
     * {@link LiveData}
     *
     * Live data that listens to status updates by the tag {@link Constants#SAVE_IMAGE}
     */
    private LiveData<List<WorkStatus>> mSavedWorkStatus;

    /**
     * Constructor.
     */
    public BlurViewModel() {
        mWorkManager = WorkManager.getInstance();
        mSavedWorkStatus = mWorkManager.getStatusesByTag(SAVE_IMAGE);
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     *
     */
    void applyBlur1() {
        // Create a BlurWorker to blur the image for us
        OneTimeWorkRequest blurRequest = new OneTimeWorkRequest.Builder(BlurWorker.class)
                .setInputData(createDataForBlur())
                .build();

        mWorkManager.enqueue(blurRequest);
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     *
     * This example is using a {@link WorkContinuation}. Which allows you to chain multiple Workers
     * One after another
     *
     */
    void applyBlur2() {
        // Using WorkContinuation
        WorkContinuation continuation = mWorkManager
                .beginWith(new OneTimeWorkRequest.Builder(CleanupWorker.class).build())
                .then(new OneTimeWorkRequest.Builder(BlurWorker.class).setInputData(
                        createDataForBlur()).build());

        // Queue Work
        continuation.enqueue();
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     *
     * @param blurLevel The amount to blur the image
     */
    void applyBlur3(int blurLevel) {
        // Creating a WorkContinuation chain that allows multiple workers to be invoked one after
        // another. Initializing it with a CleanWorker.
        WorkContinuation continuation =
                mWorkManager.beginUniqueWork(IMAGE_MANIPULATION_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        new OneTimeWorkRequest.Builder(CleanupWorker.class).build());

        // BlurWorker: Add WorkRequests to blur the image the number of times requested. This is not
        // The best way of doing this but simply to defenestrate multiple workers of the same kind
        for (int i = 0; i < blurLevel; i++) {
            OneTimeWorkRequest.Builder blurBuilder =
                    new OneTimeWorkRequest.Builder(BlurWorker.class);

            // Input the Uri if this is the first blur operation. After the first blur operation
            // the input will be the output of previous blur operations.
            if (i == 0) {
                // Create a new Data builder and add attributes to it
                Data.Builder builder = new Data.Builder();
                if (mImageUri != null) builder.putString(KEY_IMAGE_URI, mImageUri.toString());
                builder.putBoolean(KEY_SHOW_NOTIFICATION, true);
                blurBuilder.setInputData(builder.build());
            }

            continuation = continuation.then((blurBuilder.build()));
        }

        // SaveWorker: Create create a save worker that save the file to your device. You can add
        // constraints to the worker to only run when they are meet.
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresStorageNotLow(true).build();

        // Adding a tag to allow LiveData observers to receives a notification when this worker
        // is finished.
        OneTimeWorkRequest save = new OneTimeWorkRequest.Builder(SaveWorker.class)
                .addTag(SAVE_IMAGE)
                .setConstraints(constraints).build();

        // Create WorkContinuation and actually enqueue work.
        continuation = continuation.then(save);

        // Queue the chain worker
        continuation.enqueue();
    }

    /**
     * Create data used for the BlurWorker
     */
    private Data createDataForBlur() {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null) builder.putString(KEY_IMAGE_URI, mImageUri.toString());
        builder.putBoolean(KEY_SHOW_NOTIFICATION, true);
        return builder.build();
    }

    /**
     * Cancel work using the work's unique name
     */
    void cancelWork() {
        mWorkManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME);
    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * Setter for {@link BlurViewModel#mImageUri}
     */
    void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

    /**
     * Setter for {@link BlurViewModel#mOutputUri}
     */
    public void setOutputUri(String outputImageUri) {
        mOutputUri = uriOrNull(outputImageUri);
    }

    /**
     * Getter for {@link BlurViewModel#mImageUri}
     *
     * @return {@link BlurViewModel#mImageUri}
     */
    Uri getImageUri() {
        return mImageUri;
    }

    /**
     * Getter for {@link BlurViewModel#mOutputUri}
     *
     * @return {@link BlurViewModel#mOutputUri}
     */
    public Uri getOutputUri() {
        return mOutputUri;
    }

    /**
     * Getter for {@link BlurViewModel#mSavedWorkStatus}
     *
     * @return {@link BlurViewModel#mSavedWorkStatus}
     */
    LiveData<List<WorkStatus>> getOutputStatus() {
        return mSavedWorkStatus;
    }
}