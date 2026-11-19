package com.dongflix;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class WatchActivity extends AppCompatActivity {

	private ArrayList<HashMap<String, Object>> eps = new ArrayList<>();
	private RequestNetwork req_watch;
	private RequestNetwork.RequestListener _req_watch_request_listener;

	private TextureView playerTextureView;
	private WebView fallbackWebView;
	private FrameLayout layoutPlayer;
	private View playerControlsOverlay, gradientTop, gradientBottom, topBar, bottomControls, dividerLineView;
	private ImageView btnBack, btnCast, btnLock, btnSettings, btnPrevEpisode, btnRewind, btnPlay, btnForward, btnNextEpisode, btnFullscreen, imagePoster, iconExpandDetail;
	private TextView textPlayerTitle, textTime, btnFullscreenEpisodes, btnQuality, textJudul, textRating, textYear, textGenre, textActionServer, textSortEpisodes;
	private LinearLayout animSeekLeft, animSeekRight, btnUnlockScreen, pillSpeedHold, btnFill, btnOpenDetail, btnSortEpisodes, btnActionServer, btnActionDownload, btnActionShare, btnActionReport;
	private ProgressBar playerLoading, miniVideoProgressBar;
	private SeekBar playerSeekBar;
	private RecyclerView recyclerEpisode, recyclerRekomendasi;

	private String detailTitle = "";
	private String detailSynopsis = "";
	private String detailStatus = "Ongoing";
	private String detailYear = "2026";
	private String detailFirstGenre = "-";
	private String detailAllGenres = "-";
	private String detailCover = "";
	private ArrayList<String> currentGenresList = new ArrayList<>();

	private boolean isNewestFirst = true;
	private boolean isScreenLocked = false;
	private Runnable hideUnlockBtnRunnable;

	private boolean isHolding2xSpeed = false;

	private ValueAnimator shimmerAnimator;
	private float shimmerPhase = 0f;
	private boolean isDetailLoading = true;
	private boolean isEpisodesLoading = true;
	private boolean isRekomLoading = true;

	public MediaPlayer mediaPlayer;
	public Handler playerHandler;
	public Surface playerSurface;
	public Runnable progressLoop;
	public Runnable hideControlsRunnable;
	public Runnable autoDowngradeRunnable;
	public boolean areControlsVisible = true;
	public String currentPlayingUrl = "";
	public String currentStreamReferer = "";
	public boolean isFullscreen = false;
	public int savedSeekPosition = 0;

	public int currentVideoWidth = 0;
	public int currentVideoHeight = 0;
	public boolean isFillModeActive = false;

	public boolean isAutoQualityMode = true;
	public int selectedQualityIndex = 0;
	public float currentPlaybackSpeed = 1.0f;

	public int currentEpisodeIndex = 0;
	public ArrayList<HashMap<String, Object>> episodeList = new ArrayList<>();

	public ArrayList<String> currentEpisodeServerNames = new ArrayList<>();
	public ArrayList<String> currentEpisodeServerUrls = new ArrayList<>();
	public int selectedServerIndex = 0;

	public ArrayList<String> qualityNames = new ArrayList<>();
	public ArrayList<String> qualityUrls = new ArrayList<>();

	public String dmSessionCookie = "";
	private boolean isWebViewFallbackActive = false;

	private static class ScoredAnimeItem {
		final HashMap<String, Object> item;
		final int score;

		ScoredAnimeItem(HashMap<String, Object> item, int score) {
			this.item = item;
			this.score = score;
		}
	}

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_watch);
		initialize(_savedInstanceState);
		initializeLogic();
	}

	private void initialize(Bundle _savedInstanceState) {
		req_watch = new RequestNetwork(this);

		playerTextureView = findViewById(R.id.playerTextureView);
		layoutPlayer = findViewById(R.id.layoutPlayer);
		playerControlsOverlay = findViewById(R.id.playerControlsOverlay);
		gradientTop = findViewById(R.id.gradientTop);
		gradientBottom = findViewById(R.id.gradientBottom);
		topBar = findViewById(R.id.topBar);
		bottomControls = findViewById(R.id.bottomControls);
		dividerLineView = findViewById(R.id.view139);
		miniVideoProgressBar = findViewById(R.id.miniVideoProgressBar);

		animSeekLeft = findViewById(R.id.animSeekLeft);
		animSeekRight = findViewById(R.id.animSeekRight);
		btnUnlockScreen = findViewById(R.id.btnUnlockScreen);
		pillSpeedHold = findViewById(R.id.pillSpeedHold);

		btnBack = findViewById(R.id.btnBack);
		btnCast = findViewById(R.id.btnCast);
		btnLock = findViewById(R.id.btnLock);
		btnSettings = findViewById(R.id.btnSettings);
		btnPrevEpisode = findViewById(R.id.btnPrevEpisode);
		btnRewind = findViewById(R.id.btnRewind);
		btnPlay = findViewById(R.id.btnPlay);
		btnForward = findViewById(R.id.btnForward);
		btnNextEpisode = findViewById(R.id.btnNextEpisode);
		btnFullscreen = findViewById(R.id.btnFullscreen);
		imagePoster = findViewById(R.id.imagePoster);
		iconExpandDetail = findViewById(R.id.iconExpandDetail);

		textPlayerTitle = findViewById(R.id.textPlayerTitle);
		textTime = findViewById(R.id.textTime);
		btnFullscreenEpisodes = findViewById(R.id.btnFullscreenEpisodes);
		btnQuality = findViewById(R.id.btnQuality);
		textJudul = findViewById(R.id.textJudul);
		textRating = findViewById(R.id.textRating);
		textYear = findViewById(R.id.textYear);
		textGenre = findViewById(R.id.textGenre);
		textActionServer = findViewById(R.id.textActionServer);
		textSortEpisodes = findViewById(R.id.textSortEpisodes);

		btnFill = findViewById(R.id.btnFill);
		btnOpenDetail = findViewById(R.id.btnOpenDetail);
		btnSortEpisodes = findViewById(R.id.btnSortEpisodes);
		btnActionServer = findViewById(R.id.btnActionServer);
		btnActionDownload = findViewById(R.id.btnActionDownload);
		btnActionShare = findViewById(R.id.btnActionShare);
		btnActionReport = findViewById(R.id.btnActionReport);

		playerLoading = findViewById(R.id.playerLoading);
		playerSeekBar = findViewById(R.id.playerSeekBar);
		recyclerEpisode = findViewById(R.id.recyclerEpisode);
		recyclerRekomendasi = findViewById(R.id.recyclerRekomendasi);

		_req_watch_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				if (_tag.equals("slug")) {
					isEpisodesLoading = false;
					eps = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
					if (eps != null && !eps.isEmpty()) {
						Collections.reverse(eps);
						isNewestFirst = true;
						if (textSortEpisodes != null) textSortEpisodes.setText("Terbaru");

						if (recyclerEpisode != null) {
							recyclerEpisode.setAdapter(new RecyclerEpisodeAdapter(eps));
						}
						episodeList = eps;
						currentEpisodeIndex = 0;

						if (playerTextureView != null && playerTextureView.isAvailable()) {
							playEpisodeByIndex(0);
						} else if (playerTextureView != null) {
							playerTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
								@Override
								public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
									playerSurface = new Surface(st);
									playEpisodeByIndex(0);
								}
								@Override public void onSurfaceTextureSizeChanged(SurfaceTexture s, int w, int h) {}
								@Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s) { return true; }
								@Override public void onSurfaceTextureUpdated(SurfaceTexture s) {}
							});
						}
					}
					checkStopShimmerIfDone();
				} else {
					isDetailLoading = false;
					isRekomLoading = false;
					clearDetailShimmer();

					String targetSlug = getIntent().getStringExtra("slug");
					if (targetSlug != null && !targetSlug.isEmpty()) {
						try {
							JSONArray jsonArray = new JSONArray(_response);
							for (int i = 0; i < jsonArray.length(); i++) {
								JSONObject obj = jsonArray.getJSONObject(i);
								if (targetSlug.equals(obj.optString("slug"))) {
									detailTitle = obj.optString("title");
									detailSynopsis = obj.optString("synopsis")
											.replaceAll("\\[[^\\x20-\\x7E]+\\]", "")
											.replaceAll("[^\\x20-\\x7E\\n]", "")
											.trim();
									detailStatus = obj.optString("status", "Ongoing");
									if (detailStatus.isEmpty()) detailStatus = "Ongoing";

									detailCover = obj.optString("cover").trim();
									if (detailCover.contains("i.ibb.co.com")) {
										detailCover = detailCover.replace("i.ibb.co.com", "i.ibb.co");
									}
									if (detailCover.startsWith("http://")) {
										detailCover = detailCover.replace("http://", "https://");
									}
									String released = obj.optString("released");

									detailFirstGenre = "-";
									currentGenresList.clear();
									StringBuilder allGenresSb = new StringBuilder();
									JSONArray genresArray = obj.optJSONArray("genres");
									if (genresArray != null && genresArray.length() > 0) {
										detailFirstGenre = genresArray.optString(0);
										for (int g = 0; g < genresArray.length(); g++) {
											String gStr = genresArray.optString(g).trim();
											if (!gStr.isEmpty()) {
												currentGenresList.add(gStr.toLowerCase());
												if (allGenresSb.length() > 0) allGenresSb.append(", ");
												allGenresSb.append(gStr);
											}
										}
									}
									detailAllGenres = allGenresSb.length() > 0 ? allGenresSb.toString() : detailFirstGenre;

									if (released.length() >= 4) {
										detailYear = released.substring(released.length() - 4);
									}

									if (textJudul != null) textJudul.setText(detailTitle);
									if (textRating != null) textRating.setText(detailStatus);
									if (textYear != null) textYear.setText(detailYear);
									if (textGenre != null) textGenre.setText(detailFirstGenre);

									if (imagePoster != null && !detailCover.isEmpty()) {
										Glide.with(getApplicationContext()).load(Uri.parse(detailCover)).into(imagePoster);
									}
									break;
								}
							}

							ArrayList<HashMap<String, Object>> allList = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
							if (allList != null && recyclerRekomendasi != null) {
								ArrayList<HashMap<String, Object>> smartRekomList = getSmartRecommendations(allList, targetSlug, 6);
								recyclerRekomendasi.setAdapter(new RecyclerRekomendasiAdapter(smartRekomList));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					checkStopShimmerIfDone();
				}
			}

			@Override
			public void onErrorResponse(String _param1, String _param2) {
				if (_param1.equals("slug")) isEpisodesLoading = false;
				else {
					isDetailLoading = false;
					isRekomLoading = false;
					clearDetailShimmer();
				}
				checkStopShimmerIfDone();
			}
		};
	}

	private Set<String> extractKeywords(String text) {
		Set<String> keywords = new HashSet<>();
		if (text == null || text.isEmpty()) return keywords;

		Set<String> stopWords = new HashSet<>(Arrays.asList(
			"yang", "dan", "di", "ke", "dari", "ini", "itu", "untuk", "dengan", "pada", "adalah",
			"sebagai", "dalam", "tidak", "akan", "oleh", "ia", "dia", "mereka", "nya", "atau",
			"the", "of", "and", "in", "to", "a", "is", "for", "with", "on", "as", "by", "at",
			"sub", "indo", "episode", "season", "donghua", "anime", "cerita", "kisah", "dunia"
		));

		String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
		for (String t : tokens) {
			if (t.length() >= 3 && !stopWords.contains(t)) {
				keywords.add(t);
			}
		}
		return keywords;
	}

	private ArrayList<HashMap<String, Object>> getSmartRecommendations(
			ArrayList<HashMap<String, Object>> allList,
			String currentSlug,
			int maxCount) {

		final Set<String> baseTitleWords = extractKeywords(detailTitle);
		final Set<String> baseSynopsisWords = extractKeywords(detailSynopsis);
		final Set<String> baseGenres = new HashSet<>(currentGenresList);

		ArrayList<ScoredAnimeItem> scoredItems = new ArrayList<>();

		for (HashMap<String, Object> item : allList) {
			String slug = item.get("slug") != null ? item.get("slug").toString() : "";
			if (slug.isEmpty() || slug.equals(currentSlug)) continue;

			int score = 0;

			Object rawGenres = item.get("genres");
			if (rawGenres instanceof List) {
				for (Object gObj : (List<?>) rawGenres) {
					if (gObj != null && baseGenres.contains(gObj.toString().trim().toLowerCase())) {
						score += 12;
					}
				}
			} else if (rawGenres != null) {
				String gStr = rawGenres.toString().toLowerCase();
				for (String bg : baseGenres) {
					if (gStr.contains(bg)) score += 12;
				}
			}

			String itemTitle = item.get("title") != null ? item.get("title").toString() : "";
			Set<String> itemTitleWords = extractKeywords(itemTitle);
			for (String w : itemTitleWords) {
				if (baseTitleWords.contains(w)) {
					score += 15;
				}
			}

			String itemSynopsis = item.get("synopsis") != null ? item.get("synopsis").toString() : "";
			Set<String> itemSynopsisWords = extractKeywords(itemSynopsis);
			for (String sw : itemSynopsisWords) {
				if (baseSynopsisWords.contains(sw)) {
					score += 4;
				}
			}

			String itemStatus = item.get("status") != null ? item.get("status").toString() : "";
			if (!detailStatus.isEmpty() && detailStatus.equalsIgnoreCase(itemStatus)) {
				score += 2;
			}

			scoredItems.add(new ScoredAnimeItem(item, score));
		}

		Collections.sort(scoredItems, new Comparator<ScoredAnimeItem>() {
			@Override
			public int compare(ScoredAnimeItem a, ScoredAnimeItem b) {
				return Integer.compare(b.score, a.score);
			}
		});

		ArrayList<HashMap<String, Object>> result = new ArrayList<>();
		for (int i = 0; i < Math.min(maxCount, scoredItems.size()); i++) {
			result.add(scoredItems.get(i).item);
		}
		return result;
	}

	private Drawable createShimmerDrawable(final float cornerRadiusPx) {
		return new Drawable() {
			private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Matrix shaderMatrix = new Matrix();
			private LinearGradient gradient;
			private int lastWidth = -1;

			{
				basePaint.setColor(Color.parseColor("#12141A"));
			}

			@Override
			public void draw(Canvas canvas) {
				Rect b = getBounds();
				int w = b.width();
				int h = b.height();
				if (w <= 0 || h <= 0) return;

				RectF rect = new RectF(b);
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, basePaint);

				if (gradient == null || lastWidth != w) {
					lastWidth = w;
					gradient = new LinearGradient(
						0, 0, w, 0,
						new int[]{
							Color.TRANSPARENT,
							Color.parseColor("#1AFFFFFF"),
							Color.parseColor("#33FFFFFF"),
							Color.parseColor("#1AFFFFFF"),
							Color.TRANSPARENT
						},
						new float[]{0f, 0.35f, 0.5f, 0.65f, 1f},
						Shader.TileMode.CLAMP
					);
					shimmerPaint.setShader(gradient);
				}

				float translateX = -w + (2f * w * shimmerPhase);
				shaderMatrix.setTranslate(translateX, 0);
				gradient.setLocalMatrix(shaderMatrix);
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shimmerPaint);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private void startLoadingShimmer() {
		float d = getResources().getDisplayMetrics().density;

		if (imagePoster != null) {
			imagePoster.setImageDrawable(createShimmerDrawable(32 * d));
		}
		if (textJudul != null) {
			textJudul.setText("                    ");
			textJudul.setBackground(createShimmerDrawable(6 * d));
		}
		if (textRating != null) {
			textRating.setText("        ");
			textRating.setBackground(createShimmerDrawable(4 * d));
		}
		if (textYear != null) {
			textYear.setText("      ");
			textYear.setBackground(createShimmerDrawable(4 * d));
		}
		if (textGenre != null) {
			textGenre.setText("        ");
			textGenre.setBackground(createShimmerDrawable(4 * d));
		}

		if (recyclerEpisode != null) {
			recyclerEpisode.setAdapter(new ShimmerEpisodeAdapter(6));
		}

		if (recyclerRekomendasi != null) {
			recyclerRekomendasi.setAdapter(new ShimmerRekomendasiAdapter(6));
		}

		shimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
		shimmerAnimator.setDuration(1250);
		shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
		shimmerAnimator.setInterpolator(new LinearInterpolator());
		shimmerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				shimmerPhase = (float) animation.getAnimatedValue();
				if (isDetailLoading) {
					if (imagePoster != null) imagePoster.invalidate();
					if (textJudul != null) textJudul.invalidate();
					if (textRating != null) textRating.invalidate();
					if (textYear != null) textYear.invalidate();
					if (textGenre != null) textGenre.invalidate();
				}
				if (isEpisodesLoading && recyclerEpisode != null) {
					for (int i = 0; i < recyclerEpisode.getChildCount(); i++) {
						recyclerEpisode.getChildAt(i).invalidate();
					}
				}
				if (isRekomLoading && recyclerRekomendasi != null) {
					for (int i = 0; i < recyclerRekomendasi.getChildCount(); i++) {
						View child = recyclerRekomendasi.getChildAt(i);
						child.invalidate();
						if (child instanceof ViewGroup) {
							ViewGroup vg = (ViewGroup) child;
							for (int j = 0; j < vg.getChildCount(); j++) {
								vg.getChildAt(j).invalidate();
							}
						}
					}
				}
			}
		});
		shimmerAnimator.start();
	}

	private void clearDetailShimmer() {
		if (textJudul != null) textJudul.setBackground(null);
		if (textRating != null) textRating.setBackground(null);
		if (textYear != null) textYear.setBackground(null);
		if (textGenre != null) textGenre.setBackground(null);
	}

	private void checkStopShimmerIfDone() {
		if (!isDetailLoading && !isEpisodesLoading && !isRekomLoading) {
			if (shimmerAnimator != null) {
				shimmerAnimator.cancel();
				shimmerAnimator = null;
			}
		}
	}

	private void initializeLogic() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(0xFF000000);
		}

		View rootWatch = findViewById(R.id.rootWatchLayout);
		if (rootWatch != null) {
			rootWatch.setPadding(0, 0, 0, 0);
		}

		if (btnLock != null) {
			btnLock.setVisibility(View.GONE);
		}

		_WatchUI();
		updateBottomDividerState();

		if (recyclerEpisode != null) {
			recyclerEpisode.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		}

		if (recyclerRekomendasi != null) {
			recyclerRekomendasi.setLayoutManager(new GridLayoutManager(this, 3));
			recyclerRekomendasi.setNestedScrollingEnabled(false);
		}

		startLoadingShimmer();

		String slug = getIntent().getStringExtra("slug");
		if (slug == null) slug = "";

		req_watch.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/", "", _req_watch_request_listener);
		req_watch.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/?slug=" + slug, "slug", _req_watch_request_listener);

		_initPlayerViews();
		applyCustomFonts();
	}

	private void applyCustomFonts() {
		try {
			final Typeface fontReg = Typeface.createFromAsset(getAssets(), "fonts/productsans_reg.ttf");
			final Typeface fontMed = Typeface.createFromAsset(getAssets(), "fonts/productsans_med.ttf");
			final Typeface fontBold = Typeface.createFromAsset(getAssets(), "fonts/productsans_bold.ttf");

			Queue<View> viewQueue = new LinkedList<>();
			viewQueue.add(getWindow().getDecorView().findViewById(android.R.id.content));

			while (!viewQueue.isEmpty()) {
				View currentView = viewQueue.poll();
				if (currentView instanceof TextView) {
					TextView tv = (TextView) currentView;
					Typeface tf = tv.getTypeface();
					int style = (tf != null) ? tf.getStyle() : Typeface.NORMAL;

					if (style == Typeface.BOLD) {
						tv.setTypeface(fontBold, Typeface.NORMAL);
					} else if (style == Typeface.BOLD_ITALIC) {
						tv.setTypeface(fontBold, Typeface.ITALIC);
					} else if (style == Typeface.ITALIC) {
						tv.setTypeface(fontReg, Typeface.ITALIC);
					} else {
						tv.setTypeface(fontMed, Typeface.NORMAL);
					}
				} else if (currentView instanceof ViewGroup) {
					ViewGroup group = (ViewGroup) currentView;
					for (int i = 0; i < group.getChildCount(); i++) {
						viewQueue.add(group.getChildAt(i));
					}
				}
			}
		} catch (Exception ignored) {}
	}

	public void _WatchUI() {
		GradientDrawable topGrad = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{0xCC000000, 0x00000000}
		);
		if (gradientTop != null) gradientTop.setBackground(topGrad);

		GradientDrawable botGrad = new GradientDrawable(
			GradientDrawable.Orientation.BOTTOM_TOP,
			new int[]{0xCC000000, 0x00000000}
		);
		if (gradientBottom != null) gradientBottom.setBackground(botGrad);

		View[] seekPills = {animSeekLeft, animSeekRight, pillSpeedHold};
		for (View pill : seekPills) {
			if (pill != null) {
				GradientDrawable pillBg = new GradientDrawable();
				pillBg.setCornerRadius(100f);
				pillBg.setColor(0xB3000000);
				pill.setBackground(pillBg);
			}
		}

		if (btnUnlockScreen != null) {
			GradientDrawable lockBg = new GradientDrawable();
			lockBg.setCornerRadius(100f);
			lockBg.setColor(0xCC0B0D11);
			btnUnlockScreen.setBackground(lockBg);
		}

		if (btnFullscreenEpisodes != null) {
			btnFullscreenEpisodes.setBackgroundColor(Color.TRANSPARENT);
		}

		if (iconExpandDetail != null) {
			GradientDrawable expandBg = new GradientDrawable();
			expandBg.setShape(GradientDrawable.OVAL);
			expandBg.setColor(Color.parseColor("#12141A"));
			iconExpandDetail.setBackground(expandBg);
		}

		if (btnSortEpisodes != null) {
			GradientDrawable sortBg = new GradientDrawable();
			sortBg.setCornerRadius(100f);
			sortBg.setColor(Color.parseColor("#12141A"));
			btnSortEpisodes.setBackground(sortBg);
		}

		View[] actionBtns = {btnActionServer, btnActionDownload, btnActionShare, btnActionReport};
		for (View btn : actionBtns) {
			if (btn != null) {
				GradientDrawable btnBg = new GradientDrawable();
				btnBg.setCornerRadius(100f);
				btnBg.setColor(Color.parseColor("#12141A"));
				btn.setBackground(btnBg);
			}
		}
	}

	private void updateBottomDividerState() {
		if (isFullscreen) {
			if (dividerLineView != null) dividerLineView.setVisibility(View.GONE);
			if (miniVideoProgressBar != null) miniVideoProgressBar.setVisibility(View.GONE);
		} else if (areControlsVisible) {
			if (dividerLineView != null) dividerLineView.setVisibility(View.VISIBLE);
			if (miniVideoProgressBar != null) miniVideoProgressBar.setVisibility(View.GONE);
		} else {
			if (dividerLineView != null) dividerLineView.setVisibility(View.GONE);
			if (miniVideoProgressBar != null) miniVideoProgressBar.setVisibility(View.VISIBLE);
		}
	}

	private void applyPlaybackSpeedValue(float speed) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
			try {
				boolean wasPlaying = mediaPlayer.isPlaying();
				PlaybackParams params = mediaPlayer.getPlaybackParams();
				if (params == null) params = new PlaybackParams();
				params.setSpeed(speed);
				mediaPlayer.setPlaybackParams(params);
				if (!wasPlaying) mediaPlayer.pause();
			} catch (Exception ignored) {}
		}
	}

	private void startHold2xSpeed() {
		if (isScreenLocked || mediaPlayer == null || !mediaPlayer.isPlaying()) return;
		isHolding2xSpeed = true;
		applyPlaybackSpeedValue(2.0f);
		if (pillSpeedHold != null) {
			pillSpeedHold.setVisibility(View.VISIBLE);
			pillSpeedHold.bringToFront();
		}
	}

	private void stopHold2xSpeed() {
		if (!isHolding2xSpeed) return;
		isHolding2xSpeed = false;
		applyPlaybackSpeedValue(currentPlaybackSpeed);
		if (pillSpeedHold != null) {
			pillSpeedHold.setVisibility(View.GONE);
		}
	}

	public void adjustVideoAspectRatio() {
		if (playerTextureView == null || layoutPlayer == null) return;
		if (currentVideoWidth <= 0 || currentVideoHeight <= 0) return;

		layoutPlayer.post(new Runnable() {
			@Override
			public void run() {
				int containerWidth = layoutPlayer.getWidth();
				int containerHeight = layoutPlayer.getHeight();
				if (containerWidth <= 0 || containerHeight <= 0) return;

				float videoAspect = (float) currentVideoWidth / currentVideoHeight;
				float containerAspect = (float) containerWidth / containerHeight;

				int targetWidth;
				int targetHeight;

				if (isFillModeActive) {
					if (videoAspect > containerAspect) {
						targetHeight = containerHeight;
						targetWidth = Math.round(containerHeight * videoAspect);
					} else {
						targetWidth = containerWidth;
						targetHeight = Math.round(containerWidth / videoAspect);
					}
				} else {
					if (videoAspect > containerAspect) {
						targetWidth = containerWidth;
						targetHeight = Math.round(containerWidth / videoAspect);
					} else {
						targetHeight = containerHeight;
						targetWidth = Math.round(containerHeight * videoAspect);
					}
				}

				FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) playerTextureView.getLayoutParams();
				lp.width = targetWidth;
				lp.height = targetHeight;
				lp.gravity = Gravity.CENTER;
				playerTextureView.setLayoutParams(lp);
				playerTextureView.setScaleX(1.0f);
				playerTextureView.setScaleY(1.0f);
			}
		});
	}

	private void showSeekAnimation(final View animView) {
		if (animView == null) return;
		animView.animate().cancel();
		animView.setVisibility(View.VISIBLE);
		animView.setAlpha(1f);
		animView.animate()
			.alpha(0f)
			.setStartDelay(450)
			.setDuration(250)
			.withEndAction(new Runnable() {
				@Override
				public void run() {
					animView.setVisibility(View.GONE);
				}
			})
			.start();
	}

	private void lockScreenControls() {
		isScreenLocked = true;
		hideControls();
		showUnlockPillTemporarily();
		Toast.makeText(this, "Layar dikunci", Toast.LENGTH_SHORT).show();
	}

	private void unlockScreenControls() {
		isScreenLocked = false;
		if (btnUnlockScreen != null) {
			btnUnlockScreen.setVisibility(View.GONE);
		}
		if (playerHandler != null && hideUnlockBtnRunnable != null) {
			playerHandler.removeCallbacks(hideUnlockBtnRunnable);
		}
		showControls();
		Toast.makeText(this, "Kunci layar dibuka", Toast.LENGTH_SHORT).show();
	}

	private void showUnlockPillTemporarily() {
		if (btnUnlockScreen == null) return;
		btnUnlockScreen.setVisibility(View.VISIBLE);
		btnUnlockScreen.bringToFront();
		if (playerHandler != null && hideUnlockBtnRunnable != null) {
			playerHandler.removeCallbacks(hideUnlockBtnRunnable);
			playerHandler.postDelayed(hideUnlockBtnRunnable, 2500);
		}
	}

	public void toggleSortEpisodes() {
		if (episodeList == null || episodeList.size() <= 1) return;

		HashMap<String, Object> currentlyPlayingItem = episodeList.get(currentEpisodeIndex);
		Collections.reverse(episodeList);
		isNewestFirst = !isNewestFirst;

		for (int i = 0; i < episodeList.size(); i++) {
			if (episodeList.get(i) == currentlyPlayingItem) {
				currentEpisodeIndex = i;
				break;
			}
		}

		if (textSortEpisodes != null) {
			textSortEpisodes.setText(isNewestFirst ? "Terbaru" : "Terlama");
		}

		if (recyclerEpisode != null && recyclerEpisode.getAdapter() != null) {
			recyclerEpisode.getAdapter().notifyDataSetChanged();
			recyclerEpisode.scrollToPosition(currentEpisodeIndex);
		}
		updateEpisodeNavButtons();
	}

	private boolean isLandscapeOrFullscreen() {
		return isFullscreen || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
	}

	private Dialog createAdaptiveSheetDialog(View contentView) {
		return createAdaptiveSheetDialog(contentView, false);
	}

	private Dialog createAdaptiveSheetDialog(View contentView, final boolean allowVideoInteraction) {
		float d = getResources().getDisplayMetrics().density;

		if (isLandscapeOrFullscreen()) {
			final Dialog sideDialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
			sideDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			GradientDrawable panelBg = new GradientDrawable();
			panelBg.setColor(Color.parseColor("#0B0D11"));
			panelBg.setCornerRadii(new float[]{18 * d, 18 * d, 0, 0, 0, 0, 18 * d, 18 * d});
			contentView.setBackground(panelBg);

			if (contentView instanceof ViewGroup && ((ViewGroup) contentView).getChildCount() > 0) {
				View firstChild = ((ViewGroup) contentView).getChildAt(0);
				if (!(firstChild instanceof LinearLayout)) {
					firstChild.setVisibility(View.GONE);
				}
			}
			contentView.setPadding((int)(22 * d), (int)(20 * d), (int)(22 * d), (int)(20 * d));

			sideDialog.setContentView(contentView);
			sideDialog.setCanceledOnTouchOutside(true);

			Window window = sideDialog.getWindow();
			if (window != null) {
				window.setLayout((int)(340 * d), ViewGroup.LayoutParams.MATCH_PARENT);
				window.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
				window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				window.setDimAmount(0.45f);
				window.getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				);
			}

			sideDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (isFullscreen) {
						getWindow().getDecorView().setSystemUiVisibility(
							View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						);
					}
				}
			});

			sideDialog.show();
			return sideDialog;
		} else {
			final BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
			sheetDialog.setContentView(contentView);

			if (allowVideoInteraction) {
				sheetDialog.setCanceledOnTouchOutside(false);
				Window sheetWindow = sheetDialog.getWindow();
				if (sheetWindow != null) {
					sheetWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
					sheetWindow.setDimAmount(0f);
					sheetWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
					sheetWindow.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
				}
			}

			sheetDialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					BottomSheetDialog dialogObj = (BottomSheetDialog) dialog;
					FrameLayout bottomSheet = dialogObj.findViewById(com.google.android.material.R.id.design_bottom_sheet);
					if (bottomSheet != null) {
						bottomSheet.setBackgroundColor(Color.TRANSPARENT);
						bottomSheet.setBackgroundTintList(null);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							bottomSheet.setElevation(0f);
						}

						View decor = getWindow().getDecorView();
						int totalHeight = decor.getHeight();
						int playerBottom = 0;
						if (layoutPlayer != null) {
							int[] loc = new int[2];
							layoutPlayer.getLocationInWindow(loc);
							playerBottom = loc[1] + layoutPlayer.getHeight();
						}

						int targetSheetHeight = totalHeight - playerBottom;
						if (targetSheetHeight > (int)(200 * getResources().getDisplayMetrics().density)) {
							ViewGroup.LayoutParams sheetLp = bottomSheet.getLayoutParams();
							sheetLp.height = targetSheetHeight;
							bottomSheet.setLayoutParams(sheetLp);
						}

						BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
						behavior.setSkipCollapsed(true);
						behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
					}
				}
			});
			sheetDialog.show();
			return sheetDialog;
		}
	}

	private LinearLayout buildAdaptiveHeader(String titleText, final Dialog[] holderDialog) {
		float d = getResources().getDisplayMetrics().density;

		LinearLayout header = new LinearLayout(this);
		header.setOrientation(LinearLayout.HORIZONTAL);
		header.setGravity(Gravity.CENTER_VERTICAL);
		header.setPadding(0, 0, 0, (int)(16 * d));

		TextView tvTitle = new TextView(this);
		tvTitle.setText(titleText);
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(19f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		header.addView(tvTitle);

		TextView btnClose = new TextView(this);
		LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams((int)(32 * d), (int)(32 * d));
		btnClose.setLayoutParams(closeLp);
		btnClose.setGravity(Gravity.CENTER);
		btnClose.setText("✕");
		btnClose.setTextColor(Color.parseColor("#CBD5E1"));
		btnClose.setTextSize(14f);
		btnClose.setTypeface(null, Typeface.BOLD);

		GradientDrawable closeBg = new GradientDrawable();
		closeBg.setShape(GradientDrawable.OVAL);
		closeBg.setColor(Color.parseColor("#181B22"));
		btnClose.setBackground(closeBg);

		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (holderDialog[0] != null) holderDialog[0].dismiss();
			}
		});
		header.addView(btnClose);

		return header;
	}

	public void showSettingsBottomSheet() {
		final Dialog[] dialogHolder = new Dialog[1];
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(24 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0B0D11"));
		sheetBg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(38 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(12 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		root.addView(buildAdaptiveHeader("Pengaturan Pemutaran", dialogHolder));

		String currentQualityLabel = (btnQuality != null) ? btnQuality.getText().toString() : "Auto";
		String currentSpeedLabel = (Math.abs(currentPlaybackSpeed - 1.0f) < 0.01f) ? "Normal" : currentPlaybackSpeed + "x";
		String currentServerLabel = (textActionServer != null) ? textActionServer.getText().toString() : "Default";

		String[] menuTitles = new String[]{"Kualitas", "Kecepatan Putar", "Server Streaming"};
		String[] menuValues = new String[]{currentQualityLabel, currentSpeedLabel, currentServerLabel};

		for (int i = 0; i < menuTitles.length; i++) {
			final int idx = i;
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(Gravity.CENTER_VERTICAL);
			row.setPadding((int)(4 * d), (int)(16 * d), (int)(4 * d), (int)(16 * d));

			TextView tvLeft = new TextView(this);
			tvLeft.setText(menuTitles[i]);
			tvLeft.setTextColor(Color.WHITE);
			tvLeft.setTextSize(15f);
			tvLeft.setTypeface(null, Typeface.BOLD);
			tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
			row.addView(tvLeft);

			TextView tvRight = new TextView(this);
			tvRight.setText(menuValues[i] + "  >");
			tvRight.setTextColor(Color.parseColor("#D1D5DB"));
			tvRight.setTextSize(14f);
			row.addView(tvRight);

			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (dialogHolder[0] != null) dialogHolder[0].dismiss();
					if (idx == 0) showQualityDialog();
					else if (idx == 1) showSpeedDialog();
					else if (idx == 2) showServerDialog();
				}
			});

			root.addView(row);
		}

		dialogHolder[0] = createAdaptiveSheetDialog(root);
	}

	public void showDetailBottomSheet() {
		final Dialog[] dialogHolder = new Dialog[1];
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(28 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0B0D11"));
		sheetBg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(38 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(12 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		root.addView(buildAdaptiveHeader("Sinopsis", dialogHolder));

		ScrollView scrollView = new ScrollView(this);
		scrollView.setVerticalScrollBarEnabled(false);

		LinearLayout content = new LinearLayout(this);
		content.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(content);

		LinearLayout infoHeaderRow = new LinearLayout(this);
		infoHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
		infoHeaderRow.setGravity(Gravity.TOP);
		LinearLayout.LayoutParams infoRowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		infoRowLp.bottomMargin = (int)(18 * d);
		infoHeaderRow.setLayoutParams(infoRowLp);

		CardView posterCard = new CardView(this);
		LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams((int)(96 * d), (int)(134 * d));
		cardLp.rightMargin = (int)(16 * d);
		posterCard.setLayoutParams(cardLp);
		posterCard.setRadius(8 * d);
		posterCard.setCardElevation(0f);
		posterCard.setCardBackgroundColor(Color.parseColor("#12141A"));

		ImageView ivSheetPoster = new ImageView(this);
		ivSheetPoster.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		ivSheetPoster.setScaleType(ImageView.ScaleType.CENTER_CROP);
		if (!detailCover.isEmpty()) {
			Glide.with(getApplicationContext()).load(Uri.parse(detailCover)).into(ivSheetPoster);
		}
		posterCard.addView(ivSheetPoster);
		infoHeaderRow.addView(posterCard);

		LinearLayout rightCol = new LinearLayout(this);
		rightCol.setOrientation(LinearLayout.VERTICAL);
		rightCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		TextView tvSheetTitle = new TextView(this);
		tvSheetTitle.setText(detailTitle.isEmpty() ? textJudul.getText().toString() : detailTitle);
		tvSheetTitle.setTextColor(Color.WHITE);
		tvSheetTitle.setTextSize(18f);
		tvSheetTitle.setTypeface(null, Typeface.BOLD);
		tvSheetTitle.setMaxLines(2);
		tvSheetTitle.setEllipsize(TextUtils.TruncateAt.END);
		rightCol.addView(tvSheetTitle);

		TextView tvSubGenres = new TextView(this);
		tvSubGenres.setText(detailAllGenres);
		tvSubGenres.setTextColor(Color.parseColor("#9CA3AF"));
		tvSubGenres.setTextSize(13f);
		tvSubGenres.setMaxLines(1);
		tvSubGenres.setEllipsize(TextUtils.TruncateAt.END);
		tvSubGenres.setPadding(0, (int)(6 * d), 0, (int)(10 * d));
		rightCol.addView(tvSubGenres);

		LinearLayout badgeRow = new LinearLayout(this);
		badgeRow.setOrientation(LinearLayout.HORIZONTAL);
		badgeRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams badgeRowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		badgeRowLp.bottomMargin = (int)(12 * d);
		badgeRow.setLayoutParams(badgeRowLp);

		String[] badgeItems = new String[]{detailStatus, detailYear, detailFirstGenre};
		for (int b = 0; b < badgeItems.length; b++) {
			TextView bView = new TextView(this);
			bView.setText(badgeItems[b]);
			bView.setTextSize(12f);
			bView.setTypeface(null, Typeface.BOLD);
			bView.setTextColor(Color.parseColor("#CBD5E1"));
			bView.setPadding((int)(8 * d), (int)(4 * d), (int)(8 * d), (int)(4 * d));

			GradientDrawable bBg = new GradientDrawable();
			bBg.setCornerRadius(5 * d);
			bBg.setColor(Color.parseColor("#12141A"));
			bView.setBackground(bBg);

			LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			if (b < badgeItems.length - 1) bLp.rightMargin = (int)(8 * d);
			bView.setLayoutParams(bLp);
			badgeRow.addView(bView);
		}
		rightCol.addView(badgeRow);

		LinearLayout studioRow = new LinearLayout(this);
		studioRow.setOrientation(LinearLayout.HORIZONTAL);
		studioRow.setGravity(Gravity.CENTER_VERTICAL);

		CardView miniThumb = new CardView(this);
		LinearLayout.LayoutParams miniLp = new LinearLayout.LayoutParams((int)(22 * d), (int)(22 * d));
		miniLp.rightMargin = (int)(8 * d);
		miniThumb.setLayoutParams(miniLp);
		miniThumb.setRadius(11 * d);
		miniThumb.setCardElevation(0f);
		miniThumb.setCardBackgroundColor(Color.parseColor("#12141A"));

		ImageView ivMini = new ImageView(this);
		ivMini.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		ivMini.setScaleType(ImageView.ScaleType.CENTER_CROP);
		if (!detailCover.isEmpty()) {
			Glide.with(getApplicationContext()).load(Uri.parse(detailCover)).into(ivMini);
		}
		miniThumb.addView(ivMini);
		studioRow.addView(miniThumb);

		TextView tvEpSummary = new TextView(this);
		int epTotal = (episodeList != null) ? episodeList.size() : 0;
		tvEpSummary.setText("Dongflix • " + epTotal + " Episode");
		tvEpSummary.setTextColor(Color.parseColor("#E5E7EB"));
		tvEpSummary.setTextSize(13f);
		studioRow.addView(tvEpSummary);

		rightCol.addView(studioRow);
		infoHeaderRow.addView(rightCol);
		content.addView(infoHeaderRow);

		TextView tvSinopsisBody = new TextView(this);
		tvSinopsisBody.setText(detailSynopsis.isEmpty() ? "Sinopsis belum tersedia untuk donghua ini." : detailSynopsis);
		tvSinopsisBody.setTextColor(Color.parseColor("#9CA3AF"));
		tvSinopsisBody.setTextSize(13.5f);
		tvSinopsisBody.setLineSpacing(5 * d, 1f);
		content.addView(tvSinopsisBody);

		root.addView(scrollView);
		dialogHolder[0] = createAdaptiveSheetDialog(root, true);
	}

	private void showCleanOptionBottomSheet(String title, ArrayList<String> optionTitles, int activeIdx, final OnSheetItemClickListener listener) {
		final Dialog[] dialogHolder = new Dialog[1];
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(24 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0B0D11"));
		sheetBg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(38 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(12 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		root.addView(buildAdaptiveHeader(title, dialogHolder));

		ScrollView scrollView = new ScrollView(this);
		scrollView.setVerticalScrollBarEnabled(false);
		LinearLayout listContainer = new LinearLayout(this);
		listContainer.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(listContainer);

		for (int i = 0; i < optionTitles.size(); i++) {
			final int index = i;
			boolean isSelected = (i == activeIdx);

			LinearLayout itemRow = new LinearLayout(this);
			itemRow.setOrientation(LinearLayout.HORIZONTAL);
			itemRow.setGravity(Gravity.CENTER_VERTICAL);
			itemRow.setPadding((int)(16 * d), (int)(14 * d), (int)(16 * d), (int)(14 * d));

			LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			rowLp.bottomMargin = (int)(4 * d);
			itemRow.setLayoutParams(rowLp);

			if (isSelected) {
				GradientDrawable selectedBg = new GradientDrawable();
				selectedBg.setCornerRadius(12 * d);
				selectedBg.setColor(Color.parseColor("#1E3A8A"));
				itemRow.setBackground(selectedBg);
			} else {
				itemRow.setBackgroundColor(Color.TRANSPARENT);
			}

			TextView tvName = new TextView(this);
			tvName.setText(optionTitles.get(i));
			tvName.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#E5E7EB"));
			tvName.setTextSize(15f);
			tvName.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
			LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
			tvName.setLayoutParams(nameLp);
			itemRow.addView(tvName);

			if (isSelected) {
				TextView checkCircle = new TextView(this);
				LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams((int)(20 * d), (int)(20 * d));
				checkCircle.setLayoutParams(checkLp);
				checkCircle.setGravity(Gravity.CENTER);
				checkCircle.setText("✓");
				checkCircle.setTextColor(Color.parseColor("#0B0D11"));
				checkCircle.setTextSize(11f);
				checkCircle.setTypeface(null, Typeface.BOLD);

				GradientDrawable circleBg = new GradientDrawable();
				circleBg.setShape(GradientDrawable.OVAL);
				circleBg.setColor(Color.parseColor("#38BDF8"));
				checkCircle.setBackground(circleBg);

				itemRow.addView(checkCircle);
			}

			itemRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (dialogHolder[0] != null) dialogHolder[0].dismiss();
					listener.onItemClick(index);
				}
			});

			listContainer.addView(itemRow);
		}

		root.addView(scrollView);
		dialogHolder[0] = createAdaptiveSheetDialog(root);
	}

	private interface OnSheetItemClickListener {
		void onItemClick(int index);
	}

	public void showAllEpisodesBottomSheet() {
		if (episodeList == null || episodeList.isEmpty()) {
			Toast.makeText(WatchActivity.this, "Daftar episode belum dimuat", Toast.LENGTH_SHORT).show();
			return;
		}

		final Dialog[] dialogHolder = new Dialog[1];
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(24 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0B0D11"));
		sheetBg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(38 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(12 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#1E222B"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		root.addView(buildAdaptiveHeader("Semua Episode (" + episodeList.size() + ")", dialogHolder));

		ScrollView scrollView = new ScrollView(this);
		scrollView.setVerticalScrollBarEnabled(false);

		LinearLayout gridContainer = new LinearLayout(this);
		gridContainer.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(gridContainer);

		int columns = isLandscapeOrFullscreen() ? 4 : 5;
		int total = episodeList.size();
		int rows = (int) Math.ceil((double) total / columns);

		for (int r = 0; r < rows; r++) {
			LinearLayout rowLayout = new LinearLayout(this);
			rowLayout.setOrientation(LinearLayout.HORIZONTAL);
			LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			rowLp.bottomMargin = (int)(10 * d);
			rowLayout.setLayoutParams(rowLp);

			for (int c = 0; c < columns; c++) {
				final int epIdx = (r * columns) + c;
				LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, (int)(46 * d), 1f);
				if (c < columns - 1) cellLp.rightMargin = (int)(8 * d);

				if (epIdx < total) {
					boolean isActive = (epIdx == currentEpisodeIndex);
					HashMap<String, Object> itemData = episodeList.get(epIdx);

					String epLabel = String.valueOf(epIdx + 1);
					if (itemData.containsKey("episode") && itemData.get("episode") != null) {
						epLabel = itemData.get("episode").toString().replace("Ep ", "").replace("Eps ", "").trim();
					} else if (itemData.containsKey("title") && itemData.get("title") != null) {
						String parsedNum = itemData.get("title").toString().replaceAll("[^0-9]", "").trim();
						if (!parsedNum.isEmpty()) epLabel = parsedNum;
					}

					TextView cellBtn = new TextView(this);
					cellBtn.setLayoutParams(cellLp);
					cellBtn.setGravity(Gravity.CENTER);
					cellBtn.setText(epLabel);
					cellBtn.setTextSize(14f);
					cellBtn.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
					cellBtn.setTextColor(isActive ? Color.WHITE : Color.parseColor("#CBD5E1"));

					GradientDrawable cellBg = new GradientDrawable();
					cellBg.setCornerRadius(10 * d);
					cellBg.setColor(isActive ? Color.parseColor("#3B82F6") : Color.parseColor("#12141A"));
					cellBtn.setBackground(cellBg);

					cellBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (dialogHolder[0] != null) dialogHolder[0].dismiss();
							if (currentEpisodeIndex != epIdx || mediaPlayer == null || !mediaPlayer.isPlaying()) {
								playEpisodeByIndex(epIdx);
							}
						}
					});

					rowLayout.addView(cellBtn);
				} else {
					View spacer = new View(this);
					spacer.setLayoutParams(cellLp);
					rowLayout.addView(spacer);
				}
			}
			gridContainer.addView(rowLayout);
		}

		root.addView(scrollView);
		dialogHolder[0] = createAdaptiveSheetDialog(root);
	}

	public void showServerDialog() {
		if (currentEpisodeServerNames == null || currentEpisodeServerNames.isEmpty()) {
			Toast.makeText(WatchActivity.this, "Daftar server belum siap", Toast.LENGTH_SHORT).show();
			return;
		}

		showCleanOptionBottomSheet(
			"Server",
			currentEpisodeServerNames,
			selectedServerIndex,
			new OnSheetItemClickListener() {
				@Override
				public void onItemClick(int which) {
					selectedServerIndex = which;
					String chosenName = currentEpisodeServerNames.get(which);
					String chosenUrl = currentEpisodeServerUrls.get(which);

					if (textActionServer != null) {
						textActionServer.setText(chosenName);
					}
					extractStream(chosenName, chosenUrl);
				}
			}
		);
	}

	public void showQualityDialog() {
		if (qualityNames == null || qualityNames.isEmpty()) {
			Toast.makeText(WatchActivity.this, "Pilihan resolusi belum siap", Toast.LENGTH_SHORT).show();
			return;
		}

		ArrayList<String> titles = new ArrayList<>();
		String currentActiveRes = qualityNames.get(selectedQualityIndex);

		if (currentActiveRes.equalsIgnoreCase("Auto") || currentActiveRes.equalsIgnoreCase("Default")) {
			titles.add("Auto");
		} else {
			titles.add("Auto (" + currentActiveRes.toLowerCase() + ")");
		}

		for (String q : qualityNames) {
			if (!q.equalsIgnoreCase("Auto")) {
				titles.add(q.toLowerCase());
			}
		}

		int activeSheetIdx = 0;
		if (!isAutoQualityMode) {
			String chosenLabel = qualityNames.get(selectedQualityIndex).toLowerCase();
			for (int i = 1; i < titles.size(); i++) {
				if (titles.get(i).equalsIgnoreCase(chosenLabel)) {
					activeSheetIdx = i;
					break;
				}
			}
		}

		final ArrayList<String> finalTitles = titles;

		showCleanOptionBottomSheet(
			"Kualitas",
			finalTitles,
			activeSheetIdx,
			new OnSheetItemClickListener() {
				@Override
				public void onItemClick(int which) {
					int currentPos = 0;
					if (mediaPlayer != null) {
						try {
							currentPos = mediaPlayer.getCurrentPosition();
						} catch (Exception ignored) {}
					}

					if (which == 0) {
						isAutoQualityMode = true;
						int recIdx = getRecommendedQualityIndex();
						boolean needReload = (recIdx != selectedQualityIndex);
						selectedQualityIndex = recIdx;
						updateQualityBadgeText();

						if (needReload) {
							playVideoUrl(qualityUrls.get(recIdx), currentPos);
						}
					} else {
						isAutoQualityMode = false;
						String targetRes = finalTitles.get(which);
						int realIdx = 0;
						for (int k = 0; k < qualityNames.size(); k++) {
							if (qualityNames.get(k).equalsIgnoreCase(targetRes)) {
								realIdx = k;
								break;
							}
						}
						selectedQualityIndex = realIdx;
						updateQualityBadgeText();
						playVideoUrl(qualityUrls.get(realIdx), currentPos);
					}
				}
			}
		);
	}

	public void showSpeedDialog() {
		final ArrayList<String> speedLabels = new ArrayList<>();
		speedLabels.add("0.5x");
		speedLabels.add("0.75x");
		speedLabels.add("Normal (1x)");
		speedLabels.add("1.25x");
		speedLabels.add("1.5x");
		speedLabels.add("1.75x");
		speedLabels.add("2x");

		final float[] speedVals = new float[]{0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
		int activeIdx = 2;
		for (int i = 0; i < speedVals.length; i++) {
			if (Math.abs(speedVals[i] - currentPlaybackSpeed) < 0.01f) {
				activeIdx = i;
				break;
			}
		}

		showCleanOptionBottomSheet(
			"Kecepatan Putar",
			speedLabels,
			activeIdx,
			new OnSheetItemClickListener() {
				@Override
				public void onItemClick(int which) {
					float selectedSpeed = speedVals[which];
					currentPlaybackSpeed = selectedSpeed;
					applyPlaybackSpeedValue(selectedSpeed);
				}
			}
		);
	}

	@Override
	public void onDestroy() {
		if (shimmerAnimator != null) {
			shimmerAnimator.cancel();
			shimmerAnimator = null;
		}
		if (playerHandler != null) {
			if (progressLoop != null) playerHandler.removeCallbacks(progressLoop);
			if (hideControlsRunnable != null) playerHandler.removeCallbacks(hideControlsRunnable);
			if (autoDowngradeRunnable != null) playerHandler.removeCallbacks(autoDowngradeRunnable);
			if (hideUnlockBtnRunnable != null) playerHandler.removeCallbacks(hideUnlockBtnRunnable);
			playerHandler.removeCallbacksAndMessages(null);
		}

		if (mediaPlayer != null) {
			try {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
				}
				mediaPlayer.reset();
				mediaPlayer.release();
			} catch (Exception ignored) {}
			mediaPlayer = null;
		}

		if (fallbackWebView != null) {
			try {
				fallbackWebView.stopLoading();
				fallbackWebView.destroy();
			} catch (Exception ignored) {}
			fallbackWebView = null;
		}

		if (playerSurface != null) {
			playerSurface.release();
			playerSurface = null;
		}

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopHold2xSpeed();
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			if (btnPlay != null) {
				btnPlay.setImageResource(R.drawable.ic_play1);
			}
		}
		if (fallbackWebView != null && isWebViewFallbackActive) {
			fallbackWebView.onPause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (fallbackWebView != null && isWebViewFallbackActive) {
			fallbackWebView.onResume();
		}
	}

	@Override
	public void onBackPressed() {
		if (isScreenLocked) {
			showUnlockPillTemporarily();
			return;
		}
		if (isFullscreen) {
			toggleFullscreen();
		} else if (!isFinishing()) {
			finish();
		}
	}

	public int getRecommendedQualityIndex() {
		if (qualityNames == null || qualityNames.isEmpty()) return 0;

		int targetHeight = 480;
		try {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
					if (caps != null) {
						if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
							int downMbps = caps.getLinkDownstreamBandwidthKbps() / 1000;
							if (downMbps >= 15) targetHeight = 1080;
							else if (downMbps >= 5) targetHeight = 720;
							else targetHeight = 480;
						} else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
							int downMbps = caps.getLinkDownstreamBandwidthKbps() / 1000;
							if (downMbps >= 10) targetHeight = 720;
							else if (downMbps >= 2) targetHeight = 480;
							else targetHeight = 360;
						}
					}
				} else {
					NetworkInfo info = cm.getActiveNetworkInfo();
					if (info != null && info.isConnected()) {
						if (info.getType() == ConnectivityManager.TYPE_WIFI) targetHeight = 720;
						else targetHeight = 480;
					}
				}
			}
		} catch (Exception ignored) {}

		for (int i = 0; i < qualityNames.size(); i++) {
			if (qualityNames.get(i).equalsIgnoreCase("Auto")) return i;
		}

		int bestIdx = 0;
		int smallestDiff = Integer.MAX_VALUE;
		for (int i = 0; i < qualityNames.size(); i++) {
			String numStr = qualityNames.get(i).replaceAll("[^0-9]", "");
			if (!numStr.isEmpty()) {
				int resVal = Integer.parseInt(numStr);
				int diff = Math.abs(resVal - targetHeight);
				if (diff < smallestDiff) {
					smallestDiff = diff;
					bestIdx = i;
				}
			}
		}
		return bestIdx;
	}

	public void triggerAutoDowngrade() {
		if (!isAutoQualityMode || qualityNames == null || qualityNames.size() <= 1) return;

		int currentRes = 9999;
		String curNum = qualityNames.get(selectedQualityIndex).replaceAll("[^0-9]", "");
		if (!curNum.isEmpty()) currentRes = Integer.parseInt(curNum);

		int lowerIdx = -1;
		int closestLowerRes = -1;

		for (int i = 0; i < qualityNames.size(); i++) {
			if (i == selectedQualityIndex) continue;
			String numStr = qualityNames.get(i).replaceAll("[^0-9]", "");
			if (!numStr.isEmpty()) {
				int resVal = Integer.parseInt(numStr);
				if (resVal < currentRes && resVal > closestLowerRes) {
					closestLowerRes = resVal;
					lowerIdx = i;
				}
			}
		}

		if (lowerIdx != -1) {
			selectedQualityIndex = lowerIdx;
			int currentPos = 0;
			if (mediaPlayer != null) {
				try {
					currentPos = mediaPlayer.getCurrentPosition();
				} catch (Exception ignored) {}
			}
			updateQualityBadgeText();
			Toast.makeText(WatchActivity.this, "Jaringan lambat, otomatis ke " + qualityNames.get(lowerIdx), Toast.LENGTH_SHORT).show();
			playVideoUrl(qualityUrls.get(lowerIdx), currentPos);
		}
	}

	public void updateQualityBadgeText() {
		if (btnQuality == null || qualityNames.isEmpty()) return;
		if (isAutoQualityMode) {
			String activeRes = qualityNames.get(selectedQualityIndex);
			if (activeRes.equalsIgnoreCase("Auto") || activeRes.equalsIgnoreCase("Default")) {
				btnQuality.setText("Auto");
			} else {
				btnQuality.setText("Auto (" + activeRes + ")");
			}
		} else {
			btnQuality.setText(qualityNames.get(selectedQualityIndex));
		}
	}

	public void resetControlsTimer() {
		if (playerHandler != null && hideControlsRunnable != null) {
			playerHandler.removeCallbacks(hideControlsRunnable);
			if (mediaPlayer != null && mediaPlayer.isPlaying()) {
				playerHandler.postDelayed(hideControlsRunnable, 3500);
			}
		}
	}

	public void showControls() {
		if (isScreenLocked) {
			showUnlockPillTemporarily();
			return;
		}
		if (playerControlsOverlay != null) {
			playerControlsOverlay.setVisibility(View.VISIBLE);
			playerControlsOverlay.bringToFront();
		}
		areControlsVisible = true;
		updateBottomDividerState();
		resetControlsTimer();
	}

	public void hideControls() {
		if (playerControlsOverlay != null) {
			playerControlsOverlay.setVisibility(View.GONE);
		}
		areControlsVisible = false;
		updateBottomDividerState();
		if (playerHandler != null && hideControlsRunnable != null) {
			playerHandler.removeCallbacks(hideControlsRunnable);
		}
	}

	public void toggleControls() {
		if (isScreenLocked) {
			if (btnUnlockScreen != null && btnUnlockScreen.getVisibility() == View.VISIBLE) {
				btnUnlockScreen.setVisibility(View.GONE);
			} else {
				showUnlockPillTemporarily();
			}
			return;
		}
		if (areControlsVisible) {
			hideControls();
		} else {
			showControls();
		}
	}

	public void updateEpisodeNavButtons() {
		int totalItems = (recyclerEpisode != null && recyclerEpisode.getAdapter() != null) ? recyclerEpisode.getAdapter().getItemCount() : (episodeList != null ? episodeList.size() : 0);

		if (btnPrevEpisode != null) {
			btnPrevEpisode.setEnabled(currentEpisodeIndex > 0);
			btnPrevEpisode.setAlpha(currentEpisodeIndex > 0 ? 1.0f : 0.35f);
		}

		if (btnNextEpisode != null) {
			boolean hasNext = currentEpisodeIndex < (totalItems - 1) && totalItems > 1;
			btnNextEpisode.setEnabled(hasNext);
			btnNextEpisode.setAlpha(hasNext ? 1.0f : 0.35f);
		}
	}

	public void playEpisodeByIndex(final int targetIndex) {
		if (episodeList == null || episodeList.isEmpty()) return;
		if (targetIndex < 0 || targetIndex >= episodeList.size()) return;

		currentEpisodeIndex = targetIndex;
		updateEpisodeNavButtons();

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (recyclerEpisode != null) {
					recyclerEpisode.scrollToPosition(targetIndex);
					if (recyclerEpisode.getAdapter() != null) {
						recyclerEpisode.getAdapter().notifyDataSetChanged();
					}
				}

				if (playerLoading != null) playerLoading.setVisibility(View.VISIBLE);
				if (btnPlay != null) {
					btnPlay.setEnabled(false);
					btnPlay.setAlpha(0.5f);
				}

				HashMap<String, Object> item = episodeList.get(targetIndex);
				if (!item.containsKey("servers")) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
					if (btnPlay != null) {
						btnPlay.setEnabled(true);
						btnPlay.setAlpha(1.0f);
					}
					return;
				}

				currentEpisodeServerNames.clear();
				currentEpisodeServerUrls.clear();

				try {
					Object rawServers = item.get("servers");
					JSONArray sArray;
					if (rawServers instanceof JSONArray) {
						sArray = (JSONArray) rawServers;
					} else if (rawServers instanceof ArrayList) {
						sArray = new JSONArray((ArrayList<?>) rawServers);
					} else {
						sArray = new JSONArray(rawServers.toString());
					}

					for (int i = 0; i < sArray.length(); i++) {
						JSONObject sObj;
						Object itemObj = sArray.get(i);
						if (itemObj instanceof HashMap) {
							sObj = new JSONObject((HashMap<?, ?>) itemObj);
						} else {
							sObj = sArray.getJSONObject(i);
						}

						String sName = sObj.optString("name", "Server " + (i + 1));
						String sLink = sObj.optString("link");
						if (!sLink.isEmpty()) {
							currentEpisodeServerNames.add(sName);
							currentEpisodeServerUrls.add(sLink);
						}
					}
				} catch (Exception ignored) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
					if (btnPlay != null) {
						btnPlay.setEnabled(true);
						btnPlay.setAlpha(1.0f);
					}
					return;
				}

				if (currentEpisodeServerUrls.isEmpty()) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
					if (btnPlay != null) {
						btnPlay.setEnabled(true);
						btnPlay.setAlpha(1.0f);
					}
					return;
				}

				int targetServerIdx = 0;
				for (int i = 0; i < currentEpisodeServerNames.size(); i++) {
					String name = currentEpisodeServerNames.get(i).toLowerCase();
					if (name.contains("ok") || name.contains("odnoklassniki")) {
						targetServerIdx = i;
						break;
					}
				}

				selectedServerIndex = targetServerIdx;
				String chosenServerName = currentEpisodeServerNames.get(targetServerIdx);
				String chosenServerUrl = currentEpisodeServerUrls.get(targetServerIdx);

				if (textActionServer != null) {
					textActionServer.setText(chosenServerName);
				}

				extractStream(chosenServerName, chosenServerUrl);
			}
		});
	}

	private void hideFallbackWebView() {
		isWebViewFallbackActive = false;
		if (fallbackWebView != null) {
			try {
				fallbackWebView.stopLoading();
				fallbackWebView.loadUrl("about:blank");
				fallbackWebView.setVisibility(View.GONE);
			} catch (Exception ignored) {}
		}
		if (playerTextureView != null) {
			playerTextureView.setVisibility(View.VISIBLE);
		}
	}

	private void playViaWebViewInterceptor(final String embedUrl) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					if (mediaPlayer != null) {
						try { mediaPlayer.stop(); mediaPlayer.reset(); mediaPlayer.release(); } catch (Exception ignored) {}
						mediaPlayer = null;
					}

					if (fallbackWebView == null && layoutPlayer != null) {
						fallbackWebView = new WebView(WatchActivity.this);
						FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT
						);
						fallbackWebView.setLayoutParams(lp);
						fallbackWebView.setBackgroundColor(Color.BLACK);
						layoutPlayer.addView(fallbackWebView, 0);
					}

					if (fallbackWebView != null) {
						WebSettings ws = fallbackWebView.getSettings();
						ws.setJavaScriptEnabled(true);
						ws.setDomStorageEnabled(true);
						ws.setMediaPlaybackRequiresUserGesture(false);
						ws.setAllowFileAccess(true);
						ws.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

						fallbackWebView.setWebChromeClient(new WebChromeClient());
						fallbackWebView.setWebViewClient(new WebViewClient() {
							@Override
							public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null && request.getUrl() != null) {
									final String reqUrl = request.getUrl().toString();
									String low = reqUrl.toLowerCase();
									if ((low.contains(".m3u8") || low.contains(".mp4")) && !low.contains("ads") && !low.contains("analytics") && !low.contains("tracker")) {
										runOnUiThread(new Runnable() {
											@Override
											public void run() {
												if (isWebViewFallbackActive) {
													hideFallbackWebView();
													qualityNames.clear();
													qualityUrls.clear();
													qualityNames.add("Auto");
													qualityUrls.add(reqUrl);
													isAutoQualityMode = true;
													selectedQualityIndex = 0;
													updateQualityBadgeText();
													playVideoUrl(reqUrl, 0);
												}
											}
										});
									}
								}
								return super.shouldInterceptRequest(view, request);
							}

							@Override
							public void onPageFinished(WebView view, String url) {
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								if (btnPlay != null) {
									btnPlay.setEnabled(true);
									btnPlay.setAlpha(1.0f);
								}
							}
						});

						isWebViewFallbackActive = true;
						if (playerTextureView != null) playerTextureView.setVisibility(View.GONE);
						fallbackWebView.setVisibility(View.VISIBLE);
						if (playerControlsOverlay != null) playerControlsOverlay.bringToFront();

						Map<String, String> extraHeaders = new HashMap<>();
						extraHeaders.put("Referer", "https://anichin.cafe/");
						fallbackWebView.loadUrl(embedUrl, extraHeaders);
					}
				} catch (Exception e) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
				}
			}
		});
	}

	public void playVideoUrl(final String rawUrl, final int targetSeek) {
		if (rawUrl == null || rawUrl.trim().isEmpty()) return;

		hideFallbackWebView();

		String normalized = rawUrl.trim();
		if (normalized.startsWith("Https://")) {
			normalized = "https://" + normalized.substring(8);
		} else if (normalized.startsWith("Http://")) {
			normalized = "http://" + normalized.substring(7);
		}
		final String cleanUrl = normalized;
		currentPlayingUrl = cleanUrl;
		savedSeekPosition = targetSeek;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (playerLoading != null) playerLoading.setVisibility(View.VISIBLE);
				if (btnPlay != null) {
					btnPlay.setEnabled(false);
					btnPlay.setAlpha(0.5f);
				}

				if (playerHandler != null && autoDowngradeRunnable != null) {
					playerHandler.removeCallbacks(autoDowngradeRunnable);
					if (isAutoQualityMode) {
						playerHandler.postDelayed(autoDowngradeRunnable, 5000);
					}
				}

				try {
					if (mediaPlayer != null) {
						try { mediaPlayer.stop(); } catch (Exception ignored) {}
						try {
							mediaPlayer.reset();
							mediaPlayer.release();
						} catch (Exception ignored) {}
						mediaPlayer = null;
					}

					mediaPlayer = new MediaPlayer();

					if (playerTextureView != null && playerTextureView.isAvailable()) {
						SurfaceTexture st = playerTextureView.getSurfaceTexture();
						if (st != null) {
							if (playerSurface != null) playerSurface.release();
							playerSurface = new Surface(st);
							mediaPlayer.setSurface(playerSurface);
						}
					} else if (playerSurface != null && playerSurface.isValid()) {
						mediaPlayer.setSurface(playerSurface);
					}

					Map<String, String> headers = new HashMap<>();
					headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

					if (cleanUrl.contains("ok.ru") || cleanUrl.contains("odnoklassniki")) {
						headers.put("Referer", "https://ok.ru/");
					} else if (cleanUrl.contains("dailymotion") || cleanUrl.contains("dmcdn")) {
						headers.put("Referer", "https://www.dailymotion.com/");
						headers.put("Origin", "https://www.dailymotion.com");
						if (dmSessionCookie != null && !dmSessionCookie.isEmpty()) {
							headers.put("Cookie", dmSessionCookie);
						}
					} else if (cleanUrl.contains("rumble")) {
						headers.put("Referer", "https://rumble.com/");
					} else if (cleanUrl.contains("odysee") || cleanUrl.contains("odycdn")) {
						headers.put("Referer", "https://odysee.com/");
					} else if (currentStreamReferer != null && !currentStreamReferer.isEmpty()) {
						headers.put("Referer", currentStreamReferer);
					}

					headers.put("Accept", "*/*");

					mediaPlayer.setDataSource(WatchActivity.this, Uri.parse(cleanUrl), headers);
					mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

					mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
						@Override
						public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
							if (width > 0 && height > 0) {
								currentVideoWidth = width;
								currentVideoHeight = height;
								adjustVideoAspectRatio();
							}
						}
					});

					mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
						@Override
						public void onBufferingUpdate(MediaPlayer mp, int percent) {
							try {
								int duration = mp.getDuration();
								if (duration > 0 && playerSeekBar != null) {
									int cacheMillis = (int) ((duration * (long) percent) / 100);
									playerSeekBar.setSecondaryProgress(cacheMillis);
									playerSeekBar.invalidate();
								}
							} catch (Exception ignored) {}
						}
					});

					mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
						@Override
						public boolean onInfo(MediaPlayer mp, int what, int extra) {
							if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
								if (playerLoading != null) playerLoading.setVisibility(View.VISIBLE);
								if (btnPlay != null) {
									btnPlay.setEnabled(false);
									btnPlay.setAlpha(0.5f);
								}
								if (playerHandler != null && autoDowngradeRunnable != null && isAutoQualityMode) {
									playerHandler.removeCallbacks(autoDowngradeRunnable);
									playerHandler.postDelayed(autoDowngradeRunnable, 2500);
								}
							} else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
								if (playerHandler != null && autoDowngradeRunnable != null) {
									playerHandler.removeCallbacks(autoDowngradeRunnable);
								}
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								if (btnPlay != null) {
									btnPlay.setEnabled(true);
									btnPlay.setAlpha(1.0f);
								}
							}
							return false;
						}
					});

					mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
						@Override
						public void onPrepared(MediaPlayer mp) {
							if (playerHandler != null && autoDowngradeRunnable != null) {
								playerHandler.removeCallbacks(autoDowngradeRunnable);
							}
							if (playerLoading != null) playerLoading.setVisibility(View.GONE);
							if (btnPlay != null) {
								btnPlay.setEnabled(true);
								btnPlay.setAlpha(1.0f);
								btnPlay.setImageResource(R.drawable.ic_pause);
							}

							if (playerTextureView != null && playerTextureView.isAvailable() && playerSurface == null) {
								playerSurface = new Surface(playerTextureView.getSurfaceTexture());
								mp.setSurface(playerSurface);
							}

							int vw = mp.getVideoWidth();
							int vh = mp.getVideoHeight();
							if (vw > 0 && vh > 0) {
								currentVideoWidth = vw;
								currentVideoHeight = vh;
								adjustVideoAspectRatio();
							}

							int duration = mp.getDuration();
							if (playerSeekBar != null) playerSeekBar.setMax(duration);

							if (savedSeekPosition > 0 && savedSeekPosition < duration) {
								mp.seekTo(savedSeekPosition);
							}
							mp.start();

							float targetSpeed = isHolding2xSpeed ? 2.0f : currentPlaybackSpeed;
							if (targetSpeed != 1.0f) {
								applyPlaybackSpeedValue(targetSpeed);
							}

							if (playerHandler != null && progressLoop != null) {
								playerHandler.removeCallbacks(progressLoop);
								playerHandler.post(progressLoop);
							}
							updateEpisodeNavButtons();
							resetControlsTimer();
						}
					});

					mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
						@Override
						public boolean onError(MediaPlayer mp, int what, int extra) {
							if (playerHandler != null && autoDowngradeRunnable != null) {
								playerHandler.removeCallbacks(autoDowngradeRunnable);
							}
							if (cleanUrl.contains("ipfs.io/ipfs/")) {
								String altIpfs = cleanUrl.replace("https://ipfs.io/ipfs/", "https://dweb.link/ipfs/");
								playVideoUrl(altIpfs, savedSeekPosition);
								return true;
							}
							if (playerLoading != null) playerLoading.setVisibility(View.GONE);
							if (btnPlay != null) {
								btnPlay.setEnabled(true);
								btnPlay.setAlpha(1.0f);
								btnPlay.setImageResource(R.drawable.ic_play1);
							}
							Toast.makeText(WatchActivity.this, "Gagal memutar stream, coba server lain", Toast.LENGTH_SHORT).show();
							return true;
						}
					});

					mediaPlayer.prepareAsync();

				} catch (Exception e) {
					if (playerLoading != null) playerLoading.setVisibility(View.GONE);
					if (btnPlay != null) {
						btnPlay.setEnabled(true);
						btnPlay.setAlpha(1.0f);
					}
				}
			}
		});
	}

	public void toggleFullscreen() {
		final Window window = getWindow();
		final View decorView = window.getDecorView();
		final View rootWatch = findViewById(R.id.rootWatchLayout);
		final View scrollContent = findViewById(R.id.vscroll16);
		final FrameLayout playContainer = findViewById(R.id.frame_layout5);
		final float density = getResources().getDisplayMetrics().density;

		if (!isFullscreen) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

			if (rootWatch != null) {
				rootWatch.setPadding(0, 0, 0, 0);
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				WindowManager.LayoutParams lp = window.getAttributes();
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
				window.setAttributes(lp);
			}

			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

			decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			);

			if (scrollContent != null) scrollContent.setVisibility(View.GONE);

			if (layoutPlayer != null) {
				ViewGroup.LayoutParams lp = layoutPlayer.getLayoutParams();
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				layoutPlayer.setLayoutParams(lp);
			}

			if (textPlayerTitle != null && textJudul != null) {
				textPlayerTitle.setText(textJudul.getText().toString());
				textPlayerTitle.setTextSize(16f);
				textPlayerTitle.setVisibility(View.VISIBLE);
			}

			if (btnLock != null) {
				btnLock.setVisibility(View.VISIBLE);
			}

			if (btnFullscreenEpisodes != null) {
				btnFullscreenEpisodes.setBackgroundColor(Color.TRANSPARENT);
				btnFullscreenEpisodes.setVisibility(View.VISIBLE);
			}

			int sideMarginFull = (int) (28 * density);
			if (btnPrevEpisode != null) {
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btnPrevEpisode.getLayoutParams();
				lp.width = (int) (38 * density);
				lp.height = (int) (38 * density);
				lp.setMargins(sideMarginFull, 0, 0, 0);
				btnPrevEpisode.setLayoutParams(lp);
			}

			if (btnNextEpisode != null) {
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btnNextEpisode.getLayoutParams();
				lp.width = (int) (38 * density);
				lp.height = (int) (38 * density);
				lp.setMargins(0, 0, sideMarginFull, 0);
				btnNextEpisode.setLayoutParams(lp);
			}

			int gapCenterFull = (int) (36 * density);
			if (btnRewind != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnRewind.getLayoutParams();
				lp.width = (int) (42 * density);
				lp.height = (int) (42 * density);
				btnRewind.setLayoutParams(lp);
			}

			if (playContainer != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) playContainer.getLayoutParams();
				lp.width = (int) (74 * density);
				lp.height = (int) (74 * density);
				lp.setMargins(gapCenterFull, 0, gapCenterFull, 0);
				playContainer.setLayoutParams(lp);
			}

			if (playerLoading != null) {
				ViewGroup.LayoutParams lp = playerLoading.getLayoutParams();
				lp.width = (int) (74 * density);
				lp.height = (int) (74 * density);
				playerLoading.setLayoutParams(lp);
			}

			if (btnPlay != null) {
				ViewGroup.LayoutParams lp = btnPlay.getLayoutParams();
				lp.width = (int) (60 * density);
				lp.height = (int) (60 * density);
				btnPlay.setLayoutParams(lp);
				int padPlay = (int) (15 * density);
				btnPlay.setPadding(padPlay, padPlay, padPlay, padPlay);
			}

			if (btnForward != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnForward.getLayoutParams();
				lp.width = (int) (42 * density);
				lp.height = (int) (42 * density);
				btnForward.setLayoutParams(lp);
			}

			if (btnBack != null) {
				ViewGroup.LayoutParams lp = btnBack.getLayoutParams();
				lp.width = (int) (28 * density);
				lp.height = (int) (28 * density);
				btnBack.setLayoutParams(lp);
			}
			if (btnFullscreen != null) {
				btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);
				ViewGroup.LayoutParams lp = btnFullscreen.getLayoutParams();
				lp.width = (int) (26 * density);
				lp.height = (int) (26 * density);
				btnFullscreen.setLayoutParams(lp);
			}
			if (btnQuality != null) btnQuality.setTextSize(14f);
			if (textTime != null) textTime.setTextSize(13.5f);

			int padHoriz = (int) (28 * density);
			int padTop = (int) (18 * density);
			int padBottom = (int) (12 * density);
			if (topBar != null) topBar.setPadding(padHoriz, padTop, padHoriz, (int)(6 * density));
			if (bottomControls != null) bottomControls.setPadding(padHoriz, 0, padHoriz, padBottom);

			isFullscreen = true;
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

			if (rootWatch != null) {
				rootWatch.setPadding(0, 0, 0, 0);
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				WindowManager.LayoutParams lp = window.getAttributes();
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
				window.setAttributes(lp);
			}

			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

			int heightPx = (int) (220 * density);
			if (layoutPlayer != null) {
				ViewGroup.LayoutParams lp = layoutPlayer.getLayoutParams();
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
				lp.height = heightPx;
				layoutPlayer.setLayoutParams(lp);
			}

			if (scrollContent != null) scrollContent.setVisibility(View.VISIBLE);
			if (textPlayerTitle != null) textPlayerTitle.setVisibility(View.GONE);
			if (btnLock != null) btnLock.setVisibility(View.GONE);
			if (btnFullscreenEpisodes != null) btnFullscreenEpisodes.setVisibility(View.GONE);

			int sideMarginNormal = (int) (12 * density);
			if (btnPrevEpisode != null) {
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btnPrevEpisode.getLayoutParams();
				lp.width = (int) (32 * density);
				lp.height = (int) (32 * density);
				lp.setMargins(sideMarginNormal, 0, 0, 0);
				btnPrevEpisode.setLayoutParams(lp);
			}

			if (btnNextEpisode != null) {
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) btnNextEpisode.getLayoutParams();
				lp.width = (int) (32 * density);
				lp.height = (int) (32 * density);
				lp.setMargins(0, 0, sideMarginNormal, 0);
				btnNextEpisode.setLayoutParams(lp);
			}

			int gapCenterNormal = (int) (22 * density);
			if (btnRewind != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnRewind.getLayoutParams();
				lp.width = (int) (36 * density);
				lp.height = (int) (36 * density);
				btnRewind.setLayoutParams(lp);
			}

			if (playContainer != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) playContainer.getLayoutParams();
				lp.width = (int) (64 * density);
				lp.height = (int) (64 * density);
				lp.setMargins(gapCenterNormal, 0, gapCenterNormal, 0);
				playContainer.setLayoutParams(lp);
			}

			if (playerLoading != null) {
				ViewGroup.LayoutParams lp = playerLoading.getLayoutParams();
				lp.width = (int) (64 * density);
				lp.height = (int) (64 * density);
				playerLoading.setLayoutParams(lp);
			}

			if (btnPlay != null) {
				ViewGroup.LayoutParams lp = btnPlay.getLayoutParams();
				lp.width = (int) (54 * density);
				lp.height = (int) (54 * density);
				btnPlay.setLayoutParams(lp);
				int padPlay = (int) (14 * density);
				btnPlay.setPadding(padPlay, padPlay, padPlay, padPlay);
			}

			if (btnForward != null) {
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) btnForward.getLayoutParams();
				lp.width = (int) (36 * density);
				lp.height = (int) (36 * density);
				btnForward.setLayoutParams(lp);
			}

			if (btnBack != null) {
				ViewGroup.LayoutParams lp = btnBack.getLayoutParams();
				lp.width = (int) (28 * density);
				lp.height = (int) (28 * density);
				btnBack.setLayoutParams(lp);
			}
			if (btnFullscreen != null) {
				btnFullscreen.setImageResource(R.drawable.ic_fullscreen);
				ViewGroup.LayoutParams lp = btnFullscreen.getLayoutParams();
				lp.width = (int) (24 * density);
				lp.height = (int) (24 * density);
				btnFullscreen.setLayoutParams(lp);
			}
			if (btnQuality != null) btnQuality.setTextSize(13f);
			if (textTime != null) textTime.setTextSize(13f);

			int defPadX = (int) (16 * density);
			int defPadTop = (int) (14 * density);
			int defPadBottom = (int) (6 * density);
			if (topBar != null) topBar.setPadding(defPadX, defPadTop, defPadX, (int)(6 * density));
			if (bottomControls != null) bottomControls.setPadding(0, 0, 0, defPadBottom);

			isFullscreen = false;
		}
		updateBottomDividerState();
		adjustVideoAspectRatio();
		resetControlsTimer();
	}

	public void _initPlayerViews() {
		final boolean[] isUserTrackingSeekBar = new boolean[]{false};
		playerHandler = new Handler(Looper.getMainLooper());

		autoDowngradeRunnable = new Runnable() {
			@Override
			public void run() {
				triggerAutoDowngrade();
			}
		};

		hideUnlockBtnRunnable = new Runnable() {
			@Override
			public void run() {
				if (btnUnlockScreen != null) {
					btnUnlockScreen.setVisibility(View.GONE);
				}
			}
		};

		GradientDrawable playCircle = new GradientDrawable();
		playCircle.setShape(GradientDrawable.OVAL);
		playCircle.setColor(0xFFFFFFFF);
		if (btnPlay != null) btnPlay.setBackground(playCircle);

		final float density = getResources().getDisplayMetrics().density;
		final int barHeight = (int) (4 * density);
		final int cornerRadius = (int) (2 * density);
		final int gapSize = (int) (4 * density);

		Drawable customTrackDrawable = new Drawable() {
			private final Paint paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint paintCache = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint paintProgress = new Paint(Paint.ANTI_ALIAS_FLAG);

			{
				paintBg.setColor(0x33FFFFFF);
				paintCache.setColor(0x80CCCCCC);
				paintProgress.setColor(0xFF3B82F6);
			}

			@Override
			public void draw(Canvas canvas) {
				Rect bounds = getBounds();
				float width = bounds.width();
				float top = bounds.centerY() - (barHeight / 2f);
				float bottom = bounds.centerY() + (barHeight / 2f);

				int max = playerSeekBar.getMax();
				if (max <= 0) max = 1;

				float progressRatio = (float) playerSeekBar.getProgress() / max;
				float cacheRatio = (float) playerSeekBar.getSecondaryProgress() / max;

				float progressRight = bounds.left + (width * progressRatio);
				float cacheRight = bounds.left + (width * cacheRatio);

				if (cacheRight > bounds.left) {
					canvas.drawRoundRect(new RectF(bounds.left, top, cacheRight, bottom), cornerRadius, cornerRadius, paintCache);
				}

				if (progressRight > bounds.left) {
					canvas.drawRoundRect(new RectF(bounds.left, top, progressRight, bottom), cornerRadius, cornerRadius, paintProgress);
				}

				float bgStart = Math.max(cacheRight, progressRight) + gapSize;
				if (bgStart < bounds.right) {
					canvas.drawRoundRect(new RectF(bgStart, top, bounds.right, bottom), cornerRadius, cornerRadius, paintBg);
				}
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};

		if (playerSeekBar != null) {
			playerSeekBar.setProgressDrawable(customTrackDrawable);

			final int normalThumbSize = (int) (12 * density);
			final int activeThumbSize = (int) (16 * density);

			final GradientDrawable customThumb = new GradientDrawable();
			customThumb.setShape(GradientDrawable.OVAL);
			customThumb.setColor(0xFF3B82F6);
			customThumb.setSize(normalThumbSize, normalThumbSize);

			playerSeekBar.setThumb(customThumb);
			playerSeekBar.setThumbOffset(normalThumbSize / 2);

			playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					seekBar.invalidate();
					if (fromUser && textTime != null) {
						int curSec = (progress / 1000) % 60;
						int curMin = (progress / (1000 * 60)) % 60;
						int total = (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
						int totSec = (total / 1000) % 60;
						int totMin = (total / (1000 * 60)) % 60;
						textTime.setText(String.format("%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec));
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					isUserTrackingSeekBar[0] = true;
					customThumb.setSize(activeThumbSize, activeThumbSize);
					seekBar.setThumb(customThumb);
					seekBar.setThumbOffset(activeThumbSize / 2);

					if (playerHandler != null && hideControlsRunnable != null) {
						playerHandler.removeCallbacks(hideControlsRunnable);
					}
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					if (mediaPlayer != null) {
						mediaPlayer.seekTo(seekBar.getProgress());
					}
					customThumb.setSize(normalThumbSize, normalThumbSize);
					seekBar.setThumb(customThumb);
					seekBar.setThumbOffset(normalThumbSize / 2);

					isUserTrackingSeekBar[0] = false;
					resetControlsTimer();
				}
			});
		}

		hideControlsRunnable = new Runnable() {
			@Override
			public void run() {
				hideControls();
			}
		};

		final GestureDetector playerGestureDetector = new GestureDetector(WatchActivity.this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				toggleControls();
				return true;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				startHold2xSpeed();
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if (isScreenLocked) {
					showUnlockPillTemporarily();
					return true;
				}
				if (mediaPlayer == null || layoutPlayer == null) return false;

				float touchX = e.getX();
				int viewWidth = layoutPlayer.getWidth();

				if (touchX < (viewWidth / 2f)) {
					int targetPos = Math.max(0, mediaPlayer.getCurrentPosition() - 10000);
					mediaPlayer.seekTo(targetPos);
					if (playerSeekBar != null) {
						playerSeekBar.setProgress(targetPos);
						playerSeekBar.invalidate();
					}
					showSeekAnimation(animSeekLeft);
				} else {
					int targetPos = Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 10000);
					mediaPlayer.seekTo(targetPos);
					if (playerSeekBar != null) {
						playerSeekBar.setProgress(targetPos);
						playerSeekBar.invalidate();
					}
					showSeekAnimation(animSeekRight);
				}
				resetControlsTimer();
				return true;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}
		});

		View.OnTouchListener playerTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
					if (isHolding2xSpeed) {
						stopHold2xSpeed();
						return true;
					}
				}
				return playerGestureDetector.onTouchEvent(event);
			}
		};

		if (layoutPlayer != null) {
			layoutPlayer.setOnTouchListener(playerTouchListener);
		}

		if (playerControlsOverlay != null) {
			playerControlsOverlay.setOnTouchListener(playerTouchListener);
		}

		if (playerTextureView != null) {
			playerTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
				@Override
				public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
					playerSurface = new Surface(surfaceTexture);
					if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
						mediaPlayer.setSurface(playerSurface);
					}
					adjustVideoAspectRatio();
				}
				@Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
				@Override
				public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
					if (playerSurface != null) {
						playerSurface.release();
						playerSurface = null;
					}
					if (mediaPlayer != null) {
						mediaPlayer.release();
						mediaPlayer = null;
					}
					return true;
				}
				@Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
			});
		}

		progressLoop = new Runnable() {
			@Override
			public void run() {
				try {
					if (mediaPlayer != null && mediaPlayer.isPlaying() && !isUserTrackingSeekBar[0]) {
						int cur = mediaPlayer.getCurrentPosition();
						int tot = mediaPlayer.getDuration();

						if (playerSeekBar != null) {
							playerSeekBar.setProgress(cur);
							playerSeekBar.invalidate();
						}

						if (miniVideoProgressBar != null && tot > 0) {
							int permille = (int) ((cur * 1000L) / tot);
							miniVideoProgressBar.setProgress(permille);
						}

						if (textTime != null) {
							int curSec = (cur / 1000) % 60;
							int curMin = (cur / (1000 * 60)) % 60;
							int totSec = (tot / 1000) % 60;
							int totMin = (tot / (1000 * 60)) % 60;
							textTime.setText(String.format("%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec));
						}
					}
				} catch (Exception ignored) {}

				if (playerHandler != null) {
					playerHandler.postDelayed(this, 500);
				}
			}
		};

		if (btnPlay != null) {
			btnPlay.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					if (mediaPlayer != null) {
						if (mediaPlayer.isPlaying()) {
							mediaPlayer.pause();
							btnPlay.setImageResource(R.drawable.ic_play1);
							getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
							if (playerHandler != null && hideControlsRunnable != null) {
								playerHandler.removeCallbacks(hideControlsRunnable);
							}
						} else {
							mediaPlayer.start();
							btnPlay.setImageResource(R.drawable.ic_pause);
							getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
							resetControlsTimer();
						}
					}
				}
			});
		}

		if (btnRewind != null) {
			btnRewind.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					if (mediaPlayer != null) {
						int targetPos = Math.max(0, mediaPlayer.getCurrentPosition() - 10000);
						mediaPlayer.seekTo(targetPos);
						if (playerSeekBar != null) {
							playerSeekBar.setProgress(targetPos);
							playerSeekBar.invalidate();
						}
						showSeekAnimation(animSeekLeft);
					}
				}
			});
		}

		if (btnForward != null) {
			btnForward.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					if (mediaPlayer != null) {
						int targetPos = Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 10000);
						mediaPlayer.seekTo(targetPos);
						if (playerSeekBar != null) {
							playerSeekBar.setProgress(targetPos);
							playerSeekBar.invalidate();
						}
						showSeekAnimation(animSeekRight);
					}
				}
			});
		}

		if (btnPrevEpisode != null) {
			btnPrevEpisode.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					if (currentEpisodeIndex > 0) {
						playEpisodeByIndex(currentEpisodeIndex - 1);
					}
				}
			});
		}

		if (btnNextEpisode != null) {
			btnNextEpisode.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					int totalItems = (recyclerEpisode != null && recyclerEpisode.getAdapter() != null) ? recyclerEpisode.getAdapter().getItemCount() : 0;
					if (currentEpisodeIndex < (totalItems - 1)) {
						playEpisodeByIndex(currentEpisodeIndex + 1);
					}
				}
			});
		}

		if (btnLock != null) {
			btnLock.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					lockScreenControls();
				}
			});
		}

		if (btnUnlockScreen != null) {
			btnUnlockScreen.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					unlockScreenControls();
				}
			});
		}

		if (btnCast != null) {
			btnCast.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					try {
						startActivity(new Intent(Settings.ACTION_CAST_SETTINGS));
					} catch (Exception e) {
						Toast.makeText(WatchActivity.this, "Fitur Cast tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show();
					}
				}
			});
		}

		if (btnSettings != null) {
			btnSettings.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showSettingsBottomSheet();
				}
			});
		}

		if (btnQuality != null) {
			btnQuality.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showQualityDialog();
				}
			});
		}

		if (btnFullscreenEpisodes != null) {
			btnFullscreenEpisodes.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showAllEpisodesBottomSheet();
				}
			});
		}

		if (btnOpenDetail != null) {
			btnOpenDetail.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showDetailBottomSheet();
				}
			});
		}

		if (btnSortEpisodes != null) {
			btnSortEpisodes.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleSortEpisodes();
				}
			});
		}

		TextView btnSeeAllEpisodes = findViewById(R.id.textview85);
		if (btnSeeAllEpisodes != null) {
			btnSeeAllEpisodes.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showAllEpisodesBottomSheet();
				}
			});
		}

		if (btnFill != null) {
			btnFill.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					isFillModeActive = !isFillModeActive;
					adjustVideoAspectRatio();
					Toast.makeText(WatchActivity.this, isFillModeActive ? "Mode: Fill" : "Mode: Original", Toast.LENGTH_SHORT).show();
				}
			});
		}

		if (btnActionServer != null) {
			btnActionServer.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showServerDialog();
				}
			});
		}

		if (btnFullscreen != null) {
			btnFullscreen.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					toggleFullscreen();
				}
			});
		}

		if (btnBack != null) {
			btnBack.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isFullscreen) {
						toggleFullscreen();
					} else {
						finish();
					}
				}
			});
		}

		updateEpisodeNavButtons();
	}

	private String fetchHttpString(String targetUrl, String referer, boolean saveDmCookie) throws Exception {
		String currentUrl = targetUrl;
		HttpURLConnection conn = null;

		for (int redirectCount = 0; redirectCount < 6; redirectCount++) {
			URL url = new URL(currentUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
			conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8");
			conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,id;q=0.8");
			conn.setRequestProperty("Sec-Fetch-Dest", "iframe");
			conn.setRequestProperty("Sec-Fetch-Mode", "navigate");
			if (referer != null && !referer.isEmpty()) {
				conn.setRequestProperty("Referer", referer);
			}

			if (saveDmCookie) {
				Map<String, List<String>> headerFields = conn.getHeaderFields();
				List<String> cookiesHeader = headerFields.get("Set-Cookie");
				if (cookiesHeader != null && !cookiesHeader.isEmpty()) {
					StringBuilder cookieSb = new StringBuilder();
					for (String c : cookiesHeader) {
						String mainPart = c.split(";")[0];
						if (cookieSb.length() > 0) cookieSb.append("; ");
						cookieSb.append(mainPart);
					}
					dmSessionCookie = cookieSb.toString();
				}
			}

			int code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
				String loc = conn.getHeaderField("Location");
				conn.disconnect();
				if (loc == null || loc.isEmpty()) break;
				if (loc.startsWith("//")) loc = "https:" + loc;
				else if (loc.startsWith("/")) loc = url.getProtocol() + "://" + url.getHost() + loc;
				currentUrl = loc;
				continue;
			}
			break;
		}

		if (conn == null) return "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append("\n");
		}
		reader.close();
		conn.disconnect();
		return sb.toString();
	}

	private String postJsonHttp(String apiUrl, String jsonPayload) throws Exception {
		URL url = new URL(apiUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0 Safari/537.36");

		OutputStream os = conn.getOutputStream();
		os.write(jsonPayload.getBytes("UTF-8"));
		os.flush();
		os.close();

		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		conn.disconnect();
		return sb.toString();
	}

	private String unpackDeanEdwards(String html) {
		try {
			Pattern p = Pattern.compile("eval\\(function\\(p,a,c,k,e,[dr]\\)\\{.*?\\}\\('(.*?)',(\\d+),(\\d+),'(.*?)'\\.split\\('\\|'\\)", Pattern.DOTALL);
			Matcher m = p.matcher(html);
			if (m.find()) {
				String payload = m.group(1);
				int radix = Integer.parseInt(m.group(2));
				String[] symtab = m.group(4).split("\\|");

				Pattern wordPat = Pattern.compile("\\b\\w+\\b");
				Matcher wm = wordPat.matcher(payload);
				StringBuffer sb = new StringBuffer();
				while (wm.find()) {
					String word = wm.group();
					try {
						int idx = Integer.parseInt(word, radix);
						if (idx >= 0 && idx < symtab.length && !symtab[idx].isEmpty()) {
							wm.appendReplacement(sb, Matcher.quoteReplacement(symtab[idx]));
						}
					} catch (Exception ignored) {}
				}
				wm.appendTail(sb);
				return sb.toString();
			}
		} catch (Exception ignored) {}
		return "";
	}

	public void extractStream(final String server, final String targetUrl) {
		if (targetUrl == null || targetUrl.trim().isEmpty()) return;

		String cleanUrl = targetUrl.trim();
		if (cleanUrl.startsWith("//")) {
			cleanUrl = "https:" + cleanUrl;
		} else if (!cleanUrl.startsWith("http")) {
			cleanUrl = "https://" + cleanUrl;
		}
		final String finalTargetUrl = cleanUrl;
		final String serverKey = (server != null) ? server.trim().toLowerCase() : "";
		final String lowerUrl = finalTargetUrl.toLowerCase();
		currentStreamReferer = finalTargetUrl;

		if (serverKey.contains("ok") || serverKey.contains("odnoklassniki") || lowerUrl.contains("ok.ru")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String html = fetchHttpString(finalTargetUrl, "https://ok.ru/", false);
						String foundOptions = "";
						int idx = html.indexOf("data-options=");
						if (idx != -1) {
							int qStart = idx + 13;
							char quote = html.charAt(qStart);
							int qEnd = html.indexOf(quote, qStart + 1);
							if (qEnd != -1) {
								foundOptions = html.substring(qStart + 1, qEnd);
							}
						}

						final ArrayList<String> tempNames = new ArrayList<>();
						final ArrayList<String> tempUrls = new ArrayList<>();

						if (!foundOptions.isEmpty()) {
							String cleanJson = foundOptions
								.replace("&quot;", "\"")
								.replace("&amp;", "&")
								.replace("&#39;", "'")
								.replace("&lt;", "<")
								.replace("&gt;", ">");

							JSONObject optJson = new JSONObject(cleanJson);
							JSONObject metadata = null;
							if (optJson.has("flashvars")) {
								JSONObject fv = optJson.getJSONObject("flashvars");
								if (fv.has("metadata")) {
									Object mObj = fv.get("metadata");
									metadata = (mObj instanceof JSONObject) ? (JSONObject) mObj : new JSONObject(mObj.toString());
								}
							} else if (optJson.has("metadata")) {
								Object mObj = optJson.get("metadata");
								metadata = (mObj instanceof JSONObject) ? (JSONObject) mObj : new JSONObject(mObj.toString());
							}

							if (metadata != null && metadata.has("videos")) {
								JSONArray videos = metadata.getJSONArray("videos");
								for (int k = 0; k < videos.length(); k++) {
									JSONObject v = videos.getJSONObject(k);
									if (v.has("url")) {
										String rawUrl = v.getString("url").replace("\\/", "/").replace("\\u0026", "&");
										String rName = v.optString("name", "Resolusi " + (k + 1)).toUpperCase();

										if (rName.equalsIgnoreCase("mobile")) rName = "144P";
										if (rName.equalsIgnoreCase("lowest")) rName = "240P";
										if (rName.equalsIgnoreCase("low")) rName = "360P";
										if (rName.equalsIgnoreCase("medium")) rName = "480P";
										if (rName.equalsIgnoreCase("high")) rName = "720P";
										if (rName.equalsIgnoreCase("hd")) rName = "1080P";

										tempNames.add(rName);
										tempUrls.add(rawUrl);
									}
								}
							}
						}

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!tempUrls.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.addAll(tempNames);
									qualityUrls.addAll(tempUrls);

									isAutoQualityMode = true;
									selectedQualityIndex = getRecommendedQualityIndex();
									updateQualityBadgeText();

									playVideoUrl(qualityUrls.get(selectedQualityIndex), 0);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else if (serverKey.contains("daily") || lowerUrl.contains("dailymotion") || lowerUrl.contains("dai.ly")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String dailyId = "";
						Uri parsedUri = Uri.parse(finalTargetUrl);
						if (parsedUri.getQueryParameter("id") != null && !parsedUri.getQueryParameter("id").isEmpty()) {
							dailyId = parsedUri.getQueryParameter("id");
						} else if (parsedUri.getQueryParameter("video") != null && !parsedUri.getQueryParameter("video").isEmpty()) {
							dailyId = parsedUri.getQueryParameter("video");
						} else if (finalTargetUrl.contains("/video/")) {
							String[] parts = finalTargetUrl.split("/video/");
							if (parts.length > 1) dailyId = parts[1].split("[?&#_]")[0];
						} else if (finalTargetUrl.contains("dai.ly/")) {
							String[] parts = finalTargetUrl.split("dai.ly/");
							if (parts.length > 1) dailyId = parts[1].split("[?&#]")[0];
						} else {
							String cPath = finalTargetUrl.split("\\?")[0];
							dailyId = cPath.substring(cPath.lastIndexOf('/') + 1);
						}

						if (dailyId == null || dailyId.trim().isEmpty()) {
							throw new Exception("ID Dailymotion kosong");
						}

						final String cleanDmId = dailyId.trim();
						String dmApiUrl = "https://www.dailymotion.com/player/metadata/video/" + cleanDmId + "?embedder=https%3A%2F%2Fwww.dailymotion.com%2F&app=com.dailymotion.neon";
						String resBody = fetchHttpString(dmApiUrl, "https://www.dailymotion.com/", true);

						final ArrayList<String> dmNames = new ArrayList<>();
						final ArrayList<String> dmUrls = new ArrayList<>();

						if (!resBody.isEmpty()) {
							JSONObject metaObj = new JSONObject(resBody);
							if (metaObj.has("qualities")) {
								JSONObject qualities = metaObj.getJSONObject("qualities");
								String[] checkKeys = new String[]{"1080", "720", "480", "380", "240", "auto"};

								for (String qKey : checkKeys) {
									if (qualities.has(qKey)) {
										JSONArray qArr = qualities.optJSONArray(qKey);
										if (qArr != null) {
											String chosenUrl = "";
											for (int idx = 0; idx < qArr.length(); idx++) {
												JSONObject itemQ = qArr.getJSONObject(idx);
												String mime = itemQ.optString("type", "");
												String u = itemQ.optString("url", "");
												if (!u.isEmpty() && mime.contains("mp4")) {
													chosenUrl = u;
													break;
												}
											}
											if (chosenUrl.isEmpty() && qArr.length() > 0) {
												chosenUrl = qArr.getJSONObject(0).optString("url", "");
											}
											if (!chosenUrl.isEmpty()) {
												String label = qKey.equalsIgnoreCase("auto") ? "Auto" : qKey + "P";
												dmNames.add(label);
												dmUrls.add(chosenUrl);
											}
										}
									}
								}
							}
						}

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!dmUrls.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.addAll(dmNames);
									qualityUrls.addAll(dmUrls);

									isAutoQualityMode = true;
									selectedQualityIndex = getRecommendedQualityIndex();
									updateQualityBadgeText();

									playVideoUrl(qualityUrls.get(selectedQualityIndex), 0);
								} else {
									playViaWebViewInterceptor("https://geo.dailymotion.com/player.html?video=" + cleanDmId);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else if (serverKey.contains("d-tube") || serverKey.contains("dtube") || lowerUrl.contains("d.tube")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String streamFound = "";

						Pattern pDirectQm = Pattern.compile("(Qm[a-zA-Z0-9]{44})");
						Matcher mDirectQm = pDirectQm.matcher(finalTargetUrl);
						if (mDirectQm.find()) {
							streamFound = "https://dweb.link/ipfs/" + mDirectQm.group(1);
						}

						if (streamFound.isEmpty() && finalTargetUrl.contains("/v/")) {
							String afterV = finalTargetUrl.substring(finalTargetUrl.indexOf("/v/") + 3).split("[?#]")[0];
							String[] parts = afterV.split("/");
							if (parts.length >= 2) {
								String author = parts[0].trim();
								String permlink = parts[1].trim();
								String rpcBody = "{\"jsonrpc\":\"2.0\",\"method\":\"condenser_api.get_content\",\"params\":[\"" + author + "\",\"" + permlink + "\"],\"id\":1}";
								String rpcRes = postJsonHttp("https://api.hive.blog", rpcBody);
								JSONObject rpcJson = new JSONObject(rpcRes);
								if (rpcJson.has("result")) {
									JSONObject resObj = rpcJson.getJSONObject("result");
									String metaStr = resObj.optString("json_metadata", "");
									if (!metaStr.isEmpty()) {
										JSONObject metaJson = new JSONObject(metaStr);
										if (metaJson.has("video")) {
											JSONObject videoObj = metaJson.getJSONObject("video");
											if (videoObj.has("files")) {
												JSONObject filesObj = videoObj.getJSONObject("files");
												if (filesObj.has("ipfs")) {
													JSONObject ipfsObj = filesObj.getJSONObject("ipfs");
													if (ipfsObj.has("vid")) {
														JSONObject vidObj = ipfsObj.getJSONObject("vid");
														String hash = vidObj.optString("720", vidObj.optString("480", vidObj.optString("src", "")));
														if (!hash.isEmpty()) {
															streamFound = "https://dweb.link/ipfs/" + hash;
														}
													}
												}
											}
										}
									}
								}
							}
						}

						if (streamFound.isEmpty()) {
							String html = fetchHttpString(finalTargetUrl, "https://emb.d.tube/", false);
							Matcher mQm = pDirectQm.matcher(html);
							if (mQm.find()) {
								streamFound = "https://dweb.link/ipfs/" + mQm.group(1);
							}
						}

						final String finalDtubeUrl = streamFound;
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!finalDtubeUrl.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.add("Default");
									qualityUrls.add(finalDtubeUrl);
									isAutoQualityMode = true;
									selectedQualityIndex = 0;
									updateQualityBadgeText();

									playVideoUrl(finalDtubeUrl, 0);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else if (serverKey.contains("rumble") || lowerUrl.contains("rumble.com")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String embedId = "";
						if (finalTargetUrl.contains("/embed/")) {
							String[] parts = finalTargetUrl.split("/embed/");
							if (parts.length > 1) embedId = parts[1].split("[/?#]")[0];
						}

						final ArrayList<String> rNames = new ArrayList<>();
						final ArrayList<String> rUrls = new ArrayList<>();

						if (embedId.isEmpty()) {
							String pageHtml = fetchHttpString(finalTargetUrl, "https://rumble.com/", false);
							Pattern pEmbed = Pattern.compile("rumble\\.com/embed/([a-zA-Z0-9]+)");
							Matcher mEmbed = pEmbed.matcher(pageHtml);
							if (mEmbed.find()) {
								embedId = mEmbed.group(1);
							}
						}

						if (!embedId.isEmpty()) {
							String apiUrl = "https://rumble.com/embedJS/u3/?request=video&ver=2&v=" + embedId;
							String jsonStr = fetchHttpString(apiUrl, "https://rumble.com/", false);
							JSONObject rootObj = new JSONObject(jsonStr);

							if (rootObj.has("ua")) {
								JSONObject uaObj = rootObj.getJSONObject("ua");
								String[] resKeys = new String[]{"1080", "720", "480", "360", "240"};

								if (uaObj.has("mp4")) {
									JSONObject mp4Obj = uaObj.getJSONObject("mp4");
									for (String rk : resKeys) {
										if (mp4Obj.has(rk)) {
											String u = mp4Obj.getJSONObject(rk).optString("url");
											if (!u.isEmpty()) {
												rNames.add(rk + "P");
												rUrls.add(u);
											}
										}
									}
								}

								if (rUrls.isEmpty()) {
									for (String rk : resKeys) {
										if (uaObj.has(rk)) {
											JSONObject resObj = uaObj.optJSONObject(rk);
											if (resObj != null) {
												String u = resObj.optString("url");
												if (!u.isEmpty()) {
													rNames.add(rk + "P");
													rUrls.add(u);
												}
											}
										}
									}
								}

								if (rUrls.isEmpty() && uaObj.has("hls")) {
									JSONObject hlsObj = uaObj.getJSONObject("hls");
									if (hlsObj.has("auto")) {
										String u = hlsObj.getJSONObject("auto").optString("url");
										if (!u.isEmpty()) {
											rNames.add("Auto");
											rUrls.add(u);
										}
									}
								}
							}
						}

						if (rUrls.isEmpty()) {
							String html = fetchHttpString(finalTargetUrl, "https://rumble.com/", false);
							Pattern pMp4 = Pattern.compile("(https?://[a-zA-Z0-9.-]+\\.rumble\\.com/[^\"'\\s]+\\.mp4)");
							Matcher mMp4 = pMp4.matcher(html);
							if (mMp4.find()) {
								rNames.add("Default");
								rUrls.add(mMp4.group(1));
							}
						}

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!rUrls.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.addAll(rNames);
									qualityUrls.addAll(rUrls);

									isAutoQualityMode = true;
									selectedQualityIndex = getRecommendedQualityIndex();
									updateQualityBadgeText();

									playVideoUrl(qualityUrls.get(selectedQualityIndex), 0);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else if (serverKey.contains("odysee") || lowerUrl.contains("odysee.com") || lowerUrl.contains("odycdn.com")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String streamOdysee = "";
						if (finalTargetUrl.contains("odycdn.com") && (finalTargetUrl.contains(".mp4") || finalTargetUrl.contains(".m3u8") || finalTargetUrl.contains("/streams/"))) {
							streamOdysee = finalTargetUrl;
						} else if (finalTargetUrl.contains("signature=")) {
							Uri uri = Uri.parse(finalTargetUrl);
							String sig = uri.getQueryParameter("signature");
							String sigTs = uri.getQueryParameter("signature_ts");
							String path = uri.getPath();
							if (path != null && path.contains("/embed/")) {
								String cleanClaim = path.replace("/$/embed/", "").replace("/embed/", "");
								streamOdysee = "https://player.odycdn.com/api/v4/streams/free/" + cleanClaim + "/" + sig + "/" + sigTs;
							}
						}

						if (streamOdysee.isEmpty()) {
							String html = fetchHttpString(finalTargetUrl, "https://odysee.com/", false);
							Pattern pOdy = Pattern.compile("(https?://player\\.odycdn\\.com/api/v4/streams/[^\"'\\s]+)");
							Matcher mOdy = pOdy.matcher(html);
							if (mOdy.find()) {
								streamOdysee = mOdy.group(1).replace("\\/", "/");
							} else {
								Pattern pMedia = Pattern.compile("(https?://[^\"'\\s]+\\.(?:mp4|m3u8)[^\"'\\s]*)");
								Matcher mMedia = pMedia.matcher(html);
								if (mMedia.find()) {
									streamOdysee = mMedia.group(1).replace("\\/", "/");
								}
							}
						}

						if (streamOdysee.isEmpty() && finalTargetUrl.contains("/embed/")) {
							String afterEmbed = finalTargetUrl.substring(finalTargetUrl.indexOf("/embed/") + 7).split("[?#]")[0];
							String lbryUri = "lbry://" + afterEmbed.replace("/", "#");
							String lbryReq = "{\"method\":\"get\",\"params\":{\"uri\":\"" + lbryUri + "\"}}";
							String lbryRes = postJsonHttp("https://api.na-backend.odysee.com/api/v1/proxy", lbryReq);
							JSONObject lbryJson = new JSONObject(lbryRes);
							if (lbryJson.has("result")) {
								streamOdysee = lbryJson.getJSONObject("result").optString("streaming_url", "");
							}
						}

						final String finalOdyUrl = streamOdysee;
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!finalOdyUrl.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.add("Default");
									qualityUrls.add(finalOdyUrl);
									isAutoQualityMode = true;
									selectedQualityIndex = 0;
									updateQualityBadgeText();

									playVideoUrl(finalOdyUrl, 0);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();

		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (lowerUrl.contains(".mp4") || lowerUrl.contains(".m3u8")) {
							new Handler(Looper.getMainLooper()).post(new Runnable() {
								@Override
								public void run() {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.add("Default");
									qualityUrls.add(finalTargetUrl);
									isAutoQualityMode = true;
									selectedQualityIndex = 0;
									updateQualityBadgeText();

									playVideoUrl(finalTargetUrl, 0);
								}
							});
							return;
						}

						String html = fetchHttpString(finalTargetUrl, finalTargetUrl, false);
						String unpacked = unpackDeanEdwards(html);
						String combinedSource = html + "\n" + unpacked;
						String streamFound = "";

						Pattern pSource = Pattern.compile("<(?:source|video)[^>]+src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
						Matcher mSource = pSource.matcher(combinedSource);
						if (mSource.find()) {
							streamFound = mSource.group(1).replace("&amp;", "&");
						}

						if (streamFound.isEmpty()) {
							Pattern pMedia = Pattern.compile("(https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:m3u8|mp4)[^\"'\\s<>]*)", Pattern.CASE_INSENSITIVE);
							Matcher mMedia = pMedia.matcher(combinedSource);
							if (mMedia.find()) {
								streamFound = mMedia.group(1).replace("\\/", "/").replace("\\u0026", "&");
							}
						}

						if (streamFound.isEmpty()) {
							Pattern pIframe = Pattern.compile("<iframe[^>]+src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
							Matcher mIframe = pIframe.matcher(combinedSource);
							if (mIframe.find()) {
								String innerUrl = mIframe.group(1).replace("&amp;", "&");
								currentStreamReferer = innerUrl;
								String innerHtml = fetchHttpString(innerUrl, finalTargetUrl, false);
								String innerUnpacked = unpackDeanEdwards(innerHtml);
								String innerCombined = innerHtml + "\n" + innerUnpacked;
								Pattern pInnerMedia = Pattern.compile("(https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:m3u8|mp4)[^\"'\\s<>]*)", Pattern.CASE_INSENSITIVE);
								Matcher mInnerMedia = pInnerMedia.matcher(innerCombined);
								if (mInnerMedia.find()) {
									streamFound = mInnerMedia.group(1).replace("\\/", "/").replace("\\u0026", "&");
								}
							}
						}

						final String finalGenericUrl = streamFound;
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!finalGenericUrl.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.add("Default");
									qualityUrls.add(finalGenericUrl);
									isAutoQualityMode = true;
									selectedQualityIndex = 0;
									updateQualityBadgeText();

									playVideoUrl(finalGenericUrl, 0);
								} else {
									playViaWebViewInterceptor(finalTargetUrl);
								}
							}
						});
					} catch (Exception e) {
						playViaWebViewInterceptor(finalTargetUrl);
					}
				}
			}).start();
		}
	}

	public class ShimmerEpisodeAdapter extends RecyclerView.Adapter<ShimmerEpisodeAdapter.ViewHolder> {
		private final int count;
		public ShimmerEpisodeAdapter(int count) { this.count = count; }

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			float d = parent.getContext().getResources().getDisplayMetrics().density;
			View v = new View(parent.getContext());
			RecyclerView.LayoutParams params = new RecyclerView.LayoutParams((int)(65 * d), (int)(60 * d));
			params.setMarginEnd((int)(8 * d));
			v.setLayoutParams(params);
			v.setBackground(createShimmerDrawable(12 * d));
			return new ViewHolder(v);
		}

		@Override public void onBindViewHolder(ViewHolder holder, int position) {}
		@Override public int getItemCount() { return count; }
		public class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(View v) { super(v); } }
	}

	public class ShimmerRekomendasiAdapter extends RecyclerView.Adapter<ShimmerRekomendasiAdapter.ViewHolder> {
		private final int count;
		public ShimmerRekomendasiAdapter(int count) { this.count = count; }

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			Context ctx = parent.getContext();
			float d = ctx.getResources().getDisplayMetrics().density;

			LinearLayout box = new LinearLayout(ctx);
			box.setOrientation(LinearLayout.VERTICAL);
			GridLayoutManager.LayoutParams lp = new GridLayoutManager.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			lp.setMargins((int)(4 * d), 0, (int)(4 * d), (int)(14 * d));
			box.setLayoutParams(lp);

			View coverStub = new View(ctx);
			LinearLayout.LayoutParams covLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(145 * d));
			coverStub.setLayoutParams(covLp);
			coverStub.setBackground(createShimmerDrawable(10 * d));
			box.addView(coverStub);

			View titleStub = new View(ctx);
			LinearLayout.LayoutParams titLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(14 * d));
			titLp.topMargin = (int)(8 * d);
			titleStub.setLayoutParams(titLp);
			titleStub.setBackground(createShimmerDrawable(5 * d));
			box.addView(titleStub);

			return new ViewHolder(box);
		}

		@Override public void onBindViewHolder(ViewHolder holder, int position) {}
		@Override public int getItemCount() { return count; }
		public class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(View v) { super(v); } }
	}

	public class RecyclerEpisodeAdapter extends RecyclerView.Adapter<RecyclerEpisodeAdapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public RecyclerEpisodeAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.eps_list, parent, false);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			final int pos = _position;
			final View itemView = _holder.itemView;
			final float density = itemView.getContext().getResources().getDisplayMetrics().density;

			int widthPx = (int) (65 * density);
			int heightPx = (int) (60 * density);
			int marginEndPx = (int) (8 * density);

			RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(widthPx, heightPx);
			params.setMarginEnd(marginEndPx);
			itemView.setLayoutParams(params);

			GradientDrawable itemBg = new GradientDrawable();
			itemBg.setCornerRadius(12 * density);

			TextView tvEpisode = itemView.findViewById(R.id.textview1);
			final boolean isActive = (pos == currentEpisodeIndex);

			if (isActive) {
				itemBg.setColor(0xFF3B82F6);
				if (tvEpisode != null) {
					tvEpisode.setTextColor(0xFFFFFFFF);
					tvEpisode.setTypeface(null, Typeface.BOLD);
				}
			} else {
				itemBg.setColor(Color.parseColor("#12141A"));
				if (tvEpisode != null) {
					tvEpisode.setTextColor(0xFFCBD5E1);
					tvEpisode.setTypeface(null, Typeface.NORMAL);
				}
			}
			itemView.setBackground(itemBg);

			if (tvEpisode != null && _data.get(pos) != null) {
				HashMap<String, Object> itemData = _data.get(pos);
				if (itemData.containsKey("episode")) {
					Object epObj = itemData.get("episode");
					if (epObj != null) {
						tvEpisode.setText(epObj.toString().replace("Ep ", "").replace("Eps ", "").trim());
					}
				} else if (itemData.containsKey("title")) {
					Object tObj = itemData.get("title");
					if (tObj != null) {
						tvEpisode.setText(tObj.toString().replaceAll("[^0-9]", "").trim());
					}
				} else {
					tvEpisode.setText(String.valueOf(pos + 1));
				}
			}

			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentEpisodeIndex == pos && mediaPlayer != null && mediaPlayer.isPlaying()) {
						return;
					}

					if (episodeList != _data) {
						episodeList.clear();
						episodeList.addAll(_data);
					}

					playEpisodeByIndex(pos);
				}
			});
		}

		@Override
		public int getItemCount() {
			return _data.size();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}

	public class RecyclerRekomendasiAdapter extends RecyclerView.Adapter<RecyclerRekomendasiAdapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public RecyclerRekomendasiAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = getLayoutInflater().inflate(R.layout.custom_anime, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			final HashMap<String, Object> item = _data.get(position);
			View view = holder.itemView;
			float d = view.getResources().getDisplayMetrics().density;

			GridLayoutManager.LayoutParams lp = new GridLayoutManager.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			lp.setMargins((int)(4 * d), 0, (int)(4 * d), (int)(14 * d));
			view.setLayoutParams(lp);

			TextView textTitle = view.findViewById(R.id.text_title);
			TextView textStatus = view.findViewById(R.id.text_status);
			TextView textEpisode = view.findViewById(R.id.text_episode);
			ImageView imgCover = view.findViewById(R.id.img_cover);
			View view2 = view.findViewById(R.id.view2);

			if (textStatus != null) {
				GradientDrawable statusBg = new GradientDrawable();
				statusBg.setCornerRadius(50f);
				statusBg.setColor(0xFF3B82F6);
				textStatus.setBackground(statusBg);
				textStatus.setText(item.get("status") != null ? item.get("status").toString() : "");
			}
			if (view2 != null) {
				view2.setBackground(new GradientDrawable(
					GradientDrawable.Orientation.BOTTOM_TOP,
					new int[]{Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT}
				));
			}
			if (textTitle != null) textTitle.setText(item.get("title") != null ? item.get("title").toString() : "");
			if (textEpisode != null) textEpisode.setText(item.get("episode") != null ? item.get("episode").toString() : "");

			String coverUrl = item.get("cover") != null ? item.get("cover").toString().trim() : "";
			if (coverUrl.contains("i.ibb.co.com")) coverUrl = coverUrl.replace("i.ibb.co.com", "i.ibb.co");
			if (coverUrl.startsWith("http://")) coverUrl = coverUrl.replace("http://", "https://");
			if (imgCover != null && !coverUrl.isEmpty()) {
				Glide.with(getApplicationContext()).load(Uri.parse(coverUrl)).into(imgCover);
			}

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String nextSlug = item.get("slug") != null ? item.get("slug").toString() : "";
					if (!nextSlug.isEmpty()) {
						Intent intent = new Intent(WatchActivity.this, WatchActivity.class);
						intent.putExtra("slug", nextSlug);
						startActivity(intent);
						finish();
					}
				}
			});
		}

		@Override
		public int getItemCount() {
			return _data.size();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}
