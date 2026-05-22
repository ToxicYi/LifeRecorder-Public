package com.jiacun.liferecorderserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Daily Context 完整数据结构
 * 对应 daily_context.json 文件
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyContextData {
    
    private int schemaVersion = 1;
    private String date;
    private String timezone;
    
    private Location location;
    private Weather weather;
    private Health health;
    private Device device;
    private App app;
    private Privacy privacy;
    
    private Long createdTime;
    private Long updatedTime;

    // 内部类：Location
    public static class Location {
        private String city;
        private String country;
        private Double latitude;
        private Double longitude;
        private String precision;
        private String source;
        private Long updatedTime;

        public Location() {}

        public Location(String city, String country) {
            this.city = city;
            this.country = country;
            this.latitude = null;
            this.longitude = null;
            this.precision = "city";
            this.source = "manual_city";
            this.updatedTime = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public String getPrecision() { return precision; }
        public void setPrecision(String precision) { this.precision = precision; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public Long getUpdatedTime() { return updatedTime; }
        public void setUpdatedTime(Long updatedTime) { this.updatedTime = updatedTime; }
    }

    // 内部类：Weather
    public static class Weather {
        private String provider;
        private String condition;
        private Integer weatherCode;
        private Double temperatureC;
        private Double apparentTemperatureC;
        private Integer relativeHumidity;
        private Double precipitationMm;
        private Double windSpeedKmh;
        private Long fetchedTime;
        private String error;

        public Weather() {}

        // Getters and Setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public Integer getWeatherCode() { return weatherCode; }
        public void setWeatherCode(Integer weatherCode) { this.weatherCode = weatherCode; }
        public Double getTemperatureC() { return temperatureC; }
        public void setTemperatureC(Double temperatureC) { this.temperatureC = temperatureC; }
        public Double getApparentTemperatureC() { return apparentTemperatureC; }
        public void setApparentTemperatureC(Double apparentTemperatureC) { this.apparentTemperatureC = apparentTemperatureC; }
        public Integer getRelativeHumidity() { return relativeHumidity; }
        public void setRelativeHumidity(Integer relativeHumidity) { this.relativeHumidity = relativeHumidity; }
        public Double getPrecipitationMm() { return precipitationMm; }
        public void setPrecipitationMm(Double precipitationMm) { this.precipitationMm = precipitationMm; }
        public Double getWindSpeedKmh() { return windSpeedKmh; }
        public void setWindSpeedKmh(Double windSpeedKmh) { this.windSpeedKmh = windSpeedKmh; }
        public Long getFetchedTime() { return fetchedTime; }
        public void setFetchedTime(Long fetchedTime) { this.fetchedTime = fetchedTime; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    // 内部类：Health
    public static class Health {
        private Integer steps;
        private Integer distanceMeters;
        private Integer activeCalories;
        private Integer exerciseMinutes;
        private Integer sleepMinutes;
        private Long sleepStartTime;
        private Long sleepEndTime;
        private Integer heartRateAvg;
        private String source;
        private Long updatedTime;

        public Health() {
            // 所有字段默认为 null
        }

        // Getters and Setters
        public Integer getSteps() { return steps; }
        public void setSteps(Integer steps) { this.steps = steps; }
        public Integer getDistanceMeters() { return distanceMeters; }
        public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
        public Integer getActiveCalories() { return activeCalories; }
        public void setActiveCalories(Integer activeCalories) { this.activeCalories = activeCalories; }
        public Integer getExerciseMinutes() { return exerciseMinutes; }
        public void setExerciseMinutes(Integer exerciseMinutes) { this.exerciseMinutes = exerciseMinutes; }
        public Integer getSleepMinutes() { return sleepMinutes; }
        public void setSleepMinutes(Integer sleepMinutes) { this.sleepMinutes = sleepMinutes; }
        public Long getSleepStartTime() { return sleepStartTime; }
        public void setSleepStartTime(Long sleepStartTime) { this.sleepStartTime = sleepStartTime; }
        public Long getSleepEndTime() { return sleepEndTime; }
        public void setSleepEndTime(Long sleepEndTime) { this.sleepEndTime = sleepEndTime; }
        public Integer getHeartRateAvg() { return heartRateAvg; }
        public void setHeartRateAvg(Integer heartRateAvg) { this.heartRateAvg = heartRateAvg; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public Long getUpdatedTime() { return updatedTime; }
        public void setUpdatedTime(Long updatedTime) { this.updatedTime = updatedTime; }
    }

    // 内部类：Device
    public static class Device {
        private Integer batteryPercent;
        private Boolean isCharging;
        private String networkType;
        private String deviceName;
        private String androidVersion;
        private String appVersion;

        public Device() {}

        public Device(Integer batteryPercent, Boolean isCharging, String networkType) {
            this.batteryPercent = batteryPercent;
            this.isCharging = isCharging;
            this.networkType = networkType;
            this.deviceName = null;
            this.androidVersion = null;
            this.appVersion = null;
        }

        // Getters and Setters
        public Integer getBatteryPercent() { return batteryPercent; }
        public void setBatteryPercent(Integer batteryPercent) { this.batteryPercent = batteryPercent; }
        public Boolean getIsCharging() { return isCharging; }
        public void setIsCharging(Boolean isCharging) { this.isCharging = isCharging; }
        public String getNetworkType() { return networkType; }
        public void setNetworkType(String networkType) { this.networkType = networkType; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        public String getAndroidVersion() { return androidVersion; }
        public void setAndroidVersion(String androidVersion) { this.androidVersion = androidVersion; }
        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    }

    // 内部类：App
    public static class App {
        private Integer notesCount;
        private Integer filesCount;
        private Integer photosCount;
        private Integer chatMessagesCount;
        private Integer eventsCount;

        public App() {
            // 所有字段默认为 null
        }

        // Getters and Setters
        public Integer getNotesCount() { return notesCount; }
        public void setNotesCount(Integer notesCount) { this.notesCount = notesCount; }
        public Integer getFilesCount() { return filesCount; }
        public void setFilesCount(Integer filesCount) { this.filesCount = filesCount; }
        public Integer getPhotosCount() { return photosCount; }
        public void setPhotosCount(Integer photosCount) { this.photosCount = photosCount; }
        public Integer getChatMessagesCount() { return chatMessagesCount; }
        public void setChatMessagesCount(Integer chatMessagesCount) { this.chatMessagesCount = chatMessagesCount; }
        public Integer getEventsCount() { return eventsCount; }
        public void setEventsCount(Integer eventsCount) { this.eventsCount = eventsCount; }
    }

    // 内部类：Privacy
    public static class Privacy {
        private boolean locationEnabled;
        private boolean healthEnabled;
        private boolean weatherEnabled;
        private String precisionLevel;

        public Privacy() {
            this.locationEnabled = false;
            this.healthEnabled = false;
            this.weatherEnabled = true;
            this.precisionLevel = "city";
        }

        // Getters and Setters
        public boolean isLocationEnabled() { return locationEnabled; }
        public void setLocationEnabled(boolean locationEnabled) { this.locationEnabled = locationEnabled; }
        public boolean isHealthEnabled() { return healthEnabled; }
        public void setHealthEnabled(boolean healthEnabled) { this.healthEnabled = healthEnabled; }
        public boolean isWeatherEnabled() { return weatherEnabled; }
        public void setWeatherEnabled(boolean weatherEnabled) { this.weatherEnabled = weatherEnabled; }
        public String getPrecisionLevel() { return precisionLevel; }
        public void setPrecisionLevel(String precisionLevel) { this.precisionLevel = precisionLevel; }
    }

    // 构造函数
    public DailyContextData() {}

    public DailyContextData(String date, String timezone) {
        this.date = date;
        this.timezone = timezone;
        this.health = new Health();
        this.app = new App();
        this.privacy = new Privacy();
        this.createdTime = System.currentTimeMillis();
        this.updatedTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public Weather getWeather() { return weather; }
    public void setWeather(Weather weather) { this.weather = weather; }
    public Health getHealth() { return health; }
    public void setHealth(Health health) { this.health = health; }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }
    public App getApp() { return app; }
    public void setApp(App app) { this.app = app; }
    public Privacy getPrivacy() { return privacy; }
    public void setPrivacy(Privacy privacy) { this.privacy = privacy; }
    public Long getCreatedTime() { return createdTime; }
    public void setCreatedTime(Long createdTime) { this.createdTime = createdTime; }
    public Long getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Long updatedTime) { this.updatedTime = updatedTime; }
}
