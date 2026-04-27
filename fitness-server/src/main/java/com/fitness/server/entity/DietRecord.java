package com.fitness.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("diet_records")
public class DietRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long localId;
    private Long date;
    private String mealType;
    private String foodName;
    private Integer calories;
    private Float protein;
    private Float carbs;
    private Float fat;
    private String amount;
    private Long createdAt;
    private Long updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getLocalId() { return localId; }
    public void setLocalId(Long localId) { this.localId = localId; }
    public Long getDate() { return date; }
    public void setDate(Long date) { this.date = date; }
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
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
