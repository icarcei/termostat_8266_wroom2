package ro.sun.thermostat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class Wired extends AppCompatActivity implements AsyncResponse {
    private Thermo thermo;
    private ListView listViewsensors;
    private SensorAdapter sensorAdapter;
    private boolean Local;
    private String file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);

        /*Bundle extras = getIntent().getExtras();
        if(extras == null){
            return;
        }
        thermo = (Thermo) extras.get("THERMO");
        if(thermo == null ){
            return;
        }*/
        Intent intent = getIntent();
        Local = intent.getBooleanExtra("LOCAL", true);
        file = intent.getStringExtra("FILE");
        thermo = new Thermo(getApplicationContext(), file, Wired.this, 2, Local);
        sensorAdapter = new SensorAdapter(getApplicationContext(), thermo.getWired().sensorList, true);
        listViewsensors = findViewById(R.id.idlvRadio);
        listViewsensors.setAdapter(sensorAdapter);

        this.setTitle(thermo.getName());
        registerForContextMenu(listViewsensors);

    }

    @Override
    protected void onResume() {
        super.onResume();
        thermo.load();
        refreshed(1);
        thermo.startTimer();
        thermo.reqWired();
    }

    @Override
    protected void onPause() {
        thermo.save();
        thermo.stoptimertask();
        super.onPause();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //super.onCreateContextMenu(menu, v, menuInfo);
            getMenuInflater().inflate(R.menu.contextsensors, menu);
        if(sensorAdapter.getCount()>3){
            menu.findItem(R.id.idcmAddsensor).setEnabled(false);
        }else
            menu.findItem(R.id.idcmAddsensor).setEnabled(true);
        if(sensorAdapter.getCount()<1){
            menu.findItem(R.id.idcmRemove).setEnabled(false);
        }else
            menu.findItem(R.id.idcmRemove).setEnabled(true);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()){
            case R.id.idcmManual:
                thermo.setState(1, sensorAdapter.getItem(info.position).number, 22);
                onBackPressed();
                return true;
            case R.id.idcmForced:
                thermo.setState(2,sensorAdapter.getItem(info.position).number, 22);
                onBackPressed();
                return true;
            case R.id.idcmOff:
                thermo.setState(0,sensorAdapter.getItem(info.position).number, 22);
                onBackPressed();
                return true;
            case R.id.idcmRename:
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.change_name_of_sensor);
                final EditText editText = new EditText(this);
                editText.setText(sensorAdapter.getItem(info.position).location);
                builder.setView(editText);
                builder.setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!editText.getText().toString().isEmpty()){
                            thermo.stoptimertask();
                            thermo.setWired(sensorAdapter.getItem(info.position).number, editText.getText().toString());
                            refreshed(1);
                            thermo.startTimer();
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                AlertDialog a = builder.create();
                a.show();
                return true;
            case R.id.idcmRemove:
                AlertDialog.Builder builderr = new AlertDialog.Builder(this);
                builderr.setTitle(R.string.warning);
                builderr.setIcon(R.drawable.warning);
                builderr.setMessage(R.string.areyoushoreremovesensor);
                builderr.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            thermo.stoptimertask();
                            thermo.remWired(sensorAdapter.getItem(info.position).number);
                            refreshed(1);
                            thermo.startTimer();
                    }
                });
                builderr.setNegativeButton(R.string.cancel, null);
                AlertDialog ar = builderr.create();
                ar.show();
                return true;
            case R.id.idcmAddsensor:
                AlertDialog.Builder buildera = new AlertDialog.Builder(this);
                View view = getLayoutInflater().inflate(R.layout.addsensor, null);
                final EditText editTextName =  (EditText) view.findViewById(R.id.addsensorname);
                final EditText editTextId = (EditText)view.findViewById(R.id.addsensorid);
                Button buttonadd = (Button)view.findViewById(R.id.addsensorbutton);

                buildera.setView(view);
                final AlertDialog aa = buildera.create();

                buttonadd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        AlertDialog.Builder builderr = new AlertDialog.Builder(Wired.this);
                        builderr.setTitle(R.string.warning);
                        builderr.setIcon(R.drawable.warning);
                        builderr.setMessage(R.string.id_of_sensor_is_incorect);

                        String id = editTextId.getText().toString();
                        if(id.length() > 22){// has correct length
                            String[] arrayOfid = id.split(":",8);
                            Integer[] iid = new Integer[8];
                            try{
                                for (int j =0; j<8; j++){
                                    iid[j] = Integer.parseInt(arrayOfid[j],16);
                                }
                                if(thermo.getWired().SensorWithId(iid) == null) {
                                    String nos = editTextName.getText().toString();
                                    if ((nos.length() == 0) || (nos.equals(R.string.name_of_sensor_or_location))) {
                                        nos = getString(R.string.unknow);
                                    }
                                    thermo.stoptimertask();
                                    thermo.addWired(nos, iid);
                                    refreshed(1);
                                    thermo.startTimer();
                                    aa.cancel();
                                    return;
                                }else{
                                    builderr.setMessage(R.string.sensor_allready);
                                }

                            }catch (NumberFormatException e){
                                e.printStackTrace();
                            }

                        }// incorect id
                            AlertDialog ad = builderr.create();
                            ad.show();

                    }
                });

                aa.show();
                return true;

            default:return super.onContextItemSelected(item);
        }

    }

    @Override
    public void onBackPressed() {
        thermo.save();
        thermo.stoptimertask();
        super.onBackPressed();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sensors, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_sensor) {
            AlertDialog.Builder buildera = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.addsensor, null);
            final EditText editTextName =  (EditText) view.findViewById(R.id.addsensorname);
            final EditText editTextId = (EditText)view.findViewById(R.id.addsensorid);
            Button buttonadd = (Button)view.findViewById(R.id.addsensorbutton);

            buildera.setView(view);
            final AlertDialog aa = buildera.create();

            buttonadd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    AlertDialog.Builder builderr = new AlertDialog.Builder(Wired.this);
                    builderr.setTitle(R.string.warning);
                    builderr.setIcon(R.drawable.warning);
                    builderr.setMessage(R.string.id_of_sensor_is_incorect);

                    String id = editTextId.getText().toString();
                    if(id.length() > 22){// has correct length
                        String[] arrayOfid = id.split(":",8);
                        Integer[] iid = new Integer[8];
                        try{
                            for (int j =0; j<8; j++){
                                iid[j] = Integer.parseInt(arrayOfid[j],16);
                            }
                            if(thermo.getWired().SensorWithId(iid) == null) {
                                String nos = editTextName.getText().toString();
                                if ((nos.length() == 0) || (nos.equals(R.string.name_of_sensor_or_location))) {
                                    nos = getString(R.string.unknow);
                                }
                                thermo.stoptimertask();
                                thermo.addWired(nos, iid);
                                refreshed(1);
                                thermo.startTimer();
                                aa.cancel();
                                return;
                            }else{
                                builderr.setMessage(R.string.sensor_allready);
                            }

                        }catch (NumberFormatException e){
                            e.printStackTrace();
                        }

                    }// incorect id
                    AlertDialog ad = builderr.create();
                    ad.show();

                }
            });

            aa.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void refreshed(int success) {
        sensorAdapter.STATE = thermo.getState();
        sensorAdapter.SENSORUSED = thermo.getActSensor().number;
        sensorAdapter.notifyDataSetChanged();
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
