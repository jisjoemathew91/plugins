// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import <Flutter/Flutter.h>
#import <AVFoundation/AVFoundation.h>

@interface FLTVideoPlayerPlugin : NSObject <FlutterPlugin>
- (instancetype)initWithRegistrar:(NSObject<FlutterPluginRegistrar> *)registrar;
@end

@interface FLTVideoPlayer : NSObject <FlutterTexture, FlutterStreamHandler>
@property(readonly, nonatomic) AVPlayer* player;
- (void)play;
- (void)pause;
@end
