package com.tour.core.index;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "tours")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourIndex {

    @Id
    private String id; // Lưu dạng String cho chuẩn cấu trúc ES (Giá trị là Tour ID)

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Double)
    private BigDecimal basePrice;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Boolean)
    private Boolean isHot;

    @Field(type = FieldType.Keyword, index = false)
    private String coverImageUrl;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String categoryName;

    @Field(type = FieldType.Integer)
    private Integer durationDays;

    @Field(type = FieldType.Keyword)
    private String region;

    @Field(type = FieldType.Long)
    private Long departureId;

    // ES lưu ngày tháng chuẩn theo định dạng date_hour_minute_second
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime departureStartDate;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String pickupName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String pickupAddress;

    @Field(type = FieldType.Double)
    private BigDecimal rating;

    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    @Field(type = FieldType.Keyword)
    private String transportationType;

    // 💡 Thêm mảng này để gom toàn bộ địa danh thuộc Tour vào đây, giúp tìm theo "Địa danh / Điểm đến" cực dễ
    @Field(type = FieldType.Text, analyzer = "standard")
    private List<String> destinations;
}