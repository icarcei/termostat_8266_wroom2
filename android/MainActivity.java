package ro.sun.thermostat;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.tv.TvContract;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.skumar.flexibleciruclarseekbar.CircularSeekBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import ro.sun.thermostat.async.ScanHostsAsyncTask;
import ro.sun.thermostat.network.Wireless;
import ro.sun.thermostat.network.Host;



public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AsyncResponse {
    public CircularSeekBar circularSeekBar ;
    public TextView tvLocation;
    public TextView tvActualValue ;
    public TextView tvSetValue;
    public TextView tvHygro;
    public ImageView ivWired;
    public ImageView ivRadio;
    public ImageView ivBell;
    public ImageView ivAlarms;
    public Thermo    thermo;
    public ImageView ivSetState;
    public TextView  tvName;
    public TextView  tvEmail;
    public TextView  tvShowValue2;
    public ImageView ivIcon;
    private JSONArray macslist;
    private Wireless wifi;
    private ProgressDialog scanProgressDialog;
    private devt devices;
    private AlertDialog alertDialogfind;
    private int hitforbonjour;
    private boolean checkTheClock;
    private boolean Local;
    private String file;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState != null){

        }
        //reading mac table
        try{
            InputStream is = getAssets().open("forCheck.mac");
            byte[] iscontent = new byte[is.available()];
            is.read(iscontent);
            is.close();
            String jsonArrayString = new String(iscontent);
            try{
                macslist = new JSONArray(jsonArrayString);
            }catch (final JSONException e){
                Toast.makeText(getApplicationContext(),
                        "Mac's parsing error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }catch (IOException e){
            Toast.makeText(MainActivity.this, "Error reading mac table.", Toast.LENGTH_LONG).show();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Bla Bla Bla", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        wifi = new Wireless(getApplicationContext());
        devices = new devt();

        tvLocation    = findViewById(R.id.idtvLocation);
        tvActualValue = findViewById(R.id.idtvActValue);
        tvSetValue    = findViewById(R.id.idtvSetValue);
        tvHygro       = findViewById(R.id.idtvHygro);
        ivBell        = findViewById(R.id.idivBell);
        ivRadio       = findViewById(R.id.idivAlarmRadio);
        ivWired       = findViewById(R.id.idivAlarmWire);
        ivAlarms      = findViewById(R.id.idivAlarms);
        ivSetState    = findViewById(R.id.idivState);
        tvShowValue2  = findViewById(R.id.idtvShowValue2);

        ivBell.setVisibility(View.INVISIBLE);
        ivRadio.setVisibility(View.INVISIBLE);
        ivWired.setVisibility(View.INVISIBLE);
        ivIcon        = findViewById(R.id.idivIcon);
        checkTheClock = false;

        circularSeekBar = (CircularSeekBar)findViewById(R.id.mCircularSeekBar);
        circularSeekBar.setDrawMarkings(true);
        //circularSeekBar.setDotMarkers(true);;
        circularSeekBar.setRoundedEdges(true);
        circularSeekBar.setIsGradient(true);
        //circularSeekBar.setPopup(true);
        circularSeekBar.setArcThickness(20);
        circularSeekBar.setArcRotation(225);
        circularSeekBar.setSweepAngle(270);
        circularSeekBar.setMin(10);
        circularSeekBar.setMax(30);
        //circularSeekBar.progress = progressValue;
        circularSeekBar.setIncreaseCenterNeedle(20);
        circularSeekBar.setValueStep(2);
        circularSeekBar.setNeedleFrequency(0.5f);
        circularSeekBar.setNeedleDistanceFromCenter(30);
        circularSeekBar.setNeedleLengthInDP(12);
        circularSeekBar.setIncreaseCenterNeedle(24);
        circularSeekBar.setNeedleThickness(1.f);
        circularSeekBar.setHeightForPopupFromThumb(10);
        //?circularSeekBar.setProgressBarThickness(100);

        circularSeekBar.setEnabled(false);
        Intent intent = getIntent();
        Local          = intent.getBooleanExtra("LOCAL", true);
        String  thermostatFILE  = intent.getStringExtra("NAME");
        if (thermostatFILE == null || thermostatFILE.isEmpty()) {

            thermostatFILE = Load("thermostats.sun");//"thermodata.jjj";
            file = thermostatFILE;
        }
        //thermo       = new Thermo(getApplicationContext(), thermostatFILE, MainActivity.this, 0, LOCAL);

        ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams)circularSeekBar.getLayoutParams();
        //Toast.makeText(getApplicationContext(),"width:"+layoutParams.width+" height:"+layoutParams.height, Toast.LENGTH_SHORT).show();
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            //layoutParams.height = 400; layoutParams.width = 400;
            //circularSeekBar.setLayoutParams(layoutParams);
            //circularSeekBar.setArcThickness(10);
        }

        circularSeekBar.setOnCircularSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            float value;
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float v, boolean b) {
                tvSetValue.setText(v+"\u2103");
                value = v;
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar circularSeekBar) {
                thermo.stoptimertask();

            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar circularSeekBar) {
                thermo.setState(thermo.getState(),value);
                thermo.startTimer();
            }
        });

        ivBell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AlarmsActivity.class);
                thermo.stoptimertask();
                thermo.save();
                intent.putExtra("FILE", file);
                intent.putExtra("LOCAL", thermo.isLocal());
                startActivity(intent);
            }
        });
        ivRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Radio.class);
                thermo.stoptimertask();
                thermo.save();
                intent.putExtra("FILE", file);
                intent.putExtra("LOCAL", thermo.isLocal());
                startActivity(intent);
            }
        });
        ivWired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Wired.class);
                thermo.stoptimertask();
                thermo.save();
                intent.putExtra("FILE", file);
                intent.putExtra("LOCAL", thermo.isLocal());
                startActivity(intent);
            }
        });

        hitforbonjour = 0;

        boolean r = intent.getBooleanExtra("ANSWERED", true);
        if(!r) {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setTitle("ATENTION!");
            ab.setIcon(R.drawable.warning);
            ab.setMessage(R.string.therdoesntanswer);
            ab.setPositiveButton(R.string.ok, null);
            ab.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            ab.create().show();
        }

        registerForContextMenu((ImageView)findViewById(R.id.idivState));

    }

    @Override
    protected void onResume() {
        String thermostatFILE = Load("thermostats.sun");//"thermodata.jjj";
        file = thermostatFILE;
        thermo       = new Thermo(getApplicationContext(), thermostatFILE, MainActivity.this, 0, Local);
        //thermo.load();
        refreshed(1);
        thermo.startTimer();
        thermo.reqState();
        super.onResume();
    }

    protected String Load(String file){
        Context context = getApplicationContext();
        int thermostatinuse =0;
        try {
            InputStream is = context.openFileInput(file);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, "UTF-8");
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                if(jsonObject.has("THERMOSTATINUSE")){
                    thermostatinuse = jsonObject.getInt("THERMOSTATINUSE");
                }
                if(jsonObject.has("THERMOSTATS")){
                    JSONArray jaa = jsonObject.getJSONArray("THERMOSTATS");
                    return jaa.getJSONObject(thermostatinuse).getString("FILE");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String rez="thermostat.jj0";
        return rez;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            finishAffinity();
            System.exit(0);
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        tvName        = findViewById(R.id.idtvName);
        tvEmail       = findViewById(R.id.idtvemail);
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
            showOptions();
            return true;
        }
        if (id == R.id.action_email) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.emailchange);
            builder.setMessage(R.string.email_exp);
            final EditText editText = new EditText(getApplicationContext());
            editText.setText(thermo.getEmails());
            builder.setView(editText);
            builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    thermo.setEmails(editText.getText().toString());
                    thermo.save();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if((hitforbonjour++ > 3) && thermo.isLocal()){
                        AlertDialog.Builder builderb = new AlertDialog.Builder(MainActivity.this);
                        builderb.setTitle("Change Bonjour Params");
                        View view = getLayoutInflater().inflate(R.layout.bonjour,null);
                        builderb.setView(view);
                        final EditText bonjourserver = view.findViewById(R.id.etbonjourserver);
                        final EditText bonjourport = view.findViewById(R.id.atbonjourport);
                        bonjourport.setText(thermo.getBonjourport()+"");
                        bonjourserver.setText(thermo.getBonjourserver());
                        builderb.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                thermo.setBonjourserver(bonjourserver.getText().toString());
                                thermo.setBonjourport(Integer.parseInt(bonjourport.getText().toString()));
                                thermo.setParams();
                            }
                        });
                        builderb.setNegativeButton(R.string.cancel, null);
                        builderb.create().show();
                        hitforbonjour = 0;
                    }
                }
            });
            builder.create().show();

            return true;
        }
        if (id == R.id.action_clock){
            if(checkTheClock){
                Toast.makeText(this, getString(R.string.already_requested_time), Toast.LENGTH_SHORT).show();
            }else {
                checkTheClock = true;
                thermo.reqTime();
            }
        }
        if (id == R.id.action_repeatalarm) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.alarmrepeatinterval);
            builder.setMessage(R.string.alarmrepeatmessageexplain);
            final NumberPicker np = new NumberPicker(getApplicationContext());
            String[] sval={"0", "5", "10", "15", "30", "60", "2h", "4h", "6h", "12h", "24h"};
            np.setMinValue(0);
            np.setMaxValue(10);
            np.setDisplayedValues(sval);
            if(thermo.getRepeat_alarm_sensors() < 5){
                np.setValue(0);
            }else{
                if(thermo.getRepeat_alarm_sensors() < 10){
                    np.setValue(1);
                }else{
                    if(thermo.getRepeat_alarm_sensors() < 15){
                        np.setValue(2);
                    }else {
                        if (thermo.getRepeat_alarm_sensors() < 30) {
                            np.setValue(3);
                        } else {
                            if (thermo.getRepeat_alarm_sensors() < 60) {
                                np.setValue(4);
                            } else {
                                if (thermo.getRepeat_alarm_sensors() < 120) {
                                    np.setValue(5);
                                } else {
                                    if (thermo.getRepeat_alarm_sensors() < 240) {
                                        np.setValue(6);
                                    } else {
                                        if (thermo.getRepeat_alarm_sensors() < 360) {
                                            np.setValue(7);
                                        } else {
                                            if (thermo.getRepeat_alarm_sensors() < 720) {
                                                np.setValue(8);
                                            } else {
                                                if (thermo.getRepeat_alarm_sensors() < 1440) {
                                                    np.setValue(9);
                                                } else {
                                                    np.setValue(10);
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
            builder.setView(np);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (np.getValue()){
                        case 1: thermo.setRepeat_alarm_sensors(5); ;break;
                        case 2: thermo.setRepeat_alarm_sensors(10);break;
                        case 3: thermo.setRepeat_alarm_sensors(15);break;
                        case 4: thermo.setRepeat_alarm_sensors(30);break;
                        case 5: thermo.setRepeat_alarm_sensors(60);break;
                        case 6: thermo.setRepeat_alarm_sensors(120);break;
                        case 7: thermo.setRepeat_alarm_sensors(240);break;
                        case 8: thermo.setRepeat_alarm_sensors(360);break;
                        case 9: thermo.setRepeat_alarm_sensors(720);break;
                        case 10: thermo.setRepeat_alarm_sensors(1440);break;
                        default: thermo.setRepeat_alarm_sensors(0);
                    }
                    thermo.save();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.create().show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showOptions(){

        AlertDialog.Builder buildera = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.parameter, null);
        final EditText editTextName =  (EditText) view.findViewById(R.id.thermostatName);
        final EditText editTextMac = (EditText)view.findViewById(R.id.thermostatMac);
        final EditText textViewip = (EditText) view.findViewById(R.id.thermostatip);
        final Switch switchlocal = (Switch) view.findViewById(R.id.idswitchlocal);
        Button buttonfind = (Button)view.findViewById(R.id.thermostatfindIP);
        Button buttonsetwifi = (Button)view.findViewById(R.id.thermostatsetup);
        final AlertDialog aa;

        editTextName.setText(thermo.getName());
        editTextMac.setText(thermo.getMac());
        textViewip.setText(thermo.getIP());
        switchlocal.setChecked(thermo.isUselocalap());

        buildera.setView(view);

        buildera.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String mac=editTextMac.getText().toString();
                String name =editTextName.getText().toString();
                boolean save=false;

                AlertDialog.Builder builderr = new AlertDialog.Builder(MainActivity.this);
                builderr.setTitle(R.string.warning);
                builderr.setIcon(R.drawable.warning);
                builderr.setMessage(R.string.mac_incorect);
                String id = editTextMac.getText().toString();
                if(id.length() > 16){// has correct length
                    String[] arrayOfid = id.split(":",6);
                    Integer[] iid = new Integer[6];
                    try{
                        for (int j =0; j<6; j++){
                            iid[j] = Integer.parseInt(arrayOfid[j],16);
                        }
                        save = false;
                        for(int jj=0; jj< macslist.length(); jj++){
                            if(mac.startsWith(macslist.getString(jj)))
                                save = true;
                        }
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                        save = false;
                    } catch (JSONException e) {
                        e.printStackTrace();
                        save = false;
                    }

                    if(name.length()<4) {
                        save = false;
                        builderr.setMessage(R.string.nametosmall);
                    }


                }// incorect id
                AlertDialog ad = builderr.create();
                if(!save) {
                    ad.show();
                }else {
                    thermo.setIP(textViewip.getText().toString());
                    thermo.setMac(mac);
                    thermo.setName(name);
                    thermo.setUselocalap(switchlocal.isChecked());
                }

            }
        }) ;
        buildera.setNegativeButton(R.string.cancel,null);

        aa = buildera.create();

        buttonfind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderr = new AlertDialog.Builder(MainActivity.this);
                builderr.setTitle(R.string.warning);
                builderr.setIcon(R.drawable.warning);
                builderr.setMessage(R.string.mac_incorect);
                String id = editTextMac.getText().toString();
                if(id.length() > 16){// has correct length
                    String[] arrayOfid = id.split(":",6);
                    Integer[] iid = new Integer[6];
                    try{
                        for (int j =0; j<6; j++){
                            iid[j] = Integer.parseInt(arrayOfid[j],16);
                        }
                        //look for device in local network
                        devices.find();
                        Search();
                        aa.cancel();

                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }

                }// incorect id
                AlertDialog ad = builderr.create();
                if(!devices.isLookingfordevice())
                    ad.show();

            }
        });

        buttonsetwifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builderw = new AlertDialog.Builder(MainActivity.this);
                View view = getLayoutInflater().inflate(R.layout.wifiparameter, null);
                wifi                = new Wireless(getApplicationContext());
                //networkId           =  -1;
                final ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
                String wifiSsid     = "";
                try {
                    wifiSsid = wifi.getSSID();
                } catch (Wireless.NoWifiManagerException e) {
                    //err
                }
                final EditText ssideditText =  (EditText) view.findViewById(R.id.thermostatWiFiName);
                final EditText passwordeditText = (EditText)view.findViewById(R.id.thermostatPasswordWiFi);
                Button buttonfindandset = (Button)view.findViewById(R.id.thermostatfindandset);
                ssideditText.setText(wifiSsid);
                builderw.setView(view);
                buttonfindandset.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Client mc = new Client(ssideditText.getText().toString(), passwordeditText.getText().toString(), getApplicationContext(), MainActivity.this, connectionManager);
                        mc.execute();
                    }
                });

                alertDialogfind = builderw.create();
                alertDialogfind.show();
            }
        });

        aa.show();

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_radio) {
            Intent intent = new Intent(MainActivity.this, Radio.class);
            thermo.stoptimertask();
            thermo.save();
            intent.putExtra("LOCAL", thermo.isLocal());
            intent.putExtra("FILE",file);
            startActivity(intent);

        } else if (id == R.id.nav_wired) {
            Intent intent = new Intent(MainActivity.this, Wired.class);
            thermo.stoptimertask();
            thermo.save();
            intent.putExtra("LOCAL", thermo.isLocal());
            intent.putExtra("FILE",file);
            startActivity(intent);

        } else if (id == R.id.nav_programs) {
            Intent intent = new Intent(MainActivity.this, ProgramsActivity.class);
            thermo.stoptimertask();
            thermo.save();
            intent.putExtra("LOCAL", thermo.isLocal());
            intent.putExtra("FILE",file);
            startActivity(intent);

        } else if (id == R.id.nav_alarms) {
            Intent intent = new Intent(MainActivity.this, AlarmsActivity.class);
            thermo.stoptimertask();
            thermo.save();
            intent.putExtra("LOCAL", thermo.isLocal());
            intent.putExtra("FILE",file);
            startActivity(intent);

        } else if (id == R.id.nav_manage) {
            Intent intent = new Intent(MainActivity.this, SelectActivity.class);
            thermo.stoptimertask();
            thermo.save();
            intent.putExtra("LOCAL", thermo.isLocal());
            startActivity(intent);

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        //menu.setHeaderTitle(getString(R.string.set_state));
        getMenuInflater().inflate(R.menu.menu_setstate, menu);
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
                case R.id.action_auto:
                    //ivSetState.setImageResource(R.drawable.auto);
                    //circularSeekBar.setEnabled(false);
                    thermo.setState(3+thermo.getWeekenddays());
                    return true;
                case R.id.action_forced:
                    //circularSeekBar.setEnabled(true);
                    //ivSetState.setImageResource(R.drawable.obliq);
                    thermo.setState(2);
                    return  true;
                case R.id.action_manual:
                    //circularSeekBar.setEnabled(true);
                    //ivSetState.setImageResource(R.drawable.hand);
                    thermo.setState(1);
                    return true;
                case R.id.action_off:
                    //circularSeekBar.setEnabled(false);
                    //ivSetState.setImageResource(R.drawable.logout);
                    thermo.setState(0);
                    return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        thermo.save();
        thermo.stoptimertask();
        super.onPause();
    }

    @Override
    public void refreshed(int success) {
        if(thermo.getActSensor() != null)
            tvLocation.setText(thermo.getActSensor().location);
        boolean inalarm;
        if(thermo.isAlarm()){
            ivBell.setVisibility(View.VISIBLE);
            inalarm = true;
        }
        else{
            ivBell.setVisibility(View.INVISIBLE);
            inalarm = false;
        }
        if(thermo.isRadioalarm()){
            ivRadio.setVisibility(View.VISIBLE);
            inalarm |= true;
        }
        else {
            ivRadio.setVisibility(View.INVISIBLE);
        }
        if(thermo.isWiredalarm()){
            ivWired.setVisibility(View.VISIBLE);
            inalarm |= true;
        }
        else{
            ivWired.setVisibility(View.INVISIBLE);
        }
        if(success < 1) {
            ivAlarms.setImageResource(R.drawable.blear);
        }else{
        if(inalarm){
            if(thermo.isEnergized()){
                ivAlarms.setImageResource(R.drawable.red1);
            }
            else{
                ivAlarms.setImageResource(R.drawable.purple);
            }
            //ivAlarms.setImageResource(R.drawable.magenta);
        }
        else{
            if(thermo.isEnergized()){
                ivAlarms.setImageResource(R.drawable.orange);
            }
            else{
                ivAlarms.setImageResource(R.drawable.blue2);
            }
            }
        }
        this.setTitle(thermo.getName());
        try{
            tvName.setText(thermo.getName());
            tvEmail.setText(thermo.getEmails());
        }catch (Exception e){
            e.printStackTrace();
        }

        tvActualValue.setText(thermo.getValue() + "\u00B0");
        circularSeekBar.setProgress(thermo.getValueset());
        tvSetValue.setText(thermo.getValueset() + "\u2103");
        float vext = thermo.getExternalValue();
        if(vext > -50) {
            tvShowValue2.setVisibility(View.VISIBLE);
            tvShowValue2.setText(getString(R.string.externalshowvalue, vext));
        }else{
            tvShowValue2.setVisibility(View.INVISIBLE);
            //tvShowValue2.setText(getString(R.string.externalshowvalue, -12.5));
        }
        int hygro = thermo.getHygro();
        if (hygro > 5 && hygro < 101){
            tvHygro.setVisibility(View.VISIBLE);
            tvHygro.setText("Hygro: " + hygro + "%");
        }
        else{
            tvHygro.setVisibility(View.INVISIBLE);
        }
        switch (thermo.getState()){
            case 0:
                ivSetState.setImageResource(R.drawable.logout);
                circularSeekBar.setEnabled(false);
                break;
            case 1:
                ivSetState.setImageResource(R.drawable.hand);
                circularSeekBar.setEnabled(true);
                break;
            case 2:
                ivSetState.setImageResource(R.drawable.obliq);
                circularSeekBar.setEnabled(true);
                break;
                default:{
                    ivSetState.setImageResource(R.drawable.auto);
                    circularSeekBar.setEnabled(false);
                }

        }
        if(success == 7 && checkTheClock){
            checkTheClock = false;
            final Context context = MainActivity.this;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.chech_time_of_device);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String time = dateFormat.format(thermo.getActTime() * 1000 );// - thermo.getTimeZone() * 3600000);
            String daylight;
            if( thermo.getDayLight() == 1)
                daylight = getString(R.string.yes);
            else
                daylight = getString(R.string.no);
            builder.setMessage(getString(R.string.timemessage, time, thermo.getTimeZone(), daylight));
            builder.setPositiveButton(R.string.phoneclocktoThermostat, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TimeZone tz = TimeZone.getDefault();
                    long offset=tz.getOffset(new Date().getTime())/1000;
                    long millis = System.currentTimeMillis() / 1000;
                    long mm = millis + offset;
                    //Toast.makeText(context, millis +" sec off "+(offset >=0?"+":"-")+offset+" =>"+mm, Toast.LENGTH_LONG).show();
                    thermo.setTime(mm);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.setNeutralButton(R.string.change_time, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Calendar calendar = Calendar.getInstance();
                    calendar.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    calendar.setTimeInMillis(thermo.getActTime() * 1000);
                    int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                    int currentMinute = calendar.get(Calendar.MINUTE);
                    TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                            new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            AlertDialog.Builder ab = new AlertDialog.Builder(context);
                            ab.setTitle(R.string.setTimeZone);
                            final NumberPicker np = new NumberPicker(context);
                            String[] sval={"-11", "-10", "-9", "-8", "-7", "-6","-5", "-4", "-3", "-2", "-1", "0 - UTC", "+1 Central Europe", "+2 Eastern Europe", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10", "+11", "+12"};
                            np.setMinValue(0);
                            np.setMaxValue(23);
                            np.setDisplayedValues(sval);
                            np.setValue(thermo.getTimeZone() + 11);
                            ab.setView(np);

                            TimeZone tz = TimeZone.getTimeZone("UTC");
                            long offset=tz.getOffset(new Date().getTime())/1000;
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(Calendar.MINUTE, minute);
                            final long millis = calendar.getTimeInMillis() / 1000 + offset;
                            //Toast.makeText(context, calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+ "ts:"+millis +" sec", Toast.LENGTH_LONG).show();
                            ab.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AlertDialog.Builder abb = new AlertDialog.Builder(context);
                                    abb.setTitle(R.string.savedaylight);
                                    abb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            thermo.setTime(millis , np.getValue() - 11, 1);
                                        }
                                    });
                                    abb.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            thermo.setTime(calendar.getTimeInMillis() / 1000, np.getValue(), 0);
                                        }
                                    });
                                    abb.create().show();
                                }
                            });
                            ab.create().show();
                        }

                    }, currentHour, currentMinute, true);//false
                    timePickerDialog.show();
                }
            });

            builder.create().show();

        }

    }

    @Override
    public void processFinish(int v) {
        if (scanProgressDialog != null && scanProgressDialog.isShowing()) {
            scanProgressDialog.incrementProgressBy(v);
        }
    }

    @Override
    public void processFinish(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    public void processFinish(Boolean b) {
        if (b && scanProgressDialog != null && scanProgressDialog.isShowing()) {
            scanProgressDialog.dismiss();
        }
        devices.stop();
    }

    @Override
    public void processFinish(String IP, boolean f) {

        if(f) {
            Toast.makeText(getApplicationContext(),getString(R.string.devicefoundataddress)+IP, Toast.LENGTH_SHORT).show();
            devices.IP = IP;
            devices.seFind(f);
        }else{
            Toast.makeText(getApplicationContext(), R.string.devicenotfoud, Toast.LENGTH_SHORT).show();
        }
        thermo.setIP(IP);
        thermo.save();
        showOptions();


    }

    @Override
    public void processFinish(boolean finded, String mac) {
        if(finded){
            if(mac.equals(thermo.getMac())){
                Toast.makeText(getApplicationContext(),getString(R.string.findedOK), Toast.LENGTH_SHORT).show();
            }else{
                AlertDialog.Builder builderr = new AlertDialog.Builder(MainActivity.this);
                builderr.setTitle(R.string.warning);
                builderr.setIcon(R.drawable.warning);
                builderr.setMessage(R.string.foundAddresWrong);
                builderr.create().show();
            }
            if(alertDialogfind != null){
                if(alertDialogfind.isShowing())alertDialogfind.cancel();
            }
        }else{
            AlertDialog.Builder builderr = new AlertDialog.Builder(MainActivity.this);
            builderr.setTitle(R.string.warning);
            builderr.setIcon(R.drawable.warning);
            builderr.setMessage(R.string.notfinded);
            builderr.create().show();
        }
    }

    private void Search(){
        Resources resources = getResources();
        Context context     = getApplicationContext();
        try {
            if (!wifi.isEnabled()) {
                Errors.showError(context, resources.getString(R.string.wifiDisabled));
                return;
            }

            if (!wifi.isConnectedWifi()) {
                Errors.showError(context, resources.getString(R.string.notConnectedWifi));
                return;
            }
        } catch (Wireless.NoWifiManagerException | Wireless.NoConnectivityManagerException e) {
            Errors.showError(context, resources.getString(R.string.failedWifiManager));
            return;
        }
        int numSubnetHosts;
        try {
            numSubnetHosts = wifi.getNumberOfHostsInWifiSubnet();
        } catch (Wireless.NoWifiManagerException e) {
            Errors.showError(context, resources.getString(R.string.failedSubnetHosts));
            return;
        }

        scanProgressDialog = new ProgressDialog(MainActivity.this);
        scanProgressDialog.setCancelable(false);
        scanProgressDialog.setTitle(resources.getString(R.string.hostScan));
        scanProgressDialog.setMessage(String.format(resources.getString(R.string.subnetHosts), numSubnetHosts));
        scanProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        scanProgressDialog.setProgress(0);
        scanProgressDialog.setMax(numSubnetHosts);
        scanProgressDialog.show();

        try {
            Integer ip = wifi.getInternalWifiIpAddress(Integer.class);
            //new ScanHostsAsyncTask(MainActivity.this, macslist).execute(ip, wifi.getInternalWifiSubnet(), 500);
            new ScanHostsAsyncTask(MainActivity.this, thermo.getMac()).execute(ip, wifi.getInternalWifiSubnet(), 500);
        } catch (UnknownHostException | Wireless.NoWifiManagerException e) {
            Errors.showError(context, resources.getString(R.string.notConnectedWifi));
        }

    }
}

class devt{
    private boolean lookingfordevice;
    private boolean newfind;
    public boolean finded(){lookingfordevice = false; return newfind;}
    public void seFind(boolean f){lookingfordevice =false; newfind=f;}
    public void stop(){newfind=false; lookingfordevice=false;}
    public boolean isLookingfordevice() {return lookingfordevice;}
    public void find(){lookingfordevice=true;}
    public String IP;
    public devt(){lookingfordevice=false;newfind=false;};

}

class Client extends AsyncTask<Void, Void, Void> {

    String wifissid;
    String wifipassword;
    String response = "";
    boolean finded;
    Context context;
    AsyncResponse asyncResponse;
    int networkId;
    int devnetworkId = -1;
    ConnectivityManager connectionManager;

    Client(String wifissid, String wifipassword , Context context, AsyncResponse asyncResponse, ConnectivityManager connectionManager) {
        this.wifipassword           =  wifipassword;
        this.wifissid               = wifissid;
        this.context                = context;
        this.finded                 = false;
        this.asyncResponse          = asyncResponse;
        this.connectionManager      = connectionManager;
    }

    public boolean reconectWifi(){
        try {

            WifiManager wifiManager =
                    (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            if(!wifiManager.isWifiEnabled()){
                wifiManager.setWifiEnabled(true);
            }
            wifiManager.disconnect();
            if(devnetworkId > 0)
                wifiManager.removeNetwork(devnetworkId);
            wifiManager.enableNetwork(networkId, true);
            wifiManager.reconnect();
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
    public void remove (Context context, final String networkSSID){
        try {

            WifiManager wifiManager =
                    (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

            if(!wifiManager.isWifiEnabled()){
                wifiManager.setWifiEnabled(true);
            }
            List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                if (wifiConfiguration.SSID.equals("\"" + networkSSID + "\"")) {
                    wifiManager.removeNetwork(wifiConfiguration.networkId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean connect(Context context, final String networkSSID, final String networkPassword) {
        try {

            WifiManager wifiManager =
                    (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

            if(!wifiManager.isWifiEnabled()){
                wifiManager.setWifiEnabled(true);
            }
            Integer existingNetworkId = null;
            List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                if (wifiConfiguration.SSID.equals("\"" + networkSSID + "\"")) {
                    existingNetworkId = wifiConfiguration.networkId;
                    //wifiManager.removeNetwork(wifiConfiguration.networkId);
                    //wifiManager.saveConfiguration();
                }
            }
            boolean r=true;
            if(existingNetworkId!=null){
                r=false;
                r=wifiManager.removeNetwork(existingNetworkId);
                r &= wifiManager.saveConfiguration();
            }
            int netid = -1;
            if (r) {
                WifiConfiguration conf = new WifiConfiguration();
                conf.SSID = '\"' + networkSSID + '\"';
                //conf.preSharedKey = "\"" + networkPassword + "\"";
                conf.allowedAuthAlgorithms.clear();
                conf.allowedGroupCiphers.clear();
                conf.allowedKeyManagement.clear();
                conf.allowedPairwiseCiphers.clear();
                conf.allowedProtocols.clear();
                conf.hiddenSSID = false;

                if (networkPassword.equals("")) {
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                } else {
                    conf.preSharedKey = "\"" + networkPassword + "\"";
                }

                netid = wifiManager.addNetwork(conf);
            }



            if (netid == -1) return false;

            String wifiSsid="";
            try {
                wifiSsid = wifiManager.getConnectionInfo().getSSID();
                networkId = wifiManager.getConnectionInfo().getNetworkId();
                wifiManager.disconnect();
                if(networkId > 0)
                    wifiManager.disableNetwork(networkId);

            } catch (Exception e) {
                //err
            }


            wifiManager.disconnect();
            /////important!!!
            //wifiManager.disableNetwork(wifi_inf.getNetworkId());
            /////////////////
            if (wifiManager.enableNetwork(netid, true)) {
                wifiManager.reconnect();
                Long timeStamp = System.currentTimeMillis();
                NetworkInfo activeNetwork = connectionManager.getActiveNetworkInfo();
                while(activeNetwork == null && ((System.currentTimeMillis() - timeStamp) < 4000)){
                    activeNetwork = connectionManager.getActiveNetworkInfo();
                }
                activeNetwork = connectionManager.getActiveNetworkInfo();
                while (!activeNetwork.isConnected() && ((System.currentTimeMillis() - timeStamp) < 4000)){
                    activeNetwork = connectionManager.getActiveNetworkInfo();
                }
                if(activeNetwork.isConnected() && activeNetwork.getExtraInfo().equals("\"" + networkSSID + "\"")) {
                    devnetworkId = wifiManager.getConnectionInfo().getNetworkId();
                    return true;
                } else
                    return false;
            }
            wifiManager.reconnect();
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //return false;
    }

    @Override
    protected Void doInBackground(Void... arg0) {

        Socket socket   = null;
        finded          = false;
        boolean c = connect(context, "IoT_SUN_Portal", "");
        for (int j = 0; j < 5 && !c; j++)
            c = connect(context, "IoT_SUN_Portal", "");

        if (c) try{
            Long timeStamp = System.currentTimeMillis();
            while((System.currentTimeMillis() - timeStamp) < 2000){}
            socket = new Socket("192.168.4.1", 4321);
            socket.setSoTimeout(3000);

            PrintWriter printWritter = new PrintWriter(socket.getOutputStream());
            printWritter.write("{\"WiFiSSID\":\"" + wifissid + "\", \"WiFiPASS\":\"" + wifipassword + "\"}\n");
            printWritter.flush();

            InputStreamReader is = new InputStreamReader(socket.getInputStream());
            BufferedReader br = new BufferedReader(is);
            response = br.readLine();
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.getString("ans").equals("ok")) {
                    response = jsonObject.getString("mac");
                    finded = true;
                } else {
                    response = jsonObject.getString("ans");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                response = e.toString();
            }
            socket.close();

        } catch(UnknownHostException e){
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch(IOException e){
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } finally{
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        reconectWifi();
        Long timeStamp = System.currentTimeMillis();
        while((System.currentTimeMillis() - timeStamp) < 2000){}
        remove(context, "IoT_SUN_Portal");
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        //textResponse.setText(response);
        if(asyncResponse != null)
            asyncResponse.processFinish(finded, response);
        //Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
        super.onPostExecute(result);
    }

}