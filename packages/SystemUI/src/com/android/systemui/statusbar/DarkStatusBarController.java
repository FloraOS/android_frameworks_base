package com.android.systemui.statusbar;

import com.android.systemui.tuner.TunerService;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.data.repository.StatusBarModePerDisplayRepository;
import javax.inject.Inject;

/**
 * @LineageExtension
 * Controls the dark status bar feature.
 */
public class DarkStatusBarController implements TunerService.Tunable {

    private static final String DARK_MODE_KEY = "dark_statusbar";  // Define your key

    private final StatusBarModePerDisplayRepository statusBarModeRepository;
    private final TunerService tunerService;

    @Inject
    public DarkStatusBarController(StatusBarModePerDisplayRepository repository) {
        TunerService tunerService = Dependency.get(TunerService.class); 
	this.statusBarModeRepository = repository;
        this.tunerService = tunerService;
        
        // Register for tuning updates
        tunerService.addTunable(this, DARK_MODE_KEY);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (DARK_MODE_KEY.equals(key)) {
            boolean isDarkModeEnabled = "1".equals(newValue); // Assuming "1" enables dark mode
            updateStatusBarMode(isDarkModeEnabled);
        }
    }

    private void updateStatusBarMode(boolean isDarkModeEnabled) {
        // Update the mode in the repository based on the dark mode setting
        statusBarModeRepository.setDarkModeEnabled(isDarkModeEnabled);
    }

    public void unregister() {
        // Unregister from TunerService to avoid leaks when not needed
        tunerService.removeTunable(this);
    }
}

