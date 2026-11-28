package com.dongflix;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.style.UnderlineSpan;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class LoginActivity extends AppCompatActivity {

	private static final String DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";

	private static final int ICON_ARROW_RIGHT = 1;
	private static final int ICON_WARNING = 2;
	private static final int ICON_EYE_OPEN = 3;
	private static final int ICON_EYE_CLOSED = 4;
	private static final int ICON_CHECK = 5;
	private static final int ICON_CROSS = 6;

	private FirebaseAuth mAuth;
	private DatabaseReference usersRef;

	private HorizontalScrollView pageScrollView;
	private LinearLayout pagesStrip;
	private View[] pageContainers = new View[3];
	private View[] indicatorBars = new View[3];
	private LinearLayout bottomPagerBar;
	private TextView textNextPage;
	private ImageView iconNextPage;
	private int currentPageIndex = 0;
	private int screenWidthPx = 0;

	// Page 3 (Auth & Register)
	private LinearLayout registerOnlyBlock;
	private TextView textPageTitle, textPageDesc, textActionPrimary, textSwitchMode, textUsernameCheck, textLegalNote;
	private ImageView iconUsernameStatus;
	private EditText inputName, inputUsername, inputAccount, inputPassword;
	private ImageView btnEyeToggle;
	private ProgressBar loadingSpinner;

	private boolean isRegisterMode = false;
	private boolean isBusy = false;
	private boolean isPassVisible = false;
	private boolean isUsernameValidAndFree = false;
	private String verifiedUsername = "";

	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private Runnable usernameDebounceRunnable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAuth = FirebaseAuth.getInstance();
		usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("users");

		if (mAuth.getCurrentUser() != null) {
			goToMainActivity();
			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window w = getWindow();
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.setStatusBarColor(Color.parseColor("#000000"));
			w.setNavigationBarColor(Color.parseColor("#000000"));
		}

		setContentView(R.layout.activity_login);
		screenWidthPx = getResources().getDisplayMetrics().widthPixels;
		if (screenWidthPx <= 0) screenWidthPx = 1080;

		buildLayout();
		applyFonts();
		bindEvents();
		syncIndicatorSelection(0);
		refreshAuthTexts();
	}

	private String resolveVersionLabel() {
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			if (pi != null && pi.versionName != null && !pi.versionName.trim().isEmpty()) {
				return "v" + pi.versionName.trim();
			}
		} catch (Throwable ignored) {}
		return "v1.0";
	}

	private Drawable createMonoIcon(final int iconType, final int tintColor, final float strokeDp) {
		final float d = getResources().getDisplayMetrics().density;
		return new Drawable() {
			private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

			{
				strokePaint.setColor(tintColor);
				strokePaint.setStyle(Paint.Style.STROKE);
				strokePaint.setStrokeWidth(Math.max(1.5f, strokeDp * d));
				strokePaint.setStrokeCap(Paint.Cap.ROUND);
				strokePaint.setStrokeJoin(Paint.Join.ROUND);

				fillPaint.setColor(tintColor);
				fillPaint.setStyle(Paint.Style.FILL);
			}

			@Override
			public void draw(@NonNull Canvas canvas) {
				Rect b = getBounds();
				float w = b.width();
				float h = b.height();
				if (w <= 0 || h <= 0) return;

				Path p = new Path();
				if (iconType == ICON_ARROW_RIGHT) {
					p.moveTo(b.left + w * 0.22f, b.top + h * 0.50f);
					p.lineTo(b.right - w * 0.22f, b.top + h * 0.50f);
					p.moveTo(b.left + w * 0.56f, b.top + h * 0.24f);
					p.lineTo(b.right - w * 0.22f, b.top + h * 0.50f);
					p.lineTo(b.left + w * 0.56f, b.bottom - h * 0.24f);
					canvas.drawPath(p, strokePaint);
				} else if (iconType == ICON_WARNING) {
					p.moveTo(b.left + w * 0.50f, b.top + h * 0.14f);
					p.lineTo(b.right - w * 0.10f, b.bottom - h * 0.16f);
					p.lineTo(b.left + w * 0.10f, b.bottom - h * 0.16f);
					p.close();
					canvas.drawPath(p, strokePaint);
					canvas.drawLine(b.left + w * 0.50f, b.top + h * 0.40f, b.left + w * 0.50f, b.top + h * 0.60f, strokePaint);
					canvas.drawCircle(b.left + w * 0.50f, b.top + h * 0.72f, Math.max(1.5f, w * 0.045f), fillPaint);
				} else if (iconType == ICON_EYE_OPEN) {
					RectF eyeOval = new RectF(b.left + w * 0.10f, b.top + h * 0.24f, b.right - w * 0.10f, b.bottom - h * 0.24f);
					canvas.drawArc(eyeOval, 180f, 180f, false, strokePaint);
					canvas.drawArc(eyeOval, 0f, 180f, false, strokePaint);
					canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), Math.min(w, h) * 0.14f, fillPaint);
				} else if (iconType == ICON_EYE_CLOSED) {
					RectF eyeOval = new RectF(b.left + w * 0.10f, b.top + h * 0.24f, b.right - w * 0.10f, b.bottom - h * 0.24f);
					canvas.drawArc(eyeOval, 180f, 180f, false, strokePaint);
					canvas.drawArc(eyeOval, 0f, 180f, false, strokePaint);
					canvas.drawLine(b.left + w * 0.18f, b.bottom - h * 0.18f, b.right - w * 0.18f, b.top + h * 0.18f, strokePaint);
				} else if (iconType == ICON_CHECK) {
					p.moveTo(b.left + w * 0.20f, b.top + h * 0.52f);
					p.lineTo(b.left + w * 0.42f, b.bottom - h * 0.24f);
					p.lineTo(b.right - w * 0.20f, b.top + h * 0.26f);
					canvas.drawPath(p, strokePaint);
				} else if (iconType == ICON_CROSS) {
					canvas.drawLine(b.left + w * 0.26f, b.top + h * 0.26f, b.right - w * 0.26f, b.bottom - h * 0.26f, strokePaint);
					canvas.drawLine(b.right - w * 0.26f, b.top + h * 0.26f, b.left + w * 0.26f, b.bottom - h * 0.26f, strokePaint);
				}
			}

			@Override public void setAlpha(int alpha) { strokePaint.setAlpha(alpha); fillPaint.setAlpha(alpha); }
			@Override public void setColorFilter(ColorFilter colorFilter) { strokePaint.setColorFilter(colorFilter); fillPaint.setColorFilter(colorFilter); }
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private void buildLayout() {
		float d = getResources().getDisplayMetrics().density;

		FrameLayout root = new FrameLayout(this);
		root.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		root.setBackgroundColor(Color.parseColor("#000000"));

		pageScrollView = new HorizontalScrollView(this) {
			@Override
			protected void onScrollChanged(int l, int t, int oldl, int oldt) {
				super.onScrollChanged(l, t, oldl, oldt);
				if (screenWidthPx > 0) {
					int page = Math.round((float) l / (float) screenWidthPx);
					page = Math.max(0, Math.min(2, page));
					if (page != currentPageIndex) {
						syncIndicatorSelection(page);
					}
				}
			}

			@Override
			public boolean onTouchEvent(MotionEvent ev) {
				if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
					int scrollX = getScrollX();
					int targetPage = Math.round((float) scrollX / (float) screenWidthPx);
					targetPage = Math.max(0, Math.min(2, targetPage));
					scrollToPage(targetPage);
					return true;
				}
				return super.onTouchEvent(ev);
			}
		};
		pageScrollView.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		pageScrollView.setHorizontalScrollBarEnabled(false);
		pageScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

		pagesStrip = new LinearLayout(this);
		pagesStrip.setOrientation(LinearLayout.HORIZONTAL);
		pagesStrip.setLayoutParams(new FrameLayout.LayoutParams(
			screenWidthPx * 3, ViewGroup.LayoutParams.MATCH_PARENT
		));
		pagesStrip.setBackgroundColor(Color.parseColor("#000000"));

		pageContainers[0] = createIntroSlide(d);
		pageContainers[1] = createNoticeSlide(d);
		pageContainers[2] = createAuthSlide(d);

		for (int i = 0; i < 3; i++) {
			pagesStrip.addView(pageContainers[i], new LinearLayout.LayoutParams(
				screenWidthPx, ViewGroup.LayoutParams.MATCH_PARENT
			));
		}

		pageScrollView.addView(pagesStrip);
		root.addView(pageScrollView);

		bottomPagerBar = new LinearLayout(this);
		bottomPagerBar.setOrientation(LinearLayout.VERTICAL);
		bottomPagerBar.setGravity(Gravity.CENTER_HORIZONTAL);
		bottomPagerBar.setPadding((int) (24 * d), (int) (12 * d), (int) (24 * d), (int) (28 * d));
		FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
		);
		bottomPagerBar.setLayoutParams(navLp);

		LinearLayout indicatorRow = new LinearLayout(this);
		indicatorRow.setOrientation(LinearLayout.HORIZONTAL);
		indicatorRow.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams indLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		indLp.bottomMargin = (int) (16 * d);
		indicatorRow.setLayoutParams(indLp);

		for (int i = 0; i < 3; i++) {
			final int idx = i;
			FrameLayout tapZone = new FrameLayout(this);
			tapZone.setPadding((int) (4 * d), (int) (4 * d), (int) (4 * d), (int) (4 * d));
			View bar = new View(this);
			FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams((int) (10 * d), (int) (4 * d));
			bar.setLayoutParams(barLp);
			GradientDrawable barBg = new GradientDrawable();
			barBg.setCornerRadius(100f);
			barBg.setColor(Color.parseColor("#3F3F46"));
			bar.setBackground(barBg);
			indicatorBars[i] = bar;
			tapZone.addView(bar);
			tapZone.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					scrollToPage(idx);
				}
			});
			indicatorRow.addView(tapZone);
		}
		bottomPagerBar.addView(indicatorRow);

		LinearLayout nextBtn = new LinearLayout(this);
		nextBtn.setOrientation(LinearLayout.HORIZONTAL);
		nextBtn.setGravity(Gravity.CENTER);
		nextBtn.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		));
		GradientDrawable nextBg = new GradientDrawable();
		nextBg.setColor(Color.WHITE);
		nextBg.setCornerRadius(100f);
		nextBtn.setBackground(nextBg);

		textNextPage = new TextView(this);
		textNextPage.setText("Lanjut");
		textNextPage.setTextColor(Color.BLACK);
		textNextPage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
		textNextPage.setTypeface(null, Typeface.BOLD);

		iconNextPage = new ImageView(this);
		int arrPx = (int) (18 * d);
		LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(arrPx, arrPx);
		icLp.leftMargin = (int) (8 * d);
		iconNextPage.setLayoutParams(icLp);
		iconNextPage.setImageDrawable(createMonoIcon(ICON_ARROW_RIGHT, Color.BLACK, 2.2f));

		nextBtn.addView(textNextPage);
		nextBtn.addView(iconNextPage);
		nextBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (currentPageIndex < 2) {
					scrollToPage(currentPageIndex + 1);
				}
			}
		});
		bottomPagerBar.addView(nextBtn);

		root.addView(bottomPagerBar);
		setContentView(root);
	}

	private View createIntroSlide(float d) {
		LinearLayout slide = new LinearLayout(this);
		slide.setOrientation(LinearLayout.VERTICAL);
		slide.setBackgroundColor(Color.parseColor("#000000"));
		slide.setPadding((int) (24 * d), (int) (46 * d), (int) (24 * d), (int) (126 * d));

		LinearLayout topMeta = new LinearLayout(this);
		topMeta.setOrientation(LinearLayout.HORIZONTAL);
		topMeta.setGravity(Gravity.CENTER_VERTICAL);
		topMeta.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		TextView tvStep = new TextView(this);
		tvStep.setText("01 / 03");
		tvStep.setTextColor(Color.parseColor("#A1A1AA"));
		tvStep.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tvStep.setTypeface(null, Typeface.BOLD);
		tvStep.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		TextView verPill = new TextView(this);
		verPill.setText(resolveVersionLabel());
		verPill.setTextColor(Color.WHITE);
		verPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
		verPill.setTypeface(null, Typeface.BOLD);
		verPill.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
		GradientDrawable vpBg = new GradientDrawable();
		vpBg.setColor(Color.parseColor("#141418"));
		vpBg.setCornerRadius(100f);
		vpBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#27272A"));
		verPill.setBackground(vpBg);

		topMeta.addView(tvStep);
		topMeta.addView(verPill);
		slide.addView(topMeta);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Selamat datang di\nDongflix");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		tvTitle.setLineSpacing(2 * d, 1.05f);
		LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		tLp.topMargin = (int) (22 * d);
		tvTitle.setLayoutParams(tLp);
		slide.addView(tvTitle);

		TextView tvSub = new TextView(this);
		tvSub.setText("Jelajahi, tonton, dan ikuti serial donghua & anime favoritmu dalam satu tampilan monokrom yang ringkas.");
		tvSub.setTextColor(Color.parseColor("#A1A1AA"));
		tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		tvSub.setLineSpacing(3 * d, 1.08f);
		LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		subLp.topMargin = (int) (10 * d);
		tvSub.setLayoutParams(subLp);
		slide.addView(tvSub);

		View hairLine = new View(this);
		LinearLayout.LayoutParams hlLp = new LinearLayout.LayoutParams((int) (42 * d), Math.max(2, (int) (2 * d)));
		hlLp.topMargin = (int) (16 * d);
		hairLine.setLayoutParams(hlLp);
		hairLine.setBackgroundColor(Color.WHITE);
		slide.addView(hairLine);

		LinearLayout centerSurface = new LinearLayout(this);
		centerSurface.setOrientation(LinearLayout.VERTICAL);
		centerSurface.setGravity(Gravity.CENTER);
		centerSurface.setPadding((int) (20 * d), (int) (24 * d), (int) (20 * d), (int) (24 * d));
		LinearLayout.LayoutParams csLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
		);
		csLp.topMargin = (int) (22 * d);
		csLp.bottomMargin = (int) (12 * d);
		centerSurface.setLayoutParams(csLp);
		GradientDrawable csBg = new GradientDrawable();
		csBg.setColor(Color.parseColor("#0B0D11"));
		csBg.setCornerRadius(24 * d);
		csBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#22FFFFFF"));
		centerSurface.setBackground(csBg);

		ImageView ivLogo = new ImageView(this);
		int logoPx = (int) (164 * d);
		ivLogo.setLayoutParams(new LinearLayout.LayoutParams(logoPx, logoPx));
		ivLogo.setImageResource(R.drawable.img_splash_logo);
		ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
		centerSurface.addView(ivLogo);

		LinearLayout swipeHintRow = new LinearLayout(this);
		swipeHintRow.setOrientation(LinearLayout.HORIZONTAL);
		swipeHintRow.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams shLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		shLp.topMargin = (int) (18 * d);
		swipeHintRow.setLayoutParams(shLp);

		TextView tvSwipeHint = new TextView(this);
		tvSwipeHint.setText("Geser untuk lanjut");
		tvSwipeHint.setTextColor(Color.parseColor("#E4E4E7"));
		tvSwipeHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);

		ImageView sideIcon = new ImageView(this);
		int icPx = (int) (16 * d);
		LinearLayout.LayoutParams sideLp = new LinearLayout.LayoutParams(icPx, icPx);
		sideLp.leftMargin = (int) (6 * d);
		sideIcon.setLayoutParams(sideLp);
		sideIcon.setImageDrawable(createMonoIcon(ICON_ARROW_RIGHT, Color.parseColor("#E4E4E7"), 2f));

		swipeHintRow.addView(tvSwipeHint);
		swipeHintRow.addView(sideIcon);
		swipeHintRow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				scrollToPage(1);
			}
		});
		centerSurface.addView(swipeHintRow);

		slide.addView(centerSurface);
		return slide;
	}

	private View createNoticeSlide(float d) {
		LinearLayout slide = new LinearLayout(this);
		slide.setOrientation(LinearLayout.VERTICAL);
		slide.setBackgroundColor(Color.parseColor("#000000"));
		slide.setPadding((int) (24 * d), (int) (46 * d), (int) (24 * d), (int) (126 * d));

		TextView tvStep = new TextView(this);
		tvStep.setText("02 / 03");
		tvStep.setTextColor(Color.parseColor("#A1A1AA"));
		tvStep.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tvStep.setTypeface(null, Typeface.BOLD);
		slide.addView(tvStep);

		FrameLayout warnCircle = new FrameLayout(this);
		int cSize = (int) (48 * d);
		LinearLayout.LayoutParams wcLp = new LinearLayout.LayoutParams(cSize, cSize);
		wcLp.gravity = Gravity.CENTER_HORIZONTAL;
		wcLp.topMargin = (int) (16 * d);
		wcLp.bottomMargin = (int) (14 * d);
		warnCircle.setLayoutParams(wcLp);
		GradientDrawable wcBg = new GradientDrawable();
		wcBg.setShape(GradientDrawable.OVAL);
		wcBg.setColor(Color.parseColor("#141418"));
		wcBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#27272A"));
		warnCircle.setBackground(wcBg);

		ImageView warnIcon = new ImageView(this);
		int icSize = (int) (22 * d);
		warnIcon.setLayoutParams(new FrameLayout.LayoutParams(icSize, icSize, Gravity.CENTER));
		warnIcon.setImageDrawable(createMonoIcon(ICON_WARNING, Color.WHITE, 2f));
		warnCircle.addView(warnIcon);
		slide.addView(warnCircle);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Sebelum Memulai");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		tvTitle.setGravity(Gravity.CENTER);
		slide.addView(tvTitle);

		LinearLayout card = new LinearLayout(this);
		card.setOrientation(LinearLayout.VERTICAL);
		card.setPadding((int) (18 * d), (int) (18 * d), (int) (18 * d), (int) (18 * d));
		LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		cLp.topMargin = (int) (18 * d);
		card.setLayoutParams(cLp);
		GradientDrawable cBg = new GradientDrawable();
		cBg.setColor(Color.parseColor("#0B0D11"));
		cBg.setCornerRadius(20 * d);
		cBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#22FFFFFF"));
		card.setBackground(cBg);

		String[] points = new String[]{
			"Dongflix menyediakan akses katalog dan pemutar untuk tautan video yang tersedia secara publik di internet. Kami tidak menyimpan maupun mengunggah berkas video di server aplikasi.",
			"Seluruh merek dagang dan materi milik pemilik resminya. Hubungi kontak resmi Dongflix untuk pertanyaan atau permintaan terkait konten.",
			"Dengan melanjutkan ke halaman masuk, kamu setuju menggunakan aplikasi ini secara wajar dan mematuhi ketentuan yang berlaku."
		};

		for (int i = 0; i < points.length; i++) {
			TextView tvP = new TextView(this);
			tvP.setText(points[i]);
			tvP.setTextColor(Color.parseColor("#D4D4D8"));
			tvP.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
			tvP.setLineSpacing(3 * d, 1.08f);
			if (i > 0) {
				LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				pLp.topMargin = (int) (12 * d);
				tvP.setLayoutParams(pLp);
			}
			card.addView(tvP);
		}
		slide.addView(card);

		View spacer = new View(this);
		spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		slide.addView(spacer);

		LinearLayout swipeHintRow = new LinearLayout(this);
		swipeHintRow.setOrientation(LinearLayout.HORIZONTAL);
		swipeHintRow.setGravity(Gravity.CENTER);

		TextView tvSwipePrompt = new TextView(this);
		tvSwipePrompt.setText("Geser ke halaman masuk");
		tvSwipePrompt.setTextColor(Color.parseColor("#E4E4E7"));
		tvSwipePrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);

		ImageView sideIcon = new ImageView(this);
		int icPx = (int) (16 * d);
		LinearLayout.LayoutParams sideLp = new LinearLayout.LayoutParams(icPx, icPx);
		sideLp.leftMargin = (int) (6 * d);
		sideIcon.setLayoutParams(sideLp);
		sideIcon.setImageDrawable(createMonoIcon(ICON_ARROW_RIGHT, Color.parseColor("#E4E4E7"), 2f));

		swipeHintRow.addView(tvSwipePrompt);
		swipeHintRow.addView(sideIcon);
		swipeHintRow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				scrollToPage(2);
			}
		});
		slide.addView(swipeHintRow);

		return slide;
	}

	private View createAuthSlide(float d) {
		ScrollView sv = new ScrollView(this);
		sv.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		sv.setFillViewport(true);
		sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
		sv.setBackgroundColor(Color.parseColor("#000000"));

		LinearLayout col = new LinearLayout(this);
		col.setOrientation(LinearLayout.VERTICAL);
		col.setGravity(Gravity.CENTER_VERTICAL);
		col.setPadding((int) (22 * d), (int) (36 * d), (int) (22 * d), (int) (28 * d));

		ImageView ivLogo = new ImageView(this);
		int logoPx = (int) (46 * d);
		LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(logoPx, logoPx);
		lLp.bottomMargin = (int) (16 * d);
		ivLogo.setLayoutParams(lLp);
		ivLogo.setImageResource(R.drawable.img_splash_logo);
		ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
		col.addView(ivLogo);

		textPageTitle = new TextView(this);
		textPageTitle.setTextColor(Color.WHITE);
		textPageTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f);
		textPageTitle.setTypeface(null, Typeface.BOLD);
		textPageTitle.setLineSpacing(2 * d, 1.05f);
		col.addView(textPageTitle);

		textPageDesc = new TextView(this);
		textPageDesc.setTextColor(Color.parseColor("#A1A1AA"));
		textPageDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		textPageDesc.setLineSpacing(2 * d, 1.08f);
		LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		descLp.topMargin = (int) (8 * d);
		descLp.bottomMargin = (int) (20 * d);
		textPageDesc.setLayoutParams(descLp);
		col.addView(textPageDesc);

		registerOnlyBlock = new LinearLayout(this);
		registerOnlyBlock.setOrientation(LinearLayout.VERTICAL);
		registerOnlyBlock.setVisibility(View.GONE);

		inputName = createMonoField(d, "Nama", false);
		registerOnlyBlock.addView(inputName);

		inputUsername = createMonoField(d, "Username", true);
		LinearLayout.LayoutParams unLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		);
		unLp.topMargin = (int) (10 * d);
		inputUsername.setLayoutParams(unLp);
		registerOnlyBlock.addView(inputUsername);

		LinearLayout statusRow = new LinearLayout(this);
		statusRow.setOrientation(LinearLayout.HORIZONTAL);
		statusRow.setGravity(Gravity.CENTER_VERTICAL);
		statusRow.setVisibility(View.GONE);
		statusRow.setTag("username_status_row");
		LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		srLp.topMargin = (int) (6 * d);
		srLp.leftMargin = (int) (6 * d);
		statusRow.setLayoutParams(srLp);

		iconUsernameStatus = new ImageView(this);
		int stIcPx = (int) (14 * d);
		LinearLayout.LayoutParams siLp = new LinearLayout.LayoutParams(stIcPx, stIcPx);
		siLp.rightMargin = (int) (6 * d);
		iconUsernameStatus.setLayoutParams(siLp);

		textUsernameCheck = new TextView(this);
		textUsernameCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		textUsernameCheck.setTextColor(Color.parseColor("#A1A1AA"));

		statusRow.addView(iconUsernameStatus);
		statusRow.addView(textUsernameCheck);
		registerOnlyBlock.addView(statusRow);

		View regGap = new View(this);
		regGap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (10 * d)));
		registerOnlyBlock.addView(regGap);

		col.addView(registerOnlyBlock);

		inputAccount = createMonoField(d, "Username atau Email", false);
		col.addView(inputAccount);

		FrameLayout passWrap = new FrameLayout(this);
		LinearLayout.LayoutParams pwLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		);
		pwLp.topMargin = (int) (10 * d);
		passWrap.setLayoutParams(pwLp);

		inputPassword = createMonoField(d, "Kata Sandi", false);
		inputPassword.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		inputPassword.setPadding((int) (16 * d), 0, (int) (46 * d), 0);
		inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
		passWrap.addView(inputPassword);

		btnEyeToggle = new ImageView(this);
		int eyePx = (int) (20 * d);
		FrameLayout.LayoutParams eyeLp = new FrameLayout.LayoutParams(eyePx, eyePx, Gravity.END | Gravity.CENTER_VERTICAL);
		eyeLp.rightMargin = (int) (14 * d);
		btnEyeToggle.setLayoutParams(eyeLp);
		btnEyeToggle.setImageDrawable(createMonoIcon(ICON_EYE_OPEN, Color.parseColor("#A1A1AA"), 1.8f));
		btnEyeToggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isPassVisible = !isPassVisible;
				int sel = inputPassword.getSelectionStart();
				inputPassword.setTransformationMethod(
					isPassVisible ? SingleLineTransformationMethod.getInstance() : PasswordTransformationMethod.getInstance()
				);
				btnEyeToggle.setImageDrawable(createMonoIcon(
					isPassVisible ? ICON_EYE_CLOSED : ICON_EYE_OPEN,
					Color.parseColor("#A1A1AA"),
					1.8f
				));
				if (sel >= 0 && sel <= inputPassword.getText().length()) {
					inputPassword.setSelection(sel);
				}
			}
		});
		passWrap.addView(btnEyeToggle);

		col.addView(passWrap);

		FrameLayout primaryBtn = new FrameLayout(this);
		LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		);
		pbLp.topMargin = (int) (20 * d);
		primaryBtn.setLayoutParams(pbLp);
		GradientDrawable pbBg = new GradientDrawable();
		pbBg.setColor(Color.WHITE);
		pbBg.setCornerRadius(100f);
		primaryBtn.setBackground(pbBg);

		textActionPrimary = new TextView(this);
		textActionPrimary.setTextColor(Color.BLACK);
		textActionPrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
		textActionPrimary.setTypeface(null, Typeface.BOLD);
		textActionPrimary.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
		));

		loadingSpinner = new ProgressBar(this);
		int spPx = (int) (22 * d);
		loadingSpinner.setLayoutParams(new FrameLayout.LayoutParams(spPx, spPx, Gravity.CENTER));
		loadingSpinner.setIndeterminate(true);
		loadingSpinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.BLACK));
		loadingSpinner.setVisibility(View.GONE);

		primaryBtn.addView(textActionPrimary);
		primaryBtn.addView(loadingSpinner);
		primaryBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isBusy) {
					submitAuth();
				}
			}
		});
		col.addView(primaryBtn);

		textSwitchMode = new TextView(this);
		textSwitchMode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		textSwitchMode.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams smLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		smLp.topMargin = (int) (22 * d);
		smLp.gravity = Gravity.CENTER_HORIZONTAL;
		textSwitchMode.setLayoutParams(smLp);
		textSwitchMode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isBusy) return;
				isRegisterMode = !isRegisterMode;
				refreshAuthTexts();
			}
		});
		col.addView(textSwitchMode);

		textLegalNote = new TextView(this);
		textLegalNote.setTextColor(Color.parseColor("#71717A"));
		textLegalNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
		textLegalNote.setGravity(Gravity.CENTER);
		textLegalNote.setLineSpacing(2 * d, 1.05f);
		LinearLayout.LayoutParams lnLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		lnLp.topMargin = (int) (18 * d);
		textLegalNote.setLayoutParams(lnLp);

		SpannableString legalSpannable = new SpannableString(
			"Dengan masuk atau mendaftar, kamu menyetujui Ketentuan Layanan, Kebijakan Privasi, dan Catatan Dongflix."
		);
		String fullStr = legalSpannable.toString();
		String[] targets = new String[]{"Ketentuan Layanan", "Kebijakan Privasi", "Catatan Dongflix"};
		for (String t : targets) {
			int idx = fullStr.indexOf(t);
			if (idx >= 0) {
				legalSpannable.setSpan(new UnderlineSpan(), idx, idx + t.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		textLegalNote.setText(legalSpannable);
		col.addView(textLegalNote);

		sv.addView(col);
		return sv;
	}

	private EditText createMonoField(float d, String hint, boolean lowercase) {
		EditText et = new EditText(this);
		et.setHint(hint);
		et.setHintTextColor(Color.parseColor("#71717A"));
		et.setTextColor(Color.WHITE);
		et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		et.setSingleLine(true);
		et.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
		if (lowercase) {
			et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		} else {
			et.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
		}
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		);
		et.setLayoutParams(lp);
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(Color.parseColor("#111318"));
		bg.setCornerRadius(16 * d);
		bg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#27272A"));
		et.setBackground(bg);
		return et;
	}

	private void scrollToPage(int index) {
		int clamped = Math.max(0, Math.min(2, index));
		if (pageScrollView != null) {
			pageScrollView.smoothScrollTo(clamped * screenWidthPx, 0);
		}
		syncIndicatorSelection(clamped);
	}

	private void syncIndicatorSelection(int index) {
		currentPageIndex = Math.max(0, Math.min(2, index));
		float d = getResources().getDisplayMetrics().density;

		for (int i = 0; i < 3; i++) {
			if (indicatorBars[i] != null) {
				boolean active = (i == currentPageIndex);
				GradientDrawable bg = new GradientDrawable();
				bg.setCornerRadius(100f);
				bg.setColor(active ? Color.WHITE : Color.parseColor("#3F3F46"));
				indicatorBars[i].setBackground(bg);
				FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) indicatorBars[i].getLayoutParams();
				lp.width = (int) ((active ? 22 : 8) * d);
				indicatorBars[i].setLayoutParams(lp);
			}
		}

		if (bottomPagerBar != null) {
			bottomPagerBar.setVisibility(currentPageIndex >= 2 ? View.GONE : View.VISIBLE);
		}
		if (textNextPage != null) {
			textNextPage.setText(currentPageIndex == 0 ? "Lanjut" : "Masuk");
		}
	}

	private void refreshAuthTexts() {
		if (isRegisterMode) {
			if (registerOnlyBlock != null) registerOnlyBlock.setVisibility(View.VISIBLE);
			if (inputAccount != null) inputAccount.setHint("Email");
			if (textPageTitle != null) textPageTitle.setText("Buat akun tontonan\nyang cocok buatmu.");
			if (textPageDesc != null) textPageDesc.setText("Simpan daftar tontonan, lanjutkan progres, dan ikut diskusi di Dongflix.");
			if (textActionPrimary != null) textActionPrimary.setText("Daftar");

			SpannableString sw = new SpannableString("Sudah punya akun? Masuk");
			int idx = sw.toString().indexOf("Masuk");
			if (idx >= 0) {
				sw.setSpan(new UnderlineSpan(), idx, idx + 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (textSwitchMode != null) {
				textSwitchMode.setTextColor(Color.parseColor("#A1A1AA"));
				textSwitchMode.setText(sw);
			}
		} else {
			if (registerOnlyBlock != null) registerOnlyBlock.setVisibility(View.GONE);
			if (inputAccount != null) inputAccount.setHint("Username atau Email");
			if (textPageTitle != null) textPageTitle.setText("Masuk ke dunia\ntontonanmu.");
			if (textPageDesc != null) textPageDesc.setText("Lanjutkan donghua dan anime favoritmu dengan pengalaman yang lebih personal.");
			if (textActionPrimary != null) textActionPrimary.setText("Masuk");

			SpannableString sw = new SpannableString("Belum punya akun? Daftar");
			int idx = sw.toString().indexOf("Daftar");
			if (idx >= 0) {
				sw.setSpan(new UnderlineSpan(), idx, idx + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (textSwitchMode != null) {
				textSwitchMode.setTextColor(Color.parseColor("#A1A1AA"));
				textSwitchMode.setText(sw);
			}
		}
	}

	private void bindEvents() {
		if (inputUsername != null) {
			inputUsername.addTextChangedListener(new TextWatcher() {
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					queueUsernameAvailabilityCheck(s.toString());
				}
				@Override public void afterTextChanged(Editable s) {}
			});
		}
	}

	private String normalizeUsername(String raw) {
		if (raw == null) return "";
		return raw.trim().toLowerCase().replaceFirst("^@", "").replaceAll("[^a-z0-9_]", "");
	}

	private void updateUsernameStatusUi(boolean visible, int iconType, String msg, int color) {
		View statusRow = registerOnlyBlock != null ? registerOnlyBlock.findViewWithTag("username_status_row") : null;
		if (statusRow != null) {
			statusRow.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
		if (iconUsernameStatus != null) {
			if (iconType > 0) {
				iconUsernameStatus.setVisibility(View.VISIBLE);
				iconUsernameStatus.setImageDrawable(createMonoIcon(iconType, color, 2f));
			} else {
				iconUsernameStatus.setVisibility(View.GONE);
			}
		}
		if (textUsernameCheck != null) {
			textUsernameCheck.setText(msg);
			textUsernameCheck.setTextColor(color);
		}
	}

	private void queueUsernameAvailabilityCheck(String rawText) {
		if (usernameDebounceRunnable != null) {
			mainHandler.removeCallbacks(usernameDebounceRunnable);
		}

		final String uname = normalizeUsername(rawText);
		isUsernameValidAndFree = false;
		verifiedUsername = "";

		if (textUsernameCheck == null) return;

		if (uname.isEmpty()) {
			updateUsernameStatusUi(false, 0, "", Color.parseColor("#A1A1AA"));
			return;
		}

		if (uname.length() < 3) {
			updateUsernameStatusUi(true, ICON_WARNING, "Username minimal 3 karakter", Color.parseColor("#D4D4D8"));
			return;
		}

		updateUsernameStatusUi(true, 0, "Memeriksa @" + uname + "...", Color.parseColor("#A1A1AA"));

		usernameDebounceRunnable = new Runnable() {
			@Override
			public void run() {
				verifyUsernameAvailability(uname, null);
			}
		};
		mainHandler.postDelayed(usernameDebounceRunnable, 400L);
	}

	private void verifyUsernameAvailability(final String uname, final Runnable afterCheck) {
		usersRef.orderByChild("username").equalTo(uname)
			.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (isFinishing() || isDestroyed()) return;
					boolean exists = snapshot.exists() && snapshot.getChildrenCount() > 0;
					String cur = inputUsername != null ? normalizeUsername(inputUsername.getText().toString()) : "";
					if (uname.equals(cur)) {
						isUsernameValidAndFree = !exists;
						verifiedUsername = !exists ? uname : "";
						if (exists) {
							updateUsernameStatusUi(true, ICON_CROSS, "Username @" + uname + " sudah dipakai", Color.WHITE);
						} else {
							updateUsernameStatusUi(true, ICON_CHECK, "Username @" + uname + " tersedia", Color.WHITE);
						}
					}
					if (afterCheck != null) afterCheck.run();
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					if (afterCheck != null) afterCheck.run();
				}
			});
	}

	private void setBusy(boolean busy) {
		isBusy = busy;
		if (textActionPrimary != null) textActionPrimary.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
		if (loadingSpinner != null) loadingSpinner.setVisibility(busy ? View.VISIBLE : View.GONE);
	}

	private String emailToKey(String email) {
		return email.trim().toLowerCase().replace(".", ",");
	}

	private void submitAuth() {
		final String password = inputPassword != null ? inputPassword.getText().toString() : "";
		if (password.length() < 6) {
			DongflixToast.show(this, "Kata sandi minimal 6 karakter");
			return;
		}

		if (!isRegisterMode) {
			final String accountInput = inputAccount != null ? inputAccount.getText().toString().trim().toLowerCase() : "";
			if (accountInput.isEmpty()) {
				DongflixToast.show(this, "Masukkan username atau email");
				return;
			}

			setBusy(true);
			if (accountInput.contains("@")) {
				performEmailLogin(accountInput, password);
			} else {
				final String targetUname = normalizeUsername(accountInput);
				if (targetUname.isEmpty()) {
					setBusy(false);
					DongflixToast.show(this, "Username tidak valid");
					return;
				}
				usersRef.orderByChild("username").equalTo(targetUname)
					.addListenerForSingleValueEvent(new ValueEventListener() {
						@Override
						public void onDataChange(@NonNull DataSnapshot snapshot) {
							String resolvedEmail = "";
							if (snapshot.exists()) {
								for (DataSnapshot child : snapshot.getChildren()) {
									Object em = child.child("email").getValue();
									if (em != null && !em.toString().trim().isEmpty()) {
										resolvedEmail = em.toString().trim().toLowerCase();
										break;
									}
								}
							}
							if (resolvedEmail.isEmpty()) {
								setBusy(false);
								DongflixToast.show(LoginActivity.this, "Username @" + targetUname + " tidak ditemukan");
								return;
							}
							performEmailLogin(resolvedEmail, password);
						}

						@Override
						public void onCancelled(@NonNull DatabaseError error) {
							setBusy(false);
							DongflixToast.show(LoginActivity.this, "Gagal memeriksa username");
						}
					});
			}
		} else {
			final String nameVal = inputName != null ? inputName.getText().toString().trim() : "";
			final String unameVal = inputUsername != null ? normalizeUsername(inputUsername.getText().toString()) : "";
			final String emailVal = inputAccount != null ? inputAccount.getText().toString().trim().toLowerCase() : "";

			if (nameVal.isEmpty()) {
				DongflixToast.show(this, "Nama tidak boleh kosong");
				return;
			}
			if (unameVal.length() < 3) {
				DongflixToast.show(this, "Username minimal 3 karakter");
				return;
			}
			if (emailVal.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
				DongflixToast.show(this, "Alamat email tidak valid");
				return;
			}

			setBusy(true);
			verifyUsernameAvailability(unameVal, new Runnable() {
				@Override
				public void run() {
					if (isFinishing() || isDestroyed()) return;
					if (!isUsernameValidAndFree || !unameVal.equals(verifiedUsername)) {
						setBusy(false);
						DongflixToast.show(LoginActivity.this, "Username @" + unameVal + " sudah digunakan");
						return;
					}
					performRegister(nameVal, unameVal, emailVal, password);
				}
			});
		}
	}

	private void performEmailLogin(final String email, final String password) {
		mAuth.signInWithEmailAndPassword(email, password)
			.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
				@Override
				public void onComplete(@NonNull Task<AuthResult> task) {
					if (!task.isSuccessful()) {
						setBusy(false);
						String msg = task.getException() != null ? task.getException().getMessage() : "Gagal masuk";
						DongflixToast.show(LoginActivity.this, msg);
						return;
					}

					FirebaseUser user = mAuth.getCurrentUser();
					String activeEmail = (user != null && user.getEmail() != null) ? user.getEmail() : email;
					String key = emailToKey(activeEmail);

					usersRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
						@Override
						public void onDataChange(@NonNull DataSnapshot snapshot) {
							String showName = activeEmail.split("@")[0];
							if (snapshot.exists() && snapshot.child("name").getValue() != null) {
								showName = snapshot.child("name").getValue().toString();
							}
							setBusy(false);
							DongflixToast.show(LoginActivity.this, "Halo, " + showName);
							goToMainActivity();
						}

						@Override
						public void onCancelled(@NonNull DatabaseError error) {
							setBusy(false);
							goToMainActivity();
						}
					});
				}
			});
	}

	private void performRegister(final String nameVal, final String unameVal, final String emailVal, final String password) {
		mAuth.createUserWithEmailAndPassword(emailVal, password)
			.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
				@Override
				public void onComplete(@NonNull Task<AuthResult> task) {
					if (!task.isSuccessful()) {
						setBusy(false);
						String msg = task.getException() != null ? task.getException().getMessage() : "Gagal mendaftar";
						DongflixToast.show(LoginActivity.this, msg);
						return;
					}

					String key = emailToKey(emailVal);
					HashMap<String, Object> data = new HashMap<>();
					data.put("email", emailVal);
					data.put("name", nameVal);
					data.put("username", unameVal);
					data.put("bio", "Dongflix — Streaming Donghua Sub Indo");
					data.put("exp", 100);
					data.put("followersCount", 0);
					data.put("followingCount", 0);
					data.put("photoURL", "");
					data.put("updatedAt", System.currentTimeMillis());

					usersRef.child(key).setValue(data)
						.addOnCompleteListener(new OnCompleteListener<Void>() {
							@Override
							public void onComplete(@NonNull Task<Void> dbTask) {
								setBusy(false);
								DongflixToast.show(LoginActivity.this, "Akun berhasil dibuat, selamat datang " + nameVal);
								goToMainActivity();
							}
						});
				}
			});
	}

	private void goToMainActivity() {
		DongflixToast.dismissCurrent();
		Intent i = new Intent(LoginActivity.this, MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
		finish();
	}

	private void applyFonts() {
		try {
			final Typeface fontReg = Typeface.createFromAsset(getAssets(), "fonts/productsans_reg.ttf");
			final Typeface fontMed = Typeface.createFromAsset(getAssets(), "fonts/productsans_med.ttf");
			final Typeface fontBold = Typeface.createFromAsset(getAssets(), "fonts/productsans_bold.ttf");

			Queue<View> queue = new LinkedList<>();
			queue.add(getWindow().getDecorView().findViewById(android.R.id.content));

			while (!queue.isEmpty()) {
				View v = queue.poll();
				if (v instanceof TextView) {
					TextView tv = (TextView) v;
					Typeface tf = tv.getTypeface();
					int style = (tf != null) ? tf.getStyle() : Typeface.NORMAL;
					if (style == Typeface.BOLD) {
						tv.setTypeface(fontBold, Typeface.NORMAL);
					} else if (style == Typeface.ITALIC) {
						tv.setTypeface(fontReg, Typeface.ITALIC);
					} else {
						tv.setTypeface(fontMed, Typeface.NORMAL);
					}
				} else if (v instanceof ViewGroup) {
					ViewGroup vg = (ViewGroup) v;
					for (int i = 0; i < vg.getChildCount(); i++) {
						queue.add(vg.getChildAt(i));
					}
				}
			}
		} catch (Exception ignored) {}
	}

	@Override
	protected void onDestroy() {
		if (usernameDebounceRunnable != null) {
			mainHandler.removeCallbacks(usernameDebounceRunnable);
		}
		DongflixToast.dismissCurrent();
		super.onDestroy();
	}
}
