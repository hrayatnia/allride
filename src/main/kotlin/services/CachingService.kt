package me.rayatnia.services

import net.rubyeye.xmemcached.MemcachedClient
import net.rubyeye.xmemcached.MemcachedClientBuilder
import net.rubyeye.xmemcached.XMemcachedClientBuilder
import net.rubyeye.xmemcached.utils.AddrUtil
import org.slf4j.LoggerFactory

class CachingService(memcachedEndpoint: String) {
    private val logger = LoggerFactory.getLogger(CachingService::class.java)
    private val client: MemcachedClient

    init {
        try {
            logger.info("Initializing Memcached client with endpoint: $memcachedEndpoint")
            val builder: MemcachedClientBuilder = XMemcachedClientBuilder(AddrUtil.getAddresses(memcachedEndpoint))
            builder.setConnectionPoolSize(2)
            builder.setOpTimeout(3000) // 3 seconds
            client = builder.build()
            logger.info("Successfully initialized Memcached client")
        } catch (e: Exception) {
            logger.error("Failed to initialize Memcached client", e)
            throw e
        }
    }

    suspend fun get(key: String): String? {
        return try {
            client.get(key)
        } catch (e: Exception) {
            logger.error("Failed to get value for key: $key", e)
            null
        }
    }

    suspend fun set(key: String, value: String, expireSeconds: Int = 3600) {
        try {
            client.set(key, expireSeconds, value)
        } catch (e: Exception) {
            logger.error("Failed to set value for key: $key", e)
            throw e
        }
    }

    suspend fun delete(key: String) {
        try {
            client.delete(key)
        } catch (e: Exception) {
            logger.error("Failed to delete key: $key", e)
            throw e
        }
    }

    fun shutdown() {
        try {
            client.shutdown()
            logger.info("Successfully shut down Memcached client")
        } catch (e: Exception) {
            logger.error("Failed to shut down Memcached client", e)
            throw e
        }
    }
} 