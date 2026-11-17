package com.dongflix;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.dongflix.databinding.ActivityMainBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private List<DonghuaModel> animeList = new ArrayList<>();
    private GridAdapter gridAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        gridAdapter = new GridAdapter(this, animeList);
        binding.gridview1.setAdapter(gridAdapter);

        binding.gridview1.setOnItemClickListener((parent, view, position, id) -> {
            DonghuaModel selected = animeList.get(position);
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("url", selected.getUrl());
            startActivity(intent);
        });

        fetchDonghuaData();
    }

    private void fetchDonghuaData() {
        String apiUrl = "https://dongflix-api1.zahrulmo01.workers.dev/";
        Request request = new Request.Builder().url(apiUrl).get().build();

        NetworkClient.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Gagal terhubung ke API", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<DonghuaModel>>(){}.getType();
                    List<DonghuaModel> fetchedList = gson.fromJson(jsonResponse, listType);

                    runOnUiThread(() -> {
                        animeList.clear();
                        if (fetchedList != null) {
                            animeList.addAll(fetchedList);
                            if (!animeList.isEmpty()) {
                                binding.textHeroTitle.setText(animeList.get(0).getTitle());
                                binding.textHeroDesc.setText(animeList.get(0).getSynopsis());
                                com.bumptech.glide.Glide.with(MainActivity.this)
                                        .load(animeList.get(0).getCover())
                                        .into(binding.imgHeroBanner);
                            }
                        }
                        gridAdapter.notifyDataSetChanged();
                    });
                }
            }
        });
    }
}
