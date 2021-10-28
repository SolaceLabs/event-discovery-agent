package com.event.discovery.agent.framework.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component("discoveryIdGenerator")
public class IDGenerator {
    private AtomicLong atomicLongBasedOnCurrentTimestamp = new AtomicLong(System.nanoTime());

    public String generateRandomUniqueId(String callerInstanceId) {
        long nextLong = atomicLongBasedOnCurrentTimestamp.incrementAndGet();
        String plainText = nextLong + callerInstanceId;
        return new BigInteger(plainText).toString(36);
    }

    public String generateDeterministicId(String... attributes) {
        return attributes != null && attributes.length != 0 ? DigestUtils.md5DigestAsHex(String.join("-'", attributes).getBytes(UTF_8)) : "";
    }

    public String generateNameId(String name) {
        return StringUtils.isNotBlank(name) ? name.toLowerCase().replaceAll("[^\\dA-Za-z ]", "").trim().replaceAll("\\s+", "-") : "";
    }
}
