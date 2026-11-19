package com.dongflix;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

	private FrameLayout logoBadgeBox, btnSubmitAuth;
	private LinearLayout tabSwitchContainer, authFormCard, groupRegisterFields;
	private TextView textAuthHeading, textAuthSub, tabModeLogin, tabModeRegister, textSubmitAuth, textToggleHint;
	private EditText inputRegName, inputRegUsername, inputAuthEmail, inputAuthPassword;
	private ProgressBar progressAuth;

	private boolean isRegisterMode = false;
	private boolean isProcessing = false;

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
		initViews();
		setupStyle();
		applyCustomFonts();
		setupListeners();
	}

	private void initViews() {
		logoBadgeBox = findViewById(R.id.logo_badge_box);
		tabSwitchContainer = findViewById(R.id.tab_switch_container);
		authFormCard = findViewById(R.id.auth_form_card);
		groupRegisterFields = findViewById(R.id.group_register_fields);

		textAuthHeading = findViewById(R.id.text_auth_heading);
		textAuthSub = findViewById(R.id.text_auth_sub);
		tabModeLogin = findViewById(R.id.tab_mode_login);
		tabModeRegister = findViewById(R.id.tab_mode_register);
		textSubmitAuth = findViewById(R.id.text_submit_auth);
		textToggleHint = findViewById(R.id.text_toggle_hint);

		inputRegName = findViewById(R.id.input_reg_name);
		inputRegUsername = findViewById(R.id.input_reg_username);
		inputAuthEmail = findViewById(R.id.input_auth_email);
		inputAuthPassword = findViewById(R.id.input_auth_password);

		btnSubmitAuth = findViewById(R.id.btn_submit_auth);
		progressAuth = findViewById(R.id.progress_auth);
	}

	private void setupStyle() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(Color.parseColor("#000000"));
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
			switchBg.setStroke((int)(1 * d), Color.parseColor("#1F2430"));
			tabSwitchContainer.setBackground(switchBg);
		}

		if (authFormCard != null) {
			GradientDrawable cardBg = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{Color.parseColor("#12141A"), Color.parseColor("#0B0D11")}
			);
			cardBg.setCornerRadius(20 * d);
			cardBg.setStroke((int)(1 * d), Color.parseColor("#24FFFFFF"));
			authFormCard.setBackground(cardBg);
		}

		EditText[] inputs = {inputRegName, inputRegUsername, inputAuthEmail, inputAuthPassword};
		for (EditText et : inputs) {
			if (et != null) {
				GradientDrawable inputBg = new GradientDrawable();
				inputBg.setColor(Color.parseColor("#181B23"));
				inputBg.setCornerRadius(12 * d);
				inputBg.setStroke((int)(1 * d), Color.parseColor("#26FFFFFF"));
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
			if (textAuthHeading != null) textAuthHeading.setText("Buat Akun Dongflix");
			if (textAuthSub != null) textAuthSub.setText("Daftar dengan email dan password untuk menyimpan favorit & riwayat tontonan kamu.");
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
			if (textAuthHeading != null) textAuthHeading.setText("Selamat Datang Kembali");
			if (textAuthSub != null) textAuthSub.setText("Masuk menggunakan email dan password akun Dongflix kamu.");
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
		final String email = inputAuthEmail != null ? inputAuthEmail.getText().toString().trim().toLowerCase() : "";
		final String password = inputAuthPassword != null ? inputAuthPassword.getText().toString() : "";

		if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			Toast.makeText(this, "Masukkan alamat email yang valid", Toast.LENGTH_SHORT).show();
			return;
		}

		if (password.length() < 6) {
			Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
			return;
		}

		final String regName = inputRegName != null ? inputRegName.getText().toString().trim() : "";
		final String regUsername = inputRegUsername != null ? inputRegUsername.getText().toString().trim().toLowerCase() : "";

		if (isRegisterMode) {
			if (regName.isEmpty()) {
				Toast.makeText(this, "Nama tampilan tidak boleh kosong", Toast.LENGTH_SHORT).show();
				return;
			}
			if (regUsername.isEmpty()) {
				Toast.makeText(this, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show();
				return;
			}
		}

		setLoadingState(true);

		if (!isRegisterMode) {
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
		} else {
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

						String cleanUsername = regUsername.replaceAll("[^a-z0-9_]", "");
						if (cleanUsername.isEmpty()) cleanUsername = email.split("@")[0];
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
}
