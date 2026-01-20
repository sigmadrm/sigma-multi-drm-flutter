// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.sigma.packer.SigmaMediaDrm;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.Map;
import java.util.UUID;

final class HttpVideoAsset extends VideoAsset {
  @NonNull private final StreamingFormat streamingFormat;
  @NonNull private final Map<String, String> httpHeaders;
  @NonNull private final Map<String, String> drmConfiguration;
  @Nullable private final String userAgent;

  private final String DEFAULT_LICENSE_SERVER_URL = "https://license.sigmadrm.com/license/verify/widevine";

  private DataSource.Factory dataSourceFactory = null;

  HttpVideoAsset(
      @Nullable String assetUrl,
      @NonNull StreamingFormat streamingFormat,
      @NonNull Map<String, String> httpHeaders,
      @NonNull Map<String, String> drmConfiguration,
      @Nullable String userAgent) {
    super(assetUrl);
    this.streamingFormat = streamingFormat;
    this.httpHeaders = httpHeaders;
    this.drmConfiguration = drmConfiguration;
    this.userAgent = userAgent;
    this.dataSourceFactory = null;
  }

  @NonNull
  @Override
  public MediaItem getMediaItem() {
    MediaItem.Builder builder = new MediaItem.Builder().setUri(assetUrl);
    String mimeType = null;
    switch (streamingFormat) {
      case SMOOTH:
        mimeType = MimeTypes.APPLICATION_SS;
        break;
      case DYNAMIC_ADAPTIVE:
        mimeType = MimeTypes.APPLICATION_MPD;
        break;
      case HTTP_LIVE:
        mimeType = MimeTypes.APPLICATION_M3U8;
        break;
    }
    if (mimeType != null) {
      builder.setMimeType(mimeType);
    }
    return builder.build();
  }

  @NonNull
  @Override
  public MediaSource.Factory getMediaSourceFactory(@NonNull Context context) {
    return getMediaSourceFactory(context, new DefaultHttpDataSource.Factory());
  }

  /**
   * Returns a configured media source factory, starting at the provided factory.
   *
   * <p>This method is provided for ease of testing without making real HTTP calls.
   *
   * @param context application context.
   * @param initialFactory initial factory, to be configured.
   * @return configured factory, or {@code null} if not needed for this asset type.
   */
  @VisibleForTesting
  MediaSource.Factory getMediaSourceFactory(
          Context context, DefaultHttpDataSource.Factory initialFactory) {
    unstableUpdateDataSourceFactory(initialFactory, httpHeaders, userAgent);
    dataSourceFactory = new DefaultDataSource.Factory(context, initialFactory);
    return new DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)
            .setDrmSessionManagerProvider(this::createDrmSessionManager);
  }

  private DrmSessionManager createDrmSessionManager(MediaItem mediaItem) {
    DefaultDrmSessionManager drmSessionManager;
    if (Util.SDK_INT >= 18) {
      UUID drmSchemeUuid = Assertions.checkNotNull(C.WIDEVINE_UUID);
      String licenseServerUrl = drmConfiguration.get("licenseServerUrl") != null ? drmConfiguration.get("licenseServerUrl") : DEFAULT_LICENSE_SERVER_URL;
      byte[] offlineLicenseKeySetId = null;
      assert mediaItem.localConfiguration != null;
      MediaItem.DrmConfiguration drmConfiguration = mediaItem.localConfiguration.drmConfiguration;
      if (drmConfiguration != null) {
        offlineLicenseKeySetId = drmConfiguration.getKeySetId();
      }
      MediaDrmCallback drmCallback = createMediaDrmCallback(licenseServerUrl, null);
      drmSessionManager = new DefaultDrmSessionManager.Builder()
              .setMultiSession(true)
              .setUuidAndExoMediaDrmProvider(drmSchemeUuid, SigmaMediaDrm.DEFAULT_PROVIDER)
              .build(drmCallback);
      drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, offlineLicenseKeySetId);
    } else {
      drmSessionManager = (DefaultDrmSessionManager) DrmSessionManager.DRM_UNSUPPORTED;
    }

    return drmSessionManager;
  }

  private WidevineMediaDrmCallback createMediaDrmCallback(String licenseUrl, String[] keyRequestPropertiesArray) {
    WidevineMediaDrmCallback drmCallback = new WidevineMediaDrmCallback(licenseUrl, dataSourceFactory);
    if (keyRequestPropertiesArray != null) {
      for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
        drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                keyRequestPropertiesArray[i + 1]);
      }
    }
    return drmCallback;
  }

  // TODO: Migrate to stable API, see https://github.com/flutter/flutter/issues/147039.
  private static void unstableUpdateDataSourceFactory(
      @NonNull DefaultHttpDataSource.Factory factory,
      @NonNull Map<String, String> httpHeaders,
      @Nullable String userAgent) {
    if(userAgent != null && !userAgent.isEmpty()) {
      factory.setUserAgent(userAgent);
    }
    factory.setAllowCrossProtocolRedirects(true);
    if (!httpHeaders.isEmpty()) {
      factory.setDefaultRequestProperties(httpHeaders);
    }
  }
}
