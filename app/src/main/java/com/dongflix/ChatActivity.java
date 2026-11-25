package com.dongflix;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.Toast;
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
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";
	private static final String ADMIN_EMAIL = "zahrulms01@gmail.com";
	private static final String PINNED_WARNING_MESSAGE =
		"hati hati penipuan, jangan bagikan info pribadi disini, kami tidak bertanggung jawab jika data pribadi anda disalah gunakan.";

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
	private LinearLayout stickerPreviewBox;
	private ImageView stickerPreviewImg;
	private LinearLayout mentionMenu;
	private EditText chatInput;
	private LinearLayout stickerPanel;
	private LinearLayout stickerGridContainer;

	private final Map<String, HashMap<String, Object>> userProfileCache = new HashMap<>();
	private final List<HashMap<String, Object>> allUsersList = new ArrayList<>();
	private final List<String> stickerUrls = new ArrayList<>();
	private final Map<String, View> messageRowMap = new LinkedHashMap<>();

	private String selectedStickerUrl = "";
	private String replyToId = "";
	private String replyToAuthor = "";
	private String replyToSnippet = "";

	private boolean wasMiniPlayerActiveBeforeChat = false;
	private boolean wasPlayingBeforeChat = false;
	private int baseBottomPaddingPx = 0;

	private ValueAnimator chatShimmerAnimator;
	private float chatShimmerPhase = 0f;
	private boolean isChatLoading = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		}

		pauseAndHideMiniPlayerForChat();

		FirebaseDatabase db = FirebaseDatabase.getInstance(FIREBASE_DB_URL);
		liveChatRef = db.getReference("live_chat");
		usersRef = db.getReference("users");

		buildActivityLayout();
		setupKeyboardFollowBehavior();
		showLoadingPlaceholders();
		loadAllUsersDirectory();
		loadStickersFromFirebase();
		startListeningMessages();
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
		isChatLoading = true;
		stopChatShimmerOnly();
		chatShimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
		chatShimmerAnimator.setDuration(1200);
		chatShimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
		chatShimmerAnimator.setInterpolator(new LinearInterpolator());
		chatShimmerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				if (!isChatLoading || messagesContainer == null) return;
				chatShimmerPhase = (float) animation.getAnimatedValue();
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
		stopChatShimmerOnly();
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

	private int calculateLevel(long exp) {
		if (exp <= 0) return 1;
		return (int) ((exp - 1) / 1000L) + 1;
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

	private void updateScrollTopPaddingForHeader() {
		if (chatScrollView == null || headerOverlayWrapper == null) return;
		final float d = getResources().getDisplayMetrics().density;
		headerOverlayWrapper.post(new Runnable() {
			@Override
			public void run() {
				int h = headerOverlayWrapper.getHeight();
				int topPad = h > 0 ? h + (int) (8 * d) : (int) (136 * d);
				chatScrollView.setPadding(
					chatScrollView.getPaddingLeft(),
					topPad,
					chatScrollView.getPaddingRight(),
					chatScrollView.getPaddingBottom()
				);
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
			statusBarHeight + (int) (136 * d),
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

		// Wrapper header + gradiasi hitam tipis (tanpa blur di belakang header) + pesan tersemat
		headerOverlayWrapper = new FrameLayout(this);
		headerOverlayWrapper.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP
		));
		headerOverlayWrapper.setClipChildren(false);
		headerOverlayWrapper.setClipToPadding(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			headerOverlayWrapper.setElevation(16f);
		}

		View topThinGradient = new View(this);
		topThinGradient.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		topThinGradient.setBackground(new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{
				Color.parseColor("#B8000000"),
				Color.parseColor("#66000000"),
				Color.TRANSPARENT
			}
		));
		headerOverlayWrapper.addView(topThinGradient);

		LinearLayout headerStack = new LinearLayout(this);
		headerStack.setOrientation(LinearLayout.VERTICAL);
		headerStack.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		headerStack.setPadding(0, statusBarHeight + (int) (8 * d), 0, (int) (8 * d));
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
		chatHeader.setPadding((int) (14 * d), (int) (4 * d), (int) (14 * d), (int) (8 * d));

		// Tombol back bulat kiri (ukuran sama dengan tombol profil kanan)
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

		// Kapsul tengah judul & status online
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
		tvTitle.setTypeface(null, Typeface.BOLD);

		TextView tvOnline = new TextView(this);
		tvOnline.setText("online");
		tvOnline.setTextColor(Color.parseColor("#22C55E"));
		tvOnline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
		tvOnline.setTypeface(null, Typeface.BOLD);

		titleCol.addView(tvTitle);
		titleCol.addView(tvOnline);
		headerCapsule.addView(titleCol, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		// Tombol profil bulat kanan (ukuran persis sama dengan tombol back kiri)
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

		// Banner Pesan Tersemat tepat di bawah header
		pinnedMessageCard = buildPinnedMessageBanner(d);
		headerStack.addView(pinnedMessageCard);

		headerOverlayWrapper.addView(headerStack, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		chatRoot.addView(headerOverlayWrapper);
		updateScrollTopPaddingForHeader();

		chatBottomBar = new LinearLayout(this);
		chatBottomBar.setOrientation(LinearLayout.VERTICAL);
		FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
		);
		chatBottomBar.setLayoutParams(bottomLp);
		chatBottomBar.setPadding((int) (14 * d), (int) (6 * d), (int) (14 * d), (int) (26 * d));
		chatBottomBar.setClipChildren(false);
		chatBottomBar.setClipToPadding(false);

		replyBanner = new LinearLayout(this);
		replyBanner.setOrientation(LinearLayout.HORIZONTAL);
		replyBanner.setGravity(Gravity.CENTER_VERTICAL);
		GradientDrawable repBg = new GradientDrawable();
		repBg.setColor(Color.parseColor("#E6131926"));
		repBg.setCornerRadius(14 * d);
		repBg.setStroke((int) (1 * d), Color.parseColor("#2E38BDF8"));
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
		replyTargetAuthor.setTypeface(null, Typeface.BOLD);

		replyTargetSnippet = new TextView(this);
		replyTargetSnippet.setTextColor(Color.parseColor("#CBD5E1"));
		replyTargetSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		replyTargetSnippet.setMaxLines(1);
		replyTargetSnippet.setEllipsize(TextUtils.TruncateAt.END);

		replyTextCol.addView(replyTargetAuthor);
		replyTextCol.addView(replyTargetSnippet);

		TextView cancelReplyBtn = new TextView(this);
		cancelReplyBtn.setText("✕");
		cancelReplyBtn.setTextColor(Color.parseColor("#94A3B8"));
		cancelReplyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
		cancelReplyBtn.setPadding((int) (8 * d), 0, (int) (4 * d), 0);
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
		removeStickerBtn.setTextColor(Color.parseColor("#EF4444"));
		removeStickerBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		removeStickerBtn.setTypeface(null, Typeface.BOLD);
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
		mBg.setColor(Color.parseColor("#E60D1424"));
		mBg.setCornerRadius(14 * d);
		mBg.setStroke((int) (1 * d), Color.parseColor("#26FFFFFF"));
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
		inputGlassFrame.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (52 * d)
		));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			inputGlassFrame.setElevation(18f);
		}

		BlurView inputBlur = new BlurView(this);
		inputBlur.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		setupGlassBlur(inputBlur, chatRoot, 20f, 26 * d, Color.parseColor("#66141824"), Color.parseColor("#33FFFFFF"));
		inputGlassFrame.addView(inputBlur);

		LinearLayout inputRow = new LinearLayout(this);
		inputRow.setOrientation(LinearLayout.HORIZONTAL);
		inputRow.setGravity(Gravity.CENTER_VERTICAL);
		inputRow.setPadding((int) (16 * d), (int) (4 * d), (int) (6 * d), (int) (4 * d));

		chatInput = new EditText(this);
		chatInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
		chatInput.setBackground(null);
		chatInput.setTextColor(Color.WHITE);
		chatInput.setHint("Kirim pesan...");
		chatInput.setHintTextColor(Color.parseColor("#94A3B8"));
		chatInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		chatInput.setSingleLine(true);

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
		int sendPx = (int) (40 * d);
		sendBtn.setLayoutParams(new LinearLayout.LayoutParams(sendPx, sendPx));
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
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		chatBottomBar.addView(inputGlassFrame);

		stickerPanel = new LinearLayout(this);
		stickerPanel.setOrientation(LinearLayout.VERTICAL);
		GradientDrawable spPanelBg = new GradientDrawable();
		spPanelBg.setColor(Color.parseColor("#E60B0F1A"));
		spPanelBg.setCornerRadius(16 * d);
		spPanelBg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
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
		stTitle.setTypeface(null, Typeface.BOLD);
		stTitle.setPadding(0, 0, 0, (int) (8 * d));

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
		pinLp.bottomMargin = (int) (4 * d);
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
		tvPinTitle.setTypeface(null, Typeface.BOLD);

		final TextView tvPinBody = new TextView(this);
		tvPinBody.setText(PINNED_WARNING_MESSAGE);
		tvPinBody.setTextColor(Color.parseColor("#E2E8F0"));
		tvPinBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tvPinBody.setMaxLines(2);
		tvPinBody.setEllipsize(TextUtils.TruncateAt.END);
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
		LinearLayout.LayoutParams disLp = new LinearLayout.LayoutParams((int) (28 * d), (int) (28 * d));
		disLp.leftMargin = (int) (8 * d);
		btnDismissPin.setLayoutParams(disLp);
		btnDismissPin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pinBox.setVisibility(View.GONE);
				updateScrollTopPaddingForHeader();
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
				updateScrollTopPaddingForHeader();
			}
		});

		pinBox.addView(row, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		return pinBox;
	}

	private void bindHeaderProfileAvatar() {
		if (headerProfileAvatar == null || currentUser == null) return;
		String email = currentUser.getEmail() != null ? currentUser.getEmail().trim().toLowerCase(Locale.US) : "";
		String uKey = !email.isEmpty() ? email.replace(".", ",") : currentUser.getUid();
		HashMap<String, Object> prof = userProfileCache.get(uKey);

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
		String uKey = !email.isEmpty() ? email.replace(".", ",") : currentUser.getUid();
		HashMap<String, Object> prof = userProfileCache.get(uKey);

		String name = (prof != null && prof.get("name") != null && !prof.get("name").toString().trim().isEmpty())
			? prof.get("name").toString().trim()
			: (email.contains("@") ? email.split("@")[0] : "Member");
		String uname = (prof != null && prof.get("username") != null && !prof.get("username").toString().trim().isEmpty())
			? "@" + prof.get("username").toString().trim().replaceFirst("^@", "")
			: "-";
		long exp = prof != null ? parseLongVal(prof.get("exp")) : 0L;
		boolean isAdm = ADMIN_EMAIL.equalsIgnoreCase(email);

		new AlertDialog.Builder(this)
			.setTitle(name)
			.setMessage(
				"Username: " + uname
					+ "\nEmail: " + email
					+ "\nLevel: " + (isAdm ? "Lv999+ (Admin)" : "Lv" + calculateLevel(exp) + " (" + exp + " XP)")
			)
			.setPositiveButton("Tutup", null)
			.show();
	}

	private void showMessageActionsBottomSheet(
		final String msgId,
		final String authorName,
		final String text,
		final String stickerUrl,
		final boolean isMine
	) {
		if (isFinishing() || isDestroyed()) return;
		final float d = getResources().getDisplayMetrics().density;

		final Dialog sheet = new Dialog(this);
		sheet.requestWindowFeature(Window.FEATURE_NO_TITLE);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int) (18 * d), (int) (10 * d), (int) (18 * d), (int) (22 * d));
		root.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		GradientDrawable bg = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{Color.parseColor("#141B2B"), Color.parseColor("#090C14")}
		);
		float r = 24 * d;
		bg.setCornerRadii(new float[]{r, r, r, r, 0f, 0f, 0f, 0f});
		bg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#2EFFFFFF"));
		root.setBackground(bg);

		View handle = new View(this);
		LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams((int) (38 * d), (int) (4 * d));
		hLp.gravity = Gravity.CENTER_HORIZONTAL;
		hLp.bottomMargin = (int) (12 * d);
		handle.setLayoutParams(hLp);
		GradientDrawable hBg = new GradientDrawable();
		hBg.setColor(Color.parseColor("#475569"));
		hBg.setCornerRadius(100f);
		handle.setBackground(hBg);
		root.addView(handle);

		TextView tvHeader = new TextView(this);
		tvHeader.setText("Opsi Pesan • " + authorName);
		tvHeader.setTextColor(Color.parseColor("#94A3B8"));
		tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
		LinearLayout.LayoutParams hdLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		hdLp.bottomMargin = (int) (10 * d);
		tvHeader.setLayoutParams(hdLp);
		root.addView(tvHeader);

		// 1. Balas
		root.addView(buildSheetActionRow("Balas", Color.WHITE, d, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sheet.dismiss();
				replyToId = msgId;
				replyToAuthor = authorName;
				replyToSnippet = !text.isEmpty() ? text : (!stickerUrl.isEmpty() ? "[Stiker/GIF]" : "Pesan");
				if (replyBanner != null && replyTargetAuthor != null && replyTargetSnippet != null) {
					replyTargetAuthor.setText(replyToAuthor);
					replyTargetSnippet.setText(replyToSnippet);
					replyBanner.setVisibility(View.VISIBLE);
				}
				if (chatInput != null) chatInput.requestFocus();
			}
		}));

		// 2. Salin Pesan
		root.addView(buildSheetActionRow("Salin Pesan", Color.WHITE, d, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sheet.dismiss();
				String toCopy = !text.isEmpty() ? text : (!stickerUrl.isEmpty() ? stickerUrl : "");
				if (toCopy.isEmpty()) {
					Toast.makeText(ChatActivity.this, "Tidak ada teks untuk disalin", Toast.LENGTH_SHORT).show();
					return;
				}
				ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				if (cm != null) {
					cm.setPrimaryClip(ClipData.newPlainText("Pesan Live Chat", toCopy));
					Toast.makeText(ChatActivity.this, "Pesan disalin", Toast.LENGTH_SHORT).show();
				}
			}
		}));

		if (isMine) {
			// 3a. Edit Pesan (khusus pesan sendiri)
			if (!text.isEmpty()) {
				root.addView(buildSheetActionRow("Edit Pesan", Color.parseColor("#38BDF8"), d, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sheet.dismiss();
						showEditMessageDialog(msgId, text);
					}
				}));
			}

			// 4a. Hapus Pesan (khusus pesan sendiri)
			root.addView(buildSheetActionRow("Hapus Pesan", Color.parseColor("#F87171"), d, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					sheet.dismiss();
					if (msgId.isEmpty()) return;
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
			}));
		} else {
			// 3b. Laporkan Pesan (hanya untuk pesan orang lain)
			root.addView(buildSheetActionRow("Laporkan Pesan", Color.parseColor("#FBBF24"), d, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					sheet.dismiss();
					Toast.makeText(ChatActivity.this, "Pesan dari " + authorName + " telah dilaporkan", Toast.LENGTH_SHORT).show();
				}
			}));
		}

		sheet.setContentView(root);
		Window win = sheet.getWindow();
		if (win != null) {
			win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			win.setGravity(Gravity.BOTTOM);
			win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			win.setDimAmount(0.6f);
		}
		sheet.show();
	}

	private TextView buildSheetActionRow(String label, int textColor, float d, View.OnClickListener listener) {
		TextView tv = new TextView(this);
		tv.setText(label);
		tv.setTextColor(textColor);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
		tv.setTypeface(null, Typeface.BOLD);
		tv.setPadding((int) (14 * d), (int) (13 * d), (int) (14 * d), (int) (13 * d));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		lp.bottomMargin = (int) (6 * d);
		tv.setLayoutParams(lp);
		GradientDrawable rowBg = new GradientDrawable();
		rowBg.setColor(Color.parseColor("#181F30"));
		rowBg.setCornerRadius(14 * d);
		tv.setBackground(rowBg);
		tv.setOnClickListener(listener);
		return tv;
	}

	private void showEditMessageDialog(final String msgId, final String currentText) {
		if (msgId.isEmpty()) return;
		float d = getResources().getDisplayMetrics().density;

		final EditText input = new EditText(this);
		input.setText(currentText);
		input.setSelection(currentText.length());
		input.setTextColor(Color.WHITE);
		input.setHint("Edit isi pesan...");
		input.setHintTextColor(Color.parseColor("#64748B"));
		int pad = (int) (12 * d);
		input.setPadding(pad, pad, pad, pad);

		new AlertDialog.Builder(this)
			.setTitle("Edit Pesan")
			.setView(input)
			.setPositiveButton("Simpan", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String updated = input.getText().toString().trim();
					if (updated.isEmpty()) {
						Toast.makeText(ChatActivity.this, "Pesan tidak boleh kosong", Toast.LENGTH_SHORT).show();
						return;
					}
					Map<String, Object> updates = new HashMap<>();
					updates.put("text", updated);
					updates.put("edited", true);
					liveChatRef.child(msgId).updateChildren(updates);
				}
			})
			.setNegativeButton("Batal", null)
			.show();
	}

	private void toggleStickerPanel() {
		if (stickerPanel == null) return;
		boolean nextVis = stickerPanel.getVisibility() != View.VISIBLE;
		stickerPanel.setVisibility(nextVis ? View.VISIBLE : View.GONE);
		if (nextVis) renderStickerRow();
	}

	private void loadStickersFromFirebase() {
		stickerUrls.clear();
		Collections.addAll(stickerUrls, DEFAULT_STICKERS);

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
						if (stickerPanel != null && stickerPanel.getVisibility() == View.VISIBLE) {
							renderStickerRow();
						}
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {}
			});
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
			bg.setStroke((int) (1 * d), Color.parseColor("#22FFFFFF"));
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

	private void loadAllUsersDirectory() {
		usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				allUsersList.clear();
				userProfileCache.clear();
				if (!snapshot.exists()) return;
				for (DataSnapshot child : snapshot.getChildren()) {
					Object raw = child.getValue();
					if (raw instanceof Map) {
						HashMap<String, Object> uMap = new HashMap<>();
						for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
							if (e.getKey() != null) uMap.put(e.getKey().toString(), e.getValue());
						}
						String key = child.getKey() != null ? child.getKey() : "";
						userProfileCache.put(key.toLowerCase(Locale.US), uMap);
						allUsersList.add(uMap);
					}
				}
				bindHeaderProfileAvatar();
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		});
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
				stopChatShimmer();
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
				if (msgList.size() > 60) {
					msgList = msgList.subList(msgList.size() - 60, msgList.size());
				}
				renderMessages(msgList);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				stopChatShimmer();
			}
		};
		liveChatRef.limitToLast(60).addValueEventListener(chatListener);
	}

	private void renderMessages(List<HashMap<String, Object>> msgList) {
		if (messagesContainer == null) return;
		messagesContainer.removeAllViews();
		messageRowMap.clear();

		float d = getResources().getDisplayMetrics().density;
		String myEmail = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail().toLowerCase(Locale.US) : "";

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
			String uKey = !email.isEmpty()
				? email.trim().toLowerCase(Locale.US).replace(".", ",")
				: (item.get("uid") != null ? item.get("uid").toString() : "");

			HashMap<String, Object> cachedProf = userProfileCache.get(uKey);
			final String authorName = (cachedProf != null && cachedProf.get("name") != null && !cachedProf.get("name").toString().trim().isEmpty())
				? cachedProf.get("name").toString().trim()
				: (item.get("author") != null ? item.get("author").toString() : (email.contains("@") ? email.split("@")[0] : "User"));

			String photoUrl = (cachedProf != null && cachedProf.get("photoURL") != null && !cachedProf.get("photoURL").toString().trim().isEmpty())
				? cachedProf.get("photoURL").toString().trim()
				: (item.get("photoURL") != null ? item.get("photoURL").toString().trim() : "");

			long exp = cachedProf != null && cachedProf.get("exp") != null
				? parseLongVal(cachedProf.get("exp"))
				: parseLongVal(item.get("exp"));

			boolean isMsgAdmin = ADMIN_EMAIL.equalsIgnoreCase(email) || Boolean.TRUE.equals(item.get("isAdmin"));
			final boolean isMine = (!myEmail.isEmpty() && myEmail.equalsIgnoreCase(email))
				|| (currentUser != null && currentUser.getUid().equals(item.get("uid")));
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
				bubbleBg.setColors(new int[]{Color.parseColor("#1D3358"), Color.parseColor("#13213B")});
				bubbleBg.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
				bubbleBg.setCornerRadii(new float[]{
					18 * d, 18 * d,
					6 * d, 6 * d,
					18 * d, 18 * d,
					18 * d, 18 * d
				});
				bubbleBg.setStroke((int) (1 * d), Color.parseColor("#5538BDF8"));
			} else {
				bubbleBg.setColors(new int[]{Color.parseColor("#181C26"), Color.parseColor("#11141C")});
				bubbleBg.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
				bubbleBg.setCornerRadii(new float[]{
					6 * d, 6 * d,
					18 * d, 18 * d,
					18 * d, 18 * d,
					18 * d, 18 * d
				});
				bubbleBg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
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
				qBg.setStroke((int) (1 * d), Color.parseColor("#2638BDF8"));
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
				qAuthor.setTypeface(null, Typeface.BOLD);

				TextView qSnip = new TextView(this);
				qSnip.setText(rSnippet);
				qSnip.setTextColor(Color.parseColor("#CBD5E1"));
				qSnip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
				qSnip.setMaxLines(2);
				qSnip.setEllipsize(TextUtils.TruncateAt.END);

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
			tvSender.setTypeface(null, Typeface.BOLD);
			tvSender.setMaxLines(1);

			TextView tvLv = new TextView(this);
			tvLv.setText(isMsgAdmin ? "Lv999+" : "Lv" + calculateLevel(exp));
			tvLv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
			tvLv.setTypeface(null, Typeface.BOLD);
			tvLv.setMaxLines(1);
			tvLv.setTextColor(isMsgAdmin ? Color.parseColor("#451A03") : Color.parseColor("#38BDF8"));
			GradientDrawable lvBg = new GradientDrawable();
			lvBg.setCornerRadius(100f);
			lvBg.setColor(isMsgAdmin ? Color.parseColor("#FBBF24") : Color.parseColor("#2638BDF8"));
			tvLv.setBackground(lvBg);
			tvLv.setPadding((int) (6 * d), (int) (1.5f * d), (int) (6 * d), (int) (1.5f * d));
			LinearLayout.LayoutParams lvLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			lvLp.leftMargin = (int) (6 * d);
			tvLv.setLayoutParams(lvLp);

			metaRow.addView(tvSender);
			metaRow.addView(tvLv);

			if (isMsgAdmin) {
				TextView tvAdm = new TextView(this);
				tvAdm.setText("★ ADMIN");
				tvAdm.setTextColor(Color.WHITE);
				tvAdm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
				tvAdm.setTypeface(null, Typeface.BOLD);
				tvAdm.setMaxLines(1);
				GradientDrawable admBg = new GradientDrawable();
				admBg.setCornerRadius(100f);
				admBg.setColor(Color.parseColor("#EA580C"));
				tvAdm.setBackground(admBg);
				tvAdm.setPadding((int) (6 * d), (int) (1.5f * d), (int) (6 * d), (int) (1.5f * d));
				LinearLayout.LayoutParams admLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				admLp.leftMargin = (int) (5 * d);
				tvAdm.setLayoutParams(admLp);
				metaRow.addView(tvAdm);
			}

			bubble.addView(metaRow);

			if (!text.isEmpty()) {
				TextView tvBody = new TextView(this);
				tvBody.setText(formatMentionsSpannable(text));
				tvBody.setTextColor(Color.parseColor("#F8FAFC"));
				tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
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
			LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			timeLp.gravity = Gravity.END;
			timeLp.topMargin = (int) (3 * d);
			tvTime.setLayoutParams(timeLp);
			bubble.addView(tvTime);

			// Sentuh biasa tidak ada reaksi; harus pendam untuk memunculkan bottomsheet
			bubble.setOnClickListener(null);
			bubble.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					showMessageActionsBottomSheet(msgId, authorName, text, stickerUrl, isMine);
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
		badge.setTypeface(null, Typeface.BOLD);
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
				ss.setSpan(new StyleSpan(Typeface.BOLD), at, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
		fallbackBg.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor("#2AFFFFFF"));
		target.setBackground(fallbackBg);
		target.setScaleType(ImageView.ScaleType.FIT_CENTER);
		target.setPadding(pad, pad, pad, pad);
		target.setImageResource(R.drawable.ic_profile);
		target.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
	}

	private void clearReplyTarget() {
		replyToId = "";
		replyToAuthor = "";
		replyToSnippet = "";
		if (replyBanner != null) replyBanner.setVisibility(View.GONE);
	}

	private void sendMessage() {
		if (currentUser == null || chatInput == null) return;
		String text = chatInput.getText().toString().trim();
		if (text.isEmpty() && selectedStickerUrl.isEmpty()) return;

		String email = currentUser.getEmail() != null ? currentUser.getEmail().trim().toLowerCase(Locale.US) : "";
		String uKey = !email.isEmpty() ? email.replace(".", ",") : currentUser.getUid();
		HashMap<String, Object> prof = userProfileCache.get(uKey);

		String author = (prof != null && prof.get("name") != null && !prof.get("name").toString().trim().isEmpty())
			? prof.get("name").toString().trim()
			: (email.contains("@") ? email.split("@")[0] : "User");

		String username = (prof != null && prof.get("username") != null)
			? prof.get("username").toString()
			: (email.contains("@") ? email.split("@")[0] : "");

		String photo = (prof != null && prof.get("photoURL") != null)
			? prof.get("photoURL").toString()
			: "";

		long exp = prof != null && prof.get("exp") != null ? parseLongVal(prof.get("exp")) : 0L;
		boolean isAdm = ADMIN_EMAIL.equalsIgnoreCase(email);

		HashMap<String, Object> payload = new HashMap<>();
		payload.put("uid", currentUser.getUid());
		payload.put("email", email);
		payload.put("author", author);
		payload.put("username", username);
		payload.put("photoURL", photo);
		payload.put("exp", exp);
		payload.put("text", text);
		payload.put("stickerUrl", selectedStickerUrl);
		payload.put("timestamp", System.currentTimeMillis());
		payload.put("isAdmin", isAdm);

		if (!replyToId.isEmpty()) {
			HashMap<String, Object> repMap = new HashMap<>();
			repMap.put("id", replyToId);
			repMap.put("author", replyToAuthor);
			repMap.put("snippet", replyToSnippet);
			payload.put("replyTo", repMap);
		}

		String key = String.valueOf(System.currentTimeMillis());
		liveChatRef.child(key).setValue(payload).addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				if (task.isSuccessful()) {
					chatInput.setText("");
					selectedStickerUrl = "";
					if (stickerPreviewBox != null) stickerPreviewBox.setVisibility(View.GONE);
					clearReplyTarget();
				} else {
					Toast.makeText(ChatActivity.this, "Gagal mengirim pesan", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	@Override
	public void finish() {
		restoreMiniPlayerAfterChat();
		super.finish();
	}

	@Override
	protected void onDestroy() {
		stopChatShimmer();
		if (liveChatRef != null && chatListener != null) {
			liveChatRef.removeEventListener(chatListener);
		}
		super.onDestroy();
	}
}
