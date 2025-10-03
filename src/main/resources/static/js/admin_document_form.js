/**
 * Administrative Document Form Enhancement Script
 * 
 * Purpose: Progressive enhancement for administrative document creation/editing form
 * 
 * Features:
 * - Form validation and user feedback
 * - File upload enhancements with drag-and-drop
 * - Dynamic form progress tracking
 * - Keyboard shortcuts and accessibility improvements
 * - Document table interactions
 * 
 * Dependencies: Bootstrap 5.3+
 * Compatibility: Modern browsers (ES6+)
 * 
 * @author UX/UI Enhancement Team
 * @version 1.0.0
 */

(function() {
    'use strict';

    /**
     * Main AdminDocumentForm class
     * Handles all form enhancements and interactions
     */
    class AdminDocumentForm {
        constructor() {
            this.form = null;
            this.fileInput = null;
            this.progressBar = null;
            this.isInitialized = false;
            
            // Configuration
            this.config = {
                maxFileSize: 10 * 1024 * 1024, // 10MB in bytes
                allowedExtensions: ['.pdf', '.doc', '.docx', '.jpg', '.jpeg', '.png', '.webp', '.msg'],
                progressUpdateInterval: 100,
                autoSaveInterval: 30000 // 30 seconds
            };

            // Initialize when DOM is ready
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', () => this.init());
            } else {
                this.init();
            }
        }

        /**
         * Initialize all form enhancements
         * Main entry point for the class
         */
        init() {
            try {
                this.findElements();
                this.setupFormValidation();
                this.setupFileUpload();
                this.setupKeyboardShortcuts();
                this.setupFormProgress();
                this.setupDocumentTable();
                this.setupAccessibilityEnhancements();
                
                this.isInitialized = true;
                console.log('AdminDocumentForm: Initialization complete');
            } catch (error) {
                console.error('AdminDocumentForm: Initialization failed', error);
            }
        }

        /**
         * Find and cache DOM elements
         * Centralizes element selection for better performance
         */
        findElements() {
            this.form = document.querySelector('form[th\\:object="${document}"]') || 
                       document.querySelector('.admin-document-form') ||
                       document.querySelector('form');
            
            if (!this.form) {
                throw new Error('Main form not found');
            }

            // Cache commonly used elements
            this.elements = {
                fileInput: document.querySelector('input[type="file"]'),
                submitBtn: this.form.querySelector('button[type="submit"]'),
                requiredFields: this.form.querySelectorAll('[required]'),
                documentTable: document.querySelector('.documents-table table'),
                uploadForm: document.querySelector('form[enctype="multipart/form-data"]')
            };
        }

        /**
         * Setup enhanced form validation
         * Provides real-time feedback and better UX
         */
        setupFormValidation() {
            if (!this.form) return;

            // Add Bootstrap validation classes
            this.form.classList.add('needs-validation');
            this.form.noValidate = true;

            // Real-time validation for required fields
            this.elements.requiredFields.forEach(field => {
                field.addEventListener('blur', () => this.validateField(field));
                field.addEventListener('input', () => this.clearFieldError(field));
            });

            // Form submission validation
            this.form.addEventListener('submit', (e) => this.handleFormSubmit(e));

            console.log('AdminDocumentForm: Form validation setup complete');
        }

        /**
         * Validate individual form field
         * @param {HTMLElement} field - The field to validate
         */
        validateField(field) {
            const isValid = field.checkValidity();
            
            if (isValid) {
                field.classList.remove('is-invalid');
                field.classList.add('is-valid');
                this.removeFieldError(field);
            } else {
                field.classList.remove('is-valid');
                field.classList.add('is-invalid');
                this.showFieldError(field);
            }

            return isValid;
        }

        /**
         * Clear field validation error state
         * @param {HTMLElement} field - The field to clear
         */
        clearFieldError(field) {
            if (field.classList.contains('is-invalid') && field.checkValidity()) {
                field.classList.remove('is-invalid');
                this.removeFieldError(field);
            }
        }

        /**
         * Show field validation error
         * @param {HTMLElement} field - The field with error
         */
        showFieldError(field) {
            const existingError = field.parentNode.querySelector('.invalid-feedback');
            if (existingError) return;

            const errorDiv = document.createElement('div');
            errorDiv.className = 'invalid-feedback';
            errorDiv.textContent = this.getFieldErrorMessage(field);
            
            field.parentNode.appendChild(errorDiv);
        }

        /**
         * Remove field validation error
         * @param {HTMLElement} field - The field to clear error from
         */
        removeFieldError(field) {
            const errorDiv = field.parentNode.querySelector('.invalid-feedback');
            if (errorDiv) {
                errorDiv.remove();
            }
        }

        /**
         * Get appropriate error message for field
         * @param {HTMLElement} field - The field with error
         * @returns {string} Error message
         */
        getFieldErrorMessage(field) {
            if (field.validity.valueMissing) {
                return 'Questo campo è obbligatorio.';
            }
            if (field.validity.typeMismatch) {
                return 'Formato non valido.';
            }
            if (field.validity.patternMismatch) {
                return 'Il formato inserito non è corretto.';
            }
            return 'Valore non valido.';
        }

        /**
         * Handle form submission with enhanced validation
         * @param {Event} e - Submit event
         */
        handleFormSubmit(e) {
            let isFormValid = true;

            // Validate all required fields
            this.elements.requiredFields.forEach(field => {
                if (!this.validateField(field)) {
                    isFormValid = false;
                }
            });

            if (!isFormValid) {
                e.preventDefault();
                e.stopPropagation();
                
                // Focus on first invalid field
                const firstInvalid = this.form.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                    firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }

                this.showFormError('Completare tutti i campi obbligatori prima di continuare.');
                return false;
            }

            // Show loading state
            this.setSubmitButtonLoading(true);
            return true;
        }

        /**
         * Show form-level error message
         * @param {string} message - Error message to show
         */
        showFormError(message) {
            // Remove existing alerts
            const existingAlert = this.form.querySelector('.alert-danger');
            if (existingAlert) {
                existingAlert.remove();
            }

            // Create new alert
            const alert = document.createElement('div');
            alert.className = 'alert alert-danger alert-dismissible fade show';
            alert.innerHTML = `
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;

            this.form.insertBefore(alert, this.form.firstChild);

            // Auto-dismiss after 5 seconds
            setTimeout(() => {
                if (alert.parentNode) {
                    alert.remove();
                }
            }, 5000);
        }

        /**
         * Setup enhanced file upload functionality
         * Includes drag-and-drop and validation
         */
        setupFileUpload() {
            if (!this.elements.fileInput) return;

            const fileInputContainer = this.elements.fileInput.parentNode;
            
            // Add drag and drop functionality
            this.setupDragAndDrop(fileInputContainer);
            
            // File validation on change
            this.elements.fileInput.addEventListener('change', (e) => {
                this.handleFileSelection(e.target.files[0]);
            });

            // Enhanced upload form submission
            if (this.elements.uploadForm) {
                this.elements.uploadForm.addEventListener('submit', (e) => {
                    this.handleFileUpload(e);
                });
            }

            console.log('AdminDocumentForm: File upload enhancement setup complete');
        }

        /**
         * Setup drag and drop functionality
         * @param {HTMLElement} container - Container element for drag/drop
         */
        setupDragAndDrop(container) {
            ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
                container.addEventListener(eventName, this.preventDefaults, false);
            });

            ['dragenter', 'dragover'].forEach(eventName => {
                container.addEventListener(eventName, () => this.highlightDropZone(container), false);
            });

            ['dragleave', 'drop'].forEach(eventName => {
                container.addEventListener(eventName, () => this.unhighlightDropZone(container), false);
            });

            container.addEventListener('drop', (e) => this.handleDrop(e), false);
        }

        /**
         * Prevent default drag behaviors
         * @param {Event} e - Drag event
         */
        preventDefaults(e) {
            e.preventDefault();
            e.stopPropagation();
        }

        /**
         * Highlight drop zone during drag
         * @param {HTMLElement} container - Drop zone container
         */
        highlightDropZone(container) {
            container.classList.add('dragover');
        }

        /**
         * Remove drop zone highlight
         * @param {HTMLElement} container - Drop zone container
         */
        unhighlightDropZone(container) {
            container.classList.remove('dragover');
        }

        /**
         * Handle file drop
         * @param {Event} e - Drop event
         */
        handleDrop(e) {
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                this.handleFileSelection(files[0]);
                this.elements.fileInput.files = files;
            }
        }

        /**
         * Handle file selection and validation
         * @param {File} file - Selected file
         */
        handleFileSelection(file) {
            if (!file) return;

            // Validate file size
            if (file.size > this.config.maxFileSize) {
                this.showFileError(`File troppo grande. Dimensione massima: ${this.formatFileSize(this.config.maxFileSize)}`);
                return;
            }

            // Validate file extension
            const fileExtension = '.' + file.name.split('.').pop().toLowerCase();
            if (!this.config.allowedExtensions.includes(fileExtension)) {
                this.showFileError(`Formato file non supportato. Formati accettati: ${this.config.allowedExtensions.join(', ')}`);
                return;
            }

            // Show file info
            this.showFileInfo(file);
            this.clearFileError();
        }

        /**
         * Show file error message
         * @param {string} message - Error message
         */
        showFileError(message) {
            const existingError = document.querySelector('.file-error');
            if (existingError) {
                existingError.remove();
            }

            const errorDiv = document.createElement('div');
            errorDiv.className = 'file-error alert alert-danger mt-2';
            errorDiv.innerHTML = `<i class="bi bi-exclamation-triangle me-2"></i>${message}`;
            
            this.elements.fileInput.parentNode.appendChild(errorDiv);
        }

        /**
         * Clear file error message
         */
        clearFileError() {
            const errorDiv = document.querySelector('.file-error');
            if (errorDiv) {
                errorDiv.remove();
            }
        }

        /**
         * Show file information
         * @param {File} file - Selected file
         */
        showFileInfo(file) {
            const existingInfo = document.querySelector('.file-info');
            if (existingInfo) {
                existingInfo.remove();
            }

            const infoDiv = document.createElement('div');
            infoDiv.className = 'file-info alert alert-info mt-2';
            infoDiv.innerHTML = `
                <i class="bi bi-file-earmark me-2"></i>
                <strong>${file.name}</strong> 
                <span class="text-muted">(${this.formatFileSize(file.size)})</span>
            `;
            
            this.elements.fileInput.parentNode.appendChild(infoDiv);
        }

        /**
         * Format file size for display
         * @param {number} bytes - File size in bytes
         * @returns {string} Formatted file size
         */
        formatFileSize(bytes) {
            if (bytes === 0) return '0 Bytes';
            const k = 1024;
            const sizes = ['Bytes', 'KB', 'MB', 'GB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
        }

        /**
         * Handle file upload form submission
         * @param {Event} e - Submit event
         */
        handleFileUpload(e) {
            const fileInput = this.elements.uploadForm.querySelector('input[type="file"]');
            
            if (!fileInput.files[0]) {
                e.preventDefault();
                this.showFileError('Selezionare un file da caricare.');
                return;
            }

            // Show upload progress
            this.showUploadProgress();
        }

        /**
         * Show upload progress indicator
         */
        showUploadProgress() {
            const uploadBtn = this.elements.uploadForm.querySelector('button[type="submit"]');
            if (uploadBtn) {
                uploadBtn.disabled = true;
                uploadBtn.innerHTML = `
                    <span class="spinner-border spinner-border-sm me-2"></span>
                    Caricamento...
                `;
            }
        }

        /**
         * Setup keyboard shortcuts
         * Improves accessibility and power user experience
         */
        setupKeyboardShortcuts() {
            document.addEventListener('keydown', (e) => {
                // Ctrl+S or Cmd+S to save
                if ((e.ctrlKey || e.metaKey) && e.key === 's') {
                    e.preventDefault();
                    if (this.elements.submitBtn && !this.elements.submitBtn.disabled) {
                        this.elements.submitBtn.click();
                    }
                }

                // Escape to clear focus
                if (e.key === 'Escape') {
                    document.activeElement.blur();
                }
            });

            console.log('AdminDocumentForm: Keyboard shortcuts setup complete');
        }

        /**
         * Setup form progress tracking
         * Shows completion percentage for better UX
         */
        setupFormProgress() {
            if (!this.elements.requiredFields.length) return;

            // Create progress indicator if it doesn't exist
            this.createProgressIndicator();

            // Update progress on field changes
            this.elements.requiredFields.forEach(field => {
                field.addEventListener('input', () => this.updateProgress());
                field.addEventListener('change', () => this.updateProgress());
            });

            // Initial progress calculation
            this.updateProgress();
        }

        /**
         * Create progress indicator element
         */
        createProgressIndicator() {
            const existing = document.querySelector('.form-progress');
            if (existing) return;

            const progressContainer = document.createElement('div');
            progressContainer.className = 'form-progress bg-light p-3 rounded mb-3';
            progressContainer.innerHTML = `
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <small class="text-muted">Completamento form</small>
                    <small class="fw-bold text-primary progress-percentage">0%</small>
                </div>
                <div class="progress" style="height: 4px;">
                    <div class="progress-bar bg-primary progress-bar-fill" 
                         style="width: 0%" 
                         role="progressbar" 
                         aria-valuenow="0" 
                         aria-valuemin="0" 
                         aria-valuemax="100"></div>
                </div>
            `;

            this.form.insertBefore(progressContainer, this.form.firstChild);
            this.progressBar = progressContainer.querySelector('.progress-bar-fill');
        }

        /**
         * Update form completion progress
         */
        updateProgress() {
            if (!this.progressBar) return;

            const totalFields = this.elements.requiredFields.length;
            const completedFields = Array.from(this.elements.requiredFields)
                .filter(field => field.value.trim() !== '').length;

            const percentage = totalFields > 0 ? Math.round((completedFields / totalFields) * 100) : 0;

            this.progressBar.style.width = percentage + '%';
            this.progressBar.setAttribute('aria-valuenow', percentage);

            const percentageText = document.querySelector('.progress-percentage');
            if (percentageText) {
                percentageText.textContent = percentage + '%';
            }
        }

        /**
         * Setup document table enhancements
         * Improves interaction with the documents list
         */
        setupDocumentTable() {
            if (!this.elements.documentTable) return;

            // Add hover effects and enhanced interactions
            const rows = this.elements.documentTable.querySelectorAll('tbody tr');
            rows.forEach(row => {
                row.classList.add('document-row');
                
                // Confirm delete actions
                const deleteBtn = row.querySelector('a[onclick*="confirm"]');
                if (deleteBtn) {
                    deleteBtn.addEventListener('click', (e) => {
                        e.preventDefault();
                        this.confirmDocumentDelete(deleteBtn);
                    });
                }
            });

            console.log('AdminDocumentForm: Document table enhancement setup complete');
        }

        /**
         * Enhanced document delete confirmation
         * @param {HTMLElement} deleteBtn - Delete button element
         */
        confirmDocumentDelete(deleteBtn) {
            const fileName = deleteBtn.closest('tr').querySelector('td').textContent.trim();
            
            if (confirm(`Sei sicuro di voler eliminare il documento "${fileName}"?\n\nQuesta operazione non può essere annullata.`)) {
                // Show loading state
                deleteBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
                deleteBtn.style.pointerEvents = 'none';
                
                // Navigate to delete URL
                window.location.href = deleteBtn.href;
            }
        }

        /**
         * Setup accessibility enhancements
         * Improves screen reader and keyboard navigation support
         */
        setupAccessibilityEnhancements() {
            // Add ARIA labels to form sections
            const sections = document.querySelectorAll('.form-section');
            sections.forEach((section, index) => {
                const title = section.querySelector('.form-section-title');
                if (title) {
                    const titleId = `section-title-${index}`;
                    title.id = titleId;
                    section.setAttribute('aria-labelledby', titleId);
                }
            });

            // Enhance file input accessibility
            if (this.elements.fileInput) {
                this.elements.fileInput.setAttribute('aria-describedby', 'file-help');
                
                const helpText = document.createElement('div');
                helpText.id = 'file-help';
                helpText.className = 'visually-hidden';
                helpText.textContent = `Formati supportati: ${this.config.allowedExtensions.join(', ')}. Dimensione massima: ${this.formatFileSize(this.config.maxFileSize)}.`;
                
                this.elements.fileInput.parentNode.appendChild(helpText);
            }

            console.log('AdminDocumentForm: Accessibility enhancements setup complete');
        }

        /**
         * Set submit button loading state
         * @param {boolean} loading - Whether button should show loading state
         */
        setSubmitButtonLoading(loading) {
            if (!this.elements.submitBtn) return;

            if (loading) {
                this.elements.submitBtn.disabled = true;
                const originalText = this.elements.submitBtn.textContent;
                this.elements.submitBtn.dataset.originalText = originalText;
                this.elements.submitBtn.innerHTML = `
                    <span class="spinner-border spinner-border-sm me-2"></span>
                    Salvataggio...
                `;
            } else {
                this.elements.submitBtn.disabled = false;
                const originalText = this.elements.submitBtn.dataset.originalText || 'Salva';
                this.elements.submitBtn.textContent = originalText;
            }
        }

        /**
         * Public method to check if form is initialized
         * @returns {boolean} Initialization status
         */
        isReady() {
            return this.isInitialized;
        }

        /**
         * Public method to manually trigger form validation
         * @returns {boolean} Form validity status
         */
        validateForm() {
            return this.handleFormSubmit({ preventDefault: () => {}, stopPropagation: () => {} });
        }
    }

    // Initialize the form enhancement when script loads
    const adminDocumentForm = new AdminDocumentForm();

    // Make it globally available for debugging/testing
    window.AdminDocumentForm = adminDocumentForm;

    // Export for module systems if needed
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = AdminDocumentForm;
    }

})();

/**
 * Legacy support function for existing onclick handlers
 * Maintains compatibility with existing Thymeleaf templates
 */
function confirmDelete(message) {
    return confirm(message || 'Sei sicuro di voler eliminare questo elemento?');
}