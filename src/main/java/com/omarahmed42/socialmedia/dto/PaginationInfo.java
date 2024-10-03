package com.omarahmed42.socialmedia.dto;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaginationInfo implements Serializable {

    private Integer page;
    private Integer pageSize;

    public PaginationInfo(Integer page, Integer pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }
}
