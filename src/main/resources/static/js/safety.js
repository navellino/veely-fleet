/**
 * Safety Categories Management Script
 * 
 * This script handles all client-side interactions for the Safety Categories page including:
 * - Modal management for create/edit operations
 * - Form validation and submission
 * - Delete confirmations
 * - Toast notifications
 * - Loading states and user feedback
 * 
 * @author UX/UI Developer
 * @version 1.0.0
 */

// ===== GLOBAL VARIABLES AND CONFIGURATION =====
const SafetyCategories = {
    // DOM Elements Cache - populated on DOMContentLoaded
    elements: {},
    
    // Configuration object
    config: {
        // API endpoints for CRUD operations
        endpoints: {
            create: '/settings/safety-categories/new',
            update: '/settings/safety-categories/{id}/edit',
            delete: '/settings/safety-categories/{id}/delete'
        },
        
        // Validation rules
        validation: {
            name: {
                minLength: 2,
                maxLength: 100,
                required: true
            }
        },
        
        // Toast display duration in milliseconds
        toastDuration: 4000,
        
        // Loading debounce delay
        debounceDelay: 300
    },
    
    // Current state management
    state: {
        isLoading: false,
        currentCategoryId: null,
        isEditMode: false
    }
};

// ===== UTILITY FUNCTIONS =====

/**
 * Utility function to safely get CSRF token from meta tag or hidden input
 * @returns {string} CSRF token value
 */
function getCSRFToken() {
    // Try to get from meta tag first (recommended by Spring Security)
    const metaToken = document.querySelector('meta[name="_csrf"]');
    if (metaToken) {
        return metaToken.getAttribute('content');
    }
    
    // Fallback to hidden input (Thymeleaf approach)
    const inputToken = document.querySelector('input[name="_csrf"]');
    return inputToken ? inputToken.value : '';
}

/**
 * Utility function to safely get CSRF header name
 * @returns {string} CSRF header name
 */
function getCSRFHeader() {
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    return metaHeader ? metaHeader.getAttribute('content') : 'X-CSRF-TOKEN';
}

/**
 * Debounce function to limit rapid function calls
 * @param {Function} func - Function to debounce
 * @param {number} wait - Wait time in milliseconds
 * @returns {Function} Debounced function
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Sanitize HTML to prevent XSS attacks
 * @param {string} str - String to sanitize
 * @returns {string} Sanitized string
 */
function sanitizeHTML(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ===== DOM INITIALIZATION =====

/**
 * Initialize all DOM element references and cache them for performance
 */
function initializeElements() {
    SafetyCategories.elements = {
        // Buttons
        addCategoryBtn: document.getElementById('addCategoryBtn'),
        addFirstCategoryBtn: document.getElementById('addFirstCategoryBtn'),
        submitBtn: document.getElementById('submitBtn'),
        confirmDeleteBtn: document.getElementById('confirmDeleteBtn'),
        
        // Modals
        categoryModal: document.getElementById('categoryModal'),
        deleteModal: document.getElementById('deleteModal'),
        
        // Forms
        categoryForm: document.getElementById('categoryForm'),
        
        // Form fields
        categoryId: document.getElementById('categoryId'),
        categoryName: document.getElementById('categoryName'),
        csrfToken: document.getElementById('csrfToken'),
        
        // Modal elements
        modalTitle: document.getElementById('categoryModalLabel'),
        modalIcon: document.querySelector('.modal-icon'),
        submitText: document.getElementById('submitText'),
        deleteCategoryName: document.getElementById('deleteCategoryName'),
        
        // Loading and state elements
        loadingState: document.getElementById('loadingState'),
        tableContainer: document.getElementById('tableContainer'),
        emptyState: document.getElementById('emptyState'),
        
        // Toast notifications
        successToast: document.getElementById('successToast'),
        errorToast: document.getElementById('errorToast'),
        successMessage: document.getElementById('successMessage'),
        errorMessage: document.getElementById('errorMessage'),
        
        // Table
        categoriesTableBody: document.getElementById('categoriesTableBody')
    };
}

// ===== EVENT LISTENERS SETUP =====

/**
 * Set up all event listeners for the page
 */
function setupEventListeners() {
    // Create new category buttons
    if (SafetyCategories.elements.addCategoryBtn) {
        SafetyCategories.elements.addCategoryBtn.addEventListener('click', handleCreateCategory);
    }
    
    if (SafetyCategories.elements.addFirstCategoryBtn) {
        SafetyCategories.elements.addFirstCategoryBtn.addEventListener('click', handleCreateCategory);
    }
    
    // Form submission
    if (SafetyCategories.elements.categoryForm) {
        SafetyCategories.elements.categoryForm.addEventListener('submit', handleFormSubmit);
    }
    
    // Delete confirmation
    if (SafetyCategories.elements.confirmDeleteBtn) {
        SafetyCategories.elements.confirmDeleteBtn.addEventListener('click', handleDeleteConfirm);
    }
    
    // Real-time validation on name input
    if (SafetyCategories.elements.categoryName) {
        SafetyCategories.elements.categoryName.addEventListener('input', 
            debounce(handleNameValidation, SafetyCategories.config.debounceDelay)
        );
    }
    
    // Set up edit and delete button listeners (event delegation)
    setupTableEventListeners();
    
    // Modal event listeners
    setupModalEventListeners();
    
    // Keyboard shortcuts
    setupKeyboardShortcuts();
}

/**
 * Set up event listeners for table actions using event delegation
 * This allows handling dynamically added table rows
 */
function setupTableEventListeners() {
    const tableBody = SafetyCategories.elements.categoriesTableBody;
    if (!tableBody) return;
    
    tableBody.addEventListener('click', function(event) {
        const target = event.target.closest('button');
        if (!target) return;
        
        // Handle edit button clicks
        if (target.classList.contains('btn-edit')) {
            event.preventDefault();
            const categoryId = target.getAttribute('data-category-id');
            const categoryName = target.getAttribute('data-category-name');
            handleEditCategory(categoryId, categoryName);
        }
        
        // Handle delete button clicks
        if (target.classList.contains('btn-delete')) {
            event.preventDefault();
            const categoryId = target.getAttribute('data-category-id');
            const categoryName = target.getAttribute('data-category-name');
            handleDeleteCategory(categoryId, categoryName);
        }
    });
}

/**
 * Set up modal-specific event listeners
 */
function setupModalEventListeners() {
    // Reset form when modal is hidden
    if (SafetyCategories.elements.categoryModal) {
        SafetyCategories.elements.categoryModal.addEventListener('hidden.bs.modal', function() {
            resetForm();
            resetValidation();
        });
    }
    
    // Focus on name input when modal is shown
    if (SafetyCategories.elements.categoryModal) {
        SafetyCategories.elements.categoryModal.addEventListener('shown.bs.modal', function() {
            if (SafetyCategories.elements.categoryName) {
                SafetyCategories.elements.categoryName.focus();
            }
        });
    }
}

/**
 * Set up keyboard shortcuts for better UX
 */
function setupKeyboardShortcuts() {
    document.addEventListener('keydown', function(event) {
        // Ctrl+N or Cmd+N for new category
        if ((event.ctrlKey || event.metaKey) && event.key === 'n' && !event.shiftKey) {
            event.preventDefault();
            handleCreateCategory();
        }
        
        // Escape to close modals
        if (event.key === 'Escape') {
            const openModal = document.querySelector('.modal.show');
            if (openModal) {
                const modalInstance = bootstrap.Modal.getInstance(openModal);
                if (modalInstance) {
                    modalInstance.hide();
                }
            }
        }
    });
}

// ===== MODAL MANAGEMENT =====

/**
 * Handle create new category action
 */
function handleCreateCategory() {
    SafetyCategories.state.isEditMode = false;
    SafetyCategories.state.currentCategoryId = null;
    
    // Update modal appearance for create mode
    updateModalForCreate();
    
    // Show the modal
    const modal = new bootstrap.Modal(SafetyCategories.elements.categoryModal);
    modal.show();
}

/**
 * Handle edit category action
 * @param {string} categoryId - ID of the category to edit
 * @param {string} categoryName - Current name of the category
 */
function handleEditCategory(categoryId, categoryName) {
    SafetyCategories.state.isEditMode = true;
    SafetyCategories.state.currentCategoryId = categoryId;
    
    // Update modal appearance for edit mode
    updateModalForEdit();
    
    // Populate form with current data
    populateForm(categoryId, categoryName);
    
    // Show the modal
    const modal = new bootstrap.Modal(SafetyCategories.elements.categoryModal);
    modal.show();
}

/**
 * Handle delete category action
 * @param {string} categoryId - ID of the category to delete
 * @param {string} categoryName - Name of the category to delete
 */
function handleDeleteCategory(categoryId, categoryName) {
    SafetyCategories.state.currentCategoryId = categoryId;
    
    // Update delete modal with category name
    if (SafetyCategories.elements.deleteCategoryName) {
        SafetyCategories.elements.deleteCategoryName.textContent = categoryName;
    }
    
    // Show delete confirmation modal
    const modal = new bootstrap.Modal(SafetyCategories.elements.deleteModal);
    modal.show();
}

/**
 * Update modal UI for create mode
 */
function updateModalForCreate() {
    if (SafetyCategories.elements.modalTitle) {
        SafetyCategories.elements.modalTitle.textContent = 'Nuova Categoria';
    }
    
    if (SafetyCategories.elements.submitText) {
        SafetyCategories.elements.submitText.textContent = 'Crea Categoria';
    }
    
    // Update modal icon
    const modalIcon = SafetyCategories.elements.modalIcon?.querySelector('i');
    if (modalIcon) {
        modalIcon.className = 'bi bi-shield-plus';
    }
}

/**
 * Update modal UI for edit mode
 */
function updateModalForEdit() {
    if (SafetyCategories.elements.modalTitle) {
        SafetyCategories.elements.modalTitle.textContent = 'Modifica Categoria';
    }
    
    if (SafetyCategories.elements.submitText) {
        SafetyCategories.elements.submitText.textContent = 'Aggiorna Categoria';
    }
    
    // Update modal icon
    const modalIcon = SafetyCategories.elements.modalIcon?.querySelector('i');
    if (modalIcon) {
        modalIcon.className = 'bi bi-pencil-square';
    }
}

/**
 * Populate form fields with category data
 * @param {string} categoryId - Category ID
 * @param {string} categoryName - Category name
 */
function populateForm(categoryId, categoryName) {
    if (SafetyCategories.elements.categoryId) {
        SafetyCategories.elements.categoryId.value = categoryId;
    }
    
    if (SafetyCategories.elements.categoryName) {
        SafetyCategories.elements.categoryName.value = categoryName;
    }
}

// ===== FORM HANDLING =====

/**
 * Handle form submission
 * @param {Event} event - Form submit event
 */
async function handleFormSubmit(event) {
    event.preventDefault();
    
    // Prevent double submission
    if (SafetyCategories.state.isLoading) {
        return;
    }
    
    // Validate form
    if (!validateForm()) {
        return;
    }
    
    // Show loading state
    setLoadingState(true);
    
    try {
        // Prepare form data
        const formData = new FormData(SafetyCategories.elements.categoryForm);
        
        // Determine endpoint and method
        const isEdit = SafetyCategories.state.isEditMode;
        const endpoint = isEdit 
            ? SafetyCategories.config.endpoints.update.replace('{id}', SafetyCategories.state.currentCategoryId)
            : SafetyCategories.config.endpoints.create;
        
        // Make API request
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                [getCSRFHeader()]: getCSRFToken()
            },
            body: formData
        });
        
        if (response.ok) {
            // Success - close modal and refresh page
            const modal = bootstrap.Modal.getInstance(SafetyCategories.elements.categoryModal);
            modal.hide();
            
            showSuccessToast(isEdit ? 'Categoria aggiornata con successo' : 'Categoria creata con successo');
            
            // Reload page to show updated data
            setTimeout(() => {
                window.location.reload();
            }, 1000);
            
        } else {
            // Handle error response
            const errorData = await response.text();
            throw new Error(`Errore ${response.status}: ${errorData}`);
        }
        
    } catch (error) {
        console.error('Error submitting form:', error);
        showErrorToast('Errore durante il salvataggio. Riprova.');
    } finally {
        setLoadingState(false);
    }
}

/**
 * Handle delete confirmation
 */
async function handleDeleteConfirm() {
    if (!SafetyCategories.state.currentCategoryId || SafetyCategories.state.isLoading) {
        return;
    }
    
    // Show loading state on delete button
    setDeleteLoadingState(true);
    
    try {
        // Prepare form data with CSRF token
        const formData = new FormData();
        formData.append('_csrf', getCSRFToken());
        
        const endpoint = SafetyCategories.config.endpoints.delete
            .replace('{id}', SafetyCategories.state.currentCategoryId);
        
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                [getCSRFHeader()]: getCSRFToken()
            },
            body: formData
        });
        
        if (response.ok) {
            // Success - close modal and refresh page
            const modal = bootstrap.Modal.getInstance(SafetyCategories.elements.deleteModal);
            modal.hide();
            
            showSuccessToast('Categoria eliminata con successo');
            
            // Reload page to show updated data
            setTimeout(() => {
                window.location.reload();
            }, 1000);
            
        } else {
            throw new Error(`Errore ${response.status}`);
        }
        
    } catch (error) {
        console.error('Error deleting category:', error);
        showErrorToast('Errore durante l\'eliminazione. Riprova.');
    } finally {
        setDeleteLoadingState(false);
    }
}

// ===== FORM VALIDATION =====

/**
 * Validate the entire form
 * @returns {boolean} True if form is valid
 */
function validateForm() {
    let isValid = true;
    
    // Validate name field
    if (!validateNameField()) {
        isValid = false;
    }
    
    return isValid;
}

/**
 * Validate the name field
 * @returns {boolean} True if name is valid
 */
function validateNameField() {
    const nameInput = SafetyCategories.elements.categoryName;
    const nameError = document.getElementById('nameError');
    
    if (!nameInput || !nameError) return true;
    
    const name = nameInput.value.trim();
    const config = SafetyCategories.config.validation.name;
    
    // Clear previous validation state
    nameInput.classList.remove('is-valid', 'is-invalid');
    nameError.textContent = '';
    
    // Required validation
    if (config.required && !name) {
        showFieldError(nameInput, nameError, 'Il nome della categoria è obbligatorio');
        return false;
    }
    
    // Length validation
    if (name.length < config.minLength) {
        showFieldError(nameInput, nameError, `Il nome deve essere di almeno ${config.minLength} caratteri`);
        return false;
    }
    
    if (name.length > config.maxLength) {
        showFieldError(nameInput, nameError, `Il nome non può superare ${config.maxLength} caratteri`);
        return false;
    }
    
    // Success state
    nameInput.classList.add('is-valid');
    return true;
}

/**
 * Handle real-time name validation
 */
function handleNameValidation() {
    validateNameField();
}

/**
 * Show field validation error
 * @param {HTMLElement} field - Input field element
 * @param {HTMLElement} errorElement - Error display element
 * @param {string} message - Error message
 */
function showFieldError(field, errorElement, message) {
    field.classList.add('is-invalid');
    errorElement.textContent = message;
}

/**
 * Reset form to initial state
 */
function resetForm() {
    if (SafetyCategories.elements.categoryForm) {
        SafetyCategories.elements.categoryForm.reset();
    }
    
    // Clear hidden fields
    if (SafetyCategories.elements.categoryId) {
        SafetyCategories.elements.categoryId.value = '';
    }
    
    // Set CSRF token
    if (SafetyCategories.elements.csrfToken) {
        SafetyCategories.elements.csrfToken.value = getCSRFToken();
    }
}

/**
 * Reset all validation states
 */
function resetValidation() {
    const inputs = SafetyCategories.elements.categoryForm?.querySelectorAll('.form-control');
    if (inputs) {
        inputs.forEach(input => {
            input.classList.remove('is-valid', 'is-invalid');
        });
    }
    
    const errorElements = SafetyCategories.elements.categoryForm?.querySelectorAll('.invalid-feedback');
    if (errorElements) {
        errorElements.forEach(element => {
            element.textContent = '';
        });
    }
}

// ===== LOADING STATES =====

/**
 * Set loading state for form submission
 * @param {boolean} isLoading - Whether to show loading state
 */
function setLoadingState(isLoading) {
    SafetyCategories.state.isLoading = isLoading;
    
    const submitBtn = SafetyCategories.elements.submitBtn;
    if (!submitBtn) return;
    
    const btnContent = submitBtn.querySelector('.btn-content');
    const btnLoading = submitBtn.querySelector('.btn-loading');
    
    if (isLoading) {
        submitBtn.disabled = true;
        if (btnContent) btnContent.classList.add('d-none');
        if (btnLoading) btnLoading.classList.remove('d-none');
    } else {
        submitBtn.disabled = false;
        if (btnContent) btnContent.classList.remove('d-none');
        if (btnLoading) btnLoading.classList.add('d-none');
    }
}

/**
 * Set loading state for delete button
 * @param {boolean} isLoading - Whether to show loading state
 */
function setDeleteLoadingState(isLoading) {
    const deleteBtn = SafetyCategories.elements.confirmDeleteBtn;
    if (!deleteBtn) return;
    
    const btnContent = deleteBtn.querySelector('.btn-content');
    const btnLoading = deleteBtn.querySelector('.btn-loading');
    
    if (isLoading) {
        deleteBtn.disabled = true;
        if (btnContent) btnContent.classList.add('d-none');
        if (btnLoading) btnLoading.classList.remove('d-none');
    } else {
        deleteBtn.disabled = false;
        if (btnContent) btnContent.classList.remove('d-none');
        if (btnLoading) btnLoading.classList.add('d-none');
    }
}

// ===== TOAST NOTIFICATIONS =====

/**
 * Show success toast notification
 * @param {string} message - Success message to display
 */
function showSuccessToast(message) {
    const toast = SafetyCategories.elements.successToast;
    const messageElement = SafetyCategories.elements.successMessage;
    
    if (!toast || !messageElement) return;
    
    messageElement.textContent = sanitizeHTML(message);
    
    const bsToast = new bootstrap.Toast(toast, {
        autohide: true,
        delay: SafetyCategories.config.toastDuration
    });
    
    bsToast.show();
}

/**
 * Show error toast notification
 * @param {string} message - Error message to display
 */
function showErrorToast(message) {
    const toast = SafetyCategories.elements.errorToast;
    const messageElement = SafetyCategories.elements.errorMessage;
    
    if (!toast || !messageElement) return;
    
    messageElement.textContent = sanitizeHTML(message);
    
    const bsToast = new bootstrap.Toast(toast, {
        autohide: true,
        delay: SafetyCategories.config.toastDuration
    });
    
    bsToast.show();
}

// ===== PAGE INITIALIZATION =====

/**
 * Initialize the page when DOM is loaded
 */
function initializePage() {
    try {
        // Check if Bootstrap is loaded
        if (typeof bootstrap === 'undefined') {
            console.error('Bootstrap is not loaded! Modals will not work.');
            alert('Errore: Bootstrap non è caricato. I modal non funzioneranno.');
            return;
        }
        
        // Initialize DOM elements cache
        initializeElements();
        
        // Set up all event listeners
        setupEventListeners();
        
        // Set initial CSRF token
        if (SafetyCategories.elements.csrfToken) {
            SafetyCategories.elements.csrfToken.value = getCSRFToken();
        }
        
        console.log('Safety Categories page initialized successfully');
        
    } catch (error) {
        console.error('Error initializing Safety Categories page:', error);
        showErrorToast('Errore di inizializzazione della pagina');
    }
}

// ===== ENTRY POINT =====

/**
 * Wait for DOM to be fully loaded before initializing
 */
document.addEventListener('DOMContentLoaded', initializePage);

/**
 * Handle page visibility changes to refresh data when page becomes visible
 */
document.addEventListener('visibilitychange', function() {
    if (!document.hidden && window.location.pathname.includes('safety-categories')) {
        // Page became visible - could refresh data here if needed
        console.log('Safety Categories page became visible');
    }
});

/**
 * Export SafetyCategories object for potential external use
 * This allows other scripts to interact with the safety categories functionality
 */
window.SafetyCategories = SafetyCategories;