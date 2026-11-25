package com.dongflix;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";
	private static final String CATALOG_URL = "https://dongflix-api1.zahrulmo01.workers.dev/";
	private static final int REQ_PICK_AVATAR = 305;

	private String currentUserKey = "";
	private String currentUserEmail = "";
	private String currentDisplayName = "R Z";
	private String currentUsername = "rulsz";
	private String currentPhotoUrl = "";
	private String pendingEditPhotoUrl = "";
	private long currentUserExp = 0L;

	private ImageView dialogEditAvatarPreview;
	private TextView dialogEditAvatarInitial;

	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> fullCatalogList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> historyList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> favoritesList = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> downloadsList = new ArrayList<>();
	private final Map<String, Long> viewsTotalMap = new HashMap<>();
	private final Map<String, Long> viewsDailyMap = new HashMap<>();

	private DatabaseReference viewsTotalRef;
	private DatabaseReference viewsDailyTodayRef;
	private DatabaseReference userProfileRef;
	private ValueEventListener viewsTotalListener;
	private ValueEventListener viewsDailyListener;
	private ValueEventListener userProfileListener;

	private RequestNetwork dongflix_req;
	private RequestNetwork.RequestListener _dongflix_req_request_listener;

	private GridView gridview1;
	private RecyclerView recyclerPopular, recyclerPopularDongflix, recyclerRomanceDonghua, recyclerSearchList;
	private RecyclerView recyclerCultivationDonghua, recyclerCompletedDonghua, recyclerComedyDonghua, recyclerMysteryDonghua, recyclerMovieDonghua;
	private TextView textHeroTitle, textHeroDesc, textSeeAll, btnClearSearch, textSearchSectionLabel;
	private TextView textPopularDongflixSeeAll, textRomanceSeeAll;
	private TextView textCultivationSeeAll, textCompletedSeeAll, textComedySeeAll, textMysterySeeAll, textMovieSeeAll;
	private ImageView imgHeroBanner, imgPlayIcon, imgHeroFavorite, imgTopNotif, imgTopChat, imgUserAvatar;
	private LinearLayout btnHeroPlay, indicatorCapsule, searchBarContainer;
	private FrameLayout btnHeroFavorite, btnTopNotif, btnTopChat, topActionPill;
	private View topActionPillBlurBg;
	private View dot0, dot1, dot2;
	private EditText inputSearch;

	private final Handler heroSliderHandler = new Handler(Looper.getMainLooper());
	private Runnable heroSliderRunnable;
	private int heroCurrentIndex = 0;

	private FrameLayout pullRefreshIndicator;
	private ProgressBar pullRefreshSpinner;
	private ImageView imgPullRefreshIcon;
	private boolean isRefreshingCatalog = false;
	private float pullStartY = 0f;
	private boolean isPullingFromTop = false;

	private LinearLayout layoutSectionContinue;
	private TextView textTitleContinue, textContinueSeeAll;
	private RecyclerView recyclerContinue;

	// Jelajahi UI
	private RecyclerView recyclerJelajahiGrid;
	private TextView textJelajahiCountBadge;
	private final ArrayList<TextView> jelajahiChipViews = new ArrayList<>();
	private String activeJelajahiFilter = "all";

	// Pustaka UI
	private TextView tabPustakaRiwayat, tabPustakaDaftarku, tabPustakaUnduhan;
	private TextView textPustakaCountBadge;
	private LinearLayout layoutPustakaEmptyState;
	private TextView textPustakaEmptyTitle, textPustakaEmptyDesc;
	private RecyclerView recyclerPustakaList;
	private int activePustakaTab = 0;

	// Akun UI
	private ImageView imgAccountAvatar;
	private TextView tvAccountInitial, tvAccountName, tvAccountHandle;
	private TextView tvStatWatchTime, tvStatSavedCount, tvStatExpCount;
	private TextView tvAccountLevelTitle, tvAccountExpProgressLabel, tvAccountExpPercent;
	private View viewAccountExpTrack, viewAccountExpFill;

	private FrameLayout navbarExpanded, btnCompactHome, miniPlayerInlineSlot, btnCircleSearch;
	private LinearLayout tabBeranda, tabJelajahi, tabPustaka, tabAkun;
	private ImageView imgHome, imgJelajahi, imgFavorite, imgProfile, imgSearch;
	private TextView txtHome, txtJelajahi, txtFavorite, txtProfile;
	private View liquidDragPill;
	private int activeNavTabIndex = 0;
	private boolean isNavbarCompact = false;

	private boolean isDraggingNavbarPill = false;
	private float navDownRawX = 0f;
	private float navInitialPillTx = 0f;

	private ValueAnimator mainShimmerAnimator;
	private float mainShimmerPhase = 0f;
	private boolean isMainLoading = true;

	private SharedPreferences favPrefs;
	private HashMap<String, Object> currentHeroItem;
	private Typeface outfitFont;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			Intent loginIntent = new Intent(this, LoginActivity.class);
			loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(loginIntent);
			finish();
			return;
		}

		setContentView(R.layout.activity_main);

		DongflixPlayerManager.getInstance().initWithApplication(getApplication());
		favPrefs = getSharedPreferences("dongflix_favorites", Context.MODE_PRIVATE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
			&& checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 201);
		}

		initialize();
		setupContinueWatchingSection();
		setupPullToRefresh();
		setupJelajahiSection();
		setupPustakaSection();
		setupAccountSection();
		initializeLogic();
		loadLoggedInUserProfile(currentUser);
		loadUserHistoryAndFavoritesFromFirebase(currentUser);
		attachFirebasePopularityListeners();
		loadLocalDownloads();
		UpdateCheckerDialog.checkForUpdate(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQ_PICK_AVATAR && resultCode == RESULT_OK && data != null && data.getData() != null) {
			String encoded = encodeImageUriToDataUrl(data.getData());
			if (encoded != null && !encoded.isEmpty()) {
				pendingEditPhotoUrl = encoded;
				bindAvatarPreview(dialogEditAvatarPreview, dialogEditAvatarInitial, pendingEditPhotoUrl, currentDisplayName);
			} else {
				Toast.makeText(this, "Gagal memproses foto", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private String encodeImageUriToDataUrl(Uri uri) {
		try {
			InputStream is = getContentResolver().openInputStream(uri);
			if (is == null) return "";
			Bitmap raw = BitmapFactory.decodeStream(is);
			is.close();
			if (raw == null) return "";

			int edge = Math.min(raw.getWidth(), raw.getHeight());
			int x = Math.max(0, (raw.getWidth() - edge) / 2);
			int y = Math.max(0, (raw.getHeight() - edge) / 2);
			Bitmap square = Bitmap.createBitmap(raw, x, y, edge, edge);

			int maxSide = 256;
			Bitmap scaled = edge > maxSide
				? Bitmap.createScaledBitmap(square, maxSide, maxSide, true)
				: square;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			scaled.compress(Bitmap.CompressFormat.JPEG, 78, baos);
			byte[] bytes = baos.toByteArray();
			if (bytes.length > 900_000) {
				baos.reset();
				scaled.compress(Bitmap.CompressFormat.JPEG, 55, baos);
				bytes = baos.toByteArray();
			}
			return "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
		} catch (Exception e) {
			return "";
		}
	}

	private void bindAvatarPreview(ImageView imgView, TextView initialView, String photoUrl, String displayName) {
		String initial = (displayName != null && !displayName.trim().isEmpty())
			? displayName.trim().substring(0, 1).toUpperCase(Locale.US) : "R";
		if (photoUrl != null && !photoUrl.trim().isEmpty()) {
			if (imgView != null) {
				imgView.setVisibility(View.VISIBLE);
				applyCircularAvatar(imgView, photoUrl.trim());
			}
			if (initialView != null) initialView.setVisibility(View.GONE);
		} else {
			if (imgView != null) imgView.setVisibility(View.GONE);
			if (initialView != null) {
				initialView.setVisibility(View.VISIBLE);
				initialView.setText(initial);
				GradientDrawable iniBg = new GradientDrawable();
				iniBg.setShape(GradientDrawable.OVAL);
				iniBg.setColor(Color.parseColor("#1D4ED8"));
				initialView.setBackground(iniBg);
			}
		}
	}

	private String safeStr(Map<String, Object> map, String key, String fallback) {
		if (map == null || key == null) return fallback;
		Object val = map.get(key);
		return val != null ? val.toString() : fallback;
	}

	private String normalizeCoverUrl(String raw) {
		if (raw == null) return "";
		String url = raw.trim().replace("i.ibb.co.com", "i.ibb.co");
		return url.startsWith("http://") ? url.replace("http://", "https://") : url;
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

	private int resolveNotificationDrawableRes() {
		int notifId = getResources().getIdentifier("ic_notifications", "drawable", getPackageName());
		if (notifId > 0) return notifId;
		int bellId = getResources().getIdentifier("ic_notification", "drawable", getPackageName());
		return bellId > 0 ? bellId : R.drawable.ic_search;
	}

	private int resolveMessageDrawableRes(int notifRes) {
		String[] candidates = {"ic_chat", "ic_message", "ic_forum", "ic_comment", "ic_mail", "ic_send"};
		for (String name : candidates) {
			int id = getResources().getIdentifier(name, "drawable", getPackageName());
			if (id > 0 && id != notifRes) return id;
		}
		return android.R.drawable.sym_action_chat;
	}

	private int resolveActionArrowDrawableRes() {
		String[] candidates = {"ic_chevron_right", "ic_arrow_forward", "ic_arrow_right", "ic_play1"};
		for (String name : candidates) {
			int id = getResources().getIdentifier(name, "drawable", getPackageName());
			if (id > 0) return id;
		}
		return 0;
	}

	private void attachRightIconToActionText(TextView tv) {
		if (tv == null) return;
		float d = getResources().getDisplayMetrics().density;
		tv.setTextColor(Color.parseColor("#94A3B8"));
		tv.setGravity(Gravity.CENTER_VERTICAL);
		tv.setCompoundDrawablePadding((int) (4 * d));

		int arrowRes = resolveActionArrowDrawableRes();
		if (arrowRes > 0) {
			Drawable icon = getResources().getDrawable(arrowRes);
			if (icon != null) {
				icon = icon.mutate();
				icon.setColorFilter(Color.parseColor("#94A3B8"), PorterDuff.Mode.SRC_IN);
				int sz = (int) (13 * d);
				icon.setBounds(0, 0, sz, sz);
				tv.setCompoundDrawables(null, null, icon, null);
				return;
			}
		}

		String cur = tv.getText() != null ? tv.getText().toString().trim() : "";
		if (!cur.endsWith("›")) {
			tv.setText(cur + " ›");
		}
	}

	private void applyHeaderActionIcons() {
		int notifRes = resolveNotificationDrawableRes();
		int chatRes = resolveMessageDrawableRes(notifRes);

		if (imgTopNotif != null) {
			imgTopNotif.setImageResource(notifRes);
			imgTopNotif.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		}
		if (imgTopChat != null) {
			imgTopChat.setImageResource(chatRes);
			imgTopChat.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		}
	}

	private String formatRemainingMinutesText(Map<String, Object> item) {
		long posMs = parseLongSafe(item.get("positionMs"));
		long durMs = parseLongSafe(item.get("durationMs"));
		if (durMs > 0L && posMs >= 0L) {
			long remMs = Math.max(0L, durMs - posMs);
			long remMin = Math.max(1L, (long) Math.ceil(remMs / 60000.0));
			if (remMs == 0L) return "Selesai";
			if (remMin >= 60L) {
				return (remMin / 60L) + "j " + (remMin % 60L) + "m lagi";
			}
			return remMin + " mnt lagi";
		}
		String remRaw = safeStr(item, "remainingText", "").trim();
		if (!remRaw.isEmpty()) return remRaw;
		return "Lanjutkan";
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
			float minTx = 0f;
			float maxTx = Math.max(0f, trackW - liquidDragPill.getWidth() - (10 * d));

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
						if (Math.abs(dx) > (6 * d)) isDraggingNavbarPill = true;
						if (isDraggingNavbarPill) {
							liquidDragPill.setTranslationX(Math.max(minTx, Math.min(maxTx, navInitialPillTx + dx)));
							return true;
						}
					}
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					if (navDownRawX > 0f) {
						liquidDragPill.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
						float slotW = Math.max(1f, (trackW - (10 * d)) / 4f);
						int targetIdx = isDraggingNavbarPill
							? Math.max(0, Math.min(3, Math.round(liquidDragPill.getTranslationX() / slotW)))
							: Math.max(0, Math.min(3, (int) ((rawX - loc[0]) / Math.max(1f, trackW / 4f))));
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

	private void applyCircularAvatar(ImageView target, String photoUrl) {
		if (target == null) return;
		float d = getResources().getDisplayMetrics().density;
		int sizePx = Math.max((int) (46 * d), target.getWidth() > 0 ? target.getWidth() : (int) (46 * d));

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
						int edge = Math.min(rawBmp.getWidth(), rawBmp.getHeight());
						Bitmap square = Bitmap.createBitmap(rawBmp, (rawBmp.getWidth() - edge) / 2, (rawBmp.getHeight() - edge) / 2, edge, edge);
						Bitmap scaled = Bitmap.createScaledBitmap(square, sizePx, sizePx, true);
						Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
						Canvas canvas = new Canvas(out);
						Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
						Rect r = new Rect(0, 0, sizePx, sizePx);
						canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p);
						p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
						canvas.drawBitmap(scaled, r, r, p);
						target.setImageBitmap(out);
						return;
					}
				} catch (Exception ignored) {}
			} else if (!isFinishing() && !isDestroyed()) {
				Glide.with(this).load(Uri.parse(cleanUrl)).circleCrop()
					.placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(target);
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

	private void loadLoggedInUserProfile(FirebaseUser user) {
		if (user == null || user.getEmail() == null) return;
		currentUserEmail = user.getEmail().trim().toLowerCase(Locale.US);
		final String fallbackName = currentUserEmail.contains("@")
			? currentUserEmail.substring(0, currentUserEmail.indexOf("@")) : currentUserEmail;

		currentDisplayName = fallbackName;
		currentUsername = fallbackName.toLowerCase(Locale.US).replaceAll("[^a-z0-9_]", "");
		if (currentUsername.isEmpty()) currentUsername = "rulsz";
		currentPhotoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

		TextView tvUserHeader = findViewById(R.id.text_username);
		if (tvUserHeader != null) tvUserHeader.setText(currentDisplayName);
		applyCircularAvatar(imgUserAvatar, currentPhotoUrl);
		refreshAccountUi();

		currentUserKey = currentUserEmail.replace(".", ",");
		if (userProfileRef != null && userProfileListener != null) {
			userProfileRef.removeEventListener(userProfileListener);
		}
		userProfileRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("users").child(currentUserKey);
		userProfileListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed()) return;
				if (!snap.exists()) {
					refreshAccountUi();
					return;
				}
				Object nameObj = snap.child("name").getValue();
				currentDisplayName = (nameObj != null && !nameObj.toString().trim().isEmpty())
					? nameObj.toString().trim() : fallbackName;

				Object userObj = snap.child("username").getValue();
				if (userObj != null && !userObj.toString().trim().isEmpty()) {
					currentUsername = userObj.toString().trim().replaceFirst("^@", "");
				} else {
					currentUsername = currentDisplayName.toLowerCase(Locale.US).replaceAll("[^a-z0-9_]", "");
					if (currentUsername.isEmpty()) currentUsername = fallbackName;
				}

				Object photoObj = snap.child("photoURL").getValue();
				currentPhotoUrl = photoObj != null ? photoObj.toString().trim() : "";
				currentUserExp = parseLongSafe(snap.child("exp").getValue());

				TextView tvHeader = findViewById(R.id.text_username);
				if (tvHeader != null) tvHeader.setText(currentDisplayName);
				applyCircularAvatar(imgUserAvatar, currentPhotoUrl);
				refreshAccountUi();
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				if (!isFinishing() && !isDestroyed()) refreshAccountUi();
			}
		};
		userProfileRef.addValueEventListener(userProfileListener);
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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) pullRefreshIndicator.setElevation(18f);
		}
		if (pullRefreshSpinner != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			pullRefreshSpinner.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
		}
		if (imgPullRefreshIcon != null) imgPullRefreshIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

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
							if (deltaY > (16 * d) && pullRefreshIndicator != null) {
								pullRefreshIndicator.setVisibility(View.VISIBLE);
								if (imgPullRefreshIcon != null) {
									imgPullRefreshIcon.setVisibility(View.VISIBLE);
									imgPullRefreshIcon.setRotation(Math.min(180f, deltaY * 1.4f));
								}
								if (pullRefreshSpinner != null) pullRefreshSpinner.setVisibility(View.GONE);
							}
						}
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						if (isPullingFromTop && mainScroll.getScrollY() <= 0) {
							if (event.getRawY() - pullStartY >= thresholdPx) triggerCatalogRefresh();
							else if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.GONE);
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
		if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.VISIBLE);
		if (imgPullRefreshIcon != null) imgPullRefreshIcon.setVisibility(View.GONE);
		if (pullRefreshSpinner != null) pullRefreshSpinner.setVisibility(View.VISIBLE);
		if (dongflix_req != null && _dongflix_req_request_listener != null) {
			dongflix_req.startRequestNetwork("GET", CATALOG_URL, "", _dongflix_req_request_listener);
		}
	}

	private void hideRefreshIndicator() {
		isRefreshingCatalog = false;
		if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.GONE);
		if (pullRefreshSpinner != null) pullRefreshSpinner.setVisibility(View.GONE);
		if (imgPullRefreshIcon != null) imgPullRefreshIcon.setVisibility(View.VISIBLE);
	}

	private void startHeroAutoSlider() {
		stopHeroAutoSlider();
		if (listMap == null || listMap.size() < 3) return;

		float d = getResources().getDisplayMetrics().density;
		final int activeW = (int) (16 * d);
		final int inactiveW = (int) (5 * d);
		final int dotH = (int) (5 * d);
		final int dotGap = (int) (5 * d);

		final GradientDrawable activeDot = new GradientDrawable();
		activeDot.setShape(GradientDrawable.RECTANGLE);
		activeDot.setColor(Color.WHITE);
		activeDot.setCornerRadius(100f);

		final GradientDrawable inactiveDot = new GradientDrawable();
		inactiveDot.setShape(GradientDrawable.RECTANGLE);
		inactiveDot.setColor(Color.parseColor("#80FFFFFF"));
		inactiveDot.setCornerRadius(100f);

		heroCurrentIndex = 0;
		heroSliderRunnable = new Runnable() {
			@Override
			public void run() {
				if (isFinishing() || isDestroyed() || listMap == null || listMap.size() < 3) return;
				int pos = heroCurrentIndex % 3;
				HashMap<String, Object> item = listMap.get(pos);
				currentHeroItem = item;

				String heroCover = normalizeCoverUrl(safeStr(item, "cover", ""));
				final String heroSlug = safeStr(item, "slug", "");

				if (textHeroTitle != null) textHeroTitle.setText(cleanText(safeStr(item, "title", "")));
				if (textHeroDesc != null) textHeroDesc.setText(cleanText(safeStr(item, "synopsis", "")));
				updateHeroFavoriteIcon(heroSlug);

				if (imgHeroBanner != null && !heroCover.isEmpty()) {
					Glide.with(MainActivity.this).load(Uri.parse(heroCover)).centerCrop()
						.placeholder(R.drawable.default_image).error(R.drawable.default_image).into(imgHeroBanner);
				}
				if (btnHeroPlay != null) {
					btnHeroPlay.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) { openWatchBySlug(heroSlug); }
					});
				}

				View[] dots = {dot0, dot1, dot2};
				for (int i = 0; i < dots.length; i++) {
					if (dots[i] != null) {
						boolean isActive = (i == pos);
						if (dots[i].getLayoutParams() instanceof LinearLayout.LayoutParams) {
							LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dots[i].getLayoutParams();
							lp.width = isActive ? activeW : inactiveW;
							lp.height = dotH;
							if (i > 0) lp.leftMargin = dotGap;
							dots[i].setLayoutParams(lp);
						}
						dots[i].setBackground(isActive ? activeDot : inactiveDot);
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

	private void bindSectionLikePopularToday(RecyclerView rv, ArrayList<HashMap<String, Object>> items) {
		if (rv == null) return;
		float d = getResources().getDisplayMetrics().density;
		rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		rv.setNestedScrollingEnabled(false);
		rv.setOnFlingListener(null);
		rv.setClipToPadding(false);
		rv.setPadding((int) (16 * d), 0, (int) (16 * d), 0);
		rv.setAdapter(new HorizontalPosterAdapter(items, 114, 162));
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

	private void populateCategorizedSections() {
		if (fullCatalogList == null || fullCatalogList.isEmpty()) return;

		ArrayList<HashMap<String, Object>> cultivation = new ArrayList<>();
		ArrayList<HashMap<String, Object>> completed = new ArrayList<>();
		ArrayList<HashMap<String, Object>> comedy = new ArrayList<>();
		ArrayList<HashMap<String, Object>> mystery = new ArrayList<>();
		ArrayList<HashMap<String, Object>> movie = new ArrayList<>();
		ArrayList<HashMap<String, Object>> romance = new ArrayList<>();

		for (HashMap<String, Object> item : fullCatalogList) {
			String status = safeStr(item, "status", "").toLowerCase(Locale.US);
			String genres = safeStr(item, "genres", "").toLowerCase(Locale.US);
			String meta = (genres + " " + safeStr(item, "title", "") + " "
				+ safeStr(item, "synopsis", "") + " " + safeStr(item, "type", "") + " " + safeStr(item, "episode", ""))
				.toLowerCase(Locale.US);

			if (meta.contains("kultivasi") || meta.contains("cultivation") || meta.contains("abadi") || meta.contains("immortal")) {
				cultivation.add(item);
			}
			if (status.contains("completed") || status.contains("tamat")) completed.add(item);
			if (meta.contains("comedy") || meta.contains("komedi") || meta.contains("slice of life")) comedy.add(item);
			if (meta.contains("mystery") || meta.contains("misteri") || meta.contains("teki")
				|| meta.contains("thriller") || meta.contains("suspense") || meta.contains("detective")) {
				mystery.add(item);
			}
			if (meta.contains("movie") || meta.contains("film")) movie.add(item);
			if (genres.contains("romance") || genres.contains("romantis") || genres.contains("romansa")
				|| meta.contains("romance") || meta.contains("romantis")) {
				romance.add(item);
			}
		}

		ArrayList<HashMap<String, Object>> fallback = new ArrayList<>(fullCatalogList.subList(0, Math.min(8, fullCatalogList.size())));
		if (cultivation.isEmpty()) cultivation.addAll(fallback);
		if (completed.isEmpty()) completed.addAll(fallback);
		if (comedy.isEmpty()) comedy.addAll(fallback);
		if (mystery.isEmpty()) mystery.addAll(fallback);
		if (movie.isEmpty()) movie.addAll(fallback);
		if (romance.isEmpty()) romance.addAll(fallback);

		bindSectionLikePopularToday(recyclerCultivationDonghua, new ArrayList<>(cultivation.subList(0, Math.min(10, cultivation.size()))));
		bindSectionLikePopularToday(recyclerCompletedDonghua, new ArrayList<>(completed.subList(0, Math.min(10, completed.size()))));
		bindSectionLikePopularToday(recyclerComedyDonghua, new ArrayList<>(comedy.subList(0, Math.min(10, comedy.size()))));
		bindSectionLikePopularToday(recyclerMysteryDonghua, new ArrayList<>(mystery.subList(0, Math.min(10, mystery.size()))));
		bindSectionLikePopularToday(recyclerMovieDonghua, new ArrayList<>(movie.subList(0, Math.min(10, movie.size()))));
		bindSectionLikePopularToday(recyclerRomanceDonghua, new ArrayList<>(romance.subList(0, Math.min(10, romance.size()))));
	}

	private void setupContinueWatchingSection() {
		layoutSectionContinue = findViewById(R.id.layout_section_continue);
		textTitleContinue = findViewById(R.id.text_title_continue);
		textContinueSeeAll = findViewById(R.id.text_continue_see_all);
		recyclerContinue = findViewById(R.id.recyclerContinue);

		if (recyclerContinue != null) {
			float d = getResources().getDisplayMetrics().density;
			recyclerContinue.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
			recyclerContinue.setNestedScrollingEnabled(false);
			recyclerContinue.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (112 * d)));
		}
		if (textContinueSeeAll != null) {
			textContinueSeeAll.setText("Lihat riwayat");
			attachRightIconToActionText(textContinueSeeAll);
			textContinueSeeAll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					activePustakaTab = 0;
					selectNavTabByIndex(2, true);
				}
			});
		}
	}

	private void setupJelajahiSection() {
		View pageJelajahi = findViewById(R.id.layout_jelajahi);
		if (!(pageJelajahi instanceof ViewGroup)) return;

		ViewGroup jelajahiGroup = (ViewGroup) pageJelajahi;
		jelajahiGroup.removeAllViews();
		jelajahiGroup.setBackgroundColor(Color.parseColor("#000000"));
		jelajahiGroup.setPadding(0, 0, 0, 0);

		float d = getResources().getDisplayMetrics().density;
		int statusH = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusH = getResources().getDimensionPixelSize(resId);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		root.setPadding((int) (14 * d), statusH + (int) (12 * d), (int) (14 * d), 0);

		LinearLayout headerRow = new LinearLayout(this);
		headerRow.setOrientation(LinearLayout.HORIZONTAL);
		headerRow.setGravity(Gravity.CENTER_VERTICAL);
		headerRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Jelajahi");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		textJelajahiCountBadge = new TextView(this);
		textJelajahiCountBadge.setText("0 judul");
		textJelajahiCountBadge.setTextColor(Color.parseColor("#94A3B8"));
		textJelajahiCountBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		textJelajahiCountBadge.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		textJelajahiCountBadge.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
		GradientDrawable badgeBg = new GradientDrawable();
		badgeBg.setColor(Color.parseColor("#12141A"));
		badgeBg.setCornerRadius(100f);
		badgeBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E222B"));
		textJelajahiCountBadge.setBackground(badgeBg);

		headerRow.addView(tvTitle);
		headerRow.addView(textJelajahiCountBadge);
		root.addView(headerRow);

		HorizontalScrollView chipScroll = new HorizontalScrollView(this);
		chipScroll.setHorizontalScrollBarEnabled(false);
		LinearLayout.LayoutParams csLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		csLp.topMargin = (int) (12 * d);
		csLp.bottomMargin = (int) (10 * d);
		chipScroll.setLayoutParams(csLp);

		LinearLayout chipRow = new LinearLayout(this);
		chipRow.setOrientation(LinearLayout.HORIZONTAL);
		chipScroll.addView(chipRow);

		jelajahiChipViews.clear();
		String[][] filters = {
			{"Semua", "all"},
			{"Ongoing", "ongoing"},
			{"Tamat", "completed"},
			{"Kultivasi", "cultivation"},
			{"Romantis", "romance"},
			{"Komedi", "comedy"},
			{"Misteri", "mystery"},
			{"Movie", "movie"}
		};

		for (String[] f : filters) {
			final String label = f[0];
			final String key = f[1];
			TextView chip = new TextView(this);
			chip.setText(label);
			chip.setTag(key);
			chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
			chip.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			chip.setPadding((int) (14 * d), (int) (7 * d), (int) (14 * d), (int) (7 * d));
			LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			cLp.rightMargin = (int) (8 * d);
			chip.setLayoutParams(cLp);
			chip.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					activeJelajahiFilter = key;
					updateJelajahiChipVisuals();
					renderJelajahiGrid();
				}
			});
			jelajahiChipViews.add(chip);
			chipRow.addView(chip);
		}
		root.addView(chipScroll);

		recyclerJelajahiGrid = new RecyclerView(this);
		recyclerJelajahiGrid.setLayoutManager(new GridLayoutManager(this, 3));
		recyclerJelajahiGrid.setClipToPadding(false);
		recyclerJelajahiGrid.setPadding(0, (int) (4 * d), 0, (int) (145 * d));
		recyclerJelajahiGrid.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		root.addView(recyclerJelajahiGrid);

		jelajahiGroup.addView(root);
		updateJelajahiChipVisuals();
	}

	private void updateJelajahiChipVisuals() {
		float d = getResources().getDisplayMetrics().density;
		for (TextView chip : jelajahiChipViews) {
			if (chip == null) continue;
			boolean selected = activeJelajahiFilter.equals(chip.getTag());
			GradientDrawable bg = new GradientDrawable();
			bg.setCornerRadius(100f);
			if (selected) {
				bg.setColor(Color.parseColor("#2563EB"));
				chip.setTextColor(Color.WHITE);
			} else {
				bg.setColor(Color.parseColor("#12141A"));
				bg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E222B"));
				chip.setTextColor(Color.parseColor("#94A3B8"));
			}
			chip.setBackground(bg);
		}
	}

	private void renderJelajahiGrid() {
		if (recyclerJelajahiGrid == null) return;
		if (fullCatalogList == null || fullCatalogList.isEmpty()) {
			if (textJelajahiCountBadge != null) textJelajahiCountBadge.setText("Memuat...");
			recyclerJelajahiGrid.setAdapter(new JelajahiGridAdapter(buildEmptyDummyList(12)));
			return;
		}

		ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
		for (HashMap<String, Object> item : fullCatalogList) {
			String status = safeStr(item, "status", "").toLowerCase(Locale.US);
			String genres = safeStr(item, "genres", "").toLowerCase(Locale.US);
			String meta = (genres + " " + safeStr(item, "title", "") + " "
				+ safeStr(item, "synopsis", "") + " " + safeStr(item, "type", "") + " " + safeStr(item, "episode", ""))
				.toLowerCase(Locale.US);

			boolean match;
			switch (activeJelajahiFilter) {
				case "ongoing":
					match = status.contains("ongoing") || (!status.contains("completed") && !status.contains("tamat"));
					break;
				case "completed":
					match = status.contains("completed") || status.contains("tamat");
					break;
				case "cultivation":
					match = meta.contains("kultivasi") || meta.contains("cultivation") || meta.contains("abadi") || meta.contains("immortal");
					break;
				case "romance":
					match = genres.contains("romance") || genres.contains("romantis") || genres.contains("romansa") || meta.contains("romance") || meta.contains("romantis");
					break;
				case "comedy":
					match = meta.contains("comedy") || meta.contains("komedi") || meta.contains("slice of life");
					break;
				case "mystery":
					match = meta.contains("mystery") || meta.contains("misteri") || meta.contains("teki") || meta.contains("thriller") || meta.contains("suspense");
					break;
				case "movie":
					match = meta.contains("movie") || meta.contains("film");
					break;
				default:
					match = true;
					break;
			}
			if (match) filtered.add(item);
		}

		if (textJelajahiCountBadge != null) textJelajahiCountBadge.setText(filtered.size() + " judul");
		recyclerJelajahiGrid.setAdapter(new JelajahiGridAdapter(filtered));
	}

	private void setupPustakaSection() {
		View pageFavorite = findViewById(R.id.layout_favorite);
		if (!(pageFavorite instanceof ViewGroup)) return;

		ViewGroup favGroup = (ViewGroup) pageFavorite;
		favGroup.removeAllViews();
		favGroup.setBackgroundColor(Color.parseColor("#000000"));
		favGroup.setPadding(0, 0, 0, 0);
		if (favGroup.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams pLp = (ViewGroup.MarginLayoutParams) favGroup.getLayoutParams();
			pLp.leftMargin = 0;
			pLp.rightMargin = 0;
			favGroup.setLayoutParams(pLp);
		}

		float d = getResources().getDisplayMetrics().density;
		int statusH = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusH = getResources().getDimensionPixelSize(resId);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		root.setPadding((int) (12 * d), statusH + (int) (12 * d), (int) (12 * d), 0);

		LinearLayout headerRow = new LinearLayout(this);
		headerRow.setOrientation(LinearLayout.HORIZONTAL);
		headerRow.setGravity(Gravity.CENTER_VERTICAL);
		headerRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Pustaka Saya");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		textPustakaCountBadge = new TextView(this);
		textPustakaCountBadge.setText("0 item");
		textPustakaCountBadge.setTextColor(Color.parseColor("#94A3B8"));
		textPustakaCountBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		textPustakaCountBadge.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		textPustakaCountBadge.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
		GradientDrawable badgeBg = new GradientDrawable();
		badgeBg.setColor(Color.parseColor("#12141A"));
		badgeBg.setCornerRadius(100f);
		badgeBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E222B"));
		textPustakaCountBadge.setBackground(badgeBg);

		headerRow.addView(tvTitle);
		headerRow.addView(textPustakaCountBadge);
		root.addView(headerRow);

		LinearLayout tabsPillContainer = new LinearLayout(this);
		tabsPillContainer.setOrientation(LinearLayout.HORIZONTAL);
		tabsPillContainer.setPadding((int) (4 * d), (int) (4 * d), (int) (4 * d), (int) (4 * d));
		LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * d));
		tabsLp.topMargin = (int) (14 * d);
		tabsLp.bottomMargin = (int) (6 * d);
		tabsPillContainer.setLayoutParams(tabsLp);

		GradientDrawable containerBg = new GradientDrawable();
		containerBg.setColor(Color.parseColor("#12141A"));
		containerBg.setCornerRadius(100f);
		containerBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E222B"));
		tabsPillContainer.setBackground(containerBg);

		tabPustakaRiwayat = buildPustakaPillTab("Riwayat", 0);
		tabPustakaDaftarku = buildPustakaPillTab("Daftarku", 1);
		tabPustakaUnduhan = buildPustakaPillTab("Unduhan", 2);

		tabsPillContainer.addView(tabPustakaRiwayat);
		tabsPillContainer.addView(tabPustakaDaftarku);
		tabsPillContainer.addView(tabPustakaUnduhan);
		root.addView(tabsPillContainer);

		layoutPustakaEmptyState = new LinearLayout(this);
		layoutPustakaEmptyState.setOrientation(LinearLayout.VERTICAL);
		layoutPustakaEmptyState.setGravity(Gravity.CENTER);
		layoutPustakaEmptyState.setPadding((int) (24 * d), (int) (48 * d), (int) (24 * d), (int) (120 * d));
		layoutPustakaEmptyState.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		layoutPustakaEmptyState.setVisibility(View.GONE);

		textPustakaEmptyTitle = new TextView(this);
		textPustakaEmptyTitle.setTextColor(Color.WHITE);
		textPustakaEmptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		textPustakaEmptyTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		textPustakaEmptyTitle.setGravity(Gravity.CENTER);

		textPustakaEmptyDesc = new TextView(this);
		textPustakaEmptyDesc.setTextColor(Color.parseColor("#64748B"));
		textPustakaEmptyDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		textPustakaEmptyDesc.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		textPustakaEmptyDesc.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams eDescLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		eDescLp.topMargin = (int) (6 * d);
		textPustakaEmptyDesc.setLayoutParams(eDescLp);

		layoutPustakaEmptyState.addView(textPustakaEmptyTitle);
		layoutPustakaEmptyState.addView(textPustakaEmptyDesc);
		root.addView(layoutPustakaEmptyState);

		recyclerPustakaList = new RecyclerView(this);
		recyclerPustakaList.setLayoutManager(new LinearLayoutManager(this));
		recyclerPustakaList.setClipToPadding(false);
		recyclerPustakaList.setPadding(0, 0, 0, (int) (130 * d));
		recyclerPustakaList.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		root.addView(recyclerPustakaList);

		favGroup.addView(root);
		updatePustakaTabVisual(activePustakaTab);
	}

	private TextView buildPustakaPillTab(String label, final int tabIndex) {
		TextView tv = new TextView(this);
		tv.setText(label);
		tv.setGravity(Gravity.CENTER);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		tv.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
		tv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activePustakaTab = tabIndex;
				updatePustakaTabVisual(tabIndex);
				renderActivePustakaTab();
			}
		});
		return tv;
	}

	private void updatePustakaTabVisual(int idx) {
		TextView[] tabs = {tabPustakaRiwayat, tabPustakaDaftarku, tabPustakaUnduhan};
		for (int i = 0; i < tabs.length; i++) {
			TextView tv = tabs[i];
			if (tv == null) continue;
			boolean selected = (i == idx);
			if (selected) {
				GradientDrawable activeBg = new GradientDrawable();
				activeBg.setColor(Color.parseColor("#2563EB"));
				activeBg.setCornerRadius(100f);
				tv.setBackground(activeBg);
				tv.setTextColor(Color.WHITE);
			} else {
				tv.setBackgroundColor(Color.TRANSPARENT);
				tv.setTextColor(Color.parseColor("#94A3B8"));
			}
		}
	}

	private void setupAccountSection() {
		View pageProfile = findViewById(R.id.layout_profile);
		if (!(pageProfile instanceof ViewGroup)) return;

		ViewGroup profGroup = (ViewGroup) pageProfile;
		profGroup.removeAllViews();
		profGroup.setBackgroundColor(Color.parseColor("#000000"));
		profGroup.setPadding(0, 0, 0, 0);
		if (profGroup.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams pLp = (ViewGroup.MarginLayoutParams) profGroup.getLayoutParams();
			pLp.leftMargin = 0;
			pLp.rightMargin = 0;
			profGroup.setLayoutParams(pLp);
		}

		float d = getResources().getDisplayMetrics().density;
		int statusH = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusH = getResources().getDimensionPixelSize(resId);

		ScrollView scroll = new ScrollView(this);
		scroll.setFillViewport(true);
		scroll.setVerticalScrollBarEnabled(false);
		scroll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int) (12 * d), statusH + (int) (12 * d), (int) (12 * d), (int) (130 * d));
		root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		LinearLayout topHeaderBar = new LinearLayout(this);
		topHeaderBar.setOrientation(LinearLayout.HORIZONTAL);
		topHeaderBar.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams thLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		thLp.bottomMargin = (int) (12 * d);
		topHeaderBar.setLayoutParams(thLp);

		TextView tvPageTitle = new TextView(this);
		tvPageTitle.setText("Akun");
		tvPageTitle.setTextColor(Color.WHITE);
		tvPageTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		tvPageTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvPageTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		ImageView btnAccountSetting = new ImageView(this);
		LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams((int) (38 * d), (int) (38 * d));
		btnAccountSetting.setLayoutParams(setLp);
		int setPad = (int) (9 * d);
		btnAccountSetting.setPadding(setPad, setPad, setPad, setPad);
		int setRes = getResources().getIdentifier("ic_settings", "drawable", getPackageName());
		btnAccountSetting.setImageResource(setRes > 0 ? setRes : R.drawable.ic_profile);
		btnAccountSetting.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		GradientDrawable setBg = new GradientDrawable();
		setBg.setShape(GradientDrawable.OVAL);
		setBg.setColor(Color.parseColor("#12141A"));
		setBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E2433"));
		btnAccountSetting.setBackground(setBg);
		btnAccountSetting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openAccountSettingsDialog();
			}
		});

		topHeaderBar.addView(tvPageTitle);
		topHeaderBar.addView(btnAccountSetting);
		root.addView(topHeaderBar);

		LinearLayout profileCard = new LinearLayout(this);
		profileCard.setOrientation(LinearLayout.VERTICAL);
		profileCard.setPadding((int) (16 * d), (int) (16 * d), (int) (16 * d), (int) (16 * d));
		GradientDrawable profCardBg = new GradientDrawable();
		profCardBg.setColor(Color.parseColor("#12141A"));
		profCardBg.setCornerRadius(18 * d);
		profCardBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E2433"));
		profileCard.setBackground(profCardBg);

		LinearLayout topRow = new LinearLayout(this);
		topRow.setOrientation(LinearLayout.HORIZONTAL);
		topRow.setGravity(Gravity.CENTER_VERTICAL);

		FrameLayout avatarHolder = new FrameLayout(this);
		LinearLayout.LayoutParams avBoxLp = new LinearLayout.LayoutParams((int) (58 * d), (int) (58 * d));
		avBoxLp.rightMargin = (int) (12 * d);
		avatarHolder.setLayoutParams(avBoxLp);

		imgAccountAvatar = new ImageView(this);
		imgAccountAvatar.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		imgAccountAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);

		tvAccountInitial = new TextView(this);
		tvAccountInitial.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		tvAccountInitial.setGravity(Gravity.CENTER);
		tvAccountInitial.setTextColor(Color.WHITE);
		tvAccountInitial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
		tvAccountInitial.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		avatarHolder.addView(imgAccountAvatar);
		avatarHolder.addView(tvAccountInitial);

		LinearLayout identityCol = new LinearLayout(this);
		identityCol.setOrientation(LinearLayout.VERTICAL);
		identityCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		tvAccountName = new TextView(this);
		tvAccountName.setText("R Z");
		tvAccountName.setTextColor(Color.WHITE);
		tvAccountName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.5f);
		tvAccountName.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvAccountName.setMaxLines(1);
		tvAccountName.setEllipsize(TextUtils.TruncateAt.END);

		tvAccountHandle = new TextView(this);
		tvAccountHandle.setText("@rulsz");
		tvAccountHandle.setTextColor(Color.parseColor("#94A3B8"));
		tvAccountHandle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		tvAccountHandle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		hLp.topMargin = (int) (2 * d);
		tvAccountHandle.setLayoutParams(hLp);

		identityCol.addView(tvAccountName);
		identityCol.addView(tvAccountHandle);

		TextView btnEditProfile = new TextView(this);
		btnEditProfile.setText("Ubah Profil");
		btnEditProfile.setTextColor(Color.WHITE);
		btnEditProfile.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		btnEditProfile.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnEditProfile.setPadding((int) (14 * d), (int) (8 * d), (int) (14 * d), (int) (8 * d));
		GradientDrawable editBg = new GradientDrawable();
		editBg.setColor(Color.parseColor("#1A2030"));
		editBg.setCornerRadius(100f);
		editBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#2AFFFFFF"));
		btnEditProfile.setBackground(editBg);
		btnEditProfile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openEditProfileDialog();
			}
		});

		topRow.addView(avatarHolder);
		topRow.addView(identityCol);
		topRow.addView(btnEditProfile);
		profileCard.addView(topRow);

		View innerDiv = new View(this);
		LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) (1 * d)));
		divLp.topMargin = (int) (14 * d);
		divLp.bottomMargin = (int) (12 * d);
		innerDiv.setLayoutParams(divLp);
		innerDiv.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
		profileCard.addView(innerDiv);

		LinearLayout statsRow = new LinearLayout(this);
		statsRow.setOrientation(LinearLayout.HORIZONTAL);
		statsRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		tvStatWatchTime = new TextView(this);
		tvStatSavedCount = new TextView(this);
		tvStatExpCount = new TextView(this);

		statsRow.addView(buildStatColumn(tvStatWatchTime, "0m", "Waktu Tonton", d));
		statsRow.addView(buildStatColumn(tvStatSavedCount, "0", "Tersimpan", d));
		statsRow.addView(buildStatColumn(tvStatExpCount, "0", "Total XP", d));
		profileCard.addView(statsRow);

		root.addView(profileCard);

		LinearLayout lifetimeBanner = new LinearLayout(this);
		lifetimeBanner.setOrientation(LinearLayout.HORIZONTAL);
		lifetimeBanner.setGravity(Gravity.CENTER_VERTICAL);
		lifetimeBanner.setPadding((int) (16 * d), (int) (12 * d), (int) (16 * d), (int) (12 * d));
		LinearLayout.LayoutParams lifeBannerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lifeBannerLp.topMargin = (int) (12 * d);
		lifetimeBanner.setLayoutParams(lifeBannerLp);
		GradientDrawable lifeBannerBg = new GradientDrawable();
		lifeBannerBg.setColor(Color.parseColor("#0C2438"));
		lifeBannerBg.setCornerRadius(16 * d);
		lifeBannerBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#38BDF8"));
		lifetimeBanner.setBackground(lifeBannerBg);

		TextView tvLifetimeTitle = new TextView(this);
		tvLifetimeTitle.setText("✦ Premium Aktif Seumur Hidup");
		tvLifetimeTitle.setTextColor(Color.WHITE);
		tvLifetimeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		tvLifetimeTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvLifetimeTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		TextView tvLifetimeSub = new TextView(this);
		tvLifetimeSub.setText("Aktif");
		tvLifetimeSub.setTextColor(Color.parseColor("#38BDF8"));
		tvLifetimeSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
		tvLifetimeSub.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		lifetimeBanner.addView(tvLifetimeTitle);
		lifetimeBanner.addView(tvLifetimeSub);
		root.addView(lifetimeBanner);

		LinearLayout levelCard = new LinearLayout(this);
		levelCard.setOrientation(LinearLayout.VERTICAL);
		levelCard.setPadding((int) (16 * d), (int) (15 * d), (int) (16 * d), (int) (15 * d));
		LinearLayout.LayoutParams lvlCardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lvlCardLp.topMargin = (int) (12 * d);
		levelCard.setLayoutParams(lvlCardLp);
		GradientDrawable lvlCardBg = new GradientDrawable();
		lvlCardBg.setColor(Color.parseColor("#12141A"));
		lvlCardBg.setCornerRadius(18 * d);
		lvlCardBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E2433"));
		levelCard.setBackground(lvlCardBg);

		LinearLayout lvlTopRow = new LinearLayout(this);
		lvlTopRow.setOrientation(LinearLayout.HORIZONTAL);
		lvlTopRow.setGravity(Gravity.CENTER_VERTICAL);

		tvAccountLevelTitle = new TextView(this);
		tvAccountLevelTitle.setText("Level 1");
		tvAccountLevelTitle.setTextColor(Color.WHITE);
		tvAccountLevelTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
		tvAccountLevelTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvAccountLevelTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		tvAccountExpPercent = new TextView(this);
		tvAccountExpPercent.setText("0%");
		tvAccountExpPercent.setTextColor(Color.parseColor("#38BDF8"));
		tvAccountExpPercent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		tvAccountExpPercent.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		lvlTopRow.addView(tvAccountLevelTitle);
		lvlTopRow.addView(tvAccountExpPercent);
		levelCard.addView(lvlTopRow);

		tvAccountExpProgressLabel = new TextView(this);
		tvAccountExpProgressLabel.setText("0 / 1000 XP menuju level berikutnya");
		tvAccountExpProgressLabel.setTextColor(Color.parseColor("#94A3B8"));
		tvAccountExpProgressLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		tvAccountExpProgressLabel.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		LinearLayout.LayoutParams pLblLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		pLblLp.topMargin = (int) (4 * d);
		pLblLp.bottomMargin = (int) (10 * d);
		tvAccountExpProgressLabel.setLayoutParams(pLblLp);
		levelCard.addView(tvAccountExpProgressLabel);

		final FrameLayout barFrame = new FrameLayout(this);
		barFrame.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (7 * d)));

		viewAccountExpTrack = new View(this);
		viewAccountExpTrack.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		GradientDrawable trackBg = new GradientDrawable();
		trackBg.setColor(Color.parseColor("#1E2433"));
		trackBg.setCornerRadius(100f);
		viewAccountExpTrack.setBackground(trackBg);

		viewAccountExpFill = new View(this);
		viewAccountExpFill.setLayoutParams(new FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT));
		GradientDrawable fillBg = new GradientDrawable();
		fillBg.setColor(Color.parseColor("#38BDF8"));
		fillBg.setCornerRadius(100f);
		viewAccountExpFill.setBackground(fillBg);

		barFrame.addView(viewAccountExpTrack);
		barFrame.addView(viewAccountExpFill);
		levelCard.addView(barFrame);

		barFrame.post(new Runnable() {
			@Override
			public void run() {
				updateAccountExpBarWidth(barFrame.getWidth());
			}
		});

		root.addView(levelCard);

		LinearLayout menuCard = new LinearLayout(this);
		menuCard.setOrientation(LinearLayout.VERTICAL);
		menuCard.setPadding((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));
		LinearLayout.LayoutParams mCardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		mCardLp.topMargin = (int) (12 * d);
		menuCard.setLayoutParams(mCardLp);
		GradientDrawable mCardBg = new GradientDrawable();
		mCardBg.setColor(Color.parseColor("#12141A"));
		mCardBg.setCornerRadius(18 * d);
		mCardBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E2433"));
		menuCard.setBackground(mCardBg);

		menuCard.addView(buildAccountMenuItem(
			"Komentar Saya",
			"Lihat semua komentar yang pernah kamu tulis",
			Color.WHITE,
			d,
			new View.OnClickListener() {
				@Override public void onClick(View v) { openMyCommentsDialog(); }
			}
		));
		menuCard.addView(buildAccountMenuItem(
			"Pustaka & Riwayat",
			"Lihat riwayat tontonan dan favorit tersimpan",
			Color.WHITE,
			d,
			new View.OnClickListener() {
				@Override public void onClick(View v) { selectNavTabByIndex(2, true); }
			}
		));
		menuCard.addView(buildAccountMenuItem(
			"Chat & Komunitas",
			"Buka ruang obrolan dan request judul donghua",
			Color.WHITE,
			d,
			new View.OnClickListener() {
				@Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, ChatActivity.class)); }
			}
		));
		menuCard.addView(buildAccountMenuItem(
			"Cek Pembaruan Aplikasi",
			"Periksa versi terbaru Dongflix dari server",
			Color.parseColor("#38BDF8"),
			d,
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(MainActivity.this, "Memeriksa pembaruan...", Toast.LENGTH_SHORT).show();
					UpdateCheckerDialog.checkForUpdate(MainActivity.this, true);
				}
			}
		));
		menuCard.addView(buildAccountMenuItem(
			"Keluar Akun",
			"Logout dari sesi akun saat ini",
			Color.parseColor("#F87171"),
			d,
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					FirebaseAuth.getInstance().signOut();
					Intent i = new Intent(MainActivity.this, LoginActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(i);
					finish();
				}
			}
		));

		root.addView(menuCard);
		scroll.addView(root);
		profGroup.addView(scroll);
		refreshAccountUi();
	}

	private void openAccountSettingsDialog() {
		float d = getResources().getDisplayMetrics().density;
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		LinearLayout card = new LinearLayout(this);
		card.setOrientation(LinearLayout.VERTICAL);
		card.setPadding((int) (20 * d), (int) (20 * d), (int) (20 * d), (int) (18 * d));
		GradientDrawable cardBg = new GradientDrawable();
		cardBg.setColor(Color.parseColor("#12141A"));
		cardBg.setCornerRadius(20 * d);
		cardBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#24FFFFFF"));
		card.setBackground(cardBg);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Pengaturan Akun");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
		tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		card.addView(tvTitle);

		TextView tvInfo = new TextView(this);
		tvInfo.setText("Email: " + (currentUserEmail.isEmpty() ? "-" : currentUserEmail)
			+ "\nVersi Aplikasi: v" + UpdateCheckerDialog.getAppVersionName(this)
			+ "\nStatus: Premium Aktif Seumur Hidup"
			+ "\nSinkronisasi Riwayat & EXP: Aktif");
		tvInfo.setTextColor(Color.parseColor("#94A3B8"));
		tvInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		tvInfo.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		tvInfo.setLineSpacing(4 * d, 1.05f);
		LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		iLp.topMargin = (int) (10 * d);
		iLp.bottomMargin = (int) (18 * d);
		tvInfo.setLayoutParams(iLp);
		card.addView(tvInfo);

		LinearLayout btnRow = new LinearLayout(this);
		btnRow.setOrientation(LinearLayout.HORIZONTAL);
		btnRow.setGravity(Gravity.END);

		TextView btnEdit = new TextView(this);
		btnEdit.setText("Ubah Profil");
		btnEdit.setTextColor(Color.parseColor("#38BDF8"));
		btnEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnEdit.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnEdit.setPadding((int) (14 * d), (int) (8 * d), (int) (14 * d), (int) (8 * d));
		btnEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				openEditProfileDialog();
			}
		});

		TextView btnClose = new TextView(this);
		btnClose.setText("Tutup");
		btnClose.setTextColor(Color.WHITE);
		btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnClose.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnClose.setPadding((int) (18 * d), (int) (8 * d), (int) (18 * d), (int) (8 * d));
		LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		cLp.leftMargin = (int) (8 * d);
		btnClose.setLayoutParams(cLp);
		GradientDrawable cBg = new GradientDrawable();
		cBg.setColor(Color.parseColor("#2563EB"));
		cBg.setCornerRadius(100f);
		btnClose.setBackground(cBg);
		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { dialog.dismiss(); }
		});

		btnRow.addView(btnEdit);
		btnRow.addView(btnClose);
		card.addView(btnRow);

		dialog.setContentView(card);
		Window win = dialog.getWindow();
		if (win != null) {
			win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			int screenW = getResources().getDisplayMetrics().widthPixels;
			win.setLayout(Math.min((int) (340 * d), screenW - (int) (36 * d)), ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		dialog.show();
	}

	private void openMyCommentsDialog() {
		float d = getResources().getDisplayMetrics().density;
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding((int) (18 * d), (int) (18 * d), (int) (18 * d), (int) (16 * d));
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(Color.parseColor("#12141A"));
		bg.setCornerRadius(20 * d);
		bg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#24FFFFFF"));
		root.setBackground(bg);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Komentar Saya");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
		tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		root.addView(tvTitle);

		final TextView tvStatus = new TextView(this);
		tvStatus.setText("Memuat komentar...");
		tvStatus.setTextColor(Color.parseColor("#94A3B8"));
		tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		tvStatus.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		stLp.topMargin = (int) (10 * d);
		tvStatus.setLayoutParams(stLp);
		root.addView(tvStatus);

		ScrollView scroll = new ScrollView(this);
		LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, Math.min((int) (360 * d), getResources().getDisplayMetrics().heightPixels / 2)
		);
		scLp.topMargin = (int) (10 * d);
		scLp.bottomMargin = (int) (14 * d);
		scroll.setLayoutParams(scLp);

		final LinearLayout listBox = new LinearLayout(this);
		listBox.setOrientation(LinearLayout.VERTICAL);
		scroll.addView(listBox);
		root.addView(scroll);

		TextView btnClose = new TextView(this);
		btnClose.setText("Tutup");
		btnClose.setTextColor(Color.WHITE);
		btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnClose.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnClose.setGravity(Gravity.CENTER);
		btnClose.setPadding((int) (18 * d), (int) (9 * d), (int) (18 * d), (int) (9 * d));
		GradientDrawable cBg = new GradientDrawable();
		cBg.setColor(Color.parseColor("#2563EB"));
		cBg.setCornerRadius(100f);
		btnClose.setBackground(cBg);
		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { dialog.dismiss(); }
		});
		root.addView(btnClose);

		dialog.setContentView(root);
		Window win = dialog.getWindow();
		if (win != null) {
			win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			int screenW = getResources().getDisplayMetrics().widthPixels;
			win.setLayout(Math.min((int) (360 * d), screenW - (int) (28 * d)), ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		dialog.show();

		FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("comments")
			.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snap) {
					if (isFinishing() || isDestroyed()) return;
					ArrayList<HashMap<String, String>> mine = new ArrayList<>();
					FirebaseUser curU = FirebaseAuth.getInstance().getCurrentUser();
					String myUid = curU != null ? curU.getUid() : "";

					if (snap.exists()) {
						for (DataSnapshot slugSnap : snap.getChildren()) {
							String slugKey = slugSnap.getKey() != null ? slugSnap.getKey() : "";
							for (DataSnapshot itemSnap : slugSnap.getChildren()) {
								Object v = itemSnap.getValue();
								if (v instanceof Map) {
									Map<?, ?> m = (Map<?, ?>) v;
									String cEmail = m.get("email") != null ? m.get("email").toString().trim() : "";
									String cUid = m.get("uid") != null ? m.get("uid").toString().trim() : "";
									boolean match = (!currentUserEmail.isEmpty() && currentUserEmail.equalsIgnoreCase(cEmail))
										|| (!myUid.isEmpty() && myUid.equals(cUid));
									if (match) {
										HashMap<String, String> entry = new HashMap<>();
										entry.put("slug", slugKey);
										entry.put("text", m.get("text") != null ? m.get("text").toString().trim() : "");
										entry.put("stickerUrl", m.get("stickerUrl") != null ? m.get("stickerUrl").toString().trim() : "");
										entry.put("ts", String.valueOf(parseLongSafe(m.get("timestamp"))));
										mine.add(entry);
									}
								}
							}
						}
					}

					Collections.sort(mine, new Comparator<HashMap<String, String>>() {
						@Override
						public int compare(HashMap<String, String> a, HashMap<String, String> b) {
							return Long.compare(parseLongSafe(b.get("ts")), parseLongSafe(a.get("ts")));
						}
					});

					tvStatus.setText(mine.isEmpty()
						? "Kamu belum menulis komentar apa pun."
						: "Total " + mine.size() + " komentar");

					float density = getResources().getDisplayMetrics().density;
					listBox.removeAllViews();
					for (final HashMap<String, String> item : mine) {
						LinearLayout row = new LinearLayout(MainActivity.this);
						row.setOrientation(LinearLayout.VERTICAL);
						row.setPadding((int) (12 * density), (int) (10 * density), (int) (12 * density), (int) (10 * density));
						LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
						);
						rLp.bottomMargin = (int) (8 * density);
						row.setLayoutParams(rLp);
						GradientDrawable rBg = new GradientDrawable();
						rBg.setColor(Color.parseColor("#181C26"));
						rBg.setCornerRadius(12 * density);
						row.setBackground(rBg);

						TextView tvSlug = new TextView(MainActivity.this);
						tvSlug.setText(item.get("slug").replace("_", "-"));
						tvSlug.setTextColor(Color.parseColor("#38BDF8"));
						tvSlug.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
						tvSlug.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

						String bodyStr = item.get("text");
						if (bodyStr == null || bodyStr.isEmpty()) {
							bodyStr = !(item.get("stickerUrl") == null || item.get("stickerUrl").isEmpty())
								? "[Stiker]" : "";
						}

						TextView tvBody = new TextView(MainActivity.this);
						tvBody.setText(bodyStr);
						tvBody.setTextColor(Color.WHITE);
						tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
						tvBody.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
						LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
						);
						bLp.topMargin = (int) (3 * density);
						tvBody.setLayoutParams(bLp);

						row.addView(tvSlug);
						row.addView(tvBody);
						row.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								String target = item.get("slug");
								if (target != null && !target.isEmpty()) {
									dialog.dismiss();
									openWatchBySlug(target);
								}
							}
						});
						listBox.addView(row);
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					if (!isFinishing() && !isDestroyed()) {
						tvStatus.setText("Gagal memuat komentar.");
					}
				}
			});
	}

	private LinearLayout buildStatColumn(TextView valueView, String initVal, String label, float d) {
		LinearLayout col = new LinearLayout(this);
		col.setOrientation(LinearLayout.VERTICAL);
		col.setGravity(Gravity.CENTER);
		col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		valueView.setText(initVal);
		valueView.setTextColor(Color.WHITE);
		valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		valueView.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		TextView tvLbl = new TextView(this);
		tvLbl.setText(label);
		tvLbl.setTextColor(Color.parseColor("#94A3B8"));
		tvLbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tvLbl.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lLp.topMargin = (int) (2 * d);
		tvLbl.setLayoutParams(lLp);

		col.addView(valueView);
		col.addView(tvLbl);
		return col;
	}

	private LinearLayout buildAccountMenuItem(String title, String sub, int titleColor, float d, View.OnClickListener listener) {
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding((int) (14 * d), (int) (13 * d), (int) (14 * d), (int) (13 * d));
		row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		row.setOnClickListener(listener);

		LinearLayout txtCol = new LinearLayout(this);
		txtCol.setOrientation(LinearLayout.VERTICAL);
		txtCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		TextView tvTitle = new TextView(this);
		tvTitle.setText(title);
		tvTitle.setTextColor(titleColor);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
		tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		TextView tvSub = new TextView(this);
		tvSub.setText(sub);
		tvSub.setTextColor(Color.parseColor("#64748B"));
		tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tvSub.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		txtCol.addView(tvTitle);
		txtCol.addView(tvSub);

		TextView chev = new TextView(this);
		chev.setText("›");
		chev.setTextColor(Color.parseColor("#64748B"));
		chev.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
		chev.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		row.addView(txtCol);
		row.addView(chev);
		return row;
	}

	private void openEditProfileDialog() {
		float d = getResources().getDisplayMetrics().density;
		pendingEditPhotoUrl = currentPhotoUrl;

		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dInt) {
				dialogEditAvatarPreview = null;
				dialogEditAvatarInitial = null;
			}
		});

		LinearLayout card = new LinearLayout(this);
		card.setOrientation(LinearLayout.VERTICAL);
		card.setPadding((int) (20 * d), (int) (20 * d), (int) (20 * d), (int) (18 * d));
		GradientDrawable cardBg = new GradientDrawable();
		cardBg.setColor(Color.parseColor("#12141A"));
		cardBg.setCornerRadius(20 * d);
		cardBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#24FFFFFF"));
		card.setBackground(cardBg);

		TextView tvTitle = new TextView(this);
		tvTitle.setText("Ubah Profil");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
		tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		card.addView(tvTitle);

		LinearLayout photoRow = new LinearLayout(this);
		photoRow.setOrientation(LinearLayout.HORIZONTAL);
		photoRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams prLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		prLp.topMargin = (int) (14 * d);
		photoRow.setLayoutParams(prLp);

		FrameLayout avatarBox = new FrameLayout(this);
		LinearLayout.LayoutParams abLp = new LinearLayout.LayoutParams((int) (54 * d), (int) (54 * d));
		abLp.rightMargin = (int) (12 * d);
		avatarBox.setLayoutParams(abLp);

		dialogEditAvatarPreview = new ImageView(this);
		dialogEditAvatarPreview.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		dialogEditAvatarPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

		dialogEditAvatarInitial = new TextView(this);
		dialogEditAvatarInitial.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		dialogEditAvatarInitial.setGravity(Gravity.CENTER);
		dialogEditAvatarInitial.setTextColor(Color.WHITE);
		dialogEditAvatarInitial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		dialogEditAvatarInitial.setTypeface(getOutfitTypeface(), Typeface.NORMAL);

		avatarBox.addView(dialogEditAvatarPreview);
		avatarBox.addView(dialogEditAvatarInitial);
		bindAvatarPreview(dialogEditAvatarPreview, dialogEditAvatarInitial, pendingEditPhotoUrl, currentDisplayName);

		TextView btnPickPhoto = new TextView(this);
		btnPickPhoto.setText("Pilih Foto");
		btnPickPhoto.setTextColor(Color.WHITE);
		btnPickPhoto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		btnPickPhoto.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnPickPhoto.setPadding((int) (14 * d), (int) (8 * d), (int) (14 * d), (int) (8 * d));
		GradientDrawable pickBg = new GradientDrawable();
		pickBg.setColor(Color.parseColor("#1A2030"));
		pickBg.setCornerRadius(100f);
		pickBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#2AFFFFFF"));
		btnPickPhoto.setBackground(pickBg);
		btnPickPhoto.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.setType("image/*");
					startActivityForResult(Intent.createChooser(intent, "Pilih Foto Profil"), REQ_PICK_AVATAR);
				} catch (Exception e) {
					Toast.makeText(MainActivity.this, "Tidak dapat membuka galeri", Toast.LENGTH_SHORT).show();
				}
			}
		});

		TextView btnResetPhoto = new TextView(this);
		btnResetPhoto.setText("Hapus");
		btnResetPhoto.setTextColor(Color.parseColor("#94A3B8"));
		btnResetPhoto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
		btnResetPhoto.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnResetPhoto.setPadding((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
		btnResetPhoto.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pendingEditPhotoUrl = "";
				bindAvatarPreview(dialogEditAvatarPreview, dialogEditAvatarInitial, "", currentDisplayName);
			}
		});

		photoRow.addView(avatarBox);
		photoRow.addView(btnPickPhoto);
		photoRow.addView(btnResetPhoto);
		card.addView(photoRow);

		final EditText etName = new EditText(this);
		etName.setText(currentDisplayName);
		etName.setHint("Nama tampilan");
		etName.setHintTextColor(Color.parseColor("#64748B"));
		etName.setTextColor(Color.WHITE);
		etName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		etName.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		etName.setSingleLine(true);
		etName.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));
		LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		nLp.topMargin = (int) (14 * d);
		etName.setLayoutParams(nLp);
		GradientDrawable inBg = new GradientDrawable();
		inBg.setColor(Color.parseColor("#181C26"));
		inBg.setCornerRadius(12 * d);
		inBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1FFFFFFF"));
		etName.setBackground(inBg);
		card.addView(etName);

		final EditText etUsername = new EditText(this);
		etUsername.setText(currentUsername);
		etUsername.setHint("Username (tanpa @)");
		etUsername.setHintTextColor(Color.parseColor("#64748B"));
		etUsername.setTextColor(Color.WHITE);
		etUsername.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		etUsername.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		etUsername.setSingleLine(true);
		etUsername.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));
		LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		uLp.topMargin = (int) (10 * d);
		etUsername.setLayoutParams(uLp);
		etUsername.setBackground(inBg.getConstantState() != null ? inBg.getConstantState().newDrawable() : inBg);
		card.addView(etUsername);

		final TextView tvUsernameStatus = new TextView(this);
		tvUsernameStatus.setText("Username minimal 3 karakter (huruf, angka, _)");
		tvUsernameStatus.setTextColor(Color.parseColor("#94A3B8"));
		tvUsernameStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tvUsernameStatus.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		stLp.topMargin = (int) (6 * d);
		stLp.bottomMargin = (int) (16 * d);
		tvUsernameStatus.setLayoutParams(stLp);
		card.addView(tvUsernameStatus);

		final Handler checkHandler = new Handler(Looper.getMainLooper());
		final Runnable[] pendingCheck = new Runnable[1];

		etUsername.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
			@Override
			public void onTextChanged(CharSequence s, int st, int b, int c) {
				if (pendingCheck[0] != null) checkHandler.removeCallbacks(pendingCheck[0]);
				final String candidate = s.toString().trim().replaceFirst("^@", "").toLowerCase(Locale.US);
				if (candidate.length() < 3) {
					tvUsernameStatus.setText("Username minimal 3 karakter");
					tvUsernameStatus.setTextColor(Color.parseColor("#F87171"));
					return;
				}
				tvUsernameStatus.setText("Mengecek ketersediaan username...");
				tvUsernameStatus.setTextColor(Color.parseColor("#94A3B8"));
				pendingCheck[0] = new Runnable() {
					@Override
					public void run() {
						verifyUsernameAvailability(candidate, new UsernameCheckCallback() {
							@Override
							public void onResult(boolean available, String message) {
								if (dialog.isShowing()) {
									tvUsernameStatus.setText(message);
									tvUsernameStatus.setTextColor(
										available ? Color.parseColor("#34D399") : Color.parseColor("#F87171")
									);
								}
							}
						});
					}
				};
				checkHandler.postDelayed(pendingCheck[0], 350);
			}
			@Override public void afterTextChanged(Editable s) {}
		});

		LinearLayout btnRow = new LinearLayout(this);
		btnRow.setOrientation(LinearLayout.HORIZONTAL);
		btnRow.setGravity(Gravity.END);

		TextView btnCancel = new TextView(this);
		btnCancel.setText("Batal");
		btnCancel.setTextColor(Color.parseColor("#94A3B8"));
		btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnCancel.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnCancel.setPadding((int) (16 * d), (int) (9 * d), (int) (16 * d), (int) (9 * d));
		btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) { dialog.dismiss(); }
		});

		final TextView btnSave = new TextView(this);
		btnSave.setText("Simpan");
		btnSave.setTextColor(Color.WHITE);
		btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnSave.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
		btnSave.setPadding((int) (18 * d), (int) (9 * d), (int) (18 * d), (int) (9 * d));
		LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		sLp.leftMargin = (int) (8 * d);
		btnSave.setLayoutParams(sLp);
		GradientDrawable saveBg = new GradientDrawable();
		saveBg.setColor(Color.parseColor("#2563EB"));
		saveBg.setCornerRadius(100f);
		btnSave.setBackground(saveBg);
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String newName = etName.getText().toString().trim();
				String rawUser = etUsername.getText().toString().trim().replaceFirst("^@", "").replaceAll("\\s+", "_").toLowerCase(Locale.US);
				if (newName.isEmpty()) {
					Toast.makeText(MainActivity.this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
					return;
				}
				if (rawUser.isEmpty()) {
					rawUser = newName.toLowerCase(Locale.US).replaceAll("[^a-z0-9_]", "");
					if (rawUser.isEmpty()) rawUser = "user";
				}
				if (rawUser.length() < 3) {
					Toast.makeText(MainActivity.this, "Username minimal 3 karakter", Toast.LENGTH_SHORT).show();
					return;
				}

				final String finalUser = rawUser;
				btnSave.setEnabled(false);
				btnSave.setText("Mengecek...");

				verifyUsernameAvailability(finalUser, new UsernameCheckCallback() {
					@Override
					public void onResult(boolean available, String message) {
						btnSave.setEnabled(true);
						btnSave.setText("Simpan");
						if (!available) {
							tvUsernameStatus.setText(message);
							tvUsernameStatus.setTextColor(Color.parseColor("#F87171"));
							Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
							return;
						}

						currentDisplayName = newName;
						currentUsername = finalUser;
						currentPhotoUrl = pendingEditPhotoUrl != null ? pendingEditPhotoUrl : "";

						TextView tvHeader = findViewById(R.id.text_username);
						if (tvHeader != null) tvHeader.setText(currentDisplayName);
						applyCircularAvatar(imgUserAvatar, currentPhotoUrl);
						refreshAccountUi();

						if (!currentUserKey.isEmpty()) {
							Map<String, Object> updates = new HashMap<>();
							updates.put("name", currentDisplayName);
							updates.put("username", currentUsername);
							updates.put("photoURL", currentPhotoUrl);
							updates.put("email", currentUserEmail);
							updates.put("isLifetimePremium", true);
							FirebaseDatabase.getInstance(FIREBASE_DB_URL)
								.getReference("users")
								.child(currentUserKey)
								.updateChildren(updates);
						}

						Toast.makeText(MainActivity.this, "Profil diperbarui", Toast.LENGTH_SHORT).show();
						dialog.dismiss();
					}
				});
			}
		});

		btnRow.addView(btnCancel);
		btnRow.addView(btnSave);
		card.addView(btnRow);

		dialog.setContentView(card);
		Window win = dialog.getWindow();
		if (win != null) {
			win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			int screenW = getResources().getDisplayMetrics().widthPixels;
			win.setLayout(Math.min((int) (345 * d), screenW - (int) (32 * d)), ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		dialog.show();
	}

	private interface UsernameCheckCallback {
		void onResult(boolean available, String message);
	}

	private void verifyUsernameAvailability(final String candidateUsername, final UsernameCheckCallback cb) {
		final String normCandidate = candidateUsername.trim().replaceFirst("^@", "").toLowerCase(Locale.US);
		if (normCandidate.equalsIgnoreCase(currentUsername)) {
			cb.onResult(true, "Username tersedia (username kamu saat ini)");
			return;
		}
		if (!normCandidate.matches("^[a-z0-9_]{3,20}$")) {
			cb.onResult(false, "Gunakan 3-20 karakter huruf kecil, angka, atau _");
			return;
		}

		FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snap) {
					if (isFinishing() || isDestroyed()) return;
					boolean taken = false;
					if (snap.exists()) {
						for (DataSnapshot child : snap.getChildren()) {
							String childKey = child.getKey() != null ? child.getKey() : "";
							if (!currentUserKey.isEmpty() && currentUserKey.equalsIgnoreCase(childKey)) {
								continue;
							}
							Object uVal = child.child("username").getValue();
							if (uVal != null && uVal.toString().trim().replaceFirst("^@", "").equalsIgnoreCase(normCandidate)) {
								taken = true;
								break;
							}
						}
					}
					if (taken) {
						cb.onResult(false, "Username @" + normCandidate + " sudah digunakan");
					} else {
						cb.onResult(true, "Username @" + normCandidate + " tersedia");
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					cb.onResult(false, "Gagal mengecek username");
				}
			});
	}

	private void refreshAccountUi() {
		if (tvAccountName != null) tvAccountName.setText(currentDisplayName);
		if (tvAccountHandle != null) tvAccountHandle.setText("@" + currentUsername);

		String initial = !currentDisplayName.trim().isEmpty()
			? currentDisplayName.trim().substring(0, 1).toUpperCase(Locale.US) : "R";
		if (!currentPhotoUrl.trim().isEmpty()) {
			if (imgAccountAvatar != null) {
				imgAccountAvatar.setVisibility(View.VISIBLE);
				applyCircularAvatar(imgAccountAvatar, currentPhotoUrl.trim());
			}
			if (tvAccountInitial != null) tvAccountInitial.setVisibility(View.GONE);
		} else {
			if (imgAccountAvatar != null) imgAccountAvatar.setVisibility(View.GONE);
			if (tvAccountInitial != null) {
				tvAccountInitial.setVisibility(View.VISIBLE);
				tvAccountInitial.setText(initial);
				GradientDrawable iniBg = new GradientDrawable();
				iniBg.setShape(GradientDrawable.OVAL);
				iniBg.setColor(Color.parseColor("#1D4ED8"));
				tvAccountInitial.setBackground(iniBg);
			}
		}

		long totalWatchedMs = 0L;
		for (HashMap<String, Object> h : historyList) {
			totalWatchedMs += parseLongSafe(h.get("positionMs"));
		}
		long totalMinutes = Math.max(0L, totalWatchedMs / 60000L);
		String watchLabel = totalMinutes >= 60L
			? String.format(Locale.US, "%dj %dm", totalMinutes / 60L, totalMinutes % 60L)
			: totalMinutes + "m";

		if (tvStatWatchTime != null) tvStatWatchTime.setText(watchLabel);
		if (tvStatSavedCount != null) tvStatSavedCount.setText(String.valueOf(getMergedFavoritesList().size()));
		if (tvStatExpCount != null) tvStatExpCount.setText(String.valueOf(currentUserExp));

		int level = currentUserExp <= 0L ? 1 : (int) ((currentUserExp - 1L) / 1000L) + 1;
		long expInLevel = currentUserExp <= 0L ? 0L : ((currentUserExp - 1L) % 1000L) + 1L;
		int percent = (int) Math.max(0L, Math.min(100L, (expInLevel * 100L) / 1000L));

		if (tvAccountLevelTitle != null) tvAccountLevelTitle.setText("Level " + level);
		if (tvAccountExpProgressLabel != null) {
			tvAccountExpProgressLabel.setText(expInLevel + " / 1000 XP menuju level " + (level + 1));
		}
		if (tvAccountExpPercent != null) tvAccountExpPercent.setText(percent + "%");

		if (viewAccountExpTrack != null && viewAccountExpTrack.getParent() instanceof View) {
			View parentBar = (View) viewAccountExpTrack.getParent();
			if (parentBar.getWidth() > 0) {
				updateAccountExpBarWidth(parentBar.getWidth());
			}
		}
	}

	private void updateAccountExpBarWidth(int totalBarWidth) {
		if (viewAccountExpFill == null || totalBarWidth <= 0) return;
		long expInLevel = currentUserExp <= 0L ? 0L : ((currentUserExp - 1L) % 1000L) + 1L;
		float ratio = Math.max(0f, Math.min(1f, expInLevel / 1000f));
		int targetW = Math.round(totalBarWidth * ratio);
		if (ratio > 0f && targetW < 8) targetW = 8;
		ViewGroup.LayoutParams lp = viewAccountExpFill.getLayoutParams();
		if (lp != null && lp.width != targetW) {
			lp.width = targetW;
			viewAccountExpFill.setLayoutParams(lp);
		}
	}

	private void loadUserHistoryAndFavoritesFromFirebase(FirebaseUser user) {
		if (user == null || user.getEmail() == null) return;
		currentUserKey = user.getEmail().trim().toLowerCase().replace(".", ",");
		DatabaseReference userNode = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference("users").child(currentUserKey);

		userNode.child("history").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed()) return;
				ArrayList<HashMap<String, Object>> parsed = new ArrayList<>();
				if (snap.exists()) {
					for (DataSnapshot child : snap.getChildren()) {
						HashMap<String, Object> item = extractMapFromSnapshot(child);
						if (!item.isEmpty()) {
							if (!item.containsKey("slug") && child.getKey() != null) item.put("slug", child.getKey());
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
				refreshAccountUi();
				if (activePustakaTab == 0) renderActivePustakaTab();
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		});

		userNode.child("favorites").addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed()) return;
				ArrayList<HashMap<String, Object>> parsedFavs = new ArrayList<>();
				if (snap.exists()) {
					for (DataSnapshot child : snap.getChildren()) {
						HashMap<String, Object> item = extractMapFromSnapshot(child);
						if (!item.isEmpty()) {
							if (!item.containsKey("slug") && child.getKey() != null) item.put("slug", child.getKey());
							parsedFavs.add(item);
						}
					}
				}
				favoritesList = parsedFavs;
				refreshAccountUi();
				if (activePustakaTab == 1) renderActivePustakaTab();
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		});
	}

	private HashMap<String, Object> extractMapFromSnapshot(DataSnapshot snap) {
		HashMap<String, Object> map = new HashMap<>();
		if (snap == null || !snap.exists()) return map;
		Object val = snap.getValue();
		if (val instanceof Map) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) val).entrySet()) {
				if (entry.getKey() != null) map.put(entry.getKey().toString(), entry.getValue());
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
		try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return 0L; }
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
		try {
			File[] dirs = {
				getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
				getExternalFilesDir(Environment.DIRECTORY_MOVIES),
				getFilesDir()
			};
			for (File dir : dirs) {
				if (dir != null && dir.exists() && dir.isDirectory() && dir.canRead()) {
					collectDownloadFiles(dir, found, 0);
				}
			}
		} catch (Exception ignored) {}
		downloadsList = found;
	}

	private void collectDownloadFiles(File dir, ArrayList<HashMap<String, Object>> out, int depth) {
		if (dir == null || depth > 3) return;
		File[] children;
		try {
			children = dir.listFiles();
		} catch (Exception e) {
			return;
		}
		if (children == null) return;

		for (File f : children) {
			if (f == null || !f.exists() || !f.canRead()) continue;
			if (f.isDirectory()) {
				collectDownloadFiles(f, out, depth + 1);
			} else if (f.length() > 0L) {
				String lower = f.getName().toLowerCase(Locale.US);
				if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm")) {
					HashMap<String, Object> item = new HashMap<>();
					item.put("title", f.getName().replaceAll("\\.[^.]+$", "").replace("_", " "));
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

		ArrayList<HashMap<String, Object>> activeData;
		String sectionLabel;
		String emptyTitle;
		String emptyDesc;

		if (activePustakaTab == 0) {
			activeData = historyList != null ? historyList : new ArrayList<HashMap<String, Object>>();
			sectionLabel = "Terakhir Ditonton";
			emptyTitle = "Belum ada riwayat tontonan";
			emptyDesc = "Episode yang kamu tonton akan muncul di sini.";
		} else if (activePustakaTab == 1) {
			activeData = getMergedFavoritesList();
			sectionLabel = "Daftar Tersimpan";
			emptyTitle = "Daftarku masih kosong";
			emptyDesc = "Simpan donghua favoritmu untuk ditonton nanti.";
		} else {
			loadLocalDownloads();
			activeData = downloadsList != null ? downloadsList : new ArrayList<HashMap<String, Object>>();
			sectionLabel = "Video Offline";
			emptyTitle = "Belum ada unduhan";
			emptyDesc = "Video yang kamu unduh akan tersimpan di perangkat ini.";
		}

		if (textPustakaCountBadge != null) textPustakaCountBadge.setText(activeData.size() + " item");

		if (activeData.isEmpty()) {
			if (layoutPustakaEmptyState != null) {
				layoutPustakaEmptyState.setVisibility(View.VISIBLE);
				if (textPustakaEmptyTitle != null) textPustakaEmptyTitle.setText(emptyTitle);
				if (textPustakaEmptyDesc != null) textPustakaEmptyDesc.setText(emptyDesc);
			}
			recyclerPustakaList.setVisibility(View.GONE);
		} else {
			if (layoutPustakaEmptyState != null) layoutPustakaEmptyState.setVisibility(View.GONE);
			recyclerPustakaList.setVisibility(View.VISIBLE);
			recyclerPustakaList.setAdapter(new PustakaListAdapter(activeData, activePustakaTab, sectionLabel));
		}
	}

	private void openDownloadedVideoSafely(String filePath, String title) {
		if (filePath == null || filePath.trim().isEmpty()) {
			Toast.makeText(this, "File video tidak ditemukan", Toast.LENGTH_SHORT).show();
			return;
		}
		File videoFile = new File(filePath);
		if (!videoFile.exists() || !videoFile.canRead() || videoFile.length() <= 0L) {
			Toast.makeText(this, "File video rusak atau sudah dihapus", Toast.LENGTH_SHORT).show();
			renderActivePustakaTab();
			return;
		}
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			Uri uri;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				uri = androidx.core.content.FileProvider.getUriForFile(
					this,
					getPackageName() + ".fileprovider",
					videoFile
				);
				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			} else {
				uri = Uri.fromFile(videoFile);
			}
			intent.setDataAndType(uri, "video/*");
			startActivity(Intent.createChooser(intent, "Putar " + title));
		} catch (Exception e) {
			Toast.makeText(this, "Tidak ada pemutar video yang mendukung file ini", Toast.LENGTH_SHORT).show();
		}
	}

	private void removePustakaItem(int mode, HashMap<String, Object> item) {
		if (item == null) return;
		if (mode == 0) {
			String slug = safeStr(item, "slug", "");
			if (!slug.isEmpty() && !currentUserKey.isEmpty()) {
				FirebaseDatabase.getInstance(FIREBASE_DB_URL)
					.getReference("users").child(currentUserKey).child("history").child(slug)
					.removeValue();
			}
			historyList.remove(item);
			renderContinueWatchingCards();
			renderActivePustakaTab();
		} else if (mode == 1) {
			String slug = safeStr(item, "slug", "");
			if (!slug.isEmpty()) {
				if (favPrefs != null) favPrefs.edit().remove("fav_" + slug).apply();
				if (!currentUserKey.isEmpty()) {
					FirebaseDatabase.getInstance(FIREBASE_DB_URL)
						.getReference("users").child(currentUserKey).child("favorites").child(slug)
						.removeValue();
				}
			}
			favoritesList.remove(item);
			renderActivePustakaTab();
		} else {
			String path = safeStr(item, "file_path", "");
			if (!path.isEmpty()) {
				try {
					File f = new File(path);
					if (f.exists()) f.delete();
				} catch (Exception ignored) {}
			}
			renderActivePustakaTab();
		}
	}

	private ArrayList<HashMap<String, Object>> getMergedFavoritesList() {
		LinkedHashMap<String, HashMap<String, Object>> dedup = new LinkedHashMap<>();
		for (HashMap<String, Object> item : favoritesList) {
			String s = safeStr(item, "slug", "");
			if (!s.isEmpty()) dedup.put(s, item);
		}
		if (favPrefs != null) {
			for (Map.Entry<String, ?> e : favPrefs.getAll().entrySet()) {
				if (e.getKey().startsWith("fav_") && e.getValue() instanceof String) {
					try {
						HashMap<String, Object> localMap = new Gson().fromJson(
							e.getValue().toString(), new TypeToken<HashMap<String, Object>>(){}.getType()
						);
						if (localMap != null) {
							String s = safeStr(localMap, "slug", e.getKey().substring(4));
							if (!dedup.containsKey(s)) dedup.put(s, localMap);
						}
					} catch (Exception ignored) {}
				}
			}
		}
		return new ArrayList<>(dedup.values());
	}

	private ArrayList<HashMap<String, Object>> sortCatalogByViews(
		List<HashMap<String, Object>> src, final Map<String, Long> primary, final Map<String, Long> secondary
	) {
		ArrayList<HashMap<String, Object>> sorted = new ArrayList<>(src);
		Collections.sort(sorted, new Comparator<HashMap<String, Object>>() {
			@Override
			public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
				String sA = safeStr(a, "slug", "");
				String sB = safeStr(b, "slug", "");
				long pA = primary.containsKey(sA) ? primary.get(sA) : 0L;
				long pB = primary.containsKey(sB) ? primary.get(sB) : 0L;
				if (pB != pA) return Long.compare(pB, pA);
				if (secondary == null) return 0;
				long secA = secondary.containsKey(sA) ? secondary.get(sA) : 0L;
				long secB = secondary.containsKey(sB) ? secondary.get(sB) : 0L;
				return Long.compare(secB, secA);
			}
		});
		return sorted;
	}

	private void sortAndRenderPopularSections() {
		if (fullCatalogList == null || fullCatalogList.isEmpty()) return;

		ArrayList<HashMap<String, Object>> todaySorted = sortCatalogByViews(fullCatalogList, viewsDailyMap, viewsTotalMap);
		ArrayList<HashMap<String, Object>> dongflixSorted = sortCatalogByViews(fullCatalogList, viewsTotalMap, viewsDailyMap);

		ArrayList<HashMap<String, Object>> todaySlice = new ArrayList<>(todaySorted.subList(0, Math.min(8, todaySorted.size())));
		ArrayList<HashMap<String, Object>> dongflixPopularSlice = new ArrayList<>(dongflixSorted.subList(0, Math.min(10, dongflixSorted.size())));

		bindSectionLikePopularToday(recyclerPopular, todaySlice);
		bindSectionLikePopularToday(recyclerPopularDongflix, dongflixPopularSlice);

		if (recyclerSearchList != null && (inputSearch == null || inputSearch.getText().toString().trim().isEmpty())) {
			recyclerSearchList.setAdapter(new ListPopularAdapter(dongflixPopularSlice));
		}
		populateCategorizedSections();
		if (activeNavTabIndex == 1) renderJelajahiGrid();
	}

	private void attachFirebasePopularityListeners() {
		detachFirebasePopularityListeners();

		Calendar cal = Calendar.getInstance();
		String todayStr = String.format(Locale.US, "%04d-%02d-%02d",
			cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));

		DatabaseReference rootDb = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference();
		viewsTotalRef = rootDb.child("views_total");
		viewsDailyTodayRef = rootDb.child("views_daily").child(todayStr);

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
				sortAndRenderPopularSections();
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		};

		viewsDailyListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed()) return;
				viewsDailyMap.clear();
				if (snap.exists()) {
					for (DataSnapshot child : snap.getChildren()) {
						if (child.getKey() != null) {
							viewsDailyMap.put(child.getKey(), parseLongSafe(child.getValue()));
						}
					}
				}
				sortAndRenderPopularSections();
			}

			@Override public void onCancelled(@NonNull DatabaseError error) {}
		};

		viewsTotalRef.addValueEventListener(viewsTotalListener);
		viewsDailyTodayRef.addValueEventListener(viewsDailyListener);
	}

	private void detachFirebasePopularityListeners() {
		if (viewsTotalRef != null && viewsTotalListener != null) {
			viewsTotalRef.removeEventListener(viewsTotalListener);
		}
		if (viewsDailyTodayRef != null && viewsDailyListener != null) {
			viewsDailyTodayRef.removeEventListener(viewsDailyListener);
		}
	}

	private boolean isFavoriteSlug(String slug) {
		return favPrefs != null && slug != null && !slug.isEmpty() && favPrefs.contains("fav_" + slug);
	}

	private void toggleFavoriteItem(HashMap<String, Object> item) {
		if (favPrefs == null || item == null) return;
		String slug = safeStr(item, "slug", "");
		String title = safeStr(item, "title", "Donghua");
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
		Intent intent = new Intent(this, WatchActivity.class);
		intent.putExtra("slug", slug.trim());
		startActivity(intent);
		overridePendingTransition(R.anim.slide_up_enter, R.anim.no_anim);
	}

	private void openNotificationsSheet() {
		String latestTitle = currentHeroItem != null ? safeStr(currentHeroItem, "title", "Episode terbaru Dongflix telah tersedia.") : "Episode terbaru Dongflix telah tersedia.";
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
				if (imm != null) imm.showSoftInput(inputSearch, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 120);
	}

	private void hideKeyboard() {
		if (inputSearch == null) return;
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
	}

	private Drawable createMainShimmerDrawable(final float cornerRadiusPx) {
		return new Drawable() {
			private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Matrix shaderMatrix = new Matrix();
			private LinearGradient gradient;
			private int lastWidth = -1;

			{ basePaint.setColor(Color.parseColor("#12141A")); }

			@Override
			public void draw(Canvas canvas) {
				Rect b = getBounds();
				int w = b.width(), h = b.height();
				if (w <= 0 || h <= 0) return;
				canvas.drawRoundRect(new RectF(b), cornerRadiusPx, cornerRadiusPx, basePaint);

				if (gradient == null || lastWidth != w) {
					lastWidth = w;
					gradient = new LinearGradient(0, 0, w, 0,
						new int[]{Color.TRANSPARENT, Color.parseColor("#1AFFFFFF"), Color.parseColor("#33FFFFFF"), Color.parseColor("#1AFFFFFF"), Color.TRANSPARENT},
						new float[]{0f, 0.35f, 0.5f, 0.65f, 1f}, Shader.TileMode.CLAMP);
					shimmerPaint.setShader(gradient);
				}
				shaderMatrix.setTranslate(-w + (2f * w * mainShimmerPhase), 0);
				gradient.setLocalMatrix(shaderMatrix);
				canvas.drawRoundRect(new RectF(b), cornerRadiusPx, cornerRadiusPx, shimmerPaint);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	private void startMainShimmer() {
		isMainLoading = true;
		mainShimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
		mainShimmerAnimator.setDuration(1250);
		mainShimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
		mainShimmerAnimator.setInterpolator(new LinearInterpolator());
		mainShimmerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				if (!isMainLoading) return;
				mainShimmerPhase = (float) animation.getAnimatedValue();
				View[] singles = {imgHeroBanner, textHeroTitle, textHeroDesc, btnHeroPlay, btnHeroFavorite};
				for (View v : singles) if (v != null) v.invalidate();

				RecyclerView[] hRecyclers = {
					recyclerPopular, recyclerPopularDongflix, recyclerRomanceDonghua,
					recyclerCultivationDonghua, recyclerCompletedDonghua,
					recyclerComedyDonghua, recyclerMysteryDonghua, recyclerMovieDonghua,
					recyclerJelajahiGrid
				};
				for (RecyclerView rv : hRecyclers) {
					if (rv != null) for (int i = 0; i < rv.getChildCount(); i++) invalidateRecursive(rv.getChildAt(i));
				}
				if (gridview1 != null) {
					for (int i = 0; i < gridview1.getChildCount(); i++) invalidateRecursive(gridview1.getChildAt(i));
				}
			}
		});
		mainShimmerAnimator.start();
	}

	private void invalidateRecursive(View v) {
		if (v == null) return;
		v.invalidate();
		if (v instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) v;
			for (int i = 0; i < vg.getChildCount(); i++) invalidateRecursive(vg.getChildAt(i));
		}
	}

	private void stopMainShimmer() {
		isMainLoading = false;
		if (mainShimmerAnimator != null) {
			mainShimmerAnimator.cancel();
			mainShimmerAnimator = null;
		}
	}

	@Override
	protected void onDestroy() {
		stopMainShimmer();
		stopHeroAutoSlider();
		detachFirebasePopularityListeners();
		if (userProfileRef != null && userProfileListener != null) {
			userProfileRef.removeEventListener(userProfileListener);
		}
		super.onDestroy();
	}

	private void initialize() {
		dongflix_req = new RequestNetwork(this);

		pullRefreshIndicator = findViewById(R.id.pull_refresh_indicator);
		pullRefreshSpinner = findViewById(R.id.pull_refresh_spinner);
		imgPullRefreshIcon = findViewById(R.id.img_pull_refresh_icon);

		gridview1 = findViewById(R.id.gridview1);
		recyclerPopular = findViewById(R.id.recyclerPopular);
		recyclerPopularDongflix = findViewById(R.id.recyclerPopularDongflix);
		recyclerRomanceDonghua = findViewById(R.id.recyclerRomanceDonghua);
		recyclerCultivationDonghua = findViewById(R.id.recyclerCultivationDonghua);
		recyclerCompletedDonghua = findViewById(R.id.recyclerCompletedDonghua);
		recyclerComedyDonghua = findViewById(R.id.recyclerComedyDonghua);
		recyclerMysteryDonghua = findViewById(R.id.recyclerMysteryDonghua);
		recyclerMovieDonghua = findViewById(R.id.recyclerMovieDonghua);

		recyclerSearchList = findViewById(R.id.recycler_search_list);
		searchBarContainer = findViewById(R.id.search_bar_container);
		inputSearch = findViewById(R.id.input_search);
		btnClearSearch = findViewById(R.id.btn_clear_search);
		textSearchSectionLabel = findViewById(R.id.text_search_section_label);

		textHeroTitle = findViewById(R.id.text_hero_title);
		textHeroDesc = findViewById(R.id.text_hero_desc);
		textSeeAll = findViewById(R.id.text_see_all);
		textPopularDongflixSeeAll = findViewById(R.id.text_popular_dongflix_see_all);
		textRomanceSeeAll = findViewById(R.id.text_romance_see_all);
		textCultivationSeeAll = findViewById(R.id.text_cultivation_see_all);
		textCompletedSeeAll = findViewById(R.id.text_completed_see_all);
		textComedySeeAll = findViewById(R.id.text_comedy_see_all);
		textMysterySeeAll = findViewById(R.id.text_mystery_see_all);
		textMovieSeeAll = findViewById(R.id.text_movie_see_all);

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

		topActionPill = findViewById(R.id.top_action_pill);
		topActionPillBlurBg = findViewById(R.id.top_action_pill_blur_bg);
		btnTopNotif = findViewById(R.id.btn_top_search);
		btnTopChat = findViewById(R.id.btn_top_chat);
		imgTopNotif = findViewById(R.id.img_top_search);
		imgTopChat = findViewById(R.id.img_top_chat);

		navbarExpanded = findViewById(R.id.navbar);
		btnCompactHome = findViewById(R.id.btn_compact_home);
		miniPlayerInlineSlot = findViewById(R.id.mini_player_inline_slot);
		btnCircleSearch = findViewById(R.id.btn_circle_search);
		tabBeranda = findViewById(R.id.tab_beranda);
		tabJelajahi = findViewById(R.id.tab_jelajahi);
		tabPustaka = findViewById(R.id.tab_pustaka);
		tabAkun = findViewById(R.id.tab_akun);
		imgHome = findViewById(R.id.img_home);
		imgJelajahi = findViewById(R.id.img_jelajahi);
		imgFavorite = findViewById(R.id.img_favorite);
		imgProfile = findViewById(R.id.img_profile);
		imgSearch = findViewById(R.id.img_search);
		txtHome = findViewById(R.id.txt_home);
		txtJelajahi = findViewById(R.id.txt_jelajahi);
		txtFavorite = findViewById(R.id.txt_favorite);
		txtProfile = findViewById(R.id.txt_profile);

		final Map<TextView, String[]> seeAllConfig = new LinkedHashMap<>();
		seeAllConfig.put(textSeeAll, new String[]{"Baru Diupdate", "all"});
		seeAllConfig.put(textPopularDongflixSeeAll, new String[]{"Populer di Dongflix", "popular_dongflix"});
		seeAllConfig.put(textRomanceSeeAll, new String[]{"Romantis Tipis-Tipis", "romance"});
		seeAllConfig.put(textCultivationSeeAll, new String[]{"Kultivasi & Keabadian", "cultivation"});
		seeAllConfig.put(textCompletedSeeAll, new String[]{"Donghua Tamat", "completed"});
		seeAllConfig.put(textComedySeeAll, new String[]{"Komedi Ringan", "comedy"});
		seeAllConfig.put(textMysterySeeAll, new String[]{"Misteri & Teka-Teki", "mystery"});
		seeAllConfig.put(textMovieSeeAll, new String[]{"Donghua Movie", "movie"});

		for (Map.Entry<TextView, String[]> entry : seeAllConfig.entrySet()) {
			TextView tv = entry.getKey();
			final String sectionTitle = entry.getValue()[0];
			final String filterKey = entry.getValue()[1];
			if (tv != null) {
				attachRightIconToActionText(tv);
				tv.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(MainActivity.this, CatalogActivity.class);
						intent.putExtra("section_title", sectionTitle);
						intent.putExtra("filter_type", filterKey);
						startActivity(intent);
					}
				});
			}
		}

		attachRightIconToActionText(textContinueSeeAll);
		applyHeaderActionIcons();

		if (btnTopNotif != null) {
			btnTopNotif.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) { openNotificationsSheet(); }
			});
		}
		if (btnHeroFavorite != null) {
			btnHeroFavorite.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) { if (currentHeroItem != null) toggleFavoriteItem(currentHeroItem); }
			});
		}
		if (btnTopChat != null) {
			btnTopChat.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) { startActivity(new Intent(MainActivity.this, ChatActivity.class)); }
			});
		}
		if (gridview1 != null) {
			gridview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> p1, View p2, int pos, long p4) {
					if (listMap != null && pos < listMap.size()) {
						HashMap<String, Object> item = listMap.get(pos);
						if (item != null && !item.containsKey("is_loading")) openWatchBySlug(safeStr(item, "slug", ""));
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
				@Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
				@Override public void onTextChanged(CharSequence s, int st, int b, int c) {
					String q = s.toString().trim().toLowerCase();
					if (btnClearSearch != null) btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
					if (q.isEmpty()) {
						if (textSearchSectionLabel != null) {
							textSearchSectionLabel.setVisibility(View.VISIBLE);
							textSearchSectionLabel.setText("Populer di Dongflix");
						}
						if (recyclerSearchList != null) recyclerSearchList.setAdapter(new ListPopularAdapter(listMap));
					} else {
						if (textSearchSectionLabel != null) textSearchSectionLabel.setVisibility(View.GONE);
						ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
						for (HashMap<String, Object> item : fullCatalogList) {
							if (safeStr(item, "title", "").toLowerCase().contains(q)
								|| safeStr(item, "genres", "").toLowerCase().contains(q)) {
								filtered.add(item);
							}
						}
						if (recyclerSearchList != null) recyclerSearchList.setAdapter(new ListPopularAdapter(filtered));
					}
				}
				@Override public void afterTextChanged(Editable s) {}
			});
		}

		_dongflix_req_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String p1, String response, HashMap<String, Object> p3) {
				if (isFinishing() || isDestroyed()) return;
				stopMainShimmer();
				hideRefreshIndicator();
				DongflixPlayerManager.getInstance().cachedAllCatalogJson = response;

				try {
					fullCatalogList = new Gson().fromJson(response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
				} catch (Exception e) {
					fullCatalogList = new ArrayList<>();
				}
				if (fullCatalogList == null) fullCatalogList = new ArrayList<>();

				listMap = new ArrayList<>(fullCatalogList.subList(0, Math.min(12, fullCatalogList.size())));
				if (gridview1 != null) {
					gridview1.setAdapter(new Gridview1Adapter(listMap));
					setGridViewHeightBasedOnChildren(gridview1, 3);
				}
				if (recyclerSearchList != null) recyclerSearchList.setAdapter(new ListPopularAdapter(listMap));

				if (listMap.size() >= 3) {
					if (textHeroTitle != null) textHeroTitle.setBackgroundColor(Color.TRANSPARENT);
					if (textHeroDesc != null) textHeroDesc.setBackgroundColor(Color.TRANSPARENT);
					if (imgHeroBanner != null) imgHeroBanner.setBackgroundColor(Color.TRANSPARENT);

					GradientDrawable playBg = new GradientDrawable();
					playBg.setShape(GradientDrawable.RECTANGLE);
					playBg.setColor(Color.WHITE);
					playBg.setCornerRadius(100f);

					if (btnHeroPlay != null) {
						btnHeroPlay.setBackground(playBg);
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

				bindSectionLikePopularToday(recyclerPopular, listMap);
				bindSectionLikePopularToday(recyclerPopularDongflix, listMap);
				populateCategorizedSections();
				sortAndRenderPopularSections();
				renderJelajahiGrid();
				applyOutfitFontsWithoutBold();
			}

			@Override
			public void onErrorResponse(String p1, String p2) {
				if (isFinishing() || isDestroyed()) return;
				stopMainShimmer();
				hideRefreshIndicator();
			}
		};
	}

	private void initializeLogic() {
		_UI();
		setupIos26DraggableNavbar();
		_Nav();
		setupScrollResponsiveNavbar();
		applyOutfitFontsWithoutBold();

		float d = getResources().getDisplayMetrics().density;
		ArrayList<HashMap<String, Object>> dummy = buildEmptyDummyList(12);

		if (gridview1 != null) {
			gridview1.setAdapter(new Gridview1Adapter(dummy));
			gridview1.setOnTouchListener(new View.OnTouchListener() {
				@Override public boolean onTouch(View v, MotionEvent event) { return event.getAction() == MotionEvent.ACTION_MOVE; }
			});
			setGridViewHeightBasedOnChildren(gridview1, 3);
		}
		if (textHeroTitle != null) {
			textHeroTitle.setText("");
			textHeroTitle.setBackground(createMainShimmerDrawable(8 * d));
		}
		if (textHeroDesc != null) {
			textHeroDesc.setText("");
			textHeroDesc.setBackground(createMainShimmerDrawable(6 * d));
		}
		if (imgHeroBanner != null) {
			imgHeroBanner.setImageResource(android.R.color.transparent);
			imgHeroBanner.setBackground(createMainShimmerDrawable(0f));
		}
		if (btnHeroPlay != null) { btnHeroPlay.setVisibility(View.GONE); btnHeroPlay.setEnabled(false); }
		if (btnHeroFavorite != null) { btnHeroFavorite.setVisibility(View.GONE); btnHeroFavorite.setEnabled(false); }
		if (indicatorCapsule != null) indicatorCapsule.setVisibility(View.INVISIBLE);
		if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.GONE);

		RecyclerView[] standardList = {
			recyclerPopular, recyclerPopularDongflix, recyclerRomanceDonghua,
			recyclerCultivationDonghua, recyclerCompletedDonghua,
			recyclerComedyDonghua, recyclerMysteryDonghua, recyclerMovieDonghua
		};
		for (RecyclerView rv : standardList) bindSectionLikePopularToday(rv, dummy);

		if (recyclerJelajahiGrid != null) {
			recyclerJelajahiGrid.setAdapter(new JelajahiGridAdapter(dummy));
		}

		if (recyclerSearchList != null) {
			recyclerSearchList.setLayoutManager(new LinearLayoutManager(this));
			recyclerSearchList.setAdapter(new ListPopularAdapter(dummy));
		}

		startMainShimmer();
		dongflix_req.startRequestNetwork("GET", CATALOG_URL, "", _dongflix_req_request_listener);
	}

	private void setupRealTimeBlurView(View blurTarget, ViewGroup root, float radius) {
		if (!(blurTarget instanceof BlurView) || root == null) return;
		BlurView bv = (BlurView) blurTarget;
		bv.setupWith(root, new RenderScriptBlur(this))
			.setFrameClearDrawable(getWindow().getDecorView().getBackground())
			.setBlurRadius(radius);
		bv.setOverlayColor(Color.parseColor("#66141824"));
		bv.setOutlineProvider(new ViewOutlineProvider() {
			@Override public void getOutline(View view, Outline outline) { outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 1000f); }
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
			glass.setStroke((int) (1 * d), Color.parseColor("#33FFFFFF"));
			blurTarget.setBackground(glass);
		}
		if (containerTarget != null) {
			GradientDrawable border = new GradientDrawable();
			border.setColor(Color.TRANSPARENT);
			border.setCornerRadius(1000f);
			border.setStroke((int) (1 * d), Color.parseColor("#33FFFFFF"));
			containerTarget.setForeground(border);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) containerTarget.setElevation(18f);
		}
	}

	private void setupIos26DraggableNavbar() {
		if (navbarExpanded == null || tabBeranda == null || navbarExpanded.getChildCount() < 2) return;
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

			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) (72 * d), (int) (54 * d), Gravity.CENTER_VERTICAL | Gravity.START);
			lp.leftMargin = (int) (5 * d);
			liquidDragPill.setLayoutParams(lp);
			navbarExpanded.addView(liquidDragPill, 1);
		}
		tabsRow.post(new Runnable() {
			@Override public void run() { snapPillToTab(activeNavTabIndex, false); }
		});
	}

	private void snapPillToTab(int idx, boolean animate) {
		if (liquidDragPill == null || tabBeranda == null || tabJelajahi == null || tabPustaka == null || tabAkun == null) return;
		View target;
		if (idx == 1) target = tabJelajahi;
		else if (idx == 2) target = tabPustaka;
		else if (idx == 3) target = tabAkun;
		else target = tabBeranda;

		float d = getResources().getDisplayMetrics().density;

		int targetW = target.getWidth();
		if (targetW <= 0 && navbarExpanded != null && navbarExpanded.getWidth() > 0) {
			targetW = (navbarExpanded.getWidth() - (int) (10 * d)) / 4;
		}
		if (targetW <= 0) targetW = (int) (72 * d);

		ViewGroup.LayoutParams lp = liquidDragPill.getLayoutParams();
		if (lp.width != targetW) {
			lp.width = targetW;
			liquidDragPill.setLayoutParams(lp);
		}

		float tx = target.getLeft() > 0 ? Math.max(0f, target.getLeft() - (5 * d)) : idx * targetW;
		if (animate) {
			liquidDragPill.animate().translationX(tx).setInterpolator(new OvershootInterpolator(1.05f)).setDuration(220).start();
		} else {
			liquidDragPill.setTranslationX(tx);
		}
	}

	private void selectNavTabByIndex(int idx, boolean animatePill) {
		activeNavTabIndex = idx;
		snapPillToTab(idx, animatePill);

		View[] pages = {
			findViewById(R.id.main_scroll_view),
			findViewById(R.id.layout_jelajahi),
			findViewById(R.id.layout_favorite),
			findViewById(R.id.layout_profile),
			findViewById(R.id.layout_search)
		};
		for (View p : pages) if (p != null) p.setVisibility(View.GONE);

		int active = Color.WHITE, inactive = Color.parseColor("#94A3B8");
		if (imgHome != null) imgHome.setColorFilter(idx == 0 ? active : inactive);
		if (imgJelajahi != null) imgJelajahi.setColorFilter(idx == 1 ? active : inactive);
		if (imgFavorite != null) imgFavorite.setColorFilter(idx == 2 ? active : inactive);
		if (imgProfile != null) imgProfile.setColorFilter(idx == 3 ? active : inactive);
		if (imgSearch != null) imgSearch.setColorFilter(inactive);

		if (txtHome != null) txtHome.setTextColor(idx == 0 ? active : inactive);
		if (txtJelajahi != null) txtJelajahi.setTextColor(idx == 1 ? active : inactive);
		if (txtFavorite != null) txtFavorite.setTextColor(idx == 2 ? active : inactive);
		if (txtProfile != null) txtProfile.setTextColor(idx == 3 ? active : inactive);

		hideKeyboard();
		if (idx == 0) {
			if (pages[0] != null) pages[0].setVisibility(View.VISIBLE);
			setNavbarCompactState(false);
		} else if (idx == 1) {
			if (pages[1] != null) pages[1].setVisibility(View.VISIBLE);
			renderJelajahiGrid();
		} else if (idx == 2) {
			if (pages[2] != null) pages[2].setVisibility(View.VISIBLE);
			renderActivePustakaTab();
		} else if (idx == 3) {
			if (pages[3] != null) pages[3].setVisibility(View.VISIBLE);
			refreshAccountUi();
		}
	}

	public void _UI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window w = getWindow();
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
			w.setStatusBarColor(Color.TRANSPARENT);
		}

		int statusH = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusH = getResources().getDimensionPixelSize(resId);
		float d = getResources().getDisplayMetrics().density;

		View topHeader = findViewById(R.id.relativelayout69);
		if (topHeader != null) topHeader.setPadding((int) (16 * d), statusH + (int) (8 * d), (int) (16 * d), (int) (10 * d));

		applyHeaderActionIcons();
		if (imgHeroFavorite != null) imgHeroFavorite.setColorFilter(Color.WHITE);

		if (searchBarContainer != null) {
			if (searchBarContainer.getLayoutParams() instanceof LinearLayout.LayoutParams) {
				LinearLayout.LayoutParams sbLp = (LinearLayout.LayoutParams) searchBarContainer.getLayoutParams();
				sbLp.topMargin = statusH + (int) (12 * d);
				searchBarContainer.setLayoutParams(sbLp);
			}
			GradientDrawable sbBg = new GradientDrawable();
			sbBg.setColor(Color.parseColor("#12141A"));
			sbBg.setCornerRadius(100f);
			sbBg.setStroke((int) (1 * d), Color.parseColor("#26FFFFFF"));
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
			headerShadow.setBackground(new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#E6000000"), Color.TRANSPARENT}));
		}
		View heroShadow = findViewById(R.id.hero_shadow);
		if (heroShadow != null) {
			heroShadow.setBackground(new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.TRANSPARENT, Color.parseColor("#FF000000")}));
		}

		ViewGroup rootMain = findViewById(R.id.root_main_container);
		View navBlur = findViewById(R.id.navbar_blur_bg);
		View compactBlur = findViewById(R.id.compact_home_blur_bg);
		View searchBlur = findViewById(R.id.search_blur_bg);

		setupRealTimeBlurView(topActionPillBlurBg, rootMain, 20f);
		setupRealTimeBlurView(navBlur, rootMain, 20f);
		setupRealTimeBlurView(compactBlur, rootMain, 20f);
		setupRealTimeBlurView(searchBlur, rootMain, 20f);

		applyUniversalGlassStyle(topActionPillBlurBg, topActionPill);
		applyUniversalGlassStyle(navBlur, navbarExpanded);
		applyUniversalGlassStyle(compactBlur, btnCompactHome);
		applyUniversalGlassStyle(searchBlur, btnCircleSearch);
	}

	private void setupScrollResponsiveNavbar() {
		NestedScrollView mainScroll = findViewById(R.id.main_scroll_view);
		if (mainScroll != null) {
			mainScroll.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
				@Override
				public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldX, int oldY) {
					if (!DongflixPlayerManager.getInstance().isMiniPlayerActive) {
						resetNavbarToExpanded();
						return;
					}
					int dy = scrollY - oldY;
					if (scrollY <= 20 || dy < -12) setNavbarCompactState(false);
					else if (dy > 14 && scrollY > 80) setNavbarCompactState(true);
				}
			});
		}
	}

	public void resetNavbarToExpanded() { setNavbarCompactState(false); }

	private void setNavbarCompactState(boolean compact) {
		if (compact && !DongflixPlayerManager.getInstance().isMiniPlayerActive) compact = false;
		if (isNavbarCompact == compact) return;
		isNavbarCompact = compact;

		if (compact) {
			if (navbarExpanded != null) navbarExpanded.setVisibility(View.GONE);
			if (btnCompactHome != null) btnCompactHome.setVisibility(View.VISIBLE);
		} else {
			if (btnCompactHome != null) btnCompactHome.setVisibility(View.GONE);
			if (miniPlayerInlineSlot != null) miniPlayerInlineSlot.setVisibility(View.GONE);
			if (navbarExpanded != null) navbarExpanded.setVisibility(View.VISIBLE);
			snapPillToTab(activeNavTabIndex, false);
		}
		DongflixPlayerManager.getInstance().setCompactScrollMode(this, compact);
	}

	public void _Nav() {
		final View pageHome = findViewById(R.id.main_scroll_view);
		final View pageJelajahi = findViewById(R.id.layout_jelajahi);
		final View pageSearch = findViewById(R.id.layout_search);
		final View pageFavorite = findViewById(R.id.layout_favorite);
		final View pageProfile = findViewById(R.id.layout_profile);

		if (btnCompactHome != null) {
			btnCompactHome.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) { selectNavTabByIndex(0, true); }
			});
		}
		if (btnCircleSearch != null) {
			btnCircleSearch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (pageHome != null) pageHome.setVisibility(View.GONE);
					if (pageJelajahi != null) pageJelajahi.setVisibility(View.GONE);
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

	public void setGridViewHeightBasedOnChildren(final GridView gv, final int columns) {
		gv.post(new Runnable() {
			@Override
			public void run() {
				if (gv.getAdapter() == null || gv.getAdapter().getCount() == 0) return;
				int items = gv.getAdapter().getCount();
				int rows = (int) Math.ceil((double) items / columns);
				float d = getResources().getDisplayMetrics().density;
				int hGap = (int) (12 * d), vGap = (int) (16 * d), padBot = (int) (16 * d);
				int colW = (gv.getWidth() - gv.getPaddingLeft() - gv.getPaddingRight() - (hGap * (columns - 1))) / columns;

				View itemV = gv.getAdapter().getView(0, null, gv);
				itemV.measure(View.MeasureSpec.makeMeasureSpec(colW, View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

				ViewGroup.LayoutParams lp = gv.getLayoutParams();
				lp.height = (itemV.getMeasuredHeight() * rows) + (vGap * Math.max(0, rows - 1)) + padBot;
				gv.setLayoutParams(lp);
				gv.requestLayout();
			}
		});
	}

	public class JelajahiGridAdapter extends RecyclerView.Adapter<JelajahiGridAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;
		public JelajahiGridAdapter(ArrayList<HashMap<String, Object>> arr) { this.data = arr; }

		@NonNull @Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View v = getLayoutInflater().inflate(R.layout.custom_anime, parent, false);
			float d = parent.getResources().getDisplayMetrics().density;
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			int halfGap = (int) (4 * d);
			lp.setMargins(halfGap, 0, halfGap, (int) (14 * d));
			v.setLayoutParams(lp);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
			View v = holder.itemView;
			float d = v.getResources().getDisplayMetrics().density;

			TextView tvTitle = v.findViewById(R.id.text_title);
			TextView tvStatus = v.findViewById(R.id.text_status);
			TextView tvEp = v.findViewById(R.id.text_episode);
			ImageView imgCover = v.findViewById(R.id.img_cover);
			View view2 = v.findViewById(R.id.view2);

			final HashMap<String, Object> item = data.get(pos);
			if (item == null) return;

			if (item.containsKey("is_loading")) {
				if (tvTitle != null) { tvTitle.setText(""); tvTitle.setBackground(createMainShimmerDrawable(8 * d)); }
				if (tvStatus != null) { tvStatus.setText(""); tvStatus.setBackground(createMainShimmerDrawable(50f)); }
				if (tvEp != null) { tvEp.setText(""); tvEp.setBackground(createMainShimmerDrawable(6 * d)); }
				if (imgCover != null) { imgCover.setImageResource(android.R.color.transparent); imgCover.setBackground(createMainShimmerDrawable(16 * d)); }
				if (view2 != null) view2.setVisibility(View.GONE);
				v.setOnClickListener(null);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (tvTitle != null) {
					tvTitle.setBackgroundColor(Color.TRANSPARENT);
					tvTitle.setIncludeFontPadding(false);
					tvTitle.setText(cleanText(safeStr(item, "title", "")));
				}
				if (tvEp != null) { tvEp.setVisibility(View.VISIBLE); tvEp.setBackgroundColor(Color.TRANSPARENT); tvEp.setText(safeStr(item, "episode", "")); }
				if (view2 != null) {
					view2.setVisibility(View.VISIBLE);
					view2.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
						new int[]{Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT}));
				}
				if (tvStatus != null) {
					GradientDrawable badge = new GradientDrawable();
					badge.setCornerRadius(50f);
					badge.setColor(0xFF3B82F6);
					tvStatus.setBackground(badge);
					tvStatus.setText(safeStr(item, "status", ""));
				}
				String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
				if (imgCover != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(MainActivity.this).load(Uri.parse(cov)).into(imgCover);
				}
				v.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View view) { openWatchBySlug(safeStr(item, "slug", "")); }
				});
			}
		}

		@Override public int getItemCount() { return data.size(); }
		class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(@NonNull View v) { super(v); } }
	}

	public class ContinueWatchingAdapter extends RecyclerView.Adapter<ContinueWatchingAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;
		public ContinueWatchingAdapter(ArrayList<HashMap<String, Object>> arr) { this.data = arr; }

		@NonNull @Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			float d = parent.getResources().getDisplayMetrics().density;
			FrameLayout card = new FrameLayout(parent.getContext());
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams((int) (196 * d), (int) (104 * d));
			lp.rightMargin = (int) (10 * d);
			card.setLayoutParams(lp);

			GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
				new int[]{Color.parseColor("#071830"), Color.parseColor("#0C2847")});
			bg.setCornerRadius(14 * d);
			card.setBackground(bg);
			card.setClipToOutline(true);

			ImageView thumb = new ImageView(parent.getContext());
			thumb.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
			thumb.setAlpha(0.75f);
			thumb.setTag("thumb");
			card.addView(thumb);

			View scrim = new View(parent.getContext());
			scrim.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
				new int[]{Color.parseColor("#E6030A14"), Color.parseColor("#55030A14"), Color.TRANSPARENT}));
			card.addView(scrim);

			TextView tvRemaining = new TextView(parent.getContext());
			FrameLayout.LayoutParams remLp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END
			);
			remLp.setMargins(0, (int) (7 * d), (int) (7 * d), 0);
			tvRemaining.setLayoutParams(remLp);
			tvRemaining.setPadding((int) (7 * d), (int) (2 * d), (int) (7 * d), (int) (2 * d));
			tvRemaining.setTextColor(Color.WHITE);
			tvRemaining.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
			tvRemaining.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvRemaining.setMaxLines(1);
			GradientDrawable remBg = new GradientDrawable();
			remBg.setColor(Color.parseColor("#B30A0E17"));
			remBg.setCornerRadius(100f);
			remBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#33FFFFFF"));
			tvRemaining.setBackground(remBg);
			tvRemaining.setTag("remaining");
			card.addView(tvRemaining);

			LinearLayout col = new LinearLayout(parent.getContext());
			col.setOrientation(LinearLayout.VERTICAL);
			col.setPadding((int) (11 * d), (int) (8 * d), (int) (11 * d), (int) (8 * d));
			col.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

			TextView tvTitle = new TextView(parent.getContext());
			tvTitle.setTextColor(Color.WHITE);
			tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
			tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvTitle.setSingleLine(true);
			tvTitle.setMaxLines(1);
			tvTitle.setEllipsize(TextUtils.TruncateAt.END);
			tvTitle.setTag("title");

			TextView tvEp = new TextView(parent.getContext());
			tvEp.setTextColor(Color.parseColor("#CBD5E1"));
			tvEp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
			tvEp.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvEp.setMaxLines(1);
			tvEp.setEllipsize(TextUtils.TruncateAt.END);
			tvEp.setTag("ep");

			col.addView(tvTitle);
			col.addView(tvEp);
			card.addView(col);

			FrameLayout barTrack = new FrameLayout(parent.getContext());
			barTrack.setLayoutParams(new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, Math.max(2, (int) (2 * d)), Gravity.BOTTOM | Gravity.START
			));
			barTrack.setBackgroundColor(Color.parseColor("#26FFFFFF"));

			View barFill = new View(parent.getContext());
			barFill.setLayoutParams(new FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT));
			barFill.setBackgroundColor(Color.parseColor("#38BDF8"));
			barFill.setTag("bar_fill");

			barTrack.addView(barFill);
			card.addView(barTrack);

			return new ViewHolder(card);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
			final HashMap<String, Object> item = data.get(pos);
			if (item == null) return;
			ImageView thumb = holder.itemView.findViewWithTag("thumb");
			TextView tvRemaining = holder.itemView.findViewWithTag("remaining");
			TextView tvTitle = holder.itemView.findViewWithTag("title");
			TextView tvEp = holder.itemView.findViewWithTag("ep");
			final View barFill = holder.itemView.findViewWithTag("bar_fill");

			String epRaw = safeStr(item, "episode", "1").trim();
			String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
			if (cov.isEmpty()) cov = normalizeCoverUrl(safeStr(item, "poster", ""));

			if (tvRemaining != null) tvRemaining.setText(formatRemainingMinutesText(item));
			if (tvTitle != null) tvTitle.setText(cleanText(safeStr(item, "title", "Donghua")));
			if (tvEp != null) tvEp.setText(epRaw.toUpperCase(Locale.US).startsWith("EP") ? epRaw.toUpperCase(Locale.US) : "EP " + epRaw);
			if (thumb != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
				Glide.with(MainActivity.this).load(Uri.parse(cov)).into(thumb);
			}

			final long posMs = parseLongSafe(item.get("positionMs"));
			final long durMs = parseLongSafe(item.get("durationMs"));
			if (barFill != null && barFill.getParent() instanceof View) {
				final View parentTrack = (View) barFill.getParent();
				parentTrack.post(new Runnable() {
					@Override
					public void run() {
						int totalW = parentTrack.getWidth();
						if (totalW <= 0) return;
						float d = barFill.getResources().getDisplayMetrics().density;
						int fillW;
						if (durMs > 0L && posMs > 0L) {
							float ratio = Math.max(0f, Math.min(1f, (float) posMs / (float) durMs));
							fillW = Math.max(Math.round(6 * d), Math.round(totalW * ratio));
						} else if (durMs > 0L && posMs <= 0L) {
							fillW = 0;
						} else {
							fillW = totalW / 2;
						}
						FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(fillW, ViewGroup.LayoutParams.MATCH_PARENT);
						lp.leftMargin = 0;
						barFill.setLayoutParams(lp);
					}
				});
			}

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) { openWatchBySlug(safeStr(item, "slug", "")); }
			});
		}

		@Override public int getItemCount() { return data.size(); }
		class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(@NonNull View v) { super(v); } }
	}

	public class HorizontalPosterAdapter extends RecyclerView.Adapter<HorizontalPosterAdapter.ViewHolder> {
		private final ArrayList<HashMap<String, Object>> data;
		private final int cardWidthDp, cardHeightDp;

		public HorizontalPosterAdapter(ArrayList<HashMap<String, Object>> arr, int wDp, int hDp) {
			this.data = arr;
			this.cardWidthDp = wDp;
			this.cardHeightDp = hDp;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			float d = parent.getResources().getDisplayMetrics().density;
			int wPx = (int) (cardWidthDp * d), hPx = (int) (cardHeightDp * d);

			LinearLayout root = new LinearLayout(parent.getContext());
			root.setOrientation(LinearLayout.VERTICAL);
			RecyclerView.LayoutParams rlp = new RecyclerView.LayoutParams(wPx, ViewGroup.LayoutParams.WRAP_CONTENT);
			rlp.rightMargin = (int) (10 * d);
			root.setLayoutParams(rlp);

			CardView card = new CardView(parent.getContext());
			card.setLayoutParams(new LinearLayout.LayoutParams(wPx, hPx));
			card.setRadius(16 * d);
			card.setCardElevation(0f);
			card.setCardBackgroundColor(Color.parseColor("#12141A"));

			FrameLayout overlay = new FrameLayout(parent.getContext());
			overlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

			ImageView imgCover = new ImageView(parent.getContext());
			imgCover.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			imgCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imgCover.setTag("img_cover");
			overlay.addView(imgCover);

			View scrim = new View(parent.getContext());
			scrim.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (60 * d), Gravity.BOTTOM));
			scrim.setTag("bottom_scrim");
			overlay.addView(scrim);

			TextView tvStatus = new TextView(parent.getContext());
			FrameLayout.LayoutParams sLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
			sLp.setMargins((int) (8 * d), (int) (8 * d), (int) (8 * d), 0);
			tvStatus.setLayoutParams(sLp);
			tvStatus.setPadding((int) (8 * d), (int) (3 * d), (int) (8 * d), (int) (3 * d));
			tvStatus.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
			tvStatus.setTextColor(Color.WHITE);
			tvStatus.setMaxLines(1);
			tvStatus.setTag("text_status");
			overlay.addView(tvStatus);

			TextView tvEp = new TextView(parent.getContext());
			FrameLayout.LayoutParams eLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
			eLp.setMargins((int) (9 * d), 0, (int) (9 * d), (int) (8 * d));
			tvEp.setLayoutParams(eLp);
			tvEp.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvEp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
			tvEp.setTextColor(Color.WHITE);
			tvEp.setMaxLines(1);
			tvEp.setEllipsize(TextUtils.TruncateAt.END);
			tvEp.setTag("text_episode");
			overlay.addView(tvEp);

			card.addView(overlay);
			root.addView(card);

			TextView tvTitle = new TextView(parent.getContext());
			LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			);
			tLp.topMargin = (int) (7 * d);
			tvTitle.setLayoutParams(tLp);
			tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
			tvTitle.setTextColor(Color.WHITE);
			tvTitle.setIncludeFontPadding(false);
			tvTitle.setLineSpacing(2 * d, 1.05f);
			tvTitle.setMinLines(2);
			tvTitle.setMaxLines(2);
			tvTitle.setEllipsize(TextUtils.TruncateAt.END);
			tvTitle.setTag("text_title");
			root.addView(tvTitle);

			return new ViewHolder(root);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, final int pos) {
			View v = holder.itemView;
			float d = v.getResources().getDisplayMetrics().density;
			if (v.getLayoutParams() instanceof RecyclerView.LayoutParams) {
				RecyclerView.LayoutParams rlp = (RecyclerView.LayoutParams) v.getLayoutParams();
				rlp.rightMargin = (pos == data.size() - 1) ? 0 : (int) (10 * d);
				v.setLayoutParams(rlp);
			}

			ImageView imgCover = v.findViewWithTag("img_cover");
			View scrim = v.findViewWithTag("bottom_scrim");
			TextView tvStatus = v.findViewWithTag("text_status");
			TextView tvEp = v.findViewWithTag("text_episode");
			TextView tvTitle = v.findViewWithTag("text_title");

			final HashMap<String, Object> item = data.get(pos);
			if (item == null) return;

			if (item.containsKey("is_loading")) {
				if (imgCover != null) { imgCover.setImageResource(android.R.color.transparent); imgCover.setBackground(createMainShimmerDrawable(16 * d)); }
				if (scrim != null) scrim.setVisibility(View.GONE);
				if (tvStatus != null) { tvStatus.setText(""); tvStatus.setVisibility(View.INVISIBLE); }
				if (tvEp != null) { tvEp.setText(""); tvEp.setVisibility(View.INVISIBLE); }
				if (tvTitle != null) {
					tvTitle.setText("");
					tvTitle.setMinHeight((int) (36 * d));
					tvTitle.setBackground(createMainShimmerDrawable(8 * d));
				}
				v.setOnClickListener(null);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (scrim != null) {
					scrim.setVisibility(View.VISIBLE);
					scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
						new int[]{Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT}));
				}
				if (tvStatus != null) {
					tvStatus.setVisibility(View.VISIBLE);
					GradientDrawable badge = new GradientDrawable();
					badge.setCornerRadius(100f);
					badge.setColor(Color.parseColor("#3B82F6"));
					tvStatus.setBackground(badge);
					tvStatus.setText(safeStr(item, "status", ""));
				}
				if (tvEp != null) { tvEp.setVisibility(View.VISIBLE); tvEp.setText(safeStr(item, "episode", "")); }
				if (tvTitle != null) {
					tvTitle.setMinHeight(0);
					tvTitle.setBackgroundColor(Color.TRANSPARENT);
					tvTitle.setText(cleanText(safeStr(item, "title", "")));
				}

				String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
				if (imgCover != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(MainActivity.this).load(Uri.parse(cov)).into(imgCover);
				}
				v.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View view) { openWatchBySlug(safeStr(item, "slug", "")); }
				});
			}
		}

		@Override public int getItemCount() { return data.size(); }
		public class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(View v) { super(v); } }
	}

	public class PustakaListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private static final int TYPE_HEADER = 0;
		private static final int TYPE_ITEM = 1;

		private final ArrayList<HashMap<String, Object>> data;
		private final int mode;
		private final String sectionLabel;

		public PustakaListAdapter(ArrayList<HashMap<String, Object>> arr, int mode, String sectionLabel) {
			this.data = arr;
			this.mode = mode;
			this.sectionLabel = sectionLabel;
		}

		@Override
		public int getItemViewType(int position) {
			return position == 0 ? TYPE_HEADER : TYPE_ITEM;
		}

		@NonNull @Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context ctx = parent.getContext();
			float d = ctx.getResources().getDisplayMetrics().density;

			if (viewType == TYPE_HEADER) {
				TextView tvHeader = new TextView(ctx);
				tvHeader.setText(sectionLabel);
				tvHeader.setTextColor(Color.parseColor("#94A3B8"));
				tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
				tvHeader.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
				RecyclerView.LayoutParams hLp = new RecyclerView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				hLp.topMargin = (int) (4 * d);
				hLp.bottomMargin = (int) (10 * d);
				tvHeader.setLayoutParams(hLp);
				return new HeaderViewHolder(tvHeader);
			}

			LinearLayout card = new LinearLayout(ctx);
			card.setOrientation(LinearLayout.HORIZONTAL);
			card.setGravity(Gravity.CENTER_VERTICAL);
			card.setPadding((int) (10 * d), (int) (10 * d), (int) (12 * d), (int) (10 * d));
			RecyclerView.LayoutParams rlp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			rlp.bottomMargin = (int) (10 * d);
			card.setLayoutParams(rlp);

			GradientDrawable cardBg = new GradientDrawable();
			cardBg.setColor(Color.parseColor("#12141A"));
			cardBg.setCornerRadius(16 * d);
			cardBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1AFFFFFF"));
			card.setBackground(cardBg);

			CardView thumbCard = new CardView(ctx);
			LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams((int) (64 * d), (int) (86 * d));
			tLp.rightMargin = (int) (12 * d);
			thumbCard.setLayoutParams(tLp);
			thumbCard.setRadius(12 * d);
			thumbCard.setCardElevation(0f);
			thumbCard.setCardBackgroundColor(Color.parseColor("#181C26"));

			ImageView thumb = new ImageView(ctx);
			thumb.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
			thumb.setTag("thumb");
			thumbCard.addView(thumb);

			LinearLayout info = new LinearLayout(ctx);
			info.setOrientation(LinearLayout.VERTICAL);
			info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

			TextView tvTitle = new TextView(ctx);
			tvTitle.setTextColor(Color.WHITE);
			tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
			tvTitle.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvTitle.setMaxLines(2);
			tvTitle.setEllipsize(TextUtils.TruncateAt.END);
			tvTitle.setTag("title");

			TextView tvBadge = new TextView(ctx);
			tvBadge.setTextColor(Color.parseColor("#38BDF8"));
			tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
			tvBadge.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			tvBadge.setPadding((int) (8 * d), (int) (2 * d), (int) (8 * d), (int) (2 * d));
			LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			bLp.topMargin = (int) (6 * d);
			tvBadge.setLayoutParams(bLp);
			GradientDrawable bBg = new GradientDrawable();
			bBg.setColor(Color.parseColor("#0C2438"));
			bBg.setCornerRadius(100f);
			tvBadge.setBackground(bBg);
			tvBadge.setTag("ep");

			TextView tvSub = new TextView(ctx);
			tvSub.setTextColor(Color.parseColor("#94A3B8"));
			tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
			tvSub.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			sLp.topMargin = (int) (4 * d);
			tvSub.setLayoutParams(sLp);
			tvSub.setTag("sub");

			info.addView(tvTitle);
			info.addView(tvBadge);
			info.addView(tvSub);

			TextView btnRemove = new TextView(ctx);
			btnRemove.setText("✕");
			btnRemove.setTextColor(Color.parseColor("#94A3B8"));
			btnRemove.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
			btnRemove.setTypeface(getOutfitTypeface(), Typeface.NORMAL);
			btnRemove.setGravity(Gravity.CENTER);
			LinearLayout.LayoutParams rmLp = new LinearLayout.LayoutParams((int) (30 * d), (int) (30 * d));
			rmLp.leftMargin = (int) (8 * d);
			btnRemove.setLayoutParams(rmLp);
			GradientDrawable rmBg = new GradientDrawable();
			rmBg.setShape(GradientDrawable.OVAL);
			rmBg.setColor(Color.parseColor("#181C26"));
			btnRemove.setBackground(rmBg);
			btnRemove.setTag("btn_remove");

			card.addView(thumbCard);
			card.addView(info);
			card.addView(btnRemove);
			return new ItemViewHolder(card);
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
			if (getItemViewType(pos) == TYPE_HEADER) {
				if (holder.itemView instanceof TextView) {
					((TextView) holder.itemView).setText(sectionLabel);
				}
				return;
			}

			final HashMap<String, Object> item = data.get(pos - 1);
			if (item == null) return;
			ImageView img = holder.itemView.findViewWithTag("thumb");
			TextView tvTitle = holder.itemView.findViewWithTag("title");
			TextView tvEp = holder.itemView.findViewWithTag("ep");
			TextView tvSub = holder.itemView.findViewWithTag("sub");
			TextView btnRemove = holder.itemView.findViewWithTag("btn_remove");

			final String title = cleanText(safeStr(item, "title", mode == 2 ? "Video Offline" : "Donghua"));
			if (tvTitle != null) tvTitle.setText(title);

			if (mode == 0) {
				String epRaw = safeStr(item, "episode", "1").trim();
				if (tvEp != null) tvEp.setText(epRaw.toUpperCase(Locale.US).startsWith("EP") ? epRaw.toUpperCase(Locale.US) : "EP " + epRaw);
				if (tvSub != null) tvSub.setText(formatRemainingMinutesText(item));
			} else if (mode == 1) {
				String ep = safeStr(item, "episode", "");
				if (tvEp != null) tvEp.setText(ep.isEmpty() ? "Favorit" : ep);
				if (tvSub != null) tvSub.setText(safeStr(item, "status", "Tersimpan di Daftarku"));
			} else {
				if (tvEp != null) tvEp.setText("OFFLINE • " + safeStr(item, "size_mb", ""));
				if (tvSub != null) tvSub.setText("Ketuk untuk memutar video");
			}

			if (btnRemove != null) {
				btnRemove.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						removePustakaItem(mode, item);
					}
				});
			}

			if (mode == 2) {
				if (img != null) img.setImageResource(R.drawable.default_image);
				final String path = safeStr(item, "file_path", "");
				holder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						openDownloadedVideoSafely(path, title);
					}
				});
			} else {
				String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
				if (cov.isEmpty()) cov = normalizeCoverUrl(safeStr(item, "poster", ""));
				if (img != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(MainActivity.this).load(Uri.parse(cov)).into(img);
				}
				holder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View v) { openWatchBySlug(safeStr(item, "slug", "")); }
				});
			}
		}

		@Override public int getItemCount() { return data.isEmpty() ? 0 : data.size() + 1; }
		class HeaderViewHolder extends RecyclerView.ViewHolder { HeaderViewHolder(@NonNull View v) { super(v); } }
		class ItemViewHolder extends RecyclerView.ViewHolder { ItemViewHolder(@NonNull View v) { super(v); } }
	}

	public class Gridview1Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> data;
		public Gridview1Adapter(ArrayList<HashMap<String, Object>> arr) { this.data = arr; }

		@Override public int getCount() { return data.size(); }
		@Override public HashMap<String, Object> getItem(int idx) { return data.get(idx); }
		@Override public long getItemId(int idx) { return idx; }

		@Override
		public View getView(final int pos, View convertView, ViewGroup parent) {
			View v = getLayoutInflater().inflate(R.layout.custom_anime, parent, false);
			float d = v.getResources().getDisplayMetrics().density;

			TextView tvTitle = v.findViewById(R.id.text_title);
			TextView tvStatus = v.findViewById(R.id.text_status);
			TextView tvEp = v.findViewById(R.id.text_episode);
			ImageView imgCover = v.findViewById(R.id.img_cover);
			View view2 = v.findViewById(R.id.view2);

			HashMap<String, Object> item = data.get(pos);
			if (item != null && item.containsKey("is_loading")) {
				if (tvTitle != null) { tvTitle.setText(""); tvTitle.setBackground(createMainShimmerDrawable(8 * d)); }
				if (tvStatus != null) { tvStatus.setText(""); tvStatus.setBackground(createMainShimmerDrawable(50f)); }
				if (tvEp != null) { tvEp.setText(""); tvEp.setBackground(createMainShimmerDrawable(6 * d)); }
				if (imgCover != null) { imgCover.setImageResource(android.R.color.transparent); imgCover.setBackground(createMainShimmerDrawable(16 * d)); }
				if (view2 != null) view2.setVisibility(View.GONE);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (tvTitle != null) {
					tvTitle.setBackgroundColor(Color.TRANSPARENT);
					tvTitle.setIncludeFontPadding(false);
					tvTitle.setText(cleanText(safeStr(item, "title", "")));
				}
				if (tvEp != null) { tvEp.setVisibility(View.VISIBLE); tvEp.setBackgroundColor(Color.TRANSPARENT); tvEp.setText(safeStr(item, "episode", "")); }
				if (view2 != null) {
					view2.setVisibility(View.VISIBLE);
					view2.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
						new int[]{Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT}));
				}
				if (tvStatus != null) {
					GradientDrawable badge = new GradientDrawable();
					badge.setCornerRadius(50f);
					badge.setColor(0xFF3B82F6);
					tvStatus.setBackground(badge);
					tvStatus.setText(safeStr(item, "status", ""));
				}
				String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
				if (imgCover != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(MainActivity.this).load(Uri.parse(cov)).into(imgCover);
				}
			}
			return v;
		}
	}

	public class ListPopularAdapter extends RecyclerView.Adapter<ListPopularAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> data;
		public ListPopularAdapter(ArrayList<HashMap<String, Object>> arr) { this.data = arr; }

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = getLayoutInflater().inflate(R.layout.item_popular_donghua, parent, false);
			v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, final int pos) {
			View v = holder.itemView;
			float d = v.getResources().getDisplayMetrics().density;

			TextView tvTitle = v.findViewById(getResources().getIdentifier("text_popular_title", "id", getPackageName()));
			TextView tvDesc = v.findViewById(getResources().getIdentifier("text_popular_desc", "id", getPackageName()));
			TextView tvGenre = v.findViewById(getResources().getIdentifier("text_popular_genre", "id", getPackageName()));
			TextView tvStatus = v.findViewById(getResources().getIdentifier("text_popular_status", "id", getPackageName()));
			ImageView imgCover = v.findViewById(getResources().getIdentifier("img_popular_cover", "id", getPackageName()));
			View linear35 = v.findViewById(getResources().getIdentifier("linear35", "id", getPackageName()));

			final HashMap<String, Object> item = data.get(pos);
			if (item == null) return;

			if (item.containsKey("is_loading")) {
				if (tvTitle != null) { tvTitle.setText(""); tvTitle.setBackground(createMainShimmerDrawable(100f)); }
				if (tvDesc != null) { tvDesc.setText(""); tvDesc.setBackground(createMainShimmerDrawable(6 * d)); }
				if (tvGenre != null) { tvGenre.setText(""); tvGenre.setBackground(createMainShimmerDrawable(6 * d)); }
				if (tvStatus != null) { tvStatus.setText("                    "); tvStatus.setBackground(createMainShimmerDrawable(100f)); }
				if (imgCover != null) { imgCover.setImageResource(android.R.color.transparent); imgCover.setBackground(createMainShimmerDrawable(16 * d)); }
			} else {
				if (tvTitle != null) { tvTitle.setBackgroundColor(Color.TRANSPARENT); tvTitle.setText(cleanText(safeStr(item, "title", ""))); }
				if (tvDesc != null) { tvDesc.setBackgroundColor(Color.TRANSPARENT); tvDesc.setText(cleanText(safeStr(item, "synopsis", ""))); }
				if (tvGenre != null) { tvGenre.setBackgroundColor(Color.TRANSPARENT); tvGenre.setText(safeStr(item, "genres", "").replace("[", "").replace("]", "")); }
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);

				if (tvStatus != null) {
					GradientDrawable badge = new GradientDrawable();
					badge.setCornerRadius(50f);
					badge.setColor(0xFF111111);
					tvStatus.setBackground(badge);
					tvStatus.setText(safeStr(item, "episode", "") + " / " + safeStr(item, "status", ""));
				}
				String cov = normalizeCoverUrl(safeStr(item, "cover", ""));
				if (imgCover != null && !cov.isEmpty() && !isFinishing() && !isDestroyed()) {
					Glide.with(MainActivity.this).load(Uri.parse(cov)).into(imgCover);
				}
				View clickTarget = linear35 != null ? linear35 : v;
				clickTarget.setOnClickListener(new View.OnClickListener() {
					@Override public void onClick(View view) { openWatchBySlug(safeStr(item, "slug", "")); }
				});
			}
		}

		@Override public int getItemCount() { return data.size(); }
		public class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(View v) { super(v); } }
	}
}
