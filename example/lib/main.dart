import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:sigma_video_player/sigma_video_player.dart';

void main() {
  runApp(const MyApp());
}

/// Video configuration model
class VideoConfig {
  final String title;
  final String url;
  final Map<String, String> drmConfiguration;

  const VideoConfig({
    required this.title,
    required this.url,
    this.drmConfiguration = const {},
  });
}

/// My app
class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  VideoPlayerController? _videoController;
  ChewieController? _chewieController;

  int _currentIndex = 0;
  Key _playerKey = UniqueKey();

  /// Playlist
  final List<VideoConfig> _playlist = [
    VideoConfig(
      title: "Big Buck Bunny (Staging)",
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
      title: "Big Buck Bunny (Production)",
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
    const VideoConfig(
      title: "Big Buck Bunny Clear",
      url: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
    ),
  ];

  @override
  void initState() {
    super.initState();
    HardwareKeyboard.instance.addHandler(_handleKeyEvent);
    initializePlayer();
  }

  @override
  void dispose() {
    HardwareKeyboard.instance.removeHandler(_handleKeyEvent);
    _disposePlayer();
    super.dispose();
  }

  Future<void> _disposePlayer() async {
    await _videoController?.dispose();
    _chewieController?.dispose();
    _videoController = null;
    _chewieController = null;
  }

  Future<void> initializePlayer() async {
    await _disposePlayer();

    final config = _playlist[_currentIndex];

    _videoController = VideoPlayerController.networkUrl(
      Uri.parse(config.url),
      drmConfiguration: config.drmConfiguration,
    );

    await _videoController!.initialize();
    if (!mounted) return;

    _createChewieController(_videoController!);
    setState(() {});
  }

  void _createChewieController(VideoPlayerController controller) {
    _chewieController = ChewieController(
      videoPlayerController: controller,
      autoPlay: true,
      looping: false,
      allowFullScreen: true,
      allowMuting: true,
      showControls: true,
      fullScreenByDefault: false,
      additionalOptions: (context) {
        return <OptionItem>[
          OptionItem(
            onTap: (context) {
              Navigator.pop(context); // Close the menu
              _nextVideo();
            },
            iconData: Icons.skip_next,
            title: 'Next Video',
          ),
        ];
      },
    );
  }

  Future<void> _nextVideo() async {
    _playerKey = UniqueKey();
    _currentIndex = (_currentIndex + 1) % _playlist.length;
    await initializePlayer();
  }

  bool _handleKeyEvent(KeyEvent event) {
    if (event is KeyDownEvent &&
        event.logicalKey == LogicalKeyboardKey.arrowUp) {
      _nextVideo();
      return true;
    }
    return false;
  }

  /// -------------------------
  /// UI
  /// -------------------------

  @override
  Widget build(BuildContext context) {
    final current = _playlist[_currentIndex];

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        backgroundColor: Colors.black,
        body: Stack(
          children: [
            /// =====================
            /// VIDEO PLAYER (BOTTOM)
            /// =====================
            Positioned.fill(
              child: Center(
                key: _playerKey,
                child:
                    _chewieController != null &&
                        _chewieController!
                            .videoPlayerController
                            .value
                            .isInitialized
                    ? Chewie(controller: _chewieController!)
                    : const CircularProgressIndicator(),
              ),
            ),

            /// =====================
            /// OVERLAY UI (TOP)
            /// =====================
            Positioned(
              left: 16,
              right: 16,
              top: 24,
              child: Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.6),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    /// Title
                    Text(
                      current.title,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),

                    const SizedBox(height: 4),

                    Row(
                      mainAxisAlignment: MainAxisAlignment.start,
                      children: [
                        ElevatedButton(
                          onPressed: () {
                            _chewieController?.enterFullScreen();
                          },
                          child: const Text('Fullscreen'),
                        ),

                        const SizedBox(height: 8),

                        ElevatedButton(
                          onPressed: _nextVideo,
                          child: const Text('Next Video'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
