package com.task.infrastructure.persistence.mapper;

import com.task.domain.model.Coupon;
import com.task.infrastructure.persistence.entity.CouponEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    CouponMapper INSTANCE = Mappers.getMapper(CouponMapper.class);

    Coupon toDomain(CouponEntity entity);

    CouponEntity toEntity(Coupon domain);
}
