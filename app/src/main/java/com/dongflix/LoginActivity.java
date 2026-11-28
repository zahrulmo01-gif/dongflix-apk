package com.dongflix;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
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
	private View[] pageDots = new View[3];
	private TextView btnNextPage;
	private int currentPageIndex = 0;
	private int screenWidthPx = 0;

	// Auth / Register views (Page 3)
	private FrameLayout logoBadgeBox, btnSubmitAuth;
	private LinearLayout tabSwitchContainer, authFormCard, groupRegisterFields;
	private TextView textAuthHeading, textAuthSub, tabModeLogin, tabModeRegister, textSubmitAuth, textToggleHint, textUsernameStatus;
	private EditText inputRegName, inputRegUsername, inputAuthIdentifier, inputAuthPassword;
	private ImageView btnTogglePasswordVis;
	private ProgressBar progressAuth;

	private boolean isRegisterMode = false;
	private boolean isProcessing = false;
	private boolean isPasswordVisible = false;
	private boolean isUsernameAvailable = false;
	private boolean isCheckingUsername = false;
	private String lastCheckedUsername = "";

	private final Handler uiHandler = new Handler(Looper.getMainLooper());
	private Runnable usernameCheckRunnable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAuth = FirebaseAuth.getInstance();
		usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("users");

		if (mAuth.getCurrentUser() != null) {
			goToMainActivity();
			return;
		}

		setContentView(R.layout.activity_login);
		screenWidthPx = getResources().getDisplayMetrics().widthPixels;
		if (screenWidthPx <= 0) screenWidthPx = 1080;

		buildThreePageOnboardingAndAuthLayout();
		setupStyle();
		applyCustomFonts();
		setupListeners();
		showPage(0, false);
	}

	private void buildThreePageOnboardingAndAuthLayout() {
		float d = getResources().getDisplayMetrics().density;

		FrameLayout rootContainer = new FrameLayout(this);
		rootContainer.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		rootContainer.setBackgroundColor(Color.parseColor("#000000"));

		pagerViewport = new FrameLayout(this);
		pagerViewport.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		pagesStrip = new LinearLayout(this);
		pagesStrip.setOrientation(LinearLayout.HORIZONTAL);
		pagesStrip.setLayoutParams(new FrameLayout.LayoutParams(
			screenWidthPx * 3, ViewGroup.LayoutParams.MATCH_PARENT
		));

		pageContainers[0] = buildWelcomePage(d);
		pageContainers[1] = buildDisclaimerPage(d);
		pageContainers[2] = buildAuthPage(d);

		for (int i = 0; i < 3; i++) {
			pagesStrip.addView(pageContainers[i], new LinearLayout.LayoutParams(
				screenWidthPx, ViewGroup.LayoutParams.MATCH_PARENT
			));
		}

		pagerViewport.addView(pagesStrip);
		rootContainer.addView(pagerViewport);

		// Bottom indicator + next button (hanya tampil di halaman 0 dan 1)
		LinearLayout bottomNavBar = new LinearLayout(this);
		bottomNavBar.setOrientation(LinearLayout.VERTICAL);
		bottomNavBar.setGravity(Gravity.CENTER_HORIZONTAL);
		bottomNavBar.setPadding((int) (24 * d), (int) (10 * d), (int) (24 * d), (int) (28 * d));
		FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
		);
		bottomNavBar.setLayoutParams(navLp);

		LinearLayout dotsRow = new LinearLayout(this);
		dotsRow.setOrientation(LinearLayout.HORIZONTAL);
		dotsRow.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams dotsLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		dotsLp.bottomMargin = (int) (16 * d);
		dotsRow.setLayoutParams(dotsLp);

		for (int i = 0; i < 3; i++) {
			View dot = new View(this);
			LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams((int) (8 * d), (int) (8 * d));
			if (i < 2) dotLp.rightMargin = (int) (8 * d);
			dot.setLayoutParams(dotLp);
			GradientDrawable dotBg = new GradientDrawable();
			dotBg.setCornerRadius(100f);
			dotBg.setColor(i == 0 ? Color.parseColor("#3B82F6") : Color.parseColor("#334155"));
			dot.setBackground(dotBg);
			pageDots[i] = dot;
			dotsRow.addView(dot);
		}
		bottomNavBar.addView(dotsRow);

		btnNextPage = new TextView(this);
		btnNextPage.setText("Lanjut  →");
		btnNextPage.setTextColor(Color.WHITE);
		btnNextPage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
		btnNextPage.setTypeface(null, Typeface.BOLD);
		btnNextPage.setGravity(Gravity.CENTER);
		btnNextPage.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		));
		GradientDrawable nextBg = new GradientDrawable(
			GradientDrawable.Orientation.LEFT_RIGHT,
			new int[]{Color.parseColor("#2563EB"), Color.parseColor("#3B82F6")}
		);
		nextBg.setCornerRadius(100f);
		btnNextPage.setBackground(nextBg);
		btnNextPage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (currentPageIndex < 2) {
					showPage(currentPageIndex + 1, true);
				}
			}
		});
		bottomNavBar.addView(btnNextPage);

		rootContainer.addView(bottomNavBar);
		setContentView(rootContainer);

		// Gesture swipe horizontal antar 3 halaman
		final GestureDetector swipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if (e1 == null || e2 == null || isProcessing) return false;
				float dx = e2.getX() - e1.getX();
				float dy = e2.getY() - e1.getY();
				if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > (60 * d)) {
					if (dx < 0 && currentPageIndex < 2) {
						showPage(currentPageIndex + 1, true);
						return true;
					} else if (dx > 0 && currentPageIndex > 0) {
						showPage(currentPageIndex - 1, true);
						return true;
					}
				}
				return false;
			}
		});

		View.OnTouchListener swipeTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				swipeDetector.onTouchEvent(event);
				return false;
			}
		};
		pageContainers[0].setOnTouchListener(swipeTouchListener);
		pageContainers[1].setOnTouchListener(swipeTouchListener);
	}

	private View buildWelcomePage(float d) {
		LinearLayout page = new LinearLayout(this);
		page.setOrientation(LinearLayout.VERTICAL);
		page.setPadding((int) (24 * d), (int) (52 * d), (int) (24 * d), (int) (130 * d));

		GradientDrawable bg = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{Color.parseColor("#11192B"), Color.parseColor("#000000"), Color.parseColor("#000000")}
		);
		page.setBackground(bg);

		TextView tvPill = new TextView(this);
		tvPill.setText("✦ HALAMAN 1 DARI 3 • SELAMAT DATANG");
		tvPill.setTextColor(Color.parseColor("#38BDF8"));
		tvPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
		tvPill.setTypeface(null, Typeface.BOLD);
		tvPill.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
		GradientDrawable pillBg = new GradientDrawable();
		pillBg.setColor(Color.parseColor("#1A38BDF8"));
		pillBg.setCornerRadius(100f);
		tvPill.setBackground(pillBg);
		page.addView(tvPill);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Selamat Datang di\nDongflix");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		tLp.topMargin = (int) (16 * d);
		tvTitle.setLayoutParams(tLp);
		page.addView(tvTitle);

		TextView tvDesc = new TextView(this);
		tvDesc.setText("Temukan, tonton, dan pantau serial donghua & anime favoritmu dalam satu ruang dengan riwayat tontonan dan komentar komunitas.");
		tvDesc.setTextColor(Color.parseColor("#94A3B8"));
		tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		tvDesc.setLineSpacing(3 * d, 1.08f);
		LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		dLp.topMargin = (int) (10 * d);
		tvDesc.setLayoutParams(dLp);
		page.addView(tvDesc);

		LinearLayout centerCard = new LinearLayout(this);
		centerCard.setOrientation(LinearLayout.VERTICAL);
		centerCard.setGravity(Gravity.CENTER);
		centerCard.setPadding((int) (20 * d), (int) (28 * d), (int) (20 * d), (int) (28 * d));
		LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
		);
		cLp.topMargin = (int) (24 * d);
		cLp.bottomMargin = (int) (12 * d);
		centerCard.setLayoutParams(cLp);
		GradientDrawable cBg = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{Color.parseColor("#12141A"), Color.parseColor("#0B0D11")}
		);
		cBg.setCornerRadius(24 * d);
		cBg.setStroke((int) (1 * d), Color.parseColor("#22FFFFFF"));
		centerCard.setBackground(cBg);

		FrameLayout badge = new FrameLayout(this);
		badge.setLayoutParams(new LinearLayout.LayoutParams((int) (68 * d), (int) (68 * d)));
		GradientDrawable bBg = new GradientDrawable();
		bBg.setColor(Color.parseColor("#3B82F6"));
		bBg.setCornerRadius(20 * d);
		badge.setBackground(bBg);

		ImageView ivPlay = new ImageView(this);
		ivPlay.setLayoutParams(new FrameLayout.LayoutParams((int) (30 * d), (int) (30 * d), Gravity.CENTER));
		ivPlay.setImageResource(R.drawable.ic_play1);
		ivPlay.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		badge.addView(ivPlay);
		centerCard.addView(badge);

		TextView tvSwipeHint = new TextView(this);
		tvSwipeHint.setText("Geser ke kiri atau tekan Lanjut");
		tvSwipeHint.setTextColor(Color.parseColor("#CBD5E1"));
		tvSwipeHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		LinearLayout.LayoutParams shLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		shLp.topMargin = (int) (16 * d);
		tvSwipeHint.setLayoutParams(shLp);
		centerCard.addView(tvSwipeHint);

		page.addView(centerCard);
		return page;
	}

	private View buildDisclaimerPage(float d) {
		LinearLayout page = new LinearLayout(this);
		page.setOrientation(LinearLayout.VERTICAL);
		page.setPadding((int) (24 * d), (int) (52 * d), (int) (24 * d), (int) (130 * d));

		GradientDrawable bg = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{Color.parseColor("#11192B"), Color.parseColor("#000000"), Color.parseColor("#000000")}
		);
		page.setBackground(bg);

		TextView tvPill = new TextView(this);
		tvPill.setText("✦ HALAMAN 2 DARI 3 • INFORMASI & DISCLAIMER");
		tvPill.setTextColor(Color.parseColor("#38BDF8"));
		tvPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
		tvPill.setTypeface(null, Typeface.BOLD);
		tvPill.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
		GradientDrawable pillBg = new GradientDrawable();
		pillBg.setColor(Color.parseColor("#1A38BDF8"));
		pillBg.setCornerRadius(100f);
		tvPill.setBackground(pillBg);
		page.addView(tvPill);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Sebelum Memulai");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		tLp.topMargin = (int) (14 * d);
		tvTitle.setLayoutParams(tLp);
		page.addView(tvTitle);

		LinearLayout card = new LinearLayout(this);
		card.setOrientation(LinearLayout.VERTICAL);
		card.setPadding((int) (18 * d), (int) (18 * d), (int) (18 * d), (int) (18 * d));
		LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		cLp.topMargin = (int) (18 * d);
		card.setLayoutParams(cLp);
		GradientDrawable cBg = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{Color.parseColor("#12141A"), Color.parseColor("#0B0D11")}
		);
		cBg.setCornerRadius(20 * d);
		cBg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
		card.setBackground(cBg);

		String[] points = new String[]{
			"Dongflix menyediakan akses katalog dan pemutar untuk tautan video yang tersedia secara publik di internet.",
			"Kami tidak menyimpan atau mengunggah file video secara langsung di server aplikasi.",
			"Dengan melanjutkan ke halaman masuk, kamu setuju menggunakan layanan ini secara wajar dan mematuhi aturan komunitas."
		};

		for (int i = 0; i < points.length; i++) {
			TextView tvP = new TextView(this);
			tvP.setText("•  " + points[i]);
			tvP.setTextColor(Color.parseColor("#CBD5E1"));
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

		page.addView(card);
		return page;
	}

	private View buildAuthPage(float d) {
		ScrollView sv = new ScrollView(this);
		sv.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		sv.setFillViewport(true);
		sv.setOverScrollMode(View.OVER_SCROLL_NEVER);

		LinearLayout page = new LinearLayout(this);
		page.setOrientation(LinearLayout.VERTICAL);
		page.setGravity(Gravity.CENTER_VERTICAL);
		page.setPadding((int) (24 * d), (int) (36 * d), (int) (24 * d), (int) (36 * d));
		page.setBackgroundColor(Color.parseColor("#000000"));

		// Brand header
		LinearLayout brandRow = new LinearLayout(this);
		brandRow.setOrientation(LinearLayout.HORIZONTAL);
		brandRow.setGravity(Gravity.CENTER_VERTICAL);

		logoBadgeBox = new FrameLayout(this);
		logoBadgeBox.setLayoutParams(new LinearLayout.LayoutParams((int) (46 * d), (int) (46 * d)));

		ImageView ivLogo = new ImageView(this);
		ivLogo.setLayoutParams(new FrameLayout.LayoutParams((int) (22 * d), (int) (22 * d), Gravity.CENTER));
		ivLogo.setImageResource(R.drawable.ic_play1);
		ivLogo.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		logoBadgeBox.addView(ivLogo);

		TextView tvBrand = new TextView(this);
		tvBrand.setText("Dongflix");
		tvBrand.setTextColor(Color.WHITE);
		tvBrand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
		tvBrand.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		brLp.leftMargin = (int) (12 * d);
		tvBrand.setLayoutParams(brLp);

		brandRow.addView(logoBadgeBox);
		brandRow.addView(tvBrand);
		page.addView(brandRow);

		textAuthHeading = new TextView(this);
		textAuthHeading.setTextColor(Color.WHITE);
		textAuthHeading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f);
		textAuthHeading.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		hLp.topMargin = (int) (24 * d);
		textAuthHeading.setLayoutParams(hLp);
		page.addView(textAuthHeading);

		textAuthSub = new TextView(this);
		textAuthSub.setTextColor(Color.parseColor("#94A3B8"));
		textAuthSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		sLp.topMargin = (int) (6 * d);
		textAuthSub.setLayoutParams(sLp);
		page.addView(textAuthSub);

		// Tab Masuk / Daftar Baru
		tabSwitchContainer = new LinearLayout(this);
		tabSwitchContainer.setOrientation(LinearLayout.HORIZONTAL);
		tabSwitchContainer.setPadding((int) (4 * d), (int) (4 * d), (int) (4 * d), (int) (4 * d));
		LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * d)
		);
		tabLp.topMargin = (int) (22 * d);
		tabSwitchContainer.setLayoutParams(tabLp);

		tabModeLogin = new TextView(this);
		tabModeLogin.setText("Masuk");
		tabModeLogin.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		tabModeLogin.setTypeface(null, Typeface.BOLD);
		tabModeLogin.setGravity(Gravity.CENTER);
		tabModeLogin.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

		tabModeRegister = new TextView(this);
		tabModeRegister.setText("Daftar Baru");
		tabModeRegister.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		tabModeRegister.setTypeface(null, Typeface.BOLD);
		tabModeRegister.setGravity(Gravity.CENTER);
		tabModeRegister.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

		tabSwitchContainer.addView(tabModeLogin);
		tabSwitchContainer.addView(tabModeRegister);
		page.addView(tabSwitchContainer);

		// Form Card
		authFormCard = new LinearLayout(this);
		authFormCard.setOrientation(LinearLayout.VERTICAL);
		authFormCard.setPadding((int) (20 * d), (int) (20 * d), (int) (20 * d), (int) (20 * d));
		LinearLayout.LayoutParams fcLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		fcLp.topMargin = (int) (18 * d);
		authFormCard.setLayoutParams(fcLp);

		// Register fields: Nama (1 field saja) + Username + Email
		groupRegisterFields = new LinearLayout(this);
		groupRegisterFields.setOrientation(LinearLayout.VERTICAL);
		groupRegisterFields.setVisibility(View.GONE);

		TextView lblName = new TextView(this);
		lblName.setText("Nama");
		lblName.setTextColor(Color.parseColor("#CBD5E1"));
		lblName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		lblName.setTypeface(null, Typeface.BOLD);
		groupRegisterFields.addView(lblName);

		inputRegName = new EditText(this);
		inputRegName.setHint("Contoh: RZ");
		inputRegName.setHintTextColor(Color.parseColor("#64748B"));
		inputRegName.setTextColor(Color.WHITE);
		inputRegName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		inputRegName.setSingleLine(true);
		inputRegName.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
		LinearLayout.LayoutParams rnLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * d)
		);
		rnLp.topMargin = (int) (8 * d);
		inputRegName.setLayoutParams(rnLp);
		groupRegisterFields.addView(inputRegName);

		TextView lblUsername = new TextView(this);
		lblUsername.setText("Username");
		lblUsername.setTextColor(Color.parseColor("#CBD5E1"));
		lblUsername.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		lblUsername.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams luLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		luLp.topMargin = (int) (14 * d);
		lblUsername.setLayoutParams(luLp);
		groupRegisterFields.addView(lblUsername);

		inputRegUsername = new EditText(this);
		inputRegUsername.setHint("Contoh: dongflix");
		inputRegUsername.setHintTextColor(Color.parseColor("#64748B"));
		inputRegUsername.setTextColor(Color.WHITE);
		inputRegUsername.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		inputRegUsername.setSingleLine(true);
		inputRegUsername.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
		LinearLayout.LayoutParams ruLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * d)
		);
		ruLp.topMargin = (int) (8 * d);
		inputRegUsername.setLayoutParams(ruLp);
		groupRegisterFields.addView(inputRegUsername);

		textUsernameStatus = new TextView(this);
		textUsernameStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		textUsernameStatus.setTextColor(Color.parseColor("#94A3B8"));
		textUsernameStatus.setVisibility(View.GONE);
		LinearLayout.LayoutParams usLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		usLp.topMargin = (int) (6 * d);
		textUsernameStatus.setLayoutParams(usLp);
		groupRegisterFields.addView(textUsernameStatus);

		TextView lblRegEmail = new TextView(this);
		lblRegEmail.setText("Alamat Email");
		lblRegEmail.setTextColor(Color.parseColor("#CBD5E1"));
		lblRegEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		lblRegEmail.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams reLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		reLp.topMargin = (int) (14 * d);
		lblRegEmail.setLayoutParams(reLp);
		groupRegisterFields.addView(lblRegEmail);

		authFormCard.addView(groupRegisterFields);

		// Login/Register identifier: Username atau Email saat login, Email saat daftar
		TextView lblId = new TextView(this);
		lblId.setText("Username atau Email");
		lblId.setTextColor(Color.parseColor("#CBD5E1"));
		lblId.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		lblId.setTypeface(null, Typeface.BOLD);
		lblId.setTag("lbl_identifier");
		authFormCard.addView(lblId);

		inputAuthIdentifier = new EditText(this);
		inputAuthIdentifier.setHint("username atau nama@gmail.com");
		inputAuthIdentifier.setHintTextColor(Color.parseColor("#64748B"));
		inputAuthIdentifier.setTextColor(Color.WHITE);
		inputAuthIdentifier.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		inputAuthIdentifier.setSingleLine(true);
		inputAuthIdentifier.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
		LinearLayout.LayoutParams idLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * d)
		);
		idLp.topMargin = (int) (8 * d);
		inputAuthIdentifier.setLayoutParams(idLp);
		authFormCard.addView(inputAuthIdentifier);

		TextView lblPass = new TextView(this);
		lblPass.setText("Password");
		lblPass.setTextColor(Color.parseColor("#CBD5E1"));
		lblPass.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		lblPass.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams lpLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		lpLp.topMargin = (int) (14 * d);
		lblPass.setLayoutParams(lpLp);
		authFormCard.addView(lblPass);

		FrameLayout passBox = new FrameLayout(this);
		LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * d)
		);
		pbLp.topMargin = (int) (8 * d);
		passBox.setLayoutParams(pbLp);

		inputAuthPassword = new EditText(this);
		inputAuthPassword.setHint("Minimal 6 karakter");
		inputAuthPassword.setHintTextColor(Color.parseColor("#64748B"));
		inputAuthPassword.setTextColor(Color.WHITE);
		inputAuthPassword.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		inputAuthPassword.setSingleLine(true);
		inputAuthPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
		inputAuthPassword.setPadding((int) (16 * d), 0, (int) (44 * d), 0);
		inputAuthPassword.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		passBox.addView(inputAuthPassword);

		btnTogglePasswordVis = new ImageView(this);
		int eyePx = (int) (20 * d);
		FrameLayout.LayoutParams eyeLp = new FrameLayout.LayoutParams(eyePx, eyePx, Gravity.END | Gravity.CENTER_VERTICAL);
		eyeLp.rightMargin = (int) (14 * d);
		btnTogglePasswordVis.setLayoutParams(eyeLp);
		btnTogglePasswordVis.setImageResource(android.R.drawable.ic_menu_view);
		btnTogglePasswordVis.setColorFilter(Color.parseColor("#94A3B8"), PorterDuff.Mode.SRC_IN);
		btnTogglePasswordVis.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isPasswordVisible = !isPasswordVisible;
				int sel = inputAuthPassword.getSelectionStart();
				inputAuthPassword.setTransformationMethod(
					isPasswordVisible ? SingleLineTransformationMethod.getInstance() : PasswordTransformationMethod.getInstance()
				);
				if (sel >= 0 && sel <= inputAuthPassword.getText().length()) {
					inputAuthPassword.setSelection(sel);
				}
			}
		});
		passBox.addView(btnTogglePasswordVis);

		authFormCard.addView(passBox);

		btnSubmitAuth = new FrameLayout(this);
		LinearLayout.LayoutParams subBtnLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (50 * d)
		);
		subBtnLp.topMargin = (int) (22 * d);
		btnSubmitAuth.setLayoutParams(subBtnLp);

		textSubmitAuth = new TextView(this);
		textSubmitAuth.setTextColor(Color.WHITE);
		textSubmitAuth.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
		textSubmitAuth.setTypeface(null, Typeface.BOLD);
		textSubmitAuth.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
		));

		progressAuth = new ProgressBar(this);
		int pbSize = (int) (24 * d);
		FrameLayout.LayoutParams pSpinnerLp = new FrameLayout.LayoutParams(pbSize, pbSize, Gravity.CENTER);
		progressAuth.setLayoutParams(pSpinnerLp);
		progressAuth.setIndeterminate(true);
		progressAuth.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
		progressAuth.setVisibility(View.GONE);

		btnSubmitAuth.addView(textSubmitAuth);
		btnSubmitAuth.addView(progressAuth);
		authFormCard.addView(btnSubmitAuth);

		page.addView(authFormCard);

		textToggleHint = new TextView(this);
		textToggleHint.setTextColor(Color.parseColor("#3B82F6"));
		textToggleHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
		textToggleHint.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams thLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		thLp.topMargin = (int) (18 * d);
		thLp.gravity = Gravity.CENTER_HORIZONTAL;
		textToggleHint.setLayoutParams(thLp);
		page.addView(textToggleHint);

		sv.addView(page);
		return sv;
	}

	private void showPage(int index, boolean animate) {
		currentPageIndex = Math.max(0, Math.min(2, index));
		float targetX = -(currentPageIndex * screenWidthPx);

		if (pagesStrip != null) {
			if (animate) {
				pagesStrip.animate().translationX(targetX).setDuration(240L).start();
			} else {
				pagesStrip.setTranslationX(targetX);
			}
		}

		float d = getResources().getDisplayMetrics().density;
		for (int i = 0; i < 3; i++) {
			if (pageDots[i] != null) {
				GradientDrawable dotBg = new GradientDrawable();
				dotBg.setCornerRadius(100f);
				dotBg.setColor(i == currentPageIndex ? Color.parseColor("#3B82F6") : Color.parseColor("#334155"));
				pageDots[i].setBackground(dotBg);
				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) pageDots[i].getLayoutParams();
				lp.width = (int) ((i == currentPageIndex ? 20 : 8) * d);
				pageDots[i].setLayoutParams(lp);
			}
		}

		if (btnNextPage != null) {
			if (currentPageIndex >= 2) {
				btnNextPage.setVisibility(View.GONE);
			} else {
				btnNextPage.setVisibility(View.VISIBLE);
				btnNextPage.setText(currentPageIndex == 0 ? "Lanjut  →" : "Ke Halaman Masuk  →");
			}
		}
	}

	private void setupStyle() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(Color.parseColor("#000000"));
			window.setNavigationBarColor(Color.parseColor("#000000"));
		}

		float d = getResources().getDisplayMetrics().density;

		if (logoBadgeBox != null) {
			GradientDrawable logoBg = new GradientDrawable();
			logoBg.setColor(Color.parseColor("#3B82F6"));
			logoBg.setCornerRadius(14 * d);
			logoBadgeBox.setBackground(logoBg);
		}

		if (tabSwitchContainer != null) {
			GradientDrawable switchBg = new GradientDrawable();
			switchBg.setColor(Color.parseColor("#12141A"));
			switchBg.setCornerRadius(100f);
			switchBg.setStroke((int) (1 * d), Color.parseColor("#1F2430"));
			tabSwitchContainer.setBackground(switchBg);
		}

		if (authFormCard != null) {
			GradientDrawable cardBg = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{Color.parseColor("#12141A"), Color.parseColor("#0B0D11")}
			);
			cardBg.setCornerRadius(20 * d);
			cardBg.setStroke((int) (1 * d), Color.parseColor("#24FFFFFF"));
			authFormCard.setBackground(cardBg);
		}

		EditText[] inputs = {inputRegName, inputRegUsername, inputAuthIdentifier, inputAuthPassword};
		for (EditText et : inputs) {
			if (et != null) {
				GradientDrawable inputBg = new GradientDrawable();
				inputBg.setColor(Color.parseColor("#181B23"));
				inputBg.setCornerRadius(12 * d);
				inputBg.setStroke((int) (1 * d), Color.parseColor("#26FFFFFF"));
				et.setBackground(inputBg);
			}
		}

		if (btnSubmitAuth != null) {
			GradientDrawable btnBg = new GradientDrawable();
			btnBg.setColor(Color.parseColor("#3B82F6"));
			btnBg.setCornerRadius(100f);
			btnSubmitAuth.setBackground(btnBg);
		}

		updateModeUI();
	}

	private void updateModeUI() {
		GradientDrawable activeTabBg = new GradientDrawable();
		activeTabBg.setColor(Color.parseColor("#3B82F6"));
		activeTabBg.setCornerRadius(100f);

		View lblId = authFormCard != null ? authFormCard.findViewWithTag("lbl_identifier") : null;

		if (isRegisterMode) {
			if (tabModeRegister != null) {
				tabModeRegister.setBackground(activeTabBg);
				tabModeRegister.setTextColor(Color.WHITE);
			}
			if (tabModeLogin != null) {
				tabModeLogin.setBackground(null);
				tabModeLogin.setTextColor(Color.parseColor("#94A3B8"));
			}
			if (groupRegisterFields != null) groupRegisterFields.setVisibility(View.VISIBLE);
			if (lblId instanceof TextView) ((TextView) lblId).setVisibility(View.GONE);
			if (inputAuthIdentifier != null) inputAuthIdentifier.setHint("nama@gmail.com");
			if (textAuthHeading != null) textAuthHeading.setText("Buat Akun Dongflix");
			if (textAuthSub != null) textAuthSub.setText("Isi nama, username, email, dan password untuk menyimpan riwayat & komentar.");
			if (textSubmitAuth != null) textSubmitAuth.setText("Daftar Sekarang");
			if (textToggleHint != null) textToggleHint.setText("Sudah punya akun? Masuk di sini");
		} else {
			if (tabModeLogin != null) {
				tabModeLogin.setBackground(activeTabBg);
				tabModeLogin.setTextColor(Color.WHITE);
			}
			if (tabModeRegister != null) {
				tabModeRegister.setBackground(null);
				tabModeRegister.setTextColor(Color.parseColor("#94A3B8"));
			}
			if (groupRegisterFields != null) groupRegisterFields.setVisibility(View.GONE);
			if (lblId instanceof TextView) ((TextView) lblId).setVisibility(View.VISIBLE);
			if (inputAuthIdentifier != null) inputAuthIdentifier.setHint("username atau nama@gmail.com");
			if (textAuthHeading != null) textAuthHeading.setText("Selamat Datang Kembali");
			if (textAuthSub != null) textAuthSub.setText("Masuk menggunakan username atau email dan password akun Dongflix kamu.");
			if (textSubmitAuth != null) textSubmitAuth.setText("Masuk Sekarang");
			if (textToggleHint != null) textToggleHint.setText("Belum punya akun? Daftar di sini");
		}
	}

	private void setupListeners() {
		if (tabModeLogin != null) {
			tabModeLogin.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isProcessing) return;
					isRegisterMode = false;
					updateModeUI();
				}
			});
		}

		if (tabModeRegister != null) {
			tabModeRegister.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isProcessing) return;
					isRegisterMode = true;
					updateModeUI();
				}
			});
		}

		if (textToggleHint != null) {
			textToggleHint.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isProcessing) return;
					isRegisterMode = !isRegisterMode;
					updateModeUI();
				}
			});
		}

		if (inputRegUsername != null) {
			inputRegUsername.addTextChangedListener(new TextWatcher() {
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					scheduleUsernameAvailabilityCheck(s.toString());
				}
				@Override public void afterTextChanged(Editable s) {}
			});
		}

		if (btnSubmitAuth != null) {
			btnSubmitAuth.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isProcessing) return;
					handleAuthSubmit();
				}
			});
		}
	}

	private String sanitizeUsername(String raw) {
		if (raw == null) return "";
		return raw.trim().toLowerCase().replaceFirst("^@", "").replaceAll("[^a-z0-9_]", "");
	}

	private void scheduleUsernameAvailabilityCheck(String rawText) {
		if (usernameCheckRunnable != null) {
			uiHandler.removeCallbacks(usernameCheckRunnable);
		}

		final String cleanUname = sanitizeUsername(rawText);
		isUsernameAvailable = false;
		lastCheckedUsername = "";

		if (textUsernameStatus == null) return;

		if (cleanUname.isEmpty()) {
			isCheckingUsername = false;
			textUsernameStatus.setVisibility(View.GONE);
			return;
		}

		if (cleanUname.length() < 3) {
			isCheckingUsername = false;
			textUsernameStatus.setVisibility(View.VISIBLE);
			textUsernameStatus.setText("Username minimal 3 karakter (huruf, angka, _)");
			textUsernameStatus.setTextColor(Color.parseColor("#FBBF24"));
			return;
		}

		isCheckingUsername = true;
		textUsernameStatus.setVisibility(View.VISIBLE);
		textUsernameStatus.setText("Memeriksa ketersediaan @" + cleanUname + "...");
		textUsernameStatus.setTextColor(Color.parseColor("#94A3B8"));

		usernameCheckRunnable = new Runnable() {
			@Override
			public void run() {
				checkUsernameInFirebase(cleanUname, null);
			}
		};
		uiHandler.postDelayed(usernameCheckRunnable, 450L);
	}

	private void checkUsernameInFirebase(final String cleanUname, final Runnable onCheckedCallback) {
		usersRef.orderByChild("username").equalTo(cleanUname)
			.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (isFinishing() || isDestroyed()) return;
					boolean taken = snapshot.exists() && snapshot.getChildrenCount() > 0;

					String currentInputClean = inputRegUsername != null
						? sanitizeUsername(inputRegUsername.getText().toString()) : "";

					if (cleanUname.equals(currentInputClean)) {
						isCheckingUsername = false;
						isUsernameAvailable = !taken;
						lastCheckedUsername = cleanUname;

						if (textUsernameStatus != null) {
							textUsernameStatus.setVisibility(View.VISIBLE);
							if (taken) {
								textUsernameStatus.setText("✕ Username @" + cleanUname + " sudah digunakan");
								textUsernameStatus.setTextColor(Color.parseColor("#F87171"));
							} else {
								textUsernameStatus.setText("✓ Username @" + cleanUname + " tersedia");
								textUsernameStatus.setTextColor(Color.parseColor("#22C55E"));
							}
						}
					}

					if (onCheckedCallback != null) {
						onCheckedCallback.run();
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					isCheckingUsername = false;
					if (onCheckedCallback != null) {
						onCheckedCallback.run();
					}
				}
			});
	}

	private void setLoadingState(boolean loading) {
		isProcessing = loading;
		if (textSubmitAuth != null) textSubmitAuth.setVisibility(loading ? View.GONE : View.VISIBLE);
		if (progressAuth != null) progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
		if (btnSubmitAuth != null) btnSubmitAuth.setEnabled(!loading);
	}

	private String emailToFirebaseKey(String email) {
		return email.trim().toLowerCase().replace(".", ",");
	}

	private void handleAuthSubmit() {
		final String password = inputAuthPassword != null ? inputAuthPassword.getText().toString() : "";

		if (password.length() < 6) {
			Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
			return;
		}

		if (!isRegisterMode) {
			final String rawId = inputAuthIdentifier != null ? inputAuthIdentifier.getText().toString().trim().toLowerCase() : "";
			if (rawId.isEmpty()) {
				Toast.makeText(this, "Masukkan username atau email", Toast.LENGTH_SHORT).show();
				return;
			}

			setLoadingState(true);
			if (rawId.contains("@")) {
				signInWithEmailAndPasswordInternal(rawId, password);
			} else {
				final String targetUname = sanitizeUsername(rawId);
				if (targetUname.isEmpty()) {
					setLoadingState(false);
					Toast.makeText(this, "Username tidak valid", Toast.LENGTH_SHORT).show();
					return;
				}
				usersRef.orderByChild("username").equalTo(targetUname)
					.addListenerForSingleValueEvent(new ValueEventListener() {
						@Override
						public void onDataChange(@NonNull DataSnapshot snapshot) {
							String foundEmail = "";
							if (snapshot.exists()) {
								for (DataSnapshot child : snapshot.getChildren()) {
									Object emObj = child.child("email").getValue();
									if (emObj != null && !emObj.toString().trim().isEmpty()) {
										foundEmail = emObj.toString().trim().toLowerCase();
										break;
									}
								}
							}
							if (foundEmail.isEmpty()) {
								setLoadingState(false);
								Toast.makeText(LoginActivity.this, "Username @" + targetUname + " tidak ditemukan", Toast.LENGTH_SHORT).show();
								return;
							}
							signInWithEmailAndPasswordInternal(foundEmail, password);
						}

						@Override
						public void onCancelled(@NonNull DatabaseError error) {
							setLoadingState(false);
							Toast.makeText(LoginActivity.this, "Gagal mencari username", Toast.LENGTH_SHORT).show();
						}
					});
			}
		} else {
			final String regName = inputRegName != null ? inputRegName.getText().toString().trim() : "";
			final String cleanUsername = inputRegUsername != null ? sanitizeUsername(inputRegUsername.getText().toString()) : "";
			final String email = inputAuthIdentifier != null ? inputAuthIdentifier.getText().toString().trim().toLowerCase() : "";

			if (regName.isEmpty()) {
				Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
				return;
			}
			if (cleanUsername.length() < 3) {
				Toast.makeText(this, "Username minimal 3 karakter", Toast.LENGTH_SHORT).show();
				return;
			}
			if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
				Toast.makeText(this, "Masukkan alamat email yang valid", Toast.LENGTH_SHORT).show();
				return;
			}

			setLoadingState(true);
			checkUsernameInFirebase(cleanUsername, new Runnable() {
				@Override
				public void run() {
					if (isFinishing() || isDestroyed()) return;
					if (!isUsernameAvailable || !cleanUsername.equals(lastCheckedUsername)) {
						setLoadingState(false);
						Toast.makeText(LoginActivity.this, "Username @" + cleanUsername + " sudah dipakai, pilih yang lain", Toast.LENGTH_SHORT).show();
						return;
					}
					createAccountInternal(regName, cleanUsername, email, password);
				}
			});
		}
	}

	private void signInWithEmailAndPasswordInternal(final String email, final String password) {
		mAuth.signInWithEmailAndPassword(email, password)
			.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
				@Override
				public void onComplete(@NonNull Task<AuthResult> task) {
					if (!task.isSuccessful()) {
						setLoadingState(false);
						String err = task.getException() != null ? task.getException().getMessage() : "Login gagal";
						Toast.makeText(LoginActivity.this, err, Toast.LENGTH_SHORT).show();
						return;
					}

					FirebaseUser user = mAuth.getCurrentUser();
					String userEmail = (user != null && user.getEmail() != null) ? user.getEmail() : email;
					String userKey = emailToFirebaseKey(userEmail);

					usersRef.child(userKey).addListenerForSingleValueEvent(new ValueEventListener() {
						@Override
						public void onDataChange(@NonNull DataSnapshot snapshot) {
							String displayName = userEmail.split("@")[0];
							if (snapshot.exists() && snapshot.child("name").getValue() != null) {
								displayName = snapshot.child("name").getValue().toString();
							}
							setLoadingState(false);
							Toast.makeText(LoginActivity.this, "Selamat datang, " + displayName + "!", Toast.LENGTH_SHORT).show();
							goToMainActivity();
						}

						@Override
						public void onCancelled(@NonNull DatabaseError error) {
							setLoadingState(false);
							goToMainActivity();
						}
					});
				}
			});
	}

	private void createAccountInternal(final String regName, final String cleanUsername, final String email, final String password) {
		mAuth.createUserWithEmailAndPassword(email, password)
			.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
				@Override
				public void onComplete(@NonNull Task<AuthResult> task) {
					if (!task.isSuccessful()) {
						setLoadingState(false);
						String err = task.getException() != null ? task.getException().getMessage() : "Pendaftaran gagal";
						Toast.makeText(LoginActivity.this, err, Toast.LENGTH_SHORT).show();
						return;
					}

					String userKey = emailToFirebaseKey(email);

					HashMap<String, Object> profileMap = new HashMap<>();
					profileMap.put("email", email);
					profileMap.put("name", regName);
					profileMap.put("username", cleanUsername);
					profileMap.put("bio", "Dongflix — Streaming Donghua Sub Indo");
					profileMap.put("exp", 100);
					profileMap.put("followersCount", 0);
					profileMap.put("followingCount", 0);
					profileMap.put("photoURL", "");
					profileMap.put("updatedAt", System.currentTimeMillis());

					usersRef.child(userKey).setValue(profileMap)
						.addOnCompleteListener(new OnCompleteListener<Void>() {
							@Override
							public void onComplete(@NonNull Task<Void> dbTask) {
								setLoadingState(false);
								Toast.makeText(LoginActivity.this, "Akun berhasil dibuat, selamat datang " + regName + "!", Toast.LENGTH_SHORT).show();
								goToMainActivity();
							}
						});
				}
			});
	}

	private void goToMainActivity() {
		Intent intent = new Intent(LoginActivity.this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}

	private void applyCustomFonts() {
		try {
			final Typeface fontReg = Typeface.createFromAsset(getAssets(), "fonts/productsans_reg.ttf");
			final Typeface fontMed = Typeface.createFromAsset(getAssets(), "fonts/productsans_med.ttf");
			final Typeface fontBold = Typeface.createFromAsset(getAssets(), "fonts/productsans_bold.ttf");

			Queue<View> viewQueue = new LinkedList<>();
			viewQueue.add(getWindow().getDecorView().findViewById(android.R.id.content));

			while (!viewQueue.isEmpty()) {
				View currentView = viewQueue.poll();
				if (currentView instanceof TextView) {
					TextView tv = (TextView) currentView;
					Typeface tf = tv.getTypeface();
					int style = (tf != null) ? tf.getStyle() : Typeface.NORMAL;

					if (style == Typeface.BOLD) {
						tv.setTypeface(fontBold, Typeface.NORMAL);
					} else if (style == Typeface.ITALIC) {
						tv.setTypeface(fontReg, Typeface.ITALIC);
					} else {
						tv.setTypeface(fontMed, Typeface.NORMAL);
					}
				} else if (currentView instanceof ViewGroup) {
					ViewGroup group = (ViewGroup) currentView;
					for (int k = 0; k < group.getChildCount(); k++) {
						viewQueue.add(group.getChildAt(k));
					}
				}
			}
		} catch (Exception ignored) {}
	}

	@Override
	protected void onDestroy() {
		if (usernameCheckRunnable != null) {
			uiHandler.removeCallbacks(usernameCheckRunnable);
		}
		super.onDestroy();
	}
}
