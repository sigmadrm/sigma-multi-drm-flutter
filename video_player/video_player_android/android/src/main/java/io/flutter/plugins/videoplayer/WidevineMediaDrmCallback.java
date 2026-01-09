package io.flutter.plugins.videoplayer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.drm.ExoMediaDrm.KeyRequest;
import com.google.android.exoplayer2.drm.ExoMediaDrm.ProvisionRequest;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallbackException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.StatsDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import com.sigma.drm.SigmaHelper;
import com.sigma.packer.RequestInfo;
import com.sigma.packer.SigmaDrmPacker;

/**
 * A {@link MediaDrmCallback} that makes requests using {@link HttpDataSource}
 * instances.
 */
@SuppressLint("ObsoleteSdkInt")
@TargetApi(18)
public final class WidevineMediaDrmCallback implements MediaDrmCallback {

  private static final int MAX_MANUAL_REDIRECTS = 5;

  private final DataSource.Factory dataSourceFactory;
  @Nullable
  private final String defaultLicenseUrl;
  private final boolean forceDefaultLicenseUrl;
  private final Map<String, String> keyRequestProperties;

  /**
   * Constructs an instance.
   *
   * @param defaultLicenseUrl The default license URL. Used for key requests that
   *                          do not specify
   *                          their own license URL. May be {@code null} if it's
   *                          known that all key requests will specify
   *                          their own URLs.
   * @param dataSourceFactory A factory from which to obtain {@link DataSource}
   *                          instances. This will
   *                          usually be an HTTP-based {@link DataSource}.
   */
  public WidevineMediaDrmCallback(
      @Nullable String defaultLicenseUrl, DataSource.Factory dataSourceFactory) {
    this(defaultLicenseUrl, /* forceDefaultLicenseUrl= */ false, dataSourceFactory);
  }

  /**
   * Constructs an instance.
   *
   * @param defaultLicenseUrl      The default license URL. Used for key requests
   *                               that do not specify
   *                               their own license URL, or for all key requests
   *                               if {@code forceDefaultLicenseUrl} is set to
   *                               true. May be {@code null} if
   *                               {@code forceDefaultLicenseUrl} is {@code false}
   *                               and if it's
   *                               known that all key requests will specify their
   *                               own URLs.
   * @param forceDefaultLicenseUrl Whether to force use of
   *                               {@code defaultLicenseUrl} for key
   *                               requests that include their own license URL.
   * @param dataSourceFactory      A factory from which to obtain
   *                               {@link DataSource} instances. This will
   *                               * usually be an HTTP-based {@link DataSource}.
   */
  public WidevineMediaDrmCallback(
      @Nullable String defaultLicenseUrl,
      boolean forceDefaultLicenseUrl,
      DataSource.Factory dataSourceFactory) {
    Assertions.checkArgument(!(forceDefaultLicenseUrl && TextUtils.isEmpty(defaultLicenseUrl)));
    this.dataSourceFactory = dataSourceFactory;
    this.defaultLicenseUrl = defaultLicenseUrl;
    this.forceDefaultLicenseUrl = forceDefaultLicenseUrl;
    this.keyRequestProperties = new HashMap<>();
  }

  /**
   * Sets a header for key requests made by the callback.
   *
   * @param name  The name of the header field.
   * @param value The value of the field.
   */
  public void setKeyRequestProperty(String name, String value) {
    Assertions.checkNotNull(name);
    Assertions.checkNotNull(value);
    synchronized (keyRequestProperties) {
      keyRequestProperties.put(name, value);
    }
  }

  /**
   * Clears a header for key requests made by the callback.
   *
   * @param name The name of the header field.
   */
  public void clearKeyRequestProperty(String name) {
    Assertions.checkNotNull(name);
    synchronized (keyRequestProperties) {
      keyRequestProperties.remove(name);
    }
  }

  /**
   * Clears all headers for key requests made by the callback.
   */
  public void clearAllKeyRequestProperties() {
    synchronized (keyRequestProperties) {
      keyRequestProperties.clear();
    }
  }

  @Override
  public byte[] executeProvisionRequest(UUID uuid, ProvisionRequest request)
      throws MediaDrmCallbackException {
    String url = request.getDefaultUrl() + "&signedRequest=" + Util.fromUtf8Bytes(request.getData());
    return executePost(
        dataSourceFactory,
        url,
        /* httpBody= */ null,
        /* requestProperties= */ Collections.emptyMap());
  }

  @Override
  public byte[] executeKeyRequest(UUID uuid, KeyRequest request) throws MediaDrmCallbackException {
    try {
      String url = request.getLicenseServerUrl();
      if (forceDefaultLicenseUrl || TextUtils.isEmpty(url)) {
        url = defaultLicenseUrl;
      }
      Map<String, String> requestProperties = new HashMap<>();
      // Add standard request properties for supported schemes.
      String contentType = "application/octet-stream";
      requestProperties.put("Content-Type", contentType);
      requestProperties.put("custom-data", getCustomData(request));

      // Add additional request properties.
      synchronized (keyRequestProperties) {
        requestProperties.putAll(keyRequestProperties);
      }
      String base64Encoded = Base64.encodeToString(request.getData(), Base64.NO_WRAP);
      Log.e("SIGMA","License Body Requests: " + base64Encoded);
      byte[] bytes = executePost(dataSourceFactory, url, request.getData(), requestProperties);

      String responseString = new String(bytes);
      Log.e("SIGMA","License Response: " + responseString);
      JSONObject jsonObject = new JSONObject(new String(bytes));
      String licenseEncrypted = jsonObject.getString("license");
      Log.e("SIGMA","License: " + licenseEncrypted);
      return Base64.decode(licenseEncrypted, Base64.DEFAULT);
    } catch (Exception e) {
      throw new RuntimeException("Error while parsing response", e);
    }
  }

  private static byte[] executePost(
      DataSource.Factory dataSourceFactory,
      String url,
      @Nullable byte[] httpBody,
      Map<String, String> requestProperties)
      throws MediaDrmCallbackException {
    StatsDataSource dataSource = new StatsDataSource(dataSourceFactory.createDataSource());
    int manualRedirectCount = 0;
    DataSpec dataSpec = new DataSpec.Builder()
        .setUri(url)
        .setHttpRequestHeaders(requestProperties)
        .setHttpMethod(DataSpec.HTTP_METHOD_POST)
        .setHttpBody(httpBody)
        .setFlags(DataSpec.FLAG_ALLOW_GZIP)
        .build();
    DataSpec originalDataSpec = dataSpec;
    try {
      while (true) {
        DataSourceInputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
        try {
          return Util.toByteArray(inputStream);
        } catch (HttpDataSource.InvalidResponseCodeException e) {
          @Nullable
          String redirectUrl = getRedirectUrl(e, manualRedirectCount);
          if (redirectUrl == null) {
            throw e;
          }
          manualRedirectCount++;
          dataSpec = dataSpec.buildUpon().setUri(redirectUrl).build();
        } finally {
          Util.closeQuietly(inputStream);
        }
      }
    } catch (Exception e) {
      Log.e("SIGMA", e.getMessage());
      e.printStackTrace();
      throw new MediaDrmCallbackException(
          originalDataSpec,
          Assertions.checkNotNull(dataSource.getLastOpenedUri()),
          dataSource.getResponseHeaders(),
          dataSource.getBytesRead(),
          /* cause= */ e);
    }
  }

  @Nullable
  private static String getRedirectUrl(
      HttpDataSource.InvalidResponseCodeException exception, int manualRedirectCount) {
    // For POST requests, the underlying network stack will not normally follow 307
    // or 308
    // redirects automatically. Do so manually here.
    boolean manuallyRedirect = (exception.responseCode == 307 || exception.responseCode == 308)
        && manualRedirectCount < MAX_MANUAL_REDIRECTS;
    if (!manuallyRedirect) {
      return null;
    }
    Map<String, List<String>> headerFields = exception.headerFields;
    if (headerFields != null) {
      @Nullable
      List<String> locationHeaders = headerFields.get("Location");
      if (locationHeaders != null && !locationHeaders.isEmpty()) {
        return locationHeaders.get(0);
      }
    }
    return null;
  }

  private String getCustomData(KeyRequest keyRequest) throws Exception {
    JSONObject customData = new JSONObject();

    customData.put("merchantId", SigmaHelper.instance().getMerchantId());
    customData.put("appId", SigmaHelper.instance().getAppId());
    customData.put("userId", SigmaHelper.instance().getUserId());
    customData.put("sessionId", SigmaHelper.instance().getSessionId());

    RequestInfo requestInfo = SigmaDrmPacker.requestInfo(keyRequest.getData());
    customData.put("reqId", requestInfo.requestId);
    customData.put("deviceInfo", requestInfo.deviceInfo);

    String customHeader = Base64.encodeToString(customData.toString().getBytes(), Base64.NO_WRAP);
    Log.e("SIGMA", "Custom Data: " + customHeader);
    return customHeader;
  }
}
