package com.codegym.appticket.listener;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Provider to access Spring ApplicationContext from non-Spring managed classes
 * (e.g., JPA Entity Listeners)
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    
    private static ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
    
    public static ApplicationContext getApplicationContext() {
        return context;
    }
}
