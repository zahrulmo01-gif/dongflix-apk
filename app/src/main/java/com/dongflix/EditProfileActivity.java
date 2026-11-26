package com.dongflix;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditProfileActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";
	private static final String CLOUDINARY_CLOUD_NAME = "pqyvpk17";
	private static final String CLOUDINARY_UPLOAD_PRESET = "dongflix";
	private static final String CLOUDINARY_UPLOAD_URL =
		"https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/image/upload";
	private static final int REQ_PICK_IMAGE = 401;

	private FirebaseUser currentUser;
	private DatabaseReference userNodeRef;
	private String currentUserKey = "";
	private String currentEmail = "";
	private String currentName = "";
	private String currentUsername = "";
	private String currentBio = "";
	private String currentPhotoUrl = "";
	private boolean isUploadingPhoto = false;

	private ImageView ivAvatarPreview;
	private TextView tvAvatarInitial;
	private ProgressBar avatarUploadSpinner;
	private TextView tvChangePhotoLabel;
	private TextView tvNameValue;
	private TextView tvUsernameValue;
	private TextView tvEmailValue;
	private TextView tvBioValue;
	private Typeface outfitFont;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null || currentUser.getEmail() == null) {
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		}

		currentEmail = currentUser.getEmail().trim().toLowerCase(Locale.US);
		currentUserKey = currentEmail.replace(".", ",");
		String fallbackName = currentEmail.contains("@")
			? currentEmail.substring(0, currentEmail.indexOf("@")) : currentEmail;

		currentName = getIntent().getStringExtra("name") != null
			? getIntent().getStringExtra("name").trim() : fallbackName;
		currentUsername = getIntent().getStringExtra("username") != null
			? getIntent().getStringExtra("username").trim().replaceFirst("^@", "")
			: fallbackName.toLowerCase(Locale.US).replaceAll("[^a-z0-9_]", "");
		if (currentUsername.isEmpty()) currentUsername = "user";
		currentBio = getIntent().getStringExtra("bio") != null
			? getIntent().getStringExtra("bio").trim() : "";
		currentPhotoUrl = getIntent().getStringExtra("photoURL") != null
			? getIntent().getStringExtra("photoURL").trim() : "";

		userNodeRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("users")
			.child(currentUserKey);

		buildLayout();
		bindAllProfileFields();
		loadLatestProfileFromFirebase();
	}

	private Typeface getOutfitFont() {
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

	private void styleText(TextView tv) {
		if (tv == null) return;
		tv.setTypeface(getOutfitFont(), Typeface.NORMAL);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			tv.setLetterSpacing(-0.01f);
		}
	}

	private void buildLayout() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window w = getWindow();
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
			w.setStatusBarColor(Color.TRANSPARENT);
		}

		final float d = getResources().getDisplayMetrics().density;
		int statusH = 0;
		int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resId > 0) statusH = getResources().getDimensionPixelSize(resId);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(Color.parseColor("#000000"));
		root.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		// Top Bar selaras aplikasi Dongflix & referensi Edit profil
		FrameLayout topBar = new FrameLayout(this);
		topBar.setPadding((int) (14 * d), statusH + (int) (10 * d), (int) (14 * d), (int) (10 * d));
		topBar.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		FrameLayout btnBack = new FrameLayout(this);
		int backSize = (int) (38 * d);
		btnBack.setLayoutParams(new FrameLayout.LayoutParams(backSize, backSize, Gravity.START | Gravity.CENTER_VERTICAL));
		GradientDrawable backBg = new GradientDrawable();
		backBg.setShape(GradientDrawable.OVAL);
		backBg.setColor(Color.parseColor("#12141A"));
		backBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E2433"));
		btnBack.setBackground(backBg);

		ImageView ivBack = new ImageView(this);
		int arrowSz = (int) (18 * d);
		ivBack.setLayoutParams(new FrameLayout.LayoutParams(arrowSz, arrowSz, Gravity.CENTER));
		ivBack.setImageResource(R.drawable.ic_back_arrow);
		ivBack.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
		btnBack.addView(ivBack);
		btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		TextView tvHeaderTitle = new TextView(this);
		tvHeaderTitle.setText("Edit profil");
		tvHeaderTitle.setTextColor(Color.WHITE);
		tvHeaderTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
		tvHeaderTitle.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
		));
		styleText(tvHeaderTitle);

		topBar.addView(btnBack);
		topBar.addView(tvHeaderTitle);
		root.addView(topBar);

		ScrollView scroll = new ScrollView(this);
		scroll.setVerticalScrollBarEnabled(false);
		scroll.setFillViewport(true);
		scroll.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
		));

		LinearLayout content = new LinearLayout(this);
		content.setOrientation(LinearLayout.VERTICAL);
		content.setPadding((int) (16 * d), (int) (14 * d), (int) (16 * d), (int) (32 * d));
		content.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		// Bagian Avatar Tengah + Overlay Kamera + Tombol Ubah Foto
		LinearLayout avatarSection = new LinearLayout(this);
		avatarSection.setOrientation(LinearLayout.VERTICAL);
		avatarSection.setGravity(Gravity.CENTER_HORIZONTAL);
		LinearLayout.LayoutParams avSecLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		avSecLp.bottomMargin = (int) (22 * d);
		avatarSection.setLayoutParams(avSecLp);

		FrameLayout avatarCircleWrap = new FrameLayout(this);
		int avSize = (int) (96 * d);
		avatarCircleWrap.setLayoutParams(new LinearLayout.LayoutParams(avSize, avSize));

		ivAvatarPreview = new ImageView(this);
		ivAvatarPreview.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		ivAvatarPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

		tvAvatarInitial = new TextView(this);
		tvAvatarInitial.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		tvAvatarInitial.setGravity(Gravity.CENTER);
		tvAvatarInitial.setTextColor(Color.WHITE);
		tvAvatarInitial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f);
		styleText(tvAvatarInitial);

		View dimOverlay = new View(this);
		dimOverlay.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		GradientDrawable dimCircle = new GradientDrawable();
		dimCircle.setShape(GradientDrawable.OVAL);
		dimCircle.setColor(Color.parseColor("#59000000"));
		dimOverlay.setBackground(dimCircle);

		ImageView ivCameraBadge = new ImageView(this);
		int camSz = (int) (28 * d);
		ivCameraBadge.setLayoutParams(new FrameLayout.LayoutParams(camSz, camSz, Gravity.CENTER));
		ivCameraBadge.setImageResource(android.R.drawable.ic_menu_camera);
		ivCameraBadge.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

		avatarUploadSpinner = new ProgressBar(this);
		int spSz = (int) (34 * d);
		avatarUploadSpinner.setLayoutParams(new FrameLayout.LayoutParams(spSz, spSz, Gravity.CENTER));
		avatarUploadSpinner.setVisibility(View.GONE);

		avatarCircleWrap.addView(ivAvatarPreview);
		avatarCircleWrap.addView(tvAvatarInitial);
		avatarCircleWrap.addView(dimOverlay);
		avatarCircleWrap.addView(ivCameraBadge);
		avatarCircleWrap.addView(avatarUploadSpinner);

		View.OnClickListener pickPhotoClick = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isUploadingPhoto) return;
				openGalleryPicker();
			}
		};
		avatarCircleWrap.setOnClickListener(pickPhotoClick);

		tvChangePhotoLabel = new TextView(this);
		tvChangePhotoLabel.setText("Ubah foto");
		tvChangePhotoLabel.setTextColor(Color.parseColor("#38BDF8"));
		tvChangePhotoLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		LinearLayout.LayoutParams cpLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		cpLp.topMargin = (int) (10 * d);
		tvChangePhotoLabel.setLayoutParams(cpLp);
		styleText(tvChangePhotoLabel);
		tvChangePhotoLabel.setOnClickListener(pickPhotoClick);

		avatarSection.addView(avatarCircleWrap);
		avatarSection.addView(tvChangePhotoLabel);
		content.addView(avatarSection);

		// Kartu 1: Nama, Username, Email (hanya lihat)
		LinearLayout identityCard = createSectionCard(d);

		tvNameValue = new TextView(this);
		identityCard.addView(buildProfileFieldRow(
			"Nama",
			tvNameValue,
			true,
			false,
			d,
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openSingleFieldEditDialog("Nama", currentName, "Masukkan nama tampilan", 32, false, new FieldSaveCallback() {
						@Override
						public void onSave(String newValue) {
							saveProfileFieldToFirebase("name", newValue);
						}
					});
				}
			}
		));

		identityCard.addView(buildRowDivider(d));

		tvUsernameValue = new TextView(this);
		identityCard.addView(buildProfileFieldRow(
			"Username",
			tvUsernameValue,
			true,
			false,
			d,
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openUsernameEditDialog();
				}
			}
		));

		identityCard.addView(buildRowDivider(d));

		tvEmailValue = new TextView(this);
		identityCard.addView(buildProfileFieldRow(
			"Email",
			tvEmailValue,
			false,
			true,
			d,
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					if (cm != null && !currentEmail.isEmpty()) {
						cm.setPrimaryClip(ClipData.newPlainText("Email Akun", currentEmail));
						DongflixToast.show(EditProfileActivity.this, "Email disalin (hanya dapat dilihat)");
					}
				}
			}
		));

		content.addView(identityCard);

		// Label Seksi: Info dasar
		TextView tvBasicHeader = new TextView(this);
		tvBasicHeader.setText("Info dasar");
		tvBasicHeader.setTextColor(Color.parseColor("#94A3B8"));
		tvBasicHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		LinearLayout.LayoutParams bhLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		bhLp.topMargin = (int) (18 * d);
		bhLp.bottomMargin = (int) (8 * d);
		bhLp.leftMargin = (int) (4 * d);
		tvBasicHeader.setLayoutParams(bhLp);
		styleText(tvBasicHeader);
		content.addView(tvBasicHeader);

		// Kartu 2: Bio
		LinearLayout bioCard = createSectionCard(d);
		tvBioValue = new TextView(this);
		bioCard.addView(buildProfileFieldRow(
			"Bio",
			tvBioValue,
			true,
			false,
			d,
			new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openSingleFieldEditDialog(
						"Bio",
						currentBio,
						"Tulis deskripsi singkat tentang dirimu...",
						140,
						true,
						new FieldSaveCallback() {
							@Override
							public void onSave(String newValue) {
								saveProfileFieldToFirebase("bio", newValue);
							}
						}
					);
				}
			}
		));
		content.addView(bioCard);

		scroll.addView(content);
		root.addView(scroll);
		setContentView(root);
	}

	private LinearLayout createSectionCard(float d) {
		LinearLayout card = new LinearLayout(this);
		card.setOrientation(LinearLayout.VERTICAL);
		card.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(Color.parseColor("#12141A"));
		bg.setCornerRadius(16 * d);
		bg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1E2433"));
		card.setBackground(bg);
		return card;
	}

	private View buildRowDivider(float d) {
		View div = new View(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) (1 * d))
		);
		lp.leftMargin = (int) (16 * d);
		lp.rightMargin = (int) (16 * d);
		div.setLayoutParams(lp);
		div.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
		return div;
	}

	private LinearLayout buildProfileFieldRow(
		String label,
		TextView valueView,
		boolean editable,
		boolean readOnlyEmail,
		float d,
		View.OnClickListener listener
	) {
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding((int) (16 * d), (int) (15 * d), (int) (16 * d), (int) (15 * d));
		row.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));
		row.setOnClickListener(listener);

		TextView tvLabel = new TextView(this);
		tvLabel.setText(label);
		tvLabel.setTextColor(Color.WHITE);
		tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		tvLabel.setLayoutParams(new LinearLayout.LayoutParams((int) (96 * d), ViewGroup.LayoutParams.WRAP_CONTENT));
		styleText(tvLabel);

		valueView.setTextColor(readOnlyEmail ? Color.parseColor("#94A3B8") : Color.WHITE);
		valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		valueView.setMaxLines(3);
		valueView.setEllipsize(TextUtils.TruncateAt.END);
		LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		vLp.rightMargin = (int) (8 * d);
		valueView.setLayoutParams(vLp);
		styleText(valueView);

		TextView trailing = new TextView(this);
		trailing.setTextSize(TypedValue.COMPLEX_UNIT_SP, readOnlyEmail ? 11f : 16f);
		styleText(trailing);
		if (editable) {
			trailing.setText("›");
			trailing.setTextColor(Color.parseColor("#64748B"));
		} else if (readOnlyEmail) {
			trailing.setText("Terkunci");
			trailing.setTextColor(Color.parseColor("#64748B"));
			trailing.setPadding((int) (8 * d), (int) (3 * d), (int) (8 * d), (int) (3 * d));
			GradientDrawable lockTag = new GradientDrawable();
			lockTag.setColor(Color.parseColor("#181C26"));
			lockTag.setCornerRadius(100f);
			trailing.setBackground(lockTag);
		}

		row.addView(tvLabel);
		row.addView(valueView);
		row.addView(trailing);
		return row;
	}

	private void bindAllProfileFields() {
		if (tvNameValue != null) {
			tvNameValue.setText(currentName.isEmpty() ? "-" : currentName);
		}
		if (tvUsernameValue != null) {
			tvUsernameValue.setText("@" + currentUsername);
		}
		if (tvEmailValue != null) {
			tvEmailValue.setText(currentEmail.isEmpty() ? "-" : currentEmail);
		}
		if (tvBioValue != null) {
			if (currentBio.isEmpty()) {
				tvBioValue.setText("Tulis deskripsi singkat tentang dirimu");
				tvBioValue.setTextColor(Color.parseColor("#64748B"));
			} else {
				tvBioValue.setText(currentBio);
				tvBioValue.setTextColor(Color.WHITE);
			}
		}
		bindCircularAvatarPreview(currentPhotoUrl, currentName);
	}

	private void bindCircularAvatarPreview(String photoUrl, String displayName) {
		float d = getResources().getDisplayMetrics().density;
		int sizePx = (int) (96 * d);
		String initial = (displayName != null && !displayName.trim().isEmpty())
			? displayName.trim().substring(0, 1).toUpperCase(Locale.US) : "R";

		if (photoUrl != null && !photoUrl.trim().isEmpty()) {
			String clean = photoUrl.trim();
			if (ivAvatarPreview != null) {
				ivAvatarPreview.setVisibility(View.VISIBLE);
				if (clean.startsWith("data:image")) {
					try {
						String b64 = clean.substring(clean.indexOf(",") + 1);
						byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
						Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
						if (bmp != null) {
							ivAvatarPreview.setImageBitmap(cropToCircleBitmap(bmp, sizePx));
						}
					} catch (Exception ignored) {}
				} else if (!isFinishing() && !isDestroyed()) {
					Glide.with(this).load(Uri.parse(clean)).circleCrop()
						.placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(ivAvatarPreview);
				}
			}
			if (tvAvatarInitial != null) tvAvatarInitial.setVisibility(View.GONE);
		} else {
			if (ivAvatarPreview != null) ivAvatarPreview.setVisibility(View.GONE);
			if (tvAvatarInitial != null) {
				tvAvatarInitial.setVisibility(View.VISIBLE);
				tvAvatarInitial.setText(initial);
				GradientDrawable circle = new GradientDrawable();
				circle.setShape(GradientDrawable.OVAL);
				circle.setColor(Color.parseColor("#1D4ED8"));
				tvAvatarInitial.setBackground(circle);
			}
		}
	}

	private void loadLatestProfileFromFirebase() {
		if (userNodeRef == null) return;
		userNodeRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snap) {
				if (isFinishing() || isDestroyed() || !snap.exists()) return;
				if (snap.child("name").getValue() != null) {
					String n = snap.child("name").getValue().toString().trim();
					if (!n.isEmpty()) currentName = n;
				}
				if (snap.child("username").getValue() != null) {
					String u = snap.child("username").getValue().toString().trim().replaceFirst("^@", "");
					if (!u.isEmpty()) currentUsername = u;
				}
				if (snap.child("bio").getValue() != null) {
					currentBio = snap.child("bio").getValue().toString().trim();
				}
				if (snap.child("photoURL").getValue() != null) {
					currentPhotoUrl = snap.child("photoURL").getValue().toString().trim();
				}
				bindAllProfileFields();
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		});
	}

	private void saveProfileFieldToFirebase(final String fieldKey, final String value) {
		if (userNodeRef == null) return;
		Map<String, Object> updates = new HashMap<>();
		updates.put(fieldKey, value);
		updates.put("email", currentEmail);

		userNodeRef.updateChildren(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				if (isFinishing() || isDestroyed()) return;
				if (task.isSuccessful()) {
					if ("name".equals(fieldKey)) currentName = value;
					else if ("username".equals(fieldKey)) currentUsername = value;
					else if ("bio".equals(fieldKey)) currentBio = value;
					else if ("photoURL".equals(fieldKey)) currentPhotoUrl = value;

					bindAllProfileFields();
					setResult(RESULT_OK);
					DongflixToast.show(EditProfileActivity.this, "Profil disimpan");
				} else {
					DongflixToast.show(EditProfileActivity.this, "Gagal menyimpan perubahan");
				}
			}
		});
	}

	private interface FieldSaveCallback {
		void onSave(String newValue);
	}

	private void openSingleFieldEditDialog(
		String title,
		String initialValue,
		String hint,
		int maxLen,
		boolean multiline,
		final FieldSaveCallback callback
	) {
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
		tvTitle.setText("Ubah " + title);
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
		styleText(tvTitle);
		card.addView(tvTitle);

		final EditText etInput = new EditText(this);
		etInput.setText(initialValue);
		etInput.setSelection(initialValue.length());
		etInput.setHint(hint);
		etInput.setHintTextColor(Color.parseColor("#64748B"));
		etInput.setTextColor(Color.WHITE);
		etInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		etInput.setSingleLine(!multiline);
		if (multiline) {
			etInput.setMinLines(3);
			etInput.setMaxLines(5);
			etInput.setGravity(Gravity.TOP | Gravity.START);
		}
		etInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
		etInput.setPadding((int) (14 * d), (int) (12 * d), (int) (14 * d), (int) (12 * d));
		LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		inLp.topMargin = (int) (14 * d);
		inLp.bottomMargin = (int) (16 * d);
		etInput.setLayoutParams(inLp);
		GradientDrawable inBg = new GradientDrawable();
		inBg.setColor(Color.parseColor("#181C26"));
		inBg.setCornerRadius(12 * d);
		inBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1FFFFFFF"));
		etInput.setBackground(inBg);
		styleText(etInput);
		card.addView(etInput);

		LinearLayout btnRow = new LinearLayout(this);
		btnRow.setOrientation(LinearLayout.HORIZONTAL);
		btnRow.setGravity(Gravity.END);

		TextView btnCancel = new TextView(this);
		btnCancel.setText("Batal");
		btnCancel.setTextColor(Color.parseColor("#94A3B8"));
		btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnCancel.setPadding((int) (16 * d), (int) (9 * d), (int) (16 * d), (int) (9 * d));
		styleText(btnCancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		TextView btnSave = new TextView(this);
		btnSave.setText("Simpan");
		btnSave.setTextColor(Color.WHITE);
		btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnSave.setPadding((int) (18 * d), (int) (9 * d), (int) (18 * d), (int) (9 * d));
		LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		sLp.leftMargin = (int) (8 * d);
		btnSave.setLayoutParams(sLp);
		GradientDrawable saveBg = new GradientDrawable();
		saveBg.setColor(Color.parseColor("#2563EB"));
		saveBg.setCornerRadius(100f);
		btnSave.setBackground(saveBg);
		styleText(btnSave);
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String val = etInput.getText().toString().trim();
				if (!multiline && val.isEmpty()) {
					DongflixToast.show(EditProfileActivity.this, title + " tidak boleh kosong");
					return;
				}
				dialog.dismiss();
				if (callback != null) callback.onSave(val);
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

	private void openUsernameEditDialog() {
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
		tvTitle.setText("Ubah Username");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
		styleText(tvTitle);
		card.addView(tvTitle);

		final EditText etUsername = new EditText(this);
		etUsername.setText(currentUsername);
		etUsername.setSelection(currentUsername.length());
		etUsername.setHint("username (tanpa @)");
		etUsername.setHintTextColor(Color.parseColor("#64748B"));
		etUsername.setTextColor(Color.WHITE);
		etUsername.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		etUsername.setSingleLine(true);
		etUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
		etUsername.setPadding((int) (14 * d), (int) (12 * d), (int) (14 * d), (int) (12 * d));
		LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		uLp.topMargin = (int) (14 * d);
		etUsername.setLayoutParams(uLp);
		GradientDrawable inBg = new GradientDrawable();
		inBg.setColor(Color.parseColor("#181C26"));
		inBg.setCornerRadius(12 * d);
		inBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#1FFFFFFF"));
		etUsername.setBackground(inBg);
		styleText(etUsername);
		card.addView(etUsername);

		final TextView tvStatus = new TextView(this);
		tvStatus.setText("Gunakan 3–20 karakter huruf kecil, angka, atau _");
		tvStatus.setTextColor(Color.parseColor("#94A3B8"));
		tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		stLp.topMargin = (int) (8 * d);
		stLp.bottomMargin = (int) (16 * d);
		tvStatus.setLayoutParams(stLp);
		styleText(tvStatus);
		card.addView(tvStatus);

		final Handler handler = new Handler(Looper.getMainLooper());
		final Runnable[] pending = new Runnable[1];

		etUsername.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (pending[0] != null) handler.removeCallbacks(pending[0]);
				final String cand = s.toString().trim().replaceFirst("^@", "").toLowerCase(Locale.US);
				if (cand.length() < 3) {
					tvStatus.setText("Username minimal 3 karakter");
					tvStatus.setTextColor(Color.parseColor("#F87171"));
					return;
				}
				tvStatus.setText("Mengecek ketersediaan username...");
				tvStatus.setTextColor(Color.parseColor("#94A3B8"));
				pending[0] = new Runnable() {
					@Override
					public void run() {
						checkUsernameAvailability(cand, new UsernameCallback() {
							@Override
							public void onResult(boolean available, String msg) {
								if (dialog.isShowing()) {
									tvStatus.setText(msg);
									tvStatus.setTextColor(available ? Color.parseColor("#34D399") : Color.parseColor("#F87171"));
								}
							}
						});
					}
				};
				handler.postDelayed(pending[0], 320);
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
		btnCancel.setPadding((int) (16 * d), (int) (9 * d), (int) (16 * d), (int) (9 * d));
		styleText(btnCancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		final TextView btnSave = new TextView(this);
		btnSave.setText("Simpan");
		btnSave.setTextColor(Color.WHITE);
		btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnSave.setPadding((int) (18 * d), (int) (9 * d), (int) (18 * d), (int) (9 * d));
		LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		sLp.leftMargin = (int) (8 * d);
		btnSave.setLayoutParams(sLp);
		GradientDrawable saveBg = new GradientDrawable();
		saveBg.setColor(Color.parseColor("#2563EB"));
		saveBg.setCornerRadius(100f);
		btnSave.setBackground(saveBg);
		styleText(btnSave);
		btnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String cand = etUsername.getText().toString().trim().replaceFirst("^@", "").toLowerCase(Locale.US);
				btnSave.setEnabled(false);
				btnSave.setText("Mengecek...");
				checkUsernameAvailability(cand, new UsernameCallback() {
					@Override
					public void onResult(boolean available, String msg) {
						btnSave.setEnabled(true);
						btnSave.setText("Simpan");
						if (!available) {
							tvStatus.setText(msg);
							tvStatus.setTextColor(Color.parseColor("#F87171"));
							DongflixToast.show(EditProfileActivity.this, msg);
							return;
						}
						dialog.dismiss();
						saveProfileFieldToFirebase("username", cand);
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

	private interface UsernameCallback {
		void onResult(boolean available, String msg);
	}

	private void checkUsernameAvailability(final String candidate, final UsernameCallback cb) {
		if (candidate.equalsIgnoreCase(currentUsername)) {
			cb.onResult(true, "Username tersedia (username kamu saat ini)");
			return;
		}
		if (!candidate.matches("^[a-z0-9_.]{3,20}$")) {
			cb.onResult(false, "Gunakan 3-20 huruf kecil, angka, titik, atau _");
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
						for (DataSnapshot ch : snap.getChildren()) {
							if (ch.getKey() != null && ch.getKey().equalsIgnoreCase(currentUserKey)) continue;
							Object u = ch.child("username").getValue();
							if (u != null && u.toString().trim().replaceFirst("^@", "").equalsIgnoreCase(candidate)) {
								taken = true;
								break;
							}
						}
					}
					if (taken) cb.onResult(false, "Username @" + candidate + " sudah digunakan");
					else cb.onResult(true, "Username @" + candidate + " tersedia");
				}

				@Override
				public void onCancelled(@NonNull DatabaseError error) {
					cb.onResult(false, "Gagal mengecek username");
				}
			});
	}

	private void openGalleryPicker() {
		try {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			startActivityForResult(Intent.createChooser(intent, "Pilih Foto Profil"), REQ_PICK_IMAGE);
		} catch (Exception e) {
			DongflixToast.show(this, "Tidak dapat membuka galeri");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
			Bitmap loaded = decodeSampledBitmapFromUri(data.getData(), 1280);
			if (loaded == null) {
				DongflixToast.show(this, "Gagal memuat gambar");
				return;
			}
			openInteractiveCropDialog(loaded);
		}
	}

	private Bitmap decodeSampledBitmapFromUri(Uri uri, int maxDim) {
		try {
			InputStream boundsStream = getContentResolver().openInputStream(uri);
			if (boundsStream == null) return null;
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(boundsStream, null, opts);
			boundsStream.close();

			int sample = 1;
			while ((opts.outWidth / sample) > maxDim || (opts.outHeight / sample) > maxDim) {
				sample *= 2;
			}

			InputStream imgStream = getContentResolver().openInputStream(uri);
			if (imgStream == null) return null;
			BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
			decodeOpts.inSampleSize = Math.max(1, sample);
			Bitmap bmp = BitmapFactory.decodeStream(imgStream, null, decodeOpts);
			imgStream.close();
			return bmp;
		} catch (Exception e) {
			return null;
		}
	}

	// Layar/Dialog Crop Interaktif (geser & zoom 1:1) sebelum foto diunggah ke Cloudinary
	private void openInteractiveCropDialog(final Bitmap sourceBitmap) {
		final float d = getResources().getDisplayMetrics().density;
		final Dialog cropDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		cropDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(Color.parseColor("#050608"));
		root.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		FrameLayout header = new FrameLayout(this);
		header.setPadding((int) (16 * d), (int) (16 * d), (int) (16 * d), (int) (12 * d));
		header.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		TextView btnCancelCrop = new TextView(this);
		btnCancelCrop.setText("Batal");
		btnCancelCrop.setTextColor(Color.parseColor("#CBD5E1"));
		btnCancelCrop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		btnCancelCrop.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL
		));
		styleText(btnCancelCrop);
		btnCancelCrop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cropDialog.dismiss();
			}
		});

		TextView tvCropTitle = new TextView(this);
		tvCropTitle.setText("Potong Foto Profil");
		tvCropTitle.setTextColor(Color.WHITE);
		tvCropTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
		tvCropTitle.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
		));
		styleText(tvCropTitle);

		TextView btnConfirmCrop = new TextView(this);
		btnConfirmCrop.setText("Gunakan");
		btnConfirmCrop.setTextColor(Color.WHITE);
		btnConfirmCrop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnConfirmCrop.setPadding((int) (16 * d), (int) (7 * d), (int) (16 * d), (int) (7 * d));
		btnConfirmCrop.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL
		));
		GradientDrawable okBg = new GradientDrawable();
		okBg.setColor(Color.parseColor("#2563EB"));
		okBg.setCornerRadius(100f);
		btnConfirmCrop.setBackground(okBg);
		styleText(btnConfirmCrop);

		header.addView(btnCancelCrop);
		header.addView(tvCropTitle);
		header.addView(btnConfirmCrop);
		root.addView(header);

		final CircleCropView cropView = new CircleCropView(this, sourceBitmap);
		cropView.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
		));
		root.addView(cropView);

		TextView tvHint = new TextView(this);
		tvHint.setText("Geser atau cubit untuk menyesuaikan posisi foto");
		tvHint.setTextColor(Color.parseColor("#94A3B8"));
		tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
		tvHint.setGravity(Gravity.CENTER);
		tvHint.setPadding((int) (16 * d), (int) (14 * d), (int) (16 * d), (int) (24 * d));
		styleText(tvHint);
		root.addView(tvHint);

		btnConfirmCrop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Bitmap cropped = cropView.extractCroppedSquareBitmap(512);
				cropDialog.dismiss();
				if (cropped != null) {
					uploadCroppedBitmapToCloudinary(cropped);
				} else {
					DongflixToast.show(EditProfileActivity.this, "Gagal memotong gambar");
				}
			}
		});

		cropDialog.setContentView(root);
		cropDialog.show();
	}

	private void uploadCroppedBitmapToCloudinary(final Bitmap croppedBitmap) {
		isUploadingPhoto = true;
		if (avatarUploadSpinner != null) avatarUploadSpinner.setVisibility(View.VISIBLE);
		if (tvChangePhotoLabel != null) tvChangePhotoLabel.setText("Mengunggah foto...");

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				String boundary = "----DongflixCrop" + System.currentTimeMillis();
				String lineEnd = "\r\n";
				String twoHyphens = "--";

				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
					byte[] jpegBytes = baos.toByteArray();

					URL url = new URL(CLOUDINARY_UPLOAD_URL);
					conn = (HttpURLConnection) url.openConnection();
					conn.setDoInput(true);
					conn.setDoOutput(true);
					conn.setUseCaches(false);
					conn.setRequestMethod("POST");
					conn.setConnectTimeout(15000);
					conn.setReadTimeout(25000);
					conn.setRequestProperty("Connection", "Keep-Alive");
					conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

					DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"" + lineEnd + lineEnd);
					dos.writeBytes(CLOUDINARY_UPLOAD_PRESET + lineEnd);

					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"avatar.jpg\"" + lineEnd);
					dos.writeBytes("Content-Type: image/jpeg" + lineEnd + lineEnd);
					dos.write(jpegBytes);
					dos.writeBytes(lineEnd);

					dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
					dos.flush();
					dos.close();

					int status = conn.getResponseCode();
					InputStream is = (status >= 200 && status < 300)
						? conn.getInputStream() : conn.getErrorStream();

					StringBuilder sb = new StringBuilder();
					if (is != null) {
						BufferedReader br = new BufferedReader(new InputStreamReader(is));
						String line;
						while ((line = br.readLine()) != null) {
							sb.append(line);
						}
						br.close();
					}

					String resp = sb.toString();
					if (status < 200 || status >= 300) {
						String err = extractJsonField(resp, "message");
						throw new Exception(err.isEmpty() ? ("HTTP " + status) : err);
					}

					String secureUrl = extractJsonField(resp, "secure_url");
					if (secureUrl.isEmpty()) secureUrl = extractJsonField(resp, "url");
					if (secureUrl.isEmpty()) throw new Exception("URL hasil unggah kosong");

					final String finalUrl = secureUrl.startsWith("http://")
						? secureUrl.replace("http://", "https://") : secureUrl;

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (isFinishing() || isDestroyed()) return;
							isUploadingPhoto = false;
							if (avatarUploadSpinner != null) avatarUploadSpinner.setVisibility(View.GONE);
							if (tvChangePhotoLabel != null) tvChangePhotoLabel.setText("Ubah foto");
							saveProfileFieldToFirebase("photoURL", finalUrl);
						}
					});
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (isFinishing() || isDestroyed()) return;
							isUploadingPhoto = false;
							if (avatarUploadSpinner != null) avatarUploadSpinner.setVisibility(View.GONE);
							if (tvChangePhotoLabel != null) tvChangePhotoLabel.setText("Ubah foto");
							DongflixToast.show(
								EditProfileActivity.this,
								"Gagal unggah: " + (e.getMessage() != null ? e.getMessage() : "Kesalahan jaringan")
							);
						}
					});
				} finally {
					if (conn != null) conn.disconnect();
				}
			}
		}).start();
	}

	private String extractJsonField(String json, String field) {
		if (json == null || json.isEmpty() || field == null) return "";
		try {
			Matcher m = Pattern
				.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"")
				.matcher(json);
			if (m.find() && m.group(1) != null) {
				return m.group(1).replace("\\/", "/");
			}
		} catch (Exception ignored) {}
		return "";
	}

	private Bitmap cropToCircleBitmap(Bitmap src, int sizePx) {
		int edge = Math.min(src.getWidth(), src.getHeight());
		Bitmap square = Bitmap.createBitmap(
			src, (src.getWidth() - edge) / 2, (src.getHeight() - edge) / 2, edge, edge
		);
		Bitmap scaled = Bitmap.createScaledBitmap(square, sizePx, sizePx, true);
		Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(out);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Rect rect = new Rect(0, 0, sizePx, sizePx);
		canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(scaled, rect, rect, paint);
		return out;
	}

	private static class CircleCropView extends View {
		private final Bitmap bitmap;
		private final Matrix drawMatrix = new Matrix();
		private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Path clipPath = new Path();
		private final ScaleGestureDetector scaleDetector;

		private float currentScale = 1f;
		private float minScale = 1f;
		private float translateX = 0f;
		private float translateY = 0f;
		private float lastTouchX = 0f;
		private float lastTouchY = 0f;
		private boolean initialized = false;

		CircleCropView(Context context, Bitmap bitmap) {
			super(context);
			this.bitmap = bitmap;
			overlayPaint.setColor(Color.parseColor("#B3000000"));
			borderPaint.setColor(Color.parseColor("#38BDF8"));
			borderPaint.setStyle(Paint.Style.STROKE);
			borderPaint.setStrokeWidth(2f * context.getResources().getDisplayMetrics().density);

			scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
				@Override
				public boolean onScale(ScaleGestureDetector detector) {
					float nextScale = currentScale * detector.getScaleFactor();
					nextScale = Math.max(minScale, Math.min(minScale * 4f, nextScale));
					float factor = nextScale / currentScale;
					currentScale = nextScale;
					translateX = detector.getFocusX() - factor * (detector.getFocusX() - translateX);
					translateY = detector.getFocusY() - factor * (detector.getFocusY() - translateY);
					clampTranslation();
					invalidate();
					return true;
				}
			});
		}

		private float getCropRadius() {
			return Math.min(getWidth(), getHeight()) * 0.40f;
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			if (!initialized && w > 0 && h > 0 && bitmap != null) {
				initialized = true;
				float cropDiameter = getCropRadius() * 2f;
				float sx = cropDiameter / bitmap.getWidth();
				float sy = cropDiameter / bitmap.getHeight();
				minScale = Math.max(sx, sy);
				currentScale = minScale;
				translateX = (w - bitmap.getWidth() * currentScale) / 2f;
				translateY = (h - bitmap.getHeight() * currentScale) / 2f;
				clampTranslation();
			}
		}

		private void clampTranslation() {
			if (bitmap == null) return;
			float r = getCropRadius();
			float cx = getWidth() / 2f;
			float cy = getHeight() / 2f;
			float imgW = bitmap.getWidth() * currentScale;
			float imgH = bitmap.getHeight() * currentScale;

			float maxTx = cx - r;
			float minTx = (cx + r) - imgW;
			float maxTy = cy - r;
			float minTy = (cy + r) - imgH;

			translateX = Math.max(minTx, Math.min(maxTx, translateX));
			translateY = Math.max(minTy, Math.min(maxTy, translateY));
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			scaleDetector.onTouchEvent(event);
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					lastTouchX = event.getX();
					lastTouchY = event.getY();
					break;
				case MotionEvent.ACTION_MOVE:
					if (!scaleDetector.isInProgress()) {
						float dx = event.getX() - lastTouchX;
						float dy = event.getY() - lastTouchY;
						translateX += dx;
						translateY += dy;
						clampTranslation();
						invalidate();
					}
					lastTouchX = event.getX();
					lastTouchY = event.getY();
					break;
			}
			return true;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			if (bitmap == null) return;

			drawMatrix.reset();
			drawMatrix.postScale(currentScale, currentScale);
			drawMatrix.postTranslate(translateX, translateY);
			canvas.drawBitmap(bitmap, drawMatrix, bitmapPaint);

			float cx = getWidth() / 2f;
			float cy = getHeight() / 2f;
			float r = getCropRadius();

			canvas.save();
			clipPath.reset();
			clipPath.addCircle(cx, cy, r, Path.Direction.CW);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				canvas.clipOutPath(clipPath);
			} else {
				canvas.clipPath(clipPath, android.graphics.Region.Op.DIFFERENCE);
			}
			canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
			canvas.restore();

			canvas.drawCircle(cx, cy, r, borderPaint);
		}

		Bitmap extractCroppedSquareBitmap(int outputSize) {
			if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) return null;
			float r = getCropRadius();
			float cx = getWidth() / 2f;
			float cy = getHeight() / 2f;

			float leftInBmp = (cx - r - translateX) / currentScale;
			float topInBmp = (cy - r - translateY) / currentScale;
			float sideInBmp = (2f * r) / currentScale;

			int x = Math.max(0, Math.round(leftInBmp));
			int y = Math.max(0, Math.round(topInBmp));
			int side = Math.min(Math.round(sideInBmp), Math.min(bitmap.getWidth() - x, bitmap.getHeight() - y));
			if (side <= 0) return null;

			Bitmap sub = Bitmap.createBitmap(bitmap, x, y, side, side);
			return Bitmap.createScaledBitmap(sub, outputSize, outputSize, true);
		}
	}

	@Override
	protected void onDestroy() {
		DongflixToast.dismissCurrent();
		super.onDestroy();
	}
}
