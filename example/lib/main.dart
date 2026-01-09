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

  /// Dùng key để ép Flutter recreate Surface / Texture
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
