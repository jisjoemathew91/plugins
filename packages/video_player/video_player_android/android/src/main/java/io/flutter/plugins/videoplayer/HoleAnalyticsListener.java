package io.flutter.plugins.videoplayer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.analytics.PlaybackStatsListener;
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation;

import java.util.HashMap;
import java.util.Map;

/**
 * Отправка live-данных о процессе воспроизведения
 */
public class HoleAnalyticsListener implements AnalyticsListener {

  private final QueuingEventSink eventSink;

  public HoleAnalyticsListener(QueuingEventSink eventSink) {
    this.eventSink = eventSink;
  }

  // Заготовка для будущих поколений
  public static void parsePlaybackStats(PlaybackStatsListener listener) {
//    PlaybackStats playbackStats = listener.getPlaybackStats();
//    if (playbackStats == null) return;
//
//    ilog("playbackCount", playbackStats.playbackCount);
//    ilog("firstReportedTimeMs", playbackStats.firstReportedTimeMs);
//
//    ilog("nonFatalErrorCount", playbackStats.nonFatalErrorCount);
////    ilog("nonFatalErrorList", playbackStats.nonFatalErrorHistory);
//
//    ilog("fatalErrorCount", playbackStats.fatalErrorCount);
////    ilog("fatalErrorList", playbackStats.fatalErrorHistory);
//
//    ilog("getTotalRebufferTimeMs", playbackStats.getTotalRebufferTimeMs());
//    ilog("totalValidJoinTimeMs", playbackStats.totalValidJoinTimeMs);
//    ilog("validJoinTimeCount", playbackStats.validJoinTimeCount);
//    ilog("maxRebufferTimeMs", playbackStats.maxRebufferTimeMs);
//    ilog("getMeanBandwidth (hls, Mbits)", playbackStats.getMeanBandwidth() / (1024 * 1024));
//    ilog("getDroppedFramesRate", playbackStats.getDroppedFramesRate());
//    ilog("totalDroppedFrames", playbackStats.totalDroppedFrames);
  }

  @Override
  public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
    wlog("bandwidthEstimate", totalLoadTimeMs + "|" + totalBytesLoaded + "|" + bitrateEstimate);
    sendDataToTheHole("bandwidthEstimate", totalLoadTimeMs + "|" + totalBytesLoaded + "|" + bitrateEstimate);
  }

  @Override
  public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
    sendDataToTheHole("framesDropped", droppedFrames);
  }

  @Override
  public void onVideoInputFormatChanged(EventTime eventTime, Format format, @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
    sendDataToTheHole("formatChanged", format.height + "|" + format.codecs + "|" + format.sampleMimeType);
  }

  @Override
  public void onVideoFrameProcessingOffset(EventTime eventTime, long totalProcessingOffsetUs, int frameCount) {
    long vfporate = totalProcessingOffsetUs / frameCount;
    sendDataToTheHole("vfpoRate", vfporate);
  }

  @Override
  public void onVideoCodecError(EventTime eventTime, Exception videoCodecError) {
    sendDataToTheHole("nonFatalVideoCodecError", videoCodecError.getMessage());
  }

  // Для отладки. Можно использовать для логирования параметров
  private void wlog(@NonNull String field, Object message) {
    Log.w("HOLE_TAGG", field + ": " + message);
  }

  /**
   * Это отправка не_фатальных ошибок и аналитики о текущем состоянии воспроизведения
   */
  private void sendDataToTheHole(String eventName, Object value) {
    Map<String, Object> event = new HashMap<>();
    event.put("event", eventName);
    event.put("value", value);
    eventSink.success(event);
  }

}
