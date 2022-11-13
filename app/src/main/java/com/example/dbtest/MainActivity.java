package com.example.dbtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.MultipartPathOverlay;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    GpsTracker gpsTracker;

    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    MapView map;
    Marker marker;

    LinearLayout infoLayout, mainLayout;
    ImageView daynightBtn;
    MultipartPathOverlay mpath;
    boolean[] clicked;
    boolean[] nclicked;
    boolean daynight = true;
    double latitude, longitude;
    TextView location_t, place, address, phonenum, worktime;
    Typeface customFont; //setTypeface(customFont, Typeface.BOLD);로 설정 가능
    ArrayList<Pharmacy> pharmacy = new ArrayList<Pharmacy>();
    ArrayList<Info> info = new ArrayList<Info>();
    ArrayList<Users> users = new ArrayList<Users>();
    ArrayList<Marker> markers = new ArrayList<Marker>();
    ArrayList<Marker> nmarkers = new ArrayList<Marker>();
    ArrayList<Pharmacy> npharmacy = new ArrayList<Pharmacy>();
    ArrayList<Info> ninfo = new ArrayList<Info>();
    ArrayList<Integer> list = new ArrayList<Integer>();
    ArrayList<Integer> nlist = new ArrayList<Integer>();
    int fin = 0; int nfin = 0;
    int id = 0;
    double ulatitude, ulongitude;
    Long distance;

    DatabaseHelper dbHelper;
    SQLiteDatabase db = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        customFont = ResourcesCompat.getFont(this, R.font.font_regular);
        // 폰트 가져오기

        loadData();
        // db 데이터 불러오기

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, 100);
        location_t = findViewById(R.id.location_t);
        setLocation(); location_t.setText(getAddress());
        location_t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLocation();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        location_t.setText(getAddress());
                    }
                });

            }
        });
        // 상단 현재 사용자 주소 표시

        daynightBtn = findViewById(R.id.daynightBtn);

        map = findViewById(R.id.map);
        map.onCreate(savedInstanceState);
        map.getMapAsync(this);

        infoLayout = findViewById(R.id.InfoLayout);
        mainLayout = findViewById(R.id.MainLayout);
        place = findViewById(R.id.name); place.setTypeface(customFont, Typeface.BOLD);
        address = findViewById(R.id.address); address.setTypeface(customFont, Typeface.NORMAL);
        phonenum = findViewById(R.id.phonenum); phonenum.setTypeface(customFont, Typeface.NORMAL);
        worktime = findViewById(R.id.worktime); worktime.setTypeface(customFont, Typeface.NORMAL);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM users");
        dbHelper.close();
    }

    @Override
    protected void onDestroy() {
        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM users");
        dbHelper.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
                Log.d( "권한", "됨");
            }
            else Log.d( "권한", "안됨");
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        naverMap.setMapType(NaverMap.MapType.Navi);
        // 기본 설정

        daynightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (daynight == true) {
                    daynightBtn.setBackgroundResource(R.drawable.moon);
                    naverMap.setNightModeEnabled(true);
                    daynight = false;
                    map.setLayoutParams(new LinearLayout.LayoutParams(1430, 2450));

                    for(int i = 0; i < pharmacy.size(); i++){ markers.get(i).setMap(null); }
                    setnInfo();
                } // 야간 버튼 클릭

                else if (daynight == false){
                    daynightBtn.setBackgroundResource(R.drawable.sun);
                    naverMap.setNightModeEnabled(false);
                    daynight = true;
                    map.setLayoutParams(new LinearLayout.LayoutParams(1430, 2450));

                    for(int i = 0; i < npharmacy.size(); i++) { nmarkers.get(i).setMap(null); }
                    setInfo();
                } // 주간 버튼 클릭
            }
        });
        // 주간, 야간 설정

        UiSettings uiSettings= naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);
        // 현재위치 불러오는 UI 띄우기

        if(naverMap.isNightModeEnabled() == false){ setInfo(); }
        // 초기 마커 구현
    }

    private void loadData() {
        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM pharmacy", null);
        if (c.moveToFirst()) {
            while (c.moveToNext()) {
                pharmacy.add(new Pharmacy(c.getString(0), c.getString(1), c.getString(2),c.getString(3)));
            }
        } else pharmacy = null;
        if(c.moveToFirst())
            c.close();

        Cursor c2 = db.rawQuery("SELECT * FROM info", null);
        if (c2.moveToFirst()) {
            while (c2.moveToNext()) {
                info.add(new Info(c2.getString(0), c2.getInt(1), c2.getDouble(2), c2.getDouble(3), c2.getString(4), c2.getDouble(5)));
            }
        } else info = null;

        if(c2.moveToFirst())
            c2.close();

        Cursor c3 = db.rawQuery("SELECT * FROM pharmacy NATURAL JOIN info WHERE nighttime = 'Y'", null);
        if (c3.moveToFirst()) {
            while (c3.moveToNext()) {
                npharmacy.add(new Pharmacy(c3.getString(0), c3.getString(1), c3.getString(2),c3.getString(3)));
                ninfo.add(new Info(c3.getString(0), c3.getInt(4), c3.getDouble(5), c3.getDouble(6), c3.getString(7), c3.getDouble(8)));
            }
        } else {
            npharmacy = null;
            ninfo = null;
        }
        if(c3.moveToFirst())
            c3.close();
        dbHelper.close();
    } // db 불러오기

    private void insertData(){
        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        db.execSQL("INSERT INTO users VALUES ("+id+","+latitude+","+longitude+")");
        users.add(new Users(id, latitude, longitude));
        Toast.makeText(getApplicationContext(), id+"와"+latitude+"와"+longitude,Toast.LENGTH_LONG).show();
        id++;
        dbHelper.close();
    } //users 테이블에 사용자 id, latitude, longitude 넣기

    private void setMarker(int i, String place, Double latitude, Double longitude){
        marker = new Marker();
        marker.setWidth(140); marker.setHeight(180);
        marker.setIcon(MarkerIcons.BLACK);
        marker.setIconTintColor(Color.GRAY);
        marker.setCaptionText(place); // 이름
        marker.setPosition(new LatLng(latitude, longitude)); //위,경도
        marker.setMap(naverMap);
        if(naverMap.isNightModeEnabled() == true) nmarkers.add(i, marker);
        else markers.add(i, marker);
    }

    public void setLocation(){
        gpsTracker = new GpsTracker(MainActivity.this);
        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();
        insertData();
    }

    private void setInfo(){
        clicked = new boolean[pharmacy.size()];
        for(int i = 0; i < pharmacy.size(); i++) { clicked[i] = false; }

        for(int i = 0; i < pharmacy.size(); i++) {
            setMarker(i, pharmacy.get(i).name, info.get(i).latitude, info.get(i).longitude);
            int j = i;
            markers.get(i).setOnClickListener(new Overlay.OnClickListener() {
                @Override
                public boolean onClick(@NonNull Overlay overlay) {
                    list.add(fin, j);
                    if(clicked[j] == false && (fin == 0 || list.get(fin-1) == j)) {
                        markers.get(j).setIconTintColor(Color.parseColor("#AC7278"));
                        map.setLayoutParams(new LinearLayout.LayoutParams(1430, 1375));
                        setText(j); clicked[j] = true;
                    } else if(clicked[j] == true && list.get(fin-1) == j){
                        markers.get(j).setIconTintColor(Color.GRAY);
                        map.setLayoutParams(new LinearLayout.LayoutParams(1430, 2450));
                        clicked[j] = false;
                    }
                    else if(clicked[j] == false && list.get(fin-1) != j){
                        markers.get(j).setIconTintColor(Color.parseColor("#AC7278"));
                        markers.get(list.get(fin-1)).setIconTintColor(Color.GRAY);
                        map.setLayoutParams(new LinearLayout.LayoutParams(1430, 1375));
                        setText(j); clicked[j] = true; clicked[list.get(fin-1)] = false;
                    }
                    fin += 1;
                    return true;
                }
            });
        }
    }

    private void setnInfo(){
        nclicked = new boolean[npharmacy.size()];
        for(int i = 0; i < npharmacy.size(); i++) { nclicked[i] = false; }

        for(int i = 0; i < npharmacy.size(); i++) {
            setMarker(i, npharmacy.get(i).name, ninfo.get(i).latitude, ninfo.get(i).longitude);
            int j = i;
            nmarkers.get(i).setOnClickListener(new Overlay.OnClickListener() {
                @Override
                public boolean onClick(@NonNull Overlay overlay) {
                    nlist.add(nfin, j);
                    if(nclicked[j] == false && (nfin == 0 || nlist.get(nfin-1) == j)) {
                        nmarkers.get(j).setIconTintColor(Color.parseColor("#AC7278"));
                        map.setLayoutParams(new LinearLayout.LayoutParams(1430, 1375));
                        setText(j); nclicked[j] = true;
                    } else if(nclicked[j] == true && nlist.get(nfin-1) == j){
                        nmarkers.get(j).setIconTintColor(Color.GRAY);
                        map.setLayoutParams(new LinearLayout.LayoutParams(1430, 2450));
                        nclicked[j] = false;
                    }
                    else if(nclicked[j] == false && nlist.get(nfin-1) != j){
                        nmarkers.get(j).setIconTintColor(Color.parseColor("#AC7278"));
                        nmarkers.get(nlist.get(nfin-1)).setIconTintColor(Color.GRAY);
                        map.setLayoutParams(new LinearLayout.LayoutParams(1430, 1375));
                        setText(j); nclicked[j] = true; nclicked[nlist.get(nfin-1)] = false;
                    }
                    nfin += 1;
                    return true;
                }
            });
        }
    }


    private void setText(int idx){
        //address 구 수정해야됨 쪽구름로
        if(naverMap.isNightModeEnabled() == false){
            place.setText(pharmacy.get(idx).name);
            address.setText(pharmacy.get(idx).address.substring(pharmacy.get(idx).address.lastIndexOf("구")+1));
            phonenum.setText(pharmacy.get(idx).phonenum);
            worktime.setText(pharmacy.get(idx).worktime);
        }
        else{
            place.setText(npharmacy.get(idx).name);
            address.setText(npharmacy.get(idx).address.substring(npharmacy.get(idx).address.lastIndexOf("구")+1));
            phonenum.setText(npharmacy.get(idx).phonenum);
            worktime.setText(npharmacy.get(idx).worktime);
        }
    }

    // 위,경도 -> 주소 변환
    public String getAddress() {
        String finalAddress = "도로명주소 미발견";
        String finalAddress2 = "지번주소 미발견";
        try {
            BufferedReader bufferedReader = null;
            StringBuilder stringBuilder = new StringBuilder();
            String coord = longitude + "," + latitude;
            Log.d("coord", coord);
            String query = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&coords="
                    + coord + "&sourcecrs=epsg:4326&output=json&orders=roadaddr&output=xml";
            URL url = null;
            HttpURLConnection conn = null;

            BufferedReader bufferedReader2 = null;
            StringBuilder stringBuilder2 = new StringBuilder();
            String query2 = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&coords="
                    + coord + "&sourcecrs=epsg:4326&output=json&orders=addr&output=xml";
            URL url2 = null;
            HttpURLConnection conn2 = null;

            try {
                url = new URL(query);
                url2 = new URL(query2);
                Log.d("request", "URL 됨");
            } catch (MalformedURLException e) {
                Log.d("request", "URL 안됨");
            }
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn2 = (HttpURLConnection) url2.openConnection();
            } catch (IOException e) {
                Log.d("request", "http 안됨");
            }

            //도로명 주소
            if (conn != null) {
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try {
                    conn.setRequestMethod("GET");
                    Log.d("request", "conn 됨");
                } catch (ProtocolException e) {
                    Log.d("request", "conn 안됨");
                }
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", "3hxuop6xkd");
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", "illyoSwD97UiNVfZs4SI4eso09HNJ0CHjHAeRgh2");
                conn.setDoInput(true);

                int responseCode = 0;
                try {
                    responseCode = conn.getResponseCode();
                    Log.d("request", responseCode + "");
                } catch (IOException e) {
                    Log.d("request", "responseCode 안됨");
                }

                if (responseCode == 200) {
                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    Log.d("request", "if responseCode 안됨");
                }

                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }

                Gson gson = new Gson();
                Log.d("request", String.valueOf(stringBuilder));
                Addresses address = gson.fromJson(String.valueOf(stringBuilder), Addresses.class);
                if (address.results.length != 0) {
                    finalAddress = address.results[0].region.area2.name + " ";
                    finalAddress += address.results[0].region.area3.name + " ";
                    finalAddress += address.results[0].land.name + " ";
                    finalAddress += address.results[0].land.number1;
                }
                Log.d("request", finalAddress);
                bufferedReader.close();
                conn.disconnect();
            }

            //지번 주소
            if (conn2 != null) {
                conn2.setConnectTimeout(5000);
                conn2.setReadTimeout(5000);
                try {
                    conn2.setRequestMethod("GET");
                    Log.d("request2", "conn 됨");
                } catch (ProtocolException e) {
                    Log.d("request2", "conn 안됨");
                }
                conn2.setRequestProperty("X-NCP-APIGW-API-KEY-ID", "3hxuop6xkd");
                conn2.setRequestProperty("X-NCP-APIGW-API-KEY", "illyoSwD97UiNVfZs4SI4eso09HNJ0CHjHAeRgh2");
                conn2.setDoInput(true);

                int responseCode2 = 0;
                try {
                    responseCode2 = conn.getResponseCode();
                    Log.d("request2", responseCode2 + "");
                } catch (IOException e) {
                    Log.d("request2", "responseCode 안됨");
                }

                if (responseCode2 == 200) {
                    bufferedReader2 = new BufferedReader(new InputStreamReader(conn2.getInputStream()));
                } else {
                    bufferedReader2 = new BufferedReader(new InputStreamReader(conn2.getErrorStream()));
                    Log.d("request", "if responseCode 안됨");
                }

                String line2 = null;
                while ((line2 = bufferedReader2.readLine()) != null) {
                    stringBuilder2.append(line2 + "\n");
                }

                Gson gson2 = new Gson();
                Log.d("request2", String.valueOf(stringBuilder2));
                Addresses address2 = gson2.fromJson(String.valueOf(stringBuilder2), Addresses.class);
                finalAddress2 = address2.results[0].region.area2.name + " ";
                finalAddress2 += address2.results[0].region.area3.name + " ";
                finalAddress2 += address2.results[0].land.number1;
                if (address2.results[0].land.number2.length() != 0)
                    finalAddress2 += "-" + address2.results[0].land.number2;
                Log.d("request2", finalAddress2);
                bufferedReader2.close();
                conn2.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (finalAddress != "도로명주소 미발견") {
            return finalAddress;
        } else {
            return finalAddress2;
        }
    }

    public void GetRoute(){

    }

//    public void showInfo(){
//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.
//        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.acitivity_info, (LinearLayout)findViewById(R.id.MainLayout));
//
//    }

}