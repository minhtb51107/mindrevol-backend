package com.mindrevol.backend.modules.chat.service.impl;

import com.mindrevol.backend.common.exception.BadRequestException;
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.box.entity.Box;
import com.mindrevol.backend.modules.box.repository.BoxRepository; // [THÊM MỚI]
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
    private final BoxRepository boxRepository; // [THÊM MỚI]
    private final ChatMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserBlockService userBlockService;
    private final UserPresenceService userPresenceService; 

    @Override
    @Transactional
    public MessageResponse sendMessage(String senderId, SendMessageRequest request) { 
        Conversation conversation;
        User sender = userRepository.getReferenceById(senderId);
        User receiver = null;

        // 1. NẾU CÓ CONVERSATION_ID (Ưu tiên cho Box Chat và Chat đã tồn tại)
        if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện"));
            
            // Lấy tạm receiver để lưu DB (với Box Chat thì không quan trọng lắm)
            receiver = conversation.getUser1().getId().equals(senderId) ? conversation.getUser2() : conversation.getUser1();
        } 
        // 2. NẾU CHƯA CÓ (Tạo mới chat 1-1)
        else {
            String receiverId = request.getReceiverId(); 
            if (receiverId == null || receiverId.isEmpty()) throw new BadRequestException("Receiver ID không được để trống");
            if (userBlockService.isBlocked(receiverId, senderId)) throw new BadRequestException("Bạn không thể gửi tin nhắn cho người này.");

            receiver = userRepository.getReferenceById(receiverId);
            List<Conversation> existingConvs = conversationRepository.findByUsers(senderId, receiverId);
            if (existingConvs.isEmpty()) {
                conversation = createNewConversation(sender, receiver);
            } else {
                conversation = existingConvs.get(0);
            }
        }

        // Tạo và lưu tin nhắn
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .metadata(request.getMetadata())
                .clientSideId(request.getClientSideId())
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();

        message = messageRepository.save(message);

        // Cập nhật cuộc trò chuyện
        String previewContent = message.getType() == MessageType.IMAGE ? "[Hình ảnh]" : message.getContent();
        conversation.setLastMessageContent(previewContent);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastSenderId(senderId);
        conversationRepository.save(conversation);

        MessageResponse response = chatMapper.toResponse(message);
        String destination = "/topic/chat." + conversation.getId();
        messagingTemplate.convertAndSend(destination, response);

        return response;
    }

    private Conversation createNewConversation(User sender, User receiver) {
        Conversation conv = Conversation.builder()
                .user1(sender)
                .user2(receiver)
                .lastMessageAt(LocalDateTime.now())
                .status(ConversationStatus.ACTIVE)
                .build();
        
        return conversationRepository.save(conv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(String userId) { 
        List<Conversation> privateConvs = conversationRepository.findValidConversationsByUserId(userId);
        List<Conversation> boxConvs = conversationRepository.findBoxConversationsByUserId(userId);
        
        java.util.Set<Conversation> allConvs = new java.util.HashSet<>(privateConvs);
        allConvs.addAll(boxConvs);

        return allConvs.stream()
                .sorted((c1, c2) -> {
                    if (c1.getLastMessageAt() == null && c2.getLastMessageAt() == null) return 0;
                    if (c1.getLastMessageAt() == null) return 1;
                    if (c2.getLastMessageAt() == null) return -1;
                    return c2.getLastMessageAt().compareTo(c1.getLastMessageAt());
                })
                .map(conv -> mapToConversationResponse(conv, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getConversationMessages(String conversationId, Pageable pageable) { 
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(chatMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesWithUser(String currentUserId, String partnerId, Pageable pageable) { 
        List<Conversation> conversations = conversationRepository.findByUsers(currentUserId, partnerId);
        
        Conversation conversation = conversations.stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cuộc trò chuyện nào."));
        
        return getConversationMessages(conversation.getId(), pageable);
    }

    @Override
    @Transactional
    public void markConversationAsRead(String conversationId, String userId) { 
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        List<Message> unreadMessages = messageRepository.findUnreadMessagesInConversation(
                conversationId, 
                userId, 
                MessageDeliveryStatus.SEEN
        );

        if (!unreadMessages.isEmpty()) {
            unreadMessages.forEach(msg -> {
                msg.setDeliveryStatus(MessageDeliveryStatus.SEEN);
            });
            messageRepository.saveAll(unreadMessages);
        }

        Message lastMessage = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElse(null);

        if (lastMessage != null) {
            boolean updated = false;
            
            if (conversation.getUser1().getId().equals(userId)) {
                if (conversation.getUser1LastReadMessageId() == null || 
                    conversation.getUser1LastReadMessageId().compareTo(lastMessage.getId()) < 0) {
                    conversation.setUser1LastReadMessageId(lastMessage.getId());
                    updated = true;
                }
            } else if (conversation.getUser2().getId().equals(userId)) {
                if (conversation.getUser2LastReadMessageId() == null || 
                    conversation.getUser2LastReadMessageId().compareTo(lastMessage.getId()) < 0) {
                    conversation.setUser2LastReadMessageId(lastMessage.getId());
                    updated = true;
                }
            }

            if (updated) {
                conversationRepository.save(conversation);
                
                String partnerId = conversation.getUser1().getId().equals(userId) 
                        ? conversation.getUser2().getId() 
                        : conversation.getUser1().getId();
                
                messagingTemplate.convertAndSendToUser(
                    partnerId, 
                    "/queue/read-receipt",
                    new MessageReadEvent(conversationId, lastMessage.getId(), userId)
                );
            }
        }
    }

    @Override
    public Conversation getConversationById(String id) { 
        return conversationRepository.findById(id).orElseThrow();
    }
    
    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(String senderId, String receiverId) { 
        if (userBlockService.isBlocked(receiverId, senderId) || userBlockService.isBlocked(senderId, receiverId)) {
            throw new BadRequestException("Không thể bắt đầu cuộc trò chuyện do chặn người dùng.");
        }
        
        User sender = userRepository.getReferenceById(senderId);
        User receiver = userRepository.getReferenceById(receiverId);

        List<Conversation> existingConvs = conversationRepository.findByUsers(senderId, receiverId);
        Conversation conversation;
        
        if (existingConvs.isEmpty()) {
             conversation = createNewConversation(sender, receiver);
        } else {
             conversation = existingConvs.get(0);
        }

        return mapToConversationResponse(conversation, senderId);
    }

    private ConversationResponse mapToConversationResponse(Conversation conv, String currentUserId) {
        User partnerEntity = conv.getUser1().getId().equals(currentUserId) ? conv.getUser2() : conv.getUser1();
        boolean isOnline = userPresenceService.isUserOnline(partnerEntity.getId());

        UserSummaryResponse partnerDto = UserSummaryResponse.builder()
            .id(partnerEntity.getId())
            .fullname(partnerEntity.getFullname())
            .avatarUrl(partnerEntity.getAvatarUrl())
            .handle(partnerEntity.getHandle())
            .isOnline(isOnline)
            .build();

        long unread = messageRepository.countUnreadMessages(conv.getId(), currentUserId);

        // [THÊM MỚI] Tra cứu Tên và Avatar của Box
        String boxName = null;
        String boxAvatar = null;
        if (conv.getBoxId() != null) {
            Box box = boxRepository.findById(conv.getBoxId()).orElse(null);
            if (box != null) {
                boxName = box.getName();
                boxAvatar = box.getAvatar();
            }
        }

        return ConversationResponse.builder()
                .id(conv.getId())
                .boxId(conv.getBoxId())
                .boxName(boxName) // [THÊM MỚI]
                .boxAvatar(boxAvatar) // [THÊM MỚI]
                .partner(partnerDto)
                .lastMessageContent(conv.getLastMessageContent())
                .lastMessageAt(conv.getLastMessageAt())
                .lastSenderId(conv.getLastSenderId())
                .unreadCount(unread)
                .status(conv.getStatus() != null ? conv.getStatus().name() : "ACTIVE")
                .build();
    }

    @Override
    @Transactional
    public Conversation createBoxConversation(String boxId, String boxName, String creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found"));

        Conversation conv = Conversation.builder()
                .user1(creator) 
                .user2(creator) 
                .boxId(boxId)
                .lastMessageContent("Nhóm chat đã được tạo") // [ĐÃ ĐỔI] Cho đỡ sến và chuyên nghiệp hơn
                .lastMessageAt(LocalDateTime.now())
                .status(ConversationStatus.ACTIVE)
                .build();

        return conversationRepository.save(conv);
    }

    @Override
    @Transactional
    public void updateBoxConversationInfo(String boxId, String newName) {
        Conversation conv = conversationRepository.findByBoxId(boxId).orElse(null);
        if (conv != null) {
            // Tạm thời chưa cần update vào DTO vì hàm mapToConversationResponse đã tự động Join ra tên mới nhất
        }
    }

    @Override
    @Transactional
    public void addUserToBoxConversation(String boxId, String userId) {
        Conversation conv = conversationRepository.findByBoxId(boxId).orElse(null);
        if (conv == null) return;
        
        User systemOrCreator = conv.getUser1(); 
        User newUser = userRepository.findById(userId).orElseThrow();
        
        Message message = Message.builder()
                .conversation(conv)
                .sender(systemOrCreator)
                .receiver(systemOrCreator) 
                .content(newUser.getFullname() + " đã tham gia không gian.")
                .type(MessageType.SYSTEM) 
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();
                
        messageRepository.save(message);

        conv.setLastMessageContent(message.getContent());
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);
        
        messagingTemplate.convertAndSend("/topic/chat." + conv.getId(), chatMapper.toResponse(message));
    }

    @Override
    @Transactional
    public void removeUserFromBoxConversation(String boxId, String userId) {
        Conversation conv = conversationRepository.findByBoxId(boxId).orElse(null);
        if (conv == null) return;

        User systemOrCreator = conv.getUser1();
        User leftUser = userRepository.findById(userId).orElseThrow();

        Message message = Message.builder()
                .conversation(conv)
                .sender(systemOrCreator)
                .receiver(systemOrCreator)
                .content(leftUser.getFullname() + " đã rời khỏi không gian.")
                .type(MessageType.SYSTEM)
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .build();
                
        messageRepository.save(message);

        conv.setLastMessageContent(message.getContent());
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);

        messagingTemplate.convertAndSend("/topic/chat." + conv.getId(), chatMapper.toResponse(message));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getBoxConversation(String boxId, String userId) {
        Conversation conv = conversationRepository.findByBoxId(boxId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện của Không gian này"));
        
        return mapToConversationResponse(conv, userId);
    }
}