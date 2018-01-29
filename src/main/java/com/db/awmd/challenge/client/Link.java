package com.db.awmd.challenge.client;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.db.awmd.challenge.domain.AccountConstants;

import lombok.Data;

@Data
public class Link {

	// Media type for the Link representation
	public static final String LINK_BASE_MEDIA_TYPE = "application/com.db.link";
	public static final String LINK_BASE_MEDIA_TYPE_JSON = LINK_BASE_MEDIA_TYPE+AccountConstants.JSON;
	
	/**
     * Constant value for the "self" link relation
     */
    public static final String REL_SELF = "self";
    
    private int version = 1;
    
    @NotNull
    @NotEmpty
    private String method;
    
    @NotNull
    @NotEmpty
    private String rel;
    
    @NotNull
    @NotEmpty
    private String uri;
    
    @NotNull
    @NotEmpty
    private String type;
    
    @NotNull
    @NotEmpty
    private String title;
    
    public Link() {
    	
    }
    
    public Link(String title, String method, String rel, String uri, String type){
        setTitle(title);
        setMethod(method);
        setRel(rel);
        setUri(uri);
        setType(type);
    }
}