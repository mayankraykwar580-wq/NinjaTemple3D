package com.ninjatemple.game;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;

public class MainActivity extends AppCompatActivity {

    private static final String BANNER_AD_ID = "ca-app-pub-6891313948585357/4867518838";
    private static final String INTERSTITIAL_AD_ID = "ca-app-pub-6891313948585357/4867518838";

    private WebView webView;
    private AdView bannerAdView;
    private InterstitialAd interstitialAd;
    private int gameOverCount = 0;

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        LinearLayout.LayoutParams wvParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        webView.setLayoutParams(wvParams);
        webView.setBackgroundColor(Color.BLACK);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.addJavascriptInterface(new AdBridge(), "AndroidAds");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        bannerAdView = new AdView(this);
        bannerAdView.setAdSize(AdSize.BANNER);
        bannerAdView.setAdUnitId(BANNER_AD_ID);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bannerAdView.setLayoutParams(bp);

        root.addView(webView);
        root.addView(bannerAdView);
        setContentView(root);

        MobileAds.initialize(this, status -> {
            AdRequest req = new AdRequest.Builder().build();
            bannerAdView.setAdListener(new AdListener() {
                @Override public void onAdFailedToLoad(LoadAdError e) { bannerAdView.setVisibility(View.GONE); }
            });
            bannerAdView.loadAd(req);
            loadInterstitial();
        });

        webView.loadUrl("file:///android_asset/game.html");
    }

    void loadInterstitial() {
        InterstitialAd.load(this, INTERSTITIAL_AD_ID, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(InterstitialAd ad) {
                interstitialAd = ad;
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() { interstitialAd = null; loadInterstitial(); }
                    @Override public void onAdFailedToShowFullScreenContent(AdError e) { interstitialAd = null; loadInterstitial(); }
                });
            }
            @Override public void onAdFailedToLoad(LoadAdError e) { interstitialAd = null; }
        });
    }

    public class AdBridge {
        @JavascriptInterface
        public void onGameOver() {
            gameOverCount++;
            if (gameOverCount % 2 == 0) runOnUiThread(() -> { if (interstitialAd != null) interstitialAd.show(MainActivity.this); });
        }
        @JavascriptInterface
        public void onLevelComplete() { runOnUiThread(() -> { if (interstitialAd != null) interstitialAd.show(MainActivity.this); }); }
    }

    @Override protected void onResume() { super.onResume(); bannerAdView.resume(); webView.onResume(); }
    @Override protected void onPause() { super.onPause(); bannerAdView.pause(); webView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); bannerAdView.destroy(); webView.destroy(); }
}
