package ro.sun.thermostat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public final class ThermostatsAdapter extends ArrayAdapter<Thermo> {
    private Context context;
    private final List<Thermo> thermostats;
    public int STATE;
    public int THERMOUSED;
    private boolean wired;

    public ThermostatsAdapter(Context context, List<Thermo> thermostats) {
        super(context, R.layout.thermostat_list_item, thermostats);
        this.context = context;
        this.thermostats = thermostats;
        STATE =0;
        THERMOUSED =0;
        wired = false;
    }


    public List<Thermo> getSensorslist() {
        return thermostats;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        ViewHolder view;
        final Context context = getContext();

        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            rowView = inflater.inflate(R.layout.thermostat_list_item, parent, false);

            view = new ViewHolder();
            view.thermostatname     = rowView.findViewById(R.id.idNameOfThermostat);
            view.thermostatvalue    = rowView.findViewById(R.id.idtvthermostatValue1);
            view.thermostathy       = rowView.findViewById(R.id.idtvthermostatValue2);
            view.thermostatState    = rowView.findViewById(R.id.idivstatThermostat);
            view.thermostatalarm    = rowView.findViewById(R.id.idivThermostatAlarm);
            rowView.setTag(view);
        } else {
            view = (ViewHolder) rowView.getTag();
        }

        final Thermo item = thermostats.get(position);
        view.thermostatname.setText(item.getName());
        if(item.getValue()!=0 ) {
            view.thermostatvalue.setText(item.getValue() + "\u2103");
            if (item.getHygro() > 4 && item.getHygro() < 121) {
                view.thermostathy.setText("Hy:" + item.getHygro() + "%");
            } else {
                view.thermostathy.setText("");
            }

            if(item.isCommalarm()) {
                view.thermostatalarm.setVisibility(View.VISIBLE);
                view.thermostatalarm.setImageResource(R.drawable.blear);
            }else{

                if (item.isAlarm() || item.isRadioalarm() || item.isWiredalarm()) {
                    view.thermostatalarm.setVisibility(View.VISIBLE);
                    view.thermostatalarm.setImageResource(R.drawable.warning);
                } else {
                    view.thermostatalarm.setImageResource(R.drawable.green2);
                }
            }


                switch (item.getState()) {
                    case 0:
                        view.thermostatState.setImageResource(R.drawable.logout);
                        break;
                    case 1:
                        view.thermostatState.setImageResource(R.drawable.hand);
                        break;
                    case 2:
                        view.thermostatState.setImageResource(R.drawable.obliq);
                        break;
                    default:
                        view.thermostatState.setImageResource(R.drawable.auto);
                }
                view.thermostatState.setVisibility(View.VISIBLE);


        }else{
            view.thermostatState.setVisibility(View.INVISIBLE);
            view.thermostatalarm.setVisibility(View.INVISIBLE);
            view.thermostatvalue.setText("--\u2103" );
            view.thermostathy.setText("Hy:--%");
        }


        return rowView;
    }

    private static class ViewHolder {
        private TextView thermostatname;
        private TextView thermostatvalue;
        private TextView thermostathy;
        private ImageView thermostatalarm;
        private ImageView thermostatState;

    }
}
