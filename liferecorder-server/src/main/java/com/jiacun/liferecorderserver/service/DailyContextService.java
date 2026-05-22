package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jiacun.liferecorderserver.dto.DailyContextData;
import com.jiacun.liferecorderserver.dto.DailyContextRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Service
public class DailyContextService {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private LifeIndexService lifeIndexService;

    @Autowired
    private LifeChangesService lifeChangesService;

    private static final Path BASE_DIR = Paths.get("D:/LifeRecorder/days");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final ObjectMapper objectMapper;
    private final OpenMeteoService openMeteoService;

    public DailyContextService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.openMeteoService = new OpenMeteoService();
    }

    /**
     * 处理 Daily Context 请求
     */
    public String processDailyContext(DailyContextRequest request) {
        try {
            // 验证日期格式
            String dateStr = request.getDate();
            if (dateStr == null || !dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return "日期格式错误，必须是 yyyy-MM-dd";
            }
            
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);

            // 创建完整的 DailyContextData
            DailyContextData contextData = buildDailyContext(request, date);

            // 保存文件
            Path filePath = saveDailyContext(contextData, date);

            // ===== 新增：写入 Workspace Protocol v1 新结构 =====
            writeToNewStructure(contextData, date);

            return "daily_context.json 已保存：" + filePath.toString();

        } catch (Exception e) {
            return "处理失败：" + e.getMessage();
        }
    }

    /**
     * 构建完整的 DailyContextData
     */
    private DailyContextData buildDailyContext(DailyContextRequest request, LocalDate date) {
        DailyContextData context = new DailyContextData(
            request.getDate(),
            request.getTimezone() != null ? request.getTimezone() : "Asia/Shanghai"
        );

        // 1. Location - 基于城市
        String city = request.getCity() != null ? request.getCity() : "未知";
        String country = request.getCountry() != null ? request.getCountry() : "China";
        context.setLocation(new DailyContextData.Location(city, country));

        // 2. Weather - 查询天气（失败不影响整体）
        DailyContextData.Weather weather = fetchWeather(city, country, context);
        context.setWeather(weather);

        // 3. Health - 暂时全为 null
        context.setHealth(new DailyContextData.Health());

        // 4. Device - 从请求中获取
        Integer batteryPercent = request.getBatteryPercent();
        Boolean isCharging = request.getIsCharging();
        String networkType = request.getNetworkType();
        context.setDevice(new DailyContextData.Device(batteryPercent, isCharging, networkType));

        // 5. App - 暂时全为 null
        context.setApp(new DailyContextData.App());

        // 6. Privacy - 默认值
        context.setPrivacy(new DailyContextData.Privacy());

        // 7. 时间戳
        Long createdTime = request.getCreatedTime() != null ? request.getCreatedTime() : System.currentTimeMillis();
        Long updatedTime = request.getUpdatedTime() != null ? request.getUpdatedTime() : System.currentTimeMillis();
        context.setCreatedTime(createdTime);
        context.setUpdatedTime(updatedTime);

        return context;
    }

    /**
     * 查询天气信息
     */
    private DailyContextData.Weather fetchWeather(String city, String country, DailyContextData context) {
        DailyContextData.Weather weather = new DailyContextData.Weather();
        weather.setProvider("open-meteo");

        try {
            Map<String, Object> weatherData = openMeteoService.getWeatherByCity(city, country);

            if (weatherData.containsKey("error")) {
                // 天气查询失败
                weather.setError((String) weatherData.get("error"));
                weather.setFetchedTime((Long) weatherData.get("fetchedTime"));
            } else {
                // 天气查询成功
                weather.setCondition((String) weatherData.get("condition"));
                weather.setWeatherCode((Integer) weatherData.get("weatherCode"));
                weather.setTemperatureC((Double) weatherData.get("temperatureC"));
                weather.setApparentTemperatureC((Double) weatherData.get("apparentTemperatureC"));
                weather.setRelativeHumidity((Integer) weatherData.get("relativeHumidity"));
                weather.setPrecipitationMm((Double) weatherData.get("precipitationMm"));
                weather.setWindSpeedKmh((Double) weatherData.get("windSpeedKmh"));
                weather.setFetchedTime((Long) weatherData.get("fetchedTime"));
                weather.setError(null);

                // 更新 location 的经纬度
                if (context.getLocation() != null) {
                    context.getLocation().setLatitude((Double) weatherData.get("latitude"));
                    context.getLocation().setLongitude((Double) weatherData.get("longitude"));
                }
            }

        } catch (Exception e) {
            weather.setError("天气查询异常：" + e.getMessage());
            weather.setFetchedTime(System.currentTimeMillis());
        }

        return weather;
    }

    /**
     * 保存 daily_context.json 文件
     */
    private Path saveDailyContext(DailyContextData context, LocalDate date) throws Exception {
        // 创建日期目录
        Path dayDir = BASE_DIR.resolve(date.format(DATE_FORMATTER));
        Files.createDirectories(dayDir);

        // 生成 JSON
        String jsonContent = objectMapper.writeValueAsString(context);

        // 保存文件
        Path filePath = dayDir.resolve("daily_context.json");
        Files.writeString(filePath, jsonContent, StandardCharsets.UTF_8);

        return filePath;
    }

    /**
     * 写入 Workspace Protocol v1 新结构
     */
    private void writeToNewStructure(DailyContextData context, LocalDate date) {
        try {
            long now = System.currentTimeMillis();
            
            // 1. 保存 daily_context.json 到 today/context/daily_context.json
            Path contextPath = workspaceService.getTodayContextDir().resolve("daily_context.json");
            String jsonContent = objectMapper.writeValueAsString(context);
            Files.writeString(contextPath, jsonContent, StandardCharsets.UTF_8);
            
            // 2. 更新 index.json
            LifeIndexService.IndexItem item = new LifeIndexService.IndexItem();
            item.setId("daily_context");
            item.setType("daily_context");
            item.setName("daily_context.json");
            item.setRelativePath("context/daily_context.json");
            item.setMimeType("application/json");
            item.setSize(Files.size(contextPath));
            item.setSource("android_app");
            item.setCreatedTime(now);
            item.setUpdatedTime(now);
            lifeIndexService.addOrUpdateItem(item);
            
            // 3. 追加 changes.json
            LifeChangesService.ChangeEntry change = new LifeChangesService.ChangeEntry();
            change.setType("daily_context_updated");
            change.setTargetId("daily_context");
            change.setTargetPath("context/daily_context.json");
            change.setSource("android_app");
            change.setDescription("更新了当天现实世界数据");
            change.setCreatedTime(now);
            lifeChangesService.appendChange(change);
            
        } catch (Exception e) {
            System.err.println("写入新结构失败: " + e.getMessage());
            // 不抛出异常，避免影响原有功能
        }
    }
}
