package com.mindrevol.backend.modules.auth.service.strategy;

import com.mindrevol.backend.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SocialLoginFactory {

    private final Map<String, SocialLoginStrategy> strategies = new HashMap<>();

    public SocialLoginFactory(List<SocialLoginStrategy> strategyList) {
        for (SocialLoginStrategy strategy : strategyList) {
            strategies.put(strategy.getProviderName().toLowerCase(), strategy);
        }
    }

    public SocialLoginStrategy getStrategy(String provider) {
        if (provider == null) throw new BadRequestException("Provider is missing");
        SocialLoginStrategy strategy = strategies.get(provider.toLowerCase());
        if (strategy == null) throw new BadRequestException("Provider not supported: " + provider);
        return strategy;
    }
}