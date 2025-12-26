package com.mindrevol.backend.modules.chat.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.chat.dto.event.MessageReadEvent;
import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.ConversationResponse;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.*;
import com.mindrevol.backend.modules.chat.mapper.ChatMapper;
import com.mindrevol.backend.modules.chat.repository.ConversationRepository;
import com.mindrevol.backend.modules.chat.repository.MessageRepository;
import com.mindrevol.backend.modules.chat.service.ChatService;
import com.mindrevol.backend.modules.user.dto.response.UserSummaryResponse;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.UserRepository;
import com.mindrevol.backend.modules.user.service.UserBlockService;
import com.mindrevol.backend.modules.user.service.UserPresenceService; 
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ChatMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserBlockService userBlockService;
    private final UserPresenceService userPresenceService; 

    @Override
    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        Long receiverId = request.getReceiverId();

        if (userBlockService.isBlocked(receiverId, senderId)) {
            throw new BadRequestException("Bạn không thể gửi tin nhắn cho người này.");
        }

        // [CẬP NHẬT] Sử dụng hàm tìm kiếm chính xác 1-1
        Conversation conversation = conversationRepository.findByUsers(senderId, receiverId)
                .orElseGet(() -> createNewConversation(senderId, receiverId));

        Message message = Message.builder()
                .conversation(conversation)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .metadata(request.getMetadata())
                .clientSideId(request.getClientSideId())
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();

        message = messageRepository.save(message);

        String previewContent = message.getType() == MessageType.IMAGE ? "[Hình ảnh]" : message.getContent();
        conversation.setLastMessageContent(previewContent);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastSenderId(senderId);
        
        conversationRepository.save(conversation);

        MessageResponse response = chatMapper.toResponse(message);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId),
                "/queue/messages",
                response
        );

        return response;
    }

    private Conversation createNewConversation(Long senderId, Long receiverId) {
        User sender = userRepository.findById(senderId).orElseThrow();
        User receiver = userRepository.findById(receiverId).orElseThrow();

        Conversation conv = Conversation.builder()
                .user1(sender)
                .user2(receiver)
                .lastMessageAt(LocalDateTime.now())
                .build();
        
        return conversationRepository.save(conv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(Long userId) {
        // [QUAN TRỌNG] Gọi hàm findValidConversationsByUserId để lọc bỏ user bị chặn
        List<Conversation> conversations = conversationRepository.findValidConversationsByUserId(userId);

        return conversations.stream().map(conv -> {
            User partnerEntity = conv.getUser1().getId().equals(userId) ? conv.getUser2() : conv.getUser1();
            
            boolean isOnline = userPresenceService.isUserOnline(partnerEntity.getId());

            UserSummaryResponse partnerDto = UserSummaryResponse.builder()
                .id(partnerEntity.getId())
                .fullname(partnerEntity.getFullname())
                .avatarUrl(partnerEntity.getAvatarUrl())
                .handle(partnerEntity.getHandle())
                .isOnline(isOnline)
                .build();

            long unread = messageRepository.countUnreadMessages(conv.getId(), userId);

            return ConversationResponse.builder()
                    .id(conv.getId())
                    .partner(partnerDto)
                    .lastMessageContent(conv.getLastMessageContent())
                    .lastMessageAt(conv.getLastMessageAt())
                    .lastSenderId(conv.getLastSenderId())
                    .unreadCount(unread)
                    .status(conv.getStatus() != null ? conv.getStatus().name() : "ACTIVE")
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getConversationMessages(Long conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(chatMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesWithUser(Long currentUserId, Long partnerId, Pageable pageable) {
        // [CẬP NHẬT] Sử dụng hàm tìm kiếm chính xác
        Conversation conversation = conversationRepository.findByUsers(currentUserId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cuộc trò chuyện nào."));
        
        return getConversationMessages(conversation.getId(), pageable);
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        Message lastMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElse(null);

        if (lastMessage != null) {
            boolean updated = false;
            if (conversation.getUser1().getId().equals(userId)) {
                if (conversation.getUser1LastReadMessageId() == null || conversation.getUser1LastReadMessageId() < lastMessage.getId()) {
                    conversation.setUser1LastReadMessageId(lastMessage.getId());
                    updated = true;
                }
            } else if (conversation.getUser2().getId().equals(userId)) {
                if (conversation.getUser2LastReadMessageId() == null || conversation.getUser2LastReadMessageId() < lastMessage.getId()) {
                    conversation.setUser2LastReadMessageId(lastMessage.getId());
                    updated = true;
                }
            }

            if (updated) {
                conversationRepository.save(conversation);
                
                Long partnerId = conversation.getUser1().getId().equals(userId) 
                        ? conversation.getUser2().getId() 
                        : conversation.getUser1().getId();
                
                messagingTemplate.convertAndSendToUser(
                    String.valueOf(partnerId),
                    "/queue/read-receipt",
                    new MessageReadEvent(conversationId, lastMessage.getId(), userId)
                );
            }
        }
    }

    @Override
    public Conversation getConversationById(Long id) {
        return conversationRepository.findById(id).orElseThrow();
    }
    
    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(Long senderId, Long receiverId) {
        // 1. Kiểm tra xem đã chặn nhau chưa (dùng lại logic của UserBlockService)
        if (userBlockService.isBlocked(receiverId, senderId) || userBlockService.isBlocked(senderId, receiverId)) {
            throw new BadRequestException("Không thể bắt đầu cuộc trò chuyện do chặn người dùng.");
        }

        // 2. Tìm cuộc hội thoại đã tồn tại (Dùng hàm findByUsers đã có trong Repo)
        // Nếu chưa có thì gọi hàm createNewConversation (đã có ở cuối file service của bạn)
        Conversation conversation = conversationRepository.findByUsers(senderId, receiverId)
                .orElseGet(() -> createNewConversation(senderId, receiverId));

        // 3. Map Entity sang Response (Logic này lấy từ getUserConversations để đảm bảo giống format)
        return mapToConversationResponse(conversation, senderId);
    }

    // --- Helper để map dữ liệu (tránh lặp code) ---
    private ConversationResponse mapToConversationResponse(Conversation conv, Long currentUserId) {
        // Xác định ai là người đối diện (Partner)
        User partnerEntity = conv.getUser1().getId().equals(currentUserId) ? conv.getUser2() : conv.getUser1();
        
        // Kiểm tra Online
        boolean isOnline = userPresenceService.isUserOnline(partnerEntity.getId());

        // Map thông tin Partner
        UserSummaryResponse partnerDto = UserSummaryResponse.builder()
            .id(partnerEntity.getId())
            .fullname(partnerEntity.getFullname())
            .avatarUrl(partnerEntity.getAvatarUrl())
            .handle(partnerEntity.getHandle())
            .isOnline(isOnline)
            .build();

        // Đếm tin nhắn chưa đọc
        long unread = messageRepository.countUnreadMessages(conv.getId(), currentUserId);

        return ConversationResponse.builder()
                .id(conv.getId())
                .partner(partnerDto)
                .lastMessageContent(conv.getLastMessageContent())
                .lastMessageAt(conv.getLastMessageAt())
                .lastSenderId(conv.getLastSenderId())
                .unreadCount(unread)
                .status(conv.getStatus() != null ? conv.getStatus().name() : "ACTIVE") // Dùng Enum thực tế ACTIVE
                .build();
    }
}