package com.example.comp3717project;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class MatchActivity extends AppCompatActivity {

    TextView matchID;
    TextView startTime;
    TextView radiantWin;
    private String[] playerIdArray;
    private String[] heroIdArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        Log.d("ID", id);

        matchID = (TextView) findViewById(R.id.match_id);
        startTime = (TextView) findViewById(R.id.start_time);
        radiantWin = (TextView) findViewById(R.id.radiant_win);

        // Call to API and retrieve match data
        String stringUrl = "https://api.steampowered.com/IDOTA2Match_570/GetMatchDetails/V001/?" +
                "key=" + SplashActivity.API_KEY +
                "&match_id=" + id;

        new DownloadWebpageTask().execute(stringUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recent_games, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void initPLayerList() {
        ListView lv = (ListView) findViewById(R.id.playerListView);

        ArrayList<String> gameList = new ArrayList<String>();

        for (int i = 0; i < playerIdArray.length; i++) {
            gameList.add(i, "Player " + (i + 1) + "\t\t" + "ID: " + playerIdArray[i]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.row, gameList);

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Intent intent = new Intent(MainActivity.this, MatchActivity.class);
//                intent.putExtra("id", recentGameIDArray[position]);
//                startActivity(intent);
                Intent intent = new Intent(MatchActivity.this, PlayerDetail.class);
                intent.putExtra("id", MainActivity.heroNames[position]);
                intent.putExtra("2step", true);
                intent.putExtra("match", matchID.getText());
                startActivity(intent);
            }
        });

    }

    /*

        Async task for downloading
     */
    private String downloadUrl(String myurl) throws Exception {
        InputStream in = null;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Start connection
            conn.connect();
            int response = conn.getResponseCode();
            Log.d("HTTP-URL-CONNECTION", "The response is: " + response);
            in = conn.getInputStream();

            return readIt(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private String readIt(InputStream stream) throws IOException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        BufferedReader bufReader = new BufferedReader(reader);

        String line;
        String result = "";
        while ((line = bufReader.readLine()) != null) {
            result += line.replaceAll("\\s+", "");
        }

        return result;
    }

    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return downloadUrl(urls[0]);
            } catch (Exception e) {
                Log.e("Problem", e.getMessage());
                return "error404";
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            try {
                if (result.equalsIgnoreCase("error404"))
                    Toast.makeText(getApplicationContext(), "Unable to retrieve web page at the moment", Toast.LENGTH_LONG).show();

                // Start json parser
                JSONObject json = new JSONObject(result);

                matchID.setText("Match ID\t" + json.getJSONObject("result").getString("match_id"));

                // Format UNIX timestamps into date
                Date date = new Date(json.getJSONObject("result").getLong("start_time") * 1000L); // *1000 is to convert seconds to milliseconds
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of your date
                sdf.setTimeZone(TimeZone.getTimeZone("GMT-8")); // give a timezone reference for formating (see comment at the bottom
                String formattedDate = sdf.format(date);

                startTime.setText("Start Time\t" + formattedDate);

                radiantWin.setText((json.getJSONObject("result").getBoolean("radiant_win") ? "RADIANT VICTORY" : "DIRE VICTORY"));


                JSONArray player = json.getJSONObject("result").getJSONArray("players");

                playerIdArray = new String[player.length()];
                heroIdArray = new String[player.length()];

                for (int i = 0; i < player.length(); i++) {
                    playerIdArray[i] = player.getJSONObject(i).getString("account_id");
                    heroIdArray[i] = player.getJSONObject(i).getString("hero_id");
                }

                initPLayerList();
            } catch (Exception ex) {
                Log.d("Problem", ex.getMessage());
            }
        }
    }
}
