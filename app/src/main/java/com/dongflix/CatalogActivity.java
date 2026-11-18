package com.dongflix;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashMap;

public class CatalogActivity extends AppCompatActivity {

    private GridView gridCatalog;
    private TextView btnLoadMore, btnBack;

    private RequestNetwork req;
    private RequestNetwork.RequestListener reqListener;

    private ArrayList<HashMap<String, Object>> allDataList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> displayedList = new ArrayList<>();
    private CatalogAdapter adapter;

    private int currentLimit = 18;
    private final int PAGE_SIZE = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        gridCatalog = findViewById(R.id.grid_catalog);
        btnLoadMore = findViewById(R.id.btn_load_more);
        btnBack = findViewById(R.id.btn_catalog_back);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setShape(GradientDrawable.RECTANGLE);
        btnBg.setColor(Color.parseColor("#1E293B"));
        btnBg.setCornerRadius(100f);
        btnBg.setStroke(2, Color.parseColor("#3B82F6"));
        btnLoadMore.setBackground(btnBg);
        btnLoadMore.setVisibility(View.GONE);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        gridCatalog.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });

        gridCatalog.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (displayedList != null && position < displayedList.size()) {
                    HashMap<String, Object> item = displayedList.get(position);
                    if (item.containsKey("is_loading")) return;

                    Intent intent = new Intent(CatalogActivity.this, DetailActivity.class);
                    intent.putExtra("url", item.containsKey("url") ? item.get("url").toString() : "");
                    intent.putExtra("title", item.containsKey("title") ? item.get("title").toString() : "");
                    intent.putExtra("image", item.containsKey("cover") ? item.get("cover").toString() : "");
                    startActivity(intent);
                }
            }
        });

        btnLoadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLimit += PAGE_SIZE;
                updateDisplayedList();
            }
        });

        ArrayList<HashMap<String, Object>> dummy = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            HashMap<String, Object> m = new HashMap<>();
            m.put("is_loading", true);
            dummy.add(m);
        }
        adapter = new CatalogAdapter(dummy);
        gridCatalog.setAdapter(adapter);
        adjustGridHeight(gridCatalog, 3);

        req = new RequestNetwork(this);
        reqListener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> headers) {
                ArrayList<HashMap<String, Object>> parsed = new Gson().fromJson(response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
                if (parsed != null) {
                    allDataList = parsed;
                    updateDisplayedList();
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
            }
        };

        req.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/", "", reqListener);
    }

    private void updateDisplayedList() {
        int end = Math.min(currentLimit, allDataList.size());
        displayedList = new ArrayList<>(allDataList.subList(0, end));
        adapter = new CatalogAdapter(displayedList);
        gridCatalog.setAdapter(adapter);
        adjustGridHeight(gridCatalog, 3);

        if (end >= allDataList.size()) {
            btnLoadMore.setVisibility(View.GONE);
        } else {
            btnLoadMore.setVisibility(View.VISIBLE);
        }
    }

    private void adjustGridHeight(GridView gridView, int columns) {
        if (gridView.getAdapter() == null) return;
        int items = gridView.getAdapter().getCount();
        int rows = (int) Math.ceil((double) items / columns);
        float density = getResources().getDisplayMetrics().density;

        int itemHeightPx = (int) (222 * density);
        int verticalSpacingPx = (int) (12 * density);

        ViewGroup.LayoutParams params = gridView.getLayoutParams();
        params.height = (itemHeightPx * rows) + (verticalSpacingPx * Math.max(0, rows - 1)) + (int) (20 * density);
        gridView.setLayoutParams(params);
        gridView.requestLayout();
    }

    public class CatalogAdapter extends BaseAdapter {
        ArrayList<HashMap<String, Object>> _data;
        public CatalogAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }

        @Override public int getCount() { return _data.size(); }
        @Override public HashMap<String, Object> getItem(int index) { return _data.get(index); }
        @Override public long getItemId(int index) { return index; }

        @Override
        public View getView(int position, View v, ViewGroup container) {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.custom_anime, container, false);

            TextView textTitle = view.findViewById(R.id.text_title);
            TextView textStatus = view.findViewById(R.id.text_status);
            TextView textEpisode = view.findViewById(R.id.text_episode);
            ImageView imgCover = view.findViewById(R.id.img_cover);
            View view2 = view.findViewById(R.id.view2);

            HashMap<String, Object> item = _data.get(position);

            if (item.containsKey("is_loading")) {
                textTitle.setText("");
                textTitle.setBackground(new GradientDrawable() {{ setCornerRadius(20); setColor(0xFF111111); }});
                textStatus.setBackgroundColor(0xFF111111);
                textEpisode.setBackgroundColor(0xFF111111);
                imgCover.setBackgroundColor(0xFF111111);
                view2.setVisibility(View.GONE);
            } else {
                imgCover.setBackgroundColor(Color.TRANSPARENT);
                textTitle.setBackgroundColor(Color.TRANSPARENT);
                textEpisode.setBackgroundColor(Color.TRANSPARENT);
                view2.setVisibility(View.VISIBLE);

                textStatus.setBackground(new GradientDrawable() {{ setCornerRadius(50); setColor(0xFF3B82F6); }});

                GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[] { Color.parseColor("#F2000000"), Color.parseColor("#80000000"), Color.TRANSPARENT }
                );
                view2.setBackground(gradient);

                textTitle.setText(item.get("title") != null ? item.get("title").toString() : "");
                textStatus.setText(item.get("status") != null ? item.get("status").toString() : "");
                textEpisode.setText(item.get("episode") != null ? item.get("episode").toString() : "");

                String cover = item.get("cover") != null ? item.get("cover").toString().trim() : "";
                if (cover.contains("i.ibb.co.com")) cover = cover.replace("i.ibb.co.com", "i.ibb.co");
                if (cover.startsWith("http://")) cover = cover.replace("http://", "https://");

                Glide.with(getApplicationContext()).load(Uri.parse(cover)).into(imgCover);
            }
            return view;
        }
    }
}
