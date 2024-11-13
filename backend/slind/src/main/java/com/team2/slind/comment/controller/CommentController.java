package com.team2.slind.comment.controller;

import com.team2.slind.comment.dto.response.CommentListResponse;
import com.team2.slind.comment.dto.response.CommentResponse;
import com.team2.slind.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/{articlePk}")
    public ResponseEntity<CommentListResponse> getCommentList(
            @PathVariable Long articlePk,
            @RequestParam(value = "lastCommentPk") Long lastCommentPk
            ) {
        return commentService.getCommentList(articlePk, lastCommentPk, 10);
    }

    @GetMapping("/{articlePk}/best")
    public ResponseEntity<List<CommentResponse>> getBestCommentList(
            @PathVariable Long articlePk
            ) {
        return commentService.getBestCommentList(articlePk, 3);
    }
}