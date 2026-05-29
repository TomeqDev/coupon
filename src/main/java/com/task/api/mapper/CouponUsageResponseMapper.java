package com.task.api.mapper;

import com.task.api.dto.CouponUsageResponse;
import com.task.domain.model.CouponUsage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CouponUsageResponseMapper {

    @Mapping(source = "couponCode", target = "couponCode")
    @Mapping(target = "id", source = "couponUsage.id")
    @Mapping(target = "userId", source = "couponUsage.userId")
    @Mapping(target = "usedAt", source = "couponUsage.usedAt")
    CouponUsageResponse toResponse(CouponUsage couponUsage, String couponCode);
}
