package com.ft.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Entry.
 */
@Data @NoArgsConstructor
@Document(collection = "node")
public class Node implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("name")
    private String name;
    
    @Indexed(unique = true)
    @Field("slug")
    private String slug;
    
    @Field("state")
    private Integer state;

    @Field("type")
    @Indexed
    private String type;
    
    @Field("fields")
    @Indexed
    private Map<String, Object> fields = new HashMap<String, Object>();
    
    @Field("meta")
    private Map<String, Object> meta = new HashMap<String, Object>();
    
    @Indexed
    private Set<String> tags = new HashSet<>();
    
    @Field("created_at")
    @CreatedDate
    private Instant createdAt = Instant.now();

    @Field("created_by")
    @CreatedBy
    private String createdBy;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    @Field("updated_by")
    @LastModifiedBy
    private String updatedBy;
    
    @Field("touched_by")
    private Set<String> touchedBy = new HashSet<>(); 

}
