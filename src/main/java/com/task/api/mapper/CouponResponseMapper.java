package com.task.api.mapper;

import com.task.api.dto.CouponResponse;
import com.task.domain.model.Coupon;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CouponResponseMapper {

    CouponResponse toResponse(Coupon coupon);
}
