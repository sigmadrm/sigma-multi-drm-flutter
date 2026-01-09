// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.source.MediaSource;

final class RtspVideoAsset extends VideoAsset {
  RtspVideoAsset(@NonNull String assetUrl) {
    super(assetUrl);
  }

  @NonNull
  @Override
  public MediaItem getMediaItem() {
    return new MediaItem.Builder().setUri(assetUrl).build();
  }

  // TODO: Migrate to stable API, see https://github.com/flutter/flutter/issues/147039.
  @Override
  @NonNull
  public MediaSource.Factory getMediaSourceFactory(@NonNull Context context) {
    return new RtspMediaSource.Factory();
  }
}
