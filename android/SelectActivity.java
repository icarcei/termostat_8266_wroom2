package ro.sun.thermostat;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SelectActivity extends AppCompatActivity implements AsyncResponse {
    public List<ThermoDev> thermoDevs;
    public int THERMOSTATINUSE;
    public int indexRefresed;
    public boolean waitforrequest;
    public String BonjourServer;
    public int BonjourPort;
    public Thermo thermo;

    public String database="thermostats.sun";

    private ThermostatsAdapter thermostatsAdapter;
    private ListView thermostatsListView;
    private Context context;

    public ThermoDevs thermostats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        BonjourServer = "89.121.205.44";
        BonjourPort   = 55000;
        thermoDevs = new ArrayList<>();
        THERMOSTATINUSE = 0;
        context = getApplicationContext();

        thermostats = new ThermoDevs(context, database, SelectActivity.this);

        thermostatsAdapter = new ThermostatsAdapter(SelectActivity.this, thermostats.thermoList);
        thermostatsListView = findViewById(R.id.idlvSelect);
        thermostatsListView.setAdapter(thermostatsAdapter);

        thermostats.getAll();

        this.setTitle(R.string.selectThermostat);
        thermostatsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(context,getString(R.string.open, thermostats.thermoList.get(position).getName()), Toast.LENGTH_SHORT).show();
                thermostats.setTHERMOSTATINUSE(position);
                thermostats.Save();
                onBackPressed();
            }
        });

        registerForContextMenu(thermostatsListView);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.context_thermostats, menu);
        if(thermostatsAdapter.getCount()>8){
            menu.findItem(R.id.idcmAddThermostat).setEnabled(false);
        }else
            menu.findItem(R.id.idcmAddThermostat).setEnabled(true);
        if(thermostatsAdapter.getCount()<2){
            menu.findItem(R.id.idcmRemoveThermostat).setEnabled(false);
        }else
            menu.findItem(R.id.idcmRemoveThermostat).setEnabled(true);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.idcmOpen:
                //thermoDevs.add(new ThermoDev("No Name", "00:00:00:00:00:00", thermoDevs.size()));
                //thermostatsAdapter.notifyDataSetChanged();
                Toast.makeText(context, "Open Thermostat "+ thermostats.thermoList.get(info.position).getName(),Toast.LENGTH_SHORT ).show();
                thermostats.setTHERMOSTATINUSE(info.position);
                thermostats.Save();
                onBackPressed();
                return true;
            case R.id.idcmAddThermostat:
                //thermoDevs.add(new ThermoDev("No Name", "00:00:00:00:00:00", thermoDevs.size()));
                thermostats.Add();
                thermostatsAdapter.notifyDataSetChanged();
                //Save(database);
                onBackPressed();
                return true;
            case R.id.idcmRemoveThermostat:
                AlertDialog.Builder builderr = new AlertDialog.Builder(this);
                builderr.setTitle(R.string.warning);
                builderr.setIcon(R.drawable.warning);
                builderr.setMessage(R.string.areyoushoreallthedatawillbedeleted);
                builderr.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        thermostats.Delete(info.position);
                        thermostatsAdapter.notifyDataSetChanged();
                        //thermo.remRadio(sensorAdapter.getItem(info.position).number);

                    }
                });
                builderr.setNegativeButton(R.string.cancel, null);
                AlertDialog ar = builderr.create();
                ar.show();
                return true;

            default:return super.onContextItemSelected(item);
        }

    }

    @Override
    public void refreshed(int success) {

        waitforrequest = false;
        thermostatsAdapter.notifyDataSetChanged();
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


class ThermoDev{
    public String Name;
    public String Mac;
    public float ActValue;
    public float SetValue;
    public int ActHy;
    public int SetHy;
    public int State;
    public boolean Alarm;
    public boolean Refreshed;
    private int id;
    public ThermoDev(int id){
        Name    = "Unknown";
        Mac     = "00:00:00:00:00:00";
        Refreshed = false;
        this.id  = id;
    }
    public ThermoDev(String Name, String Mac, int id){
        this.Name = Name;
        this.Mac  = Mac;
        Refreshed = false;
        this.id = id;
    }
    public int getId(){
        return  id;
    }
}
