package com.mindrevol.backend.modules.checkin.service;

import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.checkin.dto.request.ReactionRequest;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.entity.CheckinReaction;
import com.mindrevol.backend.modules.checkin.entity.ReactionType;
import com.mindrevol.backend.modules.checkin.repository.CheckinReactionRepository;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final CheckinRepository checkinRepository;
    private final CheckinReactionRepository reactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void toggleReaction(ReactionRequest request, User user) {
        Checkin checkin = checkinRepository.findById(request.getCheckinId())
                .orElseThrow(() -> new ResourceNotFoundException("Checkin not found"));

        // Validate: Nếu là VOICE thì phải có link
        if (request.getType() == ReactionType.VOICE && (request.getMediaUrl() == null || request.getMediaUrl().isEmpty())) {
            throw new BadRequestException("Voice reaction requires mediaUrl");
        }

        Optional<CheckinReaction> existingReaction = reactionRepository
                .findByCheckinIdAndUserId(checkin.getId(), user.getId());

        if (existingReaction.isPresent()) {
            // Logic cũ: Toggle (Bấm lại thì xóa)
            // Tuy nhiên với Voice, có thể ta muốn cho phép gửi nhiều voice? 
            // Nhưng DB đang set Unique Constraint (1 người 1 reaction/ảnh).
            // -> Giữ nguyên logic Toggle: Gửi mới sẽ đè cái cũ.
            
            CheckinReaction reaction = existingReaction.get();
            
            // Nếu gửi cùng loại -> Xóa (Unlike)
            if (reaction.getReactionType() == request.getType()) {
                reactionRepository.delete(reaction);
            } else {
                // Nếu gửi loại khác -> Update (Ví dụ đang thả Tim chuyển sang thả Voice)
                reaction.setReactionType(request.getType());
                reaction.setMediaUrl(request.getMediaUrl()); // Cập nhật URL nếu có
                reactionRepository.save(reaction);
                
                // Bắn Socket Update
                pushReactionSocket(checkin.getId(), request);
            }
        } else {
            // Tạo mới
            CheckinReaction reaction = CheckinReaction.builder()
                    .checkin(checkin)
                    .user(user)
                    .reactionType(request.getType())
                    .mediaUrl(request.getMediaUrl()) // Lưu URL
                    .build();
            reactionRepository.save(reaction);

            // Bắn Socket New
            pushReactionSocket(checkin.getId(), request);
        }
    }

    private void pushReactionSocket(java.util.UUID checkinId, ReactionRequest request) {
        // Gửi cả type và mediaUrl xuống client để client biết đường play âm thanh
        messagingTemplate.convertAndSend(
                "/topic/checkin/" + checkinId + "/reactions",
                request 
        );
    }
}