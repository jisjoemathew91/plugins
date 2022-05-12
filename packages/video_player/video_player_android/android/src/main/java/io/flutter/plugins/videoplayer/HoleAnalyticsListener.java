package io.flutter.plugins.videoplayer;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.analytics.PlaybackStats;
import com.google.android.exoplayer2.analytics.PlaybackStatsListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;

import java.util.HashMap;
import java.util.Map;

/**
 * Отправка live-данных о процессе воспроизведения
 */
public class HoleAnalyticsListener implements AnalyticsListener {

  private QueuingEventSink eventSink;

  private PlaybackStatsListener playbackStatsListener;

  /**
   * Лучше устанавливать поля здесь, а не в конструкторе, потому что мы хотим снимать слушателя
   * с экземпляра плеера и устанавливать по новой
   *
   * @param eventSink             сюда шлём события
   * @param playbackStatsListener отсюда берем часть статистики
   */
  public void setup(QueuingEventSink eventSink, PlaybackStatsListener playbackStatsListener) {
    this.eventSink = eventSink;
    this.playbackStatsListener = playbackStatsListener;
  }

  /**
   * Очищаем зависимости
   */
  public void dispose() {
    eventSink = null;
    playbackStatsListener = null;
  }

  @Override
  public void onEvents(Player player, Events events) {
    PlaybackStats playbackStats = playbackStatsListener.getPlaybackStats();
    if (!(player instanceof SimpleExoPlayer) || playbackStats == null) return;

    SimpleExoPlayer exoPlayer = (SimpleExoPlayer) player;

    Format vformat = exoPlayer.getVideoFormat();
    Format aformat = exoPlayer.getAudioFormat();
    DecoderCounters vdc = exoPlayer.getVideoDecoderCounters();
    DecoderCounters adc = exoPlayer.getAudioDecoderCounters();
    if (vformat == null || vdc == null || aformat == null || adc == null) return;

    long vfpo = 0;
    if (vdc.videoFrameProcessingOffsetCount != 0) {
      vfpo = (long) ((double) vdc.totalVideoFrameProcessingOffsetUs / vdc.videoFrameProcessingOffsetCount);
    }
    // video
    Map<String, Object> event = new HashMap<>();
    event.put("event", "playbackMetrics");
    if (vformat.sampleMimeType != null) {
      event.put("videoMimeType", vformat.sampleMimeType);
    } else {
      event.put("videoMimeType", "");
    }
    if (vformat.codecs != null) {
      event.put("codec", vformat.codecs);
    } else {
      event.put("codec", "");
    }
    event.put("height", vformat.height);
    event.put("framesDropped", playbackStats.totalDroppedFrames);
    event.put("frameDropRate", playbackStats.getDroppedFramesRate());
    event.put("vfpo", vfpo);
    event.put("meanBandWidth", playbackStats.getMeanBandwidth());
    // audio
    if (aformat.sampleMimeType != null) {
      event.put("audioMimeType", aformat.sampleMimeType);
    } else {
      event.put("audioMimeType", "");
    }
    event.put("hz", aformat.sampleRate);
    event.put("channelCount", aformat.channelCount);
    eventSink.success(event);
  }

  // Для отладки. Можно использовать для логирования параметров
  private void wlog(@NonNull String field, Object message) {
    Log.w("HOLE_TAGG", field + ": " + message);
  }
}
