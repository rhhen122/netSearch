package org.netSearch.System;

import org.netSearch.util.netError;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class LoadChecker {
    public void LoadChecker() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        netError nsError = new netError();

        double loadAverage = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();
        boolean debugCheck = false;

        if (loadAverage > availableProcessors || debugCheck) {
            nsError.error("System is under intense load! Load: " + loadAverage, 2);
        }

        double cpuLoad = osBean.getCpuLoad();

        if (cpuLoad > 0.85) {
            nsError.error("CPU usage is critically high: " + (cpuLoad * 100) + "%", 2);
        }
    }
}
