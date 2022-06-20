package com.netease.cloud.lowcode.naslstorage.repository.redis.impl;

import com.netease.cloud.lowcode.naslstorage.context.AppIdContext;
import com.netease.cloud.lowcode.naslstorage.context.RepositoryOperationContext;
import com.netease.cloud.lowcode.naslstorage.repository.redis.RedisAppRepository;
import com.netease.cloud.lowcode.naslstorage.util.JedisPathUtil;
import com.netease.cloud.lowcode.naslstorage.util.JedisPoolUtils;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;

import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/16 14:22
 */

@Repository
public class RedisAppRepositoryImpl implements RedisAppRepository {

    @Override
    public Object get(RepositoryOperationContext context, String jsonPath, List<String> excludes) {
        JedisPooled client = JedisPoolUtils.getJedis();
        String appId = AppIdContext.get();
        String convert = JedisPathUtil.convert(jsonPath);
        if (convert.isEmpty()) {
            return client.jsonGet(appId);
        }
        return client.jsonGet(appId, new Path(convert));
    }

    @Override
    public void initApp(Map<String, Object> object) {
        JedisPooled client = JedisPoolUtils.getJedis();
        String appId = AppIdContext.get();
        client.jsonSet(appId, new Path("$"), object);
    }

    @Override
    public void update(String path, Map<String, Object> object) {
        JedisPooled jedis = JedisPoolUtils.getJedis();
        String appId = AppIdContext.get();
        String convert = JedisPathUtil.convert(path);
        for (String key : object.keySet()) {
            jedis.jsonSet(appId, new Path(convert + "." + key), object.get(key));
        }
    }

    @Override
    public void create(String path, Map<String, Object> object) {
        JedisPooled jedis = JedisPoolUtils.getJedis();
        String appId = AppIdContext.get();
        int lastLeft = path.lastIndexOf('[');
        String jsonPath = path.substring(0, lastLeft);
        int idx = Integer.parseInt(path.substring(lastLeft + 1, path.length() - 1));

        String convert = JedisPathUtil.convert(jsonPath);
        jedis.jsonArrInsert(appId, new Path(convert), idx, object);
    }

    @Override
    public void delete(String path) {
        String convert = JedisPathUtil.convert(path);
        JedisPooled jedis = JedisPoolUtils.getJedis();
        String appId = AppIdContext.get();
        jedis.jsonDel(appId, new Path(convert));
    }
}