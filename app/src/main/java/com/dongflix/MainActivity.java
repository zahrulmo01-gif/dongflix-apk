package com.dongflix;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.view.animation.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";
	private static final String CATALOG_URL = "https://dongflix-api1.zahrulmo01.workers.dev/";

	private String currentUserKey = "";

	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> fullCatalogList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> historyList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> favoritesList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> downloadsList = new ArrayList<>();
	private final Map<String, Long> viewsTotalMap = new HashMap<>();
	private final Map<String, Long> viewsDailyMap = new HashMap<>();

	private DatabaseReference viewsTotalRef;
	private DatabaseReference viewsDailyTodayRef;
	private ValueEventListener viewsTotalListener;
	private ValueEventListener viewsDailyListener;

	private RequestNetwork dongflix_req;
	private RequestNetwork.RequestListener _dongflix_req_request_listener;

	private GridView gridview1;
	private RecyclerView recyclerPopular, recyclerPopularDongflix, recyclerRomanceDonghua, recyclerSearchList;
	private RecyclerView recyclerCultivationDonghua, recyclerCompletedDonghua, recyclerComedyDonghua, recyclerMysteryDonghua, recyclerMovieDonghua;
	private TextView textHeroTitle, textHeroDesc, textSeeAll, btnClearSearch, textSearchSectionLabel;
	private TextView textCultivationSeeAll, textCompletedSeeAll, textComedySeeAll, textMysterySeeAll, textMovieSeeAll;
	private ImageView imgHeroBanner, imgPlayIcon, imgHeroFavorite, imgTopNotif, imgTopChat, imgUserAvatar;
	private LinearLayout btnHeroPlay, indicatorCapsule, searchBarContainer;
	private FrameLayout btnHeroFavorite, btnTopNotif, btnTopChat;
	private View dot0, dot1, dot2;
	private EditText inputSearch;

	private final Handler heroSliderHandler = new Handler(Looper.getMainLooper());
	private Runnable heroSliderRunnable;
	private int heroCurrentIndex = 0;

	private FrameLayout pullRefreshIndicator;
	private ProgressBar pullRefreshSpinner;
	private ImageView imgPullRefreshIcon;
	private boolean isRefreshingCatalog = false;
	private float pullStartY = 0f;
	private boolean isPullingFromTop = false;

	private LinearLayout layoutSectionContinue;
	private TextView textTitleContinue, textContinueSeeAll;
	private RecyclerView recyclerContinue;

	private TextView tabPustakaRiwayat, tabPustakaDaftarku, tabPustakaUnduhan;
	private View linePustakaRiwayat, linePustakaDaftarku, linePustakaUnduhan;
	private TextView textPustakaSectionLabel;
	private RecyclerView recyclerPustakaList;
	private int activePustakaTab = 0;

	private FrameLayout navbarExpanded, btnCompactHome, miniPlayerInlineSlot, btnCircleSearch;
	private LinearLayout tabBeranda, tabPustaka, tabAkun;
	private ImageView imgHome, imgFavorite, imgProfile, imgSearch;
	private TextView txtHome, txtFavorite, txtProfile;
	private View liquidDragPill;
	private int activeNavTabIndex = 0;
	private boolean isNavbarCompact = false;

	private boolean isDraggingNavbarPill = false;
	private float navDownRawX = 0f;
	private float navInitialPillTx = 0f;

	private ValueAnimator mainShimmerAnimator;
	private float mainShimmerPhase = 0f;
	private boolean isMainLoading = true;

	private SharedPreferences favPrefs;
	private HashMap<String, Object> currentHeroItem;
	private Typeface outfitFont;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			Intent loginIntent = new Intent(this, LoginActivity.class);
			loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(loginIntent);
			finish();
			return;
		}

		setContentView(R.layout.activity_main);
		CrashLogDialog.showIfAvailable(this);

		DongflixPlayerManager.getInstance().initWithApplication(getApplication());
		favPrefs = getSharedPreferences("dongflix_favorites", Context.MODE_PRIVATE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
			&& checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 201);
		}

		initialize();
		setupContinueWatchingSection();
		setupPullToRefresh();
		setupPustakaSection();
		initializeLogic();
		loadLoggedInUserProfile(currentUser);
		loadUserHistoryAndFavoritesFromFirebase(currentUser);
		attachFirebasePopularityListeners();
		loadLocalDownloads();
	}

	private String safeStr(Map<String, Object> map, String key, String fallback) {
		if (map == null || key == null) return fallback;
		Object val = map.get(key);
		return val != null ? val.toString() : fallback;
	}

	private String normalizeCoverUrl(String raw) {
		if (raw == null) return "";
		String url = raw.trim().replace("i.ibb.co.com", "i.ibb.co");
		return url.startsWith("http://") ? url.replace("http://", "https://") : url;
	}

	private String cleanText(String input) {
		if (input == null) return "";
		return input.replaceAll("\\[[^\\x20-\\x7E]+\\]", "").replaceAll("[^\\x20-\\x7E\\n]", "").trim();
	}

	private Typeface getOutfitTypeface() {
		if (outfitFont != null) return outfitFont;
		String[] candidates = {
			"fonts/outfit_regular.ttf",
			"fonts/outfit_medium.ttf",
			"fonts/outfit.ttf",
			"fonts/Outfit-Regular.ttf",
			"fonts/productsans_reg.ttf"
		};
		for (String path : candidates) {
			try {
				outfitFont = Typeface.createFromAsset(getAssets(), path);
				if (outfitFont != null) return outfitFont;
			} catch (Exception ignored) {}
		}
		outfitFont = Typeface.DEFAULT;
		return outfitFont;
	}

	private void applyOutfitFontsWithoutBold() {
		Typeface tf = getOutfitTypeface();
		View contentView = getWindow().getDecorView().findViewById(android.R.id.content);
		if (contentView == null) return;

		Queue<View> queue = new LinkedList<>();
		queue.add(contentView);
		while (!queue.isEmpty()) {
			View v = queue.poll();
			if (v instanceof TextView) {
				TextView tv = (TextView) v;
				tv.setTypeface(tf, Typeface.NORMAL);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					tv.setLetterSpacing(-0.01f);
				}
			} else if (v instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) v;
				for (int i = 0; i < vg.getChildCount(); i++) queue.add(vg.getChildAt(i));
			}
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (navbarExpanded != null && navbarExpanded.getVisibility() == View.VISIBLE && liquidDragPill != null) {
			int[] loc = new int[2];
			navbarExpanded.getLocationOnScreen(loc);
			float rawX = ev.getRawX();
			float rawY = ev.getRawY();
			boolean insideNav = rawX >= loc[0] && rawX <= (loc[0] + navbarExpanded.getWidth())
				&& rawY >= loc[1] && rawY <= (loc[1] + navbarExpanded.getHeight());

			float d = getResources().getDisplayMetrics().density;
			int trackW = navbarExpanded.getWidth();
			float minTx = 0f;
			float maxTx = Math.max(0f, trackW - liquidDragPill.getWidth() - (10 * d));

			switch (ev.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					if (insideNav) {
						navDownRawX = rawX;
						navInitialPillTx = liquidDragPill.getTranslationX();
						isDraggingNavbarPill = false;
						liquidDragPill.animate().scaleX(1.06f).scaleY(1.04f).setDuration(100).start();
						return true;
					}
					break;

				case MotionEvent.ACTION_MOVE:
					if (navDownRawX > 0f) {
						float dx = rawX - navDownRawX;
						if (Math.abs(dx) > (6 * d)) isDraggingNavbarPill = true;
						if (isDraggingNavbarPill) {
							liquidDragPill.setTranslationX(Math.max(minTx, Math.min(maxTx, navInitialPillTx + dx)));
							return true;
						}
					}
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					if (navDownRawX > 0f) {
						liquidDragPill.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
						float slotW = Math.max(1f, (trackW - (10 * d)) / 3f);
						int targetIdx = isDraggingNavbarPill
							? Math.max(0, Math.min(2, Math.round(liquidDragPill.getTranslationX() / slotW)))
							: Math.max(0, Math.min(2, (int) ((rawX - loc[0]) / Math.max(1f, trackW / 3f))));
						navDownRawX = 0f;
						isDraggingNavbarPill = false;
						selectNavTabByIndex(targetIdx, true);
						return true;
					}
					break;
			}
		}
		return super.dispatchTouchEvent(ev);
	}

	private void applyCircularAvatar(ImageView target, String photoUrl) {
		if (target == null) return;
		float d = getResources().getDisplayMetrics().density;
		int sizePx = Math.max((int) (46 * d), target.getWidth() > 0 ? target.getWidth() : (int) (46 * d));

		if (photoUrl != null && !photoUrl.trim().isEmpty()) {
			String cleanUrl = photoUrl.trim();
			target.setPadding(0, 0, 0, 0);
			target.clearColorFilter();
			target.setBackground(null);
			target.setScaleType(ImageView.ScaleType.CENTER_CROP);

			if (cleanUrl.startsWith("data:image")) {
				try {
					String base64Part = cleanUrl.substring(cleanUrl.indexOf(",") + 1);
					byte[] decoded = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT);
					Bitmap rawBmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
					if (rawBmp != null) {
						int edge = Math.min(rawBmp.getWidth(), rawBmp.getHeight());
						Bitmap square = Bitmap.createBitmap(rawBmp, (rawBmp.getWidth() - edge) / 2, (rawBmp.getHeight() - edge) / 2, edge, edge);
						Bitmap scaled = Bitmap.createScaledBitmap(square, sizePx, sizePx, true);
						Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
						Canvas canvas = new Canvas(out);
						Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
						Rect r = new Rect(0, 0, sizePx, sizePx);
						canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p);
						p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
						canvas.drawBitmap(scaled, r, r, p);
						target.setImageBitmap(out);
						return;
					}
				} catch (Exception ignored) {}
			} else if (!isFinishing() && !isDestroyed()) {
				Glide.with(this).load(Uri.parse(cleanUrl)).circleCrop()
					.placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(target);
				return;
			}
		}

		int pad = (int) (3 * d);
		target.setBackground(null);
		target.setScaleType(ImageView.ScaleType.FIT_CENTER);
		target.setPadding(pad, pad, pad, pad);
		target.setImageResource(R.drawable.ic_profile);
		target.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
	}

	private void loadLoggedInUserProfile(FirebaseUser user) {
		if (user == null || user.getEmail() == null) return;
		String email = user.getEmail().trim().toLowerCase();
		final String fallbackName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;

		TextView tvUserHeader = findViewById(R.id.text_username);
		TextView tvProfileName = findViewById(R.id.textview65);
		if (tvUserHeader != null) tvUserHeader.setText(fallbackName);
		if (tvProfileName != null) tvProfileName.setText(fallbackName);

		applyCircularAvatar(imgUserAvatar, user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
		currentUserKey = email.replace(".", ",");

		FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("users").child(currentUserKey)
			.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snap) {
					if (isFinishing() || isDestroyed()) return;
					if (!snap.exists()) {
						applyCircularAvatar(imgUserAvatar, "");
						return;
					}
					Object nameObj = snap.child("name").getValue();
					String resolvedName = (nameObj != null && !nameObj.toString().trim().isEmpty())
						? nameObj.toString().trim() : fallbackName;
					if (tvUserHeader != null) tvUserHeader.setText(resolvedName);
					if (tvProfileName != null) tvProfileName.setText(resolvedName);

					Object photoObj = snap.child("photoURL").getValue();
					applyCircularAvatar(imgUserAvatar, photoObj != null ? photoObj.toString().trim() : "");
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					if (!isFinishing() && !isDestroyed()) applyCircularAvatar(imgUserAvatar, "");
				}
			});
	}

	private void setupPullToRefresh() {
		final NestedScrollView mainScroll = findViewById(R.id.main_scroll_view);
		if (mainScroll == null) return;

		float d = getResources().getDisplayMetrics().density;
		if (pullRefreshIndicator != null) {
			GradientDrawable pillBg = new GradientDrawable();
			pillBg.setShape(GradientDrawable.OVAL);
			pillBg.setColor(Color.parseColor("#181C26"));
			pullRefreshIndicator.setBackground(pillBg);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) pullRefreshIndicator.setElevation(18f);
		}
		if (pullRefreshSpinner != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			pullRefreshSpinner.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
		}
		if (imgPullRefreshIcon != null) imgPullRefreshIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

		final float thresholdPx = 90 * d;
		mainScroll.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (isRefreshingCatalog) return false;
				switch (event.getActionMasked()) {
					case MotionEvent.ACTION_DOWN:
						pullStartY = event.getRawY();
						isPullingFromTop = (mainScroll.getScrollY() <= 0);
						break;
					case MotionEvent.ACTION_MOVE:
						if (isPullingFromTop && mainScroll.getScrollY() <= 0) {
							float deltaY = event.getRawY() - pullStartY;
							if (deltaY > (16 * d) && pullRefreshIndicator != null) {
								pullRefreshIndicator.setVisibility(View.VISIBLE);
								if (imgPullRefreshIcon != null) {
									imgPullRefreshIcon.setVisibility(View.VISIBLE);
									imgPullRefreshIcon.setRotation(Math.min(180f, deltaY * 1.4f));
								}
								if (pullRefreshSpinner != null) pullRefreshSpinner.setVisibility(View.GONE);
							}
						}
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						if (isPullingFromTop && mainScroll.getScrollY() <= 0) {
							if (event.getRawY() - pullStartY >= thresholdPx) triggerCatalogRefresh();
							else if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.GONE);
						}
						isPullingFromTop = false;
						break;
				}
				return false;
			}
		});
	}

	private void triggerCatalogRefresh() {
		if (isRefreshingCatalog) return;
		isRefreshingCatalog = true;
		if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.VISIBLE);
		if (imgPullRefreshIcon != null) imgPullRefreshIcon.setVisibility(View.GONE);
		if (pullRefreshSpinner != null) pullRefreshSpinner.setVisibility(View.VISIBLE);
		if (dongflix_req != null && _dongflix_req_request_listener != null) {
			dongflix_req.startRequestNetwork("GET", CATALOG_URL, "", _dongflix_req_request_listener);
		}
	}

	private void hideRefreshIndicator() {
		isRefreshingCatalog = false;
		if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.GONE);
		if (pullRefreshSpinner != null) pullRefreshSpinner.setVisibility(View.GONE);
		if (imgPullRefreshIcon != null) imgPullRefreshIcon.setVisibility(View.VISIBLE);
	}

	private void startHeroAutoSlider() {
		stopHeroAutoSlider();
		if (listMap == null || listMap.size() < 3) return;

		float d = getResources().getDisplayMetrics().density;
		final int activeW = (int) (16 * d);
		final int inactiveW = (int) (5 * d);
		final int dotH = (int) (5 * d);
		final int dotGap = (int) (5 * d);

		final GradientDrawable activeDot = new GradientDrawable();
		activeDot.setShape(GradientDrawable.RECTANGLE);
		activeDot.setColor(Color.WHITE);
		activeDot.setCornerRadius(100f);

		final GradientDrawable inactiveDot = new GradientDrawable();
		inactiveDot.setShape(GradientDrawable.RECTANGLE);
		inactiveDot.setColor(Color.parseColor("#80FFFFFF"));
		inactiveDot.setCornerRadius(100f);

		heroCurrentIndex = 0;
		heroSliderRunnable = new Runnable() {
			@Override
			public void run() {
				if (isFinishing() || isDestroyed() || listMap == null || listMap.size() < 3) return;
				int pos = heroCurrentIndex % 3;
				HashMap<String, Object> item = listMap.get(pos);
				currentHeroItem = item;

				String heroCover = normalizeCoverUrl(safeStr(item, "cover", ""));
				final String heroSlug = safeStr(item, "slug", "");

				if (textHeroTitle != null) textHeroTitle.setText(safeStr(item, "title", "").trim());
				if (textHeroDesc != null) textHeroDesc.setText(cleanText(safeStr(item, "synopsis", "")));
				updateHeroFavoriteIcon(heroSlug);

				if (imgHeroBanner != null && !heroCover.isEmpty()) {
					Glide.with(MainActivity.this).load(Uri.parse(heroCover)).centerCrop()
						.placeholder(R.drawable.default_image).error(R.drawable.default_image).into(imgHeroBanner);
				}
				if (btnHeroPlay != null) {
					btnHeroPlay.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) { openWatchBySlug(heroSlug); }
					});
				}

				View[] dots = {dot0, dot1, dot2};
				for (int i = 0; i < dots.length; i++) {
					if (dots[i] != null) {
						boolean isActive = (i == pos);
						if (dots[i].getLayoutParams() instanceof LinearLayout.LayoutParams) {
							LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dots[i].getLayoutParams();
							lp.width = isActive ? activeW : inactiveW;
							lp.height = dotH;
							if (i > 0) lp.leftMargin = dotGap;
							dots[i].setLayoutParams(lp);
						}
						dots[i].setBackground(isActive ? activeDot : inactiveDot);
					}
				}

				heroCurrentIndex = (pos + 1) % 3;
				heroSliderHandler.postDelayed(this, 4000);
			}
		};
		heroSliderHandler.post(heroSliderRunnable);
	}

	private void stopHeroAutoSlider() {
		if (heroSliderRunnable != null) {
			heroSliderHandler.removeCallbacks(heroSliderRunnable);
			heroSliderRunnable = null;
		}
		heroSliderHandler.removeCallbacksAndMessages(null);
	}

	private void bindSectionLikePopularToday(RecyclerView rv, ArrayList<HashMap<String, Object>> items) {
		if (rv == null) return;
		float d = getResources().getDisplayMetrics().density;
		rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		rv.setNestedScrollingEnabled(false);
		rv.setOnFlingListener(null);
		rv.setClipToPadding(false);
		rv.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
		rv.setAdapter(new HorizontalPosterAdapter(items, 114, 162, 16f, 10, 12.5f));
	}

	private void bindSlimRomanceSection(RecyclerView rv, ArrayList<HashMap<String, Object>> items) {
		if (rv == null) return;
		float d = getResources().getDisplayMetrics().density;
		rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		rv.setNestedScrollingEnabled(false);
		rv.setOnFlingListener(null);
		rv.setClipToPadding(false);
		rv.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
		rv.setAdapter(new HorizontalPosterAdapter(items, 102, 144, 14f, 8, 11.5f));
	}

	private ArrayList<HashMap<String, Object>> buildEmptyDummyList(int count) {
		ArrayList<HashMap<String, Object>> list = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			HashMap<String, Object> m = new HashMap<>();
			m.put("is_loading", true);
			list.add(m);
		}
		return list;
	}

	private void populateCategorizedSections() {
		if (fullCatalogList == null || fullCatalogList.isEmpty()) return;

		ArrayList<HashMap<String, Object>> cultivation = new ArrayList<>();
		ArrayList<HashMap<String, Object>> completed = new ArrayList<>();
		ArrayList<HashMap<String, Object>> comedy = new ArrayList<>();
		ArrayList<HashMap<String, Object>> mystery = new ArrayList<>();
		ArrayList<HashMap<String, Object>> movie = new ArrayList<>();
		ArrayList<HashMap<String, Object>> romance = new ArrayList<>();

		for (HashMap<String, Object> item : fullCatalogList) {
			String status = safeStr(item, "status", "").toLowerCase(Locale.US);
			String genres = safeStr(item, "genres", "").toLowerCase(Locale.US);
			String meta = (genres + " " + safeStr(item, "title", "") + " "
				+ safeStr(item, "synopsis", "") + " " + safeStr(item, "type", "") + " " + safeStr(item, "episode", ""))
				.toLowerCase(Locale.US);

			if (meta.contains("kultivasi") || meta.contains("cultivation") || meta.contains("abadi") || meta.contains("immortal")) {
				cultivation.add(item);
			}
			if (status.contains("completed") || status.contains("tamat")) completed.add(item);
			if (meta.contains("comedy") || meta.contains("komedi") || meta.contains("slice of life")) comedy.add(item);
			if (meta.contains("mystery") || meta.contains("misteri") || meta.contains("teki")
				|| meta.contains("thriller") || meta.contains("suspense") || meta.contains("detective")) {
				mystery.add(item);
			}
			if (meta.contains("movie") || meta.contains("film")) movie.add(item);
			if (genres.contains("romance") || genres.contains("romantis") || genres.contains("romansa")
				|| meta.contains("romance") || meta.contains("romantis")) {
				romance.add(item);
			}
		}

		ArrayList<HashMap<String, Object>> fallback = new ArrayList<>(fullCatalogList.subList(0, Math.min(8, fullCatalogList.size())));
		if (cultivation.isEmpty()) cultivation.addAll(fallback);
		if (completed.isEmpty()) completed.addAll(fallback);
		if (comedy.isEmpty()) comedy.addAll(fallback);
		if (mystery.isEmpty()) mystery.addAll(fallback);
		if (movie.isEmpty()) movie.addAll(fallback);
		if (romance.isEmpty()) romance.addAll(fallback);

		bindSectionLikePopularToday(recyclerCultivationDonghua, new ArrayList<>(cultivation.subList(0, Math.min(10, cultivation.size()))));
		bindSectionLikePopularToday(recyclerCompletedDonghua, new ArrayList<>(completed.subList(0, Math.min(10, completed.size()))));
		bindSectionLikePopularToday(recyclerComedyDonghua, new ArrayList<>(comedy.subList(0, Math.min(10, comedy.size()))));
		bindSectionLikePopularToday(recyclerMysteryDonghua, new ArrayList<>(mystery.subList(0, Math.min(10, mystery.size()))));
		bindSectionLikePopularToday(recyclerMovieDonghua, new ArrayList<>(movie.subList(0, Math.min(10, movie.size()))));
		bindSlimRomanceSection(recyclerRomanceDonghua, new ArrayList<>(romance.subList(0, Math.min(10, romance.size()))));
	}

	private void setupContinueWatchingSection() {
		layoutSectionContinue = findViewById(R.id.layout_section_continue);
		textTitleContinue = findViewById(R.id.text_title_continue);
		textContinueSeeAll = findViewById(R.id.text_continue_see_all);
		recyclerContinue = findViewById(R.id.recyclerContinue);

		if (recyclerContinue != null) {
			float d = getResources().getDisplayMetrics().density;
			recyclerContinue.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
			recyclerContinue.setNestedScrollingEnabled(false);
			recyclerContinue.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (132 * d)));
		}
		if (textContinueSeeAll != null) {
			textContinueSeeAll.setText("Lihat riwayat");
			textContinueSeeAll.setTextColor(Color.parseColor("#94A3B8"));
			textContinueSeeAll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					activePustakaTab = 0;
					selectNavTabByIndex(1, true);
				}
			});
		}
	}

	private void setupPustakaSection() {
		View pageFavorite = findViewById(R.id.layout_favorite);
		if (!(pageFavorite instanceof ViewGroup)) return;

		ViewGroup favGroup = (ViewGroup) pageFavorite;
		favGroup.removeAllViews();
		favGroup.setBackgroundColor(Color.parseColor("#000000"));

		float d = getResources().getDisplayMetrics().density;
		int statusH = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusH = getResources().getDimensionPixelSize(resId);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		root.setPadding((int) (16 * d), statusH + (int) (16 * d), (int) (16 * d), (int) (110 * d));

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Pustaka");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
		root.addView(tvTitle);

		LinearLayout tabsBar = new LinearLayout(this);
		tabsBar.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		tabsLp.topMargin = (int) (18 * d);
		tabsBar.setLayoutParams(tabsLp);

		tabPustakaRiwayat = new TextView(this);
		tabPustakaDaftarku = new TextView(this);
		tabPustakaUnduhan = new TextView(this);
		linePustakaRiwayat = new View(this);
		linePustakaDaftarku = new View(this);
		linePustakaUnduhan = new View(this);

		tabsBar.addView(buildPustakaTabItem("Riwayat", tabPustakaRiwayat, linePustakaRiwayat, 0, d));
		tabsBar.addView(buildPustakaTabItem("Daftarku", tabPustakaDaftarku, linePustakaDaftarku, 1, d));
		tabsBar.addView(buildPustakaTabItem("Unduhan", tabPustakaUnduhan, linePustakaUnduhan, 2, d));
		root.addView(tabsBar);

		textPustakaSectionLabel = new TextView(this);
		textPustakaSectionLabel.setText("Kemarin");
		textPustakaSectionLabel.setTextColor(Color.WHITE);
		textPustakaSectionLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
		LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		secLp.topMargin = (int) (22 * d);
		secLp.bottomMargin = (int) (14 * d);
		textPustakaSectionLabel.setLayoutParams(secLp);
		root.addView(textPustakaSectionLabel);

		recyclerPustakaList = new RecyclerView(this);
		recyclerPustakaList.setLayoutManager(new LinearLayoutManager(this));
		recyclerPustakaList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		root.addView(recyclerPustakaList);

		favGroup.addView(root);
		updatePustakaTabVisual(activePustakaTab);
	}

	private LinearLayout buildPustakaTabItem(String label, TextView tv, View line, final int tabIndex, float d) {
		LinearLayout wrapper = new LinearLayout(this);
		wrapper.setOrientation(LinearLayout.VERTICAL);
		wrapper.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		wrapper.setPadding(0, 0, (int) (12 * d), 0);
		wrapper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activePustakaTab = tabIndex;
				updatePustakaTabVisual(tabIndex);
				renderActivePustakaTab();
			}
		});

		tv.setText(label);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
		tv.setPadding(0, 0, 0, (int) (8 * d));
		line.setLayoutParams(new LinearLayout.LayoutParams((int) (24 * d), (int) (3 * d)));
		wrapper.addView(tv);
		wrapper.addView(line);
		return wrapper;
	}

	private void updatePustakaTabVisual(int idx) {
		int active = Color.WHITE;
		int inactive = Color.parseColor("#94A3B8");
		int indicator = Color.parseColor("#38BDF8");

		if (tabPustakaRiwayat != null) tabPustakaRiwayat.setTextColor(idx == 0 ? active : inactive);
		if (tabPustakaDaftarku != null) tabPustakaDaftarku.setTextColor(idx == 1 ? active : inactive);
		if (tabPustakaUnduhan != null) tabPustakaUnduhan.setTextColor(idx == 2 ? active : inactive);

		if (linePustakaRiwayat != null) linePustakaRiwayat.setBackgroundColor(idx == 0 ? indicator : Color.TRANSPARENT);
		if (linePustakaDaftarku != null) linePustakaDaftarku.setBackgroundColor(idx == 1 ? indicator : Color.TRANSPARENT);
		if (linePustakaUnduhan != null) linePustakaUnduhan.setBackgroundColor(idx == 2 ? indicator : Color.TRANSPARENT);
	}

	private void loadUserHistoryAndFavoritesFromFirebase(FirebaseUser user) {
		if (user == null || user.getEmail() == null) return;
		currentUserKey = user.getEmail().trim().toLowerCase().replace(".", ",");
		DatabaseReference userNode = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("users").child(currentUserKey);

		userNode.child("history").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed()) return;
				ArrayList<HashMap<String, Object>> parsed = new ArrayList<>();
				if (snap.exists()) {
					for (DataSnapshot child : snap.getChildren()) {
						HashMap<String, Object> item = extractMapFromSnapshot(child);
						if (!item.isEmpty()) {
							if (!item.containsKey("slug") && child.getKey() != null) item.put("slug", child.getKey());
							parsed.add(item);
						}
					}
					Collections.sort(parsed, new Comparator<HashMap<String, Object>>() {
						@Override
						public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
							long tsA = parseLongSafe(a.get("updatedAt") != null ? a.get("updatedAt") : a.get("timestamp"));
							long tsB = parseLongSafe(b.get("updatedAt") != null ? b.get("updatedAt") : b.get("timestamp"));
							return Long.compare(tsB, tsA);
						}
					});
				}
				historyList = parsed;
				renderContinueWatchingCards();
				if (activePustakaTab == 0) renderActivePustakaTab();
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		});

		userNode.child("favorites").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed()) return;
				ArrayList<HashMap<String, Object>> parsedFavs = new ArrayList<>();
				if (snap.exists()) {
					for (DataSnapshot child : snap.getChildren()) {
						HashMap<String, Object> item = extractMapFromSnapshot(child);
						if (!item.isEmpty()) {
							if (!item.containsKey("slug") && child.getKey() != null) item.put("slug", child.getKey());
							parsedFavs.add(item);
						}
					}
				}
				favoritesList = parsedFavs;
				if (activePustakaTab == 1) renderActivePustakaTab();
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		});
	}

	private HashMap<String, Object> extractMapFromSnapshot(DataSnapshot snap) {
		HashMap<String, Object> map = new HashMap<>();
		if (snap == null || !snap.exists()) return map;
		Object val = snap.getValue();
		if (val instanceof Map) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) val).entrySet()) {
				if (entry.getKey() != null) map.put(entry.getKey().toString(), entry.getValue());
			}
		} else if (val instanceof String) {
			String s = val.toString().trim();
			if (s.startsWith("{")) {
				try {
					HashMap<String, Object> jsonMap = new Gson().fromJson(s, new TypeToken<HashMap<String, Object>>(){}.getType());
					if (jsonMap != null) map.putAll(jsonMap);
				} catch (Exception ignored) {}
			} else if (!s.isEmpty()) {
				map.put("title", s);
				map.put("slug", snap.getKey() != null ? snap.getKey() : s);
			}
		}
		return map;
	}

	private long parseLongSafe(Object o) {
		if (o == null) return 0L;
		if (o instanceof Number) return ((Number) o).longValue();
		try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return 0L; }
	}

	private void renderContinueWatchingCards() {
		if (layoutSectionContinue == null || recyclerContinue == null) return;
		if (historyList == null || historyList.isEmpty()) {
			layoutSectionContinue.setVisibility(View.GONE);
			return;
		}
		layoutSectionContinue.setVisibility(View.VISIBLE);
		recyclerContinue.setAdapter(new ContinueWatchingAdapter(historyList));
	}

	private void loadLocalDownloads() {
		ArrayList<HashMap<String, Object>> found = new ArrayList<>();
		File[] dirs = {
			getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
			getExternalFilesDir(Environment.DIRECTORY_MOVIES),
			getFilesDir()
		};
		for (File dir : dirs) {
			if (dir != null && dir.exists() && dir.isDirectory()) collectDownloadFiles(dir, found);
		}
		downloadsList = found;
		if (activePustakaTab == 2) renderActivePustakaTab();
	}

	private void collectDownloadFiles(File dir, ArrayList<HashMap<String, Object>> out) {
		File[] children = dir.listFiles();
		if (children == null) return;
		for (File f : children) {
			if (f.isDirectory()) {
				collectDownloadFiles(f, out);
			} else {
				String lower = f.getName().toLowerCase(Locale.US);
				if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".m3u8") || lower.endsWith(".webm")) {
					HashMap<String, Object> item = new HashMap<>();
					item.put("title", f.getName().replaceAll("\\.[^.]+$", "").replace("_", " "));
					item.put("episode", "Offline");
					item.put("file_path", f.getAbsolutePath());
					item.put("size_mb", String.format(Locale.US, "%.1f MB", f.length() / (1024f * 1024f)));
					out.add(item);
				}
			}
		}
	}

	private void renderActivePustakaTab() {
		if (recyclerPustakaList == null) return;
		if (activePustakaTab == 0) {
			if (textPustakaSectionLabel != null) textPustakaSectionLabel.setText("Kemarin");
			recyclerPustakaList.setAdapter(new PustakaListAdapter(historyList, 0));
		} else if (activePustakaTab == 1) {
			if (textPustakaSectionLabel != null) textPustakaSectionLabel.setText("Daftarku");
			recyclerPustakaList.setAdapter(new PustakaListAdapter(getMergedFavoritesList(), 1));
		} else {
			loadLocalDownloads();
			if (textPustakaSectionLabel != null) textPustakaSectionLabel.setText("Unduhan Tersimpan");
			recyclerPustakaList.setAdapter(new PustakaListAdapter(downloadsList, 2));
		}
	}

	private ArrayList<HashMap<String, Object>> getMergedFavoritesList() {
		LinkedHashMap<String, HashMap<String, Object>> dedup = new LinkedHashMap<>();
		for (HashMap<String, Object> item : favoritesList) {
			String s = safeStr(item, "slug", "");
			if (!s.isEmpty()) dedup.put(s, item);
		}
		if (favPrefs != null) {
			for (Map.Entry<String, ?> e : favPrefs.getAll().entrySet()) {
				if (e.getKey().startsWith("fav_") && e.getValue() instanceof String) {
					try {
						HashMap<String, Object> localMap = new Gson().fromJson(
							e.getValue().toString(), new TypeToken<HashMap<String, Object>>(){}.getType()
						);
						if (localMap != null) {
							String s = safeStr(localMap, "slug", e.getKey().substring(4));
							if (!dedup.containsKey(s)) dedup.put(s, localMap);
						}
					} catch (Exception ignored) {}
				}
			}
		}
		return new ArrayList<>(dedup.values());
	}

	private ArrayList<HashMap<String, Object>> sortCatalogByViews(
		List<HashMap<String, Object>> src, final Map<String, Long> primary, final Map<String, Long> secondary
	) {
		ArrayList<HashMap<String, Object>> sorted = new ArrayList<>(src);
		Collections.sort(sorted, new Comparator<HashMap<String, Object>>() {
			@Override
			public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
				String sA = safeStr(a, "slug", "");
				String sB = safeStr(b, "slug", "");
				long pA = primary.containsKey(sA) ? primary.get(sA) : 0L;
				long pB = primary.containsKey(sB) ? primary.get(sB) : 0L;
				if (pB != pA) return Long.compare(pB, pA);
				if (secondary == null) return 0;
				long secA = secondary.containsKey(sA) ? secondary.get(sA) : 0L;
				long secB = secondary.containsKey(sB) ? secondary.get(sB) : 0L;
				return Long.compare(secB, secA);
			}
		});
		return sorted;
	}

	private void sortAndRenderPopularSections() {
		if (fullCatalogList == null || fullCatalogList.isEmpty()) return;

		ArrayList<HashMap<String, Object>> todaySorted = sortCatalogByViews(fullCatalogList, viewsDailyMap, viewsTotalMap);
		ArrayList<HashMap<String, Object>> dongflixSorted = sortCatalogByViews(fullCatalogList, viewsTotalMap, viewsDailyMap);

		ArrayList<HashMap<String, Object>> todaySlice = new ArrayList<>(todaySorted.subList(0, Math.min(8, todaySorted.size())));
		ArrayList<HashMap<String, Object>> dongflixPopularSlice = new ArrayList<>(dongflixSorted.subList(0, Math.min(10, dongflixSorted.size())));

		bindSectionLikePopularToday(recyclerPopular, todaySlice);
		bindSectionLikePopularToday(recyclerPopularDongflix, dongflixPopularSlice);

		if (recyclerSearchList != null && (inputSearch == null || inputSearch.getText().toString().trim().isEmpty())) {
			recyclerSearchList.setAdapter(new ListPopularAdapter(dongflixPopularSlice));
		}
		populateCategorizedSections();
	}

	private void attachFirebasePopularityListeners() {
		detachFirebasePopularityListeners();

		Calendar cal = Calendar.getInstance();
		String todayStr = String.format(Locale.US, "%04d-%02d-%02d",
			cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

		DatabaseReference rootDb = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference();
		viewsTotalRef = rootDb.child("views_total");
		viewsDailyTodayRef = rootDb.child("views_daily").child(todayStr);

		viewsTotalListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed()) return;
				viewsTotalMap.clear();
				if (snap.exists()) {
					for (DataSnapshot child : snap.getChildren()) {
						if (child.getKey() != null) {
							viewsTotalMap.put(child.getKey(), parseLongSafe(child.getValue()));
						}
					}
				}
				sortAndRenderPopularSections();
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		};

		viewsDailyListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed()) return;
				viewsDailyMap.clear();
				if (snap.exists()) {
					for (DataSnapshot child : snap.getChildren()) {
						if (child.getKey() != null) {
							viewsDailyMap.put(child.getKey(), parseLongSafe(child.getValue()));
						}
					}
				}
				sortAndRenderPopularSections();
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		};

		viewsTotalRef.addValueEventListener(viewsTotalListener);
		viewsDailyTodayRef.addValueEventListener(viewsDailyListener);
	}

	private void detachFirebasePopularityListeners() {
		if (viewsTotalRef != null && viewsTotalListener != null) {
			viewsTotalRef.removeEventListener(viewsTotalListener);
		}
		if (viewsDailyTodayRef != null && viewsDailyListener != null) {
			viewsDailyTodayRef.removeEventListener(viewsDailyListener);
		}
	}

	private boolean isFavoriteSlug(String slug) {
		return favPrefs != null && slug != null && !slug.isEmpty() && favPrefs.contains("fav_" + slug);
	}

	private void toggleFavoriteItem(HashMap<String, Object> item) {
		if (favPrefs == null || item == null) return;
		String slug = safeStr(item, "slug", "");
		String title = safeStr(item, "title", "Donghua");
		if (slug.isEmpty()) return;

		DatabaseReference favNode = !currentUserKey.isEmpty()
			? FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("users").child(currentUserKey).child("favorites").child(slug)
			: null;

		if (isFavoriteSlug(slug)) {
			favPrefs.edit().remove("fav_" + slug).apply();
			if (favNode != null) favNode.removeValue();
			Toast.makeText(this, "Dihapus dari Daftarku", Toast.LENGTH_SHORT).show();
		} else {
			favPrefs.edit().putString("fav_" + slug, new Gson().toJson(item)).apply();
			if (favNode != null) favNode.setValue(item);
			Toast.makeText(this, "Disimpan ke Daftarku: " + title, Toast.LENGTH_SHORT).show();
		}
		updateHeroFavoriteIcon(slug);
		if (activePustakaTab == 1) renderActivePustakaTab();
	}

	private void updateHeroFavoriteIcon(String slug) {
		if (imgHeroFavorite == null) return;
		if (isFavoriteSlug(slug)) {
			imgHeroFavorite.setImageResource(R.drawable.ic_bookmark_filled);
			imgHeroFavorite.setColorFilter(Color.parseColor("#3B82F6"));
		} else {
			imgHeroFavorite.setImageResource(R.drawable.ic_bookmark);
			imgHeroFavorite.setColorFilter(Color.WHITE);
		}
	}

	private void openWatchBySlug(String slug) {
		if (slug == null || slug.trim().isEmpty()) return;
		hideKeyboard();
		Intent intent = new Intent(this, WatchActivity.class);
		intent.putExtra("slug", slug.trim());
		startActivity(intent);
		overridePendingTransition(R.anim.slide_up_enter, R.anim.no_anim);
	}

	private void openNotificationsSheet() {
		String latestTitle = currentHeroItem != null ? safeStr(currentHeroItem, "title", "Episode terbaru Dongflix telah tersedia.") : "Episode terbaru Dongflix telah tersedia.";
		new AlertDialog.Builder(this)
			.setTitle("Notifikasi")
			.setMessage("• Update katalog harian aktif\n• " + latestTitle)
			.setPositiveButton("Tutup", null)
			.show();
	}

	private void showKeyboardOnSearch() {
		if (inputSearch == null) return;
		inputSearch.requestFocus();
		inputSearch.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null) imm.showSoftInput(inputSearch, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 120);
	}

	private void hideKeyboard() {
		if (inputSearch == null) return;
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
	}

	private Drawable createMainShimmerDrawable(final float cornerRadiusPx) {
		return new Drawable() {
			private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Matrix shaderMatrix = new Matrix();
			private LinearGradient gradient;
			private int lastWidth = -1;

			{ basePaint.setColor(Color.parseColor("#12141A")); }

			@Override
			public void draw(Canvas canvas) {
				Rect b = getBounds();
				int w = b.width(), h = b.height();
				if (w <= 0 || h <= 0) return;
				canvas.drawRoundRect(new RectF(b), cornerRadiusPx, cornerRadiusPx, basePaint);

				if (gradient == null || lastWidth != w) {
					lastWidth = w;
					gradient = new LinearGradient(0, 0, w, 0,
						new int[]{Color.TRANSPARENT, Color.parseColor("#1AFFFFFF"), Color.parseColor("#33FFFFFF"), Color.parseColor("#1AFFFFFF"), Color.TRANSPARENT},
						new float[]{0f, 0.35f, 0.5f, 0.65f, 1f}, Shader.TileMode.CLAMP);
					shimmerPaint.setShader(gradient);
				}
				shaderMatrix.setTranslate(-w + (2f * w * mainShimmerPhase), 0);
				gradient.setLocalMatrix(shaderMatrix);
				canvas.drawRoundRect(new RectF(b), cornerRadiusPx, cornerRadiusPx, shimmerPaint);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private void startMainShimmer() {
		isMainLoading = true;
		mainShimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
		mainShimmerAnimator.setDuration(1250);
		mainShimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
		mainShimmerAnimator.setInterpolator(new LinearInterpolator());
		mainShimmerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				if (!isMainLoading) return;
				mainShimmerPhase = (float) animation.getAnimatedValue();
				View[] singles = {imgHeroBanner, textHeroTitle, textHeroDesc, btnHeroPlay, btnHeroFavorite};
				for (View v : singles) if (v != null) v.invalidate();

				RecyclerView[] hRecyclers = {
					recyclerPopular, recyclerPopularDongflix, recyclerRomanceDonghua,
					recyclerCultivationDonghua, recyclerCompletedDonghua,
					recyclerComedyDonghua, recyclerMysteryDonghua, recyclerMovieDonghua
				};
				for (RecyclerView rv : hRecyclers) {
					if (rv != null) for (int i = 0; i < rv.getChildCount(); i++) invalidateRecursive(rv.getChildAt(i));
				}
				if (gridview1 != null) {
					for (int i = 0; i < gridview1.getChildCount(); i++) invalidateRecursive(gridview1.getChildAt(i));
				}
			}
		});
		mainShimmerAnimator.start();
	}

	private void invalidateRecursive(View v) {
		if (v == null) return;
		v.invalidate();
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) invalidateRecursive(vg.getChildAt(i));
		}
	}

	private void stopMainShimmer() {
		isMainLoading = false;
		if (mainShimmerAnimator != null) {
			mainShimmerAnimator.cancel();
			mainShimmerAnimator = null;
		}
	}

	@Override
	protected void onDestroy() {
		stopMainShimmer();
		stopHeroAutoSlider();
		detachFirebasePopularityListeners();
		super.onDestroy();
	}

	private void initialize() {
		dongflix_req = new RequestNetwork(this);

		pullRefreshIndicator = findViewById(R.id.pull_refresh_indicator);
		pullRefreshSpinner = findViewById(R.id.pull_refresh_spinner);
		imgPullRefreshIcon = findViewById(R.id.img_pull_refresh_icon);

		gridview1 = findViewById(R.id.gridview1);
		recyclerPopular = findViewById(R.id.recyclerPopular);
		recyclerPopularDongflix = findViewById(R.id.recyclerPopularDongflix);
		recyclerRomanceDonghua = findViewById(R.id.recyclerRomanceDonghua);
		recyclerCultivationDonghua = findViewById(R.id.recyclerCultivationDonghua);
		recyclerCompletedDonghua = findViewById(R.id.recyclerCompletedDonghua);
		recyclerComedyDonghua = findViewById(R.id.recyclerComedyDonghua);
		recyclerMysteryDonghua = findViewById(R.id.recyclerMysteryDonghua);
		recyclerMovieDonghua = findViewById(R.id.recyclerMovieDonghua);

		recyclerSearchList = findViewById(R.id.recycler_search_list);
		searchBarContainer = findViewById(R.id.search_bar_container);
		inputSearch = findViewById(R.id.input_search);
		btnClearSearch = findViewById(R.id.btn_clear_search);
		textSearchSectionLabel = findViewById(R.id.text_search_section_label);

		textHeroTitle = findViewById(R.id.text_hero_title);
		textHeroDesc = findViewById(R.id.text_hero_desc);
		textSeeAll = findViewById(R.id.text_see_all);
		textCultivationSeeAll = findViewById(R.id.text_cultivation_see_all);
		textCompletedSeeAll = findViewById(R.id.text_completed_see_all);
		textComedySeeAll = findViewById(R.id.text_comedy_see_all);
		textMysterySeeAll = findViewById(R.id.text_mystery_see_all);
		textMovieSeeAll = findViewById(R.id.text_movie_see_all);

		imgHeroBanner = findViewById(R.id.img_hero_banner);
		imgUserAvatar = findViewById(R.id.img_user_avatar);
		btnHeroPlay = findViewById(R.id.btn_hero_play);
		btnHeroFavorite = findViewById(R.id.btn_hero_favorite);
		imgHeroFavorite = findViewById(R.id.img_hero_favorite);
		imgPlayIcon = findViewById(R.id.img_play_icon);
		indicatorCapsule = findViewById(R.id.indicator_capsule);
		dot0 = findViewById(R.id.dot_0);
		dot1 = findViewById(R.id.dot_1);
		dot2 = findViewById(R.id.dot_2);

		btnTopNotif = findViewById(R.id.btn_top_search);
		btnTopChat = findViewById(R.id.btn_top_chat);
		imgTopNotif = findViewById(R.id.img_top_search);
		imgTopChat = findViewById(R.id.img_top_chat);

		navbarExpanded = findViewById(R.id.navbar);
		btnCompactHome = findViewById(R.id.btn_compact_home);
		miniPlayerInlineSlot = findViewById(R.id.mini_player_inline_slot);
		btnCircleSearch = findViewById(R.id.btn_circle_search);
		tabBeranda = findViewById(R.id.tab_beranda);
		tabPustaka = findViewById(R.id.tab_pustaka);
		tabAkun = findViewById(R.id.tab_akun);
		imgHome = findViewById(R.id.img_home);
		imgFavorite = findViewById(R.id.img_favorite);
		imgProfile = findViewById(R.id.img_profile);
		imgSearch = findViewById(R.id.img_search);
		txtHome = findViewById(R.id.txt_home);
		txtFavorite = findViewById(R.id.txt_favorite);
		txtProfile = findViewById(R.id.txt_profile);

		TextView[] seeAllList = {
			textSeeAll, textCultivationSeeAll, textCompletedSeeAll,
			textComedySeeAll, textMysterySeeAll, textMovieSeeAll, textContinueSeeAll
		};
		View.OnClickListener openCatalog = new View.OnClickListener() {
			@Override
			public void onClick(View v) { startActivity(new Intent(MainActivity.this, CatalogActivity.class)); }
		};
		for (TextView tv : seeAllList) {
			if (tv != null) {
				tv.setTextColor(Color.parseColor("#94A3B8"));
				if (tv != textContinueSeeAll) tv.setOnClickListener(openCatalog);
			}
		}

		if (imgTopNotif != null) {
			int notifId = getResources().getIdentifier("ic_notifications", "drawable", getPackageName());
			imgTopNotif.setImageResource(notifId > 0 ? notifId : R.drawable.ic_search);
		}
		if (btnTopNotif != null) {
			btnTopNotif.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) { openNotificationsSheet(); }
			});
		}
		if (btnHeroFavorite != null) {
			btnHeroFavorite.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) { if (currentHeroItem != null) toggleFavoriteItem(currentHeroItem); }
			});
		}
		if (btnTopChat != null) {
			btnTopChat.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) { startActivity(new Intent(MainActivity.this, ChatActivity.class)); }
			});
		}
		if (gridview1 != null) {
			gridview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> p1, View p2, int pos, long p4) {
					if (listMap != null && pos < listMap.size()) {
						HashMap<String, Object> item = listMap.get(pos);
						if (item != null && !item.containsKey("is_loading")) openWatchBySlug(safeStr(item, "slug", ""));
					}
				}
			});
		}
		if (btnClearSearch != null) {
			btnClearSearch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (inputSearch != null) {
						inputSearch.setText("");
						showKeyboardOnSearch();
					}
				}
			});
		}
		if (inputSearch != null) {
			inputSearch.addTextChangedListener(new TextWatcher() {
				@Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
				@Override public void onTextChanged(CharSequence s, int st, int b, int c) {
					String q = s.toString().trim().toLowerCase();
					if (btnClearSearch != null) btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
					if (q.isEmpty()) {
						if (textSearchSectionLabel != null) {
							textSearchSectionLabel.setVisibility(View.VISIBLE);
							textSearchSectionLabel.setText("Populer di Dongflix");
						}
						if (recyclerSearchList != null) recyclerSearchList.setAdapter(new ListPopularAdapter(listMap));
					} else {
						if (textSearchSectionLabel != null) textSearchSectionLabel.setVisibility(View.GONE);
						ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
						for (HashMap<String, Object> item : fullCatalogList) {
							if (safeStr(item, "title", "").toLowerCase().contains(q)
								|| safeStr(item, "genres", "").toLowerCase().contains(q)) {
								filtered.add(item);
							}
						}
						if (recyclerSearchList != null) recyclerSearchList.setAdapter(new ListPopularAdapter(filtered));
					}
				}
				@Override public void afterTextChanged(Editable s) {}
			});
		}

		_dongflix_req_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String p1, String response, HashMap<String, Object> p3) {
				if (isFinishing() || isDestroyed()) return;
				stopMainShimmer();
				hideRefreshIndicator();
				DongflixPlayerManager.getInstance().cachedAllCatalogJson = response;

				try {
					fullCatalogList = new Gson().fromJson(response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
				} catch (Exception e) {
					fullCatalogList = new ArrayList<>();
				}
				if (fullCatalogList == null) fullCatalogList = new ArrayList<>();

				listMap = new ArrayList<>(fullCatalogList.subList(0, Math.min(12, fullCatalogList.size())));
				if (gridview1 != null) {
					gridview1.setAdapter(new Gridview1Adapter(listMap));
					setGridViewHeightBasedOnChildren(gridview1, 3);
				}
				if (recyclerSearchList != null) recyclerSearchList.setAdapter(new ListPopularAdapter(listMap));

				if (listMap.size() >= 3) {
					if (textHeroTitle != null) textHeroTitle.setBackgroundColor(Color.TRANSPARENT);
					if (textHeroDesc != null) textHeroDesc.setBackgroundColor(Color.TRANSPARENT);
					if (imgHeroBanner != null) imgHeroBanner.setBackgroundColor(Color.TRANSPARENT);

					GradientDrawable playBg = new GradientDrawable();
					playBg.setShape(GradientDrawable.RECTANGLE);
					playBg.setColor(Color.WHITE);
					playBg.setCornerRadius(100f);

					if (btnHeroPlay != null) {
						btnHeroPlay.setBackground(playBg);
						btnHeroPlay.setVisibility(View.VISIBLE);
						btnHeroPlay.setEnabled(true);
					}
					if (btnHeroFavorite != null) {
						btnHeroFavorite.setVisibility(View.VISIBLE);
						btnHeroFavorite.setEnabled(true);
						applyUniversalGlassStyle(btnHeroFavorite, btnHeroFavorite);
					}
					if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.VISIBLE);
					if (indicatorCapsule != null) indicatorCapsule.setVisibility(View.VISIBLE);
					startHeroAutoSlider();
				}

				bindSectionLikePopularToday(recyclerPopular, listMap);
				bindSectionLikePopularToday(recyclerPopularDongflix, listMap);
				populateCategorizedSections();
				sortAndRenderPopularSections();
				applyOutfitFontsWithoutBold();
			}

			@Override
			public void onErrorResponse(String p1, String p2) {
				if (isFinishing() || isDestroyed()) return;
				stopMainShimmer();
				hideRefreshIndicator();
			}
		};
	}

	private void initializeLogic() {
		_UI();
		setupIos26DraggableNavbar();
		_Nav();
		setupScrollResponsiveNavbar();
		applyOutfitFontsWithoutBold();

		float d = getResources().getDisplayMetrics().density;
		ArrayList<HashMap<String, Object>> dummy = buildEmptyDummyList(12);

		if (gridview1 != null) {
			gridview1.setAdapter(new Gridview1Adapter(dummy));
			gridview1.setOnTouchListener(new View.OnTouchListener() {
				@Override public boolean onTouch(View v, MotionEvent event) { return event.getAction() == MotionEvent.ACTION_MOVE; }
			});
			setGridViewHeightBasedOnChildren(gridview1, 3);
		}
		if (textHeroTitle != null) {
			textHeroTitle.setText("");
			textHeroTitle.setBackground(createMainShimmerDrawable(8 * d));
		}
		if (textHeroDesc != null) {
			textHeroDesc.setText("");
			textHeroDesc.setBackground(createMainShimmerDrawable(6 * d));
		}
		if (imgHeroBanner != null) {
			imgHeroBanner.setImageResource(android.R.color.transparent);
			imgHeroBanner.setBackground(createMainShimmerDrawable(0f));
		}
		if (btnHeroPlay != null) { btnHeroPlay.setVisibility(View.GONE); btnHeroPlay.setEnabled(false); }
		if (btnHeroFavorite != null) { btnHeroFavorite.setVisibility(View.GONE); btnHeroFavorite.setEnabled(false); }
		if (indicatorCapsule != null) indicatorCapsule.setVisibility(View.INVISIBLE);
		if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.GONE);

		RecyclerView[] standardList = {
			recyclerPopular, recyclerPopularDongflix, recyclerCultivationDonghua,
			recyclerCompletedDonghua, recyclerComedyDonghua, recyclerMysteryDonghua, recyclerMovieDonghua
		};
		for (RecyclerView rv : standardList) bindSectionLikePopularToday(rv, dummy);
		bindSlimRomanceSection(recyclerRomanceDonghua, dummy);

		if (recyclerSearchList != null) {
			recyclerSearchList.setLayoutManager(new LinearLayoutManager(this));
			recyclerSearchList.setAdapter(new ListPopularAdapter(dummy));
		}

		startMainShimmer();
		dongflix_req.startRequestNetwork("GET", CATALOG_URL, "", _dongflix_req_request_listener);
	}

	private void setupRealTimeBlurView(View blurTarget, ViewGroup root, float radius) {
		if (!(blurTarget instanceof BlurView) || root == null) return;
		BlurView bv = (BlurView) blurTarget;
		bv.setupWith(root, new RenderScriptBlur(this))
			.setFrameClearDrawable(getWindow().getDecorView().getBackground())
			.setBlurRadius(radius);
		bv.setOutlineProvider(new ViewOutlineProvider() {
			@Override public void getOutline(View view, Outline outline) { outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 1000f); }
		});
		bv.setClipToOutline(true);
	}

	private void applyUniversalGlassStyle(View blurTarget, View containerTarget) {
		float d = getResources().getDisplayMetrics().density;
		if (blurTarget != null && !(blurTarget instanceof BlurView)) {
			GradientDrawable glass = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{Color.parseColor("#B3161923"), Color.parseColor("#CC0A0C12")}
			);
			glass.setCornerRadius(100f);
			glass.setStroke((int) (1 * d), Color.parseColor("#33FFFFFF"));
			blurTarget.setBackground(glass);
		}
		if (containerTarget != null) {
			GradientDrawable border = new GradientDrawable();
			border.setColor(Color.TRANSPARENT);
			border.setCornerRadius(1000f);
			border.setStroke((int) (1 * d), Color.parseColor("#33FFFFFF"));
			containerTarget.setForeground(border);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) containerTarget.setElevation(18f);
		}
	}

	private void setupIos26DraggableNavbar() {
		if (navbarExpanded == null || tabBeranda == null || navbarExpanded.getChildCount() < 2) return;
		final ViewGroup tabsRow = (ViewGroup) navbarExpanded.getChildAt(1);
		final float d = getResources().getDisplayMetrics().density;

		if (liquidDragPill == null) {
			liquidDragPill = new View(this);
			GradientDrawable pillBg = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{Color.parseColor("#38FFFFFF"), Color.parseColor("#22FFFFFF")}
			);
			pillBg.setCornerRadius(100f);
			liquidDragPill.setBackground(pillBg);

			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) (96 * d), (int) (54 * d), Gravity.CENTER_VERTICAL | Gravity.START);
			lp.leftMargin = (int) (5 * d);
			liquidDragPill.setLayoutParams(lp);
			navbarExpanded.addView(liquidDragPill, 1);
		}
		tabsRow.post(new Runnable() {
			@Override public void run() { snapPillToTab(activeNavTabIndex, false); }
		});
	}

	private void snapPillToTab(int idx, boolean animate) {
		if (liquidDragPill == null || tabBeranda == null || tabPustaka == null || tabAkun == null) return;
		View target = (idx == 1) ? tabPustaka : (idx == 2 ? tabAkun : tabBeranda);
		float d = getResources().getDisplayMetrics().density;

		int targetW = target.getWidth();
		if (targetW <= 0 && navbarExpanded != null && navbarExpanded.getWidth() > 0) {
			targetW = (navbarExpanded.getWidth() - (int) (10 * d)) / 3;
		}
		if (targetW <= 0) targetW = (int) (96 * d);

		ViewGroup.LayoutParams lp = liquidDragPill.getLayoutParams();
		if (lp.width != targetW) {
			lp.width = targetW;
			liquidDragPill.setLayoutParams(lp);
		}

		float tx = target.getLeft() > 0 ? Math.max(0f, target.getLeft() - (5 * d)) : idx * targetW;
		if (animate) {
			liquidDragPill.animate().translationX(tx).setInterpolator(new OvershootInterpolator(1.05f)).setDuration(220).start();
		} else {
			liquidDragPill.setTranslationX(tx);
		}
	}

	private void selectNavTabByIndex(int idx, boolean animatePill) {
		activeNavTabIndex = idx;
		snapPillToTab(idx, animatePill);

		View[] pages = {
			findViewById(R.id.main_scroll_view),
			findViewById(R.id.layout_favorite),
			findViewById(R.id.layout_profile),
			findViewById(R.id.layout_search)
		};
		for (View p : pages) if (p != null) p.setVisibility(View.GONE);

		int active = Color.WHITE, inactive = Color.parseColor("#94A3B8");
		if (imgHome != null) imgHome.setColorFilter(idx == 0 ? active : inactive);
		if (imgFavorite != null) imgFavorite.setColorFilter(idx == 1 ? active : inactive);
		if (imgProfile != null) imgProfile.setColorFilter(idx == 2 ? active : inactive);
		if (imgSearch != null) imgSearch.setColorFilter(inactive);

		if (txtHome != null) txtHome.setTextColor(idx == 0 ? active : inactive);
		if (txtFavorite != null) txtFavorite.setTextColor(idx == 1 ? active : inactive);
		if (txtProfile != null) txtProfile.setTextColor(idx == 2 ? active : inactive);

		hideKeyboard();
		if (idx == 0) {
			if (pages[0] != null) pages[0].setVisibility(View.VISIBLE);
			setNavbarCompactState(false);
		} else if (idx == 1) {
			if (pages[1] != null) pages[1].setVisibility(View.VISIBLE);
			renderActivePustakaTab();
		} else if (pages[2] != null) {
			pages[2].setVisibility(View.VISIBLE);
		}
	}

	public void _UI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window w = getWindow();
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
			w.setStatusBarColor(Color.TRANSPARENT);
		}

		int statusH = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusH = getResources().getDimensionPixelSize(resId);
		float d = getResources().getDisplayMetrics().density;

		View topHeader = findViewById(R.id.relativelayout69);
		if (topHeader != null) topHeader.setPadding((int) (16 * d), statusH + (int) (8 * d), (int) (16 * d), (int) (10 * d));

		applyUniversalGlassStyle(btnTopNotif, btnTopNotif);
		applyUniversalGlassStyle(btnTopChat, btnTopChat);
		if (imgTopNotif != null) imgTopNotif.setColorFilter(Color.WHITE);
		if (imgTopChat != null) imgTopChat.setColorFilter(Color.WHITE);
		if (imgHeroFavorite != null) imgHeroFavorite.setColorFilter(Color.WHITE);

		if (searchBarContainer != null) {
			if (searchBarContainer.getLayoutParams() instanceof LinearLayout.LayoutParams) {
				LinearLayout.LayoutParams sbLp = (LinearLayout.LayoutParams) searchBarContainer.getLayoutParams();
				sbLp.topMargin = statusH + (int) (12 * d);
				searchBarContainer.setLayoutParams(sbLp);
			}
			GradientDrawable sbBg = new GradientDrawable();
			sbBg.setColor(Color.parseColor("#12141A"));
			sbBg.setCornerRadius(100f);
			sbBg.setStroke((int) (1 * d), Color.parseColor("#26FFFFFF"));
			searchBarContainer.setBackground(sbBg);
		}

		if (btnClearSearch != null) {
			GradientDrawable clearBg = new GradientDrawable();
			clearBg.setShape(GradientDrawable.OVAL);
			clearBg.setColor(Color.parseColor("#252936"));
			btnClearSearch.setBackground(clearBg);
		}

		View headerShadow = findViewById(R.id.header_shadow);
		if (headerShadow != null) {
			headerShadow.setBackground(new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#E6000000"), Color.TRANSPARENT}));
		}
		View heroShadow = findViewById(R.id.hero_shadow);
		if (heroShadow != null) {
			heroShadow.setBackground(new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.TRANSPARENT, Color.parseColor("#FF000000")}));
		}

		ViewGroup rootMain = findViewById(R.id.root_main_container);
		View navBlur = findViewById(R.id.navbar_blur_bg);
		View compactBlur = findViewById(R.id.compact_home_blur_bg);
		View searchBlur = findViewById(R.id.search_blur_bg);

		setupRealTimeBlurView(navBlur, rootMain, 20f);
		setupRealTimeBlurView(compactBlur, rootMain, 20f);
		setupRealTimeBlurView(searchBlur, rootMain, 20f);

		applyUniversalGlassStyle(navBlur, navbarExpanded);
		applyUniversalGlassStyle(compactBlur, btnCompactHome);
		applyUniversalGlassStyle(searchBlur, btnCircleSearch);
	}

	private void setupScrollResponsiveNavbar() {
		NestedScrollView mainScroll = findViewById(R.id.main_scroll_view);
		if (mainScroll != null) {
			mainScroll.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
				@Override
				public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldX, int oldY) {
					if (!DongflixPlayerManager.getInstance().isMiniPlayerActive) {
						resetNavbarToExpanded();
						return;
					}
					int dy = scrollY - oldY;
					if (scrollY <= 20 || dy < -12) setNavbarCompactState(false);
					else if (dy > 14 && scrollY > 80) setNavbarCompactState(true);
				}
			});
		}
	}

	public void resetNavbarToExpanded() { setNavbarCompactState(false); }

	private void setNavbarCompactState(boolean compact) {
		if (compact && !DongflixPlayerManager.getInstance().isMiniPlayerActive) compact = false;
		if (isNavbarCompact == compact) return;
		isNavbarCompact = compact;

		if (compact) {
			if (navbarExpanded != null) navbarExpanded.setVisibility(View.GONE);
			if (btnCompactHome != null) btnCompactHome.setVisibility(View.VISIBLE);
		} else {
			if (btnCompactHome != null) btnCompactHome.setVisibility(View.GONE);
			if (miniPlayerInlineSlot != null) miniPlayerInlineSlot.setVisibility(View.GONE);
			if (navbarExpanded != null) navbarExpanded.setVisibility(View.VISIBLE);
			snapPillToTab(activeNavTabIndex, false);
		}
		DongflixPlayerManager.getInstance().setCompactScrollMode(this, compact);
	}

	public void _Nav() {
		final View pageHome = findViewById(R.id.main_scroll_view);
		final View pageSearch = findViewById(R.id.layout_search);
		final View pageFavorite = findViewById(R.id.layout_favorite);
		final View pageProfile = findViewById(R.id.layout_profile);

		if (btnCompactHome != null) {
			btnCompactHome.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) { selectNavTabByIndex(0, true); }
			});
		}
		if (btnCircleSearch != null) {
			btnCircleSearch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (pageHome != null) pageHome.setVisibility(View.GONE);
					if (pageFavorite != null) pageFavorite.setVisibility(View.GONE);
					if (pageProfile != null) pageProfile.setVisibility(View.GONE);
					if (pageSearch != null) pageSearch.setVisibility(View.VISIBLE);
					if (imgSearch != null) imgSearch.setColorFilter(Color.WHITE);
					showKeyboardOnSearch();
				}
			});
		}
		selectNavTabByIndex(0, false);
	}

	public void setGridViewHeightBasedOnChildren(final GridView gv, final int columns) {
		gv.post(new Runnable() {
			@Override
			public void run() {
				if (gv.getAdapter() == null || gv.getAdapter().getCount() == 0) return;
				int items = gv.getAdapter().getCount();
				int rows = (int) Math.ceil((double) items / columns);
				float d = getResources().getDisplayMetrics().density;
				int hGap = (int) (12 * d), vGap = (int) (16 * d), padBot = (int) (16 * d);
				int colW = (gv.getWidth() - gv.getPaddingLeft() - gv.getPaddingRight() - (hGap * (columns - 1))) / columns;

				View itemV = gv.getAdapter().getView(0, null, gv);
				itemV.measure(View.MeasureSpec.makeMeasureSpec(colW, View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

				ViewGroup.LayoutParams lp = gv.getLayoutParams();
				lp.height = (itemV.getMeasuredHeight() * rows) + (vGap * Math.max(0, rows - 1)) + padBot;
				gv.setLayoutParams(lp);
				gv.requestLayout();
			}
		});
	}

	public class ContinueWatchingAdapter extends RecyclerView.Adapter<ContinueWatchingAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;
		public ContinueWatchingAdapter(ArrayList<HashMap<String, Object>> arr) { this.data = arr; }

		@NonNull @Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			float d = parent.getResources().getDisplayMetrics().density;
			FrameLayout card = new FrameLayout(parent.getContext());
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams((int) (245 * d), (int) (124 * d));
			lp.rightMargin = (int) (12 * d);
			card.setLayoutParams(lp);

			GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
				new int[]{Color.parseColor("#071830"), Color.parseColor("#0C2847")});
			bg.setCornerRadius(16 * d);
			card.setBackground(bg);
			card.setClipToOutline(true);

			ImageView thumb = new ImageView(parent.getContext());
			thumb.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
			thumb.setAlpha(0.7f);
			thumb.setTag("thumb");
			card.addView(thumb);

			View scrim = new View(parent.getContext());
			scrim.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
				new int[]{Color.parseColor("#E6030A14"), Color.parseColor("#66030A14"), Color.TRANSPARENT}));
			card.addView(scrim);

			LinearLayout col = new LinearLayout(parent.getContext());
			col.setOrientation(LinearLayout.VERTICAL);
			col.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (12 * d));
			col.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

			TextView tvTitle = new TextView(parent.getContext());
			tvTitle.setTextColor(Color.WHITE);
			tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
			tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvTitle.setMaxLines(2);
			tvTitle.setEllipsize(TextUtils.TruncateAt.END);
			tvTitle.setTag("title");

			TextView tvEp = new TextView(parent.getContext());
			tvEp.setTextColor(Color.parseColor("#CBD5E1"));
			tvEp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			tvEp.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvEp.setTag("ep");

			col.addView(tvTitle);
			col.addView(tvEp);
			card.addView(col);

			View bar = new View(parent.getContext());
			bar.setLayoutParams(new FrameLayout.LayoutParams((int) (145 * d), (int) (3 * d), Gravity.BOTTOM | Gravity.START));
			bar.setBackgroundColor(Color.parseColor("#38BDF8"));
			card.addView(bar);

			return new ViewHolder(card);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
			final HashMap<String, Object> item = data.get(pos);
			if (item == null) return;
			ImageView thumb = holder.itemView.findViewWithTag("thumb");
			TextView tvTitle = holder.itemView.findViewWithTag("title");
			TextView tvEp = holder.itemView.findViewWithTag("ep");

			String epRaw = safeStr(item, "episode", "1").trim();
			String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
			if (cov.isEmpty()) cov = normalizeCoverUrl(safeStr(item, "poster", ""));

			if (tvTitle != null) tvTitle.setText(safeStr(item, "title", "Donghua"));
			if (tvEp != null) tvEp.setText(epRaw.toUpperCase(Locale.US).startsWith("EP") ? epRaw.toUpperCase(Locale.US) : "EP " + epRaw);
			if (thumb != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
				Glide.with(MainActivity.this).load(Uri.parse(cov)).into(thumb);
			}
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) { openWatchBySlug(safeStr(item, "slug", "")); }
			});
		}

		@Override public int getItemCount() { return data.size(); }
		class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(@NonNull View v) { super(v); } }
	}

	public class HorizontalPosterAdapter extends RecyclerView.Adapter<HorizontalPosterAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;
		private final int cardWidthDp, cardHeightDp, rightMarginDp;
		private final float radiusDp, titleSp;

		public HorizontalPosterAdapter(ArrayList<HashMap<String, Object>> arr, int wDp, int hDp, float radDp, int gapDp, float tSp) {
			this.data = arr;
			this.cardWidthDp = wDp;
			this.cardHeightDp = hDp;
			this.radiusDp = radDp;
			this.rightMarginDp = gapDp;
			this.titleSp = tSp;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			float d = parent.getResources().getDisplayMetrics().density;
			int wPx = (int) (cardWidthDp * d), hPx = (int) (cardHeightDp * d);

			LinearLayout root = new LinearLayout(parent.getContext());
			root.setOrientation(LinearLayout.VERTICAL);
			RecyclerView.LayoutParams rlp = new RecyclerView.LayoutParams(wPx, ViewGroup.LayoutParams.WRAP_CONTENT);
			rlp.rightMargin = (int) (rightMarginDp * d);
			root.setLayoutParams(rlp);

			androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(parent.getContext());
			card.setLayoutParams(new LinearLayout.LayoutParams(wPx, hPx));
			card.setRadius(radiusDp * d);
			card.setCardElevation(0f);
			card.setCardBackgroundColor(Color.parseColor("#12141A"));

			FrameLayout overlay = new FrameLayout(parent.getContext());
			overlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

			ImageView imgCover = new ImageView(parent.getContext());
			imgCover.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			imgCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imgCover.setTag("img_cover");
			overlay.addView(imgCover);

			View scrim = new View(parent.getContext());
			scrim.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (56 * d), Gravity.BOTTOM));
			scrim.setTag("bottom_scrim");
			overlay.addView(scrim);

			TextView tvStatus = new TextView(parent.getContext());
			FrameLayout.LayoutParams sLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
			sLp.setMargins((int) (7 * d), (int) (7 * d), (int) (7 * d), 0);
			tvStatus.setLayoutParams(sLp);
			tvStatus.setPadding((int) (7 * d), (int) (2 * d), (int) (7 * d), (int) (2 * d));
			tvStatus.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
			tvStatus.setTextColor(Color.WHITE);
			tvStatus.setMaxLines(1);
			tvStatus.setTag("text_status");
			overlay.addView(tvStatus);

			TextView tvEp = new TextView(parent.getContext());
			FrameLayout.LayoutParams eLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
			eLp.setMargins((int) (8 * d), 0, (int) (8 * d), (int) (7 * d));
			tvEp.setLayoutParams(eLp);
			tvEp.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvEp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
			tvEp.setTextColor(Color.WHITE);
			tvEp.setMaxLines(1);
			tvEp.setEllipsize(TextUtils.TruncateAt.END);
			tvEp.setTag("text_episode");
			overlay.addView(tvEp);

			card.addView(overlay);
			root.addView(card);

			TextView tvTitle = new TextView(parent.getContext());
			LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (34 * d));
			tLp.topMargin = (int) (6 * d);
			tvTitle.setLayoutParams(tLp);
			tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp);
			tvTitle.setTextColor(Color.parseColor("#E2E8F0"));
			tvTitle.setLineSpacing(0f, 1.1f);
			tvTitle.setMaxLines(2);
			tvTitle.setEllipsize(TextUtils.TruncateAt.END);
			tvTitle.setTag("text_title");
			root.addView(tvTitle);

			return new ViewHolder(root);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, final int pos) {
			View v = holder.itemView;
			float d = v.getResources().getDisplayMetrics().density;
			if (v.getLayoutParams() instanceof RecyclerView.LayoutParams) {
				RecyclerView.LayoutParams rlp = (RecyclerView.LayoutParams) v.getLayoutParams();
				rlp.rightMargin = (pos == data.size() - 1) ? 0 : (int) (rightMarginDp * d);
				v.setLayoutParams(rlp);
			}

			ImageView imgCover = v.findViewWithTag("img_cover");
			View scrim = v.findViewWithTag("bottom_scrim");
			TextView tvStatus = v.findViewWithTag("text_status");
			TextView tvEp = v.findViewWithTag("text_episode");
			TextView tvTitle = v.findViewWithTag("text_title");

			final HashMap<String, Object> item = data.get(pos);
			if (item == null) return;

			if (item.containsKey("is_loading")) {
				if (imgCover != null) { imgCover.setImageResource(android.R.color.transparent); imgCover.setBackground(createMainShimmerDrawable(radiusDp * d)); }
				if (scrim != null) scrim.setVisibility(View.GONE);
				if (tvStatus != null) { tvStatus.setText(""); tvStatus.setVisibility(View.INVISIBLE); }
				if (tvEp != null) { tvEp.setText(""); tvEp.setVisibility(View.INVISIBLE); }
				if (tvTitle != null) { tvTitle.setText(""); tvTitle.setBackground(createMainShimmerDrawable(8 * d)); }
				v.setOnClickListener(null);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (scrim != null) {
					scrim.setVisibility(View.VISIBLE);
					scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
						new int[]{Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT}));
				}
				if (tvStatus != null) {
					tvStatus.setVisibility(View.VISIBLE);
					GradientDrawable badge = new GradientDrawable();
					badge.setCornerRadius(100f);
					badge.setColor(Color.parseColor("#3B82F6"));
					tvStatus.setBackground(badge);
					tvStatus.setText(safeStr(item, "status", ""));
				}
				if (tvEp != null) { tvEp.setVisibility(View.VISIBLE); tvEp.setText(safeStr(item, "episode", "")); }
				if (tvTitle != null) { tvTitle.setBackgroundColor(Color.TRANSPARENT); tvTitle.setText(safeStr(item, "title", "")); }

				String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
				if (imgCover != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(MainActivity.this).load(Uri.parse(cov)).into(imgCover);
				}
				v.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View view) { openWatchBySlug(safeStr(item, "slug", "")); }
				});
			}
		}

		@Override public int getItemCount() { return data.size(); }
		public class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(View v) { super(v); } }
	}

	public class PustakaListAdapter extends RecyclerView.Adapter<PustakaListAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;
		private final int mode;

		public PustakaListAdapter(ArrayList<HashMap<String, Object>> arr, int mode) {
			this.data = arr;
			this.mode = mode;
		}

		@NonNull @Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context ctx = parent.getContext();
			float d = ctx.getResources().getDisplayMetrics().density;

			LinearLayout row = new LinearLayout(ctx);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(Gravity.CENTER_VERTICAL);
			RecyclerView.LayoutParams rlp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			rlp.bottomMargin = (int) (14 * d);
			row.setLayoutParams(rlp);

			ImageView thumb = new ImageView(ctx);
			LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams((int) (68 * d), (int) (92 * d));
			tLp.rightMargin = (int) (14 * d);
			thumb.setLayoutParams(tLp);
			thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
			thumb.setTag("thumb");

			LinearLayout info = new LinearLayout(ctx);
			info.setOrientation(LinearLayout.VERTICAL);
			info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

			TextView tvTitle = new TextView(ctx);
			tvTitle.setTextColor(Color.WHITE);
			tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
			tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvTitle.setMaxLines(1);
			tvTitle.setEllipsize(TextUtils.TruncateAt.END);
			tvTitle.setTag("title");

			TextView tvEp = new TextView(ctx);
			tvEp.setTextColor(Color.parseColor("#CBD5E1"));
			tvEp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
			tvEp.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvEp.setTag("ep");

			TextView tvSub = new TextView(ctx);
			tvSub.setTextColor(Color.parseColor("#94A3B8"));
			tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
			tvSub.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvSub.setTag("sub");

			info.addView(tvTitle);
			info.addView(tvEp);
			info.addView(tvSub);
			row.addView(thumb);
			row.addView(info);
			return new ViewHolder(row);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
			final HashMap<String, Object> item = data.get(pos);
			if (item == null) return;
			ImageView img = holder.itemView.findViewWithTag("thumb");
			TextView tvTitle = holder.itemView.findViewWithTag("title");
			TextView tvEp = holder.itemView.findViewWithTag("ep");
			TextView tvSub = holder.itemView.findViewWithTag("sub");

			final String title = safeStr(item, "title", mode == 2 ? "Video" : "Donghua");
			if (tvTitle != null) tvTitle.setText(title);

			if (mode == 0) {
				String epRaw = safeStr(item, "episode", "1").trim();
				if (tvEp != null) tvEp.setText(epRaw.toUpperCase(Locale.US).startsWith("EP") ? epRaw.toUpperCase(Locale.US) : "EP " + epRaw);
				if (tvSub != null) tvSub.setText(safeStr(item, "remainingText", "Lanjutkan menonton"));
			} else if (mode == 1) {
				String ep = safeStr(item, "episode", "");
				if (tvEp != null) tvEp.setText(ep.isEmpty() ? "Daftarku" : ep);
				if (tvSub != null) tvSub.setText(safeStr(item, "status", "Favorit"));
			} else {
				if (tvEp != null) tvEp.setText("OFFLINE");
				if (tvSub != null) tvSub.setText(safeStr(item, "size_mb", ""));
			}

			if (mode == 2) {
				if (img != null) img.setImageResource(R.drawable.default_image);
				final String path = safeStr(item, "file_path", "");
				holder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!path.isEmpty()) {
							Intent play = new Intent(MainActivity.this, WatchActivity.class);
							play.putExtra("video_url", Uri.fromFile(new File(path)).toString());
							play.putExtra("title", title);
							startActivity(play);
						}
					}
				});
			} else {
				String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
				if (cov.isEmpty()) cov = normalizeCoverUrl(safeStr(item, "poster", ""));
				if (img != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(MainActivity.this).load(Uri.parse(cov)).into(img);
				}
				holder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View v) { openWatchBySlug(safeStr(item, "slug", "")); }
				});
			}
		}

		@Override public int getItemCount() { return data.size(); }
		class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(@NonNull View v) { super(v); } }
	}

	public class Gridview1Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> data;
		public Gridview1Adapter(ArrayList<HashMap<String, Object>> arr) { this.data = arr; }

		@Override public int getCount() { return data.size(); }
		@Override public HashMap<String, Object> getItem(int idx) { return data.get(idx); }
		@Override public long getItemId(int idx) { return idx; }

		@Override
		public View getView(final int pos, View convertView, ViewGroup parent) {
			View v = getLayoutInflater().inflate(R.layout.custom_anime, parent, false);
			float d = v.getResources().getDisplayMetrics().density;

			TextView tvTitle = v.findViewById(R.id.text_title);
			TextView tvStatus = v.findViewById(R.id.text_status);
			TextView tvEp = v.findViewById(R.id.text_episode);
			ImageView imgCover = v.findViewById(R.id.img_cover);
			View view2 = v.findViewById(R.id.view2);

			HashMap<String, Object> item = data.get(pos);
			if (item != null && item.containsKey("is_loading")) {
				if (tvTitle != null) { tvTitle.setText(""); tvTitle.setBackground(createMainShimmerDrawable(8 * d)); }
				if (tvStatus != null) { tvStatus.setText(""); tvStatus.setBackground(createMainShimmerDrawable(50f)); }
				if (tvEp != null) { tvEp.setText(""); tvEp.setBackground(createMainShimmerDrawable(6 * d)); }
				if (imgCover != null) { imgCover.setImageResource(android.R.color.transparent); imgCover.setBackground(createMainShimmerDrawable(16 * d)); }
				if (view2 != null) view2.setVisibility(View.GONE);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (tvTitle != null) { tvTitle.setBackgroundColor(Color.TRANSPARENT); tvTitle.setText(safeStr(item, "title", "")); }
				if (tvEp != null) { tvEp.setVisibility(View.VISIBLE); tvEp.setBackgroundColor(Color.TRANSPARENT); tvEp.setText(safeStr(item, "episode", "")); }
				if (view2 != null) {
					view2.setVisibility(View.VISIBLE);
					view2.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
						new int[]{Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT}));
				}
				if (tvStatus != null) {
					GradientDrawable badge = new GradientDrawable();
					badge.setCornerRadius(50f);
					badge.setColor(0xFF3B82F6);
					tvStatus.setBackground(badge);
					tvStatus.setText(safeStr(item, "status", ""));
				}
				String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
				if (imgCover != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(MainActivity.this).load(Uri.parse(cov)).into(imgCover);
				}
			}
			return v;
		}
	}

	public class ListPopularAdapter extends RecyclerView.Adapter<ListPopularAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> data;
		public ListPopularAdapter(ArrayList<HashMap<String, Object>> arr) { this.data = arr; }

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = getLayoutInflater().inflate(R.layout.item_popular_donghua, parent, false);
			v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, final int pos) {
			View v = holder.itemView;
			float d = v.getResources().getDisplayMetrics().density;

			TextView tvTitle = v.findViewById(getResources().getIdentifier("text_popular_title", "id", getPackageName()));
			TextView tvDesc = v.findViewById(getResources().getIdentifier("text_popular_desc", "id", getPackageName()));
			TextView tvGenre = v.findViewById(getResources().getIdentifier("text_popular_genre", "id", getPackageName()));
			TextView tvStatus = v.findViewById(getResources().getIdentifier("text_popular_status", "id", getPackageName()));
			ImageView imgCover = v.findViewById(getResources().getIdentifier("img_popular_cover", "id", getPackageName()));
			View linear35 = v.findViewById(getResources().getIdentifier("linear35", "id", getPackageName()));

			final HashMap<String, Object> item = data.get(pos);
			if (item == null) return;

			if (item.containsKey("is_loading")) {
				if (tvTitle != null) { tvTitle.setText(""); tvTitle.setBackground(createMainShimmerDrawable(100f)); }
				if (tvDesc != null) { tvDesc.setText(""); tvDesc.setBackground(createMainShimmerDrawable(6 * d)); }
				if (tvGenre != null) { tvGenre.setText(""); tvGenre.setBackground(createMainShimmerDrawable(6 * d)); }
				if (tvStatus != null) { tvStatus.setText("                    "); tvStatus.setBackground(createMainShimmerDrawable(100f)); }
				if (imgCover != null) { imgCover.setImageResource(android.R.color.transparent); imgCover.setBackground(createMainShimmerDrawable(16 * d)); }
			} else {
				if (tvTitle != null) { tvTitle.setBackgroundColor(Color.TRANSPARENT); tvTitle.setText(safeStr(item, "title", "")); }
				if (tvDesc != null) { tvDesc.setBackgroundColor(Color.TRANSPARENT); tvDesc.setText(cleanText(safeStr(item, "synopsis", ""))); }
				if (tvGenre != null) { tvGenre.setBackgroundColor(Color.TRANSPARENT); tvGenre.setText(safeStr(item, "genres", "").replace("[", "").replace("]", "")); }
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);

				if (tvStatus != null) {
					GradientDrawable badge = new GradientDrawable();
					badge.setCornerRadius(50f);
					badge.setColor(0xFF111111);
					tvStatus.setBackground(badge);
					tvStatus.setText(safeStr(item, "episode", "") + " / " + safeStr(item, "status", ""));
				}
				String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
				if (imgCover != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(MainActivity.this).load(Uri.parse(cov)).into(imgCover);
				}
				View clickTarget = linear35 != null ? linear35 : v;
				clickTarget.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View view) { openWatchBySlug(safeStr(item, "slug", "")); }
				});
			}
		}

		@Override public int getItemCount() { return data.size(); }
		public class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(View v) { super(v); } }
	}
}
