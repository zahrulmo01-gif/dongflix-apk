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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import java.util.ArrayList;
import java.util.HashMap;

public class CatalogActivity extends AppCompatActivity {

	private FrameLayout catalogRoot;
	private LinearLayout catalogHeader;
	private NestedScrollView catalogScrollView;
	private GridView gridCatalog;
	private TextView btnLoadMore;
	private View btnBack;

	private RequestNetwork req;
	private RequestNetwork.RequestListener reqListener;

	private ArrayList<HashMap<String, Object>> allDataList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> displayedList = new ArrayList<>();
	private CatalogAdapter adapter;

	private int currentLimit = 18;
	private final int PAGE_SIZE = 18;

	private ValueAnimator catalogShimmerAnimator;
	private float catalogShimmerPhase = 0f;
	private boolean isCatalogLoading = true;

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
		btnLoadMore = findViewById(R.id.btn_load_more);
		btnBack = findViewById(R.id.btn_catalog_back);

		applyHeaderGlassStyle();
		applyLoadMoreButtonStyle();
		updateBottomSpacingForMiniPlayer();

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
				if (displayedList != null && position < displayedList.size()) {
					HashMap<String, Object> item = displayedList.get(position);
					if (item.containsKey("is_loading")) return;

					Intent intent = new Intent(CatalogActivity.this, DetailActivity.class);
					intent.putExtra("url", item.containsKey("url") ? item.get("url").toString() : "");
					intent.putExtra("title", item.containsKey("title") ? item.get("title").toString() : "");
					intent.putExtra("image", item.containsKey("cover") ? item.get("cover").toString() : "");
					startActivity(intent);
				}
			}
		});

		btnLoadMore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				currentLimit += PAGE_SIZE;
				updateDisplayedList();
			}
		});

		showCatalogLoadingPlaceholders();

		req = new RequestNetwork(this);
		reqListener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String tag, String response, HashMap<String, Object> headers) {
				stopCatalogShimmer();
				ArrayList<HashMap<String, Object>> parsed = new Gson().fromJson(
					response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType()
				);
				if (parsed != null) {
					allDataList = parsed;
					updateDisplayedList();
				}
			}

			@Override
			public void onErrorResponse(String tag, String message) {
				stopCatalogShimmer();
			}
		};

		req.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/", "", reqListener);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateBottomSpacingForMiniPlayer();
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
				statusBarHeight + (int) (66 * d),
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

	private void applyLoadMoreButtonStyle() {
		if (btnLoadMore == null) return;
		GradientDrawable btnBg = new GradientDrawable();
		btnBg.setShape(GradientDrawable.RECTANGLE);
		btnBg.setColor(Color.parseColor("#2563EB"));
		btnBg.setCornerRadius(100f);
		btnLoadMore.setBackground(btnBg);
		btnLoadMore.setTextColor(Color.WHITE);
	}

	private void updateBottomSpacingForMiniPlayer() {
		if (btnLoadMore == null) return;
		float d = getResources().getDisplayMetrics().density;
		boolean miniActive = DongflixPlayerManager.getInstance().isMiniPlayerActive
			&& DongflixPlayerManager.getInstance().sharedMediaPlayer != null;

		ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) btnLoadMore.getLayoutParams();
		if (lp != null) {
			lp.bottomMargin = miniActive ? (int) (96 * d) : (int) (20 * d);
			btnLoadMore.setLayoutParams(lp);
		}
		if (catalogScrollView != null) {
			catalogScrollView.setPadding(
				catalogScrollView.getPaddingLeft(),
				catalogScrollView.getPaddingTop(),
				catalogScrollView.getPaddingRight(),
				miniActive ? (int) (140 * d) : (int) (48 * d)
			);
		}
	}

	private void showCatalogLoadingPlaceholders() {
		isCatalogLoading = true;
		ArrayList<HashMap<String, Object>> dummy = new ArrayList<>();
		for (int i = 0; i < 18; i++) {
			HashMap<String, Object> m = new HashMap<>();
			m.put("is_loading", true);
			dummy.add(m);
		}
		adapter = new CatalogAdapter(dummy);
		gridCatalog.setAdapter(adapter);
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
		isCatalogLoading = true;
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
		stopCatalogShimmer();
		super.onDestroy();
	}

	private void updateDisplayedList() {
		int end = Math.min(currentLimit, allDataList.size());
		displayedList = new ArrayList<>(allDataList.subList(0, end));
		adapter = new CatalogAdapter(displayedList);
		gridCatalog.setAdapter(adapter);
		adjustGridHeight(gridCatalog, 3);
		updateBottomSpacingForMiniPlayer();

		if (end >= allDataList.size()) {
			btnLoadMore.setVisibility(View.GONE);
		} else {
			btnLoadMore.setVisibility(View.VISIBLE);
		}
	}

	private void adjustGridHeight(final GridView gridView, final int columns) {
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

				int availableWidth = gridView.getWidth() - paddingHorizontal - (horizontalSpacingPx * (columns - 1));
				int columnWidth = availableWidth / columns;

				View itemView = gridView.getAdapter().getView(0, null, gridView);
				itemView.measure(
					View.MeasureSpec.makeMeasureSpec(columnWidth, View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
				);

				int exactItemHeight = itemView.getMeasuredHeight();
				ViewGroup.LayoutParams params = gridView.getLayoutParams();
				params.height = (exactItemHeight * rows) + (verticalSpacingPx * Math.max(0, rows - 1));
				gridView.setLayoutParams(params);
				gridView.requestLayout();
			}
		});
	}

	public class CatalogAdapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		public CatalogAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }

		@Override public int getCount() { return _data.size(); }
		@Override public HashMap<String, Object> getItem(int index) { return _data.get(index); }
		@Override public long getItemId(int index) { return index; }

		@Override
		public View getView(int position, View v, ViewGroup container) {
			LayoutInflater inflater = getLayoutInflater();
			View view = inflater.inflate(R.layout.custom_anime, container, false);
			float d = view.getResources().getDisplayMetrics().density;

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
				if (textTitle != null) textTitle.setBackgroundColor(Color.TRANSPARENT);
				if (textEpisode != null) textEpisode.setBackgroundColor(Color.TRANSPARENT);
				if (view2 != null) view2.setVisibility(View.VISIBLE);

				if (textStatus != null) {
					textStatus.setBackground(new GradientDrawable() {{ setCornerRadius(50); setColor(0xFF3B82F6); }});
				}

				if (view2 != null) {
					GradientDrawable gradient = new GradientDrawable(
						GradientDrawable.Orientation.BOTTOM_TOP,
						new int[] { Color.parseColor("#F2000000"), Color.parseColor("#80000000"), Color.TRANSPARENT }
					);
					view2.setBackground(gradient);
				}

				if (textTitle != null) textTitle.setText(item.get("title") != null ? item.get("title").toString() : "");
				if (textStatus != null) textStatus.setText(item.get("status") != null ? item.get("status").toString() : "");
				if (textEpisode != null) textEpisode.setText(item.get("episode") != null ? item.get("episode").toString() : "");

				String cover = item.get("cover") != null ? item.get("cover").toString().trim() : "";
				if (cover.contains("i.ibb.co.com")) cover = cover.replace("i.ibb.co.com", "i.ibb.co");
				if (cover.startsWith("http://")) cover = cover.replace("http://", "https://");

				if (imgCover != null) {
					Glide.with(getApplicationContext()).load(Uri.parse(cover)).into(imgCover);
				}
			}
			return view;
		}
	}
}
