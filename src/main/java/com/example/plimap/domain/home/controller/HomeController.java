package com.example.plimap.domain.home.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.home.controller.docs.HomeControllerDocs;
import com.example.plimap.domain.home.dto.response.HomeResponse;
import com.example.plimap.domain.home.exception.HomeSuccessCode;
import com.example.plimap.domain.home.service.query.HomeQueryService;
import com.example.plimap.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
@Validated
public class HomeController implements HomeControllerDocs {

    private final HomeQueryService homeQueryService;

    @Override
    @GetMapping("/context")
    public ResponseEntity<ApiResponse<HomeResponse.Context>> getHomeContext(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        HomeResponse.Context result = homeQueryService.getHomeContext(
                currentMember.getMember().getNickname(),
                latitude,
                longitude
        );
        return ResponseEntity
                .status(HomeSuccessCode.CONTEXT_FETCHED.getStatus())
                .body(ApiResponse.success(HomeSuccessCode.CONTEXT_FETCHED, result));
    }
}
