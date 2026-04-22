package com.vulinh.data.mapper;

import com.vulinh.data.dto.AccountBasicResponse;
import com.vulinh.data.entity.Account;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(builder = @Builder(disableBuilder = true))
public interface AccountMapper {

  AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

  AccountBasicResponse toBasicResponse(Account account);
}
