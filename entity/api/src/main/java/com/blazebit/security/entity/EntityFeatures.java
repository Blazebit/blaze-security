package com.blazebit.security.entity;


public final class EntityFeatures {

    private static volatile boolean interceptorActive = false;
    
    private EntityFeatures() {
        
    }
    
    public static void activateInterceptor() {
        interceptorActive = true;
    }
    
    public static void deactivateInterceptor() {
        interceptorActive = false;
    }
    
    public static boolean isInterceptorActive() {
        return interceptorActive;
    }
}
