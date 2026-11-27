package com.dongflix;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";

	private FirebaseUser currentUser;
	private String currentUserKey = "";
	private DatabaseReference notifRef;
	private ValueEventListener notifListener;

	private FrameLayout rootLayout;
	private ScrollView scrollView;
	private LinearLayout listContainer;
	private LinearLayout emptyStateBox;
	private Typeface appFont;

	private ValueAnimator shimmerAnimator;
	private float shimmerPhase = 0f;
	private boolean isLoading = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null || currentUser.getEmail() == null) {
			finish();
			return;
		}

		currentUserKey = currentUser.getEmail().trim().toLowerCase(Locale.US).replace(".", ",");
		notifRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("notifications")
			.child(currentUserKey);

		buildLayout();
		showLoadingPlaceholders();
		startListeningNotifications();
	}

	private Typeface getAppFont() {
		if (appFont != null) return appFont;
		String[] candidates = {
			"fonts/outfit_regular.ttf",
			"fonts/outfit_medium.ttf",
			"fonts/outfit.ttf",
			"fonts/Outfit-Regular.ttf",
			"fonts/productsans_reg.ttf"
		};
		for (String path : candidates) {
			try {
				appFont = Typeface.createFromAsset(getAssets(), path);
				if (appFont != null) return appFont;
			} catch (Exception ignored) {}
		}
		appFont = Typeface.DEFAULT;
		return appFont;
	}

	private void styleText(TextView tv) {
		if (tv == null) return;
		tv.setTypeface(getAppFont(), Typeface.NORMAL);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			tv.setLetterSpacing(-0.01f);
		}
	}

	private void setupGlassBlur(BlurView blurView, ViewGroup rootContainer, float radius, float cornerRadiusPx, int overlayColor, int strokeColor) {
		if (blurView == null || rootContainer == null) return;
		float d = getResources().getDisplayMetrics().density;
		final float rPx = cornerRadiusPx;

		blurView.setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(View view, Outline outline) {
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), rPx);
			}
		});
		blurView.setClipToOutline(true);

		Drawable winBg = getWindow().getDecorView().getBackground();
		blurView.setupWith(rootContainer, new RenderScriptBlur(this))
			.setFrameClearDrawable(winBg)
			.setBlurRadius(radius);
		blurView.setOverlayColor(overlayColor);

		if (strokeColor != Color.TRANSPARENT) {
			GradientDrawable border = new GradientDrawable();
			border.setColor(Color.TRANSPARENT);
			border.setCornerRadius(rPx);
			border.setStroke(Math.max(1, (int) (1 * d)), strokeColor);
			blurView.setForeground(border);
		}
	}

	private Drawable createShimmerDrawable(final float cornerRadiusPx) {
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

	private void startShimmer() {
		isLoading = true;
		stopShimmerOnly();
		shimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
		shimmerAnimator.setDuration(1200L);
		shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
		shimmerAnimator.setInterpolator(new LinearInterpolator());
		shimmerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				if (!isLoading || listContainer == null) return;
				shimmerPhase = (float) animation.getAnimatedValue();
				invalidateShimmerRecursive(listContainer);
			}
		});
		shimmerAnimator.start();
	}

	private void invalidateShimmerRecursive(View v) {
		if (v == null) return;
		v.invalidate();
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) {
				invalidateShimmerRecursive(vg.getChildAt(i));
			}
		}
	}

	private void stopShimmerOnly() {
		if (shimmerAnimator != null) {
			shimmerAnimator.cancel();
			shimmerAnimator = null;
		}
	}

	private void stopShimmer() {
		isLoading = false;
		stopShimmerOnly();
	}

	private void showLoadingPlaceholders() {
		if (listContainer == null) return;
		listContainer.removeAllViews();
		if (emptyStateBox != null) emptyStateBox.setVisibility(View.GONE);

		float d = getResources().getDisplayMetrics().density;

		for (int i = 0; i < 5; i++) {
			LinearLayout block = new LinearLayout(this);
			block.setOrientation(LinearLayout.VERTICAL);
			block.setPadding(0, (int) (12 * d), 0, (int) (14 * d));

			View datePh = new View(this);
			LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams((int) (130 * d), (int) (13 * d));
			dLp.bottomMargin = (int) (14 * d);
			datePh.setLayoutParams(dLp);
			datePh.setBackground(createShimmerDrawable(6 * d));
			block.addView(datePh);

			LinearLayout topRow = new LinearLayout(this);
			topRow.setOrientation(LinearLayout.HORIZONTAL);
			topRow.setGravity(Gravity.CENTER_VERTICAL);
			LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			trLp.bottomMargin = (int) (10 * d);
			topRow.setLayoutParams(trLp);

			View iconPh = new View(this);
			LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams((int) (16 * d), (int) (16 * d));
			icLp.rightMargin = (int) (8 * d);
			iconPh.setLayoutParams(icLp);
			iconPh.setBackground(createShimmerDrawable(100f));

			View titlePh = new View(this);
			titlePh.setLayoutParams(new LinearLayout.LayoutParams((int) (85 * d), (int) (12 * d)));
			titlePh.setBackground(createShimmerDrawable(6 * d));

			View spacer = new View(this);
			spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));

			View timePh = new View(this);
			timePh.setLayoutParams(new LinearLayout.LayoutParams((int) (38 * d), (int) (11 * d)));
			timePh.setBackground(createShimmerDrawable(6 * d));

			topRow.addView(iconPh);
			topRow.addView(titlePh);
			topRow.addView(spacer);
			topRow.addView(timePh);
			block.addView(topRow);

			View bodyLine1 = new View(this);
			bodyLine1.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (int) (13 * d)
			));
			bodyLine1.setBackground(createShimmerDrawable(6 * d));
			block.addView(bodyLine1);

			View bodyLine2 = new View(this);
			LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams((int) (210 * d), (int) (13 * d));
			b2Lp.topMargin = (int) (6 * d);
			bodyLine2.setLayoutParams(b2Lp);
			bodyLine2.setBackground(createShimmerDrawable(6 * d));
			block.addView(bodyLine2);

			View divider = new View(this);
			LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) (1 * d))
			);
			divLp.topMargin = (int) (16 * d);
			divider.setLayoutParams(divLp);
			divider.setBackgroundColor(Color.parseColor("#1E222B"));
			block.addView(divider);

			listContainer.addView(block);
		}
		startShimmer();
	}

	private void showClearAllNotificationsDialog() {
		if (notifRef == null || isFinishing() || isDestroyed()) return;
		new AlertDialog.Builder(this)
			.setTitle("Hapus Semua Notifikasi")
			.setMessage("Hapus semua notifikasi yang ada?")
			.setPositiveButton("Hapus", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					notifRef.removeValue();
					DongflixToast.show(NotificationActivity.this, "Semua notifikasi dihapus");
				}
			})
			.setNegativeButton("Batal", null)
			.show();
	}

	private void buildLayout() {
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window w = getWindow();
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.setStatusBarColor(Color.TRANSPARENT);
			w.setNavigationBarColor(Color.parseColor("#000000"));
		}

		float d = getResources().getDisplayMetrics().density;
		int statusBarH = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusBarH = getResources().getDimensionPixelSize(resId);

		rootLayout = new FrameLayout(this);
		rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		rootLayout.setBackgroundColor(Color.parseColor("#0A0B0E"));

		scrollView = new ScrollView(this);
		scrollView.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		scrollView.setPadding(
			(int) (18 * d),
			statusBarH + (int) (72 * d),
			(int) (18 * d),
			(int) (28 * d)
		);
		scrollView.setClipToPadding(false);
		scrollView.setVerticalScrollBarEnabled(false);

		FrameLayout contentHolder = new FrameLayout(this);
		contentHolder.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		listContainer = new LinearLayout(this);
		listContainer.setOrientation(LinearLayout.VERTICAL);
		listContainer.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		emptyStateBox = new LinearLayout(this);
		emptyStateBox.setOrientation(LinearLayout.VERTICAL);
		emptyStateBox.setGravity(Gravity.CENTER);
		emptyStateBox.setPadding((int) (24 * d), (int) (90 * d), (int) (24 * d), (int) (40 * d));
		emptyStateBox.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		emptyStateBox.setVisibility(View.GONE);

		TextView tvEmptyTitle = new TextView(this);
		tvEmptyTitle.setText("Belum ada notifikasi");
		tvEmptyTitle.setTextColor(Color.WHITE);
		tvEmptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		styleText(tvEmptyTitle);

		TextView tvEmptyDesc = new TextView(this);
		tvEmptyDesc.setText("Saat seseorang membalas atau menyebut kamu di komentar atau live chat, notifikasinya akan muncul di sini.");
		tvEmptyDesc.setTextColor(Color.parseColor("#94A3B8"));
		tvEmptyDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		tvEmptyDesc.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams edDescLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		edDescLp.topMargin = (int) (6 * d);
		tvEmptyDesc.setLayoutParams(edDescLp);
		styleText(tvEmptyDesc);

		emptyStateBox.addView(tvEmptyTitle);
		emptyStateBox.addView(tvEmptyDesc);

		contentHolder.addView(listContainer);
		contentHolder.addView(emptyStateBox);
		scrollView.addView(contentHolder);
		rootLayout.addView(scrollView);

		LinearLayout topHeader = new LinearLayout(this);
		topHeader.setOrientation(LinearLayout.HORIZONTAL);
		topHeader.setGravity(Gravity.CENTER_VERTICAL);
		topHeader.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP
		));
		topHeader.setPadding((int) (14 * d), statusBarH + (int) (8 * d), (int) (14 * d), (int) (8 * d));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) topHeader.setElevation(16f);

		int btnSize = (int) (46 * d);
		FrameLayout backBtn = new FrameLayout(this);
		LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(btnSize, btnSize);
		backLp.rightMargin = (int) (10 * d);
		backBtn.setLayoutParams(backLp);

		BlurView backBlur = new BlurView(this);
		backBlur.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		setupGlassBlur(backBlur, rootLayout, 20f, btnSize / 2f, Color.parseColor("#66141824"), Color.parseColor("#33FFFFFF"));
		backBtn.addView(backBlur);

		ImageView backArrow = new ImageView(this);
		int arrowSize = (int) (20 * d);
		backArrow.setLayoutParams(new FrameLayout.LayoutParams(arrowSize, arrowSize, Gravity.CENTER));
		backArrow.setImageResource(R.drawable.ic_back_arrow);
		backArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		backBtn.addView(backArrow);
		backBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		FrameLayout titleCapsule = new FrameLayout(this);
		LinearLayout.LayoutParams capLp = new LinearLayout.LayoutParams(0, btnSize, 1f);
		capLp.rightMargin = (int) (10 * d);
		titleCapsule.setLayoutParams(capLp);

		BlurView capBlur = new BlurView(this);
		capBlur.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		setupGlassBlur(capBlur, rootLayout, 20f, btnSize / 2f, Color.parseColor("#66141824"), Color.parseColor("#33FFFFFF"));
		titleCapsule.addView(capBlur);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Notifikasi");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
		tvTitle.setGravity(Gravity.CENTER);
		styleText(tvTitle);
		titleCapsule.addView(tvTitle, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		FrameLayout clearBtn = new FrameLayout(this);
		clearBtn.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));

		BlurView clearBlur = new BlurView(this);
		clearBlur.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		setupGlassBlur(clearBlur, rootLayout, 20f, btnSize / 2f, Color.parseColor("#66141824"), Color.parseColor("#33FFFFFF"));
		clearBtn.addView(clearBlur);

		TextView tvClear = new TextView(this);
		tvClear.setText("✕");
		tvClear.setTextColor(Color.WHITE);
		tvClear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
		tvClear.setGravity(Gravity.CENTER);
		styleText(tvClear);
		clearBtn.addView(tvClear, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		clearBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showClearAllNotificationsDialog();
			}
		});

		topHeader.addView(backBtn);
		topHeader.addView(titleCapsule);
		topHeader.addView(clearBtn);
		rootLayout.addView(topHeader);

		setContentView(rootLayout);
	}

	private void startListeningNotifications() {
		if (notifRef == null) return;
		notifListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (isFinishing() || isDestroyed()) return;
				stopShimmer();

				List<HashMap<String, Object>> items = new ArrayList<>();
				if (snapshot.exists()) {
					for (DataSnapshot child : snapshot.getChildren()) {
						Object raw = child.getValue();
						if (raw instanceof Map) {
							HashMap<String, Object> map = new HashMap<>();
							for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
								if (e.getKey() != null) map.put(e.getKey().toString(), e.getValue());
							}
							String id = child.getKey() != null ? child.getKey() : "";
							map.put("_id", id);
							items.add(map);

							if (!id.isEmpty() && !Boolean.TRUE.equals(map.get("read"))) {
								notifRef.child(id).child("read").setValue(true);
							}
						}
					}
				}

				Collections.sort(items, new Comparator<HashMap<String, Object>>() {
					@Override
					public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
						return Long.compare(parseLong(b.get("timestamp")), parseLong(a.get("timestamp")));
					}
				});

				renderNotifications(items);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				stopShimmer();
			}
		};
		notifRef.limitToLast(50).addValueEventListener(notifListener);
	}

	private int resolveBellIconRes() {
		int id = getResources().getIdentifier("ic_notifications", "drawable", getPackageName());
		if (id > 0) return id;
		int id2 = getResources().getIdentifier("ic_notification", "drawable", getPackageName());
		return id2 > 0 ? id2 : android.R.drawable.ic_popup_reminder;
	}

	private void renderNotifications(List<HashMap<String, Object>> items) {
		if (listContainer == null || emptyStateBox == null) return;
		listContainer.removeAllViews();

		if (items == null || items.isEmpty()) {
			emptyStateBox.setVisibility(View.VISIBLE);
			return;
		}
		emptyStateBox.setVisibility(View.GONE);

		float d = getResources().getDisplayMetrics().density;
		int bellRes = resolveBellIconRes();
		String lastDateHeader = "";

		for (HashMap<String, Object> item : items) {
			String type = safe(item, "type", "mention");
			String source = safe(item, "source", "");
			final String slug = safe(item, "slug", "");
			String title = safe(item, "title", "");
			String episode = safe(item, "episode", "");
			String actorName = safe(item, "actorName", "Pengguna");
			String snippet = safe(item, "snippet", "");
			long ts = parseLong(item.get("timestamp"));
			if (ts <= 0L) ts = System.currentTimeMillis();

			String dateHeader = formatDateHeader(ts);
			String clockText = formatClock(ts);

			LinearLayout itemWrap = new LinearLayout(this);
			itemWrap.setOrientation(LinearLayout.VERTICAL);
			itemWrap.setPadding(0, (int) (10 * d), 0, 0);
			itemWrap.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			));

			if (!dateHeader.equals(lastDateHeader)) {
				lastDateHeader = dateHeader;
				TextView tvDate = new TextView(this);
				tvDate.setText(dateHeader);
				tvDate.setTextColor(Color.parseColor("#E2E8F0"));
				tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
				styleText(tvDate);
				LinearLayout.LayoutParams dtLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				dtLp.topMargin = (int) (4 * d);
				dtLp.bottomMargin = (int) (12 * d);
				tvDate.setLayoutParams(dtLp);
				itemWrap.addView(tvDate);
			}

			LinearLayout metaRow = new LinearLayout(this);
			metaRow.setOrientation(LinearLayout.HORIZONTAL);
			metaRow.setGravity(Gravity.CENTER_VERTICAL);
			LinearLayout.LayoutParams mrLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			mrLp.bottomMargin = (int) (6 * d);
			metaRow.setLayoutParams(mrLp);

			ImageView ivBell = new ImageView(this);
			int bellPx = (int) (16 * d);
			LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(bellPx, bellPx);
			bLp.rightMargin = (int) (7 * d);
			ivBell.setLayoutParams(bLp);
			ivBell.setImageResource(bellRes);
			ivBell.setColorFilter(Color.parseColor("#E2E8F0"), PorterDuff.Mode.SRC_IN);

			TextView tvLabel = new TextView(this);
			tvLabel.setText("Notifikasi");
			tvLabel.setTextColor(Color.parseColor("#CBD5E1"));
			tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
			styleText(tvLabel);

			View spacer = new View(this);
			spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));

			TextView tvTime = new TextView(this);
			tvTime.setText(clockText);
			tvTime.setTextColor(Color.parseColor("#94A3B8"));
			tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
			styleText(tvTime);

			metaRow.addView(ivBell);
			metaRow.addView(tvLabel);
			metaRow.addView(spacer);
			metaRow.addView(tvTime);
			itemWrap.addView(metaRow);

			boolean isCommentNotif = "comment".equalsIgnoreCase(source) || (!slug.isEmpty() && !"live-chat".equalsIgnoreCase(slug));
			String actionPhrase;
			if (isCommentNotif) {
				String contextLabel = !title.isEmpty()
					? title + (!episode.isEmpty() ? " (" + episode + ")" : "")
					: (!slug.isEmpty() ? slug : "komentar");
				actionPhrase = "reply".equalsIgnoreCase(type)
					? actorName + " membalas komentar di " + contextLabel + ": " + snippet
					: actorName + " menyebut di komentar " + contextLabel + ": " + snippet;
			} else {
				actionPhrase = "reply".equalsIgnoreCase(type)
					? actorName + " membalas di #live-chat: " + snippet
					: actorName + " menyebut di #live-chat: " + snippet;
			}

			TextView tvContent = new TextView(this);
			tvContent.setText(actionPhrase);
			tvContent.setTextColor(Color.WHITE);
			tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
			tvContent.setLineSpacing(2 * d, 1.08f);
			tvContent.setMaxLines(4);
			tvContent.setEllipsize(TextUtils.TruncateAt.END);
			styleText(tvContent);
			itemWrap.addView(tvContent);

			View divider = new View(this);
			LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) (1 * d))
			);
			divLp.topMargin = (int) (14 * d);
			divider.setLayoutParams(divLp);
			divider.setBackgroundColor(Color.parseColor("#262932"));
			itemWrap.addView(divider);

			itemWrap.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!slug.isEmpty() && !"live-chat".equalsIgnoreCase(slug)) {
						Intent i = new Intent(NotificationActivity.this, WatchActivity.class);
						i.putExtra("slug", slug);
						startActivity(i);
					} else {
						startActivity(new Intent(NotificationActivity.this, ChatActivity.class));
					}
				}
			});

			listContainer.addView(itemWrap);
		}
	}

	private String formatDateHeader(long ts) {
		return new SimpleDateFormat("dd, MMMM yyyy", new Locale("id", "ID")).format(new Date(ts));
	}

	private String formatClock(long ts) {
		return new SimpleDateFormat("HH.mm", Locale.getDefault()).format(new Date(ts));
	}

	private String safe(Map<String, Object> map, String key, String fallback) {
		if (map == null || key == null) return fallback;
		Object v = map.get(key);
		return v != null ? v.toString().trim() : fallback;
	}

	private long parseLong(Object o) {
		if (o == null) return 0L;
		if (o instanceof Number) return ((Number) o).longValue();
		try {
			return Long.parseLong(o.toString().trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	@Override
	protected void onDestroy() {
		stopShimmer();
		DongflixToast.dismissCurrent();
		if (notifRef != null && notifListener != null) {
			notifRef.removeEventListener(notifListener);
		}
		super.onDestroy();
	}
}
