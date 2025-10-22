package com.xiongdwm.faultpredictor.sdk;

public class GpsGridMapping {

    // notice: default grid size 500m, default area Chengdu
    private double gridSize = 0.005d;
    private double minLon =  97.21d;
    private double maxLon = 108.33d;
    private double minLat = 26.03d;
    private double maxLat = 34.19d;

    public GpsGridMapping() {}
    public GpsGridMapping(double gridSize, double minLon, double maxLon, double minLat, double maxLat) {
        this.gridSize = gridSize;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.minLat = minLat;
        this.maxLat = maxLat;
    }
    
    public double toGridIndex(double latitude,double longitude){
        double normalizedLon = Math.max(minLon,Math.min(longitude,maxLon));
        double normalizedLat = Math.max(minLat,Math.min(latitude,maxLat));

        int lonIndex = (int)((normalizedLon - minLon)/gridSize);
        int latIndex = (int)((normalizedLat - minLat)/gridSize);
        int gridIndex = (int)(lonIndex * (1L<<12) + latIndex);
        return((double)gridIndex);
    }

    public double[] gridIndexToCenter(double gridIndex){
        int lonIndex = (int)(gridIndex / (1L<<12));
        int latIndex = (int)(gridIndex % (1L<<12));

        double centerLon = minLon + (lonIndex + 0.5d) * gridSize;
        double centerLat = minLat + (latIndex + 0.5d) * gridSize;
        return new double[]{centerLat,centerLon};
    }

    public static void main(String[] args) {
        double lon = 104.073556;
        double lat = 30.663648;
        GpsGridMapping mapping = new GpsGridMapping();
        double gridIndex = mapping.toGridIndex(lat,lon);
        System.out.println("grid index: "+gridIndex);
        double[] center = mapping.gridIndexToCenter(gridIndex);
        System.out.println("center lat: "+center[0]+", lon: "+center[1]);
    }
}
