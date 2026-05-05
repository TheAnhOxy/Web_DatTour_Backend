package com.tour.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Document(indexName = "tours_index")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TourDocument {

    @Id
    private String id; // Tour ID

    @Field(type = FieldType.Text, analyzer = "icu_analyzer") // Analyzer cho tiếng Việt
    private String title;

    @Field(type = FieldType.Keyword)
    private List<String> destinations;

    @Field(type = FieldType.Double)
    private BigDecimal basePrice;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Integer)
    private Integer slotsLeft;

    @Field(type = FieldType.Boolean)
    private Boolean isHot;

    @Field(type = FieldType.Date)
    private Date startDate;
}