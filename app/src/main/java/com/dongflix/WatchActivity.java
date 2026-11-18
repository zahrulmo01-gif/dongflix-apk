package com.dongflix;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class WatchActivity extends AppCompatActivity {

	private ArrayList<HashMap<String, Object>> eps = new ArrayList<>();
	private RequestNetwork req_watch;
	private RequestNetwork.RequestListener _req_watch_request_listener;

	private TextureView playerTextureView;
	private FrameLayout layoutPlayer;
	private View playerControlsOverlay, gradientTop, gradientBottom, topBar, bottomControls;
	private ImageView btnBack, btnPrevEpisode, btnRewind, btnPlay, btnForward, btnNextEpisode, btnFullscreen, imagePoster, iconExpandDetail;
	private TextView textPlayerTitle, btnSpeed, textTime, btnQuality, textJudul, textRating, textYear, textGenre, textActionServer, textSortEpisodes;
	private LinearLayout btnFill, btnOpenDetail, btnSortEpisodes, btnActionServer, btnActionDownload, btnActionShare, btnActionReport;
	private ProgressBar playerLoading;
	private SeekBar playerSeekBar;
	private RecyclerView recyclerEpisode, recyclerRekomendasi;

	// Detail data untuk Bottom Sheet ala YouTube
	private String detailTitle = "";
	private String detailSynopsis = "";
	private String detailStatus = "Donghua";
	private String detailYear = "2026";
	private String detailAllGenres = "-";
	private String detailCover = "";

	// State Sort Episode (true = Terbaru di depan, false = Terlama / Ep 1 di depan)
	private boolean isNewestFirst = true;

	public MediaPlayer mediaPlayer;
	public Handler playerHandler;
	public Surface playerSurface;
	public Runnable progressLoop;
	public Runnable hideControlsRunnable;
	public Runnable autoDowngradeRunnable;
	public boolean areControlsVisible = true;
	public String currentPlayingUrl = "";
	public boolean isFullscreen = false;
	public int savedSeekPosition = 0;

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

		btnBack = findViewById(R.id.btnBack);
		btnPrevEpisode = findViewById(R.id.btnPrevEpisode);
		btnRewind = findViewById(R.id.btnRewind);
		btnPlay = findViewById(R.id.btnPlay);
		btnForward = findViewById(R.id.btnForward);
		btnNextEpisode = findViewById(R.id.btnNextEpisode);
		btnFullscreen = findViewById(R.id.btnFullscreen);
		imagePoster = findViewById(R.id.imagePoster);
		iconExpandDetail = findViewById(R.id.iconExpandDetail);

		textPlayerTitle = findViewById(R.id.textPlayerTitle);
		btnSpeed = findViewById(R.id.btnSpeed);
		textTime = findViewById(R.id.textTime);
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
					eps = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
					if (eps != null && !eps.isEmpty()) {
						// API default urut dari lama ke baru -> reverse agar terbaru di depan
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
				} else {
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
									detailStatus = obj.optString("status", "Donghua");
									if (detailStatus.isEmpty()) detailStatus = "Donghua";

									detailCover = obj.optString("cover").trim();
									if (detailCover.contains("i.ibb.co.com")) {
										detailCover = detailCover.replace("i.ibb.co.com", "i.ibb.co");
									}
									if (detailCover.startsWith("http://")) {
										detailCover = detailCover.replace("http://", "https://");
									}
									String released = obj.optString("released");

									String firstGenre = "-";
									StringBuilder allGenresSb = new StringBuilder();
									JSONArray genresArray = obj.optJSONArray("genres");
									if (genresArray != null && genresArray.length() > 0) {
										firstGenre = genresArray.optString(0);
										for (int g = 0; g < genresArray.length(); g++) {
											if (g > 0) allGenresSb.append(", ");
											allGenresSb.append(genresArray.optString(g));
										}
									}
									detailAllGenres = allGenresSb.length() > 0 ? allGenresSb.toString() : firstGenre;

									if (released.length() >= 4) {
										detailYear = released.substring(released.length() - 4);
									}

									if (textJudul != null) textJudul.setText(detailTitle);
									if (textRating != null) textRating.setText(detailStatus);
									if (textYear != null) textYear.setText(detailYear);
									if (textGenre != null) textGenre.setText(firstGenre);

									if (imagePoster != null && !detailCover.isEmpty()) {
										Glide.with(getApplicationContext()).load(Uri.parse(detailCover)).into(imagePoster);
									}
									break;
								}
							}

							// Muat Rekomendasi Serupa (6 donghua)
							ArrayList<HashMap<String, Object>> allList = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
							if (allList != null && recyclerRekomendasi != null) {
								ArrayList<HashMap<String, Object>> rekomList = new ArrayList<>();
								for (HashMap<String, Object> item : allList) {
									String s = item.get("slug") != null ? item.get("slug").toString() : "";
									if (!s.equals(targetSlug)) {
										rekomList.add(item);
										if (rekomList.size() >= 6) break;
									}
								}
								recyclerRekomendasi.setAdapter(new RecyclerRekomendasiAdapter(rekomList));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}

			@Override
			public void onErrorResponse(String _param1, String _param2) {
			}
		};
	}

	private void initializeLogic() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(0xFF000000);
		}

		_WatchUI();

		String slug = getIntent().getStringExtra("slug");
		if (slug == null) slug = "";

		req_watch.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/", "", _req_watch_request_listener);
		req_watch.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/?slug=" + slug, "slug", _req_watch_request_listener);

		if (recyclerEpisode != null) {
			recyclerEpisode.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		}

		if (recyclerRekomendasi != null) {
			recyclerRekomendasi.setLayoutManager(new GridLayoutManager(this, 3));
			recyclerRekomendasi.setNestedScrollingEnabled(false);
		}

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
		float d = getResources().getDisplayMetrics().density;

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

		// Styling Badge Status (Biru elegan transparan)
		if (textRating != null) {
			GradientDrawable statusBg = new GradientDrawable();
			statusBg.setCornerRadius(100f);
			statusBg.setColor(Color.parseColor("#172554"));
			statusBg.setStroke((int)(1 * d), Color.parseColor("#3B82F6"));
			textRating.setBackground(statusBg);
		}

		// Styling Badge Tahun (Slate gelap elegan)
		if (textYear != null) {
			GradientDrawable yearBg = new GradientDrawable();
			yearBg.setCornerRadius(100f);
			yearBg.setColor(Color.parseColor("#1E293B"));
			yearBg.setStroke((int)(1 * d), Color.parseColor("#334155"));
			textYear.setBackground(yearBg);
		}

		// Styling Badge Genre (Slate gelap elegan)
		if (textGenre != null) {
			GradientDrawable genreBg = new GradientDrawable();
			genreBg.setCornerRadius(100f);
			genreBg.setColor(Color.parseColor("#0F172A"));
			genreBg.setStroke((int)(1 * d), Color.parseColor("#334155"));
			textGenre.setBackground(genreBg);
		}

		// Styling Ikon Expand Detail (Lingkaran tipis)
		if (iconExpandDetail != null) {
			GradientDrawable expandBg = new GradientDrawable();
			expandBg.setShape(GradientDrawable.OVAL);
			expandBg.setColor(Color.parseColor("#111827"));
			expandBg.setStroke((int)(1 * d), Color.parseColor("#1E293B"));
			iconExpandDetail.setBackground(expandBg);
		}

		// Styling Pill Sort Episode (Terbaru / Terlama)
		if (btnSortEpisodes != null) {
			GradientDrawable sortBg = new GradientDrawable();
			sortBg.setCornerRadius(100f);
			sortBg.setColor(Color.parseColor("#111827"));
			sortBg.setStroke((int)(1 * d), Color.parseColor("#1E293B"));
			btnSortEpisodes.setBackground(sortBg);
		}

		View[] actionBtns = {btnActionServer, btnActionDownload, btnActionShare, btnActionReport};
		for (View btn : actionBtns) {
			if (btn != null) {
				GradientDrawable btnBg = new GradientDrawable();
				btnBg.setCornerRadius(100f);
				btnBg.setColor(0xFF111111);
				btnBg.setStroke(2, 0xFF1E293B);
				btn.setBackground(btnBg);
			}
		}
	}

	// ========================================================
	// SORT EPISODE: TERBARU <-> TERLAMA
	// ========================================================
	public void toggleSortEpisodes() {
		if (episodeList == null || episodeList.size() <= 1) return;

		HashMap<String, Object> currentlyPlayingItem = episodeList.get(currentEpisodeIndex);
		Collections.reverse(episodeList);
		isNewestFirst = !isNewestFirst;

		// Pertahankan sorotan pada episode yang sedang diputar
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

	// ========================================================
	// BOTTOM SHEET: DETAIL DONGHUA & SINOPSIS (ALA YOUTUBE)
	// ========================================================
	public void showDetailBottomSheet() {
		final BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(28 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0F172A"));
		sheetBg.setCornerRadii(new float[]{24 * d, 24 * d, 24 * d, 24 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		// Drag handle
		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(40 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(16 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#334155"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		// Header Deskripsi + Tombol Tutup (X)
		LinearLayout topHeader = new LinearLayout(this);
		topHeader.setOrientation(LinearLayout.HORIZONTAL);
		topHeader.setGravity(Gravity.CENTER_VERTICAL);
		topHeader.setPadding(0, 0, 0, (int)(14 * d));

		TextView tvHeader = new TextView(this);
		tvHeader.setText("Deskripsi");
		tvHeader.setTextColor(Color.WHITE);
		tvHeader.setTextSize(18f);
		tvHeader.setTypeface(null, Typeface.BOLD);
		tvHeader.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		topHeader.addView(tvHeader);

		TextView btnClose = new TextView(this);
		btnClose.setText("✕");
		btnClose.setTextColor(Color.parseColor("#94A3B8"));
		btnClose.setTextSize(18f);
		btnClose.setPadding((int)(10 * d), (int)(4 * d), (int)(4 * d), (int)(4 * d));
		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sheetDialog.dismiss();
			}
		});
		topHeader.addView(btnClose);
		root.addView(topHeader);

		// Divider
		View divider = new View(this);
		LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(1 * d));
		divLp.bottomMargin = (int)(16 * d);
		divider.setLayoutParams(divLp);
		divider.setBackgroundColor(Color.parseColor("#1E293B"));
		root.addView(divider);

		ScrollView scrollView = new ScrollView(this);
		scrollView.setVerticalScrollBarEnabled(false);

		LinearLayout content = new LinearLayout(this);
		content.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(content);

		// Judul Lengkap
		TextView tvFullTitle = new TextView(this);
		tvFullTitle.setText(detailTitle.isEmpty() ? textJudul.getText().toString() : detailTitle);
		tvFullTitle.setTextColor(Color.WHITE);
		tvFullTitle.setTextSize(17f);
		tvFullTitle.setTypeface(null, Typeface.BOLD);
		tvFullTitle.setPadding(0, 0, 0, (int)(14 * d));
		content.addView(tvFullTitle);

		// Kartu Metadata (Status, Tahun, Episode, Genre)
		LinearLayout metaCard = new LinearLayout(this);
		metaCard.setOrientation(LinearLayout.VERTICAL);
		metaCard.setPadding((int)(14 * d), (int)(14 * d), (int)(14 * d), (int)(14 * d));
		LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		metaLp.bottomMargin = (int)(16 * d);
		metaCard.setLayoutParams(metaLp);

		GradientDrawable metaBg = new GradientDrawable();
		metaBg.setColor(Color.parseColor("#1E293B"));
		metaBg.setCornerRadius(14 * d);
		metaCard.setBackground(metaBg);

		TextView tvMetaInfo = new TextView(this);
		String epTotalText = (episodeList != null && !episodeList.isEmpty()) ? episodeList.size() + " Episode" : "-";
		tvMetaInfo.setText(
			"Status  :  " + detailStatus + "\n" +
			"Rilis      :  " + detailYear + "\n" +
			"Total     :  " + epTotalText + "\n" +
			"Genre   :  " + detailAllGenres
		);
		tvMetaInfo.setTextColor(Color.parseColor("#CBD5E1"));
		tvMetaInfo.setTextSize(13f);
		tvMetaInfo.setLineSpacing(6 * d, 1f);
		metaCard.addView(tvMetaInfo);
		content.addView(metaCard);

		// Label Sinopsis
		TextView tvSinopsisLabel = new TextView(this);
		tvSinopsisLabel.setText("Sinopsis");
		tvSinopsisLabel.setTextColor(Color.WHITE);
		tvSinopsisLabel.setTextSize(14f);
		tvSinopsisLabel.setTypeface(null, Typeface.BOLD);
		tvSinopsisLabel.setPadding(0, 0, 0, (int)(8 * d));
		content.addView(tvSinopsisLabel);

		// Isi Sinopsis Lengkap
		TextView tvSinopsisBody = new TextView(this);
		tvSinopsisBody.setText(detailSynopsis.isEmpty() ? "Sinopsis belum tersedia untuk donghua ini." : detailSynopsis);
		tvSinopsisBody.setTextColor(Color.parseColor("#94A3B8"));
		tvSinopsisBody.setTextSize(13.5f);
		tvSinopsisBody.setLineSpacing(5 * d, 1f);
		content.addView(tvSinopsisBody);

		root.addView(scrollView);
		attachTransparentSheetContainer(sheetDialog, root);
	}

	@Override
	public void onDestroy() {
		if (playerHandler != null) {
			if (progressLoop != null) playerHandler.removeCallbacks(progressLoop);
			if (hideControlsRunnable != null) playerHandler.removeCallbacks(hideControlsRunnable);
			if (autoDowngradeRunnable != null) playerHandler.removeCallbacks(autoDowngradeRunnable);
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
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			if (btnPlay != null) {
				btnPlay.setImageResource(R.drawable.ic_play1);
			}
		}
	}

	@Override
	public void onBackPressed() {
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
		if (playerControlsOverlay != null) {
			playerControlsOverlay.setVisibility(View.VISIBLE);
			playerControlsOverlay.bringToFront();
		}
		areControlsVisible = true;
		resetControlsTimer();
	}

	public void hideControls() {
		if (playerControlsOverlay != null) {
			playerControlsOverlay.setVisibility(View.GONE);
		}
		areControlsVisible = false;
		if (playerHandler != null && hideControlsRunnable != null) {
			playerHandler.removeCallbacks(hideControlsRunnable);
		}
	}

	public void toggleControls() {
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

	public void playVideoUrl(final String rawUrl, final int targetSeek) {
		if (rawUrl == null || rawUrl.trim().isEmpty()) return;

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
					headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

					if (cleanUrl.contains("ok.ru") || cleanUrl.contains("odnoklassniki")) {
						headers.put("Referer", "https://ok.ru/");
					} else if (cleanUrl.contains("dailymotion") || cleanUrl.contains("dmcdn")) {
						headers.put("Referer", "https://www.dailymotion.com/");
						if (dmSessionCookie != null && !dmSessionCookie.isEmpty()) {
							headers.put("Cookie", dmSessionCookie);
						}
					}

					headers.put("Accept", "*/*");

					mediaPlayer.setDataSource(WatchActivity.this, Uri.parse(cleanUrl), headers);
					mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

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

							int duration = mp.getDuration();
							if (playerSeekBar != null) playerSeekBar.setMax(duration);

							if (savedSeekPosition > 0 && savedSeekPosition < duration) {
								mp.seekTo(savedSeekPosition);
							}
							mp.start();

							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentPlaybackSpeed != 1.0f) {
								try {
									PlaybackParams params = mp.getPlaybackParams();
									if (params == null) params = new PlaybackParams();
									params.setSpeed(currentPlaybackSpeed);
									mp.setPlaybackParams(params);
								} catch (Exception ignored) {}
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
							if (playerLoading != null) playerLoading.setVisibility(View.GONE);
							if (btnPlay != null) {
								btnPlay.setEnabled(true);
								btnPlay.setAlpha(1.0f);
								btnPlay.setImageResource(R.drawable.ic_play1);
							}
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

	private void attachTransparentSheetContainer(final BottomSheetDialog sheetDialog, View contentView) {
		sheetDialog.setContentView(contentView);
		sheetDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				BottomSheetDialog d = (BottomSheetDialog) dialog;
				FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				if (bottomSheet != null) {
					bottomSheet.setBackgroundColor(Color.TRANSPARENT);
					bottomSheet.setBackgroundTintList(null);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						bottomSheet.setElevation(0f);
					}
				}
			}
		});
		sheetDialog.show();
	}

	private void showModernBottomSheet(String title, String subtitle, ArrayList<String> optionTitles, ArrayList<String> optionBadges, int activeIdx, final OnSheetItemClickListener listener) {
		final BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(24 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0F172A"));
		sheetBg.setCornerRadii(new float[]{24 * d, 24 * d, 24 * d, 24 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(40 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(16 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#334155"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		TextView tvTitle = new TextView(this);
		tvTitle.setText(title);
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(18f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		root.addView(tvTitle);

		if (subtitle != null && !subtitle.isEmpty()) {
			TextView tvSub = new TextView(this);
			tvSub.setText(subtitle);
			tvSub.setTextColor(Color.parseColor("#94A3B8"));
			tvSub.setTextSize(12f);
			tvSub.setPadding(0, (int)(2 * d), 0, (int)(14 * d));
			root.addView(tvSub);
		} else {
			tvTitle.setPadding(0, 0, 0, (int)(14 * d));
		}

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
			rowLp.bottomMargin = (int)(10 * d);
			itemRow.setLayoutParams(rowLp);

			GradientDrawable rowBg = new GradientDrawable();
			rowBg.setCornerRadius(14 * d);
			if (isSelected) {
				rowBg.setColor(Color.parseColor("#1E3A8A"));
				rowBg.setStroke((int)(1.5f * d), Color.parseColor("#3B82F6"));
			} else {
				rowBg.setColor(Color.parseColor("#1E293B"));
				rowBg.setStroke((int)(1 * d), Color.parseColor("#334155"));
			}
			itemRow.setBackground(rowBg);

			TextView tvName = new TextView(this);
			tvName.setText(optionTitles.get(i));
			tvName.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#E2E8F0"));
			tvName.setTextSize(15f);
			tvName.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
			LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
			tvName.setLayoutParams(nameLp);
			itemRow.addView(tvName);

			if (optionBadges != null && i < optionBadges.size() && !optionBadges.get(i).isEmpty()) {
				TextView tvBadge = new TextView(this);
				tvBadge.setText(optionBadges.get(i));
				tvBadge.setTextSize(11f);
				tvBadge.setTypeface(null, Typeface.BOLD);
				tvBadge.setTextColor(isSelected ? Color.parseColor("#93C5FD") : Color.parseColor("#94A3B8"));
				tvBadge.setPadding((int)(8 * d), (int)(3 * d), (int)(8 * d), (int)(3 * d));
				GradientDrawable bBg = new GradientDrawable();
				bBg.setCornerRadius(8 * d);
				bBg.setColor(isSelected ? Color.parseColor("#172554") : Color.parseColor("#0F172A"));
				tvBadge.setBackground(bBg);
				LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				bLp.rightMargin = (int)(10 * d);
				tvBadge.setLayoutParams(bLp);
				itemRow.addView(tvBadge);
			}

			if (isSelected) {
				TextView tvCheck = new TextView(this);
				tvCheck.setText("✓");
				tvCheck.setTextColor(Color.parseColor("#3B82F6"));
				tvCheck.setTextSize(16f);
				tvCheck.setTypeface(null, Typeface.BOLD);
				itemRow.addView(tvCheck);
			}

			itemRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					sheetDialog.dismiss();
					listener.onItemClick(index);
				}
			});

			listContainer.addView(itemRow);
		}

		root.addView(scrollView);
		attachTransparentSheetContainer(sheetDialog, root);
	}

	private interface OnSheetItemClickListener {
		void onItemClick(int index);
	}

	public void showAllEpisodesBottomSheet() {
		if (episodeList == null || episodeList.isEmpty()) {
			Toast.makeText(WatchActivity.this, "Daftar episode belum dimuat", Toast.LENGTH_SHORT).show();
			return;
		}

		final BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int)(20 * d), (int)(12 * d), (int)(20 * d), (int)(24 * d));

		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0F172A"));
		sheetBg.setCornerRadii(new float[]{24 * d, 24 * d, 24 * d, 24 * d, 0, 0, 0, 0});
		root.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int)(40 * d), (int)(4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int)(16 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#334155"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		root.addView(handle);

		LinearLayout headerRow = new LinearLayout(this);
		headerRow.setOrientation(LinearLayout.HORIZONTAL);
		headerRow.setGravity(Gravity.CENTER_VERTICAL);
		headerRow.setPadding(0, 0, 0, (int)(16 * d));

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Semua Episode (" + (isNewestFirst ? "Terbaru" : "Terlama") + ")");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(17f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		headerRow.addView(tvTitle);

		TextView tvCount = new TextView(this);
		tvCount.setText(episodeList.size() + " Ep");
		tvCount.setTextColor(Color.parseColor("#3B82F6"));
		tvCount.setTextSize(13f);
		tvCount.setTypeface(null, Typeface.BOLD);
		headerRow.addView(tvCount);

		root.addView(headerRow);

		ScrollView scrollView = new ScrollView(this);
		scrollView.setVerticalScrollBarEnabled(false);

		LinearLayout gridContainer = new LinearLayout(this);
		gridContainer.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(gridContainer);

		int columns = 5;
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
				LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, (int)(48 * d), 1f);
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
					cellBtn.setTextColor(isActive ? Color.WHITE : Color.parseColor("#94A3B8"));

					GradientDrawable cellBg = new GradientDrawable();
					cellBg.setCornerRadius(12 * d);
					if (isActive) {
						cellBg.setColor(Color.parseColor("#3B82F6"));
					} else {
						cellBg.setColor(Color.parseColor("#1E293B"));
						cellBg.setStroke((int)(1 * d), Color.parseColor("#334155"));
					}
					cellBtn.setBackground(cellBg);

					cellBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							sheetDialog.dismiss();
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
		attachTransparentSheetContainer(sheetDialog, root);
	}

	public void showServerDialog() {
		if (currentEpisodeServerNames == null || currentEpisodeServerNames.isEmpty()) {
			Toast.makeText(WatchActivity.this, "Daftar server belum siap", Toast.LENGTH_SHORT).show();
			return;
		}

		ArrayList<String> badges = new ArrayList<>();
		for (String s : currentEpisodeServerNames) {
			if (s.toLowerCase().contains("ok")) badges.add("Rekomendasi");
			else if (s.toLowerCase().contains("daily")) badges.add("Multi-Res");
			else badges.add("Mirror");
		}

		showModernBottomSheet(
			"Pilih Server Streaming",
			"Ganti server jika video terasa lambat atau gagal dimuat",
			currentEpisodeServerNames,
			badges,
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
		ArrayList<String> badges = new ArrayList<>();

		String currentActiveRes = qualityNames.get(selectedQualityIndex);
		titles.add("Auto");
		badges.add(currentActiveRes.equalsIgnoreCase("Auto") ? "Otomatis" : "Aktif: " + currentActiveRes);

		for (String q : qualityNames) {
			titles.add(q);
			if (q.contains("1080")) badges.add("Full HD");
			else if (q.contains("720")) badges.add("HD");
			else if (q.contains("480")) badges.add("SD");
			else if (q.contains("360") || q.contains("240") || q.contains("144")) badges.add("Hemat Kuota");
			else badges.add("");
		}

		int activeSheetIdx = isAutoQualityMode ? 0 : (selectedQualityIndex + 1);

		showModernBottomSheet(
			"Kualitas Video",
			"Auto menyesuaikan resolusi secara otomatis dengan kecepatan jaringan",
			titles,
			badges,
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
						int realIdx = which - 1;
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
		speedLabels.add("1.0x (Normal)");
		speedLabels.add("1.25x");
		speedLabels.add("1.5x");
		speedLabels.add("2.0x");

		final ArrayList<String> speedBadges = new ArrayList<>();
		speedBadges.add("Lambat");
		speedBadges.add("");
		speedBadges.add("Default");
		speedBadges.add("");
		speedBadges.add("Cepat");
		speedBadges.add("Sangat Cepat");

		final float[] speedVals = new float[]{0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
		int activeIdx = 2;
		for (int i = 0; i < speedVals.length; i++) {
			if (Math.abs(speedVals[i] - currentPlaybackSpeed) < 0.01f) {
				activeIdx = i;
				break;
			}
		}

		showModernBottomSheet(
			"Kecepatan Pemutaran",
			"Atur tempo pemutaran video sesuai kenyamananmu",
			speedLabels,
			speedBadges,
			activeIdx,
			new OnSheetItemClickListener() {
				@Override
				public void onItemClick(int which) {
					float selectedSpeed = speedVals[which];
					currentPlaybackSpeed = selectedSpeed;
					String label = (which == 2) ? "1x" : speedLabels.get(which);

					if (btnSpeed != null) {
						btnSpeed.setText(label);
					}

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						if (mediaPlayer != null) {
							try {
								boolean isPlaying = mediaPlayer.isPlaying();
								PlaybackParams params = mediaPlayer.getPlaybackParams();
								if (params == null) params = new PlaybackParams();
								params.setSpeed(selectedSpeed);
								mediaPlayer.setPlaybackParams(params);
								if (!isPlaying) mediaPlayer.pause();
							} catch (Exception ignored) {}
						}
					}
				}
			}
		);
	}

	public void toggleFullscreen() {
		final Window window = getWindow();
		final View decorView = window.getDecorView();
		final View scrollContent = findViewById(R.id.vscroll16);
		final float density = getResources().getDisplayMetrics().density;

		if (!isFullscreen) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				WindowManager.LayoutParams lp = window.getAttributes();
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
				window.setAttributes(lp);
			}

			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

			decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
				textPlayerTitle.setVisibility(View.VISIBLE);
			}

			if (btnRewind != null) btnRewind.setVisibility(View.GONE);
			if (btnForward != null) btnForward.setVisibility(View.GONE);
			if (btnFullscreen != null) btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);

			int padHoriz = (int) (32 * density);
			int padTop = (int) (16 * density);
			int padBottom = (int) (12 * density);
			if (topBar != null) topBar.setPadding(padHoriz, padTop, padHoriz, 0);
			if (bottomControls != null) bottomControls.setPadding(padHoriz, 0, padHoriz, padBottom);

			isFullscreen = true;
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				WindowManager.LayoutParams lp = window.getAttributes();
				lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
				window.setAttributes(lp);
			}

			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
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
			if (btnRewind != null) btnRewind.setVisibility(View.VISIBLE);
			if (btnForward != null) btnForward.setVisibility(View.VISIBLE);
			if (btnFullscreen != null) btnFullscreen.setImageResource(R.drawable.ic_fullscreen);

			int defPadX = (int) (16 * density);
			int defPadTop = (int) (12 * density);
			int defPadBottom = (int) (6 * density);
			if (topBar != null) topBar.setPadding(defPadX, defPadTop, defPadX, 0);
			if (bottomControls != null) bottomControls.setPadding(0, 0, 0, defPadBottom);

			isFullscreen = false;
		}
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
			public boolean onDoubleTap(MotionEvent e) {
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
					Toast.makeText(WatchActivity.this, "-10 detik", Toast.LENGTH_SHORT).show();
				} else {
					int targetPos = Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 10000);
					mediaPlayer.seekTo(targetPos);
					if (playerSeekBar != null) {
						playerSeekBar.setProgress(targetPos);
						playerSeekBar.invalidate();
					}
					Toast.makeText(WatchActivity.this, "+10 detik", Toast.LENGTH_SHORT).show();
				}
				resetControlsTimer();
				return true;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}
		});

		if (layoutPlayer != null) {
			layoutPlayer.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return playerGestureDetector.onTouchEvent(event);
				}
			});
		}

		if (playerControlsOverlay != null) {
			playerControlsOverlay.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return playerGestureDetector.onTouchEvent(event);
				}
			});
		}

		if (playerTextureView != null) {
			playerTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
				@Override
				public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
					playerSurface = new Surface(surfaceTexture);
					if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
						mediaPlayer.setSurface(playerSurface);
					}
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

		if (btnQuality != null) {
			btnQuality.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showQualityDialog();
				}
			});
		}

		if (btnSpeed != null) {
			btnSpeed.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showSpeedDialog();
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

		final boolean[] isFillMode = new boolean[]{false};
		if (btnFill != null) {
			btnFill.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetControlsTimer();
					isFillMode[0] = !isFillMode[0];
					if (playerTextureView != null) {
						if (isFillMode[0]) {
							playerTextureView.setScaleX(1.33f);
							playerTextureView.setScaleY(1.33f);
							Toast.makeText(WatchActivity.this, "Mode: Fill", Toast.LENGTH_SHORT).show();
						} else {
							playerTextureView.setScaleX(1.0f);
							playerTextureView.setScaleY(1.0f);
							Toast.makeText(WatchActivity.this, "Mode: Original", Toast.LENGTH_SHORT).show();
						}
					}
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

		if (serverKey.contains("ok") || serverKey.contains("odnoklassniki")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					HttpURLConnection conn = null;
					BufferedReader reader = null;
					try {
						URL url = new URL(finalTargetUrl);
						conn = (HttpURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						conn.setConnectTimeout(8000);
						conn.setReadTimeout(8000);
						conn.setInstanceFollowRedirects(true);
						conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
						conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
						conn.setRequestProperty("Referer", "https://ok.ru/");

						reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
						String line;
						String foundOptions = "";

						while ((line = reader.readLine()) != null) {
							int idx = line.indexOf("data-options=");
							if (idx != -1) {
								int qStart = idx + 13;
								char quote = line.charAt(qStart);
								int qEnd = line.indexOf(quote, qStart + 1);
								if (qEnd != -1) {
									foundOptions = line.substring(qStart + 1, qEnd);
									break;
								}
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
									if (playerLoading != null) playerLoading.setVisibility(View.GONE);
									Toast.makeText(WatchActivity.this, "Gagal parsing video OK.ru", Toast.LENGTH_SHORT).show();
								}
							}
						});

					} catch (final Exception e) {
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								String err = (e != null && e.getMessage() != null) ? e.getMessage() : "Koneksi terputus";
								Toast.makeText(WatchActivity.this, "Error OK.ru: " + err, Toast.LENGTH_SHORT).show();
							}
						});
					} finally {
						try {
							if (reader != null) reader.close();
							if (conn != null) conn.disconnect();
						} catch (Exception ignored) {}
					}
				}
			}).start();
		} else if (serverKey.contains("daily") || serverKey.contains("dailymotion")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					HttpURLConnection conn = null;
					BufferedReader reader = null;
					try {
						String dailyId = "";
						if (finalTargetUrl.contains("id=")) {
							Uri uri = Uri.parse(finalTargetUrl);
							dailyId = uri.getQueryParameter("id");
						} else if (finalTargetUrl.contains("/video/")) {
							String[] parts = finalTargetUrl.split("/video/");
							if (parts.length > 1) dailyId = parts[1].split("\\?")[0];
						} else {
							String cPath = finalTargetUrl.split("\\?")[0];
							dailyId = cPath.substring(cPath.lastIndexOf('/') + 1);
						}

						if (dailyId == null || dailyId.trim().isEmpty()) {
							throw new Exception("ID tidak ditemukan");
						}

						String dmApiUrl = "https://www.dailymotion.com/player/metadata/video/" + dailyId.trim();
						URL url = new URL(dmApiUrl);
						conn = (HttpURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						conn.setConnectTimeout(6000);
						conn.setReadTimeout(6000);
						conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0.0.0 Safari/537.36");
						conn.setRequestProperty("Referer", "https://www.dailymotion.com/");
						conn.setRequestProperty("Origin", "https://www.dailymotion.com");

						reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
						StringBuilder sb = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) sb.append(line);

						final ArrayList<String> dmNames = new ArrayList<>();
						final ArrayList<String> dmUrls = new ArrayList<>();
						String resBody = sb.toString();

						if (!resBody.isEmpty()) {
							JSONObject metaObj = new JSONObject(resBody);
							if (metaObj.has("qualities")) {
								JSONObject qualities = metaObj.getJSONObject("qualities");
								String[] checkKeys = new String[]{"auto", "1080", "720", "480", "380", "240"};

								for (String qKey : checkKeys) {
									if (qualities.has(qKey)) {
										JSONArray qArr = qualities.optJSONArray(qKey);
										if (qArr != null && qArr.length() > 0) {
											String streamUrl = qArr.getJSONObject(0).optString("url");
											if (!streamUrl.isEmpty()) {
												String label = qKey.equalsIgnoreCase("auto") ? "Auto" : qKey + "P";
												dmNames.add(label);
												dmUrls.add(streamUrl);
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
									if (playerLoading != null) playerLoading.setVisibility(View.GONE);
									Toast.makeText(WatchActivity.this, "Stream Dailymotion tidak tersedia", Toast.LENGTH_SHORT).show();
								}
							}
						});

					} catch (final Exception e) {
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								Toast.makeText(WatchActivity.this, "Gagal memproses Dailymotion", Toast.LENGTH_SHORT).show();
							}
						});
					} finally {
						try {
							if (reader != null) reader.close();
							if (conn != null) conn.disconnect();
						} catch (Exception ignored) {}
					}
				}
			}).start();
		} else if (serverKey.contains("d-tube") || serverKey.contains("dtube")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String videoHash = "";
						if (finalTargetUrl.contains("?v=")) {
							Uri uri = Uri.parse(finalTargetUrl);
							videoHash = uri.getQueryParameter("v");
						} else if (finalTargetUrl.contains("/v/")) {
							String[] parts = finalTargetUrl.split("/v/");
							if (parts.length > 1) videoHash = parts[1].split("[/?#]")[0];
						} else {
							String cleanPath = finalTargetUrl.split("\\?")[0];
							videoHash = cleanPath.substring(cleanPath.lastIndexOf('/') + 1);
						}

						if (videoHash == null || videoHash.trim().isEmpty()) {
							throw new Exception("Hash D-Tube tidak ditemukan");
						}

						final String directDtubeUrl = "https://ipfs.io/ipfs/" + videoHash.trim();

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								qualityNames.clear();
								qualityUrls.clear();
								qualityNames.add("Default");
								qualityUrls.add(directDtubeUrl);
								isAutoQualityMode = true;
								selectedQualityIndex = 0;
								updateQualityBadgeText();

								playVideoUrl(directDtubeUrl, 0);
							}
						});
					} catch (Exception e) {
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								Toast.makeText(WatchActivity.this, "Gagal memuat server D-Tube", Toast.LENGTH_SHORT).show();
							}
						});
					}
				}
			}).start();
		} else if (serverKey.contains("rumble")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					HttpURLConnection conn = null;
					BufferedReader reader = null;
					try {
						URL url = new URL(finalTargetUrl);
						conn = (HttpURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						conn.setConnectTimeout(6000);
						conn.setReadTimeout(6000);
						conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0.0.0 Safari/537.36");

						reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
						StringBuilder sb = new StringBuilder();
						String line;
						String streamUrl = "";

						while ((line = reader.readLine()) != null) {
							sb.append(line);
							if (line.contains(".mp4") && line.contains("https://")) {
								Pattern p = Pattern.compile("\"url\":\\s*\"(https:[^\"\\s]+?\\.mp4)\"");
								Matcher m = p.matcher(line);
								if (m.find()) {
									streamUrl = m.group(1).replace("\\/", "/");
									break;
								}
							}
						}

						if (streamUrl.isEmpty()) {
							Pattern p2 = Pattern.compile("(https?://[a-zA-Z0-9.-]+\\.rumble\\.com/[^\"'\\s]+\\.mp4)");
							Matcher m2 = p2.matcher(sb.toString());
							if (m2.find()) {
								streamUrl = m2.group(1);
							}
						}

						final String finalRumbleUrl = streamUrl;
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (!finalRumbleUrl.isEmpty()) {
									qualityNames.clear();
									qualityUrls.clear();
									qualityNames.add("Default");
									qualityUrls.add(finalRumbleUrl);
									isAutoQualityMode = true;
									selectedQualityIndex = 0;
									updateQualityBadgeText();

									playVideoUrl(finalRumbleUrl, 0);
								} else {
									if (playerLoading != null) playerLoading.setVisibility(View.GONE);
									Toast.makeText(WatchActivity.this, "Rumble membutuhkan Private DNS", Toast.LENGTH_SHORT).show();
								}
							}
						});

					} catch (Exception e) {
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								Toast.makeText(WatchActivity.this, "Gagal koneksi Rumble", Toast.LENGTH_SHORT).show();
							}
						});
					} finally {
						try {
							if (reader != null) reader.close();
							if (conn != null) conn.disconnect();
						} catch (Exception ignored) {}
					}
				}
			}).start();
		} else if (serverKey.contains("odysee")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String streamOdysee = "";
						if (finalTargetUrl.contains("signature=")) {
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
							streamOdysee = finalTargetUrl;
						}

						final String finalOdyUrl = streamOdysee;
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								qualityNames.clear();
								qualityUrls.clear();
								qualityNames.add("Default");
								qualityUrls.add(finalOdyUrl);
								isAutoQualityMode = true;
								selectedQualityIndex = 0;
								updateQualityBadgeText();

								playVideoUrl(finalOdyUrl, 0);
							}
						});
					} catch (Exception e) {
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								Toast.makeText(WatchActivity.this, "Gagal memproses server Odysee", Toast.LENGTH_SHORT).show();
							}
						});
					}
				}
			}).start();
		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					HttpURLConnection conn = null;
					BufferedReader reader = null;
					try {
						String lowerTarget = finalTargetUrl.toLowerCase();
						if (lowerTarget.contains(".mp4") || lowerTarget.contains(".m3u8")) {
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

						URL url = new URL(finalTargetUrl);
						conn = (HttpURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						conn.setConnectTimeout(5000);
						conn.setReadTimeout(5000);
						conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0.0.0 Safari/537.36");

						reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
						String line;
						String streamFound = "";

						while ((line = reader.readLine()) != null) {
							if (line.contains(".m3u8") || line.contains(".mp4")) {
								Pattern p = Pattern.compile("(https?://[^\"'\\s]+\\.(?:m3u8|mp4)[^\"'\\s]*)");
								Matcher m = p.matcher(line);
								if (m.find()) {
									streamFound = m.group(1).replace("\\/", "/");
									break;
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
									if (playerLoading != null) playerLoading.setVisibility(View.GONE);
									Toast.makeText(WatchActivity.this, "Server " + serverKey + " terproteksi captcha/iklan", Toast.LENGTH_SHORT).show();
								}
							}
						});

					} catch (Exception e) {
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								if (playerLoading != null) playerLoading.setVisibility(View.GONE);
								Toast.makeText(WatchActivity.this, "Server gagal dimuat", Toast.LENGTH_SHORT).show();
							}
						});
					} finally {
						try {
							if (reader != null) reader.close();
							if (conn != null) conn.disconnect();
						} catch (Exception ignored) {}
					}
				}
			}).start();
		}
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
			itemBg.setCornerRadius(14 * density);

			TextView tvEpisode = itemView.findViewById(R.id.textview1);
			final boolean isActive = (pos == currentEpisodeIndex);

			if (isActive) {
				itemBg.setColor(0xFF3B82F6);
				if (tvEpisode != null) {
					tvEpisode.setTextColor(0xFFFFFFFF);
					tvEpisode.setTypeface(null, Typeface.BOLD);
				}
			} else {
				itemBg.setColor(0xFF111111);
				if (tvEpisode != null) {
					tvEpisode.setTextColor(0xFF94A3B8);
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
