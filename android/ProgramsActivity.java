package ro.sun.thermostat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class ProgramsActivity extends AppCompatActivity implements AsyncResponse{
    private Thermo thermo;
    private ListView lvWeekprograms;
    private ListView lvWeekendprograms;
    private ProgramsAdapter weekprogramsAdapter;
    private ProgramsAdapter weekendprogramsAdapter;
    private TextView textViewTitleWeekendPrograms;
    private boolean Local;
    private String file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programs);

        //get the spinner from the xml.
        Spinner dropdown = findViewById(R.id.spinnerWeekend);
        //create a list of items for the spinner.
        String[] items = new String[]{getString(R.string.without_weekend), getString(R.string.sunday_is_weekend), getString(R.string.saturday_and_sunday_weekends)};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                thermo.setWeekenddays(position);
                if(position == 0){
                    lvWeekendprograms.setVisibility(View.INVISIBLE);
                    textViewTitleWeekendPrograms.setVisibility(View.INVISIBLE);
                }else{
                    lvWeekendprograms.setVisibility(View.VISIBLE);
                    textViewTitleWeekendPrograms.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        Intent intent = getIntent();
        Local = intent.getBooleanExtra("LOCAL", true);
        file = intent.getStringExtra("FILE");
        thermo = new Thermo(getApplicationContext(), file, ProgramsActivity.this, 0, Local);
        dropdown.setSelection(thermo.getWeekenddays());

        weekprogramsAdapter = new ProgramsAdapter(ProgramsActivity.this, thermo.getPrograms().weekprogramsList,thermo, false);
        weekendprogramsAdapter = new ProgramsAdapter(ProgramsActivity.this,thermo.getPrograms().weekendprogramsList, thermo, true);
        lvWeekprograms = findViewById(R.id.lvWeekPrograms);
        lvWeekprograms.setAdapter(weekprogramsAdapter);
        textViewTitleWeekendPrograms = findViewById(R.id.textViewTitleWeekendPrograms);
        lvWeekendprograms = findViewById(R.id.lvWeekendPrograms);
        lvWeekendprograms.setAdapter(weekendprogramsAdapter);

        if(thermo.getWeekenddays() == 0){
            lvWeekendprograms.setVisibility(View.INVISIBLE);
            textViewTitleWeekendPrograms.setVisibility(View.INVISIBLE);
        }else{
            lvWeekendprograms.setVisibility(View.VISIBLE);
            textViewTitleWeekendPrograms.setVisibility(View.VISIBLE);
        }

        this.setTitle(thermo.getName());
        thermo.startTimer();
        registerForContextMenu(lvWeekprograms);
    }

    @Override
    protected void onPause() {
        super.onPause();
        thermo.setPrograms();
        thermo.stoptimertask();
    }
    @Override
    protected void onResume() {
        super.onResume();
        thermo.load();
        refreshed(1);
        thermo.startTimer();
        thermo.reqPrograms();
    }

    @Override
    public void refreshed(int success) {
        weekprogramsAdapter.notifyDataSetChanged();
        weekendprogramsAdapter.notifyDataSetChanged();
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
