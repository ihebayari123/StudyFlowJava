package edu.connexion3a36.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeoLocationService {

    private static final String API_URL = "http://ip-api.com/json/?fields=status,country,city,query";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public static class GeoInfo {
        public final String ip;
        public final String country;
        public final String city;
        public final boolean success;

        public GeoInfo(String ip, String country, String city, boolean success) {
            this.ip      = ip;
            this.country = country;
            this.city    = city;
            this.success = success;
        }
    }

    public GeoInfo fetchGeoInfo() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.body());

            if ("success".equals(json.get("status").asText())) {
                return new GeoInfo(
                        json.get("query").asText(),
                        json.get("country").asText(),
                        json.get("city").asText(),
                        true
                );
            }
        } catch (Exception e) {
            System.err.println("[GeoLocation] Erreur API : " + e.getMessage());
        }
        return new GeoInfo("inconnu", "inconnu", "inconnu", false);
    }
}