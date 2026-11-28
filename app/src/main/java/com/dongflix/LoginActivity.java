package com.dongflix;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
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
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
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

	private FirebaseAuth mAuth;
	private DatabaseReference usersRef;

	private FrameLayout pagerViewport;
	private LinearLayout pagesStrip;
	private View[] pageContainers = new View[3];
	private int currentPageIndex = 0;
	private int screenWidthPx = 0;

	// Page 3 (Auth & Register)
	private LinearLayout registerOnlyBlock;
	private TextView textPageTitle, textPageDesc, textActionPrimary, textSwitchMode, textUsernameCheck, textLegalNote;
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
			w.setStatusBarColor(Color.parseColor("#0A0D16"));
			w.setNavigationBarColor(Color.parseColor("#0A0D16"));
		}

		setContentView(R.layout.activity_login);
		screenWidthPx = getResources().getDisplayMetrics().widthPixels;
		if (screenWidthPx <= 0) screenWidthPx = 1080;

		buildLayout();
		applyFonts();
		bindEvents();
		setCurrentPage(0, false);
		refreshAuthTexts();
	}

	private void buildLayout() {
		float d = getResources().getDisplayMetrics().density;

		FrameLayout root = new FrameLayout(this);
		root.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		root.setBackgroundColor(Color.parseColor("#0A0D16"));

		pagerViewport = new FrameLayout(this);
		pagerViewport.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		pagesStrip = new LinearLayout(this);
		pagesStrip.setOrientation(LinearLayout.HORIZONTAL);
		pagesStrip.setLayoutParams(new FrameLayout.LayoutParams(
			screenWidthPx * 3, ViewGroup.LayoutParams.MATCH_PARENT
		));

		pageContainers[0] = createIntroSlide(d);
		pageContainers[1] = createNoticeSlide(d);
		pageContainers[2] = createAuthSlide(d);

		for (int i = 0; i < 3; i++) {
			pagesStrip.addView(pageContainers[i], new LinearLayout.LayoutParams(
				screenWidthPx, ViewGroup.LayoutParams.MATCH_PARENT
			));
		}

		pagerViewport.addView(pagesStrip);
		root.addView(pagerViewport);
		setContentView(root);

		final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if (e1 == null || e2 == null || isBusy) return false;
				float dx = e2.getX() - e1.getX();
				float dy = e2.getY() - e1.getY();
				if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > (55 * d)) {
					if (dx < 0 && currentPageIndex < 2) {
						setCurrentPage(currentPageIndex + 1, true);
						return true;
					} else if (dx > 0 && currentPageIndex > 0) {
						setCurrentPage(currentPageIndex - 1, true);
						return true;
					}
				}
				return false;
			}
		});

		View.OnTouchListener slideTouch = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				gestureDetector.onTouchEvent(event);
				return false;
			}
		};

		pageContainers[0].setOnTouchListener(slideTouch);
		pageContainers[1].setOnTouchListener(slideTouch);
	}

	private View createIntroSlide(float d) {
		FrameLayout slide = new FrameLayout(this);
		slide.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		GradientDrawable backdrop = new GradientDrawable(
			GradientDrawable.Orientation.BL_TR,
			new int[]{
				Color.parseColor("#2B1A12"),
				Color.parseColor("#111622"),
				Color.parseColor("#1C1710")
			}
		);
		slide.setBackground(backdrop);

		// Top-right version pill
		TextView verPill = new TextView(this);
		verPill.setText("v1.0.3 (APK)");
		verPill.setTextColor(Color.parseColor("#E2E8F0"));
		verPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
		verPill.setTypeface(null, Typeface.BOLD);
		verPill.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
		FrameLayout.LayoutParams vpLp = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END
		);
		vpLp.topMargin = (int) (40 * d);
		vpLp.rightMargin = (int) (18 * d);
		verPill.setLayoutParams(vpLp);
		GradientDrawable vpBg = new GradientDrawable();
		vpBg.setColor(Color.parseColor("#55262A36"));
		vpBg.setCornerRadius(100f);
		verPill.setBackground(vpBg);
		slide.addView(verPill);

		// Main column
		LinearLayout col = new LinearLayout(this);
		col.setOrientation(LinearLayout.VERTICAL);
		col.setPadding((int) (24 * d), (int) (68 * d), (int) (24 * d), (int) (36 * d));
		col.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Selamat datang di\nDongflix");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 27f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		tvTitle.setLineSpacing(2 * d, 1.05f);
		col.addView(tvTitle);

		TextView tvSub = new TextView(this);
		tvSub.setText("Jelajahi, tonton, dan ikuti serial donghua & anime pilihanmu dalam satu tempat. Bergabunglah bersama komunitas penggemar.");
		tvSub.setTextColor(Color.parseColor("#CBD5E1"));
		tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		tvSub.setLineSpacing(3 * d, 1.08f);
		LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		subLp.topMargin = (int) (10 * d);
		tvSub.setLayoutParams(subLp);
		col.addView(tvSub);

		View accentBar = new View(this);
		LinearLayout.LayoutParams abLp = new LinearLayout.LayoutParams((int) (46 * d), (int) (3 * d));
		abLp.topMargin = (int) (14 * d);
		accentBar.setLayoutParams(abLp);
		accentBar.setBackgroundColor(Color.parseColor("#F59E0B"));
		col.addView(accentBar);

		FrameLayout logoCenter = new FrameLayout(this);
		logoCenter.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
		));

		ImageView ivLogo = new ImageView(this);
		int logoPx = (int) (176 * d);
		FrameLayout.LayoutParams lLp = new FrameLayout.LayoutParams(logoPx, logoPx, Gravity.CENTER);
		ivLogo.setLayoutParams(lLp);
		ivLogo.setImageResource(R.drawable.img_splash_logo);
		ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
		logoCenter.addView(ivLogo);
		col.addView(logoCenter);

		TextView tvSwipePrompt = new TextView(this);
		tvSwipePrompt.setText("^  Geser untuk lanjut");
		tvSwipePrompt.setTextColor(Color.parseColor("#E2E8F0"));
		tvSwipePrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		tvSwipePrompt.setGravity(Gravity.CENTER);
		tvSwipePrompt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setCurrentPage(1, true);
			}
		});
		col.addView(tvSwipePrompt);

		slide.addView(col);
		return slide;
	}

	private View createNoticeSlide(float d) {
		FrameLayout slide = new FrameLayout(this);
		slide.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		GradientDrawable backdrop = new GradientDrawable(
			GradientDrawable.Orientation.BL_TR,
			new int[]{
				Color.parseColor("#2B1A12"),
				Color.parseColor("#111622"),
				Color.parseColor("#1C1710")
			}
		);
		slide.setBackground(backdrop);

		LinearLayout col = new LinearLayout(this);
		col.setOrientation(LinearLayout.VERTICAL);
		col.setGravity(Gravity.CENTER_HORIZONTAL);
		col.setPadding((int) (24 * d), (int) (68 * d), (int) (24 * d), (int) (36 * d));
		col.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		FrameLayout warnCircle = new FrameLayout(this);
		int cSize = (int) (46 * d);
		LinearLayout.LayoutParams wcLp = new LinearLayout.LayoutParams(cSize, cSize);
		wcLp.bottomMargin = (int) (18 * d);
		warnCircle.setLayoutParams(wcLp);
		GradientDrawable wcBg = new GradientDrawable();
		wcBg.setShape(GradientDrawable.OVAL);
		wcBg.setColor(Color.parseColor("#382814"));
		warnCircle.setBackground(wcBg);

		ImageView warnIcon = new ImageView(this);
		int icSize = (int) (22 * d);
		warnIcon.setLayoutParams(new FrameLayout.LayoutParams(icSize, icSize, Gravity.CENTER));
		warnIcon.setImageDrawable(new Drawable() {
			private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

			{
				paint.setColor(Color.parseColor("#F59E0B"));
				paint.setStyle(Paint.Style.FILL);
			}

			@Override
			public void draw(@NonNull Canvas canvas) {
				android.graphics.Rect b = getBounds();
				float w = b.width();
				float h = b.height();
				if (w <= 0 || h <= 0) return;
				Path tri = new Path();
				tri.moveTo(b.left + w * 0.50f, b.top + h * 0.14f);
				tri.lineTo(b.right - w * 0.10f, b.bottom - h * 0.16f);
				tri.lineTo(b.left + w * 0.10f, b.bottom - h * 0.16f);
				tri.close();
				canvas.drawPath(tri, paint);

				Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
				inner.setColor(Color.parseColor("#382814"));
				inner.setStrokeWidth(Math.max(2f, w * 0.09f));
				inner.setStrokeCap(Paint.Cap.ROUND);
				canvas.drawLine(b.left + w * 0.50f, b.top + h * 0.38f, b.left + w * 0.50f, b.top + h * 0.62f, inner);
				canvas.drawCircle(b.left + w * 0.50f, b.top + h * 0.74f, Math.max(1.6f, w * 0.055f), inner);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		});
		warnCircle.addView(warnIcon);
		col.addView(warnCircle);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Sebelum Memulai");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		tvTitle.setGravity(Gravity.CENTER);
		col.addView(tvTitle);

		TextView tvP1 = new TextView(this);
		tvP1.setText("Dongflix adalah aplikasi tidak resmi yang membantu pengguna menemukan tautan tontonan yang tersedia di internet. Kami tidak menyimpan maupun mengunggah berkas video di server kami, dan seluruh merek dagang milik pemilik resminya.");
		tvP1.setTextColor(Color.parseColor("#E2E8F0"));
		tvP1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		tvP1.setLineSpacing(3 * d, 1.08f);
		LinearLayout.LayoutParams p1Lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		p1Lp.topMargin = (int) (18 * d);
		tvP1.setLayoutParams(p1Lp);
		col.addView(tvP1);

		TextView tvP2 = new TextView(this);
		tvP2.setText("Dongflix berfungsi sebagai peramban katalog untuk sumber tautan publik. Untuk pertanyaan atau keberatan hak cipta, hubungi kontak resmi Dongflix dan laporan yang sah akan segera ditindaklanjuti.");
		tvP2.setTextColor(Color.parseColor("#E2E8F0"));
		tvP2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		tvP2.setLineSpacing(3 * d, 1.08f);
		LinearLayout.LayoutParams p2Lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		p2Lp.topMargin = (int) (14 * d);
		tvP2.setLayoutParams(p2Lp);
		col.addView(tvP2);

		TextView tvP3 = new TextView(this);
		tvP3.setText("Dengan melanjutkan penggunaan Dongflix, kamu menyetujui catatan ini. Jika tidak setuju, silakan hentikan penggunaan aplikasi.");
		tvP3.setTextColor(Color.parseColor("#E2E8F0"));
		tvP3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		tvP3.setLineSpacing(3 * d, 1.08f);
		LinearLayout.LayoutParams p3Lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		p3Lp.topMargin = (int) (14 * d);
		tvP3.setLayoutParams(p3Lp);
		col.addView(tvP3);

		View spacer = new View(this);
		spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		col.addView(spacer);

		TextView tvSwipePrompt = new TextView(this);
		tvSwipePrompt.setText("^  Geser untuk lanjut");
		tvSwipePrompt.setTextColor(Color.parseColor("#E2E8F0"));
		tvSwipePrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		tvSwipePrompt.setGravity(Gravity.CENTER);
		tvSwipePrompt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setCurrentPage(2, true);
			}
		});
		col.addView(tvSwipePrompt);

		slide.addView(col);
		return slide;
	}

	private View createAuthSlide(float d) {
		ScrollView sv = new ScrollView(this);
		sv.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		sv.setFillViewport(true);
		sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
		sv.setBackgroundColor(Color.parseColor("#0A0D16"));

		LinearLayout col = new LinearLayout(this);
		col.setOrientation(LinearLayout.VERTICAL);
		col.setGravity(Gravity.CENTER_VERTICAL);
		col.setPadding((int) (22 * d), (int) (36 * d), (int) (22 * d), (int) (28 * d));

		ImageView ivLogo = new ImageView(this);
		int logoPx = (int) (44 * d);
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
		textPageDesc.setTextColor(Color.parseColor("#94A3B8"));
		textPageDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		textPageDesc.setLineSpacing(2 * d, 1.08f);
		LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		descLp.topMargin = (int) (8 * d);
		descLp.bottomMargin = (int) (20 * d);
		textPageDesc.setLayoutParams(descLp);
		col.addView(textPageDesc);

		// Register-only fields (1 field Nama + Username)
		registerOnlyBlock = new LinearLayout(this);
		registerOnlyBlock.setOrientation(LinearLayout.VERTICAL);
		registerOnlyBlock.setVisibility(View.GONE);

		inputName = createField(d, "Nama", false);
		registerOnlyBlock.addView(inputName);

		inputUsername = createField(d, "Username", true);
		LinearLayout.LayoutParams unLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		);
		unLp.topMargin = (int) (10 * d);
		inputUsername.setLayoutParams(unLp);
		registerOnlyBlock.addView(inputUsername);

		textUsernameCheck = new TextView(this);
		textUsernameCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		textUsernameCheck.setVisibility(View.GONE);
		LinearLayout.LayoutParams ucLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		ucLp.topMargin = (int) (6 * d);
		ucLp.leftMargin = (int) (6 * d);
		textUsernameCheck.setLayoutParams(ucLp);
		registerOnlyBlock.addView(textUsernameCheck);

		View regGap = new View(this);
		regGap.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (10 * d)));
		registerOnlyBlock.addView(regGap);

		col.addView(registerOnlyBlock);

		// Login: Username atau Email | Register: Email
		inputAccount = createField(d, "Username atau Email", false);
		col.addView(inputAccount);

		// Password + eye icon
		FrameLayout passWrap = new FrameLayout(this);
		LinearLayout.LayoutParams pwLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		);
		pwLp.topMargin = (int) (10 * d);
		passWrap.setLayoutParams(pwLp);

		inputPassword = createField(d, "Kata Sandi", false);
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
		btnEyeToggle.setImageResource(android.R.drawable.ic_menu_view);
		btnEyeToggle.setColorFilter(Color.parseColor("#94A3B8"));
		btnEyeToggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isPassVisible = !isPassVisible;
				int sel = inputPassword.getSelectionStart();
				inputPassword.setTransformationMethod(
					isPassVisible ? SingleLineTransformationMethod.getInstance() : PasswordTransformationMethod.getInstance()
				);
				if (sel >= 0 && sel <= inputPassword.getText().length()) {
					inputPassword.setSelection(sel);
				}
			}
		});
		passWrap.addView(btnEyeToggle);

		col.addView(passWrap);

		// Primary action button (pill biru muda seperti referensi)
		FrameLayout primaryBtn = new FrameLayout(this);
		LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		);
		pbLp.topMargin = (int) (20 * d);
		primaryBtn.setLayoutParams(pbLp);
		GradientDrawable pbBg = new GradientDrawable();
		pbBg.setColor(Color.parseColor("#8AB4F8"));
		pbBg.setCornerRadius(100f);
		primaryBtn.setBackground(pbBg);

		textActionPrimary = new TextView(this);
		textActionPrimary.setTextColor(Color.parseColor("#0B1324"));
		textActionPrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
		textActionPrimary.setTypeface(null, Typeface.BOLD);
		textActionPrimary.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
		));

		loadingSpinner = new ProgressBar(this);
		int spPx = (int) (22 * d);
		loadingSpinner.setLayoutParams(new FrameLayout.LayoutParams(spPx, spPx, Gravity.CENTER));
		loadingSpinner.setIndeterminate(true);
		loadingSpinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0B1324")));
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

		// Switch between Masuk and Daftar
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

		// Footer note
		textLegalNote = new TextView(this);
		textLegalNote.setTextColor(Color.parseColor("#94A3B8"));
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
		int blue = Color.parseColor("#60A5FA");
		String fullStr = legalSpannable.toString();
		String[] targets = new String[]{"Ketentuan Layanan", "Kebijakan Privasi", "Catatan Dongflix"};
		for (String t : targets) {
			int idx = fullStr.indexOf(t);
			if (idx >= 0) {
				legalSpannable.setSpan(new ForegroundColorSpan(blue), idx, idx + t.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				legalSpannable.setSpan(new UnderlineSpan(), idx, idx + t.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		textLegalNote.setText(legalSpannable);
		col.addView(textLegalNote);

		sv.addView(col);
		return sv;
	}

	private EditText createField(float d, String hint, boolean lowercase) {
		EditText et = new EditText(this);
		et.setHint(hint);
		et.setHintTextColor(Color.parseColor("#94A3B8"));
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
		bg.setColor(Color.parseColor("#2A2F42"));
		bg.setCornerRadius(16 * d);
		et.setBackground(bg);
		return et;
	}

	private void setCurrentPage(int index, boolean animate) {
		currentPageIndex = Math.max(0, Math.min(2, index));
		float targetX = -(currentPageIndex * screenWidthPx);
		if (pagesStrip != null) {
			if (animate) {
				pagesStrip.animate().translationX(targetX).setDuration(240L).start();
			} else {
				pagesStrip.setTranslationX(targetX);
			}
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
				sw.setSpan(new ForegroundColorSpan(Color.parseColor("#60A5FA")), idx, idx + 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (textSwitchMode != null) textSwitchMode.setText(sw);
		} else {
			if (registerOnlyBlock != null) registerOnlyBlock.setVisibility(View.GONE);
			if (inputAccount != null) inputAccount.setHint("Username atau Email");
			if (textPageTitle != null) textPageTitle.setText("Masuk ke dunia\ntontonanmu.");
			if (textPageDesc != null) textPageDesc.setText("Lanjutkan donghua dan anime favoritmu dengan pengalaman yang lebih personal.");
			if (textActionPrimary != null) textActionPrimary.setText("Masuk");

			SpannableString sw = new SpannableString("Belum punya akun? Daftar");
			int idx = sw.toString().indexOf("Daftar");
			if (idx >= 0) {
				sw.setSpan(new ForegroundColorSpan(Color.parseColor("#60A5FA")), idx, idx + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (textSwitchMode != null) textSwitchMode.setText(sw);
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

	private void queueUsernameAvailabilityCheck(String rawText) {
		if (usernameDebounceRunnable != null) {
			mainHandler.removeCallbacks(usernameDebounceRunnable);
		}

		final String uname = normalizeUsername(rawText);
		isUsernameValidAndFree = false;
		verifiedUsername = "";

		if (textUsernameCheck == null) return;

		if (uname.isEmpty()) {
			textUsernameCheck.setVisibility(View.GONE);
			return;
		}

		if (uname.length() < 3) {
			textUsernameCheck.setVisibility(View.VISIBLE);
			textUsernameCheck.setText("Username minimal 3 karakter (huruf, angka, _)");
			textUsernameCheck.setTextColor(Color.parseColor("#FBBF24"));
			return;
		}

		textUsernameCheck.setVisibility(View.VISIBLE);
		textUsernameCheck.setText("Mengecek username @" + uname + "...");
		textUsernameCheck.setTextColor(Color.parseColor("#94A3B8"));

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
						if (textUsernameCheck != null) {
							textUsernameCheck.setVisibility(View.VISIBLE);
							if (exists) {
								textUsernameCheck.setText("Username @" + uname + " sudah dipakai");
								textUsernameCheck.setTextColor(Color.parseColor("#F87171"));
							} else {
								textUsernameCheck.setText("Username @" + uname + " tersedia");
								textUsernameCheck.setTextColor(Color.parseColor("#34D399"));
							}
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
			Toast.makeText(this, "Kata sandi minimal 6 karakter", Toast.LENGTH_SHORT).show();
			return;
		}

		if (!isRegisterMode) {
			final String accountInput = inputAccount != null ? inputAccount.getText().toString().trim().toLowerCase() : "";
			if (accountInput.isEmpty()) {
				Toast.makeText(this, "Masukkan username atau email", Toast.LENGTH_SHORT).show();
				return;
			}

			setBusy(true);
			if (accountInput.contains("@")) {
				performEmailLogin(accountInput, password);
			} else {
				final String targetUname = normalizeUsername(accountInput);
				if (targetUname.isEmpty()) {
					setBusy(false);
					Toast.makeText(this, "Username tidak valid", Toast.LENGTH_SHORT).show();
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
								Toast.makeText(LoginActivity.this, "Username @" + targetUname + " tidak ditemukan", Toast.LENGTH_SHORT).show();
								return;
							}
							performEmailLogin(resolvedEmail, password);
						}

						@Override
						public void onCancelled(@NonNull DatabaseError error) {
							setBusy(false);
							Toast.makeText(LoginActivity.this, "Gagal memeriksa username", Toast.LENGTH_SHORT).show();
						}
					});
			}
		} else {
			final String nameVal = inputName != null ? inputName.getText().toString().trim() : "";
			final String unameVal = inputUsername != null ? normalizeUsername(inputUsername.getText().toString()) : "";
			final String emailVal = inputAccount != null ? inputAccount.getText().toString().trim().toLowerCase() : "";

			if (nameVal.isEmpty()) {
				Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
				return;
			}
			if (unameVal.length() < 3) {
				Toast.makeText(this, "Username minimal 3 karakter", Toast.LENGTH_SHORT).show();
				return;
			}
			if (emailVal.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
				Toast.makeText(this, "Alamat email tidak valid", Toast.LENGTH_SHORT).show();
				return;
			}

			setBusy(true);
			verifyUsernameAvailability(unameVal, new Runnable() {
				@Override
				public void run() {
					if (isFinishing() || isDestroyed()) return;
					if (!isUsernameValidAndFree || !unameVal.equals(verifiedUsername)) {
						setBusy(false);
						Toast.makeText(LoginActivity.this, "Username @" + unameVal + " sudah digunakan", Toast.LENGTH_SHORT).show();
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
						Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
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
							Toast.makeText(LoginActivity.this, "Halo, " + showName, Toast.LENGTH_SHORT).show();
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
						Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
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
								Toast.makeText(LoginActivity.this, "Akun berhasil dibuat, selamat datang " + nameVal, Toast.LENGTH_SHORT).show();
								goToMainActivity();
							}
						});
				}
			});
	}

	private void goToMainActivity() {
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
		super.onDestroy();
	}
}
