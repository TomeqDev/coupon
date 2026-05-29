package com.task.infrastructure.persistence.mapper;

import com.task.domain.model.CouponUsage;
import com.task.infrastructure.persistence.entity.CouponUsageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CouponUsageMapper {

    CouponUsageMapper INSTANCE = Mappers.getMapper(CouponUsageMapper.class);

    CouponUsage toDomain(CouponUsageEntity entity);

    CouponUsageEntity toEntity(CouponUsage domain);
}
