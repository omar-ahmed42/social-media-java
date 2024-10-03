package com.omarahmed42.socialmedia.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CursorBasedPagination extends PaginationInfo {
    private String cursor;

    public CursorBasedPagination(Integer pageSize, String cursor) {
        super(0, pageSize);
        this.cursor = cursor;
    }

    public static CursorBasedPagination of(String cursor) {
        return new CursorBasedPagination(15, cursor);
    }
}
