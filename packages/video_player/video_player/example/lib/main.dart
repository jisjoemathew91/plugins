// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// ignore_for_file: public_member_api_docs

/// An example of using the plugin, controlling lifecycle and playback of the
/// video.
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

void main() {
  runApp(
    MaterialApp(
      home: _App(),
    ),
  );
}

class _App extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        key: const ValueKey<String>('home_page'),
        appBar: AppBar(
          title: const Text('Video player example'),
          actions: <Widget>[
            IconButton(
              key: const ValueKey<String>('push_tab'),
              icon: const Icon(Icons.navigation),
              onPressed: () {
                Navigator.push<_PlayerVideoAndPopPage>(
                  context,
                  MaterialPageRoute<_PlayerVideoAndPopPage>(
                    builder: (BuildContext context) => _PlayerVideoAndPopPage(),
                  ),
                );
              },
            )
          ],
          bottom: const TabBar(
            isScrollable: true,
            tabs: <Widget>[
              Tab(
                icon: Icon(Icons.cloud),
                text: 'Remote',
              ),
              Tab(icon: Icon(Icons.insert_drive_file), text: 'Asset'),
              Tab(icon: Icon(Icons.list), text: 'List example'),
            ],
          ),
        ),
        body: TabBarView(
          children: <Widget>[
            const _RemoteVideo(),
            _ExampleVideo.asset(key: UniqueKey()),
            _ExampleVideoInList(),
          ],
        ),
      ),
    );
  }
}

class _ExampleVideoInList extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return ListView(
      children: <Widget>[
        const _ExampleCard(title: 'Item a'),
        const _ExampleCard(title: 'Item b'),
        const _ExampleCard(title: 'Item c'),
        const _ExampleCard(title: 'Item d'),
        const _ExampleCard(title: 'Item e'),
        const _ExampleCard(title: 'Item f'),
        const _ExampleCard(title: 'Item g'),
        Card(
            child: Column(children: <Widget>[
          Column(
            children: <Widget>[
              const ListTile(
                leading: Icon(Icons.cake),
                title: Text('Video video'),
              ),
              Stack(
                alignment: FractionalOffset.bottomRight +
                    const FractionalOffset(-0.1, -0.1),
                children: <Widget>[
                  _ExampleVideo.asset(key: UniqueKey()),
                ],
              ),
            ],
          ),
        ])),
        const _ExampleCard(title: 'Item h'),
        const _ExampleCard(title: 'Item i'),
        const _ExampleCard(title: 'Item j'),
        const _ExampleCard(title: 'Item k'),
        const _ExampleCard(title: 'Item l'),
      ],
    );
  }
}

/// A filler card to show the video in a list of scrolling contents.
class _ExampleCard extends StatelessWidget {
  const _ExampleCard({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          ListTile(
            leading: const Icon(Icons.airline_seat_flat_angled),
            title: Text(title),
          ),
          ButtonBar(
            children: <Widget>[
              TextButton(
                child: const Text('BUY TICKETS'),
                onPressed: () {
                  /* ... */
                },
              ),
              TextButton(
                child: const Text('SELL TICKETS'),
                onPressed: () {
                  /* ... */
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _RemoteVideo extends StatefulWidget {
  const _RemoteVideo({Key? key}) : super(key: key);

  @override
  State<_RemoteVideo> createState() => _RemoteVideoState();
}

class _RemoteVideoState extends State<_RemoteVideo> {
  List<bool> isSelected = [true, false];

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 20.0),
      child: ListView(
        children: [
          Align(
            child: ToggleButtons(
              children: const <Widget>[
                Text('MP4'),
                Text('HLS'),
              ],
              onPressed: (int index) {
                setState(() {
                  for (int buttonIndex = 0;
                      buttonIndex < isSelected.length;
                      buttonIndex++) {
                    if (buttonIndex == index) {
                      isSelected[buttonIndex] = true;
                    } else {
                      isSelected[buttonIndex] = false;
                    }
                  }
                });
              },
              isSelected: isSelected,
            ),
          ),
          if (isSelected[0])
            _ExampleVideo.mp4(key: UniqueKey())
          else
            _ExampleVideo.streaming(key: UniqueKey())
        ],
      ),
    );
  }
}

class ControlsOverlay extends StatelessWidget {
  const ControlsOverlay({Key? key, required this.controller}) : super(key: key);

  static const List<Duration> _exampleCaptionOffsets = <Duration>[
    Duration(seconds: -10),
    Duration(seconds: -3),
    Duration(seconds: -1, milliseconds: -500),
    Duration(milliseconds: -250),
    Duration(milliseconds: 0),
    Duration(milliseconds: 250),
    Duration(seconds: 1, milliseconds: 500),
    Duration(seconds: 3),
    Duration(seconds: 10),
  ];
  static const List<double> _examplePlaybackRates = <double>[
    0.25,
    0.5,
    1.0,
    1.5,
    2.0,
    3.0,
    5.0,
    10.0,
  ];

  final VideoPlayerController controller;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: <Widget>[
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 50),
          reverseDuration: const Duration(milliseconds: 200),
          child: controller.value.isPlaying
              ? const SizedBox.shrink()
              : Container(
                  color: Colors.black26,
                  child: const Center(
                    child: Icon(
                      Icons.play_arrow,
                      color: Colors.white,
                      size: 100.0,
                      semanticLabel: 'Play',
                    ),
                  ),
                ),
        ),
        GestureDetector(
          onTap: () {
            controller.value.isPlaying ? controller.pause() : controller.play();
          },
        ),
        Align(
          alignment: Alignment.topLeft,
          child: PopupMenuButton<Duration>(
            initialValue: controller.value.captionOffset,
            tooltip: 'Caption Offset',
            onSelected: (Duration delay) {
              controller.setCaptionOffset(delay);
            },
            itemBuilder: (BuildContext context) {
              return <PopupMenuItem<Duration>>[
                for (final Duration offsetDuration in _exampleCaptionOffsets)
                  PopupMenuItem<Duration>(
                    value: offsetDuration,
                    child: Text('${offsetDuration.inMilliseconds}ms'),
                  )
              ];
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(
                // Using less vertical padding as the text is also longer
                // horizontally, so it feels like it would need more spacing
                // horizontally (matching the aspect ratio of the video).
                vertical: 12,
                horizontal: 16,
              ),
              child: Text('${controller.value.captionOffset.inMilliseconds}ms'),
            ),
          ),
        ),
        Align(
          alignment: Alignment.topRight,
          child: PopupMenuButton<double>(
            initialValue: controller.value.playbackSpeed,
            tooltip: 'Playback speed',
            onSelected: (double speed) {
              controller.setPlaybackSpeed(speed);
            },
            itemBuilder: (BuildContext context) {
              return <PopupMenuItem<double>>[
                for (final double speed in _examplePlaybackRates)
                  PopupMenuItem<double>(
                    value: speed,
                    child: Text('${speed}x'),
                  )
              ];
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(
                // Using less vertical padding as the text is also longer
                // horizontally, so it feels like it would need more spacing
                // horizontally (matching the aspect ratio of the video).
                vertical: 12,
                horizontal: 16,
              ),
              child: Text('${controller.value.playbackSpeed}x'),
            ),
          ),
        ),
        Align(
          alignment: Alignment.bottomLeft,
          child: IconButton(
            onPressed: onPressed,
            icon: const Icon(Icons.fast_forward),
          ),
        )
      ],
    );
  }

  Future<void> onPressed() async {
    final Duration? currentDuration = await controller.position;
    if (currentDuration != null) {
      final Duration newDuration = currentDuration + const Duration(seconds: 1);
      await controller.seekTo(newDuration);
    }
  }
}

class _PlayerVideoAndPopPage extends StatefulWidget {
  @override
  _PlayerVideoAndPopPageState createState() => _PlayerVideoAndPopPageState();
}

class _PlayerVideoAndPopPageState extends State<_PlayerVideoAndPopPage> {
  late VideoPlayerController _videoPlayerController;
  bool startedPlaying = false;

  @override
  void initState() {
    super.initState();

    _videoPlayerController =
        VideoPlayerController.asset('assets/Butterfly-209.mp4');
    _videoPlayerController.addListener(() {
      if (startedPlaying && !_videoPlayerController.value.isPlaying) {
        Navigator.pop(context);
      }
    });
  }

  @override
  void dispose() {
    _videoPlayerController.dispose();
    super.dispose();
  }

  Future<bool> started() async {
    await _videoPlayerController.initialize();
    await _videoPlayerController.play();
    startedPlaying = true;
    return true;
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      elevation: 0,
      child: Center(
        child: FutureBuilder<bool>(
          future: started(),
          builder: (BuildContext context, AsyncSnapshot<bool> snapshot) {
            if (snapshot.data == true) {
              return AspectRatio(
                aspectRatio: _videoPlayerController.value.aspectRatio,
                child: VideoPlayer(_videoPlayerController),
              );
            } else {
              return const Text('waiting for video to load');
            }
          },
        ),
      ),
    );
  }
}

class _ExampleVideo extends StatefulWidget {
  const _ExampleVideo._({
    required this.isStreaming,
    required this.isAsset,
    Key? key,
  }) : super(key: key);

  factory _ExampleVideo.mp4({Key? key}) {
    return _ExampleVideo._(
      isStreaming: false,
      isAsset: false,
      key: key,
    );
  }

  factory _ExampleVideo.streaming({Key? key}) {
    return _ExampleVideo._(
      isStreaming: true,
      isAsset: false,
      key: key,
    );
  }

  factory _ExampleVideo.asset({Key? key}) {
    return _ExampleVideo._(
      isStreaming: false,
      isAsset: true,
      key: key,
    );
  }

  /// Is video using streaming source
  final bool isStreaming;

  /// Is video using asset source
  final bool isAsset;

  @override
  _ExampleVideoState createState() => _ExampleVideoState();
}

class _ExampleVideoState extends State<_ExampleVideo> {
  late TextEditingController _textController;
  late VideoPlayerController _videoController;

  Duration _startDuration = Duration.zero;

  @override
  void initState() {
    super.initState();
    _textController = TextEditingController();
    _initVideoController();
  }

  @override
  void dispose() {
    _videoController.dispose();
    _textController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Column(
        children: <Widget>[
          Container(
            padding: const EdgeInsets.all(20),
            child: AspectRatio(
              aspectRatio: _videoController.value.aspectRatio,
              child: Stack(
                alignment: Alignment.bottomCenter,
                children: <Widget>[
                  VideoPlayer(_videoController),
                  ControlsOverlay(controller: _videoController),
                  VideoProgressIndicator(_videoController,
                      allowScrubbing: true),
                ],
              ),
            ),
          ),
          ListTile(
            title: const Text('Video first start duration'),
            subtitle: Text('${_startDuration.inSeconds.toString()} s'),
            trailing: IconButton(
                onPressed: _showDialog, icon: const Icon(Icons.edit)),
          ),
          ElevatedButton(
            onPressed: () {
              _videoController.seekTo(
                _videoController.value.buffered.last.end -
                    const Duration(seconds: 5),
              );
            },
            child: Text('buffer end'),
          ),
          Wrap(
            children: [
              Text('isBuffering: ${_videoController.value.isBuffering}'),
              Text('hasError: ${_videoController.value.hasError}'),
              Text('position: ${_videoController.value.position}'),
              Text('playbackSpeed: ${_videoController.value.playbackSpeed}'),
              Text('dropped: ${_videoController.value.framesBeenDropped}'),
              Text('vfpoRate: ${_videoController.value.vfpoRate}'),
              Text('format: ${_videoController.value.mediaItemFormat}'),
            ]
                .map(
                  (child) => Padding(
                    padding: EdgeInsets.all(4.0),
                    child: child,
                  ),
                )
                .toList(),
          ),
        ],
      ),
    );
  }

  void _reinitVideoController() {
    _videoController.pause();
    _videoController.dispose();
    _initVideoController();
  }

  void _initVideoController() {
    if (widget.isAsset) {
      _videoController = VideoPlayerController.asset(
        'assets/Butterfly-209.mp4',
        enableLog: true,
      );
    } else if (widget.isStreaming) {
      _videoController = VideoPlayerController.network(
        'https://multiplatform-f.akamaihd.net/i/multi/will/bunny/big_buck_bunny_,640x360_400,640x360_700,640x360_1000,950x540_1500,.f4v.csmil/master.m3u8',
        formatHint: VideoFormat.hls,
        videoPlayerOptions: const VideoPlayerOptions(),
      );
    } else {
      _videoController = VideoPlayerController.network(
        'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
        closedCaptionFile: _loadCaptions(),
        videoPlayerOptions: const VideoPlayerOptions(mixWithOthers: true),
      );
    }

    _videoController.addListener(() {
      setState(() {});
    });

    _videoController.setLooping(true);
    _videoController.initialize(duration: _startDuration).then(
      (_) => setState(() {}),
      onError: (Object e) {
        print(_videoController.value.errorDescription);
        print('-------------------------------------');
        print(_videoController.value.holeErrorDescription);
      },
    );
    _videoController.play();
  }

  Future<void> _showDialog() async {
    return showDialog<void>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Choose video start duration'),
          content: TextField(
            controller: _textController,
            keyboardType: TextInputType.number,
          ),
          actions: <Widget>[
            TextButton(
              child: const Text('Submit'),
              onPressed: () {
                _onSubmitStartTime();
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  void _onSubmitStartTime() {
    final int? seconds = int.tryParse(_textController.text);
    if (seconds != null) {
      final Duration newStartDuration = Duration(seconds: seconds);
      if (newStartDuration != _startDuration) {
        setState(() {
          _startDuration = newStartDuration;
        });
        _reinitVideoController();
      }
    }
  }

  Future<ClosedCaptionFile> _loadCaptions() async {
    final String fileContents = await DefaultAssetBundle.of(context)
        .loadString('assets/bumble_bee_captions.vtt');
    return WebVTTCaptionFile(
        fileContents); // For vtt files, use WebVTTCaptionFile
  }
}
