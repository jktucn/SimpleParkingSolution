package com.jktuxiyang.simpleparkingsolution;

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
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LotDetails extends ActionBarActivity {
    private ParseQueryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lot_details);

        // Retrieve the lot information
        Intent intent = getIntent();
        String lotId = intent.getStringExtra("infoCarried");
        final ParseObject lot = ParseObject.createWithoutData("ParkingLot", lotId);
        final ParseUser user = ParseUser.getCurrentUser();
        final Button user_button = (Button) findViewById(R.id.user_status);

        // Set up a customized query
        ParseQueryAdapter.QueryFactory factory = new ParseQueryAdapter.QueryFactory() {
            public ParseQuery create() {
                ParseQuery query = ParseQuery.getQuery("ParkingSpot");
                query.whereEqualTo("belongsTo", lot);
                query.orderByAscending("spotNumber");
                return query;
            }
        };

        // Set up the query adapter
        adapter = new ParseQueryAdapter(this, factory) {
            @Override
            public View getItemView(ParseObject spot, View view, ViewGroup parent) {
                if (view == null) {
                    view = View.inflate(getContext(), R.layout.spot_item, null);
                }
                TextView name = (TextView) view.findViewById(R.id.spot_name);
                name.setText(""+spot.getInt("spotNumber"));
                if (spot.getBoolean("isAvailable")) {
                    name.setBackgroundColor(Color.GREEN);
                }
                else {
                    name.setBackgroundColor(Color.RED);
                }
                return view;
            }
        };


        // Attach the query adapter to the GridView
        GridView spotGridView = (GridView) this.findViewById(R.id.spot_gridview);
        spotGridView.setAdapter(adapter);

        // Set up the handler for item's selection
        spotGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Works only when user is roaming (not parked)
                if(!user.getBoolean("isParked"))
                {
                    // The spot being clicked
                    final ParseObject spot = adapter.getItem(position);
                    Date now = new Date();
                    final ProgressDialog checkInDialog = new ProgressDialog(LotDetails.this);
                    checkInDialog.setMessage("Checking in");
                    checkInDialog.show();

                    try {
                        user.fetch();
                        spot.fetch();
                        lot.fetch();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    // Update user information
                    user.put("isParked", true);
                    user.put("lastCheckInTime", now);
                    user.put("spot", spot);

                    // Update spot information
                    List<ParseUser> temp = spot.getList("user");
                    ArrayList<ParseUser> uArray;
                    if(temp!=null) {
                        uArray = new ArrayList(temp);
                    }
                    else {
                        uArray = new ArrayList();
                    }
                    uArray.add(user);
                    spot.put("user", uArray);

                    // Update lot information
                    if (spot.getBoolean("isAvailable")) {
                        int availableNum = lot.getInt("availableNum");
                        availableNum--;
                        lot.put("availableNum", availableNum);
                    }
                    spot.put("isAvailable", false);

                    // Save changes

                    lot.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                // Show the error message
                                Toast.makeText(LotDetails.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            else {
                                user.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            // Show the error message
                                            Toast.makeText(LotDetails.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                        else {
                                            spot.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    checkInDialog.dismiss();
                                                    if (e!=null){
                                                        // Show the error message
                                                        Toast.makeText(LotDetails.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                    else {
                                                        // Show the action is done
                                                        Toast.makeText(LotDetails.this, "Checked In", Toast.LENGTH_SHORT).show();
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

        // spotGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        spotGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
                ParseObject spot = adapter.getItem(position);
                Intent intent = new Intent(LotDetails.this, ReportViolation.class);
                intent.putExtra("infoCarried", spot.getObjectId());
                intent.putExtra("isAvailable", ""+ spot.getBoolean("isAvailable"));
                startActivity(intent);
                return true;
            }

        });

        // Update the user status button
        updateButton();

        // Set up handler for user status button click
        user_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Button works only when user is parked
                if (!user_button.getText().equals("Roaming")){
                    final ProgressDialog checkOutDialog = new ProgressDialog(LotDetails.this);
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
                                Toast.makeText(LotDetails.this, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            else {
                                user.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            // Show the error message
                                            Toast.makeText(LotDetails.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                        else {
                                            user_spot.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    checkOutDialog.dismiss();
                                                    if (e!=null){
                                                        // Show the error message
                                                        Toast.makeText(LotDetails.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                    else {
                                                        // Show the action is done
                                                        Toast.makeText(LotDetails.this, "Checked Out", Toast.LENGTH_SHORT).show();
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

   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_lot_details, menu);
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
}
