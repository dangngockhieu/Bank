package vn.bank.khieu.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import vn.bank.khieu.dto.response.user.ResTellerDTO;
import vn.bank.khieu.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    ResTellerDTO toTellerDTO(User user);
}
