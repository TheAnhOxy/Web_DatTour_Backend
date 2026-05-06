//package com.tour.booking.client;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//
//import java.math.BigDecimal;
//
//@FeignClient(name = "core-service")
//public interface CoreClient {
//    @GetMapping("/cores/departures/{id}/price")
//    BigDecimal getDeparturePrice(@PathVariable("id") Long id);
//}
