/**
 * Payslips Management Page JavaScript
 * 
 * Handles user interactions, form validation, loading states and UI enhancements
 * for the payslips management page. Follows modern ES6+ standards and best practices.
 * 
 * Features:
 * - Form validation and submission handling
 * - Loading states and user feedback
 * - Bulk selection management
 * - File upload validation
 * - Accessibility enhancements
 * - Error handling and user notifications
 */

class PayslipsManager {
    constructor() {
        this.form = null;
        this.uploadForm = null;
        this.sendForm = null;
        this.selectAllCheckbox = null;
        this.payslipCheckboxes = [];
        this.submitButtons = [];
        
        // Configuration
        this.config = {
            maxFileSize: 10 * 1024 * 1024, // 10MB per file
            allowedTypes: ['application/pdf'],
            loadingDelay: 300, // Minimum loading time for better UX
            animationDuration: 300
        };
        
        this.init();
    }

    /**
     * Initialize the payslips manager
     * Sets up event listeners and initial UI state
     */
    init() {
        this.bindElements();
        this.setupEventListeners();
        this.initializeAOS();
        this.setupFormValidation();
        this.updateSelectionUI();
        
        console.log('PayslipsManager initialized successfully');
    }

    /**
     * Bind DOM elements to class properties
     * Caches frequently used elements for better performance
     */
    bindElements() {
        // Forms
        this.uploadForm = document.querySelector('form[action*="/payslips/upload"]');
        this.sendForm = document.getElementById('sendForm');
        
        // Selection controls
        this.selectAllCheckbox = document.getElementById('selectAll');
        this.payslipCheckboxes = document.querySelectorAll('.payslip-checkbox');
        
        // Submit buttons
        this.submitButtons = document.querySelectorAll('button[type="submit"]');
        
        // File input
        this.fileInput = document.getElementById('files');
        
        // Month inputs
        this.monthInputs = document.querySelectorAll('input[type="month"]');
        
        console.log(`Bound ${this.payslipCheckboxes.length} payslip checkboxes`);
    }

    /**
     * Setup all event listeners
     * Organizes event binding for better maintainability
     */
    setupEventListeners() {
        this.setupSelectionListeners();
        this.setupFormListeners();
        this.setupFileUploadListeners();
        this.setupButtonListeners();
        this.setupAccessibilityListeners();
    }

    /**
     * Setup checkbox selection functionality
     * Handles "select all" and individual checkbox behavior
     */
    setupSelectionListeners() {
        // Select all functionality
        if (this.selectAllCheckbox) {
            this.selectAllCheckbox.addEventListener('change', (e) => {
                const isChecked = e.target.checked;
                this.payslipCheckboxes.forEach(checkbox => {
                    if (!checkbox.disabled) {
                        checkbox.checked = isChecked;
                        this.animateCheckbox(checkbox);
                    }
                });
                this.updateSelectionUI();
                this.announceToScreenReader(
                    isChecked ? 'Tutti i cedolini selezionati' : 'Selezione rimossa'
                );
            });
        }

        // Individual checkbox listeners
        this.payslipCheckboxes.forEach(checkbox => {
            checkbox.addEventListener('change', () => {
                this.updateSelectAllState();
                this.updateSelectionUI();
                this.animateCheckbox(checkbox);
            });
        });
    }

    /**
     * Setup form submission handlers
     * Includes validation and loading states
     */
    setupFormListeners() {
        // Upload form validation and submission
        if (this.uploadForm) {
            this.uploadForm.addEventListener('submit', (e) => {
                if (!this.validateUploadForm()) {
                    e.preventDefault();
                    return false;
                }
                this.handleFormSubmission(e.target, 'Caricamento in corso...');
            });
        }

        // Send form validation and submission
        if (this.sendForm) {
            this.sendForm.addEventListener('submit', (e) => {
                const action = e.submitter?.formAction || e.target.action;
                
                if (action.includes('/send')) {
                    if (!this.validateSendForm()) {
                        e.preventDefault();
                        return false;
                    }
                    this.handleFormSubmission(e.target, 'Invio email in corso...');
                } else if (action.includes('/delete')) {
                    if (!this.confirmDeletion(e.submitter)) {
                        e.preventDefault();
                        return false;
                    }
                    this.handleFormSubmission(e.target, 'Eliminazione in corso...');
                }
            });
        }
    }

    /**
     * Setup file upload validation and feedback
     * Provides real-time validation as user selects files
     */
    setupFileUploadListeners() {
        if (this.fileInput) {
            this.fileInput.addEventListener('change', (e) => {
                this.validateFiles(e.target.files);
            });

            // Drag and drop enhancement
            this.setupDragAndDrop();
        }
    }

    /**
     * Setup button interaction enhancements
     * Adds loading states and prevents double submission
     */
    setupButtonListeners() {
        this.submitButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                // Prevent double submission
                if (button.classList.contains('loading')) {
                    e.preventDefault();
                    return false;
                }
            });
        });
    }

    /**
     * Setup accessibility enhancements
     * Keyboard navigation and screen reader support
     */
    setupAccessibilityListeners() {
        // Keyboard navigation for checkboxes
        this.payslipCheckboxes.forEach(checkbox => {
            checkbox.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    checkbox.click();
                }
            });
        });

        // Focus management for forms
        document.addEventListener('focusin', (e) => {
            if (e.target.classList.contains('form-control')) {
                this.highlightFormGroup(e.target, true);
            }
        });

        document.addEventListener('focusout', (e) => {
            if (e.target.classList.contains('form-control')) {
                this.highlightFormGroup(e.target, false);
            }
        });
    }

    /**
     * Initialize AOS (Animate On Scroll) library
     * Handles fallback if AOS is not available
     */
    initializeAOS() {
        if (window.AOS) {
            document.documentElement.classList.add('aos-enabled');
            AOS.init({
                once: true,
                duration: 600,
                easing: 'ease-out'
            });
        } else {
            // Fallback: show all elements immediately
            document.querySelectorAll('[data-aos]').forEach(el => {
                el.classList.add('aos-animate');
            });
        }
    }

    /**
     * Setup form validation
     * Client-side validation for better UX
     */
    setupFormValidation() {
        // Add required validation to month inputs
        this.monthInputs.forEach(input => {
            input.addEventListener('blur', () => {
                this.validateMonthInput(input);
            });
        });

        // Real-time validation for text inputs
        const textInputs = document.querySelectorAll('input[type="text"], textarea');
        textInputs.forEach(input => {
            input.addEventListener('input', () => {
                this.clearValidationState(input);
            });
        });
    }

    /**
     * Validate upload form before submission
     * @returns {boolean} True if form is valid
     */
    validateUploadForm() {
        let isValid = true;
        const monthInput = this.uploadForm.querySelector('input[name="referenceMonth"]');
        const fileInput = this.uploadForm.querySelector('input[name="files"]');

        // Validate month selection
        if (!monthInput.value) {
            this.showFieldError(monthInput, 'Seleziona il mese di riferimento');
            isValid = false;
        } else {
            this.showFieldSuccess(monthInput);
        }

        // Validate file selection
        if (!fileInput.files.length) {
            this.showFieldError(fileInput, 'Seleziona almeno un file PDF');
            isValid = false;
        } else {
            isValid = this.validateFiles(fileInput.files) && isValid;
        }

        return isValid;
    }

    /**
     * Validate send form before submission
     * @returns {boolean} True if form is valid
     */
    validateSendForm() {
        let isValid = true;
        const subjectInput = this.sendForm.querySelector('input[name="subject"]');
        const bodyInput = this.sendForm.querySelector('textarea[name="body"]');
        const selectedCheckboxes = this.sendForm.querySelectorAll('.payslip-checkbox:checked');

        // Validate subject
        if (!subjectInput.value.trim()) {
            this.showFieldError(subjectInput, 'Inserisci l\'oggetto dell\'email');
            isValid = false;
        } else {
            this.showFieldSuccess(subjectInput);
        }

        // Validate body
        if (!bodyInput.value.trim()) {
            this.showFieldError(bodyInput, 'Inserisci il messaggio dell\'email');
            isValid = false;
        } else {
            this.showFieldSuccess(bodyInput);
        }

        // Validate selection
        if (selectedCheckboxes.length === 0) {
            this.showNotification('Seleziona almeno un cedolino da inviare', 'warning');
            isValid = false;
        }

        return isValid;
    }

    /**
     * Validate selected files
     * @param {FileList} files - Files to validate
     * @returns {boolean} True if all files are valid
     */
    validateFiles(files) {
        let isValid = true;
        const errors = [];

        Array.from(files).forEach(file => {
            // Check file type
            if (!this.config.allowedTypes.includes(file.type)) {
                errors.push(`${file.name}: Tipo file non supportato (solo PDF)`);
                isValid = false;
            }

            // Check file size
            if (file.size > this.config.maxFileSize) {
                const sizeMB = (this.config.maxFileSize / (1024 * 1024)).toFixed(0);
                errors.push(`${file.name}: File troppo grande (max ${sizeMB}MB)`);
                isValid = false;
            }

            // Check filename for fiscal code pattern (basic validation)
            if (!this.containsFiscalCodePattern(file.name)) {
                errors.push(`${file.name}: Nome file deve contenere il codice fiscale`);
                isValid = false;
            }
        });

        if (errors.length > 0) {
            this.showFieldError(this.fileInput, errors.join('<br>'));
        } else {
            this.showFieldSuccess(this.fileInput);
            this.showNotification(`${files.length} file${files.length > 1 ? 's' : ''} selezionato${files.length > 1 ? 'i' : ''} correttamente`, 'success');
        }

        return isValid;
    }

    /**
     * Check if filename contains a fiscal code pattern
     * @param {string} filename - Filename to check
     * @returns {boolean} True if pattern found
     */
    containsFiscalCodePattern(filename) {
        // Basic Italian fiscal code pattern: 6 letters + 2 digits + 1 letter + 2 digits + 1 letter + 3 alphanumeric
        const fiscalCodePattern = /[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9A-Z]{3}/i;
        return fiscalCodePattern.test(filename);
    }

    /**
     * Handle form submission with loading state
     * @param {HTMLFormElement} form - Form element
     * @param {string} loadingText - Text to show during loading
     */
    handleFormSubmission(form, loadingText) {
        const submitButton = form.querySelector('button[type="submit"]:focus') || 
                           form.querySelector('button[type="submit"]');
        
        if (submitButton) {
            this.setButtonLoading(submitButton, true, loadingText);
            
            // Set minimum loading time for better UX
            setTimeout(() => {
                // Form will submit naturally, loading state will be cleared on page reload
            }, this.config.loadingDelay);
        }
    }

    /**
     * Confirm deletion action
     * @param {HTMLElement} button - Delete button
     * @returns {boolean} True if confirmed
     */
    confirmDeletion(button) {
        const isMultiple = button.textContent.includes('selezionati');
        const message = isMultiple 
            ? 'Sei sicuro di voler eliminare i cedolini selezionati?'
            : 'Sei sicuro di voler eliminare questo cedolino?';
        
        return confirm(message);
    }

    /**
     * Update "select all" checkbox state based on individual selections
     */
    updateSelectAllState() {
        if (!this.selectAllCheckbox) return;

        const enabledCheckboxes = Array.from(this.payslipCheckboxes).filter(cb => !cb.disabled);
        const checkedCount = enabledCheckboxes.filter(cb => cb.checked).length;
        
        if (checkedCount === 0) {
            this.selectAllCheckbox.checked = false;
            this.selectAllCheckbox.indeterminate = false;
        } else if (checkedCount === enabledCheckboxes.length) {
            this.selectAllCheckbox.checked = true;
            this.selectAllCheckbox.indeterminate = false;
        } else {
            this.selectAllCheckbox.checked = false;
            this.selectAllCheckbox.indeterminate = true;
        }
    }

    /**
     * Update UI based on current selection
     * Shows/hides action buttons and updates counters
     */
    updateSelectionUI() {
        const selectedCount = Array.from(this.payslipCheckboxes).filter(cb => cb.checked).length;
        const actionButtons = document.querySelectorAll('[data-selection-required]');
        
        actionButtons.forEach(button => {
            button.disabled = selectedCount === 0;
        });

        // Update selection counter if exists
        const selectionCounter = document.querySelector('.selection-counter');
        if (selectionCounter) {
            selectionCounter.textContent = `${selectedCount} selezionati`;
            selectionCounter.style.display = selectedCount > 0 ? 'inline' : 'none';
        }
    }

    /**
     * Setup drag and drop functionality for file input
     */
    setupDragAndDrop() {
        const dropZone = this.fileInput.closest('.col-md-6');
        if (!dropZone) return;

        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, this.preventDefaults, false);
        });

        ['dragenter', 'dragover'].forEach(eventName => {
            dropZone.addEventListener(eventName, () => {
                dropZone.classList.add('drag-over');
            }, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, () => {
                dropZone.classList.remove('drag-over');
            }, false);
        });

        dropZone.addEventListener('drop', (e) => {
            const files = e.dataTransfer.files;
            this.fileInput.files = files;
            this.validateFiles(files);
        }, false);
    }

    /**
     * Prevent default drag behaviors
     * @param {Event} e - Event object
     */
    preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    /**
     * Set button loading state
     * @param {HTMLElement} button - Button element
     * @param {boolean} isLoading - Loading state
     * @param {string} loadingText - Text to show during loading
     */
    setButtonLoading(button, isLoading, loadingText = 'Caricamento...') {
        if (isLoading) {
            button.dataset.originalText = button.innerHTML;
            button.classList.add('loading');
            button.disabled = true;
            button.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span>${loadingText}`;
        } else {
            button.classList.remove('loading');
            button.disabled = false;
            button.innerHTML = button.dataset.originalText || button.innerHTML;
        }
    }

    /**
     * Show field validation error
     * @param {HTMLElement} field - Form field
     * @param {string} message - Error message
     */
    showFieldError(field, message) {
        this.clearValidationState(field);
        field.classList.add('is-invalid');
        
        const feedback = document.createElement('div');
        feedback.className = 'invalid-feedback';
        feedback.innerHTML = message;
        field.parentNode.appendChild(feedback);
        
        // Focus the field for accessibility
        field.focus();
    }

    /**
     * Show field validation success
     * @param {HTMLElement} field - Form field
     */
    showFieldSuccess(field) {
        this.clearValidationState(field);
        field.classList.add('is-valid');
    }

    /**
     * Clear field validation state
     * @param {HTMLElement} field - Form field
     */
    clearValidationState(field) {
        field.classList.remove('is-invalid', 'is-valid');
        const feedback = field.parentNode.querySelector('.invalid-feedback, .valid-feedback');
        if (feedback) {
            feedback.remove();
        }
    }

    /**
     * Validate month input
     * @param {HTMLElement} input - Month input element
     */
    validateMonthInput(input) {
        if (!input.value) {
            this.showFieldError(input, 'Seleziona un mese');
        } else {
            this.showFieldSuccess(input);
        }
    }

    /**
     * Highlight form group on focus
     * @param {HTMLElement} field - Form field
     * @param {boolean} highlight - Whether to highlight
     */
    highlightFormGroup(field, highlight) {
        const formGroup = field.closest('.col-md-3, .col-md-6, .col-12');
        if (formGroup) {
            formGroup.classList.toggle('focused', highlight);
        }
    }

    /**
     * Animate checkbox state change
     * @param {HTMLElement} checkbox - Checkbox element
     */
    animateCheckbox(checkbox) {
        checkbox.style.transform = 'scale(1.1)';
        setTimeout(() => {
            checkbox.style.transform = '';
        }, 150);
    }

    /**
     * Show notification to user
     * @param {string} message - Notification message
     * @param {string} type - Notification type (success, warning, error)
     */
    showNotification(message, type = 'info') {
        // Create toast notification
        const toast = document.createElement('div');
        toast.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show position-fixed`;
        toast.style.cssText = 'top: 20px; right: 20px; z-index: 9999; max-width: 400px;';
        toast.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(toast);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (toast.parentNode) {
                toast.classList.remove('show');
                setTimeout(() => toast.remove(), 150);
            }
        }, 5000);
    }

    /**
     * Announce message to screen readers
     * @param {string} message - Message to announce
     */
    announceToScreenReader(message) {
        const announcement = document.createElement('div');
        announcement.setAttribute('aria-live', 'polite');
        announcement.setAttribute('aria-atomic', 'true');
        announcement.className = 'sr-only';
        announcement.textContent = message;
        
        document.body.appendChild(announcement);
        
        setTimeout(() => {
            document.body.removeChild(announcement);
        }, 1000);
    }
}

/**
 * Utility functions
 */
const PayslipsUtils = {
    /**
     * Format file size for display
     * @param {number} bytes - File size in bytes
     * @returns {string} Formatted size string
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    /**
     * Debounce function for performance optimization
     * @param {Function} func - Function to debounce
     * @param {number} wait - Wait time in milliseconds
     * @returns {Function} Debounced function
     */
    debounce(func, wait) {
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
};

/**
 * Initialize the application when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize the payslips manager
    const payslipsManager = new PayslipsManager();
    
    // Make manager available globally for debugging
    window.payslipsManager = payslipsManager;
    
    // Add custom CSS for drag and drop
    const style = document.createElement('style');
    style.textContent = `
        .drag-over {
            border: 2px dashed var(--primary-500) !important;
            background-color: var(--primary-50) !important;
        }
        .focused {
            background-color: var(--primary-50);
            border-radius: 0.5rem;
            transition: background-color 0.15s ease-in-out;
        }
        .sr-only {
            position: absolute !important;
            width: 1px !important;
            height: 1px !important;
            padding: 0 !important;
            margin: -1px !important;
            overflow: hidden !important;
            clip: rect(0, 0, 0, 0) !important;
            white-space: nowrap !important;
            border: 0 !important;
        }
    `;
    document.head.appendChild(style);
});