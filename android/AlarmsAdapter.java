package ro.sun.thermostat;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.skumar.flexibleciruclarseekbar.CircularSeekBar;

import java.util.Calendar;
import java.util.List;

public final class AlarmsAdapter extends ArrayAdapter<Alarm> {
    private Context             activitycontext;
    private final List<Alarm>   alarmlist;
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

    public AlarmsAdapter(Context context, List<Alarm> alarmlist, Thermo thermo) {
        super(context, R.layout.alarm_list_item, alarmlist);
        this.activitycontext    = context;
        this.alarmlist          = alarmlist;
        this.thermo             = thermo;
        this.nameofsensors      = thermo.getNameOfSensors();
        this.numberofsensors    = thermo.getNumberofsensors();
    }

    public List<Alarm> getAlarmlist() {
        return alarmlist;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        final ViewHolder viewh;
        final Context context = getContext();
        final Alarm item = alarmlist.get(position);
        final int pnr = position;
        final LayoutInflater inflater = LayoutInflater.from(context);

        if (rowView == null) {
            rowView = inflater.inflate(R.layout.alarm_list_item, parent, false);
            viewh = new ViewHolder();
            viewh.message       = rowView.findViewById(R.id.textViewMessage);
            viewh.enabled       = rowView.findViewById(R.id.switchenable);
            viewh.type          = rowView.findViewById(R.id.switchtype);
            viewh.sensorvalue   = rowView.findViewById(R.id.textViewAlarmValue);
            viewh.time          = rowView.findViewById(R.id.textViewAlarmRepeat);
            viewh.status        = rowView.findViewById(R.id.imageViewStatusAlarm);
            viewh.sensorid      = rowView.findViewById(R.id.spinnerASensorUsed);
            rowView.setTag(viewh);
        } else {
            viewh = (ViewHolder) rowView.getTag();
        }

        //image status
        if(item.active)
            viewh.status.setImageResource(R.drawable.magenta);
        else
            viewh.status.setImageResource(R.drawable.blear);
        //message
        viewh.message.setOnClickListener(null);
        viewh.message.setText(item.message);
        viewh.message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder ab = new AlertDialog.Builder(context);
                ab.setTitle(R.string.choosealarmmessage);
                final EditText editText = new EditText(context);
                editText.setText(item.message);
                ab.setView(editText);
                ab.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        thermo.setAlarmMessage(pnr, editText.getText().toString());
                        viewh.message.setText(editText.getText().toString());
                    }
                });
                ab.setNegativeButton(R.string.cancel, null);
                ab.create().show();

            }
        });
        //enabled
        viewh.enabled.setOnClickListener(null);
        if(item.type > -1){
            viewh.enabled.setChecked(true);
            viewh.enabled.setText(R.string.enabled);
        }else{
            viewh.enabled.setChecked(false);
            viewh.enabled.setText(R.string.disabled);
        }
        viewh.enabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thermo.setAlarmEnabled(position, ((Switch)v).isChecked());
                if(((Switch)v).isChecked()){
                    viewh.enabled.setText(R.string.enabled);
                }else{
                    viewh.enabled.setText(R.string.disabled);
                }
            }
        });

        //type
        viewh.type.setOnClickListener(null);
        if(thermo.isAlarmMaximal(pnr)){
            viewh.type.setChecked(true);
            viewh.type.setText(R.string.maximal);
        }else{
            viewh.type.setChecked(false);
            viewh.type.setText(R.string.minimal);
        }
        viewh.type.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thermo.setAlarmEnabled(pnr, thermo.isAlarmEnabled(pnr), ((Switch)v).isChecked());
                if (((Switch)v).isChecked())viewh.type.setText(R.string.maximal);
                else viewh.type.setText(R.string.minimal);
            }
        });
        // time
        viewh.time.setText(context.getString(R.string.repeat_at, item.repeat));
        viewh.time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                android.support.v7.app.AlertDialog.Builder buildera = new android.support.v7.app.AlertDialog.Builder(context);
                buildera.setTitle("Choose repeat time");
                final NumberPicker np = new NumberPicker(context);
                String[] sval={"0", "5", "10", "15", "30", "60", "2h", "4h", "6h", "12h", "24h"};
                np.setMinValue(0);
                np.setMaxValue(10);
                np.setDisplayedValues(sval);
                np.setValue(item.repeat);
                buildera.setView(np);
                buildera.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (np.getValue()){
                            case 1: thermo.setAlarmRepeat(pnr, 5);break;
                            case 2: thermo.setAlarmRepeat(pnr, 10);break;
                            case 3: thermo.setAlarmRepeat(pnr, 15);break;
                            case 4: thermo.setAlarmRepeat(pnr, 30);break;
                            case 5: thermo.setAlarmRepeat(pnr, 60);break;
                            case 6: thermo.setAlarmRepeat(pnr, 120);break;
                            case 7: thermo.setAlarmRepeat(pnr, 240);break;
                            case 8: thermo.setAlarmRepeat(pnr, 360);break;
                            case 9: thermo.setAlarmRepeat(pnr, 720);break;
                            case 10: thermo.setAlarmRepeat(pnr, 1440);break;
                            default: thermo.setAlarmRepeat(pnr, 0);
                        }
                        thermo.save();
                        viewh.time.setText(context.getString(R.string.repeat_at,thermo.getAlarms().getAlarm(pnr).repeat));
                    }
                });
                buildera.setNegativeButton(R.string.cancel, null);
                buildera.create().show();

            }
        });
        //sensorvalue
        viewh.sensorvalue.setText(context.getString(R.string.activation_value, item.value));
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
                circularSeekBar.setMin(-20);
                circularSeekBar.setMax(85);
                //circularSeekBar.progress = progressValue;
                circularSeekBar.setIncreaseCenterNeedle(20);
                circularSeekBar.setValueStep(2);
                circularSeekBar.setNeedleFrequency(0.5f);
                circularSeekBar.setNeedleDistanceFromCenter(30);
                circularSeekBar.setNeedleLengthInDP(12);
                circularSeekBar.setIncreaseCenterNeedle(24);
                circularSeekBar.setNeedleThickness(1.f);

                circularSeekBar.setEnabled(true);
                circularSeekBar.setProgress(item.value);
                textViewValue.setText(item.value+" \u2103");
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
                        thermo.setAlarmValue(position, value[0]);
                        viewh.sensorvalue.setText(context.getString(R.string.activation_value, value[0]));
                    }
                });
                buildera.setNegativeButton(R.string.cancel, null);
                android.support.v7.app.AlertDialog aa = buildera.create();
                aa.show();
            }
        });
        //sensorid
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, nameofsensors);
        //set the spinners adapter to the previously created one.
        if(viewh.sensorid != null) {
            viewh.sensorid.setAdapter(adapter);
            viewh.sensorid.setSelection(getNamePos(item.sensor));
            viewh.sensorid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    thermo.setAlarmSensor(pnr, numberofsensors[position]);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
        return rowView;
    }

    private static class ViewHolder {
        private TextView time;
        private TextView sensorvalue;
        private Spinner sensorid;
        private ImageView status;
        private TextView message;
        private Switch enabled;
        private Switch type;

    }
}

