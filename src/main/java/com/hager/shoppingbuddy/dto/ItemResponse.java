package com.hager.shoppingbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemResponse {

    private Long id;
    private String name;
    private String description;
    private int amount;
    private String category;
}
