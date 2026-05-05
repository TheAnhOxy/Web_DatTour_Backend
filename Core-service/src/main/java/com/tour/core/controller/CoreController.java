package com.tour.core.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cores")
@RequiredArgsConstructor
public class CoreController {


    @GetMapping
    public String getHello(){
        return "hello core";
    }

//    @PostMapping
//    public ResponseEntity<ApiResponse> create(@RequestBody FoodRequest request) {
//        return ResponseEntity.status(201).body(ApiResponse.builder()
//                .status(201)
//                .message("Thêm món thành công")
//                .data(foodService.createFood(request))
//                .build());
//    }

}