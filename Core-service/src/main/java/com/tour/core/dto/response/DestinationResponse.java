package com.tour.core.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String cityName;

    private String region;

    private String country;

    @JsonProperty("image_url")
    @JsonAlias("imageUrl")
    private String imageUrl;
}
