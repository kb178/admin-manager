package com.ddd.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.ddd.admin.common.Result;
import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 系统硬件信息控制器 — 纯 JDK 实现，零外部依赖
 */
@Slf4j
@RestController
@RequestMapping("/system")
public class SystemController {

    private static final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final DecimalFormat df = new DecimalFormat("#.##");

    @GetMapping("/info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cpu", getCpuInfo());
        data.put("gpu", getGpuInfo());
        data.put("memory", getMemoryInfo());
        data.put("disk", getDiskInfo());
        data.put("os", getOsInfo());
        return Result.ok(data);
    }

    // ======================== CPU ========================

    private Map<String, Object> getCpuInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // CPU 型号
        String cpuName = getCpuName();
        info.put("name", cpuName);
        info.put("vendor", getCpuVendor(cpuName));
        info.put("physicalCores", Runtime.getRuntime().availableProcessors());
        info.put("logicalCores", Runtime.getRuntime().availableProcessors());

        // CPU 使用率
        double cpuLoad = osBean.getCpuLoad() * 100;
        info.put("usage", cpuLoad < 0 ? 0 : Double.parseDouble(df.format(cpuLoad)));

        // CPU 频率
        info.put("maxFreq", getCpuFreq());

        return info;
    }

    /**
     * 获取 CPU 名称（跨平台）
     */
    private String getCpuName() {
        // Windows: PROCESSOR_IDENTIFIER 环境变量
        String name = System.getenv("PROCESSOR_IDENTIFIER");
        if (StrUtil.isNotBlank(name)) return name.trim();

        // Linux: /proc/cpuinfo
        try {
            Process p = new ProcessBuilder("sh", "-c",
                    "grep -m1 'model name' /proc/cpuinfo | cut -d: -f2").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (StrUtil.isNotBlank(line)) return line.trim();
            }
        } catch (Exception ignored) {}

        // macOS: sysctl
        try {
            Process p = new ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (StrUtil.isNotBlank(line)) return line.trim();
            }
        } catch (Exception ignored) {}

        return System.getProperty("os.arch") + " CPU";
    }

    private String getCpuVendor(String name) {
        if (StrUtil.isBlank(name)) return "Unknown";
        String lower = name.toLowerCase();
        if (lower.contains("intel")) return "Intel";
        if (lower.contains("amd")) return "AMD";
        if (lower.contains("apple")) return "Apple";
        return "Unknown";
    }

    private String getCpuFreq() {
        // Windows: wmic
        try {
            Process p = new ProcessBuilder("wmic", "cpu", "get", "MaxClockSpeed").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("\\d+")) {
                        return df.format(Long.parseLong(line) / 1000.0) + " GHz";
                    }
                }
            }
        } catch (Exception ignored) {}

        // Linux: /proc/cpuinfo
        try {
            Process p = new ProcessBuilder("sh", "-c",
                    "grep -m1 'cpu MHz' /proc/cpuinfo | cut -d: -f2").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (StrUtil.isNotBlank(line)) {
                    return df.format(Double.parseDouble(line.trim()) / 1000.0) + " GHz";
                }
            }
        } catch (Exception ignored) {}

        return "未知";
    }

    // ======================== GPU ========================

    private List<Map<String, Object>> getGpuInfo() {
        List<Map<String, Object>> gpuList = new ArrayList<>();

        // Windows: wmic 获取 GPU 名称
        try {
            Process p = new ProcessBuilder("wmic", "path", "win32_VideoController",
                    "get", "name,AdapterRAM").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("Name")) continue;
                    // 格式: "NVIDIA GeForce RTX 3060    4294967296"
                    String[] parts = line.split("\\s{2,}");
                    if (parts.length >= 1) {
                        Map<String, Object> gpu = new LinkedHashMap<>();
                        gpu.put("name", parts[0].trim());
                        gpu.put("vendor", detectGpuVendor(parts[0]));
                        // VRAM
                        if (parts.length >= 2 && parts[1].trim().matches("\\d+")) {
                            gpu.put("vram", formatBytes(Long.parseLong(parts[1].trim())));
                        } else {
                            gpu.put("vram", "未知");
                        }
                        // NV 使用率
                        Map<String, String> usage = getNvidiaGpuUsage();
                        gpu.put("usage", usage.getOrDefault("usage", "N/A"));
                        gpu.put("temperature", usage.getOrDefault("temp", "N/A"));
                        gpuList.add(gpu);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("wmic GPU 查询失败: {}", e.getMessage());
        }

        if (gpuList.isEmpty()) {
            Map<String, Object> gpu = new LinkedHashMap<>();
            gpu.put("name", "未检测到独立显卡");
            gpu.put("vendor", "");
            gpu.put("vram", "共享内存");
            gpu.put("usage", "N/A");
            gpu.put("temperature", "N/A");
            gpuList.add(gpu);
        }
        return gpuList;
    }

    private String detectGpuVendor(String name) {
        if (StrUtil.isBlank(name)) return "";
        String l = name.toLowerCase();
        if (l.contains("nvidia")) return "NVIDIA";
        if (l.contains("amd") || l.contains("radeon")) return "AMD";
        if (l.contains("intel")) return "Intel";
        return "";
    }

    private Map<String, String> getNvidiaGpuUsage() {
        Map<String, String> r = new HashMap<>();
        try {
            Process p = new ProcessBuilder("nvidia-smi",
                    "--query-gpu=utilization.gpu,temperature.gpu",
                    "--format=csv,noheader,nounits").start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = br.readLine();
                if (StrUtil.isNotBlank(line)) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        r.put("usage", parts[0].trim() + "%");
                        r.put("temp", parts[1].trim() + "°C");
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return r;
    }

    // ======================== 内存 ========================

    private Map<String, Object> getMemoryInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        long total = osBean.getTotalMemorySize();
        long free = osBean.getFreeMemorySize();
        long used = total - free;

        info.put("total", formatBytes(total));
        info.put("used", formatBytes(used));
        info.put("available", formatBytes(free));
        info.put("usage", total > 0
                ? Double.parseDouble(df.format((double) used / total * 100)) : 0);
        return info;
    }

    // ======================== 磁盘 ========================

    private Map<String, Object> getDiskInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        List<Map<String, Object>> disks = new ArrayList<>();
        long totalAll = 0, freeAll = 0;

        for (File root : File.listRoots()) {
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            if (total == 0) continue;
            totalAll += total;
            freeAll += free;

            Map<String, Object> disk = new LinkedHashMap<>();
            disk.put("mount", root.getPath());
            disk.put("total", formatBytes(total));
            disk.put("free", formatBytes(free));
            disk.put("usage", Double.parseDouble(
                    df.format((double) (total - free) / total * 100)));
            disks.add(disk);
        }

        info.put("totalAll", formatBytes(totalAll));
        info.put("freeAll", formatBytes(freeAll));
        info.put("usageAll", totalAll > 0
                ? Double.parseDouble(df.format((double) (totalAll - freeAll) / totalAll * 100))
                : 0);
        info.put("disks", disks);
        return info;
    }

    // ======================== 操作系统 ========================

    private Map<String, Object> getOsInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        info.put("manufacturer", System.getProperty("os.name").startsWith("Win") ? "Microsoft" : "Unknown");
        info.put("uptime", formatUptime(
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000));
        return info;
    }

    // ======================== 工具 ========================

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] u = {"B", "KB", "MB", "GB", "TB"};
        int idx = (int) (Math.log(bytes) / Math.log(1024));
        idx = Math.min(idx, u.length - 1);
        return df.format(bytes / Math.pow(1024, idx)) + " " + u[idx];
    }

    private String formatUptime(long seconds) {
        if (seconds < 60) return seconds + "秒";
        long d = seconds / 86400, h = (seconds % 86400) / 3600, m = (seconds % 3600) / 60;
        if (d > 0) return d + "天 " + h + "小时 " + m + "分";
        if (h > 0) return h + "小时 " + m + "分";
        return m + "分";
    }
}
