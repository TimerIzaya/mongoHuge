package jedis;

import com.netease.cloud.lowcode.naslstorage.util.JedisPoolUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/16 14:25
 */

public class test {
    public static void main(String[] args) {
        JedisPooled jedis = JedisPoolUtils.getJedis();

        long s = jedis.jsonArrInsert("course", new Path("views[0].type"), 7, "know");
        System.out.println(s);

    }

}