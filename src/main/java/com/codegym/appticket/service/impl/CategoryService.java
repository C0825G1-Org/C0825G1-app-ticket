package com.codegym.appticket.service.impl;

import com.codegym.appticket.entity.EventCategory;
import com.codegym.appticket.repository.ICategoryRepository;
import com.codegym.appticket.service.ICategoryService;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService implements ICategoryService {
    private final ICategoryRepository categoryRepository;

    public CategoryService(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<EventCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public EventCategory findById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NoResultException("No category found with id: " + id + ""));
    }

    @Override
    public Boolean save(EventCategory eventCategory) {
        if (eventCategory.getId() == null) {
            categoryRepository.save(eventCategory);
            return true;
        } else {
            if (!categoryRepository.existsById(eventCategory.getId())) {
                return false;
            } else {
                categoryRepository.save(eventCategory);
                return true;
            }
        }
    }

    @Override
    public Boolean delete(Long id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
