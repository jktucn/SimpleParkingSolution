package com.jktuxiyang.simpleparkingsolution;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private ParseQueryAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Set up a customized query
        ParseQueryAdapter.QueryFactory factory = new ParseQueryAdapter.QueryFactory() {
            public ParseQuery create() {
                ParseQuery query = ParseQuery.getQuery("ParkingLot");
                query.orderByAscending("name");
                return query;
            }
        };

        // Set up the query adapter
        adapter = new ParseQueryAdapter(this, factory) {
            @Override
            public View getItemView(ParseObject lot, View view, ViewGroup parent) {
                if (view == null) {
                    view = View.inflate(getContext(), R.layout.lot_item, null);
                }
                TextView name = (TextView) view.findViewById(R.id.lot_name);
                TextView text = (TextView) view.findViewById(R.id.lot_status);
                int availableNum = lot.getInt("availableNum");
                int totalNum = lot.getInt("totalNum");
                float rate = (float) 1.0 * (totalNum - availableNum) / totalNum;
                int co = interpolateColor(Color.GREEN, Color.RED, rate);
                name.setText(lot.getString("name"));
                // Set color according to fullness
                name.setBackgroundColor(co);
                text.setText("" + availableNum + "/" + totalNum);
                return view;
            }
        };

        // Attach the query adapter to the ListView
        ListView lotListView = (ListView) this.findViewById(R.id.lot_listview);
        lotListView.setAdapter(adapter);

        // Set up the handler for item's selection
        lotListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ParseObject lot = adapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, LotDetails.class);
                intent.putExtra("infoCarried", lot.getObjectId());
                startActivity(intent);
            }
        });

        // Update the user status button
        updateButton();

        final ParseUser user = ParseUser.getCurrentUser();
        final Button user_button = (Button) findViewById(R.id.user_status);
        // Set up handler for user status button click
        user_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Button works only when user is parked
                if (!user_button.getText().equals("Roaming")){
                    final ProgressDialog checkOutDialog = new ProgressDialog(MainActivity.this);
                    checkOutDialog.setMessage("Checking out");
                    checkOutDialog.show();
                    final ParseObject user_spot = user.getParseObject("spot");
                    final ParseObject user_lot = user_spot.getParseObject("belongsTo");

                    try {
                        user.fetch();
                        user_spot.fetch();
                        user_lot.fetch();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    // Update user information
                    user.put("isParked", false);

                    // Update spot information
                    List<ParseUser> temp = user_spot.getList("user");
                    ArrayList<ParseUser> uArray = new ArrayList(temp);

                    // Remove user from the spot
                    int i;
                    for (i = 0; i < uArray.size(); i++){
                        if (uArray.get(i) == user)
                            break;
                    }
                    if (i < uArray.size())
                        uArray.remove(i);
                    user_spot.put("user", uArray);
                    if(uArray.size() == 0) {
                        user_spot.put("isAvailable", true);
                        // Update lot information
                        int availableNum = user_lot.getInt("availableNum");
                        availableNum++;
                        user_lot.put("availableNum", availableNum);
                    }

                    // Save changes

                    user_lot.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                // Show the error message
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            else {
                                user.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            // Show the error message
                                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                        else {
                                            user_spot.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    checkOutDialog.dismiss();
                                                    if (e!=null){
                                                        // Show the error message
                                                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                    else {
                                                        // Show the action is done
                                                        Toast.makeText(MainActivity.this, "Checked Out", Toast.LENGTH_SHORT).show();
                                                        // Update UI
                                                        adapter.loadObjects();
                                                        updateButton();
                                                    }
                                                }
                                            });
                                        }

                                    }
                                });
                            }
                        }
                    });

                }
            }
        });

    }
    // Update the user button UI
    public void updateButton(){
        ParseUser user = ParseUser.getCurrentUser();
        Button user_button = (Button) findViewById(R.id.user_status);
        if (!user.getBoolean("isParked"))
            user_button.setText("Roaming");
        else {
            ParseObject spot = user.getParseObject("spot");
            try {
                spot.fetch();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            ParseObject lot = spot.getParseObject("belongsTo");
            try {
                lot.fetch();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String text = "Check out from " + lot.getString("name") + " / " + spot.getInt("spotNumber");
            user_button.setText(text);
        }
    }



    // Hotness color auxiliary function
    private float interpolate(float a, float b, float proportion) {
        return (a+((b-a)*proportion));
    }

    // Hotness color auxiliary function
    private int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Log out

        if (id == R.id.action_logout) {
            ParseUser.logOut();
            Intent intent = new Intent(this, DispatchActivity.class);
            startActivity(intent);
            return true;
        }

        // Refresh
        if (id == R.id.action_refresh) {
            adapter.loadObjects();
            updateButton();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
