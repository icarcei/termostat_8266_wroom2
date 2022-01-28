package ro.sun.thermostat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.skumar.flexibleciruclarseekbar.CircularSeekBar;

import java.util.Calendar;
import java.util.List;

public final class ProgramsAdapter extends ArrayAdapter<Program> {
    private Context             activitycontext;
    private final List<Program> programlist;
    private Calendar            calendar;
    private TimePickerDialog    timePickerDialog;
    private String[]            nameofsensors;
    private int[]               numberofsensors;
    private Thermo              thermo;
    private boolean             weekend;

    private int getNamePos(int nos){
        for(int i=0; i< numberofsensors.length; i++){
            if (nos == numberofsensors[i])
                return i;
        }
        return 0;
    }

    public ProgramsAdapter(Context context, List<Program> programlist, Thermo thermo, boolean weekend) {
        super(context, R.layout.program_list_item, programlist);
        this.activitycontext    = context;
        this.programlist        = programlist;
        this.thermo             = thermo;
        this.nameofsensors      = thermo.getNameOfSensors();
        this.numberofsensors    = thermo.getNumberofsensors();
        this.weekend            = weekend;
    }

    public List<Program> getProgramlist() {
        return programlist;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        final ViewHolder viewh;
        final Context context = getContext();
        final Program item = programlist.get(position);
        final int pnr = position;

        if (rowView == null) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            rowView = inflater.inflate(R.layout.program_list_item, parent, false);

            viewh = new ViewHolder();
            viewh.sensorvalue = rowView.findViewById(R.id.textViewTemp);
            viewh.time     = rowView.findViewById(R.id.textViewTime);
            viewh.time.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    //calendar = Calendar.getInstance();
                    //int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                    //int currentMinute = calendar.get(Calendar.MINUTE);
                    timePickerDialog = new TimePickerDialog(activitycontext, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                            if(weekend)thermo.setWeekendtime(position, hourOfDay, minute);
                            else thermo.setWeektime(position, hourOfDay, minute);

                            String h,m;
                            if(minute < 10)m = "0" + minute;
                            else           m = ""  + minute;
                            if(hourOfDay   < 10)h = "0" + hourOfDay;
                            else                h = ""  + hourOfDay;
                            viewh.time.setText(context.getResources().getString(R.string.start)+" "+h+":"+m);
                        }

                    }, item.hour, item.minute, false);
                    timePickerDialog.show();
                }
            });
            viewh.status         = rowView.findViewById(R.id.ivProgramStatus);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, nameofsensors);
            viewh.sensorvalue.setText(item.value+"\u2103");
            viewh.sensorvalue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    android.support.v7.app.AlertDialog.Builder buildera = new android.support.v7.app.AlertDialog.Builder(context);
                    //View view = getLayoutInflater().inflate(R.layout.temppicker, null);
                    View view = inflater.inflate(R.layout.temppicker,null);
                    final TextView textViewValue = (TextView) view.findViewById(R.id.tvTemppicker);
                    CircularSeekBar circularSeekBar;
                    circularSeekBar = (CircularSeekBar)view.findViewById(R.id.tCircularSeekBar);
                    circularSeekBar.setDrawMarkings(true);
                    circularSeekBar.setRoundedEdges(true);
                    circularSeekBar.setIsGradient(true);
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

                    circularSeekBar.setEnabled(true);
                    final float[] value = new float[1];
                    circularSeekBar.setOnCircularSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(CircularSeekBar circularSeekBar, float v, boolean b) {
                            textViewValue.setText(v+" \u2103");
                            value[0] = v;
                        }

                        @Override
                        public void onStartTrackingTouch(CircularSeekBar circularSeekBar) {
                            //thermo.stoptimertask();

                        }

                        @Override
                        public void onStopTrackingTouch(CircularSeekBar circularSeekBar) {
                            //thermo.setState(thermo.getState(),value);
                            //thermo.startTimer();
                        }
                    });

                    buildera.setView(view);
                    buildera.setTitle(R.string.choosetemp);
                    buildera.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(weekend)thermo.setWeekendValue(position, value[0]);
                            else thermo.setWeekValue(position, value[0]);
                            viewh.sensorvalue.setText(value[0]+"\u2103");
                        }
                    });
                    buildera.setNegativeButton(R.string.cancel, null);
                    android.support.v7.app.AlertDialog aa = buildera.create();
                    aa.show();
                }
            });

            String h,m;
            if(item.minute < 10)m = "0" + item.minute;
            else                m = ""  + item.minute;
            if(item.hour   < 10)h = "0" + item.hour;
            else                h = ""  + item.hour;
            viewh.time.setText(context.getResources().getString(R.string.start)+" "+h+":"+m);

            viewh.sensorid = rowView.findViewById(R.id.spinnerSensorUsed);

            //set the spinners adapter to the previously created one.
            if(viewh.sensorid != null) {

                viewh.sensorid.setAdapter(adapter);
                viewh.sensorid.setSelection(getNamePos(item.sensor));
                viewh.sensorid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if(weekend)thermo.setWeekendSensor(pnr, numberofsensors[position]);
                        else thermo.setWeekSensor(pnr, numberofsensors[position]);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }

            rowView.setTag(viewh);
        } else {
            viewh = (ViewHolder) rowView.getTag();
        }

        //view.sensorid.setText(item.getadd());
        //view.sensorid.setText(item.add[0] + ":" +item.add[1]+ ":" +item.add[2]+ ":" +item.add[3]+
        //        ":" + item.add[4] + ":" +item.add[5]+ ":" +item.add[6]+ ":" +item.add[7]);
        if(weekend){
            if(thermo.getProgram_number()-10 == item.getNr()){
                viewh.status.setVisibility(View.VISIBLE);
                switch(thermo.getState()){
                    case 0:
                        viewh.status.setImageResource(R.drawable.logout);
                        break;
                    case 1:
                        viewh.status.setImageResource(R.drawable.hand);
                        break;
                    case 2:
                        viewh.status.setImageResource(R.drawable.obliq);
                        break;
                    default: viewh.status.setImageResource(R.drawable.auto);
                }
            }else{
                viewh.status.setVisibility(View.INVISIBLE);
            }

        }else {
            if (thermo.getProgram_number() == item.getNr()) {
                viewh.status.setVisibility(View.VISIBLE);
                switch (thermo.getState()) {
                    case 0:
                        viewh.status.setImageResource(R.drawable.logout);
                        break;
                    case 1:
                        viewh.status.setImageResource(R.drawable.hand);
                        break;
                    case 2:
                        viewh.status.setImageResource(R.drawable.obliq);
                        break;
                    default:
                        viewh.status.setImageResource(R.drawable.auto);
                }
            } else {
                viewh.status.setVisibility(View.INVISIBLE);
            }
        }

        return rowView;
    }

    private static class ViewHolder {
        private TextView time;
        private TextView sensorvalue;
        private Spinner sensorid;
        private ImageView status;

    }
}

