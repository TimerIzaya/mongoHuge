package com.netease.cloud.lowcode.naslstorage.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @description:
 * @author: sunhaoran
 * @time: 2022/6/16 14:24
 */


public class JedisPoolUtils {
    private static JedisPooled jedisPool;

    static {
        //获取数据，设置到Jedi sPoolConfig中
        JedisPoolConfig config = new JedisPoolConfig();

        //初始化JedisPool
        jedisPool = new JedisPooled("127.0.0.1", 6379);

    }

    //获取连接方法
    public static JedisPooled getJedis() {
        return jedisPool;
    }
}

