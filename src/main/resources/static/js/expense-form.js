/**
 * EXPENSE FORM JAVASCRIPT
 * This file handles all the dynamic functionality for the expense report form
 * including row management, calculations, validation, and attachments
 */

'use strict';

/**
 * ExpenseForm - Main class to handle expense form functionality
 * Encapsulates all form logic to avoid global namespace pollution
 */
class ExpenseForm {
    constructor() {
        // Configuration from server-side (injected via Thymeleaf)
        this.reportId = window.EXPENSE_CONFIG?.reportId || null;
        this.allItemDocs = window.EXPENSE_CONFIG?.itemDocs || {};
        this.docTypes = window.EXPENSE_CONFIG?.docTypes || [];
        this.csrfToken = window.EXPENSE_CONFIG?.csrfToken || '';
        this.csrfHeader = window.EXPENSE_CONFIG?.csrfHeader || '';
        
        // DOM element references
        this.elements = {
            form: document.getElementById('mainForm'),
            itemsBody: document.getElementById('itemsBody'),
            rowTemplate: document.getElementById('rowTemplate'),
            totalDisplay: document.getElementById('totalDisplay'),
            reimbursableDisplay: document.getElementById('reimbursableDisplay'),
            nonReimbursableDisplay: document.getElementById('nonReimbursableDisplay'),
            expenseReportTotal: document.getElementById('expenseReportTotal'),
            reimbursableTotal: document.getElementById('reimbursableTotal'),
            nonReimbursableTotal: document.getElementById('nonReimbursableTotal'),
            employeeSelect: document.getElementById('employeeSelect'),
            expenseReportNum: document.getElementById('expenseReportNum'),
            attachmentsModal: document.getElementById('attachmentsModal')
        };
        
        // Utility configurations
        this.currencyFormatter = new Intl.NumberFormat('it-IT', {
            style: 'currency',
            currency: 'EUR'
        });
        
        this.init();
    }
    
    /**
     * Initialize all event listeners and perform initial calculations
     */
    init() {
        this.attachEventListeners();
        this.attachAmountListeners();
        this.updateTotals();
        this.setupFormValidation();
        this.setupAttachmentsModal();
        
        // Only auto-update report number for new reports
        if (!this.reportId) {
            this.updateReportNum();
        }
        
        console.log('ExpenseForm initialized successfully');
    }
    
    /**
     * Attach all event listeners to form elements
     */
    attachEventListeners() {
        // Employee selection change for report number generation
        if (this.elements.employeeSelect) {
            this.elements.employeeSelect.addEventListener('change', () => {
                this.updateReportNum();
            });
        }
        
        // Reimbursable total input for calculations
        if (this.elements.reimbursableTotal) {
            this.elements.reimbursableTotal.addEventListener('input', () => {
                this.updateNonReimbursable();
            });
        }
        
        // Form submission validation
        if (this.elements.form) {
            this.elements.form.addEventListener('submit', (event) => {
                this.handleFormSubmission(event);
            });
        }
        
        // Add row button (delegated event listener for dynamic content)
        document.addEventListener('click', (event) => {
            if (event.target.closest('.btn-add-row')) {
                event.preventDefault();
                this.addRow();
            }
        });
    }
    
    /**
     * Add event listeners to all amount input fields
     * This method is called whenever new rows are added
     */
    attachAmountListeners() {
        const amountInputs = document.querySelectorAll('.item-amount');
        amountInputs.forEach(input => {
            // Remove existing listener to prevent duplicates
            input.removeEventListener('input', this.updateTotalsHandler);
            // Add new listener
            input.addEventListener('input', this.updateTotalsHandler);
        });
    }
    
    /**
     * Handler for amount input changes (bound to preserve 'this' context)
     */
    updateTotalsHandler = () => {
        this.updateTotals();
    }
    
    /**
     * Format currency values using Italian locale
     * @param {number} amount - The amount to format
     * @returns {string} Formatted currency string
     */
    formatCurrency(amount) {
        return this.currencyFormatter.format(amount || 0);
    }
    
    /**
     * Add a new expense item row to the table
     * Clones the template and attaches necessary event listeners
     */
    addRow() {
        if (!this.elements.itemsBody || !this.elements.rowTemplate) {
            console.error('Required elements for adding rows not found');
            return;
        }
        
        const clone = this.elements.rowTemplate.content.cloneNode(true);
        this.elements.itemsBody.appendChild(clone);
        
        // Re-attach amount listeners to include the new row
        this.attachAmountListeners();
        
        // Add fade-in animation to the new row
        const newRow = this.elements.itemsBody.lastElementChild;
        if (newRow) {
            newRow.style.opacity = '0';
            newRow.style.transform = 'translateY(-10px)';
            
            // Trigger animation on next frame
            requestAnimationFrame(() => {
                newRow.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
                newRow.style.opacity = '1';
                newRow.style.transform = 'translateY(0)';
            });
        }
        
        console.log('New expense row added successfully');
    }
    
    /**
     * Remove an expense item row from the table
     * @param {HTMLElement} button - The remove button that was clicked
     */
    removeRow(button) {
        const row = button.closest('tr');
        if (!row) {
            console.error('Could not find row to remove');
            return;
        }
        
        // Add fade-out animation before removal
        row.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        row.style.opacity = '0';
        row.style.transform = 'translateX(-20px)';
        
        // Remove the row after animation completes
        setTimeout(() => {
            row.remove();
            this.updateTotals();
        }, 300);
        
        console.log('Expense row removed successfully');
    }
    
    /**
     * Calculate and update all total fields
     * Updates both display values and hidden form fields
     */
    updateTotals() {
        let total = 0;
        const amountInputs = document.querySelectorAll('.item-amount');
        
        // Sum all valid amount inputs
        amountInputs.forEach(input => {
            const value = parseFloat(input.value);
            if (!isNaN(value) && value > 0) {
                total += value;
            }
        });
        
        // Update hidden field for form submission
        if (this.elements.expenseReportTotal) {
            this.elements.expenseReportTotal.value = total.toFixed(2);
        }
        
        // Update display
        if (this.elements.totalDisplay) {
            this.elements.totalDisplay.textContent = this.formatCurrency(total);
        }
        
        // Update non-reimbursable calculation
        this.updateNonReimbursable();
        
        console.log('Totals updated - Total:', total.toFixed(2));
    }
    
    /**
     * Calculate and update non-reimbursable amount
     * Called when total or reimbursable amount changes
     */
    updateNonReimbursable() {
        const total = parseFloat(this.elements.expenseReportTotal?.value || '0');
        const reimbursable = parseFloat(this.elements.reimbursableTotal?.value || '0');
        const nonReimbursable = Math.max(0, total - reimbursable);
        
        // Update hidden field
        if (this.elements.nonReimbursableTotal) {
            this.elements.nonReimbursableTotal.value = nonReimbursable.toFixed(2);
        }
        
        // Update displays
        if (this.elements.reimbursableDisplay) {
            this.elements.reimbursableDisplay.textContent = this.formatCurrency(reimbursable);
        }
        
        if (this.elements.nonReimbursableDisplay) {
            this.elements.nonReimbursableDisplay.textContent = this.formatCurrency(nonReimbursable);
        }
        
        console.log('Non-reimbursable amount updated:', nonReimbursable.toFixed(2));
    }
    
    /**
     * Generate expense report number based on selected employee
     * Format: YYYY/MM/DD/[EmployeeInitials]
     */
    updateReportNum() {
        if (!this.elements.employeeSelect || !this.elements.expenseReportNum) {
            return;
        }
        
        const baseNum = this.elements.expenseReportNum.dataset.base || '';
        if (!baseNum) {
            // Try to extract base from existing value
            const match = (this.elements.expenseReportNum.value || '').match(/^(\d+\/\d+\/\d+\/)/);
            if (match) {
                this.elements.expenseReportNum.dataset.base = match[1];
            } else {
                console.warn('No base report number found');
                return;
            }
        }
        
        const selectedOption = this.elements.employeeSelect.options[this.elements.employeeSelect.selectedIndex];
        if (!selectedOption || !selectedOption.text) {
            return;
        }
        
        // Extract employee initials from full name
        const fullName = selectedOption.text.trim();
        const nameParts = fullName.split(' ');
        const firstName = nameParts[0] || '';
        const lastName = nameParts.slice(1).join(' ') || '';
        
        // Generate initials: LastName first letter + FirstName first letter
        const initials = (lastName.charAt(0) + firstName.charAt(0)).toUpperCase();
        
        // Update the report number
        this.elements.expenseReportNum.value = baseNum + initials;
        
        console.log('Report number updated:', this.elements.expenseReportNum.value);
    }
    
    /**
     * Setup form validation with Bootstrap validation classes
     */
    setupFormValidation() {
        if (!this.elements.form) return;
        
        // Add Bootstrap validation classes on input
        const inputs = this.elements.form.querySelectorAll('input[required], select[required]');
        inputs.forEach(input => {
            input.addEventListener('input', () => {
                this.validateField(input);
            });
            
            input.addEventListener('blur', () => {
                this.validateField(input);
            });
        });
    }
    
    /**
     * Validate individual form field
     * @param {HTMLElement} field - The form field to validate
     */
    validateField(field) {
        const isValid = field.checkValidity();
        
        // Remove existing validation classes
        field.classList.remove('is-valid', 'is-invalid');
        
        // Add appropriate validation class
        if (field.value.trim() !== '') {
            field.classList.add(isValid ? 'is-valid' : 'is-invalid');
        }
        
        return isValid;
    }
    
    /**
     * Handle form submission with validation
     * @param {Event} event - The form submission event
     */
    handleFormSubmission(event) {
        const form = event.target;
        
        if (!form.checkValidity()) {
            event.preventDefault();
            event.stopPropagation();
            
            // Highlight invalid fields
            const invalidFields = form.querySelectorAll(':invalid');
            invalidFields.forEach(field => {
                this.validateField(field);
            });
            
            // Focus first invalid field
            if (invalidFields.length > 0) {
                invalidFields[0].focus();
            }
            
            // Show validation feedback
            this.showValidationMessage('Compila tutti i campi obbligatori prima di procedere.', 'error');
        } else {
            // Update totals one final time before submission
            this.updateTotals();
            
            // Show loading state
            this.showLoadingState();
        }
        
        form.classList.add('was-validated');
    }
    
    /**
     * Show validation message to user
     * @param {string} message - The message to display
     * @param {string} type - Message type ('error', 'success', 'warning')
     */
    showValidationMessage(message, type = 'info') {
        // Remove existing alerts
        const existingAlerts = document.querySelectorAll('.validation-alert');
        existingAlerts.forEach(alert => alert.remove());
        
        // Create new alert
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert validation-alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show`;
        alertDiv.innerHTML = `
            <i class="bi bi-${type === 'error' ? 'exclamation-triangle' : 'info-circle'} me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        // Insert at the beginning of the form
        const form = this.elements.form;
        if (form) {
            form.insertBefore(alertDiv, form.firstChild);
            
            // Scroll to alert
            alertDiv.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }
    
    /**
     * Show loading state during form submission
     */
    showLoadingState() {
        const submitButtons = document.querySelectorAll('button[type="submit"]');
        submitButtons.forEach(button => {
            button.disabled = true;
            const originalText = button.textContent;
            button.innerHTML = '<i class="bi bi-hourglass-split me-2"></i> Salvataggio...';
            
            // Store original text for potential restoration
            button.dataset.originalText = originalText;
        });
    }
    
    /**
     * Setup attachments modal functionality
     */
    setupAttachmentsModal() {
        if (!this.elements.attachmentsModal) return;
        
        this.elements.attachmentsModal.addEventListener('show.bs.modal', (event) => {
            this.handleAttachmentsModalShow(event);
        });
    }
    
    /**
     * Handle attachments modal show event
     * @param {Event} event - The modal show event
     */
    handleAttachmentsModalShow(event) {
        const button = event.relatedTarget;
        const itemId = button.getAttribute('data-item-id');
        const itemDesc = button.getAttribute('data-item-desc');
        
        // Update modal title
        const modalTitle = document.getElementById('modalItemDescription');
        if (modalTitle) {
            modalTitle.textContent = itemDesc || 'Voce non specificata';
        }
        
        // Update upload form action
        const uploadForm = document.getElementById('docUploadForm');
        if (uploadForm && itemId) {
            uploadForm.action = `/fleet/expense-reports/items/${itemId}/docs`;
        }
        
        // Populate existing documents list
        this.populateExistingDocuments(itemId);
        
        console.log('Attachments modal opened for item:', itemId);
    }
    
    /**
     * Populate existing documents list in the modal
     * @param {string} itemId - The expense item ID
     */
    populateExistingDocuments(itemId) {
        const docsList = document.getElementById('existingDocsList');
        if (!docsList) return;
        
        // Clear existing list
        docsList.innerHTML = '';
        
        const docs = this.allItemDocs[itemId] || [];
        
        if (docs.length === 0) {
            docsList.innerHTML = '<li class="list-group-item text-muted">Nessun documento presente.</li>';
            return;
        }
        
        docs.forEach(doc => {
            const fileName = doc.path.substring(doc.path.lastIndexOf('/') + 1);
            const deleteUrl = `/fleet/expense-reports/items/${itemId}/docs/${doc.id}/delete`;
            const downloadUrl = `/fleet/expense-reports/docs/${doc.id}`;
            
            const listItem = document.createElement('li');
            listItem.className = 'list-group-item d-flex justify-content-between align-items-center';
            listItem.innerHTML = `
                <div class="d-flex align-items-center">
                    <i class="bi bi-file-earmark me-2 text-primary"></i>
                    <a href="${downloadUrl}" class="text-decoration-none" target="_blank">${fileName}</a>
                </div>
                <button type="button" class="btn btn-sm btn-outline-danger" 
                        onclick="return confirm('Sei sicuro di voler eliminare questo documento?') && (window.location.href='${deleteUrl}');"
                        title="Elimina documento">
                    <i class="bi bi-trash"></i>
                </button>
            `;
            
            docsList.appendChild(listItem);
        });
    }
}

/**
 * Global function to remove row (called from HTML onclick)
 * Delegates to the ExpenseForm instance
 * @param {HTMLElement} button - The remove button
 */
window.removeRow = function(button) {
    if (window.expenseForm) {
        window.expenseForm.removeRow(button);
    }
};

/**
 * Initialize the expense form when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize the expense form
    window.expenseForm = new ExpenseForm();
    
    console.log('Expense form module loaded and initialized');
});