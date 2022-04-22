import 'package:flutter/foundation.dart';

/// (Android only) Live-метрики для плеера
@immutable
class HolePlaybackMetrics {
  /// Создаем экземпляр
  const HolePlaybackMetrics({
    required this.videoMimeType,
    required this.codec,
    required this.height,
    required this.framesDropped,
    required this.frameDropRate,
    required this.vfpo,
    required this.meanBandWidth,
    required this.audioMimeType,
    required this.hz,
    required this.channelCount,
  });

  /// Video sample mime type, may be empty
  final String videoMimeType;

  /// Video codes
  final String codec;

  /// Video height
  final int height;

  /// Total frames drop count
  final int framesDropped;

  /// Frame drop rate
  final double frameDropRate;

  /// Video Frame Processing Offset.
  /// Метрика fps. Говорят, что стоит беспокоиться, если значение меньше 40000
  /// https://medium.com/google-exoplayer/improved-rendering-performance-operating-mediacodec-in-asynchronous-mode-and-asynchronous-buffer-3026207850b2
  final int vfpo;

  /// Mean network bandwidth based on transfer measurements, in bits per second
  final int meanBandWidth;

  /// Audio sample mime type, may be empty
  final String audioMimeType;

  /// The audio sampling rate in Hz, may be -1
  final int hz;

  /// The number of audio channels, mat be -1
  final int channelCount;

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        other is HolePlaybackMetrics &&
            videoMimeType == other.videoMimeType &&
            codec == other.codec &&
            height == other.height &&
            framesDropped == other.framesDropped &&
            frameDropRate == other.frameDropRate &&
            vfpo == other.vfpo &&
            meanBandWidth == other.meanBandWidth &&
            audioMimeType == other.audioMimeType &&
            hz == other.hz &&
            channelCount == other.channelCount;
  }

  @override
  int get hashCode =>
      videoMimeType.hashCode ^
      codec.hashCode ^
      height.hashCode ^
      framesDropped.hashCode ^
      frameDropRate.hashCode ^
      vfpo.hashCode ^
      meanBandWidth.hashCode ^
      audioMimeType.hashCode ^
      hz.hashCode ^
      channelCount.hashCode;
}
