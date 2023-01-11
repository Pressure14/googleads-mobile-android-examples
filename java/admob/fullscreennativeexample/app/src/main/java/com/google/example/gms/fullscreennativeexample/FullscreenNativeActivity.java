//
//  Copyright (C) 2023 Google LLC
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package com.google.example.gms.fullscreennativeexample;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaAspectRatio;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import java.util.Locale;

/** A simple activity class that displays native ad formats. */
public class FullscreenNativeActivity extends FragmentActivity {

  private static final String TAG = "FullscreenNativeActivity";
  private static final String ADMOB_AD_UNIT_ID = "ca-app-pub-3940256099942544/7342230711";

  private Button refresh;
  private CheckBox startVideoAdsMuted;
  private TextView videoStatus;
  private NativeAd nativeAd;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.fullscreen_native_activity);

    // Initialize the Mobile Ads SDK.
    MobileAds.initialize(
        this,
        new OnInitializationCompleteListener() {
          @Override
          public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });

    refresh = findViewById(R.id.btn_refresh);
    videoStatus = findViewById(R.id.tv_video_status);
    refresh.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View unusedView) {
            refreshAd();
          }
        });

    refreshAd();
  }

  /**
   * Creates a request for a new native ad based on the boolean parameters and calls the
   * corresponding "populate" method when one is successfully returned.
   */
  private void refreshAd() {
    refresh.setEnabled(false);

    AdLoader.Builder builder = new AdLoader.Builder(this, ADMOB_AD_UNIT_ID);
    builder.forNativeAd(
        new NativeAd.OnNativeAdLoadedListener() {
          // OnLoadedListener implementation.
          @Override
          public void onNativeAdLoaded(final NativeAd nativeAd) {
            // If this callback occurs after the activity is destroyed, you must call
            // destroy and return or you may get a memory leak.
            boolean isDestroyed = false;
            refresh.setEnabled(true);
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
              isDestroyed = isDestroyed();
            }
            if (isDestroyed || isFinishing() || isChangingConfigurations()) {
              nativeAd.destroy();
              return;
            }
            // You must call destroy on old ads when you are done with them,
            // otherwise you will have a memory leak.
            if (FullscreenNativeActivity.this.nativeAd != null) {
              FullscreenNativeActivity.this.nativeAd.destroy();
            }
            FullscreenNativeActivity.this.nativeAd = nativeAd;
            FrameLayout frameLayout = findViewById(R.id.fl_adplaceholder);
            NativeAdView adView =
                (NativeAdView) getLayoutInflater().inflate(R.layout.ad_unified, null);
            AdsViewHolder.populateNativeAdView(nativeAd, adView);
            frameLayout.removeAllViews();
            frameLayout.addView(adView);
          }
        });

    VideoOptions videoOptions =
        new VideoOptions.Builder().setStartMuted(true).setCustomControlsRequested(true).build();

    NativeAdOptions adOptions =
        new NativeAdOptions.Builder()
            .setMediaAspectRatio(MediaAspectRatio.PORTRAIT)
            .setVideoOptions(videoOptions)
            .build();

    builder.withNativeAdOptions(adOptions);

    AdLoader adLoader =
        builder
            .withAdListener(
                new AdListener() {
                  @Override
                  public void onAdFailedToLoad(LoadAdError loadAdError) {
                    refresh.setEnabled(true);
                    String error =
                        String.format(
                            Locale.ENGLISH,
                            "domain: %s, code: %d, message: %s",
                            loadAdError.getDomain(),
                            loadAdError.getCode(),
                            loadAdError.getMessage());
                    Toast.makeText(
                            FullscreenNativeActivity.this,
                            "Failed to load native ad with error " + error,
                            Toast.LENGTH_SHORT)
                        .show();
                  }
                })
            .build();

    adLoader.loadAd(new AdRequest.Builder().build());
  }

  @Override
  protected void onDestroy() {
    if (nativeAd != null) {
      nativeAd.destroy();
    }
    super.onDestroy();
  }
}
