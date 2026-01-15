package com.codegym.appticket.controller;

import com.codegym.appticket.entity.EventCategory;
import com.codegym.appticket.service.ICategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {
    private final ICategoryService categoryService;

    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String showCategories(Model model){
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/category/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new EventCategory());
        return "admin/category/create";
    }

    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("category") EventCategory category,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", category);
            return "admin/category/create";
        }

        categoryService.save(category);
        redirectAttributes.addFlashAttribute("success", "Thêm danh mục thành công!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            EventCategory category = categoryService.findById(id);
            model.addAttribute("category", category);
            return "admin/category/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục!");
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("category") EventCategory category,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        EventCategory eventCategory = categoryService.findById(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("category", category);
            return "admin/category/edit";
        }

        if (eventCategory != null) {
            category.setId(id);
            categoryService.save(category);
            redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi cập nhật danh mục!");
        }

        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        EventCategory category = categoryService.findById(id);

        if (category != null) {
            categoryService.delete(id);
                redirectAttributes.addFlashAttribute("success", "Xóa danh mục thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục để xóa!");
            }
        return "redirect:/admin/categories";
    }
}
