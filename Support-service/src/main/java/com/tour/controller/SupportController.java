package com.tour.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supports")
@RequiredArgsConstructor
public class SupportController {


    @GetMapping
    public String getHello(){
        return "hello sup";
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