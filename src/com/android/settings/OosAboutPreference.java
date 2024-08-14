package com.android.settings;

import android.content.Intent;
import android.os.Build;
import android.os.SystemProperties;
import com.android.settingslib.DeviceInfoUtils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.PhoneData;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import com.android.settings.core.BasePreferenceController;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.widget.LayoutPreference;
import com.airbnb.lottie.LottieAnimationView;

public class OosAboutPreference extends BasePreferenceController implements View.OnTouchListener {

    Context context;
    List<AboutPhoneData> data = PhoneData.getData();

    public OosAboutPreference(Context context, String key) {
        super(context, key);
        this.context = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }


    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference mPreference = screen.findPreference("infinity_about_layout");
	onBindItems(mPreference.findViewById(R.id.oos_about_root));
    }

    private static int findIndex(
            String name) {

        for (AboutPhoneData phoneData : PhoneData.data) {
            if (phoneData.getCodename().equals(name)) {
                return phoneData.getIndex();
            }
        }
        return 0;
    }

    public static void setInfo(String prop, TextView textview) {
        if (TextUtils.isEmpty(SystemProperties.get(prop))) {
            textview.setText("Unknown");
        } else {

            textview.setText(SystemProperties.get(prop));
        }
    }
    
    LottieAnimationView greenAnimationView;
    LottieAnimationView redAnimationView;

    public void onBindItems(View holder) {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        int total = 0;
        double totalInt = 0.0;
        try {
             totalInt = (statFs.getBlockSizeLong() * statFs.getBlockCountLong() / Math.pow(1024, 3));
             
             if (totalInt > 0 && totalInt < 17)
        	total = 16;
    	     else if (totalInt > 16 && totalInt < 33)
        	total = 32;
	     else if (totalInt > 32 && totalInt < 65)
        	total = 64;
	     else if (totalInt > 64 && totalInt < 129)
        	total = 128;
	     else if (totalInt > 128 && totalInt < 257)
                total = 256;
	    else if (totalInt > 256 && totalInt < 513)
                total = 512;
        } catch (NumberFormatException e) {
    	     Log.e("OosAboutPreference", "Error parsing totalInt: " + e.getMessage());
        }
            
        View root = holder;
        int index = findIndex(SystemProperties.get("ro.product.device"));

        TextView display = null, battery = null, soc = null, cam = null,
                device, deviceSec, infinity_version, infinity_status,
                kernel, maintainer, rom;

        View leftMini, rightMini;

        leftMini = root.findViewById(R.id.leftFinalDetail);
        rightMini = root.findViewById(R.id.righFinalDetail);
        rom = root.findViewById(R.id.rom_about);
        device = root.findViewById(R.id.device_name);
        deviceSec = root.findViewById(R.id.security_update);
        kernel = root.findViewById(R.id.kernel_version);
        maintainer = root.findViewById(R.id.infinity_maintainer);
        infinity_version = root.findViewById(R.id.infinity_version);
	infinity_status = root.findViewById(R.id.infinity_status);
	greenAnimationView = holder.findViewById(R.id.green_animation_view);
        redAnimationView = holder.findViewById(R.id.red_animation_view);
        
        String buildType = SystemProperties.get("ro.infinity.buildtype");
        
        if (buildType.equalsIgnoreCase("OFFICIAL")) {
            greenAnimationView.setVisibility(View.VISIBLE);
            redAnimationView.setVisibility(View.GONE);
    	} else {
            greenAnimationView.setVisibility(View.GONE);
            redAnimationView.setVisibility(View.VISIBLE);
        }

        TextView[] var = {display, battery, soc, cam};
        int[] ids = {R.id.display_about, R.id.battery_about, R.id.soc_about, R.id.camera_about, R.id.device_about};
        final String[] texts = {
                data.get(index).getDisplay(), data.get(index).getBattery(),
                data.get(index).getSoc(), data.get(index).getCamera(),
        };

        for (int i = 0; i < var.length; i++) {
            var[i] = root.findViewById(ids[i]);
                if (var[i].getId() == R.id.camera_about && texts[i].length() > 16)
                    var[i].setTextSize(12);
                if (!texts[i].equals("0"))
                    var[i].setText(texts[i]);
		else var[i].setText(texts[0]);
        }

	infinity_version.setText(String.format("Infinity-X v%s", SystemProperties.get("ro.infinity.version")));
        setInfo("ro.infinity.build.status", infinity_status);
        setInfo("ro.infinity.maintainer", maintainer);
        deviceSec.setText(DeviceInfoUtils.getSecurityPatch());
        kernel.setText(DeviceInfoUtils.getFormattedKernelVersion(context));
	String modelName = SystemProperties.get("ro.product.model");
        String marketName = SystemProperties.get("ro.product.marketname");
        String infinityDevice = SystemProperties.get("ro.infinity.device");
	    if (!TextUtils.isEmpty(marketName)) {
		device.setText(marketName + " (" + infinityDevice + ")");
	    } else if (!TextUtils.isEmpty(modelName)) {
		device.setText(modelName + " (" + infinityDevice + ")");
            } else {
                device.setText(infinityDevice);
	}
        
        if (index != 0) {
	    double ramInGB = Double.parseDouble(getMem()) / Math.pow(1000, 2);
	    int roundedRamInGB = (int) Math.ceil(ramInGB);
	    
	    rom.setText(String.format(Locale.ENGLISH, "%dGB RAM + %dGB ROM", roundedRamInGB, total));
	    if (rom.getText().length() >= 14) rom.setTextSize(12);
	} else {
	    rom.setText(texts[0]);
	}
        
        leftMini.setOnTouchListener(this);
        rightMini.setOnTouchListener(this);

	final Intent intent = new Intent(Intent.ACTION_MAIN)
                    .setClassName(
                            "android", com.android.internal.app.PlatLogoActivity.class.getName());

	ImageView androidversion = root.findViewById(R.id.android_version);

	androidversion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
            try {
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
            }
                return false;
            }
        });

    }

    private String getMem() {
    	if (getMemInfoMap().containsKey("MemTotal")) {
            String s = getMemInfoMap().get("MemTotal");
            return s.split(" ")[0];
        }
        return "Unknown";
    }


    private Map<String, String> getMemInfoMap() {
        Map<String, String> map = new HashMap<>();
        try {
            Scanner s = new Scanner(new File("/proc/meminfo"));
            while (s.hasNextLine()) {
                String[] vals = s.nextLine().split(": ");
                if (vals.length > 1)
                    map.put(vals[0].trim(), vals[1].trim());
            }
        } catch (Exception e) {
            Log.e("getMemInfoMap", Log.getStackTraceString(e));
        }
        return map;
    }
    
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            v.animate().scaleX(.97f).setDuration(300).start();
            v.animate().scaleY(.97f).setDuration(300).start();
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            v.animate().cancel();
            v.animate().scaleX(1f).setDuration(500).start();
            v.animate().scaleY(1f).setDuration(500).start();
            return true;
        }
        return false;
    }
}
