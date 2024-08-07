package com.vipa.medllm.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vipa.medllm.dto.response.ResponseResult;
import com.vipa.medllm.model.Project;
import com.vipa.medllm.model.Session;
import com.vipa.medllm.service.session.SessionService;

import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@AllArgsConstructor
@RequestMapping("/session")
public class SessionController {
    private SessionService sessionService;

    @GetMapping("/search")
    public ResponseEntity<ResponseResult<List<Session>>> searchSessions(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Integer imageId) {
        List<Session> sessions = sessionService.searchSessions(sessionId, imageId);

        ResponseResult<List<Session>> response = new ResponseResult<>(200, "Session search successfully", sessions);

        return ResponseEntity.ok(response);
    }

}
