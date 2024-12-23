package com.team2.slind.common.exception;

public class AlreadyReactedException extends RuntimeException {
    public static final String ALREADY_REACTED_ARTICLE = "이미 좋아요/싫어요한 게시글입니다.";
    public static final String ALREADY_REACTED_JUDGEMENT = "이미 투표한 재판입니다.";
    public static final String ALREADY_REACTED_COMMENT = "이미 좋아요/싫어요한 댓글입니다.";
    public AlreadyReactedException(String message) {
        super(message);
    }
}
