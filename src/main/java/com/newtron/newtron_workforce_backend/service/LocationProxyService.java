package com.newtron.newtron_workforce_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.common.exception.BusinessException;
import com.newtron.newtron_workforce_backend.dto.LocationSearchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LocationProxyService {

    private static final Logger log = LoggerFactory.getLogger(LocationProxyService.class);
    
    private final RestClient googlePlacesRestClient;
    private final ObjectMapper objectMapper;
    
    @Value("${google.places.api-key}")
    private String apiKey;
    
    public LocationProxyService(RestClient googlePlacesRestClient, ObjectMapper objectMapper) {
        this.googlePlacesRestClient = googlePlacesRestClient;
        this.objectMapper = objectMapper;
    }
    
    public List<LocationSearchResultDto> searchLocation(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of("textQuery", query.trim()));
            
            String responseJson = googlePlacesRestClient.post()
                    .uri("https://places.googleapis.com/v1/places:searchText")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "places.displayName,places.location")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
                    
            JsonNode root = objectMapper.readTree(responseJson);
            List<LocationSearchResultDto> results = new ArrayList<>();
            
            if (root.has("places") && root.get("places").isArray()) {
                for (JsonNode placeNode : root.get("places")) {
                    String displayName = "";
                    if (placeNode.has("displayName") && placeNode.get("displayName").has("text")) {
                        displayName = placeNode.get("displayName").get("text").asText();
                    }
                    
                    Double lat = null;
                    Double lng = null;
                    if (placeNode.has("location")) {
                        JsonNode locationNode = placeNode.get("location");
                        if (locationNode.has("latitude")) lat = locationNode.get("latitude").asDouble();
                        if (locationNode.has("longitude")) lng = locationNode.get("longitude").asDouble();
                    }
                    
                    if (lat != null && lng != null && !displayName.isEmpty()) {
                        results.add(LocationSearchResultDto.builder()
                                .displayName(displayName)
                                .latitude(lat)
                                .longitude(lng)
                                .build());
                    }
                }
            }
            
            return results;
            
        } catch (RestClientResponseException e) {
            log.error("Google Places API error. Status: {}, Query length: {}", e.getStatusCode(), query.length());
            throw new BusinessException("LOCATION_SERVICE_UNAVAILABLE", "Location search is temporarily unavailable");
        } catch (Exception e) {
            log.error("Failed to parse Google Places response or network error. Query length: {}", query.length(), e);
            throw new BusinessException("LOCATION_SERVICE_ERROR", "Location search is temporarily unavailable");
        }
    }
}
