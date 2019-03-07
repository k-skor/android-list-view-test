package com.example.listviewtestapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    class SomeListAdapter extends ArrayAdapter<String> {

        class ViewHolder {
            EditText et1;
            //TextWatcher tw1;
            EditText et2;
            //TextWatcher tw2;
            int position;
            LinearLayout trigger;
            TextView title;
            ConstraintLayout content;
        }

        public SomeListAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            ViewHolder vh;
            if (view == null) {
                view = super.getView(position, convertView, parent);
                vh = new ViewHolder();
                vh.et1 = view.findViewById(R.id.editText1);
                vh.et2 = view.findViewById(R.id.editText2);
                vh.et1.setOnFocusChangeListener(create1FocusLsnr(vh));
                vh.et2.setOnFocusChangeListener(create2FocusLsnr(vh));
                vh.position = position;
                vh.trigger = view.findViewById(R.id.trigger);
                vh.title = view.findViewById(R.id.title);
                vh.content = view.findViewById(R.id.content);
                vh.title.setText(getItem(position));
                view.setTag(vh);
            } else {
                vh = (ViewHolder) view.getTag();
                if (vh.position != position) {
                    vh.position = position;
                    vh.title.setText(getItem(position));
                }
            }
            return view;
        }

        View.OnClickListener createOnClickLsnr(final ViewHolder viewHolder) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean expanded = viewHolder.content.getVisibility() == View.VISIBLE;

                }
            };
        }

        View.OnFocusChangeListener create1FocusLsnr(final ViewHolder viewHolder) {
            return new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    Log.i("some item", String.format("[1]onFocusChange: has focus: %b, position: %d", hasFocus, viewHolder.position));
                }
            };
        }

        View.OnFocusChangeListener create2FocusLsnr(final ViewHolder viewHolder) {
            return new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    Log.i("some item", String.format("[2]onFocusChange: has focus: %b, position: %d", hasFocus, viewHolder.position));
                }
            };
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView someList = findViewById(R.id.some_list);
        String[] data = new String[32];
        for (int i = 0; i < 32; i++) {
            data[i] = "Some text " + i;
        }
        SomeListAdapter adapter = new SomeListAdapter(this, R.layout.some_list_item, data);
        someList.setAdapter(adapter);
    }
}
