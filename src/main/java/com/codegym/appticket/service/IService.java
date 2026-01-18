package com.codegym.appticket.service;

import java.util.List;

public interface IService<T> {
    List<T> findAll();
    T findById(Long id);
    T save(T entity);
    void delete(Long id);
}
