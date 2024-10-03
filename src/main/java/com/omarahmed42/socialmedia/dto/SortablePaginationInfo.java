package com.omarahmed42.socialmedia.dto;

import com.omarahmed42.socialmedia.enums.SortOrder;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SortablePaginationInfo extends PaginationInfo {

    private SortOrder sort;

    public SortablePaginationInfo(Integer page, Integer pageSize, SortOrder sort) {
        super(page, pageSize);
        this.sort = sort;
    }

}
