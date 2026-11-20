package com.dongflix;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class CatalogActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";
	private static final String CATALOG_URL = "https://dongflix-api1.zahrulmo01.workers.dev/";

	private static final int FILTER_SEMUA = 0;
	private static final int FILTER_DONGHUA = 1;

	private static final int STATUS_ALL = 0;
	private static final int STATUS_ONGOING = 1;
	private static final int STATUS_COMPLETED = 2;

	private static final int SORT_DEFAULT = 0;
	private static final int SORT_TITLE_ASC = 1;
	private static final int SORT_TITLE_DESC = 2;
	private static final int SORT_POPULAR = 3;

	private static final String[] BASE_CATEGORIES = {
		"Action", "Adventure", "Comedy", "Cultivation", "Demons",
		"Drama", "Fantasy", "Game", "Historical", "Isekai",
		"Martial Arts", "Mystery", "Reincarnation", "Romance",
		"Sci-Fi", "Slice of Life", "Super Power", "Supernatural",
		"Suspense", "Thriller", "Time Travel", "Urban Fantasy"
	};

	private FrameLayout catalogRoot;
	private LinearLayout catalogHeader;
	private NestedScrollView catalogScrollView;
	private GridView gridCatalog;
	private LinearLayout layoutLoadingMore;
	private View btnBack;
	private FrameLayout btnScrollTop;

	private TextView filterMenu;
	private TextView filterSemua;
	private TextView filterDonghua;
	private TextView textCatalogTitle;
	private TextView textCatalogSubtitle;

	private RequestNetwork req;
	private RequestNetwork.RequestListener reqListener;

	private ArrayList<HashMap<String, Object>> allDataList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> filteredDataList = new ArrayList<>();
	private final ArrayList<HashMap<String, Object>> displayedList = new ArrayList<>();
	private final List<String> availableCategories = new ArrayList<>();
	private final Map<String, Long> viewsTotalMap = new HashMap<>();
	private CatalogAdapter adapter;

	private DatabaseReference viewsTotalRef;
	private ValueEventListener viewsTotalListener;

	private int activeFilter = FILTER_SEMUA;
	private int activeStatusFilter = STATUS_ALL;
	private int activeSortMode = SORT_DEFAULT;
	private String selectedCategory = "Semua Kategori";
	private String incomingFilterType = "all";
	private String baseHeaderTitle = "Katalog Donghua";

	private int currentLimit = 18;
	private final int PAGE_SIZE = 18;
	private int lastMeasuredRowCount = -1;
	private int lastMeasuredItemHeight = -1;
	private boolean isLoadingMore = false;
	private boolean isScrollTopVisible = false;
	private final Handler paginationHandler = new Handler(Looper.getMainLooper());

	private ValueAnimator catalogShimmerAnimator;
	private float catalogShimmerPhase = 0f;
	private boolean isCatalogLoading = true;
	private Typeface outfitFont;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_catalog);

		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window w = getWindow();
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
			w.setStatusBarColor(Color.TRANSPARENT);
		}

		catalogRoot = findViewById(R.id.catalog_root);
		catalogHeader = findViewById(R.id.catalog_header);
		catalogScrollView = findViewById(R.id.catalog_scroll_view);
		gridCatalog = findViewById(R.id.grid_catalog);
		layoutLoadingMore = findViewById(R.id.layout_loading_more);
		btnBack = findViewById(R.id.btn_catalog_back);
		btnScrollTop = findViewById(R.id.btn_scroll_top);

		filterMenu = findViewById(R.id.filter_menu);
		filterSemua = findViewById(R.id.filter_semua);
		filterDonghua = findViewById(R.id.filter_donghua);
		textCatalogTitle = findViewById(R.id.text_catalog_title);
		textCatalogSubtitle = findViewById(R.id.text_catalog_subtitle);

		initDefaultCategories();
		applyIncomingMainSectionPreset();

		adapter = new CatalogAdapter(displayedList);
		gridCatalog.setAdapter(adapter);

		applyHeaderGlassStyle();
		setupScrollTopButton();
		setupFilterTabs();
		setupInfiniteScroll();
		updateBottomSpacingForMiniPlayer();
		applyOutfitFontsWithoutBold();

		btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		gridCatalog.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return event.getAction() == MotionEvent.ACTION_MOVE;
			}
		});

		gridCatalog.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position < displayedList.size()) {
					HashMap<String, Object> item = displayedList.get(position);
					if (item.containsKey("is_loading")) return;

					String slug = safeStr(item, "slug", "");
					if (!slug.isEmpty()) {
						Intent watchIntent = new Intent(CatalogActivity.this, WatchActivity.class);
						watchIntent.putExtra("slug", slug);
						startActivity(watchIntent);
						overridePendingTransition(R.anim.slide_up_enter, R.anim.no_anim);
					} else {
						Intent intent = new Intent(CatalogActivity.this, DetailActivity.class);
						intent.putExtra("url", safeStr(item, "url", ""));
						intent.putExtra("title", safeStr(item, "title", ""));
						intent.putExtra("image", safeStr(item, "cover", ""));
						startActivity(intent);
					}
				}
			}
		});

		attachFirebaseViewsListener();

		String cachedJson = DongflixPlayerManager.getInstance().cachedAllCatalogJson;
		if (cachedJson != null && !cachedJson.trim().isEmpty()) {
			ArrayList<HashMap<String, Object>> cachedList = parseCatalogJson(cachedJson);
			if (!cachedList.isEmpty()) {
				isCatalogLoading = false;
				allDataList = cachedList;
				rebuildCategoriesFromCatalog(allDataList);
				applyFilterAndResetPagination();
			} else {
				showCatalogLoadingPlaceholders();
			}
		} else {
			showCatalogLoadingPlaceholders();
		}

		req = new RequestNetwork(this);
		reqListener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String tag, String response, HashMap<String, Object> headers) {
				if (isFinishing() || isDestroyed()) return;
				stopCatalogShimmer();
				DongflixPlayerManager.getInstance().cachedAllCatalogJson = response;
				ArrayList<HashMap<String, Object>> parsed = parseCatalogJson(response);
				if (!parsed.isEmpty()) {
					allDataList = parsed;
					rebuildCategoriesFromCatalog(allDataList);
					applyFilterAndResetPagination();
				}
			}

			@Override
			public void onErrorResponse(String tag, String message) {
				if (isFinishing() || isDestroyed()) return;
				stopCatalogShimmer();
			}
		};

		req.startRequestNetwork("GET", CATALOG_URL, "", reqListener);
	}

	private void applyIncomingMainSectionPreset() {
		String extraTitle = getIntent().getStringExtra("section_title");
		String extraFilter = getIntent().getStringExtra("filter_type");

		if (extraTitle != null && !extraTitle.trim().isEmpty()) {
			baseHeaderTitle = extraTitle.trim();
		}
		if (textCatalogTitle != null) {
			textCatalogTitle.setText(baseHeaderTitle);
		}

		if (extraFilter == null || extraFilter.trim().isEmpty()) {
			incomingFilterType = "all";
			return;
		}

		incomingFilterType = extraFilter.trim().toLowerCase(Locale.US);
		switch (incomingFilterType) {
			case "popular_dongflix":
				activeSortMode = SORT_POPULAR;
				break;
			case "romance":
				selectedCategory = "Romance";
				break;
			case "cultivation":
				selectedCategory = "Cultivation";
				break;
			case "completed":
				activeStatusFilter = STATUS_COMPLETED;
				break;
			case "comedy":
				selectedCategory = "Comedy";
				break;
			case "mystery":
				selectedCategory = "Mystery";
				break;
			case "movie":
				// Ditangani di matchesActiveFilter via incomingFilterType = "movie"
				break;
			default:
				incomingFilterType = "all";
				break;
		}
	}

	private ArrayList<HashMap<String, Object>> parseCatalogJson(String json) {
		try {
			ArrayList<HashMap<String, Object>> parsed = new Gson().fromJson(
				json, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType()
			);
			return parsed != null ? parsed : new ArrayList<HashMap<String, Object>>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private void attachFirebaseViewsListener() {
		detachFirebaseViewsListener();
		viewsTotalRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("views_total");
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
				if (!isCatalogLoading && !allDataList.isEmpty() && activeSortMode == SORT_POPULAR) {
					applyFilterAndResetPagination();
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		};
		viewsTotalRef.addValueEventListener(viewsTotalListener);
	}

	private void detachFirebaseViewsListener() {
		if (viewsTotalRef != null && viewsTotalListener != null) {
			viewsTotalRef.removeEventListener(viewsTotalListener);
		}
	}

	private String safeStr(Map<String, Object> map, String key, String fallback) {
		if (map == null || key == null) return fallback;
		Object val = map.get(key);
		return val != null ? val.toString() : fallback;
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

	private String cleanText(String input) {
		if (input == null) return "";
		return input
			.replace("â€™", "'")
			.replace("â€œ", "\"")
			.replace("â€", "\"")
			.replace("â€“", "-")
			.replaceAll("\\[[^\\x20-\\x7E]+\\]", "")
			.replaceAll("[^\\x20-\\x7E\\n]", "")
			.trim();
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

	private void initDefaultCategories() {
		availableCategories.clear();
		availableCategories.add("Semua Kategori");
		Collections.addAll(availableCategories, BASE_CATEGORIES);
	}

	private void rebuildCategoriesFromCatalog(List<HashMap<String, Object>> items) {
		Set<String> unique = new LinkedHashSet<>();
		Collections.addAll(unique, BASE_CATEGORIES);

		if (items != null) {
			for (HashMap<String, Object> item : items) {
				Object genresObj = item.get("genres");
				if (genresObj instanceof List) {
					for (Object g : (List<?>) genresObj) {
						if (g != null) {
							String clean = g.toString().trim();
							if (!clean.isEmpty()) unique.add(clean);
						}
					}
				} else if (genresObj != null) {
					String raw = genresObj.toString().replace("[", "").replace("]", "");
					for (String part : raw.split(",")) {
						String clean = part.trim();
						if (!clean.isEmpty()) unique.add(clean);
					}
				}
			}
		}

		List<String> sorted = new ArrayList<>(unique);
		Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

		availableCategories.clear();
		availableCategories.add("Semua Kategori");
		availableCategories.addAll(sorted);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateBottomSpacingForMiniPlayer();
	}

	private void setupScrollTopButton() {
		if (btnScrollTop == null) return;
		BlurView topBlur = findViewById(R.id.scroll_top_blur_bg);
		ImageView topArrow = findViewById(R.id.img_scroll_top_arrow);

		if (topArrow != null) {
			topArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			btnScrollTop.setElevation(16f);
		}
		setupGlassBlur(topBlur, catalogRoot, 20f, Color.parseColor("#33FFFFFF"));

		btnScrollTop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (catalogScrollView != null) {
					catalogScrollView.smoothScrollTo(0, 0);
				}
			}
		});
	}

	private void updateScrollTopVisibility(int scrollY) {
		if (btnScrollTop == null) return;
		float d = getResources().getDisplayMetrics().density;
		boolean shouldShow = scrollY > (int) (320 * d);
		if (shouldShow == isScrollTopVisible) return;
		isScrollTopVisible = shouldShow;

		if (shouldShow) {
			btnScrollTop.setVisibility(View.VISIBLE);
			btnScrollTop.animate().alpha(1f).setDuration(160).start();
		} else {
			btnScrollTop.animate().alpha(0f).setDuration(140).withEndAction(new Runnable() {
				@Override
				public void run() {
					if (!isScrollTopVisible && btnScrollTop != null) {
						btnScrollTop.setVisibility(View.GONE);
					}
				}
			}).start();
		}
	}

	private void setupFilterTabs() {
		updateFilterVisualState();

		if (filterMenu != null) {
			filterMenu.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openFilterBottomSheet();
				}
			});
		}

		if (filterSemua != null) {
			filterSemua.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (activeFilter != FILTER_SEMUA) {
						activeFilter = FILTER_SEMUA;
						updateFilterVisualState();
						applyFilterAndResetPagination();
					}
				}
			});
		}

		if (filterDonghua != null) {
			filterDonghua.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (activeFilter != FILTER_DONGHUA) {
						activeFilter = FILTER_DONGHUA;
						updateFilterVisualState();
						applyFilterAndResetPagination();
					}
				}
			});
		}
	}

	private void openFilterBottomSheet() {
		final float d = getResources().getDisplayMetrics().density;
		final BottomSheetDialog sheet = new BottomSheetDialog(this);

		final int[] tempStatus = {activeStatusFilter};
		final int[] tempSort = {activeSortMode};
		final String[] tempCategory = {selectedCategory};

		ScrollView scroll = new ScrollView(this);
		scroll.setFillViewport(true);
		scroll.setVerticalScrollBarEnabled(false);

		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setPadding((int) (18 * d), (int) (14 * d), (int) (18 * d), (int) (24 * d));
		GradientDrawable sheetBg = new GradientDrawable();
		sheetBg.setColor(Color.parseColor("#0D111A"));
		sheetBg.setCornerRadii(new float[]{
			24 * d, 24 * d,
			24 * d, 24 * d,
			0f, 0f, 0f, 0f
		});
		sheetBg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
		container.setBackground(sheetBg);

		View handle = new View(this);
		LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams((int) (42 * d), (int) (4 * d));
		handleLp.gravity = Gravity.CENTER_HORIZONTAL;
		handleLp.bottomMargin = (int) (14 * d);
		handle.setLayoutParams(handleLp);
		GradientDrawable handleBg = new GradientDrawable();
		handleBg.setColor(Color.parseColor("#38FFFFFF"));
		handleBg.setCornerRadius(100f);
		handle.setBackground(handleBg);
		container.addView(handle);

		TextView tvSheetTitle = new TextView(this);
		tvSheetTitle.setText("Filter & Kategori");
		tvSheetTitle.setTextColor(Color.WHITE);
		tvSheetTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
		tvSheetTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		stLp.bottomMargin = (int) (16 * d);
		tvSheetTitle.setLayoutParams(stLp);
		container.addView(tvSheetTitle);

		container.addView(buildSheetSectionLabel("Status Tayang", d));
		final String[] statusLabels = {"Semua Status", "Ongoing", "Completed"};
		final List<TextView> statusChips = new ArrayList<>();
		HorizontalScrollView statusScroll = new HorizontalScrollView(this);
		statusScroll.setHorizontalScrollBarEnabled(false);
		LinearLayout.LayoutParams sScrollLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		sScrollLp.bottomMargin = (int) (16 * d);
		statusScroll.setLayoutParams(sScrollLp);
		LinearLayout statusRow = new LinearLayout(this);
		statusRow.setOrientation(LinearLayout.HORIZONTAL);

		for (int i = 0; i < statusLabels.length; i++) {
			final int idx = i;
			final TextView chip = createSheetChip(statusLabels[i], idx == tempStatus[0], d);
			chip.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					tempStatus[0] = idx;
					refreshChipList(statusChips, idx);
				}
			});
			statusChips.add(chip);
			statusRow.addView(chip);
		}
		statusScroll.addView(statusRow);
		container.addView(statusScroll);

		container.addView(buildSheetSectionLabel("Urutkan", d));
		final String[] sortLabels = {"Terbaru", "Judul A - Z", "Judul Z - A", "Populer"};
		final List<TextView> sortChips = new ArrayList<>();
		HorizontalScrollView sortScroll = new HorizontalScrollView(this);
		sortScroll.setHorizontalScrollBarEnabled(false);
		LinearLayout.LayoutParams soScrollLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		soScrollLp.bottomMargin = (int) (16 * d);
		sortScroll.setLayoutParams(soScrollLp);
		LinearLayout sortRow = new LinearLayout(this);
		sortRow.setOrientation(LinearLayout.HORIZONTAL);

		for (int i = 0; i < sortLabels.length; i++) {
			final int idx = i;
			final TextView chip = createSheetChip(sortLabels[i], idx == tempSort[0], d);
			chip.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					tempSort[0] = idx;
					refreshChipList(sortChips, idx);
				}
			});
			sortChips.add(chip);
			sortRow.addView(chip);
		}
		sortScroll.addView(sortRow);
		container.addView(sortScroll);

		container.addView(buildSheetSectionLabel("Kategori Lengkap", d));
		final List<TextView> categoryChips = new ArrayList<>();
		LinearLayout currentCatRow = null;

		for (int i = 0; i < availableCategories.size(); i++) {
			final String catName = availableCategories.get(i);
			if (i % 3 == 0) {
				currentCatRow = new LinearLayout(this);
				currentCatRow.setOrientation(LinearLayout.HORIZONTAL);
				LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				rLp.bottomMargin = (int) (8 * d);
				currentCatRow.setLayoutParams(rLp);
				container.addView(currentCatRow);
			}

			final TextView catChip = new TextView(this);
			catChip.setText(catName);
			catChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			catChip.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			catChip.setGravity(Gravity.CENTER);
			catChip.setSingleLine(true);
			catChip.setPadding((int) (8 * d), (int) (8 * d), (int) (8 * d), (int) (8 * d));
			LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, (int) (36 * d), 1f);
			if (i % 3 != 2) cLp.rightMargin = (int) (8 * d);
			catChip.setLayoutParams(cLp);
			styleSheetChip(catChip, catName.equalsIgnoreCase(tempCategory[0]), d);

			catChip.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					tempCategory[0] = catName;
					for (TextView ch : categoryChips) {
						boolean match = ch.getText().toString().equalsIgnoreCase(catName);
						styleSheetChip(ch, match, d);
					}
				}
			});

			categoryChips.add(catChip);
			if (currentCatRow != null) {
				currentCatRow.addView(catChip);
			}
		}

		int remainder = availableCategories.size() % 3;
		if (remainder != 0 && currentCatRow != null) {
			int fillers = 3 - remainder;
			for (int f = 0; f < fillers; f++) {
				View spacer = new View(this);
				LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, (int) (36 * d), 1f);
				if (f != fillers - 1) spLp.rightMargin = (int) (8 * d);
				spacer.setLayoutParams(spLp);
				currentCatRow.addView(spacer);
			}
		}

		LinearLayout actionRow = new LinearLayout(this);
		actionRow.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		actLp.topMargin = (int) (14 * d);
		actionRow.setLayoutParams(actLp);

		TextView btnReset = new TextView(this);
		btnReset.setText("Reset");
		btnReset.setTextColor(Color.parseColor("#CBD5E1"));
		btnReset.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnReset.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnReset.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(0, (int) (44 * d), 1f);
		resetLp.rightMargin = (int) (10 * d);
		btnReset.setLayoutParams(resetLp);
		GradientDrawable resetBg = new GradientDrawable();
		resetBg.setColor(Color.parseColor("#181E2C"));
		resetBg.setCornerRadius(100f);
		resetBg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
		btnReset.setBackground(resetBg);
		btnReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				incomingFilterType = "all";
				activeStatusFilter = STATUS_ALL;
				activeSortMode = SORT_DEFAULT;
				selectedCategory = "Semua Kategori";
				if (textCatalogTitle != null) textCatalogTitle.setText("Katalog Donghua");
				updateFilterVisualState();
				applyFilterAndResetPagination();
				sheet.dismiss();
			}
		});

		TextView btnApply = new TextView(this);
		btnApply.setText("Terapkan");
		btnApply.setTextColor(Color.WHITE);
		btnApply.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnApply.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnApply.setGravity(Gravity.CENTER);
		btnApply.setLayoutParams(new LinearLayout.LayoutParams(0, (int) (44 * d), 1f));
		GradientDrawable applyBg = new GradientDrawable();
		applyBg.setColor(Color.parseColor("#2563EB"));
		applyBg.setCornerRadius(100f);
		btnApply.setBackground(applyBg);
		btnApply.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!"movie".equals(incomingFilterType)) {
					incomingFilterType = "all";
				}
				activeStatusFilter = tempStatus[0];
				activeSortMode = tempSort[0];
				selectedCategory = tempCategory[0];
				updateFilterVisualState();
				applyFilterAndResetPagination();
				sheet.dismiss();
			}
		});

		actionRow.addView(btnReset);
		actionRow.addView(btnApply);
		container.addView(actionRow);

		scroll.addView(container);
		sheet.setContentView(scroll);
		sheet.show();
	}

	private TextView buildSheetSectionLabel(String label, float d) {
		TextView tv = new TextView(this);
		tv.setText(label);
		tv.setTextColor(Color.parseColor("#94A3B8"));
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		tv.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		lp.bottomMargin = (int) (8 * d);
		tv.setLayoutParams(lp);
		return tv;
	}

	private TextView createSheetChip(String text, boolean selected, float d) {
		TextView chip = new TextView(this);
		chip.setText(text);
		chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		chip.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		chip.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, (int) (34 * d)
		);
		lp.rightMargin = (int) (8 * d);
		chip.setLayoutParams(lp);
		chip.setPadding((int) (14 * d), 0, (int) (14 * d), 0);
		styleSheetChip(chip, selected, d);
		return chip;
	}

	private void styleSheetChip(TextView chip, boolean selected, float d) {
		GradientDrawable bg = new GradientDrawable();
		bg.setCornerRadius(100f);
		if (selected) {
			bg.setColor(Color.parseColor("#2563EB"));
			chip.setTextColor(Color.WHITE);
		} else {
			bg.setColor(Color.parseColor("#141926"));
			bg.setStroke((int) (1 * d), Color.parseColor("#22FFFFFF"));
			chip.setTextColor(Color.parseColor("#CBD5E1"));
		}
		chip.setBackground(bg);
	}

	private void refreshChipList(List<TextView> chips, int activeIdx) {
		float d = getResources().getDisplayMetrics().density;
		for (int i = 0; i < chips.size(); i++) {
			styleSheetChip(chips.get(i), i == activeIdx, d);
		}
	}

	private void updateFilterVisualState() {
		float d = getResources().getDisplayMetrics().density;
		boolean hasExtraFilter = (activeStatusFilter != STATUS_ALL)
			|| (activeSortMode != SORT_DEFAULT)
			|| !"Semua Kategori".equalsIgnoreCase(selectedCategory)
			|| "movie".equals(incomingFilterType);

		if (filterMenu != null) {
			GradientDrawable menuBg = new GradientDrawable();
			menuBg.setShape(GradientDrawable.RECTANGLE);
			menuBg.setCornerRadius(100f);
			if (hasExtraFilter) {
				menuBg.setColor(Color.parseColor("#1E3A8A"));
				menuBg.setStroke((int) (1 * d), Color.parseColor("#38BDF8"));
				filterMenu.setTextColor(Color.WHITE);
			} else {
				menuBg.setColor(Color.parseColor("#141822"));
				menuBg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
				filterMenu.setTextColor(Color.parseColor("#CBD5E1"));
			}
			filterMenu.setBackground(menuBg);
		}

		TextView[] tabs = {filterSemua, filterDonghua};
		for (int i = 0; i < tabs.length; i++) {
			TextView tv = tabs[i];
			if (tv == null) continue;
			boolean selected = (i == activeFilter);

			GradientDrawable bg = new GradientDrawable();
			bg.setShape(GradientDrawable.RECTANGLE);
			bg.setCornerRadius(100f);
			if (selected) {
				bg.setColor(Color.parseColor("#2563EB"));
				tv.setTextColor(Color.WHITE);
			} else {
				bg.setColor(Color.parseColor("#141822"));
				bg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
				tv.setTextColor(Color.parseColor("#CBD5E1"));
			}
			tv.setBackground(bg);
		}

		if (textCatalogSubtitle != null) {
			String sub = (activeSortMode == SORT_POPULAR) ? "Populer"
				: (activeSortMode == SORT_TITLE_ASC) ? "Judul A - Z"
				: (activeSortMode == SORT_TITLE_DESC) ? "Judul Z - A"
				: "Terbaru";
			if ("movie".equals(incomingFilterType)) {
				sub = sub + " • Movie";
			} else if (!"Semua Kategori".equalsIgnoreCase(selectedCategory)) {
				sub = sub + " • " + selectedCategory;
			}
			if (activeStatusFilter == STATUS_ONGOING) sub = sub + " • Ongoing";
			else if (activeStatusFilter == STATUS_COMPLETED) sub = sub + " • Completed";
			textCatalogSubtitle.setText(sub);
		}
	}

	private boolean matchesActiveFilter(HashMap<String, Object> item) {
		String statusStr = safeStr(item, "status", "").toLowerCase(Locale.US);
		String typeStr = safeStr(item, "type", "").toLowerCase(Locale.US);
		String categoryStr = safeStr(item, "category", "").toLowerCase(Locale.US);
		String genresStr = safeStr(item, "genres", "").toLowerCase(Locale.US);
		String titleStr = safeStr(item, "title", "").toLowerCase(Locale.US);
		String synopsisStr = safeStr(item, "synopsis", "").toLowerCase(Locale.US);
		String episodeStr = safeStr(item, "episode", "").toLowerCase(Locale.US);
		String urlStr = safeStr(item, "url", "").toLowerCase(Locale.US);
		String slugStr = safeStr(item, "slug", "").toLowerCase(Locale.US);
		String combined = typeStr + " " + categoryStr + " " + genresStr + " "
			+ titleStr + " " + synopsisStr + " " + episodeStr + " " + urlStr + " " + slugStr;

		if (activeFilter == FILTER_DONGHUA) {
			boolean isAnime = combined.contains("anime") || combined.contains("samehadaku");
			boolean isDonghua = combined.contains("donghua") || combined.contains("anichin") || combined.contains("3d");
			if (!isDonghua && isAnime) return false;
		}

		if ("movie".equals(incomingFilterType)) {
			if (!combined.contains("movie") && !combined.contains("film")) {
				return false;
			}
		}

		if (!"Semua Kategori".equalsIgnoreCase(selectedCategory)) {
			String targetCat = selectedCategory.toLowerCase(Locale.US);
			if ("romance".equals(targetCat)) {
				boolean matchRomance = genresStr.contains("romance") || genresStr.contains("romantis")
					|| genresStr.contains("romansa") || combined.contains("romance") || combined.contains("romantis");
				if (!matchRomance) return false;
			} else if ("cultivation".equals(targetCat)) {
				boolean matchCult = combined.contains("kultivasi") || combined.contains("cultivation")
					|| combined.contains("abadi") || combined.contains("immortal");
				if (!matchCult) return false;
			} else if ("comedy".equals(targetCat)) {
				boolean matchCom = combined.contains("comedy") || combined.contains("komedi") || combined.contains("slice of life");
				if (!matchCom) return false;
			} else if ("mystery".equals(targetCat)) {
				boolean matchMys = combined.contains("mystery") || combined.contains("misteri") || combined.contains("teki")
					|| combined.contains("thriller") || combined.contains("suspense") || combined.contains("detective");
				if (!matchMys) return false;
			} else if (!genresStr.contains(targetCat) && !categoryStr.contains(targetCat)) {
				return false;
			}
		}

		if (activeStatusFilter != STATUS_ALL) {
			if (activeStatusFilter == STATUS_ONGOING && !statusStr.contains("ongoing")) {
				return false;
			}
			if (activeStatusFilter == STATUS_COMPLETED && !statusStr.contains("completed") && !statusStr.contains("tamat")) {
				return false;
			}
		}
		return true;
	}

	private void applyFilterAndResetPagination() {
		paginationHandler.removeCallbacksAndMessages(null);
		isLoadingMore = false;
		lastMeasuredRowCount = -1;
		if (layoutLoadingMore != null) {
			layoutLoadingMore.setVisibility(View.GONE);
		}

		filteredDataList = new ArrayList<>();
		for (HashMap<String, Object> item : allDataList) {
			if (matchesActiveFilter(item)) {
				filteredDataList.add(item);
			}
		}

		if (filteredDataList.isEmpty() && !allDataList.isEmpty() && !"all".equals(incomingFilterType)) {
			filteredDataList.addAll(allDataList);
		}

		if (activeSortMode == SORT_POPULAR) {
			Collections.sort(filteredDataList, new Comparator<HashMap<String, Object>>() {
				@Override
				public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
					String sA = safeStr(a, "slug", "");
					String sB = safeStr(b, "slug", "");
					long vA = viewsTotalMap.containsKey(sA) ? viewsTotalMap.get(sA) : 0L;
					long vB = viewsTotalMap.containsKey(sB) ? viewsTotalMap.get(sB) : 0L;
					return Long.compare(vB, vA);
				}
			});
		} else if (activeSortMode == SORT_TITLE_ASC) {
			Collections.sort(filteredDataList, new Comparator<HashMap<String, Object>>() {
				@Override
				public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
					String tA = cleanText(safeStr(a, "title", ""));
					String tB = cleanText(safeStr(b, "title", ""));
					return tA.compareToIgnoreCase(tB);
				}
			});
		} else if (activeSortMode == SORT_TITLE_DESC) {
			Collections.sort(filteredDataList, new Comparator<HashMap<String, Object>>() {
				@Override
				public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
					String tA = cleanText(safeStr(a, "title", ""));
					String tB = cleanText(safeStr(b, "title", ""));
					return tB.compareToIgnoreCase(tA);
				}
			});
		}

		currentLimit = PAGE_SIZE;
		updateDisplayedList(true);
		if (catalogScrollView != null) {
			catalogScrollView.scrollTo(0, 0);
		}
	}

	private void setupInfiniteScroll() {
		if (catalogScrollView == null) return;
		catalogScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
			@Override
			public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
				updateScrollTopVisibility(scrollY);

				if (isCatalogLoading || isLoadingMore) return;
				if (displayedList.size() >= filteredDataList.size()) return;

				View child = v.getChildAt(0);
				if (child == null) return;

				int diff = child.getBottom() - (v.getHeight() + scrollY);
				if (diff <= 180) {
					triggerLoadMore();
				}
			}
		});
	}

	private void triggerLoadMore() {
		if (isLoadingMore || displayedList.size() >= filteredDataList.size()) return;
		isLoadingMore = true;

		if (layoutLoadingMore != null) {
			layoutLoadingMore.setVisibility(View.VISIBLE);
		}

		paginationHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				currentLimit += PAGE_SIZE;
				isLoadingMore = false;
				if (layoutLoadingMore != null) {
					layoutLoadingMore.setVisibility(View.GONE);
				}
				updateDisplayedList(false);
			}
		}, 250);
	}

	private void applyHeaderGlassStyle() {
		float d = getResources().getDisplayMetrics().density;
		int statusBarHeight = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusBarHeight = getResources().getDimensionPixelSize(resId);

		if (catalogHeader != null) {
			catalogHeader.setPadding(
				(int) (14 * d),
				statusBarHeight + (int) (10 * d),
				(int) (14 * d),
				(int) (10 * d)
			);
		}
		if (catalogScrollView != null) {
			catalogScrollView.setPadding(
				catalogScrollView.getPaddingLeft(),
				statusBarHeight + (int) (72 * d),
				catalogScrollView.getPaddingRight(),
				catalogScrollView.getPaddingBottom()
			);
		}

		BlurView backBlur = findViewById(R.id.catalog_back_blur_bg);
		BlurView headerBlur = findViewById(R.id.catalog_header_blur_bg);
		ImageView backArrow = findViewById(R.id.img_catalog_back_arrow);

		if (backArrow != null) {
			backArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		}
		if (btnBack != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			btnBack.setElevation(14f);
		}
		View headerPill = findViewById(R.id.catalog_header_pill);
		if (headerPill != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			headerPill.setElevation(14f);
		}

		setupGlassBlur(backBlur, catalogRoot, 20f, Color.parseColor("#33FFFFFF"));
		setupGlassBlur(headerBlur, catalogRoot, 20f, Color.parseColor("#33FFFFFF"));
	}

	private void setupGlassBlur(BlurView blurView, ViewGroup rootContainer, float radius, int strokeColor) {
		if (blurView == null || rootContainer == null) return;
		float d = getResources().getDisplayMetrics().density;

		blurView.setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(View view, Outline outline) {
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 1000f);
			}
		});
		blurView.setClipToOutline(true);

		Drawable winBg = getWindow().getDecorView().getBackground();
		blurView.setupWith(rootContainer, new RenderScriptBlur(this))
			.setFrameClearDrawable(winBg)
			.setBlurRadius(radius);
		blurView.setOverlayColor(Color.parseColor("#66141824"));

		GradientDrawable glassBorder = new GradientDrawable();
		glassBorder.setColor(Color.TRANSPARENT);
		glassBorder.setCornerRadius(1000f);
		glassBorder.setStroke((int) (1 * d), strokeColor);
		blurView.setForeground(glassBorder);
	}

	private void updateBottomSpacingForMiniPlayer() {
		float d = getResources().getDisplayMetrics().density;
		boolean miniActive = DongflixPlayerManager.getInstance().isMiniPlayerActive
			&& DongflixPlayerManager.getInstance().sharedMediaPlayer != null;

		if (catalogScrollView != null) {
			catalogScrollView.setPadding(
				catalogScrollView.getPaddingLeft(),
				catalogScrollView.getPaddingTop(),
				catalogScrollView.getPaddingRight(),
				miniActive ? (int) (124 * d) : (int) (40 * d)
			);
		}
		if (btnScrollTop != null) {
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) btnScrollTop.getLayoutParams();
			if (lp != null) {
				lp.bottomMargin = miniActive ? (int) (108 * d) : (int) (28 * d);
				btnScrollTop.setLayoutParams(lp);
			}
		}
	}

	private void showCatalogLoadingPlaceholders() {
		isCatalogLoading = true;
		displayedList.clear();
		for (int i = 0; i < 18; i++) {
			HashMap<String, Object> m = new HashMap<>();
			m.put("is_loading", true);
			displayedList.add(m);
		}
		adapter.notifyDataSetChanged();
		adjustGridHeight(gridCatalog, 3);
		startCatalogShimmer();
	}

	private Drawable createCatalogShimmerDrawable(final float cornerRadiusPx) {
		return new Drawable() {
			private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Matrix shaderMatrix = new Matrix();
			private LinearGradient gradient;
			private int lastWidth = -1;

			{
				basePaint.setColor(Color.parseColor("#141822"));
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

				float translateX = -w + (2f * w * catalogShimmerPhase);
				shaderMatrix.setTranslate(translateX, 0);
				gradient.setLocalMatrix(shaderMatrix);
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shimmerPaint);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private void startCatalogShimmer() {
		stopCatalogShimmerOnly();
		catalogShimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
		catalogShimmerAnimator.setDuration(1200);
		catalogShimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
		catalogShimmerAnimator.setInterpolator(new LinearInterpolator());
		catalogShimmerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				if (!isCatalogLoading || gridCatalog == null) return;
				catalogShimmerPhase = (float) animation.getAnimatedValue();
				for (int i = 0; i < gridCatalog.getChildCount(); i++) {
					invalidateShimmerRecursive(gridCatalog.getChildAt(i));
				}
			}
		});
		catalogShimmerAnimator.start();
	}

	private void invalidateShimmerRecursive(View v) {
		if (v == null) return;
		v.invalidate();
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int k = 0; k < vg.getChildCount(); k++) {
				invalidateShimmerRecursive(vg.getChildAt(k));
			}
		}
	}

	private void stopCatalogShimmerOnly() {
		if (catalogShimmerAnimator != null) {
			catalogShimmerAnimator.cancel();
			catalogShimmerAnimator = null;
		}
	}

	private void stopCatalogShimmer() {
		isCatalogLoading = false;
		stopCatalogShimmerOnly();
	}

	@Override
	protected void onDestroy() {
		paginationHandler.removeCallbacksAndMessages(null);
		detachFirebaseViewsListener();
		stopCatalogShimmer();
		super.onDestroy();
	}

	private void updateDisplayedList(boolean forceMeasure) {
		int end = Math.min(currentLimit, filteredDataList.size());
		displayedList.clear();
		displayedList.addAll(filteredDataList.subList(0, end));
		adapter.notifyDataSetChanged();
		if (forceMeasure) {
			lastMeasuredRowCount = -1;
		}
		adjustGridHeight(gridCatalog, 3);
		updateBottomSpacingForMiniPlayer();
	}

	private void adjustGridHeight(final GridView gridView, final int columns) {
		if (gridView == null || adapter == null || adapter.getCount() == 0) return;

		final int items = adapter.getCount();
		final int rows = (int) Math.ceil((double) items / columns);
		if (rows == lastMeasuredRowCount && lastMeasuredItemHeight > 0) {
			float density = getResources().getDisplayMetrics().density;
			int verticalSpacingPx = (int) (16 * density);
			ViewGroup.LayoutParams params = gridView.getLayoutParams();
			params.height = (lastMeasuredItemHeight * rows) + (verticalSpacingPx * Math.max(0, rows - 1));
			gridView.setLayoutParams(params);
			return;
		}

		gridView.post(new Runnable() {
			@Override
			public void run() {
				if (gridView.getAdapter() == null || gridView.getAdapter().getCount() == 0) return;

				float density = getResources().getDisplayMetrics().density;
				int horizontalSpacingPx = (int) (12 * density);
				int verticalSpacingPx = (int) (16 * density);
				int paddingHorizontal = gridView.getPaddingLeft() + gridView.getPaddingRight();

				int availableWidth = gridView.getWidth() - paddingHorizontal - (horizontalSpacingPx * (columns - 1));
				if (availableWidth <= 0) return;
				int columnWidth = availableWidth / columns;

				View itemView = gridView.getAdapter().getView(0, null, gridView);
				itemView.measure(
					View.MeasureSpec.makeMeasureSpec(columnWidth, View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
				);

				int exactItemHeight = itemView.getMeasuredHeight();
				lastMeasuredItemHeight = exactItemHeight;
				lastMeasuredRowCount = rows;

				ViewGroup.LayoutParams params = gridView.getLayoutParams();
				params.height = (exactItemHeight * rows) + (verticalSpacingPx * Math.max(0, rows - 1));
				gridView.setLayoutParams(params);
				gridView.requestLayout();
			}
		});
	}

	public class CatalogAdapter extends BaseAdapter {
		private final ArrayList<HashMap<String, Object>> _data;

		public CatalogAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override public int getCount() { return _data.size(); }
		@Override public HashMap<String, Object> getItem(int index) { return _data.get(index); }
		@Override public long getItemId(int index) { return index; }

		@Override
		public View getView(int position, View v, ViewGroup container) {
			View view = v;
			if (view == null) {
				view = getLayoutInflater().inflate(R.layout.custom_anime, container, false);
			}
			float d = view.getResources().getDisplayMetrics().density;
			Typeface tf = getOutfitTypeface();

			TextView textTitle = view.findViewById(R.id.text_title);
			TextView textStatus = view.findViewById(R.id.text_status);
			TextView textEpisode = view.findViewById(R.id.text_episode);
			ImageView imgCover = view.findViewById(R.id.img_cover);
			View view2 = view.findViewById(R.id.view2);

			HashMap<String, Object> item = _data.get(position);

			if (item.containsKey("is_loading")) {
				if (textTitle != null) {
					textTitle.setText("");
					textTitle.setBackground(createCatalogShimmerDrawable(6 * d));
				}
				if (textStatus != null) {
					textStatus.setText("");
					textStatus.setBackground(createCatalogShimmerDrawable(50f));
				}
				if (textEpisode != null) {
					textEpisode.setText("");
					textEpisode.setBackground(createCatalogShimmerDrawable(4 * d));
				}
				if (imgCover != null) {
					imgCover.setImageResource(android.R.color.transparent);
					imgCover.setBackground(createCatalogShimmerDrawable(10 * d));
				}
				if (view2 != null) view2.setVisibility(View.GONE);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (textTitle != null) {
					textTitle.setBackgroundColor(Color.TRANSPARENT);
					textTitle.setTypeface(tf, Typeface.NORMAL);
					textTitle.setIncludeFontPadding(false);
					textTitle.setText(cleanText(safeStr(item, "title", "")));
				}
				if (textEpisode != null) {
					textEpisode.setBackgroundColor(Color.TRANSPARENT);
					textEpisode.setTypeface(tf, Typeface.NORMAL);
					textEpisode.setText(safeStr(item, "episode", ""));
				}
				if (view2 != null) {
					view2.setVisibility(View.VISIBLE);
					GradientDrawable gradient = new GradientDrawable(
						GradientDrawable.Orientation.BOTTOM_TOP,
						new int[] { Color.parseColor("#F2000000"), Color.parseColor("#80000000"), Color.TRANSPARENT }
					);
					view2.setBackground(gradient);
				}

				if (textStatus != null) {
					GradientDrawable statusBg = new GradientDrawable();
					statusBg.setCornerRadius(50);
					statusBg.setColor(0xFF3B82F6);
					textStatus.setBackground(statusBg);
					textStatus.setTypeface(tf, Typeface.NORMAL);
					textStatus.setText(safeStr(item, "status", ""));
				}

				String cover = safeStr(item, "cover", "").trim();
				if (cover.contains("i.ibb.co.com")) cover = cover.replace("i.ibb.co.com", "i.ibb.co");
				if (cover.startsWith("http://")) cover = cover.replace("http://", "https://");

				if (imgCover != null && !cover.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(CatalogActivity.this).load(Uri.parse(cover)).into(imgCover);
				}
			}
			return view;
		}
	}
}
