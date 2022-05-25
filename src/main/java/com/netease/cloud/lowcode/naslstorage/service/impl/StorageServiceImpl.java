package com.netease.cloud.lowcode.naslstorage.service.impl;

import com.netease.cloud.lowcode.naslstorage.dto.ActionDTO;
import com.netease.cloud.lowcode.naslstorage.dto.QueryDTO;
import com.netease.cloud.lowcode.naslstorage.repository.AppRepository;
import com.netease.cloud.lowcode.naslstorage.service.StorageService;
import com.netease.cloud.lowcode.naslstorage.util.PathUtil;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StorageServiceImpl implements StorageService {
    @Resource
    private AppRepository appRepository;

    @Override
    public List<Map> batchQuery(List<QueryDTO> queryDTOS) {
        if (CollectionUtils.isEmpty(queryDTOS)) {
            return new ArrayList<>();
        }
        return queryDTOS.stream().map(this::get).collect(Collectors.toList());
    }

    /**
     * 需要支持多文档事务，mongodb版本大于等于4
     * 默认情况下，MongoDB将自动中止任何运行超过60秒的多文档事务
     * @param actionDTOS
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batch(List<ActionDTO> actionDTOS) {
        if (CollectionUtils.isEmpty(actionDTOS)) {
            return;
        }
        actionDTOS.stream().forEach(this::save);
    }

    private Map get(QueryDTO queryDTO) {
        return appRepository.get(queryDTO.getPath(), queryDTO.getExcludes());
    }

    private List<Map> saveView(Object views) {
        List<Map> retIds = new ArrayList<>();
        if (views instanceof ArrayList) {
            List<Map> viewList = (ArrayList<Map>) views;
            viewList.stream().forEach(v->{
                Object children = v.get("children");
                List<Map> ids = saveView(children);
                v.put("children", ids);
                Map t = appRepository.insert("", v);
                ObjectId objectId = (ObjectId) t.get("_id");
                Map r = new HashMap();
                r.put("refId", objectId);
                r.put("name", v.get("name"));
                r.put("children", v.get("children"));
                retIds.add(r);
            });
        }
        return retIds;
    }

    private void save(ActionDTO actionDTO) {
        if (PathUtil.isAppOps(actionDTO.getPath())) {
            /**
             * 由于单个文档16M 的限制，把views 和logics 拆分出来
             * 只保留name 字段占位，结构保持，用于搜索，具体信息打散存放
             * 并且前一层包含下一层的_id 索引
             **/
            Object views = actionDTO.getObject().get("views");
            Object logics = actionDTO.getObject().get("logics");


            List<Map> newViews = saveView(views);
            actionDTO.getObject().put("views", newViews);

            List<Map> newLogicList = new ArrayList<>();
            if (logics instanceof ArrayList) {
                List<Map> logicList = (ArrayList) logics;
                logicList.stream().forEach(v->{
                    Map newLogic = new HashMap();
                    Map logic = appRepository.insert(actionDTO.getPath(), v);
                    ObjectId objectId = (ObjectId) logic.get("_id");
                    newLogic.put("refId", objectId);
                    newLogic.put("name", logic.get("name"));
                    newLogicList.add(newLogic);
                });
            }
            actionDTO.getObject().put("logics", newLogicList);

            appRepository.insert(actionDTO.getPath(), actionDTO.getObject());
        } else {
            appRepository.update(actionDTO.getPath(), actionDTO.getObject());
        }
    }
}
