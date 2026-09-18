package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.UnauthorizedException;
import com.newtron.newtron_workforce_backend.dto.chat.ChatMessageRequest;
import com.newtron.newtron_workforce_backend.dto.chat.ChatMessageResponse;
import com.newtron.newtron_workforce_backend.dto.chat.ChatSessionResponse;
import com.newtron.newtron_workforce_backend.dto.chat.CreateChatSessionRequest;
import com.newtron.newtron_workforce_backend.dto.chat.ConversationSummaryResponse;
import com.newtron.newtron_workforce_backend.service.ChatService;
import com.newtron.newtron_workforce_backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final StorageService storageService;

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "User is not authenticated");
        }
        return userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("USER_NOT_FOUND", "User not found"));
    }

    @GetMapping("/conversations")
    public List<ConversationSummaryResponse> getUserConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = getAuthenticatedUser(userDetails);
        
        return chatService.getUserConversations(currentUser);
    }

    @PostMapping("/sessions")
    public ChatSessionResponse createOrGetChatSession(
            @RequestBody CreateChatSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = getAuthenticatedUser(userDetails);
        
        return chatService.getOrCreateChatSession(
                request.getTeamId(), 
                request.getCompanyId(), 
                currentUser);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageResponse> getSessionMessages(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = getAuthenticatedUser(userDetails);
        
        return chatService.getSessionMessages(sessionId, currentUser);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ChatMessageResponse saveMessage(
            @PathVariable Long sessionId,
            @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = getAuthenticatedUser(userDetails);
        
        return chatService.saveMessage(sessionId, currentUser, request.getMessage());
    }

    @PostMapping("/sessions/{sessionId}/messages/media")
    public ChatMessageResponse saveMediaMessage(
            @PathVariable Long sessionId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "message", required = false) String caption,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = getAuthenticatedUser(userDetails);
        
        return chatService.saveMediaMessage(sessionId, currentUser, file, caption);
    }

    @GetMapping("/sessions/{sessionId}/messages/{messageId}/media")
    public ResponseEntity<byte[]> getMediaContent(
            @PathVariable Long sessionId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = getAuthenticatedUser(userDetails);
        
        byte[] content = chatService.getMediaContent(sessionId, messageId, currentUser);
        
        HttpHeaders headers = new HttpHeaders();
        // Since we don't have the key directly here, we could retrieve the ChatMessage in the controller, 
        // but ChatService.getMediaContent abstracts that. 
        // If we want the exact content type, we can use application/octet-stream or guess it. 
        // Given constraints and limited methods, octet-stream is the safest.
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
