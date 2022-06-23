package com.netease.cloud.lowcode.naslstorage.naslstorage;

import com.mongodb.client.ClientSession;
import com.netease.cloud.lowcode.naslstorage.NaslstorageApplication;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NaslstorageApplication.class)
class SpringBootApplicationTests {

    @Resource
    MongoTemplate mongoTemplate;

    @Test
    public void testAggregation() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("name").is("course")),
                Aggregation.project("views"),
                Aggregation.unwind("children")
        ).withOptions(AggregationOptions.builder().allowDiskUse(true).build());
        AggregationResults<Map> result = mongoTemplate.aggregate(aggregation, "app", Map.class);

        Iterator<Map> it = result.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

    @Test
    public void testDelete(){

    }

}
