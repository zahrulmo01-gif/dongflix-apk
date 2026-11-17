package com.dongflix;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashMap;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
	
	private String cover = "";
	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> listPopularMap = new ArrayList<>();
	private Intent i = new Intent();
	
	// Deklarasi View manual untuk layout Sketchware
	private View mainScrollView, layoutSearch, layoutFavorite, layoutProfile;
	private ImageView imgHome, imgSearch, imgFavorite, imgProfile;
	private GridView gridview1;
	private RecyclerView recyclerPopular, listPopular;
	private TextView textHeroTitle, textHeroDesc, textview77, textSeeAll, textUsername;
	private ImageView imgHeroBanner, imgPlayIcon;
	private LinearLayout btnHeroPlay, indicatorCapsule;
	private View dot0, dot1, dot2;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_main);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		mainScrollView = findViewById(R.id.main_scroll_view);
		layoutSearch = findViewById(R.id.layout_search);
		layoutFavorite = findViewById(R.id.layout_favorite);
		layoutProfile = findViewById(R.id.layout_profile);
		
		imgHome = findViewById(R.id.img_home);
		imgSearch = findViewById(R.id.img_search);
		imgFavorite = findViewById(R.id.img_favorite);
		imgProfile = findViewById(R.id.img_profile);
		
		gridview1 = findViewById(R.id.gridview1);
		recyclerPopular = findViewById(R.id.recyclerPopular);
		listPopular = findViewById(R.id.listPopular);
		
		textHeroTitle = findViewById(R.id.text_hero_title);
		textHeroDesc = findViewById(R.id.text_hero_desc);
		imgHeroBanner = findViewById(R.id.img_hero_banner);
		btnHeroPlay = findViewById(R.id.btn_hero_play);
		imgPlayIcon = findViewById(R.id.img_play_icon);
		indicatorCapsule = findViewById(R.id.indicator_capsule);
		dot0 = findViewById(R.id.dot_0);
		dot1 = findViewById(R.id.dot_1);
		dot2 = findViewById(R.id.dot_2);
		
		if (gridview1 != null) {
			gridview1.setOnItemClickListener((_param1, _param2, _param3, _param4) -> {
				final int _position = _param3;
				if (listMap != null && _position < listMap.size()) {
					HashMap<String, Object> selectedData = listMap.get(_position);
					String animeUrl = selectedData.containsKey("url") ? selectedData.get("url").toString() : "";
					String animeTitle = selectedData.containsKey("title") ? selectedData.get("title").toString() : "";
					String animeCover = selectedData.containsKey("image") ? selectedData.get("image").toString() : "";
					
					Intent intent = new Intent(MainActivity.this, DetailActivity.class);
					intent.putExtra("url", animeUrl);
					intent.putExtra("title", animeTitle);
					intent.putExtra("image", animeCover);
					startActivity(intent);
				}
			});
		}
	}
	
	private void initializeLogic() {
		_UI();
		_Nav();
		
		// Setup Skeleton Loading Dummy
		ArrayList<HashMap<String, Object>> dummyList = new ArrayList<>();
		for (int j = 0; j < 12; j++) {
			HashMap<String, Object> map = new HashMap<>();
			map.put("is_loading", true);
			dummyList.add(map);
		}
		
		if (gridview1 != null) {
			gridview1.setAdapter(new Gridview1Adapter(dummyList));
			gridview1.setOnTouchListener((v, event) -> event.getAction() == MotionEvent.ACTION_MOVE);
		}
		
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
		}
		
		fetchDataFromApi();
	}
	
	private void fetchDataFromApi() {
		String apiUrl = "https://dongflix-api1.zahrulmo01.workers.dev/";
		Request request = new Request.Builder().url(apiUrl).get().build();
		
		NetworkClient.getInstance().newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				runOnUiThread(() -> Toast.makeText(MainActivity.this, "Gagal memuat data API", Toast.LENGTH_SHORT).show());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (response.isSuccessful() && response.body() != null) {
					String responseBody = response.body().string();
					listMap = new Gson().fromJson(responseBody, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
					
					if (listMap != null && listMap.size() > 12) {
						listMap = new ArrayList<>(listMap.subList(0, 12));
					}
					
					runOnUiThread(() -> {
						if (gridview1 != null) {
							gridview1.setAdapter(new Gridview1Adapter(listMap));
						}
						if (recyclerPopular != null) {
							recyclerPopular.setAdapter(new RecyclerPopularAdapter(listMap));
						}
						if (listPopular != null) {
							listPopular.setAdapter(new ListPopularAdapter(listMap));
						}
					});
				}
			}
		});
	}
	
	public void _UI() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			android.view.Window window = getWindow();
			window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
	}
	
	public void _Nav() {
		final View pageHome = findViewById(R.id.main_scroll_view);
		final View pageSearch = findViewById(R.id.layout_search);
		final View pageFavorite = findViewById(R.id.layout_favorite);
		final View pageProfile = findViewById(R.id.layout_profile);
		
		final ImageView imgHome = findViewById(R.id.img_home);
		final ImageView imgSearch = findViewById(R.id.img_search);
		final ImageView imgFavorite = findViewById(R.id.img_favorite);
		final ImageView imgProfile = findViewById(R.id.img_profile);
		
		final int colorActive = Color.parseColor("#3B82F6");
		final int colorInactive = Color.parseColor("#64748B");
		
		View.OnClickListener tabClickListener = v -> {
			int id = v.getId();
			
			if (pageHome != null) pageHome.setVisibility(View.GONE);
			if (pageSearch != null) pageSearch.setVisibility(View.GONE);
			if (pageFavorite != null) pageFavorite.setVisibility(View.GONE);
			if (pageProfile != null) pageProfile.setVisibility(View.GONE);
			
			if (imgHome != null) imgHome.setColorFilter(colorInactive);
			if (imgSearch != null) imgSearch.setColorFilter(colorInactive);
			if (imgFavorite != null) imgFavorite.setColorFilter(colorInactive);
			if (imgProfile != null) imgProfile.setColorFilter(colorInactive);
			
			if (imgHome != null && id == imgHome.getId()) {
				if (pageHome != null) pageHome.setVisibility(View.VISIBLE);
				imgHome.setColorFilter(colorActive);
			} else if (imgSearch != null && id == imgSearch.getId()) {
				if (pageSearch != null) pageSearch.setVisibility(View.VISIBLE);
				imgSearch.setColorFilter(colorActive);
			} else if (imgFavorite != null && id == imgFavorite.getId()) {
				if (pageFavorite != null) pageFavorite.setVisibility(View.VISIBLE);
				imgFavorite.setColorFilter(colorActive);
			} else if (imgProfile != null && id == imgProfile.getId()) {
				if (pageProfile != null) pageProfile.setVisibility(View.VISIBLE);
				imgProfile.setColorFilter(colorActive);
			}
		};
		
		if (imgHome != null) imgHome.setOnClickListener(tabClickListener);
		if (imgSearch != null) imgSearch.setOnClickListener(tabClickListener);
		if (imgFavorite != null) imgFavorite.setOnClickListener(tabClickListener);
		if (imgProfile != null) imgProfile.setOnClickListener(tabClickListener);
		
		if (imgHome != null) imgHome.performClick();
	}

	// --- ADAPTER KELAS PENDUKUNG ---
	public class Gridview1Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		public Gridview1Adapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		@Override public int getCount() { return _data.size(); }
		@Override public Object getItem(int _index) { return _data.get(_index); }
		@Override public long getItemId(int _index) { return _index; }
		@Override public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater inflater = getLayoutInflater();
			View _view = inflater.inflate(R.layout.custom_anime, _container, false);
			
			TextView textTitle = _view.findViewById(R.id.text_title);
			TextView textStatus = _view.findViewById(R.id.text_status);
			TextView textEpisode = _view.findViewById(R.id.text_episode);
			ImageView imgCover = _view.findViewById(R.id.img_cover);
			
			HashMap<String, Object> item = _data.get(_position);
			if (!item.containsKey("is_loading")) {
				if (textTitle != null) textTitle.setText(item.get("title") != null ? item.get("title").toString() : "");
				if (textStatus != null) textStatus.setText(item.get("status") != null ? item.get("status").toString() : "");
				if (textEpisode != null) textEpisode.setText(item.get("episode") != null ? item.get("episode").toString() : "");
				
				String coverUrl = item.get("cover") != null ? item.get("cover").toString() : "";
				Glide.with(getApplicationContext()).load(Uri.parse(coverUrl)).into(imgCover);
			}
			return _view;
		}
	}

	public class RecyclerPopularAdapter extends RecyclerView.Adapter<RecyclerPopularAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> _data;
		public RecyclerPopularAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		@Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popular_donghua, parent, false);
			return new ViewHolder(v);
		}
		@Override public void onBindViewHolder(ViewHolder holder, int position) {
			// Binding data popular
		}
		@Override public int getItemCount() { return _data.size(); }
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) { super(v); }
		}
	}

	public class ListPopularAdapter extends RecyclerView.Adapter<ListPopularAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> _data;
		public ListPopularAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		@Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popular_donghua, parent, false);
			return new ViewHolder(v);
		}
		@Override public void onBindViewHolder(ViewHolder holder, int position) {
			// Binding data list popular
		}
		@Override public int getItemCount() { return _data.size(); }
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) { super(v); }
		}
	}
}
