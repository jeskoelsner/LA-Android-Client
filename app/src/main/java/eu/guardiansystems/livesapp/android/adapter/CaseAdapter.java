package eu.guardiansystems.livesapp.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;
import static eu.guardiansystems.livesapp.MqttApplication.APPLICATION;
import eu.guardiansystems.livesapp.R;
import eu.guardiansystems.livesapp.android.config.Base;
import eu.guardiansystems.livesapp.models.EmrCaseData;
import eu.guardiansystems.livesapp.models.Shared;

public class CaseAdapter extends ArrayAdapter<EmrCaseData> {

		private final static int RESSOURCE = R.layout.part_caselist_element;
		private final Context context;

		class ViewHolder {

				public TextView caseAddress;
				public TextView caseDistance;
				public Chronometer caseTime;
				public TextView caseVolunteers;
				public TextView caseDistanceBase;
		}

		public CaseAdapter(Context context) {
				super(context, RESSOURCE, APPLICATION.activeCases);
				this.context = context;
				this.setNotifyOnChange(true);
		}

		@Override
		public void add(EmrCaseData caseData) {
				APPLICATION.activeCases.add(caseData);
				notifyDataSetChanged();
		}

		public void removeCaseById(String caseId) {
				EmrCaseData upToDateCaseData = APPLICATION.getCaseById(caseId);
				if (upToDateCaseData != null) {
						APPLICATION.activeCases.remove(upToDateCaseData);
						notifyDataSetChanged();
				}
		}

		public void update(EmrCaseData caseData) {
				EmrCaseData oldCaseData = APPLICATION.getCaseById(caseData.getId());
				int entry = APPLICATION.activeCases.indexOf(oldCaseData);
				if (entry != -1) {
						Base.log("********** UPDATING CASE IN ADAPTER :) **********");
						APPLICATION.activeCases.remove(oldCaseData);
						APPLICATION.activeCases.add(entry, caseData);
						notifyDataSetChanged();
				} else {
						Base.log("********** CASEUPDATE FAILED SINCE CASE WAS NOT FOUND **********");
				}
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
				View view = convertView;
				// if view is not inflated yet, lets inflate it
				if (view == null) {
						LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						view = layoutInflater.inflate(RESSOURCE, null);
						ViewHolder viewHolder = new ViewHolder();
						viewHolder.caseAddress = (TextView) view.findViewById(R.id.caseAddress);
						viewHolder.caseVolunteers = (TextView) view.findViewById(R.id.caseMiniVolunteers);
						viewHolder.caseTime = (Chronometer) view.findViewById(R.id.caseMiniTimer);
						viewHolder.caseDistance = (TextView) view.findViewById(R.id.caseMiniDistance);
						viewHolder.caseDistanceBase = (TextView) view.findViewById(R.id.caseMiniDistanceBase);
						view.setTag(viewHolder);
				}

				final ViewHolder holder = (ViewHolder) view.getTag();
				holder.caseAddress.setText(getItem(position).getAddress());
				holder.caseVolunteers.setText("" + getItem(position).getVolunteers().size());
				holder.caseTime.setBase(getItem(position).getCreated() + getItem(position).getServer_offset());
				holder.caseTime.start();
				calculateDistance(getItem(position).getLocation(), holder);
				return view;
		}

		private void calculateDistance(LatLng caseLocation, ViewHolder holder) {
				LatLng userLocation = APPLICATION.getCurrentUser().getCurrentLocation();

				double distance = Shared.calculateDistance(caseLocation.latitude, caseLocation.latitude, userLocation.latitude, userLocation.longitude);
				if (distance > 10000) {
						holder.caseDistance.setText("10+");
						holder.caseDistanceBase.setText(context.getString(R.string.case_distance_kilometers));
				} else if (distance > 1000) {
						distance /= 1000;
						holder.caseDistance.setText(String.format("%.1f", distance));
						holder.caseDistanceBase.setText(context.getString(R.string.case_distance_kilometers));
				} else {
						holder.caseDistance.setText(String.format("%d", (int) distance));
						holder.caseDistanceBase.setText(context.getString(R.string.case_distance_meters));
				}
		}

}
