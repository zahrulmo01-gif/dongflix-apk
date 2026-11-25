package com.dongflix;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";
	private static final String ADMIN_EMAIL = "zahrulms01@gmail.com";
	private static final String PINNED_WARNING_MESSAGE =
		"hati hati penipuan, jangan bagikan info pribadi disini, kami tidak bertanggung jawab jika data pribadi anda disalah gunakan.";

	private static final int ICON_REPLY = 1;
	private static final int ICON_EDIT = 2;
	private static final int ICON_COPY = 3;
	private static final int ICON_REPORT = 4;
	private static final int ICON_DELETE = 5;

	private static final int MAX_VISIBLE_MESSAGES = 25;
	private static final int MAX_PROFILE_FETCH_PER_PASS = 6;
	private static final long FAST_TIMEOUT_MS = 2500L;
	private static final long STICKER_CACHE_TTL_MS = 12L * 60L * 60L * 1000L;

	private static final String[] DEFAULT_STICKERS = {
		"https://cdn-icons-png.flaticon.com/128/9408/9408175.png",
		"https://media.tenor.com/On7KVXhzuf8AAAAj/chibi-cat.gif",
		"https://cdn-icons-png.flaticon.com/128/9408/9408201.png",
		"https://media.tenor.com/2s_jOsl0e8MAAAAj/dance-happy.gif",
		"https://cdn-icons-png.flaticon.com/128/9408/9408166.png",
		"https://media.tenor.com/kHIdycPoP6AAAAAj/anime-cry.gif",
		"https://cdn-icons-png.flaticon.com/128/9408/9408224.png",
		"https://media.tenor.com/gK2J_J7KkE8AAAAj/cute-love.gif"
	};

	private FirebaseUser currentUser;
	private DatabaseReference liveChatRef;
	private DatabaseReference usersRef;
	private ValueEventListener chatListener;
	private SharedPreferences chatCachePrefs;

	private final Handler uiHandler = new Handler(Looper.getMainLooper());
	private Runnable loadTimeoutRunnable;

	private FrameLayout chatRoot;
	private FrameLayout headerOverlayWrapper;
	private LinearLayout chatHeader;
	private ImageView headerProfileAvatar;
	private FrameLayout pinnedMessageCard;
	private LinearLayout chatBottomBar;
	private ScrollView chatScrollView;
	private LinearLayout messagesContainer;

	private LinearLayout replyBanner;
	private TextView replyTargetAuthor, replyTargetSnippet;

	private LinearLayout editBanner;
	private TextView editTargetPreview;
	private String editingMessageId = "";

	private LinearLayout stickerPreviewBox;
	private ImageView stickerPreviewImg;
	private LinearLayout mentionMenu;
	private EditText chatInput;
	private LinearLayout stickerPanel;
	private LinearLayout stickerGridContainer;

	private final Map<String, HashMap<String, Object>> userProfileCache = new HashMap<>();
	private final List<HashMap<String, Object>> allUsersList = new ArrayList<>();
	private final Set<String> requestedUserIds = new HashSet<>();
	private final List<String> stickerUrls = new ArrayList<>();
	private final Map<String, View> messageRowMap = new LinkedHashMap<>();
	private List<HashMap<String, Object>> latestMessages = new ArrayList<>();

	private String selectedStickerUrl = "";
	private String replyToId = "";
	private String replyToUserKey = "";
	private String replyToAuthor = "";
	private String replyToSnippet = "";

	private boolean wasMiniPlayerActiveBeforeChat = false;
	private boolean wasPlayingBeforeChat = false;
	private int baseBottomPaddingPx = 0;

	private ValueAnimator chatShimmerAnimator;
	private float chatShimmerPhase = 0f;
	private boolean isChatLoading = true;
	private boolean isProfileShimmerActive = false;
	private Typeface appFont;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		}

		chatCachePrefs = getSharedPreferences("dongflix_chat_cache", Context.MODE_PRIVATE);
		pauseAndHideMiniPlayerForChat();

		FirebaseDatabase db = FirebaseDatabase.getInstance(FIREBASE_DB_URL);
		liveChatRef = db.getReference("live_chat");
		usersRef = db.getReference("users");

		buildActivityLayout();
		applyAppFontToHierarchy(chatRoot);
		setupKeyboardFollowBehavior();
		showLoadingPlaceholders();
		armFastLoadTimeout();
		loadCachedMessagesIfAny();
		loadCurrentUserProfileOnce();
		loadStickersWithCache();
		startListeningMessages();
	}

	private void armFastLoadTimeout() {
		clearFastLoadTimeout();
		loadTimeoutRunnable = new Runnable() {
			@Override
			public void run() {
				if (isFinishing() || isDestroyed()) return;
				isChatLoading = false;
				isProfileShimmerActive = false;
				stopChatShimmerOnly();
				if (!latestMessages.isEmpty()) {
					renderMessages(latestMessages);
				} else {
					bindHeaderProfileAvatar();
				}
			}
		};
		uiHandler.postDelayed(loadTimeoutRunnable, FAST_TIMEOUT_MS);
	}

	private void clearFastLoadTimeout() {
		if (loadTimeoutRunnable != null) {
			uiHandler.removeCallbacks(loadTimeoutRunnable);
			loadTimeoutRunnable = null;
		}
	}

	private void loadCachedMessagesIfAny() {
		if (chatCachePrefs == null) return;
		String json = chatCachePrefs.getString("last_messages_json", "");
		if (json == null || json.isEmpty()) return;
		try {
			List<HashMap<String, Object>> cached = new Gson().fromJson(
				json, new TypeToken<List<HashMap<String, Object>>>(){}.getType()
			);
			if (cached != null && !cached.isEmpty()) {
				latestMessages = cached;
				isChatLoading = false;
				stopChatShimmerOnly();
				renderMessages(latestMessages);
			}
		} catch (Exception ignored) {}
	}

	private void saveCachedMessages(List<HashMap<String, Object>> msgList) {
		if (chatCachePrefs == null || msgList == null) return;
		try {
			chatCachePrefs.edit().putString("last_messages_json", new Gson().toJson(msgList)).apply();
		} catch (Exception ignored) {}
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

	private void applyAppFontToHierarchy(View root) {
		if (root == null) return;
		if (root instanceof TextView) {
			styleText((TextView) root);
		} else if (root instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) root;
			for (int i = 0; i < vg.getChildCount(); i++) {
				applyAppFontToHierarchy(vg.getChildAt(i));
			}
		}
	}

	private void pauseAndHideMiniPlayerForChat() {
		DongflixPlayerManager mgr = DongflixPlayerManager.getInstance();
		wasMiniPlayerActiveBeforeChat = mgr.isMiniPlayerActive;
		mgr.removeMiniPlayerFromActivity(this);
		mgr.isMiniPlayerActive = false;

		if (mgr.sharedMediaPlayer != null) {
			try {
				wasPlayingBeforeChat = mgr.sharedMediaPlayer.isPlaying();
				if (wasPlayingBeforeChat) {
					mgr.sharedMediaPlayer.pause();
				}
			} catch (Exception ignored) {}
		}
		mgr.updatePlaybackNotification(this);
	}

	private void restoreMiniPlayerAfterChat() {
		DongflixPlayerManager mgr = DongflixPlayerManager.getInstance();
		if (wasMiniPlayerActiveBeforeChat && mgr.sharedMediaPlayer != null) {
			mgr.isMiniPlayerActive = true;
			mgr.attachMiniPlayerToActivity(this);
			if (wasPlayingBeforeChat) {
				try {
					if (!mgr.sharedMediaPlayer.isPlaying()) {
						mgr.sharedMediaPlayer.start();
					}
				} catch (Exception ignored) {}
			}
			mgr.updatePlaybackNotification(this);
		}
	}

	private void setupGlassBlur(BlurView blurView, ViewGroup rootContainer, float radius, float cornerRadiusPx, int overlayColor, int strokeColor) {
		if (blurView == null || rootContainer == null) return;
		float d = getResources().getDisplayMetrics().density;
		final float rPx = cornerRadiusPx;

		if (rPx > 0f) {
			blurView.setOutlineProvider(new ViewOutlineProvider() {
				@Override
				public void getOutline(View view, Outline outline) {
					outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), rPx);
				}
			});
			blurView.setClipToOutline(true);
		} else {
			blurView.setClipToOutline(false);
		}

		Drawable winBg = getWindow().getDecorView().getBackground();
		blurView.setupWith(rootContainer, new RenderScriptBlur(this))
			.setFrameClearDrawable(winBg)
			.setBlurRadius(radius);
		blurView.setOverlayColor(overlayColor);

		if (strokeColor != Color.TRANSPARENT) {
			GradientDrawable glassBorder = new GradientDrawable();
			glassBorder.setColor(Color.TRANSPARENT);
			glassBorder.setCornerRadius(rPx > 0f ? rPx : 0f);
			glassBorder.setStroke((int) (1 * d), strokeColor);
			blurView.setForeground(glassBorder);
		}
	}

	private Drawable createChatShimmerDrawable(final float cornerRadiusPx) {
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

				float translateX = -w + (2f * w * chatShimmerPhase);
				shaderMatrix.setTranslate(translateX, 0);
				gradient.setLocalMatrix(shaderMatrix);
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shimmerPaint);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private void startChatShimmer() {
		stopChatShimmerOnly();
		chatShimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
		chatShimmerAnimator.setDuration(1200);
		chatShimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
		chatShimmerAnimator.setInterpolator(new LinearInterpolator());
		chatShimmerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				if ((!isChatLoading && !isProfileShimmerActive) || messagesContainer == null) return;
				chatShimmerPhase = (float) animation.getAnimatedValue();
				if (headerProfileAvatar != null) {
					headerProfileAvatar.invalidate();
				}
				invalidateShimmerRecursive(messagesContainer);
			}
		});
		chatShimmerAnimator.start();
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

	private void stopChatShimmerOnly() {
		if (chatShimmerAnimator != null) {
			chatShimmerAnimator.cancel();
			chatShimmerAnimator = null;
		}
	}

	private void stopChatShimmer() {
		isChatLoading = false;
		if (!isProfileShimmerActive) {
			stopChatShimmerOnly();
		}
	}

	private void syncProfileShimmerState(boolean hasPendingProfiles) {
		isProfileShimmerActive = hasPendingProfiles;
		if (isProfileShimmerActive && (chatShimmerAnimator == null || !chatShimmerAnimator.isRunning())) {
			startChatShimmer();
		} else if (!isProfileShimmerActive && !isChatLoading) {
			stopChatShimmerOnly();
		}
	}

	private void showLoadingPlaceholders() {
		if (messagesContainer == null) return;
		messagesContainer.removeAllViews();
		isChatLoading = true;

		float d = getResources().getDisplayMetrics().density;
		boolean[] minePattern = {false, true, false, true, false};
		int[] lineWidthsDp = {195, 225, 175, 210, 185};

		for (int i = 0; i < minePattern.length; i++) {
			boolean isMine = minePattern[i];
			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(isMine ? Gravity.END : Gravity.START);
			LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			rowLp.bottomMargin = (int) (12 * d);
			row.setLayoutParams(rowLp);

			View avatarPh = new View(this);
			int avSize = (int) (36 * d);
			LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(avSize, avSize);
			if (isMine) avLp.leftMargin = (int) (8 * d);
			else avLp.rightMargin = (int) (8 * d);
			avatarPh.setLayoutParams(avLp);
			avatarPh.setBackground(createChatShimmerDrawable(100f));

			LinearLayout bubblePh = new LinearLayout(this);
			bubblePh.setOrientation(LinearLayout.VERTICAL);
			bubblePh.setPadding((int) (14 * d), (int) (12 * d), (int) (14 * d), (int) (12 * d));
			bubblePh.setBackground(createChatShimmerDrawable(18 * d));
			LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
				(int) (lineWidthsDp[i] * d), ViewGroup.LayoutParams.WRAP_CONTENT
			);
			bLp.gravity = isMine ? Gravity.END : Gravity.START;
			bubblePh.setLayoutParams(bLp);

			View line1 = new View(this);
			line1.setLayoutParams(new LinearLayout.LayoutParams((int) (110 * d), (int) (10 * d)));
			line1.setBackground(createChatShimmerDrawable(6 * d));

			View line2 = new View(this);
			LinearLayout.LayoutParams l2Lp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (int) (12 * d)
			);
			l2Lp.topMargin = (int) (8 * d);
			line2.setLayoutParams(l2Lp);
			line2.setBackground(createChatShimmerDrawable(6 * d));

			bubblePh.addView(line1);
			bubblePh.addView(line2);

			if (isMine) {
				row.addView(bubblePh);
				row.addView(avatarPh);
			} else {
				row.addView(avatarPh);
				row.addView(bubblePh);
			}
			messagesContainer.addView(row);
		}
		startChatShimmer();
	}

	private long parseLongVal(Object o) {
		if (o == null) return 0L;
		if (o instanceof Number) return ((Number) o).longValue();
		try {
			return Long.parseLong(o.toString().trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private String formatClock(long ts) {
		return new SimpleDateFormat("HH.mm", Locale.getDefault()).format(new Date(ts));
	}

	private String getDateKey(long ts) {
		return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(ts));
	}

	private String getDateDividerLabel(long ts) {
		Calendar msgCal = Calendar.getInstance();
		msgCal.setTimeInMillis(ts);

		Calendar nowCal = Calendar.getInstance();
		if (msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
			&& msgCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
			return "HARI INI";
		}

		Calendar yestCal = Calendar.getInstance();
		yestCal.add(Calendar.DAY_OF_YEAR, -1);
		if (msgCal.get(Calendar.YEAR) == yestCal.get(Calendar.YEAR)
			&& msgCal.get(Calendar.DAY_OF_YEAR) == yestCal.get(Calendar.DAY_OF_YEAR)) {
			return "KEMARIN";
		}

		return new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(new Date(ts)).toUpperCase(Locale.US);
	}

	private void syncScrollPaddingWithHeader() {
		if (headerOverlayWrapper == null || chatScrollView == null) return;
		final float d = getResources().getDisplayMetrics().density;

		headerOverlayWrapper.post(new Runnable() {
			@Override
			public void run() {
				int headerHeight = headerOverlayWrapper.getHeight();
				if (headerHeight <= 0) return;

				int topPad = headerHeight + (int) (4 * d);
				if (chatScrollView.getPaddingTop() != topPad) {
					chatScrollView.setPadding(
						chatScrollView.getPaddingLeft(),
						topPad,
						chatScrollView.getPaddingRight(),
						chatScrollView.getPaddingBottom()
					);
				}
			}
		});
	}

	private void setupKeyboardFollowBehavior() {
		if (chatRoot == null || chatBottomBar == null) return;
		final float d = getResources().getDisplayMetrics().density;
		baseBottomPaddingPx = (int) (26 * d);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		ViewCompat.setOnApplyWindowInsetsListener(chatRoot, new androidx.core.view.OnApplyWindowInsetsListener() {
			@Override
			public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
				Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
				Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
				int bottomInset = Math.max(ime.bottom, nav.bottom);

				chatBottomBar.setPadding(
					(int) (14 * d),
					(int) (6 * d),
					(int) (14 * d),
					baseBottomPaddingPx + bottomInset
				);

				if (chatScrollView != null) {
					int extraScrollPad = (int) (132 * d) + bottomInset;
					chatScrollView.setPadding(
						chatScrollView.getPaddingLeft(),
						chatScrollView.getPaddingTop(),
						chatScrollView.getPaddingRight(),
						extraScrollPad
					);
					syncScrollPaddingWithHeader();
					if (ime.bottom > 0) {
						chatScrollView.postDelayed(new Runnable() {
							@Override
							public void run() {
								chatScrollView.fullScroll(View.FOCUS_DOWN);
							}
						}, 90);
					}
				}
				return insets;
			}
		});

		chatRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			private int lastBottomInset = -1;

			@Override
			public void onGlobalLayout() {
				Rect visibleRect = new Rect();
				chatRoot.getWindowVisibleDisplayFrame(visibleRect);
				int screenHeight = chatRoot.getRootView().getHeight();
				int heightDiff = screenHeight - (visibleRect.bottom - visibleRect.top);
				int keyboardHeight = heightDiff > (int) (140 * d) ? heightDiff : 0;

				if (keyboardHeight != lastBottomInset) {
					lastBottomInset = keyboardHeight;
					chatBottomBar.setPadding(
						(int) (14 * d),
						(int) (6 * d),
						(int) (14 * d),
						baseBottomPaddingPx + keyboardHeight
					);
					if (chatScrollView != null) {
						chatScrollView.setPadding(
							chatScrollView.getPaddingLeft(),
							chatScrollView.getPaddingTop(),
							chatScrollView.getPaddingRight(),
							(int) (132 * d) + keyboardHeight
						);
						syncScrollPaddingWithHeader();
						if (keyboardHeight > 0) {
							chatScrollView.postDelayed(new Runnable() {
								@Override
								public void run() {
									chatScrollView.fullScroll(View.FOCUS_DOWN);
								}
							}, 80);
						}
					}
				}
			}
		});

		if (chatInput != null) {
			chatInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus && chatScrollView != null) {
						chatScrollView.postDelayed(new Runnable() {
							@Override
							public void run() {
								chatScrollView.fullScroll(View.FOCUS_DOWN);
							}
						}, 180);
					}
				}
			});
		}
	}

	private void buildActivityLayout() {
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window w = getWindow();
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.setStatusBarColor(Color.TRANSPARENT);
		}

		float d = getResources().getDisplayMetrics().density;
		int statusBarHeight = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusBarHeight = getResources().getDimensionPixelSize(resId);

		chatRoot = new FrameLayout(this);
		chatRoot.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		chatRoot.setBackgroundColor(Color.parseColor("#050608"));

		chatScrollView = new ScrollView(this);
		chatScrollView.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		chatScrollView.setPadding(
			(int) (14 * d),
			statusBarHeight + (int) (118 * d),
			(int) (14 * d),
			(int) (132 * d)
		);
		chatScrollView.setClipToPadding(false);
		chatScrollView.setVerticalScrollBarEnabled(false);

		messagesContainer = new LinearLayout(this);
		messagesContainer.setOrientation(LinearLayout.VERTICAL);
		messagesContainer.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		chatScrollView.addView(messagesContainer);
		chatRoot.addView(chatScrollView);

		headerOverlayWrapper = new FrameLayout(this);
		headerOverlayWrapper.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP
		));
		headerOverlayWrapper.setBackgroundColor(Color.TRANSPARENT);
		headerOverlayWrapper.setClipChildren(false);
		headerOverlayWrapper.setClipToPadding(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			headerOverlayWrapper.setElevation(16f);
		}

		LinearLayout headerStack = new LinearLayout(this);
		headerStack.setOrientation(LinearLayout.VERTICAL);
		headerStack.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		headerStack.setPadding(0, statusBarHeight + (int) (8 * d), 0, (int) (6 * d));
		headerStack.setClipChildren(false);
		headerStack.setClipToPadding(false);

		int btnSize = (int) (46 * d);
		float pillCorner = 23 * d;

		chatHeader = new LinearLayout(this);
		chatHeader.setOrientation(LinearLayout.HORIZONTAL);
		chatHeader.setGravity(Gravity.CENTER_VERTICAL);
		chatHeader.setClipChildren(false);
		chatHeader.setClipToPadding(false);
		chatHeader.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		chatHeader.setPadding((int) (14 * d), (int) (4 * d), (int) (14 * d), (int) (6 * d));

		FrameLayout backBtn = new FrameLayout(this);
		LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(btnSize, btnSize);
		backLp.rightMargin = (int) (10 * d);
		backBtn.setLayoutParams(backLp);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) backBtn.setElevation(14f);

		BlurView backBlur = new BlurView(this);
		backBlur.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		setupGlassBlur(backBlur, chatRoot, 20f, btnSize / 2f, Color.parseColor("#66141824"), Color.parseColor("#33FFFFFF"));
		backBtn.addView(backBlur);

		ImageView backArrow = new ImageView(this);
		int arrowSize = (int) (20 * d);
		FrameLayout.LayoutParams arrowLp = new FrameLayout.LayoutParams(arrowSize, arrowSize, Gravity.CENTER);
		backArrow.setLayoutParams(arrowLp);
		backArrow.setImageResource(R.drawable.ic_back_arrow);
		backArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		backArrow.setScaleType(ImageView.ScaleType.FIT_CENTER);
		backBtn.addView(backArrow);
		backBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		FrameLayout headerCapsule = new FrameLayout(this);
		LinearLayout.LayoutParams capLp = new LinearLayout.LayoutParams(0, btnSize, 1f);
		capLp.rightMargin = (int) (10 * d);
		headerCapsule.setLayoutParams(capLp);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) headerCapsule.setElevation(14f);

		BlurView headerBlur = new BlurView(this);
		headerBlur.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		setupGlassBlur(headerBlur, chatRoot, 20f, pillCorner, Color.parseColor("#66141824"), Color.parseColor("#33FFFFFF"));
		headerCapsule.addView(headerBlur);

		LinearLayout titleCol = new LinearLayout(this);
		titleCol.setOrientation(LinearLayout.VERTICAL);
		titleCol.setGravity(Gravity.CENTER);
		titleCol.setPadding((int) (14 * d), 0, (int) (14 * d), 0);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Live Chat");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
		styleText(tvTitle);

		TextView tvOnline = new TextView(this);
		tvOnline.setText("online");
		tvOnline.setTextColor(Color.parseColor("#22C55E"));
		tvOnline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
		styleText(tvOnline);

		titleCol.addView(tvTitle);
		titleCol.addView(tvOnline);
		headerCapsule.addView(titleCol, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		FrameLayout profileBtn = new FrameLayout(this);
		profileBtn.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) profileBtn.setElevation(14f);

		BlurView profileBlur = new BlurView(this);
		profileBlur.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		setupGlassBlur(profileBlur, chatRoot, 20f, btnSize / 2f, Color.parseColor("#66141824"), Color.parseColor("#33FFFFFF"));
		profileBtn.addView(profileBlur);

		headerProfileAvatar = new ImageView(this);
		headerProfileAvatar.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		headerProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
		bindHeaderProfileAvatar();
		profileBtn.addView(headerProfileAvatar);
		profileBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showCurrentUserProfileInfo();
			}
		});

		chatHeader.addView(backBtn);
		chatHeader.addView(headerCapsule);
		chatHeader.addView(profileBtn);
		headerStack.addView(chatHeader);

		pinnedMessageCard = buildPinnedMessageBanner(d);
		headerStack.addView(pinnedMessageCard);

		headerOverlayWrapper.addView(headerStack, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		chatRoot.addView(headerOverlayWrapper);
		syncScrollPaddingWithHeader();

		chatBottomBar = new LinearLayout(this);
		chatBottomBar.setOrientation(LinearLayout.VERTICAL);
		FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
		);
		chatBottomBar.setLayoutParams(bottomLp);
		chatBottomBar.setPadding((int) (14 * d), (int) (6 * d), (int) (14 * d), (int) (26 * d));
		chatBottomBar.setClipChildren(false);
		chatBottomBar.setClipToPadding(false);

		editBanner = new LinearLayout(this);
		editBanner.setOrientation(LinearLayout.HORIZONTAL);
		editBanner.setGravity(Gravity.CENTER_VERTICAL);
		GradientDrawable edBg = new GradientDrawable();
		edBg.setColor(Color.parseColor("#F0141720"));
		edBg.setCornerRadius(14 * d);
		editBanner.setBackground(edBg);
		editBanner.setPadding((int) (12 * d), (int) (8 * d), (int) (12 * d), (int) (8 * d));
		LinearLayout.LayoutParams edLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		edLp.bottomMargin = (int) (6 * d);
		editBanner.setLayoutParams(edLp);
		editBanner.setVisibility(View.GONE);

		LinearLayout editTextCol = new LinearLayout(this);
		editTextCol.setOrientation(LinearLayout.VERTICAL);
		editTextCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		TextView editLabel = new TextView(this);
		editLabel.setText("Edit pesan");
		editLabel.setTextColor(Color.parseColor("#38BDF8"));
		editLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		styleText(editLabel);

		editTargetPreview = new TextView(this);
		editTargetPreview.setTextColor(Color.parseColor("#94A3B8"));
		editTargetPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		editTargetPreview.setMaxLines(1);
		editTargetPreview.setEllipsize(TextUtils.TruncateAt.END);
		styleText(editTargetPreview);

		editTextCol.addView(editLabel);
		editTextCol.addView(editTargetPreview);

		TextView cancelEditBtn = new TextView(this);
		cancelEditBtn.setText("✕");
		cancelEditBtn.setTextColor(Color.parseColor("#CBD5E1"));
		cancelEditBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
		cancelEditBtn.setPadding((int) (8 * d), 0, (int) (4 * d), 0);
		styleText(cancelEditBtn);
		cancelEditBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (chatInput != null) {
					chatInput.setText("");
				}
				cancelEditMode();
			}
		});

		editBanner.addView(editTextCol);
		editBanner.addView(cancelEditBtn);
		chatBottomBar.addView(editBanner);

		replyBanner = new LinearLayout(this);
		replyBanner.setOrientation(LinearLayout.HORIZONTAL);
		replyBanner.setGravity(Gravity.CENTER_VERTICAL);
		GradientDrawable repBg = new GradientDrawable();
		repBg.setColor(Color.parseColor("#F0141720"));
		repBg.setCornerRadius(14 * d);
		replyBanner.setBackground(repBg);
		replyBanner.setPadding((int) (12 * d), (int) (8 * d), (int) (12 * d), (int) (8 * d));
		LinearLayout.LayoutParams repLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		repLp.bottomMargin = (int) (6 * d);
		replyBanner.setLayoutParams(repLp);
		replyBanner.setVisibility(View.GONE);

		LinearLayout replyTextCol = new LinearLayout(this);
		replyTextCol.setOrientation(LinearLayout.VERTICAL);
		replyTextCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		replyTargetAuthor = new TextView(this);
		replyTargetAuthor.setTextColor(Color.parseColor("#38BDF8"));
		replyTargetAuthor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		styleText(replyTargetAuthor);

		replyTargetSnippet = new TextView(this);
		replyTargetSnippet.setTextColor(Color.parseColor("#94A3B8"));
		replyTargetSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		replyTargetSnippet.setMaxLines(1);
		replyTargetSnippet.setEllipsize(TextUtils.TruncateAt.END);
		styleText(replyTargetSnippet);

		replyTextCol.addView(replyTargetAuthor);
		replyTextCol.addView(replyTargetSnippet);

		TextView cancelReplyBtn = new TextView(this);
		cancelReplyBtn.setText("✕");
		cancelReplyBtn.setTextColor(Color.parseColor("#CBD5E1"));
		cancelReplyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
		cancelReplyBtn.setPadding((int) (8 * d), 0, (int) (4 * d), 0);
		styleText(cancelReplyBtn);
		cancelReplyBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clearReplyTarget();
			}
		});

		replyBanner.addView(replyTextCol);
		replyBanner.addView(cancelReplyBtn);
		chatBottomBar.addView(replyBanner);

		stickerPreviewBox = new LinearLayout(this);
		stickerPreviewBox.setOrientation(LinearLayout.HORIZONTAL);
		stickerPreviewBox.setGravity(Gravity.CENTER_VERTICAL);
		stickerPreviewBox.setVisibility(View.GONE);
		LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		spLp.bottomMargin = (int) (6 * d);
		stickerPreviewBox.setLayoutParams(spLp);

		stickerPreviewImg = new ImageView(this);
		int prevPx = (int) (58 * d);
		stickerPreviewImg.setLayoutParams(new LinearLayout.LayoutParams(prevPx, prevPx));
		stickerPreviewImg.setScaleType(ImageView.ScaleType.FIT_CENTER);

		TextView removeStickerBtn = new TextView(this);
		removeStickerBtn.setText("  ✕");
		removeStickerBtn.setTextColor(Color.parseColor("#CBD5E1"));
		removeStickerBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		styleText(removeStickerBtn);
		removeStickerBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedStickerUrl = "";
				stickerPreviewBox.setVisibility(View.GONE);
			}
		});

		stickerPreviewBox.addView(stickerPreviewImg);
		stickerPreviewBox.addView(removeStickerBtn);
		chatBottomBar.addView(stickerPreviewBox);

		mentionMenu = new LinearLayout(this);
		mentionMenu.setOrientation(LinearLayout.VERTICAL);
		GradientDrawable mBg = new GradientDrawable();
		mBg.setColor(Color.parseColor("#F212151E"));
		mBg.setCornerRadius(14 * d);
		mentionMenu.setBackground(mBg);
		mentionMenu.setPadding((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));
		LinearLayout.LayoutParams mmLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		mmLp.bottomMargin = (int) (6 * d);
		mentionMenu.setLayoutParams(mmLp);
		mentionMenu.setVisibility(View.GONE);
		chatBottomBar.addView(mentionMenu);

		FrameLayout inputGlassFrame = new FrameLayout(this);
		FrameLayout.LayoutParams igLp = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		inputGlassFrame.setMinimumHeight((int) (50 * d));
		inputGlassFrame.setLayoutParams(igLp);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			inputGlassFrame.setElevation(18f);
		}

		BlurView inputBlur = new BlurView(this);
		inputBlur.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		setupGlassBlur(inputBlur, chatRoot, 20f, 24 * d, Color.parseColor("#66141824"), Color.parseColor("#33FFFFFF"));
		inputGlassFrame.addView(inputBlur);

		LinearLayout inputRow = new LinearLayout(this);
		inputRow.setOrientation(LinearLayout.HORIZONTAL);
		inputRow.setGravity(Gravity.CENTER_VERTICAL);
		inputRow.setMinimumHeight((int) (50 * d));
		inputRow.setPadding((int) (14 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));

		chatInput = new EditText(this);
		LinearLayout.LayoutParams ciLp = new LinearLayout.LayoutParams(
			0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
		);
		ciLp.rightMargin = (int) (6 * d);
		chatInput.setLayoutParams(ciLp);
		chatInput.setBackground(null);
		chatInput.setPadding((int) (2 * d), (int) (6 * d), (int) (2 * d), (int) (6 * d));
		chatInput.setTextColor(Color.WHITE);
		chatInput.setHint("Kirim pesan...");
		chatInput.setHintTextColor(Color.parseColor("#94A3B8"));
		chatInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		chatInput.setLineSpacing(2 * d, 1.05f);
		chatInput.setSingleLine(false);
		chatInput.setMinLines(1);
		chatInput.setMaxLines(3);
		chatInput.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
		styleText(chatInput);

		chatInput.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateMentionDropdown(s.toString());
			}
			@Override public void afterTextChanged(Editable s) {}
		});

		FrameLayout stickerBtn = new FrameLayout(this);
		int stBtnPx = (int) (38 * d);
		LinearLayout.LayoutParams stBtnLp = new LinearLayout.LayoutParams(stBtnPx, stBtnPx);
		stBtnLp.gravity = Gravity.CENTER_VERTICAL;
		stBtnLp.rightMargin = (int) (6 * d);
		stickerBtn.setLayoutParams(stBtnLp);
		GradientDrawable stBtnBg = new GradientDrawable();
		stBtnBg.setShape(GradientDrawable.OVAL);
		stBtnBg.setColor(Color.parseColor("#26FFFFFF"));
		stickerBtn.setBackground(stBtnBg);

		ImageView stickerIcon = new ImageView(this);
		int stIconSize = (int) (20 * d);
		stickerIcon.setLayoutParams(new FrameLayout.LayoutParams(stIconSize, stIconSize, Gravity.CENTER));
		stickerIcon.setImageResource(R.drawable.ic_sticker);
		stickerIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		stickerIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
		stickerBtn.addView(stickerIcon);
		stickerBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleStickerPanel();
			}
		});

		FrameLayout sendBtn = new FrameLayout(this);
		int sendPx = (int) (38 * d);
		LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(sendPx, sendPx);
		sendLp.gravity = Gravity.CENTER_VERTICAL;
		sendBtn.setLayoutParams(sendLp);
		GradientDrawable sendBg = new GradientDrawable();
		sendBg.setShape(GradientDrawable.OVAL);
		sendBg.setColor(Color.parseColor("#38BDF8"));
		sendBtn.setBackground(sendBg);

		ImageView sendIcon = new ImageView(this);
		int sendIconSize = (int) (18 * d);
		sendIcon.setLayoutParams(new FrameLayout.LayoutParams(sendIconSize, sendIconSize, Gravity.CENTER));
		sendIcon.setImageResource(R.drawable.ic_send);
		sendIcon.setColorFilter(Color.parseColor("#07101C"), PorterDuff.Mode.SRC_IN);
		sendIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
		sendBtn.addView(sendIcon);
		sendBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});

		inputRow.addView(chatInput);
		inputRow.addView(stickerBtn);
		inputRow.addView(sendBtn);
		inputGlassFrame.addView(inputRow, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		chatBottomBar.addView(inputGlassFrame);

		stickerPanel = new LinearLayout(this);
		stickerPanel.setOrientation(LinearLayout.VERTICAL);
		GradientDrawable spPanelBg = new GradientDrawable();
		spPanelBg.setColor(Color.parseColor("#F212151E"));
		spPanelBg.setCornerRadius(16 * d);
		stickerPanel.setBackground(spPanelBg);
		stickerPanel.setPadding((int) (12 * d), (int) (10 * d), (int) (12 * d), (int) (10 * d));
		stickerPanel.setVisibility(View.GONE);
		LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		panelLp.topMargin = (int) (8 * d);
		stickerPanel.setLayoutParams(panelLp);

		TextView stTitle = new TextView(this);
		stTitle.setText("Pilih Stiker & GIF");
		stTitle.setTextColor(Color.WHITE);
		stTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		stTitle.setPadding(0, 0, 0, (int) (8 * d));
		styleText(stTitle);

		HorizontalScrollView stScroll = new HorizontalScrollView(this);
		stScroll.setHorizontalScrollBarEnabled(false);
		stickerGridContainer = new LinearLayout(this);
		stickerGridContainer.setOrientation(LinearLayout.HORIZONTAL);
		stScroll.addView(stickerGridContainer);

		stickerPanel.addView(stTitle);
		stickerPanel.addView(stScroll);
		chatBottomBar.addView(stickerPanel);

		chatRoot.addView(chatBottomBar);
		setContentView(chatRoot);
	}

	private FrameLayout buildPinnedMessageBanner(float d) {
		final FrameLayout pinBox = new FrameLayout(this);
		LinearLayout.LayoutParams pinLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		pinLp.leftMargin = (int) (14 * d);
		pinLp.rightMargin = (int) (14 * d);
		pinLp.topMargin = (int) (2 * d);
		pinLp.bottomMargin = (int) (2 * d);
		pinBox.setLayoutParams(pinLp);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			pinBox.setElevation(12f);
		}

		float pinRadius = 18 * d;
		BlurView pinBlur = new BlurView(this);
		pinBlur.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		setupGlassBlur(pinBlur, chatRoot, 20f, pinRadius, Color.parseColor("#80161B28"), Color.parseColor("#33FFFFFF"));
		pinBox.addView(pinBlur);

		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding((int) (12 * d), (int) (9 * d), (int) (12 * d), (int) (9 * d));

		View accentBar = new View(this);
		LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
			Math.max(2, (int) (3 * d)), (int) (34 * d)
		);
		barLp.rightMargin = (int) (10 * d);
		accentBar.setLayoutParams(barLp);
		GradientDrawable barBg = new GradientDrawable();
		barBg.setColor(Color.WHITE);
		barBg.setCornerRadius(100f);
		accentBar.setBackground(barBg);

		LinearLayout textCol = new LinearLayout(this);
		textCol.setOrientation(LinearLayout.VERTICAL);
		textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		TextView tvPinTitle = new TextView(this);
		tvPinTitle.setText("Pesan Tersemat");
		tvPinTitle.setTextColor(Color.WHITE);
		tvPinTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		styleText(tvPinTitle);

		final TextView tvPinBody = new TextView(this);
		tvPinBody.setText(PINNED_WARNING_MESSAGE);
		tvPinBody.setTextColor(Color.parseColor("#E2E8F0"));
		tvPinBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tvPinBody.setMaxLines(2);
		tvPinBody.setEllipsize(TextUtils.TruncateAt.END);
		styleText(tvPinBody);
		LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		bLp.topMargin = (int) (2 * d);
		tvPinBody.setLayoutParams(bLp);

		textCol.addView(tvPinTitle);
		textCol.addView(tvPinBody);

		TextView btnDismissPin = new TextView(this);
		btnDismissPin.setText("✕");
		btnDismissPin.setTextColor(Color.WHITE);
		btnDismissPin.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		btnDismissPin.setGravity(Gravity.CENTER);
		styleText(btnDismissPin);
		LinearLayout.LayoutParams disLp = new LinearLayout.LayoutParams((int) (28 * d), (int) (28 * d));
		disLp.leftMargin = (int) (8 * d);
		btnDismissPin.setLayoutParams(disLp);
		btnDismissPin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pinBox.setVisibility(View.GONE);
				syncScrollPaddingWithHeader();
			}
		});

		row.addView(accentBar);
		row.addView(textCol);
		row.addView(btnDismissPin);

		row.setOnClickListener(new View.OnClickListener() {
			private boolean expanded = false;

			@Override
			public void onClick(View v) {
				expanded = !expanded;
				tvPinBody.setMaxLines(expanded ? 6 : 2);
				syncScrollPaddingWithHeader();
			}
		});

		pinBox.addView(row, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		return pinBox;
	}

	private String resolveUserId(String uid, String email) {
		if (uid != null && !uid.trim().isEmpty()) {
			return uid.trim();
		}
		if (email != null && !email.trim().isEmpty()) {
			return email.trim().toLowerCase(Locale.US).replace(".", ",");
		}
		return "";
	}

	private String resolveNotifUserKey(String uid, String email) {
		if (email != null && !email.trim().isEmpty()) {
			return email.trim().toLowerCase(Locale.US).replace(".", ",");
		}
		if (uid != null && !uid.trim().isEmpty()) {
			return uid.trim();
		}
		return "";
	}

	private String getCurrentUserId() {
		if (currentUser == null) return "";
		String email = currentUser.getEmail() != null ? currentUser.getEmail().trim().toLowerCase(Locale.US) : "";
		return resolveUserId(currentUser.getUid(), email);
	}

	private void cacheProfileEntry(String userId, HashMap<String, Object> profile) {
		if (userId == null || userId.isEmpty() || profile == null) return;
		userProfileCache.put(userId, profile);
		boolean existsInList = false;
		for (HashMap<String, Object> item : allUsersList) {
			if (userId.equals(item.get("_userId"))) {
				existsInList = true;
				break;
			}
		}
		if (!existsInList) {
			HashMap<String, Object> copy = new HashMap<>(profile);
			copy.put("_userId", userId);
			allUsersList.add(copy);
		}
	}

	private void loadCurrentUserProfileOnce() {
		String myId = getCurrentUserId();
		if (myId.isEmpty()) return;

		String cachedJson = chatCachePrefs != null ? chatCachePrefs.getString("profile_" + myId, "") : "";
		if (!cachedJson.isEmpty()) {
			try {
				HashMap<String, Object> cachedMap = new Gson().fromJson(
					cachedJson, new TypeToken<HashMap<String, Object>>(){}.getType()
				);
				if (cachedMap != null) {
					cacheProfileEntry(myId, cachedMap);
					bindHeaderProfileAvatar();
				}
			} catch (Exception ignored) {}
		}

		fetchUserProfileOnceById(myId);
	}

	private void fetchUserProfileOnceById(final String userId) {
		if (userId == null || userId.trim().isEmpty()) return;
		final String cleanId = userId.trim();
		if (userProfileCache.containsKey(cleanId) || requestedUserIds.contains(cleanId)) {
			return;
		}
		requestedUserIds.add(cleanId);

		usersRef.child(cleanId).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (isFinishing() || isDestroyed()) return;
				if (snapshot.exists() && snapshot.getValue() instanceof Map) {
					HashMap<String, Object> uMap = new HashMap<>();
					for (Map.Entry<?, ?> e : ((Map<?, ?>) snapshot.getValue()).entrySet()) {
						if (e.getKey() != null) uMap.put(e.getKey().toString(), e.getValue());
					}
					cacheProfileEntry(cleanId, uMap);
					saveUserProfileToLocalCache(cleanId, uMap);

					if (cleanId.equals(getCurrentUserId())) {
						bindHeaderProfileAvatar();
					}
					if (!latestMessages.isEmpty()) {
						renderMessages(latestMessages);
					}
				}
				syncProfileShimmerState(false);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				syncProfileShimmerState(false);
			}
		});
	}

	private void prefetchProfilesForMessagesOnce(List<HashMap<String, Object>> msgList) {
		if (msgList == null || msgList.isEmpty()) {
			syncProfileShimmerState(false);
			return;
		}
		int fetchedCount = 0;
		for (int i = msgList.size() - 1; i >= 0 && fetchedCount < MAX_PROFILE_FETCH_PER_PASS; i--) {
			HashMap<String, Object> item = msgList.get(i);
			String email = item.get("email") != null ? item.get("email").toString().trim().toLowerCase(Locale.US) : "";
			String uid = item.get("uid") != null ? item.get("uid").toString().trim() : "";
			String userId = resolveUserId(uid, email);
			if (userId.isEmpty() || userProfileCache.containsKey(userId) || requestedUserIds.contains(userId)) {
				continue;
			}

			String cachedJson = chatCachePrefs != null ? chatCachePrefs.getString("profile_" + userId, "") : "";
			if (!cachedJson.isEmpty()) {
				try {
					HashMap<String, Object> cachedMap = new Gson().fromJson(
						cachedJson, new TypeToken<HashMap<String, Object>>(){}.getType()
					);
					if (cachedMap != null) {
						cacheProfileEntry(userId, cachedMap);
						continue;
					}
				} catch (Exception ignored) {}
			}

			fetchedCount++;
			fetchUserProfileOnceById(userId);
		}
		syncProfileShimmerState(false);
	}

	private void saveUserProfileToLocalCache(String key, HashMap<String, Object> uMap) {
		if (chatCachePrefs == null || key.isEmpty() || uMap == null) return;
		try {
			HashMap<String, Object> slim = new HashMap<>(uMap);
			Object photo = slim.get("photoURL");
			if (photo != null && photo.toString().startsWith("data:image")) {
				slim.put("photoURL", "");
			}
			chatCachePrefs.edit().putString("profile_" + key, new Gson().toJson(slim)).apply();
		} catch (Exception ignored) {}
	}

	private void bindHeaderProfileAvatar() {
		if (headerProfileAvatar == null || currentUser == null) return;
		String myId = getCurrentUserId();
		HashMap<String, Object> prof = userProfileCache.get(myId);

		String photoUrl = "";
		if (prof != null && prof.get("photoURL") != null && !prof.get("photoURL").toString().trim().isEmpty()) {
			photoUrl = prof.get("photoURL").toString().trim();
		} else if (currentUser.getPhotoUrl() != null) {
			photoUrl = currentUser.getPhotoUrl().toString().trim();
		}

		bindChatAvatar(headerProfileAvatar, photoUrl);
	}

	private void showCurrentUserProfileInfo() {
		if (currentUser == null) return;
		String email = currentUser.getEmail() != null ? currentUser.getEmail().trim().toLowerCase(Locale.US) : "-";
		String myId = getCurrentUserId();
		HashMap<String, Object> prof = userProfileCache.get(myId);

		String name = (prof != null && prof.get("name") != null && !prof.get("name").toString().trim().isEmpty())
			? prof.get("name").toString().trim()
			: (email.contains("@") ? email.split("@")[0] : "Member");
		String uname = (prof != null && prof.get("username") != null && !prof.get("username").toString().trim().isEmpty())
			? "@" + prof.get("username").toString().trim().replaceFirst("^@", "")
			: "-";
		long exp = prof != null ? parseLongVal(prof.get("exp")) : 0L;
		boolean isAdm = ADMIN_EMAIL.equalsIgnoreCase(email);
		int level = UserBadgeView.computeLevelFromExp(exp);

		new AlertDialog.Builder(this)
			.setTitle(name)
			.setMessage(
				"Username: " + uname
					+ "\nEmail: " + email
					+ "\nLevel: " + (isAdm ? "Lv999+ (Admin)" : "Lv" + level + " (" + exp + " XP)")
			)
			.setPositiveButton("Tutup", null)
			.show();
	}

	private Drawable createVectorActionIcon(final int iconType, final int color) {
		return new Drawable() {
			private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

			{
				paint.setColor(color);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeCap(Paint.Cap.ROUND);
				paint.setStrokeJoin(Paint.Join.ROUND);
			}

			@Override
			public void draw(Canvas canvas) {
				Rect b = getBounds();
				float w = b.width();
				float h = b.height();
				if (w <= 0 || h <= 0) return;

				paint.setStrokeWidth(Math.max(2f, w * 0.1f));
				Path p = new Path();

				if (iconType == ICON_REPLY) {
					p.moveTo(w * 0.42f, h * 0.25f);
					p.lineTo(w * 0.18f, h * 0.46f);
					p.lineTo(w * 0.42f, h * 0.67f);
					p.moveTo(w * 0.20f, h * 0.46f);
					p.quadTo(w * 0.78f, h * 0.44f, w * 0.80f, h * 0.76f);
					canvas.drawPath(p, paint);
				} else if (iconType == ICON_EDIT) {
					p.moveTo(w * 0.64f, h * 0.20f);
					p.lineTo(w * 0.80f, h * 0.36f);
					p.lineTo(w * 0.36f, h * 0.80f);
					p.lineTo(w * 0.18f, h * 0.82f);
					p.lineTo(w * 0.20f, h * 0.64f);
					p.close();
					canvas.drawPath(p, paint);
				} else if (iconType == ICON_COPY) {
					canvas.drawRoundRect(new RectF(w * 0.34f, h * 0.34f, w * 0.80f, h * 0.82f), w * 0.10f, h * 0.10f, paint);
					p.moveTo(w * 0.66f, h * 0.26f);
					p.lineTo(w * 0.66f, h * 0.20f);
					p.lineTo(w * 0.20f, h * 0.20f);
					p.lineTo(w * 0.20f, h * 0.66f);
					p.lineTo(w * 0.26f, h * 0.66f);
					canvas.drawPath(p, paint);
				} else if (iconType == ICON_REPORT) {
					p.moveTo(w * 0.50f, h * 0.18f);
					p.lineTo(w * 0.84f, h * 0.80f);
					p.lineTo(w * 0.16f, h * 0.80f);
					p.close();
					canvas.drawPath(p, paint);
					canvas.drawLine(w * 0.50f, h * 0.42f, w * 0.50f, h * 0.58f, paint);
					canvas.drawPoint(w * 0.50f, h * 0.69f, paint);
				} else if (iconType == ICON_DELETE) {
					canvas.drawLine(w * 0.20f, h * 0.28f, w * 0.80f, h * 0.28f, paint);
					p.moveTo(w * 0.38f, h * 0.28f);
					p.lineTo(w * 0.38f, h * 0.18f);
					p.lineTo(w * 0.62f, h * 0.18f);
					p.lineTo(w * 0.62f, h * 0.28f);
					p.moveTo(w * 0.28f, h * 0.28f);
					p.lineTo(w * 0.32f, h * 0.82f);
					p.lineTo(w * 0.68f, h * 0.82f);
					p.lineTo(w * 0.72f, h * 0.28f);
					canvas.drawPath(p, paint);
					canvas.drawLine(w * 0.44f, h * 0.42f, w * 0.44f, h * 0.68f, paint);
					canvas.drawLine(w * 0.56f, h * 0.42f, w * 0.56f, h * 0.68f, paint);
				}
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private boolean isGifOrStickerOnlyMessage(String text, String stickerUrl) {
		return text.trim().isEmpty() && !stickerUrl.trim().isEmpty();
	}

	private void showMessageActionsBottomSheet(
		final String msgId,
		final String senderNotifKey,
		final String authorName,
		final String text,
		final String stickerUrl,
		final boolean isMine,
		final boolean isCurrentUserAdmin
	) {
		if (isFinishing() || isDestroyed()) return;
		final float d = getResources().getDisplayMetrics().density;
		final boolean isGifOnly = isGifOrStickerOnlyMessage(text, stickerUrl);

		final Dialog sheet = new Dialog(this);
		sheet.requestWindowFeature(Window.FEATURE_NO_TITLE);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (20 * d));
		root.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		GradientDrawable bg = new GradientDrawable();
		bg.setColor(Color.parseColor("#0D0F14"));
		float r = 24 * d;
		bg.setCornerRadii(new float[]{r, r, r, r, 0f, 0f, 0f, 0f});
		root.setBackground(bg);

		View handle = new View(this);
		LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams((int) (36 * d), (int) (4 * d));
		hLp.gravity = Gravity.CENTER_HORIZONTAL;
		hLp.bottomMargin = (int) (14 * d);
		handle.setLayoutParams(hLp);
		GradientDrawable hBg = new GradientDrawable();
		hBg.setColor(Color.parseColor("#3F4451"));
		hBg.setCornerRadius(100f);
		handle.setBackground(hBg);
		root.addView(handle);

		LinearLayout primaryGroup = new LinearLayout(this);
		primaryGroup.setOrientation(LinearLayout.VERTICAL);
		primaryGroup.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		GradientDrawable grpBg = new GradientDrawable();
		grpBg.setColor(Color.parseColor("#161922"));
		grpBg.setCornerRadius(18 * d);
		primaryGroup.setBackground(grpBg);

		primaryGroup.addView(buildDarkSheetRowWithIcon(
			"Balas",
			ICON_REPLY,
			Color.parseColor("#F1F5F9"),
			Color.parseColor("#38BDF8"),
			Color.parseColor("#1E293B"),
			d,
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					sheet.dismiss();
					cancelEditMode();
					replyToId = msgId;
					replyToUserKey = senderNotifKey;
					replyToAuthor = authorName;
					replyToSnippet = !text.isEmpty() ? text : (!stickerUrl.isEmpty() ? "[Stiker/GIF]" : "Pesan");
					if (replyBanner != null && replyTargetAuthor != null && replyTargetSnippet != null) {
						replyTargetAuthor.setText(replyToAuthor);
						replyTargetSnippet.setText(replyToSnippet);
						replyBanner.setVisibility(View.VISIBLE);
					}
					if (chatInput != null) chatInput.requestFocus();
				}
			}
		));

		if (isMine && !text.isEmpty()) {
			primaryGroup.addView(buildDarkSheetDivider(d));
			primaryGroup.addView(buildDarkSheetRowWithIcon(
				"Edit pesan",
				ICON_EDIT,
				Color.parseColor("#F1F5F9"),
				Color.parseColor("#38BDF8"),
				Color.parseColor("#1E293B"),
				d,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sheet.dismiss();
						startInlineEditMessage(msgId, text);
					}
				}
			));
		}

		if (!isGifOnly) {
			primaryGroup.addView(buildDarkSheetDivider(d));
			primaryGroup.addView(buildDarkSheetRowWithIcon(
				"Salin pesan",
				ICON_COPY,
				Color.parseColor("#F1F5F9"),
				Color.parseColor("#38BDF8"),
				Color.parseColor("#1E293B"),
				d,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sheet.dismiss();
						if (text.trim().isEmpty()) {
							DongflixToast.show(ChatActivity.this, "Pesan GIF tidak bisa disalin");
							return;
						}
						ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						if (cm != null) {
							cm.setPrimaryClip(ClipData.newPlainText("Pesan Live Chat", text));
							DongflixToast.show(ChatActivity.this, "Pesan disalin");
						}
					}
				}
			));
		}

		if (!isMine) {
			primaryGroup.addView(buildDarkSheetDivider(d));
			primaryGroup.addView(buildDarkSheetRowWithIcon(
				"Laporkan pesan",
				ICON_REPORT,
				Color.parseColor("#F1F5F9"),
				Color.parseColor("#FBBF24"),
				Color.parseColor("#242018"),
				d,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sheet.dismiss();
						DongflixToast.show(ChatActivity.this, "Pesan dari " + authorName + " telah dilaporkan");
					}
				}
			));
		}

		root.addView(primaryGroup);

		if (isMine || isCurrentUserAdmin) {
			LinearLayout deleteCard = buildDarkSheetRowWithIcon(
				"Hapus",
				ICON_DELETE,
				Color.parseColor("#EF4444"),
				Color.parseColor("#EF4444"),
				Color.parseColor("#2A1519"),
				d,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sheet.dismiss();
						if (msgId.isEmpty()) return;
						if (msgId.equals(editingMessageId)) cancelEditMode();
						new AlertDialog.Builder(ChatActivity.this)
							.setTitle("Hapus Pesan")
							.setMessage("Hapus pesan ini dari live chat?")
							.setPositiveButton("Hapus", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									liveChatRef.child(msgId).removeValue();
								}
							})
							.setNegativeButton("Batal", null)
							.show();
					}
				}
			);
			LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			delLp.topMargin = (int) (10 * d);
			deleteCard.setLayoutParams(delLp);
			GradientDrawable delBg = new GradientDrawable();
			delBg.setColor(Color.parseColor("#1D1215"));
			delBg.setCornerRadius(18 * d);
			deleteCard.setBackground(delBg);
			root.addView(deleteCard);
		}

		sheet.setContentView(root);
		Window win = sheet.getWindow();
		if (win != null) {
			win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			win.setGravity(Gravity.BOTTOM);
			win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			win.setDimAmount(0.72f);
		}
		sheet.show();
	}

	private LinearLayout buildDarkSheetRowWithIcon(
		String label,
		int iconType,
		int textColor,
		int iconTint,
		int iconBadgeColor,
		float d,
		View.OnClickListener listener
	) {
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding((int) (16 * d), (int) (13 * d), (int) (16 * d), (int) (13 * d));
		row.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		FrameLayout iconBadge = new FrameLayout(this);
		int badgeSize = (int) (32 * d);
		LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(badgeSize, badgeSize);
		bLp.rightMargin = (int) (13 * d);
		iconBadge.setLayoutParams(bLp);
		GradientDrawable badgeBg = new GradientDrawable();
		badgeBg.setColor(iconBadgeColor);
		badgeBg.setCornerRadius(10 * d);
		iconBadge.setBackground(badgeBg);

		ImageView iv = new ImageView(this);
		int icSize = (int) (17 * d);
		iv.setLayoutParams(new FrameLayout.LayoutParams(icSize, icSize, Gravity.CENTER));
		iv.setImageDrawable(createVectorActionIcon(iconType, iconTint));
		iconBadge.addView(iv);

		TextView tv = new TextView(this);
		tv.setText(label);
		tv.setTextColor(textColor);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
		styleText(tv);
		tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		row.addView(iconBadge);
		row.addView(tv);
		row.setOnClickListener(listener);
		return row;
	}

	private View buildDarkSheetDivider(float d) {
		View div = new View(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) (1 * d))
		);
		lp.leftMargin = (int) (16 * d);
		lp.rightMargin = (int) (16 * d);
		div.setLayoutParams(lp);
		div.setBackgroundColor(Color.parseColor("#232836"));
		return div;
	}

	private void startInlineEditMessage(String msgId, String originalText) {
		if (msgId.isEmpty() || chatInput == null) return;
		clearReplyTarget();
		editingMessageId = msgId;
		if (editBanner != null) {
			if (editTargetPreview != null) editTargetPreview.setText(originalText);
			editBanner.setVisibility(View.VISIBLE);
		}
		chatInput.setText(originalText);
		chatInput.setSelection(originalText.length());
		chatInput.requestFocus();
	}

	private void cancelEditMode() {
		editingMessageId = "";
		if (editBanner != null) editBanner.setVisibility(View.GONE);
		if (editTargetPreview != null) editTargetPreview.setText("");
	}

	private void toggleStickerPanel() {
		if (stickerPanel == null) return;
		boolean nextVis = stickerPanel.getVisibility() != View.VISIBLE;
		stickerPanel.setVisibility(nextVis ? View.VISIBLE : View.GONE);
		if (nextVis) renderStickerRow();
	}

	private void loadStickersWithCache() {
		stickerUrls.clear();
		Collections.addAll(stickerUrls, DEFAULT_STICKERS);

		long now = System.currentTimeMillis();
		long savedAt = chatCachePrefs != null ? chatCachePrefs.getLong("stickers_ts", 0L) : 0L;
		String cachedJson = chatCachePrefs != null ? chatCachePrefs.getString("stickers_json", "") : "";

		if (!cachedJson.isEmpty()) {
			try {
				List<String> cached = new Gson().fromJson(cachedJson, new TypeToken<List<String>>(){}.getType());
				if (cached != null && !cached.isEmpty()) {
					stickerUrls.clear();
					stickerUrls.addAll(cached);
				}
			} catch (Exception ignored) {}
		}

		if (cachedJson.isEmpty() || (now - savedAt) > STICKER_CACHE_TTL_MS) {
			FirebaseDatabase.getInstance(FIREBASE_DB_URL)
				.getReference("stickers")
				.addListenerForSingleValueEvent(new ValueEventListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot snapshot) {
						if (!snapshot.exists()) return;
						List<String> fetched = new ArrayList<>();
						for (DataSnapshot child : snapshot.getChildren()) {
							Object val = child.getValue();
							if (val instanceof String && !val.toString().trim().isEmpty()) {
								fetched.add(val.toString().trim());
							} else if (val instanceof Map) {
								Map<?, ?> m = (Map<?, ?>) val;
								Object u = m.get("url") != null ? m.get("url") : (m.get("link") != null ? m.get("link") : m.get("image"));
								if (u != null && !u.toString().trim().isEmpty()) {
									fetched.add(u.toString().trim());
								}
							}
						}
						if (!fetched.isEmpty()) {
							stickerUrls.clear();
							stickerUrls.addAll(fetched);
							if (chatCachePrefs != null) {
								chatCachePrefs.edit()
									.putString("stickers_json", new Gson().toJson(fetched))
									.putLong("stickers_ts", System.currentTimeMillis())
									.apply();
							}
							if (stickerPanel != null && stickerPanel.getVisibility() == View.VISIBLE) {
								renderStickerRow();
							}
						}
					}

					@Override
					public void onCancelled(@NonNull DatabaseError error) {}
				});
		}
	}

	private void renderStickerRow() {
		if (stickerGridContainer == null) return;
		stickerGridContainer.removeAllViews();
		float d = getResources().getDisplayMetrics().density;

		for (final String url : stickerUrls) {
			FrameLayout box = new FrameLayout(this);
			int boxSize = (int) (68 * d);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(boxSize, boxSize);
			lp.rightMargin = (int) (8 * d);
			box.setLayoutParams(lp);
			GradientDrawable bg = new GradientDrawable();
			bg.setColor(Color.parseColor("#141A29"));
			bg.setCornerRadius(14 * d);
			box.setBackground(bg);

			ImageView img = new ImageView(this);
			int pad = (int) (6 * d);
			img.setPadding(pad, pad, pad, pad);
			img.setScaleType(ImageView.ScaleType.FIT_CENTER);
			Glide.with(getApplicationContext()).load(Uri.parse(url)).into(img);

			box.addView(img, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
			));
			box.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectedStickerUrl = url;
					if (stickerPreviewImg != null && stickerPreviewBox != null) {
						Glide.with(getApplicationContext()).load(Uri.parse(url)).into(stickerPreviewImg);
						stickerPreviewBox.setVisibility(View.VISIBLE);
					}
					stickerPanel.setVisibility(View.GONE);
				}
			});
			stickerGridContainer.addView(box);
		}
	}

	private void updateMentionDropdown(String fullText) {
		if (mentionMenu == null) return;
		int cursor = chatInput != null ? chatInput.getSelectionStart() : fullText.length();
		String before = fullText.substring(0, Math.max(0, Math.min(cursor, fullText.length())));
		int atIdx = before.lastIndexOf('@');

		if (atIdx == -1 || (atIdx > 0 && !Character.isWhitespace(before.charAt(atIdx - 1)))) {
			mentionMenu.setVisibility(View.GONE);
			return;
		}

		String query = before.substring(atIdx + 1).toLowerCase(Locale.US);
		List<HashMap<String, Object>> matches = new ArrayList<>();
		for (HashMap<String, Object> u : allUsersList) {
			String uname = u.get("username") != null ? u.get("username").toString().toLowerCase(Locale.US) : "";
			String dname = u.get("name") != null ? u.get("name").toString().toLowerCase(Locale.US) : "";
			if (uname.contains(query) || dname.contains(query)) {
				matches.add(u);
			}
			if (matches.size() >= 6) break;
		}

		if (matches.isEmpty()) {
			mentionMenu.setVisibility(View.GONE);
			return;
		}

		mentionMenu.removeAllViews();
		mentionMenu.setVisibility(View.VISIBLE);
		float d = getResources().getDisplayMetrics().density;

		for (HashMap<String, Object> u : matches) {
			String name = u.get("name") != null ? u.get("name").toString() : "User";
			final String tagHandle = u.get("username") != null && !u.get("username").toString().trim().isEmpty()
				? u.get("username").toString().trim()
				: name;

			TextView itemTv = new TextView(this);
			itemTv.setText(name + " (@" + tagHandle + ")");
			itemTv.setTextColor(Color.parseColor("#38BDF8"));
			itemTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
			itemTv.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
			styleText(itemTv);
			itemTv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String currentVal = chatInput.getText().toString();
					int pos = chatInput.getSelectionStart();
					String pre = currentVal.substring(0, Math.max(0, Math.min(pos, currentVal.length())));
					int lastAt = pre.lastIndexOf('@');
					String post = currentVal.substring(Math.max(0, Math.min(pos, currentVal.length())));
					String updated = (lastAt >= 0 ? pre.substring(0, lastAt) : pre) + "@" + tagHandle + " " + post;
					chatInput.setText(updated);
					chatInput.setSelection(Math.min(updated.length(), (lastAt >= 0 ? lastAt : pre.length()) + tagHandle.length() + 2));
					mentionMenu.setVisibility(View.GONE);
				}
			});
			mentionMenu.addView(itemTv);
		}
	}

	private void startListeningMessages() {
		chatListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (isFinishing() || isDestroyed()) return;
				clearFastLoadTimeout();
				isChatLoading = false;
				stopChatShimmerOnly();
				renderSnapshotMessages(snapshot);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				clearFastLoadTimeout();
				isChatLoading = false;
				stopChatShimmerOnly();
			}
		};
		liveChatRef.limitToLast(MAX_VISIBLE_MESSAGES).addValueEventListener(chatListener);
	}

	private void renderSnapshotMessages(@NonNull DataSnapshot snapshot) {
		List<HashMap<String, Object>> msgList = new ArrayList<>();
		if (snapshot.exists()) {
			for (DataSnapshot child : snapshot.getChildren()) {
				Object raw = child.getValue();
				if (raw instanceof Map) {
					HashMap<String, Object> m = new HashMap<>();
					for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
						if (e.getKey() != null) m.put(e.getKey().toString(), e.getValue());
					}
					m.put("_id", child.getKey());
					msgList.add(m);
				}
			}
		}
		Collections.sort(msgList, new Comparator<HashMap<String, Object>>() {
			@Override
			public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
				return Long.compare(parseLongVal(a.get("timestamp")), parseLongVal(b.get("timestamp")));
			}
		});
		if (msgList.size() > MAX_VISIBLE_MESSAGES) {
			msgList = msgList.subList(msgList.size() - MAX_VISIBLE_MESSAGES, msgList.size());
		}
		latestMessages = msgList;
		saveCachedMessages(msgList);
		prefetchProfilesForMessagesOnce(msgList);
		renderMessages(msgList);
	}

	private void renderMessages(List<HashMap<String, Object>> msgList) {
		if (messagesContainer == null) return;
		messagesContainer.removeAllViews();
		messageRowMap.clear();

		float d = getResources().getDisplayMetrics().density;
		String myEmail = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail().toLowerCase(Locale.US) : "";
		final boolean isCurrentUserAdmin = ADMIN_EMAIL.equalsIgnoreCase(myEmail);

		String lastDateKey = "";

		for (final HashMap<String, Object> item : msgList) {
			final String msgId = item.get("_id") != null ? item.get("_id").toString() : "";
			long ts = parseLongVal(item.get("timestamp"));
			if (ts <= 0L) ts = System.currentTimeMillis();

			String dateKey = getDateKey(ts);
			if (!dateKey.equals(lastDateKey)) {
				lastDateKey = dateKey;
				messagesContainer.addView(buildDateDivider(getDateDividerLabel(ts), d));
			}

			String email = item.get("email") != null ? item.get("email").toString() : "";
			String uid = item.get("uid") != null ? item.get("uid").toString() : "";
			String userId = resolveUserId(uid, email);
			final String senderNotifKey = resolveNotifUserKey(uid, email);

			HashMap<String, Object> cachedProf = !userId.isEmpty() ? userProfileCache.get(userId) : null;

			final String authorName = (cachedProf != null && cachedProf.get("name") != null && !cachedProf.get("name").toString().trim().isEmpty())
				? cachedProf.get("name").toString().trim()
				: (item.get("author") != null && !item.get("author").toString().trim().isEmpty()
					? item.get("author").toString().trim()
					: (email.contains("@") ? email.split("@")[0] : "User"));

			String photoUrl = (cachedProf != null && cachedProf.get("photoURL") != null && !cachedProf.get("photoURL").toString().trim().isEmpty())
				? cachedProf.get("photoURL").toString().trim()
				: (item.get("photoURL") != null ? item.get("photoURL").toString().trim() : "");

			long exp = cachedProf != null && cachedProf.get("exp") != null
				? parseLongVal(cachedProf.get("exp"))
				: parseLongVal(item.get("exp"));

			boolean isMsgAdmin = ADMIN_EMAIL.equalsIgnoreCase(email) || Boolean.TRUE.equals(item.get("isAdmin"));
			final boolean isMine = (!myEmail.isEmpty() && myEmail.equalsIgnoreCase(email))
				|| (currentUser != null && currentUser.getUid().equals(uid));
			boolean isEdited = Boolean.TRUE.equals(item.get("edited"));

			final String text = item.get("text") != null ? item.get("text").toString() : "";
			final String stickerUrl = item.get("stickerUrl") != null ? item.get("stickerUrl").toString() : "";

			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setGravity(isMine ? Gravity.END : Gravity.START);
			row.setClipChildren(false);
			LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			rowLp.bottomMargin = (int) (12 * d);
			row.setLayoutParams(rowLp);

			ImageView avatar = new ImageView(this);
			int avSize = (int) (36 * d);
			LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(avSize, avSize);
			if (isMine) avLp.leftMargin = (int) (8 * d);
			else avLp.rightMargin = (int) (8 * d);
			avatar.setLayoutParams(avLp);
			avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
			bindChatAvatar(avatar, photoUrl);

			LinearLayout bubbleCol = new LinearLayout(this);
			bubbleCol.setOrientation(LinearLayout.VERTICAL);
			bubbleCol.setClipChildren(false);
			LinearLayout.LayoutParams bColLp = new LinearLayout.LayoutParams(
				0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
			);
			bubbleCol.setLayoutParams(bColLp);

			LinearLayout bubble = new LinearLayout(this);
			bubble.setOrientation(LinearLayout.VERTICAL);
			bubble.setMinimumWidth((int) (185 * d));
			bubble.setPadding((int) (14 * d), (int) (11 * d), (int) (18 * d), (int) (10 * d));

			GradientDrawable bubbleBg = new GradientDrawable();
			if (isMine) {
				bubbleBg.setColors(new int[]{Color.parseColor("#13294B"), Color.parseColor("#0B172B")});
				bubbleBg.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
				bubbleBg.setCornerRadii(new float[]{
					18 * d, 18 * d,
					6 * d, 6 * d,
					18 * d, 18 * d,
					18 * d, 18 * d
				});
			} else {
				bubbleBg.setColors(new int[]{Color.parseColor("#1E2028"), Color.parseColor("#14161C")});
				bubbleBg.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
				bubbleBg.setCornerRadii(new float[]{
					6 * d, 6 * d,
					18 * d, 18 * d,
					18 * d, 18 * d,
					18 * d, 18 * d
				});
			}
			bubble.setBackground(bubbleBg);
			LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			bLp.gravity = isMine ? Gravity.END : Gravity.START;
			bubble.setLayoutParams(bLp);

			Object replyObj = item.get("replyTo");
			if (replyObj instanceof Map) {
				Map<?, ?> rMap = (Map<?, ?>) replyObj;
				final String targetId = rMap.get("id") != null ? rMap.get("id").toString() : "";
				String rAuthor = rMap.get("author") != null ? rMap.get("author").toString() : "";
				String rSnippet = rMap.get("snippet") != null ? rMap.get("snippet").toString() : "";

				LinearLayout quoteBox = new LinearLayout(this);
				quoteBox.setOrientation(LinearLayout.VERTICAL);
				GradientDrawable qBg = new GradientDrawable();
				qBg.setColor(Color.parseColor("#66070B14"));
				qBg.setCornerRadius(12 * d);
				quoteBox.setBackground(qBg);
				quoteBox.setPadding((int) (10 * d), (int) (6 * d), (int) (12 * d), (int) (6 * d));
				LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				qLp.bottomMargin = (int) (7 * d);
				quoteBox.setLayoutParams(qLp);

				TextView qAuthor = new TextView(this);
				qAuthor.setText(rAuthor);
				qAuthor.setTextColor(Color.parseColor("#38BDF8"));
				qAuthor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
				styleText(qAuthor);

				TextView qSnip = new TextView(this);
				qSnip.setText(rSnippet);
				qSnip.setTextColor(Color.parseColor("#CBD5E1"));
				qSnip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
				qSnip.setMaxLines(2);
				qSnip.setEllipsize(TextUtils.TruncateAt.END);
				styleText(qSnip);

				quoteBox.addView(qAuthor);
				quoteBox.addView(qSnip);
				quoteBox.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						View targetRow = messageRowMap.get(targetId);
						if (targetRow != null && chatScrollView != null) {
							chatScrollView.smoothScrollTo(0, targetRow.getTop());
						}
					}
				});
				bubble.addView(quoteBox);
			}

			LinearLayout metaRow = new LinearLayout(this);
			metaRow.setOrientation(LinearLayout.HORIZONTAL);
			metaRow.setGravity(Gravity.CENTER_VERTICAL);
			metaRow.setPadding(0, 0, (int) (6 * d), 0);

			TextView tvSender = new TextView(this);
			tvSender.setText(authorName);
			tvSender.setTextColor(isMsgAdmin ? Color.parseColor("#FBBF24") : Color.parseColor("#7DD3FC"));
			tvSender.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
			tvSender.setMaxLines(1);
			styleText(tvSender);

			UserBadgeView badgeView = new UserBadgeView(this);
			badgeView.bindUserBadges(exp, isMsgAdmin);
			LinearLayout.LayoutParams bvLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			bvLp.leftMargin = (int) (6 * d);
			badgeView.setLayoutParams(bvLp);

			metaRow.addView(tvSender);
			metaRow.addView(badgeView);
			bubble.addView(metaRow);

			if (!text.isEmpty()) {
				TextView tvBody = new TextView(this);
				tvBody.setText(formatMentionsSpannable(text));
				tvBody.setTextColor(Color.parseColor("#F8FAFC"));
				tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
				styleText(tvBody);
				LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				bodyLp.topMargin = (int) (5 * d);
				tvBody.setLayoutParams(bodyLp);
				bubble.addView(tvBody);
			}

			if (!stickerUrl.isEmpty()) {
				ImageView stImg = new ImageView(this);
				int stSize = (int) (124 * d);
				LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(stSize, stSize);
				stLp.topMargin = (int) (6 * d);
				stImg.setLayoutParams(stLp);
				stImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
				Glide.with(getApplicationContext()).load(Uri.parse(stickerUrl)).into(stImg);
				bubble.addView(stImg);
			}

			TextView tvTime = new TextView(this);
			tvTime.setText((isEdited ? "diedit • " : "") + formatClock(ts));
			tvTime.setTextColor(Color.parseColor("#94A3B8"));
			tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
			styleText(tvTime);
			LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			timeLp.gravity = Gravity.END;
			timeLp.topMargin = (int) (3 * d);
			tvTime.setLayoutParams(timeLp);
			bubble.addView(tvTime);

			bubble.setOnClickListener(null);
			bubble.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					showMessageActionsBottomSheet(msgId, senderNotifKey, authorName, text, stickerUrl, isMine, isCurrentUserAdmin);
					return true;
				}
			});

			bubbleCol.addView(bubble);

			if (isMine) {
				row.addView(bubbleCol);
				row.addView(avatar);
			} else {
				row.addView(avatar);
				row.addView(bubbleCol);
			}

			if (!msgId.isEmpty()) {
				messageRowMap.put(msgId, row);
			}
			messagesContainer.addView(row);
		}

		syncProfileShimmerState(false);

		chatScrollView.post(new Runnable() {
			@Override
			public void run() {
				chatScrollView.fullScroll(View.FOCUS_DOWN);
			}
		});
	}

	private View buildDateDivider(String label, float d) {
		FrameLayout wrap = new FrameLayout(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		lp.topMargin = (int) (6 * d);
		lp.bottomMargin = (int) (10 * d);
		wrap.setLayoutParams(lp);

		TextView badge = new TextView(this);
		badge.setText(label);
		badge.setTextColor(Color.parseColor("#E2E8F0"));
		badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
		styleText(badge);
		badge.setPadding((int) (14 * d), (int) (4 * d), (int) (14 * d), (int) (4 * d));
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(Color.parseColor("#181E2B"));
		bg.setCornerRadius(100f);
		bg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
		badge.setBackground(bg);

		FrameLayout.LayoutParams bLp = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
		);
		wrap.addView(badge, bLp);
		return wrap;
	}

	private SpannableString formatMentionsSpannable(String raw) {
		SpannableString ss = new SpannableString(raw);
		int idx = 0;
		while (idx < raw.length()) {
			int at = raw.indexOf('@', idx);
			if (at < 0) break;
			int end = at + 1;
			while (end < raw.length() && !Character.isWhitespace(raw.charAt(end))) {
				end++;
			}
			if (end > at + 1) {
				ss.setSpan(new ForegroundColorSpan(Color.parseColor("#38BDF8")), at, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				ss.setSpan(new StyleSpan(Typeface.NORMAL), at, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			idx = end;
		}
		return ss;
	}

	private void bindChatAvatar(ImageView target, String photoUrl) {
		target.setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(View view, Outline outline) {
				outline.setOval(0, 0, view.getWidth(), view.getHeight());
			}
		});
		target.setClipToOutline(true);

		if (photoUrl != null && !photoUrl.isEmpty()) {
			target.setPadding(0, 0, 0, 0);
			target.clearColorFilter();
			target.setBackgroundColor(Color.parseColor("#161A24"));
			if (photoUrl.startsWith("data:image")) {
				try {
					String b64 = photoUrl.substring(photoUrl.indexOf(",") + 1);
					byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
					Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
					if (bmp != null) {
						target.setImageBitmap(bmp);
						return;
					}
				} catch (Exception ignored) {}
			} else {
				Glide.with(getApplicationContext())
					.load(Uri.parse(photoUrl))
					.circleCrop()
					.into(target);
				return;
			}
		}

		int pad = (int) (5 * getResources().getDisplayMetrics().density);
		GradientDrawable fallbackBg = new GradientDrawable();
		fallbackBg.setShape(GradientDrawable.OVAL);
		fallbackBg.setColor(Color.parseColor("#1D2436"));
		target.setBackground(fallbackBg);
		target.setScaleType(ImageView.ScaleType.FIT_CENTER);
		target.setPadding(pad, pad, pad, pad);
		target.setImageResource(R.drawable.ic_profile);
		target.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
	}

	private void clearReplyTarget() {
		replyToId = "";
		replyToUserKey = "";
		replyToAuthor = "";
		replyToSnippet = "";
		if (replyBanner != null) replyBanner.setVisibility(View.GONE);
	}

	private void dispatchChatNotifications(
		String text,
		String stickerUrl,
		String senderAuthor,
		String senderUsername,
		String senderEmail,
		long senderExp,
		boolean senderIsAdmin,
		long timestamp
	) {
		FirebaseDatabase db = FirebaseDatabase.getInstance(FIREBASE_DB_URL);
		String snippet = !text.isEmpty() ? text : (!stickerUrl.isEmpty() ? "[Stiker/GIF]" : "Pesan");
		String myNotifKey = resolveNotifUserKey(currentUser != null ? currentUser.getUid() : "", senderEmail);
		Set<String> notifiedKeys = new HashSet<>();

		if (!replyToId.isEmpty() && !replyToUserKey.isEmpty()) {
			String normTarget = replyToUserKey.trim().toLowerCase(Locale.US);
			if (!normTarget.equals(myNotifKey) && notifiedKeys.add(normTarget)) {
				pushNotificationToUser(
					db, replyToUserKey.trim(), "reply",
					senderAuthor, senderUsername, senderEmail, senderExp, senderIsAdmin,
					snippet, timestamp
				);
			}
		}

		if (!text.isEmpty()) {
			String lowerText = text.toLowerCase(Locale.US);
			for (HashMap<String, Object> prof : allUsersList) {
				if (prof == null) continue;
				String uId = prof.get("_userId") != null ? prof.get("_userId").toString() : "";
				String uEmail = prof.get("email") != null ? prof.get("email").toString().trim().toLowerCase(Locale.US) : "";
				String targetKey = resolveNotifUserKey(uId, uEmail);
				if (targetKey.isEmpty() || targetKey.equalsIgnoreCase(myNotifKey)) continue;

				String uname = prof.get("username") != null ? prof.get("username").toString().trim().replaceFirst("^@", "").toLowerCase(Locale.US) : "";
				String dname = prof.get("name") != null ? prof.get("name").toString().trim().toLowerCase(Locale.US) : "";

				boolean tagged = (!uname.isEmpty() && lowerText.contains("@" + uname))
					|| (!dname.isEmpty() && lowerText.contains("@" + dname));

				if (tagged && notifiedKeys.add(targetKey.toLowerCase(Locale.US))) {
					pushNotificationToUser(
						db, targetKey, "mention",
						senderAuthor, senderUsername, senderEmail, senderExp, senderIsAdmin,
						text, timestamp
					);
				}
			}
		}
	}

	private void pushNotificationToUser(
		FirebaseDatabase db,
		String targetUserKey,
		String type,
		String actorName,
		String actorUsername,
		String actorEmail,
		long actorExp,
		boolean actorIsAdmin,
		String snippet,
		long timestamp
	) {
		if (targetUserKey == null || targetUserKey.trim().isEmpty()) return;
		Map<String, Object> notifPayload = new HashMap<>();
		notifPayload.put("type", type);
		notifPayload.put("actorName", actorName);
		notifPayload.put("actorUsername", actorUsername);
		notifPayload.put("actorEmail", actorEmail);
		notifPayload.put("actorExp", actorExp);
		notifPayload.put("actorIsAdmin", actorIsAdmin);
		notifPayload.put("snippet", snippet);
		notifPayload.put("timestamp", timestamp);
		notifPayload.put("read", false);

		String notifId = db.getReference("notifications").child(targetUserKey).push().getKey();
		if (notifId != null) {
			db.getReference("notifications").child(targetUserKey).child(notifId).setValue(notifPayload);
		}
	}

	private void sendMessage() {
		if (currentUser == null || chatInput == null) return;
		String text = chatInput.getText().toString().trim();

		if (!editingMessageId.isEmpty()) {
			if (text.isEmpty()) {
				DongflixToast.show(this, "Pesan tidak boleh kosong");
				return;
			}
			final String targetId = editingMessageId;
			Map<String, Object> updates = new HashMap<>();
			updates.put("text", text);
			updates.put("edited", true);
			liveChatRef.child(targetId).updateChildren(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
				@Override
				public void onComplete(@NonNull Task<Void> task) {
					if (task.isSuccessful()) {
						chatInput.setText("");
						cancelEditMode();
					} else {
						DongflixToast.show(ChatActivity.this, "Gagal menyimpan perubahan pesan");
					}
				}
			});
			return;
		}

		if (text.isEmpty() && selectedStickerUrl.isEmpty()) return;

		String email = currentUser.getEmail() != null ? currentUser.getEmail().trim().toLowerCase(Locale.US) : "";
		String myId = getCurrentUserId();
		HashMap<String, Object> prof = userProfileCache.get(myId);

		final String author = (prof != null && prof.get("name") != null && !prof.get("name").toString().trim().isEmpty())
			? prof.get("name").toString().trim()
			: (email.contains("@") ? email.split("@")[0] : "User");

		final String username = (prof != null && prof.get("username") != null)
			? prof.get("username").toString()
			: (email.contains("@") ? email.split("@")[0] : "");

		String photo = (prof != null && prof.get("photoURL") != null)
			? prof.get("photoURL").toString()
			: "";

		final long exp = prof != null && prof.get("exp") != null ? parseLongVal(prof.get("exp")) : 0L;
		final boolean isAdm = ADMIN_EMAIL.equalsIgnoreCase(email);
		final long nowTs = System.currentTimeMillis();
		final String sentText = text;
		final String sentSticker = selectedStickerUrl;

		HashMap<String, Object> payload = new HashMap<>();
		payload.put("uid", currentUser.getUid());
		payload.put("email", email);
		payload.put("author", author);
		payload.put("username", username);
		payload.put("photoURL", photo);
		payload.put("exp", exp);
		payload.put("text", sentText);
		payload.put("stickerUrl", sentSticker);
		payload.put("timestamp", nowTs);
		payload.put("isAdmin", isAdm);

		if (!replyToId.isEmpty()) {
			HashMap<String, Object> repMap = new HashMap<>();
			repMap.put("id", replyToId);
			repMap.put("userKey", replyToUserKey);
			repMap.put("author", replyToAuthor);
			repMap.put("snippet", replyToSnippet);
			payload.put("replyTo", repMap);
		}

		String key = String.valueOf(nowTs);
		liveChatRef.child(key).setValue(payload).addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				if (task.isSuccessful()) {
					dispatchChatNotifications(sentText, sentSticker, author, username, email, exp, isAdm, nowTs);
					chatInput.setText("");
					selectedStickerUrl = "";
					if (stickerPreviewBox != null) stickerPreviewBox.setVisibility(View.GONE);
					clearReplyTarget();
				} else {
					DongflixToast.show(ChatActivity.this, "Gagal mengirim pesan");
				}
			}
		});
	}

	@Override
	public void finish() {
		DongflixToast.dismissCurrent();
		restoreMiniPlayerAfterChat();
		super.finish();
	}

	@Override
	protected void onDestroy() {
		clearFastLoadTimeout();
		isChatLoading = false;
		isProfileShimmerActive = false;
		stopChatShimmerOnly();
		DongflixToast.dismissCurrent();
		if (liveChatRef != null && chatListener != null) {
			liveChatRef.removeEventListener(chatListener);
		}
		super.onDestroy();
	}
}
