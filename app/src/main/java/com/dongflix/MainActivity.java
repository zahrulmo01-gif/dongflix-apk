package com.dongflix;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.*;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";

	private String currentUserKey = "";

	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> fullCatalogList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> historyList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> favoritesList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> downloadsList = new ArrayList<>();
	private final Map<String, Long> viewsTotalMap = new HashMap<>();
	private final Map<String, Long> viewsDailyMap = new HashMap<>();

	private RequestNetwork dongflix_req;
	private RequestNetwork.RequestListener _dongflix_req_request_listener;

	private GridView gridview1;
	private RecyclerView recyclerPopular, listPopular, recyclerSearchList;
	private RecyclerView recyclerMovieDonghua, recyclerCompletedDonghua, recyclerComedyDonghua, recyclerMysteryDonghua;
	private TextView textHeroTitle, textHeroDesc, textSeeAll, btnClearSearch, textSearchSectionLabel;
	private TextView textMovieSeeAll, textCompletedSeeAll, textComedySeeAll, textMysterySeeAll, textPopularSeeAll;
	private ImageView imgHeroBanner, imgPlayIcon, imgHeroFavorite, imgTopNotif, imgTopChat, imgUserAvatar;
	private LinearLayout btnHeroPlay, indicatorCapsule, searchBarContainer;
	private FrameLayout btnHeroFavorite, btnTopNotif, btnTopChat;
	private View dot0, dot1, dot2;
	private EditText inputSearch;

	// Single global Hero Slider timer agar tidak bertumpuk saat refresh
	private final Handler heroSliderHandler = new Handler(Looper.getMainLooper());
	private Runnable heroSliderRunnable;
	private int heroCurrentIndex = 0;

	// Pull-to-refresh icon only (YouTube style, floating)
	private FrameLayout pullRefreshIndicator;
	private ProgressBar pullRefreshSpinner;
	private ImageView imgPullRefreshIcon;
	private boolean isRefreshingCatalog = false;
	private float pullStartY = 0f;
	private boolean isPullingFromTop = false;

	// Lanjutkan Menonton
	private LinearLayout layoutSectionContinue;
	private TextView textTitleContinue, textContinueSeeAll;
	private RecyclerView recyclerContinue;

	// Pustaka (Riwayat, Daftarku, Unduhan)
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

	private SharedPreferences favPrefs;
	private HashMap<String, Object> currentHeroItem;
	private Typeface outfitFont;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);

		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
			loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(loginIntent);
			finish();
			return;
		}

		setContentView(R.layout.activity_main);

		DongflixPlayerManager.getInstance().initWithApplication(getApplication());
		favPrefs = getSharedPreferences("dongflix_favorites", Context.MODE_PRIVATE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 201);
			}
		}

		initialize(_savedInstanceState);
		setupContinueWatchingSection();
		setupPullToRefresh();
		setupPustakaSection();
		initializeLogic();
		loadLoggedInUserProfile(currentUser);
		loadUserHistoryAndFavoritesFromFirebase(currentUser);
		loadLocalDownloads();
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
			int pillW = liquidDragPill.getWidth();
			float minTx = 0f;
			float maxTx = Math.max(0f, trackW - pillW - (10 * d));

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
						if (Math.abs(dx) > (6 * d)) {
							isDraggingNavbarPill = true;
						}
						if (isDraggingNavbarPill) {
							float nextTx = Math.max(minTx, Math.min(maxTx, navInitialPillTx + dx));
							liquidDragPill.setTranslationX(nextTx);
							return true;
						}
					}
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					if (navDownRawX > 0f) {
						liquidDragPill.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
						float slotW = Math.max(1f, (trackW - (10 * d)) / 3f);
						int targetIdx;
						if (isDraggingNavbarPill) {
							float currentTx = liquidDragPill.getTranslationX();
							targetIdx = Math.max(0, Math.min(2, Math.round(currentTx / slotW)));
						} else {
							float localX = rawX - loc[0];
							targetIdx = Math.max(0, Math.min(2, (int) (localX / Math.max(1f, trackW / 3f))));
						}
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

	private void stylePlainOutfitText(TextView tv, float spSize, int colorHex) {
		if (tv == null) return;
		tv.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, spSize);
		tv.setTextColor(colorHex);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			tv.setLetterSpacing(-0.01f);
		}
	}

	private Drawable createEmptyPlaceholderDrawable(int colorHex, float radiusPx) {
		GradientDrawable gd = new GradientDrawable();
		gd.setShape(GradientDrawable.RECTANGLE);
		gd.setColor(colorHex);
		gd.setCornerRadius(radiusPx);
		return gd;
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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				pullRefreshIndicator.setElevation(18f);
			}
		}
		if (pullRefreshSpinner != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			pullRefreshSpinner.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
		}
		if (imgPullRefreshIcon != null) {
			imgPullRefreshIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		}

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
							if (deltaY > (16 * d)) {
								if (pullRefreshIndicator != null) {
									pullRefreshIndicator.setVisibility(View.VISIBLE);
									if (imgPullRefreshIcon != null) {
										imgPullRefreshIcon.setVisibility(View.VISIBLE);
										imgPullRefreshIcon.setRotation(Math.min(180f, deltaY * 1.4f));
									}
									if (pullRefreshSpinner != null) {
										pullRefreshSpinner.setVisibility(View.GONE);
									}
								}
							}
						}
						break;

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						if (isPullingFromTop && mainScroll.getScrollY() <= 0) {
							float deltaY = event.getRawY() - pullStartY;
							if (deltaY >= thresholdPx) {
								triggerCatalogRefresh();
							} else if (pullRefreshIndicator != null && !isRefreshingCatalog) {
								pullRefreshIndicator.setVisibility(View.GONE);
							}
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
		if (pullRefreshIndicator != null) {
			pullRefreshIndicator.setVisibility(View.VISIBLE);
		}
		if (imgPullRefreshIcon != null) {
			imgPullRefreshIcon.setVisibility(View.GONE);
		}
		if (pullRefreshSpinner != null) {
			pullRefreshSpinner.setVisibility(View.VISIBLE);
		}
		if (dongflix_req != null && _dongflix_req_request_listener != null) {
			dongflix_req.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/", "", _dongflix_req_request_listener);
		}
	}

	private void hideRefreshIndicator() {
		isRefreshingCatalog = false;
		if (pullRefreshIndicator != null) {
			pullRefreshIndicator.setVisibility(View.GONE);
		}
		if (pullRefreshSpinner != null) {
			pullRefreshSpinner.setVisibility(View.GONE);
		}
		if (imgPullRefreshIcon != null) {
			imgPullRefreshIcon.setVisibility(View.VISIBLE);
		}
	}

	private void startHeroAutoSlider() {
		stopHeroAutoSlider();
		if (listMap == null || listMap.size() < 3) return;

		final float density = getResources().getDisplayMetrics().density;
		final int activeWidth = (int) (16 * density);
		final int inactiveWidth = (int) (5 * density);
		final int fixedHeight = (int) (5 * density);
		final int marginSpacing = (int) (5 * density);

		final GradientDrawable activeDotDrawable = new GradientDrawable();
		activeDotDrawable.setShape(GradientDrawable.RECTANGLE);
		activeDotDrawable.setColor(Color.WHITE);
		activeDotDrawable.setCornerRadius(100f);

		final GradientDrawable inactiveDotDrawable = new GradientDrawable();
		inactiveDotDrawable.setShape(GradientDrawable.RECTANGLE);
		inactiveDotDrawable.setColor(Color.parseColor("#80FFFFFF"));
		inactiveDotDrawable.setCornerRadius(100f);

		heroCurrentIndex = 0;
		heroSliderRunnable = new Runnable() {
			@Override
			public void run() {
				if (listMap == null || listMap.size() < 3) return;
				int pos = heroCurrentIndex % 3;
				HashMap<String, Object> heroItem = listMap.get(pos);
				currentHeroItem = heroItem;

				String heroTitle = heroItem.get("title") != null ? heroItem.get("title").toString().trim() : "";
				String heroSynopsis = heroItem.get("synopsis") != null ? cleanText(heroItem.get("synopsis").toString()) : "";
				String heroCover = heroItem.get("cover") != null ? heroItem.get("cover").toString().trim() : "";
				final String heroSlug = heroItem.get("slug") != null ? heroItem.get("slug").toString() : "";

				if (heroCover.contains("i.ibb.co.com")) {
					heroCover = heroCover.replace("i.ibb.co.com", "i.ibb.co");
				}
				if (heroCover.startsWith("http://")) {
					heroCover = heroCover.replace("http://", "https://");
				}

				if (textHeroTitle != null) textHeroTitle.setText(heroTitle);
				if (textHeroDesc != null) textHeroDesc.setText(heroSynopsis);
				updateHeroFavoriteIcon(heroSlug);

				if (imgHeroBanner != null && !heroCover.isEmpty()) {
					Glide.with(getApplicationContext())
						.load(Uri.parse(heroCover))
						.centerCrop()
						.placeholder(R.drawable.default_image)
						.error(R.drawable.default_image)
						.into(imgHeroBanner);
				}

				if (btnHeroPlay != null) {
					btnHeroPlay.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							openWatchBySlug(heroSlug);
						}
					});
				}

				View[] dots = {dot0, dot1, dot2};
				for (int i = 0; i < dots.length; i++) {
					if (dots[i] != null) {
						boolean isActive = (i == pos);
						LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dots[i].getLayoutParams();
						params.width = isActive ? activeWidth : inactiveWidth;
						params.height = fixedHeight;
						params.leftMargin = (i == 0) ? 0 : marginSpacing;
						dots[i].setLayoutParams(params);
						dots[i].setBackground(isActive ? activeDotDrawable : inactiveDotDrawable);
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

	private void bindHorizontalPosterRecycler(RecyclerView rv, ArrayList<HashMap<String, Object>> items, int widthDp, int heightDp) {
		if (rv == null) return;
		float d = getResources().getDisplayMetrics().density;
		rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		rv.setNestedScrollingEnabled(false);
		rv.setOnFlingListener(null);
		rv.setClipToPadding(false);
		rv.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
		rv.setAdapter(new HorizontalPosterAdapter(items, widthDp, heightDp));
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

	private void populateExtraCatalogSections() {
		if (fullCatalogList == null) return;

		ArrayList<HashMap<String, Object>> movieList = new ArrayList<>();
		ArrayList<HashMap<String, Object>> completedList = new ArrayList<>();
		ArrayList<HashMap<String, Object>> comedyList = new ArrayList<>();
		ArrayList<HashMap<String, Object>> mysteryList = new ArrayList<>();

		for (HashMap<String, Object> item : fullCatalogList) {
			String status = item.get("status") != null ? item.get("status").toString().toLowerCase(Locale.US) : "";
			String genres = item.get("genres") != null ? item.get("genres").toString().toLowerCase(Locale.US) : "";
			String title = item.get("title") != null ? item.get("title").toString().toLowerCase(Locale.US) : "";
			String type = item.get("type") != null ? item.get("type").toString().toLowerCase(Locale.US) : "";
			String episode = item.get("episode") != null ? item.get("episode").toString().toLowerCase(Locale.US) : "";
			String combinedMeta = genres + " " + title + " " + type + " " + episode;

			if (combinedMeta.contains("movie") || combinedMeta.contains("film")) {
				movieList.add(item);
			}
			if (status.contains("completed") || status.contains("tamat")) {
				completedList.add(item);
			}
			if (combinedMeta.contains("comedy") || combinedMeta.contains("komedi") || combinedMeta.contains("slice of life")) {
				comedyList.add(item);
			}
			if (combinedMeta.contains("mystery") || combinedMeta.contains("misteri")
				|| combinedMeta.contains("thriller") || combinedMeta.contains("suspense") || combinedMeta.contains("detective")) {
				mysteryList.add(item);
			}
		}

		if (movieList.isEmpty()) movieList.addAll(fullCatalogList.subList(0, Math.min(8, fullCatalogList.size())));
		if (completedList.isEmpty()) completedList.addAll(fullCatalogList.subList(0, Math.min(8, fullCatalogList.size())));
		if (comedyList.isEmpty()) comedyList.addAll(fullCatalogList.subList(0, Math.min(8, fullCatalogList.size())));
		if (mysteryList.isEmpty()) mysteryList.addAll(fullCatalogList.subList(0, Math.min(8, fullCatalogList.size())));

		bindHorizontalPosterRecycler(
			recyclerMovieDonghua,
			new ArrayList<>(movieList.subList(0, Math.min(10, movieList.size()))),
			114, 162
		);
		bindHorizontalPosterRecycler(
			recyclerCompletedDonghua,
			new ArrayList<>(completedList.subList(0, Math.min(10, completedList.size()))),
			114, 162
		);
		bindHorizontalPosterRecycler(
			recyclerComedyDonghua,
			new ArrayList<>(comedyList.subList(0, Math.min(10, comedyList.size()))),
			114, 162
		);
		bindHorizontalPosterRecycler(
			recyclerMysteryDonghua,
			new ArrayList<>(mysteryList.subList(0, Math.min(10, mysteryList.size()))),
			114, 162
		);
	}

	private void applyCircularAvatar(ImageView target, String photoUrl) {
		if (target == null) return;
		float d = getResources().getDisplayMetrics().density;
		int sizePx = Math.max((int) (38 * d), target.getWidth() > 0 ? target.getWidth() : (int) (38 * d));

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
						Bitmap square = cropCenterSquare(rawBmp);
						Bitmap circular = getCircularBitmap(Bitmap.createScaledBitmap(square, sizePx, sizePx, true));
						target.setImageBitmap(circular);
						return;
					}
				} catch (Exception ignored) {}
			} else {
				Glide.with(getApplicationContext())
					.load(Uri.parse(cleanUrl))
					.circleCrop()
					.placeholder(R.drawable.ic_profile)
					.error(R.drawable.ic_profile)
					.into(target);
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

	private Bitmap cropCenterSquare(Bitmap src) {
		int w = src.getWidth();
		int h = src.getHeight();
		int edge = Math.min(w, h);
		int x = (w - edge) / 2;
		int y = (h - edge) / 2;
		return Bitmap.createBitmap(src, x, y, edge, edge);
	}

	private Bitmap getCircularBitmap(Bitmap squareBmp) {
		int size = Math.min(squareBmp.getWidth(), squareBmp.getHeight());
		Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Rect rect = new Rect(0, 0, size, size);
		canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(squareBmp, rect, rect, paint);
		return output;
	}

	private void loadLoggedInUserProfile(FirebaseUser currentUser) {
		if (currentUser == null || currentUser.getEmail() == null) return;

		String email = currentUser.getEmail().trim().toLowerCase();
		final String fallbackName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;

		TextView tvUserHeader = findViewById(R.id.text_username);
		if (tvUserHeader != null) tvUserHeader.setText(fallbackName);

		TextView tvProfileName = findViewById(R.id.textview65);
		if (tvProfileName != null) tvProfileName.setText(fallbackName);

		String initialPhoto = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";
		applyCircularAvatar(imgUserAvatar, initialPhoto);

		currentUserKey = email.replace(".", ",");
		FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(currentUserKey)
			.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (!snapshot.exists()) {
						applyCircularAvatar(imgUserAvatar, "");
						return;
					}

					String resolvedName = fallbackName;
					Object nameObj = snapshot.child("name").getValue();
					if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
						resolvedName = nameObj.toString().trim();
					}

					if (tvUserHeader != null) tvUserHeader.setText(resolvedName);
					if (tvProfileName != null) tvProfileName.setText(resolvedName);

					String photoUrl = "";
					Object photoObj = snapshot.child("photoURL").getValue();
					if (photoObj != null && !photoObj.toString().trim().isEmpty()) {
						photoUrl = photoObj.toString().trim();
					}

					applyCircularAvatar(imgUserAvatar, photoUrl);
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					applyCircularAvatar(imgUserAvatar, "");
				}
			});
	}

	private void setupContinueWatchingSection() {
		layoutSectionContinue = findViewById(R.id.layout_section_continue);
		textTitleContinue = findViewById(R.id.text_title_continue);
		textContinueSeeAll = findViewById(R.id.text_continue_see_all);
		recyclerContinue = findViewById(R.id.recyclerContinue);

		if (recyclerContinue != null) {
			float d = getResources().getDisplayMetrics().density;
			recyclerContinue.setLayoutManager(
				new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
			);
			recyclerContinue.setNestedScrollingEnabled(false);
			recyclerContinue.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				(int) (132 * d)
			));
			recyclerContinue.setAdapter(new ContinueWatchingAdapter(buildEmptyDummyList(3)));
		}

		if (textContinueSeeAll != null) {
			textContinueSeeAll.setText("Lihat riwayat");
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
		int statusBarHeight = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusBarHeight = getResources().getDimensionPixelSize(resId);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		));
		root.setPadding((int) (16 * d), statusBarHeight + (int) (16 * d), (int) (16 * d), (int) (118 * d));

		TextView tvHeaderTitle = new TextView(this);
		tvHeaderTitle.setText("Pustaka");
		stylePlainOutfitText(tvHeaderTitle, 22f, Color.WHITE);
		root.addView(tvHeaderTitle);

		LinearLayout tabsBar = new LinearLayout(this);
		tabsBar.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
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
		stylePlainOutfitText(textPustakaSectionLabel, 17f, Color.WHITE);
		LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		secLp.topMargin = (int) (22 * d);
		secLp.bottomMargin = (int) (14 * d);
		textPustakaSectionLabel.setLayoutParams(secLp);
		root.addView(textPustakaSectionLabel);

		recyclerPustakaList = new RecyclerView(this);
		recyclerPustakaList.setLayoutManager(new LinearLayoutManager(this));
		recyclerPustakaList.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		));
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
		tv.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
		tv.setPadding(0, 0, 0, (int) (8 * d));

		line.setLayoutParams(new LinearLayout.LayoutParams((int) (24 * d), (int) (3 * d)));

		wrapper.addView(tv);
		wrapper.addView(line);
		return wrapper;
	}

	private void updatePustakaTabVisual(int idx) {
		int activeColor = Color.WHITE;
		int inactiveColor = Color.parseColor("#94A3B8");
		int indicatorColor = Color.parseColor("#38BDF8");

		if (tabPustakaRiwayat != null) tabPustakaRiwayat.setTextColor(idx == 0 ? activeColor : inactiveColor);
		if (tabPustakaDaftarku != null) tabPustakaDaftarku.setTextColor(idx == 1 ? activeColor : inactiveColor);
		if (tabPustakaUnduhan != null) tabPustakaUnduhan.setTextColor(idx == 2 ? activeColor : inactiveColor);

		if (linePustakaRiwayat != null) linePustakaRiwayat.setBackgroundColor(idx == 0 ? indicatorColor : Color.TRANSPARENT);
		if (linePustakaDaftarku != null) linePustakaDaftarku.setBackgroundColor(idx == 1 ? indicatorColor : Color.TRANSPARENT);
		if (linePustakaUnduhan != null) linePustakaUnduhan.setBackgroundColor(idx == 2 ? indicatorColor : Color.TRANSPARENT);
	}

	private void loadUserHistoryAndFavoritesFromFirebase(FirebaseUser user) {
		if (user == null || user.getEmail() == null) return;
		String key = user.getEmail().trim().toLowerCase().replace(".", ",");
		currentUserKey = key;

		DatabaseReference userNodeRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(key);

		userNodeRef.child("history").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				ArrayList<HashMap<String, Object>> parsed = new ArrayList<>();
				if (snapshot.exists()) {
					for (DataSnapshot child : snapshot.getChildren()) {
						HashMap<String, Object> item = extractMapFromSnapshot(child);
						if (!item.isEmpty()) {
							if (!item.containsKey("slug") && child.getKey() != null) {
								item.put("slug", child.getKey());
							}
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

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		});

		userNodeRef.child("favorites").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				ArrayList<HashMap<String, Object>> parsedFavs = new ArrayList<>();
				if (snapshot.exists()) {
					for (DataSnapshot child : snapshot.getChildren()) {
						HashMap<String, Object> item = extractMapFromSnapshot(child);
						if (!item.isEmpty()) {
							if (!item.containsKey("slug") && child.getKey() != null) {
								item.put("slug", child.getKey());
							}
							parsedFavs.add(item);
						}
					}
				}
				favoritesList = parsedFavs;
				if (activePustakaTab == 1) renderActivePustakaTab();
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		});
	}

	private HashMap<String, Object> extractMapFromSnapshot(DataSnapshot snap) {
		HashMap<String, Object> map = new HashMap<>();
		if (snap == null || !snap.exists()) return map;
		Object val = snap.getValue();
		if (val instanceof Map) {
			Map<?, ?> rawMap = (Map<?, ?>) val;
			for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
				if (entry.getKey() != null) {
					map.put(entry.getKey().toString(), entry.getValue());
				}
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
		try {
			return Long.parseLong(o.toString().trim());
		} catch (Exception e) {
			return 0L;
		}
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
			if (dir != null && dir.exists() && dir.isDirectory()) {
				collectDownloadFiles(dir, found);
			}
		}
		downloadsList = found;
		if (activePustakaTab == 2) {
			renderActivePustakaTab();
		}
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
					String cleanName = f.getName().replaceAll("\\.[^.]+$", "").replace("_", " ");
					item.put("title", cleanName);
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
			recyclerPustakaList.setAdapter(new PustakaHistoryAdapter(historyList));
		} else if (activePustakaTab == 1) {
			if (textPustakaSectionLabel != null) textPustakaSectionLabel.setText("Daftarku");
			ArrayList<HashMap<String, Object>> combinedFavs = getMergedFavoritesList();
			recyclerPustakaList.setAdapter(new PustakaFavoritesAdapter(combinedFavs));
		} else {
			loadLocalDownloads();
			if (textPustakaSectionLabel != null) textPustakaSectionLabel.setText("Unduhan Tersimpan");
			recyclerPustakaList.setAdapter(new PustakaDownloadsAdapter(downloadsList));
		}
	}

	private ArrayList<HashMap<String, Object>> getMergedFavoritesList() {
		LinkedHashMap<String, HashMap<String, Object>> dedup = new LinkedHashMap<>();
		for (HashMap<String, Object> item : favoritesList) {
			String s = item.get("slug") != null ? item.get("slug").toString() : "";
			if (!s.isEmpty()) dedup.put(s, item);
		}
		if (favPrefs != null) {
			Map<String, ?> all = favPrefs.getAll();
			for (Map.Entry<String, ?> e : all.entrySet()) {
				if (e.getKey().startsWith("fav_") && e.getValue() instanceof String) {
					try {
						HashMap<String, Object> localMap = new Gson().fromJson(
							e.getValue().toString(),
							new TypeToken<HashMap<String, Object>>(){}.getType()
						);
						if (localMap != null) {
							String s = localMap.get("slug") != null ? localMap.get("slug").toString() : e.getKey().substring(4);
							if (!dedup.containsKey(s)) {
								dedup.put(s, localMap);
							}
						}
					} catch (Exception ignored) {}
				}
			}
		}
		return new ArrayList<>(dedup.values());
	}

	public class ContinueWatchingAdapter extends RecyclerView.Adapter<ContinueWatchingAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;

		public ContinueWatchingAdapter(ArrayList<HashMap<String, Object>> arr) {
			this.data = arr;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			float d = parent.getResources().getDisplayMetrics().density;

			FrameLayout card = new FrameLayout(parent.getContext());
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
				(int) (248 * d),
				(int) (124 * d)
			);
			lp.rightMargin = (int) (12 * d);
			card.setLayoutParams(lp);

			GradientDrawable cardBg = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT,
				new int[]{Color.parseColor("#071830"), Color.parseColor("#0C2847")}
			);
			cardBg.setCornerRadius(16 * d);
			card.setBackground(cardBg);
			card.setClipToOutline(true);
			card.setTag("card_bg");

			ImageView thumb = new ImageView(parent.getContext());
			thumb.setLayoutParams(new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
			));
			thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
			thumb.setAlpha(0.7f);
			thumb.setTag("thumb");
			card.addView(thumb);

			View scrim = new View(parent.getContext());
			scrim.setLayoutParams(new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
			));
			scrim.setBackground(new GradientDrawable(
				GradientDrawable.Orientation.BOTTOM_TOP,
				new int[]{Color.parseColor("#E6030A14"), Color.parseColor("#66030A14"), Color.TRANSPARENT}
			));
			scrim.setTag("scrim");
			card.addView(scrim);

			LinearLayout textCol = new LinearLayout(parent.getContext());
			textCol.setOrientation(LinearLayout.VERTICAL);
			textCol.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (12 * d));
			FrameLayout.LayoutParams textLp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
			);
			textCol.setLayoutParams(textLp);

			TextView tvTitle = new TextView(parent.getContext());
			stylePlainOutfitText(tvTitle, 14.5f, Color.WHITE);
			tvTitle.setMaxLines(2);
			tvTitle.setEllipsize(TextUtils.TruncateAt.END);
			tvTitle.setTag("title");

			TextView tvEp = new TextView(parent.getContext());
			stylePlainOutfitText(tvEp, 12f, Color.parseColor("#CBD5E1"));
			tvEp.setTag("ep");

			textCol.addView(tvTitle);
			textCol.addView(tvEp);
			card.addView(textCol);

			View progressBar = new View(parent.getContext());
			FrameLayout.LayoutParams progLp = new FrameLayout.LayoutParams(
				(int) (145 * d), (int) (3 * d), Gravity.BOTTOM | Gravity.START
			);
			progressBar.setLayoutParams(progLp);
			progressBar.setBackgroundColor(Color.parseColor("#38BDF8"));
			progressBar.setTag("progress_bar");
			card.addView(progressBar);

			return new ViewHolder(card);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			final HashMap<String, Object> item = data.get(position);
			float d = holder.itemView.getResources().getDisplayMetrics().density;

			ImageView thumb = holder.itemView.findViewWithTag("thumb");
			View scrim = holder.itemView.findViewWithTag("scrim");
			View progressBar = holder.itemView.findViewWithTag("progress_bar");
			TextView tvTitle = holder.itemView.findViewWithTag("title");
			TextView tvEp = holder.itemView.findViewWithTag("ep");

			if (item.containsKey("is_loading")) {
				holder.itemView.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 16 * d));
				if (thumb != null) {
					thumb.setImageResource(0);
					thumb.setVisibility(View.GONE);
				}
				if (scrim != null) scrim.setVisibility(View.GONE);
				if (progressBar != null) progressBar.setVisibility(View.GONE);
				if (tvTitle != null) {
					tvTitle.setText("");
					tvTitle.setVisibility(View.GONE);
				}
				if (tvEp != null) {
					tvEp.setText("");
					tvEp.setVisibility(View.GONE);
				}
				holder.itemView.setOnClickListener(null);
				return;
			}

			GradientDrawable cardBg = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT,
				new int[]{Color.parseColor("#071830"), Color.parseColor("#0C2847")}
			);
			cardBg.setCornerRadius(16 * d);
			holder.itemView.setBackground(cardBg);

			if (thumb != null) thumb.setVisibility(View.VISIBLE);
			if (scrim != null) scrim.setVisibility(View.VISIBLE);
			if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
			if (tvTitle != null) tvTitle.setVisibility(View.VISIBLE);
			if (tvEp != null) tvEp.setVisibility(View.VISIBLE);

			String title = item.get("title") != null ? item.get("title").toString() : "Donghua";
			String epRaw = item.get("episode") != null ? item.get("episode").toString().trim() : "1";
			String epLabel = epRaw.toUpperCase(Locale.US).startsWith("EP") ? epRaw.toUpperCase(Locale.US) : "EP " + epRaw;
			String cov = item.get("cover") != null ? item.get("cover").toString() : (item.get("poster") != null ? item.get("poster").toString() : "");

			if (tvTitle != null) tvTitle.setText(title);
			if (tvEp != null) tvEp.setText(epLabel);
			if (thumb != null && !cov.isEmpty()) {
				Glide.with(getApplicationContext()).load(Uri.parse(cov)).into(thumb);
			}

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String slug = item.get("slug") != null ? item.get("slug").toString() : "";
					openWatchBySlug(slug);
				}
			});
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			ViewHolder(@NonNull View itemView) {
				super(itemView);
			}
		}
	}

	public class PustakaHistoryAdapter extends RecyclerView.Adapter<PustakaHistoryAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;

		public PustakaHistoryAdapter(ArrayList<HashMap<String, Object>> arr) {
			this.data = arr;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ViewHolder(buildPustakaRowView(parent.getContext()));
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			final HashMap<String, Object> item = data.get(position);
			ImageView img = holder.itemView.findViewWithTag("thumb");
			TextView tvTitle = holder.itemView.findViewWithTag("title");
			TextView tvEp = holder.itemView.findViewWithTag("ep");
			TextView tvSub = holder.itemView.findViewWithTag("sub");

			String title = item.get("title") != null ? item.get("title").toString() : "Donghua";
			String epRaw = item.get("episode") != null ? item.get("episode").toString().trim() : "1";
			String epText = epRaw.toUpperCase(Locale.US).startsWith("EP") ? epRaw.toUpperCase(Locale.US) : "EP " + epRaw;
			String remText = item.get("remainingText") != null ? item.get("remainingText").toString() : "Lanjutkan menonton";
			String cov = item.get("cover") != null ? item.get("cover").toString() : (item.get("poster") != null ? item.get("poster").toString() : "");

			if (tvTitle != null) tvTitle.setText(title);
			if (tvEp != null) tvEp.setText(epText);
			if (tvSub != null) tvSub.setText(remText);
			if (img != null && !cov.isEmpty()) {
				Glide.with(getApplicationContext()).load(Uri.parse(cov)).into(img);
			}

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String slug = item.get("slug") != null ? item.get("slug").toString() : "";
					openWatchBySlug(slug);
				}
			});
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			ViewHolder(@NonNull View itemView) {
				super(itemView);
			}
		}
	}

	public class PustakaFavoritesAdapter extends RecyclerView.Adapter<PustakaFavoritesAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;

		public PustakaFavoritesAdapter(ArrayList<HashMap<String, Object>> arr) {
			this.data = arr;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ViewHolder(buildPustakaRowView(parent.getContext()));
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			final HashMap<String, Object> item = data.get(position);
			ImageView img = holder.itemView.findViewWithTag("thumb");
			TextView tvTitle = holder.itemView.findViewWithTag("title");
			TextView tvEp = holder.itemView.findViewWithTag("ep");
			TextView tvSub = holder.itemView.findViewWithTag("sub");

			String title = item.get("title") != null ? item.get("title").toString() : "Donghua";
			String status = item.get("status") != null ? item.get("status").toString() : "Favorit";
			String ep = item.get("episode") != null ? item.get("episode").toString() : "";
			String cov = item.get("cover") != null ? item.get("cover").toString() : (item.get("poster") != null ? item.get("poster").toString() : "");

			if (tvTitle != null) tvTitle.setText(title);
			if (tvEp != null) tvEp.setText(ep.isEmpty() ? "Daftarku" : ep);
			if (tvSub != null) tvSub.setText(status);
			if (img != null && !cov.isEmpty()) {
				Glide.with(getApplicationContext()).load(Uri.parse(cov)).into(img);
			}

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String slug = item.get("slug") != null ? item.get("slug").toString() : "";
					openWatchBySlug(slug);
				}
			});
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			ViewHolder(@NonNull View itemView) {
				super(itemView);
			}
		}
	}

	public class PustakaDownloadsAdapter extends RecyclerView.Adapter<PustakaDownloadsAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;

		public PustakaDownloadsAdapter(ArrayList<HashMap<String, Object>> arr) {
			this.data = arr;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ViewHolder(buildPustakaRowView(parent.getContext()));
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			final HashMap<String, Object> item = data.get(position);
			ImageView img = holder.itemView.findViewWithTag("thumb");
			TextView tvTitle = holder.itemView.findViewWithTag("title");
			TextView tvEp = holder.itemView.findViewWithTag("ep");
			TextView tvSub = holder.itemView.findViewWithTag("sub");

			String title = item.get("title") != null ? item.get("title").toString() : "Video";
			String sizeMb = item.get("size_mb") != null ? item.get("size_mb").toString() : "";
			final String path = item.get("file_path") != null ? item.get("file_path").toString() : "";

			if (tvTitle != null) tvTitle.setText(title);
			if (tvEp != null) tvEp.setText("OFFLINE");
			if (tvSub != null) tvSub.setText(sizeMb);
			if (img != null) img.setImageResource(R.drawable.default_image);

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!path.isEmpty()) {
						Intent playIntent = new Intent(MainActivity.this, WatchActivity.class);
						playIntent.putExtra("video_url", Uri.fromFile(new File(path)).toString());
						playIntent.putExtra("title", title);
						startActivity(playIntent);
					}
				}
			});
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			ViewHolder(@NonNull View itemView) {
				super(itemView);
			}
		}
	}

	private View buildPustakaRowView(Context ctx) {
		float d = ctx.getResources().getDisplayMetrics().density;

		LinearLayout row = new LinearLayout(ctx);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		RecyclerView.LayoutParams rowLp = new RecyclerView.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		rowLp.bottomMargin = (int) (14 * d);
		row.setLayoutParams(rowLp);

		androidx.cardview.widget.CardView thumbCard = new androidx.cardview.widget.CardView(ctx);
		LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(
			(int) (72 * d), (int) (98 * d)
		);
		thumbLp.rightMargin = (int) (14 * d);
		thumbCard.setLayoutParams(thumbLp);
		thumbCard.setRadius(16 * d);
		thumbCard.setCardElevation(0f);
		thumbCard.setCardBackgroundColor(Color.parseColor("#12141A"));

		ImageView thumb = new ImageView(ctx);
		thumb.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
		thumb.setTag("thumb");
		thumbCard.addView(thumb);

		LinearLayout info = new LinearLayout(ctx);
		info.setOrientation(LinearLayout.VERTICAL);
		info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		TextView tvTitle = new TextView(ctx);
		stylePlainOutfitText(tvTitle, 15.5f, Color.WHITE);
		tvTitle.setMaxLines(1);
		tvTitle.setEllipsize(TextUtils.TruncateAt.END);
		tvTitle.setTag("title");

		TextView tvEp = new TextView(ctx);
		stylePlainOutfitText(tvEp, 12.5f, Color.parseColor("#CBD5E1"));
		tvEp.setTag("ep");

		TextView tvSub = new TextView(ctx);
		stylePlainOutfitText(tvSub, 12.5f, Color.parseColor("#94A3B8"));
		tvSub.setTag("sub");

		info.addView(tvTitle);
		info.addView(tvEp);
		info.addView(tvSub);

		row.addView(thumbCard);
		row.addView(info);
		return row;
	}

	private String getTodayDateString() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
	}

	private long parseViewNumber(Object val) {
		if (val == null) return 0L;
		if (val instanceof Number) return ((Number) val).longValue();
		try {
			return Long.parseLong(val.toString().trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private void sortAndRenderPopularSections() {
		if (fullCatalogList == null || fullCatalogList.isEmpty()) return;

		ArrayList<HashMap<String, Object>> todaySorted = new ArrayList<>(fullCatalogList);
		Collections.sort(todaySorted, new Comparator<HashMap<String, Object>>() {
			@Override
			public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
				String slugA = a.get("slug") != null ? a.get("slug").toString() : "";
				String slugB = b.get("slug") != null ? b.get("slug").toString() : "";
				long dailyA = viewsDailyMap.containsKey(slugA) ? viewsDailyMap.get(slugA) : 0L;
				long dailyB = viewsDailyMap.containsKey(slugB) ? viewsDailyMap.get(slugB) : 0L;
				if (dailyB != dailyA) {
					return Long.compare(dailyB, dailyA);
				}
				long totalA = viewsTotalMap.containsKey(slugA) ? viewsTotalMap.get(slugA) : 0L;
				long totalB = viewsTotalMap.containsKey(slugB) ? viewsTotalMap.get(slugB) : 0L;
				return Long.compare(totalB, totalA);
			}
		});

		ArrayList<HashMap<String, Object>> totalSorted = new ArrayList<>(fullCatalogList);
		Collections.sort(totalSorted, new Comparator<HashMap<String, Object>>() {
			@Override
			public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
				String slugA = a.get("slug") != null ? a.get("slug").toString() : "";
				String slugB = b.get("slug") != null ? b.get("slug").toString() : "";
				long totalA = viewsTotalMap.containsKey(slugA) ? viewsTotalMap.get(slugA) : 0L;
				long totalB = viewsTotalMap.containsKey(slugB) ? viewsTotalMap.get(slugB) : 0L;
				return Long.compare(totalB, totalA);
			}
		});

		ArrayList<HashMap<String, Object>> todaySlice = new ArrayList<>(
			todaySorted.subList(0, Math.min(8, todaySorted.size()))
		);
		ArrayList<HashMap<String, Object>> allPopularSlice = new ArrayList<>(
			totalSorted.subList(0, Math.min(10, totalSorted.size()))
		);

		bindHorizontalPosterRecycler(recyclerPopular, todaySlice, 114, 162);
		bindHorizontalPosterRecycler(listPopular, allPopularSlice, 114, 162);

		if (recyclerSearchList != null && (inputSearch == null || inputSearch.getText().toString().trim().isEmpty())) {
			recyclerSearchList.setAdapter(new ListPopularAdapter(allPopularSlice));
		}

		populateExtraCatalogSections();
	}

	private void syncFirebaseViewsAndSort() {
		String todayStr = getTodayDateString();
		DatabaseReference rootDb = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference();

		rootDb.child("views_total").addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				viewsTotalMap.clear();
				if (snapshot.exists()) {
					for (DataSnapshot child : snapshot.getChildren()) {
						if (child.getKey() != null) {
							viewsTotalMap.put(child.getKey(), parseViewNumber(child.getValue()));
						}
					}
				}
				sortAndRenderPopularSections();
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		});

		rootDb.child("views_daily").child(todayStr).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				viewsDailyMap.clear();
				if (snapshot.exists()) {
					for (DataSnapshot child : snapshot.getChildren()) {
						if (child.getKey() != null) {
							viewsDailyMap.put(child.getKey(), parseViewNumber(child.getValue()));
						}
					}
				}
				sortAndRenderPopularSections();
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		});
	}

	private void applyOutfitFontsWithoutBold() {
		Typeface tf = getOutfitTypeface();
		Queue<View> viewQueue = new LinkedList<>();
		viewQueue.add(getWindow().getDecorView().findViewById(android.R.id.content));

		while (!viewQueue.isEmpty()) {
			View currentView = viewQueue.poll();
			if (currentView instanceof TextView) {
				TextView tv = (TextView) currentView;
				tv.setTypeface(tf, Typeface.NORMAL);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					tv.setLetterSpacing(-0.01f);
				}
			} else if (currentView instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) currentView;
				for (int k = 0; k < group.getChildCount(); k++) {
					viewQueue.add(group.getChildAt(k));
				}
			}
		}
	}

	private boolean isFavoriteSlug(String slug) {
		if (favPrefs == null || slug == null || slug.isEmpty()) return false;
		return favPrefs.contains("fav_" + slug);
	}

	private void toggleFavoriteItem(HashMap<String, Object> item) {
		if (favPrefs == null || item == null) return;
		String slug = item.get("slug") != null ? item.get("slug").toString() : "";
		String title = item.get("title") != null ? item.get("title").toString() : "Donghua";
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
		Intent intent = new Intent(MainActivity.this, WatchActivity.class);
		intent.putExtra("slug", slug.trim());
		startActivity(intent);
		overridePendingTransition(R.anim.slide_up_enter, R.anim.no_anim);
	}

	private void openNotificationsSheet() {
		String latestTitle = (currentHeroItem != null && currentHeroItem.get("title") != null)
			? currentHeroItem.get("title").toString()
			: "Episode terbaru Dongflix telah tersedia.";
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
				if (imm != null) {
					imm.showSoftInput(inputSearch, InputMethodManager.SHOW_IMPLICIT);
				}
			}
		}, 120);
	}

	private void hideKeyboard() {
		if (inputSearch == null) return;
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
		}
	}

	private void initialize(Bundle _savedInstanceState) {
		dongflix_req = new RequestNetwork(this);

		pullRefreshIndicator = findViewById(R.id.pull_refresh_indicator);
		pullRefreshSpinner = findViewById(R.id.pull_refresh_spinner);
		imgPullRefreshIcon = findViewById(R.id.img_pull_refresh_icon);

		gridview1 = findViewById(R.id.gridview1);
		recyclerPopular = findViewById(R.id.recyclerPopular);
		listPopular = findViewById(R.id.listPopular);
		recyclerMovieDonghua = findViewById(R.id.recyclerMovieDonghua);
		recyclerCompletedDonghua = findViewById(R.id.recyclerCompletedDonghua);
		recyclerComedyDonghua = findViewById(R.id.recyclerComedyDonghua);
		recyclerMysteryDonghua = findViewById(R.id.recyclerMysteryDonghua);

		textSeeAll = findViewById(R.id.text_see_all);
		textMovieSeeAll = findViewById(R.id.text_movie_see_all);
		textCompletedSeeAll = findViewById(R.id.text_completed_see_all);
		textComedySeeAll = findViewById(R.id.text_comedy_see_all);
		textMysterySeeAll = findViewById(R.id.text_mystery_see_all);
		textPopularSeeAll = findViewById(R.id.text_popular_see_all);

		recyclerSearchList = findViewById(R.id.recycler_search_list);
		searchBarContainer = findViewById(R.id.search_bar_container);
		inputSearch = findViewById(R.id.input_search);
		btnClearSearch = findViewById(R.id.btn_clear_search);
		textSearchSectionLabel = findViewById(R.id.text_search_section_label);

		textHeroTitle = findViewById(R.id.text_hero_title);
		textHeroDesc = findViewById(R.id.text_hero_desc);
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

		if (imgTopNotif != null) {
			int notifResId = getResources().getIdentifier("ic_notifications", "drawable", getPackageName());
			imgTopNotif.setImageResource(notifResId > 0 ? notifResId : R.drawable.ic_search);
		}

		if (btnTopNotif != null) {
			btnTopNotif.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openNotificationsSheet();
				}
			});
		}

		View.OnClickListener openCatalogListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, CatalogActivity.class));
			}
		};
		if (textSeeAll != null) textSeeAll.setOnClickListener(openCatalogListener);
		if (textMovieSeeAll != null) textMovieSeeAll.setOnClickListener(openCatalogListener);
		if (textCompletedSeeAll != null) textCompletedSeeAll.setOnClickListener(openCatalogListener);
		if (textComedySeeAll != null) textComedySeeAll.setOnClickListener(openCatalogListener);
		if (textMysterySeeAll != null) textMysterySeeAll.setOnClickListener(openCatalogListener);
		if (textPopularSeeAll != null) textPopularSeeAll.setOnClickListener(openCatalogListener);

		if (btnHeroFavorite != null) {
			btnHeroFavorite.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentHeroItem != null) {
						toggleFavoriteItem(currentHeroItem);
					}
				}
			});
		}

		if (btnTopChat != null) {
			btnTopChat.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(MainActivity.this, ChatActivity.class));
				}
			});
		}

		if (gridview1 != null) {
			gridview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
					if (listMap != null && _param3 < listMap.size()) {
						HashMap<String, Object> selectedData = listMap.get(_param3);
						if (selectedData.containsKey("is_loading")) return;
						String slug = selectedData.get("slug") != null ? selectedData.get("slug").toString() : "";
						openWatchBySlug(slug);
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
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
					String q = s.toString().trim().toLowerCase();
					if (btnClearSearch != null) {
						btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
					}

					if (q.isEmpty()) {
						if (textSearchSectionLabel != null) {
							textSearchSectionLabel.setVisibility(View.VISIBLE);
							textSearchSectionLabel.setText("Populer");
						}
						if (recyclerSearchList != null) {
							recyclerSearchList.setAdapter(new ListPopularAdapter(listMap));
						}
					} else {
						if (textSearchSectionLabel != null) {
							textSearchSectionLabel.setVisibility(View.GONE);
						}
						ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
						for (HashMap<String, Object> item : fullCatalogList) {
							String title = item.get("title") != null ? item.get("title").toString().toLowerCase() : "";
							String genres = item.get("genres") != null ? item.get("genres").toString().toLowerCase() : "";
							if (title.contains(q) || genres.contains(q)) {
								filtered.add(item);
							}
						}
						if (recyclerSearchList != null) {
							recyclerSearchList.setAdapter(new ListPopularAdapter(filtered));
						}
					}
				}
				@Override public void afterTextChanged(Editable s) {}
			});
		}

		_dongflix_req_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _response = _param2;
				hideRefreshIndicator();

				DongflixPlayerManager.getInstance().cachedAllCatalogJson = _response;

				fullCatalogList = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
				if (fullCatalogList == null) fullCatalogList = new ArrayList<>();

				listMap = new ArrayList<>(fullCatalogList);
				if (listMap.size() > 12) {
					listMap = new ArrayList<>(listMap.subList(0, 12));
				}

				if (gridview1 != null) {
					gridview1.setAdapter(new Gridview1Adapter(listMap));
					setGridViewHeightBasedOnChildren(gridview1, 3);
				}

				if (recyclerSearchList != null) {
					recyclerSearchList.setAdapter(new ListPopularAdapter(listMap));
				}

				if (listMap != null && listMap.size() >= 3) {
					if (textHeroTitle != null) textHeroTitle.setBackgroundColor(Color.TRANSPARENT);
					if (textHeroDesc != null) textHeroDesc.setBackgroundColor(Color.TRANSPARENT);
					if (imgHeroBanner != null) imgHeroBanner.setBackgroundColor(Color.TRANSPARENT);

					GradientDrawable playActiveBg = new GradientDrawable();
					playActiveBg.setShape(GradientDrawable.RECTANGLE);
					playActiveBg.setColor(Color.WHITE);
					playActiveBg.setCornerRadius(100f);

					if (btnHeroPlay != null) {
						btnHeroPlay.setBackground(playActiveBg);
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

				bindHorizontalPosterRecycler(recyclerPopular, listMap, 114, 162);
				bindHorizontalPosterRecycler(listPopular, listMap, 114, 162);

				sortAndRenderPopularSections();
				syncFirebaseViewsAndSort();
				applyOutfitFontsWithoutBold();
			}

			@Override
			public void onErrorResponse(String _param1, String _param2) {
				hideRefreshIndicator();
			}
		};
	}

	private String cleanText(String input) {
		if (input == null) return "";
		return input.replaceAll("\\[[^\\x20-\\x7E]+\\]", "").replaceAll("[^\\x20-\\x7E\\n]", "").trim();
	}

	private void initializeLogic() {
		_UI();
		setupIos26DraggableNavbar();
		_Nav();
		setupScrollResponsiveNavbar();
		applyOutfitFontsWithoutBold();

		float d = getResources().getDisplayMetrics().density;
		ArrayList<HashMap<String, Object>> dummyList = buildEmptyDummyList(12);

		if (gridview1 != null) {
			gridview1.setAdapter(new Gridview1Adapter(dummyList));
			gridview1.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return event.getAction() == MotionEvent.ACTION_MOVE;
				}
			});
			setGridViewHeightBasedOnChildren(gridview1, 3);
		}

		if (textHeroTitle != null) {
			textHeroTitle.setText("");
			textHeroTitle.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 10 * d));
		}
		if (textHeroDesc != null) {
			textHeroDesc.setText("");
			textHeroDesc.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 8 * d));
		}

		if (imgHeroBanner != null) {
			imgHeroBanner.setImageResource(0);
			imgHeroBanner.setBackgroundColor(Color.parseColor("#12141A"));
		}

		if (btnHeroPlay != null) {
			btnHeroPlay.setVisibility(View.GONE);
			btnHeroPlay.setEnabled(false);
		}
		if (btnHeroFavorite != null) {
			btnHeroFavorite.setVisibility(View.GONE);
			btnHeroFavorite.setEnabled(false);
		}

		if (indicatorCapsule != null) indicatorCapsule.setVisibility(View.INVISIBLE);
		if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.GONE);

		bindHorizontalPosterRecycler(recyclerPopular, dummyList, 114, 162);
		bindHorizontalPosterRecycler(listPopular, dummyList, 114, 162);
		bindHorizontalPosterRecycler(recyclerMovieDonghua, dummyList, 114, 162);
		bindHorizontalPosterRecycler(recyclerCompletedDonghua, dummyList, 114, 162);
		bindHorizontalPosterRecycler(recyclerComedyDonghua, dummyList, 114, 162);
		bindHorizontalPosterRecycler(recyclerMysteryDonghua, dummyList, 114, 162);

		if (recyclerSearchList != null) {
			recyclerSearchList.setLayoutManager(new LinearLayoutManager(this));
			recyclerSearchList.setAdapter(new ListPopularAdapter(dummyList));
		}

		dongflix_req.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/", "", _dongflix_req_request_listener);
	}

	private void setupRealTimeBlurView(View blurViewTarget, ViewGroup rootContainer, float radius) {
		if (!(blurViewTarget instanceof BlurView) || rootContainer == null) return;
		BlurView bv = (BlurView) blurViewTarget;
		Drawable windowBackground = getWindow().getDecorView().getBackground();
		bv.setupWith(rootContainer, new RenderScriptBlur(this))
			.setFrameClearDrawable(windowBackground)
			.setBlurRadius(radius);
		bv.setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(View view, Outline outline) {
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 1000f);
			}
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
			glass.setStroke((int)(1 * d), Color.parseColor("#33FFFFFF"));
			blurTarget.setBackground(glass);
		}
		if (containerTarget != null) {
			GradientDrawable borderOverlay = new GradientDrawable();
			borderOverlay.setColor(Color.TRANSPARENT);
			borderOverlay.setCornerRadius(1000f);
			borderOverlay.setStroke((int)(1 * d), Color.parseColor("#33FFFFFF"));
			containerTarget.setForeground(borderOverlay);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				containerTarget.setElevation(18f);
			}
		}
	}

	private void setupIos26DraggableNavbar() {
		if (navbarExpanded == null || tabBeranda == null) return;
		if (navbarExpanded.getChildCount() < 2) return;

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

			FrameLayout.LayoutParams pillLp = new FrameLayout.LayoutParams(
				(int) (96 * d),
				(int) (54 * d),
				Gravity.CENTER_VERTICAL | Gravity.START
			);
			pillLp.leftMargin = (int) (5 * d);
			liquidDragPill.setLayoutParams(pillLp);
			navbarExpanded.addView(liquidDragPill, 1);
		}

		tabsRow.post(new Runnable() {
			@Override
			public void run() {
				snapPillToTab(activeNavTabIndex, false);
			}
		});
	}

	private void snapPillToTab(int idx, boolean animate) {
		if (liquidDragPill == null || tabBeranda == null || tabPustaka == null || tabAkun == null) return;
		View targetTab = (idx == 1) ? tabPustaka : (idx == 2 ? tabAkun : tabBeranda);
		float d = getResources().getDisplayMetrics().density;

		int targetW = targetTab.getWidth();
		if (targetW <= 0 && navbarExpanded != null && navbarExpanded.getWidth() > 0) {
			targetW = (navbarExpanded.getWidth() - (int) (10 * d)) / 3;
		}
		if (targetW <= 0) targetW = (int) (96 * d);

		ViewGroup.LayoutParams lp = liquidDragPill.getLayoutParams();
		if (lp.width != targetW) {
			lp.width = targetW;
			liquidDragPill.setLayoutParams(lp);
		}

		float targetTx = idx * targetW;
		if (targetTab.getLeft() > 0) {
			targetTx = Math.max(0f, targetTab.getLeft() - (5 * d));
		}

		if (animate) {
			liquidDragPill.animate()
				.translationX(targetTx)
				.setInterpolator(new OvershootInterpolator(1.05f))
				.setDuration(220)
				.start();
		} else {
			liquidDragPill.setTranslationX(targetTx);
		}
	}

	private void selectNavTabByIndex(int idx, boolean animatePill) {
		activeNavTabIndex = idx;
		snapPillToTab(idx, animatePill);

		final View pageHome = findViewById(R.id.main_scroll_view);
		final View pageSearch = findViewById(R.id.layout_search);
		final View pageFavorite = findViewById(R.id.layout_favorite);
		final View pageProfile = findViewById(R.id.layout_profile);

		if (pageHome != null) pageHome.setVisibility(View.GONE);
		if (pageSearch != null) pageSearch.setVisibility(View.GONE);
		if (pageFavorite != null) pageFavorite.setVisibility(View.GONE);
		if (pageProfile != null) pageProfile.setVisibility(View.GONE);

		final int colorActive = Color.WHITE;
		final int colorInactive = Color.parseColor("#94A3B8");

		if (imgHome != null) imgHome.setColorFilter(idx == 0 ? colorActive : colorInactive);
		if (imgFavorite != null) imgFavorite.setColorFilter(idx == 1 ? colorActive : colorInactive);
		if (imgProfile != null) imgProfile.setColorFilter(idx == 2 ? colorActive : colorInactive);
		if (imgSearch != null) imgSearch.setColorFilter(colorInactive);

		if (txtHome != null) txtHome.setTextColor(idx == 0 ? colorActive : colorInactive);
		if (txtFavorite != null) txtFavorite.setTextColor(idx == 1 ? colorActive : colorInactive);
		if (txtProfile != null) txtProfile.setTextColor(idx == 2 ? colorActive : colorInactive);

		hideKeyboard();
		if (idx == 0) {
			if (pageHome != null) pageHome.setVisibility(View.VISIBLE);
			setNavbarCompactState(false);
		} else if (idx == 1) {
			if (pageFavorite != null) pageFavorite.setVisibility(View.VISIBLE);
			renderActivePustakaTab();
		} else {
			if (pageProfile != null) pageProfile.setVisibility(View.VISIBLE);
		}
	}

	public void _UI() {
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(Color.TRANSPARENT);
		}

		final float density = getResources().getDisplayMetrics().density;
		final View topHeaderBar = findViewById(R.id.relativelayout69);
		final View navWrapper = findViewById(R.id.navbar_wrapper);
		final View rootContainer = findViewById(R.id.root_main_container);

		if (rootContainer != null) {
			ViewCompat.setOnApplyWindowInsetsListener(rootContainer, new androidx.core.view.OnApplyWindowInsetsListener() {
				@Override
				public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
					int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
					int bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

					if (topHeaderBar != null) {
						topHeaderBar.setBackgroundColor(Color.TRANSPARENT);
						topHeaderBar.setPadding(
							(int) (16 * density),
							topInset + (int) (8 * density),
							(int) (16 * density),
							(int) (10 * density)
						);
					}
					if (pullRefreshIndicator != null && pullRefreshIndicator.getLayoutParams() instanceof FrameLayout.LayoutParams) {
						FrameLayout.LayoutParams rlp = (FrameLayout.LayoutParams) pullRefreshIndicator.getLayoutParams();
						rlp.topMargin = topInset + (int) (12 * density);
						pullRefreshIndicator.setLayoutParams(rlp);
					}
					if (navWrapper != null) {
						navWrapper.setPadding(
							(int) (14 * density),
							0,
							(int) (14 * density),
							bottomInset + (int) (16 * density)
						);
					}
					return insets;
				}
			});
			ViewCompat.requestApplyInsets(rootContainer);
		}

		// Hapus background & stroke pada tombol notifikasi dan pesan
		if (btnTopNotif != null) {
			btnTopNotif.setBackgroundColor(Color.TRANSPARENT);
			btnTopNotif.setForeground(null);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				btnTopNotif.setElevation(0f);
			}
		}
		if (btnTopChat != null) {
			btnTopChat.setBackgroundColor(Color.TRANSPARENT);
			btnTopChat.setForeground(null);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				btnTopChat.setElevation(0f);
			}
		}
		if (imgTopNotif != null) imgTopNotif.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		if (imgTopChat != null) imgTopChat.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		if (imgHeroFavorite != null) imgHeroFavorite.setColorFilter(Color.WHITE);

		int statusBarHeight = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			statusBarHeight = getResources().getDimensionPixelSize(resourceId);
		}

		if (searchBarContainer != null) {
			LinearLayout.LayoutParams sbLp = (LinearLayout.LayoutParams) searchBarContainer.getLayoutParams();
			sbLp.topMargin = statusBarHeight + (int) (12 * density);
			searchBarContainer.setLayoutParams(sbLp);

			GradientDrawable sbBg = new GradientDrawable();
			sbBg.setColor(Color.parseColor("#12141A"));
			sbBg.setCornerRadius(100f);
			sbBg.setStroke((int) (1 * density), Color.parseColor("#26FFFFFF"));
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
			GradientDrawable topGradient = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { Color.parseColor("#B3000000"), Color.TRANSPARENT }
			);
			headerShadow.setBackground(topGradient);
		}

		View heroShadow = findViewById(R.id.hero_shadow);
		if (heroShadow != null) {
			GradientDrawable bottomGradient = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { Color.TRANSPARENT, Color.parseColor("#FF000000") }
			);
			heroShadow.setBackground(bottomGradient);
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

	@Override
	public void onDestroy() {
		stopHeroAutoSlider();
		super.onDestroy();
	}

	private void setupScrollResponsiveNavbar() {
		NestedScrollView mainScroll = findViewById(R.id.main_scroll_view);
		if (mainScroll != null) {
			mainScroll.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
				@Override
				public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
					if (!DongflixPlayerManager.getInstance().isMiniPlayerActive) {
						resetNavbarToExpanded();
						return;
					}

					int dy = scrollY - oldScrollY;
					if (scrollY <= 20 || dy < -12) {
						setNavbarCompactState(false);
					} else if (dy > 14 && scrollY > 80) {
						setNavbarCompactState(true);
					}
				}
			});
		}
	}

	public void resetNavbarToExpanded() {
		setNavbarCompactState(false);
	}

	private void setNavbarCompactState(boolean compact) {
		if (compact && !DongflixPlayerManager.getInstance().isMiniPlayerActive) {
			compact = false;
		}
		if (isNavbarCompact == compact) return;
		isNavbarCompact = compact;
		float d = getResources().getDisplayMetrics().density;

		if (compact) {
			if (navbarExpanded != null) navbarExpanded.setVisibility(View.GONE);
			if (btnCompactHome != null) btnCompactHome.setVisibility(View.VISIBLE);
			if (btnCircleSearch != null && btnCircleSearch.getLayoutParams() instanceof LinearLayout.LayoutParams) {
				LinearLayout.LayoutParams slp = (LinearLayout.LayoutParams) btnCircleSearch.getLayoutParams();
				slp.leftMargin = 0;
				btnCircleSearch.setLayoutParams(slp);
			}
		} else {
			if (btnCompactHome != null) btnCompactHome.setVisibility(View.GONE);
			if (miniPlayerInlineSlot != null) miniPlayerInlineSlot.setVisibility(View.GONE);
			if (navbarExpanded != null) navbarExpanded.setVisibility(View.VISIBLE);
			if (btnCircleSearch != null && btnCircleSearch.getLayoutParams() instanceof LinearLayout.LayoutParams) {
				LinearLayout.LayoutParams slp = (LinearLayout.LayoutParams) btnCircleSearch.getLayoutParams();
				slp.leftMargin = (int) (10 * d);
				btnCircleSearch.setLayoutParams(slp);
			}
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
				@Override
				public void onClick(View v) {
					selectNavTabByIndex(0, true);
				}
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

	public void setGridViewHeightBasedOnChildren(final GridView gridView, final int columns) {
		gridView.post(new Runnable() {
			@Override
			public void run() {
				if (gridView.getAdapter() == null || gridView.getAdapter().getCount() == 0) return;

				int items = gridView.getAdapter().getCount();
				int rows = (int) Math.ceil((double) items / columns);
				float density = getResources().getDisplayMetrics().density;

				int horizontalSpacingPx = (int) (12 * density);
				int verticalSpacingPx = (int) (16 * density);
				int paddingHorizontal = gridView.getPaddingLeft() + gridView.getPaddingRight();
				int paddingBottomPx = (int) (16 * density);

				int availableWidth = gridView.getWidth() - paddingHorizontal - (horizontalSpacingPx * (columns - 1));
				int columnWidth = availableWidth / columns;

				View itemView = gridView.getAdapter().getView(0, null, gridView);
				itemView.measure(
					View.MeasureSpec.makeMeasureSpec(columnWidth, View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
				);

				int exactItemHeight = itemView.getMeasuredHeight();
				ViewGroup.LayoutParams params = gridView.getLayoutParams();
				params.height = (exactItemHeight * rows) + (verticalSpacingPx * Math.max(0, rows - 1)) + paddingBottomPx;
				gridView.setLayoutParams(params);
				gridView.requestLayout();
			}
		});
	}

	public class HorizontalPosterAdapter extends RecyclerView.Adapter<HorizontalPosterAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> _data;
		private final int cardWidthDp;
		private final int cardHeightDp;

		public HorizontalPosterAdapter(ArrayList<HashMap<String, Object>> _arr, int widthDp, int heightDp) {
			_data = _arr;
			this.cardWidthDp = widthDp;
			this.cardHeightDp = heightDp;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			float d = parent.getResources().getDisplayMetrics().density;
			int wPx = (int) (cardWidthDp * d);
			int hPx = (int) (cardHeightDp * d);

			LinearLayout root = new LinearLayout(parent.getContext());
			root.setOrientation(LinearLayout.VERTICAL);
			RecyclerView.LayoutParams rootLp = new RecyclerView.LayoutParams(
				wPx,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			rootLp.rightMargin = (int) (10 * d);
			root.setLayoutParams(rootLp);

			androidx.cardview.widget.CardView coverCard = new androidx.cardview.widget.CardView(parent.getContext());
			coverCard.setLayoutParams(new LinearLayout.LayoutParams(wPx, hPx));
			coverCard.setRadius(16 * d);
			coverCard.setCardElevation(0f);
			coverCard.setCardBackgroundColor(Color.parseColor("#12141A"));
			coverCard.setTag("cover_card");

			FrameLayout coverOverlayContainer = new FrameLayout(parent.getContext());
			coverOverlayContainer.setLayoutParams(new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			));

			ImageView imgCover = new ImageView(parent.getContext());
			imgCover.setLayoutParams(new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			));
			imgCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imgCover.setTag("img_cover");
			coverOverlayContainer.addView(imgCover);

			View bottomScrim = new View(parent.getContext());
			FrameLayout.LayoutParams scrimLp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				(int) (60 * d),
				Gravity.BOTTOM
			);
			bottomScrim.setLayoutParams(scrimLp);
			bottomScrim.setTag("bottom_scrim");
			coverOverlayContainer.addView(bottomScrim);

			TextView tvStatus = new TextView(parent.getContext());
			FrameLayout.LayoutParams statusLp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				Gravity.TOP | Gravity.START
			);
			statusLp.setMargins((int) (8 * d), (int) (8 * d), (int) (8 * d), 0);
			tvStatus.setLayoutParams(statusLp);
			tvStatus.setPadding((int) (8 * d), (int) (3 * d), (int) (8 * d), (int) (3 * d));
			stylePlainOutfitText(tvStatus, 9.5f, Color.WHITE);
			tvStatus.setMaxLines(1);
			tvStatus.setTag("text_status");
			coverOverlayContainer.addView(tvStatus);

			TextView tvEpisode = new TextView(parent.getContext());
			FrameLayout.LayoutParams epLp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.START
			);
			epLp.setMargins((int) (9 * d), 0, (int) (9 * d), (int) (8 * d));
			tvEpisode.setLayoutParams(epLp);
			stylePlainOutfitText(tvEpisode, 10.5f, Color.WHITE);
			tvEpisode.setMaxLines(1);
			tvEpisode.setEllipsize(TextUtils.TruncateAt.END);
			tvEpisode.setTag("text_episode");
			coverOverlayContainer.addView(tvEpisode);

			coverCard.addView(coverOverlayContainer);
			root.addView(coverCard);

			TextView tvTitle = new TextView(parent.getContext());
			LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				(int) (36 * d)
			);
			titleLp.topMargin = (int) (7 * d);
			tvTitle.setLayoutParams(titleLp);
			stylePlainOutfitText(tvTitle, 12.5f, Color.WHITE);
			tvTitle.setLineSpacing(0f, 1.12f);
			tvTitle.setMaxLines(2);
			tvTitle.setEllipsize(TextUtils.TruncateAt.END);
			tvTitle.setTag("text_title");
			root.addView(tvTitle);

			return new ViewHolder(root);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			float d = _view.getResources().getDisplayMetrics().density;

			if (_view.getLayoutParams() instanceof RecyclerView.LayoutParams) {
				RecyclerView.LayoutParams rlp = (RecyclerView.LayoutParams) _view.getLayoutParams();
				rlp.rightMargin = (_position == _data.size() - 1) ? 0 : (int) (10 * d);
				_view.setLayoutParams(rlp);
			}

			ImageView imgCover = _view.findViewWithTag("img_cover");
			View bottomScrim = _view.findViewWithTag("bottom_scrim");
			TextView textStatus = _view.findViewWithTag("text_status");
			TextView textEpisode = _view.findViewWithTag("text_episode");
			TextView textTitle = _view.findViewWithTag("text_title");

			final HashMap<String, Object> item = _data.get(_position);

			if (item.containsKey("is_loading")) {
				if (imgCover != null) {
					imgCover.setImageResource(0);
					imgCover.setBackgroundColor(Color.parseColor("#12141A"));
				}
				if (bottomScrim != null) bottomScrim.setVisibility(View.GONE);
				if (textStatus != null) {
					textStatus.setText("");
					textStatus.setVisibility(View.INVISIBLE);
				}
				if (textEpisode != null) {
					textEpisode.setText("");
					textEpisode.setVisibility(View.INVISIBLE);
				}
				if (textTitle != null) {
					textTitle.setText("");
					textTitle.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 8 * d));
				}
				_view.setOnClickListener(null);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (bottomScrim != null) {
					bottomScrim.setVisibility(View.VISIBLE);
					bottomScrim.setBackground(new GradientDrawable(
						GradientDrawable.Orientation.BOTTOM_TOP,
						new int[]{Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT}
					));
				}
				if (textStatus != null) {
					textStatus.setVisibility(View.VISIBLE);
					GradientDrawable statusBg = new GradientDrawable();
					statusBg.setCornerRadius(100f);
					statusBg.setColor(Color.parseColor("#3B82F6"));
					textStatus.setBackground(statusBg);
					textStatus.setText(item.get("status") != null ? item.get("status").toString() : "");
				}
				if (textEpisode != null) {
					textEpisode.setVisibility(View.VISIBLE);
					textEpisode.setText(item.get("episode") != null ? item.get("episode").toString() : "");
				}
				if (textTitle != null) {
					textTitle.setBackgroundColor(Color.TRANSPARENT);
					textTitle.setText(item.get("title") != null ? item.get("title").toString() : "");
				}

				String cover = item.get("cover") != null ? item.get("cover").toString().trim() : "";
				if (cover.contains("i.ibb.co.com")) {
					cover = cover.replace("i.ibb.co.com", "i.ibb.co");
				}
				if (cover.startsWith("http://")) {
					cover = cover.replace("http://", "https://");
				}

				if (imgCover != null && !cover.isEmpty()) {
					Glide.with(getApplicationContext()).load(Uri.parse(cover)).into(imgCover);
				}

				_view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String slug = item.get("slug") != null ? item.get("slug").toString() : "";
						openWatchBySlug(slug);
					}
				});
			}
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

	public class Gridview1Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		public Gridview1Adapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }

		@Override public int getCount() { return _data.size(); }
		@Override public HashMap<String, Object> getItem(int _index) { return _data.get(_index); }
		@Override public long getItemId(int _index) { return _index; }

		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater inflater = getLayoutInflater();
			View _view = inflater.inflate(R.layout.custom_anime, _container, false);
			float d = _view.getResources().getDisplayMetrics().density;

			TextView textTitle = _view.findViewById(R.id.text_title);
			TextView textStatus = _view.findViewById(R.id.text_status);
			TextView textEpisode = _view.findViewById(R.id.text_episode);
			ImageView imgCover = _view.findViewById(R.id.img_cover);
			View view2 = _view.findViewById(R.id.view2);

			HashMap<String, Object> item = _data.get(_position);

			if (item.containsKey("is_loading")) {
				if (textTitle != null) {
					textTitle.setText("");
					textTitle.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 8 * d));
				}
				if (textStatus != null) {
					textStatus.setText("");
					textStatus.setVisibility(View.INVISIBLE);
				}
				if (textEpisode != null) {
					textEpisode.setText("");
					textEpisode.setVisibility(View.INVISIBLE);
				}
				if (imgCover != null) {
					imgCover.setImageResource(0);
					imgCover.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 16 * d));
				}
				if (view2 != null) view2.setVisibility(View.GONE);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (textTitle != null) textTitle.setBackgroundColor(Color.TRANSPARENT);
				if (textEpisode != null) {
					textEpisode.setVisibility(View.VISIBLE);
					textEpisode.setBackgroundColor(Color.TRANSPARENT);
				}
				if (view2 != null) view2.setVisibility(View.VISIBLE);

				if (textStatus != null) {
					textStatus.setVisibility(View.VISIBLE);
					textStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF3B82F6));
				}

				if (view2 != null) {
					GradientDrawable gradient = new GradientDrawable(
						GradientDrawable.Orientation.BOTTOM_TOP,
						new int[] { Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT }
					);
					view2.setBackground(gradient);
				}

				if (textTitle != null) textTitle.setText(_data.get(_position).get("title").toString());
				if (textStatus != null) textStatus.setText(_data.get(_position).get("status").toString());
				if (textEpisode != null) textEpisode.setText(_data.get(_position).get("episode").toString());

				String cover = _data.get(_position).get("cover") != null ? _data.get(_position).get("cover").toString().trim() : "";
				if (cover.contains("i.ibb.co.com")) {
					cover = cover.replace("i.ibb.co.com", "i.ibb.co");
				}
				if (cover.startsWith("http://")) {
					cover = cover.replace("http://", "https://");
				}

				if (imgCover != null) {
					Glide.with(getApplicationContext()).load(Uri.parse(cover)).into(imgCover);
				}
			}
			return _view;
		}
	}

	public class ListPopularAdapter extends RecyclerView.Adapter<ListPopularAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> _data;
		public ListPopularAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.item_popular_donghua, parent, false);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			float d = _view.getResources().getDisplayMetrics().density;

			TextView textPopularTitle = _view.findViewById(getResources().getIdentifier("text_popular_title", "id", getPackageName()));
			TextView textPopularDesc = _view.findViewById(getResources().getIdentifier("text_popular_desc", "id", getPackageName()));
			TextView textPopularGenre = _view.findViewById(getResources().getIdentifier("text_popular_genre", "id", getPackageName()));
			TextView textPopularStatus = _view.findViewById(getResources().getIdentifier("text_popular_status", "id", getPackageName()));
			ImageView imgPopularCover = _view.findViewById(getResources().getIdentifier("img_popular_cover", "id", getPackageName()));
			View linear35 = _view.findViewById(getResources().getIdentifier("linear35", "id", getPackageName()));

			HashMap<String, Object> item = _data.get(_position);

			if (item.containsKey("is_loading")) {
				if (textPopularTitle != null) {
					textPopularTitle.setText("");
					textPopularTitle.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 8 * d));
				}
				if (textPopularDesc != null) {
					textPopularDesc.setText("");
					textPopularDesc.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 6 * d));
				}
				if (textPopularGenre != null) {
					textPopularGenre.setText("");
					textPopularGenre.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 6 * d));
				}
				if (textPopularStatus != null) {
					textPopularStatus.setText("");
					textPopularStatus.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 100f));
				}
				if (imgPopularCover != null) {
					imgPopularCover.setImageResource(0);
					imgPopularCover.setBackground(createEmptyPlaceholderDrawable(Color.parseColor("#12141A"), 16 * d));
				}
				View clickTarget = (linear35 != null) ? linear35 : _view;
				clickTarget.setOnClickListener(null);
			} else {
				if (textPopularTitle != null) textPopularTitle.setBackgroundColor(Color.TRANSPARENT);
				if (textPopularDesc != null) textPopularDesc.setBackgroundColor(Color.TRANSPARENT);
				if (textPopularGenre != null) textPopularGenre.setBackgroundColor(Color.TRANSPARENT);
				if (imgPopularCover != null) imgPopularCover.setBackgroundColor(Color.TRANSPARENT);

				if (textPopularStatus != null) textPopularStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF111111));
				if (textPopularTitle != null) textPopularTitle.setText(item.get("title").toString());
				if (textPopularDesc != null) textPopularDesc.setText(cleanText(item.get("synopsis").toString()));
				if (textPopularGenre != null) textPopularGenre.setText(item.get("genres").toString().replace("[", "").replace("]", ""));

				if (textPopularStatus != null) {
					textPopularStatus.setText(item.get("episode").toString().concat(" / ".concat(item.get("status").toString())));
				}
				if (imgPopularCover != null) {
					Glide.with(getApplicationContext()).load(Uri.parse(item.get("cover").toString())).into(imgPopularCover);
				}

				View clickTarget = (linear35 != null) ? linear35 : _view;
				clickTarget.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String slug = item.get("slug") != null ? item.get("slug").toString() : "";
						openWatchBySlug(slug);
					}
				});
			}
		}

		@Override
		public int getItemCount() { return _data.size(); }

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) { super(v); }
		}
	}
}
