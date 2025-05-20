package com.example.weatherappnewest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private TextView cityNameText, temeperatureText, humidityText, descriptionText, windText, pollutionText;
    private ImageView weatherIcon;
    private Button refreshButton;
    private EditText cityNameInput;

    private static final String API_KEY = "faad48424d4262d7d8d3d04e2e5367bd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cityNameText = findViewById(R.id.cityNameText);
        temeperatureText = findViewById(R.id.temperatureText);
        humidityText = findViewById(R.id.humidityText);
        windText = findViewById(R.id.windText);
        descriptionText = findViewById(R.id.descriptionText);
        weatherIcon = findViewById(R.id.weatherIcon);
        refreshButton = findViewById(R.id.fetchWeatherButton);
        cityNameInput = findViewById(R.id.cityNameInput);
        pollutionText = findViewById(R.id.pollutionText);

        refreshButton.setOnClickListener(view -> {
            String cityName = cityNameInput.getText().toString();
            if (!cityName.isEmpty()) {
                fetchWeatherData(cityName);
            } else {
                cityNameInput.setError("Please enter a city name");
            }
        });

        fetchWeatherData("Ljubljana");
    }

    private void fetchWeatherData(String cityName) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&appid=" + API_KEY + "&units=metric";
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            try {
                Response response = client.newCall(request).execute();
                String result = response.body().string();
                runOnUiThread(() -> updateUI(result));

                // Extract coordinates for pollution
                JSONObject jsonObject = new JSONObject(result);
                JSONObject coord = jsonObject.getJSONObject("coord");
                double lat = coord.getDouble("lat");
                double lon = coord.getDouble("lon");

                fetchPollutionData(lat, lon);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void fetchPollutionData(double lat, double lon) {
        String url = "https://api.openweathermap.org/data/2.5/air_pollution?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY;
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            try {
                Response response = client.newCall(request).execute();
                String result = response.body().string();
                runOnUiThread(() -> updatePollutionUI(result));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void updateUI(String result) {
        if (result != null) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONObject main = jsonObject.getJSONObject("main");
                double temperature = main.getDouble("temp");
                double humidity = main.getDouble("humidity");
                double windSpeed = jsonObject.getJSONObject("wind").getDouble("speed");
                String description = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
                String iconCode = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");
                String resourceName = "ic_" + iconCode;
                int resId = getResources().getIdentifier(resourceName, "drawable", getPackageName());

                weatherIcon.setImageResource(resId);
                cityNameText.setText(jsonObject.getString("name"));
                temeperatureText.setText(String.format("%.0f°", temperature));
                humidityText.setText(String.format("%.0f%%", humidity));
                windText.setText(String.format("%.0f km/h", windSpeed));
                descriptionText.setText(description);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void updatePollutionUI(String result) {
        if (result != null) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray list = jsonObject.getJSONArray("list");
                JSONObject pollution = list.getJSONObject(0);
                JSONObject components = pollution.getJSONObject("components");
                double pm2_5 = components.getDouble("pm2_5");

                String quality;
                if (pm2_5 < 12) {
                    quality = "Good";
                } else if (pm2_5 < 35) {
                    quality = "Moderate";
                } else if (pm2_5 < 55) {
                    quality = "Unhealthy";
                } else {
                    quality = "Very Unhealthy";
                }

                pollutionText.setText(String.format("%.1f µg/m³ (%s)", pm2_5, quality));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
