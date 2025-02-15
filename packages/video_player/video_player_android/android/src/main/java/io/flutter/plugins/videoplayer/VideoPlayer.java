// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.Listener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.PlaybackStatsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;

import io.flutter.plugin.common.EventChannel;
import io.flutter.view.TextureRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class VideoPlayer {
  private static final String FORMAT_SS = "ss";
  private static final String FORMAT_DASH = "dash";
  private static final String FORMAT_HLS = "hls";
  private static final String FORMAT_OTHER = "other";

  private SimpleExoPlayer exoPlayer;

  private Surface surface;

  private final TextureRegistry.SurfaceTextureEntry textureEntry;

  private QueuingEventSink eventSink = new QueuingEventSink();

  private final EventChannel eventChannel;

  private boolean isInitialized = false;
  private boolean needLogging = false;

  private final VideoPlayerOptions options;

  // Слушатель аналитики. Данные доступны только после смерти плеера. Зато очень подробные
  private final PlaybackStatsListener playbackStatsListener = new PlaybackStatsListener(true, null);
  // Слушатель аналитии. Отправляет события на платформу live
  private final HoleAnalyticsListener holeAnalyticsListener = new HoleAnalyticsListener();

  VideoPlayer(
      Context context,
      EventChannel eventChannel,
      TextureRegistry.SurfaceTextureEntry textureEntry,
      String dataSource,
      String formatHint,
      @NonNull Map<String, String> httpHeaders,
      VideoPlayerOptions options,
      Messages.BufferMessage bufferMessage,
      long duration,
      Boolean enableLog) {
    this.eventChannel = eventChannel;
    this.textureEntry = textureEntry;
    this.options = options;

    if (enableLog != null) {
      this.needLogging = enableLog;
    }

    exoPlayer = new SimpleExoPlayer.Builder(context)
        .setLoadControl(createDefaultLoadControlBy(bufferMessage))
        .build();

    exoPlayer.addAnalyticsListener(playbackStatsListener);

    Uri uri = Uri.parse(dataSource);

    DataSource.Factory dataSourceFactory;
    if (isHTTP(uri)) {
      DefaultHttpDataSource.Factory httpDataSourceFactory =
          new DefaultHttpDataSource.Factory()
              .setUserAgent("ExoPlayer")
              .setAllowCrossProtocolRedirects(true);

      if (httpHeaders != null && !httpHeaders.isEmpty()) {
        httpDataSourceFactory.setDefaultRequestProperties(httpHeaders);
      }
      if (this.needLogging) {
        httpDataSourceFactory.setTransferListener(new TransferListenerImpl());
      }
      dataSourceFactory = httpDataSourceFactory;
    } else {
      dataSourceFactory = new DefaultDataSourceFactory(context, "ExoPlayer");
    }

    MediaSource mediaSource = buildMediaSource(uri, dataSourceFactory, formatHint, context);
    exoPlayer.setMediaSource(mediaSource);
    exoPlayer.prepare();

    setupVideoPlayer(eventChannel, textureEntry);
    seekTo((int) duration);
  }

  private static DefaultLoadControl createDefaultLoadControlBy(Messages.BufferMessage bufferMessage) {
    int minBufferMs = DefaultLoadControl.DEFAULT_MIN_BUFFER_MS;
    if (bufferMessage.getMinBufferMs() != null) {
      minBufferMs = bufferMessage.getMinBufferMs().intValue();
    }

    int maxBufferMs = DefaultLoadControl.DEFAULT_MAX_BUFFER_MS;
    if (bufferMessage.getMaxBufferMs() != null) {
      maxBufferMs = bufferMessage.getMaxBufferMs().intValue();
    }

    int bufferForPlaybackMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS;
    if (bufferMessage.getBufferForPlaybackMs() != null) {
      bufferForPlaybackMs = bufferMessage.getBufferForPlaybackMs().intValue();
    }

    int bufferForPlaybackAfterRebufferMs = DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
    if (bufferMessage.getBufferForPlaybackAfterRebufferMs() != null) {
      bufferForPlaybackAfterRebufferMs = bufferMessage
          .getBufferForPlaybackAfterRebufferMs()
          .intValue();
    }

    int backBufferDurationMs = DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS;
    if (bufferMessage.getBackBufferDurationMs() != null) {
      backBufferDurationMs = bufferMessage.getBackBufferDurationMs().intValue();
    }

    Boolean retainBackBufferFromKeyframe = DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME;
    if (bufferMessage.getRetainBackBufferFromKeyframe() != null) {
      retainBackBufferFromKeyframe = bufferMessage.getRetainBackBufferFromKeyframe();
    }

    return new DefaultLoadControl.Builder()
        .setBackBuffer(backBufferDurationMs, retainBackBufferFromKeyframe)
        .setBufferDurationsMs(minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferMs)
        .build();
  }

  private static boolean isHTTP(Uri uri) {
    if (uri == null || uri.getScheme() == null) {
      return false;
    }
    String scheme = uri.getScheme();
    return scheme.equals("http") || scheme.equals("https");
  }

  private MediaSource buildMediaSource(
      Uri uri, DataSource.Factory mediaDataSourceFactory, String formatHint, Context context) {
    int type;
    if (formatHint == null) {
      type = Util.inferContentType(uri.getLastPathSegment());
    } else {
      switch (formatHint) {
        case FORMAT_SS:
          type = C.TYPE_SS;
          break;
        case FORMAT_DASH:
          type = C.TYPE_DASH;
          break;
        case FORMAT_HLS:
          type = C.TYPE_HLS;
          break;
        case FORMAT_OTHER:
          type = C.TYPE_OTHER;
          break;
        default:
          type = -1;
          break;
      }
    }
    switch (type) {
      case C.TYPE_SS:
        return new SsMediaSource.Factory(
            new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
            new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
            .createMediaSource(MediaItem.fromUri(uri));
      case C.TYPE_DASH:
        return new DashMediaSource.Factory(
            new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
            new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
            .createMediaSource(MediaItem.fromUri(uri));
      case C.TYPE_HLS:
        return new HlsMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri));
      case C.TYPE_OTHER:
        return new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri));
      default: {
        throw new IllegalStateException("Unsupported type: " + type);
      }
    }
  }

  @SuppressWarnings("deprecation")
  private void setupVideoPlayer(
      EventChannel eventChannel, TextureRegistry.SurfaceTextureEntry textureEntry) {
    eventChannel.setStreamHandler(
        new EventChannel.StreamHandler() {
          @Override
          public void onListen(Object o, EventChannel.EventSink sink) {
            eventSink.setDelegate(sink);
          }

          @Override
          public void onCancel(Object o) {
            eventSink.setDelegate(null);
          }
        });

    surface = new Surface(textureEntry.surfaceTexture());
    exoPlayer.setVideoSurface(surface);
    setAudioAttributes(exoPlayer, options.mixWithOthers);

    if (needLogging)
      exoPlayer.addAnalyticsListener(new EventLogger(new DefaultTrackSelector()));

    holeAnalyticsListener.setup(eventSink, playbackStatsListener);
    exoPlayer.addAnalyticsListener(holeAnalyticsListener);

    exoPlayer.addListener(
        new Listener() {
          private boolean isBuffering = false;

          public void setBuffering(boolean buffering) {
            if (isBuffering != buffering) {
              isBuffering = buffering;
              Map<String, Object> event = new HashMap<>();
              event.put("event", isBuffering ? "bufferingStart" : "bufferingEnd");
              eventSink.success(event);
            }
          }

          @Override
          public void onPlaybackStateChanged(final int playbackState) {
            if (playbackState == Player.STATE_BUFFERING) {
              setBuffering(true);
              sendBufferingUpdate();
            } else if (playbackState == Player.STATE_READY) {
              if (!isInitialized) {
                isInitialized = true;
                sendInitialized();
              }
            } else if (playbackState == Player.STATE_ENDED) {
              Map<String, Object> event = new HashMap<>();
              event.put("event", "completed");
              eventSink.success(event);
            }

            if (playbackState != Player.STATE_BUFFERING) {
              setBuffering(false);
            }
          }

          @Override
          public void onPlayerError(final ExoPlaybackException error) {
            setBuffering(false);
            if (eventSink != null) {
              HoleErrorBuilder holeError = new HoleErrorBuilder();
              holeError.parseError(error);

              eventSink.error(
                  "VideoError",
                  "Video player had error " + error,
                  holeError.toString()
              );
            }
          }
        });
  }

  void sendBufferingUpdate() {
    Map<String, Object> event = new HashMap<>();
    event.put("event", "bufferingUpdate");
    List<? extends Number> range = Arrays.asList(0, exoPlayer.getBufferedPosition());
    // iOS supports a list of buffered ranges, so here is a list with a single range.
    event.put("values", Collections.singletonList(range));
    eventSink.success(event);
  }

  @SuppressWarnings("deprecation")
  private static void setAudioAttributes(SimpleExoPlayer exoPlayer, boolean isMixMode) {
    exoPlayer.setAudioAttributes(
        new AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MOVIE).build(), !isMixMode);
  }

  void play() {
    exoPlayer.setPlayWhenReady(true);
  }

  void pause() {
    exoPlayer.setPlayWhenReady(false);
  }

  void setLooping(boolean value) {
    exoPlayer.setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
  }

  void setVolume(double value) {
    float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
    exoPlayer.setVolume(bracketedValue);
  }

  void setPlaybackSpeed(double value) {
    // We do not need to consider pitch and skipSilence for now as we do not handle them and
    // therefore never diverge from the default values.
    final PlaybackParameters playbackParameters = new PlaybackParameters(((float) value));

    exoPlayer.setPlaybackParameters(playbackParameters);
  }

  void seekTo(int location) {
    exoPlayer.seekTo(location);
  }

  void setPreferredVideoSize(int width, int height) {
    int preferredWidth = width;
    int preferredHeight = height;
    if (preferredWidth <= 0 || preferredHeight <= 0) {
      preferredWidth = Integer.MAX_VALUE;
      preferredHeight = Integer.MAX_VALUE;
    }

    TrackSelector trackSelector = exoPlayer.getTrackSelector();
    if (trackSelector instanceof DefaultTrackSelector) {
      DefaultTrackSelector defaultTrackSelector = (DefaultTrackSelector) trackSelector;
      DefaultTrackSelector.ParametersBuilder builder = defaultTrackSelector.getParameters().buildUpon();
      defaultTrackSelector.setParameters(
          builder.setMaxVideoSize(preferredWidth, preferredHeight)
              .build()
      );
    }
  }

  long getPosition() {
    return exoPlayer.getCurrentPosition();
  }

  @SuppressWarnings("SuspiciousNameCombination")
  private void sendInitialized() {
    if (isInitialized) {
      Map<String, Object> event = new HashMap<>();
      event.put("event", "initialized");
      event.put("duration", exoPlayer.getDuration());

      if (exoPlayer.getVideoFormat() != null) {
        Format videoFormat = exoPlayer.getVideoFormat();
        int width = videoFormat.width;
        int height = videoFormat.height;
        int rotationDegrees = videoFormat.rotationDegrees;
        // Switch the width/height if video was taken in portrait mode
        if (rotationDegrees == 90 || rotationDegrees == 270) {
          width = exoPlayer.getVideoFormat().height;
          height = exoPlayer.getVideoFormat().width;
        }
        event.put("width", width);
        event.put("height", height);
      }
      eventSink.success(event);
    }
  }

  void dispose() {
    if (isInitialized) {
      exoPlayer.stop();
    }
    textureEntry.release();
    eventChannel.setStreamHandler(null);
    if (surface != null) {
      surface.release();
    }
    if (exoPlayer != null) {
      // Здесь обязательно удалить как миниум playbackStatsListener до вызова release, чтоб не было крэша
      // https://github.com/google/ExoPlayer/issues/8772
      exoPlayer.removeAnalyticsListener(playbackStatsListener);
      exoPlayer.removeAnalyticsListener(holeAnalyticsListener);
      holeAnalyticsListener.dispose();
      exoPlayer.release();
    }
  }
}
