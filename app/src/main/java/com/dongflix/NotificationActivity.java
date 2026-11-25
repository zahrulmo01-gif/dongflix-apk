package com.dongflix;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff;
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
	private static final String ADMIN_EMAIL = "zahrulms01@gmail.com";

	private FirebaseUser currentUser;
	private String currentUserKey = "";
	private DatabaseReference notifRef;
	private ValueEventListener notifListener;

	private FrameLayout rootLayout;
	private ScrollView scrollView;
	private LinearLayout listContainer;
	private LinearLayout emptyStateBox;
	private TextView tvUnreadCount;
	private Typeface appFont;

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
		rootLayout.setBackgroundColor(Color.parseColor("#000000"));

		scrollView = new ScrollView(this);
		scrollView.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		scrollView.setPadding(
			(int) (14 * d),
			statusBarH + (int) (74 * d),
			(int) (14 * d),
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
		emptyStateBox.setPadding((int) (24 * d), (int) (80 * d), (int) (24 * d), (int) (40 * d));
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
		tvEmptyDesc.setText("Saat seseorang membalas pesanmu atau menandai kamu dengan @ di live chat, notifikasinya akan muncul di sini.");
		tvEmptyDesc.setTextColor(Color.parseColor("#64748B"));
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

		// Header glass selaras aplikasi
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

		LinearLayout titleRow = new LinearLayout(this);
		titleRow.setOrientation(LinearLayout.HORIZONTAL);
		titleRow.setGravity(Gravity.CENTER);
		titleRow.setPadding((int) (14 * d), 0, (int) (14 * d), 0);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Notifikasi");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
		styleText(tvTitle);

		tvUnreadCount = new TextView(this);
		tvUnreadCount.setTextColor(Color.parseColor("#38BDF8"));
		tvUnreadCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
		tvUnreadCount.setPadding((int) (8 * d), (int) (2 * d), (int) (8 * d), (int) (2 * d));
		LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		uLp.leftMargin = (int) (8 * d);
		tvUnreadCount.setLayoutParams(uLp);
		GradientDrawable uBg = new GradientDrawable();
		uBg.setColor(Color.parseColor("#2438BDF8"));
		uBg.setCornerRadius(100f);
		tvUnreadCount.setBackground(uBg);
		tvUnreadCount.setVisibility(View.GONE);
		styleText(tvUnreadCount);

		titleRow.addView(tvTitle);
		titleRow.addView(tvUnreadCount);
		titleCapsule.addView(titleRow, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		// Tombol bersihkan semua notifikasi di kanan (ukuran sama dengan tombol back)
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
				if (notifRef != null) {
					notifRef.removeValue();
					DongflixToast.show(NotificationActivity.this, "Semua notifikasi dihapus");
				}
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
				List<HashMap<String, Object>> items = new ArrayList<>();
				int unread = 0;

				if (snapshot.exists()) {
					for (DataSnapshot child : snapshot.getChildren()) {
						Object raw = child.getValue();
						if (raw instanceof Map) {
							HashMap<String, Object> map = new HashMap<>();
							for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
								if (e.getKey() != null) map.put(e.getKey().toString(), e.getValue());
							}
							map.put("_id", child.getKey());
							if (!Boolean.TRUE.equals(map.get("read"))) {
								unread++;
							}
							items.add(map);
						}
					}
				}

				Collections.sort(items, new Comparator<HashMap<String, Object>>() {
					@Override
					public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
						return Long.compare(parseLong(b.get("timestamp")), parseLong(a.get("timestamp")));
					}
				});

				if (tvUnreadCount != null) {
					if (unread > 0) {
						tvUnreadCount.setText(unread + " baru");
						tvUnreadCount.setVisibility(View.VISIBLE);
					} else {
						tvUnreadCount.setVisibility(View.GONE);
					}
				}

				renderNotifications(items);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		};
		notifRef.limitToLast(50).addValueEventListener(notifListener);
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

		for (final HashMap<String, Object> item : items) {
			final String notifId = safe(item, "_id", "");
			String type = safe(item, "type", "mention");
			String actorName = safe(item, "actorName", "Pengguna");
			String actorEmail = safe(item, "actorEmail", "");
			String snippet = safe(item, "snippet", "");
			long exp = parseLong(item.get("actorExp"));
			long ts = parseLong(item.get("timestamp"));
			if (ts <= 0L) ts = System.currentTimeMillis();
			boolean isRead = Boolean.TRUE.equals(item.get("read"));
			boolean isActorAdmin = ADMIN_EMAIL.equalsIgnoreCase(actorEmail) || Boolean.TRUE.equals(item.get("actorIsAdmin"));

			LinearLayout card = new LinearLayout(this);
			card.setOrientation(LinearLayout.VERTICAL);
			card.setPadding((int) (14 * d), (int) (12 * d), (int) (14 * d), (int) (12 * d));
			LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			cardLp.bottomMargin = (int) (10 * d);
			card.setLayoutParams(cardLp);

			GradientDrawable bg = new GradientDrawable();
			bg.setColor(isRead ? Color.parseColor("#0F172A") : Color.parseColor("#131D33"));
			bg.setCornerRadius(16 * d);
			bg.setStroke(
				Math.max(1, (int) (1 * d)),
				isRead ? Color.parseColor("#1E2433") : Color.parseColor("#4438BDF8")
			);
			card.setBackground(bg);

			LinearLayout topRow = new LinearLayout(this);
			topRow.setOrientation(LinearLayout.HORIZONTAL);
			topRow.setGravity(Gravity.CENTER_VERTICAL);

			TextView tvTagType = new TextView(this);
			tvTagType.setText("reply".equalsIgnoreCase(type) ? "BALASAN DI CHAT" : "MENANDAI KAMU (@)");
			tvTagType.setTextColor("reply".equalsIgnoreCase(type) ? Color.parseColor("#38BDF8") : Color.parseColor("#FBBF24"));
			tvTagType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
			tvTagType.setPadding((int) (8 * d), (int) (2 * d), (int) (8 * d), (int) (2 * d));
			GradientDrawable tagBg = new GradientDrawable();
			tagBg.setColor("reply".equalsIgnoreCase(type) ? Color.parseColor("#1E38BDF8") : Color.parseColor("#1EFBBF24"));
			tagBg.setCornerRadius(100f);
			tvTagType.setBackground(tagBg);
			styleText(tvTagType);

			TextView tvTime = new TextView(this);
			tvTime.setText(formatTime(ts));
			tvTime.setTextColor(Color.parseColor("#94A3B8"));
			tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
			LinearLayout.LayoutParams tmLp = new LinearLayout.LayoutParams(
				0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
			);
			tvTime.setLayoutParams(tmLp);
			tvTime.setGravity(Gravity.END);
			styleText(tvTime);

			topRow.addView(tvTagType);
			topRow.addView(tvTime);
			card.addView(topRow);

			LinearLayout actorRow = new LinearLayout(this);
			actorRow.setOrientation(LinearLayout.HORIZONTAL);
			actorRow.setGravity(Gravity.CENTER_VERTICAL);
			LinearLayout.LayoutParams arLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			arLp.topMargin = (int) (8 * d);
			actorRow.setLayoutParams(arLp);

			TextView tvActor = new TextView(this);
			tvActor.setText(actorName);
			tvActor.setTextColor(isActorAdmin ? Color.parseColor("#FBBF24") : Color.WHITE);
			tvActor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
			tvActor.setMaxLines(1);
			styleText(tvActor);

			UserBadgeView badgeView = new UserBadgeView(this);
			badgeView.bindUserBadges(exp, isActorAdmin);
			LinearLayout.LayoutParams bvLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			bvLp.leftMargin = (int) (6 * d);
			badgeView.setLayoutParams(bvLp);

			actorRow.addView(tvActor);
			actorRow.addView(badgeView);
			card.addView(actorRow);

			if (!snippet.isEmpty()) {
				TextView tvBody = new TextView(this);
				tvBody.setText(snippet);
				tvBody.setTextColor(Color.parseColor("#CBD5E1"));
				tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
				tvBody.setMaxLines(3);
				tvBody.setEllipsize(TextUtils.TruncateAt.END);
				LinearLayout.LayoutParams bdLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				bdLp.topMargin = (int) (4 * d);
				tvBody.setLayoutParams(bdLp);
				styleText(tvBody);
				card.addView(tvBody);
			}

			card.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!notifId.isEmpty() && notifRef != null) {
						notifRef.child(notifId).child("read").setValue(true);
					}
					startActivity(new Intent(NotificationActivity.this, ChatActivity.class));
				}
			});

			listContainer.addView(card);
		}
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

	private String formatTime(long ts) {
		return new SimpleDateFormat("dd MMM • HH.mm", new Locale("id", "ID")).format(new Date(ts));
	}

	@Override
	protected void onDestroy() {
		DongflixToast.dismissCurrent();
		if (notifRef != null && notifListener != null) {
			notifRef.removeEventListener(notifListener);
		}
		super.onDestroy();
	}
}
