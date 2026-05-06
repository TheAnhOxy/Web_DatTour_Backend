package com.tour.core.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CategoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
}