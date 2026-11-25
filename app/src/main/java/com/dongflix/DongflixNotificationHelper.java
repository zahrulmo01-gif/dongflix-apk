package com.dongflix;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DongflixNotificationHelper {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";
	private static final String CHANNEL_ID = "dongflix_chat_alerts";
	private static final String CHANNEL_NAME = "Notifikasi Dongflix";
	private static final String DOT_VIEW_TAG = "notif_unread_dot";

	private static DatabaseReference activeNotifRef;
	private static ValueEventListener activeNotifListener;
	private static final Set<String> shownSystemNotifIds = new HashSet<>();
	private static boolean initialSnapshotLoaded = false;

	public static void attachUnreadBadgeAndSystemPush(final Activity activity, final FrameLayout bellContainer) {
		if (activity == null || bellContainer == null) return;

		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null || user.getEmail() == null) return;

		final View dotView = ensureDotView(activity, bellContainer);
		String userKey = user.getEmail().trim().toLowerCase(Locale.US).replace(".", ",");

		detachListener();
		initialSnapshotLoaded = false;

		activeNotifRef = FirebaseDatabase.getInstance(FIREBASE_DB_URL)
			.getReference("notifications")
			.child(userKey);

		activeNotifListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (activity.isFinishing() || activity.isDestroyed()) return;

				boolean hasUnread = false;
				if (snapshot.exists()) {
					for (DataSnapshot child : snapshot.getChildren()) {
						String key = child.getKey() != null ? child.getKey() : "";
						Object readObj = child.child("read").getValue();
						boolean isRead = Boolean.TRUE.equals(readObj);

						if (!isRead) {
							hasUnread = true;
							// Kirim notif ke HP untuk notifikasi baru yang masuk setelah listener aktif
							if (initialSnapshotLoaded && !key.isEmpty() && !shownSystemNotifIds.contains(key)) {
								shownSystemNotifIds.add(key);
								String actorName = child.child("actorName").getValue() != null
									? child.child("actorName").getValue().toString() : "Pengguna";
								String type = child.child("type").getValue() != null
									? child.child("type").getValue().toString() : "mention";
								String snippet = child.child("snippet").getValue() != null
									? child.child("snippet").getValue().toString() : "";

								String bodyText = "reply".equalsIgnoreCase(type)
									? actorName + " membalas di Live Chat: " + snippet
									: actorName + " menyebut di Live Chat: " + snippet;

								pushPhoneNotification(activity, "Notifikasi Dongflix", bodyText, key.hashCode());
							} else if (!initialSnapshotLoaded && !key.isEmpty()) {
								shownSystemNotifIds.add(key);
							}
						}
					}
				}
				initialSnapshotLoaded = true;
				dotView.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		};

		activeNotifRef.limitToLast(40).addValueEventListener(activeNotifListener);
	}

	public static void detachListener() {
		if (activeNotifRef != null && activeNotifListener != null) {
			activeNotifRef.removeEventListener(activeNotifListener);
			activeNotifListener = null;
			activeNotifRef = null;
		}
	}

	private static View ensureDotView(Context ctx, FrameLayout container) {
		View existing = container.findViewWithTag(DOT_VIEW_TAG);
		if (existing != null) return existing;

		float d = ctx.getResources().getDisplayMetrics().density;
		int dotSize = (int) (8.5f * d);

		View dot = new View(ctx);
		dot.setTag(DOT_VIEW_TAG);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dotSize, dotSize, Gravity.TOP | Gravity.END);
		lp.topMargin = (int) (7 * d);
		lp.rightMargin = (int) (7 * d);
		dot.setLayoutParams(lp);

		GradientDrawable circle = new GradientDrawable();
		circle.setShape(GradientDrawable.OVAL);
		circle.setColor(Color.parseColor("#EF4444"));
		circle.setStroke(Math.max(1, (int) (1.2f * d)), Color.parseColor("#0F172A"));
		dot.setBackground(circle);
		dot.setVisibility(View.GONE);

		container.addView(dot);
		return dot;
	}

	public static void pushPhoneNotification(Context context, String title, String message, int notifId) {
		if (context == null) return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
			&& context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			return;
		}

		try {
			NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (nm == null) return;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(
					CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
				);
				channel.setDescription("Notifikasi balasan dan mention Live Chat Dongflix");
				channel.enableLights(true);
				channel.setLightColor(Color.parseColor("#38BDF8"));
				channel.enableVibration(true);
				nm.createNotificationChannel(channel);
			}

			Intent intent = new Intent(context, NotificationActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			int flags = PendingIntent.FLAG_UPDATE_CURRENT;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				flags |= PendingIntent.FLAG_IMMUTABLE;
			}
			PendingIntent pi = PendingIntent.getActivity(context, notifId, intent, flags);

			int smallIcon = context.getResources().getIdentifier("ic_notifications", "drawable", context.getPackageName());
			if (smallIcon <= 0) {
				smallIcon = R.drawable.logo_dongflix;
			}

			NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(smallIcon)
				.setContentTitle(title)
				.setContentText(message)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(message))
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setAutoCancel(true)
				.setContentIntent(pi);

			nm.notify(notifId, builder.build());
		} catch (Throwable ignored) {}
	}
}
