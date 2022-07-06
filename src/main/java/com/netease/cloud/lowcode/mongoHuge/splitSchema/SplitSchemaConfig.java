package com.netease.cloud.lowcode.mongoHuge.splitSchema;

import com.netease.cloud.lowcode.mongoHuge.pathEntity.SegPath;
import com.netease.cloud.lowcode.mongoHuge.pathEntity.QueryPath;
import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/7/4 16:51
 */

@Data
public class SplitSchemaConfig {

    static public List<SplitQuery> splitQueries = new ArrayList<>();

    static public Map<String, SplitQuery> splitTargets = new HashMap<>();

    static public Map<String, SplitQuery> firstLevelTargets = new HashMap<>();

    static public Map<String, SplitQuery> recurFields = new HashMap<>();

    static {
        Yaml yaml = new Yaml();
        InputStream in = SplitSchemaConfig.class.getClassLoader().getResourceAsStream("mongoSplitSchema.yml");
        Map<String, List> map = yaml.load(in);
        List<Map> splitschema = map.get("splitschema");
        for (Map m : splitschema) {

            QueryPath location = new QueryPath((String) m.get("querypath"));
            String recurField = (String) m.get("recurfield");
            List<String> joint = (List<String>) m.get("jointfield");
            String splitTarget = location.getPath().get(location.getPath().size() - 1).getPath();

            SplitQuery splitQuery = new SplitQuery();
            splitQuery.setJointFields(joint);
            splitQuery.setRecurField(recurField);
            splitQuery.setLocation(location);
            splitQuery.setSplitTarget(splitTarget);

            recurFields.put(recurField, splitQuery);
            splitTargets.put(splitTarget, splitQuery);
            if (location.getPath().size() == 1) {
                firstLevelTargets.put(splitTarget, splitQuery);
            }

            splitQueries.add(splitQuery);
        }
    }

    static public Map<String, SplitQuery> getFirstLevel() {
        Map<String, SplitQuery> concernField = new HashMap<>();
        for (SplitQuery splitQuery : SplitSchemaConfig.splitQueries) {
            List<SegPath> path = splitQuery.getLocation().getPath();
            if (path.size() == 1) {
                concernField.put(path.get(0).getPath(), splitQuery);
            }
        }
        return concernField;
    }

}
























