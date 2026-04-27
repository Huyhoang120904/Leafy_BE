package com.leafy.messageservice.mapper;

import com.leafy.messageservice.dto.response.AttachmentInfoResponse;
import com.leafy.messageservice.dto.response.ChatNotification;
import com.leafy.messageservice.dto.response.LinkPreviewResponse;
import com.leafy.messageservice.dto.response.MessageResponse;
import com.leafy.messageservice.dto.response.ReplyMetadataResponse;
import com.leafy.messageservice.model.AttachmentInfo;
import com.leafy.messageservice.model.LinkPreview;
import com.leafy.messageservice.model.Message;
import com.leafy.messageservice.model.LastMessageInfo;
import com.leafy.messageservice.dto.response.ReplyMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageMapper {
    
    default OffsetDateTime map(LocalDateTime value) {
        if (value == null) return null;
        return value.atOffset(ZoneOffset.ofHours(7));
    }

    @Mapping(target = "senderAvatar", expression = "java(msg.getSenderAvatar() != null ? baseUrl + msg.getSenderAvatar() : null)")
    @Mapping(target = "replyTo", source = "msg.replyTo")
    @Mapping(target = "metadata", source = "msg.metadata")
    @Mapping(target = "attachments", source = "msg.attachments")
    @Mapping(target = "linkPreview", source = "msg.linkPreview")
    @Mapping(target = "reactions", source = "msg.reactions")
    MessageResponse mapToMessageResponse(Message msg, String baseUrl);

    @Mapping(target = "senderAvatar", expression = "java(msg.getSenderAvatar() != null ? baseUrl + msg.getSenderAvatar() : null)")
    @Mapping(target = "timestamp", source = "msg.createdAt")
    @Mapping(target = "replyTo", source = "msg.replyTo")
    @Mapping(target = "unreadCount", source = "unreadCount")
    @Mapping(target = "metadata", source = "msg.metadata")
    @Mapping(target = "attachments", source = "msg.attachments")
    @Mapping(target = "linkPreview", source = "msg.linkPreview")
    @Mapping(target = "reactions", source = "msg.reactions")
    ChatNotification mapToChatNotification(Message msg, String baseUrl, Integer unreadCount);

    ReplyMetadataResponse mapToReplyMetadataResponse(ReplyMetadata metadata);

    AttachmentInfoResponse mapToAttachmentInfoResponse(AttachmentInfo info);

    LinkPreviewResponse mapToLinkPreviewResponse(LinkPreview linkPreview);

    LinkPreviewResponse.MemberSnapshot mapToMemberSnapshot(LinkPreview.MemberSnapshot snapshot);

    @Mapping(target = "messageId", source = "msg.id")
    @Mapping(target = "timestamp", source = "msg.createdAt")
    LastMessageInfo mapToLastMessageInfo(Message msg);
}
