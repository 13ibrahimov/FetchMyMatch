package com.wordpress.iibrahimov.fetchmymatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.app.DownloadManager;
import android.os.AsyncTask;
import android.view.View;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MainActivity extends AppCompatActivity {

    //searchURl: google search url for search keyword
    //finalURL: url of the first google search image for download
    //editTextSearch: search keyword input by user
    protected String searchUrl;
    protected String finalUrl;
    protected EditText editTextSearch;

    //dmanager: DownloadManager Class object to handle downloading process
    //requestid: used for displaying downloaded image on ImageView
    DownloadManager dmanager;
    long requestid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextSearch = findViewById(R.id.editTextSearch);

        //To be executed when Fetch button is clicked by user
        Button fetchButton = findViewById(R.id.buttonFetch);
        fetchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Get user input keyword
                //Get google search url String
                searchUrl = editTextSearch.getText().toString();
                searchUrl = searchUrl.replace(" ", "+");
                searchUrl = ("https://google.com/search?q=" + searchUrl + "&tbm=isch").toLowerCase();

                //Run Async Task which handles google image search
                new GoogleScrape().execute(searchUrl);
            }
                                       });

        //Wait for image download to complete
        //Display the image on user screen once download is complete
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    DownloadManager.Query requestQuery = new DownloadManager.Query();
                    requestQuery.setFilterById(requestid);

                    Cursor cursor = dmanager.query(requestQuery);
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);

                        if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                            ImageView imageView = (ImageView)findViewById(R.id.imageView);
                            String uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                            imageView.requestFocus();
                            imageView.setImageURI(Uri.parse(uri));
                        }
                    }
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    //Method that handles View button click
    //Lets the user to view all downloads made on phone
    public void viewClick(View view) {
        Intent intent = new Intent();
        intent.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
        startActivity(intent);
    }

    //Async Task to perform network operations
    private class GoogleScrape extends AsyncTask<String,String,String> {

        //imgUrls: list that stores image source urls from google image search
        //could be used to increase number of downloads each search if needed(currently 1)
        List<String> imgUrls = new ArrayList<String>();

        //Running network operations in background
        @Override
        protected String doInBackground(String... furl) {

            //Traversing HTML doc
            //Jsoup is used to get and parse the HTML doc
            //JSON.simple is used to parse JSON objects in div elements of HTML which store image urls
            //image source urls are added to list imgUrls
            try {
                URL url = new URL(furl[0]);

                //Fetching image search page
                Document doc = Jsoup.connect(url.toString()).get();

                Elements elements = doc.select("div.rg_meta");

                JSONObject jsonObject;
                for (Element element : elements) {

                    if (element.childNodeSize() > 0) {
                        jsonObject = (JSONObject) new JSONParser().parse(element.childNode(0).toString());
                        imgUrls.add((String) jsonObject.get("ou"));
                    }
                }

            } catch (IOException | org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }

            return null;
        }

        //Runs after google image search operations are complete
        //Assigns the url of first image appeared on google image search to finalUrl
        //And downloads that image
        protected void onPostExecute(String result) {

            finalUrl = imgUrls.get(0);

            dmanager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(finalUrl));
            requestid = dmanager.enqueue(request);
        }
    }

}
