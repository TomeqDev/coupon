package com.task.api.controller;

import com.task.api.dto.CouponResponse;
import com.task.api.dto.CouponUsageResponse;
import com.task.api.dto.CreateCouponRequest;
import com.task.api.dto.RegisterCouponUsageRequest;
import com.task.api.mapper.CouponResponseMapper;
import com.task.api.mapper.CouponUsageResponseMapper;
import com.task.api.util.ClientIpExtractor;
import com.task.application.CreateCouponUseCase;
import com.task.application.RegisterCouponUsageUseCase;
import com.task.domain.model.Coupon;
import com.task.domain.model.CouponUsage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(path = "/api/coupons", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CouponController {

    private final CreateCouponUseCase createCouponUseCase;
    private final RegisterCouponUsageUseCase registerCouponUsageUseCase;
    private final CouponResponseMapper couponResponseMapper;
    private final CouponUsageResponseMapper couponUsageResponseMapper;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = createCouponUseCase.execute(request.code(), request.maxUsages(), request.country());
        CouponResponse response = couponResponseMapper.toResponse(coupon);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(coupon.getCode())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping(path = "/{code}/usages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CouponUsageResponse> registerUsage(
            @PathVariable String code,
            @Valid @RequestBody RegisterCouponUsageRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = ClientIpExtractor.extract(httpRequest);
        CouponUsage usage = registerCouponUsageUseCase.execute(code, request.userId(), ipAddress);
        CouponUsageResponse response = couponUsageResponseMapper.toResponse(usage, code.toUpperCase());

        return ResponseEntity.ok(response);
    }
}
