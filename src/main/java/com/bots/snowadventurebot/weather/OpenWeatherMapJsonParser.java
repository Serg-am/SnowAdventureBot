package com.bots.snowadventurebot.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Data
@Configuration
public class OpenWeatherMapJsonParser {
    public OpenWeatherMapJsonParser(WeatherConfig weatherConfig) {
        this.weatherConfig = weatherConfig;
    }

    private WeatherConfig weatherConfig;
    private final static DateTimeFormatter INPUT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final static DateTimeFormatter OUTPUT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM-dd HH:mm", Locale.US);


    public String getReadyForecast(String city) {
        String result;
        try {
            String jsonRawData = downloadJsonRawData(city);
            List<String> linesOfForecast = convertRawDataToList(jsonRawData);
            result = String.format("%s:%s%s", city, System.lineSeparator(), parseForecastDataFromList(linesOfForecast));
        } catch (IllegalArgumentException e) {
            return String.format("Не могу найти город \"%s\". Попробуйте другой город.", city);
        } catch (Exception e) {
            e.printStackTrace();
            return "В данный момент погодный сервис не исправен, приносим свои извинения, в ближайшее время работа возобновится. Хорошего дня!";
        }
        return result;
    }

    private String downloadJsonRawData(String city) throws Exception {
        String urlString = weatherConfig.apiTemplate + city + weatherConfig.apiKey;
        URL urlObject = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", weatherConfig.userAgent);

        int responseCode = connection.getResponseCode();
        if (responseCode == 404) {
            throw new IllegalArgumentException();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    private static List<String> convertRawDataToList(String data) throws Exception {
        List<String> weatherList = new ArrayList<>();

        JsonNode arrNode = new ObjectMapper().readTree(data).get("list");
        if (arrNode.isArray()) {
            for (final JsonNode objNode : arrNode) {
                String forecastTime = objNode.get("dt_txt").toString();
                if (forecastTime.contains("09:00") || forecastTime.contains("18:00")) {
                    weatherList.add(objNode.toString());
                }
            }
        }
        return weatherList;
    }

    private static String parseForecastDataFromList(List<String> weatherList) throws Exception {
        final StringBuffer sb = new StringBuffer();
        ObjectMapper objectMapper = new ObjectMapper();

        for (String line : weatherList) {
            {
                String dateTime;
                JsonNode mainNode;
                JsonNode weatherArrNode;
                try {
                    mainNode = objectMapper.readTree(line).get("main");
                    weatherArrNode = objectMapper.readTree(line).get("weather");
                    for (final JsonNode objNode : weatherArrNode) {
                        dateTime = objectMapper.readTree(line).get("dt_txt").toString();
                        sb.append(formatForecastData(dateTime, objNode.get("main").toString(), mainNode.get("temp").asDouble()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    private static String formatForecastData(String dateTime, String description, double temperature) throws Exception {
        LocalDateTime forecastDateTime = LocalDateTime.parse(dateTime.replaceAll("\"", ""), INPUT_DATE_TIME_FORMAT);
        String formattedDateTime = forecastDateTime.format(OUTPUT_DATE_TIME_FORMAT);

        String formattedTemperature;
        long roundedTemperature = Math.round(temperature);
        if (roundedTemperature > 0) {
            formattedTemperature = "+" + Math.round(temperature);
        } else {
            formattedTemperature = String.valueOf(Math.round(temperature));
        }

        String formattedDescription = description.replaceAll("\"", "");
        if(formattedDescription.equals("Clouds")){
            formattedDescription = "Облачно";
        }

        String weatherIconCode = WeatherUtils.weatherIconsCodes.get(formattedDescription);

        return String.format("%s   %s %s %s%s", formattedDateTime, formattedTemperature, formattedDescription, weatherIconCode, System.lineSeparator());
    }
}
