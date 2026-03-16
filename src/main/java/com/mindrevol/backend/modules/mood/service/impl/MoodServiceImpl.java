package com.mindrevol.backend.modules.mood.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.box.repository.BoxMemberRepository;
import com.mindrevol.backend.modules.box.repository.BoxRepository;
import com.mindrevol.backend.modules.mood.dto.request.MoodRequest;
import com.mindrevol.backend.modules.mood.dto.response.MoodResponse;
import com.mindrevol.backend.modules.mood.entity.Mood;
import com.mindrevol.backend.modules.mood.entity.MoodReaction;
import com.mindrevol.backend.modules.mood.event.MoodCreatedEvent;
import com.mindrevol.backend.modules.mood.event.MoodReactedEvent;
import com.mindrevol.backend.modules.mood.mapper.MoodMapper;
import com.mindrevol.backend.modules.mood.repository.MoodReactionRepository;
import com.mindrevol.backend.modules.mood.repository.MoodRepository;
import com.mindrevol.backend.modules.mood.service.MoodService;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoodServiceImpl implements MoodService {

    private final MoodRepository moodRepository;
    private final MoodReactionRepository moodReactionRepository;
    private final BoxRepository boxRepository;
    private final UserRepository userRepository;
    private final BoxMemberRepository boxMemberRepository;
    private final MoodMapper moodMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MoodResponse createOrUpdateMood(String boxId, String userId, MoodRequest request) {
        // Kiểm tra xem user có ở trong Box này không
        if (!boxMemberRepository.existsByBoxIdAndUserId(boxId, userId)) {
            throw new BadRequestException("Bạn không phải là thành viên của Không gian này!");
        }

        Optional<Mood> existingMood = moodRepository.findByBoxIdAndUserId(boxId, userId);
        Mood mood;
        
        if (existingMood.isPresent()) {
            mood = existingMood.get();
            mood.setIcon(request.getIcon());
            mood.setMessage(request.getMessage());
            mood.setExpiresAt(LocalDateTime.now().plusHours(24));
            
            // [VÁ LỖ HỔNG 1]: Xóa các reactions cũ vì Mood đã thay đổi (Ví dụ đang Buồn đổi sang Vui)
            moodReactionRepository.deleteAllByMoodId(mood.getId());
            if (mood.getReactions() != null) {
                mood.getReactions().clear(); // Xóa khỏi context Hibernate hiện tại
            }
        } else {
            mood = Mood.builder()
                    .box(boxRepository.getReferenceById(boxId))
                    .user(userRepository.getReferenceById(userId))
                    .icon(request.getIcon())
                    .message(request.getMessage())
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
        }

        mood = moodRepository.save(mood);

        // Phát sự kiện để xử lý thông báo hoặc socket realtime
        eventPublisher.publishEvent(MoodCreatedEvent.builder()
                .moodId(mood.getId())
                .boxId(mood.getBox().getId())
                .userId(mood.getUser().getId())
                .icon(mood.getIcon())
                .build());

        return moodMapper.toResponse(mood);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoodResponse> getActiveMoodsInBox(String boxId) {
        // Chỉ lấy những Mood có expires_at lớn hơn thời điểm hiện tại
        List<Mood> activeMoods = moodRepository.findByBoxIdAndExpiresAtAfterOrderByUpdatedAtDesc(boxId, LocalDateTime.now());
        
        return activeMoods.stream()
                .map(moodMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteMyMood(String boxId, String userId) {
        Mood mood = moodRepository.findByBoxIdAndUserId(boxId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không có trạng thái nào trong Không gian này"));
        moodRepository.delete(mood);
    }

    @Override
    @Transactional
    public void reactToMood(String moodId, String userId, String emoji) {
        Mood mood = moodRepository.findById(moodId)
                .orElseThrow(() -> new ResourceNotFoundException("Trạng thái này không tồn tại"));

        // [VÁ LỖ HỔNG 2]: Chặn người lạ thả tim (Kiểm tra xem người thả tim có nằm trong Box chứa Mood không)
        if (!boxMemberRepository.existsByBoxIdAndUserId(mood.getBox().getId(), userId)) {
            throw new BadRequestException("Bạn phải là thành viên của Không gian này mới có thể tương tác!");
        }

        // Kiểm tra xem Mood đã hết hạn chưa
        if (mood.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Trạng thái này đã bốc hơi, không thể tương tác nữa");
        }

        Optional<MoodReaction> existingReaction = moodReactionRepository.findByMoodIdAndUserId(moodId, userId);

        if (existingReaction.isPresent()) {
            MoodReaction reaction = existingReaction.get();
            reaction.setEmoji(emoji); // Cập nhật lại emoji nếu họ thả icon khác
            moodReactionRepository.save(reaction);
        } else {
            MoodReaction newReaction = MoodReaction.builder()
                    .mood(mood)
                    .user(userRepository.getReferenceById(userId))
                    .emoji(emoji)
                    .build();
            moodReactionRepository.save(newReaction);
        }

        // Không gửi thông báo nếu người dùng tự thả tim vào Mood của chính mình
        if (!userId.equals(mood.getUser().getId())) {
            eventPublisher.publishEvent(MoodReactedEvent.builder()
                    .moodId(mood.getId())
                    .boxId(mood.getBox().getId())
                    .reactorId(userId)
                    .moodOwnerId(mood.getUser().getId())
                    .emoji(emoji)
                    .build());
        }
    }

    @Override
    @Transactional
    public void removeReaction(String moodId, String userId) {
        MoodReaction reaction = moodReactionRepository.findByMoodIdAndUserId(moodId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa thả cảm xúc vào trạng thái này"));
        moodReactionRepository.delete(reaction);
    }
}