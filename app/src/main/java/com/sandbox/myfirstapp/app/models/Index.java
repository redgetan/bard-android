package com.sandbox.myfirstapp.app.models;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.dsl.Table;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Table
public class Index {

    private String name;
    private String description;
    private String error;
    private Date createdAt;
    private Long id;

    @Ignore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Index(){

    }

    public Index(String name, String description){
        this.name = name;
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    /**
     *
     * @return createdAt
     * The url
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     *
     * @param createdAt
     * The url
     */
    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
    }

    /**
     *
     * @return
     * The error
     */
    public String getError() {
        return error;
    }

    /**
     *
     * @param error
     * The error
     */
    public void setError(String error) {
        this.error = error;
    }
    /**
     *
     * @return
     * The description
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     * The word_list
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Long getId() {
        return id;
    }

}
