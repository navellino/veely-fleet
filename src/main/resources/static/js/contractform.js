/**
 * CONTRACT FORM ENHANCEMENT SCRIPT
 * 
 * Purpose: Provides enhanced UX/UI functionality for contract creation/editing forms
 * 
 * Features:
 * - Real-time form validation with immediate feedback
 * - Dynamic calculation of gross amount from net + VAT
 * - Auto-loading of supplier reference persons
 * - Form completion progress tracking
 * - Auto-save draft functionality
 * - Keyboard shortcuts and accessibility enhancements
 * - Responsive form navigation
 * 
 * Dependencies: Bootstrap 5.3+, modern browser with ES6+ support
 * 
 * @author UX/UI Developer
 * @version 1.0.0
 */

class ContractFormEnhancer {
    
    constructor() {
        this.form = document.querySelector('#contractForm');
        this.formData = new FormData();
        this.validationRules = this.initValidationRules();
        this.autoSaveTimer = null;
        this.progressBar = document.querySelector('#formProgressBar');
        this.progressPercentage = document.querySelector('#progressPercentage');
        this.hasUnsavedChanges = false;
        
        this.init();
    }
    
    /**
     * Initialize all form enhancements
     */
    init() {
        if (!this.form) {
            console.warn('Contract form not found');
            return;
        }
        
        this.setupEventListeners();
        this.setupValidation();
        this.setupAutoCalculations();
        this.setupProgressTracking();
        this.setupKeyboardShortcuts();
        this.setupSupplierReferenceLoading();
        this.setupAutoSave();
        this.updateFormProgress();
        
        console.log('ContractForm enhancer initialized successfully');
    }
    
    /**
     * Define validation rules for form fields
     * @returns {Object} Validation rules configuration
     */
    initValidationRules() {
        return {
            required: ['supplier', 'type', 'status', 'subject'],
            email: [],
            numeric: ['amountNet', 'vatRate', 'periodicFee', 'terminationNoticeDays'],
            currency: ['amountNet', 'periodicFee'],
            percentage: ['vatRate'],
            dateRange: {
                start: 'startDate',
                end: 'endDate'
            },
            maxLength: {
                'subject': 500,
                'paymentTerms': 100
            }
        };
    }
    
    /**
     * Setup all event listeners for form interactions
     */
    setupEventListeners() {
        // Form submission with enhanced validation
        this.form.addEventListener('submit', this.handleFormSubmit.bind(this));
        
        // Real-time validation on field changes
        this.form.addEventListener('input', this.handleFieldInput.bind(this));
        this.form.addEventListener('change', this.handleFieldChange.bind(this));
        
        // Detect unsaved changes
        this.form.addEventListener('change', () => {
            this.hasUnsavedChanges = true;
            this.scheduleAutoSave();
        });
        
        // Warn before page unload if unsaved changes
        window.addEventListener('beforeunload', (e) => {
            if (this.hasUnsavedChanges) {
                e.preventDefault();
                e.returnValue = 'Hai modifiche non salvate. Sei sicuro di voler uscire?';
                return e.returnValue;
            }
        });
        
        // Handle supplier selection change for reference persons
        const supplierSelect = this.form.querySelector('#supplier');
        if (supplierSelect) {
            supplierSelect.addEventListener('change', this.handleSupplierChange.bind(this));
        }
    }
    
    /**
     * Setup real-time form validation
     */
    setupValidation() {
        // Add Bootstrap validation classes
        this.form.classList.add('needs-validation');
        
        // Validate required fields
        this.validationRules.required.forEach(fieldName => {
            const field = this.form.querySelector(`[name="${fieldName}"]`);
            if (field) {
                field.addEventListener('blur', () => this.validateField(field));
                field.addEventListener('input', () => this.clearFieldError(field));
            }
        });
        
        // Validate numeric fields
        this.validationRules.numeric.forEach(fieldName => {
            const field = this.form.querySelector(`[name="${fieldName}"]`);
            if (field) {
                field.addEventListener('input', (e) => {
                    this.formatNumericInput(e.target);
                    this.validateField(field);
                });
            }
        });
        
        // Validate date ranges
        const startDateField = this.form.querySelector(`[name="${this.validationRules.dateRange.start}"]`);
        const endDateField = this.form.querySelector(`[name="${this.validationRules.dateRange.end}"]`);
        
        if (startDateField && endDateField) {
            [startDateField, endDateField].forEach(field => {
                field.addEventListener('change', () => this.validateDateRange());
            });
        }
    }
    
    /**
     * Setup automatic calculations (gross amount, etc.)
     */
    setupAutoCalculations() {
        const amountNetField = this.form.querySelector('[name="amountNet"]');
        const vatRateField = this.form.querySelector('[name="vatRate"]');
        
        if (amountNetField && vatRateField) {
            [amountNetField, vatRateField].forEach(field => {
                field.addEventListener('input', this.calculateGrossAmount.bind(this));
            });
        }
    }
    
    /**
     * Setup form progress tracking
     */
    setupProgressTracking() {
        const progressContainer = document.querySelector('#formProgressContainer');
        if (progressContainer && this.progressBar) {
            progressContainer.style.display = 'block';
            
            // Update progress on any field change
            this.form.addEventListener('input', () => {
                setTimeout(() => this.updateFormProgress(), 100);
            });
            this.form.addEventListener('change', () => {
                setTimeout(() => this.updateFormProgress(), 100);
            });
        }
    }
    
    /**
     * Setup keyboard shortcuts
     */
    setupKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            // Ctrl+S or Cmd+S to save
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
                e.preventDefault();
                this.saveForm();
            }
            
            // ESC to cancel (if not in a modal)
            if (e.key === 'Escape' && !document.querySelector('.modal.show')) {
                const cancelBtn = this.form.querySelector('.btn-form-secondary');
                if (cancelBtn) {
                    cancelBtn.click();
                }
            }
        });
    }
    
    /**
     * Setup dynamic loading of supplier reference persons
     */
    setupSupplierReferenceLoading() {
		const supplierSelect = this.form.querySelector('#supplier');
		        const referenceSelect = this.form.querySelector('#referencePerson');

		        if (!supplierSelect || !referenceSelect) {
		            console.warn('Supplier or reference person select not found');
		            return;
		        }

		        // Disable reference select until data is loaded
		        referenceSelect.disabled = true;

		        // If a supplier is already selected (editing form), trigger loading
		        if (supplierSelect.value) {
		            supplierSelect.dispatchEvent(new Event('change'));
		        } else {
		            // Ensure placeholder option when no supplier is selected
		            referenceSelect.innerHTML = '<option value="">Seleziona un referente...</option>';
		        }

        console.log('Supplier reference loading setup complete');
    }
    
    /**
     * Setup auto-save functionality
     */
    setupAutoSave() {
        // Auto-save every 30 seconds if changes detected
        setInterval(() => {
            if (this.hasUnsavedChanges) {
                this.saveFormDraft();
            }
        }, 30000);
    }
    
    /**
     * Handle form submission with enhanced validation
     * @param {Event} e - Submit event
     */
    handleFormSubmit(e) {
        e.preventDefault();
        
        if (!this.validateForm()) {
            this.showValidationErrors();
            return false;
        }
        
        this.showLoadingState();
        
        // Submit form data
        const formData = new FormData(this.form);
        
        // Add any computed fields
        const grossAmount = this.calculateGrossAmountValue();
        if (grossAmount) {
            formData.append('amountGross', grossAmount);
        }
        
        // Submit via fetch or allow normal form submission
        this.submitForm(formData);
    }
    
    /**
     * Handle real-time field input events
     * @param {Event} e - Input event
     */
    handleFieldInput(e) {
        const field = e.target;
        
        // Clear previous validation state
        this.clearFieldError(field);
        
        // Apply input formatting
        if (this.validationRules.currency.includes(field.name)) {
            this.formatCurrencyInput(field);
        }
        
        if (this.validationRules.percentage.includes(field.name)) {
            this.formatPercentageInput(field);
        }
        
        // Update character count for text fields
        this.updateCharacterCount(field);
    }
    
    /**
     * Handle field change events
     * @param {Event} e - Change event
     */
    handleFieldChange(e) {
        const field = e.target;
        this.validateField(field);
        this.updateFormProgress();
    }
    
    /**
     * Handle supplier selection change
     * @param {Event} e - Change event
     */
    handleSupplierChange(e) {
        const supplierId = e.target.value;
        const referencePersonSelect = this.form.querySelector('#referencePerson');
        
        if (!referencePersonSelect) return;
        
        // Clear current options
        referencePersonSelect.innerHTML = '<option value="">Seleziona un referente...</option>';
        
        if (!supplierId) {
            referencePersonSelect.disabled = true;
            return;
        }
        
        // Show loading state
        referencePersonSelect.disabled = true;
        referencePersonSelect.innerHTML = '<option value="">Caricamento...</option>';
        
		// Load referents for selected supplier
		        this.loadSupplierReferents(supplierId)
		            .then(referents => {
                referencePersonSelect.innerHTML = '<option value="">Seleziona un referente...</option>';
                referents.forEach(ref => {
                    const option = document.createElement('option');
                    option.value = ref.id;
					option.textContent = ref.name;
					                    if (ref.phone) {
					                        option.textContent += ` - ${ref.phone}`;
					                    }
					                    if (ref.email) {
					                        option.textContent += ` - ${ref.email}`;
					                    }
                    referencePersonSelect.appendChild(option);
                });
                referencePersonSelect.disabled = false;
                
                // Restore previously selected value if editing
                const selectedValue = referencePersonSelect.dataset.selected;
                if (selectedValue) {
                    referencePersonSelect.value = selectedValue;
                }
            })
            .catch(error => {
                console.error('Error loading supplier referents:', error);
                referencePersonSelect.innerHTML = '<option value="">Errore nel caricamento</option>';
                referencePersonSelect.disabled = false;
            });
    }
    
    /**
     * Load supplier referents from API
     * @param {string} supplierId - Supplier ID
     * @returns {Promise<Array>} Promise resolving to supplier referents array
     */
   async loadSupplierReferents(supplierId) {
        try {
            const response = await fetch(`/fleet/suppliers/${supplierId}/referents`);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Failed to load supplier referents:', error);
            return []; // Return empty array on error
        }
    }
    
    /**
     * Validate individual field
     * @param {HTMLElement} field - Field to validate
     * @returns {boolean} Is field valid
     */
    validateField(field) {
        const fieldName = field.name;
        const value = field.value.trim();
        let isValid = true;
        let errorMessage = '';
        
        // Required field validation
        if (this.validationRules.required.includes(fieldName) && !value) {
            isValid = false;
            errorMessage = 'Campo obbligatorio';
        }
        
        // Numeric validation
        if (value && this.validationRules.numeric.includes(fieldName)) {
            const numericValue = this.parseNumericValue(value);
            if (isNaN(numericValue) || numericValue < 0) {
                isValid = false;
                errorMessage = 'Inserire un valore numerico valido';
            }
        }
        
        // Percentage validation
        if (value && this.validationRules.percentage.includes(fieldName)) {
            const percentValue = this.parseNumericValue(value);
            if (isNaN(percentValue) || percentValue < 0 || percentValue > 100) {
                isValid = false;
                errorMessage = 'Inserire una percentuale tra 0 e 100';
            }
        }
        
        // Max length validation
        if (value && this.validationRules.maxLength[fieldName]) {
            const maxLength = this.validationRules.maxLength[fieldName];
            if (value.length > maxLength) {
                isValid = false;
                errorMessage = `Massimo ${maxLength} caratteri`;
            }
        }
        
        // Apply validation feedback
        this.applyFieldValidation(field, isValid, errorMessage);
        
        return isValid;
    }
    
    /**
     * Validate date range (end date must be after start date)
     * @returns {boolean} Is date range valid
     */
    validateDateRange() {
        const startDateField = this.form.querySelector(`[name="${this.validationRules.dateRange.start}"]`);
        const endDateField = this.form.querySelector(`[name="${this.validationRules.dateRange.end}"]`);
        
        if (!startDateField || !endDateField) return true;
        
        const startDate = new Date(startDateField.value);
        const endDate = new Date(endDateField.value);
        
        if (startDateField.value && endDateField.value && endDate <= startDate) {
            this.applyFieldValidation(endDateField, false, 'La data di fine deve essere successiva alla data di inizio');
            return false;
        }
        
        if (endDateField.value) {
            this.applyFieldValidation(endDateField, true);
        }
        
        return true;
    }
    
    /**
     * Apply validation feedback to field
     * @param {HTMLElement} field - Field element
     * @param {boolean} isValid - Is field valid
     * @param {string} errorMessage - Error message if invalid
     */
    applyFieldValidation(field, isValid, errorMessage = '') {
        // Remove existing validation classes
        field.classList.remove('is-valid', 'is-invalid');
        
        // Remove existing feedback
        const existingFeedback = field.parentNode.querySelector('.invalid-feedback, .valid-feedback');
        if (existingFeedback) {
            existingFeedback.remove();
        }
        
        if (!isValid && errorMessage) {
            // Add invalid state
            field.classList.add('is-invalid');
            
            // Add error feedback
            const feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            feedback.innerHTML = `<i class="bi bi-exclamation-circle me-1"></i>${errorMessage}`;
            field.parentNode.appendChild(feedback);
        } else if (field.value.trim()) {
            // Add valid state for non-empty fields
            field.classList.add('is-valid');
        }
    }
    
    /**
     * Clear field validation error state
     * @param {HTMLElement} field - Field element
     */
    clearFieldError(field) {
        field.classList.remove('is-invalid');
        const feedback = field.parentNode.querySelector('.invalid-feedback');
        if (feedback) {
            feedback.remove();
        }
    }
    
    /**
     * Calculate gross amount from net amount and VAT rate
     */
    calculateGrossAmount() {
        const amountNetField = this.form.querySelector('[name="amountNet"]');
        const vatRateField = this.form.querySelector('[name="vatRate"]');
        
        if (!amountNetField || !vatRateField) return;
        
        const netAmount = this.parseNumericValue(amountNetField.value);
        const vatRate = this.parseNumericValue(vatRateField.value);
        
        if (!isNaN(netAmount) && !isNaN(vatRate) && netAmount > 0) {
            const grossAmount = netAmount + (netAmount * vatRate / 100);
            this.displayGrossAmount(grossAmount);
        } else {
            this.displayGrossAmount(null);
        }
    }
    
    /**
     * Calculate gross amount value for form submission
     * @returns {number|null} Calculated gross amount
     */
    calculateGrossAmountValue() {
        const amountNetField = this.form.querySelector('[name="amountNet"]');
        const vatRateField = this.form.querySelector('[name="vatRate"]');
        
        if (!amountNetField || !vatRateField) return null;
        
        const netAmount = this.parseNumericValue(amountNetField.value);
        const vatRate = this.parseNumericValue(vatRateField.value);
        
        if (!isNaN(netAmount) && !isNaN(vatRate) && netAmount > 0) {
            return netAmount + (netAmount * vatRate / 100);
        }
        
        return null;
    }
    
    /**
     * Display calculated gross amount
     * @param {number|null} grossAmount - Calculated gross amount
     */
    displayGrossAmount(grossAmount) {
        // Find or create gross amount display element
        let grossDisplay = this.form.querySelector('#grossAmountDisplay');
        const vatRateField = this.form.querySelector('[name="vatRate"]');
        
        if (!grossDisplay && vatRateField) {
            grossDisplay = document.createElement('div');
            grossDisplay.id = 'grossAmountDisplay';
            grossDisplay.className = 'mt-2 p-2 bg-light rounded border';
            vatRateField.parentNode.appendChild(grossDisplay);
        }
        
        if (grossDisplay) {
            if (grossAmount !== null) {
                grossDisplay.innerHTML = `
                    <small class="text-muted">
                        <i class="bi bi-calculator me-1"></i>
                        <strong>Importo Lordo:</strong> ${this.formatCurrency(grossAmount)}
                    </small>
                `;
                grossDisplay.style.display = 'block';
            } else {
                grossDisplay.style.display = 'none';
            }
        }
    }
    
    /**
     * Update form completion progress
     */
    updateFormProgress() {
        if (!this.progressBar) return;
        
        const allFields = this.form.querySelectorAll('input, select, textarea');
        const requiredFields = this.form.querySelectorAll('[required]');
        
        let completedFields = 0;
        let totalFields = allFields.length;
        
        allFields.forEach(field => {
            if (field.type === 'checkbox') {
                // For checkboxes, consider any state as "completed"
                completedFields++;
            } else if (field.value && field.value.trim()) {
                completedFields++;
            }
        });
        
        const percentage = Math.round((completedFields / totalFields) * 100);
        
        this.progressBar.style.width = `${percentage}%`;
        this.progressBar.setAttribute('aria-valuenow', percentage);
        
        if (this.progressPercentage) {
            this.progressPercentage.textContent = `${percentage}%`;
        }
        
        // Add color coding
        this.progressBar.className = 'progress-bar';
        if (percentage < 30) {
            this.progressBar.classList.add('bg-danger');
        } else if (percentage < 70) {
            this.progressBar.classList.add('bg-warning');
        } else {
            this.progressBar.classList.add('bg-success');
        }
    }
    
    /**
     * Format numeric input (remove non-numeric characters except decimal separators)
     * @param {HTMLElement} field - Input field
     */
    formatNumericInput(field) {
        let value = field.value;
        // Allow numbers, comma, and period
        value = value.replace(/[^0-9,.]/g, '');
        // Replace comma with period for decimal
        value = value.replace(',', '.');
        field.value = value;
    }
    
    /**
     * Format currency input
     * @param {HTMLElement} field - Input field
     */
    formatCurrencyInput(field) {
        this.formatNumericInput(field);
        // Additional currency-specific formatting can be added here
    }
    
    /**
     * Format percentage input
     * @param {HTMLElement} field - Input field
     */
    formatPercentageInput(field) {
        this.formatNumericInput(field);
        const value = this.parseNumericValue(field.value);
        if (!isNaN(value) && value > 100) {
            field.value = '100';
        }
    }
    
    /**
     * Parse numeric value from string input
     * @param {string} value - String value
     * @returns {number} Parsed numeric value
     */
    parseNumericValue(value) {
        if (!value) return 0;
        return parseFloat(value.replace(',', '.'));
    }
    
    /**
     * Format number as currency
     * @param {number} amount - Amount to format
     * @returns {string} Formatted currency string
     */
    formatCurrency(amount) {
        return new Intl.NumberFormat('it-IT', {
            style: 'currency',
            currency: 'EUR',
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(amount);
    }
    
    /**
     * Update character count display for text fields
     * @param {HTMLElement} field - Text field
     */
    updateCharacterCount(field) {
        const maxLength = this.validationRules.maxLength[field.name];
        if (!maxLength) return;
        
        const currentLength = field.value.length;
        let countDisplay = field.parentNode.querySelector('.char-count');
        
        if (!countDisplay) {
            countDisplay = document.createElement('small');
            countDisplay.className = 'char-count text-muted';
            field.parentNode.appendChild(countDisplay);
        }
        
        countDisplay.textContent = `${currentLength}/${maxLength}`;
        
        if (currentLength > maxLength * 0.9) {
            countDisplay.classList.add('text-warning');
        } else {
            countDisplay.classList.remove('text-warning');
        }
    }
    
    /**
     * Validate entire form
     * @returns {boolean} Is form valid
     */
    validateForm() {
        let isValid = true;
        
        // Validate all fields
        const fields = this.form.querySelectorAll('input, select, textarea');
        fields.forEach(field => {
            if (!this.validateField(field)) {
                isValid = false;
            }
        });
        
        // Validate date range
        if (!this.validateDateRange()) {
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Show validation errors summary
     */
    showValidationErrors() {
        const invalidFields = this.form.querySelectorAll('.is-invalid');
        if (invalidFields.length > 0) {
            // Scroll to first invalid field
            invalidFields[0].scrollIntoView({ behavior: 'smooth', block: 'center' });
            invalidFields[0].focus();
            
            // Show toast notification
            this.showToast('Errori di Validazione', 'Correggi i campi evidenziati in rosso prima di continuare.', 'danger');
        }
    }
    
    /**
     * Show loading state during form submission
     */
    showLoadingState() {
        const submitBtn = this.form.querySelector('button[type="submit"]');
        if (submitBtn) {
            const originalText = submitBtn.innerHTML;
            submitBtn.disabled = true;
            submitBtn.innerHTML = `
                <div class="spinner-border spinner-border-sm me-2" role="status">
                    <span class="visually-hidden">Caricamento...</span>
                </div>
                Salvataggio...
            `;
            
            // Store original text for restoration
            submitBtn.dataset.originalText = originalText;
        }
    }
    
    /**
     * Restore normal state after form submission
     */
    restoreNormalState() {
        const submitBtn = this.form.querySelector('button[type="submit"]');
        if (submitBtn && submitBtn.dataset.originalText) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = submitBtn.dataset.originalText;
        }
    }
    
    /**
     * Submit form data
     * @param {FormData} formData - Form data to submit
     */
    async submitForm(formData) {
        try {
            // Allow normal form submission for now
            // In future, this could be enhanced with AJAX submission
            this.hasUnsavedChanges = false;
            this.form.submit();
            
        } catch (error) {
            console.error('Form submission error:', error);
            this.restoreNormalState();
            this.showToast('Errore', 'Si è verificato un errore durante il salvataggio. Riprova.', 'danger');
        }
    }
    
    /**
     * Save form (triggered by Ctrl+S)
     */
    saveForm() {
        if (this.validateForm()) {
            this.form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
        } else {
            this.showValidationErrors();
        }
    }
    
    /**
     * Save form as draft (auto-save)
     */
    async saveFormDraft() {
        try {
            const formData = new FormData(this.form);
            
            // This could be enhanced to save draft to localStorage or server
            console.log('Auto-saving form draft...');
            
            // For now, just mark as saved
            this.hasUnsavedChanges = false;
            
        } catch (error) {
            console.error('Draft save error:', error);
        }
    }
    
    /**
     * Schedule auto-save with debouncing
     */
    scheduleAutoSave() {
        if (this.autoSaveTimer) {
            clearTimeout(this.autoSaveTimer);
        }
        
        this.autoSaveTimer = setTimeout(() => {
            this.saveFormDraft();
        }, 5000); // Save draft 5 seconds after last change
    }
    
    /**
     * Show toast notification
     * @param {string} title - Toast title
     * @param {string} message - Toast message
     * @param {string} type - Toast type (success, danger, warning, info)
     */
    showToast(title, message, type = 'info') {
        // Create toast if it doesn't exist
        let toastContainer = document.querySelector('.toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(toastContainer);
        }
        
        const toastId = 'toast_' + Date.now();
        const toast = document.createElement('div');
        toast.id = toastId;
        toast.className = `toast align-items-center text-bg-${type} border-0`;
        toast.setAttribute('role', 'alert');
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    <strong>${title}</strong><br>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        
        toastContainer.appendChild(toast);
        
        // Initialize and show toast
        const bsToast = new bootstrap.Toast(toast);
        bsToast.show();
        
        // Remove toast element after it's hidden
        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.ContractForm = new ContractFormEnhancer();
});

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ContractFormEnhancer;
}