package com.fitness.server.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 饮食记录草案 DTO
 * 用于 create_diet_record_draft 工具
 */
public class DietRecordDraftDto {
    
    private String date;  // 日期，格式：yyyy-MM-dd
    private List<RecordItem> records;  // 饮食记录列表
    
    public static class RecordItem {
        @JsonProperty("meal_type")
        private String mealType;   // 餐次：早餐、午餐、晚餐、加餐
        
        @JsonProperty("food_name")
        private String foodName;   // 食物名称
        
        private Integer calories;  // 热量(kcal)
        private Float protein;     // 蛋白质(g)
        private Float carbs;       // 碳水化合物(g)
        private Float fat;         // 脂肪(g)
        private String amount;     // 份量描述

        @JsonProperty("is_estimated")
        private boolean estimated; // 是否由模型根据食物名称和份量估算
        
        // Getters and Setters
        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }
        
        public String getFoodName() { return foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        
        public Integer getCalories() { return calories; }
        public void setCalories(Integer calories) { this.calories = calories; }
        
        public Float getProtein() { return protein; }
        public void setProtein(Float protein) { this.protein = protein; }
        
        public Float getCarbs() { return carbs; }
        public void setCarbs(Float carbs) { this.carbs = carbs; }
        
        public Float getFat() { return fat; }
        public void setFat(Float fat) { this.fat = fat; }
        
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }

        public boolean isEstimated() { return estimated; }
        public void setEstimated(boolean estimated) { this.estimated = estimated; }
    }
    
    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public List<RecordItem> getRecords() { return records; }
    public void setRecords(List<RecordItem> records) { this.records = records; }
}
