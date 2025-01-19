package com.bds.awss3interface.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the result of a listing operation, along with an optional pagination cursor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListResult<T> {
    private List<T> resources;
    private String cursor;
}
