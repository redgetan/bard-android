package com.roplabs.bard.adapters;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.roplabs.bard.R;
import com.roplabs.bard.ui.activity.BardEditorActivity;

import java.util.ArrayList;
import java.util.List;

public class ShareListAdapter extends ArrayAdapter<String> {
    private PackageManager packageManager;

    public ShareListAdapter(Context context, String apps[]) {
        super(context, 0, apps);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String app = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.share_list_item, parent, false);
        }

        ImageView shareIcon = (ImageView) convertView.findViewById(R.id.share_icon);
        TextView shareName = (TextView) convertView.findViewById(R.id.share_name);

        shareName.setText(app);

        if (app.equals("messenger")) {
            shareIcon.setBackgroundResource(R.drawable.facebookmessenger);
        } else if (app.equals("whatsapp")) {
            shareIcon.setBackgroundResource(R.drawable.whatsapp);
        } else if (app.equals("telegram")) {
            shareIcon.setBackgroundResource(R.drawable.telegram);
        } else if (app.equals("kik")) {
            shareIcon.setBackgroundResource(R.drawable.kik);
        } else if (app.equals("tumblr")) {
            shareIcon.setBackgroundResource(R.drawable.tumblr);
        } else {
            shareIcon.setBackgroundResource(R.drawable.twitter);
        }

        convertView.setTag(app);

        return convertView;
    }
}
