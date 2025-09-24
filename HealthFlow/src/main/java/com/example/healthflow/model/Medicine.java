package com.example.healthflow.model;

import java.time.OffsetDateTime;

public class Medicine {
    private Long id;
    private String name;
    private String description;
    private int availableQuantity;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Medicine(){}

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public String getName(){return name;}
    public void setName(String name){this.name=name;}
    public String getDescription(){return description;}
    public void setDescription(String description){this.description=description;}
    public int getAvailableQuantity(){return availableQuantity;}
    public void setAvailableQuantity(int availableQuantity){this.availableQuantity=availableQuantity;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(OffsetDateTime createdAt){this.createdAt=createdAt;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(OffsetDateTime updatedAt){this.updatedAt=updatedAt;}
}