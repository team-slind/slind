package com.team2.slind.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class  InfiniteListResponse<T>{

    private List<T> list;
    private Boolean hasNext;


}
