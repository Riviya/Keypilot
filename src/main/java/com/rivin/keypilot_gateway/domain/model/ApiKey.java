package com.rivin.keypilot_gateway.domain.model;

import com.rivin.keypilot_gateway.domain.exception.InvalidApiException;
import com.rivin.keypilot_gateway.domain.exception.InvalidApiProviderException;

import java.util.UUID;

public class ApiKey {

    private final String keyValue;   // the actual secret key
    private final String provider;
    private boolean active;
    private final String id;


    public ApiKey(String keyValue,  String provider) {
        if(keyValue == null || keyValue.isBlank()){
            throw new InvalidApiException("API key cannot be null or blank");
        }
        else if(keyValue.contains(" ")){
            throw new InvalidApiException("API key cannot be contains spaces");
        }
        else if(provider == null || provider.isBlank()){
            throw new InvalidApiProviderException("API key provider cannot be null or blank");
        }
        else if(provider.contains(" ")){
            throw new InvalidApiProviderException("API key provider cannot contains spaces");
        }



        this.id = UUID.randomUUID().toString();
        this.keyValue = keyValue;
        this.provider = provider;
        this.active = true;
    }
    public String getId() { return id; }

    public String getKeyValue() { return keyValue; }
    public String getProvider() { return provider; }
    public boolean isActive() { return active; }

    public String deactivate() {

        if (!isActive()) {
            return "API key has been already deactivated!";
        }
        this.active = false;
        return "API Key Deactivated!";
    }


}