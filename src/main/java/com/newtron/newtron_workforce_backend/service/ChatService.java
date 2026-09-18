package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.BusinessException;
import com.newtron.newtron_workforce_backend.common.exception.ForbiddenException;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.dto.chat.ChatMessageResponse;
import com.newtron.newtron_workforce_backend.dto.chat.ChatSessionResponse;
import com.newtron.newtron_workforce_backend.dto.chat.ConversationSummaryResponse;
import com.newtron.newtron_workforce_backend.entity.Application;
import com.newtron.newtron_workforce_backend.entity.ChatMessage;
import com.newtron.newtron_workforce_backend.entity.ChatSession;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.entity.Team;
import com.newtron.newtron_workforce_backend.repository.ApplicationRepository;
import com.newtron.newtron_workforce_backend.repository.ChatMessageRepository;
import com.newtron.newtron_workforce_backend.repository.ChatSessionRepository;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import com.newtron.newtron_workforce_backend.repository.JobRepository;
import com.newtron.newtron_workforce_backend.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final StorageService storageService;

    @Transactional
    public ChatSessionResponse getOrCreateChatSession(Long teamId, Long companyId, User currentUser) {
        ChatSession session = chatSessionRepository.findByTeamIdAndCompanyId(teamId, companyId)
                .orElseGet(() -> createChatSession(teamId, companyId));

        validateChatAccess(session, currentUser);
        return mapToSessionResponse(session);
    }

    private ChatSession createChatSession(Long teamId, Long companyId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND", "Team not found with ID: " + teamId));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("COMPANY_NOT_FOUND", "Company not found with ID: " + companyId));

        ChatSession session = ChatSession.builder()
                .team(team)
                .company(company)
                .build();

        return chatSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getUserConversations(User currentUser) {
        List<ChatSession> sessions = chatSessionRepository.findUserConversations(currentUser.getId());
        
        return sessions.stream().map(session -> {
            List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
            ChatMessage latestMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);
            
            return ConversationSummaryResponse.builder()
                    .sessionId(session.getId())
                    .companyId(session.getCompany().getId())
                    .companyName(session.getCompany().getCompanyName())
                    .latestMessage(latestMessage != null ? latestMessage.getMessage() : null)
                    .latestMessageTimestamp(latestMessage != null ? latestMessage.getCreatedAt() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getSessionMessages(Long sessionId, User currentUser) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SESSION_NOT_FOUND", "ChatSession not found with ID: " + sessionId));

        validateChatAccess(session, currentUser);

        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long sessionId, User currentUser, String messageText) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SESSION_NOT_FOUND", "ChatSession not found with ID: " + sessionId));

        validateChatAccess(session, currentUser);

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .senderUser(currentUser)
                .message(messageText)
                .build();

        return mapToMessageResponse(chatMessageRepository.save(message));
    }

    @Transactional
    public ChatMessageResponse saveMediaMessage(Long sessionId, User currentUser, MultipartFile file, String caption) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SESSION_NOT_FOUND", "ChatSession not found with ID: " + sessionId));

        validateChatAccess(session, currentUser);

        String fileKey = storageService.uploadDocument(file);

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .senderUser(currentUser)
                .message(caption != null ? caption : "")
                .messageType("IMAGE")
                .attachmentUrl("/api/v1/chat/sessions/" + sessionId + "/messages/{messageId}/media") // Will be updated after save
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        savedMessage.setAttachmentUrl("/api/v1/chat/sessions/" + sessionId + "/messages/" + savedMessage.getId() + "/media?key=" + fileKey);
        chatMessageRepository.save(savedMessage);

        return mapToMessageResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public byte[] getMediaContent(Long sessionId, Long messageId, User currentUser) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SESSION_NOT_FOUND", "ChatSession not found with ID: " + sessionId));

        validateChatAccess(session, currentUser);

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("MESSAGE_NOT_FOUND", "Message not found with ID: " + messageId));

        if (!message.getSession().getId().equals(session.getId())) {
            throw new BusinessException("INVALID_RELATION", "Message does not belong to the specified ChatSession.");
        }

        if (!"IMAGE".equals(message.getMessageType())) {
            throw new BusinessException("INVALID_MESSAGE_TYPE", "Message is not an image.");
        }

        // Extract key from the URL safely. The URL was constructed as .../media?key=<fileKey>
        String url = message.getAttachmentUrl();
        if (url == null || !url.contains("key=")) {
             throw new ResourceNotFoundException("MEDIA_NOT_FOUND", "Media key not found");
        }
        String fileKey = url.substring(url.indexOf("key=") + 4);

        byte[] content = storageService.getDocumentContent(fileKey);
        if (content == null) {
             throw new ResourceNotFoundException("FILE_CONTENT_NOT_FOUND", "File content was not found or is empty");
        }
        return content;
    }

    private void validateChatAccess(ChatSession session, User currentUser) {
        boolean isTeamLead = session.getTeam() != null 
            && session.getTeam().getOwnerWorkerProfile() != null 
            && session.getTeam().getOwnerWorkerProfile().getUser().getId().equals(currentUser.getId());
            
        boolean isCompanyOwner = session.getCompany() != null 
            && session.getCompany().getOwner() != null 
            && session.getCompany().getOwner().getId().equals(currentUser.getId());
            
        boolean isRecruiter = session.getJob() != null 
            && session.getJob().getRecruiter() != null 
            && session.getJob().getRecruiter().getId().equals(currentUser.getId());

        if (!isTeamLead && !isCompanyOwner && !isRecruiter) {
            throw new ForbiddenException("FORBIDDEN", "You do not have permission to access this chat session.");
        }

        // Post-hire consistency checks
        if (session.getApplication() != null) {
            if (session.getJob() != null && !session.getApplication().getJob().getId().equals(session.getJob().getId())) {
                throw new BusinessException("INVALID_RELATION", "Application does not belong to the specified Job.");
            }
            if (session.getTeam() != null && !session.getApplication().getTeam().getId().equals(session.getTeam().getId())) {
                throw new BusinessException("INVALID_RELATION", "Application does not belong to the specified Team.");
            }
        }
    }

    private ChatSessionResponse mapToSessionResponse(ChatSession session) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .jobId(session.getJob() != null ? session.getJob().getId() : null)
                .teamId(session.getTeam().getId())
                .applicationId(session.getApplication() != null ? session.getApplication().getId() : null)
                .build();
    }

    private ChatMessageResponse mapToMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSession().getId())
                .senderUserId(message.getSenderUser().getId())
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .attachmentUrl(message.getAttachmentUrl())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
