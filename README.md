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
    ref: v1.0.0 # Replace with the latest version
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

### Example Implementation

Here's a complete example of a video player with playlist support:

```dart
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:sigma_video_player/sigma_video_player.dart';

void main() {
  runApp(const App());
}

/// Root app
class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Sigma Player Demo',
      home: HomePage(),
    );
  }
}

/// Video configuration model
class VideoConfig {
  final String url;
  final Map<String, String> drmConfiguration; // Base64 JSON (DRM info)

  const VideoConfig({required this.url, this.drmConfiguration = const {}});
}

/// Home page
class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  VideoPlayerController? _videoController;

  int _playerKey = 0;

  /// Playlist
  final List<VideoConfig> _playlist = [
    VideoConfig(
      url:
          "https://sdrm-test.gviet.vn:9080/static/vod_staging/the_box/manifest.mpd",
      drmConfiguration: {
        "licenseServerUrl":
            "https://license-staging.sigmadrm.com/license/verify/widevine",
        "merchantId": "sctv",
        "appId": "RedTV",
        "userId": "flutter user id",
        "sessionId": "session id",
      },
    ),
    VideoConfig(
      url:
          "https://sdrm-test.gviet.vn:9080/static/vod_production/big_bug_bunny/manifest.mpd",
      drmConfiguration: {
        "licenseServerUrl":
            "https://license.sigmadrm.com/license/verify/widevine",
        "merchantId": "sigma_packager_lite",
        "appId": "demo",
        "userId": "flutter user id",
        "sessionId": "session id",
      },
    ),
    const VideoConfig(url: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
  ];

  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    _playByConfig(_playlist[_currentIndex]);
  }

  @override
  void dispose() {
    _disposePlayer();
    super.dispose();
  }

  /// -------------------------
  /// Utils
  /// -------------------------

  Future<void> _disposePlayer() async {
    await _videoController?.dispose();
    _videoController = null;
  }

  /// -------------------------
  /// Player init
  /// -------------------------

  Future<void> _initializePlayer({
    required String url,
    required Map<String, String> drmConfiguration,
  }) async {
    final controller = VideoPlayerController.networkUrl(
      Uri.parse(url),
      drmConfiguration: drmConfiguration,
    );

    _videoController = controller;

    await controller.initialize();
    if (!mounted) return;

    await controller.play();

    setState(() {});
  }

  /// -------------------------
  /// Playlist control
  /// -------------------------

  Future<void> _playByConfig(VideoConfig config) async {
    await _disposePlayer();
    _playerKey++;

    await _initializePlayer(
      url: config.url,
      drmConfiguration: config.drmConfiguration,
    );
  }

  void _changeVideo() {
    _currentIndex = (_currentIndex + 1) % _playlist.length;
    _playByConfig(_playlist[_currentIndex]);
  }

  Future<void> _togglePlayPause() async {
    final c = _videoController;
    if (c == null || !c.value.isInitialized) return;

    if (c.value.isPlaying) {
      await c.pause();
    } else {
      await c.play();
    }
    setState(() {});
  }

  /// -------------------------
  /// UI
  /// -------------------------

  Widget _buildPlayer() {
    final controller = _videoController;

    if (controller == null || !controller.value.isInitialized) {
      return const CircularProgressIndicator();
    }

    if (kIsWeb) {
      return AspectRatio(
        aspectRatio: controller.value.aspectRatio,
        child: VideoPlayer(controller),
      );
    }

    return KeyedSubtree(
      key: ValueKey(_playerKey),
      child: JkVideoControlPanel(
        controller,
        showFullscreenButton: true,
        showVolumeButton: true,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          const SizedBox(height: 40),
          Expanded(child: Center(child: _buildPlayer())),
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton(
                onPressed: _togglePlayPause,
                child: Text(
                  (_videoController != null &&
                          _videoController!.value.isPlaying)
                      ? 'Pause'
                      : 'Play',
                ),
              ),
              const SizedBox(width: 20),
              ElevatedButton(
                onPressed: _changeVideo,
                child: const Text('Change Video'),
              ),
            ],
          ),
          const SizedBox(height: 30),
        ],
      ),
    );
  }
}

```

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

## Platform Support

- Android

## Reference

- [video_player](https://pub.dev/packages/video_player): Official Flutter plugin for cross-platform video playback, used as the base player in this project.
