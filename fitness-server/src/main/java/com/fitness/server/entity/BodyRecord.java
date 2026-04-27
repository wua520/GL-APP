package com.fitness.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("body_records")
public class BodyRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long localId;
    private Long date;
    private Float weight;
    private Float bodyFat;
    private Float muscleMass;
    private String note;
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
    public Float getWeight() { return weight; }
    public void setWeight(Float weight) { this.weight = weight; }
    public Float getBodyFat() { return bodyFat; }
    public void setBodyFat(Float bodyFat) { this.bodyFat = bodyFat; }
    public Float getMuscleMass() { return muscleMass; }
    public void setMuscleMass(Float muscleMass) { this.muscleMass = muscleMass; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
