package com.jktuxiyang.simpleparkingsolution;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.parse.ParseException;
import com.parse.ParseObject;


public class ReportViolation extends ActionBarActivity {
    private ParseObject report;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        report = new ParseObject("Report");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_violation);
        Intent intent = getIntent();
        String spotId = intent.getStringExtra("infoCarried");
        ParseObject spot = ParseObject.createWithoutData("ParkingSpot", spotId);
        String isAvailable = intent.getStringExtra("isAvailable");
        report.put("spot", spot);
        report.put("isAvailable", isAvailable);


        setContentView(R.layout.activity_report_violation);
        FragmentManager manager = getFragmentManager();
        Fragment fragment = manager.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragment = new NewReportFragment();
            manager.beginTransaction().add(R.id.fragmentContainer, fragment).commit();
        }



    }

    public ParseObject getCurrentReport() {
        return report;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_report_violation, menu);
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
}
