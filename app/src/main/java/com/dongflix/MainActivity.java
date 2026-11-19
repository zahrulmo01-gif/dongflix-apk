package com.dongflix;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.*;
import androidx.core.ktx.*;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ui.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";

	private String cover = "";

	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> fullCatalogList = new ArrayList<>();
	private final Map<String, Long> viewsTotalMap = new HashMap<>();
	private final Map<String, Long> viewsDailyMap = new HashMap<>();

	private RequestNetwork dongflix_req;
	private RequestNetwork.RequestListener _dongflix_req_request_listener;
	private Intent i = new Intent();

	private GridView gridview1;
	private RecyclerView recyclerPopular, listPopular, recyclerSearchList;
	private TextView textHeroTitle, textHeroDesc, textSeeAll, btnClearSearch, textSearchSectionLabel;
	private ImageView imgHeroBanner, imgPlayIcon, imgHeroFavorite, imgTopSearch, imgTopChat, imgUserAvatar;
	private LinearLayout btnHeroPlay, indicatorCapsule, searchBarContainer;
	private FrameLayout btnHeroFavorite, btnTopSearch, btnTopChat;
	private View dot0, dot1, dot2;
	private EditText inputSearch;

	private FrameLayout navbarExpanded, btnCompactHome, miniPlayerInlineSlot, btnCircleSearch;
	private LinearLayout tabBeranda, tabPustaka, tabAkun;
	private ImageView imgHome, imgFavorite, imgProfile, imgSearch;
	private TextView txtHome, txtFavorite, txtProfile;
	private boolean isNavbarCompact = false;

	private ValueAnimator mainShimmerAnimator;
	private float mainShimmerPhase = 0f;
	private boolean isMainLoading = true;

	private SharedPreferences favPrefs;
	private HashMap<String, Object> currentHeroItem;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);

		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
			loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(loginIntent);
			finish();
			return;
		}

		setContentView(R.layout.activity_main);

		DongflixPlayerManager.getInstance().initWithApplication(getApplication());
		favPrefs = getSharedPreferences("dongflix_favorites", Context.MODE_PRIVATE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 201);
			}
		}

		initialize(_savedInstanceState);
		initializeLogic();
		loadLoggedInUserProfile(currentUser);
	}

	private void loadLoggedInUserProfile(FirebaseUser currentUser) {
		if (currentUser == null || currentUser.getEmail() == null) return;

		String email = currentUser.getEmail().trim().toLowerCase();
		String fallbackName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;

		TextView tvUserHeader = findViewById(R.id.text_username);
		if (tvUserHeader != null) tvUserHeader.setText(fallbackName);

		TextView tvProfileName = findViewById(R.id.textview65);
		if (tvProfileName != null) tvProfileName.setText(fallbackName);

		String userKey = email.replace(".", ",");
		FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(userKey)
			.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot snapshot) {
					if (!snapshot.exists()) return;

					Object nameObj = snapshot.child("name").getValue();
					if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
						String nameStr = nameObj.toString().trim();
						if (tvUserHeader != null) tvUserHeader.setText(nameStr);
						if (tvProfileName != null) tvProfileName.setText(nameStr);
					}

					Object photoObj = snapshot.child("photoURL").getValue();
					if (photoObj != null && !photoObj.toString().trim().isEmpty() && imgUserAvatar != null) {
						String photoUrl = photoObj.toString().trim();
						if (photoUrl.startsWith("data:image")) {
							try {
								String base64Part = photoUrl.substring(photoUrl.indexOf(",") + 1);
								byte[] decoded = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT);
								Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
								if (bmp != null) imgUserAvatar.setImageBitmap(bmp);
							} catch (Exception ignored) {}
						} else {
							Glide.with(getApplicationContext())
								.load(Uri.parse(photoUrl))
								.placeholder(R.drawable.ic_profile)
								.error(R.drawable.ic_profile)
								.into(imgUserAvatar);
						}
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {}
			});
	}

	private String getTodayDateString() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
	}

	private long parseViewNumber(Object val) {
		if (val == null) return 0L;
		if (val instanceof Number) return ((Number) val).longValue();
		try {
			return Long.parseLong(val.toString().trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private void sortAndRenderPopularSections() {
		if (fullCatalogList == null || fullCatalogList.isEmpty()) return;

		ArrayList<HashMap<String, Object>> todaySorted = new ArrayList<>(fullCatalogList);
		Collections.sort(todaySorted, new Comparator<HashMap<String, Object>>() {
			@Override
			public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
				String slugA = a.get("slug") != null ? a.get("slug").toString() : "";
				String slugB = b.get("slug") != null ? b.get("slug").toString() : "";
				long dailyA = viewsDailyMap.containsKey(slugA) ? viewsDailyMap.get(slugA) : 0L;
				long dailyB = viewsDailyMap.containsKey(slugB) ? viewsDailyMap.get(slugB) : 0L;
				if (dailyB != dailyA) {
					return Long.compare(dailyB, dailyA);
				}
				long totalA = viewsTotalMap.containsKey(slugA) ? viewsTotalMap.get(slugA) : 0L;
				long totalB = viewsTotalMap.containsKey(slugB) ? viewsTotalMap.get(slugB) : 0L;
				return Long.compare(totalB, totalA);
			}
		});

		ArrayList<HashMap<String, Object>> totalSorted = new ArrayList<>(fullCatalogList);
		Collections.sort(totalSorted, new Comparator<HashMap<String, Object>>() {
			@Override
			public int compare(HashMap<String, Object> a, HashMap<String, Object> b) {
				String slugA = a.get("slug") != null ? a.get("slug").toString() : "";
				String slugB = b.get("slug") != null ? b.get("slug").toString() : "";
				long totalA = viewsTotalMap.containsKey(slugA) ? viewsTotalMap.get(slugA) : 0L;
				long totalB = viewsTotalMap.containsKey(slugB) ? viewsTotalMap.get(slugB) : 0L;
				return Long.compare(totalB, totalA);
			}
		});

		ArrayList<HashMap<String, Object>> todaySlice = new ArrayList<>(
			todaySorted.subList(0, Math.min(8, todaySorted.size()))
		);
		ArrayList<HashMap<String, Object>> allPopularSlice = new ArrayList<>(
			totalSorted.subList(0, Math.min(10, totalSorted.size()))
		);

		if (recyclerPopular != null) {
			recyclerPopular.setAdapter(new RecyclerPopularAdapter(todaySlice));
		}
		if (listPopular != null) {
			listPopular.setAdapter(new ListPopularAdapter(allPopularSlice));
		}
		if (recyclerSearchList != null && (inputSearch == null || inputSearch.getText().toString().trim().isEmpty())) {
			recyclerSearchList.setAdapter(new ListPopularAdapter(allPopularSlice));
		}
	}

	private void syncFirebaseViewsAndSort() {
		String todayStr = getTodayDateString();
		DatabaseReference rootDb = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference();

		rootDb.child("views_total").addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				viewsTotalMap.clear();
				if (snapshot.exists()) {
					for (DataSnapshot child : snapshot.getChildren()) {
						if (child.getKey() != null) {
							viewsTotalMap.put(child.getKey(), parseViewNumber(child.getValue()));
						}
					}
				}
				sortAndRenderPopularSections();
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		});

		rootDb.child("views_daily").child(todayStr).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				viewsDailyMap.clear();
				if (snapshot.exists()) {
					for (DataSnapshot child : snapshot.getChildren()) {
						if (child.getKey() != null) {
							viewsDailyMap.put(child.getKey(), parseViewNumber(child.getValue()));
						}
					}
				}
				sortAndRenderPopularSections();
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		});
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
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							tv.setLetterSpacing(-0.01f);
						}
					} else if (style == Typeface.BOLD_ITALIC) {
						tv.setTypeface(fontBold, Typeface.ITALIC);
					} else if (style == Typeface.ITALIC) {
						tv.setTypeface(fontReg, Typeface.ITALIC);
					} else if (tv.getId() == R.id.text_hero_desc) {
						tv.setTypeface(fontReg, Typeface.NORMAL);
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

	private boolean isFavoriteSlug(String slug) {
		if (favPrefs == null || slug == null || slug.isEmpty()) return false;
		return favPrefs.contains("fav_" + slug);
	}

	private void toggleFavoriteItem(HashMap<String, Object> item) {
		if (favPrefs == null || item == null) return;
		String slug = item.get("slug") != null ? item.get("slug").toString() : "";
		String title = item.get("title") != null ? item.get("title").toString() : "Donghua";
		if (slug.isEmpty()) return;

		if (isFavoriteSlug(slug)) {
			favPrefs.edit().remove("fav_" + slug).apply();
			Toast.makeText(this, "Dihapus dari Favorit", Toast.LENGTH_SHORT).show();
		} else {
			favPrefs.edit().putString("fav_" + slug, new Gson().toJson(item)).apply();
			Toast.makeText(this, "Disimpan ke Favorit: " + title, Toast.LENGTH_SHORT).show();
		}
		updateHeroFavoriteIcon(slug);
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
		Intent intent = new Intent(MainActivity.this, WatchActivity.class);
		intent.putExtra("slug", slug.trim());
		startActivity(intent);
		overridePendingTransition(R.anim.slide_up_enter, R.anim.no_anim);
	}

	private void showKeyboardOnSearch() {
		if (inputSearch == null) return;
		inputSearch.requestFocus();
		inputSearch.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null) {
					imm.showSoftInput(inputSearch, InputMethodManager.SHOW_IMPLICIT);
				}
			}
		}, 120);
	}

	private void hideKeyboard() {
		if (inputSearch == null) return;
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
		}
	}

	private Drawable createMainShimmerDrawable(final float cornerRadiusPx) {
		return new Drawable() {
			private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Matrix shaderMatrix = new Matrix();
			private LinearGradient gradient;
			private int lastWidth = -1;

			{
				basePaint.setColor(Color.parseColor("#12141A"));
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

				float translateX = -w + (2f * w * mainShimmerPhase);
				shaderMatrix.setTranslate(translateX, 0);
				gradient.setLocalMatrix(shaderMatrix);
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shimmerPaint);
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

				if (imgHeroBanner != null) imgHeroBanner.invalidate();
				if (textHeroTitle != null) textHeroTitle.invalidate();
				if (textHeroDesc != null) textHeroDesc.invalidate();
				if (btnHeroPlay != null) btnHeroPlay.invalidate();
				if (btnHeroFavorite != null) btnHeroFavorite.invalidate();

				if (recyclerPopular != null) {
					for (int idx = 0; idx < recyclerPopular.getChildCount(); idx++) {
						invalidateRecursive(recyclerPopular.getChildAt(idx));
					}
				}
				if (listPopular != null) {
					for (int idx = 0; idx < listPopular.getChildCount(); idx++) {
						invalidateRecursive(listPopular.getChildAt(idx));
					}
				}
				if (gridview1 != null) {
					for (int idx = 0; idx < gridview1.getChildCount(); idx++) {
						invalidateRecursive(gridview1.getChildAt(idx));
					}
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
			for (int k = 0; k < vg.getChildCount(); k++) {
				invalidateRecursive(vg.getChildAt(k));
			}
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
	public void onDestroy() {
		stopMainShimmer();
		super.onDestroy();
	}

	private void initialize(Bundle _savedInstanceState) {
		dongflix_req = new RequestNetwork(this);

		gridview1 = findViewById(R.id.gridview1);
		recyclerPopular = findViewById(R.id.recyclerPopular);
		listPopular = findViewById(R.id.listPopular);
		recyclerSearchList = findViewById(R.id.recycler_search_list);
		searchBarContainer = findViewById(R.id.search_bar_container);
		inputSearch = findViewById(R.id.input_search);
		btnClearSearch = findViewById(R.id.btn_clear_search);
		textSearchSectionLabel = findViewById(R.id.text_search_section_label);

		textHeroTitle = findViewById(R.id.text_hero_title);
		textHeroDesc = findViewById(R.id.text_hero_desc);
		textSeeAll = findViewById(R.id.text_see_all);
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

		btnTopSearch = findViewById(R.id.btn_top_search);
		btnTopChat = findViewById(R.id.btn_top_chat);
		imgTopSearch = findViewById(R.id.img_top_search);
		imgTopChat = findViewById(R.id.img_top_chat);

		navbarExpanded = findViewById(R.id.navbar);
		btnCompactHome = findViewById(R.id.btn_compact_home);
		miniPlayerInlineSlot = findViewById(R.id.mini_player_inline_slot);
		btnCircleSearch = findViewById(R.id.btn_circle_search);
		tabBeranda = findViewById(R.id.tab_beranda);
		tabPustaka = findViewById(R.id.tab_pustaka);
		tabAkun = findViewById(R.id.tab_akun);
		imgHome = findViewById(R.id.img_home);
		imgFavorite = findViewById(R.id.img_favorite);
		imgProfile = findViewById(R.id.img_profile);
		imgSearch = findViewById(R.id.img_search);
		txtHome = findViewById(R.id.txt_home);
		txtFavorite = findViewById(R.id.txt_favorite);
		txtProfile = findViewById(R.id.txt_profile);

		if (textSeeAll != null) {
			textSeeAll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(MainActivity.this, CatalogActivity.class);
					startActivity(intent);
				}
			});
		}

		if (btnHeroFavorite != null) {
			btnHeroFavorite.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentHeroItem != null) {
						toggleFavoriteItem(currentHeroItem);
					}
				}
			});
		}

		if (btnTopChat != null) {
			btnTopChat.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Class<?> chatCls = Class.forName("com.dongflix.ChatActivity");
						startActivity(new Intent(MainActivity.this, chatCls));
					} catch (Exception e) {
						Toast.makeText(MainActivity.this, "Membuka Obrolan Komunitas", Toast.LENGTH_SHORT).show();
					}
				}
			});
		}

		if (gridview1 != null) {
			gridview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
					if (listMap != null && _param3 < listMap.size()) {
						HashMap<String, Object> selectedData = listMap.get(_param3);
						if (selectedData.containsKey("is_loading")) return;
						String slug = selectedData.get("slug") != null ? selectedData.get("slug").toString() : "";
						openWatchBySlug(slug);
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
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
					String q = s.toString().trim().toLowerCase();
					if (btnClearSearch != null) {
						btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
					}

					if (q.isEmpty()) {
						if (textSearchSectionLabel != null) {
							textSearchSectionLabel.setVisibility(View.VISIBLE);
							textSearchSectionLabel.setText("Populer");
						}
						if (recyclerSearchList != null) {
							recyclerSearchList.setAdapter(new ListPopularAdapter(listMap));
						}
					} else {
						if (textSearchSectionLabel != null) {
							textSearchSectionLabel.setVisibility(View.GONE);
						}
						ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
						for (HashMap<String, Object> item : fullCatalogList) {
							String title = item.get("title") != null ? item.get("title").toString().toLowerCase() : "";
							String genres = item.get("genres") != null ? item.get("genres").toString().toLowerCase() : "";
							if (title.contains(q) || genres.contains(q)) {
								filtered.add(item);
							}
						}
						if (recyclerSearchList != null) {
							recyclerSearchList.setAdapter(new ListPopularAdapter(filtered));
						}
					}
				}
				@Override public void afterTextChanged(Editable s) {}
			});
		}

		_dongflix_req_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _response = _param2;
				stopMainShimmer();

				DongflixPlayerManager.getInstance().cachedAllCatalogJson = _response;

				fullCatalogList = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
				if (fullCatalogList == null) fullCatalogList = new ArrayList<>();

				listMap = new ArrayList<>(fullCatalogList);
				if (listMap.size() > 12) {
					listMap = new ArrayList<>(listMap.subList(0, 12));
				}

				if (gridview1 != null) {
					gridview1.setAdapter(new Gridview1Adapter(listMap));
					setGridViewHeightBasedOnChildren(gridview1, 3);
				}

				if (recyclerSearchList != null) {
					recyclerSearchList.setAdapter(new ListPopularAdapter(listMap));
				}

				if (listMap != null && listMap.size() >= 3) {
					if (textHeroTitle != null) textHeroTitle.setBackgroundColor(Color.TRANSPARENT);
					if (textHeroDesc != null) textHeroDesc.setBackgroundColor(Color.TRANSPARENT);
					if (imgHeroBanner != null) imgHeroBanner.setBackgroundColor(Color.TRANSPARENT);

					GradientDrawable playActiveBg = new GradientDrawable();
					playActiveBg.setShape(GradientDrawable.RECTANGLE);
					playActiveBg.setColor(Color.WHITE);
					playActiveBg.setCornerRadius(100f);

					if (btnHeroPlay != null) {
						btnHeroPlay.setBackground(playActiveBg);
						btnHeroPlay.setEnabled(true);
					}
					if (btnHeroFavorite != null) {
						btnHeroFavorite.setEnabled(true);
						applyUniversalGlassStyle(btnHeroFavorite, btnHeroFavorite);
					}
					if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.VISIBLE);
					if (indicatorCapsule != null) indicatorCapsule.setVisibility(View.VISIBLE);

					final float density = getResources().getDisplayMetrics().density;
					final int activeWidth = (int) (16 * density);
					final int inactiveWidth = (int) (5 * density);
					final int fixedHeight = (int) (5 * density);
					final int marginSpacing = (int) (5 * density);

					final GradientDrawable activeDotDrawable = new GradientDrawable();
					activeDotDrawable.setShape(GradientDrawable.RECTANGLE);
					activeDotDrawable.setColor(Color.WHITE);
					activeDotDrawable.setCornerRadius(100f);

					final GradientDrawable inactiveDotDrawable = new GradientDrawable();
					inactiveDotDrawable.setShape(GradientDrawable.RECTANGLE);
					inactiveDotDrawable.setColor(Color.parseColor("#80FFFFFF"));
					inactiveDotDrawable.setCornerRadius(100f);

					final int[] heroCurrentIndex = {0};
					final Handler heroHandler = new Handler(Looper.getMainLooper());

					Runnable heroRunnable = new Runnable() {
						@Override
						public void run() {
							int pos = heroCurrentIndex[0];
							HashMap<String, Object> heroItem = listMap.get(pos);
							currentHeroItem = heroItem;

							String heroTitle = heroItem.get("title") != null ? heroItem.get("title").toString() : "";
							String heroSynopsis = heroItem.get("synopsis") != null ? cleanText(heroItem.get("synopsis").toString()) : "";
							String heroCover = heroItem.get("cover") != null ? heroItem.get("cover").toString().trim() : "";
							final String heroSlug = heroItem.get("slug") != null ? heroItem.get("slug").toString() : "";

							if (heroCover.contains("i.ibb.co.com")) {
								heroCover = heroCover.replace("i.ibb.co.com", "i.ibb.co");
							}
							if (heroCover.startsWith("http://")) {
								heroCover = heroCover.replace("http://", "https://");
							}

							if (textHeroTitle != null) textHeroTitle.setText(heroTitle);
							if (textHeroDesc != null) textHeroDesc.setText(heroSynopsis);
							updateHeroFavoriteIcon(heroSlug);

							if (imgHeroBanner != null) {
								Glide.with(getApplicationContext())
								.load(Uri.parse(heroCover))
								.centerCrop()
								.placeholder(R.drawable.default_image)
								.error(R.drawable.default_image)
								.into(imgHeroBanner);
							}

							if (btnHeroPlay != null) {
								btnHeroPlay.setOnClickListener(new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										openWatchBySlug(heroSlug);
									}
								});
							}

							View[] dots = {dot0, dot1, dot2};
							for (int i = 0; i < dots.length; i++) {
								if (dots[i] != null) {
									boolean isActive = (i == pos);
									LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dots[i].getLayoutParams();
									params.width = isActive ? activeWidth : inactiveWidth;
									params.height = fixedHeight;
									params.leftMargin = (i == 0) ? 0 : marginSpacing;
									dots[i].setLayoutParams(params);
									dots[i].setBackground(isActive ? activeDotDrawable : inactiveDotDrawable);
								}
							}

							heroCurrentIndex[0] = (pos + 1) % 3;
							heroHandler.postDelayed(this, 4000);
						}
					};

					heroHandler.removeCallbacksAndMessages(null);
					heroHandler.post(heroRunnable);
				}

				if (recyclerPopular != null) recyclerPopular.setAdapter(new RecyclerPopularAdapter(listMap));
				if (listPopular != null) listPopular.setAdapter(new ListPopularAdapter(listMap));

				sortAndRenderPopularSections();
				syncFirebaseViewsAndSort();
			}

			@Override
			public void onErrorResponse(String _param1, String _param2) {
				stopMainShimmer();
			}
		};
	}

	private String cleanText(String input) {
		if (input == null) return "";
		return input.replaceAll("\\[[^\\x20-\\x7E]+\\]", "").replaceAll("[^\\x20-\\x7E\\n]", "").trim();
	}

	private void initializeLogic() {
		_UI();
		_Nav();
		setupScrollResponsiveNavbar();
		applyCustomFonts();

		float d = getResources().getDisplayMetrics().density;

		ArrayList<HashMap<String, Object>> dummyList = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			HashMap<String, Object> map = new HashMap<>();
			map.put("is_loading", true);
			dummyList.add(map);
		}

		if (gridview1 != null) {
			gridview1.setAdapter(new Gridview1Adapter(dummyList));
			gridview1.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return event.getAction() == MotionEvent.ACTION_MOVE;
				}
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

		if (btnHeroPlay != null) {
			btnHeroPlay.setEnabled(false);
			btnHeroPlay.setBackground(createMainShimmerDrawable(100f));
		}
		if (btnHeroFavorite != null) {
			btnHeroFavorite.setEnabled(false);
			btnHeroFavorite.setBackground(createMainShimmerDrawable(100f));
		}

		if (indicatorCapsule != null) indicatorCapsule.setVisibility(View.INVISIBLE);
		if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.GONE);

		if (recyclerPopular != null) {
			LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
			recyclerPopular.setLayoutManager(layoutManager);
			if (recyclerPopular.getOnFlingListener() == null) {
				PagerSnapHelper snapHelper = new PagerSnapHelper();
				snapHelper.attachToRecyclerView(recyclerPopular);
			}
			recyclerPopular.setAdapter(new RecyclerPopularAdapter(dummyList));
		}

		if (listPopular != null) {
			listPopular.setLayoutManager(new LinearLayoutManager(this));
			listPopular.setNestedScrollingEnabled(false);
			listPopular.setAdapter(new RecyclerPopularAdapter(dummyList));
		}

		if (recyclerSearchList != null) {
			recyclerSearchList.setLayoutManager(new LinearLayoutManager(this));
			recyclerSearchList.setAdapter(new RecyclerPopularAdapter(dummyList));
		}

		startMainShimmer();
		dongflix_req.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/", "", _dongflix_req_request_listener);
	}

	private void applyUniversalGlassStyle(View blurTarget, View containerTarget) {
		float d = getResources().getDisplayMetrics().density;
		if (blurTarget != null) {
			GradientDrawable glass = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{Color.parseColor("#B3161923"), Color.parseColor("#CC0A0C12")}
			);
			glass.setCornerRadius(100f);
			glass.setStroke((int)(1 * d), Color.parseColor("#33FFFFFF"));
			blurTarget.setBackground(glass);
		}
		if (containerTarget != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			containerTarget.setElevation(16f);
		}
	}

	public void _UI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
			window.setStatusBarColor(Color.TRANSPARENT);
		}

		int statusBarHeight = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			statusBarHeight = getResources().getDimensionPixelSize(resourceId);
		}
		float density = getResources().getDisplayMetrics().density;

		View topHeaderBar = findViewById(R.id.relativelayout69);
		if (topHeaderBar != null) {
			int horizontalPadding = (int)(16 * density);
			int topPadding = statusBarHeight + (int)(8 * density);
			int bottomPadding = (int)(10 * density);
			topHeaderBar.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding);
		}

		applyUniversalGlassStyle(btnTopSearch, btnTopSearch);
		applyUniversalGlassStyle(btnTopChat, btnTopChat);
		if (imgTopSearch != null) imgTopSearch.setColorFilter(Color.WHITE);
		if (imgTopChat != null) imgTopChat.setColorFilter(Color.WHITE);
		if (imgHeroFavorite != null) imgHeroFavorite.setColorFilter(Color.WHITE);

		if (searchBarContainer != null) {
			LinearLayout.LayoutParams sbLp = (LinearLayout.LayoutParams) searchBarContainer.getLayoutParams();
			sbLp.topMargin = statusBarHeight + (int)(12 * density);
			searchBarContainer.setLayoutParams(sbLp);

			GradientDrawable sbBg = new GradientDrawable();
			sbBg.setColor(Color.parseColor("#12141A"));
			sbBg.setCornerRadius(100f);
			sbBg.setStroke((int)(1 * density), Color.parseColor("#26FFFFFF"));
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
			GradientDrawable topGradient = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { Color.parseColor("#E6000000"), Color.TRANSPARENT }
			);
			headerShadow.setBackground(topGradient);
		}

		View heroShadow = findViewById(R.id.hero_shadow);
		if (heroShadow != null) {
			GradientDrawable bottomGradient = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { Color.TRANSPARENT, Color.parseColor("#FF000000") }
			);
			heroShadow.setBackground(bottomGradient);
		}

		applyUniversalGlassStyle(findViewById(R.id.navbar_blur_bg), navbarExpanded);
		applyUniversalGlassStyle(findViewById(R.id.compact_home_blur_bg), btnCompactHome);
		applyUniversalGlassStyle(findViewById(R.id.search_blur_bg), btnCircleSearch);
	}

	private void setupScrollResponsiveNavbar() {
		NestedScrollView mainScroll = findViewById(R.id.main_scroll_view);
		if (mainScroll != null) {
			mainScroll.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
				@Override
				public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
					if (!DongflixPlayerManager.getInstance().isMiniPlayerActive) {
						resetNavbarToExpanded();
						return;
					}

					int dy = scrollY - oldScrollY;
					if (scrollY <= 20 || dy < -12) {
						setNavbarCompactState(false);
					} else if (dy > 14 && scrollY > 80) {
						setNavbarCompactState(true);
					}
				}
			});
		}
	}

	public void resetNavbarToExpanded() {
		setNavbarCompactState(false);
	}

	private void setNavbarCompactState(boolean compact) {
		if (compact && !DongflixPlayerManager.getInstance().isMiniPlayerActive) {
			compact = false;
		}
		if (isNavbarCompact == compact) return;
		isNavbarCompact = compact;

		if (compact) {
			if (navbarExpanded != null) navbarExpanded.setVisibility(View.GONE);
			if (btnCompactHome != null) btnCompactHome.setVisibility(View.VISIBLE);
		} else {
			if (btnCompactHome != null) btnCompactHome.setVisibility(View.GONE);
			if (miniPlayerInlineSlot != null) miniPlayerInlineSlot.setVisibility(View.GONE);
			if (navbarExpanded != null) navbarExpanded.setVisibility(View.VISIBLE);
		}

		DongflixPlayerManager.getInstance().setCompactScrollMode(this, compact);
	}

	public void _Nav() {
		final View pageHome = findViewById(R.id.main_scroll_view);
		final View pageSearch = findViewById(R.id.layout_search);
		final View pageFavorite = findViewById(R.id.layout_favorite);
		final View pageProfile = findViewById(R.id.layout_profile);

		final GradientDrawable activeTabPill = new GradientDrawable();
		activeTabPill.setColor(Color.parseColor("#26FFFFFF"));
		activeTabPill.setCornerRadius(100f);

		final int colorActive = Color.WHITE;
		final int colorInactive = Color.parseColor("#94A3B8");

		View.OnClickListener tabClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int id = v.getId();

				if (pageHome != null) pageHome.setVisibility(View.GONE);
				if (pageSearch != null) pageSearch.setVisibility(View.GONE);
				if (pageFavorite != null) pageFavorite.setVisibility(View.GONE);
				if (pageProfile != null) pageProfile.setVisibility(View.GONE);

				if (tabBeranda != null) tabBeranda.setBackground(null);
				if (tabPustaka != null) tabPustaka.setBackground(null);
				if (tabAkun != null) tabAkun.setBackground(null);

				if (imgHome != null) imgHome.setColorFilter(colorInactive);
				if (imgFavorite != null) imgFavorite.setColorFilter(colorInactive);
				if (imgProfile != null) imgProfile.setColorFilter(colorInactive);
				if (imgSearch != null) imgSearch.setColorFilter(colorInactive);

				if (txtHome != null) txtHome.setTextColor(colorInactive);
				if (txtFavorite != null) txtFavorite.setTextColor(colorInactive);
				if (txtProfile != null) txtProfile.setTextColor(colorInactive);

				if ((tabBeranda != null && id == tabBeranda.getId()) || (btnCompactHome != null && id == btnCompactHome.getId())) {
					hideKeyboard();
					if (pageHome != null) pageHome.setVisibility(View.VISIBLE);
					if (tabBeranda != null) tabBeranda.setBackground(activeTabPill);
					if (imgHome != null) imgHome.setColorFilter(colorActive);
					if (txtHome != null) txtHome.setTextColor(colorActive);
					setNavbarCompactState(false);
				} else if (tabPustaka != null && id == tabPustaka.getId()) {
					hideKeyboard();
					if (pageFavorite != null) pageFavorite.setVisibility(View.VISIBLE);
					tabPustaka.setBackground(activeTabPill);
					if (imgFavorite != null) imgFavorite.setColorFilter(colorActive);
					if (txtFavorite != null) txtFavorite.setTextColor(colorActive);
				} else if (tabAkun != null && id == tabAkun.getId()) {
					hideKeyboard();
					if (pageProfile != null) pageProfile.setVisibility(View.VISIBLE);
					tabAkun.setBackground(activeTabPill);
					if (imgProfile != null) imgProfile.setColorFilter(colorActive);
					if (txtProfile != null) txtProfile.setTextColor(colorActive);
				} else if (btnCircleSearch != null && id == btnCircleSearch.getId()) {
					if (pageSearch != null) pageSearch.setVisibility(View.VISIBLE);
					if (imgSearch != null) imgSearch.setColorFilter(colorActive);
					showKeyboardOnSearch();
				}
			}
		};

		if (tabBeranda != null) tabBeranda.setOnClickListener(tabClickListener);
		if (btnCompactHome != null) btnCompactHome.setOnClickListener(tabClickListener);
		if (tabPustaka != null) tabPustaka.setOnClickListener(tabClickListener);
		if (tabAkun != null) tabAkun.setOnClickListener(tabClickListener);
		if (btnCircleSearch != null) btnCircleSearch.setOnClickListener(tabClickListener);

		if (btnTopSearch != null && btnCircleSearch != null) {
			btnTopSearch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					btnCircleSearch.performClick();
				}
			});
		}

		if (tabBeranda != null) tabBeranda.performClick();
	}

	public void setGridViewHeightBasedOnChildren(final GridView gridView, final int columns) {
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
				int paddingBottomPx = (int) (16 * density);

				int availableWidth = gridView.getWidth() - paddingHorizontal - (horizontalSpacingPx * (columns - 1));
				int columnWidth = availableWidth / columns;

				View itemView = gridView.getAdapter().getView(0, null, gridView);
				itemView.measure(
					View.MeasureSpec.makeMeasureSpec(columnWidth, View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
				);

				int exactItemHeight = itemView.getMeasuredHeight();
				ViewGroup.LayoutParams params = gridView.getLayoutParams();
				params.height = (exactItemHeight * rows) + (verticalSpacingPx * Math.max(0, rows - 1)) + paddingBottomPx;
				gridView.setLayoutParams(params);
				gridView.requestLayout();
			}
		});
	}

	public class RecyclerPopularAdapter extends RecyclerView.Adapter<RecyclerPopularAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> _data;
		public RecyclerPopularAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.item_popular_donghua, parent, false);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			float d = _view.getResources().getDisplayMetrics().density;

			TextView textPopularTitle = _view.findViewById(getResources().getIdentifier("text_popular_title", "id", getPackageName()));
			TextView textPopularDesc = _view.findViewById(getResources().getIdentifier("text_popular_desc", "id", getPackageName()));
			TextView textPopularGenre = _view.findViewById(getResources().getIdentifier("text_popular_genre", "id", getPackageName()));
			TextView textPopularStatus = _view.findViewById(getResources().getIdentifier("text_popular_status", "id", getPackageName()));
			ImageView imgPopularCover = _view.findViewById(getResources().getIdentifier("img_popular_cover", "id", getPackageName()));
			View linear35 = _view.findViewById(getResources().getIdentifier("linear35", "id", getPackageName()));

			if (_data.get(_position).containsKey("is_loading")) {
				if (textPopularTitle != null) {
					textPopularTitle.setText("");
					textPopularTitle.setBackground(createMainShimmerDrawable(100f));
				}
				if (textPopularDesc != null) {
					textPopularDesc.setText("");
					textPopularDesc.setBackground(createMainShimmerDrawable(6 * d));
				}
				if (textPopularGenre != null) {
					textPopularGenre.setText("");
					textPopularGenre.setBackground(createMainShimmerDrawable(6 * d));
				}
				if (textPopularStatus != null) {
					textPopularStatus.setText("                    ");
					textPopularStatus.setBackground(createMainShimmerDrawable(100f));
				}
				if (imgPopularCover != null) {
					imgPopularCover.setImageResource(android.R.color.transparent);
					imgPopularCover.setBackground(createMainShimmerDrawable(10 * d));
				}
			} else {
				if (textPopularTitle != null) textPopularTitle.setBackgroundColor(Color.TRANSPARENT);
				if (textPopularDesc != null) textPopularDesc.setBackgroundColor(Color.TRANSPARENT);
				if (textPopularGenre != null) textPopularGenre.setBackgroundColor(Color.TRANSPARENT);
				if (imgPopularCover != null) imgPopularCover.setBackgroundColor(Color.TRANSPARENT);

				if (textPopularStatus != null) textPopularStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF111111));
				if (textPopularTitle != null) textPopularTitle.setText(_data.get(_position).get("title").toString());
				if (textPopularDesc != null) textPopularDesc.setText(cleanText(_data.get(_position).get("synopsis").toString()));
				if (textPopularGenre != null) textPopularGenre.setText(_data.get(_position).get("genres").toString().replace("[", "").replace("]", ""));

				if (textPopularStatus != null) {
					textPopularStatus.setText(_data.get(_position).get("episode").toString().concat(" / ".concat(_data.get(_position).get("status").toString())));
				}
				if (imgPopularCover != null) {
					Glide.with(getApplicationContext()).load(Uri.parse(_data.get(_position).get("cover").toString())).into(imgPopularCover);
				}

				View clickTarget = (linear35 != null) ? linear35 : _view;
				clickTarget.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String slug = _data.get(_position).get("slug") != null ? _data.get(_position).get("slug").toString() : "";
						openWatchBySlug(slug);
					}
				});
			}
		}

		@Override
		public int getItemCount() { return _data.size(); }

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) { super(v); }
		}
	}

	public class Gridview1Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		public Gridview1Adapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }

		@Override public int getCount() { return _data.size(); }
		@Override public HashMap<String, Object> getItem(int _index) { return _data.get(_index); }
		@Override public long getItemId(int _index) { return _index; }

		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater inflater = getLayoutInflater();
			View _view = inflater.inflate(R.layout.custom_anime, _container, false);
			float d = _view.getResources().getDisplayMetrics().density;

			TextView textTitle = _view.findViewById(R.id.text_title);
			TextView textStatus = _view.findViewById(R.id.text_status);
			TextView textEpisode = _view.findViewById(R.id.text_episode);
			ImageView imgCover = _view.findViewById(R.id.img_cover);
			View view2 = _view.findViewById(R.id.view2);

			HashMap<String, Object> item = _data.get(_position);

			if (item.containsKey("is_loading")) {
				if (textTitle != null) {
					textTitle.setText("");
					textTitle.setBackground(createMainShimmerDrawable(6 * d));
				}
				if (textStatus != null) {
					textStatus.setText("");
					textStatus.setBackground(createMainShimmerDrawable(50f));
				}
				if (textEpisode != null) {
					textEpisode.setText("");
					textEpisode.setBackground(createMainShimmerDrawable(4 * d));
				}
				if (imgCover != null) {
					imgCover.setImageResource(android.R.color.transparent);
					imgCover.setBackground(createMainShimmerDrawable(10 * d));
				}
				if (view2 != null) view2.setVisibility(View.GONE);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (textTitle != null) textTitle.setBackgroundColor(Color.TRANSPARENT);
				if (textEpisode != null) textEpisode.setBackgroundColor(Color.TRANSPARENT);
				if (view2 != null) view2.setVisibility(View.VISIBLE);

				if (textStatus != null) {
					textStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF3B82F6));
				}

				if (view2 != null) {
					GradientDrawable gradient = new GradientDrawable(
						GradientDrawable.Orientation.BOTTOM_TOP,
						new int[] { Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT }
					);
					view2.setBackground(gradient);
				}

				if (textTitle != null) textTitle.setText(_data.get(_position).get("title").toString());
				if (textStatus != null) textStatus.setText(_data.get(_position).get("status").toString());
				if (textEpisode != null) textEpisode.setText(_data.get(_position).get("episode").toString());

				String cover = _data.get(_position).get("cover") != null ? _data.get(_position).get("cover").toString().trim() : "";
				if (cover.contains("i.ibb.co.com")) {
					cover = cover.replace("i.ibb.co.com", "i.ibb.co");
				}
				if (cover.startsWith("http://")) {
					cover = cover.replace("http://", "https://");
				}

				if (imgCover != null) {
					Glide.with(getApplicationContext()).load(Uri.parse(cover)).into(imgCover);
				}
			}
			return _view;
		}
	}

	public class ListPopularAdapter extends RecyclerView.Adapter<ListPopularAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> _data;
		public ListPopularAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.item_popular_donghua, parent, false);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			TextView textPopularTitle = _view.findViewById(getResources().getIdentifier("text_popular_title", "id", getPackageName()));
			TextView textPopularDesc = _view.findViewById(getResources().getIdentifier("text_popular_desc", "id", getPackageName()));
			TextView textPopularGenre = _view.findViewById(getResources().getIdentifier("text_popular_genre", "id", getPackageName()));
			TextView textPopularStatus = _view.findViewById(getResources().getIdentifier("text_popular_status", "id", getPackageName()));
			ImageView imgPopularCover = _view.findViewById(getResources().getIdentifier("img_popular_cover", "id", getPackageName()));
			View linear35 = _view.findViewById(getResources().getIdentifier("linear35", "id", getPackageName()));

			if (textPopularStatus != null) textPopularStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF111111));
			if (textPopularTitle != null) textPopularTitle.setText(_data.get(_position).get("title").toString());
			if (textPopularDesc != null) textPopularDesc.setText(cleanText(_data.get(_position).get("synopsis").toString()));
			if (textPopularGenre != null) textPopularGenre.setText(_data.get(_position).get("genres").toString().replace("[", "").replace("]", ""));

			if (textPopularStatus != null) {
				textPopularStatus.setText(_data.get(_position).get("episode").toString().concat(" / ".concat(_data.get(_position).get("status").toString())));
			}
			if (imgPopularCover != null) {
				Glide.with(getApplicationContext()).load(Uri.parse(_data.get(_position).get("cover").toString())).into(imgPopularCover);
			}

			View clickTarget = (linear35 != null) ? linear35 : _view;
			clickTarget.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String slug = _data.get(_position).get("slug") != null ? _data.get(_position).get("slug").toString() : "";
					openWatchBySlug(slug);
				}
			});
		}

		@Override
		public int getItemCount() { return _data.size(); }

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) { super(v); }
		}
	}
}
