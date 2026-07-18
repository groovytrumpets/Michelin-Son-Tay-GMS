package com.g42.platform.gms.chat.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode {
    CONVERSATION_NOT_FOUND("CONVERSATION_NOT_FOUND", "Không tìm thấy cuộc trò chuyện"),
    NOT_PARTICIPANT("NOT_PARTICIPANT", "Bạn không thuộc cuộc trò chuyện này"),
    INVALID_PARTICIPANTS("INVALID_PARTICIPANTS", "Danh sách người tham gia không hợp lệ"),
    STAFF_NOT_FOUND("STAFF_NOT_FOUND", "Không tìm thấy nhân viên");

    private final String code;
    private final String message;
}
