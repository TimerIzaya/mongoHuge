package jedis;

import com.netease.cloud.lowcode.naslstorage.util.JedisPoolUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/16 14:25
 */


@Slf4j
public class test {
    public static void main(String[] args) {
//        JedisPooled jedis = JedisPoolUtils.getJedis();
//        long s = jedis.jsonArrInsert("course", new Path("views[0].type"), 7, "know");
//        System.out.println(s);
        log.debug("asdasd");

    }

}