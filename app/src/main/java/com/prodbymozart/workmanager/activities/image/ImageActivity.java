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

package com.prodbymozart.workmanager.activities.image;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.prodbymozart.workmanager.Constants;
import com.prodbymozart.workmanager.R;
import com.prodbymozart.workmanager.activities.blur.BlurActivity;

import java.util.Arrays;
import java.util.List;

public class ImageActivity extends AppCompatActivity {
    private static final String TAG = ImageActivity.class.getName();
    private static final int REQUEST_CODE_IMAGE = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final int MAX_NUMBER_REQUEST_PERMISSIONS = 2;
    private static final String KEY_PERMISSIONS_REQUEST_COUNT = "KEY_PERMISSIONS_REQUEST_COUNT";
    private static final List<String> sPermissions = Arrays.asList(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    );

    private int mPermissionRequestCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        if (savedInstanceState != null) {
            mPermissionRequestCount =
                    savedInstanceState.getInt(KEY_PERMISSIONS_REQUEST_COUNT, 0);
        }

        // Make sure the app has correct permissions to run
        requestPermissionsIfNecessary();

        // Create request to get image from filesystem when button clicked
        findViewById(R.id.selectImage).setOnClickListener(view -> {
            Intent chooseIntent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(chooseIntent, REQUEST_CODE_IMAGE);
        });
    }

    /**
     * Save the permission request count on a rotate
     *
     * @param outState ~ {@link Bundle}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_PERMISSIONS_REQUEST_COUNT, mPermissionRequestCount);
    }

    /**
     * Request permissions twice.
     *
     * If the user denies twice, then show a toast about how to update the permission for storage.
     * Also disable the button if we don't have access to pictures on the device.
     */
    private void requestPermissionsIfNecessary() {
        // If we already have permission, do nothing.
        if (checkAllPermissions()) return;

        // Check the amount of times we have asked for permissions. If we have exceeded the limit,
        // then disable the button and show a toast
        if (mPermissionRequestCount >= MAX_NUMBER_REQUEST_PERMISSIONS) {
            Toast.makeText(this, R.string.set_permissions_in_settings,
                    Toast.LENGTH_LONG).show();
            findViewById(R.id.selectImage).setEnabled(false);
            return;
        }

        // Request the permissions
        mPermissionRequestCount += 1;
        ActivityCompat.requestPermissions(this, sPermissions.toArray(new String[0]),
                REQUEST_CODE_PERMISSIONS);
    }

    /**
     * Check if we have all permissions.
     * For this app to work correctly, we need all the permissions that are requested.
     *
     * @return ~ True if we have all permission, otherwise false.
     */
    private boolean checkAllPermissions() {
        boolean hasPermissions = true;

        // Check each requested permission.
        for (String permission : sPermissions) {
            hasPermissions &= ContextCompat.checkSelfPermission(
                    this, permission) == PackageManager.PERMISSION_GRANTED;
        }

        return hasPermissions;
    }

    /**
     * Permission Checking
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            requestPermissionsIfNecessary(); // no-op if permissions are granted already.
        }
    }

    /**
     * Image Selection
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            Log.e(TAG, String.format("Unexpected Result code %s", resultCode));
        }

        switch (requestCode) {
            case REQUEST_CODE_IMAGE:
                handleImageRequestResult(data);
                break;
            default:
                Log.d(TAG, "Unknown request code.");
        }
    }

    private void handleImageRequestResult(Intent data) {
        Uri imageUri = null;
        if (data.getClipData() != null) {
            imageUri = data.getClipData().getItemAt(0).getUri();
        } else if (data.getData() != null) {
            imageUri = data.getData();
        }

        if (imageUri == null) {
            Log.e(TAG, "Invalid input image Uri.");
            return;
        }

        Intent filterIntent = new Intent(this, BlurActivity.class);
        filterIntent.putExtra(Constants.KEY_IMAGE_URI, imageUri.toString());
        startActivity(filterIntent);
    }
}
