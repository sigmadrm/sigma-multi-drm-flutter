# Sigma Multi-DRM Flutter Plugin

A Flutter plugin for integrating Multi-DRM protection with video playback. This plugin supports Widevine DRM and provides a customizable video player with DRM support.

## Features

- Play DRM-protected video content
- Support for Widevine DRM
- Customizable video player controls
- Support for both DRM and non-DRM content
- Fullscreen playback support
- Volume control
- Play/pause functionality
- Video playlist support

## Installation

Add the following to your `pubspec.yaml` file:

```yaml
dependencies:
  sigma_video_player:
    git:
      url: https://github.com/sigmadrm/sigma-multi-drm-flutter.git
      ref: v1.1.0 # Replace with the latest version
```

Then run:

```bash
flutter pub get
```

## Usage

### Basic Setup

1. Import the package:

```dart
import 'package:sigma_video_player/sigma_video_player.dart';
```

### Playing DRM-protected Content

```dart
// Initialize the video player with DRM configuration
final controller = VideoPlayerController.networkUrl(
  Uri.parse('YOUR_DRM_PROTECTED_VIDEO_URL'),
  drmConfiguration: {
    'licenseServerUrl': 'LICENSE_SERVER_URL',
    'merchantId': 'YOUR_MERCHANT_ID',
    'appId': 'YOUR_APP_ID',
    'userId': 'USER_ID',
    'sessionId': 'SESSION_ID',
  },
);

// Initialize the controller
await controller.initialize();

// Start playback
await controller.play();
```

## ▶️ Run the Example Application

```bash
cd example
flutter pub get
flutter run


## Configuration

### DRM Configuration

The plugin requires the following DRM configuration parameters:

- `LICENSE_SERVER_URL`: The URL of Sigma DRM license server (optional).
  - Production is `https://license.sigmadrm.com/license/verify/widevine` (default).
  - Staging is `https://license-staging.sigmadrm.com/license/verify/widevine`
- `MERCHANT_ID`: Your merchant ID provided by Sigma DRM
- `APP_ID`: Your application ID
- `USER_ID`: Current user ID
- `SESSION_ID`: Session ID for authentication

## 📱 Platform Support

| Platform       | Status                          |
| -------------- | ------------------------------- |
| Android Mobile | ✅ Supported                    |
| Android Tablet | ✅ Supported                    |
| Android TV     | ✅ Supported (TV Box, Smart TV) |

## 📚 Reference

- [video_player](https://pub.dev/packages/video_player): Official Flutter plugin for cross-platform video playback, used as the base player in this project.
- [chewie](https://pub.dev/packages/chewie): A video player for Flutter with Material and Cupertino skins, providing customizable UI controls for video playback.
- [video_player_control_panel](https://pub.dev/packages/video_player_control_panel): A flexible and customizable control panel for Flutter video players, offering granular control over UI elements.
```
