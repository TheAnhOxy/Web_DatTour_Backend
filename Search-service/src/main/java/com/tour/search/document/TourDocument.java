package com.tour.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
@Document(indexName = "tours_index") // Phải khớp với tên index tiền bối dùng
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourDocument {
    @Id
    private String id; // Tour ID

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private List<String> destinations;

    @Field(type = FieldType.Nested)
    private List<DepartureDoc> departures;
}