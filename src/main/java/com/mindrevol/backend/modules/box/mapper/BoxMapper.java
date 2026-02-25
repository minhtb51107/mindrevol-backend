package com.mindrevol.backend.modules.box.mapper;

import com.mindrevol.backend.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.backend.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.backend.modules.box.dto.response.BoxResponse;
import com.mindrevol.backend.modules.box.entity.Box;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BoxMapper {

    Box toEntity(CreateBoxRequest request);

    @Mapping(target = "ownerId", source = "box.owner.id")
    @Mapping(target = "memberCount", source = "memberCount")
    BoxResponse toResponse(Box box, long memberCount);

    // Cập nhật Entity từ Request (Chỉ update các trường không null)
    void updateEntityFromRequest(UpdateBoxRequest request, @MappingTarget Box box);
}