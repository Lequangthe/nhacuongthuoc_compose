/* (C) 2022 */
package com.quangthe.nhacnho_uongthuoc;

import static com.quangthe.nhacnho_uongthuoc.Pill.PILL_TAKEN_VALUE;
import static com.quangthe.nhacnho_uongthuoc.Pill.PILL_TAKEN_VIA_NOTIFICATION_INTENT_KEY;
import static com.quangthe.nhacnho_uongthuoc.Pill.PRIMARY_KEY_INTENT_KEY_STRING;

import com.quangthe.nhacnho_uongthuoc.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.google.android.material.chip.ChipGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

public class MainRecyclerViewAdapter
        extends RecyclerView.Adapter<MainRecyclerViewAdapter.MyViewHolder> {

    SharedPrefs sharedPrefs;
    Dialogs dialogs;
    DatabaseHelper myDatabase;
    AudioHelper audioHelper;
    Toasts toasts;
    DateTimeManager dateTimeManager;

    String pillName;
    final int alarmCodeForAllAlarms = 0;
    final Context context;
    final MainActivity mainActivity;
    Activity myActivity;
    Typeface interReg, interMed;
    MediaPlayer takenMediaPlayer, resetMediaPlayer;

    Pill[] pills;

    MainRecyclerViewAdapter(MainActivity mainActivity, Activity myActivity, Context context) {
        this.myActivity = myActivity;
        this.mainActivity = mainActivity;
        this.pills = mainActivity.pills;
        this.context = context;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder
            implements View.OnCreateContextMenuListener {

        final ConstraintLayout constraintLayout;
        final TextView pillTimeTextView;
        final TextView pillNameTextView;
        final ImageButton takenBtn;
        final ImageButton resetBtn;
        final ImageButton deleteBtn;
        final Button bigButton;
        final ImageView pillBottleImage;
        final ChipGroup doseLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            constraintLayout =
                    itemView.findViewById(R.id.pill_recycler_view_item_constraint_layout);
            pillNameTextView = itemView.findViewById(R.id.pillName);
            pillTimeTextView = itemView.findViewById(R.id.pillTime);
            takenBtn = itemView.findViewById(R.id.tickButton);
            resetBtn = itemView.findViewById(R.id.resetButton);
            deleteBtn = itemView.findViewById(R.id.deleteButton);
            pillBottleImage = itemView.findViewById(R.id.pill_bottle_image);
            bigButton = itemView.findViewById(R.id.bigButton);
            doseLayout = itemView.findViewById(R.id.dose_layout);
            pillBottleImage.setOnCreateContextMenuListener(this);
            bigButton.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(
                ContextMenu menu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            menu.add(this.getAbsoluteAdapterPosition() + 1, 1, 0, R.string.context_menu_update);
            menu.add(this.getAbsoluteAdapterPosition() + 1, 2, 0, R.string.context_menu_delete);
            menu.add(
                    this.getAbsoluteAdapterPosition() + 1,
                    3,
                    0,
                    R.string.context_menu_change_color);
            menu.add(this.getAbsoluteAdapterPosition() + 1, 4, 0, R.string.context_menu_test_dose);
        }
    }

    @Override
    public int getItemCount() {
        return pills.length;
    }

    @NonNull @Override
    public MainRecyclerViewAdapter.MyViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.example_pill_new, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Pill currentPill = pills[position];
        checkForNotificationOpenedOnAppStart(holder, position);
        initAll(holder, currentPill, position);
    }

    private void checkForNotificationOpenedOnAppStart(MyViewHolder holder, int position) {
        Intent mainActivityIntent = mainActivity.getIntent();
        if (mainActivityIntent.hasExtra(PILL_TAKEN_VIA_NOTIFICATION_INTENT_KEY)) {
            int pk = mainActivityIntent.getIntExtra(PILL_TAKEN_VIA_NOTIFICATION_INTENT_KEY, -1);
            ArrayHelper arrayHelper = new ArrayHelper();
            int pillPosition = arrayHelper.findPillUsingPrimaryKey(pills, pk);
            if (pillPosition == position)
                holder.constraintLayout.startAnimation(
                        AnimationUtils.loadAnimation(context, R.anim.recycler_item_enlarge));
        }
    }

    private void initAll(MyViewHolder holder, Pill pill, int position) {

        initClasses();
        initTextViews(holder, pill);
        initBottleImage(holder, pill);
        initButtons(holder, pill, position);
    }

    private void initClasses() {
        myActivity = new Activity();
        dialogs = new Dialogs(context);
        myDatabase = new DatabaseHelper(context);
        sharedPrefs = new SharedPrefs(context);
        audioHelper = new AudioHelper(context);
        toasts = new Toasts(context);
        dateTimeManager = new DateTimeManager();
    }

    private void initTextViews(MyViewHolder holder, Pill pill) {
        interReg = ResourcesCompat.getFont(context, R.font.inter_reg);
        interMed = ResourcesCompat.getFont(context, R.font.inter_medium);

        holder.pillNameTextView.setTypeface(interMed);
        holder.pillTimeTextView.setTypeface(interMed);
        holder.pillNameTextView.setTextSize(27.0f);
        holder.pillTimeTextView.setTextSize(15.0f);

        holder.pillNameTextView.setText(pill.getName());

        String times =
                sharedPrefs.get24HourFormatPref()
                        ? pill.getTimes24HrFormat()
                        : pill.getTimes12HrFormat();
        String frequencyString;

        int pillFrequency = pill.getFrequency();

        switch (pillFrequency) {
            case 0:
            case 1:
                times = context.getString(R.string.take_at, times);
                break;
            case 2:
                frequencyString =
                        context.getString(R.string.choose_frequency_dialog_every_other_day);
                times =
                        context.getString(
                                R.string.take_at_custom_interval, times, frequencyString);
                break;
            case 7:
                frequencyString = context.getString(R.string.choose_frequency_dialog_weekly);
                times =
                        context.getString(
                                R.string.take_at_custom_interval, times, frequencyString);
                break;
            default:
                frequencyString =
                        context.getString(
                                R.string.append_days_to_custom_interval, pillFrequency);
                times =
                        context.getString(
                                R.string.take_at_custom_interval, times, frequencyString);
                break;
        }

        holder.pillTimeTextView.setText(times);

        // Hide old buttons, we'll use chips
        holder.takenBtn.setVisibility(View.GONE);
        holder.resetBtn.setVisibility(View.GONE);

        // Populate dose chips
        holder.doseLayout.removeAllViews();
        String[] doseTimes = pill.getTimesArray();
        for (int i = 0; i < doseTimes.length; i++) {
            final int doseIndex = i;
            TextView doseChip = new TextView(context);
            
            doseChip.setPadding(24, 8, 24, 8);
            doseChip.setTextSize(14f);
            doseChip.setTypeface(interMed);
            
            String displayTime = sharedPrefs.get24HourFormatPref() ? doseTimes[i] : dateTimeManager.convert24HrTimeTo12HrTime(doseTimes[i]);
            doseChip.setText(displayTime);

            if (pill.isDoseTaken(i)) {
                doseChip.setBackground(AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_green));
                doseChip.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.white, null));
            } else {
                doseChip.setBackground(AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_dark));
                doseChip.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.alice_blue, null));
            }

            doseChip.setOnClickListener(v -> {
                if (pill.isDoseTaken(doseIndex)) {
                    pill.setDoseTaken(doseIndex, false);
                    pill.setSupply(pill.getSupply() + 1);
                } else {
                    pill.takePill(context, doseIndex);
                    if (sharedPrefs.getPillSoundPref()) audioHelper.getTakenPlayer().start();
                    toasts.showCustomToast(context.getString(R.string.pill_taken_toast, pill.getName()));
                }
                pill.updatePillInDatabase(context);
                notifyItemChanged(holder.getAbsoluteAdapterPosition());
            });

            doseChip.setOnLongClickListener(v -> {
                toasts.showCustomToast("Đang test thông báo cữ " + displayTime + "...");
                pill.sendPillNotification(context, doseIndex);
                return true;
            });
            
            holder.doseLayout.addView(doseChip);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            holder.pillNameTextView.setContextClickable(true);
            holder.pillTimeTextView.setContextClickable(true);
        }
    }

    private void initBottleImage(MyViewHolder holder, Pill pill) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            holder.pillBottleImage.setContextClickable(true);
        }

        holder.pillBottleImage.setImageDrawable(pill.getBottleDrawable(context));

        holder.pillBottleImage.setOnClickListener(
                v -> {
                    holder.constraintLayout.startAnimation(
                            AnimationUtils.loadAnimation(context, R.anim.bottle_shake));
                    if ((pill.getSupply() > 0)) {
                        toasts.showCustomToast(
                                context.getString(
                                        R.string.pill_bottle_amount_toast,
                                        pill.getSupply(),
                                        pill.getName()));
                        if (sharedPrefs.getPillSoundPref()) audioHelper.getShakePlayer().start();
                    }
                });
    }

    private void initButtons(MyViewHolder holder, Pill pill, int position) {
        holder.bigButton.setOnClickListener(
                view -> {
                    Intent intent = new Intent(context, CreatePill.class);
                    intent.putExtra(PRIMARY_KEY_INTENT_KEY_STRING, pill.getPrimaryKey());
                    context.startActivity(intent);
                    MainActivity.backPresses = 0;
                });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            holder.bigButton.setContextClickable(true);
        }
    }
}
