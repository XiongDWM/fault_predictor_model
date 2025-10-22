package com.xiongdwm.faultpredictor.sdk;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;


public class StringUtils {
    /**
     * simple one-hot encoding to [0,1) based on 32-bit Java String.hashCode()
     */
    public static double oneHotEncode(String value){
        if(value == null||value.isEmpty()) return 0.0;
        int hash = value.hashCode();
        long unsignedHash = hash & 0xffffffffL;
        
        return (double)unsignedHash/(1L<<32);
    }

    /**
     * simple one-hot encoding to [0,1) based on 64-bit hash
     */
    public static double oneHotEncode64(String value){
        if (value == null || value.isEmpty()) {
            return Double.NaN;
        }
        long h64 = simpleStableHash64(value);
        long low53 = h64 & ((1L << 53) - 1);
        return (double) low53 / (double) (1L << 53);
    }

    private static long simpleStableHash64(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        long h = 1469598103934665603L; // FNV offset basis
        for (byte b : bytes) {
            h ^= (b & 0xff);
            h *= 1099511628211L; // FNV prime
        }
        return h;
    }

     public static int bucketByHash64(String v, int buckets) {
        if (v == null || buckets <= 0) return -1;
        long h = simpleStableHash64(v);
        long unsigned = h ^ (h >>> 32); // mix high/low ensure better distribution
        // notice long % buckets can be negative, so use floorMod to ensure non-negative
        int bucket = (int) (Math.floorMod(unsigned, buckets));
        return bucket;
    }

    /**
     * bucket-distribution based on 32-bit Java String.hashCode() (easier, slightly higher collision rate).
     */
    public static int bucketByHash(String v, int buckets) {
        if (v == null || buckets <= 0) return -1;
        int h = v.hashCode();
        long unsigned = h & 0xffffffffL;
        int bucket = (int) (unsigned % buckets);
        if (bucket < 0) bucket += buckets;
        return bucket;
    }

    /**
     * if there is a maintained vocab, this can return a stable index 0..vocab.size()-1,
     * if not found, return -1.
     */
    public static int ordinalEncode(String value, List<String> vocab) {
        Objects.requireNonNull(vocab, "vocab must not be null");
        if (value == null) return -1;
        for (int i = 0; i < vocab.size(); i++) {
            if (value.equals(vocab.get(i))) return i;
        }
        return -1;
    }

    public static void main(String[] args) {
        double v1 = oneHotEncode64("主网一级");
        double v2 = oneHotEncode64("主网四级");
        int b1= bucketByHash64("OPGW+ADSS", 10);
        int b2= bucketByHash64("ADSS", 10);
        System.out.println(b1);
        System.out.println(b2);
        System.out.println(Math.abs(v1 - v2));
    }
}
