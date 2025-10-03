/**
 * Supplier Form JavaScript Module
 * Unified version combining referents management with form validation and UX enhancements
 */

/**
 * Manages dynamic referents table functionality
 * Based on existing SupplierReferentsManager with enhancements
 */
class SupplierReferentsManager {
    constructor() {
        this.table = document.getElementById('referentsTable');
        this.tbody = this.table ? this.table.querySelector('tbody') : null;
        this.addBtn = document.getElementById('addReferentBtn');

        if (this.table && this.addBtn && this.tbody) {
            this.init();
        }
    }

    init() {
        // Original event listeners - preserved as-is
        this.addBtn.addEventListener('click', () => this.addReferent());
        this.tbody.addEventListener('click', (e) => {
            if (e.target.closest('.btn-remove-referent')) {
                const row = e.target.closest('tr');
                // Add smooth removal animation
                this.removeReferentWithAnimation(row);
            }
        });
        this.updateIndices();
    }

    addReferent() {
        const empty = this.tbody.querySelector('.empty-state-row');
        if (empty) empty.remove();

        const index = this.tbody.querySelectorAll('tr').length;
        const row = document.createElement('tr');
        row.className = 'fade-in-up'; // Add animation class
        row.innerHTML = `
            <td><input class="form-control form-control-sm" name="referents[${index}].name" placeholder="Nome" /></td>
            <td><input class="form-control form-control-sm" name="referents[${index}].phone" placeholder="+39 xxx xxx xxxx" /></td>
            <td><input class="form-control form-control-sm" name="referents[${index}].email" type="email" placeholder="email@example.com" /></td>
            <td class="text-center"><button type="button" class="btn btn-outline-danger btn-sm btn-remove-referent" title="Rimuovi referente" aria-label="Rimuovi referente"><i class="bi bi-trash"></i></button></td>
        `;
        this.tbody.appendChild(row);
        this.updateIndices();

        // Focus on first input of new row for better UX
        const firstInput = row.querySelector('input');
        if (firstInput) {
            firstInput.focus();
        }
    }

    /**
     * Remove referent with smooth animation
     * @param {HTMLElement} row - The row to remove
     */
    removeReferentWithAnimation(row) {
        row.style.transition = 'opacity 0.3s ease-out, transform 0.3s ease-out';
        row.style.opacity = '0';
        row.style.transform = 'translateX(-20px)';
        
        setTimeout(() => {
            row.remove();
            this.updateIndices();
            this.checkEmptyState();
        }, 300);
    }

    updateIndices() {
        const rows = this.tbody.querySelectorAll('tr:not(.empty-state-row)');
        rows.forEach((row, index) => {
            row.querySelectorAll('input').forEach(input => {
                const name = input.getAttribute('name');
                if (name && name.includes('referents[')) {
                    input.setAttribute('name', name.replace(/referents\[\d+\]/, `referents[${index}]`));
                }
            });
        });
    }

    checkEmptyState() {
        if (!this.tbody.querySelector('tr')) {
            const row = document.createElement('tr');
            row.className = 'empty-state-row';
            row.innerHTML = `<td colspan="4" class="text-center py-4 text-muted"><i class="bi bi-person-plus-fill me-2"></i>Nessun referente presente. Clicca "Aggiungi Referente" per iniziare.</td>`;
            this.tbody.appendChild(row);
        }
    }
}

/**
 * Form Validation and Enhancement Manager
 * Adds validation, formatting, and UX improvements to the supplier form
 */
class SupplierFormEnhancer {
    constructor() {
        this.init();
    }

    init() {
        this.setupFormValidation();
        this.addFormEnhancements();
        this.setupFieldValidation();
        this.setupReferentValidation();
    }

    /**
     * Setup real-time validation for main form fields
     */
    setupFieldValidation() {
        // VAT Number validation
        const vatInput = document.querySelector('input[th\\:field="*{vatNumber}"]');
        if (vatInput) {
            vatInput.addEventListener('blur', (e) => this.validateVatNumber(e.target));
        }

        // Email validation for company email and PEC
        const companyEmailInput = document.querySelector('input[th\\:field="*{companyEmail}"]');
        const pecInput = document.querySelector('input[th\\:field="*{pec}"]');
        
        [companyEmailInput, pecInput].forEach(input => {
            if (input) {
                input.addEventListener('blur', (e) => this.validateEmail(e.target));
            }
        });

        // IBAN validation with formatting
        const ibanInput = document.querySelector('input[th\\:field="*{iban}"]');
        if (ibanInput) {
            ibanInput.addEventListener('input', (e) => this.formatIban(e.target));
            ibanInput.addEventListener('blur', (e) => this.validateIban(e.target));
        }

        // Phone validation with formatting
        const phoneInput = document.querySelector('input[th\\:field="*{companyPhone}"]');
        if (phoneInput) {
            phoneInput.addEventListener('input', (e) => this.formatPhoneNumber(e.target));
            phoneInput.addEventListener('blur', (e) => this.validatePhoneNumber(e.target));
        }
    }

    /**
     * Setup validation for referent fields using event delegation
     */
    setupReferentValidation() {
        const referentsTable = document.getElementById('referentsTable');
        if (!referentsTable) return;

        // Use event delegation to handle dynamic referent inputs
        referentsTable.addEventListener('blur', (e) => {
            if (e.target.matches('input[name*=".email"]')) {
                this.validateEmail(e.target);
            } else if (e.target.matches('input[name*=".phone"]')) {
                this.validatePhoneNumber(e.target);
            } else if (e.target.matches('input[name*=".name"]')) {
                this.validateReferentName(e.target);
            }
        }, true);

        // Format phone numbers as user types
        referentsTable.addEventListener('input', (e) => {
            if (e.target.matches('input[name*=".phone"]')) {
                this.formatPhoneNumber(e.target);
            }
        });
    }

    /**
     * Validate referent name
     */
    validateReferentName(input) {
        const value = input.value.trim();
        
        if (value.length === 0) {
            this.clearValidation(input);
        } else if (value.length < 2) {
            this.setFieldValid(input, false, 'Il nome deve contenere almeno 2 caratteri');
        } else {
            this.setFieldValid(input, true);
        }
    }

    /**
     * Validate email format
     */
    validateEmail(input) {
        const value = input.value.trim();
        
        if (value === '') {
            this.clearValidation(input);
            return;
        }
        
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        
        if (emailRegex.test(value)) {
            this.setFieldValid(input, true);
        } else {
            this.setFieldValid(input, false, 'Inserisci un indirizzo email valido');
        }
    }

    /**
     * Format phone number with Italian formatting
     */
    formatPhoneNumber(input) {
        let value = input.value.replace(/\D/g, '');
        
        if (value.startsWith('39') && value.length > 2) {
            value = '+39 ' + value.substring(2);
        } else if (value.startsWith('3') && value.length <= 10) {
            if (value.length > 3) {
                value = '+39 ' + value.substring(0, 3) + ' ' + value.substring(3);
            }
        }
        
        input.value = value;
    }

    /**
     * Validate phone number
     */
    validatePhoneNumber(input) {
        const value = input.value.trim();
        
        if (value === '') {
            this.clearValidation(input);
            return;
        }
        
        const phoneRegex = /^(\+39|0)[0-9\s]{8,13}$/;
        
        if (phoneRegex.test(value)) {
            this.setFieldValid(input, true);
        } else {
            this.setFieldValid(input, false, 'Inserisci un numero di telefono valido');
        }
    }

    /**
     * Validate Italian VAT number or Tax Code
     */
    validateVatNumber(input) {
        const value = input.value.trim().toUpperCase();
        
        if (value === '') {
            this.clearValidation(input);
            return;
        }
        
        const vatRegex = /^[0-9]{11}$/; // P.IVA
        const cfRegex = /^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$/; // Codice Fiscale
        
        if (vatRegex.test(value) || cfRegex.test(value)) {
            this.setFieldValid(input, true);
            input.value = value;
        } else {
            this.setFieldValid(input, false, 'Inserisci una P.IVA (11 cifre) o un Codice Fiscale valido');
        }
    }

    /**
     * Format IBAN with spaces for readability
     */
    formatIban(input) {
        let value = input.value.replace(/\s/g, '').toUpperCase();
        
        if (value.length > 4) {
            value = value.replace(/(.{4})/g, '$1 ').trim();
        }
        
        input.value = value;
    }

    /**
     * Validate Italian IBAN format
     */
    validateIban(input) {
        const value = input.value.replace(/\s/g, '').toUpperCase();
        
        if (value === '') {
            this.clearValidation(input);
            return;
        }
        
        const ibanRegex = /^IT[0-9]{2}[A-Z][0-9]{10}[A-Z0-9]{12}$/;
        
        if (ibanRegex.test(value)) {
            this.setFieldValid(input, true);
        } else {
            this.setFieldValid(input, false, 'Inserisci un IBAN italiano valido');
        }
    }

    /**
     * Set field validation state with visual feedback
     */
    setFieldValid(input, isValid, message = '') {
        const existingFeedback = input.parentNode.querySelector('.invalid-feedback, .valid-feedback');
        if (existingFeedback) {
            existingFeedback.remove();
        }
        
        input.classList.remove('is-valid', 'is-invalid');
        
        if (isValid) {
            input.classList.add('is-valid');
        } else {
            input.classList.add('is-invalid');
            
            const errorDiv = document.createElement('div');
            errorDiv.className = 'invalid-feedback';
            errorDiv.textContent = message;
            input.parentNode.appendChild(errorDiv);
        }
    }

    /**
     * Clear validation state for a field
     */
    clearValidation(input) {
        input.classList.remove('is-valid', 'is-invalid');
        const feedback = input.parentNode.querySelector('.invalid-feedback, .valid-feedback');
        if (feedback) {
            feedback.remove();
        }
    }

    /**
     * Setup form submission validation
     */
    setupFormValidation() {
        const form = document.querySelector('form[th\\:object="${supplier}"]');
        if (!form) return;

        form.addEventListener('submit', (e) => {
            const isValid = this.validateForm();
            if (!isValid) {
                e.preventDefault();
                e.stopPropagation();
                this.showNotification('warning', 'Correggi gli errori evidenziati prima di continuare');
                
                const firstInvalid = form.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                    firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }
        });
    }

    /**
     * Validate entire form
     */
    validateForm() {
        let isValid = true;
        
        const requiredFields = document.querySelectorAll('input[required]');
        requiredFields.forEach(field => {
            if (field.value.trim() === '') {
                this.setFieldValid(field, false, 'Questo campo è obbligatorio');
                isValid = false;
            }
        });
        
        const invalidFields = document.querySelectorAll('.is-invalid');
        if (invalidFields.length > 0) {
            isValid = false;
        }
        
        return isValid;
    }

    /**
     * Add visual enhancements to form
     */
    addFormEnhancements() {
        this.addInputIcon('input[th\\:field="*{companyEmail}"]', 'bi-envelope');
        this.addInputIcon('input[th\\:field="*{companyPhone}"]', 'bi-telephone');
        this.addInputIcon('input[th\\:field="*{pec}"]', 'bi-envelope-at');
        this.addInputIcon('input[th\\:field="*{iban}"]', 'bi-bank');
        this.addInputIcon('input[th\\:field="*{vatNumber}"]', 'bi-receipt');
        this.addInputIcon('input[th\\:field="*{sdiCode}"]', 'bi-qr-code');
        
        this.addOptionalIndicators();
    }

    /**
     * Add icon to input field
     */
    addInputIcon(selector, iconClass) {
        const input = document.querySelector(selector);
        if (!input || input.parentNode.querySelector('.input-icon')) return;
        
        const parent = input.parentNode;
        parent.classList.add('input-group-icon');
        
        const icon = document.createElement('i');
        icon.className = `bi ${iconClass} input-icon`;
        icon.setAttribute('aria-hidden', 'true');
        
        parent.insertBefore(icon, input);
    }

    /**
     * Add optional indicators to non-required fields only
     */
    addOptionalIndicators() {
        const optionalInputs = document.querySelectorAll('input:not([required])');
        
        optionalInputs.forEach(input => {
            // Skip referents table inputs and hidden inputs
            if (input.closest('#referentsTable') || input.type === 'hidden') return;
            
            const label = input.parentNode.querySelector('.form-label');
            if (label && !label.querySelector('.optional-indicator') && !label.querySelector('.required-indicator')) {
                const indicator = document.createElement('span');
                indicator.className = 'optional-indicator';
                indicator.textContent = 'opzionale';
                label.appendChild(indicator);
            }
        });
    }

    /**
     * Show notification to user
     */
    showNotification(type, message) {
        const existing = document.querySelectorAll('.form-notification');
        existing.forEach(n => n.remove());
        
        const notification = document.createElement('div');
        notification.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show form-notification`;
        notification.style.cssText = `
            position: fixed; 
            top: 20px; 
            right: 20px; 
            z-index: 9999; 
            min-width: 300px;
        `;
        notification.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;
        
        document.body.appendChild(notification);
        
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);
    }
}

// Initialize both managers when DOM is loaded
window.addEventListener('DOMContentLoaded', () => {
    window.supplierReferentsManager = new SupplierReferentsManager();
    window.supplierFormEnhancer = new SupplierFormEnhancer();
});

// Export for potential external use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { SupplierReferentsManager, SupplierFormEnhancer };
}