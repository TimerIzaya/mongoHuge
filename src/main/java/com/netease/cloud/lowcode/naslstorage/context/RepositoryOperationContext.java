package com.netease.cloud.lowcode.naslstorage.context;

import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;

import java.util.List;

/**
 * @author pingerchen
 */
@Data
@Builder
public class RepositoryOperationContext {
    private String appId;
    private List<ObjectId> objectIds;
}
