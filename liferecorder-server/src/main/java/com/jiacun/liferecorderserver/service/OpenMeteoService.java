package com.jiacun.liferecorderserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenMeteoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Open-Meteo Geocoding API
    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    
    // Open-Meteo Forecast API
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    public OpenMeteoService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 根据城市和国家查询经纬度
     */
    public Map<String, Object> geocodeCity(String city, String country) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(GEOCODING_URL)
                .queryParam("name", city)
                .queryParam("country", country)
                .queryParam("count", 1)
                .queryParam("language", "zh")
                .queryParam("format", "json")
                .build()
                .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode results = root.get("results");
                
                if (results != null && results.isArray() && results.size() > 0) {
                    JsonNode firstResult = results.get(0);
                    
                    Map<String, Object> location = new HashMap<>();
                    location.put("latitude", firstResult.get("latitude").asDouble());
                    location.put("longitude", firstResult.get("longitude").asDouble());
                    location.put("city", firstResult.has("name") ? firstResult.get("name").asText() : city);
                    location.put("country", firstResult.has("country") ? firstResult.get("country").asText() : country);
                    
                    return location;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("Geocoding 失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据经纬度查询天气
     */
    public Map<String, Object> getWeather(double latitude, double longitude) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(FORECAST_URL)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m")
                .queryParam("timezone", "auto")
                .queryParam("forecast_days", 1)
                .build()
                .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode current = root.get("current");
                
                if (current != null) {
                    Map<String, Object> weather = new HashMap<>();
                    
                    // 提取天气数据
                    weather.put("temperatureC", current.get("temperature_2m").asDouble());
                    weather.put("apparentTemperatureC", current.get("apparent_temperature").asDouble());
                    weather.put("relativeHumidity", current.get("relative_humidity_2m").asInt());
                    weather.put("precipitationMm", current.get("precipitation").asDouble());
                    weather.put("windSpeedKmh", current.get("wind_speed_10m").asDouble());
                    
                    int weatherCode = current.get("weather_code").asInt();
                    weather.put("weatherCode", weatherCode);
                    weather.put("condition", convertWeatherCodeToChinese(weatherCode));
                    weather.put("fetchedTime", System.currentTimeMillis());
                    weather.put("error", null);
                    
                    return weather;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("天气查询失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将 WMO Weather Code 转换为中文描述
     */
    private String convertWeatherCodeToChinese(int code) {
        switch (code) {
            case 0: return "晴朗";
            case 1: return "主要晴朗";
            case 2: return "多云";
            case 3: return "阴";
            case 45: return "雾";
            case 48: return "雾凇";
            case 51: return "毛毛雨";
            case 53: return "中度毛毛雨";
            case 55: return "强毛毛雨";
            case 56: return "冻毛毛雨";
            case 57: return "强冻毛毛雨";
            case 61: return "小雨";
            case 63: return "中雨";
            case 65: return "大雨";
            case 66: return "冻雨";
            case 67: return "强冻雨";
            case 71: return "小雪";
            case 73: return "中雪";
            case 75: return "大雪";
            case 77: return "雪粒";
            case 80: return "小阵雨";
            case 81: return "中阵雨";
            case 82: return "强阵雨";
            case 85: return "小阵雪";
            case 86: return "大阵雪";
            case 95: return "雷雨";
            case 96: return "雷雨伴冰雹";
            case 99: return "强雷雨伴冰雹";
            default: return "未知";
        }
    }

    /**
     * 完整的天气查询流程：城市 -> 经纬度 -> 天气
     */
    public Map<String, Object> getWeatherByCity(String city, String country) {
        // 第一步：地理编码
        Map<String, Object> location = geocodeCity(city, country);
        
        if (location == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "无法找到城市：" + city);
            errorResult.put("fetchedTime", System.currentTimeMillis());
            return errorResult;
        }

        // 第二步：查询天气
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");
        
        Map<String, Object> weather = getWeather(latitude, longitude);
        
        if (weather == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "天气查询失败");
            errorResult.put("fetchedTime", System.currentTimeMillis());
            return errorResult;
        }

        // 添加位置信息
        weather.put("latitude", latitude);
        weather.put("longitude", longitude);
        
        return weather;
    }
}
