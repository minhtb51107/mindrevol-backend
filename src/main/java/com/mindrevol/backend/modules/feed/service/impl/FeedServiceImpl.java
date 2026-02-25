package com.mindrevol.backend.modules.feed.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.advertising.entity.SystemAd;
import com.mindrevol.backend.modules.advertising.repository.SystemAdRepository;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.feed.dto.AdFeedItemResponse;
import com.mindrevol.backend.modules.feed.dto.FeedItemResponse;
import com.mindrevol.backend.modules.feed.dto.FeedItemType;
import com.mindrevol.backend.modules.feed.service.FeedService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserBlockRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedServiceImpl implements FeedService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CheckinRepository checkinRepository;
    private final CheckinMapper checkinMapper;
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository; 
    private final SystemAdRepository systemAdRepository; 
    private final ObjectMapper objectMapper;

    private static final String FEED_CACHE_PREFIX = "feed:unified:";
    private static final long CACHE_TTL_MINUTES = 5;

    private static final int SLOT_INTERNAL_GOLD = 5;
    private static final int SLOT_AFFILIATE = 10;

    @Override
    public List<FeedItemResponse> getNewsFeed(String userId, int offset, int limit) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CheckinResponse> posts = getCachedPosts(userId, offset, limit);
        List<FeedItemResponse> finalFeed = new ArrayList<>(posts);

        if (currentUser.isPremium()) {
            return finalFeed;
        }

        if (!posts.isEmpty()) {
            return injectContextualAds(finalFeed, offset, limit);
        }

        return finalFeed;
    }

    private List<CheckinResponse> getCachedPosts(String userId, int offset, int limit) {
        int page = (limit > 0) ? (offset / limit) : 0;
        String cacheKey = FEED_CACHE_PREFIX + userId + ":" + page;

        try {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return objectMapper.convertValue(cachedData, new TypeReference<List<CheckinResponse>>() {});
            }
        } catch (Exception e) {
            log.error("Redis read error: {}", e.getMessage());
        }

        Set<String> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        if (blockedIds == null) blockedIds = new HashSet<>();
        blockedIds.add("00000000-0000-0000-0000-000000000000");

        Pageable pageable = PageRequest.of(page, limit);
        LocalDateTime cursor = LocalDateTime.now().plusSeconds(10); 
        
        // [FIX BIÊN DỊCH]: Gọi method mới (5 tham số)
        // Lấy 30 ngày cho Feed Service (để có nhiều data trộn quảng cáo hơn)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Checkin> dbCheckins = checkinRepository.findUnifiedFeedRecent(
                userId, 
                thirtyDaysAgo, // sinceDate 
                cursor, 
                blockedIds, 
                pageable
        );
        
        List<CheckinResponse> responseList = dbCheckins.stream().map(checkinMapper::toResponse).collect(Collectors.toList());

        try {
            if (!responseList.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, responseList, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("Redis write error: {}", e.getMessage());
        }

        return responseList;
    }

    private List<FeedItemResponse> injectContextualAds(List<FeedItemResponse> posts, int offset, int limit) {
        List<SystemAd> internalAds = systemAdRepository.findByTypeAndIsActiveTrue(FeedItemType.INTERNAL_AD);
        List<SystemAd> affiliateAds = systemAdRepository.findAllActiveAffiliateAds();

        List<FeedItemResponse> mixedFeed = new ArrayList<>();
        int globalIndex = offset;

        for (FeedItemResponse item : posts) {
            mixedFeed.add(item);
            globalIndex++;

            CheckinResponse postData = (item instanceof CheckinResponse) ? (CheckinResponse) item : null;

            if (globalIndex % SLOT_INTERNAL_GOLD == 0 && globalIndex % SLOT_AFFILIATE != 0) {
                if (!internalAds.isEmpty()) {
                    SystemAd ad = internalAds.get(ThreadLocalRandom.current().nextInt(internalAds.size()));
                    mixedFeed.add(mapAdToResponse(ad));
                }
            }

            if (globalIndex % SLOT_AFFILIATE == 0 && postData != null) {
                List<String> postTags = postData.getTags(); 
                SystemAd matchedAd = findMatchingAffiliate(postTags, affiliateAds);
                
                if (matchedAd != null) {
                    mixedFeed.add(mapAdToResponse(matchedAd));
                }
            }
        }
        return mixedFeed;
    }

    private SystemAd findMatchingAffiliate(List<String> postTags, List<SystemAd> ads) {
        if (postTags == null || postTags.isEmpty() || ads.isEmpty()) return null;
        
        List<SystemAd> shuffledAds = new ArrayList<>(ads);
        Collections.shuffle(shuffledAds);

        for (SystemAd ad : shuffledAds) {
            if (ad.getTargetTags() == null) continue;
            String[] adTags = ad.getTargetTags().split(",");
            
            for (String postTag : postTags) {
                for (String adTag : adTags) {
                    if (postTag.trim().equalsIgnoreCase(adTag.trim())) {
                        return ad;
                    }
                }
            }
        }
        return null;
    }

    private AdFeedItemResponse mapAdToResponse(SystemAd ad) {
        return AdFeedItemResponse.builder()
                .type(ad.getType())
                .id(ad.getId())
                .adProvider("MINDREVOL")
                .title(ad.getTitle())
                .imageUrl(ad.getImageUrl())
                .ctaLink(ad.getCtaLink())
                .ctaText(ad.getCtaText())
                .build();
    }

    @Override
    public void evictFeedCache(String userId) {
        try {
            String pattern = FEED_CACHE_PREFIX + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("Error evicting cache: {}", e.getMessage());
        }
    }
}