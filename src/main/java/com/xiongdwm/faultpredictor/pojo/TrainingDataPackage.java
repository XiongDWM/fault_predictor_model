package com.xiongdwm.faultpredictor.pojo;

import java.util.List;

public class TrainingDataPackage implements java.io.Serializable {
    static class MetaInfo implements java.io.Serializable {
        String modelVersion;
        List<String> labels;
        List<String> featureNames;
    }
    static class Record implements java.io.Serializable {
        List<Double> features;
        String label;
        String timestamp;
    }
    private static final long serialVersionUID = 1L;
    private String packageId;
    private String domain;
    private MetaInfo meta;
    private List<Record> records;

    public TrainingDataPackage() {
    }

    public TrainingDataPackage(String packageId, String domain, MetaInfo meta, List<Record> records) {
        this.packageId = packageId;
        this.domain = domain;
        this.meta = meta;
        this.records = records;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public MetaInfo getMeta() {
        return meta;
    }

    public void setMeta(MetaInfo meta) {
        this.meta = meta;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((packageId == null) ? 0 : packageId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TrainingDataPackage other = (TrainingDataPackage) obj;
        if (packageId == null) {
            if (other.packageId != null)
                return false;
        } else if (!packageId.equals(other.packageId))
            return false;
        return true;
    }
    
    
    

}
