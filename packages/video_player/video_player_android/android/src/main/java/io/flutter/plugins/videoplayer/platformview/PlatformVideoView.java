// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer.platformview;

import android.content.Context;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayer;

import io.flutter.Log;
import io.flutter.plugin.platform.PlatformView;

/**
 * A class used to create a native video view that can be embedded in a Flutter app. It wraps an
 * {@link ExoPlayer} instance and displays its video content.
 */
public final class PlatformVideoView implements PlatformView {
  private static final String TAG = "PlatformVideoView";
  @NonNull private final SurfaceView surfaceView;

  /**
   * Constructs a new PlatformVideoView.
   *
   * @param context The context in which the view is running.
   * @param exoPlayer The ExoPlayer instance used to play the video.
   */
  public PlatformVideoView(@NonNull Context context, @NonNull ExoPlayer exoPlayer) {
    Log.e(TAG, "PlatformVideoView Constructor: creating " + this.hashCode());
    this.surfaceView = new SurfaceView(context);

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
      // Avoid blank space instead of a video on Android versions below 8 by adjusting video's
      // z-layer within the Android view hierarchy:
      surfaceView.setZOrderMediaOverlay(true);
    }

    surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
      @Override
      public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.e(TAG, "PlatformVideoView surfaceCreated for " + PlatformVideoView.this.hashCode());
        connectPlayer(exoPlayer, holder);
      }

      @Override
      public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "PlatformVideoView surfaceChanged [" + width + "x" + height + "] for " + PlatformVideoView.this.hashCode());
        connectPlayer(exoPlayer, holder);
      }

      @Override
      public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.e(TAG, "PlatformVideoView surfaceDestroyed for " + PlatformVideoView.this.hashCode());
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
          exoPlayer.setVideoSurface(null);
        }
      }
    });
  }

  private void connectPlayer(@NonNull ExoPlayer exoPlayer, @NonNull SurfaceHolder holder) {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
      // Workaround for rendering issues on Android 9 (API 28).
      exoPlayer.setVideoSurface(holder.getSurface());
      // Force first frame rendering:
      exoPlayer.seekTo(1);
    } else {
      exoPlayer.setVideoSurfaceView(surfaceView);
    }
  }

  /**
   * Returns the view associated with this PlatformView.
   *
   * @return The SurfaceView used to display the video.
   */
  @NonNull
  @Override
  public View getView() {
    return surfaceView;
  }

  /** Disposes of the resources used by this PlatformView. */
  @Override
  public void dispose() {
    Log.e(TAG, "PlatformVideoView dispose called for " + this.hashCode());
    surfaceView.getHolder().getSurface().release();
  }
}
