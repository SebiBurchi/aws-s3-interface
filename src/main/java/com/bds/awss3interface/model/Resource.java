package com.bds.awss3interface.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents either a file (type=0) or folder (type=1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    private String id;
    private String name;
    private int type; // 0 for file, 1 for folder
}
