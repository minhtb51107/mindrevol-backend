package com.mindrevol.backend.modules.chat.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindrevol.backend.common.exception.BadRequestException; // Import
import com.mindrevol.backend.common.exception.ResourceNotFoundException;
import com.mindrevol.backend.modules.chat.dto.event.MessageReadEvent;
import com.mindrevol.backend.modules.chat.dto.request.SendMessageRequest;
import com.mindrevol.backend.modules.chat.dto.response.MessageResponse;
import com.mindrevol.backend.modules.chat.entity.Conversation;
import com.mindrevol.backend.modules.chat.entity.Message;
import com.mindrevol.backend.modules.chat.mapper.ChatMapper;
import com.mindrevol.backend.modules.chat.repository.ConversationRepository;
import com.mindrevol.backend.modules.chat.repository.MessageRepository;
import com.mindrevol.backend.modules.chat.service.ChatService;
import com.mindrevol.backend.modules.checkin.entity.Checkin;
import com.mindrevol.backend.modules.checkin.repository.CheckinRepository;
import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.repository.FriendshipRepository; // Import
import com.mindrevol.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CheckinRepository checkinRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMapper chatMapper;
    
    private final FriendshipRepository friendshipRepository; // <--- INJECT THÊM

    @Override
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, User sender) {
        // 1. Tìm người nhận
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Người nhận không tồn tại"));

        // --- BẢO MẬT: CHẶN NGƯỜI LẠ ---
        // Chỉ cho phép nhắn nếu đã là bạn bè (Status = ACCEPTED)
        // Lưu ý: Nếu muốn cho phép nhắn tin chờ (Message Request) thì bỏ check này, 
        // nhưng với mô hình Private Locket thì nên chặn luôn.
        boolean isFriend = friendshipRepository.existsByUsers(sender.getId(), receiver.getId());
        if (!isFriend) {
            throw new BadRequestException("Bạn chỉ có thể nhắn tin cho bạn bè.");
        }
        // ------------------------------

        // 2. Get or Create Conversation (Tối ưu Query)
        Conversation conversation = getOrCreateConversation(sender, receiver);

        // 3. Xử lý Reply Context
        Checkin replyCheckin = null;
        if (request.getReplyToCheckinId() != null) {
            replyCheckin = checkinRepository.findById(request.getReplyToCheckinId()).orElse(null);
        }

        // 4. Lưu tin nhắn
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType())
                .mediaUrl(request.getMediaUrl())
                .replyToCheckin(replyCheckin)
                .isRead(false)
                .build();
        message = messageRepository.save(message);

        // 5. Update Conversation
        conversation.setLastMessageContent(getContentPreview(message));
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 6. Map Response
        MessageResponse response = chatMapper.toResponse(message);

        // 7. Push Realtime
        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(),
                "/queue/messages",
                response
        );

        return response;
    }

    @Override
    public Page<MessageResponse> getConversationMessages(Long partnerId, User currentUser, Pageable pageable) {
        // Tìm hội thoại theo ID đã sort
        Long id1 = Math.min(currentUser.getId(), partnerId);
        Long id2 = Math.max(currentUser.getId(), partnerId);
        
        Conversation conversation = conversationRepository.findBySortedIds(id1, id2)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cuộc trò chuyện"));
        
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable)
                .map(chatMapper::toResponse);
    }

    // --- HÀM MỚI: MARK AS READ ---
    @Override
    @Transactional
    public void markAsRead(Long partnerId, User currentUser) {
        Long id1 = Math.min(currentUser.getId(), partnerId);
        Long id2 = Math.max(currentUser.getId(), partnerId);

        Conversation conversation = conversationRepository.findBySortedIds(id1, id2)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // 1. Update DB (Logic cũ)
        List<Message> unreadMessages = messageRepository.findUnreadMessages(conversation.getId(), currentUser.getId());
        if (unreadMessages.isEmpty()) {
            return; // Không có gì mới để đánh dấu
        }

        for (Message msg : unreadMessages) {
            msg.setRead(true);
        }
        messageRepository.saveAll(unreadMessages);

        // 2. --- LOGIC MỚI: BẮN SOCKET "ĐÃ XEM" ---
        // Gửi sự kiện cho partner (người kia) biết là mình (currentUser) đã đọc
        User partner = (conversation.getUser1().getId().equals(currentUser.getId())) 
                        ? conversation.getUser2() 
                        : conversation.getUser1();

        MessageReadEvent event = MessageReadEvent.builder()
                .conversationId(conversation.getId())
                .readerId(currentUser.getId())
                .partnerId(partner.getId())
                .readAt(LocalDateTime.now().toString())
                .build();

        // Client của Partner sẽ subscribe: /user/queue/chat/read
        messagingTemplate.convertAndSendToUser(
                partner.getEmail(), 
                "/queue/chat/read", 
                event
        );
    }

    private Conversation getOrCreateConversation(User u1, User u2) {
        // Luôn sắp xếp ID để đảm bảo tính duy nhất và khớp Index
        User user1 = u1.getId() < u2.getId() ? u1 : u2;
        User user2 = u1.getId() < u2.getId() ? u2 : u1;
        
        return conversationRepository.findBySortedIds(user1.getId(), user2.getId())
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .user1(user1)
                        .user2(user2)
                        .build()));
    }

    private String getContentPreview(Message msg) {
        if (msg.getType() != null) {
            return switch (msg.getType()) {
                case IMAGE -> "[Hình ảnh]";
                case VOICE -> "[Tin nhắn thoại]";
                default -> msg.getContent();
            };
        }
        return msg.getContent();
    }
}