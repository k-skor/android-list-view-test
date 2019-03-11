package com.example.listviewtestapp;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    public static class SetListViewHeightTask implements Runnable {
        WeakReference<ListView> listView;
        int extraHeight;
        boolean removed = false;
        WeakReference<View> focusView;

        public SetListViewHeightTask(ListView listView, int extraHeight, View focusView) {
            this.listView = new WeakReference<>(listView);
            this.extraHeight = extraHeight;
            this.focusView = new WeakReference<>(focusView);
        }

        @Override
        public void run() {
            ListView listView = this.listView.get();
            if (removed || listView == null) {
                return;
            }
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            int output1Count = listView.getAdapter().getCount();
            params.height = 0;
            if (output1Count > 0) {
                params.height += output1Count * (int)listView.getContext().getResources().getDimension(R.dimen.list_item_height) + ((listView.getDividerHeight())  * (output1Count - 1));
            }
            params.height += extraHeight;
            listView.setLayoutParams(params);
            listView.post(() -> {
                View focusView = this.focusView.get();
                if (focusView != null) {
                    focusView.requestFocus();
                    if (focusView instanceof EditText) {
                        focusView.post(() -> {
                            EditText fv = (EditText) focusView;
                            int position = fv.length();
                            Editable input = fv.getText();
                            Selection.setSelection(input, position);
                            Selection.selectAll(input);
                        });
                    }
                }
            });
        }
    }

    class SomeListAdapter extends ArrayAdapter<Integer> {

        class ViewHolder {
            EditText et1;
            EditText et2;
            int position;
            LinearLayout trigger;
            TextView title;
            ConstraintLayout content;
            //int lastEt1Focus = -1;
            //int lastEt2Focus = -1;
            //boolean expanded = false;
        }

        class ViewState {
            int lastEt1Focus = -1;
            int lastEt2Focus = -1;
            boolean expanded = false;
        }

        private int mListExtraSizeBalance = 0;
        private final float expandableHeight;
        private Handler mHandler;
        private SetListViewHeightTask mSetListViewHeight;
        private boolean mInit = false;
        private int mLastEtPosition = -1;
        private final SparseArray<ViewState> mViewsStates;
        private ScrollView mScrollView;

        public SomeListAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull Integer[] objects, ScrollView scrollView) {
            super(context, resource, textViewResourceId, objects);
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            expandableHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96, dm);
            mHandler = new Handler();
            mViewsStates = new SparseArray<>();
            mScrollView = scrollView;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            ViewHolder vh;
            if (view == null) {
                Log.i("some list", "getView: new @" + position);
                view = super.getView(position, convertView, parent);
                vh = new ViewHolder();
                mViewsStates.append(position, new ViewState());
                vh.et1 = view.findViewById(R.id.editText1);
                vh.et2 = view.findViewById(R.id.editText2);
                vh.et1.setOnFocusChangeListener(create1FocusLsnr(vh, (ListView) parent));
                vh.et2.setOnFocusChangeListener(create2FocusLsnr(vh));
                vh.position = position;
                vh.trigger = view.findViewById(R.id.trigger);
                vh.title = view.findViewById(R.id.title);
                vh.content = view.findViewById(R.id.content);
                int value = getItem(position);
                vh.et1.setText(String.format("%d", value));
                vh.et2.setText(String.format("%d%d", value, value));
                vh.title.setText(String.format("Some text %d", value));
                updateListHeight((ListView) parent, null);
                vh.trigger.setOnClickListener(createOnClickLsnr(vh, (ListView) parent));
                if (!mInit) {
                    vh.et1.requestFocus();
                    mInit = true;
                }
                view.setTag(vh);
            } else {
                vh = (ViewHolder) view.getTag();
                Log.i("some list", "getView: reuse @" + position + " holder @" + vh.position);
                if (vh.position != position) {
                    int holderPos = vh.position;
                    vh.position = position;
                    int value = getItem(position);
                    vh.et1.setText(String.format("%d", value));
                    vh.et2.setText(String.format("%d%d", value, value));
                    vh.title.setText(String.format("Some text %d", value));
                    vh.trigger.setOnClickListener(createOnClickLsnr(vh, (ListView) parent));
                    ViewState oldVs = mViewsStates.get(holderPos);
                    ViewState vs = mViewsStates.get(position);
                    if (vs == null) {
                        Log.i("some list", "getView: create new state");
                        vs = new ViewState();
                        mViewsStates.append(position, vs);
                    }
                    if (vh.position == oldVs.lastEt1Focus) {
                        vh.et1.clearFocus();
                    }
                    if (vh.position == oldVs.lastEt2Focus) {
                        vh.et2.clearFocus();
                    }
                    if (vs.expanded && vh.content.getVisibility() != View.VISIBLE) {
                        Log.i("some list", "getView: expanding");
                        toggleExpandableMenu(true, vh);
                    }
                    if (vs.lastEt1Focus != -1) {
                        vh.et1.requestFocus();
                    }
                    if (vs.lastEt2Focus != -1) {
                        vh.et2.requestFocus();
                    }
                    if (!vs.expanded && vh.content.getVisibility() == View.VISIBLE) {
                        Log.i("some list", "getView: collapsing");
                        toggleExpandableMenu(false, vh);
                    }
                    if (vs.lastEt1Focus == -1) {
                        vh.et1.clearFocus();
                    }
                    if (vs.lastEt2Focus == -1) {
                        vh.et2.clearFocus();
                    }
                }
            }
            return view;
        }

        View.OnClickListener createOnClickLsnr(ViewHolder viewHolder, ListView listView) {
            return v -> {
                Log.i("some list", "createOnClickLsnr: ");
                boolean expanded = viewHolder.content.getVisibility() == View.VISIBLE;
                toggleExpandableMenu(!expanded, viewHolder);
                updateListHeight(listView, !expanded ? getCurrentFocus() : null);
            };
        }

        View.OnFocusChangeListener create1FocusLsnr(final ViewHolder viewHolder, ListView listView) {
            return (v, hasFocus) -> {
                Log.i("some item", String.format("[1]onFocusChange: has focus: %b, position: %d", hasFocus, viewHolder.position));
                ViewState vs = mViewsStates.get(viewHolder.position);
                if (hasFocus && vs.lastEt1Focus == -1) {
                    if (viewHolder.position > 0 && mLastEtPosition == viewHolder.position - 1) {
                        Log.i("some item", "[1]onFocusChange: expand content");
                        toggleExpandableMenu(true, viewHolder);
                        updateListHeight(listView, getCurrentFocus());
                        mLastEtPosition = -1;
                    }
                    //Log.i("some item", "[1]onFocusChange: get focus");
                    vs.lastEt1Focus = viewHolder.position;
                    viewHolder.et1.post(viewHolder.et1::selectAll);
                    if (viewHolder.position > 0) {
                        listView.post(() -> listView.smoothScrollToPosition(viewHolder.position + 1));
                    }
                    /*if (viewHolder.position > 0) {
                        listView.setSelection(viewHolder.position - 1);
                    }*/
                    /*if (viewHolder.position > 0) {
                        //mScrollView.scrollTo(0, viewHolder.et1.getBottom());
                        mScrollView.post(() -> mScrollView.scrollTo(0, viewHolder.et1.getBottom()));
                    }*/
                    //viewHolder.et1.post(() -> viewHolder.et1.getParent().requestChildFocus(viewHolder.et1, viewHolder.et1));
                }
                if (!hasFocus) {
                    vs.lastEt1Focus = -1;
                }
            };
        }

        View.OnFocusChangeListener create2FocusLsnr(final ViewHolder viewHolder) {
            return (v, hasFocus) -> {
                Log.i("some item", String.format("[2]onFocusChange: has focus: %b, position: %d", hasFocus, viewHolder.position));
                ViewState vs = mViewsStates.get(viewHolder.position);
                if (hasFocus && vs.lastEt2Focus == -1) {
                    mLastEtPosition = viewHolder.position;
                    vs.lastEt2Focus = viewHolder.position;
                    viewHolder.et2.post(viewHolder.et2::selectAll);
                    /*if (viewHolder.position > 0) {
                        //mScrollView.scrollTo(0, viewHolder.et2.getBottom());
                        mScrollView.post(() -> mScrollView.scrollTo(0, viewHolder.et2.getBottom()));
                    }*/
                    //viewHolder.et2.post(() -> viewHolder.et2.getParent().requestChildFocus(viewHolder.et2, viewHolder.et2));
                }
                if (!hasFocus) {
                    vs.lastEt2Focus = -1;
                }
            };
        }

        void updateListHeight(ListView listView, View focusView) {
            if (mSetListViewHeight != null) {
                mHandler.removeCallbacks(mSetListViewHeight);
                mSetListViewHeight.removed = true;
            }
            mSetListViewHeight = new SetListViewHeightTask(listView, mListExtraSizeBalance, focusView);
            mHandler.post(mSetListViewHeight);
        }

        void toggleExpandableMenu(boolean expand, ViewHolder viewHolder) {
            ViewState vs = mViewsStates.get(viewHolder.position);
            mListExtraSizeBalance += expand ? expandableHeight : -expandableHeight;
            viewHolder.content.setVisibility(expand ? View.VISIBLE : View.GONE);
            vs.expanded = expand;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView someList = findViewById(R.id.some_list);
        Integer[] data = new Integer[32];
        for (int i = 0; i < 32; i++) {
            data[i] = i + 1;
        }
        ScrollView sv = findViewById(R.id.scroll_view);
        SomeListAdapter adapter = new SomeListAdapter(this, R.layout.some_list_item, R.id.title, data, sv);
        someList.setAdapter(adapter);
        Runnable setHeightTask = new SetListViewHeightTask(someList, 0, null);
        setHeightTask.run();
        someList.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.i("some list", "onFocusChange: list has focus: " + hasFocus);
            }
        });
    }
}
