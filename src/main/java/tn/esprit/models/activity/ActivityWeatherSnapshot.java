package tn.esprit.models.activity;

public class ActivityWeatherSnapshot {

    private double temperatureCelsius;
    private double apparentTemperatureCelsius;
    private double windSpeedKmh;
    private double precipitationMm;
    private int weatherCode;
    private boolean day;
    private String conditionLabel;
    private String suitabilityLabel;
    private String advisoryText;

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public double getApparentTemperatureCelsius() {
        return apparentTemperatureCelsius;
    }

    public void setApparentTemperatureCelsius(double apparentTemperatureCelsius) {
        this.apparentTemperatureCelsius = apparentTemperatureCelsius;
    }

    public double getWindSpeedKmh() {
        return windSpeedKmh;
    }

    public void setWindSpeedKmh(double windSpeedKmh) {
        this.windSpeedKmh = windSpeedKmh;
    }

    public double getPrecipitationMm() {
        return precipitationMm;
    }

    public void setPrecipitationMm(double precipitationMm) {
        this.precipitationMm = precipitationMm;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public void setWeatherCode(int weatherCode) {
        this.weatherCode = weatherCode;
    }

    public boolean isDay() {
        return day;
    }

    public void setDay(boolean day) {
        this.day = day;
    }

    public String getConditionLabel() {
        return conditionLabel;
    }

    public void setConditionLabel(String conditionLabel) {
        this.conditionLabel = conditionLabel;
    }

    public String getSuitabilityLabel() {
        return suitabilityLabel;
    }

    public void setSuitabilityLabel(String suitabilityLabel) {
        this.suitabilityLabel = suitabilityLabel;
    }

    public String getAdvisoryText() {
        return advisoryText;
    }

    public void setAdvisoryText(String advisoryText) {
        this.advisoryText = advisoryText;
    }
}
