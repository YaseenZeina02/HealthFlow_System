package com.example.healthflow.model;

import java.time.OffsetDateTime;

public class ActivityLog {
    private Long id;
    private Long userId;          // nullable
    private String action;        // NOT NULL
    private String entityType;    // nullable
    private Long entityId;        // nullable
    private String metadataJson;  // نخزّنه كنص (يمكن لاحقًا تستخدم JsonNode)

    private OffsetDateTime createdAt;

    public ActivityLog(){}

    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getUserId(){return userId;}
    public void setUserId(Long userId){this.userId=userId;}
    public String getAction(){return action;}
    public void setAction(String action){this.action=action;}
    public String getEntityType(){return entityType;}
    public void setEntityType(String entityType){this.entityType=entityType;}
    public Long getEntityId(){return entityId;}
    public void setEntityId(Long entityId){this.entityId=entityId;}
    public String getMetadataJson(){return metadataJson;}
    public void setMetadataJson(String metadataJson){this.metadataJson=metadataJson;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(OffsetDateTime createdAt){this.createdAt=createdAt;}
}