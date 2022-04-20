package io.flutter.plugins.videoplayer;

import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_UNEXPECTED;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.upstream.HttpDataSource;

public class HoleErrorBuilder {
  private final StringBuilder sb;

  public HoleErrorBuilder() {
    sb = new StringBuilder("{");
  }

  public void parseError(final ExoPlaybackException error) {
    appendErrorType(error.type);
    append("message", error.getMessage());

    if (error.type == TYPE_RENDERER) {
      append("rendererFormat", error.rendererFormat);
      append("rendererFormatSupport", error.rendererFormatSupport);

      String formatSupport = C.getFormatSupportString(error.rendererFormatSupport);
      append("rendererFormatSupportDescription", formatSupport);

      append("rendererName", error.rendererName);

      append("rawException", error.getRendererException());
    }
    if (error.type == TYPE_SOURCE) {
      if (error.getSourceException() instanceof UnrecognizedInputFormatException) {
        UnrecognizedInputFormatException e =
            (UnrecognizedInputFormatException) error.getSourceException();

        append("uri", e.uri);
        append("cause", e.getCause());
      } else if (error.getSourceException() instanceof HttpDataSource.HttpDataSourceException) {
        HttpDataSource.HttpDataSourceException e =
            (HttpDataSource.HttpDataSourceException) error.getSourceException();

        if (e instanceof HttpDataSource.InvalidContentTypeException) {
          append("contentType", ((HttpDataSource.InvalidContentTypeException) e).contentType);
        }

        if (e instanceof HttpDataSource.InvalidResponseCodeException) {
          append("responseCode", ((HttpDataSource.InvalidResponseCodeException) e).responseCode);
        }
        append("dataSourceType", e.type);
        appendDataSourceDescription(e.type);
        append("uri", e.dataSpec.uri);
      } else {
        append("generalSourceException", error.getSourceException());
      }
      append("rawException", error.getSourceException());
    }
    if (error.type == TYPE_UNEXPECTED) {
      append("rawException", error.getUnexpectedException());
    }
  }

  private void append(String field, Object value) {
    if (sb.length() > 1) {
      sb.append(", \"").append(field).append("\": ").append("\"").append(value).append("\"");
    } else {
      sb.append("\"").append(field).append("\": ").append("\"").append(value).append("\"");
    }
  }

  private void appendDataSourceDescription(int errorType) {
    String errorDescription;
    switch (errorType) {
      case HttpDataSource.HttpDataSourceException.TYPE_CLOSE:
        errorDescription = "TYPE_CLOSE";
        break;
      case HttpDataSource.HttpDataSourceException.TYPE_OPEN:
        errorDescription = "TYPE_OPEN";
        break;
      case HttpDataSource.HttpDataSourceException.TYPE_READ:
        errorDescription = "TYPE_READ";
        break;
      default:
        throw new IllegalStateException();
    }
    append("dataSourceTypeDescription", errorDescription);
  }

  private void appendErrorType(int errorType) {
    String errorDescription;
    switch (errorType) {
      case ExoPlaybackException.TYPE_REMOTE:
        errorDescription = "TYPE_REMOTE";
        break;
      case ExoPlaybackException.TYPE_RENDERER:
        errorDescription = "TYPE_RENDERER";
        break;
      case ExoPlaybackException.TYPE_SOURCE:
        errorDescription = "TYPE_SOURCE";
        break;
      case ExoPlaybackException.TYPE_UNEXPECTED:
        errorDescription = "TYPE_UNEXPECTED";
        break;
      default:
        throw new IllegalStateException();
    }
    append("errorType", errorDescription);
  }

  @NonNull
  @Override
  public String toString() {
    sb.append("}");
    return sb.toString();
  }
}
