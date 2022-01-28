package ro.sun.thermostat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ListView;

public class AlarmsActivity extends AppCompatActivity implements AsyncResponse{
    private Thermo thermo;
    private ListView alarmListView;
    private AlarmsAdapter alarmsAdapter;
    private boolean Local;
    private String file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms);
        Intent intent = getIntent();
        Local = intent.getBooleanExtra("LOCAL", true);
        file = intent.getStringExtra("FILE");
        thermo = new Thermo(getApplicationContext(), file, AlarmsActivity.this, 0, Local);
        alarmsAdapter = new AlarmsAdapter(AlarmsActivity.this, thermo.getAlarms().alarmList, thermo);
        alarmListView = findViewById(R.id.lvAlarms);
        alarmListView.setAdapter(alarmsAdapter);
    }

    @Override
    protected void onResume() {


        super.onResume();
        thermo.load();
        refreshed(1);
        thermo.startTimer();
        thermo.reqAlarms();
    }

    @Override
    protected void onPause() {
        thermo.save();
        thermo.stoptimertask();
        thermo.setAlarms();
        super.onPause();
    }
    @Override
    public void refreshed(int success) {
        alarmsAdapter.notifyDataSetChanged();
    }

    @Override
    public void processFinish(int v) {

    }

    @Override
    public void processFinish(String s) {

    }

    @Override
    public void processFinish(Boolean b) {

    }

    @Override
    public void processFinish(String IP, boolean f) {

    }

    @Override
    public void processFinish(boolean finded, String mac) {

    }
}
