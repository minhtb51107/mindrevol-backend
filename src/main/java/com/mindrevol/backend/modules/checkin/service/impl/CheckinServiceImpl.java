package com.mindrevol.backend.modules.checkin.service.impl;

import com.mindrevol.backend.common.constant.AppConstants;
import com.mindrevol.backend.common.event.CheckinSuccessEvent;
import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.common.service.ContentModerationService;
import com.mindrevol.backend.common.service.ImageMetadataService;
import com.mindrevol.backend.common.utils.SecurityUtils;
import com.mindrevol.backend.modules.checkin.dto.request.CheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.request.UpdateCheckinRequest;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinReactionDetailResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CheckinResponse;
import com.mindrevol.backend.modules.checkin.dto.response.CommentResponse;
import com.mindrevol.backend.modules.checkin.dto.response.MapMarkerResponse; 
import com.mindrevol.backend.modules.checkin.entity.*;
import com.mindrevol.backend.modules.checkin.event.CheckinDeletedEvent;
import com.mindrevol.backend.modules.checkin.event.CommentPostedEvent;
import com.mindrevol.backend.modules.checkin.mapper.CheckinMapper;
import com.mindrevol.backend.modules.checkin.repository.CheckinCommentRepository;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.checkin.repository.SavedCheckinRepository;
import com.mindrevol.backend.modules.checkin.service.CheckinService;
import com.mindrevol.backend.modules.checkin.service.ReactionService;
import com.mindrevol.backend.modules.journey.entity.Journey;
import com.mindrevol.backend.modules.journey.entity.JourneyParticipant;
import com.mindrevol.backend.modules.journey.repository.JourneyParticipantRepository;
import com.mindrevol.backend.modules.journey.repository.JourneyRepository;
import com.mindrevol.backend.modules.box.repository.BoxMemberRepository; // [THÊM MỚI]
import com.mindrevol.backend.modules.storage.service.FileStorageService;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserBlockRepository;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final SavedCheckinRepository savedCheckinRepository; 
    private final JourneyRepository journeyRepository;
    private final JourneyParticipantRepository participantRepository;
    private final BoxMemberRepository boxMemberRepository; // [THÊM MỚI]
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CheckinMapper checkinMapper;
    
    private final CheckinCommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserBlockRepository userBlockRepository; 
    
    private final ReactionService reactionService; 
    private final ContentModerationService moderationService;
    private final ImageMetadataService metadataService;
    
    private Set<String> getExcludedUserIds(String userId) {
        Set<String> blockedIds = userBlockRepository.findAllBlockedUserIdsInteraction(userId);
        if (blockedIds == null) blockedIds = new HashSet<>();
        blockedIds.add("-1"); 
        return blockedIds;
    }

    private CheckinResponse enrichResponse(CheckinResponse response, String currentUserId) {
        List<CheckinReactionDetailResponse> previews = reactionService.getPreviewReactions(response.getId());
        response.setLatestReactions(previews);
        
        if (currentUserId != null) {
            boolean isSaved = savedCheckinRepository.existsByUserIdAndCheckinId(currentUserId, response.getId());
            response.setSaved(isSaved);
        }
        
        return response;
    }

    // [THÊM MỚI] Helper check quyền truy cập (Gộp Box Member + Guest)
    private boolean hasAccessToJourney(Journey journey, String userId) {
        if (journey.getBox() != null && boxMemberRepository.existsByBoxIdAndUserId(journey.getBox().getId(), userId)) {
            return true;
        }
        return participantRepository.existsByJourneyIdAndUserId(journey.getId(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponse> getArchivedCheckins(User currentUser, Pageable pageable) {
        return checkinRepository.findArchivedCheckinsByUser(currentUser.getId(), pageable)
                .map(checkinMapper::toResponse)
                .map(res -> enrichResponse(res, currentUser.getId()));
    }

    @Override
    @CacheEvict(value = "journey_widget", key = "#request.journeyId != null ? #request.journeyId + '-' + #currentUser.id : 'archived-' + #currentUser.id")
    @Transactional
    public CheckinResponse createCheckin(CheckinRequest request, User currentUser) {
        Journey journey = null;
        JourneyParticipant participant = null;

        String tz = currentUser.getTimezone() != null ? currentUser.getTimezone() : "UTC";
        ZoneId userZone = ZoneId.of(tz);
        LocalDate todayLocal = LocalDate.now(userZone);

        if (request.getJourneyId() != null && !request.getJourneyId().trim().isEmpty()) {
            journey = journeyRepository.findById(request.getJourneyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

            // [SỬA LOGIC CHECK QUYỀN Ở ĐÂY]
            boolean isBoxMember = journey.getBox() != null && boxMemberRepository.existsByBoxIdAndUserId(journey.getBox().getId(), currentUser.getId());
            
            // Tìm thử xem có phải là Guest không (nếu là Box Member thì hàm này trả về null là bình thường)
            participant = participantRepository.findByJourneyIdAndUserId(journey.getId(), currentUser.getId()).orElse(null);

            if (!isBoxMember && participant == null) {
                throw new BadRequestException("Bạn không phải thành viên (hoặc Khách) của hành trình này.");
            }

            if (journey.getEndDate() != null && todayLocal.isAfter(journey.getEndDate().plusDays(1))) {
                 throw new BadRequestException("Hành trình này đã kết thúc (Hạn chót: " + journey.getEndDate() + ").");
            }
        }

        String imageUrl = "";
        String videoUrl = null;
        String imageFileId = null;
        MediaType mediaType = MediaType.IMAGE;

        Double exifLat = null;
        Double exifLng = null;

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            MultipartFile file = request.getFile();
            String contentType = file.getContentType();

            if (request.getLatitude() == null && request.getLongitude() == null && 
                contentType != null && !contentType.startsWith("video")) {
                try {
                    double[] coords = metadataService.extractCoordinates(file);
                    if (coords != null && coords.length == 2) {
                        exifLat = coords[0];
                        exifLng = coords[1];
                    }
                } catch (Exception e) {
                    log.warn("Không thể đọc EXIF Data: {}", e.getMessage());
                }
            }

            String folderSuffix = journey != null ? journey.getId() : "archived_" + currentUser.getId();

            if (contentType != null && contentType.startsWith("video")) {
                if (file.getSize() > AppConstants.MAX_VIDEO_SIZE_BYTES) {
                    throw new BadRequestException("Video quá lớn. Vui lòng tải video dưới 10MB (Khoảng 3s).");
                }

                mediaType = MediaType.VIDEO;
                
                try {
                    FileStorageService.FileUploadResult uploadResult = fileStorageService.uploadFile(file, AppConstants.STORAGE_CHECKIN_VIDEOS + folderSuffix);
                    imageFileId = uploadResult.getFileId();
                    String rawUrl = uploadResult.getUrl();
                    videoUrl = rawUrl; 
                    imageUrl = rawUrl + "/ik-thumbnail.jpg";
                } catch (Exception e) {
                    throw new BadRequestException("Lỗi upload video: " + e.getMessage());
                }
                
            } else {
                mediaType = MediaType.IMAGE;
                try {
                    FileStorageService.FileUploadResult uploadResult = fileStorageService.uploadFile(file, AppConstants.STORAGE_CHECKIN_IMAGES + folderSuffix);
                    imageUrl = uploadResult.getUrl() + "?tr=w-1080,q-80"; 
                    imageFileId = uploadResult.getFileId();
                    moderationService.validateImage(uploadResult.getUrl()); 
                } catch (BadRequestException e) {
                    if (imageFileId != null) fileStorageService.deleteFile(imageFileId);
                    throw e;
                } catch (Exception e) {
                    throw new BadRequestException("Lỗi xử lý hình ảnh: " + e.getMessage());
                }
            }
        } else {
            throw new BadRequestException("Vui lòng tải lên hình ảnh hoặc video.");
        }

        if (request.getLatitude() == null && exifLat != null) {
            request.setLatitude(exifLat);
            request.setLongitude(exifLng);
        }

        return saveCheckinTransaction(currentUser, journey, participant, request, 
                                    imageUrl, videoUrl, imageFileId, mediaType, todayLocal);
    }
    
    @Transactional
    protected CheckinResponse saveCheckinTransaction(User currentUser, Journey journey, 
                                                   JourneyParticipant participant, 
                                                   CheckinRequest request, 
                                                   String imageUrl,
                                                   String videoUrl,
                                                   String imageFileId,
                                                   MediaType mediaType,
                                                   LocalDate todayLocal) {
        
        CheckinStatus finalStatus = CheckinStatus.NORMAL;
        if (request.getStatusRequest() != null && request.getStatusRequest().toString().equalsIgnoreCase("REST")) {
            finalStatus = CheckinStatus.REST;
        }

        String finalActivityName = request.getActivityName();
        if (finalActivityName != null && finalActivityName.trim().isEmpty()) {
            finalActivityName = null;
        }

        String finalEmotion = Emotion.NORMAL.name(); 
        
        if (request.getEmotion() != null) {
            finalEmotion = request.getEmotion().toString();
        }

        Checkin checkin = Checkin.builder()
                .user(currentUser)
                .journey(journey)
                .caption(request.getCaption())
                .mediaType(mediaType)
                .imageUrl(imageUrl)
                .videoUrl(videoUrl)
                .imageFileId(imageFileId)
                .thumbnailUrl(imageUrl)
                .emotion(finalEmotion)
                .activityType(request.getActivityType() != null ? request.getActivityType() : ActivityType.DEFAULT)
                .activityName(finalActivityName)
                .locationName(request.getLocationName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .status(finalStatus)
                .visibility(request.getVisibility() != null ? request.getVisibility() : CheckinVisibility.PUBLIC) 
                .createdAt(LocalDateTime.now())
                .checkinDate(todayLocal)
                .build();

        checkin = checkinRepository.save(checkin);
        
        // Chỉ cập nhật participant stats (streak) nếu họ là GUEST (có object participant). 
        // Box Member (participant = null) không cập nhật streak ở bảng này nữa.
        if (participant != null) {
            updateParticipantStats(participant, finalStatus, todayLocal);
        }

        String journeyIdForEvent = journey != null ? journey.getId() : null;
        eventPublisher.publishEvent(new CheckinSuccessEvent(
                checkin.getId(),
                currentUser.getId(),
                journeyIdForEvent,
                checkin.getCreatedAt()
        ));

        return checkinMapper.toResponse(checkin);
    }

    private void updateParticipantStats(JourneyParticipant participant, CheckinStatus status, LocalDate todayLocal) {
        boolean isFirstCheckinToday = false;
        
        if (participant.getLastCheckinAt() == null) {
            isFirstCheckinToday = true;
        } else {
            LocalDate lastDate = participant.getLastCheckinAt().toLocalDate();
            if (!lastDate.equals(todayLocal)) {
                isFirstCheckinToday = true;
            }
        }

        if (isFirstCheckinToday) {
            participant.setTotalActiveDays(participant.getTotalActiveDays() + 1);
        }

        if (status == CheckinStatus.REST) {
            participant.setLastCheckinAt(LocalDateTime.now());
        } else {
            if (participant.getLastCheckinAt() == null) {
                participant.setCurrentStreak(1);
            } else {
                LocalDate lastDate = participant.getLastCheckinAt().toLocalDate();
                if (!lastDate.equals(todayLocal)) {
                    long daysGap = ChronoUnit.DAYS.between(lastDate, todayLocal);
                    if (daysGap > 1) {
                        participant.setCurrentStreak(1); 
                    } else if (daysGap == 1) {
                        participant.setCurrentStreak(participant.getCurrentStreak() + 1); 
                    }
                }
            }
            
            participant.setLastCheckinAt(LocalDateTime.now());
            participant.setTotalCheckins(participant.getTotalCheckins() + 1);
        }
        
        participantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CheckinResponse> getJourneyFeed(String journeyId, Pageable pageable, User currentUser) {
        Journey journey = journeyRepository.findById(journeyId).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        // [ĐÃ SỬA] Dùng hàm hasAccessToJourney để check chung cho cả Box Member và Guest
        if (!hasAccessToJourney(journey, currentUser.getId())) {
            throw new BadRequestException("Không có quyền xem");
        }
        return checkinRepository.findByJourneyIdOrderByCreatedAtDesc(journeyId, pageable)
                .map(checkinMapper::toResponse)
                .map(res -> enrichResponse(res, currentUser.getId())); 
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponse> getUnifiedFeed(User currentUser, LocalDateTime cursor, int limit) {
        if (cursor == null) cursor = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);
        Set<String> excludedUserIds = getExcludedUserIds(currentUser.getId());

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        return checkinRepository.findUnifiedFeedRecent(
                    currentUser.getId(), 
                    threeDaysAgo, 
                    cursor, 
                    excludedUserIds, 
                    pageable
                )
                .stream()
                .map(checkinMapper::toResponse)
                .map(res -> enrichResponse(res, currentUser.getId())) 
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckinResponse> getJourneyFeedByCursor(String journeyId, User currentUser, LocalDateTime cursor, int limit) {
        Journey journey = journeyRepository.findById(journeyId).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        // [ĐÃ SỬA]
        if (!hasAccessToJourney(journey, currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xem hành trình này");
        }
        if (cursor == null) cursor = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, limit);
        Set<String> excludedUserIds = getExcludedUserIds(currentUser.getId());

        return checkinRepository.findJourneyFeedByCursor(journeyId, cursor, excludedUserIds, pageable)
                .stream()
                .map(checkinMapper::toResponse)
                .map(res -> enrichResponse(res, currentUser.getId())) 
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public CommentResponse postComment(String checkinId, String content, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));
        
        if (userBlockRepository.existsByBlockerIdAndBlockedId(checkin.getUser().getId(), currentUser.getId())) {
             throw new BadRequestException("Bạn không thể bình luận bài viết này.");
        }

        CheckinComment comment = CheckinComment.builder()
                .checkin(checkin)
                .user(currentUser)
                .content(content)
                .build();
        
        comment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentPostedEvent(checkin, currentUser, content));
        return checkinMapper.toCommentResponse(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(String checkinId, Pageable pageable) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        Set<String> excludedUserIds = getExcludedUserIds(currentUserId);
        
        return commentRepository.findByCheckinId(checkinId, excludedUserIds, pageable)
                .map(checkinMapper::toCommentResponse);
    }

    @Override
    @Transactional
    public CheckinResponse updateCheckin(String checkinId, UpdateCheckinRequest request, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (!checkin.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền sửa bài viết này");
        }

        if (request.getCaption() != null) {
            checkin.setCaption(request.getCaption());
        }

        boolean isMovedToJourney = false;
        JourneyParticipant participant = null;

        if (request.getJourneyId() != null && !request.getJourneyId().trim().isEmpty()) {
            if (checkin.getJourney() == null) {
                Journey journey = journeyRepository.findById(request.getJourneyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Hành trình không tồn tại"));

                // [SỬA LOGIC CHECK QUYỀN MỚI]
                boolean isBoxMember = journey.getBox() != null && boxMemberRepository.existsByBoxIdAndUserId(journey.getBox().getId(), currentUser.getId());
                participant = participantRepository.findByJourneyIdAndUserId(journey.getId(), currentUser.getId()).orElse(null);

                if (!isBoxMember && participant == null) {
                    throw new BadRequestException("Bạn không phải thành viên của hành trình này");
                }

                checkin.setJourney(journey);
                isMovedToJourney = true;
            }
        }

        checkin = checkinRepository.save(checkin);

        // Chỉ tính toán lại streak nếu họ là GUEST (vì box member không lưu streak ở bảng participant nữa)
        if (isMovedToJourney && participant != null) {
            checkinRepository.flush();
            recalculateParticipantStats(participant, currentUser);
        }

        return checkinMapper.toResponse(checkin);
    }

    @Override
    @Transactional
    @CacheEvict(value = "journey_widget", allEntries = true)
    public void deleteCheckin(String checkinId, User currentUser) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        if (!checkin.getUser().getId().equals(currentUser.getId())) {
            boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
            if (!isAdmin) throw new BadRequestException("Bạn không có quyền xóa bài viết này");
        }

        Journey journey = checkin.getJourney();
        checkinRepository.delete(checkin);
        checkinRepository.flush(); 

        if (checkin.getImageFileId() != null) {
            eventPublisher.publishEvent(new CheckinDeletedEvent(checkin.getImageFileId()));
        }

        if (journey != null) {
            // Nếu là GUEST thì sẽ tìm thấy để recalculate, nếu là BOX_MEMBER thì kệ nó (ko báo lỗi)
            participantRepository.findByJourneyIdAndUserId(journey.getId(), checkin.getUser().getId())
                    .ifPresent(participant -> recalculateParticipantStats(participant, checkin.getUser()));
        }
    }

    private void recalculateParticipantStats(JourneyParticipant participant, User user) {
        String tz = user.getTimezone() != null ? user.getTimezone() : "UTC";
        ZoneId userZone = ZoneId.of(tz);
        LocalDate today = LocalDate.now(userZone);

        List<LocalDateTime> history = checkinRepository.findValidCheckinDates(
                participant.getJourney().getId(), 
                user.getId()
        );

        if (history.isEmpty()) {
            participant.setCurrentStreak(0);
            participant.setTotalCheckins(0);
            participant.setTotalActiveDays(0); 
            participant.setLastCheckinAt(null);
            participantRepository.save(participant);
            return;
        }

        participant.setTotalCheckins(history.size());

        Set<LocalDate> uniqueDates = history.stream()
                .map(dt -> dt.atZone(ZoneId.of("UTC")).withZoneSameInstant(userZone).toLocalDate())
                .collect(Collectors.toSet());
        participant.setTotalActiveDays(uniqueDates.size());

        LocalDateTime lastCheckinTime = history.get(0);
        participant.setLastCheckinAt(lastCheckinTime);

        int streak = 0;
        LocalDate cursorDate = today;
        
        if (!uniqueDates.contains(cursorDate)) {
            if (uniqueDates.contains(cursorDate.minusDays(1))) {
                cursorDate = cursorDate.minusDays(1); 
            } else {
                participant.setCurrentStreak(0);
                participantRepository.save(participant);
                return;
            }
        }

        while (uniqueDates.contains(cursorDate)) {
            streak++;
            cursorDate = cursorDate.minusDays(1);
        }

        participant.setCurrentStreak(streak);
        participantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapMarkerResponse> getMapMarkersForJourney(String journeyId, User currentUser) {
        Journey journey = journeyRepository.findById(journeyId).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        // [ĐÃ SỬA]
        if (!hasAccessToJourney(journey, currentUser.getId())) {
            throw new BadRequestException("Bạn không có quyền xem dữ liệu của hành trình này");
        }

        List<Checkin> checkins = checkinRepository.findMapMarkersByJourney(journeyId);
        
        return checkins.stream().map(c -> MapMarkerResponse.builder()
                .checkinId(c.getId())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .thumbnailUrl(c.getThumbnailUrl())
                .userAvatar(c.getUser().getAvatarUrl())
                .fullname(c.getUser().getFullname())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapMarkerResponse> getMapMarkersForBox(String boxId, User currentUser) {
        List<Checkin> checkins = checkinRepository.findMapMarkersByBox(boxId);
        
        return checkins.stream().map(c -> MapMarkerResponse.builder()
                .checkinId(c.getId())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .thumbnailUrl(c.getThumbnailUrl())
                .userAvatar(c.getUser().getAvatarUrl())
                .fullname(c.getUser().getFullname())
                .build()
        ).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MapMarkerResponse> getMyMapMarkers(User currentUser) {
        List<Checkin> checkins = checkinRepository.findMapMarkersByUser(currentUser.getId());
        
        return checkins.stream().map(c -> MapMarkerResponse.builder()
                .checkinId(c.getId())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .thumbnailUrl(c.getThumbnailUrl())
                .userAvatar(c.getUser().getAvatarUrl())
                .fullname(c.getUser().getFullname())
                .build()
        ).collect(Collectors.toList());
    }
}