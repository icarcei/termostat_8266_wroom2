package ro.sun.thermostat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class SensorAdapter extends ArrayAdapter<Sensor> {
    private Context context;
    private final List<Sensor> sensorslist;
    public int STATE;
    public int SENSORUSED;
    private boolean wired;

    public SensorAdapter(Context context, List<Sensor> sensorslist) {
        super(context, R.layout.sensors_list_item, sensorslist);
        this.context = context;
        this.sensorslist = sensorslist;
        STATE =0;
        SENSORUSED =0;
        wired = false;
    }
    public SensorAdapter(Context context, List<Sensor> sensorslist, boolean wired) {
        super(context, R.layout.sensors_list_item, sensorslist);
        this.context = context;
        this.sensorslist = sensorslist;
        STATE =0;
        SENSORUSED =0;
        this.wired = wired;
    }

    public List<Sensor> getSensorslist() {
        return sensorslist;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        ViewHolder view;
        final Context context = getContext();

        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            rowView = inflater.inflate(R.layout.sensors_list_item, parent, false);

            view = new ViewHolder();
            view.sensorname     = rowView.findViewById(R.id.idtvNameSensor);
            view.sensorid       = rowView.findViewById(R.id.idtvIDSensor);
            view.sensorvalue    = rowView.findViewById(R.id.idtvSensorValue);
            view.sensorhy       = rowView.findViewById(R.id.idtvSensorHygro);
            view.battery        = rowView.findViewById(R.id.idivBattSensor);
            view.status         = rowView.findViewById(R.id.idivStatusSensor);
            rowView.setTag(view);
        } else {
            view = (ViewHolder) rowView.getTag();
        }

        final Sensor item = sensorslist.get(position);
        view.sensorname.setText(item.location);
        view.sensorvalue.setText(item.value+"\u2103");
        if(item.humidity > 4 && item.humidity < 121) {
            view.sensorhy.setText("Hy:" + item.humidity + "%");
        }else{
            view.sensorhy.setText("");
        }
        view.sensorid.setText(item.getadd());
        //view.sensorid.setText(item.add[0] + ":" +item.add[1]+ ":" +item.add[2]+ ":" +item.add[3]+
        //        ":" + item.add[4] + ":" +item.add[5]+ ":" +item.add[6]+ ":" +item.add[7]);
        if (item.alarm){
            view.status.setVisibility(View.VISIBLE);
            view.status.setImageResource(R.drawable.warning);
        }else{
            if(item.number == SENSORUSED){
                switch(STATE){
                    case 0:
                        view.status.setImageResource(R.drawable.logout);
                        break;
                    case 1:
                        view.status.setImageResource(R.drawable.hand);
                        break;
                    case 2:
                        view.status.setImageResource(R.drawable.obliq);
                        break;
                    default: view.status.setImageResource(R.drawable.auto);
                }
                view.status.setVisibility(View.VISIBLE);
            }
            else {
                view.status.setVisibility(View.INVISIBLE);
            }
        }
        if(! wired) {
            if (item.energy > 3.09) {
                view.battery.setImageResource(R.drawable.battery_full);
            } else {
                if (item.energy > 2.9) {
                    view.battery.setImageResource(R.drawable.battery);
                } else {
                    if (item.energy > 2.85) {
                        view.battery.setImageResource(R.drawable.battery_half);
                    } else {
                        if (item.energy > 2.79) {
                            view.battery.setImageResource(R.drawable.battery_low);
                        } else {
                            view.battery.setImageResource(R.drawable.battery_empty);
                        }
                    }
                }
            }
            view.battery.setVisibility(View.VISIBLE);
        }else{
            view.battery.setVisibility(View.INVISIBLE);
        }


        return rowView;
    }

    private static class ViewHolder {
        private TextView sensorname;
        private TextView sensorvalue;
        private TextView sensorid;
        private TextView sensorhy;
        private ImageView battery;
        private ImageView status;

    }
}
