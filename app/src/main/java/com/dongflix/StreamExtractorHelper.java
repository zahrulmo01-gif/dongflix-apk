package com.dongflix;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WatchActivity extends AppCompatActivity implements SurfaceHolder.Callback {

	public static final String EXTRA_SERVER = "server";
	public static final String EXTRA_VIDEO_URL = "video_url";
	public static final String EXTRA_TITLE = "title";

	private FrameLayout playerContainer;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private WebView snifferWebView;
	private ProgressBar loadingProgressBar;
	private TextView tvStatus;
	private TextView tvQualityBadge;
	private TextView tvCurrentTime;
	private TextView tvTotalTime;
	private SeekBar seekBar;
	private ImageButton btnPlayPause;
	private ImageButton btnFullscreen;
	private ImageButton btnQuality;

	private MediaPlayer mediaPlayer;
	private boolean isSurfaceReady = false;
	private boolean isPrepared = false;
	private boolean isFullscreen = false;
	private boolean isSniffCompleted = false;

	private String currentServer = "";
	private String currentSourceUrl = "";
	private String activeReferer = "";
	private String activeCookie = "";

	private ArrayList<String> availableQualities = new ArrayList<>();
	private ArrayList<String> availableStreamUrls = new ArrayList<>();
	private int selectedQualityIndex = 0;

	private final Handler uiHandler = new Handler(Looper.getMainLooper());
	private final Handler sniffTimeoutHandler = new Handler(Looper.getMainLooper());

	private final Runnable progressRunnable = new Runnable() {
		@Override
		public void run() {
			if (mediaPlayer != null && isPrepared) {
				try {
					if (mediaPlayer.isPlaying()) {
						int pos = mediaPlayer.getCurrentPosition();
						int dur = mediaPlayer.getDuration();
						if (dur > 0) {
							seekBar.setMax(dur);
							seekBar.setProgress(pos);
							tvCurrentTime.setText(formatDurationMs(pos));
							tvTotalTime.setText(formatDurationMs(dur));
						}
					}
				} catch (Exception ignored) {}
			}
			uiHandler.postDelayed(this, 500);
		}
	};

	private final Runnable sniffTimeoutRunnable = new Runnable() {
		@Override
		public void run() {
			if (!isSniffCompleted && !isFinishing() && !isDestroyed()) {
				tvStatus.setText("Stream tidak merespons, coba ganti server.");
				setLoading(false);
			}
		}
	};

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_watch);

		initViews();
		setupMediaControls();
		setupSnifferWebView();

		Intent intent = getIntent();
		if (intent != null) {
			currentServer = safeTrim(intent.getStringExtra(EXTRA_SERVER));
			currentSourceUrl = safeTrim(intent.getStringExtra(EXTRA_VIDEO_URL));
		}

		if (currentSourceUrl.isEmpty()) {
			tvStatus.setText("URL video kosong.");
			Toast.makeText(this, "URL video tidak ditemukan", Toast.LENGTH_SHORT).show();
			return;
		}

		startExtraction(currentServer, currentSourceUrl);
	}

	private void initViews() {
		playerContainer = findViewById(R.id.playerContainer);
		surfaceView = findViewById(R.id.surfaceView);
		snifferWebView = findViewById(R.id.snifferWebView);
		loadingProgressBar = findViewById(R.id.loadingProgressBar);
		tvStatus = findViewById(R.id.tvStatus);
		tvQualityBadge = findViewById(R.id.tvQualityBadge);
		tvCurrentTime = findViewById(R.id.tvCurrentTime);
		tvTotalTime = findViewById(R.id.tvTotalTime);
		seekBar = findViewById(R.id.seekBar);
		btnPlayPause = findViewById(R.id.btnPlayPause);
		btnFullscreen = findViewById(R.id.btnFullscreen);
		btnQuality = findViewById(R.id.btnQuality);

		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
	}

	private void setupMediaControls() {
		btnPlayPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mediaPlayer == null || !isPrepared) return;
				try {
					if (mediaPlayer.isPlaying()) {
						mediaPlayer.pause();
						btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
					} else {
						mediaPlayer.start();
						btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
						uiHandler.post(progressRunnable);
					}
				} catch (Exception ignored) {}
			}
		});

		btnFullscreen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleFullscreen();
			}
		});

		btnQuality.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (availableStreamUrls.size() <= 1) {
					Toast.makeText(WatchActivity.this, "Hanya tersedia 1 kualitas", Toast.LENGTH_SHORT).show();
					return;
				}
				selectedQualityIndex = (selectedQualityIndex + 1) % availableStreamUrls.size();
				String nextLabel = StreamExtractorHelper.normalizeResolutionToP(availableQualities.get(selectedQualityIndex));
				String nextUrl = availableStreamUrls.get(selectedQualityIndex);
				tvQualityBadge.setText(nextLabel);
				playStreamUrl(nextUrl, activeReferer, activeCookie);
			}
		});

		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser && mediaPlayer != null && isPrepared) {
					try {
						mediaPlayer.seekTo(progress);
						tvCurrentTime.setText(formatDurationMs(progress));
					} catch (Exception ignored) {}
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void setupSnifferWebView() {
		if (snifferWebView == null) return;
		WebSettings settings = snifferWebView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setDomStorageEnabled(true);
		settings.setDatabaseEnabled(true);
		settings.setAllowFileAccess(false);
		settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
		settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			settings.setMediaPlaybackRequiresUserGesture(false);
		}

		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.setAcceptCookie(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			cookieManager.setAcceptThirdPartyCookies(snifferWebView, true);
		}

		snifferWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				String reqUrl = (request != null && request.getUrl() != null) ? request.getUrl().toString() : "";
				if (handlePotentialStreamUrl(reqUrl)) {
					return true;
				}
				return false;
			}

			@Override
			@SuppressWarnings("deprecation")
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (handlePotentialStreamUrl(url)) {
					return true;
				}
				return false;
			}

			@Override
			public void onLoadResource(WebView view, String url) {
				super.onLoadResource(view, url);
				handlePotentialStreamUrl(url);
			}

			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
				if (request != null && request.getUrl() != null) {
					handlePotentialStreamUrl(request.getUrl().toString());
				}
				return super.shouldInterceptRequest(view, request);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				String autoPlayJs = "(function(){" +
					"try{" +
					"var vids=document.querySelectorAll('video');" +
					"for(var i=0;i<vids.length;i++){" +
					"if(vids[i].src){window.AndroidSniffer&&window.AndroidSniffer.onMediaFound(vids[i].src);}" +
					"vids[i].muted=true;vids[i].play().catch(function(){});" +
					"}" +
					"}catch(e){}" +
					"})();";
				view.evaluateJavascript(autoPlayJs, null);
			}
		});

		snifferWebView.addJavascriptInterface(new SnifferBridge(), "AndroidSniffer");
	}

	private class SnifferBridge {
		@android.webkit.JavascriptInterface
		public void onMediaFound(final String foundUrl) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					handlePotentialStreamUrl(foundUrl);
				}
			});
		}
	}

	private boolean handlePotentialStreamUrl(String candidateUrl) {
		if (isSniffCompleted || isFinishing() || isDestroyed() || candidateUrl == null) return false;
		String trimmed = candidateUrl.trim();
		if (trimmed.isEmpty() || !trimmed.startsWith("http")) return false;

		String lower = trimmed.toLowerCase(Locale.US);
		boolean looksLikeMedia = lower.contains(".m3u8") || lower.contains(".mp4");
		boolean isPlaylistOrSegment = lower.contains(".ts") || lower.contains(".m4s") || lower.contains("seg-");

		if (looksLikeMedia && !isPlaylistOrSegment) {
			isSniffCompleted = true;
			sniffTimeoutHandler.removeCallbacks(sniffTimeoutRunnable);
			stopSnifferWebView();

			availableQualities.clear();
			availableStreamUrls.clear();
			String resLabel = StreamExtractorHelper.normalizeResolutionToP(trimmed);
			availableQualities.add(resLabel);
			availableStreamUrls.add(trimmed);
			selectedQualityIndex = 0;

			activeCookie = CookieManager.getInstance().getCookie(trimmed);
			if (activeCookie == null) activeCookie = "";

			tvQualityBadge.setText(resLabel);
			playStreamUrl(trimmed, activeReferer, activeCookie);
			return true;
		}
		return false;
	}

	public void startExtraction(String server, String videoUrl) {
		isSniffCompleted = false;
		availableQualities.clear();
		availableStreamUrls.clear();
		selectedQualityIndex = 0;
		activeReferer = "";
		activeCookie = "";

		setLoading(true);
		tvStatus.setText("Memuat sumber video...");

		StreamExtractorHelper.extract(server, videoUrl, new StreamExtractorHelper.ExtractionCallback() {
			@Override
			public void onSuccess(ArrayList<String> qualityLabels, ArrayList<String> streamUrls, String referer, String dmCookie) {
				if (isFinishing() || isDestroyed()) return;
				isSniffCompleted = true;
				sniffTimeoutHandler.removeCallbacks(sniffTimeoutRunnable);
				stopSnifferWebView();

				availableQualities.clear();
				availableStreamUrls.clear();

				if (streamUrls != null && !streamUrls.isEmpty()) {
					for (int i = 0; i < streamUrls.size(); i++) {
						String u = streamUrls.get(i);
						if (u != null && !u.trim().isEmpty()) {
							String rawLabel = (qualityLabels != null && i < qualityLabels.size()) ? qualityLabels.get(i) : u;
							availableQualities.add(StreamExtractorHelper.normalizeResolutionToP(rawLabel));
							availableStreamUrls.add(u.trim());
						}
					}
				}

				activeReferer = safeTrim(referer);
				activeCookie = safeTrim(dmCookie);

				if (availableStreamUrls.isEmpty()) {
					tvStatus.setText("Stream tidak ditemukan.");
					setLoading(false);
					return;
				}

				selectedQualityIndex = 0;
				tvQualityBadge.setText(availableQualities.get(0));
				playStreamUrl(availableStreamUrls.get(0), activeReferer, activeCookie);
			}

			@Override
			public void onNeedSniffer(String snifferUrl, String referer) {
				if (isFinishing() || isDestroyed()) return;
				isSniffCompleted = false;
				activeReferer = safeTrim(referer);
				tvStatus.setText("Menyiapkan player...");
				setLoading(true);

				if (snifferWebView != null) {
					snifferWebView.stopLoading();
					Map<String, String> headers = new HashMap<>();
					if (!activeReferer.isEmpty()) {
						headers.put("Referer", activeReferer);
					}
					if (!headers.isEmpty()) {
						snifferWebView.loadUrl(snifferUrl, headers);
					} else {
						snifferWebView.loadUrl(snifferUrl);
					}
				}

				sniffTimeoutHandler.removeCallbacks(sniffTimeoutRunnable);
				sniffTimeoutHandler.postDelayed(sniffTimeoutRunnable, 20000);
			}
		});
	}

	private void playStreamUrl(String streamUrl, String referer, String cookie) {
		if (streamUrl == null || streamUrl.trim().isEmpty()) return;
		releaseMediaPlayer();
		isPrepared = false;
		setLoading(true);
		tvStatus.setText("Menghubungkan ke stream...");

		try {
			mediaPlayer = new MediaPlayer();
			Map<String, String> headers = new HashMap<>();
			headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
			if (referer != null && !referer.trim().isEmpty()) {
				headers.put("Referer", referer.trim());
			}
			if (cookie != null && !cookie.trim().isEmpty()) {
				headers.put("Cookie", cookie.trim());
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mediaPlayer.setDataSource(this, Uri.parse(streamUrl.trim()), headers);
			} else {
				mediaPlayer.setDataSource(streamUrl.trim());
			}

			if (isSurfaceReady && surfaceHolder != null) {
				mediaPlayer.setDisplay(surfaceHolder);
			}

			mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					if (isFinishing() || isDestroyed()) return;
					isPrepared = true;
					setLoading(false);
					tvStatus.setText("");
					seekBar.setMax(mp.getDuration());
					tvTotalTime.setText(formatDurationMs(mp.getDuration()));
					mp.start();
					btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
					uiHandler.removeCallbacks(progressRunnable);
					uiHandler.post(progressRunnable);
				}
			});

			mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					if (isFinishing() || isDestroyed()) return true;
					isPrepared = false;
					setLoading(false);
					tvStatus.setText("Gagal memutar stream (" + what + "/" + extra + ")");
					return true;
				}
			});

			mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
				}
			});

			mediaPlayer.prepareAsync();
		} catch (Exception e) {
			setLoading(false);
			tvStatus.setText("Error memuat pemutar video.");
		}
	}

	private void stopSnifferWebView() {
		if (snifferWebView != null) {
			try {
				snifferWebView.stopLoading();
				snifferWebView.loadUrl("about:blank");
			} catch (Exception ignored) {}
		}
	}

	private void releaseMediaPlayer() {
		uiHandler.removeCallbacks(progressRunnable);
		if (mediaPlayer != null) {
			try {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
				}
			} catch (Exception ignored) {}
			try {
				mediaPlayer.reset();
				mediaPlayer.release();
			} catch (Exception ignored) {}
			mediaPlayer = null;
		}
		isPrepared = false;
	}

	private void setLoading(boolean loading) {
		if (loadingProgressBar != null) {
			loadingProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
		}
	}

	private void toggleFullscreen() {
		isFullscreen = !isFullscreen;
		if (isFullscreen) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			if (getSupportActionBar() != null) {
				getSupportActionBar().hide();
			}
			if (playerContainer != null) {
				ViewGroup.LayoutParams lp = playerContainer.getLayoutParams();
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				playerContainer.setLayoutParams(lp);
			}
		} else {
			exitFullscreenLayout();
		}
	}

	private void exitFullscreenLayout() {
		isFullscreen = false;
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		if (getSupportActionBar() != null) {
			getSupportActionBar().show();
		}
		if (playerContainer != null) {
			ViewGroup.LayoutParams lp = playerContainer.getLayoutParams();
			lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
			lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			playerContainer.setLayoutParams(lp);
		}
	}

	@Override
	public void onBackPressed() {
		if (isFullscreen) {
			exitFullscreenLayout();
			return;
		}
		FragmentManager fm = getSupportFragmentManager();
		if (!fm.isDestroyed() && fm.getBackStackEntryCount() > 0) {
			fm.popBackStackImmediate();
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		surfaceHolder = holder;
		isSurfaceReady = true;
		if (mediaPlayer != null) {
			try {
				mediaPlayer.setDisplay(surfaceHolder);
			} catch (Exception ignored) {}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		surfaceHolder = holder;
		isSurfaceReady = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		isSurfaceReady = false;
		if (mediaPlayer != null) {
			try {
				mediaPlayer.setDisplay(null);
			} catch (Exception ignored) {}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mediaPlayer != null && isPrepared) {
			try {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.pause();
					btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
				}
			} catch (Exception ignored) {}
		}
		uiHandler.removeCallbacks(progressRunnable);
	}

	@Override
	protected void onDestroy() {
		uiHandler.removeCallbacksAndMessages(null);
		sniffTimeoutHandler.removeCallbacksAndMessages(null);
		releaseMediaPlayer();
		if (snifferWebView != null) {
			try {
				snifferWebView.stopLoading();
				snifferWebView.removeJavascriptInterface("AndroidSniffer");
				snifferWebView.setWebViewClient(null);
				snifferWebView.destroy();
			} catch (Exception ignored) {}
			snifferWebView = null;
		}
		super.onDestroy();
	}

	private static String safeTrim(String s) {
		return (s == null) ? "" : s.trim();
	}

	private static String formatDurationMs(int ms) {
		if (ms <= 0) return "00:00";
		int totalSec = ms / 1000;
		int mins = totalSec / 60;
		int secs = totalSec % 60;
		return String.format(Locale.US, "%02d:%02d", mins, secs);
	}
}
